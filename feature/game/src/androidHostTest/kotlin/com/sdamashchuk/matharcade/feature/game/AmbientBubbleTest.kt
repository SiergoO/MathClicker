package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientBubbleTest {
    @Test
    fun `a bubble's lane and size are the same every time it is asked`() {
        // The whole point of the hash is that no remembered state is needed to keep a bubble
        // stable across frames; if this ever stops holding, the field starts flickering.
        assertEquals(ambientBubbleRadiusFraction(7), ambientBubbleRadiusFraction(7), 0f)
        assertEquals(
            ambientBubbleCenterXFraction(7, gameTimeMs = 1_234L),
            ambientBubbleCenterXFraction(7, gameTimeMs = 1_234L),
            0f,
        )
    }

    @Test
    fun `different bubbles do not share a lane`() {
        val lanes = (0 until AMBIENT_BUBBLE_COUNT).map { ambientBubbleCenterXFraction(it, gameTimeMs = 0L) }
        assertEquals(lanes.size, lanes.distinct().size)
    }

    @Test
    fun `a bubble rises and wraps rather than stopping at the top`() {
        val early = ambientBubbleProgress(3, gameTimeMs = 0L)
        val later = ambientBubbleProgress(3, gameTimeMs = 1_000L)
        assertNotEquals(early, later)
        (0..40).forEach { step ->
            val progress = ambientBubbleProgress(3, gameTimeMs = step * 900L)
            assertTrue("progress $progress out of range", progress in 0f..1f)
        }
    }

    @Test
    fun `a bubble is invisible at both ends of its run`() {
        assertEquals(0f, ambientBubbleAlpha(5, progress = 0f), 0f)
        assertEquals(0f, ambientBubbleAlpha(5, progress = 1f), 0f)
        assertTrue(ambientBubbleAlpha(5, progress = 0.5f) > 0f)
    }

    @Test
    fun `every bubble stays inside the field once it has swayed`() {
        (0 until AMBIENT_BUBBLE_COUNT).forEach { index ->
            (0..30).forEach { step ->
                val x = ambientBubbleCenterXFraction(index, gameTimeMs = step * 370L)
                assertTrue("bubble $index drifted to $x", x in 0f..1f)
            }
        }
    }
}
