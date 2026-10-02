package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.model.Booster
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameEffectsStateTest {
    @Test
    fun `freeze publishes the timed booster kind and a shrinking remaining fraction`() =
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
            assertEquals(Booster.FREEZE, game.stateFlow.value.effects.timedBooster)
            assertEquals(1f, game.stateFlow.value.effects.remainingFraction)

            repeat(6) { game.tick(250) } // 1500ms of the 3000ms budget spent
            assertEquals(0.5f, game.stateFlow.value.effects.remainingFraction)

            repeat(6) { game.tick(250) } // crosses the 3000ms mark
            assertNull(game.stateFlow.value.effects.timedBooster)
            assertEquals(0f, game.stateFlow.value.effects.remainingFraction)
        }

    @Test
    fun `applying ice pick from the stash arms it in the published state until the next tap`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 5, lifetimeMs = 100_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(boosterStash = listOf(Booster.ICE_PICK)),
            )

            assertFalse(game.stateFlow.value.effects.icePickArmed)
            game.applyBoosterFromStash(0)
            assertTrue(game.stateFlow.value.effects.icePickArmed)

            game.targetClicked(1)
            assertFalse(game.stateFlow.value.effects.icePickArmed)
        }

    @Test
    fun `shield stays reported active until the breakout it absorbs`() =
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
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.SHIELD),
            )

            game.fireButtonClicked()
            assertTrue(game.stateFlow.value.effects.shieldActive)

            repeat(5) { game.tick(50) } // clears id 1's 100ms lifetime
            assertFalse(game.stateFlow.value.effects.shieldActive)
        }

    @Test
    fun `createField publishes no active effects even when one was armed on the previous field`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 5, lifetimeMs = 100_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(boosterStash = listOf(Booster.ICE_PICK)),
            )
            game.applyBoosterFromStash(0)
            assertTrue(game.stateFlow.value.effects.icePickArmed)

            game.createField(2)
            assertFalse(game.stateFlow.value.effects.icePickArmed)
            assertNull(game.stateFlow.value.effects.timedBooster)
            assertFalse(game.stateFlow.value.effects.shieldActive)
        }

    // A running effect is part of the field's own persisted columns, so a kill mid-effect restores
    // at the same arm/activity state rather than losing it.
    @Test
    fun `fieldRestored carries an armed ice pick back into the published state`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 5, lifetimeMs = 100_000)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(boosterStash = listOf(Booster.ICE_PICK)),
            )
            game.applyBoosterFromStash(0)
            assertTrue(game.stateFlow.value.effects.icePickArmed)

            game.fieldRestored(game.stateFlow.value.field)
            assertTrue(game.stateFlow.value.effects.icePickArmed)
        }
}
