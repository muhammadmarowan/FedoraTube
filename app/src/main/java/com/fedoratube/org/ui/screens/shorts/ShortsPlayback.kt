package com.fedoratube.org.ui.screens.shorts

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.MediaSource
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.player.datasource.YouTubeHttpDataSource
import com.fedoratube.org.player.resolver.VideoPlaybackResolver
import com.fedoratube.org.player.stream.InnerTubeStreamBridge
import com.fedoratube.org.player.stream.InnerTubeVideoMapper
import com.fedoratube.org.player.stream.InnerTubeVideoStreamExtractor
import com.fedoratube.org.player.stream.VideoCodecUtils
import com.fedoratube.org.utils.NetworkState
import kotlinx.coroutines.flow.first

/**
 * Turns one Short into something ExoPlayer can play, reusing the same extraction and
 * DASH-manifest ladder as the regular player. Quality is capped by the Shorts quality the
 * viewer picked for the network they are on, so a Short never pulls a 4K stream.
 */
@UnstableApi
internal object ShortsPlayback {
    suspend fun resolveMediaSource(
        context: Context,
        preferences: PlayerPreferences,
        videoId: String,
    ): MediaSource? {
        val extraction =
            runCatching {
                kotlinx.coroutines.withTimeoutOrNull(30_000L) {
                    InnerTubeVideoStreamExtractor.extract(videoId)
                }
            }.getOrNull() ?: return null

        val videoStreams = InnerTubeStreamBridge.convertVideoFormats(extraction.videoFormats)
        val audioStreams = InnerTubeStreamBridge.convertAudioFormats(extraction.audioFormats)
        if (videoStreams.isEmpty()) return null
        val audioStream = audioStreams.firstOrNull()

        val maxHeight =
            runCatching {
                val quality =
                    if (NetworkState.isOnWifi(context)) {
                        preferences.shortsQualityWifi.first()
                    } else {
                        preferences.shortsQualityCellular.first()
                    }
                if (quality.height > 0) quality.height else Int.MAX_VALUE
            }.getOrDefault(Int.MAX_VALUE)

        val capped =
            videoStreams.filter { VideoCodecUtils.qualityHeightFromStream(it) <= maxHeight }
        val ladder = (if (capped.isNotEmpty()) capped else videoStreams).distinctBy { VideoCodecUtils.qualityHeightFromStream(it) }

        val dataSourceFactory =
            DefaultDataSource
                .Factory(context, YouTubeHttpDataSource.Factory())
        val resolver =
            VideoPlaybackResolver(
                dashDataSourceFactory = dataSourceFactory,
                progressiveDataSourceFactory = dataSourceFactory,
            )
        return resolver.resolve(
            videoStreams = ladder,
            audioStream = audioStream,
            dashManifestUrl = null,
            hlsUrl = null,
            durationSeconds = InnerTubeVideoMapper.durationSeconds(extraction),
            asAdaptiveLadder = true,
        )
    }
}
