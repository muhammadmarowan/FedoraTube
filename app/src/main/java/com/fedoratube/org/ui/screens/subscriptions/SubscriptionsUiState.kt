package com.fedoratube.org.ui.screens.subscriptions

import com.fedoratube.org.R
import com.fedoratube.org.data.innertube.SubscriptionRefreshStage
import com.fedoratube.org.data.local.ChannelSubscription
import com.fedoratube.org.data.model.Channel
import com.fedoratube.org.data.model.Comment
import com.fedoratube.org.data.model.SubscriptionGroup
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.innertube.pages.renderer.CommunityPost
import com.fedoratube.org.utils.ThumbnailUrlResolver

enum class SubscriptionSortMode {
    DEFAULT,
    NAME_ASC,
    RECENTLY_UPDATED,
    ;

    companion object {
        fun fromStorage(value: String?): SubscriptionSortMode = entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}

internal fun List<ChannelSubscription>.toSubscribedChannels(): List<Channel> =
    map { sub ->
        Channel(
            id = sub.channelId,
            name = sub.channelName,
            thumbnailUrl = ThumbnailUrlResolver.resolveChannelAvatar(sub.channelThumbnail),
            subscriberCount = 0L,
            isSubscribed = true,
            isMusic = sub.isMusic,
        )
    }

/** The counter line for a refresh in [this] stage. */
internal fun SubscriptionRefreshStage.progressTemplate(): Int =
    when (this) {
        SubscriptionRefreshStage.RSS -> R.plurals.subscriptions_refresh_progress_template
        SubscriptionRefreshStage.CHECKS -> R.plurals.subscriptions_refresh_checks_template
    }

internal fun List<Video>.withSubscriptionAvatars(channels: List<Channel>): List<Video> {
    val avatarByChannelId =
        channels
            .asSequence()
            .filter { it.thumbnailUrl.isNotBlank() }
            .associate { it.id to ThumbnailUrlResolver.resolveChannelAvatar(it.thumbnailUrl) }
    if (avatarByChannelId.isEmpty()) return this

    return map { video ->
        val normalizedExistingAvatar = ThumbnailUrlResolver.resolveChannelAvatar(video.channelThumbnailUrl)
        if (video.channelThumbnailUrl.isBlank()) {
            avatarByChannelId[video.channelId]?.let { avatar ->
                video.copy(
                    channelThumbnailUrl = avatar,
                    channelThumbnailUrls = video.channelThumbnailUrls.ifEmpty { listOf(avatar) },
                )
            } ?: video
        } else if (normalizedExistingAvatar != video.channelThumbnailUrl) {
            video.copy(channelThumbnailUrl = normalizedExistingAvatar)
        } else {
            video
        }
    }
}

data class SubscriptionsUiState(
    val subscribedChannels: List<Channel> = emptyList(),
    val recentVideos: List<Video> = emptyList(),
    val shorts: List<Video> = emptyList(),
    val selectedChannelId: String? = null,
    val isLoading: Boolean = false,
    val isFullWidthView: Boolean = false,
    val sortMode: SubscriptionSortMode = SubscriptionSortMode.DEFAULT,
    val isShortsShelfEnabled: Boolean = true,
    val notificationStates: Map<String, Boolean> = emptyMap(),
    val groups: List<SubscriptionGroup> = emptyList(),
    val selectedGroupName: String? = null,
    val refreshStage: SubscriptionRefreshStage = SubscriptionRefreshStage.RSS,
    val refreshProcessedChannels: Int = 0,
    val refreshTotalChannels: Int = 0,
    val lastRefreshTime: Long = 0L,
    val lastRefreshText: String? = null,
    val lastRefreshVideoCount: Int = 0,
    val showLastRefreshVideoCount: Boolean = true,
    val showSubscriptionVideos: Boolean = true,
    val showSubscriptionShorts: Boolean = true,
    val showSubscriptionLive: Boolean = true,
    val excludedShortsChannelIds: Set<String> = emptySet(),
    /** Channels the last refresh could not reach at all; surfaced instead of silently showing less. */
    val failedChannelIds: Set<String> = emptySet(),
    val failedChannelReasons: Map<String, String> = emptyMap(),
    val communityPosts: List<CommunityPost> = emptyList(),
    val activeCommunityPost: CommunityPost? = null,
    val communityComments: List<Comment> = emptyList(),
    val isLoadingCommunityComments: Boolean = false,
    val communityCommentsContinuation: String? = null,
    val isLoadingMoreCommunityComments: Boolean = false,
) {
    /** The selected channel or group's channels, which scope its refreshes and reads; null for every subscription. */
    fun selectedGroupScope(): Set<String>? =
        selectedChannelId?.let { setOf(it) }
            ?: selectedGroupName?.let { name -> groups.find { it.name == name }?.channelIds?.toSet() }

    /** Display names for [failedChannelIds], falling back to the raw id for an unknown channel. */
    val failedChannelNames: List<String>
        get() {
            if (failedChannelIds.isEmpty()) return emptyList()
            val namesById = subscribedChannels.associate { it.id to it.name }
            return failedChannelIds
                .map { id -> namesById[id]?.takeIf { it.isNotBlank() } ?: id }
                .sorted()
        }
}
