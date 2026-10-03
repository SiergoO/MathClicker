package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.model.logging.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

private class PersistenceFakeSessionHelper(
    private val targetAmount: Int = 1,
    // Total opening offset for id 0, mirroring FakeSessionHelper's own shape: null auto-floors to
    // the flight time (lifetimeMs below), so id 0 appears immediately unless overridden.
    private val openingOffsetMs: Int? = null,
    // Comfortably above a hundred 1ms ticks, so the clock-only tests never cross into breakout.
    // Tests that need an actual breakout override this to something a handful of ticks can clear.
    private val lifetimeMs: Int = 100_000,
    private val finishSpacingMs: Int = 200_000,
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

    override fun getTargetSpeedByLevel(level: Int) = 1f / lifetimeMs

    override fun getTargetFlightTimeMs(level: Int) = lifetimeMs

    override fun getFinishSpacingMsByLevel(level: Int) = finishSpacingMs

    override fun getOpeningOffsetMsByLevel(level: Int) = (openingOffsetMs ?: lifetimeMs).coerceAtLeast(lifetimeMs)

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
    val updateFieldCalls = mutableListOf<Field>()
    var storedField: Field? = null

    fun persistenceCallCount() = updateTargetsCalls.size + refreshTargetsCalls.size

    fun clearCalls() {
        updateTargetsCalls.clear()
        refreshTargetsCalls.clear()
        updateFieldCalls.clear()
    }

    override suspend fun insertField(field: Field) {
        storedField = field
    }

    override suspend fun updateField(field: Field) {
        updateFieldCalls += field
        storedField = field
    }

    override suspend fun getUnfinishedField(): Field? = null

    override suspend fun getNextFieldId(): Int = 1

    override suspend fun updateTargets(targets: List<Target>) {
        updateTargetsCalls += targets
    }

    override suspend fun getTargets(): List<Target> = emptyList()

    override suspend fun refreshTargets(targets: List<Target>) {
        refreshTargetsCalls += targets
    }

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

    // A target's schedule (appearsAtMs/finishesAtMs) is fixed once created, so a tick that
    // moves nothing but the clock produces no target-list change at all - not the "at most one
    // throttled write" the pre-MC-72 fallenMs/appearanceDelayMs counters needed, but exactly zero.
    @Test
    fun `a hundred ticks against an already-visible target produce zero persistence calls`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(1))
            game.start()
            GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            // One tick outside the measured window reveals the target (appearanceDelayMs 0) - not
            // that revealing it writes anything either (see the isVisible test below), but this
            // keeps the measured window itself free of the one-time creation write.
            game.tick(1)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(100) {
                game.tick(1)
                testScheduler.runCurrent()
            }

            assertEquals(0, repository.persistenceCallCount())
        }

    @Test
    fun `a hundred ticks against a still-waiting target produce zero persistence calls`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game =
                Game(
                    // openingOffsetMs is the total, not an extra past the default (which
                    // auto-floors to the flight time, lifetimeMs's own default 100_000) - 105_000
                    // leaves exactly a 5000ms gap past that floor.
                    PersistenceFakeSessionHelper(targetAmount = 1, openingOffsetMs = 105_000),
                    backgroundScope,
                    Random(1),
                )
            game.start()
            GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(100) {
                game.tick(1)
                testScheduler.runCurrent()
            }

            assertEquals(0, repository.persistenceCallCount())
        }

    @Test
    fun `a value change persists despite the clock exclusion`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(2))
            game.start()
            GameViewModel(game, repository, NoOpLogger)
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
                    // A huge finish spacing (rather than a per-id delay, no longer
                    // expressible) pushes the second target's own appearsAtMs well past this
                    // test's window.
                    PersistenceFakeSessionHelper(
                        targetAmount = 2,
                        lifetimeMs = 1000,
                        finishSpacingMs = 999_999,
                    ),
                    backgroundScope,
                    Random(3),
                )
            game.start()
            GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            // One tick outside the measured window reveals the first target (appearanceDelayMs 0) -
            // a no-op on the persisted target list under MC-72 (see the isVisible test below), so
            // the reveal itself is not what the measured ticks below persist. The breakout they do
            // cause flips isActive, the one field a breakout actually writes now.
            game.tick(1)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(4) { game.tick(250) } // 1001ms: breaks the first target out
            testScheduler.runCurrent()

            assertEquals(1, repository.persistenceCallCount())
        }

    // IsVisible is derived from the clock, not stored on Target, so crossing appearsAtMs
    // changes nothing about the persisted target list - the opposite of the pre-MC-72 claim this
    // test's name used to make, and the direct kill for a mutant that reintroduced a stored flag.
    @Test
    fun `crossing a target's appearsAtMs persists nothing`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game =
                Game(
                    // openingOffsetMs is the total, not an extra past the default (which
                    // auto-floors to the flight time, lifetimeMs's own default 100_000) - 105_000
                    // leaves exactly a 5000ms gap past that floor.
                    PersistenceFakeSessionHelper(targetAmount = 1, openingOffsetMs = 105_000),
                    backgroundScope,
                    Random(4),
                )
            game.start()
            GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(20) { game.tick(250) } // 5000ms: exactly clears the delay, nothing more
            testScheduler.runCurrent()

            assertEquals(0, repository.persistenceCallCount())
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
            GameViewModel(game, repository, NoOpLogger)
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
    fun `PersistNow writes even when the throttle key is unchanged`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(6))
            game.start()
            val viewModel = GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repository.clearCalls()

            viewModel.sendAction(GameViewModel.Action.PersistNow)
            testScheduler.runCurrent()

            assertEquals(1, repository.updateTargetsCalls.size)
        }

    @Test
    fun `PauseGame persists the current targets immediately`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(7))
            game.start()
            val viewModel = GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repository.clearCalls()

            viewModel.sendAction(GameViewModel.Action.PauseGame)
            testScheduler.runCurrent()

            assertEquals(1, repository.updateTargetsCalls.size)
        }

    @Test
    fun `six hundred ticks write the field a small bounded number of times`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(8))
            game.start()
            GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repository.clearCalls()

            repeat(600) {
                game.tick(16)
                testScheduler.runCurrent()
            }

            assertTrue("${repository.updateFieldCalls.size} field writes", repository.updateFieldCalls.size in 1..4)
        }

    @Test
    fun `PauseGame flushes the field clock that the timer had not yet written`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(9))
            game.start()
            val viewModel = GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repeat(50) { game.tick(16) }
            testScheduler.runCurrent()
            val liveClock = game.stateFlow.value.field.gameTimeMs
            assertTrue(repository.storedField?.gameTimeMs != liveClock)

            viewModel.sendAction(GameViewModel.Action.PauseGame)
            testScheduler.runCurrent()

            assertEquals(liveClock, repository.storedField?.gameTimeMs)
        }

    @Test
    fun `leaving to the menu flushes the field clock that the timer had not yet written`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(10))
            game.start()
            val viewModel = GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repeat(50) { game.tick(16) }
            testScheduler.runCurrent()
            val liveClock = game.stateFlow.value.field.gameTimeMs

            viewModel.sendAction(GameViewModel.Action.BackToMainMenuClicked)
            testScheduler.runCurrent()

            assertEquals(liveClock, repository.storedField?.gameTimeMs)
        }

    @Test
    fun `PersistNow flushes the field clock that the timer had not yet written`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 1), backgroundScope, Random(11))
            game.start()
            val viewModel = GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            repeat(50) { game.tick(16) }
            testScheduler.runCurrent()
            val liveClock = game.stateFlow.value.field.gameTimeMs

            viewModel.sendAction(GameViewModel.Action.PersistNow)
            testScheduler.runCurrent()

            assertEquals(liveClock, repository.storedField?.gameTimeMs)
        }

    @Test
    fun `ticks that leave the targets unchanged keep the very same target list in the state`() =
        runTest {
            val repository = PersistenceFakeGameRepository()
            val game = Game(PersistenceFakeSessionHelper(targetAmount = 3), backgroundScope, Random(12))
            game.start()
            val viewModel = GameViewModel(game, repository, NoOpLogger)
            testScheduler.runCurrent()
            game.tick(16)
            testScheduler.runCurrent()
            val before = viewModel.state.value.targetList

            repeat(20) { game.tick(16) }
            testScheduler.runCurrent()

            assertSame(before, viewModel.state.value.targetList)
        }
}
