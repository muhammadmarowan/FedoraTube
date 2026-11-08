package com.fedoratube.org.ui.screens.shorts

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fedoratube.org.data.comments.CommentsPageResult
import com.fedoratube.org.data.feed.ShortsQueueSource
import com.fedoratube.org.data.feed.ShortsSnapshotStore
import com.fedoratube.org.data.feed.continuesIntoFeed
import com.fedoratube.org.data.feed.openAtVideoId
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.local.PlaylistRepository
import com.fedoratube.org.data.local.SubscriptionRepository
import com.fedoratube.org.data.local.ViewHistory
import com.fedoratube.org.data.model.Comment
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.model.distinctByNonBlankKey
import com.fedoratube.org.data.model.mergeDistinctByNonBlankKey
import com.fedoratube.org.data.repository.YouTubeRepository
import com.fedoratube.org.innertube.YouTube
import com.fedoratube.org.innertube.pages.channel.ChannelShortsPage
import com.fedoratube.org.innertube.pages.reel.ReelEntry
import com.fedoratube.org.innertube.pages.reel.ReelLockup
import com.fedoratube.org.innertube.pages.reel.reelPosterUrl
import com.fedoratube.org.ui.youtubeChannelBrowseId
import com.fedoratube.org.ui.youtubeChannelUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One reel in the Shorts pager, with whatever its source already knew about it. */
data class ShortItem(
    val videoId: String,
    val title: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val channelAvatarUrl: String = "",
    val viewCountText: String? = null,
    val likeCountText: String? = null,
    val commentCountText: String? = null,
    val posterUrl: String? = null,
    val soundTitle: String? = null,
)

data class ShortsUiState(
    val items: List<ShortItem> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    /** Pull-to-refresh (or refresh button) running while the current feed stays on screen. */
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val endReached: Boolean = false,
)

/** The inline comments sheet for the Short on screen. */
data class ShortsCommentsState(
    val videoId: String = "",
    val comments: List<Comment> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val totalText: String? = null,
)

/**
 * Loads the Shorts queue for one [ShortsQueueSource]: the interest-based algorithmic feed, a
 * channel's Shorts tab, the subscribed channels' Shorts, a shelf snapshot (search topic,
 * subscriptions strip), or the saved Shorts. Overlays (title, channel, counts) are filled in
 * per reel through `reel_item_watch`, the way the reel sequence expects.
 */
@HiltViewModel
class ShortsViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: PlayerPreferences,
        private val subscriptionRepository: SubscriptionRepository,
        private val playlistRepository: PlaylistRepository,
        private val youtubeRepository: YouTubeRepository,
        private val viewHistory: ViewHistory,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ShortsUiState())
        val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

        private val _commentsState = MutableStateFlow(ShortsCommentsState())
        val commentsState: StateFlow<ShortsCommentsState> = _commentsState.asStateFlow()
        private var commentsContinuation: String? = null
        private var commentsJob: Job? = null
        private val repliesInFlight = mutableSetOf<String>()

        private var source: ShortsQueueSource = ShortsQueueSource.Feed
        private var continuation: String? = null
        private var loadJob: Job? = null
        private var exhausted = false

        fun load(
            newSource: ShortsQueueSource,
            force: Boolean = false,
        ) {
            if (!force && loadJob?.isActive == true && source == newSource && _uiState.value.items.isNotEmpty()) return
            source = newSource
            continuation = null
            exhausted = false
            // Re-entering the Shorts tab shows the last feed instantly from cache while a
            // fresh page loads behind it — no loading wait on the way back in.
            val instant =
                if (newSource == ShortsQueueSource.Feed) cachedFeedItems else null
            _uiState.value =
                if (instant.isNullOrEmpty()) {
                    ShortsUiState(isLoading = true)
                } else {
                    continuation = cachedFeedContinuation
                    ShortsUiState(items = instant, isLoading = false)
                }
            loadJob =
                viewModelScope.launch {
                    try {
                        val items = initialItems(newSource)
                        _uiState.update { it.copy(items = items, isLoading = false, error = null) }
                    } catch (e: Exception) {
                        Log.w(TAG, "Shorts load failed: ${e.message}")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = if (it.items.isEmpty()) (e.message ?: "Failed to load shorts") else it.error,
                            )
                        }
                    }
                }
        }

        /**
         * The viewer's refresh, like Home feed pull-to-refresh: the current feed stays on screen
         * (no loading takeover), a fresh first page replaces it when it lands, and repeated pulls
         * cannot stack while one is already running.
         */
        fun refresh() {
            if (_uiState.value.isLoading || _uiState.value.isRefreshing || loadJob?.isActive == true) return
            _uiState.update { it.copy(isRefreshing = true) }
            loadJob =
                viewModelScope.launch {
                    try {
                        val items = initialItems(source)
                        _uiState.update {
                            it.copy(
                                items = items,
                                isRefreshing = false,
                                isLoading = false,
                                error = null,
                                endReached = false,
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Shorts refresh failed: ${e.message}")
                        // Keep the feed already on screen; only surface the error when there is none.
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                error = if (it.items.isEmpty()) (e.message ?: "Failed to load shorts") else it.error,
                            )
                        }
                    }
                }
        }

        /**
         * Watching a Short writes a history row (isShort), like the regular player does on
         * open — without this, watched Shorts never showed up in History at all. Blank
         * fields (feed items arrive title-less) leave any richer row already on record,
         * and History enriches stubs itself.
         */
        fun recordWatch(item: ShortItem) {
            viewModelScope.launch {
                runCatching {
                    viewHistory.touchHistoryEntry(
                        videoId = item.videoId,
                        title = item.title,
                        thumbnailUrl = item.posterUrl.orEmpty(),
                        channelName = item.channelName,
                        channelId = item.channelId,
                        isShort = true,
                    )
                }
            }
        }

        fun loadMore() {
            if (_uiState.value.isLoading || _uiState.value.isLoadingMore || exhausted) return
            val cont = continuation
            val src = source
            // A snapshot or channel queue that ran dry continues into the algorithmic feed, so a
            // swipe never dead-ends. Saved Shorts is finite by design and stops here.
            if (cont == null && !src.continuesIntoFeed) {
                exhausted = true
                _uiState.update { it.copy(endReached = true) }
                return
            }
            _uiState.update { it.copy(isLoadingMore = true) }
            viewModelScope.launch {
                try {
                    val page =
                        if (cont != null) {
                            nextPage(src, cont)
                        } else {
                            val feed = YouTube.shorts().getOrThrow()
                            FeedPage(feed.entries.map { it.toItem() }, feed.continuation)
                        }
                    continuation = page.continuation
                    val newItems = page.items.filter { candidate -> _uiState.value.items.none { it.videoId == candidate.videoId } }
                    _uiState.update {
                        it.copy(
                            items = it.items + newItems,
                            isLoadingMore = false,
                            endReached = continuation == null && !src.continuesIntoFeed,
                        )
                    }
                    if (continuation == null && !src.continuesIntoFeed) exhausted = true
                } catch (e: Exception) {
                    Log.w(TAG, "Shorts load more failed: ${e.message}")
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }

        private suspend fun initialItems(src: ShortsQueueSource): List<ShortItem> =
            when (src) {
                is ShortsQueueSource.Feed -> feedPage(null)
                is ShortsQueueSource.SeededFeed -> {
                    val seed = overlayItem(src.startVideoId)
                    val page = YouTube.shortsFromVideo(src.startVideoId).getOrThrow()
                    continuation = page.continuation
                    (listOfNotNull(seed) + page.entries.map { it.toItem() }).distinctBy { it.videoId }
                }

                is ShortsQueueSource.Saved -> {
                    val saved = playlistRepository.getSavedShortsFlow().first()
                    saved.toItems(src.startVideoId)
                }

                is ShortsQueueSource.Channel -> channelItems(src)
                is ShortsQueueSource.Subscriptions -> subscriptionItems(src.startVideoId)
                is ShortsQueueSource.Snapshot -> {
                    val shelf = ShortsSnapshotStore.take(src.token)
                    if (shelf.isNullOrEmpty()) {
                        feedPage(null)
                    } else {
                        shelf.toItems(src.startVideoId)
                    }
                }
            }

        private suspend fun feedPage(sequenceParams: String?): List<ShortItem> {
            val page = YouTube.shorts(sequenceParams).getOrThrow()
            continuation = page.continuation
            // Expose the first page immediately — filling to a minimum first made entering
            // Shorts wait through several sequential page fetches. The pager is index-safe,
            // so a 1-item first page is fine; loadMore appends the rest.
            val items = page.entries.map { it.toItem() }.distinctBy { it.videoId }
            if (sequenceParams == null) {
                cachedFeedItems = items
                cachedFeedContinuation = continuation
            }
            return items
        }

        private suspend fun nextPage(
            src: ShortsQueueSource,
            cont: String,
        ): FeedPage {
            return when (src) {
                is ShortsQueueSource.Channel -> {
                    val page = YouTube.channelShortsContinuation(cont).getOrThrow()
                    continuation = page.continuation
                    FeedPage(page.shorts.map { it.toItem(page) }, page.continuation)
                }

                else -> {
                    val page = YouTube.shorts(cont).getOrThrow()
                    continuation = page.continuation
                    FeedPage(page.entries.map { it.toItem() }, page.continuation)
                }
            }
        }

        private suspend fun channelItems(src: ShortsQueueSource.Channel): List<ShortItem> {
            val browseId =
                youtubeChannelBrowseId(src.channelUrl)
                    ?: youtubeChannelUrl(src.channelUrl)?.let { YouTube.resolveChannelId(it).getOrNull() }
            if (browseId.isNullOrBlank()) throw IllegalStateException("Could not resolve channel")
            val first = YouTube.channelShorts(browseId).getOrThrow()
            continuation = first.continuation
            var items = first.shorts.map { it.toItem(first) }
            // A non-default sort chip is just a continuation that re-sorts the tab.
            if (src.sortIndex > 0) {
                val token = first.sorts.getOrNull(src.sortIndex)?.token
                if (!token.isNullOrBlank()) {
                    val sorted = YouTube.channelShortsContinuation(token).getOrThrow()
                    continuation = sorted.continuation
                    items = sorted.shorts.map { it.toItem(sorted) }
                }
            }
            val start = src.startVideoId
            if (start.isNotBlank()) {
                val index = items.indexOfFirst { it.videoId == start }
                if (index > 0) items = items.drop(index) + items.take(index)
            }
            return items.distinctBy { it.videoId }
        }

        private suspend fun subscriptionItems(startVideoId: String): List<ShortItem> {
            val subscriptions = subscriptionRepository.getAllSubscriptions().first()
            val perChannel =
                subscriptions
                    .take(MAX_SUBSCRIPTION_CHANNELS)
                    .mapNotNull { channel ->
                        YouTube.channelShorts(channel.channelId).getOrNull()?.let { page ->
                            page.shorts.map { it.toItem(page) }
                        }
                    }
            // Round-robin interleave: one Short per channel per round, newest channels first.
            val interleaved = mutableListOf<ShortItem>()
            var round = 0
            while (true) {
                var added = false
                for (list in perChannel) {
                    list.getOrNull(round)?.let {
                        interleaved += it
                        added = true
                    }
                }
                if (!added) break
                round++
            }
            continuation = null
            val distinct = interleaved.distinctBy { it.videoId }
            val start = startVideoId.takeIf { it.isNotBlank() }
            return if (start != null) {
                val index = distinct.indexOfFirst { it.videoId == start }
                if (index > 0) distinct.drop(index) else distinct
            } else {
                distinct
            }
        }

        private suspend fun overlayItem(videoId: String): ShortItem? {
            if (videoId.isBlank()) return null
            val overlay = YouTube.reelOverlay(videoId).getOrNull()
            return ShortItem(
                videoId = videoId,
                title = overlay?.title.orEmpty(),
                channelId = overlay?.channelId.orEmpty(),
                channelName = overlay?.channelName.orEmpty(),
                channelAvatarUrl = overlay?.channelAvatarUrl.orEmpty(),
                likeCountText = overlay?.likeCountText,
                commentCountText = overlay?.commentCountText,
                posterUrl = reelPosterUrl(videoId),
                soundTitle = overlay?.soundTitle,
            )
        }

        /** Fills one visible reel's overlay. Only the on-screen Short is enriched, so a page
         * load never fires a burst of overlay requests that compete with stream extraction. */
        fun enrichItem(videoId: String) {
            val current = _uiState.value.items.firstOrNull { it.videoId == videoId } ?: return
            if (current.title.isNotBlank() && current.channelName.isNotBlank()) return
            viewModelScope.launch {
                val overlay = YouTube.reelOverlay(videoId).getOrNull() ?: return@launch
                _uiState.update { state ->
                    state.copy(
                        items =
                            state.items.map { item ->
                                if (item.videoId != videoId) {
                                    item
                                } else {
                                    item.copy(
                                        title = overlay.title?.takeIf { it.isNotBlank() } ?: item.title,
                                        channelId = overlay.channelId?.takeIf { it.isNotBlank() } ?: item.channelId,
                                        channelName = overlay.channelName?.takeIf { it.isNotBlank() } ?: item.channelName,
                                        channelAvatarUrl = overlay.channelAvatarUrl?.takeIf { it.isNotBlank() } ?: item.channelAvatarUrl,
                                        likeCountText = overlay.likeCountText ?: item.likeCountText,
                                        commentCountText = overlay.commentCountText ?: item.commentCountText,
                                        soundTitle = overlay.soundTitle ?: item.soundTitle,
                                    )
                                }
                            },
                    )
                }
            }
        }

        private data class FeedPage(
            val items: List<ShortItem>,
            val continuation: String?,
        )

        private fun ReelEntry.toItem(): ShortItem =
            ShortItem(
                videoId = videoId,
                posterUrl = posterUrl ?: reelPosterUrl(videoId),
            )

        private fun ReelLockup.toItem(page: ChannelShortsPage): ShortItem =
            ShortItem(
                videoId = id,
                title = title,
                channelId = page.channelId,
                channelName = page.channelName,
                channelAvatarUrl = page.channelAvatarUrl,
                viewCountText = viewsText,
                posterUrl = posterUrl ?: thumbnailUrl.ifBlank { reelPosterUrl(id) },
            )

        private fun List<Video>.toItems(startVideoId: String): List<ShortItem> {
            val items =
                map {
                    ShortItem(
                        videoId = it.id,
                        title = it.title,
                        channelId = it.channelId,
                        channelName = it.channelName,
                        channelAvatarUrl = it.channelThumbnailUrl,
                        posterUrl = it.thumbnailUrl.ifBlank { reelPosterUrl(it.id) },
                    )
                }
            val start = startVideoId.takeIf { v -> v.isNotBlank() }
            val index = start?.let { s -> items.indexOfFirst { it.videoId == s } } ?: -1
            return if (index > 0) items.drop(index) else items
        }

        fun onLikeToggled(
            videoId: String,
            liked: Boolean,
        ) {
            viewModelScope.launch {
                runCatching { YouTube.likeVideo(videoId, liked) }
            }
        }

        private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
        val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

        /** Saves (or unsaves) a Short to Watch Later, YouTube-Shorts style. */
        fun toggleSave(item: ShortItem) {
            val saved = item.videoId in _savedIds.value
            _savedIds.update { ids -> if (saved) ids - item.videoId else ids + item.videoId }
            viewModelScope.launch {
                runCatching {
                    if (saved) {
                        playlistRepository.removeFromWatchLater(item.videoId)
                    } else {
                        playlistRepository.addToWatchLater(
                            Video(
                                id = item.videoId,
                                title = item.title,
                                channelName = item.channelName,
                                channelId = item.channelId,
                                thumbnailUrl = item.posterUrl.orEmpty(),
                                duration = 0,
                                viewCount = 0L,
                                uploadDate = "",
                                channelThumbnailUrl = item.channelAvatarUrl,
                                isShort = true,
                            ),
                        )
                    }
                }
            }
        }

        /** Whether [videoId] is already in Watch Later, checked when it becomes visible. */
        fun refreshSaved(videoId: String) {
            if (videoId in _savedIds.value) return
            viewModelScope.launch {
                runCatching {
                    val inWatchLater =
                        playlistRepository.getWatchLaterVideosFlow().first().any { it.id == videoId }
                    if (inWatchLater) _savedIds.update { it + videoId }
                }
            }
        }

        /** Opens the inline comments sheet for one Short. Opening never navigates away, so the reel keeps playing underneath. */
        fun openComments(videoId: String) {
            if (_commentsState.value.videoId == videoId && _commentsState.value.comments.isNotEmpty()) return
            commentsJob?.cancel()
            commentsContinuation = null
            _commentsState.value = ShortsCommentsState(videoId = videoId, isLoading = true)
            commentsJob =
                viewModelScope.launch {
                    try {
                        val page: CommentsPageResult = youtubeRepository.getVideoComments(videoId)
                        if (_commentsState.value.videoId != videoId) return@launch
                        commentsContinuation = page.continuation
                        _commentsState.update {
                            it.copy(
                                comments = page.comments,
                                isLoading = false,
                                hasMore = page.hasMore,
                                totalText = page.totalText,
                            )
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Shorts comments failed: ${e.message}")
                        _commentsState.update { it.copy(isLoading = false) }
                    }
                }
        }

        fun loadMoreComments() {
            val state = _commentsState.value
            val continuation = commentsContinuation ?: return
            if (state.isLoadingMore || state.videoId.isBlank()) return
            _commentsState.update { it.copy(isLoadingMore = true) }
            viewModelScope.launch {
                try {
                    val page = youtubeRepository.getMoreVideoComments(state.videoId, continuation)
                    if (_commentsState.value.videoId != state.videoId) return@launch
                    commentsContinuation = page.continuation
                    _commentsState.update {
                        it.copy(
                            comments = it.comments + page.comments,
                            isLoadingMore = false,
                            hasMore = page.hasMore,
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Shorts comments load more failed: ${e.message}")
                    _commentsState.update { it.copy(isLoadingMore = false) }
                }
            }
        }

        fun loadCommentReplies(comment: Comment) = loadCommentReplies(comment, append = false)

        fun loadMoreCommentReplies(comment: Comment) = loadCommentReplies(comment, append = true)

        /**
         * Comment replies for the Short on screen — the same fetch the regular player uses
         * (CommentsPager.loadReplies), wired up here so "view replies" actually loads instead of
         * spinning forever. One request per comment at a time; a late answer for a Short the user
         * has swiped away from is dropped, and closeComments() clears the in-flight set.
         */
        private fun loadCommentReplies(
            comment: Comment,
            append: Boolean,
        ) {
            val videoId = _commentsState.value.videoId
            if (videoId.isBlank()) return
            val continuation = comment.continuationToken
            val repliesPage = comment.repliesPage
            if (continuation == null && repliesPage == null) return
            if (!repliesInFlight.add(comment.id)) return
            viewModelScope.launch {
                try {
                    val (replies, nextContinuation, nextLegacyPage) =
                        if (continuation != null) {
                            val page = youtubeRepository.getVideoCommentReplies(videoId, continuation)
                            Triple(page.comments, page.continuation, null)
                        } else {
                            val url = "https://www.youtube.com/watch?v=$videoId"
                            val (items, nextPage) = youtubeRepository.getCommentReplies(url, requireNotNull(repliesPage))
                            Triple(items, null, nextPage)
                        }
                    if (_commentsState.value.videoId != videoId) return@launch
                    _commentsState.update { state ->
                        state.copy(
                            comments =
                                state.comments.map { current ->
                                    if (current.id != comment.id) {
                                        current
                                    } else {
                                        current.copy(
                                            replies =
                                                if (append) {
                                                    current.replies.mergeDistinctByNonBlankKey(replies, Comment::id)
                                                } else {
                                                    replies.distinctByNonBlankKey(Comment::id)
                                                },
                                            continuationToken = nextContinuation,
                                            repliesPage = nextLegacyPage,
                                        )
                                    }
                                },
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Shorts comment replies failed: ${e.message}")
                } finally {
                    repliesInFlight.remove(comment.id)
                }
            }
        }

        fun closeComments() {
            commentsJob?.cancel()
            repliesInFlight.clear()
            _commentsState.value = ShortsCommentsState()
        }

        companion object {
            private const val TAG = "ShortsViewModel"
            private const val MAX_SUBSCRIPTION_CHANNELS = 20

            /** Last algorithmic feed, kept across ViewModel instances so the tab reopens instantly. */
            @Volatile private var cachedFeedItems: List<ShortItem> = emptyList()
            @Volatile private var cachedFeedContinuation: String? = null
        }
    }
