package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val FIRE_BUTTON_TAG = "fireButton"

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FireButtonSwipeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `fire button snaps back to centre when the action promotes mid-drag`() {
        var action by mutableStateOf<FieldAction>(FieldAction.BoosterAction(Booster.FREEZE))

        composeTestRule.setContent {
            MathBubblesTheme {
                FireButton(
                    action = action,
                    countdownColor = null,
                    countdownFraction = { 0f },
                    onFireClicked = {},
                    onStashBooster = {},
                    modifier = Modifier.testTag(FIRE_BUTTON_TAG),
                )
            }
        }

        val initialX =
            composeTestRule
                .onNodeWithTag(FIRE_BUTTON_TAG)
                .fetchSemanticsNode()
                .positionInRoot.x

        composeTestRule.onNodeWithTag(FIRE_BUTTON_TAG).performTouchInput {
            down(center)
            repeat(10) {
                moveBy(Offset(-20f, 0f))
                advanceEventTime(16)
            }
        }
        composeTestRule.waitForIdle()
        val shiftedX =
            composeTestRule
                .onNodeWithTag(FIRE_BUTTON_TAG)
                .fetchSemanticsNode()
                .positionInRoot.x
        assertNotEquals(initialX, shiftedX, "the drag never shifted the button")

        // Promotes the next action while the pointer is still down, the way a real stash reaching
        // the engine asynchronously can.
        action = FieldAction.Operation(OperationSign.SUBTRACTION, 1)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(FIRE_BUTTON_TAG).performTouchInput { up() }
        composeTestRule.waitForIdle()

        val finalX =
            composeTestRule
                .onNodeWithTag(FIRE_BUTTON_TAG)
                .fetchSemanticsNode()
                .positionInRoot.x
        assertEquals(initialX, finalX, 0.5f, "the button stayed shifted after the action promoted mid-drag")
    }
}
