package com.sdamashchuk.mathbubbles.feature.game.model

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

// Hoisted above Field in GameScreen so a Field remount (pause, level intro) never replays it:
// absorb and finish are its only two writes, and nothing but a fresh absorb re-arms it.
class ShieldCrackCue {
    private val cracking = mutableStateOf(false)
    val isCracking: State<Boolean> = cracking

    fun absorb() {
        cracking.value = true
    }

    fun finish() {
        cracking.value = false
    }
}
