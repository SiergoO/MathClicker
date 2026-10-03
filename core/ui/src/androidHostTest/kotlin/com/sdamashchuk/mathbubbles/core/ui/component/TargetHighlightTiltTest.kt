package com.sdamashchuk.mathbubbles.core.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

private const val TOLERANCE = 0.0001f

class TargetHighlightTiltTest {
    @Test
    fun `horizontal fraction mirrors the leftmost and rightmost column`() {
        val left = TargetHighlightTilt.horizontalFraction(columnId = 0, columnCount = 4)
        val right = TargetHighlightTilt.horizontalFraction(columnId = 3, columnCount = 4)
        assertEquals(-0.75f, left, TOLERANCE)
        assertEquals(0.75f, right, TOLERANCE)
    }

    @Test
    fun `horizontal fraction mirrors the two inner columns`() {
        val innerLeft = TargetHighlightTilt.horizontalFraction(columnId = 1, columnCount = 4)
        val innerRight = TargetHighlightTilt.horizontalFraction(columnId = 2, columnCount = 4)
        assertEquals(-0.25f, innerLeft, TOLERANCE)
        assertEquals(0.25f, innerRight, TOLERANCE)
    }

    @Test
    fun `base highlight offset leans opposite ways for mirrored columns`() {
        val leftOffset = TargetHighlightTilt.baseOffsetFraction(horizontalFraction = -0.75f)
        val rightOffset = TargetHighlightTilt.baseOffsetFraction(horizontalFraction = 0.75f)
        assertEquals(-0.505f, leftOffset.x, TOLERANCE)
        assertEquals(-0.175f, rightOffset.x, TOLERANCE)
        assertEquals(leftOffset.y, rightOffset.y, TOLERANCE)
    }

    @Test
    fun `fall offset delta sinks deeper as the bubble falls`() {
        val top = TargetHighlightTilt.fallOffsetDelta(fallFraction = 0f)
        val bottom = TargetHighlightTilt.fallOffsetDelta(fallFraction = 1f)
        assertEquals(0f, top.y, TOLERANCE)
        assertEquals(0.05f, bottom.y, TOLERANCE)
        assertEquals(0f, top.x, TOLERANCE)
        assertEquals(0f, bottom.x, TOLERANCE)
    }

    @Test
    fun `highlight angle mirrors to opposite sides`() {
        val left = TargetHighlightTilt.angleDegrees(horizontalFraction = -0.75f)
        val right = TargetHighlightTilt.angleDegrees(horizontalFraction = 0.75f)
        assertEquals(-13.5f, left, TOLERANCE)
        assertEquals(13.5f, right, TOLERANCE)
        assertEquals(0f, left + right, TOLERANCE)
    }
}
