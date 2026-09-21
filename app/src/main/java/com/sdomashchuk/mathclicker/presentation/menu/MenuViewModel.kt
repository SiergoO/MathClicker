package com.sdomashchuk.mathclicker.presentation.menu

import com.sdomashchuk.mathclicker.core.component.ComponentViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class MenuViewModel : ComponentViewModel() {
    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.UNLIMITED)
    val uiEvents: ReceiveChannel<UiEvent> = _uiEvents

    init {
        handleAction()
    }

    fun sendAction(actionToSend: Action) {
        action.trySend(actionToSend)
    }

    private fun handleAction() {
        viewModelScope.launch {
            action.consumeAsFlow().collect { action ->
                when (action) {
                    Action.ButtonPlayClicked -> {
                        _uiEvents.trySend(UiEvent.NavigateToGameScreen)
                    }

                    Action.OpenDialog -> {
                        _state.value =
                            state.value.copy(
                                isOpenDialog = true,
                            )
                    }

                    Action.CloseDialog -> {
                        _state.value =
                            state.value.copy(
                                isOpenDialog = false,
                            )
                    }
                }
            }
        }
    }

    sealed class UiEvent {
        object NavigateToGameScreen : UiEvent()
    }

    sealed class Action {
        object ButtonPlayClicked : Action()

        object OpenDialog : Action()

        object CloseDialog : Action()
    }

    data class State(
        val isOpenDialog: Boolean = false,
    )
}
