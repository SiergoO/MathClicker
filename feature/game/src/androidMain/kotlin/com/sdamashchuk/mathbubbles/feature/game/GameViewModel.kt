package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.component.ComponentViewModel
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.database.repository.PersistenceException
import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.model.ActiveEffects
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import com.sdamashchuk.mathbubbles.feature.game.model.FeedbackEffect
import com.sdamashchuk.mathbubbles.feature.game.model.ResultsSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

// Never a stored id, and not 0, which the state collector treats as "no session yet".
private const val UNSAVED_FIELD_ID = -1

class GameViewModel(
    private val game: Game,
    private val gameRepository: GameRepository,
    private val logger: Logger,
) : ComponentViewModel() {
    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.UNLIMITED)
    val uiEvents: ReceiveChannel<UiEvent> = _uiEvents

    // Not routed through _uiEvents: feedback has to reach the composable that owns LocalHapticFeedback,
    // while GameComponent consumes uiEvents itself.
    private val _feedback = Channel<FeedbackEffect>(capacity = Channel.UNLIMITED)
    val feedback: ReceiveChannel<FeedbackEffect> = _feedback

    // False until updateSession() finishes establishing a session. Game is a Koin single, so a
    // collector pass that lands before that point is reading a prior session's field, not this one.
    private var sessionEstablished = false

    // Serialises the collector's writes with the abandon write, so a slow open-field write cannot land
    // after the closing one.
    private val persistenceLock = Mutex()

    init {
        handleAction()
        viewModelScope.launch {
            updateSession()
        }
        viewModelScope.launch {
            // game.events replays nothing, so a second subscriber would never see anything - this is the only one.
            game.events.collect { event ->
                _feedback.trySend(effectFor(event))
            }
        }
        viewModelScope.launch {
            // One collector, not one per half: two read-modify-write copy() calls on _state could clobber each other.
            game.stateFlow.collect { gameState ->
                val (field, targets, effects) = gameState
                if (field.id == 0) return@collect
                val previousField = state.value.field
                val previousTargets = state.value.targetList
                val fieldChanged = field != previousField
                val targetsChanged = targets.isNotEmpty() && targets != previousTargets
                val justClosed = field.isClosed && !previousField.isClosed
                _state.value =
                    state.value.copy(
                        field = field,
                        effects = effects,
                        targetList = if (targets.isNotEmpty()) targets.toImmutableList() else previousTargets,
                        // Neither branch is a player action, so both bypass nextPhase. isClosed stays first: GameOver must
                        // always win over LevelIntro.
                        phase =
                            if (field.isClosed) {
                                GamePhase.GameOver
                            } else if (shouldShowLevelIntro(previousField, field)) {
                                GamePhase.LevelIntro
                            } else {
                                state.value.phase
                            },
                    )
                val persisted =
                    persistenceLock.withLock {
                        if (!sessionEstablished) return@withLock false
                        val targetsReallyChanged = targetsChanged && shouldPersistTargets(previousTargets, targets)
                        guarded("mirror the game state") {
                            if (fieldChanged && targetsReallyChanged) {
                                val previousIds = previousTargets.map { it.id }.toSet()
                                val refresh = shouldRefreshTargets(previousIds, targets.map { it.id }.toSet())
                                gameRepository.saveFieldAndTargets(field, targets, refresh)
                            } else if (fieldChanged) {
                                gameRepository.updateField(field)
                            } else if (targetsReallyChanged) {
                                val previousIds = previousTargets.map { it.id }.toSet()
                                if (shouldRefreshTargets(previousIds, targets.map { it.id }.toSet())) {
                                    gameRepository.refreshTargets(targets)
                                } else {
                                    gameRepository.updateTargets(targets)
                                }
                            }
                        }
                        true
                    }
                if (persisted && justClosed) {
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

                    Action.StashBooster -> {
                        game.stashBooster()
                    }

                    is Action.ApplyBoosterFromStash -> {
                        game.applyBoosterFromStash(action.slotIndex)
                    }

                    Action.DisarmIcePick -> {
                        game.disarmIcePick()
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

    // Writes unconditionally - the throttle in the collector exists to skip this call, not to skip inside
    // it - so a kill right after an untracked fall is not lost.
    private suspend fun persistTargetsNow() {
        val targets = state.value.targetList
        if (targets.isEmpty() || !sessionEstablished) return
        guarded("persist the targets") { gameRepository.updateTargets(targets) }
    }

    // All history, not the recent window: a best outside the last seven must still be found.
    private suspend fun loadResults() {
        guarded("load the results") {
            _state.value =
                state.value.copy(
                    recentResults = gameRepository.getRecentClosedFields().toImmutableList(),
                    bestResult = gameRepository.getBestClosedField(),
                )
        }
    }

    // Closes the open field first, or updateSession would restore the very session Restart was asked to
    // discard. Left closed rather than deleted, so it still surfaces in results history.
    private suspend fun abandonUnfinishedField() {
        persistenceLock.withLock {
            val field = state.value.field
            if (field.id == 0 || field.isClosed || !sessionEstablished) return
            sessionEstablished = false
            closeField(field)
        }
    }

    private suspend fun closeField(field: Field) {
        guarded("close field ${field.id}") {
            gameRepository.updateField(
                field.copy(isClosed = true, finishedAt = Clock.System.now().toEpochMilliseconds()),
            )
        }
    }

    private suspend fun guarded(
        what: String,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (failure: PersistenceException) {
            logger.error("Could not $what", failure)
        }
    }

    private suspend fun updateSession() {
        sessionEstablished = false
        game.resetForNewSession()
        val unfinishedField =
            try {
                gameRepository.getUnfinishedField()
            } catch (failure: PersistenceException) {
                logger.error("Could not read the unfinished session, starting a new one", failure)
                null
            }
        if (unfinishedField != null && restoreSession(unfinishedField)) {
            sessionEstablished = true
        } else {
            startNewSession()
        }
    }

    private suspend fun restoreSession(unfinishedField: Field): Boolean =
        try {
            val unfinishedTargets = gameRepository.getTargets()
            game.fieldRestored(unfinishedField)
            if (unfinishedTargets.isEmpty()) {
                // An old install or a corrupt row can hand back zero targets for an open field. targetsRestored with
                // an empty list would be a no-op - the flow already starts empty - so recreate the level directly.
                game.createTargets()
                val regenerated = game.stateFlow.value
                gameRepository.saveFieldAndTargets(regenerated.field, regenerated.targets, replaceTargets = true)
            } else {
                game.targetsRestored(unfinishedTargets)
            }
            _state.value = state.value.copy(phase = GamePhase.Paused)
            true
        } catch (failure: PersistenceException) {
            logger.error("Could not restore session ${unfinishedField.id}, abandoning it", failure)
            game.resetForNewSession()
            closeField(unfinishedField)
            false
        }

    private suspend fun startNewSession() {
        val id =
            try {
                gameRepository.getNextFieldId()
            } catch (failure: PersistenceException) {
                logger.error("Could not read the next field id, playing without saving", failure)
                UNSAVED_FIELD_ID
            }
        game.createField(id)
        game.createTargets()
        try {
            gameRepository.insertField(game.stateFlow.value.field)
            gameRepository.refreshTargets(game.stateFlow.value.targets)
            sessionEstablished = true
        } catch (failure: PersistenceException) {
            logger.error("Could not save the new session, playing without saving", failure)
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

        object StashBooster : Action()

        data class ApplyBoosterFromStash(
            val slotIndex: Int,
        ) : Action()

        object DisarmIcePick : Action()

        data class Tick(
            val elapsedMs: Int,
        ) : Action()

        object PersistTargetsNow : Action()
    }

    data class State(
        val targetList: ImmutableList<Target> = persistentListOf(),
        val field: Field = Field(),
        val effects: ActiveEffects = ActiveEffects(),
        val phase: GamePhase = GamePhase.ReadyToPlay,
        val recentResults: ImmutableList<Field> = persistentListOf(),
        val bestResult: Field? = null,
    )

    sealed class UiEvent {
        object NavigateToMainMenuScreen : UiEvent()
    }
}

// Always against best, never against recentResults' first entry: the most recent run can be worse
// than a best sitting further back.
internal fun resultsSummaryOf(
    current: Field,
    best: Field?,
): ResultsSummary =
    when {
        best == null -> ResultsSummary.NoHistory(current.score)
        current.score >= best.score -> ResultsSummary.NewRecord(current.score)
        else -> ResultsSummary.ShortOfBest(current.score, best.score - current.score)
    }

// A level-up hands the engine a new id set, so an UPDATE alone would drop the grown rows or leave the
// shrunk ones as ghosts. Only safe when the ids are exactly what was last persisted.
internal fun shouldRefreshTargets(
    previousIds: Set<Int>,
    nextIds: Set<Int>,
) = previousIds != nextIds

// Nothing on Target moves per tick - only Field.gameTimeMs does - so plain equality is the right check.
internal fun shouldPersistTargets(
    previousTargets: List<Target>,
    nextTargets: List<Target>,
): Boolean = previousTargets != nextTargets

// previousField.id != 0 excludes the first emission after a restore, where previousField is still the
// default Field(level = 1): without it, restoring a level-7 session would fire the intro on launch.
internal fun shouldShowLevelIntro(
    previousField: Field,
    nextField: Field,
): Boolean = previousField.id != 0 && nextField.level > previousField.level

// Extracted because each guard counted against nextPhase's own cyclomatic complexity.
private fun GamePhase.transitionTo(
    vararg from: GamePhase,
    to: GamePhase,
): GamePhase = if (this in from) to else this

// The single source of truth for what an action does to the phase. isClosed and shouldShowLevelIntro
// are not here: neither is a player action, and both are applied where the field is collected.
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

        // Only Playing can be paused: the other phases are already showing their own not-playing screen.
        GameViewModel.Action.PauseGame -> {
            current.transitionTo(GamePhase.Playing, to = GamePhase.Paused)
        }

        // ReadyToPlay, not Paused: landing on Paused left the pause menu up over a session that really had
        // restarted underneath, so Restart read as a dead button until the player pressed Resume.
        GameViewModel.Action.RestartGame -> {
            GamePhase.ReadyToPlay
        }

        GameViewModel.Action.BackToMainMenuClicked -> {
            GamePhase.Paused
        }

        is GameViewModel.Action.TargetClicked,
        GameViewModel.Action.FireButtonClicked,
        GameViewModel.Action.StashBooster,
        is GameViewModel.Action.ApplyBoosterFromStash,
        GameViewModel.Action.DisarmIcePick,
        is GameViewModel.Action.Tick,
        GameViewModel.Action.PersistTargetsNow,
        -> {
            current
        }
    }
