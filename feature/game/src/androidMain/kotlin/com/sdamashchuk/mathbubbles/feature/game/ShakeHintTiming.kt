package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp

internal object ShakeHintTiming {
    internal const val FIRST_HINT_DELAY_MS = 1_000L
    internal const val HINT_INTERVAL_MS = 4_000L
    internal const val HINT_DURATION_MS = 300L

    fun isShaking(
        reachableSinceMs: Long,
        nowMs: Long,
    ): Boolean {
        val elapsed = nowMs - reachableSinceMs
        if (elapsed < FIRST_HINT_DELAY_MS) return false
        return (elapsed - FIRST_HINT_DELAY_MS) % HINT_INTERVAL_MS < HINT_DURATION_MS
    }

    fun offsetAt(
        reachableSinceMs: Long,
        nowMs: Long,
        seed: Int,
        amplitudePx: Float,
    ): Offset {
        if (!isShaking(reachableSinceMs, nowMs)) return Offset.Zero
        val elapsedSinceFirstHint = nowMs - reachableSinceMs - FIRST_HINT_DELAY_MS
        val hintIndex = elapsedSinceFirstHint / HINT_INTERVAL_MS
        val positionInHint = elapsedSinceFirstHint % HINT_INTERVAL_MS
        val keyframes = ShakeMotion.keyframes(seed + hintIndex.toInt(), amplitudePx)
        val progress = positionInHint.toFloat() / HINT_DURATION_MS * keyframes.size
        val segmentIndex = progress.toInt().coerceAtMost(keyframes.lastIndex)
        val from = if (segmentIndex == 0) Offset.Zero else keyframes[segmentIndex - 1]
        return lerp(from, keyframes[segmentIndex], (progress - segmentIndex).coerceIn(0f, 1f))
    }
}
