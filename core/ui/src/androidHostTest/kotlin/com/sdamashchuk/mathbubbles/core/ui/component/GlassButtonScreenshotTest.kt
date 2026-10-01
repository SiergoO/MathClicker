package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.center
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.github.takahirom.roborazzi.captureRoboImage
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
class GlassButtonScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders a labelled glass button`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                Column {
                    GlassButton(text = "Play", onClick = {})
                }
            }
        }

        composeTestRule.onNodeWithText("Play").captureRoboImage()
    }

    @Test
    fun `renders a brighter highlight while pressed`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                Column {
                    GlassButton(text = "Play", onClick = {})
                }
            }
        }

        val button = composeTestRule.onNodeWithText("Play")
        button.performTouchInput { down(center) }

        button.captureRoboImage()
    }
}
