package com.fedoratube.org.widget.nowplaying

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fedoratube.org.player.EnhancedMusicPlayerManager
import com.fedoratube.org.widget.core.state.NowPlayingSnapshot
import com.fedoratube.org.widget.core.state.clearNowPlayingSnapshot
import com.fedoratube.org.widget.core.state.markNowPlayingStopped
import com.fedoratube.org.widget.core.state.nowPlayingSnapshotFlow
import com.fedoratube.org.widget.core.state.writeNowPlayingSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the music session to the Now Playing widget: captures a snapshot on player
 * events (event-driven — never polled) and pushes it to the widget's DataStore.
 */
@Singleton
class NowPlayingWidgetPublisher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val updateChannel = Channel<NowPlayingSnapshot?>(Channel.CONFLATED)
        private val progressTicker = NowPlayingProgressTicker(context, scope)

        init {
            scope.launch {
                for (snapshot in updateChannel) {
                    try {
                        if (snapshot == null) {
                            context.clearNowPlayingSnapshot()
                        } else {
                            context.writeNowPlayingSnapshot(snapshot)
                        }
                        updatePlayerWidgets()
                        progressTicker.follow(snapshot)
                    } catch (e: Exception) {
                        Log.w("NowPlayingPublisher", "Failed to update now playing widgets: ${e.message}")
                    }
                }
            }
        }

        /** Must be called on the player's application thread (service listener callbacks are). */
        @OptIn(UnstableApi::class)
        fun publish(player: Player) {
            hasPublished = true
            val item = player.currentMediaItem
            val track = EnhancedMusicPlayerManager.currentTrack.value
            val snapshot =
                item?.let {
                    val rawTitle = it.mediaMetadata.title?.toString()
                    val title = if (!rawTitle.isNullOrBlank()) rawTitle else track?.title.orEmpty()
                    val rawArtist = it.mediaMetadata.artist?.toString()
                    val artist = if (!rawArtist.isNullOrBlank()) rawArtist else track?.artist.orEmpty()
                    val artworkUrl = it.mediaMetadata.artworkUri?.toString()
                        ?: track?.highResThumbnailUrl

                    NowPlayingSnapshot(
                        mediaId = it.mediaId,
                        title = title,
                        artist = artist,
                        artworkUrl = artworkUrl,
                        // Buffering still shows pause, matching the notification's own button.
                        isPlaying = !Util.shouldShowPlayButton(player),
                        isLiked = EnhancedMusicPlayerManager.isLiked.value,
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.takeIf { d -> d != C.TIME_UNSET } ?: 0L,
                        capturedAtElapsedMs = SystemClock.elapsedRealtime(),
                        speed = player.playbackParameters.speed,
                    )
                }
            updateChannel.trySend(snapshot)
        }

        /** Service is going away — keep the last track on the widget, but shown paused. */
        fun publishStopped() {
            progressTicker.follow(null)
            scope.launch {
                try {
                    context.markNowPlayingStopped()
                    updatePlayerWidgets()
                } catch (e: Exception) {
                    Log.w("NowPlayingPublisher", "Widget stop error: ${e.message}")
                }
            }
        }

        companion object {
            /**
             * Whether this process has published. A snapshot that says "playing" but was written by an
             * earlier process outlived a kill that skipped onDestroy, so the widget shows it paused.
             */
            @Volatile
            var hasPublished = false
                private set
        }

        /**
         * Called at app start: a widget left "playing" by a killed process keeps its clock running in the
         * launcher until something re-renders it, so it is re-rendered paused once.
         */
        fun repairStalePlayback() {
            scope.launch {
                try {
                    val stored = context.nowPlayingSnapshotFlow { true }.first() ?: return@launch
                    if (!stored.isPlaying || hasPublished) return@launch
                    context.markNowPlayingStopped()
                    updatePlayerWidgets()
                } catch (e: Exception) {
                    Log.w("NowPlayingPublisher", "repairStalePlayback error: ${e.message}")
                }
            }
        }

        private suspend fun updatePlayerWidgets() {
            try {
                NowPlayingWidget().updateAll(context)
            } catch (e: Exception) {
                Log.w("NowPlayingPublisher", "updatePlayerWidgets error: ${e.message}")
            }
        }
    }
