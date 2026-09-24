package com.sdamashchuk.matharcade.feature.game

enum class GamePhase {
    ReadyToPlay,
    CountingDown,
    Playing,
    Paused,

    // Entered from Playing on a level-up delta and left after the 800ms LEVEL N announcement
    // (MC-57). Field is only composed for Playing, so this phase stops the engine by itself.
    LevelIntro,
    GameOver,
}
