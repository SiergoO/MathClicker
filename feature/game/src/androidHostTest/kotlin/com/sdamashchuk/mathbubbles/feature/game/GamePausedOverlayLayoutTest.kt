package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GamePausedOverlayLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the frozen chrome stays in the tree behind the pause dialog`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                Box {
                    GameChrome(level = 3, score = 90, appliedMultiplier = 1, onPauseClicked = {}) {}
                    GamePausedDialog(
                        onResumeClicked = {},
                        onRestartClicked = {},
                        onBackToMainMenuClicked = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_SLOT_TAG).assertExists()
        composeTestRule.onNodeWithText("Resume").assertExists()
    }
}
