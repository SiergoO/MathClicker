package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameHudTitleLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `growing the level digit count does not move the level label`() {
        var level by mutableStateOf(1)
        composeTestRule.setContent {
            MathBubblesTheme {
                GameHudTitle(level = level, score = 9, appliedMultiplier = 1)
            }
        }

        val labelBefore = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_LABEL_TAG).fetchSemanticsNode().boundsInRoot

        composeTestRule.runOnIdle { level = 999 }
        composeTestRule.waitForIdle()

        val labelAfter = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_LABEL_TAG).fetchSemanticsNode().boundsInRoot

        assertEquals(labelBefore, labelAfter, "the level label moved when the level grew digits")
    }

    @Test
    fun `growing the score and level digit count does not move either slot`() {
        var level by mutableStateOf(1)
        var score by mutableStateOf(9)
        composeTestRule.setContent {
            MathBubblesTheme {
                GameHudTitle(level = level, score = score, appliedMultiplier = 1)
            }
        }

        val levelBefore = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        val scoreBefore = composeTestRule.onNodeWithTag(GAME_HUD_SCORE_SLOT_TAG).fetchSemanticsNode().boundsInRoot

        composeTestRule.runOnIdle {
            level = 999
            score = 9_999_999
        }
        composeTestRule.waitForIdle()

        val levelAfter = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        val scoreAfter = composeTestRule.onNodeWithTag(GAME_HUD_SCORE_SLOT_TAG).fetchSemanticsNode().boundsInRoot

        assertEquals(levelBefore, levelAfter, "level slot bounds moved when the digit counts grew")
        assertEquals(scoreBefore, scoreAfter, "score slot bounds moved when the digit counts grew")
    }

    @Test
    fun `the combo bubble appearing does not move the level or score slots`() {
        var appliedMultiplier by mutableStateOf(1)
        composeTestRule.setContent {
            MathBubblesTheme {
                GameHudTitle(level = 3, score = 90, appliedMultiplier = appliedMultiplier)
            }
        }

        val levelBefore = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        val scoreBefore = composeTestRule.onNodeWithTag(GAME_HUD_SCORE_SLOT_TAG).fetchSemanticsNode().boundsInRoot

        composeTestRule.runOnIdle { appliedMultiplier = 4 }
        composeTestRule.waitForIdle()

        val levelAfter = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        val scoreAfter = composeTestRule.onNodeWithTag(GAME_HUD_SCORE_SLOT_TAG).fetchSemanticsNode().boundsInRoot

        assertEquals(levelBefore, levelAfter, "level slot bounds moved when the combo bubble appeared")
        assertEquals(scoreBefore, scoreAfter, "score slot bounds moved when the combo bubble appeared")
    }
}
