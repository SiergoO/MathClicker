package com.sdamashchuk.matharcade.feature.menu

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.sdamashchuk.matharcade.core.component.viewModel
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
    private val onPlayClicked: () -> Unit,
    private val onSettingsClicked: () -> Unit,
) : ComponentContext by componentContext {
    private val viewModel: MenuViewModel = viewModel { MenuViewModel() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val state: StateFlow<MenuViewModel.State> = viewModel.state

    init {
        lifecycle.doOnDestroy { scope.cancel() }
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
