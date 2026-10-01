package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettings

class FakeSoundSettings(
    private var soundEnabled: Boolean,
) : SoundSettings {
    override fun isSoundEnabled(): Boolean = soundEnabled

    override fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }
}
