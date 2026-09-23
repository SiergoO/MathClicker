package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SAMPLE_ITERATIONS = 200
private const val SAMPLE_LEVEL = 50

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
}
