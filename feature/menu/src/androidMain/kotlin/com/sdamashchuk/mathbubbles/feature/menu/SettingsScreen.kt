package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent
import com.sdamashchuk.mathbubbles.core.ui.theme.AccentSoft

@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state = component.state.collectAsState()

    // No explicit BackHandler needed: RootContent's ChildStackHost already wraps every screen in
    // predictive back that pops the stack, which is exactly what leaving Settings should do -
    // GameScreen overrides it because pausing instead of popping is the one exception, not this.
    ScreenWrapper(
        topBar = {
            NavBar(
                title = stringResource(id = R.string.settings_screen_title),
                onBack = { component.onBack() },
            )
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.settings_sound_switch_label),
                style = MaterialTheme.typography.body1,
            )
            // Material's default switch colours are teal; every other control in this app is
            // Accent, and an off-brand accent on the only settings screen reads as unfinished.
            Switch(
                checked = state.value.soundEnabled,
                onCheckedChange = {
                    component.sendAction(SettingsViewModel.Action.SoundSwitchToggled(it))
                },
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = Accent,
                        checkedTrackColor = AccentSoft,
                    ),
            )
        }
    }
}
