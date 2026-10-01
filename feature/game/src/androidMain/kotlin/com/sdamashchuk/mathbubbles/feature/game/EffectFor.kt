package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.game.model.GameEvent
import com.sdamashchuk.mathbubbles.feature.game.model.FeedbackEffect

// No `else` branch: a new GameEvent variant must fail this at compile time rather than silently
// carry no feedback (the failure mode MC-58's M3 mutant exists to catch).
internal fun effectFor(event: GameEvent): FeedbackEffect =
    when (event) {
        is GameEvent.TargetZeroed -> FeedbackEffect.TargetZeroed(event.id, event.awarded, event.viaIcePick)
        is GameEvent.OperationResolved -> FeedbackEffect.OperationResolved(event.gained, event.streak)
        is GameEvent.TargetBrokeOut -> FeedbackEffect.TargetBrokeOut(event.livesLeft, event.shieldAbsorbed)
        is GameEvent.LevelUp -> FeedbackEffect.LevelUp
        is GameEvent.LifeGranted -> FeedbackEffect.LifeGranted(event.livesLeft)
        GameEvent.GameOver -> FeedbackEffect.GameOver
    }
