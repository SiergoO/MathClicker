package com.sdomashchuk.mathclicker.presentation.game

import android.util.Log
import android.util.Size
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdomashchuk.mathclicker.domain.model.game.session.Field
import com.sdomashchuk.mathclicker.domain.model.game.session.Target
import com.sdomashchuk.mathclicker.domain.model.game.session.toDomainList
import com.sdomashchuk.mathclicker.domain.model.game.session.toDomainModel
import com.sdomashchuk.mathclicker.domain.model.game.session.toGameList
import com.sdomashchuk.mathclicker.domain.model.game.session.toGameModel
import com.sdomashchuk.mathclicker.domain.model.game.session.toImmutableDomainList
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.game.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val game: Game,
    private val gameRepository: GameRepository,
) : ViewModel() {

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
            game.targetsFlow.collect { targets ->
                if (targets.isNotEmpty()) {
                    val updatedTargets = targets.toDomainList()
                    _state.value = state.value.copy(
                        targetList = updatedTargets.toImmutableList()
                    )
                    gameRepository.updateTargets(updatedTargets)
                }
            }
        }
        viewModelScope.launch {
            game.fieldFlow.collect { field ->
                if (field.id != 0) {
                    val updatedField = field.toDomainModel()
                    _state.value = state.value.copy(
                        field = updatedField
                    )
                    gameRepository.updateField(updatedField)
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
                        _state.value = state.value.copy(
                            isGamePaused = false
                        )
                    }
                    Action.ShowCountDown -> {
                        _state.value = state.value.copy(
                            isGameStarted = false
                        )
                    }
                    Action.StartGame -> {
                        _state.value = state.value.copy(
                            isGameStarted = true
                        )
                    }
                    Action.PauseGame -> {
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false
                        )
                    }
                    Action.RestartGame -> {
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false
                        )
                        updateSession()
                    }
                    Action.BackToMainMenuClicked -> {
                        _uiEvents.trySend(UiEvent.NavigateToMainMenuScreen)
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false
                        )
                    }
                    is Action.GameColumnSizeMeasured -> {
                        game.gameColumnSizeMeasured(action.size.width, action.size.height)
                    }
                    is Action.TargetRevealed -> {
                        game.targetRevealed(action.id)
                    }
                    is Action.TargetClicked -> {
                        game.targetClicked(action.id)
                    }
                    is Action.TargetDidBreakout -> {
                        game.targetDidBreakout(action.id)
                    }
                    is Action.SaveTargetPosition -> {
                        game.targetShouldBeSaved(action.id, action.position, action.gameColumnHeightPx)
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false
                        )
                    }
                    is Action.FireButtonClicked -> {
                        game.fireButtonClicked()
                    }
                }
            }
        }
    }

    private suspend fun updateSession() {
        with(gameRepository) {
            val unfinishedField = getUnfinishedField()
            val unfinishedTargets = getTargets()
            if (unfinishedField == null) {
                val sessionCount = getFieldCount()
                game.createField(sessionCount + 1)
                game.createTargets()
                insertField(game.fieldFlow.value.toDomainModel())
                refreshTargets(game.targetsFlow.value.toDomainList())
            } else {
                game.fieldRestored(unfinishedField.toGameModel())
                game.targetsRestored(unfinishedTargets.toGameList())
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
        data class GameColumnSizeMeasured(val size: Size) : Action()
        data class TargetRevealed(val id: Int) : Action()
        data class TargetClicked(val id: Int) : Action()
        data class TargetDidBreakout(val id: Int) : Action()
        data class SaveTargetPosition(val id: Int, val position: Int, val gameColumnHeightPx: Int) : Action()
        object FireButtonClicked : Action()
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