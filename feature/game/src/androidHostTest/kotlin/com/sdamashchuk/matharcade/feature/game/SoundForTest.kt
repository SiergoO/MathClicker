package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.ui.sound.model.SoundSample
import com.sdamashchuk.matharcade.feature.game.model.FeedbackEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundForTest {
    @Test
    fun `TargetZeroed maps to the target-cleared sample`() {
        assertEquals(SoundSample.TargetCleared, soundFor(FeedbackEffect.TargetZeroed(targetId = 1, awarded = 1)))
    }

    @Test
    fun `OperationResolved with a gain maps to the operation-success sample`() {
        assertEquals(
            SoundSample.OperationSuccess,
            soundFor(FeedbackEffect.OperationResolved(gained = 2, streak = 3)),
        )
    }

    // M3: zero is the only signal a miss carries - mapping it to the success sample instead would
    // still compile and pass every other test here.
    @Test
    fun `OperationResolved with zero gained maps to the operation-miss sample`() {
        assertEquals(
            SoundSample.OperationMiss,
            soundFor(FeedbackEffect.OperationResolved(gained = 0, streak = 3)),
        )
    }

    @Test
    fun `TargetBrokeOut maps to the life-lost sample`() {
        assertEquals(SoundSample.LifeLost, soundFor(FeedbackEffect.TargetBrokeOut(livesLeft = 1)))
    }

    @Test
    fun `LevelUp maps to the level-up sample`() {
        assertEquals(SoundSample.LevelUp, soundFor(FeedbackEffect.LevelUp))
    }

    @Test
    fun `LifeGranted has no sample of its own`() {
        assertNull(soundFor(FeedbackEffect.LifeGranted(livesLeft = 3)))
    }

    @Test
    fun `GameOver has no sample of its own`() {
        assertNull(soundFor(FeedbackEffect.GameOver))
    }
}
