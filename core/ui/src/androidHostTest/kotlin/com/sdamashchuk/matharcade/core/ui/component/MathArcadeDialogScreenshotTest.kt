package com.sdamashchuk.matharcade.core.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.sdamashchuk.matharcade.core.ui.theme.MathArcadeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MathArcadeDialogScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the header the body and both buttons`() {
        composeTestRule.setContent {
            MathArcadeTheme {
                MathArcadeDialog(
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
}
