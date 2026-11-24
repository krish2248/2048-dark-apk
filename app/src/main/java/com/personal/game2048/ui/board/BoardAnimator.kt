package com.personal.game2048.ui.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.personal.game2048.game.Direction
import com.personal.game2048.game.MoveResult
import kotlinx.coroutines.channels.Channel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Owns every tile's on-screen position. One frame loop advances all of them and bumps [frame],
 * which is only read while drawing, so a swipe never recomposes or re-lays-out anything.
 *
 * Input is never blocked: a swipe that arrives mid-slide retargets each tile from wherever it
 * currently is, so fast play stays fluid instead of queueing or snapping.
 */
class BoardAnimator {
    class Sprite(val id: Int, var exp: Int, col: Float, row: Float) {
        var x = col
        var y = row
        var fromX = col
        var fromY = row
        var toX = col
        var toY = row
        var slideAt = NONE
        var mergeAt = NONE
        var mergeExp = 0
        var popAt = NONE
        var spawnAt = NONE
        var absorbed = false
        var scale = 1f
        var flash = 0f
    }

    class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, val exp: Int, val size: Float, var bornAt: Long, val life: Float) {
        var alpha = 1f
    }

    val sprites = ArrayList<Sprite>(32)
    val particles = ArrayList<Particle>(128)
    var bumpX = 0f
        private set
    var bumpY = 0f
        private set

    var size by mutableIntStateOf(4)
        private set
    var frame by mutableLongStateOf(0L)
        private set
    var slideMs = 110
    var effects = true

    private var bumpAt = NONE
    private var bumpDir = Direction.UP
    private var now = 0L
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val random = Random(7)

    /** Frame loop; sleeps whenever nothing is moving. Run from a LaunchedEffect. */
    suspend fun run() {
        while (true) {
            wake.receive()
            var active = true
            while (active) withFrameNanos { active = tick(it) }
        }
    }

    /** Rebuilds the board from scratch (new game, undo, restore). */
    fun reset(size: Int, cells: IntArray, ids: IntArray, animate: Boolean) {
        this.size = size
        sprites.clear()
        particles.clear()
        for (i in cells.indices) {
            if (cells[i] == 0) continue
            val s = Sprite(ids[i], cells[i], (i % size).toFloat(), (i / size).toFloat())
            if (animate) {
                s.spawnAt = PENDING
                s.scale = 0f
            }
            sprites.add(s)
        }
        frame++
        wake.trySend(Unit)
    }

    /** Undo: tiles that exist in both states stay put, the rest grow back in. */
    fun rewind(cells: IntArray, ids: IntArray) {
        val before = HashMap<Int, Int>()
        for (s in sprites) if (!s.absorbed) before[s.toY.toInt() * size + s.toX.toInt()] = s.exp
        sprites.clear()
        for (i in cells.indices) {
            if (cells[i] == 0) continue
            val s = Sprite(ids[i], cells[i], (i % size).toFloat(), (i / size).toFloat())
            if (before[i] != cells[i]) {
                s.spawnAt = PENDING
                s.scale = 0f
            }
            sprites.add(s)
        }
        frame++
        wake.trySend(Unit)
    }

    fun apply(r: MoveResult) {
        // finish anything the previous swipe left half-done, keeping current positions
        val it = sprites.iterator()
        while (it.hasNext()) {
            val s = it.next()
            if (s.absorbed) {
                it.remove()
            } else if (s.mergeAt != NONE) {
                s.exp = s.mergeExp
                s.mergeAt = NONE
                s.popAt = PENDING
            }
        }
        val byId = HashMap<Int, Sprite>(sprites.size * 2)
        for (s in sprites) byId[s.id] = s
        for (m in r.slides) {
            val s = byId[m.id] ?: continue
            s.fromX = s.x
            s.fromY = s.y
            s.toX = (m.to % size).toFloat()
            s.toY = (m.to / size).toFloat()
            s.slideAt = PENDING
            s.absorbed = m.absorbed
        }
        for (m in r.merges) {
            val s = byId[m.id] ?: continue
            s.mergeAt = PENDING
            s.mergeExp = m.exp
        }
        r.spawn?.let { sp ->
            val s = Sprite(sp.id, sp.exp, (sp.at % size).toFloat(), (sp.at / size).toFloat())
            s.spawnAt = PENDING
            s.scale = 0f
            sprites.add(s)
        }
        wake.trySend(Unit)
    }

    /** Small nudge when a swipe changes nothing. */
    fun bump(dir: Direction) {
        bumpDir = dir
        bumpAt = PENDING
        wake.trySend(Unit)
    }

    fun celebrate() {
        if (!effects) return
        repeat(70) {
            val a = random.nextFloat() * 2 * PI.toFloat()
            val v = 1.2f + random.nextFloat() * 4.2f
            particles.add(
                Particle(size / 2f, size / 2f, cos(a) * v, sin(a) * v, 5 + random.nextInt(9), 0.05f + random.nextFloat() * 0.07f, PENDING, 1.1f + random.nextFloat() * 0.5f)
            )
        }
        wake.trySend(Unit)
    }

    private var lastFrame = 0L

    private fun tick(t: Long): Boolean {
        // a swipe starts half a frame "in the past" so the first drawn frame already moves
        val dt = if (lastFrame == 0L || t - lastFrame > 50_000_000L) 8_000_000L else t - lastFrame
        lastFrame = t
        now = t
        var active = false
        val slideNs = slideMs * 1_000_000f
        val mergePoint = slideNs * 0.70f
        val spawnDelay = slideNs * 0.45f

        val it = sprites.listIterator()
        while (it.hasNext()) {
            val s = it.next()
            if (s.slideAt == PENDING) s.slideAt = t - dt / 2
            if (s.mergeAt == PENDING) s.mergeAt = t - dt / 2
            if (s.popAt == PENDING) s.popAt = t
            if (s.spawnAt == PENDING) s.spawnAt = t

            if (s.slideAt != NONE) {
                val elapsed = t - s.slideAt
                if (s.absorbed && elapsed >= mergePoint) {
                    it.remove()
                    continue
                }
                val p = min(elapsed / slideNs, 1f)
                val e = easeOut(p)
                s.x = s.fromX + (s.toX - s.fromX) * e
                s.y = s.fromY + (s.toY - s.fromY) * e
                if (p >= 1f) s.slideAt = NONE else active = true
            } else if (s.absorbed) {
                it.remove()
                continue
            }
            if (s.mergeAt != NONE) {
                active = true
                if (t - s.mergeAt >= mergePoint) {
                    s.mergeAt = NONE
                    s.exp = s.mergeExp
                    s.popAt = t
                    if (effects && s.exp >= 7) burst(s, t)
                }
            }

            s.scale = 1f
            s.flash = 0f
            if (s.spawnAt != NONE) {
                val q = (t - s.spawnAt - spawnDelay) / SPAWN_NS
                when {
                    q <= 0f -> { s.scale = 0f; active = true }
                    q < 1f -> { s.scale = easeOutBack(q); active = true }
                    else -> s.spawnAt = NONE
                }
            }
            if (s.popAt != NONE) {
                val q = (t - s.popAt) / POP_NS
                if (q < 1f) {
                    s.scale = 1f + 0.15f * sin(PI.toFloat() * (1f - (1f - q) * (1f - q)))
                    s.flash = (1f - q) * 0.35f
                    active = true
                } else {
                    s.popAt = NONE
                }
            }
        }

        val sec = dt / 1e9f
        val pi = particles.listIterator()
        while (pi.hasNext()) {
            val p = pi.next()
            if (p.bornAt == PENDING) p.bornAt = t
            val age = (t - p.bornAt) / 1e9f / p.life
            if (age >= 1f) {
                pi.remove()
                continue
            }
            val drag = 1f - age * 0.85f
            p.x += p.vx * drag * sec
            p.y += p.vy * drag * sec + age * 1.6f * sec
            p.alpha = 1f - age * age
            active = true
        }

        if (bumpAt != NONE) {
            if (bumpAt == PENDING) bumpAt = t
            val q = (t - bumpAt) / BUMP_NS
            val d = if (q >= 1f) 0f else 0.05f * sin(PI.toFloat() * q)
            bumpX = when (bumpDir) { Direction.LEFT -> -d; Direction.RIGHT -> d; else -> 0f }
            bumpY = when (bumpDir) { Direction.UP -> -d; Direction.DOWN -> d; else -> 0f }
            if (q >= 1f) bumpAt = NONE else active = true
        }

        frame = t
        return active
    }

    private fun burst(s: Sprite, t: Long) {
        val n = min(4 + s.exp, 20)
        repeat(n) {
            val a = random.nextFloat() * 2 * PI.toFloat()
            val v = 0.7f + random.nextFloat() * (0.8f + s.exp * 0.08f)
            particles.add(
                Particle(
                    s.toX + 0.5f + cos(a) * 0.4f, s.toY + 0.5f + sin(a) * 0.4f,
                    cos(a) * v, sin(a) * v, s.exp, 0.025f + random.nextFloat() * 0.04f, t, 0.35f + random.nextFloat() * 0.3f,
                )
            )
        }
    }

    private fun easeOut(p: Float): Float {
        val q = 1f - p
        return 1f - q * q * q * q
    }

    private fun easeOutBack(p: Float): Float {
        val c = 1.35f
        val q = p - 1f
        return 1f + (c + 1f) * q * q * q + c * q * q
    }

    companion object {
        const val NONE = Long.MIN_VALUE
        const val PENDING = Long.MIN_VALUE + 1
        private const val SPAWN_NS = 150_000_000f
        private const val POP_NS = 170_000_000f
        private const val BUMP_NS = 130_000_000f
    }
}
