package com.sdomashchuk.mathclicker.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("route_splash_screen")
    object Menu : Screen("route_menu_screen")
    object Main : Screen("route_main_screen")
}