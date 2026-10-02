package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

private const val TOLERANCE = 0.0001f

class ShakeMotionTest {
    @Test
    fun `angle is deterministic from the seed`() {
        assertEquals(241f, ShakeMotion.angleDegrees(seed = 1), TOLERANCE)
        assertEquals(122f, ShakeMotion.angleDegrees(seed = 2), TOLERANCE)
    }

    @Test
    fun `direction is not confined to the X axis`() {
        val direction = ShakeMotion.direction(seed = 1)
        assertNotEquals(0f, direction.y)
    }

    @Test
    fun `different seeds shake in different directions`() {
        assertNotEquals(ShakeMotion.angleDegrees(seed = 1), ShakeMotion.angleDegrees(seed = 2))
    }

    @Test
    fun `offset magnitude never exceeds the given amplitude`() {
        val amplitude = 12f
        val offset = ShakeMotion.offset(seed = 5, amplitudePx = amplitude)
        val magnitude = hypot(offset.x, offset.y)
        assertTrue(magnitude <= amplitude + TOLERANCE)
    }

    @Test
    fun `keyframes carry a non-zero Y component and stay within amplitude`() {
        val amplitude = 12f
        val keyframes = ShakeMotion.keyframes(seed = 1, amplitudePx = amplitude)
        assertTrue(keyframes.any { it.y != 0f })
        keyframes.forEach { assertTrue(hypot(it.x, it.y) <= amplitude + TOLERANCE) }
    }

    @Test
    fun `keyframes rattle out and back to zero`() {
        val peak = ShakeMotion.offset(seed = 3, amplitudePx = 12f)
        assertEquals(listOf(-peak, peak, Offset.Zero), ShakeMotion.keyframes(3, 12f))
    }
}
