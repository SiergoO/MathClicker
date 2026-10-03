package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val COUNTDOWN_SETTLE_MS = 6_000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenPauseTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fixture =
        GameScreenFixture(
            savedField = GameScreenFixture.openField(score = 55, level = 3),
            savedTargets = listOf(columnTarget(1, 0, 40), columnTarget(2, 3, 50)),
        )

    private fun playAndPause() {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)
    }

    @Test
    fun `the pause icon opens the pause dialog`() {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(32)
        composeTestRule.onNodeWithText("Resume").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Pause").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertEquals(GamePhase.Paused, fixture.state.phase)
        composeTestRule.onNodeWithText("Resume").assertExists()
        composeTestRule.onNodeWithText("Restart").assertExists()
        composeTestRule.onNodeWithText("Main Menu").assertExists()
    }

    @Test
    fun `the game clock runs while playing`() {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        val before = fixture.state.field.gameTimeMs

        composeTestRule.mainClock.advanceTimeBy(500)

        assertTrue(fixture.state.field.gameTimeMs >= before + 400)
    }

    @Test
    fun `pausing freezes the board clock and the bubbles`() {
        playAndPause()
        val frozenClock = fixture.state.field.gameTimeMs
        val frozenTargets = fixture.state.targetList

        composeTestRule.mainClock.advanceTimeBy(2_000)

        assertEquals(frozenClock, fixture.state.field.gameTimeMs)
        assertEquals(frozenTargets, fixture.state.targetList)
    }

    @Test
    fun `pausing saves the session so far`() {
        playAndPause()

        val saved = fixture.repository.fields.getValue(1)
        assertTrue(saved.gameTimeMs > 5_000)
        assertEquals(55, saved.score)
    }

    @Test
    fun `resume goes through the countdown and then the clock runs again`() {
        playAndPause()
        val frozenClock = fixture.state.field.gameTimeMs

        composeTestRule.onNodeWithText("Resume").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)
        assertEquals(GamePhase.CountingDown, fixture.state.phase)
        composeTestRule.onNodeWithText("Resume").assertDoesNotExist()
        assertEquals(frozenClock, fixture.state.field.gameTimeMs)

        composeTestRule.mainClock.advanceTimeBy(COUNTDOWN_SETTLE_MS)
        assertEquals(GamePhase.Playing, fixture.state.phase)
        val resumedAt = fixture.state.field.gameTimeMs
        composeTestRule.mainClock.advanceTimeBy(500)
        assertNotEquals(resumedAt, fixture.state.field.gameTimeMs)
    }

    @Test
    fun `restart from the pause dialog starts a fresh session on the ready screen`() {
        playAndPause()

        composeTestRule.onNodeWithText("Restart").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)

        assertEquals(GamePhase.ReadyToPlay, fixture.state.phase)
        composeTestRule.onNodeWithText("TOUCH SCREEN TO START").assertExists()
        assertEquals(2, fixture.state.field.id)
        assertEquals(1, fixture.state.field.level)
        assertEquals(0, fixture.state.field.score)
    }

    @Test
    fun `restart closes the abandoned run and opens a new one in storage`() {
        playAndPause()

        composeTestRule.onNodeWithText("Restart").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)

        assertTrue(
            fixture.repository.fields
                .getValue(1)
                .isClosed,
        )
        assertEquals(
            false,
            fixture.repository.fields
                .getValue(2)
                .isClosed,
        )
    }

    @Test
    fun `main menu from the pause dialog leaves the game screen once`() {
        playAndPause()
        assertEquals(0, fixture.menuExits)

        composeTestRule.onNodeWithText("Main Menu").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)

        assertEquals(1, fixture.menuExits)
    }
}
