package com.fedoratube.org.innertube.pages

import com.fedoratube.org.innertube.models.Album
import com.fedoratube.org.innertube.models.Artist
import com.fedoratube.org.innertube.models.MusicResponsiveListItemRenderer
import com.fedoratube.org.innertube.models.PlaylistItem
import com.fedoratube.org.innertube.models.Runs
import com.fedoratube.org.innertube.models.SongItem
import com.fedoratube.org.innertube.models.oddElements
import com.fedoratube.org.innertube.models.splitBySeparator
import com.fedoratube.org.innertube.utils.parseTime

data class PlaylistPage(
    val playlist: PlaylistItem,
    val songs: List<SongItem>,
    val songsContinuation: String?,
    val continuation: String?,
    /** The playlist's own song count, which can exceed the rows served when some are unavailable. */
    val trackCount: Int? = null,
) {
    companion object {
        /**
         * The song count from the header's second subtitle, read by position because its words are
         * localized: "[views •] 49 tracks • 5+ hours", so the count is the group before the length.
         */
        fun trackCountFrom(secondSubtitle: Runs?): Int? {
            val groups = secondSubtitle?.runs?.splitBySeparator().orEmpty()
            if (groups.size < 2) return null
            return groups[groups.size - 2]
                .joinToString("") { it.text }
                .filter(Char::isDigit)
                .toIntOrNull()
        }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title =
                    renderer.flexColumns
                        .firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                artists =
                    renderer.flexColumns
                        .getOrNull(1)
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.oddElements()
                        ?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        }.orEmpty(),
                album =
                    renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null,
                        )
                    },
                duration =
                    renderer.fixedColumns
                        ?.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text
                        ?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit =
                    renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
                endpoint =
                    renderer.overlay
                        ?.musicItemThumbnailOverlayRenderer
                        ?.content
                        ?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint
                        ?.watchEndpoint,
                setVideoId = renderer.playlistItemData.playlistSetVideoId ?: return null,
            )
        }
    }
}
