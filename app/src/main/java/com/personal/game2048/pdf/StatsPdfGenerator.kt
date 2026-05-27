package com.personal.game2048.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.personal.game2048.storage.StatsData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class StatsPdfGenerator {

    private val bgColor = Color.parseColor("#0D0D0D")
    private val cardBg = Color.parseColor("#1A1A1A")
    private val green = Color.parseColor("#4CAF70")
    private val dimGreen = Color.parseColor("#2E7A47")
    private val lightText = Color.parseColor("#CCFFDD")
    private val dimText = Color.parseColor("#666666")
    private val lineColor = Color.parseColor("#2E2E2E")
    private val mono = Typeface.MONOSPACE

    fun generate(context: Context, stats: StatsData): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = green; textSize = 32f; isAntiAlias = true; typeface = Typeface.create(mono, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = lightText; textSize = 16f; isAntiAlias = true; typeface = Typeface.create(mono, Typeface.BOLD)
        }
        val datePaint = Paint().apply {
            color = dimText; textSize = 11f; isAntiAlias = true; typeface = mono
        }
        val headerPaint = Paint().apply {
            color = green; textSize = 16f; isAntiAlias = true; typeface = Typeface.create(mono, Typeface.BOLD)
        }
        val labelPaint = Paint().apply {
            color = dimText; textSize = 12f; isAntiAlias = true; typeface = mono
        }
        val valuePaint = Paint().apply {
            color = lightText; textSize = 13f; isAntiAlias = true; typeface = Typeface.create(mono, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = lineColor; strokeWidth = 1f; style = Paint.Style.STROKE
        }
        val cardPaint = Paint().apply {
            color = cardBg; style = Paint.Style.FILL; isAntiAlias = true
        }
        val checkPaint = Paint().apply {
            color = green; textSize = 12f; isAntiAlias = true; typeface = Typeface.create(mono, Typeface.BOLD)
        }
        val lockedPaint = Paint().apply {
            color = Color.parseColor("#444444"); textSize = 12f; isAntiAlias = true; typeface = mono
        }
        val footerPaint = Paint().apply {
            color = dimText; textSize = 9f; isAntiAlias = true; typeface = mono
        }

        canvas.drawColor(bgColor)

        var y = 45f
        val ml = 35f
        val mr = 560f

        // Header
        canvas.drawText("2048", ml, y, titlePaint)
        titlePaint.textSize = 18f
        canvas.drawText("Dark Edition", ml + titlePaint.measureText("2048  ") - 20f, y, Paint().apply {
            color = dimGreen; textSize = 18f; isAntiAlias = true; typeface = Typeface.create(mono, Typeface.BOLD)
        })
        y += 24f
        canvas.drawText("Stats Report", ml, y, subtitlePaint)
        y += 18f
        canvas.drawText("Generated ${LocalDate.now()}", ml, y, datePaint)
        y += 20f

        // Divider
        canvas.drawLine(ml, y, mr, y, linePaint)
        y += 25f

        // Stats section
        canvas.drawText("// STATISTICS", ml, y, headerPaint)
        y += 20f

        val statsItems = listOf(
            "Best Score" to "${stats.bestScore}",
            "Highest Tile" to "${stats.highestTile}",
            "Games Played" to "${stats.totalGames}",
            "Games Won" to "${stats.gamesWon}",
            "Games Lost" to "${stats.gamesLost}",
            "Win Rate" to "${stats.winRate}%",
            "Hours Played" to formatTime(stats.totalSeconds),
            "Current Streak" to "${stats.currentStreak} days",
            "Best Streak" to "${stats.longestStreak} days",
            "Fastest Win" to if (stats.bestWinTimeSecs > 0) formatMinSec(stats.bestWinTimeSecs) else "--",
            "Best Score Time" to if (stats.bestScoreTimeSecs > 0) formatMinSec(stats.bestScoreTimeSecs) else "--"
        )

        val cardW = 250f
        val cardH = 38f
        val gap = 12f

        for (i in statsItems.indices step 2) {
            val x1 = ml
            canvas.drawRoundRect(RectF(x1, y - 14f, x1 + cardW, y + cardH - 14f), 8f, 8f, cardPaint)
            canvas.drawText(statsItems[i].first, x1 + 12f, y + 4f, labelPaint)
            canvas.drawText(statsItems[i].second, x1 + 12f, y + 20f, valuePaint)

            if (i + 1 < statsItems.size) {
                val x2 = ml + cardW + gap
                canvas.drawRoundRect(RectF(x2, y - 14f, x2 + cardW, y + cardH - 14f), 8f, 8f, cardPaint)
                canvas.drawText(statsItems[i + 1].first, x2 + 12f, y + 4f, labelPaint)
                canvas.drawText(statsItems[i + 1].second, x2 + 12f, y + 20f, valuePaint)
            }
            y += cardH + 6f
        }

        y += 10f
        canvas.drawLine(ml, y, mr, y, linePaint)
        y += 22f

        // Achievements section
        canvas.drawText("// ACHIEVEMENTS", ml, y, headerPaint)
        y += 6f

        val countPaint = Paint().apply {
            color = dimGreen; textSize = 11f; isAntiAlias = true; typeface = mono
        }
        val unlocked = stats.achievements.size
        canvas.drawText("$unlocked unlocked", ml + headerPaint.measureText("// ACHIEVEMENTS  "), y, countPaint)
        y += 18f

        val achievements = listOf(
            "first_game" to "First Move",
            "games_10" to "Dedicated",
            "games_50" to "Veteran",
            "games_100" to "Century Club",
            "score_100" to "Centurion",
            "score_500" to "Rising Star",
            "score_1000" to "High Roller",
            "score_5000" to "Expert",
            "score_10000" to "Legend",
            "score_50000" to "Unstoppable",
            "first_win" to "First Win",
            "win_3" to "Hat Trick",
            "win_10" to "Champion",
            "win_under_5min" to "Speed Demon",
            "win_under_3min" to "Lightning",
            "hours_1" to "Getting Hooked",
            "hours_5" to "Time Flies",
            "hours_10" to "Marathon",
            "streak_3" to "On a Roll",
            "streak_7" to "Devoted",
            "streak_14" to "Committed",
            "streak_30" to "Obsessed",
            "tile_512" to "Halfway There",
            "tile_1024" to "Tile Hunter",
            "tile_2048" to "Master",
            "tile_4096" to "Grandmaster",
            "tile_8192" to "Beyond Limits"
        )

        val achCardW = 165f
        val achCardH = 22f

        for (i in achievements.indices step 3) {
            for (j in 0 until 3) {
                if (i + j >= achievements.size) break
                val (id, name) = achievements[i + j]
                val isUnlocked = id in stats.achievements
                val xPos = ml + j * (achCardW + 8f)
                val paint = if (isUnlocked) checkPaint else lockedPaint
                val prefix = if (isUnlocked) "> " else "  "
                canvas.drawText("$prefix$name", xPos, y + 12f, paint)
            }
            y += achCardH
        }

        y += 15f
        canvas.drawLine(ml, y, mr, y, linePaint)
        y += 16f

        // Footer
        canvas.drawText("2048 Dark Edition  |  sonikrish.com", ml, y, footerPaint)

        pdf.finishPage(page)

        val file = File(context.cacheDir, "2048_stats_report.pdf")
        pdf.writeTo(FileOutputStream(file))
        pdf.close()
        return file
    }

    private fun formatTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes}m"
    }

    private fun formatMinSec(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
