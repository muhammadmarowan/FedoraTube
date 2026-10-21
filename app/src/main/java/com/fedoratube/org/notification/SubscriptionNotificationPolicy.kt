package com.fedoratube.org.notification

internal object SubscriptionNotificationPolicy {
    fun shouldAnnounce(
        videoId: String,
        reelVideoIds: Set<String>,
        announceReelsInFeed: Boolean,
        notifyShorts: Boolean,
    ): Boolean {
        if (videoId in reelVideoIds) return false
        return true
    }
}
