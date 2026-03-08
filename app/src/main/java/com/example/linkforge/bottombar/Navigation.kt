package com.example.linkforge.bottombar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.linkforge.Screens.ChatScreen
import com.example.linkforge.Screens.HomeScreen
import com.example.linkforge.Screens.ProfileScreen

@Composable
fun NavigationGraph(
    navController: NavHostController,
    onLogout: () -> Unit = {},
    clearHomeSubScreen: Int = 0
) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(onLogout = onLogout, clearHomeSubScreen = clearHomeSubScreen)
        }
        composable(Screen.Chat.route) { ChatScreen(onBack = {}) }
        composable(Screen.Analysis.route) { AnalysisScreen() }
        composable(Screen.Profile.route) { ProfileScreen() }
    }
}


@Composable
private fun AnalysisScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Welcome to SplitWise",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}

