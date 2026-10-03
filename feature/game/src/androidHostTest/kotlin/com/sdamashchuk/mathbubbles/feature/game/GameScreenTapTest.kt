package com.sdamashchuk.mathbubbles.feature.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sdamashchuk.mathbubbles.core.ui.sound.model.SoundSample
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

private val COLUMN_VALUES = listOf(21, 32, 43, 54)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class GameScreenTapTest {
    @get:Rule(order = 0)
    val mainDispatcher = MainDispatcherRule()

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fixture =
        GameScreenFixture(
            savedField = GameScreenFixture.openField(),
            savedTargets = COLUMN_VALUES.mapIndexed { column, value -> columnTarget(column + 1, column, value) },
        )

    private fun startPlayingAndTap(value: Int) {
        fixture.show(composeTestRule)
        fixture.startPlaying()
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithText("$value").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)
    }

    private fun assertOnlyColumnTapped(column: Int) {
        val expected = COLUMN_VALUES.mapIndexed { index, value -> if (index == column) value - 1 else value }
        assertEquals(
            expected,
            fixture.state.targetList
                .sortedBy { it.columnId }
                .map { it.value },
        )
        assertEquals(1, fixture.state.field.score)
        composeTestRule.onNodeWithText("${expected[column]}").assertExists()
    }

    @Test
    fun `tapping the bubble in the first column lowers only that bubble and scores`() {
        startPlayingAndTap(COLUMN_VALUES[0])
        assertOnlyColumnTapped(0)
    }

    @Test
    fun `tapping the bubble in the second column lowers only that bubble and scores`() {
        startPlayingAndTap(COLUMN_VALUES[1])
        assertOnlyColumnTapped(1)
    }

    @Test
    fun `tapping the bubble in the third column lowers only that bubble and scores`() {
        startPlayingAndTap(COLUMN_VALUES[2])
        assertOnlyColumnTapped(2)
    }

    @Test
    fun `tapping the bubble in the fourth column lowers only that bubble and scores`() {
        startPlayingAndTap(COLUMN_VALUES[3])
        assertOnlyColumnTapped(3)
    }

    @Test
    fun `a tap plays the tap sound once`() {
        startPlayingAndTap(COLUMN_VALUES[0])
        assertEquals(listOf(SoundSample.Tap), fixture.sounds.played)
    }

    @Test
    fun `tapping the same bubble twice scores twice`() {
        startPlayingAndTap(COLUMN_VALUES[2])
        composeTestRule.onNodeWithText("${COLUMN_VALUES[2] - 1}").performClick()
        composeTestRule.mainClock.advanceTimeBy(32)

        assertEquals(2, fixture.state.field.score)
        assertEquals(
            COLUMN_VALUES[2] - 2,
            fixture.state.targetList
                .single { it.columnId == 2 }
                .value,
        )
    }
}
