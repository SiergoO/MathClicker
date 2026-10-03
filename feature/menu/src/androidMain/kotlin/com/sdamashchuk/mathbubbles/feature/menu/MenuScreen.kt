package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sdamashchuk.mathbubbles.core.ui.component.AmbientBubbles
import com.sdamashchuk.mathbubbles.core.ui.component.BottomActions
import com.sdamashchuk.mathbubbles.core.ui.component.GlassButton
import com.sdamashchuk.mathbubbles.core.ui.component.MathBubblesDialog
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.NavBarAction
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper

private const val LOGO_AREA_WEIGHT = 2f
private const val BUTTON_AREA_WEIGHT = 1f

@Composable
fun MenuScreen(
    component: MenuComponent,
    ambientTimeMsProvider: () -> Long = rememberMenuAmbientClock(component.lifecycle),
) {
    val state = component.state.collectAsState()

    if (state.value.isOpenDialog) {
        MathBubblesDialog(
            headerText = stringResource(id = R.string.how_to_play_dialog_header),
            bodyText = stringResource(id = R.string.how_to_play_dialog_body),
            onDismiss = { component.sendAction(MenuViewModel.Action.CloseDialog) },
            positiveButtonText = stringResource(id = R.string.how_to_play_dialog_button_positive),
            onPositive = { component.sendAction(MenuViewModel.Action.CloseDialog) },
        )
    }

    ScreenWrapper(
        background = {
            AmbientBubbles(
                style = MenuAmbientBubbleStyle,
                timeMsProvider = ambientTimeMsProvider,
                modifier = Modifier.fillMaxSize(),
            )
        },
        topBar = {
            NavBar(
                actions = {
                    NavBarAction(
                        painter = painterResource(id = R.drawable.ic_help),
                        contentDescription = stringResource(id = R.string.how_to_play_icon_content_description),
                        onClick = { component.sendAction(MenuViewModel.Action.OpenDialog) },
                    )
                },
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(LOGO_AREA_WEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                MenuLogo(timeMsProvider = ambientTimeMsProvider, modifier = Modifier.fillMaxSize())
            }
            Box(modifier = Modifier.fillMaxWidth().weight(BUTTON_AREA_WEIGHT)) {
                BottomActions {
                    if (state.value.hasUnfinishedField) {
                        GlassButton(
                            text = stringResource(id = R.string.menu_button_continue),
                            onClick = { component.sendAction(MenuViewModel.Action.ButtonContinueClicked) },
                        )
                    }
                    GlassButton(
                        text = stringResource(id = R.string.menu_button_play),
                        onClick = { component.sendAction(MenuViewModel.Action.ButtonPlayClicked) },
                    )
                    GlassButton(
                        text = stringResource(id = R.string.menu_button_settings),
                        onClick = { component.sendAction(MenuViewModel.Action.ButtonSettingsClicked) },
                    )
                }
            }
        }
    }
}
