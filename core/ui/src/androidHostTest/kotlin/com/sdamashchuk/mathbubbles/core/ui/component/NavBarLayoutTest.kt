package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOUCH_TARGET_DP = 48f
private const val NAV_BAR_HEIGHT_DP = 56f
private const val NAV_BAR_WIDTH_DP = 360f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class NavBarLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `back icon is a 48dp touch target and invokes its callback`() {
        var backClicked = false

        composeTestRule.setContent {
            MathBubblesTheme {
                NavBar(title = "Settings", onBack = { backClicked = true })
            }
        }

        val backButton = hasClickAction().and(hasAnyDescendant(hasContentDescription("Back")))
        val density = composeTestRule.density
        val bounds = composeTestRule.onNode(backButton, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

        assertEquals(TOUCH_TARGET_DP, with(density) { bounds.width.toDp().value }, 0.5f, "back touch width")
        assertEquals(TOUCH_TARGET_DP, with(density) { bounds.height.toDp().value }, 0.5f, "back touch height")

        composeTestRule.onNode(backButton, useUnmergedTree = true).performClick()
        assertTrue(backClicked, "back callback was invoked")
    }

    @Test
    fun `title sits on the same baseline on a bar with a back icon and one without`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                Column {
                    NavBar(title = "Settings", onBack = {})
                    NavBar(title = "Settings")
                }
            }
        }

        val density = composeTestRule.density
        val titleBaselines =
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().map { node ->
                firstBaselineInRoot(node, density)
            }

        assertEquals(
            titleBaselines[0],
            titleBaselines[1] - NAV_BAR_HEIGHT_DP,
            0.5f,
            "title baselines relative to each bar's own top: $titleBaselines",
        )
    }

    @Test
    fun `title text is centred across the bar's full width`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                NavBar(title = "Settings")
            }
        }

        val density = composeTestRule.density
        val bounds = composeTestRule.onNodeWithText("Settings").fetchSemanticsNode().boundsInRoot
        val left = with(density) { bounds.left.toDp().value }
        val right = with(density) { bounds.right.toDp().value }

        assertEquals(left, NAV_BAR_WIDTH_DP - right, 0.5f, "title should be centred, not start-aligned")
    }
}

private fun firstBaselineInRoot(
    node: SemanticsNode,
    density: Density,
): Float {
    val textLayoutResults = mutableListOf<TextLayoutResult>()
    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(textLayoutResults)
    val firstBaseline = textLayoutResults.first().firstBaseline
    return with(density) { (node.boundsInRoot.top + firstBaseline).toDp().value }
}
