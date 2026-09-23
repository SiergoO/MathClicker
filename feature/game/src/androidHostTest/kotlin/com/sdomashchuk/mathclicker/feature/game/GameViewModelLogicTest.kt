package com.sdomashchuk.mathclicker.feature.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelLogicTest {
    @Test
    fun `shouldRefreshTargets is false when the id set is unchanged`() {
        assertFalse(shouldRefreshTargets(setOf(1, 2, 3), setOf(1, 2, 3)))
    }

    @Test
    fun `shouldRefreshTargets is true when a level-up grows the id set`() {
        assertTrue(shouldRefreshTargets(setOf(1, 2, 3, 4, 5, 6), setOf(1, 2, 3, 4, 5, 6, 7, 8, 9)))
    }

    @Test
    fun `shouldRefreshTargets is true when a level-up shrinks the id set`() {
        assertTrue(shouldRefreshTargets(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), setOf(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun `shouldRefreshTargets is true for the very first restore, an empty previous set`() {
        assertTrue(shouldRefreshTargets(emptySet(), setOf(1, 2, 3)))
    }
}
