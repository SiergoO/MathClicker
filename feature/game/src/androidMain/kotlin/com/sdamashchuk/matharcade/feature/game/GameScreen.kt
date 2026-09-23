package com.sdamashchuk.matharcade.feature.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
