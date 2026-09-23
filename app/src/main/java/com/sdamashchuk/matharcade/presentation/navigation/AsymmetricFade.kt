package com.sdamashchuk.matharcade.presentation.navigation

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.isFinished
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.arkivanov.decompose.extensions.compose.stack.animation.Direction
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimator
import kotlin.math.abs

/**
 * Decompose's own `fade()` animates enter and exit with the same [FiniteAnimationSpec][androidx.compose.animation.core.FiniteAnimationSpec].
 * MathClicker's screens fade in and out over different durations (e.g. Menu: 500ms in, 200ms out),
 * so this picks the duration by [Direction] instead.
 */
fun asymmetricFade(
    enterDurationMillis: Int,
    exitDurationMillis: Int,
): StackAnimator =
    StackAnimator { direction, isInitial, onFinished, content ->
        val durationMillis =
            when (direction) {
                Direction.ENTER_FRONT, Direction.ENTER_BACK -> enterDurationMillis
                Direction.EXIT_FRONT, Direction.EXIT_BACK -> exitDurationMillis
            }
        val onFinishedRef by rememberUpdatedState(onFinished)
        val animationState = remember(direction, isInitial) { AnimationState(initialValue = if (isInitial) 0F else 1F) }

        LaunchedEffect(animationState, durationMillis) {
            animationState.animateTo(
                targetValue = 0F,
                animationSpec = tween(durationMillis),
                sequentialAnimation = !animationState.isFinished,
            )
            onFinishedRef()
        }

        val factor =
            when (direction) {
                Direction.ENTER_FRONT -> animationState.value
                Direction.EXIT_FRONT -> 1F - animationState.value
                Direction.ENTER_BACK -> -animationState.value
                Direction.EXIT_BACK -> animationState.value - 1F
            }

        content(Modifier.alpha((1F - abs(factor)).coerceIn(0F, 1F)))
    }
