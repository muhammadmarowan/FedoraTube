package com.fedoratube.org.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.fedoratube.org.data.local.LikedVideosRepository
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.local.PlaylistRepository
import com.fedoratube.org.data.local.ViewHistory
import com.fedoratube.org.data.model.toVideo
import com.fedoratube.org.data.notes.Note
import com.fedoratube.org.data.notes.NotesRepository
import com.fedoratube.org.data.playlist.sortedFor
import com.fedoratube.org.data.feed.ShortsContentFilter
import com.fedoratube.org.data.stats.RecapPeriod
import com.fedoratube.org.data.stats.RecapReadiness
import com.fedoratube.org.data.video.VideoDownloadManager
import com.fedoratube.org.ui.components.library.LIBRARY_SHELF_ITEM_LIMIT
import com.fedoratube.org.ui.components.library.LibraryMediaItem
import com.fedoratube.org.ui.components.library.LibrarySection
import com.fedoratube.org.ui.components.library.toLibraryMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.fedoratube.org.data.music.DownloadManager as MusicDownloadManager

internal data class LibraryCounts(
    val history: Int,
    val videoPlaylists: Int,
    val musicPlaylists: Int,
    val watchLater: Int,
    val likedVideos: Int,
    val likedMusic: Int,
    val downloadedVideos: Int,
    val downloadedTracks: Int,
    val savedShorts: Int,
) {
    val isEmpty: Boolean
        get() =
            history == 0 &&
                videoPlaylists == 0 &&
                musicPlaylists == 0 &&
                watchLater == 0 &&
                likedVideos == 0 &&
                likedMusic == 0 &&
                downloadedVideos == 0 &&
                downloadedTracks == 0 &&
                savedShorts == 0
}

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val playlistRepository: PlaylistRepository,
        likedVideosRepository: LikedVideosRepository,
        viewHistory: ViewHistory,
        private val videoDownloadManager: VideoDownloadManager,
        private val musicDownloadManager: MusicDownloadManager,
        shortsContentFilter: ShortsContentFilter,
        playerPreferences: PlayerPreferences,
        private val recapReadiness: RecapReadiness,
        private val notesRepository: NotesRepository,
    ) : ViewModel() {
        private val sharing = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L)

        /** Null while notes are turned off in Content settings, which hides the Notes row. */
        internal val notesCount: StateFlow<Int?> =
            combine(playerPreferences.notesEnabled, notesRepository.observeCount()) { enabled, count -> count.takeIf { enabled } }
                .distinctUntilChanged()
                .stateIn(viewModelScope, sharing, null)

        private val _recapReady = MutableStateFlow<RecapPeriod?>(null)

        /** A month or year that just closed with a recap waiting; checked once each time Library is created. */
        val recapReady: StateFlow<RecapPeriod?> = _recapReady.asStateFlow()

        init {
            viewModelScope.launch { _recapReady.value = runCatching { recapReadiness.readyPeriod() }.getOrNull() }
        }

        /** The waiting recap was opened or dismissed; it is not offered again. */
        fun onRecapHandled() {
            val period = _recapReady.value ?: return
            _recapReady.value = null
            viewModelScope.launch { recapReadiness.markShown(period) }
        }

        private fun <T> Flow<T>.shared(): StateFlow<T?> {
            val upstream: Flow<T?> = this
            return upstream
                .distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, sharing, null)
        }

        private val allLikes = likedVideosRepository.getAllLikedVideos().shared()
        private val playlistOrder = playerPreferences.playlistListOrder
        private val allVideoPlaylists =
            combine(playlistRepository.getAllPlaylistsFlow(), playlistOrder) { playlists, order -> playlists.sortedFor(order) }
                .shared()
        private val allMusicPlaylists =
            combine(playlistRepository.getMusicPlaylistsFlow(), playlistOrder) { playlists, order -> playlists.sortedFor(order) }
                .shared()
        private val allWatchLater = playlistRepository.getVideoOnlyWatchLaterFlow().shared()
        private val allSavedShorts = playlistRepository.getVideoOnlySavedShortsFlow().shared()

        private val allDownloads =
            combine(
                videoDownloadManager.downloadedVideos,
                musicDownloadManager.downloadedTracks,
            ) { videos, tracks -> videos to tracks }.shared()

        internal val combinedPlaylists =
            combine(allVideoPlaylists, allMusicPlaylists) { videoList, musicList ->
                val v = (videoList.orEmpty()).map { it to false }
                val m = (musicList.orEmpty()).map { it to true }
                (v + m).sortedByDescending { it.first.createdAt }
            }.shared()

        internal val fullVideoPlaylists = allVideoPlaylists
        internal val fullMusicPlaylists = allMusicPlaylists
        internal val fullWatchLaterVideos = allWatchLater
        internal val fullDownloadedVideos = videoDownloadManager.downloadedVideos.shared()
        internal val fullDownloadedTracks = musicDownloadManager.downloadedTracks.shared()
        internal val fullNotes = notesRepository.observeAll().shared()

        internal val history =
            viewHistory
                .getRecentLibraryHistory(LIBRARY_SHELF_ITEM_LIMIT)
                .map { entries -> entries.map { it.toLibraryMediaItem() } }
                .shared()

        internal val likedVideos =
            allLikes
                .map { liked ->
                    liked
                        ?.asSequence()
                        ?.filterNot { it.isMusic }
                        ?.take(LIBRARY_SHELF_ITEM_LIMIT)
                        ?.map { it.toVideo() }
                        ?.toList()
                }.shared()

        internal val likedMusic =
            allLikes
                .map { liked ->
                    liked
                        ?.asSequence()
                        ?.filter { it.isMusic }
                        ?.take(LIBRARY_SHELF_ITEM_LIMIT)
                        ?.map { it.toLibraryMediaItem() }
                        ?.toList()
                }.shared()

        internal val playlists = allVideoPlaylists.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

        internal val musicPlaylists = allMusicPlaylists.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

        internal val watchLater = allWatchLater.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

        internal val savedShorts = allSavedShorts.map { it?.take(LIBRARY_SHELF_ITEM_LIMIT) }.shared()

        internal val downloads =
            allDownloads
                .map { downloaded ->
                    downloaded?.let { (videos, tracks) ->
                        buildList<LibraryMediaItem> {
                            videos.forEach { add(LibraryMediaItem.DownloadedVideoItem(it)) }
                            tracks.forEach { add(LibraryMediaItem.DownloadedMusicItem(it)) }
                        }.sortedByDescending { item -> item.downloadedAt }
                            .take(LIBRARY_SHELF_ITEM_LIMIT)
                    }
                }.shared()

        internal val shortsEnabled =
            shortsContentFilter.enabled
                .stateIn(viewModelScope, sharing, true)

        internal val separatePlaylistKinds =
            playerPreferences.separatePlaylistKinds
                .stateIn(viewModelScope, sharing, false)

        internal val isFocusModeEnabled: StateFlow<Boolean> =
            playerPreferences.focusModeEnabled
                .stateIn(viewModelScope, sharing, false)

        /** Null until the stored choice is read, so a hidden section never shows for a frame. */
        internal val hiddenSections: StateFlow<Set<LibrarySection>?> =
            playerPreferences.hiddenLibrarySections
                .map(LibrarySection::fromStored)
                .stateIn(viewModelScope, sharing, null)

        internal val shelfPreviewsEnabled =
            playerPreferences.libraryShelfPreviewsEnabled
                .distinctUntilChanged()
                .stateIn(viewModelScope, sharing, true)

        /**
         * Counts come off the same shared sources the shelves read, so the compact layout costs one
         * extra `COUNT(*)` on watch history and nothing else.
         */
        internal val counts: StateFlow<LibraryCounts?> =
            combine(
                viewHistory.getLibraryHistoryCount(),
                combine(allVideoPlaylists, allMusicPlaylists) { video, music ->
                    if (video == null || music == null) null else video.size to music.size
                },
                combine(allWatchLater, allSavedShorts) { later, shorts ->
                    if (later == null || shorts == null) null else later.size to shorts.size
                },
                allLikes,
                allDownloads,
            ) { historyCount, playlistCount, saved, liked, downloaded ->
                if (playlistCount == null || saved == null || liked == null || downloaded == null) {
                    null
                } else {
                    LibraryCounts(
                        history = historyCount,
                        videoPlaylists = playlistCount.first,
                        musicPlaylists = playlistCount.second,
                        watchLater = saved.first,
                        likedVideos = liked.count { !it.isMusic },
                        likedMusic = liked.count { it.isMusic },
                        downloadedVideos = downloaded.first.size,
                        downloadedTracks = downloaded.second.size,
                        savedShorts = saved.second,
                    )
                }
            }.distinctUntilChanged()
                .flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, sharing, null)

        /** Queued, running and paused downloads; null until Room first answers. */
        internal val activeDownloadCount: StateFlow<Int?> =
            videoDownloadManager.activeDownloads
                .map { it.size }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .stateIn(viewModelScope, sharing, null)

        internal val isLibraryEmpty =
            combine(counts, activeDownloadCount, ::libraryIsEmpty)
                .distinctUntilChanged()
                .stateIn(viewModelScope, sharing, false)

        fun createVideoPlaylist(name: String, description: String) {
            viewModelScope.launch {
                playlistRepository.createPlaylist(
                    playlistId = System.currentTimeMillis().toString(),
                    name = name,
                    description = description,
                    isPrivate = true,
                    isMusic = false,
                )
            }
        }

        fun createMusicPlaylist(name: String, description: String) {
            viewModelScope.launch {
                playlistRepository.createPlaylist(
                    playlistId = java.util.UUID.randomUUID().toString(),
                    name = name,
                    description = description,
                    isPrivate = true,
                    isMusic = true,
                )
            }
        }

        fun deletePlaylist(playlistId: String) {
            viewModelScope.launch { playlistRepository.deletePlaylist(playlistId) }
        }

        fun removeFromWatchLater(videoId: String) {
            viewModelScope.launch { playlistRepository.removeFromWatchLater(videoId) }
        }

        fun deleteDownloadedVideo(videoId: String) {
            viewModelScope.launch { videoDownloadManager.deleteDownload(videoId) }
        }

        fun deleteDownloadedMusic(videoId: String) {
            viewModelScope.launch { musicDownloadManager.deleteDownload(videoId) }
        }

        fun deleteNote(note: Note) {
            viewModelScope.launch { notesRepository.delete(note.kind, note.targetId) }
        }
    }

/**
 * Empty only once both counts are known: a download still in progress is library content too, and
 * deciding before Room answers would flash the empty state.
 */
internal fun libraryIsEmpty(
    counts: LibraryCounts?,
    activeDownloads: Int?,
): Boolean = counts != null && activeDownloads == 0 && counts.isEmpty

private val LibraryMediaItem.downloadedAt: Long
    get() =
        when (this) {
            is LibraryMediaItem.DownloadedVideoItem -> download.downloadedAt
            is LibraryMediaItem.DownloadedMusicItem -> download.downloadedAt
            else -> 0L
        }
