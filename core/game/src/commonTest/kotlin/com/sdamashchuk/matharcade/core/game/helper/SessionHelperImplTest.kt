package com.sdamashchuk.matharcade.core.game.helper

import com.sdamashchuk.matharcade.core.game.objectmapper.decrementValue
import com.sdamashchuk.matharcade.core.game.scoring.performOperation
import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SAMPLE_ITERATIONS = 200
private const val SAMPLE_LEVEL = 50

// Mirrors the constants in SessionHelperImpl (private there) so these tests fail when production
// stops scaling, stops flooring, or reverses direction (MC-52 mutations M1-M5) rather than passing
// by construction because both sides share one formula.
private const val LIFETIME_BASE_MS = 9500
private const val LIFETIME_STEP_MS = 110
private const val LIFETIME_FLOOR_MS = 3500
private const val LIFETIME_SPREAD_MS = 2500

private const val WAVE_GAP_BASE_MS = 3800
private const val WAVE_GAP_STEP_MS = 55
private const val WAVE_GAP_FLOOR_MS = 1400
private const val WAVE_GAP_SPREAD_MS = 800

private fun expectedLifetimeRange(level: Int): IntRange {
    val floor = (LIFETIME_BASE_MS - level * LIFETIME_STEP_MS).coerceAtLeast(LIFETIME_FLOOR_MS)
    return IntRange(floor, floor + LIFETIME_SPREAD_MS)
}

private fun expectedWaveGapRange(level: Int): IntRange {
    val floor = (WAVE_GAP_BASE_MS - level * WAVE_GAP_STEP_MS).coerceAtLeast(WAVE_GAP_FLOOR_MS)
    return IntRange(floor, floor + WAVE_GAP_SPREAD_MS)
}

// Captured once from a real run against Random(PINNED_SEED) and hardcoded, the same way
// failedGrowthCap's own pinned test works: proof the formula, not just its range, is unchanged.
private const val PINNED_SEED = 2024L
private const val PINNED_LIFETIME_LEVEL_1 = 11216
private const val PINNED_LIFETIME_LEVEL_10 = 10226
private const val PINNED_LIFETIME_LEVEL_30 = 8026
private const val PINNED_LIFETIME_LEVEL_55 = 5326
private val PINNED_WAVE_DELAYS_LEVEL_1 = listOf(0, 1094, 2076, 3078, 3817, 5221, 5994, 7758)
private val PINNED_WAVE_DELAYS_LEVEL_10 = listOf(0, 970, 1830, 2706, 3322, 4602, 5251, 6891)
private val PINNED_WAVE_DELAYS_LEVEL_30 = listOf(0, 695, 1280, 1881, 2222, 3227, 3601, 4966)
private val PINNED_WAVE_DELAYS_LEVEL_55 = listOf(0, 508, 904, 1317, 1472, 2290, 2475, 3655)

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
        assertEquals(9500..12000, helper.initialTargetLifetimeMsRange)
        assertEquals(3800..4600, helper.initialTargetAppearanceDelayMsRange)
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
        val lifetimeRange = expectedLifetimeRange(SAMPLE_LEVEL)
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
    fun `only the very first target gets zero delay - every level not just level one`() {
        // Pre-MC-52, every id in the first wave (0..GAME_COLUMN_COUNT - 1) got delay zero: the
        // whole opening wave fell at once, which a device run measured closing a fresh session in
        // ~28s with no input at all (MC-52 spec). Only id 0 is allowed that now - every other wave's
        // own first id is offset by that wave's own gap, covered separately below.
        for (level in listOf(1, 10, 30, 55, 999)) {
            assertEquals(0, helper.getTargetAppearanceDelayMsByIdAndLevel(0, level))
        }
    }

    @Test
    fun `MC-52 - every non-leading id in a wave is staggered - never sharing the wave's own zero`() {
        // Structural, not probabilistic: a positionInWave above zero always multiplies a wave gap
        // that can never drop below WAVE_GAP_FLOOR_MS, so this can't land on zero by an unlucky draw
        // (mutation M1 - the stagger term removed - is what makes it zero again).
        repeat(SAMPLE_ITERATIONS) {
            for (level in listOf(1, 10, 30, 55, 999)) {
                for (id in 1 until GAME_COLUMN_COUNT) {
                    assertTrue(helper.getTargetAppearanceDelayMsByIdAndLevel(id, level) > 0)
                }
            }
        }
    }

    @Test
    fun `MC-52 - the four targets of level one's opening wave enter as a strict staircase`() {
        // level 1's wave gap floor (3745ms) comfortably clears the margin the staircase needs to stay
        // strictly ordered id 0 through 3 regardless of the random draw - see getTargetAppearanceDelayMsByIdAndLevel's
        // own reasoning. Not true at every level: once the wave gap curve is at its floor (level 44+)
        // position 2 and 3 can theoretically tie or invert, which is why this is pinned to level 1
        // rather than asserted generally.
        repeat(SAMPLE_ITERATIONS) {
            val delays = (0 until GAME_COLUMN_COUNT).map { id -> helper.getTargetAppearanceDelayMsByIdAndLevel(id, 1) }
            assertEquals(delays.sorted(), delays)
            assertEquals(delays.toSet().size, delays.size)
        }
    }

    @Test
    fun `the second wave's first target is delayed by exactly one wave gap`() {
        val waveGapRange = expectedWaveGapRange(SAMPLE_LEVEL)
        repeat(SAMPLE_ITERATIONS) {
            assertTrue(helper.getTargetAppearanceDelayMsByIdAndLevel(GAME_COLUMN_COUNT, SAMPLE_LEVEL) in waveGapRange)
        }
    }

    @Test
    fun `no level from 1 to 999 produces an empty range for the lifetime or wave-gap curve`() {
        // The class of failure LEVEL_MAX exists for: pre-MC-52, getTargetLifetimeMsByLevel's own
        // min-max thresholds crossed past level 1334 and IntRange.random() threw. Flooring the
        // minimum before adding the spread (mutation M5 undoes exactly this) makes the range
        // structurally unable to go empty at any level - proven here by actually calling both curves
        // at every level in range rather than only reasoning about the formula.
        var previousLifetimeFloor = Int.MAX_VALUE
        var previousWaveGapFloor = Int.MAX_VALUE
        for (level in helper.levelRange) {
            val lifetimeRange = expectedLifetimeRange(level)
            val waveGapRange = expectedWaveGapRange(level)

            assertTrue(helper.getTargetLifetimeMsByLevel(level) in lifetimeRange)
            assertTrue(helper.getTargetAppearanceDelayMsByIdAndLevel(GAME_COLUMN_COUNT, level) in waveGapRange)

            // Never gentler than the level below it (mutation M5): both floors are non-increasing.
            assertTrue(lifetimeRange.first <= previousLifetimeFloor)
            assertTrue(waveGapRange.first <= previousWaveGapFloor)
            previousLifetimeFloor = lifetimeRange.first
            previousWaveGapFloor = waveGapRange.first
        }
    }

    @Test
    fun `lifetime is pinned to an exact seeded number at level 1`() {
        assertEquals(
            PINNED_LIFETIME_LEVEL_1,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetLifetimeMsByLevel(1),
        )
    }

    @Test
    fun `lifetime is pinned to an exact seeded number at level 10`() {
        assertEquals(
            PINNED_LIFETIME_LEVEL_10,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetLifetimeMsByLevel(10),
        )
    }

    @Test
    fun `lifetime is pinned to an exact seeded number at level 30`() {
        assertEquals(
            PINNED_LIFETIME_LEVEL_30,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetLifetimeMsByLevel(30),
        )
    }

    @Test
    fun `lifetime is pinned to an exact seeded number at level 55`() {
        assertEquals(
            PINNED_LIFETIME_LEVEL_55,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetLifetimeMsByLevel(55),
        )
    }

    private fun waveDelays(
        level: Int,
        seed: Long = PINNED_SEED,
    ): List<Int> {
        val seededHelper = SessionHelperImpl(random = Random(seed))
        return (0 until GAME_COLUMN_COUNT * 2).map { id ->
            seededHelper.getTargetAppearanceDelayMsByIdAndLevel(id, level)
        }
    }

    @Test
    fun `wave timing across two waves is pinned to exact seeded numbers at level 1`() {
        assertEquals(PINNED_WAVE_DELAYS_LEVEL_1, waveDelays(level = 1))
    }

    @Test
    fun `wave timing across two waves is pinned to exact seeded numbers at level 10`() {
        assertEquals(PINNED_WAVE_DELAYS_LEVEL_10, waveDelays(level = 10))
    }

    @Test
    fun `wave timing across two waves is pinned to exact seeded numbers at level 30`() {
        assertEquals(PINNED_WAVE_DELAYS_LEVEL_30, waveDelays(level = 30))
    }

    @Test
    fun `wave timing across two waves is pinned to exact seeded numbers at level 55`() {
        assertEquals(PINNED_WAVE_DELAYS_LEVEL_55, waveDelays(level = 55))
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

    @Test
    fun `MC-60 - preparation cost profile matches the level 1 table at generation`() {
        assertProfileMatches(level = 1, expectedPercentages = listOf(25, 65, 10, 0))
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
            105,
            SessionHelperImpl(random = Random(PINNED_SEED)).getTargetValueByLevel(level = 30, operationDigit = 8),
        )
    }

    @Test
    fun `MC-60 - the generated value never leaves the level's own range after the shift`() {
        val seededHelper = SessionHelperImpl(random = Random(PINNED_SEED))
        repeat(SAMPLE_ITERATIONS) {
            for (level in listOf(1, 10, 30, 55, 999)) {
                val minThreshold = seededHelper.initialTargetValueRange.first + level / 10
                val maxThreshold = seededHelper.initialTargetValueRange.last + level * 3
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
            val maxThreshold = seededHelper.initialTargetValueRange.last + level * 3
            val expectedRange = IntRange(minThreshold, maxThreshold)
            val divisor = seededHelper.getDivisionDigitByLevel(level)
            val value = seededHelper.getTargetValueByLevel(level, divisor)
            assertTrue(value in expectedRange, "level $level: value $value left $expectedRange")
        }
    }
}
