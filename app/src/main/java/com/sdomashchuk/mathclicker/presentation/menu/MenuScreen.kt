package com.sdomashchuk.mathclicker.presentation.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.core.ui.component.MathClickerDialog
import com.sdomashchuk.mathclicker.core.ui.component.MenuButton
import com.sdomashchuk.mathclicker.core.ui.theme.MathClickerTheme

@Composable
fun MenuScreen(component: MenuComponent) {
    val state = component.state.collectAsState()

    if (state.value.isOpenDialog) {
        MathClickerDialog(
            headerText = stringResource(id = R.string.how_to_play_dialog_header),
            bodyText = stringResource(id = R.string.how_to_play_dialog_body),
            onDismiss = { component.sendAction(MenuViewModel.Action.CloseDialog) },
            positiveButtonText = stringResource(id = R.string.how_to_play_dialog_button_positive),
            onPositive = { component.sendAction(MenuViewModel.Action.CloseDialog) },
        )
    }

    MathClickerTheme {
        ConstraintLayout(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        ) {
            val (howToPlayButton, logoImage, menuButtons) = createRefs()

            IconButton(
                modifier =
                    Modifier
                        .constrainAs(howToPlayButton) {
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                        }.padding(16.dp),
                content = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_help),
                        contentDescription = stringResource(id = R.string.how_to_play_icon_content_description),
                        modifier = Modifier.size(40.dp),
                    )
                },
                onClick = {
                    component.sendAction(MenuViewModel.Action.OpenDialog)
                },
            )
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = stringResource(id = R.string.logo),
                modifier =
                    Modifier
                        .constrainAs(logoImage) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }.padding(horizontal = 40.dp),
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
                MenuButton(
                    text = stringResource(id = R.string.menu_button_play),
                    onClick = { component.sendAction(MenuViewModel.Action.ButtonPlayClicked) },
                )
            }
            createVerticalChain(logoImage, menuButtons, chainStyle = ChainStyle.Spread)
        }
    }
}
