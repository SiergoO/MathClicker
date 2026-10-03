package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.ui.component.model.AmbientBubbleStyle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val CLOCK_MS = 7_000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class AmbientBubblesScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders rising bubbles at a fixed clock`() {
        val style =
            AmbientBubbleStyle(
                count = 16,
                slowestRiseMs = 22_000f,
                fastestRiseMs = 11_000f,
                smallestRadiusFraction = 0.008f,
                largestRadiusFraction = 0.026f,
                minAlpha = 0.06f,
                maxAlpha = 0.20f,
            )

        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF234C61))) {
                AmbientBubbles(style = style, timeMsProvider = { CLOCK_MS }, modifier = Modifier.fillMaxSize())
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }
}
