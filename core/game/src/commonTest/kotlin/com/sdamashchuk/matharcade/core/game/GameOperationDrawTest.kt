package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.game.helper.SessionHelperImpl
import com.sdamashchuk.matharcade.core.model.Field
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
// MC-65: the same guarantee for the operation a session opens with - recreateField draws it before a
// single target exists to check it against, so MC-50's redraw never covered it. createTargets now
// validates against the first wave of targets to become visible (not "now": none of them are visible
// yet at creation - see ensureOpeningOperationSucceeds's own comment in Game.kt).
// MC-79: the session's second operation - recreateField's nextOperationSign/Digit, promoted to current
// by the first press's updateActionButtons - was still a blind draw MC-65 never covered. Validated at
// promotion time instead of at creation (see ensurePromotedOperationSucceeds's own comment in Game.kt):
// validating it against the opening wave the same way current is was tried and measured first, and
// left 12.5% of seeded first presses still promoting a dud once real ticks separated creation from the
// press.

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

private const val SIGN_MIX_TRIALS = 2000

// The engine and the session helper must not share a seed here, or the sign drawn from one and the
// digit drawn from the other move together and the measured mix is an artefact of that, not the mix.
private const val SIGN_MIX_HELPER_SALT = 7919L

// What one offer costs against AlwaysDudSessionHelper, whose digits are constants and so draw no
// randomness at all: only the sign draw is counted. MC-93 made that exactly one - the sign is drawn
// once and each sign's own digit curve is then retried in turn, where before every retry redrew the
// sign too and a dud board cost the full MAX_OPERATION_DRAW_ATTEMPTS of 20. Kept here rather than
// exported, since the bound is an implementation detail of the redraw loop, not part of Game's public
// contract - but the termination tests below need a concrete number, not just "some finite number".
private const val EXPECTED_DRAW_BOUND = 1

// Always fails, regardless of which sign is drawn: subtraction's digit is pinned above the sole
// target's value (never true that value - 999 >= 0), and division's digit is pinned to 0 - the
// Field() sentinel that performOperation already treats as a failed split rather than a crash, and
// which the validity check must reject the same way, without ever evaluating value % 0.
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
            // Zero, not a tolerance. MC-60 briefly made this 3-in-10000 by shaping values toward
            // a residue of the current digit: a lone 1 is below every division digit and every
            // subtraction digit past level 9, so the bounded redraw could spend all its attempts and
            // hand over the dud it started with. The exhaustion fallback in getNextSignAndDigit
            // closes that by construction rather than by luck.
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

    // MC-79: recreateField's next was a blind draw the same way current was before MC-65 - the very
    // first press promotes it to current via updateActionButtons with no validation of its own ever
    // having run against it. Checked here on the promoted field, after the first press, against
    // whatever the press itself left on the board - the same shape as the 10000-draw test above, but
    // reading currentOperationSign/Digit (what updateActionButtons just promoted next into) instead of
    // nextOperationSign/Digit (the fresh draw MC-50 already covers).
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
            // Several levels, not just the level every fresh session actually starts at: createTargets
            // is also the recovery path a restored field with an arbitrary level routes through when
            // its persisted targets are gone (GameViewModel.updateSession), and the validation this
            // covers applies there identically.
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

            // AlwaysDudSessionHelper's sole target is visible at creation (see its own comment), so
            // this bound is spent inside createTargets() itself, not deferred to a later tick or press.
            // MC-93: one sign draw, not twenty - both signs are now tried from a single draw.
            // MC-79 leaves nextOperationSign/Digit untouched here by design (see ensureOpeningOperationSucceeds's
            // own comment), so this count is unchanged by that task - only current is fixed at creation.
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
            // Every target is 9: a multiple of one level-1 divisor (3) but not the other (2), and
            // above every level-1 subtraction digit (1..3). So neither sign is impossible, but the
            // first draw is a dud often enough to put the redraw loop to work - which is where the
            // skew lived. A board of, say, all 12s proves nothing here: every first draw succeeds,
            // the loop never runs, and old and new code both measure 50%.
            //
            // Redrawing the sign together with the digit meant every retry re-rolled the sign, and
            // subtraction succeeds far more readily than division, so the loop quietly walked the mix
            // toward minus - the owner saw it dominate the early levels. This board measures 33% on
            // the pre-MC-93 loop against 50% after it; the window is wide enough for sampling noise
            // and far tighter than the skew it guards against.
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
            // MC-72: game.createTargets() no longer produces this case - the real session helper
            // always schedules the first target's appearsAtMs at field.gameTimeMs (delay 0), so it is
            // visible immediately, not hidden until a tick advances the clock past it. A genuinely
            // fully-hidden board is now built directly: every target scheduled to appear in the future.
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

            // MC-79: this is the session's first press, so pendingOpeningPromotionCheck also spends a
            // full EXPECTED_DRAW_BOUND fixing the about-to-be-promoted next (recreateField's own draw,
            // equally a dud against AlwaysDudSessionHelper) before getNextSignAndDigit spends a second
            // one on the fresh next below - 2 * EXPECTED_DRAW_BOUND (was EXPECTED_DRAW_BOUND before
            // that task). The bound is spent in full each time and no attempt is made past it - but what
            // comes back is a move, not the last dud. Subtracting the smallest visible value is always
            // valid, so a board this helper can never satisfy by drawing is still never handed a dead
            // press, promoted or fresh.
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

            // MC-79: this is also the session's first press, so both redraw loops (the promotion-time
            // check and the fresh next draw) see the same hidden target and can't be rescued by it -
            // 2 * EXPECTED_DRAW_BOUND (40, was EXPECTED_DRAW_BOUND before this task).
            assertEquals(2 * EXPECTED_DRAW_BOUND, countingRandom.drawCount - drawsBeforeFiring)
        }
}
