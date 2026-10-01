package com.sdamashchuk.mathbubbles.feature.game.model

import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * The player-facing response to one `GameEvent`: a haptic type, how many times to fire it, and a
 * severity that only orders effects against each other - a breakout must always outrank a
 * zeroing, never just happen to depending on the payload.
 */
sealed interface FeedbackEffect {
    val haptic: HapticFeedbackType
    val hapticRepeatCount: Int
    val severity: Int

    data class TargetZeroed(
        val targetId: Int,
        val awarded: Int,
    ) : FeedbackEffect {
        override val haptic = HapticFeedbackType.LongPress
        override val hapticRepeatCount = 1
        override val severity = SEVERITY_ZEROED
    }

    // Weight is proportional to streak so a growing combo reads as more than a reset one; capped
    // below SEVERITY_BROKE_OUT so no streak, however long, can outrank a life loss.
    data class OperationResolved(
        val gained: Int,
        val streak: Int,
    ) : FeedbackEffect {
        override val haptic = if (gained > 0) HapticFeedbackType.Confirm else HapticFeedbackType.Reject
        override val hapticRepeatCount = 1
        override val severity =
            if (gained > 0) {
                (SEVERITY_ZEROED + streak).coerceAtMost(SEVERITY_OPERATION_CAP)
            } else {
                SEVERITY_ZEROED
            }
    }

    // Doubled haptic, not just a stronger single one: this is the only effect in the table that
    // costs the player something irreversible, and the repeat is what makes it read that way.
    data class TargetBrokeOut(
        val livesLeft: Int,
    ) : FeedbackEffect {
        override val haptic = HapticFeedbackType.Reject
        override val hapticRepeatCount = 2
        override val severity = SEVERITY_BROKE_OUT
    }

    // LevelIntroOverlay already announces this; the haptic is the entire response here.
    data object LevelUp : FeedbackEffect {
        override val haptic = HapticFeedbackType.Confirm
        override val hapticRepeatCount = 1
        override val severity = SEVERITY_LEVEL_UP
    }

    // Outranks a plain LevelUp - gaining a life back is rarer and more consequential than the level
    // bump it always rides in on - but still reads as good news, unlike the two loss effects above.
    data class LifeGranted(
        val livesLeft: Int,
    ) : FeedbackEffect {
        override val haptic = HapticFeedbackType.Confirm
        override val hapticRepeatCount = 1
        override val severity = SEVERITY_LIFE_GRANTED
    }

    // A single Reject, not TargetBrokeOut's double one, so the last life and the game itself don't
    // feel identical even though both are losses.
    data object GameOver : FeedbackEffect {
        override val haptic = HapticFeedbackType.Reject
        override val hapticRepeatCount = 1
        override val severity = SEVERITY_GAME_OVER
    }
}

private const val SEVERITY_ZEROED = 1
private const val SEVERITY_LEVEL_UP = 2
private const val SEVERITY_LIFE_GRANTED = 3
private const val SEVERITY_OPERATION_CAP = 8
private const val SEVERITY_GAME_OVER = 9
private const val SEVERITY_BROKE_OUT = 10
