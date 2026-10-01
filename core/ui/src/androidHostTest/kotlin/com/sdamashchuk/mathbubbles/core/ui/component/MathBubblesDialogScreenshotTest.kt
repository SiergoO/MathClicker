package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val LONG_BODY_REPEATS = 40

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MathBubblesDialogScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the header the body and both buttons`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                MathBubblesDialog(
                    headerText = "Reset progress?",
                    bodyText = "This clears every saved run and cannot be undone.",
                    onDismiss = {},
                    positiveButtonText = "Reset",
                    onPositive = {},
                    negativeButtonText = "Cancel",
                )
            }
        }

        captureScreenRoboImage()
    }

    @Test
    fun `keeps the button on screen when the body overflows`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                MathBubblesDialog(
                    headerText = "How to play",
                    bodyText = "Tap a bubble to apply the operation to its number. ".repeat(LONG_BODY_REPEATS),
                    onDismiss = {},
                    positiveButtonText = "Got it",
                    onPositive = {},
                )
            }
        }

        captureScreenRoboImage()
    }
}
