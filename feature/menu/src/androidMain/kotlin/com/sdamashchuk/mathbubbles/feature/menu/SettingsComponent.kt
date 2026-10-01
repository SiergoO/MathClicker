package com.sdamashchuk.mathbubbles.feature.menu

import com.arkivanov.decompose.ComponentContext
import com.sdamashchuk.mathbubbles.core.component.viewModel
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns navigation for the settings screen: [onBackClicked] is called both by the screen's back
 * affordance and the system back button, mirroring how MenuComponent forwards its own events.
 */
class SettingsComponent(
    componentContext: ComponentContext,
    soundSettings: SoundSettings,
    private val onBackClicked: () -> Unit,
) : ComponentContext by componentContext {
    private val viewModel: SettingsViewModel = viewModel { SettingsViewModel(soundSettings) }

    val state: StateFlow<SettingsViewModel.State> = viewModel.state

    fun sendAction(action: SettingsViewModel.Action) = viewModel.sendAction(action)

    fun onBack() = onBackClicked()
}
