package com.sdamashchuk.matharcade.feature.game.model

// The sequence number, not just targetId/awarded equality, is what makes this safe to key a
// LaunchedEffect on: two zeroings can carry an identical payload (same column, same awarded
// score), and a plain data-class key would make Compose treat the second as a no-op change,
// silently dropping its burst.
data class TargetZeroedSignal(
    val targetId: Int,
    val sequence: Int,
)
