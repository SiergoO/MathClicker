package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeHintTimingTest {
    @Test
    fun `no hint plays before the bubble has been reachable for 1 second`() {
        assertFalse(ShakeHintTiming.isShaking(reachableSinceMs = 0L, nowMs = 999L))
    }

    @Test
    fun `a hint starts the instant the bubble has been reachable for 1 second`() {
        assertTrue(ShakeHintTiming.isShaking(reachableSinceMs = 0L, nowMs = 1_000L))
    }

    @Test
    fun `the hint ends after its own short duration and goes quiet until the next interval`() {
        assertFalse(ShakeHintTiming.isShaking(reachableSinceMs = 0L, nowMs = 1_000L + ShakeHintTiming.HINT_DURATION_MS))
        assertFalse(
            ShakeHintTiming.isShaking(
                reachableSinceMs = 0L,
                nowMs =
                    1_000L + ShakeHintTiming.HINT_INTERVAL_MS - 1L,
            ),
        )
    }

    @Test
    fun `the hint repeats at the calm interval`() {
        val secondHintStart = 1_000L + ShakeHintTiming.HINT_INTERVAL_MS
        assertTrue(ShakeHintTiming.isShaking(reachableSinceMs = 0L, nowMs = secondHintStart))
    }

    @Test
    fun `offsetAt is zero while not shaking`() {
        assertEquals(
            Offset.Zero,
            ShakeHintTiming.offsetAt(reachableSinceMs = 0L, nowMs = 500L, seed = 1, amplitudePx = 12f),
        )
    }

    @Test
    fun `offsetAt never exceeds the given amplitude while shaking`() {
        val amplitude = 12f
        for (offsetMs in 0 until ShakeHintTiming.HINT_DURATION_MS step 10L) {
            val offset =
                ShakeHintTiming.offsetAt(
                    reachableSinceMs = 0L,
                    nowMs = 1_000L + offsetMs,
                    seed = 7,
                    amplitudePx = amplitude,
                )
            assertTrue(kotlin.math.hypot(offset.x, offset.y) <= amplitude + 0.0001f)
        }
    }

    @Test
    fun `offsetAt is deterministic for the same reachable time seed and clock`() {
        val first = ShakeHintTiming.offsetAt(reachableSinceMs = 0L, nowMs = 1_050L, seed = 3, amplitudePx = 10f)
        val second = ShakeHintTiming.offsetAt(reachableSinceMs = 0L, nowMs = 1_050L, seed = 3, amplitudePx = 10f)
        assertEquals(first, second)
    }

    @Test
    fun `offsetAt uses a different shake for a later hint index`() {
        val firstHint =
            ShakeHintTiming.offsetAt(reachableSinceMs = 0L, nowMs = 1_050L, seed = 3, amplitudePx = 10f)
        val secondHint =
            ShakeHintTiming.offsetAt(
                reachableSinceMs = 0L,
                nowMs = 1_000L + ShakeHintTiming.HINT_INTERVAL_MS + 50L,
                seed = 3,
                amplitudePx = 10f,
            )
        org.junit.Assert.assertNotEquals(firstHint, secondHint)
    }
}
