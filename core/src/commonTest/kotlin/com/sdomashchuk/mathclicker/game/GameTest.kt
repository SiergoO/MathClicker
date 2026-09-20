package com.sdomashchuk.mathclicker.game

import com.sdomashchuk.mathclicker.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.model.OperationSign
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeSessionHelper(
    private val targetAmount: Int = 1,
    private val targetValue: Int = 10,
    private val appearanceDelayMs: Int = 0,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(level: Int) = targetValue

    override fun getTargetLifetimeMsByLevel(level: Int) = 1000

    override fun getTargetAppearanceDelayMsById(id: Int) = appearanceDelayMs

    override fun getTargetAmountByLevel(level: Int) = targetAmount

    // Sign-dependent digits so the reproducibility test can notice a sign that came out different.
    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = if (operationSign == OperationSign.DIVISION) 2 else 3

    override fun getDivisionDigitByLevel(level: Int) = 2

    override fun getSubtractionDigitByLevel(level: Int) = 3
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameTest {
    @Test
    fun `all targets inactive increments the level and regenerates the target set`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id
            game.targetRevealed(targetId)
            testScheduler.runCurrent()

            game.targetDidBreakout(targetId)
            testScheduler.runCurrent()

            assertEquals(2, game.fieldFlow.value.level)
            assertEquals(1, game.targetsFlow.value.size)
            assertTrue(game.targetsFlow.value.all { it.isActive })
        }

    @Test
    fun `no visible targets shortens every appearance delay to zero`() =
        runTest {
            // Equal delays make the outcome independent of shortenAppearanceDelay's own random
            // sample count: whichever subset is picked, subtracting the shared delay zeroes them all.
            val game = Game(FakeSessionHelper(targetAmount = 3, appearanceDelayMs = 500), backgroundScope, Random(2))
            game.start()
            game.createField(1)

            game.createTargets()
            testScheduler.runCurrent()

            assertTrue(game.targetsFlow.value.all { it.appearanceDelayMs == 0 })
        }

    @Test
    fun `targetDidBreakout decrements life count and closes the field once it hits zero`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 3), backgroundScope, Random(3))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targets = game.targetsFlow.value

            game.targetDidBreakout(targets[0].id)
            testScheduler.runCurrent()
            assertEquals(2, game.fieldFlow.value.lifeCount)
            assertFalse(game.fieldFlow.value.isClosed)

            game.targetDidBreakout(targets[1].id)
            testScheduler.runCurrent()
            assertEquals(1, game.fieldFlow.value.lifeCount)
            assertFalse(game.fieldFlow.value.isClosed)

            game.targetDidBreakout(targets[2].id)
            testScheduler.runCurrent()
            assertEquals(0, game.fieldFlow.value.lifeCount)
            assertTrue(game.fieldFlow.value.isClosed)
        }

    @Test
    fun `seeded random reproduces the same field across separate game instances`() =
        runTest {
            val sessionHelper = FakeSessionHelper()
            val first = Game(sessionHelper, backgroundScope, Random(42))
            val second = Game(sessionHelper, backgroundScope, Random(42))

            first.createField(1)
            second.createField(1)

            assertEquals(first.fieldFlow.value.currentOperationSign, second.fieldFlow.value.currentOperationSign)
            assertEquals(first.fieldFlow.value.currentOperationDigit, second.fieldFlow.value.currentOperationDigit)
            assertEquals(first.fieldFlow.value.nextOperationSign, second.fieldFlow.value.nextOperationSign)
            assertEquals(first.fieldFlow.value.nextOperationDigit, second.fieldFlow.value.nextOperationDigit)
        }

    @Test
    fun `stop cancels the collector so a state change no longer triggers a level-up`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(4))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id
            game.targetRevealed(targetId)
            testScheduler.runCurrent()

            game.stop()
            game.targetDidBreakout(targetId)
            testScheduler.runCurrent()

            assertEquals(1, game.fieldFlow.value.level)
        }
}
