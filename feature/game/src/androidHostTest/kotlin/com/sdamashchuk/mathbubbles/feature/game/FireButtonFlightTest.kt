package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private const val FIRE_BUTTON_TAG = "fireButton"
private const val FLIGHT_TARGET_PX = -40f
private const val FLIGHT_CHECKPOINT_MS = 300L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class FireButtonFlightTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun swipePastThreshold() {
        composeTestRule.onNodeWithTag(FIRE_BUTTON_TAG).performTouchInput {
            down(center)
            repeat(10) {
                moveBy(Offset(-20f, 0f))
                advanceEventTime(16)
            }
            up()
        }
    }

    @Test
    fun `a swipe past threshold flies the token to the measured slot centre and stashes once`() {
        var stashCount = 0
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MathBubblesTheme {
                FireButton(
                    action = FieldAction.BoosterAction(Booster.FREEZE),
                    countdownColor = null,
                    countdownFraction = { 0f },
                    onFireClicked = {},
                    onStashBooster = { stashCount++ },
                    flightTargetOffsetPx = { FLIGHT_TARGET_PX },
                    flightLandingDiameter = 36.dp,
                    modifier = Modifier.testTag(FIRE_BUTTON_TAG),
                )
            }
        }

        val node = composeTestRule.onNodeWithTag(FIRE_BUTTON_TAG)
        val initialBounds = node.fetchSemanticsNode().boundsInRoot
        val targetCentreX = initialBounds.left + initialBounds.width / 2 + FLIGHT_TARGET_PX

        swipePastThreshold()
        composeTestRule.mainClock.advanceTimeBy(FLIGHT_CHECKPOINT_MS)

        val landedBounds = node.fetchSemanticsNode().boundsInRoot
        val landedCentreX = landedBounds.left + landedBounds.width / 2
        val toleranceX = with(composeTestRule.density) { 2.dp.toPx() }
        assertEquals(targetCentreX, landedCentreX, toleranceX, "token centre did not reach the slot centre at landing")

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        assertEquals(1, stashCount, "stash callback fired the wrong number of times")
    }

    @Test
    fun `the token returns to rest after the flight even when the action does not change`() {
        composeTestRule.setContent {
            MathBubblesTheme {
                FireButton(
                    action = FieldAction.BoosterAction(Booster.FREEZE),
                    countdownColor = null,
                    countdownFraction = { 0f },
                    onFireClicked = {},
                    onStashBooster = {},
                    flightTargetOffsetPx = { FLIGHT_TARGET_PX },
                    flightLandingDiameter = 36.dp,
                    modifier = Modifier.testTag(FIRE_BUTTON_TAG),
                )
            }
        }

        val node = composeTestRule.onNodeWithTag(FIRE_BUTTON_TAG)
        val initialX = node.fetchSemanticsNode().boundsInRoot.left

        swipePastThreshold()
        composeTestRule.waitForIdle()

        val finalX = node.fetchSemanticsNode().boundsInRoot.left
        assertEquals(initialX, finalX, 0.5f, "the token stayed displaced after a flight with no action change")
    }

    @Test
    fun `a tap mid-flight does not trigger another click or a second stash`() {
        var clickCount = 0
        var stashCount = 0
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MathBubblesTheme {
                FireButton(
                    action = FieldAction.BoosterAction(Booster.FREEZE),
                    countdownColor = null,
                    countdownFraction = { 0f },
                    onFireClicked = { clickCount++ },
                    onStashBooster = { stashCount++ },
                    flightTargetOffsetPx = { FLIGHT_TARGET_PX },
                    flightLandingDiameter = 36.dp,
                    modifier = Modifier.testTag(FIRE_BUTTON_TAG),
                )
            }
        }

        swipePastThreshold()
        composeTestRule.mainClock.advanceTimeBy(50)

        composeTestRule.onNodeWithTag(FIRE_BUTTON_TAG).performClick()

        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()

        assertEquals(0, clickCount, "a tap mid-flight must not reach onFireClicked")
        assertEquals(1, stashCount, "a tap mid-flight must not change how many times the stash fires")
    }
}
