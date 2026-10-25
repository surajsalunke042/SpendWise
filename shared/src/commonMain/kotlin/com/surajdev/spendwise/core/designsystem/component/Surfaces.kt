package com.surajdev.spendwise.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.surajdev.spendwise.core.designsystem.theme.Aurora
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource

/**
 * Space the floating/docked bottom navigation occupies, including the system navigation bar.
 * Tab screens add it to their bottom padding so content can scroll underneath the glass bar.
 */
val LocalBottomBarClearance = compositionLocalOf { 0.dp }

/**
 * Screen backdrop. Light: a soft aurora mesh (violet, cyan, rose and peach glows on porcelain).
 * Dark: flat charcoal.
 */
@Composable
fun AppBackground(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    if (!SpendWiseTheme.extended.vivid) {
        Box(modifier.background(base))
        return
    }
    Canvas(modifier) {
        drawRect(base)
        val w = size.width
        val h = size.height
        glow(Aurora.Lavender.copy(alpha = 0.55f), Offset(w * 0.05f, h * 0.02f), w * 0.95f)
        glow(Aurora.Cyan.copy(alpha = 0.28f), Offset(w * 1.02f, h * 0.20f), w * 0.80f)
        glow(Aurora.Rose.copy(alpha = 0.26f), Offset(w * 0.00f, h * 0.62f), w * 0.85f)
        glow(Aurora.Peach.copy(alpha = 0.34f), Offset(w * 0.98f, h * 0.95f), w * 0.85f)
    }
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

/** Soft, tinted ambient shadow (a no-op when the theme has none). */
@Composable
fun Modifier.ambientShadow(shape: Shape, color: Color, radius: Dp = 24.dp, offsetY: Dp = 8.dp): Modifier =
    if (color.alpha == 0f) this else dropShadow(shape, Shadow(radius = radius, color = color, offset = DpOffset(0.dp, offsetY)))

/**
 * The standard container. Light: frosted glass with a light-catching rim and a violet ambient
 * shadow. Dark: solid surface with a hairline border.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Spacing.lg,
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extended = SpendWiseTheme.extended
    Column(
        modifier = modifier
            .ambientShadow(shape, extended.cardShadow)
            .clip(shape)
            .background(extended.cardFill)
            .border(1.dp, extended.cardBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

/**
 * Feature card for headline numbers. Light: vivid violet-to-magenta gradient with light orbs and a
 * glossy top edge, white content. Dark: same as [SurfaceCard].
 * Read [SpendWiseTheme.extended].onHero / onHeroMuted for text colors inside.
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = Spacing.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    val extended = SpendWiseTheme.extended
    if (!extended.vivid) {
        SurfaceCard(modifier, onClick, contentPadding, content = content)
        return
    }
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .ambientShadow(shape, extended.primaryGlow, radius = 28.dp, offsetY = 12.dp)
            .clip(shape)
            .background(extended.heroBrush)
            .drawBehind {
                // Light orbs and a glossy top edge give the gradient depth.
                drawCircle(Color.White.copy(alpha = 0.14f), radius = size.width * 0.42f, center = Offset(size.width * 0.95f, size.height * 0.05f))
                drawCircle(Color.White.copy(alpha = 0.08f), radius = size.width * 0.28f, center = Offset(size.width * 0.78f, size.height * 1.05f))
                drawCircle(Aurora.Cyan.copy(alpha = 0.22f), radius = size.width * 0.30f, center = Offset(size.width * 0.02f, size.height * 1.0f))
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), endY = size.height * 0.5f))
            }
            .border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f))), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides extended.onHero) {
            content()
        }
    }
}

/** Main floating action. Light: gradient with a matching glow. Dark: solid champagne. */
@Composable
fun AppFab(
    onClick: () -> Unit,
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    text: String? = null,
) {
    val extended = SpendWiseTheme.extended
    val shape = MaterialTheme.shapes.medium
    val decorated = if (extended.vivid) {
        modifier.ambientShadow(shape, extended.primaryGlow, radius = 20.dp, offsetY = 8.dp).background(extended.primaryBrush, shape)
    } else {
        modifier
    }
    val container = if (extended.vivid) Color.Transparent else MaterialTheme.colorScheme.primary
    val elevation = if (extended.vivid) FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp) else FloatingActionButtonDefaults.elevation()
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    if (text != null) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = decorated,
            shape = shape,
            containerColor = container,
            contentColor = onPrimary,
            elevation = elevation,
            icon = { AppIcon(icon, contentDescription = null, tint = onPrimary) },
            text = { Text(text, style = MaterialTheme.typography.labelLarge) },
        )
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = decorated,
            shape = shape,
            containerColor = container,
            contentColor = onPrimary,
            elevation = elevation,
        ) {
            AppIcon(icon, contentDescription = contentDescription, tint = onPrimary)
        }
    }
}

/** Text field colors: frosted fill in light, hairline outline in dark. */
@Composable
fun appTextFieldColors(): TextFieldColors {
    val extended = SpendWiseTheme.extended
    return if (extended.vivid) {
        OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White.copy(alpha = 0.72f),
            focusedContainerColor = Color.White,
            errorContainerColor = Color.White,
            unfocusedBorderColor = extended.hairline,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        )
    } else {
        OutlinedTextFieldDefaults.colors(unfocusedBorderColor = extended.hairline)
    }
}

/** A small accent dot/pill used in legends. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}
