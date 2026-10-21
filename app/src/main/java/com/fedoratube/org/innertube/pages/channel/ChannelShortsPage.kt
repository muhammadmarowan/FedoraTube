package com.fedoratube.org.innertube.pages.channel

import com.fedoratube.org.innertube.pages.arrayOrNull
import com.fedoratube.org.innertube.pages.objectOrNull
import com.fedoratube.org.innertube.pages.reel.ReelLockup
import com.fedoratube.org.innertube.pages.reel.toReelLockups
import com.fedoratube.org.innertube.pages.stringOrNull
import com.fedoratube.org.innertube.pages.youtubeText
import kotlinx.serialization.json.JsonObject

data class ChannelShortsPage(
    val shorts: List<ReelLockup>,
    val sorts: List<ChannelSortOption>,
    val continuation: String?,
    /** Present on an initial browse, absent on a continuation, which carries no channel header. */
    val channelId: String = "",
    val channelName: String = "",
    val channelAvatarUrl: String = "",
)

/**
 * A channel's Shorts tab. NewPipe's `ChannelTabInfo` cannot express the sort bar, which is most of
 * what makes the tab usable (#547), so Shorts go through the native browse instead.
 */
fun JsonObject.toChannelShortsPage(): ChannelShortsPage {
    val metadata = this["metadata"].objectOrNull()?.get("channelMetadataRenderer").objectOrNull()
    return ChannelShortsPage(
        shorts = toReelLockups(),
        sorts = channelSortOptions(),
        continuation = channelItemContinuation(),
        channelId = metadata?.get("externalId").stringOrNull().orEmpty(),
        channelName = metadata?.get("title").youtubeText().orEmpty(),
        channelAvatarUrl =
            metadata
                ?.get("avatar")
                .objectOrNull()
                ?.get("thumbnails")
                .arrayOrNull()
                ?.mapNotNull { it.objectOrNull()?.get("url").stringOrNull() }
                ?.lastOrNull()
                .orEmpty(),
    )
}
