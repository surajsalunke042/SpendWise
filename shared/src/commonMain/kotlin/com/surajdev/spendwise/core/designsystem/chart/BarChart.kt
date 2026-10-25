package com.surajdev.spendwise.core.designsystem.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.surajdev.spendwise.core.designsystem.theme.SpendWiseTheme
import com.surajdev.spendwise.core.designsystem.theme.tabular
import androidx.compose.ui.geometry.RoundRect

/**
 * Slim bars with rounded tops on a single baseline, no grid. Tap a bar to show its value.
 *
 * @param labels x-axis labels; null entries are left blank (e.g. label every 6th hour).
 * @param highlightIndex drawn in the primary color; other bars use a muted tone.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String?>,
    valueLabel: (Int) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = null,
    barColor: Color = SpendWiseTheme.extended.chartMuted,
    highlightColors: List<Color> = SpendWiseTheme.extended.chartHighlightColors,
    height: Dp = 160.dp,
    maxBarWidth: Dp = 18.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.tabular().copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val valueStyle = MaterialTheme.typography.labelMedium.tabular().copy(color = MaterialTheme.colorScheme.onSurface)
    val baselineColor = SpendWiseTheme.extended.hairline
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(values) {
                detectTapGestures { offset ->
                    if (values.isEmpty()) return@detectTapGestures
                    val slot = size.width / values.size
                    val index = (offset.x / slot).toInt().coerceIn(0, values.lastIndex)
                    selected = if (selected == index || values[index] <= 0f) null else index
                }
            },
    ) {
        if (values.isEmpty()) return@Canvas
        val labelHeight = 18.dp.toPx()
        val valueHeight = 20.dp.toPx()
        val chartTop = valueHeight
        val chartBottom = size.height - labelHeight
        val chartHeight = chartBottom - chartTop
        val slot = size.width / values.size
        val barWidth = minOf(slot * 0.56f, maxBarWidth.toPx())
        val maxValue = values.maxOrNull()?.takeIf { it > 0f } ?: 1f

        drawLine(baselineColor, Offset(0f, chartBottom), Offset(size.width, chartBottom), strokeWidth = 1.dp.toPx())

        values.forEachIndexed { index, value ->
            val centerX = slot * index + slot / 2
            if (value > 0f) {
                val barHeight = (value / maxValue * chartHeight * progress.value).coerceAtLeast(2.dp.toPx())
                val highlighted = selected == index || (selected == null && highlightIndex == index)
                val top = chartBottom - barHeight
                val brush = if (highlighted) {
                    Brush.verticalGradient(highlightColors, startY = top, endY = chartBottom)
                } else {
                    SolidColor(barColor)
                }
                drawTopRoundedBar(
                    brush = brush,
                    topLeft = Offset(centerX - barWidth / 2, top),
                    size = Size(barWidth, barHeight),
                    radius = minOf(barWidth / 2, 6.dp.toPx()),
                )
            }
            labels.getOrNull(index)?.let { label ->
                val measured = textMeasurer.measure(label, labelStyle)
                drawText(
                    measured,
                    topLeft = Offset(
                        (centerX - measured.size.width / 2).coerceIn(0f, size.width - measured.size.width),
                        chartBottom + 4.dp.toPx(),
                    ),
                )
            }
        }

        selected?.let { index ->
            val text = textMeasurer.measure(valueLabel(index), valueStyle)
            val centerX = slot * index + slot / 2
            val barHeight = values[index] / maxValue * chartHeight
            drawText(
                text,
                topLeft = Offset(
                    (centerX - text.size.width / 2).coerceIn(0f, size.width - text.size.width),
                    (chartBottom - barHeight - text.size.height - 4.dp.toPx()).coerceAtLeast(0f),
                ),
            )
        }
    }
}

private fun DrawScope.drawTopRoundedBar(brush: Brush, topLeft: Offset, size: Size, radius: Float) {
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = topLeft.x,
                top = topLeft.y,
                right = topLeft.x + size.width,
                bottom = topLeft.y + size.height,
                topLeftCornerRadius = CornerRadius(radius),
                topRightCornerRadius = CornerRadius(radius),
                bottomLeftCornerRadius = CornerRadius.Zero,
                bottomRightCornerRadius = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, brush)
}
