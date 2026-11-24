package com.personal.game2048

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.screens.AchievementsScreen
import com.personal.game2048.screens.Dest
import com.personal.game2048.screens.GameScreen
import com.personal.game2048.screens.GoalsScreen
import com.personal.game2048.screens.HomeScreen
import com.personal.game2048.screens.LeaderboardScreen
import com.personal.game2048.screens.RanksScreen
import com.personal.game2048.screens.SettingsScreen
import com.personal.game2048.screens.StatsScreen
import com.personal.game2048.ui.theme.App
import com.personal.game2048.ui.theme.AppTheme
import com.personal.game2048.ui.theme.isDark

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestRefreshRate()

        setContent {
            val settings by vm.store.settings.collectAsState()
            val dark = isDark(settings)
            LaunchedEffect(dark) {
                val clear = android.graphics.Color.TRANSPARENT
                val style = if (dark) SystemBarStyle.dark(clear) else SystemBarStyle.light(clear, 0x66000000)
                enableEdgeToEdge(style, style)
            }
            AppTheme(settings) {
                val view = LocalView.current
                DisposableEffect(view) {
                    vm.feedback.view = view
                    onDispose { if (vm.feedback.view === view) vm.feedback.view = null }
                }
                Box(Modifier.fillMaxSize().background(App.colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)) { Navigation() }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Navigation() {
        val nav = rememberNavController()
        val go: (Dest) -> Unit = { nav.navigate(it.route) { launchSingleTop = true } }
        val back: () -> Unit = { if (!nav.popBackStack()) nav.navigate(Dest.HOME.route) }
        val play: (GameMode, Boolean) -> Unit = { mode, fresh ->
            vm.open(mode, fresh)
            go(Dest.GAME)
        }
        NavHost(
            nav, Dest.HOME.route,
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 10 } },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(140)) + slideOutHorizontally(tween(220)) { it / 10 } },
        ) {
            composable(Dest.HOME.route) { HomeScreen(vm, play, go) }
            composable(Dest.GAME.route) {
                // the process was recreated straight into the game screen
                LaunchedEffect(Unit) { vm.ensureOpen() }
                val home: () -> Unit = {
                    vm.leave()
                    nav.popBackStack(Dest.HOME.route, inclusive = false)
                }
                BackHandler(onBack = home)
                GameScreen(
                    vm, onHome = home,
                    onStats = { vm.onPause(); go(Dest.STATS) },
                    onRanks = { vm.onPause(); go(Dest.LEADERBOARD) },
                )
            }
            composable(Dest.LEADERBOARD.route) { LeaderboardScreen(vm, back) }
            composable(Dest.STATS.route) { StatsScreen(vm, back) }
            composable(Dest.ACHIEVEMENTS.route) { AchievementsScreen(vm, back) }
            composable(Dest.GOALS.route) { GoalsScreen(vm, back) }
            composable(Dest.RANKS.route) { RanksScreen(vm, back) }
            composable(Dest.SETTINGS.route) { SettingsScreen(vm, back) }
        }
    }

    /** Asks for the panel's fastest mode at the current resolution, so 90/120 Hz phones use it. */
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= 30) display else @Suppress("DEPRECATION") windowManager.defaultDisplay
        val current = display?.mode ?: return
        val fastest = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        if (fastest.refreshRate > current.refreshRate + 1f) {
            window.attributes = window.attributes.apply { preferredDisplayModeId = fastest.modeId }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.store.flush()
    }
}
