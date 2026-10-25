package com.surajdev.spendwise.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.surajdev.spendwise.core.domain.model.ThemeMode
import com.surajdev.spendwise.core.platform.SystemBarsEffect

/** Dark: restrained corners. */
internal val QuietShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Light: softer, pebble-like corners. */
internal val AuroraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

/** Spacing scale used across screens. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val screen = 20.dp
}

@Composable
fun SpendWiseTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SystemBarsEffect(darkTheme = dark)
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtended else LightExtended) {
        val colors = if (dark) DarkColors else LightColors
        MaterialTheme(
            colorScheme = colors,
            typography = spendWiseTypography(),
            shapes = if (dark) QuietShapes else AuroraShapes,
        ) {
            // Screens use transparent Scaffolds over the app background, so they can't derive their
            // text color from a container; set the default here.
            CompositionLocalProvider(LocalContentColor provides colors.onBackground, content = content)
        }
    }
}

/** Access to tokens beyond Material's: `SpendWiseTheme.extended.success`. */
object SpendWiseTheme {
    val extended: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
