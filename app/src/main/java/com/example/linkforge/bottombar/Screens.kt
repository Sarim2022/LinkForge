package com.example.linkforge.bottombar

import com.example.linkforge.R


sealed class Screen(val route: String, val icon: Int) {
    object Home : Screen("home", R.drawable.home)
    object Chat : Screen("chat", R.drawable.chat)
    object Analysis : Screen("analysis", R.drawable.chart)
    object Profile : Screen("profile", R.drawable.setting)
}

val items = listOf(
    Screen.Home,
    Screen.Chat,
    Screen.Analysis,
    Screen.Profile
)