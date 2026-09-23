package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetButtonLogicTest {
    @Test
    fun `remainingFallDurationMs scales lifetime by the share of the column not yet fallen`() {
        val target = target(lifetimeMs = 2000, position = 250)

        assertEquals(1500, remainingFallDurationMs(target, gameColumnHeightPx = 1000))
    }

    @Test
    fun `remainingFallDurationMs returns the full lifetime when the column has no height yet`() {
        val target = target(lifetimeMs = 2000, position = 250)

        assertEquals(2000, remainingFallDurationMs(target, gameColumnHeightPx = 0))
    }

    @Test
    fun `remainingFallDurationMs floors at zero when the saved position is past the column`() {
        val target = target(lifetimeMs = 2000, position = 1500)

        assertEquals(0, remainingFallDurationMs(target, gameColumnHeightPx = 1000))
    }

    // 2001 * 0.75 = 1500.75, which toInt() truncates to 1500 and roundToInt() would round to
    // 1501 — the two prior cases land on whole numbers, where the two agree.
    @Test
    fun `remainingFallDurationMs truncates the fractional result rather than rounding it`() {
        val target = target(lifetimeMs = 2001, position = 250)

        assertEquals(1500, remainingFallDurationMs(target, gameColumnHeightPx = 1000))
    }

    @Test
    fun `shouldReveal is true once the target has moved and was not already revealed`() {
        assertTrue(shouldReveal(targetButtonYOffset = 10f, isVisible = false, hasRevealedThisActivation = false))
    }

    @Test
    fun `shouldReveal is false before the target has moved`() {
        assertFalse(shouldReveal(targetButtonYOffset = 0f, isVisible = false, hasRevealedThisActivation = false))
    }

    @Test
    fun `shouldReveal is false once the target is already visible`() {
        assertFalse(shouldReveal(targetButtonYOffset = 10f, isVisible = true, hasRevealedThisActivation = false))
    }

    @Test
    fun `shouldReveal is false once this activation already revealed`() {
        assertFalse(shouldReveal(targetButtonYOffset = 10f, isVisible = false, hasRevealedThisActivation = true))
    }

    @Test
    fun `shouldBreakout is true once the offset reaches the bottom of the column`() {
        assertTrue(
            shouldBreakout(targetButtonYOffset = 99f, gameColumnHeightPx = 100, hasBrokenOutThisActivation = false),
        )
    }

    // 98.6.roundToInt() is 99, but 98.6.toInt() is 98 — the whole-number offsets above (99f, 98f)
    // don't distinguish rounding from truncation, and animateFloat hands this a fractional offset
    // on essentially every frame, not just at whole pixels.
    @Test
    fun `shouldBreakout rounds a fractional offset rather than truncating it`() {
        assertTrue(
            shouldBreakout(targetButtonYOffset = 98.6f, gameColumnHeightPx = 100, hasBrokenOutThisActivation = false),
        )
    }

    @Test
    fun `shouldBreakout is false one pixel short of the bottom`() {
        assertFalse(
            shouldBreakout(targetButtonYOffset = 98f, gameColumnHeightPx = 100, hasBrokenOutThisActivation = false),
        )
    }

    @Test
    fun `shouldBreakout is false when the column has no height yet`() {
        assertFalse(
            shouldBreakout(targetButtonYOffset = 99f, gameColumnHeightPx = 0, hasBrokenOutThisActivation = false),
        )
    }

    @Test
    fun `shouldBreakout is false once this activation already broke out`() {
        assertFalse(
            shouldBreakout(targetButtonYOffset = 99f, gameColumnHeightPx = 100, hasBrokenOutThisActivation = true),
        )
    }

    private fun target(
        lifetimeMs: Int,
        position: Int,
    ) = Target(
        id = 1,
        relatedFieldId = 1,
        columnId = 1,
        value = 5,
        position = position,
        appearanceDelayMs = 0,
        lifetimeMs = lifetimeMs,
    )
}
