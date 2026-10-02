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
private const val SCRIM_INSET_DP = 16f
private const val CARD_PADDING_DP = 16f
private const val CARD_INSET_DP = SCRIM_INSET_DP + CARD_PADDING_DP

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GamePausedDialogLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `resume and restart sit on the card's own 16dp padding`() {
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

        assertEquals(CARD_INSET_DP, resumeLeft, 0.5f, "resume button left edge vs the scrim + card padding")
        assertEquals(
            SCREEN_WIDTH_DP - CARD_INSET_DP,
            restartRight,
            0.5f,
            "restart button right edge vs the scrim + card padding",
        )
    }
}
