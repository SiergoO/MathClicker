package com.sdamashchuk.mathbubbles.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpawnMoteTest {
    @Test
    fun `a mote is invisible before it is born and after it reaches the spawn point`() {
        assertEquals(0f, spawnMoteProgress(msUntilAppear = SPAWN_WARNING_WINDOW_MS + 500L, moteIndex = 0), 0f)
        assertEquals(1f, spawnMoteProgress(msUntilAppear = 0L, moteIndex = 0), 0f)
    }

    @Test
    fun `later motes are born later, staggered evenly`() {
        val starts = (0 until SPAWN_MOTE_COUNT).map { spawnMoteStartMs(it) }
        assertEquals(starts.sortedDescending(), starts)
        assertEquals(starts.size, starts.distinct().size)
    }

    @Test
    fun `a mote's progress is the same every time it is asked for the same clock reading`() {
        assertEquals(
            spawnMoteProgress(msUntilAppear = 800L, moteIndex = 1),
            spawnMoteProgress(msUntilAppear = 800L, moteIndex = 1),
            0f,
        )
    }

    @Test
    fun `progress moves the opposite way when the clock runs backward`() {
        val forward = spawnMoteProgress(msUntilAppear = 900L, moteIndex = 0)
        val rewound = spawnMoteProgress(msUntilAppear = 1_100L, moteIndex = 0)
        assertTrue(rewound < forward)
    }

    @Test
    fun `a mote fades in at birth and fades out at the spawn point`() {
        assertEquals(0f, spawnMoteAlpha(progress = 0f), 0f)
        assertEquals(0f, spawnMoteAlpha(progress = 1f), 0f)
        assertTrue(spawnMoteAlpha(progress = 0.5f) > 0f)
    }

    @Test
    fun `a mote rises from below the spawn point to it`() {
        assertEquals(1f, spawnMoteRiseFraction(progress = 0f), 0f)
        assertEquals(0f, spawnMoteRiseFraction(progress = 1f), 0f)
    }

    @Test
    fun `a mote's horizontal jitter is stable for a given target and mote index`() {
        assertEquals(
            spawnMoteJitterFraction(targetId = 42, moteIndex = 2),
            spawnMoteJitterFraction(targetId = 42, moteIndex = 2),
            0f,
        )
    }

    @Test
    fun `motes on different targets do not share the same jitter`() {
        val jitters = (0 until SPAWN_MOTE_COUNT).map { spawnMoteJitterFraction(targetId = 7, moteIndex = it) }
        assertEquals(jitters.size, jitters.distinct().size)
    }

    @Test
    fun `jitter never pushes a mote outside a small band around its column`() {
        assertNotEquals(0f, spawnMoteJitterFraction(targetId = 1, moteIndex = 0))
        assertTrue(kotlin.math.abs(spawnMoteJitterFraction(targetId = 1, moteIndex = 0)) <= 0.12f)
    }
}
