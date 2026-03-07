package com.example.linkforge.bottombar

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.linkforge.Screens.HomeScreen
import com.example.linkforge.Screens.ProfileScreen

@Composable
fun NavigationGraph(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Analysis.route) { AnalysisScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}


@Composable
private fun ChatScreen() {
    Text("Chats")
}

@Composable
private fun AnalysisScreen() {
    Text("Analysis")
}

