package com.sdamashchuk.mathbubbles.feature.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.sdamashchuk.mathbubbles.core.model.Field
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RememberPhaseTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val stateHolder = mutableStateOf(GameViewModel.State(phase = GamePhase.Playing))
    private var state by stateHolder
    private var probeCompositions = 0
    private var observedPhase = GamePhase.ReadyToPlay

    @Composable
    private fun Probe() {
        val phase by rememberPhase(stateHolder)
        probeCompositions++
        observedPhase = phase
    }

    @Test
    fun `ticks that leave the phase alone do not recompose the reader`() {
        composeTestRule.setContent { Probe() }
        val baseline = probeCompositions
        repeat(TICKS) { tick ->
            composeTestRule.runOnUiThread {
                state = state.copy(field = Field(gameTimeMs = (tick + 1) * TICK_MS))
            }
            composeTestRule.waitForIdle()
        }
        assertEquals(baseline, probeCompositions)
    }

    @Test
    fun `a phase change recomposes the reader with the new phase`() {
        composeTestRule.setContent { Probe() }
        composeTestRule.runOnUiThread { state = state.copy(phase = GamePhase.Paused) }
        composeTestRule.waitForIdle()
        assertEquals(GamePhase.Paused, observedPhase)
    }

    private companion object {
        const val TICKS = 20
        const val TICK_MS = 16L
    }
}
