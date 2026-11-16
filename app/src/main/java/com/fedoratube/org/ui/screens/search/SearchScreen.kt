package com.fedoratube.org.ui.screens.search

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.fedoratube.org.R
import com.fedoratube.org.data.local.ContentType
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

enum class SearchTabCategory {
    VIDEOS,
    MUSIC,
}

/**
 * Modern Search destination supporting both Video and Music categories, designed
 * as a first-class navigation tab without unexpected auto-keyboard popups.
 */
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
    var selectedCategory by rememberSaveable { mutableStateOf(SearchTabCategory.VIDEOS) }
    val uiState by viewModel.uiState.collectAsState()
    val musicUiState by musicSearchViewModel.uiState.collectAsState()
    val musicQuery by musicSearchViewModel.query.collectAsState()
    val state = rememberSearchState(viewModel)
    val focusPreferences = remember(context) { com.fedoratube.org.data.local.PlayerPreferences(context) }
    val isFocusModeEnabled by focusPreferences.focusModeEnabled.collectAsState(initial = false)

    val quickActions: QuickActionsViewModel = sharedQuickActionsViewModel()
    val subscribedIds by quickActions.subscribedChannelIds.collectAsStateWithLifecycle()
    val pagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    OnTabReselected(FlowTab.Search.route) { gridState.animateScrollToItem(0) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val mediaNavigator = LocalMediaNavigator.current
    val search: (String, SearchFilter) -> Unit = { query, filters ->
        val clean = query.trim().trimEnd('.', '?', '!', ',', ';')
        state.onSubmit(clean)
        viewModel.submit(clean, filters)
    }
    val submitWith: (String, SearchFilter) -> Unit = { raw, filters ->
        val text = raw.trim().trimEnd('.', '?', '!', ',', ';')
        val link = parseYouTubeLink(text)
        when {
            text.isEmpty() -> Unit
            link == null -> search(text, filters)
            link is YouTubeLink.Search -> search(link.query, filters)
            !mediaNavigator.openLink(link) -> quickActions.announce(R.string.link_not_supported)
        }
    }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val submit: (String) -> Unit = { raw ->
        if (selectedCategory == SearchTabCategory.VIDEOS) {
            submitWith(raw, uiState.filters)
        } else {
            val text = raw.trim().trimEnd('.', '?', '!', ',', ';')
            state.onSubmit(text)
            musicSearchViewModel.onQueryChange(text)
            musicSearchViewModel.performSearch(text)
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    val showResults =
        if (selectedCategory == SearchTabCategory.VIDEOS) {
            uiState.query.isNotBlank() && !state.isTyping
        } else {
            (musicUiState.searchSummary != null || musicUiState.filteredResults.isNotEmpty()) && !state.isTyping
        }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(showResults) {
        if (showResults) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    // A new query starts at the top; coming back from a result keeps the grid where it was.
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

    // Back leaves the screen or dismisses typing
    BackHandler(enabled = state.isTyping && (uiState.query.isNotBlank() || musicQuery.isNotBlank())) {
        state.stopTyping()
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchTopBar(
                textFieldState = state.textFieldState,
                onSearch = submit,
                onBack = onBack,
                placeholder =
                    if (selectedCategory == SearchTabCategory.VIDEOS) {
                        stringResource(R.string.search_videos_channels_placeholder)
                    } else {
                        stringResource(R.string.search_music_placeholder)
                    },
                focusRequester = focusRequester,
                onFieldFocused = state::startTyping,
                actions = null,
            )

            // Category selector tabs (Videos & Music)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedCategory == SearchTabCategory.VIDEOS,
                    onClick = {
                        if (selectedCategory != SearchTabCategory.VIDEOS) {
                            selectedCategory = SearchTabCategory.VIDEOS
                            val text = state.textFieldState.text.toString().trim()
                            if (text.isNotEmpty()) {
                                submit(text)
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.tv_filter_videos)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.SmartDisplay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    shape = CircleShape,
                )

                FilterChip(
                    selected = selectedCategory == SearchTabCategory.MUSIC,
                    onClick = {
                        if (selectedCategory != SearchTabCategory.MUSIC) {
                            selectedCategory = SearchTabCategory.MUSIC
                            val text = state.textFieldState.text.toString().trim()
                            if (text.isNotEmpty()) {
                                state.onSubmit(text)
                                musicSearchViewModel.onQueryChange(text)
                                musicSearchViewModel.performSearch(text)
                                focusManager.clearFocus(force = true)
                                keyboard?.hide()
                            }
                        }
                    },
                    label = { Text(stringResource(R.string.nav_music)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    shape = CircleShape,
                )
            }

            if (selectedCategory == SearchTabCategory.VIDEOS) {
                if (!showResults) {
                    SearchSuggestionsPanel(
                        query = state.query,
                        history = state.matchingHistory,
                        suggestions = state.suggestions,
                        onSubmit = { text ->
                            state.textFieldState.setTextAndPlaceCursorAtEnd(text)
                            submit(text)
                        },
                        onHistorySelect = { item ->
                            state.textFieldState.setTextAndPlaceCursorAtEnd(item.query)
                            submitWith(item.query, item.filters?.availableWith(state.shortsContentEnabled) ?: uiState.filters)
                        },
                        onFill = state.textFieldState::setTextAndPlaceCursorAtEnd,
                        onDeleteHistoryItem = state::deleteHistoryItem,
                        onClearHistory = state::clearHistory,
                        deepFlow = state.deepFlow,
                        onDeepFlowChange = state::setDeepFlowEnabled,
                    )
                    return@Column
                }

                val refreshState = pagingItems.loadState.refresh
                BoxWithConstraints(modifier = Modifier.weight(1f)) {
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
                // MUSIC MODE
                if (!showResults) {
                    SearchSuggestionsPanel(
                        query = state.query,
                        history = state.matchingHistory,
                        suggestions =
                            if (musicUiState.suggestions.isNotEmpty()) {
                                musicUiState.suggestions.map { com.fedoratube.org.innertube.pages.search.SearchSuggestion(it) }
                            } else {
                                state.suggestions
                            },
                        onSubmit = { text ->
                            state.textFieldState.setTextAndPlaceCursorAtEnd(text)
                            submit(text)
                        },
                        onHistorySelect = { item ->
                            state.textFieldState.setTextAndPlaceCursorAtEnd(item.query)
                            submit(item.query)
                        },
                        onFill = state.textFieldState::setTextAndPlaceCursorAtEnd,
                        onDeleteHistoryItem = state::deleteHistoryItem,
                        onClearHistory = state::clearHistory,
                        deepFlow = state.deepFlow,
                        onDeepFlowChange = state::setDeepFlowEnabled,
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.weight(1f)) {
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

private fun SearchFilter.emptyResultsRes(): Int =
    when {
        !contentType.supportsVideoFilters() -> R.string.no_results_found
        watchState == WatchState.WATCHED -> R.string.search_watch_empty_watched
        watchState == WatchState.UNWATCHED -> R.string.search_watch_empty_unwatched
        else -> R.string.no_results_found
    }
