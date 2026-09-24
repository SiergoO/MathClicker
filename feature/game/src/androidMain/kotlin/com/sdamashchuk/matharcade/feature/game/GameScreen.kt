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
                        onTargetClicked = { id -> component.sendAction(GameViewModel.Action.TargetClicked(id)) },
                        onFireClicked = { component.sendAction(GameViewModel.Action.FireButtonClicked) },
                        onTick = { component.sendAction(GameViewModel.Action.Tick(it)) },
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
