package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val ANIMATION_SETTLE_MS = 300L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameChromeScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the playing field through the ScreenWrapper NavBar chrome with the combo bubble shown`() {
        composeTestRule.mainClock.autoAdvance = false
        val gameState = mutableStateOf(FIXED_FIELD_STATE)

        composeTestRule.setContent {
            MathBubblesTheme {
                GameChrome(
                    level = gameState.value.field.level,
                    score = gameState.value.field.score,
                    appliedMultiplier = gameState.value.field.appliedMultiplier,
                    onPauseClicked = {},
                ) {
                    Field(
                        gameState = gameState,
                        onTargetClicked = {},
                        onFireClicked = {},
                        onTick = {},
                        targetZeroedSignal = null,
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `renders the playing field through the ScreenWrapper NavBar chrome with no combo`() {
        composeTestRule.mainClock.autoAdvance = false
        val noComboState = FIXED_FIELD_STATE.copy(field = FIXED_FIELD_STATE.field.copy(bonusMultiplier = 0))
        val gameState = mutableStateOf(noComboState)

        composeTestRule.setContent {
            MathBubblesTheme {
                GameChrome(
                    level = gameState.value.field.level,
                    score = gameState.value.field.score,
                    appliedMultiplier = gameState.value.field.appliedMultiplier,
                    onPauseClicked = {},
                ) {
                    Field(
                        gameState = gameState,
                        onTargetClicked = {},
                        onFireClicked = {},
                        onTick = {},
                        targetZeroedSignal = null,
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun `renders the playing field through the ScreenWrapper NavBar chrome at extreme level, score and combo`() {
        composeTestRule.mainClock.autoAdvance = false
        val extremeState =
            FIXED_FIELD_STATE.copy(
                field = FIXED_FIELD_STATE.field.copy(level = 999, score = 9999999, bonusMultiplier = 4),
            )
        val gameState = mutableStateOf(extremeState)

        composeTestRule.setContent {
            MathBubblesTheme {
                GameChrome(
                    level = gameState.value.field.level,
                    score = gameState.value.field.score,
                    appliedMultiplier = gameState.value.field.appliedMultiplier,
                    onPauseClicked = {},
                ) {
                    Field(
                        gameState = gameState,
                        onTargetClicked = {},
                        onFireClicked = {},
                        onTick = {},
                        targetZeroedSignal = null,
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)

        composeTestRule.onRoot().captureRoboImage()
    }
}
