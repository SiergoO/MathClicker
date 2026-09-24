package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.Target
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun target(
    id: Int = 1,
    value: Int = 10,
    fallenMs: Int = 0,
    appearanceDelayMs: Int = 0,
    isActive: Boolean = true,
    isVisible: Boolean = false,
) = Target(
    id = id,
    relatedFieldId = 1,
    columnId = 0,
    value = value,
    fallenMs = fallenMs,
    appearanceDelayMs = appearanceDelayMs,
    lifetimeMs = 1000,
    isActive = isActive,
    isVisible = isVisible,
)

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

    @Test
    fun `shouldPersistTargets is false when only fallenMs moves`() {
        val previous = listOf(target(fallenMs = 0))
        val next = listOf(target(fallenMs = 40))
        assertFalse(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is false when only appearanceDelayMs moves`() {
        val previous = listOf(target(appearanceDelayMs = 5000))
        val next = listOf(target(appearanceDelayMs = 4960))
        assertFalse(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is false when both clock fields move at once`() {
        val previous = listOf(target(fallenMs = 0, appearanceDelayMs = 5000))
        val next = listOf(target(fallenMs = 40, appearanceDelayMs = 0))
        assertFalse(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when value changes`() {
        val previous = listOf(target(value = 10))
        val next = listOf(target(value = 9))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when isActive changes`() {
        val previous = listOf(target(isActive = true))
        val next = listOf(target(isActive = false))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when isVisible changes`() {
        val previous = listOf(target(isVisible = false))
        val next = listOf(target(isVisible = true))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when the id set changes`() {
        val previous = listOf(target(id = 1))
        val next = listOf(target(id = 1), target(id = 2))
        assertTrue(shouldPersistTargets(previous, next))
    }
}
