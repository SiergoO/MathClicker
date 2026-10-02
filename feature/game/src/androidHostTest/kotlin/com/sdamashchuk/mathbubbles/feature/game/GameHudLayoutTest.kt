package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SCREEN_WIDTH_DP = 360f
private const val WRAPPER_HORIZONTAL_INSET_DP = 20f
private const val EXTREME_LEVEL = 999
private const val EXTREME_SCORE = 9999999
private const val LARGE_FONT_SCALE = 1.3f
private const val MAX_COMBO_MULTIPLIER = 5

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameHudLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setHud(
        appliedMultiplier: Int,
        fontScale: Float = 1f,
    ) {
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
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
        assertHudFitsOnOneLine(appliedMultiplier = 1)
    }

    @Test
    fun `level and score stay one line at 360dp with an extreme score and the combo bubble shown`() {
        assertHudFitsOnOneLine(appliedMultiplier = MAX_COMBO_MULTIPLIER)
    }

    @Test
    fun `level and score do not truncate at font scale 1point3 with an extreme score`() {
        assertHudFitsOnOneLine(appliedMultiplier = 1, fontScale = LARGE_FONT_SCALE)
    }

    @Test
    fun `the combo bubble never overlaps level or score text at extreme level, score and font scale`() {
        setHud(appliedMultiplier = MAX_COMBO_MULTIPLIER, fontScale = LARGE_FONT_SCALE)

        val comboBounds = composeTestRule.onNodeWithTag(GAME_HUD_COMBO_SLOT_TAG).fetchSemanticsNode().boundsInRoot
        val comboTextNode = composeTestRule.onNodeWithTag(GAME_HUD_COMBO_VALUE_TAG).fetchSemanticsNode()
        val levelLabelBounds = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_LABEL_TAG).fetchSemanticsNode().boundsInRoot
        val levelValueBounds = composeTestRule.onNodeWithTag(GAME_HUD_LEVEL_VALUE_TAG).fetchSemanticsNode().boundsInRoot
        val scoreValueBounds = composeTestRule.onNodeWithTag(GAME_HUD_SCORE_VALUE_TAG).fetchSemanticsNode().boundsInRoot

        assertFalse(comboBounds.overlaps(levelLabelBounds), "combo bubble overlapped the level label")
        assertFalse(comboBounds.overlaps(levelValueBounds), "combo bubble overlapped the level value")
        assertFalse(comboBounds.overlaps(scoreValueBounds), "combo bubble overlapped the score value")

        val comboTextLayout = textLayoutResultOf(comboTextNode)
        assertEquals(1, comboTextLayout.lineCount, "combo text wrapped instead of fitting on one line")
        val comboTextBounds = comboTextNode.boundsInRoot
        assertTrue(
            comboBounds.left <= comboTextBounds.left &&
                comboBounds.top <= comboTextBounds.top &&
                comboBounds.right >= comboTextBounds.right &&
                comboBounds.bottom >= comboTextBounds.bottom,
            "combo text $comboTextBounds did not fit inside the bubble $comboBounds",
        )

        listOf(GAME_HUD_LEVEL_LABEL_TAG, GAME_HUD_LEVEL_VALUE_TAG, GAME_HUD_SCORE_VALUE_TAG).forEach { tag ->
            val layout = textLayoutResultOf(composeTestRule.onNodeWithTag(tag).fetchSemanticsNode())
            assertEquals(1, layout.lineCount, "expected a single HUD line, text layout reported ${layout.lineCount}")
            assertFalse(layout.hasVisualOverflow, "HUD text overflowed its bounds")
        }
    }

    private fun assertHudFitsOnOneLine(
        appliedMultiplier: Int,
        fontScale: Float = 1f,
    ) {
        setHud(appliedMultiplier = appliedMultiplier, fontScale = fontScale)

        val pauseBounds = composeTestRule.onNodeWithContentDescription("Pause").fetchSemanticsNode().boundsInRoot
        val hudTextNodes =
            listOf(GAME_HUD_LEVEL_LABEL_TAG, GAME_HUD_LEVEL_VALUE_TAG, GAME_HUD_SCORE_VALUE_TAG).map { tag ->
                composeTestRule.onNodeWithTag(tag).fetchSemanticsNode()
            }

        hudTextNodes.forEach { node ->
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
