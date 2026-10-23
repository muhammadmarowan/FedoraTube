package com.fedoratube.org.ui

import androidx.compose.runtime.MutableState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fedoratube.org.data.movies.MovieDetails
import com.fedoratube.org.data.movies.MovieEpisode
import com.fedoratube.org.data.movies.MovieStream
import com.fedoratube.org.ui.screens.movies.AddonsSettingsScreen
import com.fedoratube.org.ui.screens.movies.MovieDetailsScreen
import com.fedoratube.org.ui.screens.movies.MoviePlayerScreen
import com.fedoratube.org.ui.screens.movies.MovieSearchScreen
import com.fedoratube.org.ui.screens.movies.MoviesScreen

object MoviePlaybackHolder {
    var stream: MovieStream? = null
    var streamUrl: String = ""
    var isTorrent: Boolean = false
    var magnetUri: String? = null
    var fileIdx: Int? = null
    var title: String = ""
    var subtitle: String? = null
    var poster: String? = null
    var headers: Map<String, String>? = null
    var availableStreams: List<MovieStream> = emptyList()
    var episodes: List<MovieEpisode> = emptyList()
    var currentEpisode: MovieEpisode? = null
    var movieDetails: MovieDetails? = null
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
            onBack = {
                navController.popBackStack()
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
            onPlayStream = { stream, movie, episode, allStreams ->
                MoviePlaybackHolder.stream = stream
                MoviePlaybackHolder.isTorrent = stream.isTorrent
                MoviePlaybackHolder.magnetUri = stream.getMagnetUri(movie.name)
                MoviePlaybackHolder.fileIdx = stream.fileIdx
                MoviePlaybackHolder.streamUrl = stream.playbackUrl ?: ""
                MoviePlaybackHolder.title = if (episode != null) "${movie.name} - S${episode.season ?: 1}E${episode.episode ?: 1} ${episode.title}" else movie.name
                MoviePlaybackHolder.subtitle = stream.displayTitle
                MoviePlaybackHolder.poster = episode?.thumbnail ?: movie.poster
                MoviePlaybackHolder.headers = stream.proxyHeaders
                MoviePlaybackHolder.availableStreams = allStreams
                MoviePlaybackHolder.episodes = movie.videos
                MoviePlaybackHolder.currentEpisode = episode
                MoviePlaybackHolder.movieDetails = movie
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
            initialStreamUrl = MoviePlaybackHolder.streamUrl,
            initialIsTorrent = MoviePlaybackHolder.isTorrent,
            initialMagnetUri = MoviePlaybackHolder.magnetUri,
            initialFileIdx = MoviePlaybackHolder.fileIdx,
            title = MoviePlaybackHolder.title,
            subtitle = MoviePlaybackHolder.subtitle,
            poster = MoviePlaybackHolder.poster,
            headers = MoviePlaybackHolder.headers,
            availableStreams = MoviePlaybackHolder.availableStreams,
            episodes = MoviePlaybackHolder.episodes,
            currentEpisode = MoviePlaybackHolder.currentEpisode,
            movieDetails = MoviePlaybackHolder.movieDetails,
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

