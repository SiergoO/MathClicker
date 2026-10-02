package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.ui.component.moteScatter

// Small motes rising toward a target's spawn point in the window before it appears - driven by
// appearsAtMs vs gameTimeMs alone, so freeze and rewind affect it the same way they affect a fall.
internal const val SPAWN_MOTE_COUNT = 4

internal const val SPAWN_WARNING_WINDOW_MS = 1_600L
private const val SPAWN_MOTE_STAGGER_MS = 250L
private const val SPAWN_MOTE_FADE_IN_FRACTION = 0.15f
private const val SPAWN_MOTE_FADE_OUT_FRACTION = 0.85f
private const val SPAWN_MOTE_JITTER_FRACTION = 0.12f

private const val SALT_JITTER = 11

/**
 * How long before the target appears this mote is born; later motes are born later, staggered evenly.
 */
internal fun spawnMoteStartMs(moteIndex: Int): Long = SPAWN_WARNING_WINDOW_MS - moteIndex * SPAWN_MOTE_STAGGER_MS

/**
 * 0 where a mote is born, 1 where it reaches the spawn point as the target appears.
 */
internal fun spawnMoteProgress(
    msUntilAppear: Long,
    moteIndex: Int,
): Float {
    val startMs = spawnMoteStartMs(moteIndex)
    return (1f - msUntilAppear.toFloat() / startMs).coerceIn(0f, 1f)
}

internal fun spawnMoteAlpha(progress: Float): Float {
    val fadeIn = (progress / SPAWN_MOTE_FADE_IN_FRACTION).coerceIn(0f, 1f)
    val fadeOut = ((1f - progress) / (1f - SPAWN_MOTE_FADE_OUT_FRACTION)).coerceIn(0f, 1f)
    return minOf(fadeIn, fadeOut)
}

/**
 * 1 below the spawn point, 0 at it.
 */
internal fun spawnMoteRiseFraction(progress: Float): Float = 1f - progress

internal fun spawnMoteJitterFraction(
    targetId: Int,
    moteIndex: Int,
): Float {
    val scatter = moteScatter(targetId * SPAWN_MOTE_COUNT + moteIndex, SALT_JITTER)
    return (scatter * 2f - 1f) * SPAWN_MOTE_JITTER_FRACTION
}
