package com.arka.moodflix.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import com.arka.moodflix.ui.search.SearchScreen
import com.arka.moodflix.ui.settings.SettingsScreen
import com.arka.moodflix.ui.splash.SplashScreen
import com.arka.moodflix.ui.watchlist.WatchlistScreen

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val DISCOVER = "discover"
    const val SEARCH = "search"
    const val WATCHLIST = "watchlist"
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

// Tabs that show the bottom nav bar. Every other route (splash, login,
// results, detail, settings) is a full-screen destination reached from one
// of these two.
private val bottomNavRoutes = setOf(Routes.DISCOVER, Routes.SEARCH, Routes.WATCHLIST)

@Composable
fun MoodFlixNavHost(
    onOpenUrl: (String) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val currentRoute by navController.currentBackStackEntryAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Every screen already manages its own system-bar insets (own
        // TopAppBar, or edge-to-edge by design for splash/login) - this outer
        // Scaffold should only reserve space for the bottom nav bar it draws,
        // not double up on status/navigation-bar insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute?.destination?.route in bottomNavRoutes) {
                MoodFlixBottomBar(
                    currentRoute = currentRoute?.destination?.route,
                    onTabSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
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

        composable(Routes.SEARCH) {
            SearchScreen(
                onOpenMovie = { id, mediaType ->
                    navController.navigate(Routes.detail(id, mediaType))
                }
            )
        }

        composable(Routes.WATCHLIST) {
            WatchlistScreen(
                onOpenMovie = { id, mediaType ->
                    navController.navigate(Routes.detail(id, mediaType))
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
                onOpenMovie = { id, mediaType ->
                    navController.navigate(Routes.detail(id, mediaType))
                },
                onPlayTrailer = { key -> onOpenUrl("https://www.youtube.com/watch?v=$key") },
                onOpenUrl = onOpenUrl
            )
        }
        }
    }
}

@Composable
private fun MoodFlixBottomBar(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = currentRoute == Routes.DISCOVER,
            onClick = { onTabSelected(Routes.DISCOVER) },
            icon = { Icon(Icons.Filled.Explore, contentDescription = null) },
            label = { Text("Discover") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == Routes.SEARCH,
            onClick = { onTabSelected(Routes.SEARCH) },
            icon = { Icon(Icons.Filled.Search, contentDescription = null) },
            label = { Text("Search") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationBarItem(
            selected = currentRoute == Routes.WATCHLIST,
            onClick = { onTabSelected(Routes.WATCHLIST) },
            icon = { Icon(Icons.Filled.Bookmark, contentDescription = null) },
            label = { Text("Watchlist") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}