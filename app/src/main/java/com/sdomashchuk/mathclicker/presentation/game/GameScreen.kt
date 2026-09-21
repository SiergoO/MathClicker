package com.sdomashchuk.mathclicker.presentation.game

import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.core.model.Target
import com.sdomashchuk.mathclicker.core.ui.theme.MathClickerTheme
import com.sdomashchuk.mathclicker.core.ui.theme.Red200
import com.sdomashchuk.mathclicker.core.ui.theme.Red500
import com.sdomashchuk.mathclicker.core.ui.theme.Translucent
import com.sdomashchuk.mathclicker.core.ui.theme.White
import com.sdomashchuk.mathclicker.presentation.component.GameMenuDialog
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlin.math.roundToInt

@Composable
fun GameScreen(component: GameComponent) {
    val gameState = component.state.collectAsState()

    MathClickerTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                gameState.value.field.isClosed -> {
                    GameMenuDialog(
                        headerText = stringResource(id = R.string.game_over),
                        onRestartClicked = { component.sendAction(GameViewModel.Action.RestartGame) },
                        onBackToMainMenuClicked = {
                            component.sendAction(
                                GameViewModel.Action.BackToMainMenuClicked,
                            )
                        },
                    )
                }

                gameState.value.isGamePaused -> {
                    GamePausedOverlay {
                        component.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
                    }
                }

                !gameState.value.isGameStarted -> {
                    CountdownOverlay {
                        component.sendAction(GameViewModel.Action.StartGame)
                    }
                }

                else -> {
                    Field(
                        gameState,
                        onGameColumnSizeMeasured = { size ->
                            component.sendAction(GameViewModel.Action.GameColumnSizeMeasured(size))
                        },
                        onTargetRevealed = { id -> component.sendAction(GameViewModel.Action.TargetRevealed(id)) },
                        onTargetClicked = { id -> component.sendAction(GameViewModel.Action.TargetClicked(id)) },
                        onTargetDidBreakout = { id ->
                            component.sendAction(GameViewModel.Action.TargetDidBreakout(id))
                        },
                        onTargetPositionSave = { id, position, gameColumnHeightPx ->
                            component.sendAction(
                                GameViewModel.Action.SaveTargetPosition(
                                    id,
                                    position,
                                    gameColumnHeightPx,
                                ),
                            )
                        },
                        onFireClicked = { component.sendAction(GameViewModel.Action.FireButtonClicked) },
                    )
                }
            }
        }
        BackHandler {
            component.sendAction(GameViewModel.Action.PauseGame)
        }
    }
}

@Composable
fun Field(
    gameState: State<GameViewModel.State>,
    onGameColumnSizeMeasured: (gameColumnSize: Size) -> Unit,
    onTargetRevealed: (id: Int) -> Unit,
    onTargetClicked: (id: Int) -> Unit,
    onTargetDidBreakout: (id: Int) -> Unit,
    onTargetPositionSave: (id: Int, position: Int, gameColumnHeightPx: Int) -> Unit,
    onFireClicked: () -> Unit,
) {
    var gameColumnSize by remember { mutableStateOf(Size(0, 0)) }
    val localDensity = LocalDensity.current

    Row(
        modifier =
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .fillMaxHeight(0.05f),
    ) {
        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text =
                stringResource(
                    id = R.string.game_session_level,
                    gameState.value.field.level,
                ).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
        Text(
            modifier =
                Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text =
                stringResource(
                    id = R.string.game_session_score,
                    gameState.value.field.score,
                ).toUpperCase(Locale.current),
            style = MaterialTheme.typography.body1,
        )
    }
    Divider()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .onGloballyPositioned { coordinates ->
                    val gameColumnWidth =
                        with(localDensity) {
                            (
                                coordinates.size.width
                                    .toDp()
                                    .value
                                    .toInt() - 3
                            ) / 4
                        }
                    val gameColumnHeight =
                        with(localDensity) {
                            coordinates.size.height
                                .toDp()
                                .value
                                .toInt() - (gameColumnWidth * 0.8).toInt()
                        }
                    gameColumnSize = Size(gameColumnWidth, gameColumnHeight)
                    onGameColumnSizeMeasured.invoke(gameColumnSize)
                },
    ) {
        repeat(4) { columnId ->
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(gameColumnSize.width.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                gameState.value.targetList.filter { target -> target.columnId == columnId }.forEach {
                    TargetButton(
                        it,
                        gameColumnSize,
                        onTargetRevealed = onTargetRevealed,
                        onTargetClicked = onTargetClicked,
                        onTargetDidBreakout = onTargetDidBreakout,
                        onTargetPositionSave = onTargetPositionSave,
                    )
                }
            }
            if (columnId < 4) {
                VerticalDivider()
            }
        }
    }

    Box(
        modifier =
            Modifier
                .navigationBarsPadding()
                .fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
        ) {
            repeat(3) {
                Divider(color = if (it < gameState.value.field.lifeCount) Red500 else Color.LightGray, thickness = 3.dp)
                Spacer(modifier = Modifier.padding(bottom = 2.dp))
            }
        }
        Row(
            modifier =
                Modifier
                    .navigationBarsPadding()
                    .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text =
                    stringResource(
                        id = R.string.game_session_combo,
                        gameState.value.field.bonusMultiplier,
                    ).toUpperCase(Locale.current),
                style = MaterialTheme.typography.h2,
            )
            Button(
                onClick = onFireClicked,
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .clip(CircleShape)
                        .width(100.dp)
                        .height(100.dp),
            ) {
                Text(
                    text = gameState.value.field.let { "${it.currentOperationSign.sign}${it.currentOperationDigit}" },
                    fontSize = 36.sp,
                    color = White,
                )
            }
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(backgroundColor = Red200),
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentSize()
                        .clip(CircleShape)
                        .width(48.dp)
                        .height(48.dp)
                        .alpha(0.8f),
            ) {
                Text(
                    text = gameState.value.field.let { "${it.nextOperationSign.sign}${it.nextOperationDigit}" },
                    fontSize = 12.sp,
                    color = White,
                )
            }
        }
    }
}

@Composable
fun GamePausedOverlay(onClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Translucent)
                .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(0.5f),
            contentAlignment = Alignment.Center,
        ) {
            val resources = LocalResources.current
            val composition by rememberLottieComposition {
                LottieCompositionSpec.DotLottie(resources.openRawResource(R.raw.tap_higlight).readBytes())
            }
            Image(
                painter = rememberLottiePainter(composition = composition, iterations = Compottie.IterateForever),
                contentDescription = null,
            )
            Text(
                text = stringResource(id = R.string.pause).toUpperCase(Locale.current),
                style = MaterialTheme.typography.body1,
                color = White,
            )
        }
        Text(
            text = stringResource(id = R.string.ready_to_pay_overlay_hint).toUpperCase(Locale.current),
            style = MaterialTheme.typography.h2,
            color = Red200,
        )
    }
}

@Composable
fun CountdownOverlay(onFinish: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxSize()
                .background(Translucent)
                .padding(40.dp),
    ) {
        val resources = LocalResources.current
        val composition by rememberLottieComposition {
            LottieCompositionSpec.DotLottie(resources.openRawResource(R.raw.countdown).readBytes())
        }
        val progress by animateLottieCompositionAsState(composition)
        Image(
            painter = rememberLottiePainter(composition = composition, progress = { progress }),
            contentDescription = null,
        )
        LaunchedEffect(progress) {
            if (progress >= 1f) {
                onFinish.invoke()
            }
        }
    }
}

@Composable
fun TargetButton(
    target: Target,
    gameColumnSize: Size,
    onTargetRevealed: (id: Int) -> Unit,
    onTargetClicked: (id: Int) -> Unit,
    onTargetDidBreakout: (id: Int) -> Unit,
    onTargetPositionSave: (id: Int, position: Int, gameColumnHeightPx: Int) -> Unit,
) {
    var isNeedToRefreshAnimation by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = target.appearanceDelayMs, key2 = target.isActive) {
        isNeedToRefreshAnimation = true
    }
    val infiniteTransition =
        if (!isNeedToRefreshAnimation && target.isActive) {
            rememberInfiniteTransition()
        } else {
            isNeedToRefreshAnimation = false
            null
        }
    val targetButtonYOffset =
        if (infiniteTransition != null) {
            val yOffset by infiniteTransition.animateFloat(
                initialValue = target.position.toFloat(),
                targetValue = gameColumnSize.height.toFloat(),
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                target.lifetimeMs,
                                easing = LinearEasing,
                                delayMillis = target.appearanceDelayMs,
                            ),
                        repeatMode = RepeatMode.Restart,
                    ),
            )
            yOffset
        } else {
            0f
        }
    if (targetButtonYOffset > 0f && !target.isVisible) onTargetRevealed.invoke(target.id)
    if (gameColumnSize.height != 0 && targetButtonYOffset.roundToInt() + 1 >= gameColumnSize.height) {
        onTargetDidBreakout.invoke(target.id)
    }
    if (target.isActive && targetButtonYOffset.dp > 0.dp) {
        Button(
            modifier =
                Modifier
                    .width((gameColumnSize.width * 0.8).dp)
                    .height((gameColumnSize.width * 0.8).dp)
                    .offset(0.dp, targetButtonYOffset.dp)
                    .clip(CircleShape),
            colors =
                ButtonDefaults.buttonColors(
                    backgroundColor = if (target.isProfitable) Red200 else Color.LightGray,
                ),
            onClick = { onTargetClicked.invoke(target.id) },
        ) {
            Text(text = target.value.toString(), fontSize = 20.sp, color = Color.White)
        }
    }
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                onTargetPositionSave.invoke(target.id, targetButtonYOffset.toInt(), gameColumnSize.height)
            }

            else -> { /* do nothing */ }
        }
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
    )
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer =
            LifecycleEventObserver { owner, event ->
                eventHandler.value(owner, event)
            }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}
