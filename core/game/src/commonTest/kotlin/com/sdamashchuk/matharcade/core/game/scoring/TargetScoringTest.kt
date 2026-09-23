package com.sdamashchuk.matharcade.core.game.scoring

import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlin.random.Random
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

class TargetScoringTest {
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

    @Test
    fun `division by a zero digit fails the target instead of throwing`() {
        val targets = listOf(target(id = 1, value = 10))

        val (updated, score) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 0)

        assertEquals(10, updated.first().value)
        assertFalse(updated.first().isProfitable)
        assertEquals(0, score)
    }

    @Test
    fun `interleaved fire presses never drive a target's value negative or past the inflation cap`() {
        // A fixed digit that only ever fails cancels itself out (multiply then divide by the same
        // digit lands back where it started), so this mirrors the audit's actual trigger: division
        // and subtraction alternating on the same target, which keeps knocking the value off any
        // multiple of the next digit and lets a failed division re-fail every time.
        val random = Random(42)
        repeat(2000) {
            var targets = listOf(target(id = 1, value = random.nextInt(1, 21)))
            repeat(100) {
                val sign = if (random.nextBoolean()) OperationSign.DIVISION else OperationSign.SUBTRACTION
                val digit = random.nextInt(2, 6)
                targets = targets.performOperation(sign, currentOperationDigit = digit).first
                val value = targets.first().value
                assertTrue(value >= 0, "value went negative: $value")
                assertTrue(value <= 1_000_000, "value exceeded the inflation cap: $value")
            }
        }
    }
}
