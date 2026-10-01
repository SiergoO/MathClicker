package com.sdamashchuk.mathbubbles.core.ui.sound.di

import android.content.Context
import com.sdamashchuk.mathbubbles.core.ui.sound.DeviceSilentModeChecker
import com.sdamashchuk.mathbubbles.core.ui.sound.SilentModeChecker
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundEventPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundPoolPlayer
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettings
import com.sdamashchuk.mathbubbles.core.ui.sound.SoundSettingsRepository
import org.koin.dsl.module

// SoundPlayer and SoundEventPlayer are factories, not singles: each wraps a SoundPool that must be
// released when the game screen is left (see GameComponent), so a process-lifetime singleton here
// would either leak across sessions or hand the second session an already-released pool.
val soundModule =
    module {
        single<SoundSettings> { SoundSettingsRepository(get<Context>()) }
        single<SilentModeChecker> { DeviceSilentModeChecker(get<Context>()) }
        factory<SoundPlayer> { SoundPoolPlayer(get<Context>()) }
        factory { SoundEventPlayer(get<SoundPlayer>(), get<SoundSettings>(), get<SilentModeChecker>()) }
    }
