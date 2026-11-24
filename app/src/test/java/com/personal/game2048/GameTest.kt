package com.personal.game2048

import com.personal.game2048.data.DailyState
import com.personal.game2048.data.Levels
import com.personal.game2048.data.ModeStats
import com.personal.game2048.data.Stats
import com.personal.game2048.game.Achievements
import com.personal.game2048.game.Direction
import com.personal.game2048.game.Engine
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.Goals
import com.personal.game2048.game.Rank
import com.personal.game2048.game.Snapshot
import com.personal.game2048.game.UNDO_CODE
import com.personal.game2048.game.flag
import com.personal.game2048.game.tileLabel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GameTest {
    @Serializable
    private data class Vector(val mode: String, val seed: Int, val log: String, val score: Long, val exp: Int, val cells: List<Int>)

    private fun board(vararg cells: Int) = Engine(4, 1).apply { restore(Snapshot(cells.toList(), 0, 1)) }

    /** The server replays runs with a port of this engine; these vectors are shared with its tests. */
    @Test
    fun matchesServerVectors() {
        val vectors = Json.decodeFromString<List<Vector>>(File("../server/test/vectors.json").readText())
        assertTrue(vectors.size >= 4)
        for (v in vectors) {
            val e = Engine(GameMode.of(v.mode).size, v.seed).apply { start() }
            val undo = ArrayDeque<Snapshot>()
            for (ch in v.log) {
                if (ch == UNDO_CODE) {
                    e.restore(undo.removeLast())
                } else {
                    undo.addLast(e.snapshot())
                    assertTrue(e.move(Direction.entries.first { it.code == ch }).changed)
                }
            }
            assertEquals(v.score, e.score)
            assertEquals(v.exp, e.maxExp)
            assertEquals(v.cells, e.cells.toList())
        }
    }

    @Test
    fun eachTileMergesOncePerMove() {
        val e = board(1, 1, 1, 1, 1, 1, 2, 0, 2, 1, 1, 0, 0, 0, 0, 0)
        val r = e.move(Direction.LEFT)
        assertEquals(listOf(2, 2), e.cells.slice(0..1))
        assertEquals(listOf(2, 2), e.cells.slice(4..5)) // 2 2 4 -> 4 4, not 8
        assertEquals(listOf(2, 2), e.cells.slice(8..9))
        assertEquals(16L, e.score)
        assertEquals(4, r.merges.size)
    }

    @Test
    fun mergesNearestTheWallFirst() {
        val e = board(1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        e.move(Direction.RIGHT)
        assertEquals(2, e.cells[3])
        assertEquals(1, e.cells[2])
    }

    @Test
    fun noOpMoveSpawnsNothing() {
        val e = board(1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertFalse(e.move(Direction.LEFT).changed)
        assertEquals(2, e.cells.count { it != 0 })
    }

    @Test
    fun goesFarPast2048() {
        val e = board(16, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        e.move(Direction.LEFT)
        assertEquals(17, e.cells[0])
        assertEquals(131_072L, e.score)
        assertEquals("131072", tileLabel(17))
        assertEquals("4M", tileLabel(22))
    }

    @Test
    fun detectsStuckBoard() {
        assertTrue(board(1, 2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 1).isStuck())
        assertFalse(board(1, 2, 1, 2, 2, 1, 2, 1, 1, 2, 1, 2, 2, 1, 2, 2).isStuck())
    }

    @Test
    fun slidesReportEveryTileExactlyWhereItEndsUp() {
        repeat(200) { seed ->
            val e = Engine(5, seed).apply { start() }
            val pos = HashMap<Int, Int>()
            for (i in e.cells.indices) if (e.cells[i] != 0) pos[e.ids[i]] = i
            repeat(300) { step ->
                val r = e.move(Direction.entries[(step * 7 + seed) % 4])
                for (s in r.slides) if (s.absorbed) pos.remove(s.id) else pos[s.id] = s.to
                r.spawn?.let { pos[it.id] = it.at }
                val expected = e.cells.indices.filter { e.cells[it] != 0 }.associateBy { e.ids[it] }
                assertEquals(expected, pos)
            }
        }
    }

    @Test
    fun ranksAreOrdered() {
        assertEquals(Rank.PEBBLE_1, Rank.of(0))
        assertEquals(Rank.EMERALD_1, Rank.of(20_000))
        assertEquals(Rank.INFINITE, Rank.of(5_000_000))
        assertTrue(Rank.entries.zipWithNext().all { (a, b) -> a.minScore < b.minScore })
        assertEquals(0.5f, Rank.progress(250), 0.001f)
    }

    @Test
    fun goalsAreStableAndDistinct() {
        for (day in 20_000L..20_400L) {
            val g = Goals.forDay(day)
            assertEquals(3, g.map { it.id }.toSet().size)
            assertEquals(g.map { it.id }, Goals.forDay(day).map { it.id })
            assertTrue(g.none { it.done(DailyState(day)) })
        }
    }

    @Test
    fun achievementsAreUniqueAndLockedAtStart() {
        assertEquals(Achievements.all.size, Achievements.all.map { it.id }.toSet().size)
        assertTrue(Achievements.newlyUnlocked(Stats()).isEmpty())
        val v1 = listOf("first_game", "games_100", "score_50000", "win_under_3min", "streak_30", "tile_8192", "hours_10")
        assertTrue(Achievements.all.map { it.id }.containsAll(v1))
    }

    @Test
    fun zenNeverCountsTowardsRecords() {
        val s = Stats().withMode(GameMode.ZEN, ModeStats(bestScore = 999_999, bestExp = 16, wins = 5))
        assertEquals(0L, s.bestScore)
        assertEquals(0, s.bestExp)
        assertEquals(0, s.wins)
    }

    @Test
    fun levels() {
        assertEquals(1, Levels.levelFor(0))
        assertEquals(1, Levels.levelFor(149))
        assertEquals(2, Levels.levelFor(150))
        assertEquals(10, Levels.levelFor(Levels.xpFor(10)))
    }

    @Test
    fun flags() {
        assertEquals("🇪🇬", flag("EG"))
        assertEquals("🏳️", flag("e1"))
    }
}
