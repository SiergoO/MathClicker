package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

private const val FLOAT_DELTA = 0.001f

class CountdownSweepTest {
    @Test
    fun `an empty countdown sweeps nothing and a full one sweeps the whole circle`() {
        assertEquals(0f, countdownSweepDegrees(0f), 0f)
        assertEquals(360f, countdownSweepDegrees(1f), 0f)
    }

    @Test
    fun `sixty percent remaining sweeps two hundred sixteen degrees`() {
        assertEquals(216f, countdownSweepDegrees(0.6f), FLOAT_DELTA)
    }

    @Test
    fun `a fraction outside zero to one stays within the circle`() {
        assertEquals(0f, countdownSweepDegrees(-0.2f), 0f)
        assertEquals(360f, countdownSweepDegrees(1.3f), 0f)
    }
}
