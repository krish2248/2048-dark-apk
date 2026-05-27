package com.personal.game2048.storage

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

data class StatsData(
    val bestScore: Int = 0,
    val totalGames: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val totalSeconds: Long = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val bestWinTimeSecs: Int = 0,
    val bestScoreTimeSecs: Int = 0,
    val highestTile: Int = 0,
    val achievements: Set<String> = emptySet()
) {
    val winRate: Int get() = if (totalGames > 0) (gamesWon * 100 / totalGames) else 0
}

class StatsStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("g2048_stats", Context.MODE_PRIVATE)

    fun load(): StatsData = StatsData(
        bestScore = prefs.getInt("g2048_best_score", 0),
        totalGames = prefs.getInt("g2048_total_games", 0),
        gamesWon = prefs.getInt("g2048_games_won", 0),
        gamesLost = prefs.getInt("g2048_games_lost", 0),
        totalSeconds = prefs.getLong("g2048_total_seconds", 0L),
        currentStreak = prefs.getInt("g2048_current_streak", 0),
        longestStreak = prefs.getInt("g2048_longest_streak", 0),
        bestWinTimeSecs = prefs.getInt("g2048_best_win_time_secs", 0),
        bestScoreTimeSecs = prefs.getInt("g2048_best_score_time_secs", 0),
        highestTile = prefs.getInt("g2048_highest_tile", 0),
        achievements = prefs.getStringSet("g2048_achievements", emptySet()) ?: emptySet()
    )

    fun save(stats: StatsData) {
        prefs.edit()
            .putInt("g2048_best_score", stats.bestScore)
            .putInt("g2048_total_games", stats.totalGames)
            .putInt("g2048_games_won", stats.gamesWon)
            .putInt("g2048_games_lost", stats.gamesLost)
            .putLong("g2048_total_seconds", stats.totalSeconds)
            .putInt("g2048_current_streak", stats.currentStreak)
            .putInt("g2048_longest_streak", stats.longestStreak)
            .putInt("g2048_best_win_time_secs", stats.bestWinTimeSecs)
            .putInt("g2048_best_score_time_secs", stats.bestScoreTimeSecs)
            .putInt("g2048_highest_tile", stats.highestTile)
            .putStringSet("g2048_achievements", stats.achievements)
            .apply()
    }

    fun updateStreak() {
        val today = LocalDate.now().toString()
        val last = prefs.getString("g2048_last_played_date", "") ?: ""
        val yesterday = LocalDate.now().minusDays(1).toString()

        var streak = prefs.getInt("g2048_current_streak", 0)
        var longest = prefs.getInt("g2048_longest_streak", 0)

        when {
            last == today -> { }
            last == yesterday -> {
                streak++
                if (streak > longest) longest = streak
            }
            else -> streak = 1
        }

        prefs.edit()
            .putString("g2048_last_played_date", today)
            .putInt("g2048_current_streak", streak)
            .putInt("g2048_longest_streak", longest)
            .apply()
    }

    fun checkAchievements(stats: StatsData, sessionTimeSecs: Int): Pair<StatsData, List<String>> {
        val current = stats.achievements.toMutableSet()
        val newlyUnlocked = mutableListOf<String>()

        fun unlock(id: String, name: String) {
            if (id !in current) {
                current.add(id)
                newlyUnlocked.add(name)
            }
        }

        if (stats.totalGames >= 1) unlock("first_game", "First Move")
        if (stats.totalGames >= 10) unlock("games_10", "Dedicated")
        if (stats.totalGames >= 50) unlock("games_50", "Veteran")
        if (stats.totalGames >= 100) unlock("games_100", "Century Club")
        if (stats.bestScore >= 100) unlock("score_100", "Centurion")
        if (stats.bestScore >= 500) unlock("score_500", "Rising Star")
        if (stats.bestScore >= 1000) unlock("score_1000", "High Roller")
        if (stats.bestScore >= 5000) unlock("score_5000", "Expert")
        if (stats.bestScore >= 10000) unlock("score_10000", "Legend")
        if (stats.bestScore >= 50000) unlock("score_50000", "Unstoppable")
        if (stats.gamesWon >= 1) unlock("first_win", "First Win")
        if (stats.gamesWon >= 3) unlock("win_3", "Hat Trick")
        if (stats.gamesWon >= 10) unlock("win_10", "Champion")
        if (stats.gamesWon >= 1 && stats.bestWinTimeSecs > 0 && stats.bestWinTimeSecs < 300)
            unlock("win_under_5min", "Speed Demon")
        if (stats.gamesWon >= 1 && stats.bestWinTimeSecs > 0 && stats.bestWinTimeSecs < 180)
            unlock("win_under_3min", "Lightning")
        if (stats.totalSeconds >= 3600) unlock("hours_1", "Getting Hooked")
        if (stats.totalSeconds >= 18000) unlock("hours_5", "Time Flies")
        if (stats.totalSeconds >= 36000) unlock("hours_10", "Marathon")
        if (stats.currentStreak >= 3) unlock("streak_3", "On a Roll")
        if (stats.currentStreak >= 7) unlock("streak_7", "Devoted")
        if (stats.currentStreak >= 14) unlock("streak_14", "Committed")
        if (stats.currentStreak >= 30) unlock("streak_30", "Obsessed")
        if (stats.highestTile >= 512) unlock("tile_512", "Halfway There")
        if (stats.highestTile >= 1024) unlock("tile_1024", "Tile Hunter")
        if (stats.highestTile >= 2048) unlock("tile_2048", "Master")
        if (stats.highestTile >= 4096) unlock("tile_4096", "Grandmaster")
        if (stats.highestTile >= 8192) unlock("tile_8192", "Beyond Limits")

        return stats.copy(achievements = current) to newlyUnlocked
    }
}
