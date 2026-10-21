package com.fedoratube.org.player.stream

import com.fedoratube.org.data.local.VideoQuality
import com.fedoratube.org.innertube.models.response.PlayerResponse
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.VideoStream

object MergedPlaybackAssembly {
    /**
     * Re-picks the streams for [quality] from a result that is already on screen, over the InnerTube
     * formats the screen kept from the load.
     */
    fun selectQualityStreams(
        innerTubeVideoFormats: List<PlayerResponse.StreamingData.Format>,
        innerTubeAudioFormats: List<PlayerResponse.StreamingData.Format>,
        quality: VideoQuality,
        preferredAudioLanguage: String,
        preferredCodecKey: String,
    ): Pair<VideoStream?, AudioStream?> {
        val innerTubeVideoStreams = InnerTubeStreamBridge.convertVideoFormats(innerTubeVideoFormats)
        val innerTubeAudioStreams = InnerTubeStreamBridge.convertAudioFormats(innerTubeAudioFormats)
        return ServicePlaybackStreamSelector.selectStreams(
            videoCandidates = innerTubeVideoStreams,
            audioCandidatesAll = innerTubeAudioStreams,
            preferredQuality = quality,
            preferredAudioLanguage = preferredAudioLanguage,
            preferredCodecKey = preferredCodecKey,
        )
    }
}
