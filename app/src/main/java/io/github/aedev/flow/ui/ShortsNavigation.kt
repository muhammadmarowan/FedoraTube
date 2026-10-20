package io.github.aedev.flow.ui

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.shorts.queue.ShortsQueueSource
import io.github.aedev.flow.data.shorts.queue.openAtVideoId

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
 * switched the Shorts player off. Every Shorts entry point takes this one decision.
 */
internal fun NavHostController.openShortsOrPlayer(
    source: ShortsQueueSource,
    disableShortsPlayer: Boolean = true,
) {
    val tappedId = source.openAtVideoId
    if (tappedId != null) navigateToPlayer(tappedId)
}

/** All videos always go to [playVideo], with Shorts player removed. */
internal fun NavHostController.openVideoOrShorts(
    video: Video,
    disableShortsPlayer: Boolean = true,
    playVideo: (Video) -> Unit,
) {
    playVideo(video)
}
