package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

private const val NANOS_PER_MILLI = 1_000_000L

/**
 * A millisecond clock that advances once per frame while [running] and holds its value otherwise.
 * Read the returned provider in the draw phase to avoid recomposing every frame.
 */
@Composable
fun rememberFrameClock(running: Boolean): () -> Long {
    val timeMs = remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val baseMs = timeMs.longValue
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { timeMs.longValue = baseMs + (it - startNanos) / NANOS_PER_MILLI }
        }
    }
    return remember { { timeMs.longValue } }
}
