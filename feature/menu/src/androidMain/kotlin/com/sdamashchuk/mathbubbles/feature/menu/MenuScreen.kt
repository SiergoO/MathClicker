package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import com.sdamashchuk.mathbubbles.core.ui.component.MathBubblesDialog
import com.sdamashchuk.mathbubbles.core.ui.component.MenuButton
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.component.ScreenWrapper
import com.sdamashchuk.mathbubbles.core.ui.theme.Accent

private val NAV_BAR_ICON_SIZE = 24.dp
private val NAV_BAR_TOUCH_TARGET = 48.dp
private val LOGO_HORIZONTAL_INSET = 40.dp
private val MENU_BUTTON_GAP = 12.dp

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
                    IconButton(
                        onClick = { component.sendAction(MenuViewModel.Action.OpenDialog) },
                        modifier = Modifier.size(NAV_BAR_TOUCH_TARGET),
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_help),
                                contentDescription = stringResource(id = R.string.how_to_play_icon_content_description),
                                modifier = Modifier.size(NAV_BAR_ICON_SIZE),
                                tint = Accent,
                            )
                        }
                    }
                },
            )
        },
    ) {
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            val (logoImage, menuButtons) = createRefs()

            MenuLogo(
                modifier =
                    Modifier
                        .constrainAs(logoImage) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }.padding(horizontal = LOGO_HORIZONTAL_INSET),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier =
                    Modifier.constrainAs(menuButtons) {
                        top.linkTo(logoImage.bottom)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
            ) {
                // SpaceEvenly distributes the chain's own slack, which is zero here - the two
                // buttons abutted into one notched shape on a device until this gap was explicit.
                MenuButton(
                    text = stringResource(id = R.string.menu_button_play),
                    onClick = { component.sendAction(MenuViewModel.Action.ButtonPlayClicked) },
                    horizontalPadding = 0.dp,
                )
                Spacer(modifier = Modifier.height(MENU_BUTTON_GAP))
                MenuButton(
                    text = stringResource(id = R.string.menu_button_settings),
                    onClick = { component.sendAction(MenuViewModel.Action.ButtonSettingsClicked) },
                    horizontalPadding = 0.dp,
                )
            }
            createVerticalChain(logoImage, menuButtons, chainStyle = ChainStyle.Spread)
        }
    }
}
