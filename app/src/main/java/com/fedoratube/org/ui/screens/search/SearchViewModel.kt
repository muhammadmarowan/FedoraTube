@file:Suppress("ktlint:standard:backing-property-naming")

package com.fedoratube.org.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.local.SearchFilter
import com.fedoratube.org.data.local.SearchHistoryRepository
import com.fedoratube.org.data.local.dao.WatchHistoryDao
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.paging.SearchPagingSource
import com.fedoratube.org.data.paging.SearchResultItem
import com.fedoratube.org.data.paging.WatchedVideoLookup
import com.fedoratube.org.data.paging.watchedVideoIds
import com.fedoratube.org.data.recommendation.FlowNeuroEngine
import com.fedoratube.org.data.search.SearchSuggestionsRepository
import com.fedoratube.org.data.shorts.ShortsContentFilter
import com.fedoratube.org.data.shorts.queue.ShortsQueueHandoff
import com.fedoratube.org.data.shorts.queue.ShortsQueueSource
import com.fedoratube.org.data.stats.VideoStatsRecorder
import com.fedoratube.org.innertube.pages.search.SearchHeader
import com.fedoratube.org.innertube.pages.search.SearchSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filters: SearchFilter = SearchFilter.DEFAULT,
    val header: SearchHeader = SearchHeader(),
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val suggestionsRepository: SearchSuggestionsRepository,
        private val shortsContentFilter: ShortsContentFilter,
        private val shortsQueueHandoff: ShortsQueueHandoff,
        private val searchHistory: SearchHistoryRepository,
        private val videoStats: VideoStatsRecorder,
        private val neuroEngine: FlowNeuroEngine,
        private val watchHistoryDao: WatchHistoryDao,
        private val preferences: PlayerPreferences,
    ) : ViewModel() {
        // Signal each distinct submitted query once — typing and filter churn stay silent.
        private var lastSignaledQuery: String? = null
        private val _uiState = MutableStateFlow(SearchUiState())
        val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

        private data class SearchKey(
            val query: String,
            val filter: SearchFilter,
        )

        private val _searchKey = MutableStateFlow<SearchKey?>(null)

        val searchResults: Flow<PagingData<SearchResultItem>> =
            _searchKey
                .filterNotNull()
                .filter { it.query.isNotBlank() }
                .combine(shortsContentFilter.enabled) { key, shortsEnabled -> key to shortsEnabled }
                .flatMapLatest { (key, shortsEnabled) ->
                    Pager(
                        config =
                            PagingConfig(
                                pageSize = 20,
                                prefetchDistance = 6,
                                enablePlaceholders = false,
                                initialLoadSize = 20,
                            ),
                        pagingSourceFactory = {
                            SearchPagingSource(
                                query = key.query,
                                filter = key.filter,
                                shortsEnabled = shortsEnabled,
                                onHeader = ::onHeader,
                                watchedVideos = watchedVideos,
                                exclusions = { neuroEngine.feedExclusions() },
                            )
                        },
                    ).flow
                }.cachedIn(viewModelScope)

        private val watchedVideos =
            WatchedVideoLookup { videos ->
                if (videos.isEmpty()) return@WatchedVideoLookup emptySet()
                val progress = watchHistoryDao.getProgressFor(videos.map { it.id }.distinct())
                watchedVideoIds(videos, progress, preferences.watchedThreshold.first())
            }

        fun shortsShelfSource(
            shelf: List<Video>,
            tapped: Video,
        ): ShortsQueueSource = shortsQueueHandoff.sourceForShelf(shelf, tapped)

        fun search(
            query: String,
            filters: SearchFilter = SearchFilter.DEFAULT,
        ) {
            if (query.isBlank()) {
                clearSearch()
                return
            }
            _uiState.value = SearchUiState(query = query, filters = filters)
            _searchKey.value = SearchKey(query, filters)

            // A typed search is the most explicit interest statement the user makes.
            val normalized = query.trim().lowercase()
            if (normalized != lastSignaledQuery) {
                lastSignaledQuery = normalized
                viewModelScope.launch {
                    runCatching { FlowNeuroEngine.onSearchQuery(context, query) }
                    videoStats.onSearch(query.takeIf { searchHistory.isSearchHistoryEnabled() })
                }
            }
        }

        /** A search the user asked for, kept in history with its filters. Typing on TV goes through [search] and is not kept. */
        fun submit(
            query: String,
            filters: SearchFilter,
        ) {
            search(query, filters)
            rememberSearch(query, filters)
        }

        fun onSearchHistoryCleared() = videoStats.onSearchHistoryCleared()

        fun updateFilters(filters: SearchFilter) {
            val currentQuery = _uiState.value.query
            _uiState.value = _uiState.value.copy(filters = filters)
            if (currentQuery.isNotBlank()) {
                _searchKey.value = SearchKey(currentQuery, filters)
                rememberSearch(currentQuery, filters)
            }
        }

        private fun rememberSearch(
            query: String,
            filters: SearchFilter,
        ) {
            if (query.isBlank()) return
            viewModelScope.launch { searchHistory.saveSearchQuery(query, filters = filters) }
        }

        fun clearSearch() {
            _uiState.value = SearchUiState()
            _searchKey.value = null
        }

        suspend fun getSearchSuggestions(query: String): List<SearchSuggestion> =
            runCatching { suggestionsRepository.suggestions(query) }.getOrDefault(emptyList())

        private fun onHeader(header: SearchHeader) {
            _uiState.value = _uiState.value.copy(header = header)
        }
    }
