package com.personal.game2048.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import com.personal.game2048.R
import com.personal.game2048.data.Stats
import com.personal.game2048.data.combined
import com.personal.game2048.game.Achievements
import com.personal.game2048.game.Rank
import com.personal.game2048.game.clock
import com.personal.game2048.game.fmt
import com.personal.game2048.game.hoursMinutes
import com.personal.game2048.game.tileLabel
import java.io.File
import java.time.LocalDate

/** Two-page dark stats report: numbers first, achievements second. */
class StatsPdfGenerator {
    private val bg = Color.parseColor("#0B0D0C")
    private val card = Color.parseColor("#171B19")
    private val green = Color.parseColor("#5FD38A")
    private val light = Color.parseColor("#F2F6F3")
    private val dim = Color.parseColor("#8F9A94")
    private val left = 36f
    private val width = 523f

    fun generate(context: Context, stats: Stats): File {
        val bold = ResourcesCompat.getFont(context, R.font.grotesk_bold) ?: Typeface.DEFAULT_BOLD
        val regular = ResourcesCompat.getFont(context, R.font.grotesk_regular) ?: Typeface.DEFAULT
        fun paint(c: Int, size: Float, tf: Typeface = regular) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c; textSize = size; typeface = tf }
        val h1 = paint(light, 30f, bold)
        val h2 = paint(green, 15f, bold)
        val value = paint(light, 17f, bold)
        val label = paint(dim, 10.5f)
        val box = paint(card, 1f)
        val m = stats.combined()
        val pdf = PdfDocument()

        fun header(c: Canvas, sub: String): Float {
            c.drawColor(bg)
            c.drawText("2048", left, 62f, h1)
            c.drawText("Dark Edition", left + h1.measureText("2048") + 10f, 62f, paint(green, 16f, bold))
            c.drawText(sub, left, 82f, label)
            return 116f
        }

        fun footer(c: Canvas) = c.drawText("2048 Dark Edition  |  sonikrish.com", left, 818f, paint(dim, 9f))

        val page1 = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        var c = page1.canvas
        var y = header(c, "Stats report, ${LocalDate.now()}")
        c.drawText("${Rank.of(stats.classicBest).title}   Level ${stats.level}", left, y, h2)
        y += 18f
        val cells = listOf(
            "Best score" to fmt(m.bestScore),
            "Top tile" to if (m.bestExp > 0) tileLabel(m.bestExp) else "None",
            "Games" to fmt(m.games),
            "Average score" to fmt(if (m.games > 0) m.totalScore / m.games else 0),
            "Reached 2048" to fmt(m.wins),
            "Win rate" to "${if (m.games > 0) m.wins * 100 / m.games else 0}%",
            "Time played" to hoursMinutes(m.timeMs),
            "Longest run" to clock(m.longestMs),
            "Moves" to fmt(m.moves),
            "Merges" to fmt(m.merges),
            "Fastest 2048" to (m.fastestMs[11]?.let { clock(it) } ?: "None"),
            "Best score time" to if (m.bestScoreMs > 0) clock(m.bestScoreMs) else "None",
            "Day streak" to "${stats.currentStreak}",
            "Best streak" to "${stats.longestStreak}",
            "Biggest swipe" to "+${fmt(stats.biggestGain)}",
            "Most merges in a swipe" to "${stats.biggestCombo}",
            "Daily goals done" to fmt(stats.goalsDone),
            "Undos used" to fmt(m.undos),
        )
        val w = (width - 10f) / 2
        cells.forEachIndexed { i, (l, v) ->
            val x = left + (i % 2) * (w + 10f)
            val top = y + (i / 2) * 56f
            c.drawRoundRect(x, top, x + w, top + 48f, 9f, 9f, box)
            c.drawText(v, x + 14f, top + 22f, value)
            c.drawText(l, x + 14f, top + 38f, label)
        }
        footer(c)
        pdf.finishPage(page1)

        val page2 = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
        c = page2.canvas
        val done = Achievements.all.count { it.id in stats.achievements }
        y = header(c, "Achievements, $done of ${Achievements.all.size} unlocked")
        val rows = (Achievements.all.size + 2) / 3
        val on = paint(light, 10.5f, bold)
        val off = paint(Color.parseColor("#59625D"), 10.5f)
        val step = minOf(22f, 660f / rows)
        Achievements.all.forEachIndexed { i, a ->
            val got = a.id in stats.achievements
            val x = left + (i / rows) * (width / 3)
            val yy = y + (i % rows) * step
            c.drawCircle(x + 4f, yy - 4f, 3.5f, paint(if (got) green else card, 1f))
            c.drawText(a.title, x + 14f, yy, if (got) on else off)
        }
        footer(c)
        pdf.finishPage(page2)

        val file = File(context.cacheDir, "2048_stats_report.pdf")
        file.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return file
    }
}
