package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.sdamashchuk.mathbubbles.core.ui.component.GlassButton
import com.sdamashchuk.mathbubbles.core.ui.theme.Scrim

private val CardPadding = 16.dp
private val ButtonSpacing = 12.dp
private val CardElevation = 0.dp

@Composable
fun GamePausedDialog(
    onResumeClicked: () -> Unit,
    onRestartClicked: () -> Unit,
    onBackToMainMenuClicked: () -> Unit,
) {
    Dialog(
        onDismissRequest = onResumeClicked,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
    ) {
        // The platform dialog window dims its own background by default, which would stack with
        // Scrim below; zeroing it leaves Scrim as the only darkening over the frozen game.
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        SideEffect { dialogWindowProvider?.window?.setDimAmount(0f) }

        Column(
            modifier = Modifier.fillMaxSize().background(Scrim).padding(CardPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardElevation) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(CardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = CardPadding),
                        text = stringResource(id = R.string.pause),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h1,
                    )
                    GlassButton(
                        modifier = Modifier.padding(bottom = ButtonSpacing),
                        text = stringResource(id = R.string.resume),
                        onClick = onResumeClicked,
                    )
                    GlassButton(
                        modifier = Modifier.padding(bottom = ButtonSpacing),
                        text = stringResource(id = R.string.restart),
                        onClick = onRestartClicked,
                    )
                    GlassButton(
                        text = stringResource(id = R.string.main_menu),
                        onClick = onBackToMainMenuClicked,
                    )
                }
            }
        }
    }
}
