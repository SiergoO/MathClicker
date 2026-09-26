package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FLOAT_DELTA = 0.001f

class TelegraphPulseTest {
    @Test
    fun `a target that is not telegraphing is never scaled`() {
        assertEquals(1f, telegraphPulse(gameTimeMs = 0L, isTelegraphing = false), 0f)
        assertEquals(1f, telegraphPulse(gameTimeMs = 12_345L, isTelegraphing = false), 0f)
    }

    @Test
    fun `the pulse starts at rest and peaks half a period in`() {
        assertEquals(1f, telegraphPulse(gameTimeMs = 0L, isTelegraphing = true), FLOAT_DELTA)
        assertEquals(1.15f, telegraphPulse(gameTimeMs = 300L, isTelegraphing = true), FLOAT_DELTA)
        assertEquals(1f, telegraphPulse(gameTimeMs = 600L, isTelegraphing = true), FLOAT_DELTA)
    }

    // The old InfiniteTransition carried its own phase, so it could be anywhere when a target
    // started telegraphing. This is a function of the shared clock instead, which means it repeats
    // exactly - and that is what lets it replace an animation object per target.
    @Test
    fun `the pulse repeats exactly one period later`() {
        (0..600 step 50).forEach { offset ->
            assertEquals(
                telegraphPulse(offset.toLong(), isTelegraphing = true),
                telegraphPulse(offset + 600L, isTelegraphing = true),
                FLOAT_DELTA,
            )
        }
    }

    @Test
    fun `the pulse never leaves its declared range`() {
        (0..2_000 step 7).forEach { time ->
            val pulse = telegraphPulse(time.toLong(), isTelegraphing = true)
            assertTrue("pulse $pulse at $time", pulse in 1f..1.15f + FLOAT_DELTA)
        }
    }
}
