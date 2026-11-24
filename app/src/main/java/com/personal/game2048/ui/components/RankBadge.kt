package com.personal.game2048.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.personal.game2048.game.Rank
import com.personal.game2048.game.RankFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun rankColor(f: RankFamily): Color = when (f) {
    RankFamily.PEBBLE -> Color(0xFF8B9490)
    RankFamily.IRON -> Color(0xFFB07A55)
    RankFamily.STEEL -> Color(0xFF9FB6C8)
    RankFamily.EMERALD -> Color(0xFF3FBF74)
    RankFamily.OBSIDIAN -> Color(0xFF8D6BEA)
    RankFamily.APEX -> Color(0xFFF2C14E)
    RankFamily.TITAN -> Color(0xFFFF6F61)
    RankFamily.INFINITE -> Color(0xFF5FE3F0)
}

/** Octagonal tile-shaped emblem. Divisions show as chevrons; the top three tiers get their own mark. */
@Composable
fun RankBadge(rank: Rank, size: Dp, modifier: Modifier = Modifier, locked: Boolean = false) {
    val base = rankColor(rank.family).let { if (locked) lerp(it, Color(0xFF454B48), 0.62f) else it }
    Canvas(modifier.size(size).semantics { contentDescription = rank.title }) {
        val s = this.size.minDimension
        val outer = octagon(s, 0.02f)
        drawPath(outer, Brush.verticalGradient(listOf(lerp(base, Color.White, 0.30f), lerp(base, Color.Black, 0.45f))))
        drawPath(octagon(s, 0.09f), Brush.verticalGradient(listOf(lerp(base, Color.Black, 0.72f), lerp(base, Color.Black, 0.84f))))
        val ink = lerp(base, Color.White, 0.28f)
        when (rank.family) {
            RankFamily.APEX -> star(ink, s, Offset(s / 2, s * 0.51f), s * 0.27f)
            RankFamily.TITAN -> {
                star(ink, s, Offset(s / 2, s * 0.37f), s * 0.17f)
                star(ink, s, Offset(s * 0.34f, s * 0.64f), s * 0.15f)
                star(ink, s, Offset(s * 0.66f, s * 0.64f), s * 0.15f)
            }
            RankFamily.INFINITE -> {
                val p = Path()
                for (i in 0..60) {
                    val t = i / 60f * 2 * PI.toFloat()
                    val d = 1 + sin(t) * sin(t)
                    val x = s / 2 + s * 0.29f * cos(t) / d
                    val y = s / 2 + s * 0.31f * sin(t) * cos(t) / d
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                p.close()
                drawPath(p, ink, style = Stroke(s * 0.075f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            else -> {
                val n = rank.division
                val gap = s * 0.145f
                val top = s * 0.47f - (n - 1) * gap / 2
                for (i in 0 until n) {
                    val y = top + i * gap
                    val p = Path().apply {
                        moveTo(s * 0.29f, y + s * 0.10f)
                        lineTo(s * 0.5f, y - s * 0.04f)
                        lineTo(s * 0.71f, y + s * 0.10f)
                    }
                    drawPath(p, ink, style = Stroke(s * 0.072f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}

private fun octagon(s: Float, inset: Float): Path {
    val a = s * inset
    val b = s - a
    val k = s * 0.24f
    return Path().apply {
        moveTo(a + k, a); lineTo(b - k, a); lineTo(b, a + k); lineTo(b, b - k)
        lineTo(b - k, b); lineTo(a + k, b); lineTo(a, b - k); lineTo(a, a + k); close()
    }
}

private fun DrawScope.star(color: Color, s: Float, c: Offset, r: Float) {
    val p = Path()
    for (i in 0 until 10) {
        val a = -PI.toFloat() / 2 + i * PI.toFloat() / 5
        val rr = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + cos(a) * rr
        val y = c.y + sin(a) * rr
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
    drawPath(p, color, style = Stroke(s * 0.02f, join = StrokeJoin.Round))
}
