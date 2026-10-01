package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.ui.theme.WaterDeep
import kotlin.math.cos
import kotlin.math.sin

private const val CRACK_LINE_COUNT = 3
private const val CRACK_STROKE_WIDTH_FRACTION = 0.1f
private const val CRACK_GROWTH_END_FRACTION = 0.5f
internal const val SHIELD_CRACK_TEST_TAG = "shieldCrack"

@Composable
fun ShieldCrack(
    diameter: Dp,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(CUE_MS, easing = LinearEasing))
        onFinished()
    }
    Box(modifier = modifier.size(diameter).testTag(SHIELD_CRACK_TEST_TAG)) {
        BoosterToken(
            booster = Booster.SHIELD,
            diameter = diameter,
            modifier = Modifier.graphicsLayer { alpha = crackFadeAlpha(progress.value) },
        )
        Canvas(modifier = Modifier.size(diameter)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            val growth = crackGrowth(progress.value)
            // WaterDeep, not Ink: the shield glyph itself is near-white.
            val color = WaterDeep.copy(alpha = crackFadeAlpha(progress.value))
            repeat(CRACK_LINE_COUNT) { index ->
                val angle = shardAngle(index, CRACK_LINE_COUNT)
                drawLine(
                    color = color,
                    start = center,
                    end = center + Offset(cos(angle), sin(angle)) * (maxRadius * growth),
                    strokeWidth = size.minDimension * CRACK_STROKE_WIDTH_FRACTION,
                )
            }
        }
    }
}

private fun crackGrowth(progress: Float): Float = (progress / CRACK_GROWTH_END_FRACTION).coerceIn(0f, 1f)

private fun crackFadeAlpha(progress: Float): Float =
    1f - ((progress - CRACK_GROWTH_END_FRACTION) / (1f - CRACK_GROWTH_END_FRACTION)).coerceIn(0f, 1f)
