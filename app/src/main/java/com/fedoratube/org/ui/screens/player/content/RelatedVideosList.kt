package com.fedoratube.org.ui.screens.player.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fedoratube.org.data.local.PlayerRelatedCardStyle
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.ui.components.FeedGridLayout
import com.fedoratube.org.ui.components.partialRowIndices
import com.fedoratube.org.ui.components.shared.card.MediaVideoCard
import com.fedoratube.org.ui.components.shared.card.VideoCardLayout

/**
 * Related videos content for LazyListScope.
 */
internal fun LazyListScope.relatedVideosContent(
    relatedVideos: List<Video>,
    onVideoClick: (Video) -> Unit,
    cardStyle: PlayerRelatedCardStyle = PlayerRelatedCardStyle.FULL_WIDTH,
) {
    // Video items
    items(
        count = relatedVideos.size,
        key = { index -> relatedVideos[index].id },
    ) { index ->
        val relatedVideo = relatedVideos[index]
        MediaVideoCard(
            video = relatedVideo,
            layout = VideoCardLayout.Stacked,
            useInternalPadding = false,
            onClick = { onVideoClick(relatedVideo) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
        )
    }
}

/**
 * The related list on a wide single-pane player: the shared feed grid's columns, chunked into rows
 * of a [androidx.compose.foundation.lazy.LazyColumn] so the video details above keep their own
 * padding.
 */
internal fun LazyListScope.relatedVideosGridContent(
    relatedVideos: List<Video>,
    layout: FeedGridLayout,
    onVideoClick: (Video) -> Unit,
    cardStyle: PlayerRelatedCardStyle = PlayerRelatedCardStyle.FULL_WIDTH,
) {
    val columns = layout.columns
    val rows = relatedGridRows(relatedVideos.size, columns)

    items(
        count = rows.size,
        key = { index -> rows[index].joinToString { relatedVideos[it].id } },
        contentType = { index -> if (rows[index].size == 1) "related_row" else "related_grid_row" },
    ) { index ->
        val row = rows[index]
        if (row.size == 1) {
            val video = relatedVideos[row.single()]
            MediaVideoCard(
                video = video,
                layout = VideoCardLayout.Stacked,
                useInternalPadding = false,
                onClick = { onVideoClick(video) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
            )
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = layout.contentPadding, vertical = GridRowVerticalPadding),
            ) {
                row.forEach { videoIndex ->
                    val video = relatedVideos[videoIndex]
                    Box(modifier = Modifier.weight(1f)) {
                        MediaVideoCard(
                            video = video,
                            layout = VideoCardLayout.Stacked,
                            useInternalPadding = false,
                            onClick = { onVideoClick(video) },
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Whether the related list lays out as [relatedVideosGridContent]: always on the medium layout, and on
 * a phone once Cards per row asks for more than one full-width card.
 */
internal fun relatedListUsesGrid(
    isMediumLayout: Boolean,
    cardStyle: PlayerRelatedCardStyle,
    layout: FeedGridLayout,
): Boolean = isMediumLayout || (cardStyle == PlayerRelatedCardStyle.FULL_WIDTH && layout.columns > 1)

/** Full rows of [columns] indices, then each index a full row cannot hold on a row of its own. */
internal fun relatedGridRows(
    count: Int,
    columns: Int,
): List<List<Int>> {
    val partial = partialRowIndices(List(count) { false }, columns)
    val full = (0 until count).filterNot { it in partial }.chunked(columns.coerceAtLeast(1))
    return full + partial.sorted().map(::listOf)
}

private val GridRowVerticalPadding = 6.dp
