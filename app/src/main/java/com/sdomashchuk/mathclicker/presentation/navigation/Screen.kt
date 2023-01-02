package com.sdomashchuk.mathclicker.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("route_splash_screen")
    object Menu : Screen("route_menu_screen")
    object Game : Screen("route_game_screen")
}