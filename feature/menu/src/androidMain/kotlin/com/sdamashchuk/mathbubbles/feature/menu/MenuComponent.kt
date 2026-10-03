package com.sdamashchuk.mathbubbles.feature.menu

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume
import com.sdamashchuk.mathbubbles.core.component.viewModel
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Owns navigation for the menu screen: forwards MenuViewModel's "play clicked" event to
 * [onPlayClicked] instead of leaving the composable to collect it off a NavController.
 */
class MenuComponent(
    componentContext: ComponentContext,
    gameRepository: GameRepository,
    logger: Logger,
    private val onPlayClicked: () -> Unit,
    private val onSettingsClicked: () -> Unit,
) : ComponentContext by componentContext {
    private val viewModel: MenuViewModel = viewModel { MenuViewModel(gameRepository, logger) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val state: StateFlow<MenuViewModel.State> = viewModel.state

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        // The menu instance is retained under Game on the back stack, so returning to it has to
        // re-query rather than trust whatever unfinishedField it saw on its own first creation.
        lifecycle.doOnResume { viewModel.sendAction(MenuViewModel.Action.ScreenResumed) }
        scope.launch {
            viewModel.uiEvents.receiveAsFlow().collect { event ->
                when (event) {
                    MenuViewModel.UiEvent.NavigateToGameScreen -> onPlayClicked()
                    MenuViewModel.UiEvent.NavigateToSettingsScreen -> onSettingsClicked()
                }
            }
        }
    }

    fun sendAction(action: MenuViewModel.Action) = viewModel.sendAction(action)
}
