package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Events is a SharedFlow, not a diff of stateFlow, precisely because StateFlow conflates -
// these tests exist to prove properties a state diff could never establish (two same-frame zeroes,
// replay = 0, an overflowing buffer that still lets tick proceed).
@OptIn(ExperimentalCoroutinesApi::class)
class GameEventTest {
    @Test
    fun `two targets zeroed in one frame produce two TargetZeroed events`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 3, targetValue = 1), backgroundScope, Random(1))
            game.createField(1)
            val visibleTargets = (1..3).map { scheduledTarget(id = it, columnId = it - 1, value = 1) }
            game.targetsRestored(visibleTargets)
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()
            val (idA, idB) = visibleTargets.take(2).map { it.id }

            // Two separate locked calls - exactly what two rapid taps in the same UI frame are - not
            // one call touching two targets: a stateFlow diff would only ever see the final value.
            game.targetClicked(idA)
            game.targetClicked(idB)
            testScheduler.runCurrent()

            val zeroed = events.filterIsInstance<GameEvent.TargetZeroed>()
            assertEquals(2, zeroed.size)
            assertEquals(setOf(idA, idB), zeroed.map { it.id }.toSet())
        }

    @Test
    fun `targetClicked emits TargetZeroed only on the tap that reaches zero and not every tap`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 10), backgroundScope, Random(1))
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()
            val id =
                game.stateFlow.value.targets
                    .first()
                    .id

            repeat(9) { game.targetClicked(id) } // value 10 -> 1, none of these reach zero
            testScheduler.runCurrent()
            assertTrue(events.filterIsInstance<GameEvent.TargetZeroed>().isEmpty())

            game.targetClicked(id) // value 1 -> 0, the only tap that should emit
            testScheduler.runCurrent()

            assertEquals(1, events.filterIsInstance<GameEvent.TargetZeroed>().size)
        }

    @Test
    fun `a hundred quiet ticks with nothing happening emit no events`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, openingOffsetMs = 1_000_000),
                    backgroundScope,
                    Random(1),
                )
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            repeat(100) { game.tick(1) }
            testScheduler.runCurrent()

            assertTrue(events.isEmpty())
        }

    @Test
    fun `each event carries its game-state payload`() =
        runTest {
            // TargetZeroed.awarded
            run {
                val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
                game.createField(1)
                game.createTargets()
                testScheduler.runCurrent()
                val events = mutableListOf<GameEvent>()
                backgroundScope.launch { game.events.collect { events.add(it) } }
                testScheduler.runCurrent()
                val id =
                    game.stateFlow.value.targets
                        .first()
                        .id

                game.targetClicked(id)
                testScheduler.runCurrent()

                val zeroed = events.filterIsInstance<GameEvent.TargetZeroed>().single()
                assertEquals(id, zeroed.id)
                assertEquals(1, zeroed.awarded)
            }

            // OperationResolved.gained / streak
            run {
                val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1000), backgroundScope, Random(1))
                game.createField(1)
                game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1000)))
                game.fieldRestored(
                    game.stateFlow.value.field.copy(
                        currentOperationSign = OperationSign.SUBTRACTION,
                        currentOperationDigit = 5,
                    ),
                )
                val events = mutableListOf<GameEvent>()
                backgroundScope.launch { game.events.collect { events.add(it) } }
                testScheduler.runCurrent()

                game.fireButtonClicked()
                testScheduler.runCurrent()

                val resolved = events.filterIsInstance<GameEvent.OperationResolved>().single()
                // One target changed, so the streak resets; raw score is the digit (5), gained = 5 * 1.
                assertEquals(5, resolved.gained)
                assertEquals(0, resolved.streak)
            }

            // TargetBrokeOut.livesLeft
            run {
                val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
                game.createField(1)
                game.targetsRestored(
                    listOf(scheduledTarget(id = 1, value = 5, fallenMs = 90, lifetimeMs = 100)),
                )
                val events = mutableListOf<GameEvent>()
                backgroundScope.launch { game.events.collect { events.add(it) } }
                testScheduler.runCurrent()

                game.tick(50) // fallenMs (90) + 50 clears lifetimeMs of 100
                testScheduler.runCurrent()

                val brokeOut = events.filterIsInstance<GameEvent.TargetBrokeOut>().single()
                assertEquals(1, brokeOut.id)
                assertEquals(2, brokeOut.livesLeft) // default lifeCount 3, minus this one breakout
            }

            // LevelUp.level
            run {
                val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
                game.createField(1)
                game.createTargets()
                testScheduler.runCurrent()
                val events = mutableListOf<GameEvent>()
                backgroundScope.launch { game.events.collect { events.add(it) } }
                testScheduler.runCurrent()

                repeat(4) { game.tick(250) } // lifetimeMs 1000 at level 1, delay 0
                testScheduler.runCurrent()

                val levelUp = events.filterIsInstance<GameEvent.LevelUp>().single()
                assertEquals(2, levelUp.level)
            }
        }

    @Test
    fun `replay is zero - a subscriber that arrives after an event does not receive it`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(1))
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val id =
                game.stateFlow.value.targets
                    .first()
                    .id

            game.targetClicked(id)
            testScheduler.runCurrent()

            val lateEvents = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { lateEvents.add(it) } }
            testScheduler.runCurrent()

            assertTrue(lateEvents.isEmpty())
        }

    @Test
    fun `overflowing the buffer drops the oldest never throws and tick still advances`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1))
            game.createField(1)
            val events = mutableListOf<GameEvent>()
            // Subscribed but never drained until after the flood below: a suspended collector still
            // occupies the shared buffer, which is what makes DROP_OLDEST vs SUSPEND observable at
            // all - with no subscriber at all, replay = 0 means nothing is retained either way.
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            // 40 simultaneous breakouts in one tick(), well past extraBufferCapacity of 32. tick runs
            // inside the mutex on the frame path, so it must return rather than block on a full buffer.
            val fallingTargets =
                (1..40).map { id -> scheduledTarget(id = id, value = 5, fallenMs = 99, lifetimeMs = 100) }
            game.targetsRestored(fallingTargets)

            game.tick(1)
            testScheduler.runCurrent()

            assertTrue(game.stateFlow.value.field.isClosed)
            assertEquals(0, game.stateFlow.value.field.lifeCount)

            // This same tick also empties the board (all 40 targets went inactive), but MC-59 means
            // a field this closed does not level up - so only 41 events (40 TargetBrokeOut + this
            // GameOver) compete for the buffer's 32 slots: the oldest 9 - TargetBrokeOut for ids
            // 1..9 - are the ones DROP_OLDEST discards.
            val brokeOutIds = events.filterIsInstance<GameEvent.TargetBrokeOut>().map { it.id }
            assertEquals((10..40).toList(), brokeOutIds)
            assertEquals(0, events.filterIsInstance<GameEvent.LevelUp>().size)
            assertEquals(1, events.filterIsInstance<GameEvent.GameOver>().size)
        }

    @Test
    fun `clearing the last target by tapping levels up through the activeTargetsAbsent path`() =
        runTest {
            // No tick() call anywhere below: the only way a LevelUp can reach the collector here is
            // the collector's own activeTargetsAbsent(), the second path a level-up arrives by.
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(22))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()
            val id =
                game.stateFlow.value.targets
                    .first()
                    .id

            game.targetClicked(id)
            testScheduler.runCurrent()

            assertEquals(2, game.stateFlow.value.field.level)
            val levelUps = events.filterIsInstance<GameEvent.LevelUp>()
            assertEquals(1, levelUps.size)
            assertEquals(2, levelUps.single().level)
        }

    @Test
    fun `a subscriber woken by an event always sees the state that event describes`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1000)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 5,
                ),
            )

            var scoreWhenNotified = -1
            // Dispatchers.Unconfined resumes a suspended collector synchronously, inline, inside the
            // tryEmit call that wakes it - so this reads stateFlow at the exact instant the event
            // fires rather than after some later dispatch, where the state would already have
            // settled regardless of which line ran first. This is the direct kill for the emit
            // being moved ahead of the _stateFlow.value assignment.
            backgroundScope.launch(Dispatchers.Unconfined) {
                game.events.collect { event ->
                    if (event is GameEvent.OperationResolved) {
                        scoreWhenNotified = game.stateFlow.value.field.score
                    }
                }
            }

            game.fireButtonClicked()

            assertEquals(game.stateFlow.value.field.score, scoreWhenNotified)
        }
}
