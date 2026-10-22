package com.fedoratube.org.ui.screens.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import com.fedoratube.org.ui.components.dialogs.ShareAppQrDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.local.HomeFeedColumns
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.local.VideoHistoryEntry
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.feed.ShortsQueueSource
import com.fedoratube.org.player.DeepFlowManager
import com.fedoratube.org.ui.TabScrollEventBus
import com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets
import com.fedoratube.org.ui.components.layout.floatAboveBottomChrome
import com.fedoratube.org.ui.components.layout.topbar.FlowGlobalActionsMode
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import com.fedoratube.org.ui.components.rememberFeedGridLayout
import com.fedoratube.org.ui.components.shared.FeedGridSkeleton
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.FlowPullToRefreshBox
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private const val IMPRESSION_DEBOUNCE_MS = 500L
private const val SKELETON_CARD_COUNT = 12
private const val MILLIS_PER_SECOND = 1000L

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun HomeScreen(
    onVideoClick: (Video) -> Unit,
    onShortClick: (ShortsQueueSource) -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onOpenShortsFeed: () -> Unit = {},
    onPlayMix: (videos: List<Video>, title: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences = remember(context) { PlayerPreferences(context) }
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val homeFeedEnabled by preferences.homeFeedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val refreshHomeOnReselect by preferences.refreshHomeOnReselect.collectAsStateWithLifecycle(initialValue = true)
    val showAppLogoIcon by preferences.showAppLogoIcon.collectAsStateWithLifecycle(initialValue = true)
    val deepFlowActive by preferences.deepFlowActive.collectAsStateWithLifecycle(initialValue = false)

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val refreshFailed = stringResource(R.string.home_refresh_failed)
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(viewModel) {
        viewModel.refreshFailures.collectLatest {
            val result = snackbarHostState.showSnackbar(refreshFailed, actionLabel = retryLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.refreshFeed()
        }
    }

    LifecycleStartEffect(viewModel, homeFeedEnabled) {
        if (homeFeedEnabled && !isFocusModeEnabled) {
            viewModel.onHomeVisible()
        } else {
            viewModel.onHomeHidden()
        }
        onStopOrDispose { viewModel.onHomeHidden() }
    }

    val videoIndexById =
        remember(uiState.videos) {
            buildMap(uiState.videos.size) {
                uiState.videos.forEachIndexed { index, video -> put(video.id, index) }
            }
        }
    LaunchedEffect(gridState, videoIndexById) {
        snapshotFlow {
            var lastVisibleVideoIndex = -1
            gridState.layoutInfo.visibleItemsInfo.forEach { item ->
                val index = videoIndexById[item.key as? String] ?: return@forEach
                if (index > lastVisibleVideoIndex) lastVisibleVideoIndex = index
            }
            lastVisibleVideoIndex to gridState.isScrollInProgress
        }.distinctUntilChanged()
            .collect { (lastVisibleVideoIndex, _) ->
                viewModel.onHomeViewportChanged(lastVisibleVideoIndex)
            }
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String } }
            .debounce(IMPRESSION_DEBOUNCE_MS)
            .collect { viewModel.recordImpressions(it) }
    }

    LaunchedEffect(refreshHomeOnReselect) {
        TabScrollEventBus.scrollToTopEvents
            .filter { it == "home" }
            .collectLatest {
                val atTop = gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
                if (!atTop) {
                    gridState.animateScrollToItem(0)
                } else if (refreshHomeOnReselect) {
                    viewModel.refreshFeed()
                }
            }
    }

    val haptic = LocalHapticFeedback.current
    val deepFlowLabel = stringResource(R.string.deep_flow_mode_title)
    val toggleDeepFlow: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        coroutineScope.launch { DeepFlowManager.toggle(context) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(snackbarHostState, Modifier.floatAboveBottomChrome(LocalFlowBottomInsets.current))
        },
        topBar = {
            FlowTopBar(
                title = {
                    if (!isFocusModeEnabled) {
                        Image(
                            painter = painterResource(R.drawable.ic_fedora_tube_wordmark),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.height(28.dp),
                            contentScale = ContentScale.FillHeight,
                        )
                    }
                },
                leading = null,
                actions = {
                    if (!isFocusModeEnabled) {
                        IconButton(onClick = onSearchClick) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.search),
                            )
                        }
                        IconButton(onClick = onNotificationClick) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = stringResource(R.string.notifications),
                            )
                        }
                    }
                },
                globalActions = FlowGlobalActionsMode.None,
            )
        },
    ) { padding ->
        if (isFocusModeEnabled) {
            FocusModeContent(
                onSearchClick = onSearchClick,
                onMusicClick = onNavigateToMusic,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToDownloads = onNavigateToDownloads,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
            ) {
                ResettableHomePullToRefreshBox(
                    resetKey = uiState.isLoading && uiState.videos.isEmpty(),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refreshFeed() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val feedLayout = rememberFeedGridLayout(maxWidth, HomeFeedColumns.ONE)

                        when {
                            !homeFeedEnabled -> {
                                HomeFeedDisabledState(modifier = Modifier.fillMaxSize())
                            }

                            uiState.isLoading && uiState.videos.isEmpty() -> {
                                FeedGridSkeleton(
                                    layout = feedLayout,
                                    listMode = false,
                                    stackedInset = false,
                                    placeholderCount = SKELETON_CARD_COUNT,
                                    compactRowSpacing = 16.dp,
                                )
                            }

                            uiState.error != null && uiState.videos.isEmpty() -> {
                                FlowErrorState(
                                    error = uiState.error ?: stringResource(R.string.error_occurred),
                                    onRetry = { viewModel.retry() },
                                )
                            }

                            else -> {
                                ReportDrawnWhen {
                                    uiState.videos.isNotEmpty()
                                }

                                HomeFeedGrid(
                                    uiState = uiState,
                                    feedLayout = feedLayout,
                                    isListView = false,
                                    gridState = gridState,
                                    onVideoClick = onVideoClick,
                                    onEnrichChannelMetadata = viewModel::enrichChannelMetadataIfMissing,
                                    onContinueWatchingClick = { entry -> onVideoClick(entry.toResumeVideo()) },
                                    onContinueWatchingRemove = viewModel::removeContinueWatchingEntry,
                                    onShortClick = { _, _ -> },
                                    onSeeAllHistory = onNavigateToHistory,
                                    onOpenShortsFeed = {},
                                    onShortsShown = {},
                                    onRefresh = { viewModel.refreshFeed() },
                                    onRetryLoadMore = viewModel::retryLoadMore,
                                    footerMinVideos = 0,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusModeContent(
    onSearchClick: () -> Unit,
    onMusicClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showQrDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().offset(y = (-60).dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_fedora_tube_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.height(36.dp),
                contentScale = ContentScale.FillHeight,
            )

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                onClick = onSearchClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 3.dp,
                shadowElevation = 2.dp,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.focus_mode_search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = onMusicClick,
                        modifier = Modifier.size(40.dp),
                        colors =
                            IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = stringResource(R.string.nav_music_shortcut),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Centered actions row: Settings on the left, Downloads in the center, QR code on the right
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(52.dp),
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        modifier = Modifier.size(24.dp),
                    )
                }

                FilledTonalIconButton(
                    onClick = onNavigateToDownloads,
                    modifier = Modifier.size(52.dp),
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = stringResource(R.string.library_downloads_label),
                        modifier = Modifier.size(24.dp),
                    )
                }

                FilledTonalIconButton(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.size(52.dp),
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCode2,
                        contentDescription = stringResource(R.string.share_app_qr),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }

    if (showQrDialog) {
        ShareAppQrDialog(onDismiss = { showQrDialog = false })
    }
}

// Deliberately not data/model's VideoHistoryEntry.toVideo(): that one carries isShort, which would
// reroute a resumed short to the Shorts player instead of the video player.
internal fun VideoHistoryEntry.toResumeVideo(): Video =
    Video(
        id = videoId,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = thumbnailUrl,
        duration = (duration / MILLIS_PER_SECOND).toInt(),
        viewCount = 0L,
        uploadDate = "",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResettableHomePullToRefreshBox(
    resetKey: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    key(resetKey) {
        FlowPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
            content = content,
        )
    }
}
