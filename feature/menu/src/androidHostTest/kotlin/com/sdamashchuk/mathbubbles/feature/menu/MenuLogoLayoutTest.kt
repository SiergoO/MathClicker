package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot

private const val LARGE_FONT_SCALE = 1.3f
private const val SCREEN_WIDTH_DP = 360f
private const val TOLERANCE_DP = 1f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MenuLogoLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun showLogo(fontScale: Float) {
        val density =
            RuntimeEnvironment
                .getApplication()
                .resources.displayMetrics.density
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MathBubblesTheme {
                    Box(Modifier.fillMaxSize()) {
                        MenuLogo(timeMsProvider = { 0L }, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    private fun assertTextFitsInsideCircle() {
        val circle = composeTestRule.onNodeWithContentDescription("Math Bubbles").fetchSemanticsNode().boundsInRoot
        val textNode = composeTestRule.onNodeWithText("MATH\nBUBBLES", useUnmergedTree = true).fetchSemanticsNode()
        val text = textNode.boundsInRoot
        val radius = circle.width / 2f
        corners(text).forEach { corner ->
            val distance = hypot(corner.first - circle.center.x, corner.second - circle.center.y)
            assertTrue("text corner $corner lies outside the circle", distance <= radius)
        }
        val results = mutableListOf<TextLayoutResult>()
        textNode.config
            .getOrNull(SemanticsActions.GetTextLayoutResult)
            ?.action
            ?.invoke(results)
        val layout = results.single()
        assertEquals(2, layout.lineCount)
        assertFalse(layout.didOverflowWidth)
        assertFalse(layout.didOverflowHeight)
    }

    private fun corners(rect: Rect) =
        listOf(
            rect.left to rect.top,
            rect.right to rect.top,
            rect.left to rect.bottom,
            rect.right to rect.bottom,
        )

    @Test
    fun `the app name fits inside the bubble at the default font scale`() {
        showLogo(fontScale = 1f)
        assertTextFitsInsideCircle()
    }

    @Test
    fun `the app name fits inside the bubble at font scale 1_3`() {
        showLogo(fontScale = LARGE_FONT_SCALE)
        assertTextFitsInsideCircle()
    }

    @Test
    fun `the bubble is a circle sized from the screen width and centred`() {
        showLogo(fontScale = 1f)
        val circle = composeTestRule.onNodeWithContentDescription("Math Bubbles").fetchSemanticsNode().boundsInRoot
        val density = composeTestRule.density.density
        assertEquals(SCREEN_WIDTH_DP * LOGO_DIAMETER_SCREEN_FRACTION, circle.width / density, TOLERANCE_DP)
        assertEquals(circle.width, circle.height, 1f)
        assertEquals(SCREEN_WIDTH_DP / 2f, circle.center.x / density, TOLERANCE_DP)
    }
}
