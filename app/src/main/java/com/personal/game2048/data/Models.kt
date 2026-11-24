package com.personal.game2048.data

import com.personal.game2048.game.GameMode
import com.personal.game2048.game.Snapshot
import kotlinx.serialization.Serializable

@Serializable
data class ModeStats(
    val games: Int = 0,
    val finished: Int = 0,
    val wins: Int = 0,
    val bestScore: Long = 0,
    val bestExp: Int = 0,
    val totalScore: Long = 0,
    val moves: Long = 0,
    val merges: Long = 0,
    val timeMs: Long = 0,
    val undos: Int = 0,
    val bestScoreMs: Long = 0,
    val longestMs: Long = 0,
    val mostMoves: Int = 0,
    /** exponent -> fastest time to first build that tile */
    val fastestMs: Map<Int, Long> = emptyMap(),
    /** exponent -> number of runs that reached it */
    val reached: Map<Int, Int> = emptyMap(),
)

@Serializable
data class GameRecord(
    val mode: String,
    val score: Long,
    val exp: Int,
    val moves: Int,
    val ms: Long,
    val at: Long,
)

@Serializable
data class DailyState(
    val day: Long = 0,
    val score: Long = 0,
    val merges: Int = 0,
    val moves: Int = 0,
    val games: Int = 0,
    val bestScore: Long = 0,
    val bestExp: Int = 0,
    val blitzBest: Long = 0,
    val claimed: Set<String> = emptySet(),
)

@Serializable
data class Stats(
    val modes: Map<String, ModeStats> = emptyMap(),
    val history: List<GameRecord> = emptyList(),
    /** id -> unlock time */
    val achievements: Map<String, Long> = emptyMap(),
    val daily: DailyState = DailyState(),
    val xp: Long = 0,
    val goalsDone: Int = 0,
    val perfectDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPlayedDay: Long = 0,
    val daysPlayed: Int = 0,
    val biggestGain: Long = 0,
    val biggestCombo: Int = 0,
    /** counts in Direction order */
    val swipes: List<Long> = listOf(0, 0, 0, 0),
    val noUndoWins: Int = 0,
) {
    fun mode(m: GameMode): ModeStats = modes[m.id] ?: ModeStats()
    fun withMode(m: GameMode, s: ModeStats) = copy(modes = modes + (m.id to s))

    val classicBest: Long get() = mode(GameMode.CLASSIC).bestScore
    /** Zen has unlimited undo, so it never counts towards records or achievements. */
    private val earned: Collection<ModeStats> get() = modes.filterKeys { it != GameMode.ZEN.id }.values

    val bestScore: Long get() = earned.maxOfOrNull { it.bestScore } ?: 0
    val bestExp: Int get() = earned.maxOfOrNull { it.bestExp } ?: 0
    val wins: Int get() = earned.sumOf { it.wins }
    fun fastest(exp: Int): Long? = earned.mapNotNull { it.fastestMs[exp] }.minOrNull()
    val games: Int get() = modes.values.sumOf { it.games }
    val moves: Long get() = modes.values.sumOf { it.moves }
    val merges: Long get() = modes.values.sumOf { it.merges }
    val timeMs: Long get() = modes.values.sumOf { it.timeMs }
    val level: Int get() = Levels.levelFor(xp)
}

/** All modes folded into one, for the "All" stats tab. */
fun Stats.combined(): ModeStats = modes.values.fold(ModeStats()) { a, b ->
    ModeStats(
        games = a.games + b.games,
        finished = a.finished + b.finished,
        wins = a.wins + b.wins,
        bestScore = maxOf(a.bestScore, b.bestScore),
        bestExp = maxOf(a.bestExp, b.bestExp),
        totalScore = a.totalScore + b.totalScore,
        moves = a.moves + b.moves,
        merges = a.merges + b.merges,
        timeMs = a.timeMs + b.timeMs,
        undos = a.undos + b.undos,
        bestScoreMs = if (b.bestScore > a.bestScore) b.bestScoreMs else a.bestScoreMs,
        longestMs = maxOf(a.longestMs, b.longestMs),
        mostMoves = maxOf(a.mostMoves, b.mostMoves),
        fastestMs = (a.fastestMs.keys + b.fastestMs.keys).associateWith { e ->
            minOf(a.fastestMs[e] ?: Long.MAX_VALUE, b.fastestMs[e] ?: Long.MAX_VALUE)
        },
        reached = (a.reached.keys + b.reached.keys).associateWith { e -> (a.reached[e] ?: 0) + (b.reached[e] ?: 0) },
    )
}

object Levels {
    /** Total XP needed to reach [level] (level 1 = 0). Grows quadratically. */
    fun xpFor(level: Int): Long = 75L * (level - 1) * level

    fun levelFor(xp: Long): Int {
        var l = 1
        while (xpFor(l + 1) <= xp) l++
        return l
    }

    fun progress(xp: Long): Float {
        val l = levelFor(xp)
        return (xp - xpFor(l)).toFloat() / (xpFor(l + 1) - xpFor(l))
    }
}

/** A run in progress. Seed + log reproduce it exactly, which is what the server checks. */
@Serializable
data class SavedRun(
    val mode: String,
    val seed: Int,
    val state: Snapshot,
    val undo: List<Snapshot> = emptyList(),
    val undosUsed: Int = 0,
    val log: String = "",
    val elapsedMs: Long = 0,
    val moves: Int = 0,
    /** False when the run cannot be replayed (e.g. migrated from v1). */
    val verifiable: Boolean = true,
    val topExp: Int = 0,
    /** Highest score this run reached before any undo, so undo can't re-earn XP or totals. */
    val peakScore: Long = 0,
)

@Serializable
data class Profile(val id: String, val secret: String, val name: String, val country: String)

@Serializable
data class PendingRun(val mode: String, val seed: Int, val log: String, val ms: Long, val score: Long)

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class SlideSpeed(val ms: Int) { GLIDE(155), QUICK(110), INSTANT(60) }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.DARK,
    val palette: String = "emerald",
    val speed: SlideSpeed = SlideSpeed.QUICK,
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val effects: Boolean = true,
    val lastMode: String = "classic",
    /** Public ids of players this user chose to hide on the leaderboard. */
    val hidden: Set<String> = emptySet(),
)
