package com.sdamashchuk.mathbubbles.feature.game.model

// An ice pick kill plays its own shatter before the normal burst; a plain zeroing has no shatter
// phase to wait on and starts straight at Burst.
enum class BurstPhase {
    Shatter,
    Burst,
}
