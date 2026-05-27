package com.personal.game2048.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.game2048.storage.GameSaveStorage
import com.personal.game2048.storage.StatsStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val board = GameBoard()
    private val statsStorage = StatsStorage(application)
    private val saveStorage = GameSaveStorage(application)

    var grid by mutableStateOf(Array(4) { IntArray(4) })
        private set
    var score by mutableIntStateOf(0)
        private set
    var bestScore by mutableIntStateOf(0)
        private set
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    var isGameOver by mutableStateOf(false)
        private set
    var hasWon by mutableStateOf(false)
        private set
    var showWinDialog by mutableStateOf(false)
        private set
    var showGameOverDialog by mutableStateOf(false)
        private set
    var achievementMessage by mutableStateOf<String?>(null)
        private set

    private val undoStack = ArrayDeque<BoardState>(5)
    private var timerJob: Job? = null
    private var gameActive = false

    init {
        bestScore = statsStorage.load().bestScore
    }

    fun startNewGame() {
        board.reset()
        undoStack.clear()
        score = 0
        elapsedSeconds = 0
        isGameOver = false
        hasWon = false
        showWinDialog = false
        showGameOverDialog = false
        gameActive = true
        updateGrid()
        saveStorage.clearSave()

        val stats = statsStorage.load()
        statsStorage.save(stats.copy(totalGames = stats.totalGames + 1))
        statsStorage.updateStreak()
        startTimer()
    }

    fun continueGame() {
        val saved = saveStorage.loadGame() ?: run {
            startNewGame()
            return
        }
        board.loadState(saved.first, saved.second)
        score = saved.second
        elapsedSeconds = saved.third
        undoStack.clear()
        isGameOver = false
        hasWon = board.hasWon
        showWinDialog = false
        showGameOverDialog = false
        gameActive = true
        updateGrid()
        startTimer()
    }

    fun move(direction: Direction) {
        if (!gameActive || isGameOver) return

        val stateBefore = board.captureState()

        if (board.move(direction)) {
            if (undoStack.size >= 5) undoStack.removeFirst()
            undoStack.addLast(stateBefore)

            score = board.score
            if (score > bestScore) bestScore = score
            updateGrid()

            saveStorage.saveGame(board.toFlatList(), score, elapsedSeconds)

            if (board.hasWon && !board.winAcknowledged && !showWinDialog) {
                showWinDialog = true
                hasWon = true
            }

            if (board.isGameOver()) {
                isGameOver = true
                showGameOverDialog = true
                onGameEnd(won = false)
            }
        }
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val state = undoStack.removeLast()
        board.restoreState(state)
        score = board.score
        updateGrid()
        saveStorage.saveGame(board.toFlatList(), score, elapsedSeconds)
        isGameOver = false
        showGameOverDialog = false
        return true
    }

    fun acknowledgeWin() {
        showWinDialog = false
        board.winAcknowledged = true
    }

    fun onGameEnd(won: Boolean) {
        stopTimer()
        gameActive = false
        saveStorage.clearSave()

        var stats = statsStorage.load()
        stats = stats.copy(
            totalSeconds = stats.totalSeconds + elapsedSeconds,
            highestTile = maxOf(stats.highestTile, board.highestTile())
        )

        if (won) {
            stats = stats.copy(gamesWon = stats.gamesWon + 1)
            if (stats.bestWinTimeSecs == 0 || elapsedSeconds < stats.bestWinTimeSecs) {
                stats = stats.copy(bestWinTimeSecs = elapsedSeconds)
            }
        } else {
            stats = stats.copy(gamesLost = stats.gamesLost + 1)
        }

        if (score > stats.bestScore) {
            stats = stats.copy(
                bestScore = score,
                bestScoreTimeSecs = elapsedSeconds
            )
        }

        val (updatedStats, newAchievements) = statsStorage.checkAchievements(stats, elapsedSeconds)
        statsStorage.save(updatedStats)

        if (newAchievements.isNotEmpty()) {
            achievementMessage = "Achievement Unlocked: ${newAchievements.first()}"
            viewModelScope.launch {
                delay(3000)
                achievementMessage = null
            }
        }
    }

    fun onWinKeepPlaying() {
        acknowledgeWin()
        onGameEnd(won = true)
        gameActive = true
        startTimer()
    }

    fun onWinNewGame() {
        acknowledgeWin()
        onGameEnd(won = true)
        startNewGame()
    }

    fun saveOnExit() {
        if (gameActive && !isGameOver) {
            saveStorage.saveGame(board.toFlatList(), score, elapsedSeconds)
        }
        stopTimer()
    }

    fun pauseTimer() { stopTimer() }

    fun resumeTimer() {
        if (gameActive && !isGameOver) startTimer()
    }

    fun dismissAchievement() { achievementMessage = null }

    fun dismissGameOver() { showGameOverDialog = false }

    private fun updateGrid() {
        grid = Array(4) { r -> IntArray(4) { c -> board.grid[r][c] } }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
