package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
class MenuButtonScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders a labelled button`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                // MenuButton always sits in a Column in production; a Surface's fillMaxSize gives
                // tight height constraints that stretch an unwrapped MenuButton to fill the screen.
                Column {
                    MenuButton(text = "Restart", onClick = {})
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
