package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.Field
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// MC-54 granted a life automatically every LIFE_BONUS_INTERVAL_LEVELS levels, capped at
// LIFE_BONUS_CAP. MC-76 removed that trigger on the owner's call - it may come back as a random
// event - but kept the grant itself as Game.grantLife, the mechanism this file now exercises.
@OptIn(ExperimentalCoroutinesApi::class)
class LifeBonusTest {
    private fun soleTarget(value: Int = 1) = scheduledTarget(id = 1, value = value)

    @Test
    fun `leveling across a former qualifying level changes lifeCount by nothing`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, level = 4, lifeCount = 1, isClosed = false))
            game.targetsRestored(listOf(soleTarget()))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.targetClicked(1) // clears the sole target: 4 -> 5, a former qualifying level

            testScheduler.runCurrent()

            assertEquals(5, game.stateFlow.value.field.level)
            assertEquals(1, game.stateFlow.value.field.lifeCount)
            assertTrue(events.filterIsInstance<GameEvent.LifeGranted>().isEmpty())
        }

    @Test
    fun `grantLife adds exactly one life and emits LifeGranted exactly once`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, lifeCount = 1, isClosed = false))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.grantLife()

            testScheduler.runCurrent()

            assertEquals(2, game.stateFlow.value.field.lifeCount)
            assertEquals(listOf(GameEvent.LifeGranted(2)), events.filterIsInstance<GameEvent.LifeGranted>())
        }

    @Test
    fun `grantLife at the cap is a no-op and emits nothing`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, lifeCount = 3, isClosed = false)) // 3 is Field's own cap
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.grantLife()

            testScheduler.runCurrent()

            assertEquals(3, game.stateFlow.value.field.lifeCount)
            assertTrue(events.filterIsInstance<GameEvent.LifeGranted>().isEmpty())
        }

    @Test
    fun `repeated grantLife calls never push lifeCount past the cap`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, lifeCount = 1, isClosed = false))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            repeat(5) { game.grantLife() }

            testScheduler.runCurrent()

            assertEquals(3, game.stateFlow.value.field.lifeCount)
            assertEquals(2, events.filterIsInstance<GameEvent.LifeGranted>().size)
        }
}
