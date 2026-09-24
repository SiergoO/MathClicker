package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.component.ComponentViewModel
import com.sdamashchuk.matharcade.core.database.repository.GameRepository
import com.sdamashchuk.matharcade.core.game.Game
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.Target
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class GameViewModel(
    private val game: Game,
    private val gameRepository: GameRepository,
) : ComponentViewModel() {
    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.UNLIMITED)
    val uiEvents: ReceiveChannel<UiEvent> = _uiEvents

    init {
        handleAction()
        viewModelScope.launch {
            updateSession()
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
                // Every one of these frames needs the UI to see it, so this assignment stays
                // unconditional; only the persistence below is throttled.
                _state.value =
                    state.value.copy(
                        field = field,
                        targetList = if (targets.isNotEmpty()) targets.toImmutableList() else previousTargets,
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
                    Action.ReadyToPlayButtonClicked -> {
                        _state.value =
                            state.value.copy(
                                isGamePaused = false,
                            )
                    }

                    Action.ShowCountDown -> {
                        _state.value =
                            state.value.copy(
                                isGameStarted = false,
                            )
                    }

                    Action.StartGame -> {
                        _state.value =
                            state.value.copy(
                                isGameStarted = true,
                            )
                    }

                    Action.PauseGame -> {
                        persistTargetsNow()
                        _state.value =
                            state.value.copy(
                                isGamePaused = true,
                                isGameStarted = false,
                            )
                    }

                    Action.RestartGame -> {
                        _state.value =
                            state.value.copy(
                                isGamePaused = true,
                                isGameStarted = false,
                            )
                        persistTargetsNow()
                        updateSession()
                    }

                    Action.BackToMainMenuClicked -> {
                        persistTargetsNow()
                        _uiEvents.trySend(UiEvent.NavigateToMainMenuScreen)
                        _state.value =
                            state.value.copy(
                                isGamePaused = true,
                                isGameStarted = false,
                            )
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
        val isGamePaused: Boolean = true,
        val isGameStarted: Boolean = false,
    )

    sealed class UiEvent {
        object NavigateToMainMenuScreen : UiEvent()
    }
}

// A level-up hands the engine an entirely new id set, so an UPDATE alone would silently drop the
// grown rows or leave the shrunk ones behind as ghosts (MC-34). Only the cheaper UPDATE is safe
// when the ids are exactly what was last persisted.
internal fun shouldRefreshTargets(
    previousIds: Set<Int>,
    nextIds: Set<Int>,
) = previousIds != nextIds

// fallenMs and appearanceDelayMs both tick down on every tick(), so a target mid-fall or mid-delay
// changes on every frame regardless of anything the player did; excluding only one of the two still
// persists once per frame for as long as the other is moving.
internal fun shouldPersistTargets(
    previousTargets: List<Target>,
    nextTargets: List<Target>,
): Boolean {
    fun List<Target>.withoutClock() = map { it.copy(fallenMs = 0, appearanceDelayMs = 0) }
    return previousTargets.withoutClock() != nextTargets.withoutClock()
}
