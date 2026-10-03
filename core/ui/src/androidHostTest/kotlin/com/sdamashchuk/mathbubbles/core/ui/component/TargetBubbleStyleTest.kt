package com.sdamashchuk.mathbubbles.core.ui.component

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

private const val STANDARD_LIVELINESS = 0.62f

class TargetBubbleStyleTest {
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
