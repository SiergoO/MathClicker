package com.sdamashchuk.mathbubbles.core.game

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilTest {
    @Test
    fun `values already inside 0 to 3 are returned unchanged`() {
        assertEquals(0, 0.toGameColumnId())
        assertEquals(1, 1.toGameColumnId())
        assertEquals(2, 2.toGameColumnId())
        assertEquals(3, 3.toGameColumnId())
    }

    @Test
    fun `values outside 0 to 3 wrap with an off-by-one shift - not a plain modulo`() {
        // Characterization: (this + 1) % 4 maps 4,5,6,7 to 1,2,3,0 rather than the 0,1,2,3 a
        // plain `this % 4` would give, and -1 maps to 0 rather than 3. Pinning current
        // behaviour, not asserting it is the intended design.
        assertEquals(1, 4.toGameColumnId())
        assertEquals(2, 5.toGameColumnId())
        assertEquals(3, 6.toGameColumnId())
        assertEquals(0, 7.toGameColumnId())
        assertEquals(0, (-1).toGameColumnId())
    }
}
