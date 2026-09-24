package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Target
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun target(
    id: Int,
    value: Int,
    isProfitable: Boolean = true,
    isVisible: Boolean = true,
    isActive: Boolean = true,
) = Target(
    id = id,
    relatedFieldId = 0,
    columnId = 0,
    value = value,
    fallenMs = 0,
    appearanceDelayMs = 0,
    lifetimeMs = 0,
    isProfitable = isProfitable,
    isVisible = isVisible,
    isActive = isActive,
)

class TargetsMapperTest {
    @Test
    fun `changeVisibility only changes the target with the matching id`() {
        val targets = listOf(target(id = 1, value = 5, isVisible = false), target(id = 2, value = 5, isVisible = false))

        val updated = targets.changeVisibility(id = 1, isVisible = true)

        assertTrue(updated.first { it.id == 1 }.isVisible)
        assertFalse(updated.first { it.id == 2 }.isVisible)
    }

    @Test
    fun `a target hits zero and is hidden by the follow-up ensureVisible pass`() {
        val targets = listOf(target(id = 1, value = 3, isVisible = true))

        val decremented = targets.decrementValue(id = 1, decrement = 3)
        val visibility = decremented.ensureVisible()

        assertEquals(0, visibility.first().value)
        assertFalse(visibility.first().isVisible)
    }

    @Test
    fun `advance burns the step against appearanceDelayMs while a target is still waiting`() {
        val targets = listOf(target(id = 1, value = 5, isVisible = false).copy(appearanceDelayMs = 500))

        val advanced = targets.advance(200)

        val updated = advanced.first()
        assertEquals(300, updated.appearanceDelayMs)
        assertEquals(0, updated.fallenMs)
        assertFalse(updated.isVisible)
    }

    @Test
    fun `advance carries the spill into fallenMs when a step straddles the delay-fall boundary`() {
        val targets = listOf(target(id = 1, value = 5, isVisible = false).copy(appearanceDelayMs = 100))

        val advanced = targets.advance(150)

        val updated = advanced.first()
        assertEquals(0, updated.appearanceDelayMs)
        assertEquals(50, updated.fallenMs)
        assertTrue(updated.isVisible)
    }

    @Test
    fun `advance adds the full step to fallenMs once a target has no delay left`() {
        val targets = listOf(target(id = 1, value = 5, isVisible = true).copy(fallenMs = 1000))

        val advanced = targets.advance(200)

        val updated = advanced.first()
        assertEquals(0, updated.appearanceDelayMs)
        assertEquals(1200, updated.fallenMs)
        assertTrue(updated.isVisible)
    }

    @Test
    fun `advance leaves an inactive target untouched`() {
        val targets = listOf(target(id = 1, value = 5, isActive = false).copy(appearanceDelayMs = 100, fallenMs = 0))

        val advanced = targets.advance(200)

        assertEquals(targets, advanced)
    }

    // The property itself lives on :core:model's Target, not this mapper - covered from here
    // because :core:model has no test source set, the same as Target.position.
    @Test
    fun `isTelegraphingBreakout flips on at the final 15 percent of the fall`() {
        val justBefore = target(id = 1, value = 5).copy(fallenMs = 849, lifetimeMs = 1000)
        val atTheBoundary = target(id = 1, value = 5).copy(fallenMs = 850, lifetimeMs = 1000)
        val midFall = target(id = 1, value = 5).copy(fallenMs = 200, lifetimeMs = 1000)
        val alreadyBrokenOut = target(id = 1, value = 5).copy(fallenMs = 1000, lifetimeMs = 1000)

        assertFalse(justBefore.isTelegraphingBreakout)
        assertTrue(atTheBoundary.isTelegraphingBreakout)
        assertFalse(midFall.isTelegraphingBreakout)
        assertTrue(alreadyBrokenOut.isTelegraphingBreakout)
    }

    @Test
    fun `isTelegraphingBreakout is false rather than dividing by zero for a zero or negative lifetime`() {
        val zeroLifetime = target(id = 1, value = 5).copy(fallenMs = 500, lifetimeMs = 0)
        val negativeLifetime = target(id = 1, value = 5).copy(fallenMs = 500, lifetimeMs = -100)

        assertFalse(zeroLifetime.isTelegraphingBreakout)
        assertFalse(negativeLifetime.isTelegraphingBreakout)
    }
}
