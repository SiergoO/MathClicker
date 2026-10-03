package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

class RealFrameStepMsTest {
    @Test
    fun `an ordinary frame gap counts in full`() {
        assertEquals(16, realFrameStepMs(16))
    }

    @Test
    fun `a gap at the bound still counts`() {
        assertEquals(250, realFrameStepMs(250))
    }

    @Test
    fun `a gap beyond the bound counts for nothing`() {
        assertEquals(0, realFrameStepMs(251))
        assertEquals(0, realFrameStepMs(60_000))
    }
}
