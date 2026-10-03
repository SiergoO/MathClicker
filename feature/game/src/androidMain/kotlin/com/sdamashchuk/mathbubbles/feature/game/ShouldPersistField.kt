package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.model.Field
import kotlin.math.abs

private const val CLOCK_PERSIST_INTERVAL_MS = 3_000L

/**
 * True when [next] differs from [persisted] in anything but the clock and a running effect's countdown,
 * or when the clock has moved a full interval either way (Rewind runs it backwards).
 */
internal fun shouldPersistField(
    persisted: Field,
    next: Field,
): Boolean {
    val clockOnly =
        persisted.copy(
            gameTimeMs = next.gameTimeMs,
            timedEffectRemainingMs = next.timedEffectRemainingMs,
            timedEffectRate = next.timedEffectRate,
            freezeTintEnvelope = next.freezeTintEnvelope,
        )
    return clockOnly != next || abs(next.gameTimeMs - persisted.gameTimeMs) >= CLOCK_PERSIST_INTERVAL_MS
}
