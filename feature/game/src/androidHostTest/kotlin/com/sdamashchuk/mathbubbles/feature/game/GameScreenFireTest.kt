package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sdamashchuk.mathbubbles.core.game.model.IcePickSource
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

private val BOARD_VALUES = listOf(80, 120, 60, 100)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenFireTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun play(fixture: GameScreenFixture) {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(100)
    }

    private fun boardFixture(
        currentBooster: Booster? = null,
        score: Int = 0,
    ) = GameScreenFixture(
        savedField = GameScreenFixture.openField(score = score, currentBooster = currentBooster),
        savedTargets = BOARD_VALUES.mapIndexed { column, value -> columnTarget(column + 1, column, value) },
    )

    @Test
    fun `the fire button applies the current operation to every visible bubble`() {
        val fixture = boardFixture()
        play(fixture)

        composeTestRule.onNodeWithText("${OperationSign.DIVISION.sign}2").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertEquals(
            BOARD_VALUES.map { it / 2 },
            fixture.state.targetList
                .sortedBy { it.columnId }
                .map { it.value },
        )
        composeTestRule.onNodeWithText("40").assertExists()
        composeTestRule.onNodeWithText("60").assertExists()
    }

    @Test
    fun `the fire button scores the removed value times the multiplier`() {
        val fixture = boardFixture()
        play(fixture)

        composeTestRule.onNodeWithText("${OperationSign.DIVISION.sign}2").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        val removed = BOARD_VALUES.sumOf { it - it / 2 }
        assertEquals(removed * fixture.state.field.appliedMultiplier, fixture.state.field.score)
    }

    @Test
    fun `firing promotes the queued operation onto the fire button`() {
        val fixture = boardFixture()
        play(fixture)

        composeTestRule.onNodeWithText("${OperationSign.DIVISION.sign}2").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertEquals(FieldAction.Operation(OperationSign.SUBTRACTION, 3), fixture.state.field.currentAction)
        composeTestRule.onNodeWithText("${OperationSign.SUBTRACTION.sign}3").assertExists()
    }

    @Test
    fun `without a fire tap nothing changes while time passes`() {
        val fixture = boardFixture()
        play(fixture)
        composeTestRule.mainClock.advanceTimeBy(500)

        assertEquals(
            BOARD_VALUES,
            fixture.state.targetList
                .sortedBy { it.columnId }
                .map { it.value },
        )
        assertEquals(0, fixture.state.field.score)
    }

    @Test
    fun `firing a booster applies its effect and leaves the bubbles and score alone`() {
        val fixture = boardFixture(currentBooster = Booster.FREEZE)
        play(fixture)

        composeTestRule.onNodeWithContentDescription("Freeze").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertEquals(Booster.FREEZE, fixture.state.effects.timedBooster)
        assertEquals(
            BOARD_VALUES,
            fixture.state.targetList
                .sortedBy { it.columnId }
                .map { it.value },
        )
        assertEquals(0, fixture.state.field.score)
        assertNull(fixture.state.field.currentBooster)
    }

    @Test
    fun `firing the ice pick arms it and a second fire tap disarms it`() {
        val fixture = boardFixture(currentBooster = Booster.ICE_PICK)
        play(fixture)

        composeTestRule.onNodeWithContentDescription("Ice pick").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)
        assertEquals(IcePickSource.FireButton, fixture.state.effects.icePickArmedFrom)

        composeTestRule.onNodeWithContentDescription("Ice pick armed, tap to disarm").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)
        assertNull(fixture.state.effects.icePickArmedFrom)
    }
}
