package com.sdamashchuk.mathbubbles.core.ui.sound

import com.sdamashchuk.mathbubbles.core.ui.sound.model.SoundSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSoundPlayer : SoundPlayer {
    val played = mutableListOf<SoundSample>()
    var released = false

    override fun play(sample: SoundSample) {
        played += sample
    }

    override fun release() {
        released = true
    }
}

private class FakeSoundSettings(
    private var enabled: Boolean,
) : SoundSettings {
    override fun isSoundEnabled() = enabled

    override fun setSoundEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

private class FakeSilentModeChecker(
    private val silent: Boolean,
) : SilentModeChecker {
    override fun isSilent() = silent
}

class SoundEventPlayerTest {
    @Test
    fun `an enabled switch and a non-silent device let the sample through`() {
        val player = FakeSoundPlayer()
        val eventPlayer = SoundEventPlayer(player, FakeSoundSettings(true), FakeSilentModeChecker(false))

        eventPlayer.play(SoundSample.Tap)

        assertEquals(listOf(SoundSample.Tap), player.played)
    }

    // M1: the mute switch ignored, so samples play while muted.
    @Test
    fun `a muted switch silences every sample even when the device is not silent`() {
        val player = FakeSoundPlayer()
        val eventPlayer = SoundEventPlayer(player, FakeSoundSettings(false), FakeSilentModeChecker(false))

        eventPlayer.play(SoundSample.Tap)
        eventPlayer.play(SoundSample.LevelUp)

        assertTrue(player.played.isEmpty())
    }

    // M2: silent mode ignored, so samples play with the ringer off.
    @Test
    fun `silent mode silences every sample even when the switch is on`() {
        val player = FakeSoundPlayer()
        val eventPlayer = SoundEventPlayer(player, FakeSoundSettings(true), FakeSilentModeChecker(true))

        eventPlayer.play(SoundSample.Tap)
        eventPlayer.play(SoundSample.LevelUp)

        assertTrue(player.played.isEmpty())
    }

    // M4: release() removed so the pool leaks - the underlying player must see release(), not just
    // SoundEventPlayer itself.
    @Test
    fun `releasing the event player releases the underlying player`() {
        val player = FakeSoundPlayer()
        val eventPlayer = SoundEventPlayer(player, FakeSoundSettings(true), FakeSilentModeChecker(false))

        eventPlayer.release()

        assertTrue(player.released)
    }
}
