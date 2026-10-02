package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

private class RestartFakeSessionHelper(
    private val targetAmount: Int = 1,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetFlightTimeMsRange = 20000..40000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(
        level: Int,
        operationDigit: Int,
    ) = 10

    override fun getTargetSpeedByLevel(level: Int) = 1f / 1000

    override fun getTargetFlightTimeMs(level: Int) = 1000

    override fun getFinishSpacingMsByLevel(level: Int) = 2000

    override fun getOpeningOffsetMsByLevel(level: Int) = 1000

    override fun getTargetAmountByLevel(level: Int) = targetAmount + (level - 1)

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 2

    override fun getDivisionDigitByLevel(level: Int) = 2

    override fun getSubtractionDigitByLevel(level: Int) = 3

    override fun failedGrowthCap(level: Int) = 4 * (initialTargetValueRange.last + 3 * level)
}

// Models the field table closely enough for this task: insertField/updateField mutate a row set
// keyed by id, and getUnfinishedField/getFieldCount are derived from it exactly the way FieldDao's
// real queries are (isClosed = 0, newest id first; a plain count of every row). The static
// FakeGameRepository in GameViewModelTest can't stand in here - restarting has to observe the
// close this task adds, not a value fixed for the life of the test.
private class RestartFakeGameRepository(
    seedField: Field,
    private val seedTargets: List<Target>,
) : GameRepository {
    private val fields = mutableListOf(seedField)

    override suspend fun insertField(field: Field) {
        fields += field
    }

    override suspend fun updateField(field: Field) {
        val index = fields.indexOfFirst { it.id == field.id }
        if (index >= 0) fields[index] = field
    }

    override suspend fun getUnfinishedField(): Field? = fields.filter { !it.isClosed }.maxByOrNull { it.id }

    override suspend fun getFieldCount(): Int = fields.size

    override suspend fun updateTargets(targets: List<Target>) = Unit

    override suspend fun getTargets(): List<Target> = seedTargets

    override suspend fun refreshTargets(targets: List<Target>) = Unit

    override suspend fun getRecentClosedFields(): List<Field> = emptyList()

    override suspend fun getBestClosedField(): Field? = null

    override suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    ) {
        updateField(field)
        if (replaceTargets) refreshTargets(targets) else updateTargets(targets)
    }

    fun allFields(): List<Field> = fields.toList()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelRestartTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restarting from Paused starts a new session at level 1 with score 0 and full lives`() =
        runTest {
            val abandonedField = Field(id = 5, level = 7, score = 555, lifeCount = 2, isClosed = false)
            val repository =
                RestartFakeGameRepository(
                    seedField = abandonedField,
                    seedTargets = listOf(),
                )
            val game = Game(RestartFakeSessionHelper(targetAmount = 3), CoroutineScope(Dispatchers.Unconfined))
            val viewModel = GameViewModel(game, repository)

            viewModel.sendAction(GameViewModel.Action.PauseGame)
            viewModel.sendAction(GameViewModel.Action.RestartGame)

            val field = viewModel.state.value.field
            assertEquals(1, field.level)
            assertEquals(0, field.score)
            assertEquals(INITIAL_LIFE_COUNT, field.lifeCount)
        }

    @Test
    fun `restarting from Paused closes the abandoned field leaving exactly one open field`() =
        runTest {
            val abandonedField = Field(id = 5, level = 7, score = 555, lifeCount = 2, isClosed = false)
            val repository =
                RestartFakeGameRepository(
                    seedField = abandonedField,
                    seedTargets = listOf(),
                )
            val game = Game(RestartFakeSessionHelper(targetAmount = 3), CoroutineScope(Dispatchers.Unconfined))
            val viewModel = GameViewModel(game, repository)

            viewModel.sendAction(GameViewModel.Action.PauseGame)
            viewModel.sendAction(GameViewModel.Action.RestartGame)

            val closedAbandonedField = repository.allFields().first { it.id == abandonedField.id }
            assertEquals(true, closedAbandonedField.isClosed)
            assertNotNull(closedAbandonedField.finishedAt)
            assertEquals(1, repository.allFields().count { !it.isClosed })
        }

    @Test
    fun `restarting from GameOver still starts a fresh session without touching the already-closed field`() =
        runTest {
            val soleTarget =
                Target(id = 1, relatedFieldId = 5, columnId = 0, value = 10, appearsAtMs = 0, finishesAtMs = 1000)
            val repository =
                RestartFakeGameRepository(
                    seedField = Field(id = 5, level = 1, lifeCount = 1, isClosed = false),
                    seedTargets = listOf(soleTarget),
                )
            val game = Game(RestartFakeSessionHelper(targetAmount = 1), CoroutineScope(Dispatchers.Unconfined))
            val viewModel = GameViewModel(game, repository)

            // The sole target breaking out zeroes the only life, closing the field via Game itself -
            // the same path a real GameOver takes, independent of this task's fix.
            repeat(4) { game.tick(250) }
            assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
            val closedAtGameOver = repository.allFields().first { it.id == 5 }.finishedAt

            viewModel.sendAction(GameViewModel.Action.RestartGame)

            val field = viewModel.state.value.field
            assertEquals(1, field.level)
            assertEquals(0, field.score)
            assertEquals(1, repository.allFields().count { !it.isClosed })
            // The already-closed field's own finishedAt must survive untouched - abandonUnfinishedField
            // is a no-op once isClosed is already true, not a second write with a later timestamp.
            assertEquals(closedAtGameOver, repository.allFields().first { it.id == 5 }.finishedAt)
        }
}
