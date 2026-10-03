package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sdamashchuk.mathbubbles.core.model.Field
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val BREAKOUT_AT_MS = 5_100L
private const val PAST_BREAKOUT_MS = 400L
private const val INTRO_SETTLE_MS = 1_000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenEndTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun lastTargetFixture(
        lifeCount: Int,
        score: Int = 0,
        earlierFields: List<Field> = emptyList(),
    ) = GameScreenFixture(
        savedField = GameScreenFixture.openField(lifeCount = lifeCount, score = score, level = 4),
        savedTargets = listOf(columnTarget(1, 0, 40, finishesAtMs = BREAKOUT_AT_MS)),
        earlierFields = earlierFields,
    )

    private fun playUntilBreakout(fixture: GameScreenFixture) {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(PAST_BREAKOUT_MS)
    }

    private fun closedWrites(fixture: GameScreenFixture) = fixture.repository.fieldWrites.filter { it.isClosed }

    @Test
    fun `losing the last life ends the run and shows the results`() {
        val fixture = lastTargetFixture(lifeCount = 1, score = 77)

        playUntilBreakout(fixture)

        assertEquals(GamePhase.GameOver, fixture.state.phase)
        composeTestRule.onNodeWithContentDescription("Results").assertExists()
        composeTestRule.onNodeWithText("Main Menu").assertExists()
    }

    @Test
    fun `the results screen is shown exactly once however long it stays up`() {
        val fixture = lastTargetFixture(lifeCount = 1, score = 77)

        playUntilBreakout(fixture)
        composeTestRule.mainClock.advanceTimeBy(5_000)

        composeTestRule.onAllNodesWithText("Game Over").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Main Menu").assertCountEquals(1)
        assertEquals(1, fixture.repository.recentReads)
        assertEquals(0, fixture.menuExits)
    }

    @Test
    fun `game over saves the finished run exactly once`() {
        val fixture = lastTargetFixture(lifeCount = 1, score = 77)

        playUntilBreakout(fixture)
        composeTestRule.mainClock.advanceTimeBy(5_000)

        val closed = closedWrites(fixture)
        assertEquals(1, closed.size)
        assertEquals(77, closed.single().score)
        assertEquals(4, closed.single().level)
        assertEquals(GameScreenFixture.FINISHED_AT_MS, closed.single().finishedAt)
        assertTrue(
            fixture.repository.fields
                .getValue(1)
                .isClosed,
        )
    }

    @Test
    fun `the finished run shows its score in the summary and the results table`() {
        val earlier = Field(id = 9, level = 2, score = 31, isClosed = true, finishedAt = 1_000L)
        val fixture = lastTargetFixture(lifeCount = 1, score = 77, earlierFields = listOf(earlier))

        playUntilBreakout(fixture)

        composeTestRule.onAllNodesWithText("77").assertCountEquals(2)
        composeTestRule.onNodeWithText("31").assertExists()
        composeTestRule.onNodeWithText("New Record!").assertExists()
    }

    @Test
    fun `restart from the results screen opens a new session on the ready screen`() {
        val fixture = lastTargetFixture(lifeCount = 1, score = 77)
        playUntilBreakout(fixture)

        composeTestRule.onNodeWithText("Restart").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)

        assertEquals(GamePhase.ReadyToPlay, fixture.state.phase)
        assertEquals(0, fixture.state.field.score)
        composeTestRule.onNodeWithText("TOUCH SCREEN TO START").assertExists()
    }

    @Test
    fun `main menu from the results screen leaves once`() {
        val fixture = lastTargetFixture(lifeCount = 1, score = 77)
        playUntilBreakout(fixture)

        composeTestRule.onNodeWithText("Main Menu").performClick()
        composeTestRule.mainClock.advanceTimeBy(100)

        assertEquals(1, fixture.menuExits)
    }

    @Test
    fun `clearing the board with a life to spare shows the next level announcement`() {
        val fixture = lastTargetFixture(lifeCount = 3)

        playUntilBreakout(fixture)

        assertEquals(GamePhase.LevelIntro, fixture.state.phase)
        assertEquals(5, fixture.state.field.level)
        composeTestRule.onNodeWithText("LEVEL 5").assertExists()
        composeTestRule.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun `the board stays frozen while the level announcement is up`() {
        val fixture = lastTargetFixture(lifeCount = 3)
        playUntilBreakout(fixture)
        val clock = fixture.state.field.gameTimeMs

        composeTestRule.mainClock.advanceTimeBy(100)

        assertEquals(GamePhase.LevelIntro, fixture.state.phase)
        assertEquals(clock, fixture.state.field.gameTimeMs)
    }

    @Test
    fun `the announcement finishes by itself and play continues on the new level`() {
        val fixture = lastTargetFixture(lifeCount = 3)
        playUntilBreakout(fixture)

        composeTestRule.mainClock.advanceTimeBy(INTRO_SETTLE_MS)

        assertEquals(GamePhase.Playing, fixture.state.phase)
        composeTestRule.onAllNodesWithText("LEVEL 5").assertCountEquals(0)
        composeTestRule.onNodeWithContentDescription("Pause").assertExists()
        val clock = fixture.state.field.gameTimeMs
        composeTestRule.mainClock.advanceTimeBy(300)
        assertTrue(fixture.state.field.gameTimeMs > clock)
        assertNotNull(fixture.state.targetList.firstOrNull { it.isActive })
    }
}
