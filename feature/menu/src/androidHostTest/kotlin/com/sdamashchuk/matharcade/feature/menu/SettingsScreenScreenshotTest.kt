package com.sdamashchuk.matharcade.feature.menu

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class SettingsScreenScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders with sound on`() {
        val component =
            SettingsComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                soundSettings = FakeSoundSettings(soundEnabled = true),
                onBackClicked = {},
            )

        composeTestRule.setContent {
            SettingsScreen(component = component)
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `renders with sound off`() {
        val component =
            SettingsComponent(
                componentContext = DefaultComponentContext(lifecycle = LifecycleRegistry()),
                soundSettings = FakeSoundSettings(soundEnabled = false),
                onBackClicked = {},
            )

        composeTestRule.setContent {
            SettingsScreen(component = component)
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
