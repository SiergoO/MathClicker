package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import com.sdamashchuk.mathbubbles.core.model.BOOSTER_STASH_CAPACITY
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.ui.sound.model.SoundSample
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import com.sdamashchuk.mathbubbles.feature.game.model.FeedbackEffect
import com.sdamashchuk.mathbubbles.feature.game.model.ShieldCrackCue
import com.sdamashchuk.mathbubbles.feature.game.model.TargetZeroedSignal
import io.github.alexzhirkevich.compottie.DotLottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val HAPTIC_REPEAT_GAP_MS = 80L
private const val SHAKE_STEP_MS = 60
private const val SHAKE_MAGNITUDE_PX = 12f

@Composable
fun GameScreen(component: GameComponent) {
    val gameState = component.state.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    var targetZeroedSignal by remember { mutableStateOf<TargetZeroedSignal?>(null) }
    val shieldCrackCue = remember { ShieldCrackCue() }
    val shieldCracking by shieldCrackCue.isCracking
    val resources = LocalResources.current
    val countdownComposition =
        rememberLottieComposition {
            LottieCompositionSpec.DotLottie(resources.openRawResource(R.raw.countdown).use { it.readBytes() })
        }
    val tapHighlightComposition =
        rememberLottieComposition {
            LottieCompositionSpec.DotLottie(resources.openRawResource(R.raw.tap_higlight).use { it.readBytes() })
        }
    val shakeOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var shakeSeed by remember { mutableIntStateOf(0) }

    val phase by rememberPhase(gameState)

    // See Field's own derivedStateOf comment: reading these fields straight off gameState.value
    // would drag the HUD through composition on every tick, not just the second it changes.
    val level by remember { derivedStateOf { gameState.value.field.level } }
    val score by remember { derivedStateOf { gameState.value.field.score } }
    val appliedMultiplier by remember { derivedStateOf { gameState.value.field.appliedMultiplier } }

    // Hoisted above Field: a breakout or level-up can unmount Field in the same tick that produced
    // the event, so a collector inside Field would race its own unmount and drop the feedback.
    LaunchedEffect(Unit) {
        collectFeedback(
            component = component,
            hapticFeedback = hapticFeedback,
            onShake = {
                val seed = ++shakeSeed
                launch { shakeField(shakeOffset, seed) }
            },
            onTargetZeroed = { targetZeroedSignal = it },
            onShieldAbsorbed = { shieldCrackCue.absorb() },
        )
    }

    MathBubblesTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = shakeOffset.value.x
                            translationY = shakeOffset.value.y
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (phase) {
                    GamePhase.ReadyToPlay -> {
                        ReadyToPlayOverlay(tapHighlightComposition) {
                            component.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
                        }
                    }

                    GamePhase.CountingDown -> {
                        CountdownOverlay(countdownComposition) {
                            component.sendAction(GameViewModel.Action.StartGame)
                        }
                    }

                    GamePhase.Playing, GamePhase.Paused -> {
                        if (shouldComposeField(phase)) {
                            GameChrome(
                                level = level,
                                score = score,
                                appliedMultiplier = appliedMultiplier,
                                onPauseClicked = { component.sendAction(GameViewModel.Action.PauseGame) },
                            ) {
                                Field(
                                    gameState,
                                    running = shouldRunField(phase),
                                    onTargetClicked = { id ->
                                        // Not routed through GameEvent: a tap that does not zero its
                                        // target emits nothing on that stream (see targetClicked), but
                                        // the asset table wants a sound on every tap regardless.
                                        component.playSound(SoundSample.Tap)
                                        component.sendAction(GameViewModel.Action.TargetClicked(id))
                                    },
                                    onFireClicked = {
                                        if (gameState.value.field.currentAction is FieldAction.BoosterAction) {
                                            component.playSound(SoundSample.OperationSuccess)
                                        }
                                        component.sendAction(GameViewModel.Action.FireButtonClicked)
                                    },
                                    onTick = { component.sendAction(GameViewModel.Action.Tick(it)) },
                                    targetZeroedSignal = targetZeroedSignal,
                                    shieldCracking = shieldCracking,
                                    onShieldCrackFinished = { shieldCrackCue.finish() },
                                    onStashBooster = {
                                        playTapUnlessStashFull(component, gameState.value)
                                        component.sendAction(GameViewModel.Action.StashBooster)
                                    },
                                    onApplyBoosterFromStash = { slotIndex ->
                                        component.playSound(SoundSample.OperationSuccess)
                                        component.sendAction(GameViewModel.Action.ApplyBoosterFromStash(slotIndex))
                                    },
                                    onDisarmIcePick = {
                                        component.playSound(SoundSample.Tap)
                                        component.sendAction(GameViewModel.Action.DisarmIcePick)
                                    },
                                )
                            }
                        }
                        if (phase == GamePhase.Paused) {
                            GamePausedDialog(
                                onResumeClicked = {
                                    component.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
                                },
                                onRestartClicked = { component.sendAction(GameViewModel.Action.RestartGame) },
                                onBackToMainMenuClicked = {
                                    component.sendAction(GameViewModel.Action.BackToMainMenuClicked)
                                },
                            )
                        }
                    }

                    GamePhase.LevelIntro -> {
                        LevelIntroOverlay(level = level) {
                            component.sendAction(GameViewModel.Action.LevelIntroFinished)
                        }
                    }

                    GamePhase.GameOver -> {
                        ResultsScreen(
                            field = gameState.value.field,
                            recentResults = gameState.value.recentResults,
                            bestResult = gameState.value.bestResult,
                            onRestartClicked = { component.sendAction(GameViewModel.Action.RestartGame) },
                            onBackToMainMenuClicked = {
                                component.sendAction(
                                    GameViewModel.Action.BackToMainMenuClicked,
                                )
                            },
                        )
                    }
                }
            }
        }
        BackHandler {
            component.sendAction(backActionFor(phase))
        }
        PauseOnBackground(phase = phase) {
            component.sendAction(GameViewModel.Action.PauseGame)
        }
        // Hoisted here rather than per-TargetButton: one observer for the whole screen, and it
        // observes in every branch above (paused, game-over, countdown), not only the running one.
        OnLifecycleEvent { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                component.sendAction(GameViewModel.Action.PersistTargetsNow)
            }
        }
    }
}

internal fun shouldComposeField(phase: GamePhase) = phase == GamePhase.Playing || phase == GamePhase.Paused

internal fun shouldRunField(phase: GamePhase) = phase == GamePhase.Playing

private fun playTapUnlessStashFull(
    component: GameComponent,
    gameState: GameViewModel.State,
) {
    if (gameState.field.boosterStash.size < BOOSTER_STASH_CAPACITY) {
        component.playSound(SoundSample.Tap)
    }
}

private suspend fun collectFeedback(
    component: GameComponent,
    hapticFeedback: HapticFeedback,
    onShake: () -> Unit,
    onTargetZeroed: (TargetZeroedSignal) -> Unit,
    onShieldAbsorbed: () -> Unit,
) {
    var zeroedSequence = 0
    component.feedback.receiveAsFlow().collect { effect ->
        if (effect is FeedbackEffect.TargetBrokeOut && effect.shieldAbsorbed) onShieldAbsorbed()
        repeat(effect.hapticRepeatCount) { repeatIndex ->
            hapticFeedback.performHapticFeedback(effect.haptic)
            if (repeatIndex < effect.hapticRepeatCount - 1) delay(HAPTIC_REPEAT_GAP_MS)
        }
        soundFor(effect)?.let { component.playSound(it) }
        if (effect is FeedbackEffect.TargetBrokeOut) onShake()
        if (effect is FeedbackEffect.TargetZeroed) {
            onTargetZeroed(TargetZeroedSignal(effect.targetId, ++zeroedSequence, effect.viaIcePick))
        }
    }
}

// Launched fire-and-forget on TargetBrokeOut so it never blocks the collector loop above from
// processing the next event; three steps read as a rattle rather than one big nudge.
private suspend fun shakeField(
    offset: Animatable<Offset, AnimationVector2D>,
    seed: Int,
) {
    ShakeMotion.keyframes(seed, SHAKE_MAGNITUDE_PX).forEach { target ->
        offset.animateTo(target, animationSpec = tween(SHAKE_STEP_MS))
    }
}
