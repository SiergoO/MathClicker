package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

// MC-39: the combo is a streak over presses, carried on Field.bonusMultiplier. Split out of
// GameTest to keep both files under detekt's LargeClass threshold.
@OptIn(ExperimentalCoroutinesApi::class)
class GameStreakTest {
    @Test
    fun `a clean press raises the streak by one - three in a row give three`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1000),
                ),
            )

            suspend fun fireClean() {
                val field = game.stateFlow.value.field
                game.fieldRestored(
                    field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1),
                )
                game.fireButtonClicked()
            }

            fireClean()
            // Streak was 0 going in: max(1, streak) must still apply so this scores 1, not 0.
            assertEquals(1, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(1, game.stateFlow.value.field.score)

            fireClean()
            fireClean()
            assertEquals(3, game.stateFlow.value.field.bonusMultiplier)
        }

    @Test
    fun `a press with two successful targets raises the streak by one - not per target`() =
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

            assertEquals(1, game.stateFlow.value.field.bonusMultiplier)
        }

    @Test
    fun `a press with one failure among successes resets the streak without reducing the score`() =
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

            fire(1) // dormant is not visible yet: a clean press against steady alone, streak 0 -> 1
            fire(1) // streak 1 -> 2
            assertEquals(2, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(3, game.stateFlow.value.field.score)

            // Reveal dormant and overshoot it (2 - 5 < 0) in the same press steady still succeeds in.
            game.targetsRestored(
                game.stateFlow.value.targets
                    .map { if (it.id == 2) it.copy(appearsAtMs = 0) else it },
            )

            fire(5)

            assertEquals(0, game.stateFlow.value.field.bonusMultiplier)
            // Only steady's digit counts, at the floored multiplier of one - never negative, never a
            // drop from what the run already had (the MC-47 bug this task closes).
            assertEquals(8, game.stateFlow.value.field.score)
            assertFalse(
                game.stateFlow.value.targets
                    .first { it.id == 2 }
                    .isProfitable,
            )
        }

    @Test
    fun `a breakout resets the streak - driven through tick`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            val steady = scheduledTarget(id = 1, value = 1000)
            val fallingOut = scheduledTarget(id = 2, columnId = 1, value = 5, fallenMs = 90, lifetimeMs = 100)
            game.targetsRestored(listOf(steady, fallingOut))
            val field = game.stateFlow.value.field
            game.fieldRestored(field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1))
            game.fireButtonClicked()
            assertEquals(1, game.stateFlow.value.field.bonusMultiplier)

            game.tick(50) // fallingOut's fallenMs (90) + 50 clears its lifetimeMs of 100

            assertEquals(0, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(2, game.stateFlow.value.field.lifeCount)
        }

    @Test
    fun `the streak has no ceiling and keeps paying past ten`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1000),
                ),
            )

            suspend fun fireClean() {
                val field = game.stateFlow.value.field
                game.fieldRestored(
                    field.copy(currentOperationSign = OperationSign.SUBTRACTION, currentOperationDigit = 1),
                )
                game.fireButtonClicked()
            }

            repeat(11) { fireClean() }
            assertEquals(11, game.stateFlow.value.field.bonusMultiplier)

            fireClean()

            // Each press scores 1 raw, multiplied by the streak it lands on, so twelve clean presses
            // are 1+2+...+12. Under the old ceiling of ten this was 75.
            assertEquals(12, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(78, game.stateFlow.value.field.score)
        }

    @Test
    fun `a press whose award overflows Int saturates instead of wrapping`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    scheduledTarget(id = 1, value = 1_000_000),
                ),
            )
            // A subtraction scores its own digit, so this press is worth 100 000 raw against a
            // streak of three million - about 3e11, well past what an Int holds.
            game.fieldRestored(
                game.stateFlow.value.field.copy(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 100_000,
                    bonusMultiplier = 3_000_000,
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
