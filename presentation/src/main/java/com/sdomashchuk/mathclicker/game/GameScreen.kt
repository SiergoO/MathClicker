package com.sdomashchuk.mathclicker.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.sdomashchuk.mathclicker.util.toSymbol

@Composable
fun GameScreen(
    navController: NavController
) {

    val gameViewModel: GameViewModel = hiltViewModel()
    val state = gameViewModel.state.collectAsState()

    MathClickerTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                state.value.isGamePaused -> GamePausedOverlay { gameViewModel.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked) }
                !state.value.isGameStarted -> CountdownOverlay { gameViewModel.sendAction(GameViewModel.Action.StartGame) }
                else -> GameField(
                    state,
                    onTargetClicked = { gameViewModel.sendAction(GameViewModel.Action.FireButtonClicked) }
                )
            }
        }
        BackHandler {
            gameViewModel.sendAction(GameViewModel.Action.PauseGame)
        }
    }
}

@Composable
fun GameField(
    state: State<GameViewModel.State>,
    onTargetClicked: () -> Unit
) {
    var gameColumnWidth by remember { mutableStateOf(0) }
    var gameColumnHeight by remember { mutableStateOf(0) }
    val localDensity = LocalDensity.current

    Row(
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .fillMaxHeight(0.05f)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.game_session_level, state.value.session.level).toUpperCase(Locale.current),
            style = Typography.body1,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.game_session_score, state.value.session.score).toUpperCase(Locale.current),
            style = Typography.body1,
        )
    }
    Divider()
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .onGloballyPositioned { coordinates ->
                gameColumnWidth = with(localDensity) { (coordinates.size.width.toDp().value.toInt() - 3) / 4 }
            }
    ) {
        items(state.value.buttons) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(gameColumnWidth.dp)
                    .onGloballyPositioned { coordinates ->
                        gameColumnHeight =
                            with(localDensity) { coordinates.size.height.toDp().value.toInt() - (gameColumnWidth * 0.8).toInt() }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TargetButton(gameColumnWidth, gameColumnHeight, it.animationDurationMs, it.id)
            }
            if (it.id < state.value.buttons.lastIndex) {
                VerticalDivider()
            }
        }
    }
    Divider()
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = stringResource(
                id = R.string.game_session_combo,
                state.value.session.bonusMultiplier,
            ).toUpperCase(Locale.current),
            style = Typography.h1,
        )
        Button(
            onClick = onTargetClicked,
            modifier = Modifier
                .weight(1f)
                .wrapContentSize()
                .clip(CircleShape)
                .width(100.dp)
                .height(100.dp),
        ) {
            Text(
                text = state.value.session.let { "${it.currentOperationSign.toSymbol()}${it.currentOperationDigit}" },
                fontSize = 36.sp,
                color = White
            )
        }
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(backgroundColor = Red200),
            modifier = Modifier
                .weight(1f)
                .wrapContentSize()
                .clip(CircleShape)
                .width(48.dp)
                .height(48.dp)
                .alpha(0.8f)

        ) {
            Text(
                text = state.value.session.let { "${it.nextOperationSign.toSymbol()}${it.nextOperationDigit}" },
                fontSize = 12.sp,
                color = White
            )
        }
    }
}

@Composable
fun GamePausedOverlay(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Translucent)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(0.5f),
            contentAlignment = Alignment.Center
        ) {
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

@Composable
fun TargetButton(
    parentWidth: Int,
    parentHeight: Int,
    duration: Int,
    id: Int
) {
    val gameViewModel: GameViewModel = hiltViewModel()
    val state = gameViewModel.state.collectAsState()
    val isVisible = remember { mutableStateOf(true) }
    val targetButtonOffset by animateOffsetAsState(
        targetValue = Offset(0f, (parentHeight).toFloat()),
        animationSpec = tween(duration, easing = LinearEasing),
        finishedListener = { isVisible.value = false }
    )
    if (isVisible.value && state.value.buttons[id].isAlive) {
        Button(
            modifier = Modifier
                .offset(targetButtonOffset.x.dp, targetButtonOffset.y.dp)
                .height((parentWidth * 0.8).dp)
                .width((parentWidth * 0.8).dp)
                .clip(CircleShape),
            onClick = { gameViewModel.sendAction(GameViewModel.Action.TargetButtonClicked(id)) }
        ) {
            Text(text = state.value.buttons[id].value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
            .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    )
}