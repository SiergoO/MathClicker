package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameInputGuardTest {
    private suspend fun TestScope.gameWith(
        targets: List<Target>,
        fieldEdit: (Field) -> Field = { it },
    ): Game {
        val game =
            Game(
                FakeSessionHelper(targetAmount = 1, targetValue = 5),
                backgroundScope,
                Random(1),
                boostersEnabled = true,
            )
        game.createField(1)
        game.targetsRestored(targets)
        game.fieldRestored(fieldEdit(game.stateFlow.value.field))
        return game
    }

    private suspend fun TestScope.closedGame(): Game {
        val game =
            gameWith(
                listOf(
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                    scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 1_000_000),
                ),
            ) {
                it.copy(
                    lifeCount = 1,
                    currentBooster = Booster.SHIELD,
                    nextBooster = Booster.FREEZE,
                    boosterStash = listOf(Booster.FREEZE),
                )
            }
        repeat(3) { game.tick(50) }
        assertTrue(game.stateFlow.value.field.isClosed)
        return game
    }

    @Test
    fun `a second tap on an already zeroed target awards nothing and keeps it at zero`() =
        runTest {
            val game =
                gameWith(
                    listOf(
                        scheduledTarget(id = 1, value = 1),
                        scheduledTarget(id = 2, columnId = 1, value = 5),
                    ),
                )
            game.targetClicked(1)
            val afterFirst = game.stateFlow.value
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.targetClicked(1)
            testScheduler.runCurrent()

            assertEquals(1, afterFirst.field.score)
            assertEquals(afterFirst, game.stateFlow.value)
            assertTrue(events.isEmpty())
        }

    @Test
    fun `tapping a broken-out target awards nothing and changes nothing`() =
        runTest {
            val game =
                gameWith(
                    listOf(
                        scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                        scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 1_000_000),
                    ),
                )
            repeat(3) { game.tick(50) }
            val before = game.stateFlow.value

            game.targetClicked(1)

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `tapping an unknown target id changes nothing and does not throw`() =
        runTest {
            val game = gameWith(listOf(scheduledTarget(id = 1, value = 5)))
            val before = game.stateFlow.value

            game.targetClicked(99)

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `tapping a target that has not appeared yet changes nothing`() =
        runTest {
            val game = gameWith(listOf(scheduledTarget(id = 1, value = 5, appearanceDelayMs = 5_000)))
            val before = game.stateFlow.value

            game.targetClicked(1)

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `a closed field ignores target taps`() =
        runTest {
            val game = closedGame()
            val before = game.stateFlow.value

            game.targetClicked(2)

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `a closed field ignores the fire button even with a booster held`() =
        runTest {
            val game = closedGame()
            val before = game.stateFlow.value

            game.fireButtonClicked()

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `a closed field ignores stashing the held booster`() =
        runTest {
            val game = closedGame()
            val before = game.stateFlow.value

            game.stashBooster()

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `a closed field ignores applying a stashed booster`() =
        runTest {
            val game = closedGame()
            val before = game.stateFlow.value

            game.applyBoosterFromStash(0)

            assertEquals(before, game.stateFlow.value)
        }

    @Test
    fun `lives never go below zero when several targets break out in one tick`() =
        runTest {
            val game =
                gameWith(
                    listOf(
                        scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                        scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 100),
                        scheduledTarget(id = 3, columnId = 2, value = 5, lifetimeMs = 100),
                    ),
                ) { it.copy(lifeCount = 1) }
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.tick(150)
            testScheduler.runCurrent()

            assertEquals(0, game.stateFlow.value.field.lifeCount)
            assertEquals(listOf(0, 0, 0), events.filterIsInstance<GameEvent.TargetBrokeOut>().map { it.livesLeft })
        }

    @Test
    fun `a restored armed stash slot beyond the stash is dropped and the next tap is a plain tap`() =
        runTest {
            val game =
                gameWith(listOf(scheduledTarget(id = 1, value = 5))) {
                    it.copy(boosterStash = emptyList(), icePickArmedStashIndex = 3)
                }

            game.targetClicked(1)

            assertEquals(null, game.stateFlow.value.field.icePickArmedStashIndex)
            assertEquals(null, game.stateFlow.value.effects.icePickArmedFrom)
            assertEquals(
                4,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `a restored armed stash slot holding a different booster is dropped`() =
        runTest {
            val game =
                gameWith(listOf(scheduledTarget(id = 1, value = 5))) {
                    it.copy(boosterStash = listOf(Booster.FREEZE), icePickArmedStashIndex = 0)
                }

            game.targetClicked(1)

            assertEquals(null, game.stateFlow.value.field.icePickArmedStashIndex)
            assertEquals(listOf(Booster.FREEZE), game.stateFlow.value.field.boosterStash)
            assertEquals(
                4,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `a restored armed stash slot holding an ice pick stays armed`() =
        runTest {
            val game =
                gameWith(listOf(scheduledTarget(id = 1, value = 5))) {
                    it.copy(boosterStash = listOf(Booster.ICE_PICK), icePickArmedStashIndex = 0)
                }

            game.targetClicked(1)

            assertEquals(emptyList(), game.stateFlow.value.field.boosterStash)
            assertEquals(
                0,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `a shield from the stash stays in the stash and does nothing while a shield is active`() =
        runTest {
            val game =
                gameWith(listOf(scheduledTarget(id = 1, value = 5))) {
                    it.copy(boosterStash = listOf(Booster.SHIELD), shieldActive = true)
                }
            val before = game.stateFlow.value

            game.applyBoosterFromStash(0)

            assertEquals(before, game.stateFlow.value)
            assertEquals(listOf(Booster.SHIELD), game.stateFlow.value.field.boosterStash)
        }

    @Test
    fun `a shield from the stash is consumed when no shield is active`() =
        runTest {
            val game =
                gameWith(listOf(scheduledTarget(id = 1, value = 5))) {
                    it.copy(boosterStash = listOf(Booster.SHIELD))
                }

            game.applyBoosterFromStash(0)

            assertTrue(game.stateFlow.value.field.shieldActive)
            assertEquals(emptyList(), game.stateFlow.value.field.boosterStash)
        }
}
