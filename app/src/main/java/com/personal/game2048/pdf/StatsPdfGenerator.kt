package com.personal.game2048.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.personal.game2048.storage.StatsData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class StatsPdfGenerator {

    fun generate(context: Context, stats: StatsData): File {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#4CAF70")
            textSize = 28f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.parseColor("#333333")
            textSize = 18f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            color = Color.parseColor("#666666")
            textSize = 14f
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.parseColor("#1A1A1A")
            textSize = 14f
            isAntiAlias = true
            isFakeBoldText = true
        }
        val linePaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            strokeWidth = 1f
        }
        val checkPaint = Paint().apply {
            color = Color.parseColor("#4CAF70")
            textSize = 14f
            isAntiAlias = true
        }
        val lockedPaint = Paint().apply {
            color = Color.parseColor("#CCCCCC")
            textSize = 14f
            isAntiAlias = true
        }

        var y = 50f
        val marginLeft = 40f

        canvas.drawColor(Color.WHITE)

        canvas.drawText("2048 Dark Edition", marginLeft, y, titlePaint)
        y += 30f
        headerPaint.textSize = 16f
        canvas.drawText("Stats Report", marginLeft, y, headerPaint)
        y += 20f
        labelPaint.textSize = 12f
        canvas.drawText("Generated on ${LocalDate.now()}", marginLeft, y, labelPaint)
        y += 25f
        canvas.drawLine(marginLeft, y, 555f, y, linePaint)
        y += 30f

        headerPaint.textSize = 18f
        labelPaint.textSize = 14f

        canvas.drawText("Statistics", marginLeft, y, headerPaint)
        y += 25f

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
            "Fastest Win" to if (stats.bestWinTimeSecs > 0) formatMinSec(stats.bestWinTimeSecs) else "—",
            "Best Score Time" to if (stats.bestScoreTimeSecs > 0) formatMinSec(stats.bestScoreTimeSecs) else "—"
        )

        val colWidth = 250f
        for (i in statsItems.indices step 2) {
            canvas.drawText(statsItems[i].first, marginLeft, y, labelPaint)
            canvas.drawText(statsItems[i].second, marginLeft + 140f, y, valuePaint)
            if (i + 1 < statsItems.size) {
                canvas.drawText(statsItems[i + 1].first, marginLeft + colWidth, y, labelPaint)
                canvas.drawText(statsItems[i + 1].second, marginLeft + colWidth + 140f, y, valuePaint)
            }
            y += 22f
        }

        y += 15f
        canvas.drawLine(marginLeft, y, 555f, y, linePaint)
        y += 25f

        canvas.drawText("Achievements", marginLeft, y, headerPaint)
        y += 25f

        val achievements = listOf(
            "first_game" to "First Move",
            "score_100" to "Centurion",
            "score_1000" to "High Roller",
            "score_10000" to "Legend",
            "first_win" to "First Win",
            "win_3" to "Hat Trick",
            "win_under_5min" to "Speed Demon",
            "hours_10" to "Marathon",
            "streak_7" to "Devoted",
            "streak_30" to "Obsessed",
            "tile_1024" to "Tile Hunter",
            "tile_2048" to "Master",
            "tile_4096" to "Grandmaster"
        )

        for ((id, name) in achievements) {
            val unlocked = id in stats.achievements
            val paint = if (unlocked) checkPaint else lockedPaint
            val prefix = if (unlocked) "✓ " else "○ "
            canvas.drawText("$prefix$name", marginLeft, y, paint)
            y += 20f
        }

        y += 20f
        canvas.drawLine(marginLeft, y, 555f, y, linePaint)
        y += 20f
        labelPaint.textSize = 10f
        canvas.drawText("2048 Dark Edition — Stats Report", marginLeft, y, labelPaint)

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
