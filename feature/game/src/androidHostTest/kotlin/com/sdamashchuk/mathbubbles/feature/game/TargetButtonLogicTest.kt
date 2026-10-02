package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleFillReady
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimIdle
import com.sdamashchuk.mathbubbles.core.ui.theme.BubbleRimReady
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The old readiness blend's not-yet-ready floor (liveliness 0.62), now the one fixed look every
// profitable bubble holds instead of animating toward BubbleFillReady/BubbleRimReady.
private const val STANDARD_LIVELINESS = 0.62f

class TargetButtonLogicTest {
    @Test
    fun `shouldSquashTarget is true when value decreases`() {
        assertTrue(shouldSquashTarget(newValue = 4, previousValue = 5))
    }

    @Test
    fun `shouldSquashTarget is false for a no-op tap`() {
        assertFalse(shouldSquashTarget(newValue = 5, previousValue = 5))
    }

    @Test
    fun `shouldSquashTarget is false when value increases`() {
        assertFalse(shouldSquashTarget(newValue = 6, previousValue = 5))
    }

    @Test
    fun `a profitable target holds the old not-yet-ready floor look - not plain idle`() {
        val style = targetBubbleStyle(diameter = 48.dp, isProfitable = true, horizontalFraction = 0.5f)
        assertEquals(lerp(BubbleFillIdle, BubbleFillReady, STANDARD_LIVELINESS), style.fillColor)
        assertEquals(lerp(BubbleRimIdle, BubbleRimReady, STANDARD_LIVELINESS), style.rimColor)
        assertFalse(style.fillColor == BubbleFillIdle)
    }

    @Test
    fun `an unprofitable target keeps its own dimmer unreachable look unchanged`() {
        val unprofitable = targetBubbleStyle(diameter = 48.dp, isProfitable = false, horizontalFraction = 0.5f)
        assertEquals(BubbleFillIdle, unprofitable.fillColor)
        assertEquals(BubbleRimIdle, unprofitable.rimColor)
    }

    @Test
    fun `the unprofitable look stays clearly dimmer than the profitable standard look`() {
        val profitable = targetBubbleStyle(diameter = 48.dp, isProfitable = true, horizontalFraction = 0.5f)
        val unprofitable = targetBubbleStyle(diameter = 48.dp, isProfitable = false, horizontalFraction = 0.5f)
        assertTrue(unprofitable.fillAlpha < profitable.fillAlpha)
        assertTrue(unprofitable.highlight!!.coreAlpha < profitable.highlight!!.coreAlpha)
    }
}
