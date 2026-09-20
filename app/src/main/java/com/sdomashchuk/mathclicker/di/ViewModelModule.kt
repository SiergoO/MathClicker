package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.presentation.game.GameViewModel
import com.sdomashchuk.mathclicker.presentation.menu.MenuViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { MenuViewModel() }
        viewModel { GameViewModel(get(), get()) }
    }
