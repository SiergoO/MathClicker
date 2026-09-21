package com.sdomashchuk.mathclicker.core.game.objectmapper

import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.core.model.Target
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
    position = 0,
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
    fun `division rewards an exact split and marks a non-exact one unprofitable in the same batch`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 7))

        val (updated, score) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 2)

        val exact = updated.first { it.id == 1 }
        val inexact = updated.first { it.id == 2 }
        assertEquals(5, exact.value)
        assertTrue(exact.isProfitable)
        assertEquals(14, inexact.value)
        assertFalse(inexact.isProfitable)
        // Characterization: the batch multiplier is shared across all targets and the failed
        // split decrements it back to zero, so the exact split's own score is zeroed out too.
        assertEquals(0, score)
    }

    @Test
    fun `subtraction succeeds for one target while an overshoot on another zeroes the batch score`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 2))

        val (updated, score) = targets.performOperation(OperationSign.SUBTRACTION, currentOperationDigit = 5)

        val succeeded = updated.first { it.id == 1 }
        val overshot = updated.first { it.id == 2 }
        assertEquals(5, succeeded.value)
        assertTrue(succeeded.isProfitable)
        assertEquals(7, overshot.value)
        assertFalse(overshot.isProfitable)
        // Same shared-multiplier quirk as the division case above: the overshoot decrements the
        // multiplier back to zero, wiping the score the first target earned.
        assertEquals(0, score)
    }

    @Test
    fun `two exact splits earn the sum of what was removed - multiplied by the batch multiplier`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 4))

        val (_, score) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 2)

        // Without a non-zero expectation somewhere, `finalScore = totalScore * multiplier` could be
        // replaced by a literal 0 and every other test here would still pass.
        // 10/2 removes 5 and 4/2 removes 2; both splits are exact, so the multiplier reaches 2.
        assertEquals(14, score)
    }

    @Test
    fun `two successful subtractions each award the operation digit at multiplier one`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 8))

        val (_, score) = targets.performOperation(OperationSign.SUBTRACTION, currentOperationDigit = 5)

        // Neither result reaches zero, so the multiplier only ever rises from its initial 0 to 1.
        assertEquals(10, score)
    }
}
