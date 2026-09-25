package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

// FLIGHT_BASE_MS and baseFlightMsByLevel live in FlightCurve.kt, top-level in this same package: this
// class was already at detekt's TooManyFunctions ceiling before getSubtractionTargetValueByLevel
// (MC-70), and baseFlightMsByLevel reads none of this class's state - not even its Random - so it
// moved out rather than raising the threshold.
class SessionHelperImpl(
    private val random: Random = Random.Default,
) : SessionHelper {
    companion object {
        private const val LEVEL_MIN = 1
        private const val LEVEL_MAX = 999

        private const val INITIAL_TARGET_VALUE_MIN = 1
        private const val INITIAL_TARGET_VALUE_MAX = 20

        // MC-73: the owner's 0...-20% speed spread. A speed multiplier drawn from
        // (MIN_SPEED_MULTIPLIER, 1] lands flight time in [base, base / MIN_SPEED_MULTIPLIER] = [base,
        // base * 1.25] - about the same 26% the old fixed-ms spread gave, but spent on how long a
        // target is on screen rather than on when it finishes.
        private const val FLIGHT_SPEED_SPREAD = 0.2f
        private const val MIN_SPEED_MULTIPLIER = 1f - FLIGHT_SPEED_SPREAD

        // Fraction of the level's own base flight time. This is the whole fix: three lives lost to
        // breakouts (INITIAL_LIFE_COUNT - 1 gaps between them) must together span at least one
        // level's worst-case flight time, i.e. FINISH_SPACING_FRACTION * (INITIAL_LIFE_COUNT - 1) >=
        // 1 / MIN_SPEED_MULTIPLIER, so FINISH_SPACING_FRACTION >= 0.625 for INITIAL_LIFE_COUNT 3. 0.7
        // keeps a deliberate margin over that floor against integer rounding -
        // SessionHelperImplTest's invariant test proves this holds at every level, not just checked
        // once here. Never lower this without re-checking that test.
        private const val FINISH_SPACING_FRACTION = 0.7f

        // Authoring unit for getTargetSpeedByLevel: one whole fall, so speed is "fraction of a fall
        // covered per millisecond" rather than a unit tied to any real screen dimension.
        private const val SPAN_UNITS = 1f

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

    // Unscaled, like initialTargetValueRange above - the raw base/ceiling this curve is built from,
    // not getTargetFlightTimeMs(1)'s own (slightly tighter) output.
    override val initialTargetFlightTimeMsRange =
        IntRange(FLIGHT_BASE_MS, ceil(FLIGHT_BASE_MS / MIN_SPEED_MULTIPLIER).toInt())
    override val initialTargetAmountRange = IntRange(INITIAL_TARGET_AMOUNT_MIN, INITIAL_TARGET_AMOUNT_MAX)
    override val initialDivisionValueRange = IntRange(INITIAL_DIVISION_VALUE_MIN, INITIAL_DIVISION_VALUE_MAX)
    override val initialSubtractionValueRange = IntRange(INITIAL_SUBTRACTION_VALUE_MIN, INITIAL_SUBTRACTION_VALUE_MAX)

    init {
        // MC-73's whole fix, checked once here rather than only in a test: three breakouts
        // (INITIAL_LIFE_COUNT - 1 gaps) must together span at least one flight time, or the bug this
        // task exists for - three lives lost within milliseconds - comes back. A future edit that
        // narrows FINISH_SPACING_FRACTION below the floor fails fast at construction, not silently
        // in production.
        check(FINISH_SPACING_FRACTION * (INITIAL_LIFE_COUNT - 1) * MIN_SPEED_MULTIPLIER >= 1f) {
            "FINISH_SPACING_FRACTION is too small: (INITIAL_LIFE_COUNT - 1) * spacing must be >= the level's max flight time"
        }
    }

    override fun getTargetSpeedByLevel(level: Int): Float = SPAN_UNITS / baseFlightMsByLevel(level)

    /**
     * Flight time is speed's inverse, with the per-target spread applied as a multiplier on speed
     * (0...-20%, FLIGHT_SPEED_SPREAD) rather than as a separate draw on time. Recomputed from
     * baseFlightMsByLevel directly rather than round-tripping through the Float
     * getTargetSpeedByLevel returns, so maxFlightMsByLevel's ceil() stays a provably safe bound
     * regardless of Float rounding.
     * @param level
     * @return random flight time in [base, base / MIN_SPEED_MULTIPLIER] for the level.
     */
    override fun getTargetFlightTimeMs(level: Int): Int {
        val speedMultiplier = 1f - random.nextFloat() * FLIGHT_SPEED_SPREAD
        return (baseFlightMsByLevel(level) / speedMultiplier).roundToInt()
    }

    // Deterministic by construction - no Random draw - which is the entire fix MC-73 exists for: the
    // distance between two consecutive finishesAtMs is exactly this value, and no per-target flight
    // spread can touch it because it is never built out of one.
    override fun getFinishSpacingMsByLevel(level: Int): Int =
        (baseFlightMsByLevel(level) * FINISH_SPACING_FRACTION).roundToInt()

    // The level's own worst-case flight time - ceil, not round, so getTargetFlightTimeMs's actual
    // draw (roundToInt of a value strictly below base / MIN_SPEED_MULTIPLIER, since the multiplier
    // never reaches MIN_SPEED_MULTIPLIER exactly) can never round up past this. At least that bound
    // is what keeps finishesAtMs(0) - flightTime(0) - and every later target's, since the opening
    // offset only ever adds headroom - from ever landing before Field.gameTimeMs.
    override fun getOpeningOffsetMsByLevel(level: Int): Int =
        ceil(baseFlightMsByLevel(level) / MIN_SPEED_MULTIPLIER).toInt()

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
     * Calculates the target value for a subtraction-armed board (MC-70). Subtraction succeeds
     * whenever value >= digit - not modular like division, so there is no remainder to shift toward.
     * Instead, desiredPreparationCost's level profile picks the fraction of targets that are genuine
     * traps: a value below the digit outright, which tapping can only shrink further and never lift
     * back to ready, so it must be cleared by hand rather than fired on. The rest are generated ready
     * to fire immediately.
     * @param level
     * @param operationDigit the subtraction digit the value is generated against.
     * @return random target value, ready or a trap according to the level's preparation profile.
     */
    override fun getSubtractionTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ): Int {
        val minThreshold = initialTargetValueRange.first + level / 10
        val maxThreshold = initialTargetValueRange.last + level * TARGET_VALUE_LEVEL_SCALE
        return if (operationDigit <= 1) {
            // Every target value is at least 1, so nothing can ever fall below this digit.
            IntRange(minThreshold, maxThreshold).random(random)
        } else {
            val desiredCost = desiredPreparationCost(level, operationDigit, random)
            if (desiredCost == 0) {
                val readyLower = maxOf(minThreshold, operationDigit).coerceAtMost(maxThreshold)
                IntRange(readyLower, maxThreshold).random(random)
            } else {
                // A trap: desiredCost is spent directly as the value, not a shift - the number of
                // plain taps this target costs to clear by hand, since firing on it would only fail
                // and grow it.
                desiredCost
            }
        }
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
