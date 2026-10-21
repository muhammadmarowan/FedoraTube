package com.fedoratube.org.player

object PlaybackStartupPolicy {
    fun shouldDelaySecondaryContent(
        isPlaybackLoading: Boolean,
        currentVideoId: String?,
        requestedVideoId: String,
    ): Boolean = isPlaybackLoading && currentVideoId == requestedVideoId
}
