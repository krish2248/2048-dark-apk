package com.personal.game2048.game

import kotlinx.serialization.Serializable

enum class Direction(val code: Char) {
    UP('U'), DOWN('D'), LEFT('L'), RIGHT('R')
}

/** Log symbol for an undo. */
const val UNDO_CODE = 'Z'

/** What happened during one move, in enough detail for the renderer to animate it. */
class MoveResult {
    class Slide(val id: Int, val to: Int, val absorbed: Boolean)
    class Merge(val id: Int, val at: Int, val exp: Int)
    class Spawn(val id: Int, val at: Int, val exp: Int)

    val slides = ArrayList<Slide>(25)
    val merges = ArrayList<Merge>(10)
    var spawn: Spawn? = null
    var gained = 0L
    var changed = false
}

@Serializable
data class Snapshot(val cells: List<Int>, val score: Long, val rng: Int)

/**
 * Deterministic sliding-merge board.
 *
 * Every random draw comes from one seeded generator, so (seed + move log) fully determines a run.
 * `server/src/engine.js` is a port that replays submitted runs: any change to the rules or the
 * draw order here must be mirrored there (both test suites pin the same vectors).
 *
 * Cells hold exponents (0 = empty, 1 = 2, 11 = 2048 ...), so there is no tile ceiling.
 */
class Engine(val size: Int, seed: Int) {
    val cells = IntArray(size * size)
    val ids = IntArray(size * size)
    var score = 0L
        private set
    var rng = seed
        private set
    private var nextId = 1

    val maxExp: Int get() = cells.max()

    fun start() {
        cells.fill(0)
        ids.fill(0)
        score = 0
        spawn()
        spawn()
    }

    fun snapshot() = Snapshot(cells.toList(), score, rng)

    fun restore(s: Snapshot) {
        for (i in cells.indices) {
            cells[i] = s.cells[i]
            ids[i] = if (cells[i] != 0) nextId++ else 0
        }
        score = s.score
        rng = s.rng
    }

    fun move(dir: Direction): MoveResult {
        val out = MoveResult()
        for (line in 0 until size) {
            // start + k * step walks the line from the wall the tiles slide towards
            val start: Int
            val step: Int
            when (dir) {
                Direction.LEFT -> { start = line * size; step = 1 }
                Direction.RIGHT -> { start = line * size + size - 1; step = -1 }
                Direction.UP -> { start = line; step = size }
                Direction.DOWN -> { start = (size - 1) * size + line; step = -size }
            }
            var write = 0
            var mergeable = false
            for (k in 0 until size) {
                val from = start + k * step
                val exp = cells[from]
                if (exp == 0) continue
                val id = ids[from]
                val prev = start + (write - 1) * step
                if (mergeable && cells[prev] == exp) {
                    cells[from] = 0
                    ids[from] = 0
                    cells[prev] = exp + 1
                    mergeable = false
                    val gain = 1L shl (exp + 1)
                    score += gain
                    out.gained += gain
                    out.slides.add(MoveResult.Slide(id, prev, absorbed = true))
                    out.merges.add(MoveResult.Merge(ids[prev], prev, exp + 1))
                    out.changed = true
                } else {
                    val to = start + write * step
                    if (to != from) {
                        cells[to] = exp
                        ids[to] = id
                        cells[from] = 0
                        ids[from] = 0
                        out.slides.add(MoveResult.Slide(id, to, absorbed = false))
                        out.changed = true
                    }
                    write++
                    mergeable = true
                }
            }
        }
        if (out.changed) out.spawn = spawn()
        return out
    }

    fun isStuck(): Boolean {
        for (r in 0 until size) for (c in 0 until size) {
            val v = cells[r * size + c]
            if (v == 0) return false
            if (c + 1 < size && cells[r * size + c + 1] == v) return false
            if (r + 1 < size && cells[(r + 1) * size + c] == v) return false
        }
        return true
    }

    private fun spawn(): MoveResult.Spawn? {
        var empty = 0
        for (v in cells) if (v == 0) empty++
        if (empty == 0) return null
        var pick = (nextUInt() % empty).toInt()
        val exp = if (nextUInt() % 10L == 0L) 2 else 1
        for (i in cells.indices) {
            if (cells[i] != 0) continue
            if (pick-- == 0) {
                cells[i] = exp
                ids[i] = nextId++
                return MoveResult.Spawn(ids[i], i, exp)
            }
        }
        return null
    }

    /** mulberry32, returned as an unsigned 32-bit value. */
    private fun nextUInt(): Long {
        rng += 0x6D2B79F5
        var t = rng
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
    }
}
