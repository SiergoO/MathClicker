package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Mirrors Game's own private succeedsAgainst - see GameOperationDrawTest for the same duplication.
private fun Target.isReachableBy(
    sign: OperationSign,
    digit: Int,
): Boolean =
    when (sign) {
        OperationSign.DIVISION -> digit != 0 && value % digit == 0
        OperationSign.SUBTRACTION -> value - digit >= 0
    }

// 20 is BoosterDropRule's own guaranteed bound (see BoosterDropRuleTest), so a booster must have
// dropped into the preview by the time this many fires have happened.
private const val GUARANTEED_FIRE_COUNT = 20

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoosterTest {
    @Test
    fun `firing enough times puts a booster in the preview then promotes it to the centre on the next fire`() =
        runTest {
            val game =
                Game(
                    FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000),
                    backgroundScope,
                    Random(1),
                    boostersEnabled = true,
                )
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 1,
                ),
            )

            var fireCount = 0
            while (game.stateFlow.value.field.nextAction !is FieldAction.BoosterAction &&
                fireCount < GUARANTEED_FIRE_COUNT
            ) {
                game.fireButtonClicked()
                fireCount++
            }
            val afterDrop = game.stateFlow.value.field
            assertIs<FieldAction.BoosterAction>(
                afterDrop.nextAction,
                "no booster dropped within $GUARANTEED_FIRE_COUNT fires",
            )
            assertIs<FieldAction.Operation>(afterDrop.currentAction)

            game.fireButtonClicked()

            val afterPromotion = game.stateFlow.value.field
            assertEquals(afterDrop.nextAction, afterPromotion.currentAction)
            assertIs<FieldAction.Operation>(afterPromotion.nextAction)
        }

    @Test
    fun `firing a centre booster performs no operation and leaves targets combo and score untouched`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            // An empty-board fire consumes the opening-promotion check before the real trace below,
            // so that check's own redraw cannot interfere with the assertions.
            game.fireButtonClicked()
            val targets = listOf(scheduledTarget(id = 1, value = 5), scheduledTarget(id = 2, columnId = 1, value = 3))
            game.targetsRestored(targets)
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    score = 42,
                    bonusMultiplier = 7,
                    currentBooster = Booster.FREEZE,
                ),
            )

            game.fireButtonClicked()

            assertEquals(targets, game.stateFlow.value.targets)
            assertEquals(42, game.stateFlow.value.field.score)
            assertEquals(7, game.stateFlow.value.field.bonusMultiplier)
            assertIs<FieldAction.Operation>(game.stateFlow.value.field.currentAction, "the booster did not advance")
        }

    @Test
    fun `firing twice through a promoted booster never re-fires the pair copied under it`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 5),
                    scheduledTarget(id = 2, columnId = 1, value = 3),
                ),
            )
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 5,
                    nextOperationSign = OperationSign.SUBTRACTION,
                    nextOperationDigit = 5,
                    nextBooster = Booster.FREEZE,
                ),
            )

            // -5 against {5, 3}: zeroes id 1, grows id 2 to 8, and promotes the pre-set booster in -
            // copying this same SUBTRACTION/5 pair into currentOperationSign/currentOperationDigit too.
            game.fireButtonClicked()
            val afterOperation = game.stateFlow.value
            assertIs<FieldAction.BoosterAction>(afterOperation.field.currentAction)
            assertEquals(8, afterOperation.targets.first { it.id == 2 }.value)
            val scoreAfterOperation = afterOperation.field.score

            game.fireButtonClicked()

            val afterBooster = game.stateFlow.value
            assertEquals(8, afterBooster.targets.first { it.id == 2 }.value, "the copied -5 pair fired again")
            assertEquals(scoreAfterOperation, afterBooster.field.score)
        }

    @Test
    fun `stashBooster moves the centre booster into the first free slot and promotes the next action`() =
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
                    currentBooster = Booster.FREEZE,
                    boosterStash = emptyList(),
                ),
            )

            game.stashBooster()

            val field = game.stateFlow.value.field
            assertEquals(listOf(Booster.FREEZE), field.boosterStash)
            assertIs<FieldAction.Operation>(field.currentAction, "the pre-set next operation should have moved in")
        }

    @Test
    fun `stashBooster is rejected when the centre holds an operation`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            val before = game.stateFlow.value.field

            game.stashBooster()

            assertEquals(before, game.stateFlow.value.field)
        }

    @Test
    fun `stashBooster is rejected when the stash is already full`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            val fullStash = listOf(Booster.FREEZE, Booster.REWIND, Booster.SHIELD)
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentBooster = Booster.ICE_PICK,
                    boosterStash = fullStash,
                ),
            )
            val before = game.stateFlow.value.field

            game.stashBooster()

            assertEquals(before, game.stateFlow.value.field)
        }

    @Test
    fun `no booster ever drops while boostersEnabled is false`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1_000_000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1_000_000)))
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 1,
                ),
            )

            repeat(GUARANTEED_FIRE_COUNT) { game.fireButtonClicked() }

            assertIs<FieldAction.Operation>(game.stateFlow.value.field.currentAction)
            assertIs<FieldAction.Operation>(game.stateFlow.value.field.nextAction)
        }

    @Test
    fun `the operation guarantee still holds for every operation drawn once boosters can appear`() =
        runTest {
            var checkedOperations = 0
            for (trial in 0 until 200) {
                val seed = trial.toLong()
                val sessionHelper = SessionHelperImpl(random = Random(seed))
                val game = Game(sessionHelper, backgroundScope, Random(seed), boostersEnabled = true)
                game.createField(1)
                game.createTargets()

                repeat(25) { fireIndex ->
                    repeat(1 + (fireIndex % 5)) { game.tick(250) }
                    game.fireButtonClicked()

                    val field = game.stateFlow.value.field
                    val action = field.nextAction
                    if (action is FieldAction.Operation) {
                        val visibleActiveTargets =
                            game.stateFlow.value.targets
                                .filter { it.isActive && it.isVisible(field.gameTimeMs) }
                        if (visibleActiveTargets.isNotEmpty()) {
                            checkedOperations++
                            assertTrue(
                                visibleActiveTargets.any { it.isReachableBy(action.sign, action.digit) },
                                "trial $trial fire $fireIndex drew a board-wide dud operation",
                            )
                        }
                    }
                }
            }
            assertTrue(checkedOperations > 0, "no operation draws were exercised")
        }
}
