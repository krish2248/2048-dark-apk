package com.personal.game2048.game

import com.personal.game2048.data.DailyState
import kotlin.random.Random

class DailyGoal(
    val id: String,
    val text: String,
    val target: Long,
    val xp: Int,
    val progress: (DailyState) -> Long,
) {
    fun done(d: DailyState) = progress(d) >= target
}

object Goals {
    private val builders: List<(Int) -> DailyGoal> = listOf(
        { t ->
            val n = listOf(3_000L, 8_000L, 20_000L)[t]
            DailyGoal("score_$n", "Score ${fmt(n)} in total today", n, 100 + t * 75) { it.score }
        },
        { t ->
            val n = listOf(150L, 400L, 900L)[t]
            DailyGoal("merge_$n", "Merge ${fmt(n)} pairs", n, 100 + t * 75) { it.merges.toLong() }
        },
        { t ->
            val n = listOf(250L, 600L, 1_200L)[t]
            DailyGoal("moves_$n", "Make ${fmt(n)} moves", n, 80 + t * 70) { it.moves.toLong() }
        },
        { t ->
            val n = listOf(2L, 3L, 5L)[t]
            DailyGoal("games_$n", "Play $n games", n, 80 + t * 50) { it.games.toLong() }
        },
        { t ->
            val n = listOf(2_000L, 6_000L, 14_000L)[t]
            DailyGoal("run_$n", "Score ${fmt(n)} in a single run", n, 120 + t * 90) { it.bestScore }
        },
        { t ->
            val e = listOf(8, 9, 10)[t]
            DailyGoal("tile_$e", "Build a ${tileLabel(e)} tile", e.toLong(), 120 + t * 90) { it.bestExp.toLong() }
        },
        { t ->
            val n = listOf(1_000L, 2_500L, 5_000L)[t]
            DailyGoal("blitz_$n", "Score ${fmt(n)} in Blitz", n, 120 + t * 90) { it.blitzBest }
        },
    )

    /** Three goals a day (easy, medium, hard), identical for every player on a given date. */
    fun forDay(day: Long): List<DailyGoal> =
        builders.shuffled(Random(day * 7_919 + 17)).take(3).mapIndexed { tier, build -> build(tier) }
}
