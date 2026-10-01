package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoosterUsageTest {
    @Test
    fun `ice pick destroys the next tapped bubble and awards its remaining value when profitable`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 7)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.ICE_PICK, score = 0),
            )
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.fireButtonClicked() // arms the pick
            game.targetClicked(1)
            testScheduler.runCurrent()

            val target =
                game.stateFlow.value.targets
                    .single()
            assertFalse(target.isActive)
            assertEquals(0, target.value)
            assertEquals(7, game.stateFlow.value.field.score)
            assertEquals(GameEvent.TargetZeroed(1, 7), events.filterIsInstance<GameEvent.TargetZeroed>().single())
        }

    @Test
    fun `ice pick awards nothing against an unprofitable bubble`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 7, isProfitable = false)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.ICE_PICK, score = 10),
            )
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.fireButtonClicked() // arms the pick
            game.targetClicked(1)
            testScheduler.runCurrent()

            assertEquals(10, game.stateFlow.value.field.score)
            assertEquals(GameEvent.TargetZeroed(1, 0), events.filterIsInstance<GameEvent.TargetZeroed>().single())
        }

    @Test
    fun `a used ice pick is spent rather than returned to the stash`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(scheduledTarget(id = 1, value = 7), scheduledTarget(id = 2, columnId = 1, value = 7)),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentBooster = Booster.ICE_PICK,
                    boosterStash = listOf(Booster.FREEZE),
                ),
            )

            game.fireButtonClicked() // arms the pick
            game.targetClicked(1)

            assertEquals(listOf(Booster.FREEZE), game.stateFlow.value.field.boosterStash, "the used pick came back")

            game.targetClicked(2) // armed flag is gone: an ordinary tap, not another outright kill
            val second =
                game.stateFlow.value.targets
                    .first { it.id == 2 }
            assertEquals(6, second.value)
            assertTrue(second.isActive)
        }

    @Test
    fun `an armed ice pick ignores a tap on an inactive or not-yet-visible bubble`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 7, isActive = false),
                    scheduledTarget(id = 2, columnId = 1, value = 7, appearanceDelayMs = 100_000),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.ICE_PICK),
            )
            val events = mutableListOf<GameEvent>()
            backgroundScope.launch { game.events.collect { events.add(it) } }
            testScheduler.runCurrent()

            game.fireButtonClicked() // arms the pick
            game.targetClicked(1) // inactive
            game.targetClicked(2) // not yet visible
            testScheduler.runCurrent()

            assertTrue(events.filterIsInstance<GameEvent.TargetZeroed>().isEmpty())
            assertEquals(0, game.stateFlow.value.field.score)

            game.targetsRestored(
                game.stateFlow.value.targets
                    .map { if (it.id == 1) it.copy(isActive = true, value = 9) else it },
            )
            game.targetClicked(1) // still armed: this tap is the one that lands
            testScheduler.runCurrent()

            assertEquals(GameEvent.TargetZeroed(1, 9), events.filterIsInstance<GameEvent.TargetZeroed>().single())
        }

    @Test
    fun `firing an operation does not disarm an already-armed ice pick`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1000)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentBooster = Booster.ICE_PICK,
                    nextOperationSign = OperationSign.SUBTRACTION,
                    nextOperationDigit = 5,
                ),
            )

            game.fireButtonClicked() // arms the pick, promotes the pre-set operation into current
            assertIs<FieldAction.Operation>(game.stateFlow.value.field.currentAction)

            game.fireButtonClicked() // fires that operation while the pick stays armed
            assertEquals(
                995,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )

            game.targetClicked(1) // still armed: destroys outright instead of -1
            assertEquals(
                0,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `disarmIcePick clears the armed flag and returns the pick to the first free stash slot`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 7)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentBooster = Booster.ICE_PICK,
                    boosterStash = listOf(Booster.SHIELD),
                ),
            )
            game.fireButtonClicked() // arms it

            game.disarmIcePick()

            assertEquals(listOf(Booster.SHIELD, Booster.ICE_PICK), game.stateFlow.value.field.boosterStash)
            game.targetClicked(1) // disarmed: an ordinary decrement, not an outright destroy
            assertEquals(
                6,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `disarmIcePick is a no-op when nothing is armed`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            val before = game.stateFlow.value.field

            game.disarmIcePick()

            assertEquals(before, game.stateFlow.value.field)
        }

    @Test
    fun `disarmIcePick is refused when the stash is full and leaves the pick armed`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 7)))
            val fullStash = listOf(Booster.FREEZE, Booster.REWIND, Booster.SHIELD)
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.ICE_PICK, boosterStash = fullStash),
            )
            game.fireButtonClicked() // arms it

            game.disarmIcePick()

            assertEquals(fullStash, game.stateFlow.value.field.boosterStash)
            game.targetClicked(1) // still armed: destroys outright instead of -1
            assertEquals(
                0,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `createField resets freeze the armed ice pick and the shield for the new session`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 2, targetValue = 5),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 5)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentBooster = Booster.ICE_PICK,
                    boosterStash = listOf(Booster.FREEZE, Booster.SHIELD),
                ),
            )
            game.fireButtonClicked() // arms the pick
            game.applyBoosterFromStash(0) // freeze
            game.applyBoosterFromStash(0) // shield, now in the freed slot 0

            game.createField(2) // restart reuses the same Game instance

            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 5, lifetimeMs = 100),
                    scheduledTarget(id = 2, columnId = 1, value = 5, lifetimeMs = 1_000_000),
                ),
            )
            val startingLives = game.stateFlow.value.field.lifeCount

            game.tick(250) // clears id 1's 100ms lifetime in one step

            assertTrue(game.stateFlow.value.field.gameTimeMs > 0, "freeze survived createField")
            assertEquals(startingLives - 1, game.stateFlow.value.field.lifeCount, "shield survived createField")

            game.targetClicked(2) // the ice pick is gone: an ordinary decrement, not an outright destroy
            assertEquals(
                4,
                game.stateFlow.value.targets
                    .first { it.id == 2 }
                    .value,
                "ice pick survived createField",
            )
        }

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

    @Test
    fun `applyBoosterFromStash starts the booster's effect and frees that slot without touching the action stream`() =
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
                    boosterStash = listOf(Booster.FREEZE, Booster.SHIELD),
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 1,
                ),
            )
            val actionBefore = game.stateFlow.value.field.currentAction
            val startingLives = game.stateFlow.value.field.lifeCount

            game.applyBoosterFromStash(1) // the SHIELD slot

            assertEquals(listOf(Booster.FREEZE), game.stateFlow.value.field.boosterStash)
            assertEquals(actionBefore, game.stateFlow.value.field.currentAction)

            repeat(5) { game.tick(50) } // clears the 100ms lifetime
            assertEquals(
                startingLives,
                game.stateFlow.value.field.lifeCount,
                "the stashed shield did not absorb the breakout",
            )
        }

    @Test
    fun `applyBoosterFromStash is ignored for an index outside the stash`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(boosterStash = listOf(Booster.FREEZE)),
            )
            val before = game.stateFlow.value.field

            game.applyBoosterFromStash(1)

            assertEquals(before, game.stateFlow.value.field)
        }
}
