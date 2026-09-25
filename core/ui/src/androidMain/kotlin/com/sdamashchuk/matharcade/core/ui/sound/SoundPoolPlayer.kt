package com.sdamashchuk.matharcade.core.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.sdamashchuk.matharcade.core.ui.R
import com.sdamashchuk.matharcade.core.ui.sound.model.SoundSample

// Eight, not four: the MC-78 samples carry a room tail, so a tap occupies a stream for 460ms
// rather than 90ms and the level-up runs 1.2s. Four voices started stealing each other back.
private const val MAX_STREAMS = 8
private const val VOLUME = 1f
private const val PRIORITY = 1
private const val NO_LOOP = 0
private const val PLAYBACK_RATE = 1f

/**
 * SoundPool, not MediaPlayer: every sample here is well under two seconds and can overlap
 * itself (the tap fires several times a second), which is exactly what SoundPool's low-latency,
 * pre-decoded playback is for. All six samples load eagerly in the constructor - callers create
 * one of these per game session (see GameComponent) and [release] it on the way out, rather than
 * keeping a single instance alive for the app's lifetime.
 */
class SoundPoolPlayer(
    context: Context,
) : SoundPlayer {
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()

    private val soundIds: Map<SoundSample, Int> =
        SoundSample.entries.associateWith { sample -> pool.load(context, sample.rawResId, 1) }

    override fun play(sample: SoundSample) {
        val id = soundIds[sample] ?: return
        pool.play(id, VOLUME, VOLUME, PRIORITY, NO_LOOP, PLAYBACK_RATE)
    }

    override fun release() = pool.release()
}

private val SoundSample.rawResId: Int
    get() =
        when (this) {
            SoundSample.Tap -> R.raw.sfx_tap
            SoundSample.TargetCleared -> R.raw.sfx_target_cleared
            SoundSample.OperationSuccess -> R.raw.sfx_operation_success
            SoundSample.OperationMiss -> R.raw.sfx_operation_miss
            SoundSample.LifeLost -> R.raw.sfx_life_lost
            SoundSample.LevelUp -> R.raw.sfx_level_up
        }
