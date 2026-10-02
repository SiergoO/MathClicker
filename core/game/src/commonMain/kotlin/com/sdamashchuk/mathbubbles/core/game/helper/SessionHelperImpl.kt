package com.sdamashchuk.mathbubbles.core.game.helper

import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

class SessionHelperImpl(
    private val random: Random = Random.Default,
) : SessionHelper {
    companion object {
        private const val LEVEL_MIN = 1
        private const val LEVEL_MAX = 999

        private const val INITIAL_TARGET_VALUE_MIN = 3
        private const val INITIAL_TARGET_VALUE_MAX = 9

        // A speed multiplier drawn from (MIN_SPEED_MULTIPLIER, 1] lands flight time in [base, base * 1.25].
        private const val FLIGHT_SPEED_SPREAD = 0.2f
        private const val MIN_SPEED_MULTIPLIER = 1f - FLIGHT_SPEED_SPREAD

        private const val FINISH_SPACING_FRACTION = 0.38f
        private const val MIN_FINISH_SPACING_MS = 1800
        private const val MIN_TOTAL_LIFE_LOSS_MS = 3500

        // One whole fall, so speed is a fraction of a fall per millisecond, not a screen dimension.
        private const val SPAN_UNITS = 1f

        private const val INITIAL_TARGET_AMOUNT_MIN = 9
        private const val INITIAL_TARGET_AMOUNT_MAX = 14

        private const val INITIAL_DIVISION_VALUE_MIN = 2
        private const val INITIAL_DIVISION_VALUE_MAX = 3

        private const val INITIAL_SUBTRACTION_VALUE_MIN = 1
        private const val INITIAL_SUBTRACTION_VALUE_MAX = 3

        private const val TARGET_VALUE_LEVEL_SCALE = 2
        private const val FAILED_GROWTH_FACTOR = 4
    }

    override val levelRange = IntRange(LEVEL_MIN, LEVEL_MAX)
    override val initialTargetValueRange = IntRange(INITIAL_TARGET_VALUE_MIN, INITIAL_TARGET_VALUE_MAX)

    // The raw base and ceiling the curve is built from, not getTargetFlightTimeMs(1)'s tighter output.
    override val initialTargetFlightTimeMsRange =
        IntRange(FLIGHT_BASE_MS, ceil(FLIGHT_BASE_MS / MIN_SPEED_MULTIPLIER).toInt())
    override val initialTargetAmountRange = IntRange(INITIAL_TARGET_AMOUNT_MIN, INITIAL_TARGET_AMOUNT_MAX)
    override val initialDivisionValueRange = IntRange(INITIAL_DIVISION_VALUE_MIN, INITIAL_DIVISION_VALUE_MAX)
    override val initialSubtractionValueRange = IntRange(INITIAL_SUBTRACTION_VALUE_MIN, INITIAL_SUBTRACTION_VALUE_MAX)

    init {
        check(MIN_FINISH_SPACING_MS * (INITIAL_LIFE_COUNT - 1) >= MIN_TOTAL_LIFE_LOSS_MS) {
            "MIN_FINISH_SPACING_MS is too small: losing every life must take at least $MIN_TOTAL_LIFE_LOSS_MS ms"
        }
    }

    override fun getTargetSpeedByLevel(level: Int): Float = SPAN_UNITS / baseFlightMsByLevel(level)

    /**
     * Random flight time in [base, base / MIN_SPEED_MULTIPLIER] for the level.
     */
    override fun getTargetFlightTimeMs(level: Int): Int {
        val speedMultiplier = 1f - random.nextFloat() * FLIGHT_SPEED_SPREAD
        return (baseFlightMsByLevel(level) / speedMultiplier).roundToInt()
    }

    // No Random draw: two consecutive finishesAtMs differ by exactly this, whatever the flight spread does.
    override fun getFinishSpacingMsByLevel(level: Int): Int =
        (baseFlightMsByLevel(level) * FINISH_SPACING_FRACTION).roundToInt().coerceAtLeast(MIN_FINISH_SPACING_MS)

    // ceil, not round, so getTargetFlightTimeMs's own roundToInt can never land past this bound - which
    // is what keeps a target's appearsAtMs from ever preceding Field.gameTimeMs.
    override fun getOpeningOffsetMsByLevel(level: Int): Int =
        ceil(baseFlightMsByLevel(level) / MIN_SPEED_MULTIPLIER).toInt()

    /**
     * Shaped against [operationDigit] so the taps needed before it succeeds follow the level's own
     * preparation profile - see desiredPreparationCost.
     */
    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int {
        val minThreshold = initialTargetValueRange.first + level / 10
        val maxThreshold = initialTargetValueRange.last + level * TARGET_VALUE_LEVEL_SCALE
        if (operationDigit <= 1) {
            // mod 1, or a non-positive digit, is always ready.
            return IntRange(minThreshold, maxThreshold).random(random)
        }

        // Headroom for the largest possible shift is reserved before the draw, so the shift below can never
        // cross maxThreshold; coerceAtLeast keeps the range non-empty if the digit outgrows the spread.
        val maxShift = operationDigit - 1
        val drawUpperBound = (maxThreshold - maxShift).coerceAtLeast(minThreshold)
        val baseValue = IntRange(minThreshold, drawUpperBound).random(random)

        val desiredCost = desiredPreparationCost(level, operationDigit, random)
        val currentCost = baseValue % operationDigit
        val shift = (desiredCost - currentCost + operationDigit) % operationDigit
        return (baseValue + shift).coerceAtMost(maxThreshold)
    }

    /**
     * Either ready to fire or a trap - a value below [operationDigit], which tapping can only shrink
     * further and never lift back to ready. The level's trap profile picks the share of each.
     */
    override fun getSubtractionTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int {
        val minThreshold = initialTargetValueRange.first + level / 10
        val maxThreshold = initialTargetValueRange.last + level * TARGET_VALUE_LEVEL_SCALE
        return if (operationDigit <= 1) {
            // Every target value is at least 1, so nothing can fall below this digit.
            IntRange(minThreshold, maxThreshold).random(random)
        } else {
            val trapValue = desiredSubtractionTrapValue(level, operationDigit, random)
            if (trapValue == null) {
                val readyLower = maxOf(minThreshold, operationDigit).coerceAtMost(maxThreshold)
                IntRange(readyLower, maxThreshold).random(random)
            } else {
                trapValue
            }
        }
    }

    override fun getTargetAmountByLevel(level: Int): Int {
        val minThreshold = initialTargetAmountRange.first + level / 5
        val maxThreshold = initialTargetAmountRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random(random)
    }

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ): Int =
        if (operationSign ==
            OperationSign.DIVISION
        ) {
            getDivisionDigitByLevel(level)
        } else {
            getSubtractionDigitByLevel(level)
        }

    override fun getDivisionDigitByLevel(level: Int): Int {
        val minThreshold = initialDivisionValueRange.first + level / 10
        val maxThreshold = initialDivisionValueRange.last + level / 5
        return IntRange(minThreshold, maxThreshold).random(random)
    }

    override fun getSubtractionDigitByLevel(level: Int): Int {
        val minThreshold = initialSubtractionValueRange.first + level / 10
        val maxThreshold = initialSubtractionValueRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random(random)
    }

    // Derived from getTargetValueByLevel's own curve so the two cannot drift apart: a factor of 1 would
    // make the ceiling equal that curve's maximum, and below 1 a failed division becomes a gift.
    override fun failedGrowthCap(level: Int): Int =
        FAILED_GROWTH_FACTOR * (initialTargetValueRange.last + level * TARGET_VALUE_LEVEL_SCALE)
}
