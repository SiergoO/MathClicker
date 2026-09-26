package com.sdamashchuk.matharcade.core.game.scoring

import com.sdamashchuk.matharcade.core.game.scheduledTarget
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Growth-cap value used by tests that only care about performOperation's own mechanics, not about
// which cap a given level produces - see SessionHelperImplTest for the level-scaled ceiling itself.
private const val TEST_FAILED_GROWTH_CAP = 1_000_000

// Every call below reads this same instant back through performOperation's own gameTimeMs
// parameter, so a target built visible here (the isVisible default) stays visible there.
private const val GAME_TIME_MS = 0L

private fun target(
    id: Int,
    value: Int,
    isProfitable: Boolean = true,
    isVisible: Boolean = true,
    isActive: Boolean = true,
) = scheduledTarget(
    id = id,
    relatedFieldId = 0,
    value = value,
    appearanceDelayMs = if (isVisible) 0 else 1,
    referenceGameTimeMs = GAME_TIME_MS,
    isProfitable = isProfitable,
    isActive = isActive,
)

class TargetScoringTest {
    @Test
    fun `division rewards an exact split and marks a non-exact one unprofitable in the same batch`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 7))

        val outcome =
            targets.performOperation(
                OperationSign.DIVISION,
                currentOperationDigit = 2,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )

        val exact = outcome.targets.first { it.id == 1 }
        val inexact = outcome.targets.first { it.id == 2 }
        assertEquals(5, exact.value)
        assertTrue(exact.isProfitable)
        assertEquals(14, inexact.value)
        assertFalse(inexact.isProfitable)
        // MC-39 reverses ASK-7: totalScore is a plain per-target sum, not gated by a shared
        // multiplier, so the failed split does not erase the exact split's own contribution. MC-95:
        // the failure does not erase the combo either - one target scored, so the combo is one.
        assertEquals(5, outcome.totalScore)
        assertEquals(1, outcome.scored)
        assertTrue(outcome.failedCount > 0)
    }

    @Test
    fun `subtraction succeeds for one target while an overshoot on another still fails the press`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 2))

        val outcome =
            targets.performOperation(
                OperationSign.SUBTRACTION,
                currentOperationDigit = 5,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )

        val succeeded = outcome.targets.first { it.id == 1 }
        val overshot = outcome.targets.first { it.id == 2 }
        assertEquals(5, succeeded.value)
        assertTrue(succeeded.isProfitable)
        assertEquals(7, overshot.value)
        assertFalse(overshot.isProfitable)
        assertEquals(5, outcome.totalScore)
        assertEquals(1, outcome.scored)
        assertTrue(outcome.failedCount > 0)
    }

    @Test
    fun `two exact splits earn the sum of what was removed`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 4))

        val outcome =
            targets.performOperation(
                OperationSign.DIVISION,
                currentOperationDigit = 2,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )

        // 10/2 removes 5 and 4/2 removes 2; neither split fails.
        assertEquals(7, outcome.totalScore)
        assertEquals(2, outcome.scored)
        assertEquals(0, outcome.failedCount)
    }

    @Test
    fun `two successful subtractions each award the operation digit`() {
        val targets = listOf(target(id = 1, value = 10), target(id = 2, value = 8))

        val outcome =
            targets.performOperation(
                OperationSign.SUBTRACTION,
                currentOperationDigit = 5,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )

        assertEquals(10, outcome.totalScore)
        assertEquals(2, outcome.scored)
        assertEquals(0, outcome.failedCount)
    }

    @Test
    fun `division by a zero digit fails the target instead of throwing`() {
        val targets = listOf(target(id = 1, value = 10))

        val outcome =
            targets.performOperation(
                OperationSign.DIVISION,
                currentOperationDigit = 0,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )

        assertEquals(10, outcome.targets.first().value)
        assertFalse(outcome.targets.first().isProfitable)
        assertEquals(0, outcome.totalScore)
        assertEquals(0, outcome.scored)
        assertTrue(outcome.failedCount > 0)
    }

    @Test
    fun `raw score and the failed flag are independent of target order - MC-39 reverses ASK-7`() {
        // Same board as the subtraction test above, reordered. Under the old shared, order-sensitive
        // multiplier (ASK-7's pinned behaviour) this pair of orders used to disagree; a plain
        // per-target sum and an OR'd failure flag cannot.
        val overshoot = target(id = 1, value = 2)
        val success = target(id = 2, value = 10)

        val forward =
            listOf(
                overshoot,
                success,
            ).performOperation(
                OperationSign.SUBTRACTION,
                currentOperationDigit = 5,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )
        val backward =
            listOf(
                success,
                overshoot,
            ).performOperation(
                OperationSign.SUBTRACTION,
                currentOperationDigit = 5,
                TEST_FAILED_GROWTH_CAP,
                GAME_TIME_MS,
            )

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
                targets =
                    targets
                        .performOperation(
                            sign,
                            currentOperationDigit = digit,
                            TEST_FAILED_GROWTH_CAP,
                            GAME_TIME_MS,
                        ).targets
                val value = targets.first().value
                assertTrue(value >= 0, "value went negative: $value")
                assertTrue(value <= TEST_FAILED_GROWTH_CAP, "value exceeded the inflation cap: $value")
            }
        }
    }
}
