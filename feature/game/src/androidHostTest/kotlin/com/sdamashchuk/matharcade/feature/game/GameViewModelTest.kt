package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.database.repository.GameRepository
import com.sdamashchuk.matharcade.core.game.Game
import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSessionHelper(
    private val targetAmount: Int = 1,
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(level: Int) = 10

    override fun getTargetLifetimeMsByLevel(level: Int) = 1000

    override fun getTargetAppearanceDelayMsById(id: Int) = 0

    // Level-dependent so a level-up actually changes the id set recreateTargets hands out -
    // a constant amount would make every level produce the same 1..amount ids.
    override fun getTargetAmountByLevel(level: Int) = targetAmount + (level - 1)

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 2

    override fun getDivisionDigitByLevel(level: Int) = 2

    override fun getSubtractionDigitByLevel(level: Int) = 3
}

private class FakeGameRepository(
    private val unfinishedField: Field?,
    private val unfinishedTargets: List<Target>,
) : GameRepository {
    val refreshTargetsCalls = mutableListOf<List<Target>>()
    val updateTargetsCalls = mutableListOf<List<Target>>()

    override suspend fun insertField(field: Field) = Unit

    override suspend fun updateField(field: Field) = Unit

    override suspend fun getUnfinishedField(): Field? = unfinishedField

    override suspend fun getFieldCount(): Int = 0

    override suspend fun updateTargets(targets: List<Target>) {
        updateTargetsCalls += targets
    }

    override suspend fun getTargets(): List<Target> = unfinishedTargets

    override suspend fun refreshTargets(targets: List<Target>) {
        refreshTargetsCalls += targets
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // The scenario MC-34 exists for: an open field restored with zero persisted targets used to
    // hand the (never actually emitting) empty list to Game and soft-lock forever.
    @Test
    fun `restoring an open field with zero targets recreates them instead of soft-locking`() =
        runTest {
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 3, isClosed = false),
                    unfinishedTargets = emptyList(),
                )
            val game = Game(FakeSessionHelper(targetAmount = 4), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository)
            val restoredTargets = viewModel.state.value.targetList

            assertTrue(restoredTargets.isNotEmpty())
            assertTrue(repository.refreshTargetsCalls.isNotEmpty())
            // Recovery re-fills the restored level rather than advancing it.
            assertEquals(3, viewModel.state.value.field.level)
        }

    // The counterpart: a normal restore must keep the exact persisted rows, not regenerate a fresh
    // set from the session helper - proving the empty-list branch above is the exception, not the rule.
    @Test
    fun `restoring an open field with existing targets keeps them exactly`() =
        runTest {
            val existingTargets =
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 5,
                        columnId = 0,
                        value = 42,
                        fallenMs = 10,
                        appearanceDelayMs = 0,
                        lifetimeMs = 1000,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 5,
                        columnId = 1,
                        value = 43,
                        fallenMs = 20,
                        appearanceDelayMs = 0,
                        lifetimeMs = 1000,
                    ),
                )
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 2, isClosed = false),
                    unfinishedTargets = existingTargets,
                )
            val game = Game(FakeSessionHelper(targetAmount = 4), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository)

            assertEquals(existingTargets, viewModel.state.value.targetList)
            assertEquals(existingTargets, game.stateFlow.value.targets)
        }

    // This is the collector's own choice of method (GameViewModel's combined stateFlow collector),
    // not the explicit refreshTargets() call updateSession() makes on restore/new-session: a same-level mutation
    // (targetClicked) must stay on the cheap updateTargets path, and a level-up growing the id set
    // must go through refreshTargets or the grown rows are silently dropped (MC-34's original bug).
    @Test
    fun `a same-level mutation updates while a level-up refreshes`() =
        runTest {
            val existingTargets =
                listOf(
                    Target(
                        id = 1,
                        relatedFieldId = 5,
                        columnId = 0,
                        value = 5,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 1000,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 5,
                        columnId = 1,
                        value = 5,
                        fallenMs = 0,
                        appearanceDelayMs = 0,
                        lifetimeMs = 1000,
                    ),
                )
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 1, isClosed = false),
                    unfinishedTargets = existingTargets,
                )
            val game = Game(FakeSessionHelper(targetAmount = 2), CoroutineScope(Dispatchers.Unconfined))
            game.start()
            GameViewModel(game, repository)
            repository.refreshTargetsCalls.clear()
            repository.updateTargetsCalls.clear()

            // Same id set: only the value/isActive/isVisible flags change.
            game.targetClicked(existingTargets[0].id)
            assertTrue(repository.updateTargetsCalls.isNotEmpty())
            assertTrue(repository.refreshTargetsCalls.isEmpty())

            // Clearing the board levels up: FakeSessionHelper hands level 2 a bigger amount, so the
            // id set actually changes (1,2 -> 1,2,3). Both targets share the same 1000ms lifetime and
            // zero appearance delay, so four 250ms ticks fall and break them out together.
            repeat(4) { game.tick(250) }

            assertTrue(repository.refreshTargetsCalls.isNotEmpty())
            assertEquals(
                setOf(1, 2, 3),
                game.stateFlow.value.targets
                    .map { it.id }
                    .toSet(),
            )
        }
}
