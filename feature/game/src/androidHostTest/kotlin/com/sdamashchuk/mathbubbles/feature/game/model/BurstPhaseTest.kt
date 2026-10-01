package com.sdamashchuk.mathbubbles.feature.game.model

import kotlin.test.Test
import kotlin.test.assertEquals

class BurstPhaseTest {
    @Test
    fun `an ice pick kill starts at the shatter phase, before the burst`() {
        assertEquals(BurstPhase.Shatter, initialBurstPhase(viaIcePick = true))
    }

    @Test
    fun `a plain zeroing starts straight at the burst phase, with no shatter to wait on`() {
        assertEquals(BurstPhase.Burst, initialBurstPhase(viaIcePick = false))
    }
}
