package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val MID_FADE_MS = 60L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameHudComboTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the bubble keeps showing the last raised value while it fades out on reset`() {
        composeTestRule.mainClock.autoAdvance = false
        var appliedMultiplier by mutableStateOf(3)
        composeTestRule.setContent {
            MathBubblesTheme {
                GameHudCombo(appliedMultiplier = appliedMultiplier)
            }
        }
        composeTestRule.mainClock.advanceTimeBy(MID_FADE_MS)

        composeTestRule.runOnIdle { appliedMultiplier = 1 }
        composeTestRule.mainClock.advanceTimeBy(MID_FADE_MS)

        composeTestRule.onNodeWithTag(GAME_HUD_COMBO_VALUE_TAG).assertTextEquals("x3")
    }
}
