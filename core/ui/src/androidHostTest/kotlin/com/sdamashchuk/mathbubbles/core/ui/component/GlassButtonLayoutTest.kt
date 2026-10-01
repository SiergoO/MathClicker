package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

private const val MIN_TOUCH_TARGET_DP = 48f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GlassButtonLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `is at least a 48dp touch target and invokes its callback`() {
        var clicked = false

        composeTestRule.setContent {
            MathBubblesTheme {
                Column {
                    GlassButton(text = "Play", onClick = { clicked = true })
                }
            }
        }

        val density = composeTestRule.density
        val bounds = composeTestRule.onNodeWithText("Play").fetchSemanticsNode().boundsInRoot
        val heightDp = with(density) { bounds.height.toDp().value }

        assertTrue(
            heightDp >= MIN_TOUCH_TARGET_DP,
            "touch height $heightDp dp should be at least $MIN_TOUCH_TARGET_DP dp",
        )

        composeTestRule.onNodeWithText("Play").performClick()
        assertTrue(clicked, "click callback was invoked")
    }
}
