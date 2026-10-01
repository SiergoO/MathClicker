package com.sdamashchuk.mathbubbles.core.game.di

import com.sdamashchuk.mathbubbles.core.game.Game
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelper
import com.sdamashchuk.mathbubbles.core.game.helper.SessionHelperImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module

val gameModule =
    module {
        // SessionHelperImpl and Game each default their own Random rather than share one instance.
        // Both are still called from two threads - the caller's, and Game's collector coroutine on
        // Dispatchers.Default (createTargets, shortenAppearanceDelay) - but MC-32 gave Game an
        // internal Mutex that every mutating call, including every sessionHelper call it makes,
        // now runs inside; SessionHelperImpl has no caller outside Game, so its draws are
        // serialised transitively. Separating the two Random instances no longer buys thread
        // safety, it never needed to - it buys that each consumer's draw sequence depends only on
        // its own call count, not on how many draws the other consumer made first, which is the
        // property a seeded, reproducible session actually needs.
        single { SessionHelperImpl() } bind SessionHelper::class
        // Game owns a flow collector that only runs once start() is called; a factory would hand
        // out un-started instances, so this must stay a single.
        // TODO(MC-115): pass boostersEnabled = true once the booster UI lands.
        single { Game(get(), CoroutineScope(Dispatchers.Default)).apply { start() } }
    }
