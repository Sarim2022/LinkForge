package com.example.linkforge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.rememberNavController
import com.example.linkforge.bottombar.MyBottomBar
import com.example.linkforge.bottombar.NavigationGraph

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val myGrey = Color(0xFFF5F5F5) // Your requested color

    Scaffold(
        modifier = Modifier.fillMaxSize(), // Ensures the scaffold fills the whole screen
        containerColor = myGrey,
        bottomBar = {
            // Apply navigationBarsPadding to keep it above the system pill
            Box(modifier = Modifier.navigationBarsPadding()) {
                MyBottomBar(navController)
            }
        }
    ) { innerPadding ->
        // Use ONLY bottom padding here so content can flow behind status bar
        Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
            NavigationGraph(navController)
        }
    }
}