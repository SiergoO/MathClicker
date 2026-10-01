package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertTrue

private const val SCREEN_HEIGHT_DP = 800f
private const val BOTTOM_THIRD_TOP_DP = SCREEN_HEIGHT_DP * 2f / 3f
private const val MIN_BOTTOM_MARGIN_DP = 24f

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MenuGlassButtonPositionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the Play button sits in the bottom third of the screen`() {
        val component =
            MenuComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                onPlayClicked = {},
                onSettingsClicked = {},
            )

        composeTestRule.setContent {
            MenuScreen(component = component)
        }

        val density = composeTestRule.density
        val playTop =
            with(density) {
                composeTestRule
                    .onNodeWithText("PLAY")
                    .fetchSemanticsNode()
                    .boundsInRoot.top
                    .toDp()
                    .value
            }

        // Measured at 496dp (the middle third) before the buttons moved lower.
        assertTrue(playTop > BOTTOM_THIRD_TOP_DP, "Play button top $playTop dp should be below $BOTTOM_THIRD_TOP_DP dp")
    }

    @Test
    fun `the Settings button keeps a margin above the screen's bottom edge`() {
        val component =
            MenuComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                onPlayClicked = {},
                onSettingsClicked = {},
            )

        composeTestRule.setContent {
            MenuScreen(component = component)
        }

        val density = composeTestRule.density
        val settingsBottom =
            with(density) {
                composeTestRule
                    .onNodeWithText("SETTINGS")
                    .fetchSemanticsNode()
                    .boundsInRoot.bottom
                    .toDp()
                    .value
            }
        val margin = SCREEN_HEIGHT_DP - settingsBottom

        assertTrue(
            margin >= MIN_BOTTOM_MARGIN_DP,
            "Settings bottom margin $margin dp should be at least $MIN_BOTTOM_MARGIN_DP dp",
        )
    }
}
