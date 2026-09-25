package com.sdamashchuk.matharcade.di

import com.sdamashchuk.matharcade.core.database.di.databaseModule
import com.sdamashchuk.matharcade.core.game.di.gameModule
import com.sdamashchuk.matharcade.core.ui.sound.di.soundModule

/**
 * The module list the application registers, named once so a test can assert against the same
 * list the app actually starts with — a module registered in one place and not the other is a
 * crash on first screen, not a compile error.
 */
val appModules = listOf(sqlDriverModule, databaseModule, gameModule, soundModule)
