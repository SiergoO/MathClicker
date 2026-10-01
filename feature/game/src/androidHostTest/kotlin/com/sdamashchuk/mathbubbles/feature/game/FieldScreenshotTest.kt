package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val FIELD_ID = 1
private const val GAME_TIME_MS = 5_000L
private const val ANIMATION_SETTLE_MS = 300L

private val FIXED_FIELD_STATE =
    GameViewModel.State(
        field =
            Field(
                id = FIELD_ID,
                level = 3,
                score = 240,
                lifeCount = 2,
                bonusMultiplier = 3,
                currentOperationSign = OperationSign.SUBTRACTION,
                currentOperationDigit = 5,
                nextOperationSign = OperationSign.DIVISION,
                nextOperationDigit = 2,
                gameTimeMs = GAME_TIME_MS,
            ),
        phase = GamePhase.Playing,
        targetList =
            persistentListOf(
                Target(
                    id = 1,
                    relatedFieldId = FIELD_ID,
                    columnId = 0,
                    value = 10,
                    appearsAtMs = 0,
                    finishesAtMs = 10_000,
                ),
                Target(
                    id = 2,
                    relatedFieldId = FIELD_ID,
                    columnId = 1,
                    value = 3,
                    appearsAtMs = 0,
                    finishesAtMs = 12_000,
                ),
                Target(
                    id = 3,
                    relatedFieldId = FIELD_ID,
                    columnId = 2,
                    value = 20,
                    appearsAtMs = 1_000,
                    finishesAtMs = 8_000,
                ),
                Target(
                    id = 4,
                    relatedFieldId = FIELD_ID,
                    columnId = 3,
                    value = 8,
                    appearsAtMs = 2_000,
                    finishesAtMs = 6_000,
                    isProfitable = false,
                ),
            ),
    )

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class FieldScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the playing field mid-play`() {
        composeTestRule.mainClock.autoAdvance = false
        val gameState = mutableStateOf(FIXED_FIELD_STATE)

        composeTestRule.setContent {
            MathBubblesTheme {
                // Field lays its HUD, play area and dock out as successive Column children; GameScreen
                // always composes it inside one, so this mirrors that rather than Surface's own Box.
                Column(modifier = Modifier.fillMaxSize()) {
                    Field(
                        gameState = gameState,
                        onTargetClicked = {},
                        onFireClicked = {},
                        onTick = {},
                        onPauseClicked = {},
                        targetZeroedSignal = null,
                    )
                }
            }
        }
        composeTestRule.mainClock.advanceTimeBy(ANIMATION_SETTLE_MS)

        composeTestRule.onRoot().captureRoboImage()
    }
}
