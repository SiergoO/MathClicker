package com.sdomashchuk.mathclicker.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.domain.model.game.session.Session
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParam
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
            action.consumeAsFlow().collect() { action ->
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
                    is Action.TargetButtonClicked -> {
                        _state.value = state.value.copy(
                            buttons = state.value.buttons.map {
                                if (action.id == it.id) {
                                    it.copy(
                                        value = it.value - 1,
                                        isAlive = it.value - 1 > 0
                                    )
                                } else it
                            }.toImmutableList()
                        )
                    }
                    is Action.FireButtonClicked -> {
                        _state.value = state.value.copy(
                            buttons = state.value.buttons.map {
                                val nextValue =
                                    if (state.value.session.currentOperationSign == OperationSign.DIVISION) {
                                        it.value / state.value.session.currentOperationDigit
                                    } else {
                                        it.value - state.value.session.currentOperationDigit
                                    }
                                it.copy(
                                    value = nextValue,
                                    isAlive = nextValue > 0
                                )
                            }.toImmutableList()
                        )
                    }
                }
            }
        }
    }

    private fun initSession() {
        viewModelScope.launch {
            val session = gameRepository.getSession()
            val targetParams = gameRepository.getTargetParams(session.level)
            _state.value = state.value.copy(
                session = session,
                buttons = targetParams.toImmutableList()
            )
        }
    }

    sealed class Action {
        object ReadyToPlayButtonClicked : Action()
        object ShowCountDown : Action()
        object StartGame : Action()
        object PauseGame : Action()
        data class TargetButtonClicked(val id: Int) : Action()
        object FireButtonClicked : Action()
    }

    data class State(
        val buttons: ImmutableList<TargetParam> = persistentListOf(),
        val session: Session = Session(),
        val isGamePaused: Boolean = true,
        val isGameStarted: Boolean = false
    )
}