package com.surajdev.spendwise.core.designsystem.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutSlice(val value: Float, val color: Color)

/**
 * Thin donut with rounded ends and small gaps between slices; [center] is drawn in the hole.
 * Slices animate in on first display and whenever the data changes.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 14.dp,
    gapDegrees: Float = 3f,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))

            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas

            val visible = slices.filter { it.value > 0f }
            val gap = if (visible.size > 1) gapDegrees else 0f
            // Rounded caps extend past the arc ends; compensate so gaps stay visible.
            val capDegrees = if (visible.size > 1) (stroke / 2) / (diameter / 2) * (180f / kotlin.math.PI.toFloat()) else 0f
            var start = -90f
            val sweepBudget = 360f * progress.value
            visible.forEach { slice ->
                val fullSweep = slice.value / total * 360f
                val drawSweep = (fullSweep - gap - capDegrees * 2).coerceAtLeast(0.5f)
                val clipped = (sweepBudget - (start + 90f)).coerceIn(0f, drawSweep)
                if (clipped > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = start + gap / 2 + capDegrees,
                        sweepAngle = clipped,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = if (visible.size > 1) StrokeCap.Round else StrokeCap.Butt),
                    )
                }
                start += fullSweep
            }
        }
        center()
    }
}
