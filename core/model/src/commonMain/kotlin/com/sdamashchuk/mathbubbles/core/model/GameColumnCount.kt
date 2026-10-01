package com.sdamashchuk.mathbubbles.core.model

// Shared by feature/game's layout math, core/game's column-id wraparound and its appearance-delay
// staggering — the one place all three can depend on without a new module edge.
const val GAME_COLUMN_COUNT = 4
