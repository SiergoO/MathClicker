package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.game.objectmapper.decrementValue
import com.sdamashchuk.matharcade.core.game.scoring.performOperation
import com.sdamashchuk.matharcade.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SAMPLE_ITERATIONS = 200
private const val SAMPLE_LEVEL = 50

// Mirrors the constants in SessionHelperImpl (private there) so these tests fail when production
// stops scaling, stops flooring, or reverses direction (MC-52/MC-73 mutations) rather than passing
// by construction because both sides share one formula.
private const val FLIGHT_BASE_MS = 7600
private const val FLIGHT_STEP_MS = 90
private const val FLIGHT_FLOOR_MS = 3500
private const val FLIGHT_SPEED_SPREAD = 0.2f
private const val MIN_SPEED_MULTIPLIER = 1f - FLIGHT_SPEED_SPREAD
private const val FINISH_SPACING_FRACTION = 0.38f
private const val MIN_FINISH_SPACING_MS = 1800
private const val MIN_TOTAL_LIFE_LOSS_MS = 3500

private fun expectedBaseFlightMs(level: Int): Int =
    (FLIGHT_BASE_MS - level * FLIGHT_STEP_MS).coerceAtLeast(FLIGHT_FLOOR_MS)

private fun expectedMaxFlightMs(level: Int): Int = ceil(expectedBaseFlightMs(level) / MIN_SPEED_MULTIPLIER).toInt()

private fun expectedFlightRange(level: Int): IntRange =
    IntRange(expectedBaseFlightMs(level), expectedMaxFlightMs(level))

private fun expectedFinishSpacingMs(level: Int): Int =
    (expectedBaseFlightMs(level) * FINISH_SPACING_FRACTION).roundToInt().coerceAtLeast(MIN_FINISH_SPACING_MS)

// Captured once from a real run against Random(PINNED_SEED) and hardcoded, the same way
// failedGrowthCap's own pinned test works: proof the formula, not just its range, is unchanged.
private const val PINNED_SEED = 2024L
private const val PINNED_FLIGHT_LEVEL_1 = 8431
private const val PINNED_FLIGHT_LEVEL_10 = 7521
private const val PINNED_FLIGHT_LEVEL_30 = 5501
private const val PINNED_FLIGHT_LEVEL_55 = 3929

private const val SIMULATION_RUNS = 2000
private const val SIMULATION_DIVISIONS = 3
private const val SIMULATION_TAP_RATE_PER_SECOND = 8
private const val SIMULATION_TAP_BUDGET_SECONDS = 3

// MC-60: profile is measured against the level's own division digit range (getDivisionDigitByLevel),
// not a fixed divisor - the spec's own "ready now at ÷5 but not ÷2" example is exactly why a single
// divisor per level would misrepresent the real board.
private const val PROFILE_SAMPLE_ITERATIONS = 10_000

// Percentage points. n=10_000 keeps binomial sampling noise for any of these bucket sizes well under
// 1pp on its own; the real source of slack is clamping - level 1's own division range starts at 2, so
// a quarter of its draws (digit 2, only costs 0 or 1 exist) can't realize the "2-3 taps" bucket at all
// and fall back into "1 tap" instead. Measured once against PROFILE_SEED: the largest observed gap
// from the table was ~3pp (level 1's "1 tap"/"2-3 taps" pair, from that same clamp). 5 is generous
// against that without being wide enough to pass mutation M1/M2/M5 (see the mutation ledger in
// MC-60's spec).
private const val PROFILE_TOLERANCE_PP = 5
private const val PROFILE_SEED = 4242L

// MC-80: subtraction's own axis is trap share, not division's four tap-cost buckets - n=10_000
// keeps binomial noise for a share this size well under 1pp, so this tolerance is mostly headroom
// against the 15pp gaps between SubtractionTrapCost's own profile rows, not sampling slack.
private const val SUBTRACTION_TRAP_SAMPLE_ITERATIONS = 10_000

// At n=10_000 the sampling error on a share is well under a point, so 3 is loose enough never
// to flake and tight enough that any real profile edit - the smallest step here is 15pp - fails.
private const val SUBTRACTION_TRAP_TOLERANCE_PP = 3

private data class CostBuckets(
    val readyNow: Int,
    val oneTap: Int,
    val twoOrThreeTaps: Int,
    val fourPlusTaps: Int,
) {
    val percentages: List<Int>
        get() =
            listOf(readyNow, oneTap, twoOrThreeTaps, fourPlusTaps)
                .map { it * 100 / (readyNow + oneTap + twoOrThreeTaps + fourPlusTaps) }
}

// Draws PROFILE_SAMPLE_ITERATIONS (divisor, value) pairs the way a real level-up does - a fresh
// division digit per target, via the same getDivisionDigitByLevel a target's own currentOperationDigit
// would come from - and buckets each by real taps-to-prepare (value % divisor, the same arithmetic
// targetClicked's own decrementValue performs one tap at a time).
private fun sampleCostBuckets(
    level: Int,
    seed: Long,
): CostBuckets {
    val seededHelper = SessionHelperImpl(random = Random(seed))
    var readyNow = 0
    var oneTap = 0
    var twoOrThreeTaps = 0
    var fourPlusTaps = 0
    repeat(PROFILE_SAMPLE_ITERATIONS) {
        val divisor = seededHelper.getDivisionDigitByLevel(level)
        val value = seededHelper.getTargetValueByLevel(level, divisor)
        when (val cost = value % divisor) {
            0 -> readyNow++
            1 -> oneTap++
            in 2..3 -> twoOrThreeTaps++
            else -> fourPlusTaps++
        }
    }
    return CostBuckets(readyNow, oneTap, twoOrThreeTaps, fourPlusTaps)
}

private fun assertProfileMatches(
    level: Int,
    expectedPercentages: List<Int>,
) {
    val actual = sampleCostBuckets(level, PROFILE_SEED).percentages
    val labels = listOf("ready now", "1 tap", "2-3 taps", "4+ taps")
    for (i in actual.indices) {
        assertTrue(
            kotlin.math.abs(actual[i] - expectedPercentages[i]) <= PROFILE_TOLERANCE_PP,
            "level $level ${labels[i]}: expected ~${expectedPercentages[i]}%, measured ${actual[i]}%",
        )
    }
}

// MC-80's own axis: the fraction of draws that land below operationDigit at all, at a fixed digit
// so the share measured is the profile's, not a mix of digits with different trap headroom.
private fun sampleSubtractionTrapShare(
    level: Int,
    operationDigit: Int,
    seed: Long,
): Int {
    val seededHelper = SessionHelperImpl(random = Random(seed))
    var traps = 0
    repeat(SUBTRACTION_TRAP_SAMPLE_ITERATIONS) {
        if (seededHelper.getSubtractionTargetValueByLevel(level, operationDigit) < operationDigit) traps++
    }
    return traps * 100 / SUBTRACTION_TRAP_SAMPLE_ITERATIONS
}

private fun failedGrowthCapTarget(value: Int) =
    Target(
        id = 1,
        relatedFieldId = 0,
        columnId = 0,
        value = value,
        appearsAtMs = 0,
        finishesAtMs = 0,
    )

class SessionHelperImplTest {
    private val helper = SessionHelperImpl()

    @Test
    fun `the starting difficulty is the one the game was balanced around`() {
        // Literals on purpose. Every other test here derives its expectation from these same
        // properties, so none of them can notice a constant changing — a ten-fold rise in starting
        // target value would leave the suite green. This test is the only thing pinning the balance.
        assertEquals(1..9, helper.initialTargetValueRange)
        assertEquals(7600..9500, helper.initialTargetFlightTimeMsRange)
        assertEquals(9..14, helper.initialTargetAmountRange)
        assertEquals(2..3, helper.initialDivisionValueRange)
        assertEquals(1..3, helper.initialSubtractionValueRange)
        assertEquals(1..999, helper.levelRange)
    }

    @Test
    fun `the narrow level-one ranges are produced in full - not merely stayed within`() {
        // Asserting membership cannot catch a range that gets narrower. Where the range is small
        // enough to be sampled exhaustively, assert the whole set instead. The wide ranges (flight
        // time) cannot be pinned this way while the helper returns a random value with no way to ask
        // it for the range it drew from; MC-6 should add that seam.
        val level = 0

        assertEquals((2..3).toSet(), sampled { helper.getDivisionDigitByLevel(level) })
        assertEquals((1..3).toSet(), sampled { helper.getSubtractionDigitByLevel(level) })
        assertEquals((9..14).toSet(), sampled { helper.getTargetAmountByLevel(level) })
    }

    private fun sampled(draw: () -> Int): Set<Int> = buildSet { repeat(SAMPLE_ITERATIONS) { add(draw()) } }

    @Test
    fun `flight time and target value and target amount all scale with level per the difficulty curve`() {
        val flightRange = expectedFlightRange(SAMPLE_LEVEL)
        val valueRange =
            IntRange(
                helper.initialTargetValueRange.first + SAMPLE_LEVEL / 10,
                helper.initialTargetValueRange.last + SAMPLE_LEVEL * 2,
            )
        val amountRange =
            IntRange(
                helper.initialTargetAmountRange.first + SAMPLE_LEVEL / 5,
                helper.initialTargetAmountRange.last + SAMPLE_LEVEL / 3,
            )

        repeat(SAMPLE_ITERATIONS) {
            assertTrue(helper.getTargetFlightTimeMs(SAMPLE_LEVEL) in flightRange)
            assertTrue(helper.getTargetValueByLevel(SAMPLE_LEVEL, operationDigit = 1) in valueRange)
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
    fun `getTargetSpeedByLevel is the exact inverse of the base - un-spread - flight curve`() {
        for (level in listOf(1, 10, 30, 55, 999)) {
            val expectedSpeed = 1f / expectedBaseFlightMs(level)
            assertEquals(expectedSpeed, helper.getTargetSpeedByLevel(level))
        }
    }

    @Test
    fun `getFinishSpacingMsByLevel never draws from random - repeated calls at the same level agree`() {
        // Deterministic by construction is the whole MC-73 fix: nothing here can vary between calls,
        // unlike the pre-MC-73 wave gap this replaces.
        for (level in listOf(1, 10, 30, 55, 999)) {
            val first = helper.getFinishSpacingMsByLevel(level)
            repeat(SAMPLE_ITERATIONS) {
                assertEquals(first, helper.getFinishSpacingMsByLevel(level))
            }
        }
    }

    @Test
    fun `getFinishSpacingMsByLevel is pinned to exact values at levels 1 10 30 and 55`() {
        assertEquals(expectedFinishSpacingMs(1), helper.getFinishSpacingMsByLevel(1))
        assertEquals(expectedFinishSpacingMs(10), helper.getFinishSpacingMsByLevel(10))
        assertEquals(expectedFinishSpacingMs(30), helper.getFinishSpacingMsByLevel(30))
        assertEquals(expectedFinishSpacingMs(55), helper.getFinishSpacingMsByLevel(55))
    }

    @Test
    fun `getOpeningOffsetMsByLevel is at least the level's own maximum possible flight time`() {
        // The MC-73 guard against a target starting mid-fall: appearsAtMs(0) = gameTimeMs +
        // openingOffset - flightTime(0), so an offset below the level's own worst-case flight would
        // let the very first target's appearsAtMs land before gameTimeMs.
        for (level in helper.levelRange step 37) {
            assertTrue(
                helper.getOpeningOffsetMsByLevel(level) >= expectedMaxFlightMs(level),
                "level $level: opening offset ${helper.getOpeningOffsetMsByLevel(
                    level,
                )} is below the max flight ${expectedMaxFlightMs(level)}",
            )
        }
    }

    // The invariant MC-73 exists to close, restated by MC-94: three breakouts (INITIAL_LIFE_COUNT - 1
    // gaps) must take real time. MC-73 measured that against the level's own flight time, which also
    // pinned the board to ~1.4 targets on screen; what the player actually needs is a reaction window,
    // which is wall-clock and independent of how long this level's fall happens to be. Looped over the
    // whole range the same way "no level from 1 to 999 produces an empty range" already does below,
    // for the same reason - a spot check can't catch a curve that only crosses somewhere it wasn't
    // asked about (the pre-MC-52 precedent this file was already written to guard against).
    @Test
    fun `losing every life takes real time at every level - not milliseconds`() {
        for (level in helper.levelRange) {
            val spanned = (INITIAL_LIFE_COUNT - 1) * helper.getFinishSpacingMsByLevel(level)
            assertTrue(
                spanned >= MIN_TOTAL_LIFE_LOSS_MS,
                "level $level: (INITIAL_LIFE_COUNT - 1) * spacing = $spanned < $MIN_TOTAL_LIFE_LOSS_MS",
            )
        }
    }

    // MC-94's own half of the change: the spacing had to come down for the board to hold more than
    // one falling target at a time, and that is the number this pins. Flight time divided by spacing
    // is how many targets are in the air at once.
    @Test
    fun `the early levels keep more than two targets in the air at once`() {
        for (level in listOf(1, 5, 10, 20, 30)) {
            val concurrent =
                expectedBaseFlightMs(level).toFloat() / helper.getFinishSpacingMsByLevel(level)
            assertTrue(
                concurrent > 2f,
                "level $level: only $concurrent targets in the air, expected more than two",
            )
        }
    }

    @Test
    fun `no level from 1 to 999 produces an empty range for the flight curve`() {
        // The class of failure LEVEL_MAX exists for: pre-MC-52, the lifetime curve's own min-max
        // thresholds crossed past level 1334 and IntRange.random() threw. Flooring the base before
        // dividing by the spread (mutation M5 undoes exactly this) makes the range structurally
        // unable to go empty at any level - proven here by actually calling the curve at every level
        // in range rather than only reasoning about the formula.
        var previousFloor = Int.MAX_VALUE
        for (level in helper.levelRange) {
            val flightRange = expectedFlightRange(level)

            assertTrue(helper.getTargetFlightTimeMs(level) in flightRange)

            // Never gentler than the level below it (mutation M5): the floor is non-increasing.
            assertTrue(flightRange.first <= previousFloor)
            previousFloor = flightRange.first
        }
    }

    @Test
    fun `flight time is pinned to an exact seeded number at level 1`() {
        assertEquals(
            PINNED_FLIGHT_LEVEL_1,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetFlightTimeMs(1),
        )
    }

    @Test
    fun `flight time is pinned to an exact seeded number at level 10`() {
        assertEquals(
            PINNED_FLIGHT_LEVEL_10,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetFlightTimeMs(10),
        )
    }

    @Test
    fun `flight time is pinned to an exact seeded number at level 30`() {
        assertEquals(
            PINNED_FLIGHT_LEVEL_30,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetFlightTimeMs(30),
        )
    }

    @Test
    fun `flight time is pinned to an exact seeded number at level 55`() {
        assertEquals(
            PINNED_FLIGHT_LEVEL_55,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetFlightTimeMs(55),
        )
    }

    @Test
    fun `the failed growth ceiling rises with level and never dips below that level's own value maximum`() {
        var previousCap = 0
        for (level in listOf(1, 5, 10, 30, 50, 200, 500, 999)) {
            val cap = helper.failedGrowthCap(level)
            val normalMax = helper.initialTargetValueRange.last + level * 2
            assertTrue(cap >= normalMax, "level $level: ceiling $cap fell below the normal maximum $normalMax")
            assertTrue(cap > previousCap, "level $level: ceiling $cap did not grow past $previousCap")
            previousCap = cap
        }
    }

    @Test
    fun `the failed growth ceiling is pinned to exact values at levels 1 10 30 and 50`() {
        assertEquals(44, helper.failedGrowthCap(1))
        assertEquals(116, helper.failedGrowthCap(10))
        assertEquals(276, helper.failedGrowthCap(30))
        assertEquals(436, helper.failedGrowthCap(50))
    }

    @Test
    fun `a target already at the ceiling stays there after another failed division`() {
        val level = 1
        val cap = helper.failedGrowthCap(level)
        // 7 does not divide 92, so this is a failed division: value * 7 would overshoot the
        // ceiling by a wide margin without the clamp.
        val targets = listOf(failedGrowthCapTarget(cap))

        val outcome =
            targets.performOperation(OperationSign.DIVISION, currentOperationDigit = 7, cap, gameTimeMs = 0)

        assertEquals(0, outcome.scored)
        assertTrue(outcome.failedCount > 0)
        assertEquals(cap, outcome.targets.first().value)
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
                    targets = targets.performOperation(OperationSign.DIVISION, digit, cap, gameTimeMs = 0).targets
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

    @Test
    fun `MC-60 - preparation cost profile matches the level 1 table at generation`() {
        // MC-93 narrowed level 1's divisors to 2..3, so half of that level's draws have a maxCost
        // of 1 and the authored "2-3 taps" bucket collapses into the 1-tap one (see PreparationCost's
        // costInClampedRange). The realized profile is what a player meets, so it is what this pins -
        // the authored table stays PREPARATION_PROFILE_LEVEL_1's (25, 65, 10, 0).
        assertProfileMatches(level = 1, expectedPercentages = listOf(25, 70, 5, 0))
    }

    @Test
    fun `MC-60 - preparation cost profile matches the level 10 table at generation`() {
        assertProfileMatches(level = 10, expectedPercentages = listOf(25, 45, 30, 0))
    }

    @Test
    fun `MC-60 - preparation cost profile matches the level 30 table at generation`() {
        assertProfileMatches(level = 30, expectedPercentages = listOf(30, 30, 30, 10))
    }

    @Test
    fun `MC-60 - the ready-now bucket is never emptied out - the rejected every-target-must-prepare design`() {
        // Named for mutation M5 in the spec: zeroing this bucket is exactly the individually-rigged
        // version the owner rejected. A tight lower bound, not just "> 0" - the table promises at
        // least 25% at every tested level, so anything far below that is already a different curve.
        for (level in listOf(1, 10, 30)) {
            val buckets = sampleCostBuckets(level, PROFILE_SEED)
            val readyNowPercent = buckets.readyNow * 100 / PROFILE_SAMPLE_ITERATIONS
            assertTrue(
                readyNowPercent >= 20,
                "level $level: ready-now bucket measured $readyNowPercent%, expected at least 20%",
            )
        }
    }

    @Test
    fun `MC-60 - getTargetValueByLevel is deterministic for a seeded Random`() {
        val level = 30
        val operationDigit = 6
        val first = SessionHelperImpl(random = Random(PINNED_SEED)).getTargetValueByLevel(level, operationDigit)
        val second = SessionHelperImpl(random = Random(PINNED_SEED)).getTargetValueByLevel(level, operationDigit)
        assertEquals(first, second)
    }

    @Test
    fun `MC-60 - getTargetValueByLevel is pinned to an exact seeded number at level 1`() {
        // Literal, not the production formula recomputed - see failedGrowthCap's own pinned tests for
        // why (a test that re-derives the formula can't catch the formula itself drifting).
        assertEquals(
            6,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetValueByLevel(level = 1, operationDigit = 5),
        )
    }

    @Test
    fun `MC-60 - getTargetValueByLevel is pinned to an exact seeded number at level 30`() {
        assertEquals(
            9,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetValueByLevel(level = 30, operationDigit = 8),
        )
    }

    @Test
    fun `MC-60 - the generated value never leaves the level's own range after the shift`() {
        val seededHelper = SessionHelperImpl(random = Random(PINNED_SEED))
        repeat(SAMPLE_ITERATIONS) {
            for (level in listOf(1, 10, 30, 55, 999)) {
                val minThreshold = seededHelper.initialTargetValueRange.first + level / 10
                val maxThreshold = seededHelper.initialTargetValueRange.last + level * 2
                val expectedRange = IntRange(minThreshold, maxThreshold)
                val divisor = seededHelper.getDivisionDigitByLevel(level)
                val value = seededHelper.getTargetValueByLevel(level, divisor)
                assertTrue(value in expectedRange, "level $level: value $value left $expectedRange")
            }
        }
    }

    @Test
    fun `MC-60 - no level from 1 to 999 produces an empty range or an out-of-range value after the shift`() {
        val seededHelper = SessionHelperImpl(random = Random(PINNED_SEED))
        for (level in seededHelper.levelRange) {
            val minThreshold = seededHelper.initialTargetValueRange.first + level / 10
            val maxThreshold = seededHelper.initialTargetValueRange.last + level * 2
            val expectedRange = IntRange(minThreshold, maxThreshold)
            val divisor = seededHelper.getDivisionDigitByLevel(level)
            val value = seededHelper.getTargetValueByLevel(level, divisor)
            assertTrue(value in expectedRange, "level $level: value $value left $expectedRange")
        }
    }

    @Test
    fun `MC-80 - subtraction trap share rises with level and stays well under half at level 1`() {
        val digit = 5
        var previousShare = -1
        for (level in listOf(1, 10, 30, 50)) {
            val share = sampleSubtractionTrapShare(level, digit, PROFILE_SEED)
            assertTrue(
                share >= previousShare,
                "level $level: trap share $share% fell below the previous level's $previousShare%",
            )
            if (level == 1) {
                assertTrue(share < 50, "level 1: trap share $share% is not well under half")
            }
            previousShare = share
        }
    }

    // Monotonicity alone lets the middle of the curve drift: moving level 10 from 35% to 45% keeps
    // the order intact and passes every other assertion here, which is exactly the strength the
    // deleted MC-70 bucket tests used to provide. Each step is pinned to the declared profile.
    @Test
    fun `MC-80 - each level's subtraction trap share matches its declared profile`() {
        val digit = 5
        val expectedByLevel = listOf(1 to 20, 10 to 35, 30 to 50, 50 to 65)
        for ((level, expected) in expectedByLevel) {
            val share = sampleSubtractionTrapShare(level, digit, PROFILE_SEED)
            assertTrue(
                kotlin.math.abs(share - expected) <= SUBTRACTION_TRAP_TOLERANCE_PP,
                "level $level: trap share $share% is not within $SUBTRACTION_TRAP_TOLERANCE_PP pp of $expected%",
            )
        }
    }

    @Test
    fun `MC-80 - subtraction trap values are spread across the digit's range - not concentrated on 1`() {
        // digit 10 gives traps nine possible values (1..9); uniform would put ~11% on each, so a
        // share this far below half proves the value isn't collapsing to 1 the way desiredPreparationCost's
        // oneTapPercent bucket did.
        val level = 50
        val digit = 10
        val seededHelper = SessionHelperImpl(random = Random(PROFILE_SEED))
        var traps = 0
        var onesAmongTraps = 0
        repeat(SUBTRACTION_TRAP_SAMPLE_ITERATIONS) {
            val value = seededHelper.getSubtractionTargetValueByLevel(level, digit)
            if (value < digit) {
                traps++
                if (value == 1) onesAmongTraps++
            }
        }
        val onePercentOfTraps = onesAmongTraps * 100 / traps
        assertTrue(onePercentOfTraps < 25, "value 1 is $onePercentOfTraps% of traps, still concentrated")
    }

    @Test
    fun `MC-70 - every subtraction value is a genuine trap below the digit or ready within the level range`() {
        val seededHelper = SessionHelperImpl(random = Random(PINNED_SEED))
        for (level in listOf(1, 10, 30, 55, 999)) {
            val minThreshold = seededHelper.initialTargetValueRange.first + level / 10
            val maxThreshold = seededHelper.initialTargetValueRange.last + level * 2
            repeat(SAMPLE_ITERATIONS) {
                val digit = seededHelper.getSubtractionDigitByLevel(level)
                val value = seededHelper.getSubtractionTargetValueByLevel(level, digit)
                if (value < digit) {
                    assertTrue(value in 1 until digit, "level $level: trap value $value not below digit $digit")
                } else {
                    val readyLower = maxOf(minThreshold, digit)
                    assertTrue(
                        value in readyLower..maxThreshold,
                        "level $level: ready value $value outside $readyLower..$maxThreshold (digit $digit)",
                    )
                }
            }
        }
    }

    @Test
    fun `MC-70 - getSubtractionTargetValueByLevel is deterministic for a seeded Random`() {
        val level = 30
        val operationDigit = 6
        val first =
            SessionHelperImpl(
                random = Random(PINNED_SEED),
            ).getSubtractionTargetValueByLevel(level, operationDigit)
        val second =
            SessionHelperImpl(
                random = Random(PINNED_SEED),
            ).getSubtractionTargetValueByLevel(level, operationDigit)
        assertEquals(first, second)
    }

    @Test
    fun `MC-80 - getSubtractionTargetValueByLevel is pinned to an exact seeded number at level 1`() {
        // Re-pinned for MC-80: SubtractionTrapCost draws a different random sequence than the
        // desiredPreparationCost it replaces, so this is a fresh capture, not the old value carried over.
        assertEquals(
            6,
            SessionHelperImpl(
                random = Random(PINNED_SEED),
            ).getSubtractionTargetValueByLevel(level = 1, operationDigit = 3),
        )
    }

    @Test
    fun `MC-80 - getSubtractionTargetValueByLevel is pinned to an exact seeded number at level 30`() {
        assertEquals(
            31,
            SessionHelperImpl(
                random = Random(PINNED_SEED),
            ).getSubtractionTargetValueByLevel(level = 30, operationDigit = 8),
        )
    }
}
