package com.sdamashchuk.mathbubbles.feature.game

import kotlin.time.Clock
import kotlin.time.Instant

class FixedClock(
    private val epochMs: Long,
) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMs)
}
