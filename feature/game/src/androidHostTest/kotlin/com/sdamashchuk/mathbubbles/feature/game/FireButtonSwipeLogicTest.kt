package com.sdamashchuk.mathbubbles.feature.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val RESISTANCE_RANGE_PX = 120f
private const val THRESHOLD_PX = -48f
private const val FLING_THRESHOLD_PX_PER_S = -1200f

class FireButtonSwipeLogicTest {
    @Test
    fun `no drag applies no resistance offset`() {
        assertEquals(0f, applyDragResistance(0f, RESISTANCE_RANGE_PX))
    }

    @Test
    fun `resisted offset is always smaller in magnitude than the raw drag`() {
        val resisted = applyDragResistance(-400f, RESISTANCE_RANGE_PX)
        assertTrue(-resisted < 400f, "resisted offset $resisted was not smaller than the raw drag")
    }

    @Test
    fun `resisted offset never exceeds the resistance range`() {
        val resisted = applyDragResistance(-100_000f, RESISTANCE_RANGE_PX)
        assertTrue(-resisted < RESISTANCE_RANGE_PX, "resisted offset $resisted reached the resistance range")
    }

    @Test
    fun `a drag past the threshold stashes on release`() {
        assertTrue(shouldStashOnRelease(-60f, 0f, THRESHOLD_PX, FLING_THRESHOLD_PX_PER_S))
    }

    @Test
    fun `a fast fling stashes even under the distance threshold`() {
        assertTrue(shouldStashOnRelease(-10f, -1500f, THRESHOLD_PX, FLING_THRESHOLD_PX_PER_S))
    }

    @Test
    fun `a slow release under the threshold snaps back instead of stashing`() {
        assertFalse(shouldStashOnRelease(-10f, 0f, THRESHOLD_PX, FLING_THRESHOLD_PX_PER_S))
    }
}
