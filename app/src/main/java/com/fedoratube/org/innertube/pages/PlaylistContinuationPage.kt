package com.fedoratube.org.innertube.pages

import com.fedoratube.org.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
