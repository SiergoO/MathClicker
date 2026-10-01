package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.isReadyFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

private val TESTED_SEEDS = listOf(1L, 42L, 99L, 12345L)
private val TESTED_LEVELS = listOf(1, 10, 30, 50)

private const val PROFILE_SAMPLE_ITERATIONS = 10_000
private const val PROFILE_TOLERANCE_PP = 5
private const val PROFILE_SEED = 4242L

// RecreateTargets must pick the value generator by the armed sign, not always the
// division-shaped one - verified through Game.createTargets (not sessionHelper directly), since the
// bug this task exists for was entirely in that dispatch, not in either generator on its own.
@OptIn(ExperimentalCoroutinesApi::class)
class GameArmedSignTargetGenerationTest {
    // The exact symptom the task reports: an all-Red500 board, because every generated value happened
    // to be well above the armed digit. Under the fix, the level's own preparation profile promises at
    // least half the board is a genuine trap at every level (readyNowPercent never exceeds 35%), so a
    // whole board with none is a statistical near-impossibility - and a mechanical one under the
    // mutant that ignores the sign (see MC-70's M1), since a division-shaped value's floor is already
    // at least the level's own minThreshold, which is virtually always above a subtraction digit.
    @Test
    fun `a subtraction-armed board is never entirely ready - across seeds and levels`() =
        runTest {
            for (seed in TESTED_SEEDS) {
                for (level in TESTED_LEVELS) {
                    val sessionHelper = SessionHelperImpl(random = Random(seed))
                    val game = Game(sessionHelper, backgroundScope, Random(seed))
                    game.createField(1)
                    val digit = sessionHelper.getSubtractionDigitByLevel(level)
                    // digit 1 (or, structurally, 0) admits no trap at all - a target's value is never
                    // below 1, so it is mechanically always ready. Not the bug under test.
                    if (digit < 2) continue
                    game.fieldRestored(
                        game.stateFlow.value.field.copy(
                            level = level,
                            currentOperationSign = OperationSign.SUBTRACTION,
                            currentOperationDigit = digit,
                        ),
                    )
                    game.createTargets()

                    val targets = game.stateFlow.value.targets
                    assertTrue(
                        targets.any { !isReadyFor(it, OperationSign.SUBTRACTION, digit) },
                        "seed $seed level $level: all ${targets.size} targets were ready against digit $digit " +
                            "- the readiness hint carries no information on this board",
                    )
                }
            }
        }

    // The other half of the same guarantee: fixing subtraction must not touch division. Sampled
    // through Game.createTargets across many boards rather than a single call, the same way MC-60's
    // own profile evidence is sampled directly against SessionHelperImpl - this is the same table,
    // just proven through the dispatch in Game.kt instead of assuming it is reached unchanged.
    @Test
    fun `a division-armed board still follows MC-60's residue profile through Game - unchanged`() =
        runTest {
            val level = 10
            val expectedPercentages = listOf(25, 45, 30, 0)
            var readyNow = 0
            var oneTap = 0
            var twoOrThreeTaps = 0
            var fourPlusTaps = 0
            val sessionHelper = SessionHelperImpl(random = Random(PROFILE_SEED))
            val game = Game(sessionHelper, backgroundScope, Random(PROFILE_SEED))
            game.createField(1)

            repeat(PROFILE_SAMPLE_ITERATIONS) {
                val digit = sessionHelper.getDivisionDigitByLevel(level)
                game.fieldRestored(
                    game.stateFlow.value.field.copy(
                        level = level,
                        currentOperationSign = OperationSign.DIVISION,
                        currentOperationDigit = digit,
                    ),
                )
                game.createTargets()
                val value =
                    game.stateFlow.value.targets
                        .first()
                        .value
                when (value % digit) {
                    0 -> readyNow++
                    1 -> oneTap++
                    in 2..3 -> twoOrThreeTaps++
                    else -> fourPlusTaps++
                }
            }

            val total = readyNow + oneTap + twoOrThreeTaps + fourPlusTaps
            val actual = listOf(readyNow, oneTap, twoOrThreeTaps, fourPlusTaps).map { it * 100 / total }
            val labels = listOf("ready now", "1 tap", "2-3 taps", "4+ taps")
            for (i in actual.indices) {
                assertTrue(
                    kotlin.math.abs(actual[i] - expectedPercentages[i]) <= PROFILE_TOLERANCE_PP,
                    "level $level ${labels[i]}: expected ~${expectedPercentages[i]}%, measured ${actual[i]}%",
                )
            }
        }
}
