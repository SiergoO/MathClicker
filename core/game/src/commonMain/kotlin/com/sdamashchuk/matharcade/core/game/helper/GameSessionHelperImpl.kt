package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlin.random.Random

class SessionHelperImpl(
    private val random: Random = Random.Default,
) : SessionHelper {
    companion object {
        private const val LEVEL_MIN = 1
        private const val LEVEL_MAX = 999

        private const val INITIAL_TARGET_VALUE_MIN = 1
        private const val INITIAL_TARGET_VALUE_MAX = 20

        // MC-52: base minus a linear step, floored, then a fixed spread added on top of the floored
        // minimum rather than the raw base - that ordering is what keeps the resulting range from
        // ever going empty at any level in 1..999 (SessionHelperImplTest pins this as a property,
        // not a number: the pre-MC-52 curve crossed at level 1334, which is why LEVEL_MAX exists).
        private const val LIFETIME_BASE_MS = 9500
        private const val LIFETIME_STEP_MS = 110
        private const val LIFETIME_FLOOR_MS = 3500
        private const val LIFETIME_SPREAD_MS = 2500

        // Wave gap, not a per-target delay: getTargetAppearanceDelayMsByIdAndLevel spends this once
        // per id/GAME_COLUMN_COUNT group, then subdivides it again within the group (see that
        // function) so the four targets sharing a wave don't all land on the same instant - the bug
        // a device run caught (game over in ~28s with zero input, MC-52 spec).
        private const val WAVE_GAP_BASE_MS = 3800
        private const val WAVE_GAP_STEP_MS = 55
        private const val WAVE_GAP_FLOOR_MS = 1400
        private const val WAVE_GAP_SPREAD_MS = 800

        private const val INITIAL_TARGET_AMOUNT_MIN = 6
        private const val INITIAL_TARGET_AMOUNT_MAX = 10

        private const val INITIAL_DIVISION_VALUE_MIN = 2
        private const val INITIAL_DIVISION_VALUE_MAX = 5

        private const val INITIAL_SUBTRACTION_VALUE_MIN = 1
        private const val INITIAL_SUBTRACTION_VALUE_MAX = 3

        private const val TARGET_VALUE_LEVEL_SCALE = 3
        private const val FAILED_GROWTH_FACTOR = 4
    }

    override val levelRange = IntRange(LEVEL_MIN, LEVEL_MAX)
    override val initialTargetValueRange = IntRange(INITIAL_TARGET_VALUE_MIN, INITIAL_TARGET_VALUE_MAX)
    override val initialTargetLifetimeMsRange = IntRange(LIFETIME_BASE_MS, LIFETIME_BASE_MS + LIFETIME_SPREAD_MS)
    override val initialTargetAppearanceDelayMsRange = IntRange(WAVE_GAP_BASE_MS, WAVE_GAP_BASE_MS + WAVE_GAP_SPREAD_MS)
    override val initialTargetAmountRange = IntRange(INITIAL_TARGET_AMOUNT_MIN, INITIAL_TARGET_AMOUNT_MAX)
    override val initialDivisionValueRange = IntRange(INITIAL_DIVISION_VALUE_MIN, INITIAL_DIVISION_VALUE_MAX)
    override val initialSubtractionValueRange = IntRange(INITIAL_SUBTRACTION_VALUE_MIN, INITIAL_SUBTRACTION_VALUE_MAX)

    /**
     * Calculates the target lifetime depending on the level. The higher the level, the less time the target should be
     * visible during the level, down to a floor it never falls below.
     * @param level
     * @return random target lifetime in a certain range of values.
     */
    override fun getTargetLifetimeMsByLevel(level: Int): Int {
        val floor = (LIFETIME_BASE_MS - level * LIFETIME_STEP_MS).coerceAtLeast(LIFETIME_FLOOR_MS)
        return IntRange(floor, floor + LIFETIME_SPREAD_MS).random(random)
    }

    // The gap between waves of GAME_COLUMN_COUNT targets, same floor-then-spread shape as the
    // lifetime curve above and for the same reason: tightens with level down to a floor it can never
    // cross, so the range this feeds getTargetAppearanceDelayMsByIdAndLevel from can never go empty.
    private fun getWaveGapMsByLevel(level: Int): Int {
        val floor = (WAVE_GAP_BASE_MS - level * WAVE_GAP_STEP_MS).coerceAtLeast(WAVE_GAP_FLOOR_MS)
        return IntRange(floor, floor + WAVE_GAP_SPREAD_MS).random(random)
    }

    /**
     * Calculates the target appearance delay depending on the level. Targets are grouped into waves of
     * GAME_COLUMN_COUNT, and staggered within their own wave rather than all landing on the wave's start:
     * a whole wave sharing one delay (the pre-MC-52 shape) let the first wave alone close out a session
     * with zero player input in ~28s on device, faster than any single target's own fall.
     * @param id
     * @param level
     * @return target appearance delay, randomized once per call by the wave gap it is built from.
     */
    override fun getTargetAppearanceDelayMsByIdAndLevel(
        id: Int,
        level: Int,
    ): Int {
        val waveGapMs = getWaveGapMsByLevel(level)
        val wave = id / GAME_COLUMN_COUNT
        val positionInWave = id % GAME_COLUMN_COUNT
        return wave * waveGapMs + positionInWave * (waveGapMs / GAME_COLUMN_COUNT)
    }

    /**
     * Calculates the target value depending on the level and the digit the player is about to press
     * with. The higher the level, the higher the target value; the number of taps needed before
     * operationDigit succeeds against it (the target's preparation cost) follows the level's own
     * profile - see desiredPreparationCost.
     * @param level
     * @param operationDigit the divisor the generated value is shaped against.
     * @return random target value in a certain range of values.
     */
    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int {
        val minThreshold = initialTargetValueRange.first + level / 10
        val maxThreshold = initialTargetValueRange.last + level * TARGET_VALUE_LEVEL_SCALE
        if (operationDigit <= 1) {
            // Nothing to prepare - mod 1 (or an invalid, non-positive digit) is always ready.
            return IntRange(minThreshold, maxThreshold).random(random)
        }

        // Reserving headroom equal to the largest possible upward shift (operationDigit - 1) before
        // drawing the base value is what keeps the shift below from ever crossing maxThreshold - the
        // failure mode named in the spec for shifting the other direction (mutation M3) is going
        // below minThreshold, and this is its mirror image for the maximum. coerceAtLeast guards the
        // pathological case where the digit is wider than the level's own value spread (never
        // observed for the real digit ranges in 1..999, but the range must never go empty either way).
        val maxShift = operationDigit - 1
        val drawUpperBound = (maxThreshold - maxShift).coerceAtLeast(minThreshold)
        val baseValue = IntRange(minThreshold, drawUpperBound).random(random)

        val desiredCost = desiredPreparationCost(level, operationDigit, random)
        val currentCost = baseValue % operationDigit
        val shift = (desiredCost - currentCost + operationDigit) % operationDigit
        return (baseValue + shift).coerceAtMost(maxThreshold)
    }

    /**
     * Calculates the target amount should be displayed during the level depending on the level. The higher the level,
     * the more targets should appear during the level.
     * @param level
     * @return random target amount value in a certain range of values.
     */
    override fun getTargetAmountByLevel(level: Int): Int {
        val minThreshold = initialTargetAmountRange.first + level / 5
        val maxThreshold = initialTargetAmountRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random(random)
    }

    /**
     * Calculates the operation digit with known sign passed via parameters.
     * @param operationSign can be either division or subtraction. The calculation of operation digit depends on the
     * sign digit passed via params.
     * @param level
     * @return random division or subtraction digit in a certain range of values.
     */
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

    /**
     * Calculates the division digit depending on the level.
     * @param level
     * @return random division digit in a certain range of values.
     */
    override fun getDivisionDigitByLevel(level: Int): Int {
        val minThreshold = initialDivisionValueRange.first + level / 10
        val maxThreshold = initialDivisionValueRange.last + level / 5
        return IntRange(minThreshold, maxThreshold).random(random)
    }

    /**
     * Calculates the subtraction digit depending on the level.
     * @param level
     * @return random subtraction digit in a certain range of values.
     */
    override fun getSubtractionDigitByLevel(level: Int): Int {
        val minThreshold = initialSubtractionValueRange.first + level / 10
        val maxThreshold = initialSubtractionValueRange.last + level / 3
        return IntRange(minThreshold, maxThreshold).random(random)
    }

    // Ceiling a failed division can raise a target's value to (MC-48). Derived from the same curve
    // as getTargetValueByLevel rather than kept as a standalone constant, so the two can never drift
    // apart: FAILED_GROWTH_FACTOR 1 would make the ceiling equal that curve's own maximum, and
    // anything below 1 would turn a failed division into a gift instead of a debt.
    //
    // Playability was checked the way the old flat 1,000,000 ceiling was (SessionHelperImplTest):
    // a target starting exactly at the ceiling, played well - a player who reads the upcoming
    // operation before firing never fires a division that will fail, tapping the remainder to a
    // multiple of the digit first, then closing what's left with plain taps at 8/sec. Three such
    // tap-adjusted divisions with the real level-scaled digit range, 2000 runs per level: level 1
    // closes within 12 taps every run (median 6); level 10 within 12 (median 7); level 30 within 20
    // (median 10); level 50 within 22 (median 14) - all under three seconds of tapping, comfortably
    // inside any level's fall.
    override fun failedGrowthCap(level: Int): Int =
        FAILED_GROWTH_FACTOR * (initialTargetValueRange.last + level * TARGET_VALUE_LEVEL_SCALE)
}
