package com.fedoratube.org.ui

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.feed.ShortsQueueSource
import com.fedoratube.org.data.feed.openAtVideoId

const val SHORTS_ROUTE_PATTERN = "shorts?src={src}"

const val SHORTS_ROUTE_ARG = "src"

const val SHORTS_ROUTE_KEY = "shorts"

fun NavController.openShorts(
    source: ShortsQueueSource,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate("shorts?$SHORTS_ROUTE_ARG=${Uri.encode(source.encode())}", builder)
}

/**
 * Opens [source] in the Shorts player, or the tapped reel in the video player when the user has
 * switched the Shorts player off (or Focus mode is on, which passes the same flag). Every Shorts
 * entry point takes this one decision. A feed with no single tapped video cannot play in the
 * regular player, so it still opens the Shorts feed.
 */
internal fun NavHostController.openShortsOrPlayer(
    source: ShortsQueueSource,
    disableShortsPlayer: Boolean = true,
) {
    val tappedId = source.openAtVideoId
    if (disableShortsPlayer && tappedId != null) {
        navigateToPlayer(tappedId)
    } else {
        openShorts(source)
    }
}

/** A Short opens in the Shorts player; anything else goes straight to [playVideo]. */
internal fun NavHostController.openVideoOrShorts(
    video: Video,
    disableShortsPlayer: Boolean = true,
    playVideo: (Video) -> Unit,
) {
    if (video.isShort && !disableShortsPlayer) {
        openShorts(ShortsQueueSource.SeededFeed(video.id))
    } else {
        playVideo(video)
    }
}
