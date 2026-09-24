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

    // Fired once per qualifying level-up (see Field.updateLevel), never per tick: livesLeft is the
    // field's lifeCount after the grant, already capped.
    data class LifeGranted(
        val livesLeft: Int,
    ) : GameEvent

    data object GameOver : GameEvent
}
