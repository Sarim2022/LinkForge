package com.example.linkforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.linkforge.Screens.AuthScreen
import com.example.linkforge.Screens.SplashScreen
import com.example.linkforge.MainScreen
import com.example.linkforge.data.UserPreferences
import com.example.linkforge.ui.theme.LinkForgeTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

private const val SPLASH_ROUTE = "splash"
private const val AUTH_ROUTE = "auth"
private const val HOME_ROUTE = "home"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LinkForgeTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = SPLASH_ROUTE
                    ) {
                        composable(SPLASH_ROUTE) {
                            SplashScreen()
                            LaunchedEffect(Unit) {
                                delay(1500)
                                val destination = if (UserPreferences(context).getUser() != null) {
                                    HOME_ROUTE
                                } else {
                                    AUTH_ROUTE
                                }
                                navController.navigate(destination) {
                                    popUpTo(SPLASH_ROUTE) { inclusive = true }
                                }
                            }
                        }
                        composable(
                            AUTH_ROUTE,
                            enterTransition = {
                                fadeIn(animationSpec = tween(700)) +
                                    scaleIn(
                                        initialScale = 0.9f,
                                        animationSpec = tween(700)
                                    )
                            }
                        ) {
                            AuthScreen(
                                onNavigateToHome = {
                                    navController.navigate(HOME_ROUTE) {
                                        popUpTo(AUTH_ROUTE) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(HOME_ROUTE) {
                            MainScreen(
                                onLogout = {
                                    Firebase.auth.signOut()
                                    UserPreferences(context).clearUser()
                                    navController.navigate(AUTH_ROUTE) {
                                        popUpTo(HOME_ROUTE) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
