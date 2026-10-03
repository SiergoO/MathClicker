package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackActionForTest {
    @Test
    fun `Back pauses a running game`() {
        assertEquals(GameViewModel.Action.PauseGame, backActionFor(GamePhase.Playing))
    }

    @Test
    fun `Back resumes a paused game through the dialog's own action`() {
        assertEquals(GameViewModel.Action.ReadyToPlayButtonClicked, backActionFor(GamePhase.Paused))
    }

    @Test
    fun `Back leaves to the main menu from every phase that is not playing or paused`() {
        listOf(GamePhase.ReadyToPlay, GamePhase.CountingDown, GamePhase.LevelIntro, GamePhase.GameOver).forEach {
            assertEquals(it.name, GameViewModel.Action.BackToMainMenuClicked, backActionFor(it))
        }
    }

    @Test
    fun `no phase maps Back to an action that leaves the phase unchanged`() {
        GamePhase.entries.forEach {
            assertNotEquals(it.name, it, nextPhase(it, backActionFor(it)))
        }
    }
}
