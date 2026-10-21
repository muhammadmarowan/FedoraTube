package com.fedoratube.org.innertube.models.body

import com.fedoratube.org.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
