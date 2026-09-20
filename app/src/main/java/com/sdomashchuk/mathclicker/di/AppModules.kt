package com.sdomashchuk.mathclicker.di

/**
 * The module list the application registers, named once so a test can assert against the same
 * list the app actually starts with — a module registered in one place and not the other is a
 * crash on first screen, not a compile error.
 */
val appModules = listOf(dataModule, gameModule, viewModelModule)
