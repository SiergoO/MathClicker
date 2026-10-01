package com.sdamashchuk.mathbubbles.core.ui.sound

import android.content.Context
import android.media.AudioManager

/**
 * Anything other than RINGER_MODE_NORMAL counts as silent - vibrate included, not only the
 * silent icon, because a sample firing several times a second during vibrate mode would be
 * exactly the buzzing the mode exists to prevent.
 */
class DeviceSilentModeChecker(
    private val context: Context,
) : SilentModeChecker {
    override fun isSilent(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL
    }
}
