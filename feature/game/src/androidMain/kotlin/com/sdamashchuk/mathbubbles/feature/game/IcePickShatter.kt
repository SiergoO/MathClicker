package com.sdamashchuk.mathbubbles.feature.game

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.Ink
import com.sdamashchuk.mathbubbles.feature.game.model.TargetScreenPosition
import kotlin.math.cos
import kotlin.math.sin

private const val SHATTER_DIAMETER_DP = 72
private const val SHATTER_STROKE_WIDTH_DP = 2
private const val SHATTER_SHARD_COUNT = 6
private const val SHATTER_SHARD_LENGTH_FRACTION = 0.3f

// The ice pick's own kill cue, played before the normal BurstRing: shards fly outward from the
// bubble's centre and fade, reading as shattered rather than merely zeroed.
@Composable
fun IcePickShatter(
    position: TargetScreenPosition,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(CUE_MS, easing = LinearOutSlowInEasing))
        onFinished()
    }
    Canvas(
        modifier =
            Modifier
                .size(SHATTER_DIAMETER_DP.dp)
                .offset {
                    val halfDiameterPx = SHATTER_DIAMETER_DP.dp.roundToPx() / 2
                    IntOffset(
                        x = position.xDp.dp.roundToPx() - halfDiameterPx,
                        y = position.yDp.dp.roundToPx() - halfDiameterPx,
                    )
                },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2f
        val shardLength = maxRadius * SHATTER_SHARD_LENGTH_FRACTION
        val color = Ink.copy(alpha = shatterAlpha(progress.value))
        repeat(SHATTER_SHARD_COUNT) { index ->
            val angle = shardAngle(index, SHATTER_SHARD_COUNT)
            val innerRadius = maxRadius * progress.value
            val outerRadius = innerRadius + shardLength
            drawLine(
                color = color,
                start = center + Offset(cos(angle) * innerRadius, sin(angle) * innerRadius),
                end = center + Offset(cos(angle) * outerRadius, sin(angle) * outerRadius),
                strokeWidth = SHATTER_STROKE_WIDTH_DP.dp.toPx(),
            )
        }
    }
}

private fun shatterAlpha(progress: Float): Float = 1f - progress
