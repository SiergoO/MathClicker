package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetButtonLogicTest {
    @Test
    fun `remainingFallDurationMs subtracts the elapsed fall time from the lifetime`() {
        val target = target(lifetimeMs = 2000, fallenMs = 500)

        assertEquals(1500, remainingFallDurationMs(target))
    }

    @Test
    fun `remainingFallDurationMs returns the full lifetime when nothing has fallen yet`() {
        val target = target(lifetimeMs = 2000, fallenMs = 0)

        assertEquals(2000, remainingFallDurationMs(target))
    }

    @Test
    fun `remainingFallDurationMs floors at zero when fallenMs exceeds the lifetime`() {
        val target = target(lifetimeMs = 2000, fallenMs = 3000)

        assertEquals(0, remainingFallDurationMs(target))
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
        fallenMs: Int,
    ) = Target(
        id = 1,
        relatedFieldId = 1,
        columnId = 1,
        value = 5,
        fallenMs = fallenMs,
        appearanceDelayMs = 0,
        lifetimeMs = lifetimeMs,
    )
}
