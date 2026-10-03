package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PauseOnBackgroundTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val owner = TestLifecycleOwner()
    private var phase by mutableStateOf(GamePhase.Playing)
    private var pauseCount = 0

    private fun setUpScreen(startState: Lifecycle.State = Lifecycle.State.RESUMED) {
        owner.moveTo(startState)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                PauseOnBackground(phase = phase, onPause = { pauseCount++ })
            }
        }
    }

    @Test
    fun `losing focus while playing pauses the game`() {
        setUpScreen()
        composeTestRule.runOnUiThread { owner.moveTo(Lifecycle.State.STARTED) }
        assertEquals(1, pauseCount)
    }

    @Test
    fun `stopping while playing pauses the game`() {
        setUpScreen(Lifecycle.State.STARTED)
        composeTestRule.runOnUiThread { owner.moveTo(Lifecycle.State.CREATED) }
        assertEquals(1, pauseCount)
    }

    @Test
    fun `backgrounding outside the playing phase does not pause`() {
        setUpScreen()
        composeTestRule.runOnUiThread { phase = GamePhase.Paused }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread { owner.moveTo(Lifecycle.State.CREATED) }
        assertEquals(0, pauseCount)
    }
}
