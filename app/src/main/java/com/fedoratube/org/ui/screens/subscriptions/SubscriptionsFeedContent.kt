package com.fedoratube.org.ui.screens.subscriptions

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
import com.fedoratube.org.R
import com.fedoratube.org.data.local.HomeFeedColumns
import com.fedoratube.org.data.model.Channel
import com.fedoratube.org.data.model.Comment
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.shorts.queue.ShortsQueueSource
import com.fedoratube.org.innertube.pages.renderer.CommunityPost
import com.fedoratube.org.ui.components.channel.CommunityPostCard
import com.fedoratube.org.ui.components.channel.CommunityPostInlineComments
import com.fedoratube.org.ui.components.layout.flowBottomContentPadding
import com.fedoratube.org.ui.components.shared.FlowPullToRefreshBox
import com.fedoratube.org.ui.components.shared.card.MediaVideoCard
import com.fedoratube.org.ui.components.shared.card.VideoCardLayout

sealed interface SubscriptionsFeedRow {
    val key: String

    data class VideoRow(val video: Video) : SubscriptionsFeedRow {
        override val key: String get() = "v_${video.id}"
    }

    data class PostRow(val post: CommunityPost) : SubscriptionsFeedRow {
        override val key: String get() = "p_${post.id}"
    }
}

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
    onCommunityPostComments: (CommunityPost) -> Unit = {},
    onCloseCommunityPostComments: () -> Unit = {},
    onLoadMoreCommunityPostComments: () -> Unit = {},
    onLoadCommunityCommentReplies: (Comment, Boolean) -> Unit = { _, _ -> },
    onCommunityPostShare: (CommunityPost) -> Unit = {},
    onOpenChannel: (Channel) -> Unit = {},
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
            val feedRows =
                remember(nonShortVideos, state.communityPosts) {
                    if (state.communityPosts.isEmpty()) {
                        nonShortVideos.map { SubscriptionsFeedRow.VideoRow(it) }
                    } else if (nonShortVideos.isEmpty()) {
                        state.communityPosts.map { SubscriptionsFeedRow.PostRow(it) }
                    } else {
                        val result = mutableListOf<SubscriptionsFeedRow>()
                        val postQueue = ArrayDeque(state.communityPosts)
                        nonShortVideos.forEachIndexed { index, video ->
                            result.add(SubscriptionsFeedRow.VideoRow(video))
                            if ((index == 0 || (index + 1) % 4 == 0) && postQueue.isNotEmpty()) {
                                result.add(SubscriptionsFeedRow.PostRow(postQueue.removeFirst()))
                            }
                        }
                        while (postQueue.isNotEmpty()) {
                            result.add(SubscriptionsFeedRow.PostRow(postQueue.removeFirst()))
                        }
                        result
                    }
                }
            LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(1),
                state = gridState,
                userScrollEnabled = state.activeCommunityPost == null,
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

                if (feedRows.isEmpty() && state.selectedChannelId != null) {
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
                        items = feedRows,
                        key = { it.key },
                        contentType = { it::class.java.simpleName },
                        span = { GridItemSpan(1) },
                    ) { row ->
                        when (row) {
                            is SubscriptionsFeedRow.VideoRow -> {
                                MediaVideoCard(
                                    video = row.video,
                                    layout = VideoCardLayout.Stacked,
                                    onClick = { onVideoClick(row.video) },
                                    useInternalPadding = false,
                                )
                            }
                            is SubscriptionsFeedRow.PostRow -> {
                                CommunityPostCard(
                                    post = row.post,
                                    onAuthorClick = {
                                        val channelId =
                                            row.post.channelId.takeIf { it.isNotEmpty() }
                                                ?: state.subscribedChannels.find { it.name.equals(row.post.authorName, ignoreCase = true) }?.id
                                        if (channelId != null) {
                                            onOpenChannel(
                                                Channel(
                                                    id = channelId,
                                                    name = row.post.authorName,
                                                    thumbnailUrl = row.post.authorAvatarUrl,
                                                    subscriberCount = 0L,
                                                ),
                                            )
                                        }
                                    },
                                    onCommentsClick = {
                                        if (state.activeCommunityPost?.id == row.post.id) {
                                            onCloseCommunityPostComments()
                                        } else {
                                            onCommunityPostComments(row.post)
                                        }
                                    },
                                    onShareClick = { onCommunityPostShare(row.post) },
                                    onVideoClick = onVideoClick,
                                    compact = false,
                                    commentsContent =
                                        if (state.activeCommunityPost?.id == row.post.id) {
                                            {
                                                CommunityPostInlineComments(
                                                    comments = state.communityComments,
                                                    isLoading = state.isLoadingCommunityComments,
                                                    onClose = onCloseCommunityPostComments,
                                                    totalText = row.post.commentCountText,
                                                    isLoadingMore = state.isLoadingMoreCommunityComments,
                                                    onLoadMore = onLoadMoreCommunityPostComments,
                                                    hasMore = state.communityCommentsContinuation != null,
                                                    onLoadReplies = { comment -> onLoadCommunityCommentReplies(comment, false) },
                                                    onLoadMoreReplies = { comment -> onLoadCommunityCommentReplies(comment, true) },
                                                    onAuthorClick = { authorRef ->
                                                        onOpenChannel(
                                                            Channel(
                                                                id = authorRef,
                                                                name = "",
                                                                thumbnailUrl = "",
                                                                subscriberCount = 0L,
                                                            ),
                                                        )
                                                    },
                                                    onAvatarClick = { authorRef ->
                                                        onOpenChannel(
                                                            Channel(
                                                                id = authorRef,
                                                                name = "",
                                                                thumbnailUrl = "",
                                                                subscriberCount = 0L,
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
