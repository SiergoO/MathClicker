package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FLOAT_DELTA = 0.0001f

class WaterDepthFactorTest {
    @Test
    fun `level 1 leaves the water at full brightness`() {
        assertEquals(1f, waterDepthFactor(1), FLOAT_DELTA)
    }

    @Test
    fun `the water darkens by a fixed step per level`() {
        assertEquals(0.838f, waterDepthFactor(10), FLOAT_DELTA)
        assertEquals(0.658f, waterDepthFactor(20), FLOAT_DELTA)
    }

    // The floor is what keeps an idle bubble separable from the water; without it the board goes
    // unreadable somewhere past level 40 rather than merely dark.
    @Test
    fun `darkening stops at the floor instead of running to black`() {
        assertEquals(0.45f, waterDepthFactor(40), FLOAT_DELTA)
        assertEquals(0.45f, waterDepthFactor(400), FLOAT_DELTA)
    }

    @Test
    fun `the factor never increases with level`() {
        val factors = (1..60).map(::waterDepthFactor)
        assertTrue(factors.zipWithNext().all { (shallower, deeper) -> deeper <= shallower })
    }
}
