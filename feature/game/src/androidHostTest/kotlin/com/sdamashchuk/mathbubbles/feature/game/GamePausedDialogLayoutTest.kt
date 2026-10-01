package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private const val SCREEN_WIDTH_DP = 360f
private const val WRAPPER_HORIZONTAL_INSET_DP = 20f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GamePausedDialogLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `resume and restart sit on the wrapper's horizontal inset`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                GamePausedDialog(
                    onResumeClicked = {},
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        val density = composeTestRule.density
        val resumeLeft =
            with(
                density,
            ) {
                composeTestRule
                    .onNodeWithText("Resume")
                    .fetchSemanticsNode()
                    .boundsInRoot.left
                    .toDp()
                    .value
            }
        val restartRight =
            with(
                density,
            ) {
                composeTestRule
                    .onNodeWithText("Restart")
                    .fetchSemanticsNode()
                    .boundsInRoot.right
                    .toDp()
                    .value
            }

        assertEquals(WRAPPER_HORIZONTAL_INSET_DP, resumeLeft, 0.5f, "resume button left edge vs the wrapper inset")
        assertEquals(
            SCREEN_WIDTH_DP - WRAPPER_HORIZONTAL_INSET_DP,
            restartRight,
            0.5f,
            "restart button right edge vs the wrapper inset",
        )
    }
}
