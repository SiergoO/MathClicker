package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// Split out of GameTest to keep both files under detekt's LargeClass threshold.
@OptIn(ExperimentalCoroutinesApi::class)
class GameComboTest {
    @Test
    fun `a press that scores on one target gives a combo of one however many presses precede it`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 1000)))

            suspend fun fireClean() {
                val field = game.stateFlow.value.field
                game.fieldRestored(
                    field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1),
                )
                game.fireButtonClicked()
            }

            fireClean()
            assertEquals(1, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(1, game.stateFlow.value.field.score)

            fireClean()
            fireClean()
            assertEquals(1, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(3, game.stateFlow.value.field.score)
        }

    @Test
    fun `a press that scores on two targets gives a combo of two and pays the square`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1000),
                    scheduledTarget(id = 2, columnId = 1, value = 1000),
                ),
            )
            val field = game.stateFlow.value.field
            game.fieldRestored(field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1))

            game.fireButtonClicked()

            assertEquals(2, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(4, game.stateFlow.value.field.score)
        }

    @Test
    fun `a failure alongside a success no longer wipes the combo - the MC-95 bug`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            val steady = scheduledTarget(id = 1, value = 1000)
            val dormant = scheduledTarget(id = 2, columnId = 1, value = 2, appearanceDelayMs = 1)
            game.targetsRestored(listOf(steady, dormant))

            suspend fun fire(digit: Int) {
                val field = game.stateFlow.value.field
                game.fieldRestored(
                    field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = digit),
                )
                game.fireButtonClicked()
            }

            fire(1)
            fire(1)
            assertEquals(2, game.stateFlow.value.field.score)

            // Reveal dormant and overshoot it (2 - 5 < 0) in the same press steady still succeeds in.
            game.targetsRestored(
                game.stateFlow.value.targets
                    .map { if (it.id == 2) it.copy(appearsAtMs = 0) else it },
            )

            fire(5)

            assertEquals(1, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(7, game.stateFlow.value.field.score)
            assertFalse(
                game.stateFlow.value.targets
                    .first { it.id == 2 }
                    .isProfitable,
            )
        }

    @Test
    fun `a press that scores on nothing drops the combo to zero`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 2), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 2)))
            val field = game.stateFlow.value.field
            game.fieldRestored(
                field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 5,
                    bonusMultiplier = 4,
                ),
            )

            game.fireButtonClicked()

            assertEquals(0, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(0, game.stateFlow.value.field.score)
        }

    @Test
    fun `a breakout clears the combo - driven through tick`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            val steady = scheduledTarget(id = 1, value = 1000)
            val fallingOut = scheduledTarget(id = 2, columnId = 1, value = 5, fallenMs = 90, lifetimeMs = 100)
            game.targetsRestored(listOf(steady, fallingOut))
            val field = game.stateFlow.value.field
            game.fieldRestored(field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1))
            game.fireButtonClicked()
            assertEquals(2, game.stateFlow.value.field.bonusMultiplier)

            game.tick(50) // fallingOut's fallenMs (90) + 50 clears its lifetimeMs of 100

            assertEquals(0, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(2, game.stateFlow.value.field.lifeCount)
        }

    @Test
    fun `the combo has no ceiling of its own - only the board bounds it`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 12, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                (1..12).map { scheduledTarget(id = it, columnId = it % 3, value = 1000) },
            )
            val field = game.stateFlow.value.field
            game.fieldRestored(field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1))

            game.fireButtonClicked()

            assertEquals(12, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(144, game.stateFlow.value.field.score)
        }

    @Test
    fun `a press whose award overflows Int saturates instead of wrapping`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 2_000_000_000),
                    scheduledTarget(id = 2, columnId = 1, value = 2_000_000_000),
                ),
            )
            // Raw award 2e9 is just inside Int; the combo of two takes the product to 4e9, which is not.
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 1_000_000_000,
                ),
            )

            game.fireButtonClicked()

            assertEquals(Int.MAX_VALUE, game.stateFlow.value.field.score)
        }

    @Test
    fun `scoring an identical board in two target orders yields the same field - MC-39 reverses ASK-7`() =
        runTest {
            val sessionHelper = FakeSessionHelper()
            val gameA = Game(sessionHelper, backgroundScope, Random(1))
            val gameB = Game(sessionHelper, backgroundScope, Random(1))
            val baseField =
                Field(
                    id = 1,
                    level = 1,
                    score = 100,
                    lifeCount = 3,
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 5,
                )
            gameA.fieldRestored(baseField)
            gameB.fieldRestored(baseField)
            val overshoot = scheduledTarget(id = 1, value = 2)
            val success = scheduledTarget(id = 2, columnId = 1, value = 10)
            gameA.targetsRestored(listOf(overshoot, success))
            gameB.targetsRestored(listOf(success, overshoot))

            gameA.fireButtonClicked()
            gameB.fireButtonClicked()

            // Old code (ASK-7, kept intentionally until this task) scored this 95 or 100 depending on
            // which of the two targets performOperation folded first; the two never agreed.
            assertEquals(gameA.stateFlow.value.field, gameB.stateFlow.value.field)
            assertEquals(105, gameA.stateFlow.value.field.score)
        }
}
