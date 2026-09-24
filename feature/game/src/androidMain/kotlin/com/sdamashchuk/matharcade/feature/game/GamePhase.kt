package com.sdamashchuk.matharcade.feature.game

enum class GamePhase {
    ReadyToPlay,
    CountingDown,
    Playing,
    Paused,

    // Unused until MC-57 wires the level-up transition; kept here now so GameScreen's when is
    // exhaustive over the final shape of the enum instead of growing again later.
    LevelIntro,
    GameOver,
}
