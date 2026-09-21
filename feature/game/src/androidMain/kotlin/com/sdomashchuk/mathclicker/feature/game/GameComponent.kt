package com.sdomashchuk.mathclicker.feature.game

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.sdomashchuk.mathclicker.core.component.viewModel
import com.sdomashchuk.mathclicker.core.database.repository.GameRepository
import com.sdomashchuk.mathclicker.core.game.Game
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Owns navigation for the game screen: forwards GameViewModel's post-game "back to menu" event to
 * [onBackToMenu] instead of leaving the composable to collect it off a NavController.
 */
class GameComponent(
    componentContext: ComponentContext,
    game: Game,
    gameRepository: GameRepository,
    private val onBackToMenu: () -> Unit,
) : ComponentContext by componentContext {
    private val viewModel: GameViewModel = viewModel { GameViewModel(game, gameRepository) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val state: StateFlow<GameViewModel.State> = viewModel.state

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        scope.launch {
            viewModel.uiEvents.receiveAsFlow().collect { event ->
                when (event) {
                    GameViewModel.UiEvent.NavigateToMainMenuScreen -> onBackToMenu()
                }
            }
        }
    }

    fun sendAction(action: GameViewModel.Action) = viewModel.sendAction(action)
}
