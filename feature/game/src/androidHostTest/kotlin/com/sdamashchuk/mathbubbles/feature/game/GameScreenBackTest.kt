package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private const val BREAKOUT_AT_MS = 5_100L
private const val PAST_BREAKOUT_MS = 400L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenBackTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun pressBack() {
        composeTestRule.runOnUiThread { composeTestRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeTestRule.mainClock.advanceTimeBy(100)
    }

    private fun lastTargetFixture(lifeCount: Int) =
        GameScreenFixture(
            savedField = GameScreenFixture.openField(lifeCount = lifeCount),
            savedTargets = listOf(columnTarget(1, 0, 40, finishesAtMs = BREAKOUT_AT_MS)),
        )

    @Test
    fun `back on the ready screen leaves for the menu`() {
        val fixture = GameScreenFixture()
        fixture.show(composeTestRule)
        assertEquals(GamePhase.ReadyToPlay, fixture.state.phase)

        pressBack()

        assertEquals(1, fixture.menuExits)
    }

    @Test
    fun `back during the countdown leaves for the menu`() {
        val fixture = GameScreenFixture()
        fixture.show(composeTestRule)
        composeTestRule.onNodeWithText("TOUCH SCREEN TO START").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)
        assertEquals(GamePhase.CountingDown, fixture.state.phase)

        pressBack()

        assertEquals(1, fixture.menuExits)
    }

    @Test
    fun `back while playing pauses instead of leaving`() {
        val fixture = lastTargetFixture(lifeCount = 3)
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(32)

        pressBack()

        assertEquals(GamePhase.Paused, fixture.state.phase)
        assertEquals(0, fixture.menuExits)
        composeTestRule.onNodeWithText("Resume").assertExists()
    }

    @Test
    fun `back on the pause dialog resumes through the countdown instead of leaving`() {
        val fixture = lastTargetFixture(lifeCount = 3)
        fixture.show(composeTestRule)
        assertEquals(GamePhase.Paused, fixture.state.phase)

        pressBack()

        assertEquals(GamePhase.CountingDown, fixture.state.phase)
        assertEquals(0, fixture.menuExits)
    }

    @Test
    fun `back during the level announcement leaves for the menu`() {
        val fixture = lastTargetFixture(lifeCount = 3)
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(PAST_BREAKOUT_MS)
        assertEquals(GamePhase.LevelIntro, fixture.state.phase)

        pressBack()

        assertEquals(1, fixture.menuExits)
    }

    @Test
    fun `back on the results screen leaves for the menu`() {
        val fixture = lastTargetFixture(lifeCount = 1)
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(PAST_BREAKOUT_MS)
        assertEquals(GamePhase.GameOver, fixture.state.phase)

        pressBack()

        assertEquals(1, fixture.menuExits)
    }
}
