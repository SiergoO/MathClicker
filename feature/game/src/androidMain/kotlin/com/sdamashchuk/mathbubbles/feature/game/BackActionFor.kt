package com.sdamashchuk.mathbubbles.feature.game

internal fun backActionFor(phase: GamePhase): GameViewModel.Action =
    when (phase) {
        GamePhase.Playing -> GameViewModel.Action.PauseGame

        GamePhase.Paused -> GameViewModel.Action.ReadyToPlayButtonClicked

        GamePhase.ReadyToPlay,
        GamePhase.CountingDown,
        GamePhase.LevelIntro,
        GamePhase.GameOver,
        -> GameViewModel.Action.BackToMainMenuClicked
    }
