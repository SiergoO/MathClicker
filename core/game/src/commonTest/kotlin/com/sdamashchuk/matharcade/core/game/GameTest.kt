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

            // level 1's lifetimeMs is 1000 and appearanceDelayMs is 0, so four 250ms ticks reveal
            // and then break the sole target out in the same locked step.
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
            val delays = listOf(5000, 6000, 7000)
            val sessionHelper = FakeSessionHelper(targetAmount = 3, appearanceDelayMsById = { delays[it] })
            val game = Game(sessionHelper, backgroundScope, countingRandom)
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val drawsBeforeTicking = countingRandom.drawCount

            repeat(1000) {
                game.tick(1)
                testScheduler.runCurrent()
            }

            assertEquals(
                listOf(4000, 5000, 6000),
                game.stateFlow.value.targets
                    .sortedBy { it.id }
                    .map { it.appearanceDelayMs },
            )
            assertTrue(
                game.stateFlow.value.targets
                    .none { it.isVisible },
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
            // Four 250ms ticks reveal and break the sole target out (lifetimeMs 1000, delay 0).
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
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 3, appearanceDelayMsById = { it * 500 }),
                    backgroundScope,
                    Random(3),
                )
            game.start()
            game.createField(1)
            game.createTargets()
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
            val soleTarget =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 10,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 1000,
                )
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
            val soleTarget =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 10,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 1000,
                )
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
            val soleTarget =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 1,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 1000,
                )
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
            val soleTarget =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 1,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 1000,
                )
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
            suspend fun runSession(): Pair<Field, List<Target>> {
                val seed = 99L
                val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
                game.createField(1)
                game.createTargets()

                // The very first target always has appearanceDelayMs 0 (see
                // SessionHelperImpl.getTargetAppearanceDelayMsByIdAndLevel), so one tick reveals it
                // before firing once against it - the same ordering the old targetRevealed +
                // fireButtonClicked call pair produced.
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
            // elapsed time), the engine's own clock plays this out to a real game over: three of the
            // eight targets break out before lifeCount reaches zero, and the other five freeze wherever
            // they were falling once the field closes. MC-60 threads operationDigit into
            // getTargetValueByLevel, adding one Random draw per target - that reshuffles every value,
            // fall time and later draw off the same seed, which is why every literal below moved.
            assertEquals(
                Field(
                    id = 1,
                    level = 1,
                    score = 9,
                    lifeCount = 0,
                    bonusMultiplier = 0,
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 2,
                    nextOperationSign = OperationSign.SUBTRACTION,
                    nextOperationDigit = 2,
                    isClosed = true,
                ),
                firstField,
            )
            assertEquals(
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 3,
                        fallenMs = 9750,
                        appearanceDelayMs = 0,
                        lifetimeMs = 9519,
                        isVisible = false,
                        isActive = false,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 13,
                        fallenMs = 10800,
                        appearanceDelayMs = 0,
                        lifetimeMs = 10750,
                        isVisible = false,
                        isActive = false,
                    ),
                    Target(
                        id = 3,
                        relatedFieldId = 1,
                        columnId = 2,
                        value = 13,
                        fallenMs = 10344,
                        appearanceDelayMs = 0,
                        lifetimeMs = 10325,
                        isVisible = false,
                        isActive = false,
                    ),
                    Target(
                        id = 4,
                        relatedFieldId = 1,
                        columnId = 3,
                        value = 21,
                        fallenMs = 9473,
                        appearanceDelayMs = 0,
                        lifetimeMs = 11365,
                        isVisible = true,
                        isActive = true,
                    ),
                    Target(
                        id = 5,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 12,
                        fallenMs = 8306,
                        appearanceDelayMs = 0,
                        lifetimeMs = 10629,
                        isVisible = true,
                        isActive = true,
                    ),
                    Target(
                        id = 6,
                        relatedFieldId = 1,
                        columnId = 2,
                        value = 17,
                        fallenMs = 6872,
                        appearanceDelayMs = 0,
                        lifetimeMs = 11242,
                        isVisible = true,
                        isActive = true,
                    ),
                    Target(
                        id = 7,
                        relatedFieldId = 1,
                        columnId = 3,
                        value = 5,
                        fallenMs = 5831,
                        appearanceDelayMs = 0,
                        lifetimeMs = 11129,
                        isVisible = true,
                        isActive = true,
                    ),
                    Target(
                        id = 8,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 5,
                        fallenMs = 5001,
                        appearanceDelayMs = 0,
                        lifetimeMs = 11101,
                        isVisible = true,
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
            // fireButtonClicked only operates on visible targets; one tick reveals it (delay 0).
            game.tick(1)
            testScheduler.runCurrent()

            // Random(42) draws SUBTRACTION/3 here (pinned by the seeded-reproducibility test above);
            // against value 1 that takes the losing branch: value grows to 4, isProfitable flips false.
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
            assertFalse(cleared.isActive)
            assertFalse(cleared.isVisible)
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
            // target set out from under the id being asserted on below. Its own delay is far past
            // this test's window, so the reveal tick below only reveals the first target.
            val game =
                Game(
                    FakeSessionHelper(
                        targetAmount = 2,
                        targetValue = 3,
                        appearanceDelayMsById = { index -> if (index == 0) 0 else 999_999 },
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
            // fireButtonClicked only operates on visible targets; one tick reveals it (delay 0).
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
            assertFalse(cleared.isActive)
            assertFalse(cleared.isVisible)
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

            // Four 250ms ticks reveal and break the sole target out (lifetimeMs 1000, delay 0).
            repeat(4) { game.tick(250) }
            testScheduler.runCurrent()
            assertEquals(2, game.stateFlow.value.field.level)

            // FakeSessionHelper: value = targetValue + (level - 1), lifetimeMs = 1000 + (level - 1).
            // A hardcoded 1 or an off-by-one level + 1 both land on a different number than this.
            val regenerated =
                game.stateFlow.value.targets
                    .first()
            assertEquals(11, regenerated.value)
            assertEquals(1001, regenerated.lifetimeMs)
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
                        fallenMs = 120,
                        appearanceDelayMs = 0,
                        lifetimeMs = 30000,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 7,
                        columnId = 1,
                        value = 4,
                        fallenMs = 0,
                        appearanceDelayMs = 5000,
                        lifetimeMs = 25000,
                    ),
                )

            game.targetsRestored(restoredTargets)
            testScheduler.runCurrent()

            assertEquals(restoredTargets, game.stateFlow.value.targets)
        }
}
