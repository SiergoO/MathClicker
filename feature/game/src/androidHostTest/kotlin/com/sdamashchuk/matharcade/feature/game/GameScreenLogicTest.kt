package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

class GameScreenLogicTest {
    // M3: Field's LaunchedEffect is the engine clock. If this widens to include Paused, pausing
    // stops being structural and the falling targets keep moving behind the pause dialog.
    @Test
    fun `shouldComposeField is true for Playing alone`() {
        val composesField = GamePhase.entries.associateWith(::shouldComposeField)
        assertEquals(
            mapOf(
                GamePhase.ReadyToPlay to false,
                GamePhase.CountingDown to false,
                GamePhase.Playing to true,
                GamePhase.Paused to false,
                GamePhase.LevelIntro to false,
                GamePhase.GameOver to false,
            ),
            composesField,
        )
    }
}
