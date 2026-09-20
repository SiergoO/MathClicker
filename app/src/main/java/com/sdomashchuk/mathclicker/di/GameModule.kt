package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.game.Game
import com.sdomashchuk.mathclicker.game.helper.SessionHelper
import com.sdomashchuk.mathclicker.game.helper.SessionHelperImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.bind
import org.koin.dsl.module

val gameModule =
    module {
        single { SessionHelperImpl() } bind SessionHelper::class
        // Game owns a flow collector that only runs once start() is called; a factory would hand
        // out un-started instances, so this must stay a single.
        single { Game(get(), CoroutineScope(Dispatchers.Default)).apply { start() } }
    }
