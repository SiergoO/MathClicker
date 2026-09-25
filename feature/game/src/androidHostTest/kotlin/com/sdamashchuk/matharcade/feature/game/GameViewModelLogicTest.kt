package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.Target
import com.sdamashchuk.matharcade.feature.game.GameViewModel.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun target(
    id: Int = 1,
    value: Int = 10,
    appearsAtMs: Long = 0,
    isActive: Boolean = true,
) = Target(
    id = id,
    relatedFieldId = 1,
    columnId = 0,
    value = value,
    appearsAtMs = appearsAtMs,
    finishesAtMs = appearsAtMs + 1000,
    isActive = isActive,
)

class GameViewModelLogicTest {
    @Test
    fun `shouldRefreshTargets is false when the id set is unchanged`() {
        assertFalse(shouldRefreshTargets(setOf(1, 2, 3), setOf(1, 2, 3)))
    }

    @Test
    fun `shouldRefreshTargets is true when a level-up grows the id set`() {
        assertTrue(shouldRefreshTargets(setOf(1, 2, 3, 4, 5, 6), setOf(1, 2, 3, 4, 5, 6, 7, 8, 9)))
    }

    @Test
    fun `shouldRefreshTargets is true when a level-up shrinks the id set`() {
        assertTrue(shouldRefreshTargets(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), setOf(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun `shouldRefreshTargets is true for the very first restore, an empty previous set`() {
        assertTrue(shouldRefreshTargets(emptySet(), setOf(1, 2, 3)))
    }

    // MC-72: a target's schedule (appearsAtMs/finishesAtMs) is fixed once created - nothing on
    // Target moves on its own between ticks any more, only Field.gameTimeMs does - so there is no
    // longer a clock-only field for an unchanged board to exclude. shouldPersistTargets is now
    // plain equality end to end; these pin that directly rather than a since-removed exclusion.
    @Test
    fun `shouldPersistTargets is false when nothing about the board changed`() {
        val previous = listOf(target())
        val next = listOf(target())
        assertFalse(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when a target's schedule moves - shortenAppearanceDelay or a level-up`() {
        val previous = listOf(target(appearsAtMs = 5000))
        val next = listOf(target(appearsAtMs = 0))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when value changes`() {
        val previous = listOf(target(value = 10))
        val next = listOf(target(value = 9))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when isActive changes`() {
        val previous = listOf(target(isActive = true))
        val next = listOf(target(isActive = false))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldPersistTargets is true when the id set changes`() {
        val previous = listOf(target(id = 1))
        val next = listOf(target(id = 1), target(id = 2))
        assertTrue(shouldPersistTargets(previous, next))
    }

    @Test
    fun `shouldShowLevelIntro is true when the level rises on a live field`() {
        assertTrue(shouldShowLevelIntro(Field(id = 5, level = 3), Field(id = 5, level = 4)))
    }

    @Test
    fun `shouldShowLevelIntro is false for the same level`() {
        assertFalse(shouldShowLevelIntro(Field(id = 5, level = 3), Field(id = 5, level = 3)))
    }

    // M3: a level rollback must not announce anything - only a rise counts as a level-up.
    @Test
    fun `shouldShowLevelIntro is false when the level drops`() {
        assertFalse(shouldShowLevelIntro(Field(id = 5, level = 5), Field(id = 5, level = 3)))
    }

    // M1: restoring a level-7 session compares against the default Field(level = 1) the collector
    // starts with, and previousField.id == 0 is what marks that comparison as not a real level-up.
    @Test
    fun `shouldShowLevelIntro is false against the default previousField, even though the level rose`() {
        assertFalse(shouldShowLevelIntro(Field(), Field(id = 5, level = 7)))
    }

    @Test
    fun `nextPhase for ReadyToPlayButtonClicked leaves ReadyToPlay and Paused through CountingDown`() {
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.ReadyToPlay, Action.ReadyToPlayButtonClicked))
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.CountingDown, Action.ReadyToPlayButtonClicked))
        assertEquals(GamePhase.Playing, nextPhase(GamePhase.Playing, Action.ReadyToPlayButtonClicked))
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.Paused, Action.ReadyToPlayButtonClicked))
        assertEquals(GamePhase.LevelIntro, nextPhase(GamePhase.LevelIntro, Action.ReadyToPlayButtonClicked))
        assertEquals(GamePhase.GameOver, nextPhase(GamePhase.GameOver, Action.ReadyToPlayButtonClicked))
    }

    @Test
    fun `nextPhase for ShowCountDown only fires while Playing`() {
        assertEquals(GamePhase.ReadyToPlay, nextPhase(GamePhase.ReadyToPlay, Action.ShowCountDown))
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.CountingDown, Action.ShowCountDown))
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.Playing, Action.ShowCountDown))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Paused, Action.ShowCountDown))
        assertEquals(GamePhase.LevelIntro, nextPhase(GamePhase.LevelIntro, Action.ShowCountDown))
        assertEquals(GamePhase.GameOver, nextPhase(GamePhase.GameOver, Action.ShowCountDown))
    }

    @Test
    fun `nextPhase for StartGame only fires from CountingDown`() {
        assertEquals(GamePhase.ReadyToPlay, nextPhase(GamePhase.ReadyToPlay, Action.StartGame))
        assertEquals(GamePhase.Playing, nextPhase(GamePhase.CountingDown, Action.StartGame))
        assertEquals(GamePhase.Playing, nextPhase(GamePhase.Playing, Action.StartGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Paused, Action.StartGame))
        assertEquals(GamePhase.LevelIntro, nextPhase(GamePhase.LevelIntro, Action.StartGame))
        assertEquals(GamePhase.GameOver, nextPhase(GamePhase.GameOver, Action.StartGame))
    }

    @Test
    fun `nextPhase for LevelIntroFinished only fires from LevelIntro`() {
        assertEquals(GamePhase.ReadyToPlay, nextPhase(GamePhase.ReadyToPlay, Action.LevelIntroFinished))
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.CountingDown, Action.LevelIntroFinished))
        assertEquals(GamePhase.Playing, nextPhase(GamePhase.Playing, Action.LevelIntroFinished))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Paused, Action.LevelIntroFinished))
        assertEquals(GamePhase.Playing, nextPhase(GamePhase.LevelIntro, Action.LevelIntroFinished))
        assertEquals(GamePhase.GameOver, nextPhase(GamePhase.GameOver, Action.LevelIntroFinished))
    }

    // M1: pausing only ever fires from Playing. GameOver in particular must be refused - there is
    // nothing running to pause once the game has ended.
    @Test
    fun `nextPhase for PauseGame only fires from Playing, and is refused from GameOver`() {
        assertEquals(GamePhase.ReadyToPlay, nextPhase(GamePhase.ReadyToPlay, Action.PauseGame))
        assertEquals(GamePhase.CountingDown, nextPhase(GamePhase.CountingDown, Action.PauseGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Playing, Action.PauseGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Paused, Action.PauseGame))
        assertEquals(GamePhase.LevelIntro, nextPhase(GamePhase.LevelIntro, Action.PauseGame))
        assertEquals(GamePhase.GameOver, nextPhase(GamePhase.GameOver, Action.PauseGame))
    }

    @Test
    fun `nextPhase for RestartGame always lands on Paused`() {
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.ReadyToPlay, Action.RestartGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.CountingDown, Action.RestartGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Playing, Action.RestartGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Paused, Action.RestartGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.LevelIntro, Action.RestartGame))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.GameOver, Action.RestartGame))
    }

    @Test
    fun `nextPhase for BackToMainMenuClicked always lands on Paused`() {
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.ReadyToPlay, Action.BackToMainMenuClicked))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.CountingDown, Action.BackToMainMenuClicked))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Playing, Action.BackToMainMenuClicked))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.Paused, Action.BackToMainMenuClicked))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.LevelIntro, Action.BackToMainMenuClicked))
        assertEquals(GamePhase.Paused, nextPhase(GamePhase.GameOver, Action.BackToMainMenuClicked))
    }

    @Test
    fun `nextPhase for TargetClicked, FireButtonClicked, Tick and PersistTargetsNow never changes phase`() {
        val gameplayActions =
            listOf(Action.TargetClicked(1), Action.FireButtonClicked, Action.Tick(16), Action.PersistTargetsNow)
        val phases =
            listOf(
                GamePhase.ReadyToPlay,
                GamePhase.CountingDown,
                GamePhase.Playing,
                GamePhase.Paused,
                GamePhase.LevelIntro,
                GamePhase.GameOver,
            )
        phases.forEach { phase ->
            gameplayActions.forEach { action ->
                assertEquals(phase, nextPhase(phase, action))
            }
        }
    }

    // M2: resuming from Paused must never drop the player straight into falling targets.
    @Test
    fun `leaving Paused lands in CountingDown, never directly in Playing`() {
        val resumed = nextPhase(GamePhase.Paused, Action.ReadyToPlayButtonClicked)
        assertEquals(GamePhase.CountingDown, resumed)
        assertNotEquals(GamePhase.Playing, resumed)
    }

    // Once a session has started, ReadyToPlay must never come back - it is the boolean-conflation
    // bug (M5) this task exists to remove. Every action, from every phase but ReadyToPlay itself.
    @Test
    fun `ReadyToPlay is not reachable again once the session has started`() {
        val startedPhases =
            listOf(
                GamePhase.CountingDown,
                GamePhase.Playing,
                GamePhase.Paused,
                GamePhase.LevelIntro,
                GamePhase.GameOver,
            )
        val allActions =
            listOf(
                Action.ReadyToPlayButtonClicked,
                Action.ShowCountDown,
                Action.StartGame,
                Action.LevelIntroFinished,
                Action.PauseGame,
                Action.RestartGame,
                Action.BackToMainMenuClicked,
                Action.TargetClicked(1),
                Action.FireButtonClicked,
                Action.Tick(16),
                Action.PersistTargetsNow,
            )
        startedPhases.forEach { phase ->
            allActions.forEach { action ->
                assertNotEquals(GamePhase.ReadyToPlay, nextPhase(phase, action))
            }
        }
    }
}
