package com.sdomashchuk.mathclicker.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdomashchuk.mathclicker.domain.model.game.OperationSign
import com.sdomashchuk.mathclicker.domain.model.game.session.GameField
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import com.sdomashchuk.mathclicker.domain.usecase.UseCase
import com.sdomashchuk.mathclicker.domain.usecase.game.session.CreateSessionUseCaseParam
import com.sdomashchuk.mathclicker.domain.usecase.game.session.CreateSessionUseCaseResult
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateLevelUseCaseParam
import com.sdomashchuk.mathclicker.domain.usecase.game.session.UpdateLevelUseCaseResult
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
    private val gameRepository: GameRepository,
    private val updateLevelUseCase: UseCase<UpdateLevelUseCaseParam, UpdateLevelUseCaseResult>,
    private val createSessionUseCase: UseCase<CreateSessionUseCaseParam, CreateSessionUseCaseResult>
) : ViewModel() {

    private val action = Channel<Action>(Channel.UNLIMITED)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private val _uiEvents = Channel<UiEvent>(capacity = Channel.UNLIMITED)
    val uiEvents: ReceiveChannel<UiEvent> = _uiEvents

    init {
        handleAction()
        createSession()
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
                            isGameStarted = false,
                            isGameFinished = false
                        )
                        createSession()
                    }
                    Action.BackToMainMenuClicked -> {
                        _uiEvents.trySend(UiEvent.NavigateToMainMenuScreen)
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false,
                            isGameFinished = false
                        )
                    }
                    is Action.GameColumnSizeMeasured -> {
                        _state.value = state.value.copy(
                            gameColumnWidth = action.width,
                            gameColumnHeight = action.height
                        )
                    }
                    is Action.TargetRevealed -> {
                        val updatedTargetParams = updateTargetVisibilityState(action)
                        _state.value = state.value.copy(
                            targetParamsList = updatedTargetParams.toImmutableList()
                        )
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                    }
                    is Action.TargetClicked -> {
                        val updatedTargetParams = updateTargetValueState(action)
                        _state.value = state.value.copy(
                            targetParamsList = updatedTargetParams.toImmutableList()
                        )
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                        updateLevelIfNecessary()
                    }
                    is Action.TargetBreakout -> {
                        val updatedTargetParams = updateTargetAliveState(action)
                        val updatedGameField = updateLifeCountState()
                        _state.value = state.value.copy(
                            targetParamsList = updatedTargetParams.toImmutableList(),
                            gameField = updatedGameField,
                            isGameFinished = updatedGameField.isClosed
                        )
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                        gameRepository.updateGameField(updatedGameField)
                        updateLevelIfNecessary()
                    }
                    is Action.SaveTargetPosition -> {
                        val updatedTargetParams = updateTargetPositionState(action)
                        _state.value = state.value.copy(
                            isGamePaused = true,
                            isGameStarted = false
                        )
                        gameRepository.updateTargetParams(updatedTargetParams[action.id - 1])
                    }
                    is Action.FireButtonClicked -> {
                        val (nextOperationSign, nextOperationDigit) = gameRepository.getNextSignAndDigit(state.value.gameField.level)
                        val currentOperationSign = state.value.gameField.nextOperationSign
                        val currentOperationDigit = state.value.gameField.nextOperationDigit
                        _state.value = state.value.copy(
                            targetParamsList = state.value.targetParamsList
                                .performOperation()
                                .toImmutableList(),
                            gameField = state.value.gameField.copy(
                                currentOperationSign = currentOperationSign,
                                currentOperationDigit = currentOperationDigit,
                                nextOperationSign = nextOperationSign,
                                nextOperationDigit = nextOperationDigit
                            )
                        )
                        gameRepository.updateTargetParamsList(state.value.targetParamsList)
                        gameRepository.updateSignAndDigit(nextOperationSign, nextOperationDigit)
                        updateLevelIfNecessary()
                    }
                }
            }
        }
    }

    private suspend fun updateLevelIfNecessary() {
        if (state.value.targetParamsList.map { it.isAlive }.none { it }) {
            updateLevelUseCase.execute(UpdateLevelUseCaseParam(state.value.gameField.level + 1)).fold(
                onSuccess = {
                    val targetParamsList = gameRepository.getTargetParams(state.value.gameField.id)
                    _state.value = state.value.copy(
                        targetParamsList = targetParamsList.toImmutableList(),
                        gameField = state.value.gameField.let {
                            it.copy(level = it.level + 1)
                        }
                    )
                },
                onFailure = {}
            )
        }
    }

    private fun List<TargetParams>.performOperation(): List<TargetParams> {
        var multiplier = 0
        var totalScore = 0
        val resultList = this.map { tp ->
            if (tp.isAlive && tp.isVisible) {
                var isProfitable = tp.isProfitable
                state.value.gameField.let { field ->
                    val nextValue = run {
                        if (field.currentOperationSign == OperationSign.DIVISION) {
                            val remainder = tp.value % field.currentOperationDigit
                            if (remainder == 0) {
                                val result = tp.value / field.currentOperationDigit
                                totalScore += if (tp.isProfitable) tp.value - result else 0
                                multiplier++
                                result
                            } else {
                                isProfitable = false
                                multiplier--
                                tp.value * field.currentOperationDigit
                            }
                        } else {
                            val result = tp.value - field.currentOperationDigit
                            return@run when {
                                result > 0 -> {
                                    totalScore += if (tp.isProfitable) field.currentOperationDigit else 0
                                    multiplier = if (multiplier == 0) 1 else multiplier
                                    result
                                }
                                result == 0 -> {
                                    totalScore += if (tp.isProfitable) field.currentOperationDigit else 0
                                    multiplier++
                                    0
                                }
                                else -> {
                                    isProfitable = false
                                    multiplier--
                                    tp.value + field.currentOperationDigit
                                }
                            }
                        }
                    }
                    tp.copy(
                        value = nextValue,
                        isProfitable = isProfitable,
                        isAlive = nextValue > 0
                    )
                }
            } else tp
        }
        updateScoreState(totalScore * multiplier)
        return resultList
    }

    private fun createSession() {
        viewModelScope.launch {
            createSessionUseCase.execute(CreateSessionUseCaseParam).fold(
                onSuccess = { gameSession ->
                    _state.value = state.value.copy(
                        gameField = gameSession.gameField,
                        targetParamsList = gameSession.targetParamsList.toImmutableList()
                    )
                },
                onFailure = {}
            )
        }
    }

    private fun updateScoreState(scoreToAdd: Int) {
        val finalScore = (state.value.gameField.score + scoreToAdd).let { if (it < 0) 0 else it }
        _state.value = state.value.copy(
            gameField = state.value.gameField.copy(score = finalScore)
        )
    }

    private fun updateLifeCountState(isSubtract: Boolean = true): GameField {
        val lifeCount = if (isSubtract) state.value.gameField.lifeCount - 1 else state.value.gameField.lifeCount + 1
        return state.value.gameField.copy(
            lifeCount = lifeCount,
            isClosed = lifeCount <= 0
        )
    }

    private fun updateTargetVisibilityState(action: Action.TargetRevealed) =
        state.value.targetParamsList.map {
            if (action.id == it.id) {
                it.copy(
                    isVisible = true
                )
            } else it
        }

    private fun updateTargetValueState(action: Action.TargetClicked): List<TargetParams> {
        return _state.value.targetParamsList.map {
            if (action.id == it.id) {
                if (it.isProfitable) updateScoreState(1)
                it.copy(
                    value = it.value - 1,
                    isAlive = it.value - 1 > 0
                )
            } else it
        }
    }

    private fun updateTargetAliveState(action: Action.TargetBreakout) =
        state.value.targetParamsList.map {
            if (action.id == it.id) {
                it.copy(
                    isAlive = false
                )
            } else it
        }

    private fun updateTargetPositionState(action: Action.SaveTargetPosition) =
        state.value.targetParamsList.map {
            if (action.id == it.id) {
                it.copy(
                    position = action.position,
                    animationDurationMs = it.animationDurationMs * action.position / state.value.gameColumnHeight,
                    animationDelayMs = if (action.position > 0) 0 else it.animationDelayMs
                )
            } else it
        }

    sealed class Action {
        object ReadyToPlayButtonClicked : Action()
        object ShowCountDown : Action()
        object StartGame : Action()
        object PauseGame : Action()
        object RestartGame : Action()
        object BackToMainMenuClicked : Action()
        data class GameColumnSizeMeasured(val width: Int, val height: Int) : Action()
        data class TargetRevealed(val id: Int) : Action()
        data class TargetClicked(val id: Int) : Action()
        data class TargetBreakout(val id: Int) : Action()
        data class SaveTargetPosition(val id: Int, val position: Int) : Action()
        object FireButtonClicked : Action()
    }

    data class State(
        val targetParamsList: ImmutableList<TargetParams> = persistentListOf(),
        val gameField: GameField = GameField(),
        val gameColumnWidth: Int = 0,
        val gameColumnHeight: Int = 0,
        val isGamePaused: Boolean = true,
        val isGameStarted: Boolean = false,
        val isGameFinished: Boolean = false
    )

    sealed class UiEvent {
        object NavigateToMainMenuScreen : UiEvent()
    }
}