package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.BoosterDropContext
import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val NEUTRAL_CONTEXT =
    BoosterDropContext(
        anyVisibleBeyondTelegraph = false,
        noVisibleBubble = false,
        oneLifeLeft = false,
        icePickArmed = false,
        shieldActive = false,
    )

// The 15th draw (counter 14) is BoosterDropRule's own guaranteed slot - forcing every sampled roll
// onto it isolates pickBooster's weighting from the chance curve above it.
private const val FORCED_DROP_COUNTER = 14

private const val WEIGHT_SAMPLE_TRIALS = 20_000

private fun sampleBoosters(
    context: BoosterDropContext,
    seed: Long,
    trials: Int = WEIGHT_SAMPLE_TRIALS,
): Map<Booster, Int> {
    val rule = BoosterDropRule(Random(seed))
    val counts = Booster.values().associateWith { 0 }.toMutableMap()
    repeat(trials) {
        val result = rule.roll(FORCED_DROP_COUNTER, hasDroppedBefore = false, stashFull = false, context = context)
        val booster = requireNotNull(result.booster) { "the 15th draw must always drop" }
        counts[booster] = counts.getValue(booster) + 1
    }
    return counts
}

private fun Map<Booster, Int>.percentOf(
    booster: Booster,
    trials: Int = WEIGHT_SAMPLE_TRIALS,
): Int = getValue(booster) * 100 / trials

class BoosterDropRuleTest {
    @Test
    fun `the first four draws of a session never drop regardless of seed`() {
        for (seed in 0 until 50) {
            val rule = BoosterDropRule(Random(seed.toLong()))
            var counter = 0
            repeat(4) {
                val result = rule.roll(counter, hasDroppedBefore = false, stashFull = false, context = NEUTRAL_CONTEXT)
                assertNull(result.booster, "seed $seed draw ${counter + 1} dropped early")
                counter = result.counter
            }
        }
    }

    @Test
    fun `the 15th draw without a drop is guaranteed and resets the counter`() {
        for (seed in 0 until 50) {
            val rule = BoosterDropRule(Random(seed.toLong()))
            val result =
                rule.roll(
                    counter = FORCED_DROP_COUNTER,
                    hasDroppedBefore = false,
                    stashFull = false,
                    context = NEUTRAL_CONTEXT,
                )
            assertTrue(result.booster != null, "seed $seed did not drop on the 15th draw")
            assertEquals(0, result.counter)
        }
    }

    // The silent opening is once per session: the 8.4 mean holds only if a later drought starts at 2%.
    @Test
    fun `the silent opening window does not repeat once the session has already dropped once`() {
        val rule = BoosterDropRule(Random(11))
        var drops = 0

        repeat(WEIGHT_SAMPLE_TRIALS) {
            val result = rule.roll(counter = 0, hasDroppedBefore = true, stashFull = false, context = NEUTRAL_CONTEXT)
            if (result.booster != null) drops++
        }

        val percent = drops * 100 / WEIGHT_SAMPLE_TRIALS
        assertTrue(percent in 1..3, "draw 1 of a later drought dropped $percent% of the time, expected about 2%")
        assertTrue(drops > 0, "draw 1 of a later drought never dropped - the silent window looks reinstated")
    }

    @Test
    fun `no drop happens while the stash is full and the counter does not grow`() {
        val rule = BoosterDropRule(Random(1))

        val result =
            rule.roll(
                counter = FORCED_DROP_COUNTER,
                hasDroppedBefore = false,
                stashFull = true,
                context = NEUTRAL_CONTEXT,
            )

        assertNull(result.booster)
        assertEquals(FORCED_DROP_COUNTER, result.counter)
    }

    @Test
    fun `drop intervals average between 7 and 10 draws and never exceed 15`() {
        val rule = BoosterDropRule(Random(1234))
        val intervals = mutableListOf<Int>()
        var counter = 0
        var hasDroppedBefore = false

        while (intervals.size < 20_000) {
            val result = rule.roll(counter, hasDroppedBefore, stashFull = false, context = NEUTRAL_CONTEXT)
            if (result.booster != null) {
                intervals += counter + 1
                hasDroppedBefore = true
            }
            counter = result.counter
        }

        val mean = intervals.average()
        assertTrue(mean in 7.0..10.0, "mean interval was $mean over ${intervals.size} drops")
        assertTrue(
            intervals.all { it <= 15 },
            "an interval of ${intervals.max()} draws exceeded the guaranteed bound",
        )
    }

    @Test
    fun `base weights favour Freeze then an even Rewind-IcePick split then Shield - 3-2-2-1`() {
        val counts = sampleBoosters(NEUTRAL_CONTEXT, seed = 1)

        assertTrue(counts.percentOf(Booster.FREEZE) in 33..42, "Freeze was ${counts.percentOf(Booster.FREEZE)}%")
        assertTrue(counts.percentOf(Booster.REWIND) in 21..29, "Rewind was ${counts.percentOf(Booster.REWIND)}%")
        assertTrue(counts.percentOf(Booster.ICE_PICK) in 21..29, "IcePick was ${counts.percentOf(Booster.ICE_PICK)}%")
        assertTrue(counts.percentOf(Booster.SHIELD) in 9..16, "Shield was ${counts.percentOf(Booster.SHIELD)}%")
    }

    @Test
    fun `a visible bubble past the telegraph doubles Freeze and IcePick`() {
        val context = NEUTRAL_CONTEXT.copy(anyVisibleBeyondTelegraph = true)

        val counts = sampleBoosters(context, seed = 2)

        assertTrue(counts.percentOf(Booster.FREEZE) in 41..51, "Freeze was ${counts.percentOf(Booster.FREEZE)}%")
        assertTrue(counts.percentOf(Booster.ICE_PICK) in 26..36, "IcePick was ${counts.percentOf(Booster.ICE_PICK)}%")
        assertTrue(counts.percentOf(Booster.REWIND) in 11..20, "Rewind was ${counts.percentOf(Booster.REWIND)}%")
        assertTrue(counts.percentOf(Booster.SHIELD) in 4..11, "Shield was ${counts.percentOf(Booster.SHIELD)}%")
    }

    @Test
    fun `one life left triples Shield`() {
        val context = NEUTRAL_CONTEXT.copy(oneLifeLeft = true)

        val counts = sampleBoosters(context, seed = 3)

        assertTrue(counts.percentOf(Booster.SHIELD) in 26..34, "Shield was ${counts.percentOf(Booster.SHIELD)}%")
        assertTrue(counts.percentOf(Booster.FREEZE) in 26..34, "Freeze was ${counts.percentOf(Booster.FREEZE)}%")
    }

    @Test
    fun `IcePick is excluded with no visible bubble`() {
        val context = NEUTRAL_CONTEXT.copy(noVisibleBubble = true)

        val counts = sampleBoosters(context, seed = 4)

        assertEquals(0, counts.getValue(Booster.ICE_PICK))
    }

    @Test
    fun `IcePick is excluded while one is armed`() {
        val context = NEUTRAL_CONTEXT.copy(icePickArmed = true)

        val counts = sampleBoosters(context, seed = 5)

        assertEquals(0, counts.getValue(Booster.ICE_PICK))
    }

    @Test
    fun `Shield is excluded while one is active`() {
        val context = NEUTRAL_CONTEXT.copy(shieldActive = true)

        val counts = sampleBoosters(context, seed = 6)

        assertEquals(0, counts.getValue(Booster.SHIELD))
    }
}
