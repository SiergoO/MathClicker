package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

class BurstRingLogicTest {
    @Test
    fun `burstAlpha is at its brightest at the start of the burst`() {
        assertEquals(0.6f, burstAlpha(progress = 0f), 0f)
    }

    @Test
    fun `burstAlpha fades to zero by the end of the burst`() {
        assertEquals(0f, burstAlpha(progress = 1f), 0f)
    }

    @Test
    fun `burstAlpha is halfway faded at the midpoint`() {
        assertEquals(0.3f, burstAlpha(progress = 0.5f), 0.0001f)
    }
}
