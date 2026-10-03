package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.logging.NoOpLogger
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
            val viewModel = MenuViewModel(FakeGameRepository(unfinishedField = null), NoOpLogger)

            assertFalse(viewModel.state.value.hasUnfinishedField)
        }

    @Test
    fun `hasUnfinishedField is true when a session is still open`() =
        runTest {
            val viewModel = MenuViewModel(FakeGameRepository(unfinishedField = Field(id = 3, score = 40)), NoOpLogger)

            assertTrue(viewModel.state.value.hasUnfinishedField)
        }

    @Test
    fun `Play closes the unfinished field before starting a new game`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository, NoOpLogger)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertEquals(1, repository.updateFieldCalls.size)
            assertTrue(repository.updateFieldCalls.single().isClosed)
            assertNull(viewModel.state.value.unfinishedField)
        }

    @Test
    fun `Play closes an unfinished field the cached menu state never saw`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = null)
            val viewModel = MenuViewModel(repository, NoOpLogger)
            repository.unfinishedField = Field(id = 7, score = 12)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertEquals(listOf(7), repository.updateFieldCalls.map { it.id })
            assertTrue(repository.updateFieldCalls.single().isClosed)
        }

    @Test
    fun `Play closes the repository's current unfinished field rather than the cached one`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository, NoOpLogger)
            repository.unfinishedField = Field(id = 4, score = 90)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertEquals(listOf(4), repository.updateFieldCalls.map { it.id })
            assertEquals(90, repository.updateFieldCalls.single().score)
        }

    @Test
    fun `Play with no unfinished field closes nothing`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = null)
            val viewModel = MenuViewModel(repository, NoOpLogger)

            viewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked)

            assertTrue(repository.updateFieldCalls.isEmpty())
        }

    @Test
    fun `Continue leaves the unfinished field untouched`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = Field(id = 3, score = 40))
            val viewModel = MenuViewModel(repository, NoOpLogger)

            viewModel.sendAction(MenuViewModel.Action.ButtonContinueClicked)

            assertTrue(repository.updateFieldCalls.isEmpty())
            assertTrue(viewModel.state.value.hasUnfinishedField)
        }
}
