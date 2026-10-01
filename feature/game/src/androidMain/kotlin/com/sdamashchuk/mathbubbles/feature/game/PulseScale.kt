package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

private const val PULSE_SCALE = 1.15f
private const val PULSE_MS = 600

// Returned as State, not Float: a caller reads .value inside graphicsLayer {} so the animation
// drives the draw phase only, instead of recomposing the whole control sixty times a second.
@Composable
fun rememberPulseScale(enabled: Boolean): State<Float> =
    if (enabled) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = PULSE_SCALE,
            animationSpec = infiniteRepeatable(tween(PULSE_MS), RepeatMode.Reverse),
            label = "pulseScale",
        )
    } else {
        remember { mutableStateOf(1f) }
    }
