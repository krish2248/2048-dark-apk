package com.personal.game2048.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.personal.game2048.R
import com.personal.game2048.data.Settings
import com.personal.game2048.data.ThemeMode
import com.personal.game2048.game.Rank

val Grotesk = FontFamily(
    Font(R.font.grotesk_regular, FontWeight.Normal),
    Font(R.font.grotesk_medium, FontWeight.Medium),
    Font(R.font.grotesk_bold, FontWeight.Bold),
)

/** Same-width digits, so counters and timers never jitter. */
const val TNUM = "tnum"

private fun style(weight: FontWeight, size: Int) =
    TextStyle(fontFamily = Grotesk, fontWeight = weight, fontSize = size.sp, fontFeatureSettings = TNUM)

private val AppTypography = Typography(
    displayLarge = style(FontWeight.Bold, 72),
    displayMedium = style(FontWeight.Bold, 44),
    headlineMedium = style(FontWeight.Bold, 28),
    titleLarge = style(FontWeight.Bold, 22),
    titleMedium = style(FontWeight.Bold, 17),
    titleSmall = style(FontWeight.Bold, 14),
    bodyLarge = style(FontWeight.Normal, 16),
    bodyMedium = style(FontWeight.Normal, 14),
    bodySmall = style(FontWeight.Normal, 12),
    labelLarge = style(FontWeight.Medium, 15),
    labelMedium = style(FontWeight.Medium, 13),
    labelSmall = style(FontWeight.Medium, 11),
)

/** Tile ramp: [low] covers 2..2048, [high] covers everything past it. */
@Immutable
class Palette(val id: String, val title: String, val unlock: Rank, low: List<Long>, high: List<Long>, accentDark: Long, accentLight: Long) {
    val accentDark = Color(accentDark)
    val accentLight = Color(accentLight)
    private val tiles: List<Color> = low.map { Color(it) } + high.map { Color(it) }
    private val inks: List<Color> = tiles.map { if (it.luminance() > 0.34f) Color(0xFF07130B) else Color(0xFFEAFBF0) }

    fun tile(exp: Int): Color = tiles[(exp - 1).coerceIn(0, tiles.lastIndex)]
    fun ink(exp: Int): Color = inks[(exp - 1).coerceIn(0, inks.lastIndex)]
    val preview: List<Color> get() = listOf(tile(3), tile(6), tile(9), tile(11), tile(14))
}

val Palettes = listOf(
    Palette(
        "emerald", "Emerald", Rank.PEBBLE_1,
        listOf(
            0xFF1B2D1E, 0xFF1E3A24, 0xFF1F4D2C, 0xFF256338, 0xFF2E7A47, 0xFF389157,
            0xFF44A866, 0xFF52C078, 0xFF63D48B, 0xFF82E4A5, 0xFFB0F0C8,
        ),
        listOf(0xFFE2F7A8, 0xFFFFE27A, 0xFFFFB84D, 0xFFFF8552, 0xFFFF5C7A, 0xFFE85CC8, 0xFFA86BFF, 0xFF6C8CFF, 0xFF4CC9F0, 0xFFFFFFFF),
        0xFF5FD38A, 0xFF23743F,
    ),
    Palette(
        "ocean", "Ocean", Rank.IRON_1,
        listOf(
            0xFF152633, 0xFF173246, 0xFF18425F, 0xFF1A557B, 0xFF1E6998, 0xFF2580B5,
            0xFF3397CF, 0xFF4DAEE2, 0xFF70C4EF, 0xFF98D8F7, 0xFFC5EBFC,
        ),
        listOf(0xFFB7F5E6, 0xFF7CF0C4, 0xFFB8F27C, 0xFFFFE27A, 0xFFFFB05C, 0xFFFF7A6B, 0xFFF061A8, 0xFFB46BFF, 0xFF7C7CFF, 0xFFFFFFFF),
        0xFF55B6EA, 0xFF15608F,
    ),
    Palette(
        "amethyst", "Amethyst", Rank.STEEL_1,
        listOf(
            0xFF241D33, 0xFF2E2346, 0xFF3C2C5F, 0xFF4D377B, 0xFF604498, 0xFF7554B3,
            0xFF8B67CB, 0xFFA17EDD, 0xFFB798EA, 0xFFCDB5F4, 0xFFE4D5FB,
        ),
        listOf(0xFFFBD0EC, 0xFFFF9FD2, 0xFFFF7A9E, 0xFFFF8F66, 0xFFFFC15A, 0xFFD7F06B, 0xFF6FE8A8, 0xFF4FD0E8, 0xFF6C9CFF, 0xFFFFFFFF),
        0xFFB08CF0, 0xFF5B3AA0,
    ),
    Palette(
        "ember", "Ember", Rank.EMERALD_1,
        listOf(
            0xFF2E1C17, 0xFF40231A, 0xFF582C1D, 0xFF743620, 0xFF924224, 0xFFB1502A,
            0xFFCC6433, 0xFFE27C43, 0xFFF0975B, 0xFFF8B480, 0xFFFDD3AE,
        ),
        listOf(0xFFFFE98F, 0xFFE5F27A, 0xFF9BE87C, 0xFF5FE0AE, 0xFF4FCBE8, 0xFF6C9CFF, 0xFF9C7CFF, 0xFFE070E8, 0xFFFF6B9C, 0xFFFFFFFF),
        0xFFF08D50, 0xFFA3451A,
    ),
    Palette(
        "mono", "Graphite", Rank.OBSIDIAN_1,
        listOf(
            0xFF232323, 0xFF2D2D2D, 0xFF3A3A3A, 0xFF4A4A4A, 0xFF5C5C5C, 0xFF707070,
            0xFF868686, 0xFF9D9D9D, 0xFFB5B5B5, 0xFFCDCDCD, 0xFFE6E6E6,
        ),
        listOf(0xFFF4E7C3, 0xFFF0D48A, 0xFFE8BC4E, 0xFFDDA62B, 0xFFC7E8F4, 0xFF9AD4EE, 0xFFD8C7F4, 0xFFBFA0F0, 0xFFF4C7D8, 0xFFFFFFFF),
        0xFFD6D6D6, 0xFF3D3D3D,
    ),
)

fun paletteOf(id: String) = Palettes.firstOrNull { it.id == id } ?: Palettes[0]

@Immutable
class AppColors(
    val dark: Boolean,
    val palette: Palette,
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val board: Color,
    val cell: Color,
) {
    val gold = if (dark) Color(0xFFF2C14E) else Color(0xFF8A6100)
    val danger = if (dark) Color(0xFFFF6B6B) else Color(0xFFB3261E)
    val accentSoft = lerp(surface, accent, if (dark) 0.14f else 0.12f)
}

private fun darkColors(p: Palette) = AppColors(
    dark = true, palette = p,
    bg = Color(0xFF0B0D0C), surface = Color(0xFF131615), raised = Color(0xFF1B1F1D), border = Color(0xFF262B28),
    text = Color(0xFFF2F6F3), muted = Color(0xFF8F9A94), accent = p.accentDark, onAccent = Color(0xFF07130B),
    board = Color(0xFF131615), cell = Color(0xFF1B1F1D),
)

private fun lightColors(p: Palette) = AppColors(
    dark = false, palette = p,
    bg = Color(0xFFF5F6F2), surface = Color(0xFFFFFFFF), raised = Color(0xFFECEEE8), border = Color(0xFFDDE0D8),
    text = Color(0xFF141A16), muted = Color(0xFF5F6862), accent = p.accentLight, onAccent = Color.White,
    board = Color(0xFFE6E9E1), cell = Color(0xFFD9DDD3),
)

private val LocalColors = staticCompositionLocalOf { darkColors(Palettes[0]) }

object App {
    val colors: AppColors @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: Typography @Composable @ReadOnlyComposable get() = MaterialTheme.typography
}

@Composable
fun isDark(settings: Settings): Boolean = when (settings.theme) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun AppTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = isDark(settings)
    val colors = remember(dark, settings.palette) {
        val p = paletteOf(settings.palette)
        if (dark) darkColors(p) else lightColors(p)
    }
    val scheme = if (dark) darkColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.text,
        surface = colors.surface, onSurface = colors.text, surfaceVariant = colors.raised, onSurfaceVariant = colors.muted,
        outline = colors.border, surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
        surfaceContainerHighest = colors.raised, surfaceContainerLow = colors.surface, error = colors.danger,
    ) else lightColorScheme(
        primary = colors.accent, onPrimary = colors.onAccent, background = colors.bg, onBackground = colors.text,
        surface = colors.surface, onSurface = colors.text, surfaceVariant = colors.raised, onSurfaceVariant = colors.muted,
        outline = colors.border, surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
        surfaceContainerHighest = colors.raised, surfaceContainerLow = colors.surface, error = colors.danger,
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography) {
        CompositionLocalProvider(LocalColors provides colors, LocalIndication provides ripple(), content = content)
    }
}
