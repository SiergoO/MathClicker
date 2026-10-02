package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

private const val FIELD_ID = 1

private val POPPED_TARGET =
    Target(
        id = 1,
        relatedFieldId = FIELD_ID,
        columnId = 0,
        value = 10,
        appearsAtMs = 0,
        finishesAtMs = 100_000,
    )

private val HALFWAY_FALLEN_TARGET =
    Target(
        id = 2,
        relatedFieldId = FIELD_ID,
        columnId = 0,
        value = 10,
        appearsAtMs = 0,
        finishesAtMs = 10_000,
    )

private fun stateWith(
    target: Target,
    gameTimeMs: Long = 0,
) = GameViewModel.State(
    field =
        Field(
            id = FIELD_ID,
            level = 1,
            currentOperationSign = OperationSign.SUBTRACTION,
            currentOperationDigit = 5,
            nextOperationSign = OperationSign.DIVISION,
            nextOperationDigit = 2,
            gameTimeMs = gameTimeMs,
        ),
    phase = GamePhase.Playing,
    targetList = persistentListOf(target),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class TargetFadeOutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `a popped target keeps rendering while it fades and stops being clickable`() {
        composeTestRule.mainClock.autoAdvance = false
        val gameState = mutableStateOf(stateWith(POPPED_TARGET))

        composeTestRule.setContent {
            MathBubblesTheme {
                PlayArea(
                    gameState = gameState,
                    onTargetClicked = {},
                    targetZeroedSignal = null,
                    readinessHintsEnabled = true,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(300L)

        composeTestRule.onNodeWithText("10").assertHasClickAction()

        gameState.value = stateWith(POPPED_TARGET.copy(isActive = false))
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(16L)

        composeTestRule.onNodeWithText("10").assertHasNoClickAction()

        composeTestRule.mainClock.advanceTimeBy(DisappearFade.DURATION_MS / 2L)
        composeTestRule.onNodeWithText("10").assertHasNoClickAction()

        composeTestRule.mainClock.advanceTimeBy(DisappearFade.DURATION_MS.toLong())
        composeTestRule.onAllNodesWithText("10").assertCountEquals(0)
    }

    @Test
    fun `a fading target's centre stays put`() {
        composeTestRule.mainClock.autoAdvance = false
        val gameState = mutableStateOf(stateWith(HALFWAY_FALLEN_TARGET, gameTimeMs = 5_000))

        composeTestRule.setContent {
            MathBubblesTheme {
                PlayArea(
                    gameState = gameState,
                    onTargetClicked = {},
                    targetZeroedSignal = null,
                    readinessHintsEnabled = true,
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(300L)
        val centreBeforeFade =
            composeTestRule
                .onNodeWithText("10")
                .fetchSemanticsNode()
                .boundsInRoot.center

        gameState.value = stateWith(HALFWAY_FALLEN_TARGET.copy(isActive = false), gameTimeMs = 5_000)
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(DisappearFade.DURATION_MS / 2L)
        val centreMidFade =
            composeTestRule
                .onNodeWithText("10")
                .fetchSemanticsNode()
                .boundsInRoot.center

        val toleranceX = with(composeTestRule.density) { 1.dp.toPx() }
        val toleranceY = with(composeTestRule.density) { 1.dp.toPx() }
        assertTrue(abs(centreMidFade.x - centreBeforeFade.x) <= toleranceX)
        assertTrue(abs(centreMidFade.y - centreBeforeFade.y) <= toleranceY)
    }
}
