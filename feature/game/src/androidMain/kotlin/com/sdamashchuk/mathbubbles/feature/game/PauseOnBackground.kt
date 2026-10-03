package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle

@Composable
internal fun PauseOnBackground(
    phase: GamePhase,
    onPause: () -> Unit,
) {
    val currentPhase = rememberUpdatedState(phase)
    OnLifecycleEvent { _, event ->
        val backgrounded = event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP
        if (backgrounded && currentPhase.value == GamePhase.Playing) onPause()
    }
}
