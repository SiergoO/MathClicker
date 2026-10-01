package com.sdamashchuk.mathbubbles.core.ui.sound

/**
 * The one switch the settings screen exposes - on or off, nothing per-sample. Reads are
 * synchronous: [SoundEventPlayer] checks this on every sample, so a suspend/Flow round-trip would
 * cost more than the SharedPreferences-backed implementation ever does.
 */
interface SoundSettings {
    fun isSoundEnabled(): Boolean

    fun setSoundEnabled(enabled: Boolean)
}
