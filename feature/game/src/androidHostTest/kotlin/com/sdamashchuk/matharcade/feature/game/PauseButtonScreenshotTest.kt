package com.sdamashchuk.matharcade.feature.game

import androidx.compose.foundation.layout.Column
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
class PauseButtonScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the pause glyph`() {
        composeTestRule.setContent {
            MathArcadeTheme {
                // The theme's fillMaxSize Surface would stretch an unwrapped 48dp button to the whole screen.
                Column {
                    PauseButton(onClick = {})
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
