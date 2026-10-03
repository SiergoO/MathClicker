package com.sdamashchuk.mathbubbles.core.ui.component

import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val Style =
    AmbientBubbleStyle(
        count = 14,
        slowestRiseMs = 14_000f,
        fastestRiseMs = 6_000f,
        smallestRadiusFraction = 0.0035f,
        largestRadiusFraction = 0.013f,
        minAlpha = 0.05f,
        maxAlpha = 0.20f,
    )

class AmbientBubbleMotionTest {
    @Test
    fun `bubble sizes stay inside the style range and differ between bubbles`() {
        val radii = (0 until Style.count).map { ambientBubbleRadiusFraction(Style, it) }
        radii.forEach {
            assertTrue("radius $it out of range", it in Style.smallestRadiusFraction..Style.largestRadiusFraction)
        }
        assertTrue(radii.distinct().size > Style.count / 2)
    }

    @Test
    fun `different bubbles do not share a lane`() {
        val lanes = (0 until Style.count).map { ambientBubbleCenterXFraction(it, timeMs = 0L) }
        assertEquals(lanes.size, lanes.distinct().size)
    }

    @Test
    fun `a bubble rises and wraps rather than stopping at the top`() {
        val early = ambientBubbleProgress(Style, 3, timeMs = 0L)
        val later = ambientBubbleProgress(Style, 3, timeMs = 1_000L)
        assertNotEquals(early, later)
        (0..40).forEach { step ->
            val progress = ambientBubbleProgress(Style, 3, timeMs = step * 900L)
            assertTrue("progress $progress out of range", progress in 0f..1f)
        }
    }

    @Test
    fun `a bubble is invisible at both ends of its run`() {
        assertEquals(0f, ambientBubbleAlpha(Style, 5, progress = 0f), 0f)
        assertEquals(0f, ambientBubbleAlpha(Style, 5, progress = 1f), 0f)
        assertTrue(ambientBubbleAlpha(Style, 5, progress = 0.5f) > 0f)
    }

    @Test
    fun `every bubble stays inside the field once it has swayed`() {
        (0 until Style.count).forEach { index ->
            (0..30).forEach { step ->
                val x = ambientBubbleCenterXFraction(index, timeMs = step * 370L)
                assertTrue("bubble $index drifted to $x", x in 0f..1f)
            }
        }
    }

    @Test
    fun `every bubble's radius sits inside the style's size range`() {
        (0 until Style.count).forEach { index ->
            val radius = ambientBubbleRadiusFraction(Style, index)
            assertTrue(
                "bubble $index radius $radius",
                radius in Style.smallestRadiusFraction..Style.largestRadiusFraction,
            )
        }
    }

    @Test
    fun `a slower style moves a bubble less far in the same time`() {
        val slow = Style.copy(slowestRiseMs = 40_000f, fastestRiseMs = 30_000f)
        val fastMove =
            (
                ambientBubbleProgress(
                    Style,
                    2,
                    timeMs = 2_000L,
                ) - ambientBubbleProgress(Style, 2, timeMs = 0L)
            ).mod(1f)
        val slowMove =
            (
                ambientBubbleProgress(
                    slow,
                    2,
                    timeMs = 2_000L,
                ) - ambientBubbleProgress(slow, 2, timeMs = 0L)
            ).mod(1f)
        assertTrue("fast $fastMove slow $slowMove", fastMove > slowMove && slowMove > 0f)
    }
}
