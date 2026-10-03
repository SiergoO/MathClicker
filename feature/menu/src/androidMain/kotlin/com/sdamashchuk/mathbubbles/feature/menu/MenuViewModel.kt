package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.component.ComponentViewModel
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.database.repository.PersistenceException
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock

class MenuViewModel(
    private val gameRepository: GameRepository,
    private val logger: Logger,
) : ComponentViewModel() {
    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.UNLIMITED)
    val uiEvents: ReceiveChannel<UiEvent> = _uiEvents

    init {
        handleAction()
        refreshUnfinishedField()
    }

    fun sendAction(actionToSend: Action) {
        action.trySend(actionToSend)
    }

    private fun handleAction() {
        viewModelScope.launch {
            action.consumeAsFlow().collect { action ->
                when (action) {
                    Action.ScreenResumed -> {
                        refreshUnfinishedField()
                    }

                    Action.ButtonPlayClicked -> {
                        abandonUnfinishedField()
                        _uiEvents.trySend(UiEvent.NavigateToGameScreen)
                    }

                    Action.ButtonContinueClicked -> {
                        _uiEvents.trySend(UiEvent.NavigateToGameScreen)
                    }

                    Action.ButtonSettingsClicked -> {
                        _uiEvents.trySend(UiEvent.NavigateToSettingsScreen)
                    }

                    Action.OpenDialog -> {
                        _state.value = state.value.copy(isOpenDialog = true)
                    }

                    Action.CloseDialog -> {
                        _state.value = state.value.copy(isOpenDialog = false)
                    }
                }
            }
        }
    }

    private fun refreshUnfinishedField() {
        viewModelScope.launch {
            val unfinishedField =
                try {
                    gameRepository.getUnfinishedField()
                } catch (failure: PersistenceException) {
                    logger.error("Could not read the unfinished field", failure)
                    null
                }
            _state.value = state.value.copy(unfinishedField = unfinishedField)
        }
    }

    // Closed, not deleted, so it still surfaces in results history the same way RestartGame's
    // abandon path does.
    private suspend fun abandonUnfinishedField() {
        try {
            val field = gameRepository.getUnfinishedField()
            if (field != null) {
                gameRepository.updateField(
                    field.copy(isClosed = true, finishedAt = Clock.System.now().toEpochMilliseconds()),
                )
            }
        } catch (failure: PersistenceException) {
            logger.error("Could not abandon the unfinished field", failure)
        }
        _state.value = state.value.copy(unfinishedField = null)
    }

    sealed class UiEvent {
        object NavigateToGameScreen : UiEvent()

        object NavigateToSettingsScreen : UiEvent()
    }

    sealed class Action {
        object ScreenResumed : Action()

        object ButtonPlayClicked : Action()

        object ButtonContinueClicked : Action()

        object ButtonSettingsClicked : Action()

        object OpenDialog : Action()

        object CloseDialog : Action()
    }

    data class State(
        val isOpenDialog: Boolean = false,
        val unfinishedField: Field? = null,
    ) {
        val hasUnfinishedField: Boolean get() = unfinishedField != null
    }
}
