package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Composable
internal fun rememberPhase(gameState: State<GameViewModel.State>): State<GamePhase> =
    remember(gameState) { derivedStateOf { gameState.value.phase } }
