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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameShieldUsageTest {
    @Test
    fun `shield absorbs the next breakout's life loss and is consumed by it`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            // id 2 outlives the test so the board never levels up into a fresh target reusing id 1.
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                    scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 1_000_000),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.SHIELD),
            )
            val startingLives = game.stateFlow.value.field.lifeCount

            game.fireButtonClicked() // raises the shield
            repeat(5) { game.tick(50) } // clears the 100ms lifetime

            assertEquals(startingLives, game.stateFlow.value.field.lifeCount)
            assertFalse(
                game.stateFlow.value.targets
                    .first { it.id == 1 }
                    .isActive,
            )
        }

    @Test
    fun `shield absorbing a breakout emits TargetBrokeOut with shieldAbsorbed exactly once`() =
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
                    .copy(currentBooster = Booster.SHIELD),
            )
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.fireButtonClicked() // raises the shield
            repeat(5) { game.tick(50) } // clears id 1's 100ms lifetime
            testScheduler.runCurrent()

            val brokeOutEvents = events.filterIsInstance<GameEvent.TargetBrokeOut>()
            assertEquals(1, brokeOutEvents.count { it.shieldAbsorbed }, "the absorb must fire exactly once")
            assertEquals(1, brokeOutEvents.single().id)
            assertTrue(brokeOutEvents.single().shieldAbsorbed)
        }

    @Test
    fun `a second breakout after the shield is spent costs a life normally`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            // id 3 outlives the test; otherwise a level-up could reuse id 2 and run the loop below past
            // the breakout it stops at.
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                    scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 10_000),
                    scheduledTarget(id = 3, columnId = 2, value = 5, lifetimeMs = 1_000_000),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.SHIELD),
            )
            val startingLives = game.stateFlow.value.field.lifeCount

            game.fireButtonClicked() // raises the shield
            repeat(3) { game.tick(50) } // id 1 breaks out, the shield absorbs it
            assertEquals(startingLives, game.stateFlow.value.field.lifeCount)

            while (game.stateFlow.value.targets
                    .first { it.id == 2 }
                    .isActive
            ) {
                game.tick(250)
            }
            assertEquals(startingLives - 1, game.stateFlow.value.field.lifeCount)
        }

    @Test
    fun `applying shield again while one is already active still protects only one breakout`() =
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
                    scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 150),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.SHIELD, nextBooster = Booster.SHIELD),
            )
            val startingLives = game.stateFlow.value.field.lifeCount

            game.fireButtonClicked() // raises the shield
            game.fireButtonClicked() // applies a second shield - still just one flag

            repeat(4) { game.tick(50) } // both targets have broken out by 200ms
            assertEquals(
                startingLives - 1,
                game.stateFlow.value.field.lifeCount,
                "shield protected more than one breakout",
            )
        }
}
