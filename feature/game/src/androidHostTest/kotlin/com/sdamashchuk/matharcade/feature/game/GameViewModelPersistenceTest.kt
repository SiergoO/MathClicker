package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.database.repository.GameRepository
import com.sdamashchuk.matharcade.core.game.Game
import com.sdamashchuk.matharcade.core.game.helper.SessionHelper
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import com.sdamashchuk.matharcade.core.model.Target
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
import kotlin.random.Random

private class PersistenceFakeSessionHelper(
    private val targetAmount: Int = 1,
    private val appearanceDelayMs: Int = 0,
    // Comfortably above a hundred 1ms ticks, so the fallenMs-only test never crosses into breakout.
    // Tests that need an actual breakout override this to something a handful of ticks can clear.
    private val lifetimeMs: Int = 100_000,
    private val appearanceDelayMsById: (Int) -> Int = { appearanceDelayMs },
) : SessionHelper {
    override val levelRange = 1..999
    override val initialTargetValueRange = 1..20
    override val initialTargetLifetimeMsRange = 20000..40000
    override val initialTargetAppearanceDelayMsRange = 10000..20000
    override val initialTargetAmountRange = 6..10
    override val initialDivisionValueRange = 2..5
    override val initialSubtractionValueRange = 1..3

    override fun getTargetValueByLevel(level: Int) = 10

    override fun getTargetLifetimeMsByLevel(level: Int) = lifetimeMs

    override fun getTargetAppearanceDelayMsById(id: Int) = appearanceDelayMsById(id)

    override fun getTargetAmountByLevel(level: Int) = targetAmount + (level - 1)

    override fun getOperationDigitByLevel(
        operationSign: OperationSign,
        level: Int,
    ) = 2

    override fun getDivisionDigitByLevel(level: Int) = 2

    override fun getSubtractionDigitByLevel(level: Int) = 3

    // Mirrors SessionHelperImpl's real shape rather than a round number: a cap below a target's
    // own value makes a failed division *shrink* it, so a stub of 0 would quietly turn every
    // failure in this module's tests into a win.
    override fun failedGrowthCap(level: Int) = 4 * (initialTargetValueRange.last + 3 * level)
}

private class PersistenceFakeGameRepository : GameRepository {
    val updateTargetsCalls = mutableListOf<List<Target>>()
    val refreshTargetsCalls = mutableListOf<List<Target>>()

    fun persistenceCallCount() = updateTargetsCalls.size + refreshTargetsCalls.size

    fun clearCalls() {
        updateTargetsCalls.clear()
        refreshTargetsCalls.clear()
    }

    override suspend fun insertField(field: Field) = Unit

    override suspend fun updateField(field: Field) = Unit

    override suspend fun getUnfinishedField(): Field? = null

    override suspend fun getFieldCount(): Int = 0

    override suspend fun updateTargets(targets: List<Target>) {
        updateTargetsCalls += targets
    }

    override suspend fun getTargets(): List<Target> = emptyList()

    override suspend fun refreshTargets(targets: List<Target>) {
        refreshTargetsCalls += targets
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPersistenceTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a hundred ticks differing only in fallenMs produce at most one persistence call`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
            game.start()
            GameViewModel(game, repository)
            testScheduler.runCurrent()
            // One tick outside the measured window reveals the target (appearanceDelayMs 0), so the
            // hundred ticks that follow move fallenMs alone.
            game.tick(1)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(100) {
                game.tick(1)
                testScheduler.runCurrent()
            }

            assertTrue(repository.persistenceCallCount() <= 1)
        }

    @Test
    fun `a hundred ticks differing only in appearanceDelayMs produce at most one persistence call`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game =
                Game(
                    PersistenceFakeSessionHelper(targetAmount = 1, appearanceDelayMs = 5000),
                    backgroundScope,
                    Random(1),
                )
            game.start()
            GameViewModel(game, repository)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(100) {
                game.tick(1)
                testScheduler.runCurrent()
            }

            assertTrue(repository.persistenceCallCount() <= 1)
        }

    @Test
    fun `a value change persists despite the clock exclusion`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(2))
            game.start()
            GameViewModel(game, repository)
            testScheduler.runCurrent()
            repository.clearCalls()

            game.targetClicked(
                game.stateFlow.value.targets
                    .first()
                    .id,
            )
            testScheduler.runCurrent()

            assertEquals(1, repository.persistenceCallCount())
        }

    @Test
    fun `an isActive change persists`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            // A second target - delayed well past this test's window - keeps the board non-empty, so
            // this is an isActive flip on the same id set rather than the level-up covered separately
            // below.
            val game =
                Game(
                    PersistenceFakeSessionHelper(
                        targetAmount = 2,
                        lifetimeMs = 1000,
                        appearanceDelayMsById = { index -> if (index == 0) 0 else 999_999 },
                    ),
                    backgroundScope,
                    Random(3),
                )
            game.start()
            GameViewModel(game, repository)
            testScheduler.runCurrent()
            // One tick outside the measured window reveals the first target (appearanceDelayMs 0), so
            // the reveal itself is not what the measured ticks below persist. The breakout they do
            // cause flips isActive and isVisible together, in one emission - which is the point:
            // a key change of any size is still one write.
            game.tick(1)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(4) { game.tick(250) } // 1001ms: breaks the first target out
            testScheduler.runCurrent()

            assertEquals(1, repository.persistenceCallCount())
        }

    @Test
    fun `an isVisible change persists`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game =
                Game(
                    PersistenceFakeSessionHelper(targetAmount = 1, appearanceDelayMs = 5000),
                    backgroundScope,
                    Random(4),
                )
            game.start()
            GameViewModel(game, repository)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(20) { game.tick(250) } // 5000ms: exactly clears the delay, nothing more
            testScheduler.runCurrent()

            assertEquals(1, repository.persistenceCallCount())
        }

    @Test
    fun `a level-up's changed id set persists via refreshTargets`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game =
                Game(
                    PersistenceFakeSessionHelper(targetAmount = 1, lifetimeMs = 1000),
                    backgroundScope,
                    Random(5),
                )
            game.start()
            GameViewModel(game, repository)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(4) { game.tick(250) } // 1000ms: reveals, falls, and breaks the sole target out
            testScheduler.runCurrent()

            // tick()'s breakout and the level-up it triggers publish as one combined state now (both
            // decided inside the same locked step), so there is exactly one persisted event here, not
            // the breakout-then-level-up pair the old collector-driven path produced - and it correctly
            // takes the refreshTargets path because the id set changes.
            assertEquals(1, repository.refreshTargetsCalls.size)
        }

    @Test
    fun `PersistTargetsNow writes even when the throttle key is unchanged`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(6))
            game.start()
            val viewModel = GameViewModel(game, repository)
            testScheduler.runCurrent()
            repository.clearCalls()

            viewModel.sendAction(GameViewModel.Action.PersistTargetsNow)
            testScheduler.runCurrent()

            assertEquals(1, repository.updateTargetsCalls.size)
        }

    @Test
    fun `PauseGame persists the current targets immediately`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(7))
            game.start()
            val viewModel = GameViewModel(game, repository)
            testScheduler.runCurrent()
            repository.clearCalls()

            viewModel.sendAction(GameViewModel.Action.PauseGame)
            testScheduler.runCurrent()

            assertEquals(1, repository.updateTargetsCalls.size)
        }
}
