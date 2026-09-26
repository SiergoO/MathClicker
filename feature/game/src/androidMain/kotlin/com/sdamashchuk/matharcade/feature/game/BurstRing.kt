package com.sdamashchuk.matharcade.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sdamashchuk.matharcade.core.ui.theme.Red500
import com.sdamashchuk.matharcade.feature.game.model.TargetScreenPosition

// MC-83: pulled well back from MC-82's 96dp / 4dp / 0.6 alpha. At that weight a zeroing threw a
// saturated red hoop across two neighbouring lanes, which read as a second game object rather
// than as the target's own departure.
private const val BURST_DIAMETER_DP = 72
private const val BURST_DURATION_MS = 260
private const val BURST_STROKE_WIDTH_DP = 2
private const val BURST_MAX_ALPHA = 0.28f

// The zeroed target's own place on screen, not a generic one: position is a snapshot TargetButton
// last reported (see TargetScreenPosition), taken because the target itself is already gone from
// the composition by the time this fires.
@Composable
fun BurstRing(
    position: TargetScreenPosition,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(BURST_DURATION_MS, easing = LinearOutSlowInEasing))
        onFinished()
    }
    Canvas(
        modifier =
            Modifier
                .size(BURST_DIAMETER_DP.dp)
                .offset {
                    val halfDiameterPx = BURST_DIAMETER_DP.dp.roundToPx() / 2
                    IntOffset(
                        x = position.xDp.dp.roundToPx() - halfDiameterPx,
                        y = position.yDp.dp.roundToPx() - halfDiameterPx,
                    )
                },
    ) {
        drawCircle(
            color = Red500.copy(alpha = burstAlpha(progress.value)),
            radius = (size.minDimension / 2f) * progress.value,
            style = Stroke(width = BURST_STROKE_WIDTH_DP.dp.toPx()),
        )
    }
}

// Fades out as the ring expands, not a fixed alpha, so a burst reads as dissipating rather than
// as a static ring that simply vanishes at the end of its tween.
internal fun burstAlpha(progress: Float): Float = (1f - progress) * BURST_MAX_ALPHA
