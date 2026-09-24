package com.sdamashchuk.matharcade.core.model

data class Field(
    val id: Int = 0,
    val level: Int = 1,
    val score: Int = 0,
    val lifeCount: Int = 3,
    val bonusMultiplier: Int = 0,
    val currentOperationSign: OperationSign = OperationSign.DIVISION,
    val currentOperationDigit: Int = 0,
    val nextOperationSign: OperationSign = OperationSign.DIVISION,
    val nextOperationDigit: Int = 0,
    val isClosed: Boolean = false,
) {
    // Computed here rather than in the engine's mapper: the HUD renders this number, and a label
    // should not have to import :core:game to lay itself out (the same reason Target.position
    // lives on the model). A resting streak of 0 must still score, hence the floor at 1.
    val appliedMultiplier: Int
        get() = maxOf(1, bonusMultiplier)
}
