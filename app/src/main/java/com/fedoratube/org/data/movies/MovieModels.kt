package com.fedoratube.org.data.movies

import kotlinx.serialization.Serializable

@Serializable
data class StremioAddon(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String = "",
    val iconUrl: String? = null,
    val transportUrl: String,
    val types: List<String> = listOf("movie", "series"),
    val resources: List<String> = listOf("catalog", "meta", "stream"),
    val enabled: Boolean = true,
    val isDeletable: Boolean = true,
)

@Serializable
data class MovieItem(
    val id: String,
    val type: String, // "movie" or "series"
    val name: String,
    val poster: String? = null,
    val banner: String? = null,
    val imdbRating: String? = null,
    val releaseInfo: String? = null,
    val genres: List<String> = emptyList(),
    val description: String? = null,
)

@Serializable
data class MovieDetails(
    val id: String,
    val type: String,
    val name: String,
    val imdbId: String? = null,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String> = emptyList(),
    val videos: List<MovieEpisode> = emptyList(),
)

@Serializable
data class MovieEpisode(
    val id: String,
    val title: String,
    val season: Int? = null,
    val episode: Int? = null,
    val thumbnail: String? = null,
    val overview: String? = null,
    val released: String? = null,
)

@Serializable
data class MovieStream(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val externalUrl: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val addonName: String = "",
    val addonLogo: String? = null,
    val quality: String? = null,
    val proxyHeaders: Map<String, String>? = null,
) {
    val displayTitle: String
        get() = title?.trim()?.ifBlank { null }
            ?: description?.trim()?.ifBlank { null }
            ?: name?.trim()?.ifBlank { null }
            ?: "Stream"

    val displayQuality: String
        get() = quality ?: extractQuality(displayTitle + " " + (name ?: ""))

    val isDirectPlayable: Boolean
        get() = !url.isNullOrBlank() &&
            !url.startsWith("magnet:") &&
            !url.startsWith("torrent://")

    val playbackUrl: String?
        get() = if (isDirectPlayable) url else externalUrl

    companion object {
        fun extractQuality(text: String): String {
            val upper = text.uppercase()
            return when {
                upper.contains("4K") || upper.contains("2160P") || upper.contains("UHD") -> "4K"
                upper.contains("1080P") || upper.contains("FHD") -> "1080p"
                upper.contains("720P") || upper.contains("HD") -> "720p"
                upper.contains("480P") || upper.contains("SD") -> "480p"
                upper.contains("CAM") || upper.contains("TELESYNC") -> "CAM"
                else -> "HD"
            }
        }
    }
}

data class MovieCatalogSection(
    val id: String,
    val title: String,
    val items: List<MovieItem>,
)

data class MoviesHomeState(
    val isLoading: Boolean = true,
    val heroItem: MovieItem? = null,
    val sections: List<MovieCatalogSection> = emptyList(),
    val selectedGenre: String? = null,
    val selectedType: String? = null, // null = All, "movie" = Movies, "series" = Series
    val errorMessage: String? = null,
)

