package com.sdamashchuk.matharcade.core.game.model

sealed interface GameEvent {
    data class TargetZeroed(
        val id: Int,
        val awarded: Int,
    ) : GameEvent

    data class OperationResolved(
        val gained: Int,
        val streak: Int,
    ) : GameEvent

    data class TargetBrokeOut(
        val id: Int,
        val livesLeft: Int,
    ) : GameEvent

    data class LevelUp(
        val level: Int,
    ) : GameEvent

    data object GameOver : GameEvent
}
