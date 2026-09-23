package com.sdomashchuk.mathclicker.core.game

import com.sdomashchuk.mathclicker.core.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.core.game.helper.SessionHelperImpl
import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.core.model.Target
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
    private val appearanceDelayMsById: ((Int) -> Int)? = null,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    // Offset by (level - 1) so every value matches its old constant at level 1 - the level every
    // existing test runs at - while still diverging at any other level, which is what makes a level
    // argument silently swapped for a literal (createTargets, getNextSignAndDigit, recreateField)
    // observable.
    override fun getTargetValueByLevel(level: Int) = targetValue + (level - 1)

    override fun getTargetLifetimeMsByLevel(level: Int) = 1000 + (level - 1)

    override fun getTargetAppearanceDelayMsById(id: Int) = appearanceDelayMsById?.invoke(id) ?: appearanceDelayMs

    override fun getTargetAmountByLevel(level: Int) = targetAmount + (level - 1)

    // Sign-dependent digits so the reproducibility test can notice a sign that came out different.
    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = if (operationSign == OperationSign.DIVISION) 2 + (level - 1) else 3 + (level - 1)

    override fun getDivisionDigitByLevel(level: Int) = 2 + (level - 1)

    override fun getSubtractionDigitByLevel(level: Int) = 3 + (level - 1)
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
            // FakeSessionHelper's amount is level-dependent: targetAmount 1 at level 2 is 1 + (2 - 1).
            assertEquals(2, game.targetsFlow.value.size)
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
    fun `fireButtonClicked derives the next operation digit from the current level`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(10))
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

            // Level up to 2 first: getNextSignAndDigit hardcoding level 1 is invisible at level 1.
            game.targetDidBreakout(targetId)
            testScheduler.runCurrent()
            assertEquals(2, game.fieldFlow.value.level)

            game.fireButtonClicked()
            testScheduler.runCurrent()

            // Random(10)'s third draw (after recreateField's two) is pinned here, not derived from
            // the field under test: reading nextOperationSign back out of the result being asserted
            // let a hardcoded sign in getNextSignAndDigit survive undetected. Seeded specifically to
            // draw SUBTRACTION so a hardcoded DIVISION is also caught - the real-helper determinism
            // test below happens to draw DIVISION at its own call site, so between the two, a
            // hardcoded sign of either value fails at least one test.
            assertEquals(OperationSign.SUBTRACTION, game.fieldFlow.value.nextOperationSign)
            assertEquals(4, game.fieldFlow.value.nextOperationDigit)
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
    fun `targetDidBreakout called twice for the same target only decrements life count once`() =
        runTest {
            // Guards against MC-27: a burst of recompositions (or any other repeat signal) firing
            // this for the same fall must not cost more than one life. A second, still-active target
            // keeps the level from advancing and regenerating ids out from under the repeated call.
            val game = Game(FakeSessionHelper(targetAmount = 2), backgroundScope, Random(7))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id

            game.targetDidBreakout(targetId)
            testScheduler.runCurrent()
            game.targetDidBreakout(targetId)
            testScheduler.runCurrent()

            assertEquals(2, game.fieldFlow.value.lifeCount)
            assertFalse(game.fieldFlow.value.isClosed)
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

            // Pins what Random(42) actually draws, so a bare .random() (agreeing 1 time in 4) or a
            // hardcoded DIVISION (agreeing always) both fail this instead of passing by luck.
            assertEquals(OperationSign.SUBTRACTION, first.fieldFlow.value.currentOperationSign)
            assertEquals(3, first.fieldFlow.value.currentOperationDigit)
            assertEquals(OperationSign.SUBTRACTION, first.fieldFlow.value.nextOperationSign)
            assertEquals(3, first.fieldFlow.value.nextOperationDigit)
        }

    @Test
    fun `seeded Game reproduces the same session with the real session helper`() =
        runTest {
            // The fake above proves Game's own Random is seeded; this proves the session as a
            // whole is, by routing the same seed through the production SessionHelperImpl - the
            // six call sites this task fixes - instead of a fake that never drew from it.
            suspend fun runSession(): Pair<Field, List<Target>> {
                val seed = 99L
                val game = Game(SessionHelperImpl(random = Random(seed)), backgroundScope, Random(seed))
                game.start()
                game.createField(1)
                game.createTargets()
                testScheduler.runCurrent()

                val firstTargetId =
                    game.targetsFlow.value
                        .first()
                        .id
                game.targetRevealed(firstTargetId)
                testScheduler.runCurrent()

                game.fireButtonClicked()
                testScheduler.runCurrent()

                game.targetsFlow.value
                    .map { it.id }
                    .forEach { game.targetDidBreakout(it) }
                testScheduler.runCurrent()

                return game.fieldFlow.value to game.targetsFlow.value
            }

            val (firstField, firstTargets) = runSession()
            val (secondField, secondTargets) = runSession()

            assertEquals(firstField, secondField)
            assertEquals(firstTargets, secondTargets)

            // Comparing the two runs to each other cannot fail on a range narrow enough that an
            // unseeded draw coincides by chance (getSubtractionDigitByLevel's 1..3 at level 1 agrees
            // roughly one run in four). Pinning exact seed-99 values is what actually catches that.
            assertEquals(
                Field(
                    id = 1,
                    level = 2,
                    score = 0,
                    lifeCount = -5,
                    bonusMultiplier = 0,
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 2,
                    nextOperationSign = OperationSign.DIVISION,
                    nextOperationDigit = 5,
                    gameColumnWidthPx = 0,
                    gameColumnHeightPx = 0,
                    isClosed = true,
                ),
                firstField,
            )
            assertEquals(
                listOf(
                    Target(1, 1, 0, 13, 0, 0, 28861),
                    Target(2, 1, 1, 25, 0, 0, 27622),
                    Target(3, 1, 2, 12, 0, 0, 39078),
                    Target(4, 1, 3, 24, 0, 0, 37001),
                    Target(5, 1, 1, 20, 0, 17208, 35309),
                    Target(6, 1, 2, 23, 0, 16318, 19998),
                ),
                firstTargets,
            )
        }

    @Test
    fun `start called twice does not leak a collector that outlives stop`() =
        runTest {
            // If start() doesn't cancel the earlier job, stop() only cancels the second one, and the
            // first keeps collecting: the level-up below would still fire after stop().
            val game = Game(FakeSessionHelper(targetAmount = 1), backgroundScope, Random(5))
            game.start()
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

    @Test
    fun `no visible targets with unequal delays shortens only the seeded subset`() =
        runTest {
            // Unequal delays make the two branches of shortenAppearanceDelay diverge, so this is only
            // pinnable once the injected Random reaches it - the workaround above sidesteps that.
            val delays = listOf(100, 200, 300, 400)
            val sessionHelper = FakeSessionHelper(targetAmount = 4, appearanceDelayMsById = { delays[it] })
            val game = Game(sessionHelper, backgroundScope, Random(6))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()

            assertEquals(
                listOf(0, 0, 0, 100),
                game.targetsFlow.value
                    .sortedBy { it.id }
                    .map { it.appearanceDelayMs },
            )
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

    @Test
    fun `targetClicked reduces the target's value by exactly one and scores a profitable hit`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 10), backgroundScope, Random(16))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            assertEquals(
                9,
                game.targetsFlow.value
                    .first { it.id == targetId }
                    .value,
            )
            assertEquals(1, game.fieldFlow.value.score)
        }

    @Test
    fun `targetClicked does not score a target that fireButtonClicked marked unprofitable`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(42))
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

            // Random(42) draws SUBTRACTION/3 here (pinned by the seeded-reproducibility test above);
            // against value 1 that takes the losing branch: value grows to 4, isProfitable flips false.
            game.fireButtonClicked()
            testScheduler.runCurrent()
            val afterFire = game.targetsFlow.value.first { it.id == targetId }
            assertFalse(afterFire.isProfitable)
            assertEquals(4, afterFire.value)
            assertEquals(0, game.fieldFlow.value.score)

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            assertEquals(
                3,
                game.targetsFlow.value
                    .first { it.id == targetId }
                    .value,
            )
            assertEquals(0, game.fieldFlow.value.score)
        }

    @Test
    fun `targetClicked retires a target it clears to zero`() =
        runTest {
            // A second, never-revealed target keeps the board non-empty so clearing the first one
            // does not level up and regenerate the id being asserted on.
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 1), backgroundScope, Random(21))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .sortedBy { it.id }
                    .first()
                    .id
            game.targetRevealed(targetId)
            testScheduler.runCurrent()

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            val cleared = game.targetsFlow.value.first { it.id == targetId }
            assertEquals(0, cleared.value)
            assertFalse(cleared.isActive)
            assertFalse(cleared.isVisible)
            assertEquals(1, game.fieldFlow.value.score)
        }

    @Test
    fun `clicking the last target to zero clears the board and advances the level`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 1), backgroundScope, Random(22))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id

            game.targetClicked(targetId)
            testScheduler.runCurrent()

            // Without ensureAlive(id) the cleared target stays active, the board never empties and
            // the level never advances - the soft-lock the engine audit named for this function.
            assertEquals(2, game.fieldFlow.value.level)
            assertEquals(2, game.targetsFlow.value.size)
            assertTrue(game.targetsFlow.value.all { it.isActive })
        }

    @Test
    fun `fireButtonClicked scores a successful hit and retires a target cleared to zero`() =
        runTest {
            // A second, never-revealed target stays active throughout so clearing the first one
            // doesn't leave the board fully inactive - that would level up and regenerate the whole
            // target set out from under the id being asserted on below.
            val game = Game(FakeSessionHelper(targetAmount = 2, targetValue = 3), backgroundScope, Random(42))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targetId =
                game.targetsFlow.value
                    .sortedBy { it.id }
                    .first()
                    .id
            game.targetRevealed(targetId)
            testScheduler.runCurrent()

            // Random(42) draws SUBTRACTION/3 here; against value 3 that is the winning branch that
            // clears the target to exactly zero and must retire it, not just leave it at 0 and alive.
            game.fireButtonClicked()
            testScheduler.runCurrent()

            val cleared = game.targetsFlow.value.first { it.id == targetId }
            assertEquals(0, cleared.value)
            assertFalse(cleared.isActive)
            assertFalse(cleared.isVisible)
            assertEquals(3, game.fieldFlow.value.score)
        }

    @Test
    fun `level-up regenerates targets whose value and lifetime scale with the new level`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, targetValue = 10), backgroundScope, Random(17))
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

            // FakeSessionHelper: value = targetValue + (level - 1), lifetimeMs = 1000 + (level - 1).
            // A hardcoded 1 or an off-by-one level + 1 both land on a different number than this.
            val regenerated = game.targetsFlow.value.first()
            assertEquals(11, regenerated.value)
            assertEquals(1001, regenerated.lifetimeMs)
        }

    @Test
    fun `targetRevealed makes only the targeted target visible`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 2), backgroundScope, Random(14))
            game.start()
            game.createField(1)
            game.createTargets()
            testScheduler.runCurrent()
            val targets = game.targetsFlow.value.sortedBy { it.id }
            val revealedId = targets[0].id
            val untouchedId = targets[1].id

            game.targetRevealed(revealedId)
            testScheduler.runCurrent()

            assertTrue(
                game.targetsFlow.value
                    .first { it.id == revealedId }
                    .isVisible,
            )
            assertFalse(
                game.targetsFlow.value
                    .first { it.id == untouchedId }
                    .isVisible,
            )
        }

    @Test
    fun `targetShouldBeSaved clears the appearance delay of a target already falling`() =
        runTest {
            // No start() here: the collector's visibleTargetsAbsent() branch zeroes appearanceDelayMs
            // on every non-visible target, so with it running the delay assertion below passes even
            // against a mapper that never writes the field at all.
            val game = Game(FakeSessionHelper(targetAmount = 1, appearanceDelayMs = 500), backgroundScope, Random(15))
            game.createField(1)
            game.createTargets()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id

            game.targetShouldBeSaved(targetId, position = 50, gameColumnHeightPx = 100)

            val saved = game.targetsFlow.value.first { it.id == targetId }
            assertEquals(50, saved.position)
            assertEquals(0, saved.appearanceDelayMs)
        }

    @Test
    fun `targetShouldBeSaved keeps the delay of a target paused before it appears`() =
        runTest {
            val game = Game(FakeSessionHelper(targetAmount = 1, appearanceDelayMs = 8000), backgroundScope, Random(15))
            game.createField(1)
            game.createTargets()
            val targetId =
                game.targetsFlow.value
                    .first()
                    .id

            game.targetShouldBeSaved(targetId, position = 0, gameColumnHeightPx = 100)

            val saved = game.targetsFlow.value.first { it.id == targetId }
            assertEquals(0, saved.position)
            assertEquals(8000, saved.appearanceDelayMs)
        }

    @Test
    fun `gameColumnSizeMeasured stores width and height without swapping them`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(13))

            game.gameColumnSizeMeasured(width = 300, height = 700)
            testScheduler.runCurrent()

            assertEquals(300, game.fieldFlow.value.gameColumnWidthPx)
            assertEquals(700, game.fieldFlow.value.gameColumnHeightPx)
        }

    @Test
    fun `fieldRestored replaces the field with exactly the restored value`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(18))
            val restoredField =
                Field(
                    id = 7,
                    level = 5,
                    score = 42,
                    lifeCount = 1,
                    isClosed = false,
                )

            game.fieldRestored(restoredField)
            testScheduler.runCurrent()

            assertEquals(restoredField, game.fieldFlow.value)
        }

    @Test
    fun `targetsRestored replaces the target list with exactly the restored targets`() =
        runTest {
            val game = Game(FakeSessionHelper(), backgroundScope, Random(19))
            val restoredTargets =
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 7,
                        columnId = 0,
                        value = 9,
                        position = 120,
                        appearanceDelayMs = 0,
                        lifetimeMs = 30000,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 7,
                        columnId = 1,
                        value = 4,
                        position = 0,
                        appearanceDelayMs = 5000,
                        lifetimeMs = 25000,
                    ),
                )

            game.targetsRestored(restoredTargets)
            testScheduler.runCurrent()

            assertEquals(restoredTargets, game.targetsFlow.value)
        }
}
