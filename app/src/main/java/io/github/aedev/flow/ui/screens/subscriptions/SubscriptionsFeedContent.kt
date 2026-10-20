package io.github.aedev.flow.ui.screens.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aedev.flow.R
import io.github.aedev.flow.data.local.HomeFeedColumns
import io.github.aedev.flow.data.model.Channel
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.shorts.queue.ShortsQueueSource
import io.github.aedev.flow.ui.components.layout.flowBottomContentPadding
import io.github.aedev.flow.ui.components.shared.FlowPullToRefreshBox
import io.github.aedev.flow.ui.components.shared.card.MediaVideoCard
import io.github.aedev.flow.ui.components.shared.card.VideoCardLayout

private val HeaderTopPadding = 8.dp
private val HeaderBottomPadding = 12.dp
private val ErrorCardPadding = 4.dp
private val FeedTopPadding = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionsFeedContent(
    state: SubscriptionsUiState,
    videos: List<Video>,
    topChannels: List<Channel>,
    gridState: LazyGridState,
    columnPreference: HomeFeedColumns,
    onRefresh: () -> Unit,
    onVideoClick: (Video) -> Unit,
    onShortClick: (ShortsQueueSource) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: ((Channel) -> Unit)? = null,
    onViewAllClick: () -> Unit,
    onMusicSubscriptionsClick: () -> Unit,
    onRetryFailedChannels: () -> Unit = {},
    onDismissFailedChannels: () -> Unit = {},
    onGroupSelected: (String?) -> Unit = {},
    onManageGroups: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            pullRefreshState.animateToHidden()
        }
    }

    FlowPullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = onRefresh,
        state = pullRefreshState,
        modifier = modifier.fillMaxSize(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val nonShortVideos = remember(videos) { videos.filter { !it.isShort } }
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(1),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = FeedTopPadding, bottom = flowBottomContentPadding()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        SubscriptionsQuickAccessRow(
                            channels = topChannels,
                            selectedChannelId = state.selectedChannelId,
                            onChannelClick = onChannelClick,
                            onChannelLongClick = onChannelLongClick,
                            onViewAllClick = onViewAllClick,
                            onMusicClick = onMusicSubscriptionsClick,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = HeaderTopPadding, bottom = HeaderBottomPadding),
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )

                        SubscriptionFeedErrorCard(
                            failedChannelNames = state.failedChannelNames,
                            failedChannelIds = state.failedChannelIds,
                            failedChannelReasons = state.failedChannelReasons,
                            onRetry = onRetryFailedChannels,
                            onDismiss = onDismissFailedChannels,
                            modifier =
                                Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = ErrorCardPadding,
                                ),
                        )
                    }
                }

                if (nonShortVideos.isEmpty() && state.selectedChannelId != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.no_videos_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(
                        items = nonShortVideos,
                        key = { it.id },
                        contentType = { "video" },
                        span = { GridItemSpan(1) },
                    ) { video ->
                        MediaVideoCard(
                            video = video,
                            layout = VideoCardLayout.Stacked,
                            onClick = { onVideoClick(video) },
                            useInternalPadding = false,
                        )
                    }
                }
            }
        }
    }
}
