package com.sdamashchuk.mathbubbles.di

import com.sdamashchuk.mathbubbles.core.database.di.databaseModule
import com.sdamashchuk.mathbubbles.core.game.di.gameModule
import com.sdamashchuk.mathbubbles.core.ui.sound.di.soundModule

/**
 * The module list the application registers, named once so a test can assert against the same
 * list the app actually starts with — a module registered in one place and not the other is a
 * crash on first screen, not a compile error.
 */
val appModules = listOf(loggerModule, sqlDriverModule, databaseModule, gameModule, soundModule)
