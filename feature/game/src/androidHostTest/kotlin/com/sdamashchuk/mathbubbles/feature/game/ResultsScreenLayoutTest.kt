package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SCREEN_WIDTH_DP = 360f
private const val WRAPPER_HORIZONTAL_INSET_DP = 20f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ResultsScreenLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `results content sits flush on the wrapper's horizontal inset`() {
        val field = Field(id = 1, level = 1, score = 60, isClosed = true)

        composeTestRule.setContent {
            MathBubblesTheme {
                ResultsScreen(
                    field = field,
                    recentResults = persistentListOf(field),
                    bestResult = null,
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        val density = composeTestRule.density
        val bounds =
            composeTestRule
                .onNodeWithContentDescription("Results")
                .fetchSemanticsNode()
                .boundsInRoot

        val left = with(density) { bounds.left.toDp().value }
        val right = with(density) { bounds.right.toDp().value }

        assertEquals(WRAPPER_HORIZONTAL_INSET_DP, left, 0.5f, "results content left edge vs the wrapper inset")
        assertEquals(
            SCREEN_WIDTH_DP - WRAPPER_HORIZONTAL_INSET_DP,
            right,
            0.5f,
            "results content right edge vs the wrapper inset",
        )
    }

    @Test
    fun `the results table sits on the same inset as the content around it`() {
        val field = Field(id = 1, level = 1, score = 60, isClosed = true)

        composeTestRule.setContent {
            MathBubblesTheme {
                ResultsScreen(
                    field = field,
                    recentResults = persistentListOf(field),
                    bestResult = null,
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        val density = composeTestRule.density
        val tableLeft =
            with(density) {
                composeTestRule
                    .onNodeWithText("DATE")
                    .fetchSemanticsNode()
                    .boundsInRoot.left
                    .toDp()
                    .value
            }
        val tableRight =
            with(
                density,
            ) {
                composeTestRule
                    .onNodeWithText("SCORE")
                    .fetchSemanticsNode()
                    .boundsInRoot.right
                    .toDp()
                    .value
            }

        assertEquals(WRAPPER_HORIZONTAL_INSET_DP, tableLeft, 0.5f, "results table left edge vs the wrapper inset")
        assertEquals(
            SCREEN_WIDTH_DP - WRAPPER_HORIZONTAL_INSET_DP,
            tableRight,
            0.5f,
            "results table right edge vs the wrapper inset",
        )
    }

    @Test
    fun `seven rows of results fit at 360x800 and font scale 1 without scrolling`() {
        val field = Field(id = 1, level = 1, score = 60, isClosed = true)
        val recentResults = (1..7).map { field.copy(id = it, level = it, score = it * 10) }.toPersistentList()

        composeTestRule.setContent {
            MathBubblesTheme {
                ResultsScreen(
                    field = field,
                    recentResults = recentResults,
                    bestResult = null,
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        val scrollRange =
            composeTestRule
                .onNodeWithTag(RESULTS_TABLE_SCROLL_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.VerticalScrollAxisRange)

        assertTrue(scrollRange == null || scrollRange.maxValue() <= 0f, "table area should not need to scroll")
    }
}
