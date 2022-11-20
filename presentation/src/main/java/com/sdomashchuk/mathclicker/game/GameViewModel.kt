package com.sdomashchuk.mathclicker.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.domain.model.game.session.GameField
import com.sdomashchuk.mathclicker.domain.model.game.session.GameSession
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        handleAction()
        initSession()
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
                    is Action.TargetClicked -> {
                        val updatedTargetParams = updateTargetValueState(action)
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                        _state.value = state.value.copy(
                            buttons = updatedTargetParams.toImmutableList()
                        )
                    }
                    is Action.TargetBreakout -> {
                        val updatedTargetParams = updateTargetAliveState(action)
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                        _state.value = state.value.copy(
                            buttons = updatedTargetParams.toImmutableList()
                        )
                    }
                    is Action.SaveTargetPosition -> {
                        val updatedTargetParams = updateTargetPositionState(action)
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false
                        )
                    }
                    is Action.FireButtonClicked -> {
                        _state.value = state.value.copy(
                            buttons = state.value.buttons.map { tp ->
                                state.value.gameSession.gameField.let { session ->
                                    val nextValue =
                                        if (session.currentOperationSign == OperationSign.DIVISION) {
                                            tp.value / session.currentOperationDigit
                                        } else {
                                            tp.value - session.currentOperationDigit
                                        }
                                    tp.copy(
                                        value = nextValue,
                                        isAlive = nextValue > 0
                                    )
                                }
                            }.toImmutableList()
                        )
                    }
                }
            }
        }
    }

    private fun initSession() {
        viewModelScope.launch {
            val gameSession = gameRepository.getUnfinishedSession().let {
                it ?: with(gameRepository) {
                    val gameField = initGameField()
                    initTargetParams(gameField)
                    getUnfinishedSession()!!
                }
            }
            _state.value = state.value.copy(
                gameSession = gameSession,
                buttons = gameSession.targetParams.toImmutableList()
            )
        }
    }

    private fun updateTargetValueState(action: Action.TargetClicked) =
        state.value.buttons.map {
            if (action.id == it.id) {
                it.copy(
                    value = it.value - 1,
                    isAlive = it.value - 1 > 0
                )
            } else it
        }

    private fun updateTargetAliveState(action: Action.TargetBreakout) =
        state.value.buttons.map {
            if (action.id == it.id) {
                it.copy(
                    isAlive = false
                )
            } else it
        }

    private fun updateTargetPositionState(action: Action.SaveTargetPosition) =
        state.value.buttons.map {
            if (action.id == it.id) {
                it.copy(
                    position = action.position,
                    animationDurationMs = it.animationDurationMs * action.position / 495,
                    animationDelayMs = if (action.position > 0) 0 else it.animationDelayMs
                )
            } else it
        }

    sealed class Action {
        object ReadyToPlayButtonClicked : Action()
        object ShowCountDown : Action()
        object StartGame : Action()
        object PauseGame : Action()
        data class TargetClicked(val id: Int) : Action()
        data class TargetBreakout(val id: Int) : Action()
        data class SaveTargetPosition(val id: Int, val position: Int) : Action()
        object FireButtonClicked : Action()
    }

    data class State(
        val buttons: ImmutableList<TargetParams> = persistentListOf(),
        val gameSession: GameSession = GameSession(GameField(), listOf()),
        val isGamePaused: Boolean = true,
        val isGameStarted: Boolean = false
    )
}