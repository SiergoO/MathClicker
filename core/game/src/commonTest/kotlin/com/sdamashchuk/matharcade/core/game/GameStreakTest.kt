package com.sdamashchuk.matharcade.core.game

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
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
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 1000,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 100_000,
                        isVisible = true,
                    ),
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
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 1000,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 100_000,
                        isVisible = true,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 1,
                        columnId = 1,
                        value = 1000,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 100_000,
                        isVisible = true,
                    ),
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
            val steady =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 1000,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 100_000,
                    isVisible = true,
                )
            val dormant =
                Target(
                    id = 2,
                    relatedFieldId = 1,
                    columnId = 1,
                    value = 2,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 100_000,
                )
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
                    .map { if (it.id == 2) it.copy(isVisible = true) else it },
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
            val steady =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 1000,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 100_000,
                    isVisible = true,
                )
            val fallingOut =
                Target(
                    id = 2,
                    relatedFieldId = 1,
                    columnId = 1,
                    value = 5,
                    fallenMs = 90,
                    appearanceDelayMs = 0,
                    lifetimeMs = 100,
                    isVisible = true,
                )
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
    fun `the streak caps at ten and a press past the cap still scores at ten`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1000), backgroundScope, Random(1))
            game.createField(1)
            game.targetsRestored(
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 1,
                        columnId = 0,
                        value = 1000,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 100_000,
                        isVisible = true,
                    ),
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
            assertEquals(10, game.stateFlow.value.field.bonusMultiplier)

            fireClean()

            assertEquals(10, game.stateFlow.value.field.bonusMultiplier)
            assertEquals(75, game.stateFlow.value.field.score)
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
            val overshoot =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 0,
                    value = 2,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 100_000,
                    isVisible = true,
                )
            val success =
                Target(
                    id = 2,
                    relatedFieldId = 1,
                    columnId = 1,
                    value = 10,
                    fallenMs = 0,
                    appearanceDelayMs = 0,
                    lifetimeMs = 100_000,
                    isVisible = true,
                )
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
