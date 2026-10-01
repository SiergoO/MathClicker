package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
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

            repeat(11) { game.tick(250) } // 2750ms - still inside the 3s window
            assertEquals(0L, game.stateFlow.value.field.gameTimeMs)

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

            repeat(8) { game.tick(250) } // exactly the 2000ms budget
            assertEquals(3000L, game.stateFlow.value.field.gameTimeMs)

            game.tick(250)
            assertEquals(3250L, game.stateFlow.value.field.gameTimeMs, "the clock never resumed forward")
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

            repeat(2) { game.tick(250) } // the budget was far from spent, so the clock should move forward again
            assertEquals(500L, game.stateFlow.value.field.gameTimeMs, "the effect did not end at the clamp")
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
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
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

            assertEquals(0L, game.stateFlow.value.field.gameTimeMs, "freeze let the clock move")
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

            repeat(10) { game.tick(250) } // would have unfrozen under the first timer alone
            assertEquals(0L, game.stateFlow.value.field.gameTimeMs, "the restarted freeze ended early")
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
            assertEquals(5000L, game.stateFlow.value.field.gameTimeMs, "freeze did not hold the clock")

            game.fireButtonClicked() // rewind cancels the freeze
            game.tick(250)
            assertEquals(
                4750L,
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
            assertEquals(0L, game.stateFlow.value.field.gameTimeMs)

            repeat(2) { game.tick(250) } // crosses the 3000ms mark exactly as if nothing happened in between
            assertTrue(game.stateFlow.value.field.gameTimeMs > 0)
        }

    @Test
    fun `fieldRestored clears an active shield and an armed ice pick so neither survives the restore`() =
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
            assertEquals(startingLives - 1, game.stateFlow.value.field.lifeCount, "the shield survived the restore")

            game.targetClicked(2)
            val tapped =
                game.stateFlow.value.targets
                    .first { it.id == 2 }
            assertEquals(4, tapped.value, "the ice pick survived the restore")
            assertTrue(tapped.isActive)
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
            assertTrue(
                firstMs in 7000 - stepMs..7000 + stepMs,
                "expected the rewind to delay the first breakout to ~7000ms, got ${firstMs}ms",
            )
            assertTrue(
                secondMs - firstMs in 1800 - stepMs..1800 + stepMs,
                "expected a ~1800ms gap between breakouts, got ${secondMs - firstMs}ms",
            )
        }
}
