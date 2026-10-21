package com.fedoratube.org.ui

import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fedoratube.org.data.local.PlaylistRepository
import com.fedoratube.org.data.localmedia.LocalMediaIds
import com.fedoratube.org.data.localmedia.toMusicTrack
import com.fedoratube.org.data.localmedia.toVideo
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.music.model.toMusicTrack
import com.fedoratube.org.data.music.model.toVideo
import com.fedoratube.org.data.shorts.queue.ShortsQueueSource
import com.fedoratube.org.player.GlobalPlayerState
import com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets
import com.fedoratube.org.ui.components.layout.navigation.MediaNavigator
import com.fedoratube.org.ui.components.musicplayer.sheet.MusicPlayerSheetState
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.ui.components.settings.SettingsTarget
import com.fedoratube.org.ui.components.videoplayer.PlayerDraggableState
import com.fedoratube.org.ui.screens.channel.ChannelScreen
import com.fedoratube.org.ui.screens.equalizer.EqualizerScreen
import com.fedoratube.org.ui.screens.history.HistoryScreen
import com.fedoratube.org.ui.screens.home.HomeScreen
import com.fedoratube.org.ui.screens.home.HomeViewModel
import com.fedoratube.org.ui.screens.library.LibraryScreen
import com.fedoratube.org.ui.screens.music.sharedMusicPlayerViewModel
import com.fedoratube.org.ui.screens.notifications.NotificationScreen
import com.fedoratube.org.ui.screens.onboarding.OnboardingScreen
import com.fedoratube.org.ui.screens.player.VideoPlayerViewModel
import com.fedoratube.org.ui.screens.player.state.VideoPlayerUiState
import com.fedoratube.org.ui.screens.playlists.PlaylistDetailScreen
import com.fedoratube.org.ui.screens.playlists.PlaylistsScreen
import com.fedoratube.org.ui.screens.recap.RecapRoutes
import com.fedoratube.org.ui.screens.recap.RecapScreen
import com.fedoratube.org.ui.screens.recap.story.RecapStoryScreen
import com.fedoratube.org.ui.screens.search.SearchScreen
import com.fedoratube.org.ui.screens.settings.SettingsHost
import com.fedoratube.org.ui.screens.shorts.ShortsScreen
import com.fedoratube.org.ui.screens.subscriptions.SubscriptionsScreen
import com.fedoratube.org.ui.screens.update.UPDATE_ROUTE
import com.fedoratube.org.ui.screens.update.UpdateScreen
import kotlinx.coroutines.flow.first

@UnstableApi
fun NavGraphBuilder.flowAppGraph(
    navController: NavHostController,
    mediaNavigator: MediaNavigator,
    currentRoute: MutableState<String>,
    playerSheetState: PlayerDraggableState,
    musicPlayerSheetState: MusicPlayerSheetState,
    homeViewModel: HomeViewModel,
    playerViewModel: VideoPlayerViewModel,
    playerUiStateResult: State<VideoPlayerUiState>,
    playerVisibleState: MutableState<Boolean>,
    disableShortsPlayer: Boolean = false,
    defaultStartRoute: String = "home",
    onMusicStarted: () -> Unit = {},
) {
    // =============================================
    // ONBOARDING (First-time user experience)
    // =============================================
    composable("onboarding") {
        currentRoute.value = "onboarding"
        OnboardingScreen(
            onComplete = {
                // Navigate to the selected default tab and clear the backstack so user can't go back to onboarding
                navController.navigate(defaultStartRoute) {
                    popUpTo("onboarding") { inclusive = true }
                }
            },
        )
    }

    composable("home") {
        currentRoute.value = "home"
        HomeScreen(
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) {
                    playerViewModel.playVideo(it)
                    GlobalPlayerState.setCurrentVideo(it)
                }
            },
            onShortClick = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onSearchClick = {
                navController.navigate("search")
            },
            onNavigateToSettings = {
                navController.navigate("settings")
            },
            onNavigateToMusic = {
                navController.navigate("music")
            },
            onNavigateToDownloads = {
                navController.navigate("downloads")
            },
            onNotificationClick = {
                navController.navigate("notifications")
            },
            onNavigateToHistory = {
                navController.navigate("history")
            },
            onOpenShortsFeed = {},
            onPlayMix = { videos, title -> playerViewModel.playPlaylist(videos, 0, title, false) },
            viewModel = homeViewModel,
        )
    }

    composable(UPDATE_ROUTE) {
        currentRoute.value = UPDATE_ROUTE
        UpdateScreen(onClose = { navController.popBackStack() })
    }

    // Notifications Screen
    composable("notifications") {
        currentRoute.value = "notifications"
        NotificationScreen(
            onBackClick = { navController.popBackStack() },
            onNotificationClick = { videoId ->
                navController.navigateToPlayer(videoId)
            },
            onOpenSettings = {
                navController.navigate("settings?target=${SettingsTarget(SettingsDestination.NOTIFICATIONS).encode()}")
            },
        )
    }

    composable(
        route = SHORTS_ROUTE_PATTERN,
        arguments =
            listOf(
                navArgument(SHORTS_ROUTE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        currentRoute.value = SHORTS_ROUTE_KEY
        val source = ShortsQueueSource.decode(backStackEntry.arguments?.getString(SHORTS_ROUTE_ARG))
        val isRootTab = source == ShortsQueueSource.Feed
        ShortsScreen(
            source = source,
            bottomNavOverlayPadding = if (isRootTab) LocalFlowBottomInsets.current.barBottom else 0.dp,
            onBack = {
                navController.popBackStack()
            },
            onChannelClick = { channelId ->
                mediaNavigator.openChannel(channelId)
            },
        )
    }

    composable("subscriptions") { backStackEntry ->
        currentRoute.value = "subscriptions"
        val openMusic by backStackEntry.savedStateHandle
            .getStateFlow(OPEN_MUSIC_SUBSCRIPTIONS, false)
            .collectAsStateWithLifecycle()
        SubscriptionsScreen(
            openMusicSubscriptions = openMusic,
            onMusicSubscriptionsOpened = { backStackEntry.savedStateHandle[OPEN_MUSIC_SUBSCRIPTIONS] = false },
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) {
                    playerViewModel.playVideo(it)
                    GlobalPlayerState.setCurrentVideo(it)
                }
            },
            onShortClick = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onChannelClick = { channel ->
                if (channel.isMusic && channel.id.isNotBlank()) {
                    mediaNavigator.openArtist(channel.id)
                } else {
                    mediaNavigator.openChannel(channel.url.ifBlank { channel.id })
                }
            },
        )
    }

    composable("library") {
        currentRoute.value = "library"
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        val downloadsSourceName =
            androidx.compose.ui.res.stringResource(
                com.fedoratube.org.R.string.library_downloads_label,
            )
        LibraryScreen(
            onOpenRecap = { period -> navController.navigate(period?.let { RecapRoutes.story(it) } ?: RecapRoutes.stats()) },
            onNavigateToHistory = {
                navController.navigate("history")
            },
            onNavigateToPlaylists = { kind ->
                navController.navigate(if (kind == null) "playlists" else "playlists?kind=${kind.name}")
            },
            onNavigateToLikedVideos = {
                navController.navigate("playlist/${PlaylistRepository.LIKED_VIDEOS_ID}")
            },
            onNavigateToLikedMusic = {
                mediaNavigator.openMusicPlaylist(PlaylistRepository.LIKED_MUSIC_ID)
            },
            onNavigateToWatchLater = {
                navController.navigate("playlist/${PlaylistRepository.WATCH_LATER_ID}")
            },
            onNavigateToSavedShorts = {},
            onNavigateToDownloads = {
                navController.navigate("downloads")
            },
            onNavigateToNotes = {
                navController.navigate("notes")
            },
            onManageData = {
                navController.navigate("settings")
            },
            onNavigateToSettings = {
                navController.navigate("settings")
            },
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onMusicClick = { track, queue, sourceName ->
                musicPlayerViewModel.loadAndPlayTrack(track, queue, sourceName)
                onMusicStarted()
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
            onMusicPlaylistClick = { playlistId ->
                mediaNavigator.openMusicPlaylist(playlistId)
            },
            onDownloadedVideoClick = { videos, index ->
                val videoList = videos.map { it.video }
                playerViewModel.playPlaylist(videoList, index, downloadsSourceName)
                GlobalPlayerState.setCurrentVideo(videoList[index])
            },
            onDownloadedMusicClick = { tracks, index ->
                val musicTracks = tracks.map { it.track }
                val selectedTrack = musicTracks[index]
                musicPlayerViewModel.loadAndPlayTrack(selectedTrack, musicTracks, downloadsSourceName)
                onMusicStarted()
            },
            onSavedShortClick = { video ->
                navController.openShortsOrPlayer(ShortsQueueSource.Saved(video.id), disableShortsPlayer)
            },
        )
    }

    composable("search") {
        currentRoute.value = "search"
        // Search owns the whole screen, the way YouTube's does.
        SearchScreen(
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onShortsQueue = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onChannelClick = { channel ->
                mediaNavigator.openChannel(channel.url.ifBlank { channel.id })
            },
            onPlaylistClick = { playlist ->
                navController.navigate("playlist/${playlist.id}")
            },
            onBack = {
                if (!navController.popBackStack()) navController.navigate("home")
            },
        )
    }

    composable(EQUALIZER_ROUTE) {
        currentRoute.value = EQUALIZER_ROUTE
        EqualizerScreen(onBack = { navController.popBackStack() })
    }

    composable("categories") {
        currentRoute.value = "categories"
        com.fedoratube.org.ui.screens.categories.CategoriesScreen(
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onShortClick = { videoId ->
                navController.openShortsOrPlayer(ShortsQueueSource.SeededFeed(videoId), disableShortsPlayer)
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
        )
    }

    composable(
        route = "settings?target={target}",
        arguments =
            listOf(
                navArgument("target") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        currentRoute.value = "settings"
        val context = androidx.compose.ui.platform.LocalContext.current
        val preferences = remember(context) { com.fedoratube.org.data.local.PlayerPreferences(context) }
        val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)
        val exitSettings: () -> Unit = {
            if (isFocusModeEnabled) {
                navController.popBackStack("home", inclusive = false)
            } else {
                navController.popBackStack()
            }
        }
        androidx.activity.compose.BackHandler(enabled = isFocusModeEnabled) {
            exitSettings()
        }
        SettingsHost(
            start = SettingsTarget.decode(backStackEntry.arguments?.getString("target")),
            onExit = exitSettings,
            onOpenDonations = { navController.navigate("donations") },
            onOpenRecap = { navController.navigate(RecapRoutes.stats()) },
            onOpenUpdate = { navController.navigate(UPDATE_ROUTE) },
        )
    }

    composable(
        route = RecapRoutes.STATS,
        arguments =
            listOf(
                navArgument(RecapRoutes.ARG_PERIOD) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        currentRoute.value = "recap"
        RecapScreen(
            onBack = { navController.popBackStack() },
            onPlayStory = { period, source -> navController.navigate(RecapRoutes.story(period, source)) },
            startAt = RecapRoutes.decode(backStackEntry.arguments?.getString(RecapRoutes.ARG_PERIOD)),
        )
    }

    composable(
        route = RecapRoutes.STORY,
        arguments =
            listOf(
                navArgument(RecapRoutes.ARG_PERIOD) { type = NavType.StringType },
                navArgument(RecapRoutes.ARG_SOURCE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        currentRoute.value = "recap_story"
        val period = RecapRoutes.decode(backStackEntry.arguments?.getString(RecapRoutes.ARG_PERIOD))
        if (period == null) {
            LaunchedEffect(Unit) { navController.popBackStack() }
        } else {
            RecapStoryScreen(
                period = period,
                source = RecapRoutes.decodeSource(backStackEntry.arguments?.getString(RecapRoutes.ARG_SOURCE)),
                onClose = { navController.popBackStack() },
            )
        }
    }

    composable("donations") {
        currentRoute.value = "donations"
        com.fedoratube.org.ui.screens.settings.about.DonationsScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable(
        route = "channel?url={channelUrl}",
        arguments = listOf(navArgument("channelUrl") { type = NavType.StringType }),
    ) { backStackEntry ->
        currentRoute.value = "channel"
        val channelUrl =
            backStackEntry.arguments?.getString("channelUrl")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            } ?: ""

        ChannelScreen(
            channelUrl = channelUrl,
            onVideoClick = { video ->
                navController.openVideoOrShorts(video, disableShortsPlayer) { playerViewModel.playVideo(it) }
            },
            onChannelClick = { channelId ->
                mediaNavigator.openChannel(channelId)
            },
            onShortClick = { videoId, sortIndex ->
                navController.openShortsOrPlayer(
                    ShortsQueueSource.Channel(channelUrl = channelUrl, startVideoId = videoId, sortIndex = sortIndex),
                    disableShortsPlayer,
                )
            },
            onPlaylistClick = { playlistId ->
                navController.navigate("playlist/$playlistId")
            },
            onBackClick = { navController.popBackStack() },
        )
    }

    // History Screen
    composable("history") {
        currentRoute.value = "history"
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        HistoryScreen(
            onVideoClick = { track ->
                val deviceFile = LocalMediaIds.videoUri(track.videoId)
                if (deviceFile != null) {
                    val video =
                        com.fedoratube.org.data.model.Video(
                            id = track.videoId,
                            title = track.title,
                            channelName = track.artist,
                            channelId = "",
                            thumbnailUrl = deviceFile.toString(),
                            duration = track.duration,
                            viewCount = 0,
                            uploadDate = "",
                        )
                    playerViewModel.playLocalVideo(video, deviceFile.toString())
                } else {
                    playerViewModel.playVideo(track.toVideo())
                }
            },
            onShortsQueue = { source ->
                navController.openShortsOrPlayer(source, disableShortsPlayer)
            },
            onMusicClick = { track, queue ->
                musicPlayerViewModel.loadAndPlayTrack(track, queue, "History")
                onMusicStarted()
            },
            onBackClick = { navController.popBackStack() },
        )
    }

    // Playlists Screen
    composable(
        route = "playlists?kind={kind}",
        arguments =
            listOf(
                navArgument("kind") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        currentRoute.value = "playlists"
        val fixedKind =
            backStackEntry.arguments?.getString("kind")?.let { name ->
                com.fedoratube.org.ui.components.shared.MediaKind.entries
                    .firstOrNull { it.name == name }
            }
        PlaylistsScreen(
            fixedKind = fixedKind,
            onBackClick = { navController.popBackStack() },
            onVideoPlaylistClick = { playlist ->
                navController.navigate("playlist/${playlist.id}")
            },
            onMusicPlaylistClick = { playlist ->
                mediaNavigator.openMusicPlaylist(playlist.id)
            },
        )
    }

    // Playlist Detail Screen
    composable("playlist/{playlistId}") { _ ->
        currentRoute.value = "playlist"
        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        PlaylistDetailScreen(
            onNavigateBack = { navController.popBackStack() },
            onPlayPlaylist = { videos, index, shuffle, title ->
                val start = videos[index]
                if (start.isMusic) {
                    // A YouTube Music playlist plays in the music player, like any other song list.
                    val tracks = videos.filter { it.isMusic }.map { it.toMusicTrack() }
                    musicPlayerViewModel.loadAndPlayTrack(start.toMusicTrack(), tracks, title)
                    onMusicStarted()
                } else {
                    playerViewModel.playPlaylist(videos, index, title, shuffle)
                }
            },
        )
    }


    composable("downloads") {
        currentRoute.value = "downloads"

        val musicPlayerViewModel = sharedMusicPlayerViewModel()
        val downloadsTitle =
            androidx.compose.ui.res.stringResource(
                com.fedoratube.org.R.string.library_downloads_label,
            )

        com.fedoratube.org.ui.screens.library.DownloadsScreen(
            onBackClick = { navController.popBackStack() },
            onVideoClick = { videos, index ->
                val videoList = videos.map { it.video }
                playerViewModel.playPlaylist(videoList, index, downloadsTitle)
                GlobalPlayerState.setCurrentVideo(videoList[index])
            },
            onMusicClick = { tracks, index ->
                val musicTracks = tracks.map { it.track }
                val selectedTrack = musicTracks[index]

                musicPlayerViewModel.loadAndPlayTrack(selectedTrack, musicTracks, downloadsTitle)
                onMusicStarted()
            },
            onOpenCollection = { summary ->
                if (summary.collection.kind.isMusic) {
                    mediaNavigator.openMusicPlaylist(summary.collection.id)
                } else {
                    navController.navigate("playlist/${summary.collection.id}")
                }
            },
            onHomeClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
        )
    }
    composable("notes") {
        currentRoute.value = "notes"
        com.fedoratube.org.ui.screens.notes.NotesScreen(
            onBackClick = { navController.popBackStack() },
            onPlay = { video, startPositionMs -> playerViewModel.playVideo(video, startPositionMs = startPositionMs) },
        )
    }

    musicRoutes(navController, mediaNavigator, currentRoute, playerViewModel, defaultStartRoute, onMusicStarted)

    widgetPlaybackRoutes(navController, currentRoute, defaultStartRoute, playerViewModel, onMusicStarted)

    linkPlaybackRoutes(
        navController = navController,
        currentRoute = currentRoute,
        playerViewModel = playerViewModel,
        playerUiStateResult = playerUiStateResult,
        playerSheetState = playerSheetState,
        playerVisibleState = playerVisibleState,
        defaultStartRoute = defaultStartRoute,
        onMusicStarted = onMusicStarted,
    )
}
