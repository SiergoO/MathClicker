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

    // livesLeft is the field's lifeCount after the grant, already capped. Fired only by Game.grantLife
    // - MC-76 removed the every-N-levels trigger MC-54 wired through updateLevel, but this event (and
    // the feedback path that renders it) stays alive for whatever future event ends up granting one.
    data class LifeGranted(
        val livesLeft: Int,
    ) : GameEvent

    data object GameOver : GameEvent
}
