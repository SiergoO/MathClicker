package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.FieldAction
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val LONG_SWIPE_PX = 300f
private const val SHORT_SWIPE_PX = 60f
private const val SWIPE_MS = 200L
private const val FLIGHT_SETTLE_MS = 4_000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenStashTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun play(fixture: GameScreenFixture) {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(100)
    }

    private fun fixtureHolding(
        current: Booster?,
        stash: List<Booster> = emptyList(),
    ) = GameScreenFixture(
        savedField = GameScreenFixture.openField(currentBooster = current, boosterStash = stash),
        savedTargets = listOf(columnTarget(1, 0, 40), columnTarget(2, 2, 50)),
    )

    private fun swipeFireButtonLeft(
        description: String,
        distancePx: Float,
        release: Boolean = true,
    ) {
        composeTestRule.onNodeWithContentDescription(description).performTouchInput {
            if (release) {
                swipe(center, center - Offset(distancePx, 0f), SWIPE_MS)
            } else {
                down(center)
                moveBy(Offset(-distancePx, 0f))
            }
        }
    }

    private fun fireButtonX(description: String) =
        composeTestRule
            .onNodeWithContentDescription(description)
            .fetchSemanticsNode()
            .positionInRoot.x

    @Test
    fun `swiping the fire button left stashes the booster`() {
        val fixture = fixtureHolding(Booster.FREEZE)
        play(fixture)

        swipeFireButtonLeft("Freeze", LONG_SWIPE_PX)
        composeTestRule.mainClock.advanceTimeBy(FLIGHT_SETTLE_MS)

        assertEquals(listOf(Booster.FREEZE), fixture.state.field.boosterStash)
        assertNull(fixture.state.effects.timedBooster)
    }

    @Test
    fun `a stashed booster is replaced on the fire button by the queued operation`() {
        val fixture = fixtureHolding(Booster.FREEZE)
        play(fixture)

        swipeFireButtonLeft("Freeze", LONG_SWIPE_PX)
        composeTestRule.mainClock.advanceTimeBy(FLIGHT_SETTLE_MS)

        assertEquals(FieldAction.Operation(OperationSign.SUBTRACTION, 3), fixture.state.field.currentAction)
    }

    @Test
    fun `the dock shows the booster in the first stash slot after a stash`() {
        val fixture = fixtureHolding(Booster.FREEZE)
        play(fixture)
        composeTestRule.onNodeWithContentDescription("Empty stash slot 1").assertExists()

        swipeFireButtonLeft("Freeze", LONG_SWIPE_PX)
        composeTestRule.mainClock.advanceTimeBy(FLIGHT_SETTLE_MS)

        composeTestRule.onNodeWithContentDescription("Stash slot 1").assertExists()
        composeTestRule.onNodeWithContentDescription("Empty stash slot 2").assertExists()
    }

    @Test
    fun `a short swipe snaps the fire button back without stashing`() {
        val fixture = fixtureHolding(Booster.FREEZE)
        play(fixture)
        val restingX = fireButtonX("Freeze")

        swipeFireButtonLeft("Freeze", SHORT_SWIPE_PX)
        composeTestRule.mainClock.advanceTimeBy(FLIGHT_SETTLE_MS)

        assertEquals(restingX, fireButtonX("Freeze"), 0.5f)
        assertTrue(
            fixture.state.field.boosterStash
                .isEmpty(),
        )
        assertEquals(Booster.FREEZE, fixture.state.field.currentBooster)
        assertNull(fixture.state.effects.timedBooster)
    }

    @Test
    fun `holding a long swipe moves the fire button without stashing until release`() {
        val fixture = fixtureHolding(Booster.FREEZE)
        play(fixture)
        val restingX = fireButtonX("Freeze")

        swipeFireButtonLeft("Freeze", LONG_SWIPE_PX, release = false)
        composeTestRule.mainClock.advanceTimeBy(100)

        assertTrue(fireButtonX("Freeze") < restingX - 1f)
        assertTrue(
            fixture.state.field.boosterStash
                .isEmpty(),
        )
    }

    @Test
    fun `tapping the stash slot applies the stashed booster and empties the slot`() {
        val fixture = fixtureHolding(current = null, stash = listOf(Booster.FREEZE))
        play(fixture)

        composeTestRule.onNodeWithContentDescription("Stash slot 1").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertEquals(Booster.FREEZE, fixture.state.effects.timedBooster)
        assertTrue(
            fixture.state.field.boosterStash
                .isEmpty(),
        )
        composeTestRule.onNodeWithContentDescription("Empty stash slot 1").assertExists()
    }

    @Test
    fun `tapping the second stash slot applies that booster and keeps the first`() {
        val fixture = fixtureHolding(current = null, stash = listOf(Booster.FREEZE, Booster.SHIELD))
        play(fixture)

        composeTestRule.onNodeWithContentDescription("Stash slot 2").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertTrue(fixture.state.effects.shieldActive)
        assertNull(fixture.state.effects.timedBooster)
        assertEquals(listOf(Booster.FREEZE), fixture.state.field.boosterStash)
    }

    @Test
    fun `tapping the fire button while the booster is in flight is ignored`() {
        val fixture = fixtureHolding(Booster.FREEZE)
        play(fixture)

        swipeFireButtonLeft("Freeze", LONG_SWIPE_PX)
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithContentDescription("Freeze").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertNull(fixture.state.effects.timedBooster)
        assertEquals(Booster.FREEZE, fixture.state.field.currentBooster)

        composeTestRule.mainClock.advanceTimeBy(FLIGHT_SETTLE_MS)
        assertEquals(listOf(Booster.FREEZE), fixture.state.field.boosterStash)
        assertNull(fixture.state.effects.timedBooster)
    }
}
