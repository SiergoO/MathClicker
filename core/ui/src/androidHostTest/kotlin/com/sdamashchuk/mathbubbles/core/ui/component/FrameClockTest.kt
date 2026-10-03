package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FrameClockTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the clock holds at zero while not running`() {
        lateinit var clock: () -> Long
        composeTestRule.setContent { clock = rememberFrameClock(running = false) }

        composeTestRule.mainClock.advanceTimeBy(1_000L)

        assertEquals(0L, clock())
    }

    @Test
    fun `the clock advances while running and holds its value once stopped`() {
        val running = mutableStateOf(true)
        lateinit var clock: () -> Long
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { clock = rememberFrameClock(running = running.value) }

        composeTestRule.mainClock.advanceTimeBy(1_000L)
        assertTrue("clock at ${clock()}", clock() in 500L..1_000L)
        running.value = false
        composeTestRule.mainClock.advanceTimeByFrame()
        val whenStopped = clock()
        composeTestRule.mainClock.advanceTimeBy(1_000L)

        assertEquals(whenStopped, clock())
    }
}
