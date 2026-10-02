package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.model.Field
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class MenuScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the default state`() {
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

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `renders the state with an unfinished session`() {
        val component =
            MenuComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                gameRepository = FakeGameRepository(unfinishedField = Field(id = 1, score = 40)),
                onPlayClicked = {},
                onSettingsClicked = {},
            )

        composeTestRule.setContent {
            MenuScreen(component = component)
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
