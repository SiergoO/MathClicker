package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.game.objectmapper.decrementValue
import com.sdamashchuk.matharcade.core.game.scoring.performOperation
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SAMPLE_ITERATIONS = 200
private const val SAMPLE_LEVEL = 50

private const val SIMULATION_RUNS = 2000
private const val SIMULATION_DIVISIONS = 3
private const val SIMULATION_TAP_RATE_PER_SECOND = 8
private const val SIMULATION_TAP_BUDGET_SECONDS = 3

private fun failedGrowthCapTarget(value: Int) =
    Target(
        id = 1,
        relatedFieldId = 0,
        columnId = 0,
        value = value,
        fallenMs = 0,
        appearanceDelayMs = 0,
        lifetimeMs = 0,
        isVisible = true,
    )

class SessionHelperImplTest {
    private val helper = SessionHelperImpl()

    @Test
    fun `the starting difficulty is the one the game was balanced around`() {
        // Literals on purpose. Every other test here derives its expectation from these same
        // properties, so none of them can notice a constant changing — a ten-fold rise in starting
        // target value would leave the suite green. This test is the only thing pinning the balance.
        assertEquals(1..20, helper.initialTargetValueRange)
        assertEquals(20000..40000, helper.initialTargetLifetimeMsRange)
        assertEquals(10000..20000, helper.initialTargetAppearanceDelayMsRange)
        assertEquals(6..10, helper.initialTargetAmountRange)
        assertEquals(2..5, helper.initialDivisionValueRange)
        assertEquals(1..3, helper.initialSubtractionValueRange)
        assertEquals(1..999, helper.levelRange)
    }

    @Test
    fun `the narrow level-one ranges are produced in full - not merely stayed within`() {
        // Asserting membership cannot catch a range that gets narrower. Where the range is small
        // enough to be sampled exhaustively, assert the whole set instead. The wide ranges
        // (lifetime, appearance delay) cannot be pinned this way while the helper returns a random
        // value with no way to ask it for the range it drew from; MC-6 should add that seam.
        val level = 0

        assertEquals((2..5).toSet(), sampled { helper.getDivisionDigitByLevel(level) })
        assertEquals((1..3).toSet(), sampled { helper.getSubtractionDigitByLevel(level) })
        assertEquals((6..10).toSet(), sampled { helper.getTargetAmountByLevel(level) })
    }

    private fun sampled(draw: () -> Int): Set<Int> = buildSet { repeat(SAMPLE_ITERATIONS) { add(draw()) } }

    @Test
    fun `lifetime and target value and target amount all scale with level per the difficulty curve`() {
        val lifetimeRange =
            IntRange(
                helper.initialTargetLifetimeMsRange.first - SAMPLE_LEVEL * 15,
                helper.initialTargetLifetimeMsRange.last - SAMPLE_LEVEL * 30,
            )
        val valueRange =
            IntRange(
                helper.initialTargetValueRange.first + SAMPLE_LEVEL / 10,
                helper.initialTargetValueRange.last + SAMPLE_LEVEL * 3,
            )
        val amountRange =
            IntRange(
                helper.initialTargetAmountRange.first + SAMPLE_LEVEL / 5,
                helper.initialTargetAmountRange.last + SAMPLE_LEVEL / 3,
            )

        repeat(SAMPLE_ITERATIONS) {
            assertTrue(helper.getTargetLifetimeMsByLevel(SAMPLE_LEVEL) in lifetimeRange)
            assertTrue(helper.getTargetValueByLevel(SAMPLE_LEVEL) in valueRange)
            assertTrue(helper.getTargetAmountByLevel(SAMPLE_LEVEL) in amountRange)
        }
    }

    @Test
    fun `division and subtraction digits each stay within their own level-scaled range`() {
        val divisionRange =
            IntRange(
                helper.initialDivisionValueRange.first + SAMPLE_LEVEL / 10,
                helper.initialDivisionValueRange.last + SAMPLE_LEVEL / 5,
            )
        val subtractionRange =
            IntRange(
                helper.initialSubtractionValueRange.first + SAMPLE_LEVEL / 10,
                helper.initialSubtractionValueRange.last + SAMPLE_LEVEL / 3,
            )

        repeat(SAMPLE_ITERATIONS) {
            assertTrue(helper.getDivisionDigitByLevel(SAMPLE_LEVEL) in divisionRange)
            assertTrue(helper.getSubtractionDigitByLevel(SAMPLE_LEVEL) in subtractionRange)
        }
    }

    @Test
    fun `operation digit delegates to the range matching its sign - not the other one`() {
        val level = 0
        val divisionOnly =
            IntRange(helper.initialDivisionValueRange.first, helper.initialDivisionValueRange.last)
        val subtractionOnly =
            IntRange(helper.initialSubtractionValueRange.first, helper.initialSubtractionValueRange.last)

        repeat(SAMPLE_ITERATIONS) {
            assertTrue(helper.getOperationDigitByLevel(OperationSign.DIVISION, level) in divisionOnly)
            assertTrue(helper.getOperationDigitByLevel(OperationSign.SUBTRACTION, level) in subtractionOnly)
        }
    }

    @Test
    fun `appearance delay is zero for the first column group and scaled for the next`() {
        assertEquals(0, helper.getTargetAppearanceDelayMsById(0))
        assertEquals(0, helper.getTargetAppearanceDelayMsById(3))

        val nextGroupRange =
            IntRange(helper.initialTargetAppearanceDelayMsRange.first, helper.initialTargetAppearanceDelayMsRange.last)
        repeat(SAMPLE_ITERATIONS) {
            assertTrue(helper.getTargetAppearanceDelayMsById(4) in nextGroupRange)
        }
    }

    @Test
    fun `the failed growth ceiling rises with level and never dips below that level's own value maximum`() {
        var previousCap = 0
        for (level in listOf(1, 5, 10, 30, 50, 200, 500, 999)) {
            val cap = helper.failedGrowthCap(level)
            val normalMax = helper.initialTargetValueRange.last + level * 3
            assertTrue(cap >= normalMax, "level $level: ceiling $cap fell below the normal maximum $normalMax")
            assertTrue(cap > previousCap, "level $level: ceiling $cap did not grow past $previousCap")
            previousCap = cap
        }
    }

    @Test
    fun `the failed growth ceiling is pinned to exact values at levels 1 10 30 and 50`() {
        assertEquals(92, helper.failedGrowthCap(1))
        assertEquals(200, helper.failedGrowthCap(10))
        assertEquals(440, helper.failedGrowthCap(30))
        assertEquals(680, helper.failedGrowthCap(50))
    }

    @Test
    fun `a target already at the ceiling stays there after another failed division`() {
        val level = 1
        val cap = helper.failedGrowthCap(level)
        // 7 does not divide 92, so this is a failed division: value * 7 would overshoot the
        // ceiling by a wide margin without the clamp.
        val targets = listOf(failedGrowthCapTarget(cap))

        val (updated, _, failed) = targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 7, cap)

        assertTrue(failed)
        assertEquals(cap, updated.first().value)
    }

    @Test
    fun `a target at the failed growth ceiling is recoverable within its own fall - playability simulation`() {
        // A player who reads the upcoming operation before firing never fires a division that will
        // fail: they tap the remainder down to a multiple of the digit first (decrementValue, the
        // same helper targetClicked uses), then fire (performOperation, the same call fireButtonClicked
        // makes). SIMULATION_RUNS runs per level, SIMULATION_DIVISIONS such divisions with the real
        // level-scaled digit range, closing whatever is left with plain taps at
        // SIMULATION_TAP_RATE_PER_SECOND: level 1 closes within 12 taps every run (median 6); level 10
        // within 12 (median 7); level 30 within 20 (median 10); level 50 within 22 (median 14) - all
        // under three seconds of tapping, comfortably inside any level's fall.
        val tapBudget = SIMULATION_TAP_RATE_PER_SECOND * SIMULATION_TAP_BUDGET_SECONDS
        for (level in listOf(1, 10, 30, 50)) {
            val cap = helper.failedGrowthCap(level)
            repeat(SIMULATION_RUNS) {
                var targets = listOf(failedGrowthCapTarget(cap))
                var tapsUsed = 0
                repeat(SIMULATION_DIVISIONS) {
                    val digit = helper.getDivisionDigitByLevel(level)
                    val remainder = targets.first().value % digit
                    if (remainder != 0) {
                        targets = targets.decrementValue(1, remainder)
                        tapsUsed += remainder
                    }
                    targets = targets.performOperation(OperationSign.DIVISION, digit, cap).targets
                }
                tapsUsed += targets.first().value
                assertTrue(
                    tapsUsed <= tapBudget,
                    "level $level: recovering the ceiling took $tapsUsed taps, over the $tapBudget budget",
                )
            }
        }
    }

    @Test
    fun `no level from 1 to 999 produces a ceiling that overflows Int when multiplied by its largest divisor`() {
        for (level in helper.levelRange) {
            val cap = helper.failedGrowthCap(level)
            val maxDivisor = helper.initialDivisionValueRange.last + level / 5
            assertTrue(cap.toLong() * maxDivisor <= Int.MAX_VALUE)
        }
    }
}
