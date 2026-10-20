package io.github.aedev.flow.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.VideoHistoryEntry
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.ui.components.FeedGridLayout
import io.github.aedev.flow.ui.components.layout.flowBottomContentPadding
import io.github.aedev.flow.ui.components.shared.FlowFeedProgress
import io.github.aedev.flow.ui.components.shared.card.MediaVideoCard
import io.github.aedev.flow.ui.components.shared.card.VideoCardLayout

private const val FEED_FOOTER_MIN_VIDEOS = 100
private val FeedTopPadding = 4.dp

@Composable
internal fun HomeFeedGrid(
    uiState: HomeUiState,
    feedLayout: FeedGridLayout,
    isListView: Boolean,
    gridState: LazyGridState,
    onVideoClick: (Video) -> Unit,
    onEnrichChannelMetadata: (Video) -> Unit,
    onContinueWatchingClick: (VideoHistoryEntry) -> Unit,
    onContinueWatchingRemove: (String) -> Unit,
    onShortClick: (List<Video>, Video) -> Unit,
    onSeeAllHistory: () -> Unit,
    onOpenShortsFeed: () -> Unit,
    onShortsShown: (ids: List<String>) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = {},
    footerMinVideos: Int = FEED_FOOTER_MIN_VIDEOS,
) {
    val videos = uiState.videos
    val channelMemoryReason = stringResource(R.string.home_reason_channel_memory)
    val rows =
        remember(videos) {
            homeFeedRows(
                videos = videos,
                columns = 1,
                showContinueWatching = false,
                showShorts = false,
            )
        }

    LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(1),
        modifier =
            modifier
                .fillMaxSize()
                .testTag("home_feed"),
        state = gridState,
        contentPadding = PaddingValues(top = FeedTopPadding, bottom = flowBottomContentPadding()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        itemsIndexed(
            items = rows,
            key = { _, row -> row.key },
            contentType = { _, row -> row::class },
            span = { _, _ -> GridItemSpan(1) },
        ) { _, row ->
            when (row) {
                is HomeFeedRow.VideoCard -> {
                    LaunchedEffect(row.video.id, row.video.channelId, row.video.channelThumbnailUrl) {
                        onEnrichChannelMetadata(row.video)
                    }
                    MediaVideoCard(
                        video = row.video,
                        layout = VideoCardLayout.Stacked,
                        onClick = { onVideoClick(row.video) },
                        useInternalPadding = false,
                        reason = channelMemoryReason.takeIf { uiState.origins[row.video.id]?.kind == HomeCardOrigin.Kind.CHANNEL_MEMORY },
                        whyShown = uiState.origins[row.video.id]?.label(),
                        modifier = Modifier.testTag("home_video_card"),
                    )
                }

                HomeFeedRow.ContinueWatching -> {
                    // Continue watching shelf disabled
                }
            }
        }

        if (uiState.isLoadingMore) {
            item(
                key = "loading_indicator",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                FlowFeedProgress()
            }
        }

        if (uiState.loadMoreFailed && !uiState.isLoadingMore) {
            item(
                key = "load_more_failed",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                HomeLoadMoreFailed(onRetry = onRetryLoadMore)
            }
        }

        if (!uiState.hasMorePages && videos.size > footerMinVideos && !uiState.isLoadingMore) {
            item(
                key = "feed_footer",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                FlowFeedFooter(
                    videoCount = videos.size,
                    onRefresh = onRefresh,
                )
            }
        }
    }
}
