package com.fedoratube.org.ui.screens.music

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fedoratube.org.R
import com.fedoratube.org.data.local.SearchHistoryItem
import com.fedoratube.org.data.local.SearchHistoryRepository
import com.fedoratube.org.data.local.SearchHistoryScope
import com.fedoratube.org.data.local.SearchType
import com.fedoratube.org.data.model.distinctByNonBlankKey
import com.fedoratube.org.data.music.DownloadManager
import com.fedoratube.org.data.newmusic.InnertubeMusicService
import com.fedoratube.org.innertube.YouTube
import com.fedoratube.org.innertube.YouTube.SearchFilter
import com.fedoratube.org.innertube.models.SearchSuggestions
import com.fedoratube.org.innertube.models.YTItem
import com.fedoratube.org.innertube.pages.ArtistSectionKind
import com.fedoratube.org.innertube.pages.MoodAndGenres
import com.fedoratube.org.innertube.pages.SearchSummary
import com.fedoratube.org.innertube.pages.SearchSummaryKind
import com.fedoratube.org.innertube.pages.SearchSummaryPage
import com.fedoratube.org.ui.components.search.matchingTyped
import com.fedoratube.org.utils.PerformanceDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MusicSearchViewModel
    @Inject
    constructor(
        private val downloadManager: DownloadManager,
        private val searchHistory: SearchHistoryRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _query = MutableStateFlow("")
        val query: StateFlow<String> = _query.asStateFlow()

        val matchingHistory: StateFlow<List<SearchHistoryItem>> =
            combine(searchHistory.getSearchHistoryFlow(SearchHistoryScope.MUSIC), _query) { history, typed ->
                history.matchingTyped(typed)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        private val _moods = MutableStateFlow<List<MoodAndGenres>>(emptyList())
        val moods: StateFlow<List<MoodAndGenres>> = _moods.asStateFlow()

        private val _uiState = MutableStateFlow(MusicSearchUiState())
        val uiState: StateFlow<MusicSearchUiState> = _uiState.asStateFlow()

        init {
            // Handle search suggestions with debounce
            _query
                .debounce(300)
                .filter { it.isNotBlank() && searchHistory.isSearchSuggestionsEnabled() }
                .onEach { q ->
                    fetchSuggestions(q)
                }.launchIn(viewModelScope)

            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                _moods.value = InnertubeMusicService.fetchMoodAndGenres()
            }

            viewModelScope.launch {
                downloadManager.downloadedTracks.collect { tracks ->
                    _uiState.update { state ->
                        state.copy(downloadedTrackIds = tracks.map { it.track.videoId }.toSet())
                    }
                }
            }
        }

        fun onQueryChange(newQuery: String) {
            _query.value = newQuery
            if (newQuery.isBlank()) {
                _uiState.update { it.copy(suggestions = emptyList(), recommendedItems = emptyList()) }
            }
        }

        /**
         *  PERFORMANCE OPTIMIZED: Fetch suggestions with timeout
         */
        private fun fetchSuggestions(q: String) {
            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                val result =
                    withTimeoutOrNull(5_000L) {
                        YouTube.searchSuggestions(q)
                    }

                result
                    ?.onSuccess { suggestions ->
                        _uiState.update {
                            it.copy(
                                suggestions =
                                    suggestions.queries.distinctByNonBlankKey { query ->
                                        query.trim().lowercase()
                                    },
                                recommendedItems = suggestions.recommendedItems.distinctByStableIdentity(),
                            )
                        }
                    }?.onFailure { throwable ->
                        android.util.Log.w("MusicSearchViewModel", "Suggestions failed: ${throwable.message}")
                        _uiState.update { it.copy(suggestions = emptyList(), recommendedItems = emptyList()) }
                    }
            }
        }

        /**
         *  PERFORMANCE OPTIMIZED: Perform search with timeout protection
         */
        fun performSearch(
            q: String = _query.value,
            type: SearchType = SearchType.TEXT,
        ) {
            val cleanQ = q.trim().trimEnd('.', '?', '!', ',', ';')
            if (cleanQ.isBlank()) return

            _query.value = cleanQ
            viewModelScope.launch { searchHistory.saveSearchQuery(cleanQ, type, SearchHistoryScope.MUSIC) }
            _uiState.update { it.copy(isLoading = true, isSearching = true, activeFilter = null, error = null) }

            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                val result =
                    withTimeoutOrNull(15_000L) {
                        YouTube.searchSummary(cleanQ)
                    }

                var summaryPage = result?.getOrNull()
                if (summaryPage == null || summaryPage.summaries.isEmpty() || summaryPage.summaries.all { it.items.isEmpty() }) {
                    val fallbackResult =
                        withTimeoutOrNull(12_000L) {
                            YouTube.search(cleanQ, SearchFilter.FILTER_SONG)
                        }?.getOrNull()

                    if (fallbackResult != null && fallbackResult.items.isNotEmpty()) {
                        summaryPage =
                            SearchSummaryPage(
                                summaries =
                                    listOf(
                                        SearchSummary(
                                            title = context.getString(R.string.filter_songs),
                                            items = fallbackResult.items,
                                            kind = SearchSummaryKind.SONGS,
                                        ),
                                    ),
                                continuation = fallbackResult.continuation,
                            )
                    }
                }

                if (summaryPage != null && summaryPage.summaries.any { it.items.isNotEmpty() }) {
                    _uiState.update { state ->
                        state.copy(
                            searchSummary = summaryPage.distinctItemsForLazyKeys(),
                            isLoading = false,
                            isSearching = true,
                            continuation = summaryPage.continuation,
                        )
                    }
                } else if (result?.isFailure == true) {
                    _uiState.update { state -> state.copy(isLoading = false, error = context.getString(R.string.search_failed)) }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            searchSummary = SearchSummaryPage(emptyList()),
                            isLoading = false,
                            isSearching = true,
                        )
                    }
                }
            }
        }

        /**
         *  PERFORMANCE OPTIMIZED: Apply filter with timeout protection
         */
        fun applyFilter(filter: SearchFilter?) {
            val q = _query.value
            if (q.isBlank()) return

            _uiState.update { state -> state.copy(isLoading = true, isSearching = true, activeFilter = filter, error = null) }

            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                if (filter == null) {
                    performSearch(q)
                } else {
                    val result =
                        withTimeoutOrNull(12_000L) {
                            YouTube.search(q, filter)
                        }

                    result
                        ?.onSuccess { searchResult ->
                            _uiState.update { state ->
                                state.copy(
                                    filteredResults = searchResult.items.distinctByStableIdentity(),
                                    isLoading = false,
                                    continuation = searchResult.continuation,
                                )
                            }
                        }?.onFailure { throwable ->
                            android.util.Log.w("MusicSearchViewModel", "Filtered search failed", throwable)
                            _uiState.update { state -> state.copy(isLoading = false, error = context.getString(R.string.search_failed)) }
                        } ?: run {
                        _uiState.update { state ->
                            state.copy(isLoading = false, error = context.getString(R.string.error_filter_search_timed_out))
                        }
                    }
                }
            }
        }

        fun deleteHistoryItem(item: SearchHistoryItem) {
            viewModelScope.launch { searchHistory.deleteSearchItem(item.id, SearchHistoryScope.MUSIC) }
        }

        fun clearHistory() {
            viewModelScope.launch { searchHistory.clearSearchHistory(SearchHistoryScope.MUSIC) }
        }

        fun clearSearch() {
            _query.value = ""
            _uiState.value = MusicSearchUiState()
        }

        /**
         *  PERFORMANCE OPTIMIZED: Get artist tracks with timeout
         */
        fun getArtistTracks(
            artistId: String,
            callback: (List<YTItem>) -> Unit,
        ) {
            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                val result =
                    withTimeoutOrNull(10_000L) {
                        YouTube.artist(artistId)
                    }

                result?.onSuccess { artistPage ->
                    val songsSection = artistPage.sections.find { it.kind == ArtistSectionKind.TOP_SONGS }
                    val items = songsSection?.items ?: artistPage.sections.firstOrNull()?.items ?: emptyList()
                    withContext(Dispatchers.Main) {
                        callback(items.distinctByStableIdentity())
                    }
                }
            }
        }

        fun loadMore() {
            val token = _uiState.value.continuation ?: return
            if (_uiState.value.isMoreLoading) return

            viewModelScope.launch(PerformanceDispatcher.networkIO) {
                _uiState.update { it.copy(isMoreLoading = true) }
                val result = YouTube.searchContinuation(token)

                result
                    .onSuccess { searchResult ->
                        _uiState.update { state ->
                            if (state.activeFilter == null) {
                                val newSummary =
                                    com.fedoratube.org.innertube.pages.SearchSummary(
                                        title = context.getString(R.string.fallback_more_results),
                                        items = searchResult.items.distinctByStableIdentity(),
                                    )
                                state.copy(
                                    searchSummary =
                                        state.searchSummary
                                            ?.copy(
                                                summaries = state.searchSummary.summaries + newSummary,
                                            )?.distinctItemsForLazyKeys(),
                                    continuation = searchResult.continuation,
                                    isMoreLoading = false,
                                )
                            } else {
                                state.copy(
                                    filteredResults =
                                        (
                                            state.filteredResults + searchResult.items
                                        ).distinctByStableIdentity(),
                                    continuation = searchResult.continuation,
                                    isMoreLoading = false,
                                )
                            }
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isMoreLoading = false) }
                    }
            }
        }
    }

data class MusicSearchUiState(
    val suggestions: List<String> = emptyList(),
    val recommendedItems: List<YTItem> = emptyList(),
    val searchSummary: SearchSummaryPage? = null,
    val filteredResults: List<YTItem> = emptyList(),
    val activeFilter: SearchFilter? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val error: String? = null,
    val continuation: String? = null,
    val isMoreLoading: Boolean = false,
    val downloadedTrackIds: Set<String> = emptySet(),
)
