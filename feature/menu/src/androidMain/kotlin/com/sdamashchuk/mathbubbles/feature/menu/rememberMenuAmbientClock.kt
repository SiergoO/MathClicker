package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.sdamashchuk.mathbubbles.core.ui.component.rememberFrameClock

@Composable
internal fun rememberMenuAmbientClock(lifecycle: Lifecycle): () -> Long {
    var resumed by remember(lifecycle) { mutableStateOf(lifecycle.state == Lifecycle.State.RESUMED) }
    DisposableEffect(lifecycle) {
        val callbacks =
            object : Lifecycle.Callbacks {
                override fun onResume() {
                    resumed = true
                }

                override fun onPause() {
                    resumed = false
                }
            }
        lifecycle.subscribe(callbacks)
        onDispose { lifecycle.unsubscribe(callbacks) }
    }
    return rememberFrameClock(running = resumed)
}
