package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.sdamashchuk.mathbubbles.core.model.Booster
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
@Config(sdk = [34])
class BoosterStashSlotIconCenteringTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `stash slot icon centre matches the slot circle centre`() {
        var iconLabel = ""
        composeTestRule.setContent {
            MathBubblesTheme {
                iconLabel = stringResource(id = boosterLabelFor(Booster.FREEZE))
                BoosterStashSlot(booster = Booster.FREEZE, slotIndex = 0, onClick = {})
            }
        }

        val slotBounds =
            composeTestRule
                .onNodeWithContentDescription("Stash slot 1")
                .fetchSemanticsNode()
                .boundsInRoot
        val iconBounds =
            composeTestRule
                .onNodeWithContentDescription(iconLabel)
                .fetchSemanticsNode()
                .boundsInRoot

        val density = composeTestRule.density
        val slotCentreX = with(density) { (slotBounds.left + slotBounds.width / 2).toDp().value }
        val slotCentreY = with(density) { (slotBounds.top + slotBounds.height / 2).toDp().value }
        val iconCentreX = with(density) { (iconBounds.left + iconBounds.width / 2).toDp().value }
        val iconCentreY = with(density) { (iconBounds.top + iconBounds.height / 2).toDp().value }

        assertEquals(slotCentreX, iconCentreX, 0.5f, "icon centre x vs slot centre x")
        assertEquals(slotCentreY, iconCentreY, 0.5f, "icon centre y vs slot centre y")
    }
}
