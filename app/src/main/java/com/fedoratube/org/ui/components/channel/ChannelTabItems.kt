package com.fedoratube.org.ui.components.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.fedoratube.org.R
import com.fedoratube.org.data.local.HomeFeedColumns
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.innertube.pages.channel.ChannelTabKind
import com.fedoratube.org.innertube.pages.renderer.FeedItem
import com.fedoratube.org.ui.components.PlaylistCard
import com.fedoratube.org.ui.components.PlaylistCardLayout
import com.fedoratube.org.ui.components.rememberFeedGridLayout
import com.fedoratube.org.ui.components.shared.FeedPagingFooter
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowLoadingIndicator
import com.fedoratube.org.ui.components.shared.MediaShortCard
import com.fedoratube.org.ui.components.shared.ShortCardDefaults
import com.fedoratube.org.ui.components.shared.card.MediaVideoCard
import com.fedoratube.org.ui.components.shared.card.VideoCardDefaults
import com.fedoratube.org.ui.components.shared.card.VideoCardLayout
import com.fedoratube.org.ui.components.shared.rememberFeedGridPlan

/**
 * Every channel tab's list, whatever it holds.
 *
 * Laid out by the same [rememberFeedGridPlan] as every other feed, so a tablet lays a channel out
 * like the rest of the app. Playlists, shows and podcasts are rows on a phone and in list mode, and
 * shelf cards in a wide grid; a thumbnail-left row shrunk into a grid cell is two words and a stamp.
 */
@Composable
internal fun ChannelTabItems(
    pagingItems: LazyPagingItems<FeedItem>?,
    kind: ChannelTabKind,
    isGridView: Boolean,
    columnPreference: HomeFeedColumns,
    hasFilterBar: Boolean,
    listState: LazyGridState,
    contentPadding: PaddingValues,
    topInset: Dp,
    onVideoClick: (Video) -> Unit,
    onShortClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onChannelClick: (String) -> Unit,
) {
    if (pagingItems == null || pagingItems.loadState.refresh is LoadState.Loading) {
        FlowLoadingIndicator(modifier = Modifier.padding(top = topInset))
        return
    }

    if (pagingItems.itemCount == 0) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = topInset),
            contentAlignment = Alignment.Center,
        ) { FlowEmptyState(title = stringResource(kind.emptyLabel())) }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val feedLayout = rememberFeedGridLayout(maxWidth, columnPreference)
        val isVideos = kind == ChannelTabKind.Videos
        val isShorts = kind == ChannelTabKind.Shorts
        val isLive = kind == ChannelTabKind.Live
        val plan =
            rememberFeedGridPlan(
                layout = feedLayout,
                listMode = !isGridView,
                itemCount = pagingItems.itemCount,
                spansOwnRow = { pagingItems.peek(it).spansRow() },
                includeLastRun = pagingItems.loadState.append.endOfPaginationReached,
                itemsKey = pagingItems.itemSnapshotList,
                compactRowSpacing = feedLayout.cardSpacing,
            )
        val sidePadding =
            when {
                isVideos || isLive -> 10.dp
                isShorts -> ShortCardDefaults.gridPadding(feedLayout)
                else -> plan.horizontalPadding
            }
        val padding =
            remember(contentPadding, sidePadding) {
                PaddingValues(
                    start = sidePadding,
                    end = sidePadding,
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
            }
        // Even a zero-height item collects the row gutter, so a wide window with chips above adds none.
        val topGap = if (feedLayout.isCompact || !hasFilterBar) 8.dp else null
        val append = pagingItems.loadState.append

        val gridColumns =
            when {
                isShorts -> GridCells.Adaptive(minSize = 120.dp)
                isVideos || isLive -> if (feedLayout.isCompact) GridCells.Fixed(2) else GridCells.Adaptive(minSize = 160.dp)
                else -> GridCells.Fixed(1)
            }

        LazyVerticalGrid(
            columns = gridColumns,
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(if (isVideos || isLive || isShorts) 10.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(if (isVideos || isLive || isShorts) 14.dp else 16.dp),
        ) {
            if (topGap != null) {
                fullSpanItem(key = "top_gap") { Spacer(Modifier.height(topGap)) }
            }
            items(
                count = pagingItems.itemCount,
                key = { index -> pagingItems.peek(index)?.itemKey() ?: "placeholder:$index" },
                contentType = { index -> pagingItems.peek(index)?.contentType() ?: "placeholder" },
                span = { index ->
                    if (pagingItems.peek(index).spansRow()) {
                        GridItemSpan(maxLineSpan)
                    } else {
                        GridItemSpan(1)
                    }
                },
            ) { index ->
                when (val item = pagingItems[index]) {
                    is FeedItem.VideoItem -> {
                        MediaVideoCard(
                            video = item.video,
                            layout = VideoCardLayout.Stacked,
                            showChannel = false,
                            onClick = { onVideoClick(item.video) },
                            useInternalPadding = false,
                        )
                    }

                    is FeedItem.ShortItem -> {
                        MediaShortCard(
                            video = item.video,
                            onClick = { onShortClick(item.video.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    is FeedItem.PlaylistItem -> {
                        val asRow = plan.isListCard(index) || feedLayout.isCompact
                        PlaylistCard(
                            playlist = item.playlist,
                            onClick = { onPlaylistClick(item.playlist.id) },
                            layout = if (asRow) PlaylistCardLayout.LIST else PlaylistCardLayout.SHELF,
                            modifier = if (asRow) Modifier else Modifier.padding(horizontal = VideoCardDefaults.Inset),
                        )
                    }

                    is FeedItem.RelatedChannelItem -> {
                        ChannelRow(channel = item.channel, onClick = { onChannelClick(item.channel.id) })
                    }

                    is FeedItem.PostItem, null -> {
                        Unit
                    }
                }
            }
            if (append is LoadState.Loading || append is LoadState.Error) {
                fullSpanItem(key = "append_footer") {
                    FeedPagingFooter(appendState = append, itemCount = pagingItems.itemCount, onRetry = pagingItems::retry)
                }
            }
            fullSpanItem(key = "bottom_gap") { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun LazyGridScope.fullSpanItem(
    key: String,
    content: @Composable () -> Unit,
) = item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }

private fun FeedItem?.spansRow(): Boolean =
    when (this) {
        is FeedItem.RelatedChannelItem, is FeedItem.PostItem -> true
        is FeedItem.VideoItem, is FeedItem.ShortItem, is FeedItem.PlaylistItem, null -> false
    }

private fun FeedItem.contentType(): String =
    when (this) {
        is FeedItem.VideoItem -> "video"
        is FeedItem.ShortItem -> "short"
        is FeedItem.PlaylistItem -> "playlist"
        is FeedItem.RelatedChannelItem -> "channel"
        is FeedItem.PostItem -> "post"
    }

private fun FeedItem.itemKey(): String =
    when (this) {
        is FeedItem.VideoItem -> "v_${video.id}"
        is FeedItem.ShortItem -> "s_${video.id}"
        is FeedItem.PlaylistItem -> "p_${playlist.id}"
        is FeedItem.RelatedChannelItem -> "c_${channel.id}"
        is FeedItem.PostItem -> "b_${post.id}"
    }

private fun ChannelTabKind.emptyLabel(): Int =
    when (this) {
        ChannelTabKind.Shorts -> R.string.error_no_shorts_found
        ChannelTabKind.Live -> R.string.error_no_live_videos_found
        ChannelTabKind.Playlists, ChannelTabKind.Podcasts, ChannelTabKind.Courses -> R.string.error_no_playlists_found
        else -> R.string.error_no_videos_found
    }
