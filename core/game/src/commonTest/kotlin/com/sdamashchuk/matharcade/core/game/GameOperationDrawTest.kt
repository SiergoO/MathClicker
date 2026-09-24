package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.helper.SessionHelperImpl
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// MC-50: getNextSignAndDigit must never offer a sign/digit that fails against every visible active
// target on the board - a press the player had no way to win, decided before they touched anything.

// Counts every draw made through the delegate, the same pattern GameTest's own CountingRandom uses
// (a separate, identically-shaped class - top-level private names collide across files in the same
// package even though visibility is file-scoped), so a test can assert exactly how many attempts a
// redraw cost instead of inferring it from state.
private class DrawCountingRandom(
    private val delegate: Random,
) : Random() {
    var drawCount = 0
        private set

    override fun nextBits(bitCount: Int): Int {
        drawCount++
        return delegate.nextBits(bitCount)
    }
}

// Mirrors Game's own private succeedsAgainst so a test can restate the invariant it enforces without
// reaching into the implementation.
private fun Target.isReachableBy(
    sign: OperationSign,
    digit: Int,
): Boolean =
    when (sign) {
        OperationSign.DIVISION -> digit != 0 && value % digit == 0
        OperationSign.SUBTRACTION -> value - digit >= 0
    }

private const val TICK_STEP_MS = 250

// Mirrors Game.kt's private MAX_OPERATION_DRAW_ATTEMPTS. Kept here rather than exported, since the
// bound is an implementation detail of the redraw loop, not part of Game's public contract - but the
// termination test below needs a concrete number to assert against, not just "some finite number".
private const val EXPECTED_DRAW_BOUND = 20

// Always fails, regardless of which sign is drawn: subtraction's digit is pinned above the sole
// target's value (never true that value - 999 >= 0), and division's digit is pinned to 0 - the
// Field() sentinel that performOperation already treats as a failed split rather than a crash, and
// which the validity check must reject the same way, without ever evaluating value % 0.
private class AlwaysDudSessionHelper : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(level: Int) = 5

    override fun getTargetLifetimeMsByLevel(level: Int) = 100_000

    override fun getTargetAppearanceDelayMsById(id: Int) = 0

    override fun getTargetAmountByLevel(level: Int) = 1

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = if (operationSign == OperationSign.DIVISION) 0 else 999

    override fun getDivisionDigitByLevel(level: Int) = 0

    override fun getSubtractionDigitByLevel(level: Int) = 999

    override fun failedGrowthCap(level: Int) = 1_000_000
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameOperationDrawTest {
    @Test
    fun `10000 seeded draws never offer a board-wide dud`() =
        runTest {
            repeat(10_000) { trial ->
                val seed = trial.toLong()
                val sessionHelper = SessionHelperImpl(random = Random(seed))
                val game = Game(sessionHelper, backgroundScope, Random(seed))
                game.createField(1)
                game.createTargets()
                // Varies how much of the board is revealed before firing - 1..30 ticks of 250ms, all
                // well under level 1's lifetime floor so nothing breaks out mid-trial - so the check
                // covers both a bare first reveal and a board further into its fall.
                repeat(1 + (trial % 30)) { game.tick(TICK_STEP_MS) }

                game.fireButtonClicked()

                val visibleActiveTargets =
                    game.stateFlow.value.targets
                        .filter { it.isActive && it.isVisible }
                val field = game.stateFlow.value.field
                val dud =
                    visibleActiveTargets.isNotEmpty() &&
                        visibleActiveTargets.none {
                            it.isReachableBy(field.nextOperationSign, field.nextOperationDigit)
                        }
                assertTrue(
                    !dud,
                    "seed $seed drew a board-wide dud: ${field.nextOperationSign}/${field.nextOperationDigit} " +
                        "against ${visibleActiveTargets.map { it.value }}",
                )
            }
        }

    @Test
    fun `an empty board draws exactly once without looping`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(1))
            val game = Game(SessionHelperImpl(random = Random(1)), backgroundScope, countingRandom)
            game.createField(1)
            // No createTargets(): the board is empty, the one case the redraw loop must never spin
            // on regardless of how invalid the first draw would otherwise be.
            val drawsBeforeFiring = countingRandom.drawCount

            game.fireButtonClicked()

            assertEquals(1, countingRandom.drawCount - drawsBeforeFiring)
        }

    @Test
    fun `a fully-hidden board draws exactly once without looping`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(1))
            val game = Game(SessionHelperImpl(random = Random(1)), backgroundScope, countingRandom)
            game.createField(1)
            game.createTargets() // every target defaults to isVisible = false, never ticked
            val drawsBeforeFiring = countingRandom.drawCount

            game.fireButtonClicked()

            assertEquals(1, countingRandom.drawCount - drawsBeforeFiring)
        }

    @Test
    fun `a board every draw fails against still terminates and returns the bounded fallback`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(7))
            val game = Game(AlwaysDudSessionHelper(), backgroundScope, countingRandom)
            game.createField(1)
            game.createTargets()
            game.tick(1) // reveals the sole target (delay 0)
            // Pins the current press to a no-op (division by the Field() sentinel digit 0) so the
            // redraw loop below is exercised in isolation from whatever recreateField happened to
            // draw for the current operation - the target's value stays exactly 5 either way.
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.DIVISION,
                    currentOperationDigit = 0,
                ),
            )
            val drawsBeforeFiring = countingRandom.drawCount

            game.fireButtonClicked()

            // The bound is spent in full, then the last (still invalid) draw is used anyway - a
            // defined fallback, not a hang and not a silent extra attempt past the stated limit.
            assertEquals(EXPECTED_DRAW_BOUND, countingRandom.drawCount - drawsBeforeFiring)
            val target =
                game.stateFlow.value.targets
                    .first()
            val field = game.stateFlow.value.field
            assertFalse(target.isReachableBy(field.nextOperationSign, field.nextOperationDigit))
        }

    @Test
    fun `an invisible or inactive target cannot rescue an otherwise board-wide dud`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(7))
            val game = Game(AlwaysDudSessionHelper(), backgroundScope, countingRandom)
            game.createField(1)
            game.targetsRestored(
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 5,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 100_000,
                        isVisible = true,
                        isActive = true,
                    ),
                    // Satisfies SUBTRACTION/999 (2000 - 999 >= 0), the one combination AlwaysDudSessionHelper
                    // ever offers - but hidden, so it must not count as a way out of the dud above.
                    Target(
                        id = 2,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 2000,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 100_000,
                        isVisible = false,
                        isActive = true,
                    ),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.DIVISION,
                    currentOperationDigit = 0,
                ),
            )
            val drawsBeforeFiring = countingRandom.drawCount

            game.fireButtonClicked()

            assertEquals(EXPECTED_DRAW_BOUND, countingRandom.drawCount - drawsBeforeFiring)
        }
}
