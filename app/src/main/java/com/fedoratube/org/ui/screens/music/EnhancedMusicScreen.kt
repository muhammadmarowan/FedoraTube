package com.fedoratube.org.ui.screens.music

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.music.model.audioMusicOnly
import com.fedoratube.org.innertube.pages.MoodAndGenres
import com.fedoratube.org.ui.TabScrollEventBus
import com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets
import com.fedoratube.org.ui.components.layout.floatAboveBottomChrome
import com.fedoratube.org.ui.components.layout.flowBottomContentPadding
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBarDefaults
import com.fedoratube.org.ui.components.music.section.HomeSectionType
import com.fedoratube.org.ui.components.music.section.MusicHomeLibrary
import com.fedoratube.org.ui.components.music.section.musicHomeFeed
import com.fedoratube.org.ui.components.music.sheet.LocalMusicMenus
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.FlowPullToRefreshBox
import com.fedoratube.org.ui.components.shared.MusicScreenShimmerLoading
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import java.util.Random

private val FeedBottomClearance = 96.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EnhancedMusicScreen(
    onSongClick: (MusicTrack, List<MusicTrack>, String?) -> Unit,
    onVideoClick: (MusicTrack) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onRecognizeClick: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onMoodsClick: (MoodAndGenres.Item?) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onAllPlaylistsClick: () -> Unit = {},
    onAllSubscriptionsClick: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    topCategoryTabs: List<com.fedoratube.org.ui.components.layout.navigation.FlowTab> = emptyList(),
    onCategorySelected: (com.fedoratube.org.ui.components.layout.navigation.FlowTab) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: MusicViewModel = sharedMusicViewModel(),
    libraryViewModel: MusicHomeLibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val ownPlaylists by libraryViewModel.playlists.collectAsStateWithLifecycle()
    val musicSubscriptions by libraryViewModel.subscriptions.collectAsStateWithLifecycle()
    val hiddenShelves by libraryViewModel.hiddenShelves.collectAsStateWithLifecycle()
    val lastFmSignedIn by libraryViewModel.lastFmSignedIn.collectAsStateWithLifecycle()
    val discovery by libraryViewModel.discovery.collectAsStateWithLifecycle()
    val library =
        remember(
            ownPlaylists,
            musicSubscriptions,
            hiddenShelves,
            onPlaylistClick,
            onAllPlaylistsClick,
            onAllSubscriptionsClick,
            lastFmSignedIn,
            discovery,
        ) {
            MusicHomeLibrary(
                playlists = ownPlaylists,
                subscriptions = musicSubscriptions,
                hidden = hiddenShelves,
                onPlaylistClick = onPlaylistClick,
                onAllPlaylistsClick = onAllPlaylistsClick,
                onAllSubscriptionsClick = onAllSubscriptionsClick,
                discoveryAvailable = lastFmSignedIn,
                discovery = discovery,
                onDiscoveryShown = libraryViewModel::loadDiscovery,
            )
        }
    val musicListState = rememberLazyListState()
    val quickPicksGridState = rememberLazyGridState()
    // Reset on every Music entry (not saveable) so re-entering Music always
    // lands at the top with Trending first instead of a restored scroll offset.
    var musicFirstScrollFixed by remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) { musicListState.scrollToItem(0, 0) }
    LaunchedEffect(uiState.trendingSongs.isNotEmpty() || uiState.dynamicSections.isNotEmpty()) {
        if ((uiState.trendingSongs.isNotEmpty() || uiState.dynamicSections.isNotEmpty()) && !musicFirstScrollFixed) {
            musicFirstScrollFixed = true
            if (musicListState.firstVisibleItemIndex != 0 || musicListState.firstVisibleItemScrollOffset != 0) {
                musicListState.scrollToItem(0, 0)
            }
        }
    }

    // Planner-lite: the brain's maturity decides which sections lead the page —
    // a cold brain has nothing personal to say, a mature one leads with taste.
    val sectionOrder =
        remember(uiState.sessionSeed, uiState.brainMaturity) {
            val defaultOrder = HomeSectionType.entries
            val anchored =
                when (uiState.brainMaturity) {
                    "cold_start" -> {
                        listOf(
                            HomeSectionType.CHARTS,
                            HomeSectionType.MOODS_AND_GENRES,
                            HomeSectionType.NEW_RELEASES,
                        )
                    }

                    "mature" -> {
                        listOf(
                            HomeSectionType.QUICK_PICKS,
                            HomeSectionType.RECOMMENDED_FOR_YOU,
                            HomeSectionType.SIMILAR_TO,
                            HomeSectionType.FROM_COMMUNITY,
                        )
                    }

                    else -> {
                        listOf(
                            HomeSectionType.QUICK_PICKS,
                            HomeSectionType.RECOMMENDED_FOR_YOU,
                            HomeSectionType.FROM_COMMUNITY,
                            HomeSectionType.DAILY_DISCOVER,
                        )
                    }
                }
            val dynamicPool = defaultOrder - anchored
            // Trending (CHARTS) always leads when entering Music.
            val ordered = (anchored + dynamicPool.shuffled(Random(uiState.sessionSeed))).distinct()
            (listOf(HomeSectionType.CHARTS) + ordered).distinct()
        }

    // Scroll to top and refresh when tapping the music tab while already on this screen
    LaunchedEffect(Unit) {
        TabScrollEventBus.scrollToTopEvents
            .filter { it == "music" }
            .collectLatest {
                musicListState.animateScrollToItem(0)
                viewModel.refresh()
            }
    }
    val musicMenus = LocalMusicMenus.current

    val bottomInsets = LocalFlowBottomInsets.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { com.fedoratube.org.data.local.PlayerPreferences(context) }
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.foundation.layout.Column {
                FlowTopBar(
                    title = {
                        if (isFocusModeEnabled) {
                            Text(
                                text = stringResource(R.string.nav_music),
                                style = FlowTopBarDefaults.titleStyle,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ic_fedora_tube_wordmark),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier.height(28.dp),
                                contentScale = ContentScale.FillHeight,
                            )
                        }
                    },
                    centeredTitle = isFocusModeEnabled,
                    onBack = if (isFocusModeEnabled) onBack else null,
                    actions = {
                        if (!isFocusModeEnabled) {
                            IconButton(onClick = onNotificationClick) {
                                Icon(Icons.Outlined.Notifications, stringResource(R.string.notifications))
                            }
                        }
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Outlined.Search, stringResource(R.string.search))
                        }
                    },
                    globalActions = com.fedoratube.org.ui.components.layout.topbar.FlowGlobalActionsMode.None,
                    scrollBehavior = scrollBehavior,
                )
                if (!isFocusModeEnabled && topCategoryTabs.isNotEmpty()) {
                    com.fedoratube.org.ui.components.layout.navigation.FlowTopCategoryBar(
                        tabs = topCategoryTabs,
                        selectedTab = com.fedoratube.org.ui.components.layout.navigation.FlowTab.Music,
                        onCategorySelected = onCategorySelected,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { paddingValues ->
        val categorySwipeModifier =
            com.fedoratube.org.ui.components.layout.navigation.rememberCategorySwipe(
                tabs = topCategoryTabs,
                selectedTab = com.fedoratube.org.ui.components.layout.navigation.FlowTab.Music,
                onCategorySelected = onCategorySelected,
            )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .then(if (!isFocusModeEnabled) categorySwipeModifier else Modifier),
        ) {
            val isInitialLoading = uiState.isLoading && uiState.trendingSongs.isEmpty() && uiState.dynamicSections.isEmpty()

            when {
                isInitialLoading -> {
                    MusicScreenShimmerLoading()
                }

                uiState.error != null && uiState.trendingSongs.isEmpty() -> {
                    FlowErrorState(
                        error = uiState.error ?: stringResource(R.string.error_occurred),
                        onRetry = { viewModel.retry() },
                    )
                }

                else -> {
                    val popularArtists =
                        remember(uiState.trendingSongs, uiState.newReleases) {
                            (uiState.trendingSongs + uiState.newReleases)
                                .distinctBy { it.artist }
                                .take(10)
                        }

                    // Raw concatenation is only the placeholder until the VM's
                    // brain-ranked speed dial lands.
                    val fallbackSpeedDial =
                        remember(uiState.history, uiState.forYouTracks, uiState.recommendedForYouTracks, uiState.listenAgain) {
                            (uiState.recommendedForYouTracks + uiState.forYouTracks + uiState.history + uiState.listenAgain)
                                .audioMusicOnly()
                                .distinctBy { it.videoId }
                                .take(26)
                        }
                    val speedDialTracks = uiState.speedDialTracks.ifEmpty { fallbackSpeedDial }
                    val quickPickTracks =
                        remember(uiState.forYouTracks) {
                            uiState.forYouTracks.audioMusicOnly().take(20)
                        }
                    val pullState = rememberPullToRefreshState()

                    FlowPullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.refresh() },
                        state = pullState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            state = musicListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = flowBottomContentPadding(FeedBottomClearance)),
                        ) {
                            musicHomeFeed(
                                uiState = uiState,
                                sectionOrder = sectionOrder,
                                quickPickTracks = quickPickTracks,
                                speedDialTracks = speedDialTracks,
                                popularArtists = popularArtists,
                                library = library,
                                quickPicksGridState = quickPicksGridState,
                                onSongClick = onSongClick,
                                onVideoClick = onVideoClick,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                                onMoodsClick = onMoodsClick,
                                onChipToggle = { viewModel.setHomeChip(it) },
                                onTrackMenu = musicMenus::openSong,
                                onCollectionMenu = musicMenus::openCollection,
                                onLoadMore = { viewModel.loadMoreHomeContent() },
                            )
                        }
                    }
                }
            }
        }
    }
}
