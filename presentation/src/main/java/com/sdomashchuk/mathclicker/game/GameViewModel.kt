package com.sdomashchuk.mathclicker.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor() : ViewModel() {

    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    init {
        handleAction()
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
                            isReadyToPlay = true
                        )
                    }
                    Action.ShowCountDown -> {
                        _state.value = state.value.copy(
                            isGameActive = false
                        )
                    }
                    Action.StartGame -> {
                        _state.value = state.value.copy(
                            isGameActive = true
                        )
                    }
                }
            }
        }
    }

    sealed class Action {
        object ReadyToPlayButtonClicked : Action()
        object ShowCountDown : Action()
        object StartGame : Action()
        object PauseGame : Action()
    }

    data class State(
        val isReadyToPlay: Boolean = false,
        val isGameActive: Boolean = false
    )
}