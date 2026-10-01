package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.component.ComponentViewModel
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val soundSettings: SoundSettings,
) : ComponentViewModel() {
    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State(soundEnabled = soundSettings.isSoundEnabled()))
    val state: StateFlow<State> = _state

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
                    is Action.SoundSwitchToggled -> {
                        soundSettings.setSoundEnabled(action.enabled)
                        _state.value = state.value.copy(soundEnabled = action.enabled)
                    }
                }
            }
        }
    }

    sealed class Action {
        data class SoundSwitchToggled(
            val enabled: Boolean,
        ) : Action()
    }

    // The results-reset control that will eventually live on this screen (see the MC-56 spec) is
    // not built here - there is nothing in this State to leave room for beyond not closing it off.
    data class State(
        val soundEnabled: Boolean = true,
    )
}
