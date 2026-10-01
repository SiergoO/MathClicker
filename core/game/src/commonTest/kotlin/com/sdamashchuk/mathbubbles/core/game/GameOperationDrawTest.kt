package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The engine must never offer a sign/digit that fails against every visible target - a press the
// player had no way to win. Covers the opening draw and the one promoted by the first press too.

// A separate, identically-shaped class from GameTest's: top-level private names collide across files
// in the same package even though visibility is file-scoped.
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

private const val SIGN_MIX_TRIALS = 2000

// A shared seed would correlate the sign drawn from one Random with the digit drawn from the other.
private const val SIGN_MIX_HELPER_SALT = 7919L

// AlwaysDudSessionHelper's digits are constants, so a dud board costs exactly one counted draw -
// the sign's.
private const val EXPECTED_DRAW_BOUND = 1

// Always fails: the subtraction digit is pinned above the sole target's value, and the division digit
// is 0 - the sentinel the validity check must reject without ever evaluating value % 0.
private class AlwaysDudSessionHelper : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetFlightTimeMsRange = 20000..40000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ) = 5

    override fun getTargetSpeedByLevel(level: Int) = 1f / 100_000

    override fun getTargetFlightTimeMs(level: Int) = 100_000

    override fun getFinishSpacingMsByLevel(level: Int) = 200_000

    // 0, matching the pre-MC-73 delay of the sole target this helper ever serves: with a single
    // target (getTargetAmountByLevel 1) and appearsAtMs(0) = gameTimeMs + openingOffset - flightTime,
    // an offset equal to the flight time is what makes it visible immediately.
    override fun getOpeningOffsetMsByLevel(level: Int) = 100_000

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
            // Zero, not a tolerance: the exhaustion fallback closes this by construction rather than by luck.
            var duds = 0
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

                val field = game.stateFlow.value.field
                val visibleActiveTargets =
                    game.stateFlow.value.targets
                        .filter { it.isActive && it.isVisible(field.gameTimeMs) }
                val dud =
                    visibleActiveTargets.isNotEmpty() &&
                        visibleActiveTargets.none {
                            it.isReachableBy(field.nextOperationSign, field.nextOperationDigit)
                        }
                if (dud) duds++
            }
            assertEquals(0, duds, "seeded draws produced $duds board-wide duds")
        }

    // Read on the promoted field after the first press - currentOperationSign, what updateActionButtons
    // just promoted into, not the fresh next draw.
    @Test
    fun `the first press never promotes an operation that duds every target still on the board`() =
        runTest {
            var duds = 0
            repeat(10_000) { trial ->
                val seed = trial.toLong()
                val sessionHelper = SessionHelperImpl(random = Random(seed))
                val game = Game(sessionHelper, backgroundScope, Random(seed))
                game.createField(1)
                game.createTargets()
                // Same 1..30 tick spread as the 10000-draw test above, so the first press lands on
                // both a bare first reveal and a board further into its fall before it is thrown.
                repeat(1 + (trial % 30)) { game.tick(TICK_STEP_MS) }

                game.fireButtonClicked()

                val field = game.stateFlow.value.field
                val visibleActiveTargets =
                    game.stateFlow.value.targets
                        .filter { it.isActive && it.isVisible(field.gameTimeMs) }
                val dud =
                    visibleActiveTargets.isNotEmpty() &&
                        visibleActiveTargets.none {
                            it.isReachableBy(field.currentOperationSign, field.currentOperationDigit)
                        }
                if (dud) duds++
            }
            assertEquals(0, duds, "the first press promoted $duds board-wide duds")
        }

    @Test
    fun `a seeded session opens with an operation that succeeds against its first visible targets`() =
        runTest {
            // Several levels: createTargets is also the recovery path a restored field with an arbitrary level
            // routes through when its persisted targets are gone.
            var duds = 0
            var trials = 0
            for (level in listOf(1, 10, 30, 100, 999)) {
                repeat(2_000) { trial ->
                    trials++
                    val seed = level * 1_000_000L + trial
                    val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
                    game.createField(1)
                    if (level != 1) {
                        game.fieldRestored(
                            game.stateFlow.value.field
                                .copy(level = level),
                        )
                    }
                    game.createTargets()

                    val field = game.stateFlow.value.field
                    val targets = game.stateFlow.value.targets
                    val firstVisibleAtMs = targets.minOf { it.appearsAtMs }
                    val openingWave = targets.filter { it.isActive && it.isVisible(firstVisibleAtMs) }
                    val dud =
                        openingWave.none {
                            it.isReachableBy(field.currentOperationSign, field.currentOperationDigit)
                        }
                    if (dud) duds++
                }
            }
            assertEquals(0, duds, "$duds of $trials seeded sessions opened with a board-wide dud")
        }

    @Test
    fun `createTargets redraws a dud opening operation but still terminates within the same bound`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(7))
            val game = Game(AlwaysDudSessionHelper(), backgroundScope, countingRandom)
            game.createField(1)
            val drawsBeforeCreatingTargets = countingRandom.drawCount

            game.createTargets()

            // The sole target is visible at creation, so this bound is spent inside createTargets itself.
            assertEquals(EXPECTED_DRAW_BOUND, countingRandom.drawCount - drawsBeforeCreatingTargets)
            val target =
                game.stateFlow.value.targets
                    .first()
            val field = game.stateFlow.value.field
            assertTrue(target.isReachableBy(field.currentOperationSign, field.currentOperationDigit))
            assertEquals(OperationSign.SUBTRACTION, field.currentOperationSign)
            assertEquals(target.value, field.currentOperationDigit)
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
    fun `both signs are offered about equally on a board that supports either - MC-93`() =
        runTest {
            // 9 is a multiple of one level-1 divisor (3) but not the other (2), so the first draw
            // is a dud often enough to exercise the redraw loop. Measured at 50% here; a loop that
            // re-rolls the sign on every retry measures 33%, which is what the window must exclude.
            var divisionOffers = 0
            repeat(SIGN_MIX_TRIALS) { trial ->
                val seed = trial.toLong()
                val game =
                    Game(SessionHelperImpl(random = Random(seed + SIGN_MIX_HELPER_SALT)), backgroundScope, Random(seed))
                game.fieldRestored(Field(id = 1, level = 1))
                game.targetsRestored(
                    (0 until 3).map { scheduledTarget(id = it + 1, columnId = it, value = 9) },
                )

                game.fireButtonClicked()

                if (game.stateFlow.value.field.nextOperationSign == OperationSign.DIVISION) divisionOffers++
            }

            val divisionPercent = divisionOffers * 100 / SIGN_MIX_TRIALS
            assertTrue(
                divisionPercent in 45..55,
                "division was offered on $divisionPercent% of draws, expected an even mix",
            )
        }

    @Test
    fun `a fully-hidden board draws exactly once without looping`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(1))
            val game = Game(SessionHelperImpl(random = Random(1)), backgroundScope, countingRandom)
            game.createField(1)
            // createTargets never produces this case - the first target is always visible immediately - so a
            // fully-hidden board is built directly instead.
            game.targetsRestored(listOf(scheduledTarget(id = 1, columnId = 0, value = 5, appearanceDelayMs = 1)))
            val drawsBeforeFiring = countingRandom.drawCount

            game.fireButtonClicked()

            assertEquals(1, countingRandom.drawCount - drawsBeforeFiring)
        }

    @Test
    fun `a board every draw fails against still terminates and returns a valid fallback`() =
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

            // The session's first press fixes the about-to-be-promoted operation as well as drawing a fresh one,
            // so the bound is spent twice. What comes back is a move, not the last dud.
            assertEquals(2 * EXPECTED_DRAW_BOUND, countingRandom.drawCount - drawsBeforeFiring)
            val target =
                game.stateFlow.value.targets
                    .first()
            val field = game.stateFlow.value.field
            assertTrue(target.isReachableBy(field.currentOperationSign, field.currentOperationDigit))
            assertEquals(OperationSign.SUBTRACTION, field.currentOperationSign)
            assertEquals(target.value, field.currentOperationDigit)
            assertTrue(target.isReachableBy(field.nextOperationSign, field.nextOperationDigit))
            assertEquals(OperationSign.SUBTRACTION, field.nextOperationSign)
            assertEquals(target.value, field.nextOperationDigit)
        }

    @Test
    fun `an invisible or inactive target cannot rescue an otherwise board-wide dud`() =
        runTest {
            val countingRandom = DrawCountingRandom(Random(7))
            val game = Game(AlwaysDudSessionHelper(), backgroundScope, countingRandom)
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, columnId = 0, value = 5),
                    // Satisfies SUBTRACTION/999 (2000 - 999 >= 0), the one combination AlwaysDudSessionHelper
                    // ever offers - but hidden (appearanceDelayMs > 0), so it must not count as a way
                    // out of the dud above.
                    scheduledTarget(id = 2, columnId = 1, value = 2000, appearanceDelayMs = 1),
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

            // This is also the session's first press, so both redraw loops (the promotion-time
            // check and the fresh next draw) see the same hidden target and can't be rescued by it -
            // 2 * EXPECTED_DRAW_BOUND (40, was EXPECTED_DRAW_BOUND before this task).
            assertEquals(2 * EXPECTED_DRAW_BOUND, countingRandom.drawCount - drawsBeforeFiring)
        }
}
