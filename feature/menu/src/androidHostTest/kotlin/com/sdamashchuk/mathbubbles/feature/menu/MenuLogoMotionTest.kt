package com.sdamashchuk.mathbubbles.feature.menu

import org.junit.Assert.assertEquals
import org.junit.Test

private const val TOLERANCE = 0.0001f

class MenuLogoMotionTest {
    @Test
    fun `breathing rests at full size at the start of a cycle`() {
        assertEquals(1f, MenuLogoMotion.breathScale(0L), TOLERANCE)
    }

    @Test
    fun `breathing peaks at three percent halfway through the cycle`() {
        assertEquals(1.03f, MenuLogoMotion.breathScale(MenuLogoMotion.BREATH_PERIOD_MS / 2), TOLERANCE)
    }

    @Test
    fun `breathing repeats every period`() {
        assertEquals(
            MenuLogoMotion.breathScale(1_234L),
            MenuLogoMotion.breathScale(1_234L + MenuLogoMotion.BREATH_PERIOD_MS),
            TOLERANCE,
        )
    }

    @Test
    fun `bob reaches full height up and down within one period`() {
        assertEquals(1f, MenuLogoMotion.bobFraction(MenuLogoMotion.BOB_PERIOD_MS / 4), TOLERANCE)
        assertEquals(-1f, MenuLogoMotion.bobFraction(MenuLogoMotion.BOB_PERIOD_MS * 3 / 4), TOLERANCE)
        assertEquals(0f, MenuLogoMotion.bobFraction(0L), TOLERANCE)
    }
}
