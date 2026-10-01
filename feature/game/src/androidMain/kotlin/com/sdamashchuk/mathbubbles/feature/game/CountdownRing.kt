package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private const val RING_WIDTH_FRACTION = 0.09f
private const val RING_START_ANGLE = -90f
private const val RING_FULL_SWEEP = 360f

// fraction and alpha are both read only in the draw phase: they change every frame.
@Composable
fun CountdownRing(
    color: Color,
    fraction: () -> Float,
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = size.minDimension * RING_WIDTH_FRACTION
        drawArc(
            color = color.copy(alpha = color.alpha * alpha()),
            startAngle = RING_START_ANGLE,
            sweepAngle = RING_FULL_SWEEP * fraction(),
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
