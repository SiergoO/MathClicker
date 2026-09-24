package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.game.model.GameEvent
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// MC-54: a life granted every LIFE_BONUS_INTERVAL_LEVELS levels, capped at LIFE_BONUS_CAP. Both
// constants are private to FieldMapper (5 and 3), so this file exercises them through Game the same
// way GameStreakTest exercises Field.advanceStreak - via the engine, not the private constant.
@OptIn(ExperimentalCoroutinesApi::class)
class LifeBonusTest {
    private fun soleTarget(value: Int = 1) =
        Target(
            id = 1,
            relatedFieldId = 1,
            columnId = 0,
            value = value,
            fallenMs = 0,
            appearanceDelayMs = 0,
            lifetimeMs = 100_000,
            isVisible = true,
        )

    @Test
    fun `leveling into a multiple of the interval grants exactly one life and emits LifeGranted once`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, level = 4, lifeCount = 1, isClosed = false))
            game.targetsRestored(listOf(soleTarget()))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.targetClicked(1) // clears the sole target: 4 -> 5, a qualifying level

            testScheduler.runCurrent()

            assertEquals(5, game.stateFlow.value.field.level)
            assertEquals(2, game.stateFlow.value.field.lifeCount)
            assertEquals(listOf(GameEvent.LifeGranted(2)), events.filterIsInstance<GameEvent.LifeGranted>())
        }

    @Test
    fun `leveling into a level that is not a multiple of the interval grants nothing`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, level = 3, lifeCount = 1, isClosed = false))
            game.targetsRestored(listOf(soleTarget()))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.targetClicked(1) // clears the sole target: 3 -> 4, not a qualifying level

            testScheduler.runCurrent()

            assertEquals(4, game.stateFlow.value.field.level)
            assertEquals(1, game.stateFlow.value.field.lifeCount)
            assertTrue(events.filterIsInstance<GameEvent.LifeGranted>().isEmpty())
        }

    @Test
    fun `repeated qualifying level-ups never push lifeCount past the cap`() =
        runTest {
            // targetAmount grows with level (FakeSessionHelper), so each new board still clears in
            // one click per remaining target - deep enough (level 4 through 15) to cross two
            // qualifying levels (5 and 10) starting already at the cap.
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, level = 4, lifeCount = 3, isClosed = false))
            game.targetsRestored(listOf(soleTarget()))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            // Clears whatever the board currently holds, one target at a time, until level 11.
            while (game.stateFlow.value.field.level < 11) {
                val targetId =
                    game.stateFlow.value.targets
                        .first { it.isActive }
                        .id
                game.targetClicked(targetId)
                testScheduler.runCurrent()
            }

            assertEquals(3, game.stateFlow.value.field.lifeCount)
            assertTrue(events.filterIsInstance<GameEvent.LifeGranted>().isEmpty())
        }

    @Test
    fun `restoring a session already sitting on a qualifying level does not grant a second time`() =
        runTest {
            // Field(level = 5, lifeCount = 2) is exactly what persistence would hold right after the
            // grant this file's first test proved happens on 4 -> 5. fieldRestored/targetsRestored
            // never call updateLevel, so nothing here should touch lifeCount again.
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
            game.start()
            game.fieldRestored(Field(id = 1, level = 5, lifeCount = 2, isClosed = false))
            game.targetsRestored(listOf(soleTarget(value = 5)))
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            // Partial taps and quiet ticks - a resumed, still-level-5 session - not a level-up.
            repeat(3) { game.targetClicked(1) }
            repeat(50) { game.tick(10) }
            testScheduler.runCurrent()

            assertEquals(5, game.stateFlow.value.field.level)
            assertEquals(2, game.stateFlow.value.field.lifeCount)
            assertTrue(events.filterIsInstance<GameEvent.LifeGranted>().isEmpty())
        }

    @Test
    fun `LifeGranted fires once on the level-up tick and not again on any later tick`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, appearanceDelayMs = 0),
                    backgroundScope,
                    Random(1),
                )
            game.start()
            // lifeCount starts at 2, not 1: the breakout below costs a life on its own (2 -> 1)
            // before the level-up it also causes grants one back (1 -> 2) - two independent effects
            // on the same tick, which is exactly what the event ordering in Game.tick is meant to
            // keep straight (see the fieldAfterBreakout comparison there).
            game.fieldRestored(Field(id = 1, level = 4, lifeCount = 2, isClosed = false))
            game.targetsRestored(
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 5,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 1000, // four 250ms ticks below exactly clear this
                        isVisible = true,
                    ),
                ),
            )
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            repeat(4) { game.tick(250) } // the breakout that also levels 4 -> 5
            repeat(50) { game.tick(250) } // many quiet ticks afterward
            testScheduler.runCurrent()

            assertEquals(5, game.stateFlow.value.field.level)
            assertEquals(listOf(GameEvent.LifeGranted(2)), events.filterIsInstance<GameEvent.LifeGranted>())
        }
}
