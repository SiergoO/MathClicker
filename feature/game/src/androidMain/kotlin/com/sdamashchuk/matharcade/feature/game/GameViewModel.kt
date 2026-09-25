package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.component.ComponentViewModel
import com.sdamashchuk.matharcade.core.database.repository.GameRepository
import com.sdamashchuk.matharcade.core.game.Game
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.feature.game.model.FeedbackEffect
import com.sdamashchuk.matharcade.feature.game.model.ResultsSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class GameViewModel(
    private val game: Game,
    private val gameRepository: GameRepository,
) : ComponentViewModel() {
    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.UNLIMITED)
    val uiEvents: ReceiveChannel<UiEvent> = _uiEvents

    // A dedicated channel, not routed through _uiEvents: GameComponent forwards uiEvents to
    // navigation and consumes them itself, but feedback needs to reach the composable that owns
    // LocalHapticFeedback, so it is exposed and collected there instead (see GameScreen).
    private val _feedback = Channel<FeedbackEffect>(capacity = Channel.UNLIMITED)
    val feedback: ReceiveChannel<FeedbackEffect> = _feedback

    init {
        handleAction()
        viewModelScope.launch {
            updateSession()
        }
        viewModelScope.launch {
            // One collector for the game session: game.events replays nothing (replay = 0), so a
            // second subscriber here would just never see anything - this is the only one.
            game.events.collect { event ->
                _feedback.trySend(effectFor(event))
            }
        }
        viewModelScope.launch {
            // A single collector on Game's combined state, not one per half: two collectors each
            // doing a read-modify-write copy() on the same _state could interleave and have one
            // clobber the other's field (MC-42). One assignment below publishes both halves at once.
            game.stateFlow.collect { gameState ->
                val (field, targets) = gameState
                if (field.id == 0) return@collect
                val previousField = state.value.field
                val previousTargets = state.value.targetList
                val fieldChanged = field != previousField
                val targetsChanged = targets.isNotEmpty() && targets != previousTargets
                val justClosed = field.isClosed && !previousField.isClosed
                // Every one of these frames needs the UI to see it, so this assignment stays
                // unconditional; only the persistence below is throttled.
                _state.value =
                    state.value.copy(
                        field = field,
                        targetList = if (targets.isNotEmpty()) targets.toImmutableList() else previousTargets,
                        // Neither branch is a player action, so both bypass nextPhase and are applied
                        // directly here. isClosed stays first regardless: GameOver must always win over
                        // LevelIntro. MC-59 means the engine itself no longer levels up a field that
                        // closes on the same step (see Game.tick's and activeTargetsAbsent's isClosed
                        // guards), so the two can no longer actually land together - this ordering is
                        // now a defensive invariant rather than a reachable race.
                        phase =
                            if (field.isClosed) {
                                GamePhase.GameOver
                            } else if (shouldShowLevelIntro(previousField, field)) {
                                GamePhase.LevelIntro
                            } else {
                                state.value.phase
                            },
                    )
                if (fieldChanged) {
                    gameRepository.updateField(field)
                }
                if (targetsChanged && shouldPersistTargets(previousTargets, targets)) {
                    val previousIds = previousTargets.map { it.id }.toSet()
                    if (shouldRefreshTargets(previousIds, targets.map { it.id }.toSet())) {
                        gameRepository.refreshTargets(targets)
                    } else {
                        gameRepository.updateTargets(targets)
                    }
                }
                // After updateField above, not before: the just-finished run has to be on disk
                // before either query below can see it, or it would neither appear in the recent
                // list nor be eligible to be the best.
                if (justClosed) {
                    loadResults()
                }
            }
        }
    }

    fun sendAction(actionToSend: Action) {
        action.trySend(actionToSend)
    }

    private fun handleAction() {
        viewModelScope.launch {
            action.consumeAsFlow().collect { action ->
                when (action) {
                    Action.ReadyToPlayButtonClicked,
                    Action.ShowCountDown,
                    Action.StartGame,
                    Action.LevelIntroFinished,
                    -> {
                        _state.value = state.value.copy(phase = nextPhase(state.value.phase, action))
                    }

                    Action.PauseGame -> {
                        persistTargetsNow()
                        _state.value = state.value.copy(phase = nextPhase(state.value.phase, action))
                    }

                    Action.RestartGame -> {
                        _state.value = state.value.copy(phase = nextPhase(state.value.phase, action))
                        persistTargetsNow()
                        abandonUnfinishedField()
                        updateSession()
                    }

                    Action.BackToMainMenuClicked -> {
                        persistTargetsNow()
                        _uiEvents.trySend(UiEvent.NavigateToMainMenuScreen)
                        _state.value = state.value.copy(phase = nextPhase(state.value.phase, action))
                    }

                    is Action.TargetClicked -> {
                        game.targetClicked(action.id)
                    }

                    is Action.FireButtonClicked -> {
                        game.fireButtonClicked()
                    }

                    is Action.Tick -> {
                        game.tick(action.elapsedMs)
                    }

                    Action.PersistTargetsNow -> {
                        persistTargetsNow()
                    }
                }
            }
        }
    }

    // Positions reach disk exactly on pause and on ON_STOP, not on every frame: writes unconditionally
    // (the throttle in the collector above exists to skip this call, not to skip inside it) so a
    // background/kill right after a fall that never triggered shouldPersistTargets isn't lost.
    private suspend fun persistTargetsNow() {
        val targets = state.value.targetList
        if (targets.isEmpty()) return
        gameRepository.updateTargets(targets)
    }

    // getBestClosedField reads all history, not gameRepository's own recent window - a best
    // outside the last ten must still be found (see GameRepository.getBestClosedField's own
    // contract, and resultsSummaryOf below which is what turns this into the near-miss delta).
    private suspend fun loadResults() {
        _state.value =
            state.value.copy(
                recentResults = gameRepository.getRecentClosedFields().toImmutableList(),
                bestResult = gameRepository.getBestClosedField(),
            )
    }

    // Restart from Paused otherwise hands the open field straight back: getUnfinishedField() in
    // updateSession() below would restore the very session Restart was asked to discard (MC-66).
    // From GameOver the field is already closed by Game itself, so this is a no-op there - the two
    // call sites end up meaning the same thing without a phase check. The abandoned run is left
    // closed rather than deleted, so it can still surface in results history the same way a run
    // that ended in GameOver does - the player still reached this score, they just chose to stop.
    private suspend fun abandonUnfinishedField() {
        val field = state.value.field
        if (field.id == 0 || field.isClosed) return
        gameRepository.updateField(field.copy(isClosed = true, finishedAt = Clock.System.now().toEpochMilliseconds()))
    }

    private suspend fun updateSession() {
        with(gameRepository) {
            val unfinishedField = getUnfinishedField()
            val unfinishedTargets = getTargets()
            if (unfinishedField == null) {
                val sessionCount = getFieldCount()
                game.createField(sessionCount + 1)
                game.createTargets()
                insertField(game.stateFlow.value.field)
                refreshTargets(game.stateFlow.value.targets)
            } else {
                game.fieldRestored(unfinishedField)
                if (unfinishedTargets.isEmpty()) {
                    // refreshTargets is now one transaction, so this shouldn't arise from persistence
                    // going forward — but an old install or a corrupt row can still hand back zero
                    // targets for an open field. targetsRestored(emptyList()) would be a no-op here
                    // (the flow already starts empty, so setting it to an equal value never emits and
                    // Game's own recovery guard never sees it), so recreate the level directly instead
                    // of relying on that.
                    game.createTargets()
                    refreshTargets(game.stateFlow.value.targets)
                } else {
                    game.targetsRestored(unfinishedTargets)
                }
            }
        }
    }

    sealed class Action {
        object ReadyToPlayButtonClicked : Action()

        object ShowCountDown : Action()

        object StartGame : Action()

        object LevelIntroFinished : Action()

        object PauseGame : Action()

        object RestartGame : Action()

        object BackToMainMenuClicked : Action()

        data class TargetClicked(
            val id: Int,
        ) : Action()

        object FireButtonClicked : Action()

        data class Tick(
            val elapsedMs: Int,
        ) : Action()

        object PersistTargetsNow : Action()
    }

    data class State(
        val targetList: ImmutableList<Target> = persistentListOf(),
        val field: Field = Field(),
        val phase: GamePhase = GamePhase.ReadyToPlay,
        val recentResults: ImmutableList<Field> = persistentListOf(),
        val bestResult: Field? = null,
    )

    sealed class UiEvent {
        object NavigateToMainMenuScreen : UiEvent()
    }
}

// The near-miss delta the results screen exists for (see the MC-53 spec): always against best,
// never against recentResults' own first entry, which is only the most recent run and can be a
// worse score than a best sitting further back in history.
internal fun resultsSummaryOf(
    current: Field,
    best: Field?,
): ResultsSummary =
    when {
        best == null -> ResultsSummary.NoHistory(current.score)
        current.score >= best.score -> ResultsSummary.NewRecord(current.score)
        else -> ResultsSummary.ShortOfBest(current.score, best.score - current.score)
    }

// A level-up hands the engine an entirely new id set, so an UPDATE alone would silently drop the
// grown rows or leave the shrunk ones behind as ghosts (MC-34). Only the cheaper UPDATE is safe
// when the ids are exactly what was last persisted.
internal fun shouldRefreshTargets(
    previousIds: Set<Int>,
    nextIds: Set<Int>,
) = previousIds != nextIds

// MC-72: a target's schedule (appearsAtMs/finishesAtMs) is fixed at creation and only ever shifted
// by an event (shorten, level-up) - unlike the old fallenMs/appearanceDelayMs pair, nothing on
// Target itself moves on every tick() any more, only Field.gameTimeMs does. Plain equality is
// therefore already the right check; no clock-only fields are left to exclude.
internal fun shouldPersistTargets(
    previousTargets: List<Target>,
    nextTargets: List<Target>,
): Boolean = previousTargets != nextTargets

// Level-up arrives from two call sites inside Game - tick() and activeTargetsAbsent() - so the
// trigger is this delta on the field the collector already sees, not either call site directly.
// previousField.id != 0 excludes the very first emission after a restore, where previousField is
// still the default Field(level = 1) State() started with: without it, restoring a level-7 session
// would compare 7 > 1 and fire the intro on launch for a level-up that never happened.
internal fun shouldShowLevelIntro(
    previousField: Field,
    nextField: Field,
): Boolean = previousField.id != 0 && nextField.level > previousField.level

// Moves the `if (current == X) to else current` shape used below out of nextPhase's own body:
// each guard counted directly against nextPhase's cyclomatic complexity, and LevelIntroFinished
// was the one branch that tipped it over detekt's threshold.
private fun GamePhase.transitionTo(
    vararg from: GamePhase,
    to: GamePhase,
): GamePhase = if (this in from) to else this

// The single source of truth for what an action does to the screen's phase, replacing the
// isGamePaused/isGameStarted pair whose implicit branch order in GameScreen's old `when` made
// "not started yet" and "paused" the same state (MC-55). field.isClosed -> GamePhase.GameOver and
// shouldShowLevelIntro -> GamePhase.LevelIntro are not here: neither is a player action, and both
// are applied directly where the field is collected.
internal fun nextPhase(
    current: GamePhase,
    action: GameViewModel.Action,
): GamePhase =
    when (action) {
        GameViewModel.Action.ReadyToPlayButtonClicked -> {
            current.transitionTo(GamePhase.ReadyToPlay, GamePhase.Paused, to = GamePhase.CountingDown)
        }

        GameViewModel.Action.ShowCountDown -> {
            current.transitionTo(GamePhase.Playing, to = GamePhase.CountingDown)
        }

        GameViewModel.Action.StartGame -> {
            current.transitionTo(GamePhase.CountingDown, to = GamePhase.Playing)
        }

        GameViewModel.Action.LevelIntroFinished -> {
            current.transitionTo(GamePhase.LevelIntro, to = GamePhase.Playing)
        }

        // Only Playing can be paused: there is nothing running to pause from GameOver, and the
        // other phases are already showing their own "not playing yet" screen.
        GameViewModel.Action.PauseGame -> {
            current.transitionTo(GamePhase.Playing, to = GamePhase.Paused)
        }

        // MC-77: ReadyToPlay, not Paused. Landing on Paused was invisible from Paused itself - the
        // session really did restart underneath (score 1 -> 0 on a device) while the pause menu
        // stayed up, so Restart read as a dead button until the player pressed Resume. The old
        // comment argued ReadyToPlay could never be reached again once a session had started, which
        // was true only while that phase shared the paused overlay's composable; MC-64 split them,
        // and a fresh session showing "touch screen to start" is exactly what this phase is for.
        GameViewModel.Action.RestartGame -> {
            GamePhase.ReadyToPlay
        }

        GameViewModel.Action.BackToMainMenuClicked -> {
            GamePhase.Paused
        }

        is GameViewModel.Action.TargetClicked,
        GameViewModel.Action.FireButtonClicked,
        is GameViewModel.Action.Tick,
        GameViewModel.Action.PersistTargetsNow,
        -> {
            current
        }
    }
