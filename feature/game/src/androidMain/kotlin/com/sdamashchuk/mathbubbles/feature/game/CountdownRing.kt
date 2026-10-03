package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private const val RING_WIDTH_FRACTION = 0.06f
private const val RING_START_ANGLE = -90f
private const val RING_TRACK_ALPHA = 0.25f

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
        val inset = strokeWidth / 2f
        val topLeft = Offset(inset, inset)
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val ringAlpha = color.alpha * alpha()
        drawArc(
            color = color.copy(alpha = ringAlpha * RING_TRACK_ALPHA),
            startAngle = RING_START_ANGLE,
            sweepAngle = countdownSweepDegrees(1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
        drawArc(
            color = color.copy(alpha = ringAlpha),
            startAngle = RING_START_ANGLE,
            sweepAngle = countdownSweepDegrees(fraction()),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}
