package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
