package com.sdamashchuk.matharcade.core.model

data class Field(
    val id: Int = 0,
    val level: Int = 1,
    val score: Int = 0,
    val lifeCount: Int = INITIAL_LIFE_COUNT,
    // MC-95: how many targets the last press scored on, not a streak over presses. See applyCombo.
    val bonusMultiplier: Int = 0,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val currentOperationDigit: Int = 0,
    val nextOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 0,
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
) {
    // Computed here rather than in the engine's mapper: the HUD renders this number, and a label
    // should not have to import :core:game to lay itself out (the same reason Target.position
    // lives on the model). MC-95: bonusMultiplier is how many targets the last press scored on, so
    // 0 means the press scored on nothing - the floor at 1 keeps that from erasing an award, which
    // is the same role it played when this was a streak.
    val appliedMultiplier: Int
        get() = maxOf(1, bonusMultiplier)
}
