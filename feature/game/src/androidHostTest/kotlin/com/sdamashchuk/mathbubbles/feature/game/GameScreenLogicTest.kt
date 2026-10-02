package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameScreenLogicTest {
    @Test
    fun `Field stays composed while Paused so the frozen game shows behind the dialog`() {
        val composesField = GamePhase.entries.associateWith(::shouldComposeField)
        assertEquals(
            mapOf(
                GamePhase.ReadyToPlay to false,
                GamePhase.CountingDown to false,
                GamePhase.Playing to true,
                GamePhase.Paused to true,
                GamePhase.LevelIntro to false,
                GamePhase.GameOver to false,
            ),
            composesField,
        )
    }

    @Test
    fun `Field only runs its frame loop for Playing, so pausing stops the engine`() {
        val runsField = GamePhase.entries.associateWith(::shouldRunField)
        assertEquals(
            mapOf(
                GamePhase.ReadyToPlay to false,
                GamePhase.CountingDown to false,
                GamePhase.Playing to true,
                GamePhase.Paused to false,
                GamePhase.LevelIntro to false,
                GamePhase.GameOver to false,
            ),
            runsField,
        )
    }
}
