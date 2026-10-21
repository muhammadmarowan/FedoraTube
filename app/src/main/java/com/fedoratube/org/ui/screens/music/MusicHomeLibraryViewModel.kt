package com.fedoratube.org.ui.screens.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.local.PlaylistRepository
import com.fedoratube.org.data.local.SubscriptionRepository
import com.fedoratube.org.data.music.model.ArtistDetails
import com.fedoratube.org.data.music.model.MusicPlaylist
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.playlist.sortedFor
import com.fedoratube.org.data.scrobble.LastFmDiscovery
import com.fedoratube.org.data.scrobble.ScrobbleService
import com.fedoratube.org.data.scrobble.ScrobbleStore
import com.fedoratube.org.ui.components.music.section.MusicHomeShelf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SHELF_LIMIT = 20

/**
 * The viewer's own corner of the music home: their music playlists in the playlists page's order,
 * the music channels they subscribe to, newest first, the sections they chose to hide, and, for
 * Last.fm accounts, the discovery shelf. Only that shelf touches the network, and only once shown.
 */
@HiltViewModel
class MusicHomeLibraryViewModel
    @Inject
    constructor(
        playlistRepository: PlaylistRepository,
        subscriptionRepository: SubscriptionRepository,
        preferences: PlayerPreferences,
        scrobbleStore: ScrobbleStore,
        private val lastFmDiscovery: LastFmDiscovery,
    ) : ViewModel() {
        private val sharing = SharingStarted.WhileSubscribed(5_000)

        val playlists: StateFlow<List<MusicPlaylist>> =
            combine(playlistRepository.getMusicPlaylistsFlow(), preferences.playlistListOrder) { playlists, order ->
                playlists.sortedFor(order).take(SHELF_LIMIT).map { playlist ->
                    MusicPlaylist(
                        id = playlist.id,
                        title = playlist.name,
                        thumbnailUrl = playlist.thumbnailUrl,
                        trackCount = playlist.videoCount,
                    )
                }
            }.stateIn(viewModelScope, sharing, emptyList())

        val subscriptions: StateFlow<List<ArtistDetails>> =
            subscriptionRepository
                .getAllSubscriptions()
                .map { subscriptions ->
                    subscriptions
                        .filter { it.isMusic }
                        .sortedByDescending { it.subscribedAt }
                        .map {
                            ArtistDetails(
                                name = it.channelName,
                                channelId = it.channelId,
                                thumbnailUrl = it.channelThumbnail,
                                subscriberCount = 0L,
                            )
                        }
                }.stateIn(viewModelScope, sharing, emptyList())

        val lastFmSignedIn: StateFlow<Boolean> =
            scrobbleStore.settings
                .map { ScrobbleService.LASTFM in it.accounts }
                .distinctUntilChanged()
                .stateIn(viewModelScope, sharing, false)

        private val _discovery = MutableStateFlow<List<MusicTrack>>(emptyList())
        val discovery: StateFlow<List<MusicTrack>> = _discovery.asStateFlow()
        private var discoveryJob: Job? = null

        /** Called when the shelf comes into view; the discovery service keeps its result while the seeds hold. */
        fun loadDiscovery() {
            if (discoveryJob?.isActive == true) return
            discoveryJob =
                viewModelScope.launch { _discovery.value = runCatching { lastFmDiscovery.tracks() }.getOrDefault(_discovery.value) }
        }

        val hiddenShelves: StateFlow<Set<MusicHomeShelf>> =
            preferences.hiddenMusicHomeShelves
                .map(MusicHomeShelf::fromStored)
                .stateIn(viewModelScope, sharing, emptySet())
    }
