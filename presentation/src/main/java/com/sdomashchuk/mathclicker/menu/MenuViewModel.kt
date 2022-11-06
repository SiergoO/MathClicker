package com.sdomashchuk.mathclicker.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(): ViewModel() {

    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _eventsUi = Channel<EventUi>(capacity = Channel.UNLIMITED)
    val eventsUi: ReceiveChannel<EventUi> = _eventsUi

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
                    Action.ButtonPlayClicked -> {
                        _eventsUi.trySend(EventUi.NavigateToGameScreen)
                    }
                    Action.OpenDialog -> {
                        _state.value = state.value.copy(
                            isOpenDialog = true
                        )
                    }
                    Action.CloseDialog -> {
                        _state.value = state.value.copy(
                            isOpenDialog = false
                        )
                    }
                }

            }
        }
    }

    sealed class EventUi {
        object NavigateToGameScreen : EventUi()
    }

    sealed class Action {
        object ButtonPlayClicked : Action()
        object OpenDialog : Action()
        object CloseDialog : Action()
    }

    data class State(
        val isOpenDialog: Boolean = false
    )
}