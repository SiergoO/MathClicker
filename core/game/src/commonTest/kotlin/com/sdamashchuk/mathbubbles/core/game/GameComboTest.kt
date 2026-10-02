package com.sdamashchuk.mathbubbles.core.game

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// Split out of GameTest to keep both files under detekt's LargeClass threshold.
@OptIn(ExperimentalCoroutinesApi::class)
class GameComboTest {
    @Test
    fun `two consecutive presses that each change two targets raise the multiplier twice to x3`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1000),
                    scheduledTarget(id = 2, columnId = 1, value = 1000),
                ),
            )

            suspend fun fire() {
                val field = game.stateFlow.value.field
                game.fieldRestored(
                    field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1),
                )
                game.fireButtonClicked()
            }

            fire()
            assertEquals(2, game.stateFlow.value.field.appliedMultiplier)
            assertEquals(4, game.stateFlow.value.field.score)

            fire()
            assertEquals(3, game.stateFlow.value.field.appliedMultiplier)
            assertEquals(10, game.stateFlow.value.field.score)
        }

    @Test
    fun `a press that changes one target resets the multiplier to x1`() =
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

            assertEquals(1, game.stateFlow.value.field.appliedMultiplier)
            assertEquals(0, game.stateFlow.value.field.score)
        }

    @Test
    fun `a failed overshoot still changes its target and counts toward the raise`() =
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

            // Reveal the dormant target so this press overshoots it (2 - 5 < 0) while steady still succeeds.
            game.targetsRestored(
                game.stateFlow.value.targets
                    .map { if (it.id == 2) it.copy(appearsAtMs = 0) else it },
            )

            fire(5)

            assertEquals(2, game.stateFlow.value.field.appliedMultiplier)
            assertEquals(12, game.stateFlow.value.field.score)
        }

    @Test
    fun `a plain bubble tap never resets an in-cap multiplier`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 10), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(listOf(scheduledTarget(id = 1, value = 10)))
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(bonusMultiplier = 2),
            )

            game.targetClicked(1)

            assertEquals(3, game.stateFlow.value.field.appliedMultiplier)
        }

    @Test
    fun `a booster action leaves an in-cap multiplier untouched`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1), boostersEnabled = true)
            game.createField(1)
            game.fieldRestored(
                game.stateFlow.value.field
                    .copy(bonusMultiplier = 2, currentBooster = Booster.FREEZE),
            )

            game.fireButtonClicked()

            assertEquals(2, game.stateFlow.value.field.bonusMultiplier)
        }

    @Test
    fun `arming and striking with the ice pick leaves an in-cap multiplier unchanged`() =
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
                    .copy(bonusMultiplier = 2, currentBooster = Booster.ICE_PICK),
            )

            game.fireButtonClicked()
            game.targetClicked(1)

            assertEquals(3, game.stateFlow.value.field.appliedMultiplier)
        }

    @Test
    fun `a breakout clears the multiplier - driven through tick`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            val steady = scheduledTarget(id = 1, value = 1000)
            val fallingOut = scheduledTarget(id = 2, columnId = 1, value = 5, fallenMs = 90, lifetimeMs = 100)
            game.targetsRestored(listOf(steady, fallingOut))
            val field = game.stateFlow.value.field
            game.fieldRestored(field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1))
            game.fireButtonClicked()
            assertEquals(2, game.stateFlow.value.field.appliedMultiplier)

            game.tick(50) // fallingOut's fallenMs (90) + 50 clears its lifetimeMs of 100

            assertEquals(1, game.stateFlow.value.field.appliedMultiplier)
            assertEquals(2, game.stateFlow.value.field.lifeCount)
        }

    @Test
    fun `repeated multi-target hits stop raising the multiplier at x10`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1_000_000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1_000_000),
                    scheduledTarget(id = 2, columnId = 1, value = 1_000_000),
                ),
            )

            repeat(50) {
                val field = game.stateFlow.value.field
                game.fieldRestored(
                    field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1),
                )
                game.fireButtonClicked()
            }

            assertEquals(10, game.stateFlow.value.field.appliedMultiplier)
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
    fun `scoring an identical board in two target orders yields the same field`() =
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

            assertEquals(gameA.stateFlow.value.field, gameB.stateFlow.value.field)
            assertEquals(110, gameA.stateFlow.value.field.score)
        }
}
