package com.surajdev.spendwise.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.surajdev.spendwise.core.designsystem.icon.AppIcons
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.Spacing
import com.surajdev.spendwise.core.designsystem.theme.categoryColor
import com.surajdev.spendwise.core.designsystem.theme.overline
import com.surajdev.spendwise.core.designsystem.theme.tabular
import com.surajdev.spendwise.core.domain.model.Money
import com.surajdev.spendwise.core.domain.model.MoneyFormatter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs
import kotlin.math.roundToInt

/** Uppercase, widely tracked section label. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.overline(),
        color = color,
        modifier = modifier,
    )
}

@Composable
fun AmountText(
    money: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
    showFraction: Boolean? = null,
) {
    Text(
        text = MoneyFormatter.format(money, showFraction),
        style = style.tabular(),
        color = color,
        modifier = modifier,
        maxLines = 1,
    )
}

@Composable
fun AppIcon(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 24.dp,
) {
    Icon(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/**
 * Category icon on a tinted background. Light: rounded square with a soft gradient tint.
 * Dark: tinted circle.
 */
@Composable
fun CategoryBadge(
    iconKey: String,
    colorArgb: Long,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val extended = SpendWiseTheme.extended
    val color = categoryColor(colorArgb, extended.isDark)
    val shape = if (extended.vivid) RoundedCornerShape(size * 0.32f) else CircleShape
    val fill = if (extended.vivid) {
        Brush.linearGradient(listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.08f)))
    } else {
        SolidColor(color.copy(alpha = 0.14f))
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(AppIcons.forKey(iconKey), contentDescription = null, tint = color, size = size * 0.5f)
    }
}

/**
 * "12% more than yesterday". Up is shown in red tones (spending more), down in green.
 * Nothing is shown when there's no baseline to compare against.
 * Light theme draws it as a tinted pill; [onHero] adapts it for a gradient hero card.
 */
@Composable
fun ChangeIndicator(change: Double?, label: String, modifier: Modifier = Modifier, onHero: Boolean = false) {
    if (change == null) return
    val extended = SpendWiseTheme.extended
    val up = change > 0
    val percent = (abs(change) * 100).roundToInt()
    val color = when {
        onHero && extended.vivid -> extended.onHero
        percent == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        up -> extended.trendUp
        else -> extended.trendDown
    }
    val pill = when {
        !extended.vivid -> Modifier
        onHero -> Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)
        else -> Modifier.clip(CircleShape).background(color.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 4.dp)
    }
    Row(modifier = modifier.then(pill), verticalAlignment = Alignment.CenterVertically) {
        if (percent != 0) {
            AppIcon(if (up) AppIcons.TrendUp else AppIcons.TrendDown, contentDescription = null, tint = color, size = 16.dp)
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(
            text = changeText(change, percent, label),
            style = MaterialTheme.typography.bodySmall.tabular(),
            color = color,
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: DrawableResource? = null,
) {
    val extended = SpendWiseTheme.extended
    val shape = MaterialTheme.shapes.small
    val active = enabled && !loading
    // Light theme: gradient fill with a soft glow, drawn behind a transparent button.
    val gradient = extended.vivid && active
    Button(
        onClick = onClick,
        enabled = active,
        modifier = modifier
            .height(52.dp)
            .then(if (gradient) Modifier.ambientShadow(shape, extended.primaryGlow, radius = 18.dp, offsetY = 6.dp).background(extended.primaryBrush, shape) else Modifier),
        shape = shape,
        colors = if (gradient) {
            ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onPrimary)
        } else {
            ButtonDefaults.buttonColors()
        },
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            if (icon != null) {
                AppIcon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, size = 20.dp)
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: DrawableResource? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) {
            AppIcon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun EmptyState(
    icon: DrawableResource,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            AppIcon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.sm))
            action()
        }
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(SpendWiseTheme.extended.hairline))
}

/** "12% more than …", or "18× …" once the jump is large enough that a percentage reads poorly. */
internal fun changeText(change: Double, percent: Int, label: String): String = when {
    percent == 0 -> "Same as $label"
    change >= 2.0 -> {
        val tenths = ((1 + change) * 10).roundToInt()
        val times = if (tenths % 10 == 0 || tenths >= 100) "${(tenths + 5) / 10}" else "${tenths / 10}.${tenths % 10}"
        "$times× $label"
    }
    change > 0 -> "$percent% more than $label"
    else -> "$percent% less than $label"
}
