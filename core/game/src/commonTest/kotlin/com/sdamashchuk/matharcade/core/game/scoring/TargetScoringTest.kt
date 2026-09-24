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

        val (updated, score, failed) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 2)

        val exact = updated.first { it.id == 1 }
        val inexact = updated.first { it.id == 2 }
        assertEquals(5, exact.value)
        assertTrue(exact.isProfitable)
        assertEquals(14, inexact.value)
        assertFalse(inexact.isProfitable)
        // MC-39 reverses ASK-7: totalScore is a plain per-target sum now, not gated by a shared
        // multiplier, so the failed split no longer erases the exact split's own contribution here -
        // the streak that used to live in this function resets on Field instead.
        assertEquals(5, score)
        assertTrue(failed)
    }

    @Test
    fun `subtraction succeeds for one target while an overshoot on another still fails the press`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 2))

        val (updated, score, failed) = targets.performOperation(OperationSign.SUBTRACTION, currentOperationDigit = 5)

        val succeeded = updated.first { it.id == 1 }
        val overshot = updated.first { it.id == 2 }
        assertEquals(5, succeeded.value)
        assertTrue(succeeded.isProfitable)
        assertEquals(7, overshot.value)
        assertFalse(overshot.isProfitable)
        assertEquals(5, score)
        assertTrue(failed)
    }

    @Test
    fun `two exact splits earn the sum of what was removed`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 4))

        val (_, score, failed) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 2)

        // 10/2 removes 5 and 4/2 removes 2; neither split fails.
        assertEquals(7, score)
        assertFalse(failed)
    }

    @Test
    fun `two successful subtractions each award the operation digit`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 8))

        val (_, score, failed) = targets.performOperation(OperationSign.SUBTRACTION, currentOperationDigit = 5)

        assertEquals(10, score)
        assertFalse(failed)
    }

    @Test
    fun `division by a zero digit fails the target instead of throwing`() {
        val targets = listOf(target(id = 1, value = 10))

        val (updated, score, failed) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 0)

        assertEquals(10, updated.first().value)
        assertFalse(updated.first().isProfitable)
        assertEquals(0, score)
        assertTrue(failed)
    }

    @Test
    fun `raw score and the failed flag are independent of target order - MC-39 reverses ASK-7`() {
        // Same board as the subtraction test above, reordered. Under the old shared, order-sensitive
        // multiplier (ASK-7's pinned behaviour) this pair of orders used to disagree; a plain
        // per-target sum and an OR'd failure flag cannot.
        val overshoot = target(id = 1, value = 2)
        val success = target(id = 2, value = 10)

        val forward = listOf(overshoot, success).performOperation(OperationSign.SUBTRACTION, currentOperationDigit = 5)
        val backward = listOf(success, overshoot).performOperation(OperationSign.SUBTRACTION, currentOperationDigit = 5)

        assertEquals(forward.totalScore, backward.totalScore)
        assertEquals(forward.failed, backward.failed)
        assertEquals(5, forward.totalScore)
        assertTrue(forward.failed)
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
                targets = targets.performOperation(sign, currentOperationDigit = digit).targets
                val value = targets.first().value
                assertTrue(value >= 0, "value went negative: $value")
                assertTrue(value <= 1_000_000, "value exceeded the inflation cap: $value")
            }
        }
    }
}
