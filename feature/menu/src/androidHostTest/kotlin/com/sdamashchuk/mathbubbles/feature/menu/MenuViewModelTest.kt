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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hasUnfinishedField is false when no session is open`() =
        runTest {
            val viewModel = MenuViewModel(FakeGameRepository(unfinishedField = null))

            assertFalse(viewModel.state.value.hasUnfinishedField)
        }

    @Test
    fun `hasUnfinishedField is true when a session is still open`() =
        runTest {
            val viewModel = MenuViewModel(FakeGameRepository(unfinishedField = Field(id = 3, score = 40)))

            assertTrue(viewModel.state.value.hasUnfinishedField)
        }

    @Test
    fun `Play closes the unfinished field before starting a new game`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertEquals(1, repository.updateFieldCalls.size)
            assertTrue(repository.updateFieldCalls.single().isClosed)
            assertNull(viewModel.state.value.unfinishedField)
        }

    @Test
    fun `Play with no unfinished field closes nothing`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = null)
            val viewModel = MenuViewModel(repository)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertTrue(repository.updateFieldCalls.isEmpty())
        }

    @Test
    fun `Continue leaves the unfinished field untouched`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository)

            viewModel.sendAction(MenuViewModel.Action.ButtonContinueClicked)

            assertTrue(repository.updateFieldCalls.isEmpty())
            assertTrue(viewModel.state.value.hasUnfinishedField)
        }
}
