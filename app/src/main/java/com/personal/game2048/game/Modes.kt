package com.personal.game2048.game

enum class GameMode(
    val id: String,
    val title: String,
    val tagline: String,
    val size: Int,
    val ranked: Boolean,
    val undoLimit: Int,
    val timeLimitMs: Long,
) {
    CLASSIC("classic", "Classic", "4×4 board, no ceiling", 4, true, 5, 0),
    GRAND("grand", "Grand", "5×5 board, go even further", 5, true, 5, 0),
    BLITZ("blitz", "Blitz", "3 minutes, highest score", 4, true, 0, 180_000),
    ZEN("zen", "Zen", "Unlimited undo, unranked", 4, false, Int.MAX_VALUE, 0);

    val timed: Boolean get() = timeLimitMs > 0

    companion object {
        val rankedModes = entries.filter { it.ranked }
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

/** Undo depth kept in memory. The server replays with the same depth. */
const val UNDO_DEPTH = 5

/** Longest run that can still be verified. Nobody reaches this by hand. */
const val MAX_LOG = 150_000
