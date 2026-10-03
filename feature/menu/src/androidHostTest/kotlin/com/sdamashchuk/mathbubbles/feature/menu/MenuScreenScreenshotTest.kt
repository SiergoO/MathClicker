package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.logging.NoOpLogger
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val AMBIENT_CLOCK_MS = 7_000L

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
                logger = NoOpLogger,
                onPlayClicked = {},
                onSettingsClicked = {},
            )

        composeTestRule.setContent {
            MenuScreen(component = component, ambientTimeMsProvider = { AMBIENT_CLOCK_MS })
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `renders the state with an unfinished session`() {
        val component =
            MenuComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                gameRepository = FakeGameRepository(unfinishedField = Field(id = 1, score = 40)),
                logger = NoOpLogger,
                onPlayClicked = {},
                onSettingsClicked = {},
            )

        composeTestRule.setContent {
            MenuScreen(component = component, ambientTimeMsProvider = { AMBIENT_CLOCK_MS })
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
