package com.personal.game2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.screens.GameScreen
import com.personal.game2048.screens.HomeScreen
import com.personal.game2048.screens.StatsScreen
import com.personal.game2048.ui.theme.Game2048Theme

class MainActivity : ComponentActivity() {

    private var _isDarkTheme = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("g2048_settings", MODE_PRIVATE)
        _isDarkTheme = prefs.getBoolean("g2048_theme_dark", true)

        setContent {
            var isDarkTheme by remember { mutableStateOf(_isDarkTheme) }
            val navController = rememberNavController()
            val gameViewModel: GameViewModel = viewModel()

            fun toggleTheme() {
                isDarkTheme = !isDarkTheme
                prefs.edit().putBoolean("g2048_theme_dark", isDarkTheme).apply()
            }

            Game2048Theme(isDarkTheme = isDarkTheme) {
                NavHost(navController = navController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { toggleTheme() },
                            onNewGame = {
                                gameViewModel.startNewGame()
                                navController.navigate("game") {
                                    popUpTo("home")
                                }
                            },
                            onContinueGame = {
                                gameViewModel.continueGame()
                                navController.navigate("game") {
                                    popUpTo("home")
                                }
                            },
                            onStats = {
                                navController.navigate("stats")
                            }
                        )
                    }

                    composable("game") {
                        BackHandler {
                            gameViewModel.saveOnExit()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                        GameScreen(
                            viewModel = gameViewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { toggleTheme() },
                            onNavigateToStats = {
                                navController.navigate("stats")
                            },
                            onNavigateHome = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("stats") {
                        StatsScreen(
                            isDarkTheme = isDarkTheme,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
