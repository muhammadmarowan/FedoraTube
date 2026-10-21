package com.fedoratube.org.data.engagement

import com.fedoratube.org.data.local.LikedVideoInfo
import com.fedoratube.org.data.local.LikedVideosRepository
import com.fedoratube.org.data.model.toMusicTrack
import com.fedoratube.org.data.scrobble.Scrobbler
import javax.inject.Inject
import com.fedoratube.org.data.music.PlaylistRepository as MusicLibrary

/**
 * Likes and unlikes from the Liked videos and Liked music pages. A liked song is also kept in the
 * music library's favorites, which the music recommendations read, so both change together.
 */
class LikedMediaUseCase
    @Inject
    constructor(
        private val likes: LikedVideosRepository,
        private val musicLibrary: MusicLibrary,
        private val scrobbler: Scrobbler,
    ) {
        /** Unlikes [videoIds]; the result is what [restore] needs to undo it. */
        suspend fun unlike(videoIds: Collection<String>): List<LikedVideoInfo> {
            val taken = likes.takeLikes(videoIds)
            taken.filter { it.isMusic }.forEach {
                musicLibrary.removeFromFavorites(it.videoId)
                scrobbler.onLikeChanged(it.toMusicTrack(), liked = false)
            }
            return taken
        }

        suspend fun restore(taken: List<LikedVideoInfo>) {
            likes.restoreLikes(taken)
            taken.filter { it.isMusic }.map { it.toMusicTrack() }.forEach {
                musicLibrary.addToFavorites(it)
                scrobbler.onLikeChanged(it, liked = true)
            }
        }
    }
