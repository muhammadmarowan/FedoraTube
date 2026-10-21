package com.fedoratube.org.ui.screens.widgets

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.fedoratube.org.data.local.PlaylistRepository
import com.fedoratube.org.data.local.entity.PlaylistEntity
import com.fedoratube.org.data.model.Video
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Reads what a widget asked to play. It loads nothing until asked, so a route can own one freely. */
@HiltViewModel
class WidgetPlaybackViewModel
    @Inject
    constructor(
        private val playlists: PlaylistRepository,
    ) : ViewModel() {
        suspend fun playlist(id: String): Pair<PlaylistEntity?, List<Video>> =
            playlists.getPlaylistEntity(id) to playlists.getPlaylistVideosFlow(id).first()
    }
