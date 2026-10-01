package com.sdamashchuk.mathbubbles.core.game

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClockStepScaleTest {
    @Test
    fun `a scale of 1 dot 0 reproduces the raw elapsedMs when it is under the clamp`() {
        assertEquals(100, scaleTickStep(elapsedMs = 100, clockScale = 1.0, maxTickMs = 250))
    }

    @Test
    fun `a scale of zero freezes the step regardless of how large elapsedMs is`() {
        assertEquals(0, scaleTickStep(elapsedMs = 30_000, clockScale = 0.0, maxTickMs = 250))
        assertEquals(0, scaleTickStep(elapsedMs = 1, clockScale = 0.0, maxTickMs = 250))
    }

    @Test
    fun `a fractional scale slows the step proportionally`() {
        assertEquals(50, scaleTickStep(elapsedMs = 100, clockScale = 0.5, maxTickMs = 250))
    }

    // M3: applying the scale after MAX_TICK_MS's clamp instead of before would let a scale above
    // 1.0 push the returned step past what the clamp is supposed to guarantee as a ceiling - here,
    // clamping the raw 200 first would leave it at 200, and only then doubling it to 400 would slip
    // straight past maxTickMs. Applying the scale first and clamping last, as this function does,
    // keeps the result bounded no matter how large clockScale gets.
    @Test
    fun `a scale above 1 can never push the result past maxTickMs`() {
        val step = scaleTickStep(elapsedMs = 200, clockScale = 2.0, maxTickMs = 250)

        assertEquals(250, step)
        assertTrue(step <= 250)
    }

    @Test
    fun `a negative elapsedMs floors at zero the same way the pre-MC-74 clamp did`() {
        assertEquals(0, scaleTickStep(elapsedMs = -50, clockScale = 1.0, maxTickMs = 250))
    }
}
