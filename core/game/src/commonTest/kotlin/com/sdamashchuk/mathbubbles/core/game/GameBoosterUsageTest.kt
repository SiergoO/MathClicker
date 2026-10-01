package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.game.model.IcePickSource
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
            assertEquals(
                GameEvent.TargetZeroed(1, 7, viaIcePick = true),
                events.filterIsInstance<GameEvent.TargetZeroed>().single(),
            )
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
            assertEquals(
                GameEvent.TargetZeroed(1, 0, viaIcePick = true),
                events.filterIsInstance<GameEvent.TargetZeroed>().single(),
            )
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

            assertEquals(
                GameEvent.TargetZeroed(1, 9, viaIcePick = true),
                events.filterIsInstance<GameEvent.TargetZeroed>().single(),
            )
        }

    @Test
    fun `ice pick armed from the fire button holds the centre until the destroying tap then promotes once`() =
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
                    nextOperationSign = OperationSign.SUBTRACTION,
                    nextOperationDigit = 5,
                ),
            )

            game.fireButtonClicked() // arms the pick without promoting
            assertEquals(Booster.ICE_PICK, game.stateFlow.value.field.currentBooster)

            game.fireButtonClicked() // re-arming while already armed: still just the pick, still not promoted
            assertEquals(Booster.ICE_PICK, game.stateFlow.value.field.currentBooster, "still holding the centre")

            game.targetClicked(1) // the destroying tap

            // A second promotion would have discarded the pre-arm next (5) into history instead of
            // landing it in current, and a second drop roll would have left the counter at 2, not 1.
            assertEquals(
                FieldAction.Operation(OperationSign.SUBTRACTION, 5),
                game.stateFlow.value.field.currentAction,
                "the destroying tap should promote exactly once",
            )
            assertEquals(1, game.stateFlow.value.field.boosterDropCounter, "exactly one drop roll should have run")
        }

    @Test
    fun `ice pick armed from a stash slot empties that slot on the destroying tap`() =
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
                    .copy(boosterStash = listOf(Booster.FREEZE, Booster.ICE_PICK)),
            )

            game.applyBoosterFromStash(1) // arms the pick in slot 1, does not free it
            assertEquals(listOf(Booster.FREEZE, Booster.ICE_PICK), game.stateFlow.value.field.boosterStash)

            game.targetClicked(1) // the destroying tap

            assertEquals(listOf(Booster.FREEZE), game.stateFlow.value.field.boosterStash)
        }

    @Test
    fun `firing an operation still works while the ice pick is armed in a stash slot`() =
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
                    boosterStash = listOf(Booster.ICE_PICK),
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 2,
                ),
            )

            game.applyBoosterFromStash(0) // arms the pick, the centre is untouched
            game.fireButtonClicked() // fires the centre operation normally

            assertEquals(
                5,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
            assertEquals(
                listOf(Booster.ICE_PICK),
                game.stateFlow.value.field.boosterStash,
                "the armed pick should stay put",
            )
        }

    @Test
    fun `tapping the fire button again disarms a pick armed there without moving it`() =
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
                    .copy(currentBooster = Booster.ICE_PICK),
            )
            game.fireButtonClicked() // arms it

            game.disarmIcePick()

            assertEquals(Booster.ICE_PICK, game.stateFlow.value.field.currentBooster, "the pick should stay put")
            game.targetClicked(1) // disarmed: an ordinary decrement, not an outright destroy
            assertEquals(
                6,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `tapping the stash slot again disarms a pick armed there without moving it`() =
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
                    .copy(boosterStash = listOf(Booster.ICE_PICK)),
            )
            game.applyBoosterFromStash(0) // arms it

            game.disarmIcePick()

            assertEquals(listOf(Booster.ICE_PICK), game.stateFlow.value.field.boosterStash, "the pick should stay put")
            game.targetClicked(1) // disarmed: an ordinary decrement, not an outright destroy
            assertEquals(
                6,
                game.stateFlow.value.targets
                    .single()
                    .value,
            )
        }

    @Test
    fun `stashBooster refuses to swipe away a pick armed in the fire button`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(currentBooster = Booster.ICE_PICK),
            )
            game.fireButtonClicked() // arms it
            val before = game.stateFlow.value.field

            game.stashBooster()

            assertEquals(before, game.stateFlow.value.field)
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
    fun `disarmIcePick succeeds even when the stash is full since it never touches the stash`() =
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
            game.targetClicked(1) // disarmed: an ordinary decrement, not an outright destroy
            assertEquals(
                6,
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

    @Test
    fun `freeing an earlier slot while the ice pick is armed later does not crash the destroying tap`() =
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
                    .copy(boosterStash = listOf(Booster.FREEZE, Booster.ICE_PICK)),
            )
            game.applyBoosterFromStash(1) // arms the pick at slot 1
            game.applyBoosterFromStash(0) // frees FREEZE at slot 0, shifting the pick down to slot 0

            game.targetClicked(1) // must free the pick, not crash on a stale index

            assertEquals(emptyList(), game.stateFlow.value.field.boosterStash)
        }

    @Test
    fun `freeing an earlier slot while the ice pick is armed later re-targets the pick instead of the wrong booster`() =
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
                    .copy(boosterStash = listOf(Booster.FREEZE, Booster.ICE_PICK, Booster.SHIELD)),
            )
            game.applyBoosterFromStash(1) // arms the pick at slot 1
            game.applyBoosterFromStash(0) // frees FREEZE at slot 0, shifting the pick down to slot 0
            assertEquals(IcePickSource.StashSlot(0), game.stateFlow.value.effects.icePickArmedFrom)

            game.targetClicked(1) // the destroying tap

            assertEquals(listOf(Booster.SHIELD), game.stateFlow.value.field.boosterStash)
        }

    @Test
    fun `freeing a later slot while the ice pick is armed leaves its own slot index unchanged`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 7),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(boosterStash = listOf(Booster.ICE_PICK, Booster.FREEZE, Booster.SHIELD)),
            )
            game.applyBoosterFromStash(0) // arms the pick at slot 0
            game.applyBoosterFromStash(2) // frees SHIELD at slot 2, after the armed slot

            assertEquals(IcePickSource.StashSlot(0), game.stateFlow.value.effects.icePickArmedFrom)
            assertEquals(listOf(Booster.ICE_PICK, Booster.FREEZE), game.stateFlow.value.field.boosterStash)
        }
}
