package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

class DisappearFadeTest {
    @Test
    fun `alpha is opaque at the start of the fade`() {
        assertEquals(1f, DisappearFade.alpha(progress = 0f), 0f)
    }

    @Test
    fun `alpha is transparent at the end of the fade`() {
        assertEquals(0f, DisappearFade.alpha(progress = 1f), 0f)
    }

    @Test
    fun `scale is full size at the start of the fade`() {
        assertEquals(1f, DisappearFade.scale(progress = 0f), 0f)
    }

    @Test
    fun `scale shrinks to the end scale by the end of the fade`() {
        assertEquals(0.85f, DisappearFade.scale(progress = 1f), 0.0001f)
    }
}
