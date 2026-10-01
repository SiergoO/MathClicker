package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val GOLDEN_TAG = "NavBarGolden"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class NavBarScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders a back icon a centred title and a trailing action`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().testTag(GOLDEN_TAG)) {
                    NavBar(
                        title = "Settings",
                        onBack = {},
                        actions = {
                            IconButton(onClick = {}) {
                                Text(text = "?")
                            }
                        },
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag(GOLDEN_TAG).captureRoboImage()
    }
}
