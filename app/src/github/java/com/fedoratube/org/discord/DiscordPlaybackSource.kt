package com.fedoratube.org.discord

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.fedoratube.org.player.EnhancedMusicPlayerManager
import com.fedoratube.org.player.EnhancedPlayerManager
import com.fedoratube.org.player.GlobalPlayerState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
class DiscordPlaybackSource(
    private val videoManager: EnhancedPlayerManager = EnhancedPlayerManager.getInstance(),
    private val selector: DiscordPlaybackSelector = DiscordPlaybackSelector(),
) {
    val playback: Flow<PlaybackSnapshot?> = discordPlaybackSnapshotFlow(
        signals = merge(
            GlobalPlayerState.currentVideo.map { Unit },
            videoManager.playerState.map { Unit },
            EnhancedMusicPlayerManager.currentTrack.map { Unit },
            EnhancedMusicPlayerManager.playerState.map { Unit },
            ticker(),
        ),
        snapshotDispatcher = Dispatchers.Main.immediate,
        readSnapshot = {
            selector.select(
                short = null,
                video = videoSnapshot(),
                music = musicSnapshot(),
            )
        },
    )

    private fun videoSnapshot(): PlaybackSnapshot? {
        val video = GlobalPlayerState.currentVideo.value ?: return null
        val state = videoManager.playerState.value
        if (state.currentVideoId != video.id) return null
        val isLive = state.isLive || video.isLive
        return PlaybackSnapshot(
            kind = when {
                isLive -> PlaybackKind.LIVE
                video.isShort -> PlaybackKind.SHORT
                else -> PlaybackKind.VIDEO
            },
            mediaId = video.id,
            title = video.title,
            subtitle = video.channelName,
            artworkUrl = video.thumbnailUrl,
            positionMs = videoManager.getCurrentPosition(),
            durationMs = if (isLive) 0L else videoManager.getDuration().coerceAtLeast(0L),
            isPlaying = state.isPlaying,
            isLive = isLive,
        )
    }

    private fun musicSnapshot(): PlaybackSnapshot? {
        val track = EnhancedMusicPlayerManager.currentTrack.value ?: return null
        val state = EnhancedMusicPlayerManager.playerState.value
        return PlaybackSnapshot(
            kind = PlaybackKind.MUSIC,
            mediaId = track.videoId,
            title = track.title,
            subtitle = track.artist,
            artworkUrl = track.highResThumbnailUrl,
            positionMs = EnhancedMusicPlayerManager.getCurrentPosition(),
            durationMs = EnhancedMusicPlayerManager.getDuration().coerceAtLeast(0L),
            isPlaying = state.isPlaying,
            isLive = false,
        )
    }

    private fun ticker(): Flow<Unit> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(POSITION_SAMPLE_INTERVAL_MS)
        }
    }

    private companion object {
        const val POSITION_SAMPLE_INTERVAL_MS = 5_000L
    }
}

internal fun discordPlaybackSnapshotFlow(
    signals: Flow<Unit>,
    snapshotDispatcher: CoroutineDispatcher,
    readSnapshot: () -> PlaybackSnapshot?,
): Flow<PlaybackSnapshot?> = signals
    .map { readSnapshot() }
    // Media3 players are owned by the main application thread. The presence runtime
    // collects on IO for its gateway calls, so keep every snapshot read upstream on main.
    .flowOn(snapshotDispatcher)
    .distinctUntilChanged()
