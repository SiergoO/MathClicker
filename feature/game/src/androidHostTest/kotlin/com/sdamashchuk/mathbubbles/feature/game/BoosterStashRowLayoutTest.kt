package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.sdamashchuk.mathbubbles.core.game.model.ActiveEffects
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class BoosterStashRowLayoutTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `three stash slots are 48dp touch targets with centres 40dp apart on a 360dp phone`() {
        val gameState =
            mutableStateOf(
                GameViewModel.State(
                    field = Field(id = 1, boosterStash = listOf(Booster.FREEZE, Booster.REWIND, Booster.ICE_PICK)),
                    effects = ActiveEffects(),
                    phase = GamePhase.Playing,
                ),
            )

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            MathBubblesTheme {
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

        val density = composeTestRule.density
        val slotBounds =
            (1..3).map { slotNumber ->
                composeTestRule.onNodeWithContentDescription("Stash slot $slotNumber").fetchSemanticsNode().boundsInRoot
            }

        slotBounds.forEachIndexed { index, bounds ->
            assertEquals(48f, with(density) { bounds.width.toDp().value }, 0.5f, "slot ${index + 1} touch width")
            assertEquals(48f, with(density) { bounds.height.toDp().value }, 0.5f, "slot ${index + 1} touch height")
        }

        val centreXsDp = slotBounds.map { with(density) { (it.left + it.width / 2).toDp().value } }
        assertEquals(40f, centreXsDp[1] - centreXsDp[0], 0.5f, "centres 1-2 spacing: $centreXsDp")
        assertEquals(40f, centreXsDp[2] - centreXsDp[1], 0.5f, "centres 2-3 spacing: $centreXsDp")
    }
}
