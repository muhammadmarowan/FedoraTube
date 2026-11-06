package com.fedoratube.org.data.feed

import com.fedoratube.org.data.model.Video
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory shelves handed to the Shorts player: a search topic row, a subscriptions strip, a
 * home shelf. Keyed by the token [ShortsQueueSource.Snapshot] carries, so the queue survives
 * navigation without serialising a whole list into a route. Process death drops the shelves and
 * the source then falls back to the algorithmic feed, which is the right queue anyway.
 */
object ShortsSnapshotStore {
    private val shelves = ConcurrentHashMap<String, List<Video>>()

    fun register(videos: List<Video>): String {
        val token = UUID.randomUUID().toString()
        shelves[token] = videos.distinctBy { it.id }
        // Keep the map small: each shelf is a screenful of rows, and a long session could
        // otherwise accumulate every topic the user ever searched.
        if (shelves.size > MAX_SHELVES) {
            shelves.keys.firstOrNull()?.let(shelves::remove)
        }
        return token
    }

    fun take(token: String): List<Video>? = shelves[token]

    private const val MAX_SHELVES = 8
}
