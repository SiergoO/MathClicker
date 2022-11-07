package com.sdomashchuk.mathclicker.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.ui.theme.MathClickerTheme
import com.sdomashchuk.mathclicker.ui.theme.Red200
import com.sdomashchuk.mathclicker.ui.theme.Translucent
import com.sdomashchuk.mathclicker.ui.theme.Typography
import com.sdomashchuk.mathclicker.ui.theme.White

@Composable
fun GameScreen(
    navController: NavController
) {

    val gameViewModel: GameViewModel = hiltViewModel()
    val state = gameViewModel.state.collectAsState()

    MathClickerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!state.value.isReadyToPlay) {
                ReadyToPlayOverlay { gameViewModel.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked) }
            } else {
                if (!state.value.isGameActive) {
                    CountdownOverlay { gameViewModel.sendAction(GameViewModel.Action.StartGame) }
                } else {
                    Text(text = "<3 Nastya")
                }
            }
        }
    }
}

@Composable
fun ReadyToPlayOverlay(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Translucent)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.tap_higlight))
            LottieAnimation(composition = composition, iterations = LottieConstants.IterateForever)
            Text(
                text = stringResource(id = R.string.pause).toUpperCase(Locale.current),
                style = Typography.body1,
                color = White
            )
        }
        Text(
            text = stringResource(id = R.string.ready_to_pay_overlay_hint).toUpperCase(Locale.current),
            style = Typography.h1,
            color = Red200
        )
    }
}

@Composable
fun CountdownOverlay(onFinish: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Translucent)
            .padding(40.dp)
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.countdown))
        val progress by animateLottieCompositionAsState(composition)
        LottieAnimation(composition)
        LaunchedEffect(progress) {
            if (progress >= 1f) {
                onFinish.invoke()
            }
        }
    }
}