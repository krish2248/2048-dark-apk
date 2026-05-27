package com.personal.game2048.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.personal.game2048.pdf.StatsPdfGenerator
import com.personal.game2048.storage.GameSaveStorage
import com.personal.game2048.storage.StatsStorage
import com.personal.game2048.ui.theme.*

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNewGame: () -> Unit,
    onContinueGame: () -> Unit,
    onStats: () -> Unit
) {
    val context = LocalContext.current
    val saveStorage = remember { GameSaveStorage(context) }
    val statsStorage = remember { StatsStorage(context) }
    val hasSave = saveStorage.hasSavedGame()
    val stats = remember { statsStorage.load() }

    val bgColor = if (isDarkTheme) AppBackground else LightAppBackground
    val textColor = if (isDarkTheme) TileTextDark else LightScoreGreen
    val subtitleColor = if (isDarkTheme) SubtitleText else LightSubtitleText
    val accentColor = if (isDarkTheme) ScoreGreen else LightScoreGreen
    val borderColor = if (isDarkTheme) NewGameButtonBorder else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle theme",
                tint = accentColor
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "2048",
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = textColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join the numbers and\nget to the 2048 tile!",
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                color = subtitleColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Best: ${stats.bestScore}",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = accentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (hasSave) {
                OutlinedButton(
                    onClick = onContinueGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(2.dp, accentColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDarkTheme) NewGameButtonBg else LightCardBg
                    )
                ) {
                    Text(
                        text = "Continue Game",
                        color = accentColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedButton(
                onClick = onNewGame,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDarkTheme) NewGameButtonBg else LightCardBg
                )
            ) {
                Text(
                    text = "New Game",
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDarkTheme) NewGameButtonBg else LightCardBg
                )
            ) {
                Text(
                    text = "Stats",
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    val pdfGen = StatsPdfGenerator()
                    val currentStats = statsStorage.load()
                    val pdfFile = pdfGen.generate(context, currentStats)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "My 2048 Stats Report")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Stats via..."))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, borderColor),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDarkTheme) NewGameButtonBg else LightCardBg
                )
            ) {
                Text(
                    text = "Share Stats",
                    color = accentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
