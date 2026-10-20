package io.github.aedev.flow.ui.screens.home

import io.github.aedev.flow.data.model.Video

/** One slot of the home grid: a video card, or one of the two shelves that own a whole row. */
internal sealed interface HomeFeedRow {
    val key: String

    data class VideoCard(
        val video: Video,
    ) : HomeFeedRow {
        override val key: String get() = video.id
    }

    data object ContinueWatching : HomeFeedRow {
        override val key: String = "continue_watching_shelf"
    }

    val spansRow: Boolean get() = this !is VideoCard
}

/**
 * Maps the videos into grid rows. Continue watching shelf is disabled for a clean feed.
 */
internal fun homeFeedRows(
    videos: List<Video>,
    columns: Int = 1,
    showContinueWatching: Boolean = false,
    showShorts: Boolean = false,
): List<HomeFeedRow> {
    if (videos.isEmpty()) return emptyList()
    return videos.map(HomeFeedRow::VideoCard)
}

