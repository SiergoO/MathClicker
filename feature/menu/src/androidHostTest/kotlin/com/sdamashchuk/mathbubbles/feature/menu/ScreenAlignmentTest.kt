package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ScreenAlignmentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `settings back glyph's left edge lines up with the sound label's left edge`() {
        val component =
            SettingsComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                soundSettings = FakeSoundSettings(soundEnabled = true),
                onBackClicked = {},
            )

        composeTestRule.setContent {
            SettingsScreen(component = component)
        }

        val backLeft =
            composeTestRule
                .onNodeWithContentDescription("Back", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.left
        val soundLeft =
            composeTestRule
                .onNodeWithText("Sound")
                .fetchSemanticsNode()
                .boundsInRoot.left

        val density = composeTestRule.density
        assertEquals(
            with(density) { soundLeft.toDp().value },
            with(density) { backLeft.toDp().value },
            0.5f,
            "back glyph left edge vs Sound label left edge",
        )
    }

    @Test
    fun `menu help glyph's right edge lines up with the menu buttons' right edge`() {
        val component =
            MenuComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                gameRepository = FakeGameRepository(),
                onPlayClicked = {},
                onSettingsClicked = {},
            )

        composeTestRule.setContent {
            MenuScreen(component = component)
        }

        val helpRight =
            composeTestRule
                .onNodeWithContentDescription("How to play?", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.right
        val playButtonRight =
            composeTestRule
                .onNodeWithText("PLAY")
                .fetchSemanticsNode()
                .boundsInRoot.right

        val density = composeTestRule.density
        assertEquals(
            with(density) { playButtonRight.toDp().value },
            with(density) { helpRight.toDp().value },
            0.5f,
            "help glyph right edge vs PLAY button right edge",
        )
    }
}
