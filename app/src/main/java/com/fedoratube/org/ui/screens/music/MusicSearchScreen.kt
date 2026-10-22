package com.fedoratube.org.ui.screens.music

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.local.SearchType
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.innertube.models.AlbumItem
import com.fedoratube.org.innertube.models.ArtistItem
import com.fedoratube.org.innertube.models.PlaylistItem
import com.fedoratube.org.innertube.models.SongItem
import com.fedoratube.org.innertube.models.YTItem
import com.fedoratube.org.innertube.pages.MoodAndGenres
import com.fedoratube.org.innertube.pages.SearchSummaryKind
import com.fedoratube.org.ui.components.layout.flowBottomContentPadding
import com.fedoratube.org.ui.components.music.card.TopResultCard
import com.fedoratube.org.ui.components.music.header.MusicSectionHeader
import com.fedoratube.org.ui.components.music.item.MusicCollectionRow
import com.fedoratube.org.ui.components.music.search.MusicSearchBar
import com.fedoratube.org.ui.components.music.search.SearchFilterChips
import com.fedoratube.org.ui.components.music.search.SearchSuggestionRow
import com.fedoratube.org.ui.components.music.search.searchSummaryTitle
import com.fedoratube.org.ui.components.music.section.MusicMoodsShelf
import com.fedoratube.org.ui.components.music.sheet.LocalMusicMenus
import com.fedoratube.org.ui.components.music.sheet.toCollectionActionItem
import com.fedoratube.org.ui.components.search.searchHistoryItems
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.FlowFeedProgress
import com.fedoratube.org.ui.components.shared.FlowLoadingIndicator
import kotlinx.coroutines.delay

private const val FOCUS_DELAY_MS = 100L
private const val RECOMMENDED_SOURCE = "Recommended"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSearchScreen(
    onBackClick: () -> Unit,
    onTrackClick: (MusicTrack, List<MusicTrack>, String?) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoodClick: (MoodAndGenres.Item) -> Unit,
    onMoodsSeeAll: () -> Unit,
    initialQuery: String? = null,
    viewModel: MusicSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.matchingHistory.collectAsStateWithLifecycle()
    val moods by viewModel.moods.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // When opened with a preset query (e.g. from music recognition), run the search instead of auto-focusing.
    LaunchedEffect(Unit) {
        if (!initialQuery.isNullOrBlank()) {
            viewModel.onQueryChange(initialQuery)
            viewModel.performSearch(initialQuery)
            keyboardController?.hide()
        } else {
            delay(FOCUS_DELAY_MS)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val musicMenus = LocalMusicMenus.current

    fun dismissSearchInput() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
    }

    fun menuActionFor(item: YTItem): (() -> Unit)? =
        when (item) {
            is SongItem -> ({ musicMenus.openSong(convertSongToMusicTrack(item)) })
            is AlbumItem, is PlaylistItem -> ({ item.toCollectionActionItem()?.let(musicMenus::openCollection) })
            else -> null
        }

    fun isDownloaded(item: YTItem): Boolean = (item as? SongItem)?.let { uiState.downloadedTrackIds.contains(it.id) } ?: false

    // A song from search plays alone, as YouTube Music does: the service seeds its radio from it,
    // so Up Next fills with related songs instead of the other results for the same title.
    fun openItem(
        item: YTItem,
        source: String,
    ) {
        dismissSearchInput()
        when (item) {
            is SongItem -> {
                val track = convertSongToMusicTrack(item)
                onTrackClick(track, listOf(track), source)
            }

            is ArtistItem -> {
                onArtistClick(item.id)
            }

            is AlbumItem -> {
                onAlbumClick(item.id)
            }

            is PlaylistItem -> {
                onPlaylistClick(item.id)
            }
        }
    }

    fun playArtistTracks(
        artist: ArtistItem,
        shuffle: Boolean,
        source: String,
    ) {
        viewModel.getArtistTracks(artist.id) { tracks ->
            val musicTracks = tracks.filterIsInstance<SongItem>().map(::convertSongToMusicTrack)
            if (musicTracks.isNotEmpty()) {
                val queue = if (shuffle) musicTracks.shuffled() else musicTracks
                dismissSearchInput()
                onTrackClick(queue.first(), queue, source)
            }
        }
    }

    Scaffold(
        topBar = {
            MusicSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = {
                    viewModel.performSearch()
                    focusManager.clearFocus(force = true)
                },
                onBackClick = onBackClick,
                onClearClick = viewModel::clearSearch,
                focusRequester = focusRequester,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (!uiState.isSearching) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = flowBottomContentPadding()),
                ) {
                    searchHistoryItems(
                        query = query,
                        history = history,
                        onSelect = { item ->
                            viewModel.performSearch(item.query)
                            dismissSearchInput()
                        },
                        onDeleteHistoryItem = viewModel::deleteHistoryItem,
                        onClearHistory = viewModel::clearHistory,
                    )
                    items(uiState.recommendedItems, key = { it.stableLazyKey("recommended") }) { item ->
                        MusicCollectionRow(
                            item = item,
                            onClick = { openItem(item, RECOMMENDED_SOURCE) },
                            onMenuClick = menuActionFor(item),
                            onLongClick = menuActionFor(item),
                            isDownloaded = isDownloaded(item),
                        )
                    }
                    items(uiState.suggestions, key = { it }) { suggestion ->
                        SearchSuggestionRow(
                            suggestion = suggestion,
                            onClick = {
                                viewModel.performSearch(suggestion)
                                keyboardController?.hide()
                            },
                        )
                    }
                    if (query.isBlank()) {
                        item(key = "moods") {
                            MusicMoodsShelf(
                                moods = moods,
                                onMoodClick = { mood ->
                                    dismissSearchInput()
                                    onMoodClick(mood)
                                },
                                onSeeAll = {
                                    dismissSearchInput()
                                    onMoodsSeeAll()
                                },
                            )
                        }
                    }
                }
            } else {
                SearchFilterChips(
                    activeFilter = uiState.activeFilter,
                    onFilterClick = viewModel::applyFilter,
                )

                val searchSource = stringResource(R.string.search_source_template).format(query)
                val artistSourceTemplate = stringResource(R.string.artist_source_template)

                val summaries = uiState.searchSummary?.summaries
                val hasResults =
                    if (uiState.activeFilter == null) {
                        summaries?.any { it.items.isNotEmpty() } == true
                    } else {
                        uiState.filteredResults.isNotEmpty()
                    }

                val error = uiState.error
                if (uiState.isLoading) {
                    FlowLoadingIndicator()
                } else if (!hasResults && error != null) {
                    FlowErrorState(
                        error = error,
                        onRetry = {
                            val filter = uiState.activeFilter
                            if (filter == null) viewModel.performSearch(query) else viewModel.applyFilter(filter)
                        },
                    )
                } else if (!hasResults) {
                    FlowEmptyState(
                        title = stringResource(R.string.music_search_no_results, query),
                        icon = Icons.Rounded.SearchOff,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = flowBottomContentPadding()),
                    ) {
                        if (uiState.activeFilter == null && summaries != null) {
                            summaries.forEachIndexed { index, summary ->
                                item(key = "summary_header_$index") {
                                    MusicSectionHeader(title = searchSummaryTitle(summary))
                                }

                                val isTopResult = summary.kind == SearchSummaryKind.TOP_RESULT
                                if (isTopResult) {
                                    val topItem = summary.items.first()
                                    item(key = "top_result") {
                                        TopResultCard(
                                            item = topItem,
                                            onClick = { openItem(topItem, searchSource) },
                                            onShuffleClick = {
                                                if (topItem is ArtistItem) {
                                                    playArtistTracks(topItem, shuffle = true, artistSourceTemplate.format(topItem.title))
                                                }
                                            },
                                            onRadioClick = {
                                                if (topItem is ArtistItem) {
                                                    playArtistTracks(topItem, shuffle = false, artistSourceTemplate.format(topItem.title))
                                                }
                                            },
                                            onLongClick = menuActionFor(topItem),
                                            onMenuClick = menuActionFor(topItem),
                                        )
                                    }
                                }

                                items(
                                    items = if (isTopResult) summary.items.drop(1) else summary.items,
                                    key = { it.stableLazyKey("summary_${summary.kind}_${summary.title}") },
                                ) { item ->
                                    MusicCollectionRow(
                                        showPlayCount = true,
                                        item = item,
                                        onClick = { openItem(item, searchSource) },
                                        onMenuClick = menuActionFor(item),
                                        onLongClick = menuActionFor(item),
                                        isDownloaded = isDownloaded(item),
                                    )
                                }
                            }
                        } else {
                            items(uiState.filteredResults, key = { it.stableLazyKey("filtered") }) { item ->
                                MusicCollectionRow(
                                    showPlayCount = true,
                                    item = item,
                                    onClick = { openItem(item, searchSource) },
                                    onMenuClick = menuActionFor(item),
                                    onLongClick = menuActionFor(item),
                                    isDownloaded = isDownloaded(item),
                                )
                            }
                        }

                        if (uiState.continuation != null) {
                            item(key = "continuation") {
                                LaunchedEffect(Unit) {
                                    viewModel.loadMore()
                                }
                                if (uiState.isMoreLoading) {
                                    FlowFeedProgress()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to convert SongItem to MusicTrack (shared with the TV search screen)
internal fun convertSongToMusicTrack(item: SongItem): MusicTrack =
    MusicTrack(
        videoId = item.id,
        title = item.title,
        artist = item.artists.joinToString { it.name },
        thumbnailUrl = item.thumbnail,
        duration = item.duration ?: 0,
        views = 0, // View count text is a string in SongItem
        sourceUrl = "https://www.youtube.com/watch?v=${item.id}",
        album = item.album?.name ?: "Unknown Album",
        channelId = item.artists.firstOrNull()?.id ?: "",
        isExplicit = item.explicit,
        isVideoSong = item.isVideoSong,
    )
