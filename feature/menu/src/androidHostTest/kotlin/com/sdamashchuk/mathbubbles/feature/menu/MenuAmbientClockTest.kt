package com.sdamashchuk.mathbubbles.feature.menu

import androidx.compose.ui.test.junit4.createComposeRule
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MenuAmbientClockTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the ambient clock does not tick before the menu is resumed`() {
        val lifecycle = LifecycleRegistry()
        lateinit var clock: () -> Long
        composeTestRule.setContent { clock = rememberMenuAmbientClock(lifecycle) }

        composeTestRule.mainClock.advanceTimeBy(1_000L)

        assertEquals(0L, clock())
    }

    @Test
    fun `the ambient clock ticks while resumed and stops again once paused`() {
        val lifecycle = LifecycleRegistry()
        lateinit var clock: () -> Long
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent { clock = rememberMenuAmbientClock(lifecycle) }
        composeTestRule.mainClock.advanceTimeByFrame()

        composeTestRule.runOnUiThread {
            lifecycle.create()
            lifecycle.start()
            lifecycle.resume()
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1_000L)
        assertTrue("clock at ${clock()} state ${lifecycle.state}", clock() > 0L)
        composeTestRule.runOnUiThread { lifecycle.pause() }
        composeTestRule.mainClock.advanceTimeByFrame()
        val whenPaused = clock()
        composeTestRule.mainClock.advanceTimeBy(1_000L)

        assertEquals(whenPaused, clock())
    }
}
