package com.personal.game2048.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.game2048.storage.StatsStorage
import com.personal.game2048.ui.theme.*

@Composable
fun StatsScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val statsStorage = remember { StatsStorage(context) }
    val stats = remember { statsStorage.load() }

    val bgColor = if (isDarkTheme) AppBackground else LightAppBackground
    val cardBg = if (isDarkTheme) CardBackground else LightCardBg
    val accentColor = if (isDarkTheme) ScoreGreen else LightScoreGreen
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val labelColor = if (isDarkTheme) SubtitleText else LightSubtitleText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = accentColor)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Stats",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Stats grid — 2 columns
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

        for (i in statsItems.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = statsItems[i].first,
                    value = statsItems[i].second,
                    bgColor = cardBg,
                    accentColor = accentColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f)
                )
                if (i + 1 < statsItems.size) {
                    StatCard(
                        label = statsItems[i + 1].first,
                        value = statsItems[i + 1].second,
                        bgColor = cardBg,
                        accentColor = accentColor,
                        labelColor = labelColor,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Achievements",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = textColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        val achievements = listOf(
            "first_game" to ("First Move" to "Play 1 game"),
            "score_100" to ("Centurion" to "Score 100+"),
            "score_1000" to ("High Roller" to "Score 1,000+"),
            "score_10000" to ("Legend" to "Score 10,000+"),
            "first_win" to ("First Win" to "Win 1 game"),
            "win_3" to ("Hat Trick" to "Win 3 games"),
            "win_under_5min" to ("Speed Demon" to "Win in < 5 min"),
            "hours_10" to ("Marathon" to "10+ hours played"),
            "streak_7" to ("Devoted" to "7-day streak"),
            "streak_30" to ("Obsessed" to "30-day streak"),
            "tile_1024" to ("Tile Hunter" to "Reach 1024"),
            "tile_2048" to ("Master" to "Reach 2048"),
            "tile_4096" to ("Grandmaster" to "Reach 4096")
        )

        for (i in achievements.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val (id1, info1) = achievements[i]
                AchievementBadge(
                    name = info1.first,
                    description = info1.second,
                    unlocked = id1 in stats.achievements,
                    bgColor = cardBg,
                    accentColor = accentColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f)
                )
                if (i + 1 < achievements.size) {
                    val (id2, info2) = achievements[i + 1]
                    AchievementBadge(
                        name = info2.first,
                        description = info2.second,
                        unlocked = id2 in stats.achievements,
                        bgColor = cardBg,
                        accentColor = accentColor,
                        labelColor = labelColor,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    bgColor: Color,
    accentColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = labelColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AchievementBadge(
    name: String,
    description: String,
    unlocked: Boolean,
    bgColor: Color,
    accentColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    val alpha = if (unlocked) 1f else 0.4f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = alpha))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (unlocked) accentColor else labelColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (unlocked) accentColor else labelColor,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = labelColor,
            textAlign = TextAlign.Center
        )
    }
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
