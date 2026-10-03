package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.model.Field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelResilienceTest {
    private val logger = RecordingLogger()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an unreadable unfinished field leaves the menu without a continue option and logs the error`() =
        runTest {
            val viewModel = MenuViewModel(ThrowingGameRepository(failReads = true), logger)

            assertNull(viewModel.state.value.unfinishedField)
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `Play still navigates to the game when abandoning the old session fails`() =
        runTest {
            val repository = ThrowingGameRepository(failWrites = true, unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository, logger)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertEquals(MenuViewModel.UiEvent.NavigateToGameScreen, viewModel.uiEvents.tryReceive().getOrNull())
            assertEquals(1, logger.errors.size)
        }

    @Test
    fun `the action loop survives a failed abandon and handles the next action`() =
        runTest {
            val repository = ThrowingGameRepository(failWrites = true, unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository, logger)
            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)
            viewModel.uiEvents.tryReceive()

            viewModel.sendAction(MenuViewModel.Action.ButtonSettingsClicked)

            assertEquals(MenuViewModel.UiEvent.NavigateToSettingsScreen, viewModel.uiEvents.tryReceive().getOrNull())
        }
}
