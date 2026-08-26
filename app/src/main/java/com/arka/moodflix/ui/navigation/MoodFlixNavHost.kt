package com.arka.moodflix.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arka.moodflix.ui.detail.DetailScreen
import com.arka.moodflix.ui.discover.DiscoverScreen
import com.arka.moodflix.ui.settings.SettingsScreen

object Routes {
    const val DISCOVER = "discover"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{movieId}"

    fun detail(movieId: Int) = "detail/$movieId"
}

@Composable
fun MoodFlixNavHost(
    onOpenUrl: (String) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DISCOVER,
        enterTransition = {
            slideInHorizontally(tween(280)) { it / 6 } + fadeIn(tween(280))
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = {
            slideOutHorizontally(tween(240)) { it / 6 } + fadeOut(tween(200))
        }
    ) {

        composable(Routes.DISCOVER) {
            DiscoverScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenMovie = { id -> navController.navigate(Routes.detail(id)) },
                onPlayTrailer = { key -> onOpenUrl("https://www.youtube.com/watch?v=$key") }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenUrl = onOpenUrl
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
                onPlayTrailer = { key -> onOpenUrl("https://www.youtube.com/watch?v=$key") },
                onOpenUrl = onOpenUrl
            )
        }
    }
}
