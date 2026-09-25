package com.sdamashchuk.matharcade.core.ui.sound.model

/**
 * The six samples the game plays, one per [com.sdamashchuk.matharcade.core.ui.sound.SoundPlayer]
 * call - resource-agnostic on purpose, so the event-to-sample mapping stays testable off the JVM
 * without pulling in an `R.raw` reference.
 */
enum class SoundSample {
    Tap,
    TargetCleared,
    OperationSuccess,
    OperationMiss,
    LifeLost,
    LevelUp,
}
