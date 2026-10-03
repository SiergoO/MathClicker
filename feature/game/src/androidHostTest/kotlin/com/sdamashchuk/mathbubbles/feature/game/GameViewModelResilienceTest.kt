package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import com.sdamashchuk.mathbubbles.core.model.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelResilienceTest {
    private val repository = FlakyGameRepository()
    private val logger = RecordingLogger()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.startGame(): Game =
        Game(SessionHelperImpl(Random(1)), backgroundScope, Random(1)).also { it.start() }

    @Test
    fun `an unreadable unfinished field starts a fresh session and logs the error`() =
        runTest {
            repository.failUnfinishedFieldRead = true

            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()

            assertEquals(1, viewModel.state.value.field.id)
            assertEquals(GamePhase.ReadyToPlay, viewModel.state.value.phase)
            assertEquals(1, repository.insertedFields)
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `an unreadable target list starts a fresh session and logs the error`() =
        runTest {
            repository.fields[7] = Field(id = 7, score = 40)
            repository.failTargetsRead = true

            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()

            assertEquals(8, viewModel.state.value.field.id)
            assertEquals(GamePhase.ReadyToPlay, viewModel.state.value.phase)
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `an unreadable next id still opens a playable session and logs the error`() =
        runTest {
            repository.failNextIdRead = true

            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()

            assertEquals(-1, viewModel.state.value.field.id)
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `a failed insert of the new session leaves the game playable and logs the error`() =
        runTest {
            repository.failInsert = true

            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()

            assertEquals(1, viewModel.state.value.field.id)
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `the persistence collector keeps mirroring state after a write throws`() =
        runTest {
            val game = startGame()
            val viewModel = GameViewModel(game, repository, logger)
            testScheduler.runCurrent()
            val updatesBefore = repository.updatedFields

            repository.failWrites = true
            game.tick(1)
            testScheduler.runCurrent()
            repository.failWrites = false
            game.tick(1)
            testScheduler.runCurrent()

            assertEquals(1, logger.errors.size)
            assertEquals(updatesBefore + 1, repository.updatedFields)
            assertEquals(2L, viewModel.state.value.field.gameTimeMs)
        }

    @Test
    fun `pausing still pauses when persisting the targets throws`() =
        runTest {
            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()
            viewModel.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
            viewModel.sendAction(GameViewModel.Action.StartGame)
            repository.failWrites = true

            viewModel.sendAction(GameViewModel.Action.PauseGame)

            assertEquals(GamePhase.Paused, viewModel.state.value.phase)
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `the action loop survives a failed write and handles the next action`() =
        runTest {
            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()
            viewModel.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
            viewModel.sendAction(GameViewModel.Action.StartGame)
            repository.failWrites = true
            viewModel.sendAction(GameViewModel.Action.PauseGame)

            viewModel.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)

            assertEquals(GamePhase.CountingDown, viewModel.state.value.phase)
        }

    @Test
    fun `an unreadable next id never lets a restart overwrite stored history`() =
        runTest {
            val history = Field(id = 1, score = 500, isClosed = true, finishedAt = 9L)
            repository.fields[1] = history
            repository.failNextIdRead = true
            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()
            repository.failNextIdRead = false

            viewModel.sendAction(GameViewModel.Action.RestartGame)
            testScheduler.runCurrent()

            assertEquals(history, repository.fields[1])
        }

    @Test
    fun `a session that fails to restore is closed so it does not resurface after the new one ends`() =
        runTest {
            repository.fields[7] = Field(id = 7, score = 40)
            repository.failTargetsRead = true

            val viewModel = GameViewModel(startGame(), repository, logger)
            testScheduler.runCurrent()

            assertEquals(8, viewModel.state.value.field.id)
            assertEquals(true, repository.fields.getValue(7).isClosed)
            repository.updateField(repository.fields.getValue(8).copy(isClosed = true))
            repository.failTargetsRead = false
            assertEquals(null, repository.getUnfinishedField())
        }
}
