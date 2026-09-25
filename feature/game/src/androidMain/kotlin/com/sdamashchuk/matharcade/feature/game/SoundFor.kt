package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.ui.sound.model.SoundSample
import com.sdamashchuk.matharcade.feature.game.model.FeedbackEffect

// No `else` branch, mirroring effectFor: a new FeedbackEffect variant must fail this at compile
// time rather than silently play nothing. LifeGranted and GameOver map to null on purpose - the
// asset table has no sample for either, and neither is worth inventing one for here.
internal fun soundFor(effect: FeedbackEffect): SoundSample? =
    when (effect) {
        is FeedbackEffect.TargetZeroed -> {
            SoundSample.TargetCleared
        }

        // gained == 0 is the only signal a miss carries - there is no separate miss event.
        is FeedbackEffect.OperationResolved -> {
            if (effect.gained > 0) SoundSample.OperationSuccess else SoundSample.OperationMiss
        }

        is FeedbackEffect.TargetBrokeOut -> {
            SoundSample.LifeLost
        }

        FeedbackEffect.LevelUp -> {
            SoundSample.LevelUp
        }

        is FeedbackEffect.LifeGranted -> {
            null
        }

        FeedbackEffect.GameOver -> {
            null
        }
    }
