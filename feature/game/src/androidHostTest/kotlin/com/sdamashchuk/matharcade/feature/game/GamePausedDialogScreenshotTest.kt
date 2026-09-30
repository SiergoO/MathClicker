package com.sdamashchuk.matharcade.feature.game

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.matharcade.core.ui.theme.MathArcadeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GamePausedDialogScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the paused state`() {
        composeTestRule.setContent {
            MathArcadeTheme {
                GamePausedDialog(
                    onResumeClicked = {},
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
