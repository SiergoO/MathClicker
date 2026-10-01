package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.ui.component.NavBar
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val GOLDEN_TAG = "PauseButtonGolden"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class PauseButtonScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the pause glyph flush to a NavBar's trailing edge`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                // PauseButton always sits in a NavBar's actions slot in production.
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().testTag(GOLDEN_TAG)) {
                    NavBar(
                        actions = { PauseButton(onClick = {}) },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(GOLDEN_TAG).captureRoboImage()
    }
}
