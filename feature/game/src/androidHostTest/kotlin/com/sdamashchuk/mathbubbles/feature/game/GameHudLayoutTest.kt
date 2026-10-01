package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private const val SCREEN_WIDTH_DP = 360f
private const val WRAPPER_HORIZONTAL_INSET_DP = 20f
private const val EXTREME_LEVEL = 999
private const val EXTREME_SCORE = 9999999

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameHudLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setHud(appliedMultiplier: Int) {
        composeTestRule.setContent {
            MathBubblesTheme {
                GameChrome(
                    level = EXTREME_LEVEL,
                    score = EXTREME_SCORE,
                    appliedMultiplier = appliedMultiplier,
                    onPauseClicked = {},
                ) {}
            }
        }
    }

    @Test
    fun `pause sits flush on the wrapper's right inset`() {
        setHud(appliedMultiplier = 1)

        val density = composeTestRule.density
        val right =
            with(density) {
                composeTestRule
                    .onNodeWithContentDescription("Pause")
                    .fetchSemanticsNode()
                    .boundsInRoot.right
                    .toDp()
                    .value
            }

        assertEquals(
            SCREEN_WIDTH_DP - WRAPPER_HORIZONTAL_INSET_DP,
            right,
            0.5f,
            "pause right edge vs the wrapper inset",
        )
    }

    @Test
    fun `level and score stay one line at 360dp with an extreme score and no combo`() {
        assertHudFitsOnOneLine(appliedMultiplier = 1, scoreText = "$EXTREME_SCORE")
    }

    @Test
    fun `level and score stay one line at 360dp with an extreme score and a combo prefix`() {
        assertHudFitsOnOneLine(appliedMultiplier = 9, scoreText = "X9 $EXTREME_SCORE")
    }

    private fun assertHudFitsOnOneLine(
        appliedMultiplier: Int,
        scoreText: String,
    ) {
        setHud(appliedMultiplier = appliedMultiplier)

        val levelNode = composeTestRule.onNodeWithText("LEVEL: $EXTREME_LEVEL").fetchSemanticsNode()
        val scoreNode = composeTestRule.onNodeWithText(scoreText).fetchSemanticsNode()
        val pauseBounds = composeTestRule.onNodeWithContentDescription("Pause").fetchSemanticsNode().boundsInRoot

        listOf(levelNode, scoreNode).forEach { node ->
            val layout = textLayoutResultOf(node)
            assertEquals(1, layout.lineCount, "expected a single HUD line, text layout reported ${layout.lineCount}")
            assertFalse(layout.hasVisualOverflow, "HUD text overflowed its bounds")
            assertFalse(
                node.boundsInRoot.overlaps(pauseBounds),
                "HUD text ${node.boundsInRoot} overlapped the pause button $pauseBounds",
            )
        }
    }
}

private fun textLayoutResultOf(node: SemanticsNode): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.first()
}
