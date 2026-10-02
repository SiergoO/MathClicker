package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoosterEffectTest {
    @Test
    fun `freeze stops the game clock for 3 seconds of real tick time then resumes`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE),
            )

            game.fireButtonClicked() // applies the freeze

            // 2750ms, inside the 3s window: 100ms of drift comes from easing down to a stop
            // over EFFECT_RAMP_MS rather than snapping to it.
            repeat(11) { game.tick(250) }
            assertEquals(100L, game.stateFlow.value.field.gameTimeMs)

            repeat(2) { game.tick(250) } // crosses the 3000ms mark
            assertTrue(game.stateFlow.value.field.gameTimeMs > 0, "the clock never resumed")
        }

    @Test
    fun `rewind moves the clock backward for 2 seconds of real tick time then resumes forward`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 5000, currentBooster = Booster.REWIND),
            )

            game.fireButtonClicked() // applies the rewind

            // 3400, not 3000: the ramp spends EFFECT_RAMP_MS easing the rate in and out of -1
            // rather than holding it for the whole budget, so the clock travels less far back.
            repeat(8) { game.tick(250) } // exactly the 2000ms budget
            assertEquals(3400L, game.stateFlow.value.field.gameTimeMs)

            game.tick(250)
            assertEquals(3650L, game.stateFlow.value.field.gameTimeMs, "the clock never resumed forward")
        }

    @Test
    fun `rewind stops early rather than push the most recently appeared bubble past its own appearance`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 500, currentBooster = Booster.REWIND),
            )

            game.fireButtonClicked() // applies the rewind, 2000ms budget - far more than needed to reach 0

            repeat(3) { game.tick(250) }
            assertEquals(0L, game.stateFlow.value.field.gameTimeMs)
            assertTrue(
                game.stateFlow.value.targets
                    .single()
                    .isVisible(game.stateFlow.value.field.gameTimeMs),
                "the bubble was pushed back past its own appearance",
            )

            // 300, not 500: hitting the clamp reserves a fresh, full ramp-out window instead of
            // folding it into whatever budget was already spent.
            repeat(2) { game.tick(250) }
            assertEquals(300L, game.stateFlow.value.field.gameTimeMs, "the effect did not end at the clamp")
        }

    @Test
    fun `freeze never shifts a waiting bubble's own appearance while shortenAppearanceDelay stays silent`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            val targets =
                listOf(
                    // 1000ms, not 100: clear of the frozen clock's own 100ms ramp-in drift, so
                    // that drift can never coincide with this target's own finish.
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 1_000),
                    scheduledTarget(id = 2, columnId = 1, value = 5, appearanceDelayMs = 100_000, lifetimeMs = 200_000),
                )
            game.targetsRestored(targets)
            val appearsAtMsBefore =
                game.stateFlow.value.targets
                    .first { it.id == 2 }
                    .appearsAtMs
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE),
            )

            game.fireButtonClicked() // applies the freeze
            repeat(11) { game.tick(250) } // 2750ms, inside the budget - would break id 1 out if not frozen

            // 100ms of ramp-in drift, same as GameBoosterEffectTest's own freeze test above.
            assertEquals(100L, game.stateFlow.value.field.gameTimeMs, "freeze let the clock move")
            assertEquals(
                appearsAtMsBefore,
                game.stateFlow.value.targets
                    .first { it.id == 2 }
                    .appearsAtMs,
            )
            assertTrue(
                game.stateFlow.value.targets
                    .first { it.id == 1 }
                    .isActive,
                "id 1 broke out despite the freeze",
            )
        }

    @Test
    fun `re-applying freeze while already frozen restarts the timer instead of stacking`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE, nextBooster = Booster.FREEZE),
            )

            game.fireButtonClicked() // first freeze
            repeat(10) { game.tick(250) } // 2500ms of its 3000ms budget spent
            game.fireButtonClicked() // second freeze restarts the timer

            // 100ms, all from the first application's ramp-in: the restart carries the rate
            // (already at the floor) across instead of ramping down from 1 again.
            repeat(10) { game.tick(250) } // would have unfrozen under the first timer alone
            assertEquals(100L, game.stateFlow.value.field.gameTimeMs, "the restarted freeze ended early")
        }

    @Test
    fun `applying rewind while freeze is active cancels the freeze outright`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 1_000_000, fallenMs = 100_000, lifetimeMs = 1_000_000)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    gameTimeMs = 5000,
                    currentBooster = Booster.FREEZE,
                    nextBooster = Booster.REWIND,
                ),
            )

            game.fireButtonClicked() // freeze
            game.tick(250)
            // 5100, not 5000: this single tick is inside freeze's own ramp-in, so the clock still
            // creeps forward a little rather than stopping outright.
            assertEquals(5100L, game.stateFlow.value.field.gameTimeMs, "freeze did not hold the clock")

            game.fireButtonClicked() // rewind cancels the freeze
            game.tick(250)
            // 4900: the rewind carries freeze's own rate (already down at 0, not back up at 1) into
            // its own ramp, which is what makes this crossover pass through 0 instead of jumping.
            assertEquals(
                4900L,
                game.stateFlow.value.field.gameTimeMs,
                "rewind did not take over from the cancelled freeze",
            )
        }

    @Test
    fun `calling other game methods between ticks never advances an effect timer the way a pause relies on`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.FREEZE),
            )

            game.fireButtonClicked()
            repeat(11) { game.tick(250) } // 2750ms, still inside the budget
            repeat(5) { game.grantLife() } // unrelated activity between ticks
            assertEquals(100L, game.stateFlow.value.field.gameTimeMs)

            repeat(2) { game.tick(250) } // crosses the 3000ms mark exactly as if nothing happened in between
            assertTrue(game.stateFlow.value.field.gameTimeMs > 0)
        }

    // An active shield and an armed ice pick are part of the field's persisted columns, so a
    // restore carries both forward instead of clearing them.
    @Test
    fun `fieldRestored carries an active shield and an armed ice pick through the restore`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                    scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 1_000_000),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.SHIELD, nextBooster = Booster.ICE_PICK),
            )
            game.fireButtonClicked() // raises the shield, promotes the ice pick into current
            game.fireButtonClicked() // arms the ice pick
            val startingLives = game.stateFlow.value.field.lifeCount

            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(),
            )

            repeat(5) { game.tick(50) } // clears id 1's 100ms lifetime
            assertEquals(startingLives, game.stateFlow.value.field.lifeCount, "the shield did not survive the restore")

            game.targetClicked(2)
            val tapped =
                game.stateFlow.value.targets
                    .first { it.id == 2 }
            assertEquals(0, tapped.value, "the ice pick did not survive the restore")
            assertFalse(tapped.isActive)
        }

    // A persisted shield and a persisted freeze (with its remaining real-time ms) both come back
    // through fieldRestored, exactly as if the field had never stopped running.
    @Test
    fun `a restored shield absorbs the next breakout and a restored freeze ends after its remaining ms`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 5, lifetimeMs = 100)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    shieldActive = true,
                    timedEffectBooster = Booster.FREEZE,
                    timedEffectRemainingMs = 1200,
                    timedEffectRate = 0.0,
                ),
            )

            assertTrue(game.stateFlow.value.effects.shieldActive)
            assertEquals(Booster.FREEZE, game.stateFlow.value.effects.timedBooster)
            val startingLives = game.stateFlow.value.field.lifeCount

            repeat(4) { game.tick(250) } // 1000ms of the 1200ms budget
            game.tick(199)
            assertEquals(Booster.FREEZE, game.stateFlow.value.effects.timedBooster, "the freeze ended early")

            game.tick(1) // crosses the 1200ms mark exactly
            assertNull(game.stateFlow.value.effects.timedBooster, "the freeze outlived its restored remaining ms")

            repeat(10) { game.tick(50) } // lets the now-resumed clock reach the target's 100ms lifetime
            assertEquals(startingLives, game.stateFlow.value.field.lifeCount, "the restored shield did not absorb")
            assertFalse(game.stateFlow.value.effects.shieldActive)
        }

    // The carried rate, not only the remaining ms, decides intensity - a freeze re-applied mid-hold
    // restores at full intensity rather than ramping back up from neutral.
    @Test
    fun `fieldRestored carries a re-applied freeze's rate back at full intensity`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    timedEffectBooster = Booster.FREEZE,
                    timedEffectRemainingMs = 1500,
                    timedEffectRate = 0.0,
                ),
            )

            assertEquals(1f, game.stateFlow.value.effects.intensity)
        }

    @Test
    fun `fieldRestored mid ramp-out restores the partial freeze tint rather than a forced full or neutral value`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000, lifetimeMs = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(freezeTintEnvelope = 0.42),
            )

            assertEquals(0.42f, game.stateFlow.value.effects.freezeTintIntensity)
        }

    @Test
    fun `a rewind applied before the first breakout leaves the real tick-time gap to the second unchanged`() =
        runTest {
            val stepMs = 50
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            // Both already visible (appearsAtMs 2000, well before gameTimeMs 5000) so the rewind below
            // has 3000ms of headroom before its own most-recently-appeared clamp would cut it short.
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 5, fallenMs = 3000, referenceGameTimeMs = 5000, lifetimeMs = 6000),
                    scheduledTarget(
                        id = 2,
                        columnId = 1,
                        value = 5,
                        fallenMs = 3000,
                        referenceGameTimeMs = 5000,
                        lifetimeMs = 7800, // 1800ms after id 1's own finish
                    ),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(gameTimeMs = 5000, currentBooster = Booster.REWIND),
            )
            val brokeOutAtRealMs = mutableListOf<Pair<Int, Int>>()
            var elapsedRealMs = 0
            backgroundScope.launch {
                game.events.collect { event ->
                    if (event is GameEvent.TargetBrokeOut) brokeOutAtRealMs.add(event.id to elapsedRealMs)
                }
            }
            testScheduler.runCurrent()

            game.fireButtonClicked() // applies the rewind well before either breakout
            while (brokeOutAtRealMs.size < 2) {
                game.tick(stepMs)
                elapsedRealMs += stepMs
                testScheduler.runCurrent()
            }

            val (firstMs, secondMs) =
                brokeOutAtRealMs
                    .sortedBy { it.first }
                    .map { it.second }
            // ~6600, not ~7000: the ramp spends part of the budget easing in and out of full
            // speed rather than holding it throughout, so the clock travels less far backward.
            assertTrue(
                firstMs in 6600 - stepMs..6600 + stepMs,
                "expected the rewind to delay the first breakout to ~6600ms, got ${firstMs}ms",
            )
            assertTrue(
                secondMs - firstMs in 1800 - stepMs..1800 + stepMs,
                "expected a ~1800ms gap between breakouts, got ${secondMs - firstMs}ms",
            )
        }
}
