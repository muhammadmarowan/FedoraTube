package com.fedoratube.org.data.subscriptions

import com.fedoratube.org.data.local.AppDatabase
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.local.ViewHistory
import com.fedoratube.org.data.feed.finishedShortIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionWatchedVideos
    @Inject
    constructor(
        private val viewHistory: ViewHistory,
        private val playerPreferences: PlayerPreferences,
        private val database: AppDatabase,
    ) {
        val ids: Flow<Set<String>> =
            combine(
                viewHistory.getVideoWatchProgress(),
                playerPreferences.watchedThreshold,
                database.downloadDao().getVideoDownloads(),
            ) { history, threshold, downloads ->
                // Always dedup what the viewer has already watched (plus downloaded ids).
                val downloadedIds = downloads.mapTo(HashSet()) { it.download.videoId }
                history
                    .asSequence()
                    .filter { threshold.isWatched(it.position, it.duration) || it.videoId in downloadedIds }
                    .map { it.videoId }
                    .toHashSet()
            }.distinctUntilChanged()

        /** Watched reels, for the Shorts shelf and the Shorts queues; governed by the Shorts setting alone. */
        val shortIds: Flow<Set<String>> =
            combine(
                viewHistory.getVideoWatchProgress(),
                playerPreferences.hideWatchedShorts,
            ) { history, hideWatched ->
                if (hideWatched) history.finishedShortIds() else emptySet()
            }.distinctUntilChanged()
    }
