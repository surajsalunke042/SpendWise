package com.surajdev.spendwise.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp

/** Light theme "Aurora": vivid violet with cyan and rose glows, frosted-glass surfaces. */
internal object Aurora {
    val Violet = Color(0xFF6C5CE7)
    val VioletDeep = Color(0xFF5341CD)
    val Lavender = Color(0xFFA29BFE)
    val Orchid = Color(0xFFB45CE0)
    val Magenta = Color(0xFFE356A8)
    val Cyan = Color(0xFF00CEC9)
    val CyanDeep = Color(0xFF00A3A0)
    val Rose = Color(0xFFFD79A8)
    val Peach = Color(0xFFFFC59E)
    val Porcelain = Color(0xFFF7F6FF)
    val Ink = Color(0xFF1E1B3A)
    val InkSoft = Color(0xFF6B6890)
    val Emerald = Color(0xFF10B981)
    val Coral = Color(0xFFE5484D)
}

/** Dark theme "Quiet Luxury": warm charcoal with a muted brass accent. */
internal object QuietLuxury {
    val Espresso = Color(0xFF2B2622)
    val Ink = Color(0xFF1C1A17)
    val Champagne = Color(0xFFE8DCC6)
    val BrassLight = Color(0xFFC9A97A)
    val Charcoal = Color(0xFF14120F)
    val CharcoalSurface = Color(0xFF1D1A16)
    val CharcoalRaised = Color(0xFF26221D)
    val Parchment = Color(0xFFEDE7DD)
    val ParchmentSoft = Color(0xFFA39B8F)
    val SageLight = Color(0xFF8DB596)
    val BrickLight = Color(0xFFE08A80)
}

internal val LightColors = lightColorScheme(
    primary = Aurora.Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE6FF),
    onPrimaryContainer = Aurora.VioletDeep,
    secondary = Aurora.CyanDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5FAF8),
    onSecondaryContainer = Color(0xFF00504E),
    tertiary = Aurora.Magenta,
    onTertiary = Color.White,
    background = Aurora.Porcelain,
    onBackground = Aurora.Ink,
    surface = Color.White,
    onSurface = Aurora.Ink,
    surfaceVariant = Color(0xFFEEECFB),
    onSurfaceVariant = Aurora.InkSoft,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFAFF),
    surfaceContainer = Color(0xFFF3F1FE),
    surfaceContainerHigh = Color(0xFFEDEAFD),
    surfaceContainerHighest = Color(0xFFE6E2FB),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFC9C5E0),
    outlineVariant = Color(0xFFE4E1F5),
    error = Color(0xFFD92D45),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1E5),
    onErrorContainer = Color(0xFF7A0E21),
    inverseSurface = Aurora.Ink,
    inverseOnSurface = Color(0xFFF2F0FF),
    inversePrimary = Aurora.Lavender,
    scrim = Color(0xFF1E1B3A),
)

internal val DarkColors = darkColorScheme(
    primary = QuietLuxury.Champagne,
    onPrimary = QuietLuxury.Ink,
    primaryContainer = Color(0xFF3A332B),
    onPrimaryContainer = QuietLuxury.Parchment,
    secondary = QuietLuxury.BrassLight,
    onSecondary = Color(0xFF2A2013),
    secondaryContainer = Color(0xFF4A3C28),
    onSecondaryContainer = Color(0xFFF1E2C8),
    tertiary = QuietLuxury.SageLight,
    onTertiary = Color(0xFF0F2415),
    background = QuietLuxury.Charcoal,
    onBackground = QuietLuxury.Parchment,
    surface = QuietLuxury.CharcoalSurface,
    onSurface = QuietLuxury.Parchment,
    surfaceVariant = QuietLuxury.CharcoalRaised,
    onSurfaceVariant = QuietLuxury.ParchmentSoft,
    surfaceContainerLowest = Color(0xFF100E0C),
    surfaceContainerLow = Color(0xFF1A1714),
    surfaceContainer = QuietLuxury.CharcoalSurface,
    surfaceContainerHigh = QuietLuxury.CharcoalRaised,
    surfaceContainerHighest = Color(0xFF2F2A24),
    surfaceTint = Color.Transparent,
    outline = Color(0xFF4A433A),
    outlineVariant = Color(0xFF2E2924),
    error = QuietLuxury.BrickLight,
    onError = Color(0xFF3D0F0A),
    errorContainer = Color(0xFF5C1F19),
    onErrorContainer = Color(0xFFF4DEDA),
    inverseSurface = QuietLuxury.Parchment,
    inverseOnSurface = QuietLuxury.Ink,
    inversePrimary = QuietLuxury.Espresso,
)

/** Tokens Material's ColorScheme doesn't have, including the light theme's gradients and glass. */
@Immutable
data class ExtendedColors(
    val accent: Color,
    val success: Color,
    val hairline: Color,
    /** Spending went up (bad). */
    val trendUp: Color,
    /** Spending went down (good). */
    val trendDown: Color,
    val isDark: Boolean,
    /** True for the colorful light theme: gradients, glass surfaces and glows are drawn. */
    val vivid: Boolean,
    /** Card fill: translucent white in light (glass), solid surface in dark. */
    val cardFill: Color,
    /** Card rim: a light-catching gradient in light, a hairline in dark. */
    val cardBorder: Brush,
    /** Tinted ambient shadow under cards; transparent when there is none. */
    val cardShadow: Color,
    /** Hero cards (today's total): vivid gradient in light. */
    val heroBrush: Brush,
    val onHero: Color,
    val onHeroMuted: Color,
    /** Primary buttons, FAB and selected states. */
    val primaryBrush: Brush,
    val primaryGlow: Color,
    /** Highlighted chart bar, top to bottom. */
    val chartHighlightColors: List<Color>,
    val chartMuted: Color,
)

internal val LightExtended = ExtendedColors(
    accent = Aurora.Magenta,
    success = Aurora.Emerald,
    hairline = Color(0x146C5CE7),
    trendUp = Aurora.Coral,
    trendDown = Aurora.Emerald,
    isDark = false,
    vivid = true,
    cardFill = Color.White.copy(alpha = 0.68f),
    cardBorder = Brush.linearGradient(
        colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.35f), Color(0x266C5CE7)),
        start = Offset.Zero,
        end = Offset.Infinite,
    ),
    cardShadow = Color(0x1F6C5CE7),
    heroBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF5B4BDB), Aurora.Violet, Aurora.Orchid, Aurora.Magenta),
        start = Offset.Zero,
        end = Offset.Infinite,
    ),
    onHero = Color.White,
    onHeroMuted = Color.White.copy(alpha = 0.78f),
    primaryBrush = Brush.linearGradient(listOf(Aurora.Violet, Color(0xFF9B6BF2))),
    primaryGlow = Aurora.Violet.copy(alpha = 0.42f),
    chartHighlightColors = listOf(Aurora.Magenta, Aurora.Violet),
    chartMuted = Aurora.Violet.copy(alpha = 0.16f),
)

internal val DarkExtended = ExtendedColors(
    accent = QuietLuxury.BrassLight,
    success = QuietLuxury.SageLight,
    hairline = Color(0xFF2E2924),
    trendUp = QuietLuxury.BrickLight,
    trendDown = QuietLuxury.SageLight,
    isDark = true,
    vivid = false,
    cardFill = QuietLuxury.CharcoalSurface,
    cardBorder = SolidColor(Color(0xFF2E2924)),
    cardShadow = Color.Transparent,
    heroBrush = SolidColor(QuietLuxury.CharcoalSurface),
    onHero = QuietLuxury.Parchment,
    onHeroMuted = QuietLuxury.ParchmentSoft,
    primaryBrush = SolidColor(QuietLuxury.Champagne),
    primaryGlow = Color.Transparent,
    chartHighlightColors = listOf(QuietLuxury.Champagne, QuietLuxury.Champagne),
    chartMuted = QuietLuxury.Champagne.copy(alpha = 0.28f),
)

/**
 * Colors offered when creating a category. These are the stored values (earthy, used as-is in dark
 * mode); the light theme shows each one as its vivid counterpart via [categoryColor].
 */
val CategoryPalette: List<Long> = listOf(
    0xFFB07D4F, // caramel    -> coral
    0xFF56707F, // slate      -> teal
    0xFF8B6C8F, // mauve      -> magenta
    0xFF6F7D4E, // olive      -> amber
    0xFFB85C45, // terracotta -> soft violet
    0xFF4E8073, // sage teal  -> emerald
    0xFFC4A46B, // sand       -> sky blue
    0xFF8C857B, // stone      -> cool grey
    0xFF7A5C4A, // cocoa      -> tangerine
    0xFF5B6C9A, // dusk blue  -> indigo
    0xFFA0697A, // rosewood   -> rose
    0xFF3F6B5C, // pine       -> mint
)

private val VividPalette: Map<Long, Long> = mapOf(
    0xFFB07D4F to 0xFFFF6B6B,
    0xFF56707F to 0xFF00B8B3,
    0xFF8B6C8F to 0xFFF0609E,
    0xFF6F7D4E to 0xFFF59E0B,
    0xFFB85C45 to 0xFF8B7CF6,
    0xFF4E8073 to 0xFF10B981,
    0xFFC4A46B to 0xFF38A9F0,
    0xFF8C857B to 0xFF8E9AB5,
    0xFF7A5C4A to 0xFFFB7A3C,
    0xFF5B6C9A to 0xFF5B50E8,
    0xFFA0697A to 0xFFF43F5E,
    0xFF3F6B5C to 0xFF20BF6B,
)

/**
 * Display color for a stored category color: vivid in the light theme, earthy (slightly lifted
 * for legibility) in the dark theme.
 */
fun categoryColor(argb: Long, dark: Boolean): Color =
    if (dark) {
        lerp(Color(argb.toInt()), Color.White, 0.18f)
    } else {
        Color((VividPalette[argb] ?: argb).toInt())
    }
