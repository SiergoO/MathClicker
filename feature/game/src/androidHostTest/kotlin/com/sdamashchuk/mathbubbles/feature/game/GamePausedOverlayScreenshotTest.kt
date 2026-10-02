package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GamePausedOverlayScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the pause dialog over a populated field`() {
        val pausedState = FIXED_FIELD_STATE.copy(phase = GamePhase.Paused)
        val gameState = mutableStateOf(pausedState)

        composeTestRule.setContent {
            MathBubblesTheme {
                GameChrome(
                    level = pausedState.field.level,
                    score = pausedState.field.score,
                    appliedMultiplier = pausedState.field.appliedMultiplier,
                    onPauseClicked = {},
                ) {
                    Field(
                        gameState = gameState,
                        running = false,
                        onTargetClicked = {},
                        onFireClicked = {},
                        onTick = {},
                        targetZeroedSignal = null,
                    )
                }
                GamePausedDialog(
                    onResumeClicked = {},
                    onRestartClicked = {},
                    onBackToMainMenuClicked = {},
                )
            }
        }

        captureScreenRoboImage()
    }
}
