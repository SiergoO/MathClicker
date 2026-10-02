package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private const val SCREEN_HEIGHT_DP = 800f

private const val EXPECTED_BOTTOM_MARGIN_DP = 28f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ResultsGlassButtonPositionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the Main Menu button sits at the shared bottom-actions margin`() {
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
        val mainMenuBottom =
            with(density) {
                composeTestRule
                    .onNodeWithText("Main Menu")
                    .fetchSemanticsNode()
                    .boundsInRoot.bottom
                    .toDp()
                    .value
            }
        val margin = SCREEN_HEIGHT_DP - mainMenuBottom

        assertEquals(
            EXPECTED_BOTTOM_MARGIN_DP,
            margin,
            0.5f,
            "Main Menu bottom margin $margin dp should match the shared bottom-actions margin",
        )
    }
}
