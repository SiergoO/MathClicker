package com.sdamashchuk.matharcade.feature.game

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.sdamashchuk.matharcade.core.game.model.GameEvent
import com.sdamashchuk.matharcade.feature.game.model.FeedbackEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectForTest {
    @Test
    fun `TargetZeroed maps to a light single tick carrying the awarded amount`() {
        val effect = effectFor(GameEvent.TargetZeroed(id = 4, awarded = 1))
        assertEquals(FeedbackEffect.TargetZeroed(targetId = 4, awarded = 1), effect)
        assertEquals(HapticFeedbackType.LongPress, effect.haptic)
        assertEquals(1, effect.hapticRepeatCount)
    }

    // M2: without the streak term this and the next test collapse to the same severity.
    @Test
    fun `OperationResolved weight grows with streak on a successful operation`() {
        val lowStreak = effectFor(GameEvent.OperationResolved(gained = 2, streak = 1))
        val highStreak = effectFor(GameEvent.OperationResolved(gained = 2, streak = 6))
        assertTrue(highStreak.severity > lowStreak.severity)
        assertEquals(HapticFeedbackType.Confirm, lowStreak.haptic)
    }

    @Test
    fun `OperationResolved reads as a miss when nothing was gained, regardless of streak`() {
        val effect = effectFor(GameEvent.OperationResolved(gained = 0, streak = 5))
        assertEquals(HapticFeedbackType.Reject, effect.haptic)
    }

    // M1: TargetBrokeOut and TargetZeroed swapped would still compile and pass every other
    // assertion here - only a direct identity check on the mapped type catches the swap.
    @Test
    fun `TargetBrokeOut maps to a doubled, heaviest response`() {
        val effect = effectFor(GameEvent.TargetBrokeOut(id = 2, livesLeft = 1))
        assertEquals(FeedbackEffect.TargetBrokeOut(livesLeft = 1), effect)
        assertEquals(HapticFeedbackType.Reject, effect.haptic)
        assertEquals(2, effect.hapticRepeatCount)
    }

    @Test
    fun `LevelUp maps to a single confirm with no repeat`() {
        val effect = effectFor(GameEvent.LevelUp(level = 3))
        assertEquals(FeedbackEffect.LevelUp, effect)
        assertEquals(HapticFeedbackType.Confirm, effect.haptic)
        assertEquals(1, effect.hapticRepeatCount)
    }

    // M3: mapping GameOver to no effect would make this null or crash on .haptic before reaching
    // the assertion below.
    @Test
    fun `GameOver maps to a single decisive reject, not a repeat of a breakout`() {
        val effect = effectFor(GameEvent.GameOver)
        assertEquals(FeedbackEffect.GameOver, effect)
        assertEquals(HapticFeedbackType.Reject, effect.haptic)
        assertEquals(1, effect.hapticRepeatCount)
    }

    // The severity ordering as a property, not a pair of literals: every TargetBrokeOut, for any
    // livesLeft, must outrank every TargetZeroed, for any awarded amount.
    @Test
    fun `TargetBrokeOut always outranks TargetZeroed, for any payload`() {
        val zeroings = (0..1).map { awarded -> effectFor(GameEvent.TargetZeroed(id = 1, awarded = awarded)) }
        val breakouts = (0..3).map { livesLeft -> effectFor(GameEvent.TargetBrokeOut(id = 1, livesLeft = livesLeft)) }
        breakouts.forEach { breakout -> zeroings.forEach { zeroed -> assertTrue(breakout.severity > zeroed.severity) } }
    }

    // The property holds even at OperationResolved's own ceiling: an arbitrarily long streak must
    // never let a successful operation outrank a life loss.
    @Test
    fun `TargetBrokeOut outranks even a long-streak OperationResolved`() {
        val breakout = effectFor(GameEvent.TargetBrokeOut(id = 1, livesLeft = 0))
        val hugeStreak = effectFor(GameEvent.OperationResolved(gained = 2, streak = 1000))
        assertTrue(breakout.severity > hugeStreak.severity)
    }

    // A `when` with no `else` over GameEvent: this compiles only as long as effectFor stays
    // exhaustive, so a new GameEvent variant fails the build here rather than silently mapping to
    // nothing.
    @Test
    fun `effectFor is exhaustive over every GameEvent variant`() {
        val events =
            listOf(
                GameEvent.TargetZeroed(id = 1, awarded = 1),
                GameEvent.OperationResolved(gained = 1, streak = 1),
                GameEvent.TargetBrokeOut(id = 1, livesLeft = 1),
                GameEvent.LevelUp(level = 1),
                GameEvent.GameOver,
            )
        events.forEach { event -> effectFor(event) }
    }
}
