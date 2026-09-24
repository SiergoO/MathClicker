package com.sdamashchuk.matharcade.feature.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import com.sdamashchuk.matharcade.core.ui.theme.MathArcadeTheme

@Composable
fun GameScreen(component: GameComponent) {
    val gameState = component.state.collectAsState()

    MathArcadeTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (gameState.value.phase) {
                GamePhase.ReadyToPlay -> {
                    GamePausedOverlay {
                        component.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked)
                    }
                }

                GamePhase.CountingDown -> {
                    CountdownOverlay {
                        component.sendAction(GameViewModel.Action.StartGame)
                    }
                }

                GamePhase.Playing -> {
                    // The frame loop lives inside Field's LaunchedEffect, so this guard - not just
                    // being the body of this branch - is what a test can pin without a Compose rule:
                    // see shouldComposeField's test for the property that pausing stops the engine.
                    if (shouldComposeField(gameState.value.phase)) {
                        Field(
                            gameState,
                            onTargetClicked = { id -> component.sendAction(GameViewModel.Action.TargetClicked(id)) },
                            onFireClicked = { component.sendAction(GameViewModel.Action.FireButtonClicked) },
                            onTick = { component.sendAction(GameViewModel.Action.Tick(it)) },
                            onPauseClicked = { component.sendAction(GameViewModel.Action.PauseGame) },
                        )
                    }
                }

                GamePhase.Paused -> {
                    GamePausedDialog(
                        onResumeClicked = { component.sendAction(GameViewModel.Action.ReadyToPlayButtonClicked) },
                        onRestartClicked = { component.sendAction(GameViewModel.Action.RestartGame) },
                        onBackToMainMenuClicked = { component.sendAction(GameViewModel.Action.BackToMainMenuClicked) },
                    )
                }

                // MC-57 adds this phase's UI; unreachable until then.
                GamePhase.LevelIntro -> {}

                GamePhase.GameOver -> {
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
            }
        }
        BackHandler {
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

// Field owns the frame loop that drives the engine clock (withFrameNanos -> onTick), so composing
// it only for Playing is what makes pausing stop that clock structurally rather than via a flag.
internal fun shouldComposeField(phase: GamePhase) = phase == GamePhase.Playing
