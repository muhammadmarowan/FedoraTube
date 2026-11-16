package com.fedoratube.org.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import com.fedoratube.org.data.local.SearchHistoryItem
import com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets
import com.fedoratube.org.ui.components.layout.LocalHideBottomBar
import com.fedoratube.org.ui.components.layout.floatAboveBottomChrome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.fedoratube.org.R
import com.fedoratube.org.data.local.SearchFilter
import com.fedoratube.org.data.local.WatchState
import com.fedoratube.org.data.local.availableWith
import com.fedoratube.org.data.model.Channel
import com.fedoratube.org.data.model.Playlist
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.paging.SearchResultItem
import com.fedoratube.org.data.paging.supportsVideoFilters
import com.fedoratube.org.ui.OnTabReselected
import com.fedoratube.org.ui.components.layout.navigation.FlowTab
import com.fedoratube.org.ui.components.layout.navigation.LocalMediaNavigator
import com.fedoratube.org.ui.components.rememberFeedGridLayout
import com.fedoratube.org.ui.components.search.SearchFilterDialog
import com.fedoratube.org.ui.components.search.SearchResultActions
import com.fedoratube.org.ui.components.search.SearchResults
import com.fedoratube.org.ui.components.search.SearchSuggestionsPanel
import com.fedoratube.org.ui.components.search.SearchTopBar
import com.fedoratube.org.ui.components.shared.FeedGridSkeleton
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.quickactions.QuickActionsViewModel
import com.fedoratube.org.ui.components.shared.quickactions.sharedQuickActionsViewModel
import com.fedoratube.org.ui.screens.music.MusicSearchResultsView
import com.fedoratube.org.ui.screens.music.MusicSearchViewModel
import com.fedoratube.org.utils.YouTubeLink
import com.fedoratube.org.utils.parseYouTubeLink
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class SearchTabCategory {
    VIDEOS,
    MUSIC,
}

/**
 * Modern Search destination supporting both Video and Music categories with a centered,
 * animated category switcher, smooth horizontal swiping, jitter-free hide-on-scroll top chrome,
 * and a floating keyboard trigger above the navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onVideoClick: (Video) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onBack: (() -> Unit)? = null,
    onTrackClick: (MusicTrack, List<MusicTrack>, String?) -> Unit = { _, _, _ -> },
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onMusicPlaylistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
    musicSearchViewModel: MusicSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()
    val musicUiState by musicSearchViewModel.uiState.collectAsState()
    val musicQuery by musicSearchViewModel.query.collectAsState()
    val musicMatchingHistory by musicSearchViewModel.matchingHistory.collectAsState()
    val state = rememberSearchState(viewModel)
    val focusPreferences = remember(context) { com.fedoratube.org.data.local.PlayerPreferences(context) }
    val isFocusModeEnabled by focusPreferences.focusModeEnabled.collectAsState(initial = false)

    val combinedHistory =
        remember(state.matchingHistory, musicMatchingHistory) {
            val seen = mutableSetOf<String>()
            val list = mutableListOf<SearchHistoryItem>()
            for (item in state.matchingHistory + musicMatchingHistory) {
                val key = item.query.trim().lowercase()
                if (key.isNotEmpty() && seen.add(key)) {
                    list.add(item)
                }
            }
            list
        }

    val combinedSuggestions =
        remember(state.suggestions, musicUiState.suggestions) {
            val musicSuggestions =
                musicUiState.suggestions.map { com.fedoratube.org.innertube.pages.search.SearchSuggestion(it) }
            val seen = mutableSetOf<String>()
            val list = mutableListOf<com.fedoratube.org.innertube.pages.search.SearchSuggestion>()
            for (sug in state.suggestions + musicSuggestions) {
                val key = sug.text.trim().lowercase()
                if (key.isNotEmpty() && seen.add(key)) {
                    list.add(sug)
                }
            }
            if (list.isNotEmpty()) list else state.suggestions
        }

    val quickActions: QuickActionsViewModel = sharedQuickActionsViewModel()
    val subscribedIds by quickActions.subscribedChannelIds.collectAsStateWithLifecycle()
    val pagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    OnTabReselected(FlowTab.Search.route) {
        if (pagerState.currentPage == 0) {
            gridState.animateScrollToItem(0)
        }
    }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val submit: (String) -> Unit = { raw ->
        val text = raw.trim().trimEnd('.', '?', '!', ',', ';')
        if (text.isNotEmpty()) {
            state.onSubmit(text)
            viewModel.submit(text, uiState.filters)
            musicSearchViewModel.onQueryChange(text)
            musicSearchViewModel.performSearch(text)
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    val mediaNavigator = LocalMediaNavigator.current
    val submitWith: (String, SearchFilter) -> Unit = { raw, filters ->
        val text = raw.trim().trimEnd('.', '?', '!', ',', ';')
        val link = parseYouTubeLink(text)
        when {
            text.isEmpty() -> Unit
            link == null -> {
                state.onSubmit(text)
                viewModel.submit(text, filters)
                musicSearchViewModel.onQueryChange(text)
                musicSearchViewModel.performSearch(text)
                focusManager.clearFocus(force = true)
                keyboard?.hide()
            }
            link is YouTubeLink.Search -> submit(link.query)
            !mediaNavigator.openLink(link) -> quickActions.announce(R.string.link_not_supported)
        }
    }

    val showResults = (uiState.query.isNotBlank() || musicQuery.isNotBlank()) && !state.isTyping

    val resetSearch: () -> Unit = {
        state.stopTyping()
        state.textFieldState.setTextAndPlaceCursorAtEnd("")
        viewModel.submit("", SearchFilter())
        musicSearchViewModel.clearSearch()
    }

    val focusRequester = remember { FocusRequester() }
    val hideBottomBar = LocalHideBottomBar.current

    LaunchedEffect(showResults) {
        hideBottomBar.value = showResults
    }

    DisposableEffect(Unit) {
        onDispose {
            hideBottomBar.value = false
        }
    }

    LaunchedEffect(isFocusModeEnabled) {
        if (isFocusModeEnabled && !showResults) {
            state.startTyping()
            kotlinx.coroutines.delay(200)
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    LaunchedEffect(Unit) {
        if (!isFocusModeEnabled && !showResults) {
            state.stopTyping()
        }
    }

    LaunchedEffect(showResults) {
        if (showResults) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    var scrolledForQuery by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(uiState.query) {
        if (uiState.query.isNotBlank() && scrolledForQuery != uiState.query) {
            if (scrolledForQuery != null) gridState.scrollToItem(0)
            scrolledForQuery = uiState.query
        }
    }

    LaunchedEffect(pagingItems.itemSnapshotList.items) {
        pagingItems.itemSnapshotList.items
            .filterIsInstance<SearchResultItem.ChannelResult>()
            .forEach { quickActions.loadSubscriptionState(it.channel.id) }
    }

    BackHandler(enabled = showResults || state.isTyping) {
        if (state.isTyping) {
            state.stopTyping()
        } else if (showResults) {
            resetSearch()
        }
    }

    // Top Bar 56.dp + Category Tabs 44.dp = 100.dp
    val totalTopBarHeight = if (showResults) 100.dp else 56.dp
    val totalTopBarHeightPx = with(density) { totalTopBarHeight.toPx() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    SideEffect {
        if (showResults) {
            if (scrollBehavior.state.heightOffsetLimit != -totalTopBarHeightPx) {
                scrollBehavior.state.heightOffsetLimit = -totalTopBarHeightPx
            }
        } else {
            if (scrollBehavior.state.heightOffsetLimit != 0f) {
                scrollBehavior.state.heightOffsetLimit = 0f
                scrollBehavior.state.heightOffset = 0f
            }
        }
    }

    LaunchedEffect(showResults) {
        if (!showResults) {
            scrollBehavior.state.heightOffset = 0f
        }
    }

    val currentOffsetPx = if (showResults) scrollBehavior.state.heightOffset.roundToInt() else 0

    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Collapsible Top Bar + Category Tabs (Category tabs shown only on results)
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
                                .background(MaterialTheme.colorScheme.background),
                    ) {
                        SearchTopBar(
                            textFieldState = state.textFieldState,
                            onSearch = submit,
                            onBack = if (showResults) resetSearch else onBack,
                            placeholder =
                                if (showResults && pagerState.currentPage == 1) {
                                    stringResource(R.string.search_music_placeholder)
                                } else {
                                    stringResource(R.string.search_videos_channels_placeholder)
                                },
                            focusRequester = focusRequester,
                            onFieldFocused = state::startTyping,
                            actions = null,
                        )

                        if (showResults) {
                            SearchCategoryTabs(
                                pagerState = pagerState,
                                onSelectPage = { page ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(page)
                                    }
                                },
                            )
                        }
                    }
                }

                AnimatedContent(
                    targetState = showResults,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally { it / 4 } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { -it / 4 } + fadeOut(animationSpec = tween(180)))
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn(animationSpec = tween(220)))
                                .togetherWith(slideOutHorizontally { it / 4 } + fadeOut(animationSpec = tween(180)))
                        }
                    },
                    label = "SearchResultsTransition",
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                ) { isResults ->
                    if (!isResults) {
                        // Unified Search Suggestions & Combined History (Single Search Bar, No Category Tabs)
                        SearchSuggestionsPanel(
                            query = state.query,
                            history = combinedHistory,
                            suggestions = if (state.query.isNotBlank()) combinedSuggestions else emptyList(),
                            onSubmit = { text ->
                                state.textFieldState.setTextAndPlaceCursorAtEnd(text)
                                submit(text)
                            },
                            onHistorySelect = { item ->
                                state.textFieldState.setTextAndPlaceCursorAtEnd(item.query)
                                submitWith(item.query, item.filters?.availableWith(state.shortsContentEnabled) ?: uiState.filters)
                            },
                            onFill = state.textFieldState::setTextAndPlaceCursorAtEnd,
                            onDeleteHistoryItem = { item ->
                                state.deleteHistoryItem(item)
                                musicSearchViewModel.deleteHistoryItem(item)
                            },
                            onClearHistory = {
                                state.clearHistory()
                                musicSearchViewModel.clearHistory()
                            },
                            deepFlow = state.deepFlow,
                            onDeepFlowChange = state::setDeepFlowEnabled,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // Swipeable Search Results Pages: Page 0 = Videos, Page 1 = Music
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { page ->
                            if (page == 0) {
                                // VIDEOS RESULT
                                val refreshState = pagingItems.loadState.refresh
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    val feedLayout = rememberFeedGridLayout(maxWidth, state.feedColumns)
                                    val actions =
                                        remember(feedLayout, subscribedIds) {
                                            SearchResultActions(
                                                onVideoClick = onVideoClick,
                                                onChannelClick = onChannelClick,
                                                onPlaylistClick = onPlaylistClick,
                                                dismissKeyboard = state::stopTyping,
                                                isSubscribed = { it in subscribedIds },
                                                onSubscribeToggle = { channel ->
                                                    quickActions.toggleSubscription(channel.id, channel.name, channel.thumbnailUrl)
                                                },
                                            )
                                        }

                                    when {
                                        refreshState is LoadState.Loading -> {
                                            FeedGridSkeleton(layout = feedLayout, listMode = state.isGridMode)
                                        }

                                        refreshState is LoadState.Error && pagingItems.itemCount == 0 -> {
                                            FlowErrorState(
                                                error = refreshState.error.localizedMessage ?: stringResource(R.string.search_failed),
                                                onRetry = pagingItems::retry,
                                            )
                                        }

                                        pagingItems.itemCount == 0 -> {
                                            FlowEmptyState(
                                                title = stringResource(uiState.filters.emptyResultsRes()),
                                                icon = Icons.Rounded.Search,
                                            )
                                        }

                                        else -> {
                                            SearchResults(
                                                pagingItems,
                                                gridState,
                                                feedLayout,
                                                state.isGridMode,
                                                actions,
                                                showSubscribe = !isFocusModeEnabled,
                                            )
                                        }
                                    }
                                }
                            } else {
                                // MUSIC RESULT
                                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                    MusicSearchResultsView(
                                        uiState = musicUiState,
                                        query = state.textFieldState.text.toString().ifBlank { musicQuery },
                                        onTrackClick = onTrackClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onPlaylistClick = onMusicPlaylistClick,
                                        onApplyFilter = musicSearchViewModel::applyFilter,
                                        onRetry = {
                                            val q = state.textFieldState.text.toString().ifBlank { musicQuery }
                                            val filter = musicUiState.activeFilter
                                            if (filter == null) musicSearchViewModel.performSearch(q) else musicSearchViewModel.applyFilter(filter)
                                        },
                                        onLoadMore = musicSearchViewModel::loadMore,
                                        onGetArtistTracks = musicSearchViewModel::getArtistTracks,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Floating Keyboard FAB above Bottom Navigation Bar (Normal Mode only)
            AnimatedVisibility(
                visible = !isFocusModeEnabled && !showResults,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .floatAboveBottomChrome(LocalFlowBottomInsets.current)
                        .padding(end = 20.dp, bottom = 20.dp),
            ) {
                Surface(
                    onClick = {
                        state.startTyping()
                        focusRequester.requestFocus()
                        keyboard?.show()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(56.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = stringResource(R.string.search),
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        SearchFilterDialog(
            filter = uiState.filters,
            shortsEnabled = state.shortsContentEnabled,
            onApply = {
                showFilters = false
                viewModel.updateFilters(it)
            },
            onDismiss = { showFilters = false },
        )
    }
}

/**
 * Centered category switcher with smooth underline indicator tracking HorizontalPager swipe position.
 */
@Composable
private fun SearchCategoryTabs(
    pagerState: PagerState,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val indicatorWidth = 68.dp
    val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
    val tabCenters = remember { mutableStateMapOf<Int, Float>() }
    var rowRootX by remember { mutableFloatStateOf(0f) }

    val currentCenterPx =
        run {
            val c0 = tabCenters[0] ?: 0f
            val c1 = tabCenters[1] ?: 0f
            if (c0 > 0f && c1 > 0f) {
                val progress = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                c0 + (c1 - c0) * progress
            } else {
                tabCenters[pagerState.currentPage] ?: 0f
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { coordinates ->
                    rowRootX = coordinates.positionInRoot().x
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabs =
                listOf(
                    Triple(0, stringResource(R.string.tv_filter_videos), Icons.Rounded.PlayArrow),
                    Triple(1, stringResource(R.string.nav_music), Icons.Rounded.MusicNote),
                )

            tabs.forEachIndexed { index, (page, title, icon) ->
                if (index > 0) {
                    Spacer(modifier = Modifier.width(36.dp))
                }
                val isSelected = pagerState.currentPage == page
                val textColor by animateColorAsState(
                    targetValue =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                    animationSpec = tween(150),
                    label = "searchTabTextColor",
                )

                Row(
                    modifier =
                        Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectPage(page) },
                            ).semantics {
                                role = Role.Tab
                            }.padding(vertical = 8.dp, horizontal = 12.dp)
                            .onGloballyPositioned { coordinates ->
                                val centerXInRoot = coordinates.positionInRoot().x + coordinates.size.width / 2f
                                tabCenters[page] = centerXInRoot - rowRootX
                            },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = textColor,
                    )
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        color = textColor,
                    )
                }
            }
        }

        // Bottom divider line
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        // Sliding indicator line
        if (currentCenterPx > 0f) {
            val leftOffsetPx = (currentCenterPx - indicatorWidthPx / 2f).coerceAtLeast(0f)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset { IntOffset(leftOffsetPx.roundToInt(), 0) }
                        .width(indicatorWidth)
                        .height(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(1.5.dp),
                        ),
            )
        }
    }
}

private fun SearchFilter.emptyResultsRes(): Int =
    when {
        !contentType.supportsVideoFilters() -> R.string.no_results_found
        watchState == WatchState.WATCHED -> R.string.search_watch_empty_watched
        watchState == WatchState.UNWATCHED -> R.string.search_watch_empty_unwatched
        else -> R.string.no_results_found
    }
