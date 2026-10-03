package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.ui.sound.SoundPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.model.SoundSample

class RecordingSoundPlayer : SoundPlayer {
    val played = mutableListOf<SoundSample>()

    override fun play(sample: SoundSample) {
        played += sample
    }

    override fun release() = Unit
}
