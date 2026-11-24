package com.personal.game2048.game

import com.personal.game2048.data.Stats

class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val group: String,
    val target: Long,
    val progress: (Stats) -> Long,
)

object Achievements {
    private fun s(n: Long) = if (n == 1L) "" else "s"

    private fun tileValue(exp: Int): Long = if (exp > 0) 1L shl exp else 0

    private fun tile(e: Int, id: String, title: String) =
        Achievement(id, title, "Build the ${tileLabel(e)} tile", "Tiles", 1L shl e) { tileValue(it.bestExp) }

    private fun score(n: Long, id: String, title: String) =
        Achievement(id, title, "Score ${fmt(n)} in one run", "Score", n) { it.bestScore }

    private fun games(n: Long, id: String, title: String) =
        Achievement(id, title, "Play $n game${s(n)}", "Dedication", n) { it.games.toLong() }

    private fun wins(n: Long, id: String, title: String) =
        Achievement(id, title, "Reach 2048 in $n run${s(n)}", "Victories", n) { it.wins.toLong() }

    private fun streak(n: Long, id: String, title: String) =
        Achievement(id, title, "Play $n days in a row", "Streaks", n) { it.longestStreak.toLong() }

    private fun hours(n: Long, id: String, title: String) =
        Achievement(id, title, "Play for $n hour${s(n)}", "Dedication", n * 60) { it.timeMs / 60_000 }

    private fun speed(e: Int, minutes: Int, id: String, title: String) =
        Achievement(id, title, "Build ${tileLabel(e)} in under $minutes min", "Speed", 1) {
            if ((it.fastest(e) ?: Long.MAX_VALUE) < minutes * 60_000L) 1 else 0
        }

    private fun rank(r: Rank, id: String) =
        Achievement(id, r.title, "Reach ${r.title}", "Ranks", r.minScore) { it.classicBest }

    private fun blitz(n: Long, id: String, title: String) =
        Achievement(id, title, "Score ${fmt(n)} in Blitz", "Speed", n) { it.mode(GameMode.BLITZ).bestScore }

    // ids shared with v1 keep existing players' unlocks
    val all: List<Achievement> = listOf(
        tile(7, "tile_128", "Warming Up"),
        tile(8, "tile_256", "Getting Somewhere"),
        tile(9, "tile_512", "Halfway There"),
        tile(10, "tile_1024", "Tile Hunter"),
        tile(11, "tile_2048", "Master"),
        tile(12, "tile_4096", "Grandmaster"),
        tile(13, "tile_8192", "Beyond Limits"),
        tile(14, "tile_16384", "Uncharted"),
        tile(15, "tile_32768", "Impossible"),
        tile(16, "tile_65536", "Ascended"),
        tile(17, "tile_131072", "The Edge"),
        tile(20, "tile_1m", "Millionaire Tile"),

        score(100, "score_100", "Centurion"),
        score(500, "score_500", "Rising Star"),
        score(1_000, "score_1000", "High Roller"),
        score(5_000, "score_5000", "Expert"),
        score(10_000, "score_10000", "Legend"),
        score(25_000, "score_25000", "Virtuoso"),
        score(50_000, "score_50000", "Unstoppable"),
        score(100_000, "score_100000", "Six Figures"),
        score(250_000, "score_250000", "Phenomenon"),
        score(500_000, "score_500000", "Half Million"),
        score(1_000_000, "score_1m", "Seven Figures"),

        wins(1, "first_win", "First Win"),
        wins(3, "win_3", "Hat Trick"),
        wins(10, "win_10", "Champion"),
        wins(25, "win_25", "Serial Winner"),
        wins(100, "win_100", "Hall of Fame"),
        Achievement("pure_win", "Pure", "Reach 2048 with no undo", "Victories", 1) { it.noUndoWins.toLong() },
        Achievement("pure_win_10", "Untouched", "Reach 2048 with no undo 10 times", "Victories", 10) { it.noUndoWins.toLong() },

        rank(Rank.IRON_1, "rank_iron"),
        rank(Rank.STEEL_1, "rank_steel"),
        rank(Rank.EMERALD_1, "rank_emerald"),
        rank(Rank.OBSIDIAN_1, "rank_obsidian"),
        rank(Rank.APEX, "rank_apex"),
        rank(Rank.TITAN, "rank_titan"),
        rank(Rank.INFINITE, "rank_infinite"),

        speed(9, 2, "speed_512_2", "Quick Hands"),
        speed(10, 4, "speed_1024_4", "Swift"),
        speed(11, 10, "win_under_10min", "Express"),
        speed(11, 5, "win_under_5min", "Speed Demon"),
        speed(11, 3, "win_under_3min", "Lightning"),
        blitz(2_500, "blitz_2500", "Blitz Rookie"),
        blitz(7_500, "blitz_7500", "Blitz Pro"),
        blitz(15_000, "blitz_15000", "Blitz Legend"),

        Achievement("combo_3", "Triple", "Merge 3 pairs in one swipe", "Skill", 3) { it.biggestCombo.toLong() },
        Achievement("combo_5", "High Five", "Merge 5 pairs in one swipe", "Skill", 5) { it.biggestCombo.toLong() },
        Achievement("combo_8", "Clean Sweep", "Merge 8 pairs in one swipe", "Skill", 8) { it.biggestCombo.toLong() },
        Achievement("gain_2048", "Big Bang", "Gain 2,048 points in one swipe", "Skill", 2_048) { it.biggestGain },
        Achievement("gain_8192", "Supernova", "Gain 8,192 points in one swipe", "Skill", 8_192) { it.biggestGain },
        Achievement("grand_4096", "Grand Opening", "Build 4096 on the 5×5 board", "Skill", 1L shl 12) { tileValue(it.mode(GameMode.GRAND).bestExp) },
        Achievement("grand_32768", "Grand Architect", "Build 32768 on the 5×5 board", "Skill", 1L shl 15) { tileValue(it.mode(GameMode.GRAND).bestExp) },

        games(1, "first_game", "First Move"),
        games(10, "games_10", "Dedicated"),
        games(50, "games_50", "Veteran"),
        games(100, "games_100", "Century Club"),
        games(500, "games_500", "Lifer"),
        hours(1, "hours_1", "Getting Hooked"),
        hours(5, "hours_5", "Time Flies"),
        hours(10, "hours_10", "Marathon"),
        hours(50, "hours_50", "Second Home"),
        Achievement("merges_1000", "Fuser", "Merge 1,000 pairs", "Dedication", 1_000) { it.merges },
        Achievement("merges_10000", "Reactor", "Merge 10,000 pairs", "Dedication", 10_000) { it.merges },
        Achievement("merges_100000", "Stellar Core", "Merge 100,000 pairs", "Dedication", 100_000) { it.merges },
        Achievement("moves_10000", "Ten Thousand Swipes", "Make 10,000 moves", "Dedication", 10_000) { it.moves },

        streak(3, "streak_3", "On a Roll"),
        streak(7, "streak_7", "Devoted"),
        streak(14, "streak_14", "Committed"),
        streak(30, "streak_30", "Obsessed"),
        streak(100, "streak_100", "Unbreakable"),
        Achievement("goals_10", "Goal Getter", "Complete 10 daily goals", "Streaks", 10) { it.goalsDone.toLong() },
        Achievement("goals_100", "Goal Machine", "Complete 100 daily goals", "Streaks", 100) { it.goalsDone.toLong() },
        Achievement("perfect_day", "Perfect Day", "Complete all 3 daily goals", "Streaks", 1) { it.perfectDays.toLong() },
        Achievement("perfect_day_10", "Perfectionist", "Have 10 perfect days", "Streaks", 10) { it.perfectDays.toLong() },
    )

    val groups: List<String> = all.map { it.group }.distinct()

    const val XP = 75

    fun newlyUnlocked(s: Stats): List<Achievement> =
        all.filter { it.id !in s.achievements && it.progress(s) >= it.target }
}
