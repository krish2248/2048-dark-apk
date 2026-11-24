package com.personal.game2048.game

import android.app.Application
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.game2048.data.DailyState
import com.personal.game2048.data.GameRecord
import com.personal.game2048.data.PendingRun
import com.personal.game2048.data.SavedRun
import com.personal.game2048.data.Stats
import com.personal.game2048.data.Store
import com.personal.game2048.fx.Feedback
import com.personal.game2048.net.Leaderboard
import com.personal.game2048.ui.board.BoardAnimator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

enum class NoticeKind { TILE, RANK, ACHIEVEMENT, GOAL, RECORD, LEVEL }

data class Notice(val kind: NoticeKind, val title: String, val text: String, val rank: Rank? = null, val exp: Int = 0)

data class GameOver(val timeUp: Boolean, val newBest: Boolean, val previousBest: Long)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    val store = Store.get(app)
    val leaderboard = Leaderboard(store)
    val feedback = Feedback(app)
    val animator = BoardAnimator()

    var mode by mutableStateOf(GameMode.CLASSIC)
        private set
    var score by mutableLongStateOf(0L)
        private set
    var best by mutableLongStateOf(0L)
        private set
    var topExp by mutableIntStateOf(0)
        private set
    var seconds by mutableIntStateOf(0)
        private set
    /** Blitz: 1 = full time left, 0 = time up. Read only while drawing. */
    var timeLeft by mutableFloatStateOf(1f)
        private set
    var moves by mutableIntStateOf(0)
        private set
    var undosLeft by mutableIntStateOf(0)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var paused by mutableStateOf(false)
        private set
    var gameOver by mutableStateOf<GameOver?>(null)
        private set
    val notices = mutableStateListOf<Notice>()

    private var engine = Engine(4, 0)
    private var seed = 0
    private val undo = ArrayDeque<Snapshot>()
    private var undosUsed = 0
    private var log = StringBuilder()
    private var verifiable = true
    private var peakScore = 0L
    private var startBest = 0L
    private var recordAnnounced = false
    private var open = false

    private var accumulatedMs = 0L
    private var clockStart = 0L
    private var statsMs = 0L
    private var ticker: Job? = null
    private var saver: Job? = null

    init {
        viewModelScope.launch {
            store.settings.collect {
                feedback.soundOn = it.sound
                feedback.hapticsOn = it.haptics
                animator.slideMs = it.speed.ms
                animator.effects = it.effects
            }
        }
        leaderboard.sync()
    }

    // ---- opening and closing runs ----

    fun hasRun(m: GameMode) = store.runs.value.containsKey(m.id)

    fun open(m: GameMode, fresh: Boolean) {
        val saved = if (fresh) null else store.runs.value[m.id]
        if (fresh) store.runs.value[m.id]?.let { abandon(it) }
        mode = m
        store.updateSettings { it.copy(lastMode = m.id) }
        undo.clear()
        gameOver = null
        paused = false
        notices.clear()
        if (saved != null) {
            seed = saved.seed
            engine = Engine(m.size, seed).apply { restore(saved.state) }
            undo.addAll(saved.undo)
            undosUsed = saved.undosUsed
            log = StringBuilder(saved.log)
            accumulatedMs = saved.elapsedMs
            moves = saved.moves
            verifiable = saved.verifiable
            topExp = saved.topExp
            peakScore = saved.peakScore
            paused = m.timed && moves > 0
        } else {
            startFresh()
        }
        startBest = store.stats.value.mode(m).bestScore
        recordAnnounced = engine.score >= startBest
        best = maxOf(startBest, engine.score)
        score = engine.score
        statsMs = accumulatedMs
        open = true
        refresh()
        animator.reset(m.size, engine.cells, engine.ids, animate = true)
        // the process may have died on the game-over screen
        val timeUp = m.timed && accumulatedMs >= m.timeLimitMs
        if (saved != null && (timeUp || engine.isStuck())) {
            paused = false
            gameOver = GameOver(timeUp, newBest = false, previousBest = startBest)
        }
    }

    fun ensureOpen() {
        if (!open) open(GameMode.of(store.settings.value.lastMode), fresh = false)
    }

    private fun startFresh() {
        seed = Random.nextInt()
        engine = Engine(mode.size, seed).apply { start() }
        undosUsed = 0
        log = StringBuilder()
        accumulatedMs = 0
        moves = 0
        verifiable = true
        topExp = engine.maxExp
        peakScore = 0
    }

    fun newGame() {
        stopClock()
        if (gameOver == null && moves > 0) record(finished = false)
        store.clearRun(mode)
        open(mode, fresh = true)
    }

    /** A saved run the player chose not to continue still belongs in the history. */
    private fun abandon(run: SavedRun) {
        if (run.moves > 0) {
            val m = GameMode.of(run.mode)
            addHistory(m, run.peakScore, run.topExp, run.moves, run.elapsedMs, finished = false)
            if (m.ranked && run.verifiable) {
                leaderboard.submit(PendingRun(run.mode, run.seed, run.log, run.elapsedMs, run.state.score))
            }
        }
        store.clearRun(GameMode.of(run.mode))
    }

    fun leave() {
        if (!open) return
        stopClock()
        if (gameOver != null) closeFinished() else if (moves > 0) saveNow()
        submitIfWorthIt()
        open = false
    }

    /** Commits a finished run once the player moves on; until then undo can still rescue it. */
    private fun closeFinished() {
        record(finished = true)
        store.clearRun(mode)
        gameOver = null
        moves = 0
    }

    fun playAgain() {
        closeFinished()
        open(mode, fresh = true)
    }

    fun onPause() {
        if (!open) return
        stopClock()
        if (gameOver == null && moves > 0) {
            saveNow()
            if (mode.timed) paused = true
            submitIfWorthIt()
        }
        store.flush()
    }

    fun resume() {
        paused = false
        if (mode.timed && moves > 0 && gameOver == null) startClock()
    }

    // ---- playing ----

    fun move(dir: Direction) {
        if (!open || paused || gameOver != null) return
        val before = engine.snapshot()
        val r = engine.move(dir)
        if (!r.changed) {
            animator.bump(dir)
            return
        }
        if (undo.size >= UNDO_DEPTH) undo.removeFirst()
        undo.addLast(before)
        if (log.length < MAX_LOG) log.append(dir.code) else verifiable = false
        moves++
        if (clockStart == 0L) startClock()

        animator.apply(r)
        if (r.merges.isEmpty()) feedback.slide() else feedback.merge(r.merges.maxOf { it.exp })

        score = engine.score
        if (score > best) best = score
        track(dir, r)
        refresh()
        scheduleSave()

        if (engine.isStuck()) end(timeUp = false)
    }

    fun undo(): Boolean {
        if (!open || paused || gameOver?.timeUp == true || !canUndo) return false
        engine.restore(undo.removeLast())
        undosUsed++
        if (log.length < MAX_LOG) log.append(UNDO_CODE) else verifiable = false
        score = engine.score
        best = maxOf(startBest, peakScore)
        gameOver = null
        store.updateStats { s -> s.withMode(mode, s.mode(mode).let { it.copy(undos = it.undos + 1) }) }
        animator.rewind(engine.cells, engine.ids)
        feedback.tap()
        refresh()
        scheduleSave()
        return true
    }

    private fun end(timeUp: Boolean) {
        stopClock()
        gameOver = GameOver(timeUp, newBest = score > startBest && score > 0, previousBest = startBest)
        feedback.over()
        saveNow()
        submitIfWorthIt(force = true)
    }

    fun dismissNotice(n: Notice) {
        notices.remove(n)
    }

    // ---- stats ----

    private fun track(dir: Direction, r: MoveResult) {
        val firstMove = moves == 1 && undosUsed == 0
        val elapsed = elapsed()
        val grew = maxOf(0L, score - peakScore)
        peakScore = maxOf(peakScore, score)
        val reachedNow = if (engine.maxExp > topExp) engine.maxExp else 0
        if (reachedNow > 0) topExp = reachedNow
        val countsAsWin = reachedNow == 11
        val zen = mode == GameMode.ZEN
        val rankBefore = Rank.of(store.stats.value.classicBest)
        val before = store.stats.value

        store.updateStats { s0 ->
            var s = touchDay(s0)
            val m = s.mode(mode)
            s = s.withMode(
                mode,
                m.copy(
                    games = m.games + if (firstMove) 1 else 0,
                    wins = m.wins + if (countsAsWin) 1 else 0,
                    bestScore = maxOf(m.bestScore, score),
                    bestScoreMs = if (score > m.bestScore) elapsed else m.bestScoreMs,
                    bestExp = maxOf(m.bestExp, topExp),
                    totalScore = m.totalScore + grew,
                    moves = m.moves + 1,
                    merges = m.merges + r.merges.size,
                    fastestMs = if (reachedNow >= 7) m.fastestMs + (reachedNow to minOf(m.fastestMs[reachedNow] ?: Long.MAX_VALUE, elapsed)) else m.fastestMs,
                    reached = if (reachedNow >= 7) m.reached + (reachedNow to (m.reached[reachedNow] ?: 0) + 1) else m.reached,
                )
            )
            val d = today(s.daily)
            s.copy(
                xp = s.xp + if (zen) 0 else grew / 10,
                swipes = s.swipes.mapIndexed { i, n -> if (i == dir.ordinal) n + 1 else n },
                biggestGain = if (zen) s.biggestGain else maxOf(s.biggestGain, r.gained),
                biggestCombo = if (zen) s.biggestCombo else maxOf(s.biggestCombo, r.merges.size),
                noUndoWins = s.noUndoWins + if (countsAsWin && undosUsed == 0 && !zen) 1 else 0,
                daily = if (zen) d else d.copy(
                    score = d.score + grew,
                    merges = d.merges + r.merges.size,
                    moves = d.moves + 1,
                    games = d.games + if (firstMove) 1 else 0,
                    bestScore = maxOf(d.bestScore, score),
                    bestExp = maxOf(d.bestExp, topExp),
                    blitzBest = if (mode == GameMode.BLITZ) maxOf(d.blitzBest, score) else d.blitzBest,
                ),
            )
        }

        if (reachedNow >= 9) {
            val big = reachedNow >= 11
            notify(
                Notice(
                    NoticeKind.TILE, "${tileLabel(reachedNow)} reached",
                    if (big) "Keep going. Next up: ${tileLabel(reachedNow + 1)}" else "Next up: ${tileLabel(reachedNow + 1)}",
                    exp = reachedNow,
                )
            )
            if (big) {
                animator.celebrate()
                feedback.milestone()
            }
        }
        if (!recordAnnounced && startBest > 0 && score > startBest) {
            recordAnnounced = true
            notify(Notice(NoticeKind.RECORD, "New personal best", "You passed ${fmt(startBest)}"))
            feedback.unlock()
        }
        val rankNow = Rank.of(store.stats.value.classicBest)
        if (rankNow.ordinal > rankBefore.ordinal) {
            notify(Notice(NoticeKind.RANK, "Rank up", rankNow.title, rank = rankNow))
            feedback.unlock()
        }
        checkRewards(before.level)
    }

    /** Claims finished daily goals and achievements, then announces any level-up they caused. */
    private fun checkRewards(levelBefore: Int) {
        val s = store.stats.value
        val goals = Goals.forDay(s.daily.day).filter { it.id !in s.daily.claimed && it.done(s.daily) }
        if (goals.isNotEmpty()) {
            store.updateStats { cur ->
                val claimed = cur.daily.claimed + goals.map { it.id }
                cur.copy(
                    daily = cur.daily.copy(claimed = claimed),
                    goalsDone = cur.goalsDone + goals.size,
                    perfectDays = cur.perfectDays + if (claimed.size >= 3 && cur.daily.claimed.size < 3) 1 else 0,
                    xp = cur.xp + goals.sumOf { it.xp },
                )
            }
            goals.forEach { notify(Notice(NoticeKind.GOAL, "Daily goal complete", "${it.text}  +${it.xp} XP")) }
            feedback.unlock()
        }
        val unlocked = Achievements.newlyUnlocked(store.stats.value)
        if (unlocked.isNotEmpty()) {
            val now = System.currentTimeMillis()
            store.updateStats { cur ->
                cur.copy(
                    achievements = cur.achievements + unlocked.associate { it.id to now },
                    xp = cur.xp + unlocked.size * Achievements.XP,
                )
            }
            unlocked.forEach { notify(Notice(NoticeKind.ACHIEVEMENT, it.title, it.desc)) }
            feedback.unlock()
        }
        val level = store.stats.value.level
        if (level > levelBefore) notify(Notice(NoticeKind.LEVEL, "Level $level", "Nice work, keep climbing"))
    }

    private fun notify(n: Notice) {
        notices.add(n)
        // never let a backlog build up; the player can review everything on its own screen
        while (notices.size > 4) notices.removeAt(1)
    }

    private fun today(d: DailyState): DailyState {
        val day = LocalDate.now().toEpochDay()
        return if (d.day == day) d else DailyState(day = day)
    }

    private fun touchDay(s: Stats): Stats {
        val day = LocalDate.now().toEpochDay()
        if (s.lastPlayedDay == day) return s
        val streak = if (s.lastPlayedDay == day - 1) s.currentStreak + 1 else 1
        return s.copy(
            lastPlayedDay = day,
            daysPlayed = s.daysPlayed + 1,
            currentStreak = streak,
            longestStreak = maxOf(s.longestStreak, streak),
        )
    }

    private fun record(finished: Boolean) = addHistory(mode, peakScore, topExp, moves, accumulatedMs, finished)

    private fun addHistory(m: GameMode, score: Long, exp: Int, moves: Int, ms: Long, finished: Boolean) {
        store.updateStats { s ->
            val ms0 = s.mode(m)
            s.withMode(
                m,
                ms0.copy(
                    finished = ms0.finished + if (finished) 1 else 0,
                    longestMs = maxOf(ms0.longestMs, ms),
                    mostMoves = maxOf(ms0.mostMoves, moves),
                )
            ).copy(history = (listOf(GameRecord(m.id, score, exp, moves, ms, System.currentTimeMillis())) + s.history).take(100))
        }
    }

    private fun submitIfWorthIt(force: Boolean = false) {
        if (!mode.ranked || !verifiable || moves < 5 || engine.score <= 0) return
        if (force || engine.score >= startBest) {
            leaderboard.submit(PendingRun(mode.id, seed, log.toString(), accumulatedMs, engine.score))
        }
    }

    // ---- clock ----

    private fun elapsed() = accumulatedMs + if (clockStart > 0) SystemClock.elapsedRealtime() - clockStart else 0

    private fun startClock() {
        if (clockStart > 0) return
        clockStart = SystemClock.elapsedRealtime()
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                updateClock()
                if (mode.timed && elapsed() >= mode.timeLimitMs) {
                    end(timeUp = true)
                    break
                }
                delay(if (mode.timed) 50 else 200)
            }
        }
    }

    private fun stopClock() {
        ticker?.cancel()
        ticker = null
        if (clockStart > 0) {
            accumulatedMs = elapsed()
            if (mode.timed) accumulatedMs = minOf(accumulatedMs, mode.timeLimitMs)
            clockStart = 0
        }
        val delta = accumulatedMs - statsMs
        if (delta > 0) {
            statsMs = accumulatedMs
            store.updateStats { s -> s.withMode(mode, s.mode(mode).let { it.copy(timeMs = it.timeMs + delta) }) }
        }
        updateClock()
    }

    private fun updateClock() {
        val e = elapsed()
        if (mode.timed) {
            val left = (mode.timeLimitMs - e).coerceAtLeast(0)
            timeLeft = left.toFloat() / mode.timeLimitMs
            seconds = ((left + 999) / 1000).toInt()
        } else {
            seconds = (e / 1000).toInt()
        }
    }

    // ---- saving ----

    private fun refresh() {
        undosLeft = if (mode.undoLimit == Int.MAX_VALUE) Int.MAX_VALUE else (mode.undoLimit - undosUsed).coerceAtLeast(0)
        canUndo = undosLeft > 0 && undo.isNotEmpty()
        if (clockStart == 0L) updateClock()
    }

    private fun scheduleSave() {
        if (saver?.isActive == true) return
        saver = viewModelScope.launch {
            delay(700)
            if (open && gameOver == null && moves > 0) saveNow()
        }
    }

    private fun saveNow() {
        saver?.cancel()
        store.saveRun(
            SavedRun(
                mode = mode.id, seed = seed, state = engine.snapshot(), undo = undo.toList(), undosUsed = undosUsed,
                log = log.toString(), elapsedMs = elapsed(), moves = moves, verifiable = verifiable,
                topExp = topExp, peakScore = peakScore,
            )
        )
    }

    override fun onCleared() {
        leave()
        store.flush()
        feedback.release()
    }
}
