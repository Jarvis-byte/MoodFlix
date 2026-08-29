package com.arka.moodflix.ui.navigation

import android.net.Uri
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
import com.arka.moodflix.domain.model.Genre
import com.arka.moodflix.domain.model.MediaType
import com.arka.moodflix.domain.model.MediaTypeFilter
import com.arka.moodflix.domain.model.Mood
import com.arka.moodflix.ui.auth.LoginScreen
import com.arka.moodflix.ui.detail.DetailScreen
import com.arka.moodflix.ui.discover.DiscoverScreen
import com.arka.moodflix.ui.results.ResultsScreen
import com.arka.moodflix.ui.settings.SettingsScreen
import com.arka.moodflix.ui.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DISCOVER = "discover"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{movieId}/{mediaType}"
    const val RESULTS = "results/{mood}/{genre}/{minRating}/{freeText}/{providers}/{mediaFilter}"

    // A blank freeText segment breaks route matching (double slash), so an
    // empty string is encoded as this sentinel instead of Uri.encode("").
    private const val BLANK_TEXT = "_blank_"
    private const val NO_PROVIDERS = "none"

    fun detail(movieId: Int, mediaType: MediaType) = "detail/$movieId/${mediaType.name}"

    fun results(
        mood: Mood,
        genre: Genre,
        minRating: Float,
        freeText: String,
        providerIds: List<Int>,
        mediaFilter: MediaTypeFilter
    ): String {
        val encodedText = if (freeText.isBlank()) BLANK_TEXT else Uri.encode(freeText)
        val providerSegment = if (providerIds.isEmpty()) {
            NO_PROVIDERS
        } else {
            providerIds.joinToString(",")
        }
        return "results/${mood.name}/${genre.name}/$minRating/$encodedText/" +
                "$providerSegment/${mediaFilter.name}"
    }

    fun decodeFreeText(raw: String): String =
        if (raw == BLANK_TEXT) "" else Uri.decode(raw)

    fun decodeProviderIds(raw: String): List<Int> =
        if (raw == NO_PROVIDERS) emptyList() else raw.split(",").mapNotNull { it.toIntOrNull() }
}

@Composable
fun MoodFlixNavHost(
    onOpenUrl: (String) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = {
            slideInHorizontally(tween(280)) { it / 6 } + fadeIn(tween(280))
        },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(200)) },
        popExitTransition = {
            slideOutHorizontally(tween(240)) { it / 6 } + fadeOut(tween(200))
        }
    ) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToDiscover = {
                    navController.navigate(Routes.DISCOVER) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.DISCOVER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.DISCOVER) {
            DiscoverScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onSearch = { mood, genre, minRating, freeText, providerIds, mediaFilter ->
                    navController.navigate(
                        Routes.results(mood, genre, minRating, freeText, providerIds, mediaFilter)
                    )
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenUrl = onOpenUrl,
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.RESULTS,
            arguments = listOf(
                navArgument("mood") { type = NavType.StringType },
                navArgument("genre") { type = NavType.StringType },
                navArgument("minRating") { type = NavType.FloatType },
                navArgument("freeText") { type = NavType.StringType },
                navArgument("providers") { type = NavType.StringType },
                navArgument("mediaFilter") { type = NavType.StringType }
            )
        ) {
            ResultsScreen(
                onBack = { navController.popBackStack() },
                onOpenMovie = { id, mediaType ->
                    navController.navigate(Routes.detail(id, mediaType))
                },
                onPlayTrailer = { key -> onOpenUrl("https://www.youtube.com/watch?v=$key") }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(
                navArgument("movieId") { type = NavType.IntType },
                navArgument("mediaType") { type = NavType.StringType }
            )
        ) {
            DetailScreen(
                onBack = { navController.popBackStack() },
                onPlayTrailer = { key -> onOpenUrl("https://www.youtube.com/watch?v=$key") },
                onOpenUrl = onOpenUrl
            )
        }
    }
}