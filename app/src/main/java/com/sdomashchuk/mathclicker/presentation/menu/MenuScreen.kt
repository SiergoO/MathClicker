package com.sdomashchuk.mathclicker.presentation.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.presentation.component.MathClickerDialog
import com.sdomashchuk.mathclicker.presentation.navigation.Screen
import com.sdomashchuk.mathclicker.presentation.ui.theme.MathClickerTheme
import com.sdomashchuk.mathclicker.presentation.ui.theme.Red500
import com.sdomashchuk.mathclicker.presentation.ui.theme.Shapes
import com.sdomashchuk.mathclicker.presentation.ui.theme.Typography
import kotlinx.coroutines.flow.receiveAsFlow

@Composable
fun MenuScreen(
    navController: NavController
) {

    val menuViewModel: MenuViewModel = hiltViewModel()
    val state = menuViewModel.state.collectAsState()

    CollectUiEvents(
        viewModel = menuViewModel,
        navController = navController
    )

    if (state.value.isOpenDialog) {
        MathClickerDialog(
            headerText = stringResource(id = R.string.how_to_play_dialog_header),
            bodyText = stringResource(id = R.string.how_to_play_dialog_body),
            onDismiss = { menuViewModel.sendAction(MenuViewModel.Action.CloseDialog) },
            positiveButtonText = stringResource(id = R.string.how_to_play_dialog_button_positive),
            onPositive = { menuViewModel.sendAction(MenuViewModel.Action.CloseDialog) }
        )
    }

    MathClickerTheme {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val (howToPlayButton, logoImage, menuButtons) = createRefs()

            IconButton(
                modifier = Modifier
                    .constrainAs(howToPlayButton) {
                        end.linkTo(parent.end)
                        top.linkTo(parent.top)
                    }
                    .padding(16.dp),
                content = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_help),
                        contentDescription = stringResource(id = R.string.how_to_play_icon_content_description),
                        modifier = Modifier.size(40.dp)
                    )
                },
                onClick = {
                    menuViewModel.sendAction(MenuViewModel.Action.OpenDialog)
                }
            )
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = stringResource(id = R.string.logo),
                modifier = Modifier
                    .constrainAs(logoImage) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(horizontal = 40.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.constrainAs(menuButtons) {
                    top.linkTo(logoImage.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) {
                MenuButton(
                    text = stringResource(id = R.string.menu_button_play),
                    onClick = { menuViewModel.sendAction(MenuViewModel.Action.ButtonPlayClicked) }
                )
            }
            createVerticalChain(logoImage, menuButtons, chainStyle = ChainStyle.Spread)
        }
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Red500),
        modifier = modifier
            .width(200.dp)
            .height(60.dp)
            .clip(Shapes.large)
    ) {
        Text(
            text = text,
            style = Typography.h2
        )
    }
}

@Composable
fun CollectUiEvents(
    viewModel: MenuViewModel,
    navController: NavController
) {
    LaunchedEffect(
        key1 = null,
        block = {
            viewModel.uiEvents.receiveAsFlow().collect {
                when (it) {
                    is MenuViewModel.UiEvent.NavigateToGameScreen -> {
                        navController.navigate(Screen.Game.route)
                    }
                }
            }
        }
    )
}