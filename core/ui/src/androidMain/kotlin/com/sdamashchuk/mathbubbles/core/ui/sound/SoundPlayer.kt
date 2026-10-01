package com.sdamashchuk.mathbubbles.core.ui.sound

import com.sdamashchuk.mathbubbles.core.ui.sound.model.SoundSample

/**
 * The raw playback seam - [SoundEventPlayer] is what decides whether a sample should sound at
 * all; this only knows how to make one happen once that decision is already made.
 */
interface SoundPlayer {
    fun play(sample: SoundSample)

    fun release()
}
