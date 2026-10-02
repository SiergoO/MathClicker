package com.sdamashchuk.mathbubbles.core.model

data class Field(
    val id: Int = 0,
    val level: Int = 1,
    val score: Int = 0,
    val lifeCount: Int = INITIAL_LIFE_COUNT,
    val bonusMultiplier: Int = 0,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val currentOperationDigit: Int = 0,
    val nextOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 0,
    // A set booster overrides the operation pair below it; Game keeps that pair a freshly drawn,
    // checked operation regardless, so it is always a valid fallback.
    val currentBooster: Booster? = null,
    val nextBooster: Booster? = null,
    val boosterStash: List<Booster> = emptyList(),
    val boosterDropCounter: Int = 0,
    val hasDroppedBoosterThisSession: Boolean = false,
    val isClosed: Boolean = false,
    // Epoch milliseconds set once, when closeIfNecessary() first closes the field (see
    // FieldMapper). Null for a still-open run and, permanently, for any run that closed before
    // MC-53 added this column - there is no date to recover for those.
    val finishedAt: Long? = null,
    // The engine's own monotonic play-time axis, advanced only by Game.tick(). Not wall-clock time
    // - finishedAt above is - and must never be read from a device clock: two engines seeded alike
    // must reach identical gameTimeMs too. Long: 999 levels of tens of seconds each outlives Int's
    // ~24-day range.
    val gameTimeMs: Long = 0,
    // A running Freeze/Rewind, its carried clock rate and tint envelope, the armed ice pick's
    // source and an active shield, so a kill mid-effect restores at the same intensity.
    val timedEffectBooster: Booster? = null,
    val timedEffectRemainingMs: Int = 0,
    val timedEffectRate: Double = 1.0,
    val freezeTintEnvelope: Double = 0.0,
    val icePickArmedFireButton: Boolean = false,
    val icePickArmedStashIndex: Int? = null,
    val shieldActive: Boolean = false,
) {
    val appliedMultiplier: Int
        get() = bonusMultiplier + 1

    val currentAction: FieldAction
        get() =
            currentBooster?.let { FieldAction.BoosterAction(it) }
                ?: FieldAction.Operation(currentOperationSign, currentOperationDigit)

    val nextAction: FieldAction
        get() =
            nextBooster?.let { FieldAction.BoosterAction(it) }
                ?: FieldAction.Operation(nextOperationSign, nextOperationDigit)
}
