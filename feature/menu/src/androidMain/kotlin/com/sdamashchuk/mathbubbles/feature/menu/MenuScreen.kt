package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.component.GlassBackdrop
import com.sdamashchuk.mathbubbles.core.ui.component.GlassButton
import com.sdamashchuk.mathbubbles.core.ui.component.MathBubblesDialog
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.NavBarAction
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper

private val LOGO_HORIZONTAL_INSET = 40.dp
private val MENU_BUTTON_GAP = 20.dp
private val MENU_BUTTON_BOTTOM_MARGIN = 28.dp
private const val LOGO_AREA_WEIGHT = 2f
private const val BUTTON_AREA_WEIGHT = 1f

@Composable
fun MenuScreen(component: MenuComponent) {
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
                MenuLogo(modifier = Modifier.padding(horizontal = LOGO_HORIZONTAL_INSET))
            }
            GlassBackdrop(
                modifier = Modifier.fillMaxWidth().weight(BUTTON_AREA_WEIGHT),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxSize().padding(bottom = MENU_BUTTON_BOTTOM_MARGIN),
                ) {
                    GlassButton(
                        text = stringResource(id = R.string.menu_button_play),
                        onClick = { component.sendAction(MenuViewModel.Action.ButtonPlayClicked) },
                    )
                    Spacer(modifier = Modifier.height(MENU_BUTTON_GAP))
                    GlassButton(
                        text = stringResource(id = R.string.menu_button_settings),
                        onClick = { component.sendAction(MenuViewModel.Action.ButtonSettingsClicked) },
                    )
                }
            }
        }
    }
}
