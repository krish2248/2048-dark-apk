package com.personal.game2048.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ScoreGreen,
    onPrimary = Color.White,
    secondary = ScoreGreen,
    background = AppBackground,
    surface = CardBackground,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = NewGameButtonBorder
)

private val LightColorScheme = lightColorScheme(
    primary = LightScoreGreen,
    onPrimary = Color.White,
    secondary = LightScoreGreen,
    background = LightAppBackground,
    surface = LightCardBg,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
    outline = Color(0xFFCCCCCC)
)

@Composable
fun Game2048Theme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
