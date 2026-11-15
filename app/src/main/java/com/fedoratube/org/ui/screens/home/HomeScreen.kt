package com.fedoratube.org.ui.screens.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Movie
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
import com.fedoratube.org.ui.screens.home.chips.HomeChip
import com.fedoratube.org.ui.screens.home.chips.HomeChipContent
import com.fedoratube.org.ui.screens.home.chips.HomeChipRow
import com.fedoratube.org.ui.screens.subscriptions.SubscriptionsEmptyState
import com.fedoratube.org.ui.screens.subscriptions.SubscriptionsFeedContent
import com.fedoratube.org.ui.screens.subscriptions.SubscriptionsViewModel
import com.fedoratube.org.ui.screens.subscriptions.quickAccessOrder
import com.fedoratube.org.ui.screens.subscriptions.sharedSubscriptionsViewModel
import com.fedoratube.org.ui.screens.subscriptions.sortSubscriptions
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import com.fedoratube.org.data.model.Channel
import kotlin.math.roundToInt
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
    onSearchClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMusic: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onPlayMix: (videos: List<Video>, title: String) -> Unit = { _, _ -> },
    topCategoryTabs: List<com.fedoratube.org.ui.components.layout.navigation.FlowTab> = emptyList(),
    onCategorySelected: (com.fedoratube.org.ui.components.layout.navigation.FlowTab) -> Unit = {},
    onChannelClick: (Channel) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chipsState by viewModel.chips.collectAsStateWithLifecycle()
    val subsViewModel: SubscriptionsViewModel = sharedSubscriptionsViewModel()
    val subUiState by subsViewModel.uiState.collectAsStateWithLifecycle()
    val subGridState = rememberLazyGridState()
    val chipGridState = rememberLazyGridState()

    val preferences = remember(context) { PlayerPreferences(context) }
    val columnPreference by preferences.homeFeedColumns.collectAsStateWithLifecycle(HomeFeedColumns.AUTO)
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val homeFeedEnabled by preferences.homeFeedEnabled.collectAsStateWithLifecycle(initialValue = true)
    val refreshHomeOnReselect by preferences.refreshHomeOnReselect.collectAsStateWithLifecycle(initialValue = true)
    val showAppLogoIcon by preferences.showAppLogoIcon.collectAsStateWithLifecycle(initialValue = true)
    val deepFlowActive by preferences.deepFlowActive.collectAsStateWithLifecycle(initialValue = false)

    val sortedChannels =
        remember(subUiState.subscribedChannels, subUiState.sortMode, subUiState.recentVideos) {
            sortSubscriptions(subUiState.subscribedChannels, subUiState.sortMode, subUiState.recentVideos)
        }
    val topChannels =
        remember(sortedChannels) {
            quickAccessOrder(sortedChannels)
        }

    val chipKeys = remember(chipsState.chips) { chipsState.chips.map { it.key } }
    val currentChipIndex = remember(chipKeys, chipsState.selected) {
        chipKeys.indexOf(chipsState.selected).coerceAtLeast(0)
    }

    val categorySwipeModifier =
        Modifier.pointerInput(chipKeys, chipsState.selected) {
            var totalDragX = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDragX = 0f },
                onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                onDragEnd = {
                    val thresholdPx = 48.dp.toPx()
                    if (totalDragX < -thresholdPx) {
                        if (currentChipIndex + 1 < chipKeys.size) {
                            viewModel.selectChip(chipKeys[currentChipIndex + 1])
                        }
                    } else if (totalDragX > thresholdPx) {
                        if (currentChipIndex - 1 >= 0) {
                            viewModel.selectChip(chipKeys[currentChipIndex - 1])
                        }
                    }
                    totalDragX = 0f
                },
                onDragCancel = { totalDragX = 0f },
            )
        }

    val gridState = rememberLazyGridState()
    var homeFirstScrollFixed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.videos.isNotEmpty()) {
        if (uiState.videos.isNotEmpty() && !homeFirstScrollFixed) {
            homeFirstScrollFixed = true
            if (gridState.firstVisibleItemIndex != 0 || gridState.firstVisibleItemScrollOffset != 0) {
                gridState.scrollToItem(0, 0)
            }
        }
    }
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
            val items = gridState.layoutInfo.visibleItemsInfo
            for (i in items.indices) {
                val index = videoIndexById[items[i].key as? String] ?: continue
                if (index > lastVisibleVideoIndex) lastVisibleVideoIndex = index
            }
            lastVisibleVideoIndex
        }.distinctUntilChanged()
            .collect { lastVisibleVideoIndex ->
                if (lastVisibleVideoIndex >= 0) {
                    viewModel.onHomeViewportChanged(lastVisibleVideoIndex)
                }
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(snackbarHostState, Modifier.floatAboveBottomChrome(LocalFlowBottomInsets.current))
        },
        topBar = {
            val density = LocalDensity.current
            val showChips = !isFocusModeEnabled && chipsState.chips.isNotEmpty()
            val baseBarHeight = 48.dp
            val chipRowHeight = 48.dp
            val totalHeight = baseBarHeight + (if (showChips) chipRowHeight else 0.dp)
            val totalHeightPx = with(density) { totalHeight.toPx() }

            SideEffect {
                if (scrollBehavior.state.heightOffsetLimit != -totalHeightPx) {
                    scrollBehavior.state.heightOffsetLimit = -totalHeightPx
                }
            }

            val currentOffsetPx = scrollBehavior.state.heightOffset.roundToInt()

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val layoutHeight = (placeable.height + currentOffsetPx).coerceAtLeast(0)
                            layout(placeable.width, layoutHeight) {
                                placeable.place(0, currentOffsetPx)
                            }
                        },
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    FlowTopBar(
                        title = {
                            if (!isFocusModeEnabled) {
                                FedoraBrandWordmark(
                                    isDeepFlowActive = deepFlowActive,
                                    onLongPress = toggleDeepFlow,
                                    longPressLabel = deepFlowLabel,
                                )
                            }
                        },
                        leading = null,
                        actions = {
                            if (!isFocusModeEnabled) {
                                IconButton(onClick = onNotificationClick) {
                                    Icon(
                                        Icons.Outlined.Notifications,
                                        contentDescription = stringResource(R.string.notifications),
                                    )
                                }
                                IconButton(onClick = onSearchClick) {
                                    Icon(
                                        Icons.Outlined.Search,
                                        contentDescription = stringResource(R.string.search),
                                    )
                                }
                            }
                        },
                        globalActions = FlowGlobalActionsMode.None,
                        scrollBehavior = null,
                    )
                    if (showChips) {
                        HomeChipRow(
                            state = chipsState,
                            onSelect = viewModel::selectChip,
                        )
                    }
                }
            }
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
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .then(categorySwipeModifier)
                        .background(MaterialTheme.colorScheme.background),
            ) {
                AnimatedContent(
                    targetState = chipsState.selected,
                    transitionSpec = {
                        val targetIdx = chipKeys.indexOf(targetState)
                        val initialIdx = chipKeys.indexOf(initialState)
                        if (targetIdx >= initialIdx) {
                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeIn(tween(200)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeOut(tween(180)))
                        } else {
                            (slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> -width / 3 } + fadeIn(tween(200)))
                                .togetherWith(slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { width -> width / 3 } + fadeOut(tween(180)))
                        }
                    },
                    label = "chipFeedContent",
                    modifier = Modifier.fillMaxSize(),
                ) { currentSelectedKey ->
                    when (currentSelectedKey) {
                        HomeChip.All.key -> {
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
                                                onRefresh = { viewModel.refreshFeed() },
                                                onRetryLoadMore = viewModel::retryLoadMore,
                                                footerMinVideos = 0,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeChip.Subscriptions.key -> {
                            if (subUiState.subscribedChannels.isEmpty()) {
                                SubscriptionsEmptyState(modifier = Modifier.fillMaxSize())
                            } else {
                                SubscriptionsFeedContent(
                                    state = subUiState,
                                    videos = subUiState.recentVideos,
                                    topChannels = topChannels,
                                    gridState = subGridState,
                                    columnPreference = columnPreference,
                                    onRefresh = subsViewModel::refreshFeed,
                                    onVideoClick = onVideoClick,
                                    onChannelClick = { channel ->
                                        subsViewModel.selectChannel(channel.id)
                                        coroutineScope.launch { subGridState.animateScrollToItem(0) }
                                    },
                                    onChannelLongClick = onChannelClick,
                                    onViewAllClick = {},
                                    onMusicSubscriptionsClick = onNavigateToMusic,
                                    onRetryFailedChannels = subsViewModel::retryFailedChannels,
                                    onDismissFailedChannels = subsViewModel::dismissFailedChannels,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }

                        else -> {
                            ResettableHomePullToRefreshBox(
                                resetKey = chipsState.isRefreshing,
                                isRefreshing = chipsState.isRefreshing,
                                onRefresh = { viewModel.refreshSelectedChip() },
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val feedLayout = rememberFeedGridLayout(maxWidth, HomeFeedColumns.ONE)

                                    HomeChipContent(
                                        state = chipsState,
                                        feedLayout = feedLayout,
                                        isListView = false,
                                        gridState = chipGridState,
                                        onVideoClick = onVideoClick,
                                        onPlayMix = { mix -> onPlayMix(mix.videos, mix.seed.title) },
                                        onRefresh = { viewModel.refreshSelectedChip() },
                                        onEnrichVideo = viewModel::enrichChipVideo,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
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

            // Centered actions row: Settings on the left, Downloads, Movie (right of Downloads), QR code on the right
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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
