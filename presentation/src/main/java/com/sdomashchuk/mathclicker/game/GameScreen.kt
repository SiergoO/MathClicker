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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.domain.model.game.session.TargetParams
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
    val gameState = gameViewModel.state.collectAsState()

    MathClickerTheme {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                gameState.value.isGamePaused -> GamePausedOverlay { gameViewModel.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked) }
                !gameState.value.isGameStarted -> CountdownOverlay { gameViewModel.sendAction(GameViewModel.Action.StartGame) }
                else -> GameField(
                    gameState,
                    onTargetClicked = { id -> gameViewModel.sendAction(GameViewModel.Action.TargetClicked(id)) },
                    onTargetBreakout = { id -> gameViewModel.sendAction(GameViewModel.Action.TargetBreakout(id)) },
                    onTargetPositionSave = { id, position ->
                        gameViewModel.sendAction(
                            GameViewModel.Action.SaveTargetPosition(
                                id,
                                position
                            )
                        )
                    },
                    onFireClicked = { gameViewModel.sendAction(GameViewModel.Action.FireButtonClicked) }
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
    gameState: State<GameViewModel.State>,
    onTargetClicked: (Int) -> Unit,
    onTargetBreakout: (Int) -> Unit,
    onTargetPositionSave: (Int, Int) -> Unit,
    onFireClicked: () -> Unit
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
            text = stringResource(
                id = R.string.game_session_level,
                gameState.value.gameSession.gameField.level
            ).toUpperCase(Locale.current),
            style = Typography.body1,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text = stringResource(
                id = R.string.game_session_score,
                gameState.value.gameSession.gameField.score
            ).toUpperCase(Locale.current),
            style = Typography.body1,
        )
    }
    Divider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.75f)
            .onGloballyPositioned { coordinates ->
                gameColumnWidth = with(localDensity) { (coordinates.size.width.toDp().value.toInt() - 3) / 4 }
            }
    ) {
        repeat(4) { columnId ->
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(gameColumnWidth.dp)
                    .onGloballyPositioned { coordinates ->
                        gameColumnHeight =
                            with(localDensity) { coordinates.size.height.toDp().value.toInt() - (gameColumnWidth * 0.8).toInt() }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                gameState.value.buttons.filter { target -> target.columnId == columnId }.forEach {
                    TargetButton(
                        it,
                        gameColumnWidth,
                        gameColumnHeight,
                        onTargetClicked = onTargetClicked,
                        onTargetBreakout = onTargetBreakout,
                        onTargetPositionSave = onTargetPositionSave
                    )
                }
            }
            if (columnId < 4) {
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
                gameState.value.gameSession.gameField.bonusMultiplier,
            ).toUpperCase(Locale.current),
            style = Typography.h1,
        )
        Button(
            onClick = onFireClicked,
            modifier = Modifier
                .weight(1f)
                .wrapContentSize()
                .clip(CircleShape)
                .width(100.dp)
                .height(100.dp),
        ) {
            Text(
                text = gameState.value.gameSession.gameField.let { "${it.currentOperationSign.toSymbol()}${it.currentOperationDigit}" },
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
                text = gameState.value.gameSession.gameField.let { "${it.nextOperationSign.toSymbol()}${it.nextOperationDigit}" },
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
    targetParams: TargetParams,
    parentWidth: Int,
    parentHeight: Int,
    onTargetClicked: (Int) -> Unit,
    onTargetBreakout: (Int) -> Unit,
    onTargetPositionSave: (Int, Int) -> Unit
) {

    val targetButtonOffset by animateOffsetAsState(
        targetValue = Offset(0f, parentHeight.toFloat() - targetParams.position.toFloat()),
        animationSpec = tween(
            targetParams.animationDurationMs,
            easing = LinearEasing,
            delayMillis = targetParams.animationDelayMs
        ),
        finishedListener = { onTargetBreakout.invoke(targetParams.id) }
    )
    if (targetParams.isAlive && targetButtonOffset.y.dp + targetParams.position.dp > 0.dp) {
        Button(
            onClick = { onTargetClicked.invoke(targetParams.id) },
            modifier = Modifier
                .width((parentWidth * 0.8).dp)
                .height((parentWidth * 0.8).dp)
                .offset(targetButtonOffset.x.dp, targetButtonOffset.y.dp + targetParams.position.dp)
                .clip(CircleShape)
        ) {
            Text(text = targetParams.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                onTargetPositionSave.invoke(targetParams.id, targetButtonOffset.y.toInt())
            }
            else -> { /* do nothing */
            }
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

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}