package com.fedoratube.org.ui

import androidx.compose.runtime.MutableState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fedoratube.org.ui.screens.movies.AddonsSettingsScreen
import com.fedoratube.org.ui.screens.movies.MovieDetailsScreen
import com.fedoratube.org.ui.screens.movies.MoviePlayerScreen
import com.fedoratube.org.ui.screens.movies.MovieSearchScreen
import com.fedoratube.org.ui.screens.movies.MoviesScreen
import java.net.URLDecoder
import java.net.URLEncoder

object MoviePlaybackHolder {
    var streamUrl: String = ""
    var title: String = ""
    var subtitle: String? = null
    var poster: String? = null
    var headers: Map<String, String>? = null
}

internal fun NavGraphBuilder.moviesRoutes(
    navController: NavHostController,
    currentRoute: MutableState<String>,
) {
    composable("movies") {
        currentRoute.value = "movies"
        MoviesScreen(
            onMovieClick = { type, id ->
                navController.navigate("movie_detail/$type/$id")
            },
            onSearchClick = {
                navController.navigate("movie_search")
            },
            onAddonsClick = {
                navController.navigate("addons_settings")
            },
        )
    }

    composable("movie_search") {
        currentRoute.value = "movie_search"
        MovieSearchScreen(
            onMovieClick = { type, id ->
                navController.navigate("movie_detail/$type/$id")
            },
            onBack = {
                navController.popBackStack()
            },
        )
    }

    composable(
        route = "movie_detail/{type}/{id}",
        arguments = listOf(
            navArgument("type") { type = NavType.StringType },
            navArgument("id") { type = NavType.StringType },
        ),
    ) { backStackEntry ->
        currentRoute.value = "movie_detail"
        val type = backStackEntry.arguments?.getString("type") ?: "movie"
        val id = backStackEntry.arguments?.getString("id") ?: ""

        MovieDetailsScreen(
            type = type,
            id = id,
            onPlayStream = { url, movieTitle, streamSub, moviePoster, streamHeaders ->
                MoviePlaybackHolder.streamUrl = url
                MoviePlaybackHolder.title = movieTitle
                MoviePlaybackHolder.subtitle = streamSub
                MoviePlaybackHolder.poster = moviePoster
                MoviePlaybackHolder.headers = streamHeaders
                navController.navigate("movie_player")
            },
            onBack = {
                navController.popBackStack()
            },
        )
    }

    composable("movie_player") {
        currentRoute.value = "movie_player"
        MoviePlayerScreen(
            streamUrl = MoviePlaybackHolder.streamUrl,
            title = MoviePlaybackHolder.title,
            subtitle = MoviePlaybackHolder.subtitle,
            poster = MoviePlaybackHolder.poster,
            headers = MoviePlaybackHolder.headers,
            onBack = {
                navController.popBackStack()
            },
        )
    }

    composable("addons_settings") {
        currentRoute.value = "addons_settings"
        AddonsSettingsScreen(
            onBack = {
                navController.popBackStack()
            },
        )
    }
}

