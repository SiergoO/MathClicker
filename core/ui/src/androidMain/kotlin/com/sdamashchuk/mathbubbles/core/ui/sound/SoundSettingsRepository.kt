package com.sdamashchuk.mathbubbles.core.ui.sound

import android.content.Context

private const val PREFS_NAME = "sound_settings"
private const val KEY_SOUND_ENABLED = "sound_enabled"

/**
 * SharedPreferences, not SQLDelight: one boolean does not earn a table, and this is the same
 * mechanism `:core:database` was replaced away from for the game's own state, not for a single
 * on/off flag with no schema to migrate.
 */
class SoundSettingsRepository(
    context: Context,
) : SoundSettings {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    override fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }
}
