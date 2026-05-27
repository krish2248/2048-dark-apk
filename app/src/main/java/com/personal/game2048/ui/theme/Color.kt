package com.personal.game2048.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme backgrounds
val AppBackground = Color(0xFF0D0D0D)
val GridBackground = Color(0xFF1A1A1A)
val TileEmpty = Color(0xFF1E1E1E)
val CardBackground = Color(0xFF1A1A1A)

// Score / accent
val ScoreGreen = Color(0xFF4CAF70)
val SubtitleText = Color(0xFF666666)

// Tile colors — green gradient
val Tile2 = Color(0xFF1B2D1E)
val Tile4 = Color(0xFF1E3A24)
val Tile8 = Color(0xFF1F4D2C)
val Tile16 = Color(0xFF256338)
val Tile32 = Color(0xFF2E7A47)
val Tile64 = Color(0xFF389157)
val Tile128 = Color(0xFF44A866)
val Tile256 = Color(0xFF52C078)
val Tile512 = Color(0xFF63D48B)
val Tile1024 = Color(0xFF82E4A5)
val Tile2048 = Color(0xFFB0F0C8)
val TileAbove2048 = Color(0xFFCCF7DC)

// Tile text
val TileTextDark = Color(0xFFCCFFDD)
val TileTextLight = Color(0xFF0A2010)

// Bottom nav
val NavBarBackground = Color(0xFF111111)
val NavButtonBg = Color(0xFF1E1E1E)
val NavIconActive = Color(0xFF4CAF70)
val NavIconDefault = Color(0xFFAAAAAA)

// New Game button
val NewGameButtonBg = Color(0xFF1A1A1A)
val NewGameButtonText = Color(0xFF4CAF70)
val NewGameButtonBorder = Color(0xFF2E2E2E)

// Light theme
val LightAppBackground = Color(0xFFF5F5F0)
val LightGridBackground = Color(0xFFE8E8E0)
val LightTileEmpty = Color(0xFFDDDDD5)
val LightCardBg = Color(0xFFFFFFFF)
val LightScoreGreen = Color(0xFF2E7D45)
val LightSubtitleText = Color(0xFF888888)
val LightNavBarBackground = Color(0xFFEEEEEE)
val LightNavButtonBg = Color(0xFFDDDDDD)

fun tileColor(value: Int): Color = when (value) {
    2 -> Tile2
    4 -> Tile4
    8 -> Tile8
    16 -> Tile16
    32 -> Tile32
    64 -> Tile64
    128 -> Tile128
    256 -> Tile256
    512 -> Tile512
    1024 -> Tile1024
    2048 -> Tile2048
    else -> if (value > 2048) TileAbove2048 else TileEmpty
}

fun tileTextColor(value: Int): Color =
    if (value >= 1024) TileTextLight else TileTextDark

fun tileFontSize(value: Int): Int = when {
    value < 10 -> 36
    value < 100 -> 30
    value < 1000 -> 24
    value < 10000 -> 18
    else -> 14
}
