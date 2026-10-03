package com.sdamashchuk.mathbubbles.feature.game

private const val MAX_REAL_FRAME_GAP_MS = 250

internal fun realFrameStepMs(frameGapMs: Int): Int = if (frameGapMs > MAX_REAL_FRAME_GAP_MS) 0 else frameGapMs
