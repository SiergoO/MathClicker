package com.sdomashchuk.mathclicker.core.game.di

import com.sdomashchuk.mathclicker.core.game.Game
import com.sdomashchuk.mathclicker.core.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.core.game.helper.SessionHelperImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module

val gameModule =
    module {
        // SessionHelperImpl and Game each default their own Random rather than share one instance.
        // Both are already called from two threads - the caller's, and Game's collector coroutine
        // on Dispatchers.Default (createTargets, shortenAppearanceDelay) - so separating them does
        // not make either Random thread-safe; that unsynchronized-mutation problem is bug 1 and is
        // untouched here. What separating them buys is that each consumer's draw sequence depends
        // only on its own call count, not on how many draws the other consumer made first - the
        // property a seeded, reproducible session actually needs.
        single { SessionHelperImpl() } bind SessionHelper::class
        // Game owns a flow collector that only runs once start() is called; a factory would hand
        // out un-started instances, so this must stay a single.
        single { Game(get(), CoroutineScope(Dispatchers.Default)).apply { start() } }
    }
