package com.personal.game2048.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.personal.game2048.game.Direction
import com.personal.game2048.game.tileLabel
import com.personal.game2048.ui.theme.App
import com.personal.game2048.ui.theme.AppColors
import com.personal.game2048.ui.theme.Grotesk
import kotlin.math.abs

/**
 * Fires as soon as a finger has clearly committed to a direction, not when it lifts.
 * Each pointer is tracked on its own, so alternating two thumbs works at full speed.
 */
fun Modifier.swipes(key: Any?, onSwipe: (Direction) -> Unit): Modifier = pointerInput(key) {
    class Track(val origin: Offset, var fired: Boolean = false)

    val threshold = 16.dp.toPx()
    val tracks = HashMap<Long, Track>()
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            for (c in event.changes) {
                val id = c.id.value
                if (!c.pressed) {
                    tracks.remove(id)
                    continue
                }
                val t = tracks[id]
                if (t == null) {
                    if (!c.previousPressed && !c.isConsumed) tracks[id] = Track(c.position)
                    continue
                }
                if (t.fired) {
                    c.consume()
                    continue
                }
                val d = c.position - t.origin
                val ax = abs(d.x)
                val ay = abs(d.y)
                // ignore near-diagonals until the dominant axis is unambiguous
                if (maxOf(ax, ay) >= threshold && (maxOf(ax, ay) > 1.15f * minOf(ax, ay) || maxOf(ax, ay) > threshold * 2.5f)) {
                    t.fired = true
                    c.consume()
                    onSwipe(
                        if (ax > ay) if (d.x > 0) Direction.RIGHT else Direction.LEFT
                        else if (d.y > 0) Direction.DOWN else Direction.UP
                    )
                }
            }
        }
    }
}

/** Tile labels laid out once per (exponent, cell size) and reused every frame. */
private class LabelCache(private val measurer: TextMeasurer, private val density: Density) {
    private val map = HashMap<Int, TextLayoutResult>()
    private var cell = -1f

    fun get(exp: Int, cellPx: Float): TextLayoutResult {
        if (cellPx != cell) {
            map.clear()
            cell = cellPx
        }
        return map.getOrPut(exp) {
            val label = tileLabel(exp)
            val ratio = when (label.length) {
                1, 2 -> 0.44f
                3 -> 0.36f
                4 -> 0.29f
                5 -> 0.235f
                else -> 0.195f
            }
            measurer.measure(
                label,
                TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = with(density) { (cellPx * ratio).toSp() }),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
fun BoardView(animator: BoardAnimator, modifier: Modifier = Modifier) {
    val colors = App.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val labels = remember(measurer, density) { LabelCache(measurer, density) }

    LaunchedEffect(animator) { animator.run() }

    Canvas(modifier.aspectRatio(1f).fillMaxSize()) {
        animator.frame // the only state read: invalidates draw, nothing else
        val n = animator.size
        val gap = this.size.width * (if (n >= 5) 0.018f else 0.022f)
        val cell = (this.size.width - gap * (n + 1)) / n
        val pitch = cell + gap

        drawRoundRect(colors.board, cornerRadius = CornerRadius(cell * 0.2f))
        drawRoundRect(colors.border, cornerRadius = CornerRadius(cell * 0.2f), style = Stroke(1.dp.toPx()))
        val r = CornerRadius(cell * 0.13f)
        for (row in 0 until n) for (col in 0 until n) {
            drawRoundRect(colors.cell, Offset(gap + col * pitch, gap + row * pitch), Size(cell, cell), r)
        }

        val ox = gap + animator.bumpX * pitch
        val oy = gap + animator.bumpY * pitch
        val sprites = animator.sprites
        // three layers: tiles being absorbed, normal tiles, popping tiles
        for (i in sprites.indices) if (sprites[i].absorbed) tile(sprites[i], labels, colors, ox, oy, pitch, cell, r)
        for (i in sprites.indices) if (!sprites[i].absorbed && sprites[i].popAt == BoardAnimator.NONE) tile(sprites[i], labels, colors, ox, oy, pitch, cell, r)
        for (i in sprites.indices) if (!sprites[i].absorbed && sprites[i].popAt != BoardAnimator.NONE) tile(sprites[i], labels, colors, ox, oy, pitch, cell, r)

        for (i in animator.particles.indices) {
            val p = animator.particles[i]
            drawCircle(colors.palette.tile(p.exp), p.size * pitch, Offset(gap + p.x * pitch, gap + p.y * pitch), alpha = p.alpha.coerceIn(0f, 1f))
        }
    }
}

private fun DrawScope.tile(
    s: BoardAnimator.Sprite, labels: LabelCache, colors: AppColors,
    ox: Float, oy: Float, pitch: Float, cell: Float, r: CornerRadius,
) {
    if (s.scale <= 0.01f) return
    val left = ox + s.x * pitch
    val top = oy + s.y * pitch
    val fill = colors.palette.tile(s.exp)
    val ink = colors.palette.ink(s.exp)
    val label = labels.get(s.exp, cell)
    withTransform({
        translate(left, top)
        if (s.scale != 1f) scale(s.scale, s.scale, Offset(cell / 2f, cell / 2f))
    }) {
        if (s.exp >= 11) {
            // soft halo, cheaper and steadier than a blur
            val step = cell * 0.028f
            for (i in 3 downTo 1) {
                val g = step * i
                drawRoundRect(
                    fill, Offset(-g, -g), Size(cell + g * 2, cell + g * 2),
                    CornerRadius(r.x + g), alpha = 0.10f,
                )
            }
        }
        drawRoundRect(fill, size = Size(cell, cell), cornerRadius = r)
        if (s.exp <= 10) drawRoundRect(Color.White, size = Size(cell, cell), cornerRadius = r, alpha = 0.06f, style = Stroke(1.dp.toPx()))
        if (s.flash > 0f) drawRoundRect(Color.White, size = Size(cell, cell), cornerRadius = r, alpha = s.flash)
        drawText(label, ink, Offset((cell - label.size.width) / 2f, (cell - label.size.height) / 2f))
    }
}
