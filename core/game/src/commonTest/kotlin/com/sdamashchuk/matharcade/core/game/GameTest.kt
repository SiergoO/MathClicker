package com.sdamashchuk.matharcade.core.game

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
import kotlin.time.Clock
import kotlin.time.Instant

// Counts every draw made through the delegate, so a test can assert "no random consumed" instead
// of inferring it from an unaffected outcome - the direct kill for a re-added visibleTargetsAbsent().
private class CountingRandom(
    private val delegate: Random,
) : Random() {
    var drawCount = 0
        private set

    override fun nextBits(bitCount: Int): Int {
        drawCount++
        return delegate.nextBits(bitCount)
    }
}

private const val TICK_STEP_MS = 250

// Empirically enough 250ms ticks for seed 99's level-1 board to run out the clock: the field closes
// at lifeCount 0 around the 130th tick and tick()'s own isClosed guard freezes every target in place
// after that, so any count at or above this lands on the identical terminal state.
private const val SEED_99_SESSION_TICKS = 200

@OptIn(ExperimentalCoroutinesApi::class)
class GameTest {
    @Test
    fun `all targets inactive increments the level and regenerates the target set`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()

            // level 1's flightTimeMs is 1000 and id 0 always appears immediately (MC-73:
            // appearsAtMs == gameTimeMs at creation), so four 250ms ticks reveal and then break the
            // sole target out in the same locked step.
            repeat(4) { game.tick(250) }
            testScheduler.runCurrent()

            assertEquals(2, game.stateFlow.value.field.level)
            // FakeSessionHelper's amount is level-dependent: targetAmount 1 at level 2 is 1 + (2 - 1).
            assertEquals(2, game.stateFlow.value.targets.size)
            assertTrue(
                game.stateFlow.value.targets
                    .all { it.isActive },
            )
        }

    @Test
    fun `tick with the collector live decrements staggered appearance delays without drawing from random`() =
        runTest {
            // No target ever becomes visible in this window (the shortest delay is well past the
            // total elapsed time below), so the collector sees "no visible target" on every one of
            // these ticks - exactly the state a re-added visibleTargetsAbsent() branch would fire on,
            // every single time, off this same Random.
            val countingRandom = CountingRandom(Random(8))
            val delays = listOf(5000L, 6000L, 7000L)
            val game = Game(FakeSessionHelper(), backgroundScope, countingRandom)
            game.start()
            game.createField(1)
            // MC-73: per-id delay staggering is no longer something SessionHelper can express -
            // getFinishSpacingMsByLevel is uniform by construction, which is the whole fix. Built
            // directly rather than through createTargets() to keep this test's own staggered
            // fixture.
            game.targetsRestored(
                delays.mapIndexed { index, delayMs ->
                    scheduledTarget(id = index + 1, value = 1, appearanceDelayMs = delayMs)
                },
            )
            testScheduler.runCurrent()
            val drawsBeforeTicking = countingRandom.drawCount

            repeat(1000) {
                game.tick(1)
                testScheduler.runCurrent()
            }

            val gameTimeMs = game.stateFlow.value.field.gameTimeMs
            assertEquals(1000L, gameTimeMs)
            // MC-72: the schedule itself (appearsAtMs) never moves - what "decrements" is the gap
            // to it, read back off the clock, the same 4000/5000/6000 the old stored delay counted
            // down to over the same 1000 ticks.
            assertEquals(
                listOf(4000L, 5000L, 6000L),
                game.stateFlow.value.targets
                    .sortedBy { it.id }
                    .map { it.appearsAtMs - gameTimeMs },
            )
            assertTrue(
                game.stateFlow.value.targets
                    .none { it.isVisible(gameTimeMs) },
            )
            assertEquals(drawsBeforeTicking, countingRandom.drawCount)
        }

    @Test
    fun `fireButtonClicked derives the next operation digit from the current level`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(10))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()

            // Level up to 2 first: getNextSignAndDigit hardcoding level 1 is invisible at level 1.
            // Four 250ms ticks reveal and break the sole target out (flightTimeMs 1000, id 0 always appears immediately).
            repeat(4) { game.tick(250) }
            testScheduler.runCurrent()
            assertEquals(2, game.stateFlow.value.field.level)

            game.fireButtonClicked()
            testScheduler.runCurrent()

            // Random(10)'s third draw (after recreateField's two) is pinned here, not derived from
            // the field under test: reading nextOperationSign back out of the result being asserted
            // let a hardcoded sign in getNextSignAndDigit survive undetected. Seeded specifically to
            // draw SUBTRACTION so a hardcoded DIVISION is also caught - the real-helper determinism
            // test below happens to draw DIVISION at its own call site, so between the two, a
            // hardcoded sign of either value fails at least one test. Reaching level 2 by ticking
            // rather than by the old targetRevealed/targetDidBreakout calls draws no extra
            // randomness: the sole target reveals with no draw, and shortenAppearanceDelay never
            // fires because it has already broken out (isActive false) by the time the board empties.
            assertEquals(OperationSign.SUBTRACTION, game.stateFlow.value.field.nextOperationSign)
            assertEquals(4, game.stateFlow.value.field.nextOperationDigit)
        }

    @Test
    fun `tick decrements life count per breakout and closes the field once it hits zero`() =
        runTest {
            // Staggered appearance delays (0, 500, 1000ms) make each of the three targets break out
            // on its own tick, so lifeCount can be observed decrementing one at a time up to closure.
            // Each successor is already visible before its predecessor breaks out (uniform 1000ms
            // lifetime, 500ms delay gaps), so the board is never briefly empty of visible targets
            // mid-sequence - if it were, tick()'s edge-triggered shortenAppearanceDelay would consume
            // the staggering by pulling the next one forward.
            // MC-73: built directly rather than through FakeSessionHelper/createTargets(), since
            // per-id delay staggering is no longer expressible through SessionHelper (spacing is
            // uniform by construction).
            val game = Game(FakeSessionHelper(), backgroundScope, Random(3))
            game.start()
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1, appearanceDelayMs = 0, lifetimeMs = 1000),
                    scheduledTarget(id = 2, value = 1, appearanceDelayMs = 500, lifetimeMs = 1000),
                    scheduledTarget(id = 3, value = 1, appearanceDelayMs = 1000, lifetimeMs = 1000),
                ),
            )
            testScheduler.runCurrent()

            repeat(4) { game.tick(250) } // 1000ms elapsed: the undelayed target breaks out
            testScheduler.runCurrent()
            assertEquals(2, game.stateFlow.value.field.lifeCount)
            assertFalse(game.stateFlow.value.field.isClosed)

            repeat(2) { game.tick(250) } // 1500ms elapsed: the 500ms-delayed target breaks out
            testScheduler.runCurrent()
            assertEquals(1, game.stateFlow.value.field.lifeCount)
            assertFalse(game.stateFlow.value.field.isClosed)

            repeat(2) { game.tick(250) } // 2000ms elapsed: the 1000ms-delayed target breaks out
            testScheduler.runCurrent()
            assertEquals(0, game.stateFlow.value.field.lifeCount)
            assertTrue(game.stateFlow.value.field.isClosed)
        }

    @Test
    fun `the field closing on the tick that empties the board keeps the level where it was`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(25))
            game.start()
            game.fieldRestored(Field(id = 1, level = 3, lifeCount = 1, isClosed = false))
            val soleTarget = scheduledTarget(id = 1, value = 10, lifetimeMs = 1000)
            game.targetsRestored(listOf(soleTarget))
            testScheduler.runCurrent()

            // The sole target breaking out both zeroes the last life (closing the field) and
            // empties the board - what would otherwise be a level-up - in the same locked tick.
            repeat(4) { game.tick(250) }
            testScheduler.runCurrent()

            assertTrue(game.stateFlow.value.field.isClosed)
            assertEquals(3, game.stateFlow.value.field.level)
        }

    @Test
    fun `the field closing on the tick that empties the board does not recreate targets`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(26))
            game.start()
            game.fieldRestored(Field(id = 1, level = 3, lifeCount = 1, isClosed = false))
            val soleTarget = scheduledTarget(id = 1, value = 10, lifetimeMs = 1000)
            game.targetsRestored(listOf(soleTarget))
            testScheduler.runCurrent()

            repeat(4) { game.tick(250) }
            testScheduler.runCurrent()

            // A fresh set would carry a new id range built off the (wrongly bumped) level; the
            // dead session must keep publishing the one broken-out target it actually has.
            assertEquals(
                listOf(1),
                game.stateFlow.value.targets
                    .map { it.id },
            )
            assertFalse(
                game.stateFlow.value.targets
                    .first()
                    .isActive,
            )
        }

    @Test
    fun `a field already closed keeps its level when the last active target is tapped to zero`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(27))
            game.start()
            game.fieldRestored(Field(id = 1, level = 4, lifeCount = 0, isClosed = true))
            val soleTarget = scheduledTarget(id = 1, value = 1, lifetimeMs = 1000)
            game.targetsRestored(listOf(soleTarget))
            testScheduler.runCurrent()

            // Not every breakout that closes the field also empties the board (other targets can
            // still be active); this reaches the same dead-session state through targetClicked's
            // own retirement path instead of a breakout, exercising activeTargetsAbsent() rather
            // than tick().
            game.targetClicked(soleTarget.id)
            testScheduler.runCurrent()

            assertEquals(4, game.stateFlow.value.field.level)
        }

    @Test
    fun `a field already closed does not recreate targets when the last active target is tapped to zero`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(28))
            game.start()
            game.fieldRestored(Field(id = 1, level = 4, lifeCount = 0, isClosed = true))
            val soleTarget = scheduledTarget(id = 1, value = 1, lifetimeMs = 1000)
            game.targetsRestored(listOf(soleTarget))
            testScheduler.runCurrent()

            game.targetClicked(soleTarget.id)
            testScheduler.runCurrent()

            assertEquals(
                listOf(1),
                game.stateFlow.value.targets
                    .map { it.id },
            )
            assertFalse(
                game.stateFlow.value.targets
                    .first()
                    .isActive,
            )
        }

    @Test
    fun `seeded random reproduces the same field across separate game instances`() =
        runTest {
            val sessionHelper = FakeSessionHelper()
            val first = Game(sessionHelper, backgroundScope, Random(42))
            val second = Game(sessionHelper, backgroundScope, Random(42))

            first.createField(1)
            second.createField(1)

            assertEquals(
                first.stateFlow.value.field.currentOperationSign,
                second.stateFlow.value.field.currentOperationSign,
            )
            assertEquals(
                first.stateFlow.value.field.currentOperationDigit,
                second.stateFlow.value.field.currentOperationDigit,
            )
            assertEquals(first.stateFlow.value.field.nextOperationSign, second.stateFlow.value.field.nextOperationSign)
            assertEquals(
                first.stateFlow.value.field.nextOperationDigit,
                second.stateFlow.value.field.nextOperationDigit,
            )

            // Pins what Random(42) actually draws, so a bare .random() (agreeing 1 time in 4) or a
            // hardcoded DIVISION (agreeing always) both fail this instead of passing by luck.
            assertEquals(OperationSign.SUBTRACTION, first.stateFlow.value.field.currentOperationSign)
            assertEquals(3, first.stateFlow.value.field.currentOperationDigit)
            assertEquals(OperationSign.SUBTRACTION, first.stateFlow.value.field.nextOperationSign)
            assertEquals(3, first.stateFlow.value.field.nextOperationDigit)
        }

    @Test
    fun `seeded Game reproduces the same session with the real session helper`() =
        runTest {
            // The fake above proves Game's own Random is seeded; this proves the session as a
            // whole is, by routing the same seed through the production SessionHelperImpl - the
            // six call sites this task fixes - instead of a fake that never drew from it.
            // Fixed rather than Clock.System: two calls to runSession() below are otherwise a real
            // clock apart, which would put a different finishedAt on the field seed 99 closes -
            // breaking the very reproducibility this test exists to pin.
            val fixedClock =
                object : Clock {
                    override fun now() = Instant.fromEpochMilliseconds(0)
                }

            suspend fun runSession(): Pair<Field, List<Target>> {
                val seed = 99L
                val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed), fixedClock)
                game.createField(1)
                game.createTargets()

                // The very first target's own appearsAtMs always equals gameTimeMs at creation (id 0
                // draws finishesAtMs = gameTimeMs + openingOffset, and openingOffset is at least the
                // level's own max flight time - see SessionHelperImpl.getOpeningOffsetMsByLevel), so
                // it is already visible before this tick. The tick itself is still load-bearing for
                // the pinned literals below - it is one real step of the simulation being
                // reproduced, not a reveal.
                game.tick(TICK_STEP_MS)
                game.fireButtonClicked()

                // Fixed step, fixed count: seed 99's level-1 board runs out its own clock (the field
                // closes at lifeCount 0 partway through), and tick()'s isClosed guard then freezes
                // every target in place, so this count only needs to reach that frozen state, not
                // land on it exactly.
                repeat(SEED_99_SESSION_TICKS - 1) { game.tick(TICK_STEP_MS) }

                return game.stateFlow.value.field to game.stateFlow.value.targets
            }

            val (firstField, firstTargets) = runSession()
            val (secondField, secondTargets) = runSession()

            assertEquals(firstField, secondField)
            assertEquals(firstTargets, secondTargets)

            // Comparing the two runs to each other cannot fail on a range narrow enough that an
            // unseeded draw coincides by chance (getSubtractionDigitByLevel's 1..3 at level 1 agrees
            // roughly one run in four). Pinning exact seed-99 values is what actually catches that.
            // Unlike the old UI-driven path (which forced every target to break out regardless of
            // elapsed time), the engine's own clock plays this out to a real game over. MC-73 assigns
            // finishesAtMs directly instead of summing an independently-rolled delay and lifetime,
            // which reshuffles every value, schedule and later draw off the same seed - every literal
            // below moved, and the field now survives twice as many ticks before closing (the fix
            // itself: GameSimulationTest's own pinned elapsedMs moved from 12496 to 24896 for the
            // same reason).
            assertEquals(
                Field(
                    id = 1,
                    level = 1,
                    score = 0,
                    lifeCount = 0,
                    bonusMultiplier = 0,
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 2,
                    nextOperationSign = OperationSign.SUBTRACTION,
                    nextOperationDigit = 2,
                    isClosed = true,
                    finishedAt = 0L,
                    // 61 of the 200 250ms ticks land before the field closes and freezes the clock -
                    // GameSimulationTest's 15104 is the same instant reached at 16ms steps.
                    gameTimeMs = 15250L,
                ),
                firstField,
            )
            // MC-72: appearsAtMs/finishesAtMs replace fallenMs/appearanceDelayMs/lifetimeMs as the
            // pinned representation - each target's schedule is now the two absolute instants it was
            // given at creation, not the countdown state a tick would have advanced it to. MC-73:
            // consecutive finishesAtMs now differ by exactly getFinishSpacingMsByLevel(1) (6573ms) -
            // by construction, not by luck of this particular seed.
            assertEquals(
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 6,
                        appearsAtMs = 1333,
                        finishesAtMs = 9388,
                        isActive = false,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 5,
                        appearsAtMs = 3475,
                        finishesAtMs = 12242,
                        isActive = false,
                    ),
                    Target(
                        id = 3,
                        relatedFieldId = 1,
                        columnId = 2,
                        value = 4,
                        appearsAtMs = 6024,
                        finishesAtMs = 15096,
                        isActive = false,
                    ),
                    Target(
                        id = 4,
                        relatedFieldId = 1,
                        columnId = 3,
                        value = 5,
                        appearsAtMs = 8924,
                        finishesAtMs = 17950,
                        isActive = true,
                    ),
                    Target(
                        id = 5,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 6,
                        appearsAtMs = 11649,
                        finishesAtMs = 20804,
                        isActive = true,
                    ),
                    Target(
                        id = 6,
                        relatedFieldId = 1,
                        columnId = 2,
                        value = 7,
                        appearsAtMs = 15833,
                        finishesAtMs = 23658,
                        isActive = true,
                    ),
                    Target(
                        id = 7,
                        relatedFieldId = 1,
                        columnId = 3,
                        value = 4,
                        appearsAtMs = 17829,
                        finishesAtMs = 26512,
                        isActive = true,
                    ),
                    Target(
                        id = 8,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 10,
                        appearsAtMs = 21210,
                        finishesAtMs = 29366,
                        isActive = true,
                    ),
                    Target(
                        id = 9,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 1,
                        appearsAtMs = 24333,
                        finishesAtMs = 32220,
                        isActive = true,
                    ),
                    Target(
                        id = 10,
                        relatedFieldId = 1,
                        columnId = 2,
                        value = 3,
                        appearsAtMs = 26798,
                        finishesAtMs = 35074,
                        isActive = true,
                    ),
                    Target(
                        id = 11,
                        relatedFieldId = 1,
                        columnId = 3,
                        value = 9,
                        appearsAtMs = 30339,
                        finishesAtMs = 37928,
                        isActive = true,
                    ),
                    Target(
                        id = 12,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 3,
                        appearsAtMs = 32914,
                        finishesAtMs = 40782,
                        isActive = true,
                    ),
                ),
                firstTargets,
            )
        }

    @Test
    fun `start called twice does not leak a collector that outlives stop`() =
        runTest {
            // If start() doesn't cancel the earlier job, stop() only cancels the second one, and the
            // first keeps collecting: the level-up below would still fire after stop(). Driven with
            // targetsRestored rather than a breakout, because tick() now levels up inline regardless
            // of whether the collector is running - the collector's only remaining trigger is exactly
            // this kind of externally-supplied, already-empty target list.
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(5))
            game.start()
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val allInactive =
                game.stateFlow.value.targets
                    .map { it.copy(isActive = false) }

            game.stop()
            game.targetsRestored(allInactive)
            testScheduler.runCurrent()

            assertEquals(1, game.stateFlow.value.field.level)
        }

    @Test
    fun `stop cancels the collector so a state change no longer triggers a level-up`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(4))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val allInactive =
                game.stateFlow.value.targets
                    .map { it.copy(isActive = false) }

            game.stop()
            game.targetsRestored(allInactive)
            testScheduler.runCurrent()

            assertEquals(1, game.stateFlow.value.field.level)
        }

    @Test
    fun `targetClicked reduces the target's value by exactly one and scores a profitable hit`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 10), backgroundScope, Random(16))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.stateFlow.value.targets
                    .first()
                    .id

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            assertEquals(
                9,
                game.stateFlow.value.targets
                    .first { it.id == targetId }
                    .value,
            )
            assertEquals(1, game.stateFlow.value.field.score)
        }

    @Test
    fun `targetClicked does not score a target that fireButtonClicked marked unprofitable`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(42))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.stateFlow.value.targets
                    .first()
                    .id
            // fireButtonClicked only operates on visible targets. A delay-0 target is already
            // visible at creation under MC-72 (appearsAtMs == the field's gameTimeMs at that
            // moment), so this tick is no longer load-bearing for the reveal - kept only to mirror
            // real play, where tick() always runs before the first press.
            game.tick(1)
            testScheduler.runCurrent()

            // Random(42) used to draw SUBTRACTION/3 here (pinned by the seeded-reproducibility test
            // above), a guaranteed fail against value 1 - which was exactly MC-65's bug: an opening
            // draw with no target to validate against. createTargets() now corrects that draw on its
            // own, so the losing operation this test needs has to be forced explicitly instead, the
            // same way GameOperationDrawTest pins one for the same reason.
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 3,
                ),
            )
            testScheduler.runCurrent()

            // Against value 1, SUBTRACTION/3 takes the losing branch: value grows to 4, isProfitable
            // flips false.
            game.fireButtonClicked()
            testScheduler.runCurrent()
            val afterFire =
                game.stateFlow.value.targets
                    .first { it.id == targetId }
            assertFalse(afterFire.isProfitable)
            assertEquals(4, afterFire.value)
            assertEquals(0, game.stateFlow.value.field.score)

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            assertEquals(
                3,
                game.stateFlow.value.targets
                    .first { it.id == targetId }
                    .value,
            )
            assertEquals(0, game.stateFlow.value.field.score)
        }

    @Test
    fun `targetClicked retires a target it clears to zero`() =
        runTest {
            // A second, never-revealed target keeps the board non-empty so clearing the first one
            // does not level up and regenerate the id being asserted on. targetClicked doesn't gate
            // on visibility, so the first target need not be revealed first.
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1), backgroundScope, Random(21))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.stateFlow.value.targets
                    .sortedBy { it.id }
                    .first()
                    .id

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            val cleared =
                game.stateFlow.value.targets
                    .first { it.id == targetId }
            assertEquals(0, cleared.value)
            // MC-72: isVisible is derived from the clock alone, so retiring a target no longer
            // forces it off - isActive false is what every consumer (TargetButton, performOperation)
            // actually gates on, and that is what targetClicked's ensureAlive(id) sets here.
            assertFalse(cleared.isActive)
            assertEquals(1, game.stateFlow.value.field.score)
        }

    @Test
    fun `clicking the last target to zero clears the board and advances the level`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(22))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.stateFlow.value.targets
                    .first()
                    .id

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            // Without ensureAlive(id) the cleared target stays active, the board never empties and
            // the level never advances - the soft-lock the engine audit named for this function.
            assertEquals(2, game.stateFlow.value.field.level)
            assertEquals(2, game.stateFlow.value.targets.size)
            assertTrue(
                game.stateFlow.value.targets
                    .all { it.isActive },
            )
        }

    @Test
    fun `fireButtonClicked scores a successful hit and retires a target cleared to zero`() =
        runTest {
            // A second, never-revealed target stays active throughout so clearing the first one
            // doesn't leave the board fully inactive - that would level up and regenerate the whole
            // target set out from under the id being asserted on below. MC-73: a huge finish spacing
            // (rather than a per-id delay, no longer expressible) pushes the second target's own
            // appearsAtMs far past this test's window, so the reveal tick below only reveals the
            // first target.
            val game =
                Game(
                    FakeSessionHelper(
                        targetAmount = 2,
                        targetValue = 3,
                        finishSpacingMs = 999_999,
                    ),
                    backgroundScope,
                    Random(42),
                )
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.stateFlow.value.targets
                    .sortedBy { it.id }
                    .first()
                    .id
            // See the MC-72 note above: this tick is no longer load-bearing for the reveal, kept
            // only to mirror real play.
            game.tick(1)
            testScheduler.runCurrent()

            // Random(42) draws SUBTRACTION/3 here; against value 3 that is the winning branch that
            // clears the target to exactly zero and must retire it, not just leave it at 0 and alive.
            game.fireButtonClicked()
            testScheduler.runCurrent()

            val cleared =
                game.stateFlow.value.targets
                    .first { it.id == targetId }
            assertEquals(0, cleared.value)
            // See the MC-72 note on the same pair of assertions above.
            assertFalse(cleared.isActive)
            assertEquals(3, game.stateFlow.value.field.score)
        }

    @Test
    fun `level-up regenerates targets whose value and lifetime scale with the new level`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 10), backgroundScope, Random(17))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()

            // Four 250ms ticks reveal and break the sole target out (flightTimeMs 1000, id 0 always appears immediately).
            repeat(4) { game.tick(250) }
            testScheduler.runCurrent()
            assertEquals(2, game.stateFlow.value.field.level)

            // FakeSessionHelper: value = targetValue + (level - 1), flightTimeMs = 1000 + (level - 1).
            // A hardcoded 1 or an off-by-one level + 1 both land on a different number than this.
            val regenerated =
                game.stateFlow.value.targets
                    .first()
            assertEquals(11, regenerated.value)
            assertEquals(1001L, regenerated.finishesAtMs - regenerated.appearsAtMs)
        }

    @Test
    fun `fieldRestored replaces the field with exactly the restored value`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(18))
            val restoredField =
                Field(
                    id = 7,
                    level = 5,
                    score = 42,
                    lifeCount = 1,
                    isClosed = false,
                )

            game.fieldRestored(restoredField)
            testScheduler.runCurrent()

            assertEquals(restoredField, game.stateFlow.value.field)
        }

    // MC-71's named risk: a restored session must keep ticking forward from the clock it was saved
    // with, not from zero - a mutant that dropped gameTimeMs from fieldRestored's copy (or reset it
    // some other way) would fail this by landing on 250, not 5250.
    @Test
    fun `a restored session resumes ticking from its saved gameTimeMs rather than zero`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(20))
            game.fieldRestored(Field(id = 1, level = 1, lifeCount = 3, gameTimeMs = 5000, isClosed = false))
            val soleTarget = scheduledTarget(id = 1, value = 10, lifetimeMs = 1_000_000)
            game.targetsRestored(listOf(soleTarget))
            testScheduler.runCurrent()

            game.tick(250)
            testScheduler.runCurrent()

            assertEquals(5250L, game.stateFlow.value.field.gameTimeMs)
        }

    // MC-72's own headline risk, sibling to the test above: a target created after a restore must
    // be scheduled against the restored gameTimeMs, not a fresh zero. M4 in the mutation ledger -
    // appearsAtMs written as the raw delay, without adding gameTimeMs - lands this at 300, already
    // 4700ms in the past against a restored clock of 5000, breaking the target out before the first
    // tick after restore even runs. That is the exact failure mode the design doc calls "the whole
    // board vylets one tick" if the clock (or, as here, the schedule built off it) comes back wrong.
    @Test
    fun `a target created after restore is scheduled against the restored gameTimeMs not zero`() =
        runTest {
            // openingOffsetMs is the total, not an extra past the default (which auto-floors to the
            // level's own flight time, 1000ms here, so id 0 appears immediately) - 1300 leaves
            // exactly a 300ms gap past that floor.
            val game = Game(FakeSessionHelper(targetAmount = 1, openingOffsetMs = 1300), backgroundScope, Random(23))
            game.fieldRestored(Field(id = 1, level = 1, lifeCount = 3, gameTimeMs = 5000, isClosed = false))

            game.createTargets()
            testScheduler.runCurrent()

            val created =
                game.stateFlow.value.targets
                    .first()
            assertEquals(5300L, created.appearsAtMs)
        }

    @Test
    fun `targetsRestored replaces the target list with exactly the restored targets`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(19))
            val restoredTargets =
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 7,
                        columnId = 0,
                        value = 9,
                        appearsAtMs = -120,
                        finishesAtMs = 29880,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 7,
                        columnId = 1,
                        value = 4,
                        appearsAtMs = 5000,
                        finishesAtMs = 30000,
                    ),
                )

            game.targetsRestored(restoredTargets)
            testScheduler.runCurrent()

            assertEquals(restoredTargets, game.stateFlow.value.targets)
        }
}
