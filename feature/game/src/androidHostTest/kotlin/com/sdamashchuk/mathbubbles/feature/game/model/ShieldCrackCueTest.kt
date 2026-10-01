package com.sdamashchuk.mathbubbles.feature.game.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShieldCrackCueTest {
    @Test
    fun `a fresh cue starts not cracking`() {
        assertFalse(ShieldCrackCue().isCracking.value)
    }

    @Test
    fun `absorb starts the crack and finish ends it`() {
        val cue = ShieldCrackCue()

        cue.absorb()
        assertTrue(cue.isCracking.value)

        cue.finish()
        assertFalse(cue.isCracking.value)
    }

    @Test
    fun `finish without a prior absorb is a no-op, so a stray call never arms it`() {
        val cue = ShieldCrackCue()

        cue.finish()

        assertFalse(cue.isCracking.value)
    }

    @Test
    fun `a second absorb after finish re-arms it, so a later breakout crack still plays`() {
        val cue = ShieldCrackCue()
        cue.absorb()
        cue.finish()

        cue.absorb()

        assertTrue(cue.isCracking.value)
    }
}
