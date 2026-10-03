package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.model.logging.NoOpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeSessionHelper(
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

    // 0 auto-floors to the flight time (1000), so id 0 always appears immediately - the same
    // default every FakeSessionHelper in this codebase uses.
    override fun getOpeningOffsetMsByLevel(level: Int) = 1000

    // Level-dependent so a level-up actually changes the id set recreateTargets hands out -
    // a constant amount would make every level produce the same 1..amount ids.
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

private class FakeGameRepository(
    private val unfinishedField: Field?,
    private val unfinishedTargets: List<Target>,
    // Deliberately distinct from each other in every test that sets them, so a wiring mistake that
    // reads one where the other belongs (M5) fails instead of passing on coincidentally equal data.
    private val recentClosedFields: List<Field> = emptyList(),
    private val bestClosedField: Field? = null,
) : GameRepository {
    val refreshTargetsCalls = mutableListOf<List<Target>>()
    val updateTargetsCalls = mutableListOf<List<Target>>()

    override suspend fun insertField(field: Field) = Unit

    override suspend fun updateField(field: Field) = Unit

    override suspend fun getUnfinishedField(): Field? = unfinishedField

    override suspend fun getNextFieldId(): Int = 1

    override suspend fun updateTargets(targets: List<Target>) {
        updateTargetsCalls += targets
    }

    override suspend fun getTargets(): List<Target> = unfinishedTargets

    override suspend fun refreshTargets(targets: List<Target>) {
        refreshTargetsCalls += targets
    }

    override suspend fun getRecentClosedFields(): List<Field> = recentClosedFields

    override suspend fun getBestClosedField(): Field? = bestClosedField

    override suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    ) {
        updateField(field)
        if (replaceTargets) refreshTargets(targets) else updateTargets(targets)
    }
}

// Tracks the latest write, unlike FakeGameRepository above which always hands back its
// constructor snapshot - what a "kill the VM, start a fresh one against the same storage" test
// needs in place of a real database.
private class StatefulFakeGameRepository(
    field: Field,
    targets: List<Target>,
) : GameRepository {
    private var storedField = field
    private var storedTargets = targets

    override suspend fun insertField(field: Field) {
        storedField = field
    }

    override suspend fun updateField(field: Field) {
        storedField = field
    }

    override suspend fun getUnfinishedField(): Field? = storedField

    override suspend fun getNextFieldId(): Int = 2

    override suspend fun updateTargets(targets: List<Target>) {
        storedTargets = targets
    }

    override suspend fun getTargets(): List<Target> = storedTargets

    override suspend fun refreshTargets(targets: List<Target>) {
        storedTargets = targets
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

// getUnfinishedField's delay gives a collector racing ahead of updateSession a window to run
// first, the same window a real DB round-trip leaves open.
private class RacyFakeGameRepository(
    initialField: Field,
) : GameRepository {
    private val fields = mutableMapOf(initialField.id to initialField)
    private var nextId = initialField.id + 1
    private var targets: List<Target> = emptyList()

    fun fieldById(id: Int): Field? = fields[id]

    override suspend fun insertField(field: Field) {
        val id = if (field.id == 0) nextId++ else field.id
        fields[id] = field.copy(id = id)
    }

    override suspend fun updateField(field: Field) {
        fields[field.id] = field
    }

    override suspend fun getUnfinishedField(): Field? {
        delay(1)
        return fields.values.filter { !it.isClosed }.maxByOrNull { it.id }
    }

    override suspend fun getNextFieldId(): Int = (fields.keys.maxOrNull() ?: 0) + 1

    override suspend fun updateTargets(targets: List<Target>) {
        this.targets = targets
    }

    override suspend fun getTargets(): List<Target> = targets

    override suspend fun refreshTargets(targets: List<Target>) {
        this.targets = targets
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

            val viewModel = GameViewModel(game, repository, NoOpLogger)
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
                        appearsAtMs = -10,
                        finishesAtMs = 990,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 5,
                        columnId = 1,
                        value = 43,
                        appearsAtMs = -20,
                        finishesAtMs = 980,
                    ),
                )
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 2, isClosed = false),
                    unfinishedTargets = existingTargets,
                )
            val game = Game(FakeSessionHelper(targetAmount = 4), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository, NoOpLogger)

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
                        appearsAtMs = 0,
                        finishesAtMs = 1000,
                    ),
                    Target(
                        id = 2,
                        relatedFieldId = 5,
                        columnId = 1,
                        value = 5,
                        appearsAtMs = 0,
                        finishesAtMs = 1000,
                    ),
                )
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 1, isClosed = false),
                    unfinishedTargets = existingTargets,
                )
            val game = Game(FakeSessionHelper(targetAmount = 2), CoroutineScope(Dispatchers.Unconfined))
            game.start()
            GameViewModel(game, repository, NoOpLogger)
            repository.refreshTargetsCalls.clear()
            repository.updateTargetsCalls.clear()

            // Same id set: only the value/isActive flags change.
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

    // field.isClosed is the engine's own signal, not a player action, so it is not part of
    // nextPhase - this pins where it actually gets applied instead: the stateFlow collector.
    @Test
    fun `the field closing forces GameOver regardless of the previous phase`() =
        runTest {
            val existingTargets =
                (1..3).map { id ->
                    Target(
                        id = id,
                        relatedFieldId = 5,
                        columnId = id - 1,
                        value = 10,
                        appearsAtMs = 0,
                        finishesAtMs = 1000,
                    )
                }
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 1, isClosed = false),
                    unfinishedTargets = existingTargets,
                )
            val game = Game(FakeSessionHelper(targetAmount = 3), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository, NoOpLogger)

            // All three share the same 1000ms lifetime and zero appearance delay, so four 250ms
            // ticks break them out together, taking lifeCount from 3 (Field's default) to 0 in one step.
            repeat(4) { game.tick(250) }

            assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
        }

    // MC-57 M4 originally staged a level-up and a GameOver landing in the same locked tick (tick()
    // resolves breakouts, including the field closing, before it checks for an empty board) to prove
    // GameOver wins the collector's `when`. MC-59 closed that combination off at the engine level - a
    // field that closes on the same step never levels up any more (see Game.tick's isClosed guard) -
    // so the two can no longer actually land together. The phase assertion stands on its own as a
    // regression pin for the isClosed-first ordering; the level assertion now pins the opposite of
    // what it used to: MC-59's fix, not the defect it existed to encode.
    @Test
    fun `GameOver wins over LevelIntro when the last life and the last target break out together`() =
        runTest {
            val soleTarget =
                Target(
                    id = 1,
                    relatedFieldId = 5,
                    columnId = 0,
                    value = 10,
                    appearsAtMs = 0,
                    finishesAtMs = 1000,
                )
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 1, lifeCount = 1, isClosed = false),
                    unfinishedTargets = listOf(soleTarget),
                )
            val game = Game(FakeSessionHelper(targetAmount = 1), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository, NoOpLogger)

            // The sole target breaking out both zeroes the last life (GameOver) and empties the
            // board, in the same locked tick.
            repeat(4) { game.tick(250) }

            assertEquals(1, game.stateFlow.value.field.level)
            assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
        }

    // M5: the delta computed against the most recent run rather than the best. bestResult and
    // recentResults are wired from two separate repository calls (getBestClosedField,
    // getRecentClosedFields); this pins that bestResult is never derived from recentResults'
    // own first entry, which here is a worse score than the true best.
    @Test
    fun `the field closing loads bestResult from getBestClosedField, not from the recent window's own first entry`() =
        runTest {
            val existingTargets =
                (1..3).map { id ->
                    Target(
                        id = id,
                        relatedFieldId = 5,
                        columnId = id - 1,
                        value = 10,
                        appearsAtMs = 0,
                        finishesAtMs = 1000,
                    )
                }
            val trueBest = Field(id = 1, score = 999, level = 9, isClosed = true)
            val mostRecent = Field(id = 12, score = 40, level = 2, isClosed = true)
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 1, isClosed = false),
                    unfinishedTargets = existingTargets,
                    recentClosedFields = listOf(mostRecent, trueBest),
                    bestClosedField = trueBest,
                )
            val game = Game(FakeSessionHelper(targetAmount = 3), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository, NoOpLogger)
            repeat(4) { game.tick(250) }

            assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
            assertEquals(trueBest, viewModel.state.value.bestResult)
            assertEquals(listOf(mostRecent, trueBest), viewModel.state.value.recentResults)
        }

    // A restored session never drops the player straight into falling bubbles - Continue always
    // lands on the pause dialog over the frozen, restored field.
    @Test
    fun `restoring an unfinished field lands in Paused, never ReadyToPlay`() =
        runTest {
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 3, isClosed = false),
                    unfinishedTargets =
                        listOf(
                            Target(
                                id = 1,
                                relatedFieldId = 5,
                                columnId = 0,
                                value = 4,
                                appearsAtMs = 0,
                                finishesAtMs = 1000,
                            ),
                        ),
                )
            val game = Game(FakeSessionHelper(targetAmount = 1), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository, NoOpLogger)

            assertEquals(GamePhase.Paused, viewModel.state.value.phase)
        }

    // Counterpart: a brand new session (no unfinished field) still opens on ReadyToPlay, exactly as
    // Play expects.
    @Test
    fun `starting a new session (no unfinished field) still lands in ReadyToPlay`() =
        runTest {
            val repository = FakeGameRepository(unfinishedField = null, unfinishedTargets = emptyList())
            val game = Game(FakeSessionHelper(targetAmount = 1), CoroutineScope(Dispatchers.Unconfined))

            val viewModel = GameViewModel(game, repository, NoOpLogger)

            assertEquals(GamePhase.ReadyToPlay, viewModel.state.value.phase)
        }

    // Game is a Koin single: Menu abandoning field 5 and navigating to a fresh GameViewModel for
    // Play must not let that new VM's own collector observe (and re-persist) the stale field 5
    // still sitting in Game's state from the session the player just left.
    @Test
    fun `Play after Back to Menu does not reopen a field the menu already abandoned`() =
        runTest {
            val repository = RacyFakeGameRepository(Field(id = 5, score = 10, isClosed = false))
            val game = Game(FakeSessionHelper(targetAmount = 1), CoroutineScope(Dispatchers.Unconfined))
            game.start()
            game.fieldRestored(Field(id = 5, score = 10, isClosed = false))
            game.targetsRestored(
                listOf(
                    Target(id = 1, relatedFieldId = 5, columnId = 0, value = 5, appearsAtMs = 0, finishesAtMs = 1000),
                ),
            )
            repository.updateField(Field(id = 5, score = 10, isClosed = true))

            GameViewModel(game, repository, NoOpLogger)
            advanceUntilIdle()

            assertTrue(repository.fieldById(5)?.isClosed == true)
            assertFalse(game.stateFlow.value.field.id == 5)
        }

    // A kill right after an action, with no pause in between, must lose nothing - a fresh VM
    // reading the same repository restores identical score, values and stash.
    @Test
    fun `a VM dropped right after an action restores identically in a fresh VM`() =
        runTest {
            val target =
                Target(id = 1, relatedFieldId = 5, columnId = 0, value = 9, appearsAtMs = 0, finishesAtMs = 100_000)
            val seedField = Field(id = 5, level = 1, isClosed = false, currentBooster = Booster.SHIELD)
            val repository = StatefulFakeGameRepository(seedField, listOf(target))
            val firstGame =
                Game(
                    FakeSessionHelper(targetAmount = 1),
                    CoroutineScope(Dispatchers.Unconfined),
                    boostersEnabled = true,
                )
            GameViewModel(firstGame, repository, NoOpLogger)

            firstGame.targetClicked(target.id)
            firstGame.stashBooster()

            val secondGame =
                Game(
                    FakeSessionHelper(targetAmount = 1),
                    CoroutineScope(Dispatchers.Unconfined),
                    boostersEnabled = true,
                )
            val secondViewModel = GameViewModel(secondGame, repository, NoOpLogger)

            assertEquals(
                8,
                secondViewModel.state.value.targetList
                    .single()
                    .value,
            )
            assertEquals(firstGame.stateFlow.value.field.score, secondViewModel.state.value.field.score)
            assertEquals(listOf(Booster.SHIELD), secondViewModel.state.value.field.boosterStash)
        }

    // MC-58 M4: dropping the game.events collector in init leaves this channel forever empty while
    // every other test in this file still passes - this is the one that would have caught it.
    @Test
    fun `a target zeroing reaches the feedback channel as the mapped effect`() =
        runTest {
            val target =
                Target(
                    id = 1,
                    relatedFieldId = 5,
                    columnId = 0,
                    value = 1,
                    appearsAtMs = 0,
                    finishesAtMs = 1000,
                )
            val repository =
                FakeGameRepository(
                    unfinishedField = Field(id = 5, level = 1, isClosed = false),
                    unfinishedTargets = listOf(target),
                )
            val game = Game(FakeSessionHelper(targetAmount = 1), CoroutineScope(Dispatchers.Unconfined))
            val viewModel = GameViewModel(game, repository, NoOpLogger)

            game.targetClicked(target.id)

            val received = viewModel.feedback.tryReceive().getOrNull()
            assertEquals(effectFor(GameEvent.TargetZeroed(id = target.id, awarded = 1)), received)
        }
}
