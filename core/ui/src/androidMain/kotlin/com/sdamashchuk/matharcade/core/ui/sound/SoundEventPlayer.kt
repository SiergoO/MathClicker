package com.sdamashchuk.matharcade.core.ui.sound

import com.sdamashchuk.matharcade.core.ui.sound.model.SoundSample

/**
 * The single gate every sample passes through - the mute switch and the device's silent mode both
 * silence everything, including the tap, so neither check lives inside [SoundPlayer] where a
 * future caller could bypass one but not the other.
 */
class SoundEventPlayer(
    private val player: SoundPlayer,
    private val settings: SoundSettings,
    private val silentMode: SilentModeChecker,
) {
    fun play(sample: SoundSample) {
        if (!settings.isSoundEnabled() || silentMode.isSilent()) return
        player.play(sample)
    }

    fun release() = player.release()
}
