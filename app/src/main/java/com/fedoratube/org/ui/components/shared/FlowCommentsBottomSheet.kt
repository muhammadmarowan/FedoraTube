package com.fedoratube.org.ui.components.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.data.model.Comment
import com.fedoratube.org.ui.components.shared.FlowBottomSheet
import com.fedoratube.org.ui.components.shared.defaultSheetExpandedHeight
import com.fedoratube.org.ui.components.shared.rememberFlowBottomSheetState

@Composable
fun FlowCommentsBottomSheet(
    comments: List<Comment>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSeekMs: (Long) -> Unit = {},
    onFilterChanged: (CommentSortFilter) -> Unit = {},
    onLoadReplies: (Comment) -> Unit = {},
    onLoadMoreReplies: (Comment) -> Unit = {},
    selectedFilter: CommentSortFilter = CommentSortFilter.TOP,
    totalText: String? = null,
    artworkUrl: String? = null,
    timedOnly: Boolean = false,
    onTimedChange: ((Boolean) -> Unit)? = null,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = false,
    onAuthorClick: (String) -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    onSheetProgressChange: (Float) -> Unit = {},
    dismissOnOutsideTap: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    // Shorts passes both false: no sort-category chips and no cross (close) button — the sheet
    // closes by dragging it down or pressing back, exactly like YouTube Shorts comments.
    showSortChips: Boolean = true,
    showCloseButton: Boolean = true,
    externalProgress: Animatable<Float, AnimationVector1D>? = null,
    animationSpec: AnimationSpec<Float> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberFlowBottomSheetState()
    val commentsListState = rememberLazyListState()
    val tint = rememberMediaArtworkTint(artworkUrl)
    val chipsVisible = rememberCommentSortChipsVisible(commentsListState)

    LaunchedEffect(selectedFilter) {
        commentsListState.scrollToItem(0)
    }

    FlowBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissOnOutsideTap = dismissOnOutsideTap,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surface,
        onProgressChange = onSheetProgressChange,
        externalProgress = externalProgress,
        animationSpec = animationSpec,
        header = { dragModifier ->
            // Not FlowSheetHeader: the sort chips sit under the title row, inside the same padding
            // and above the divider, which no header parameter can express.
            CommentsSheetHeader(
                selectedFilter = selectedFilter,
                totalText = totalText,
                timedOnly = timedOnly,
                onTimedChange = onTimedChange,
                onFilterChanged = onFilterChanged,
                onClose = { sheetState.dismiss() },
                showSortChips = showSortChips,
                sortChipsVisible = chipsVisible,
                showCloseButton = showCloseButton,
                dragModifier = dragModifier,
            )
        },
    ) {
        FlowCommentsList(
            comments = comments,
            isLoading = isLoading,
            listState = commentsListState,
            selectedFilter = selectedFilter,
            onSeekMs = onSeekMs,
            onLoadReplies = onLoadReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onAuthorClick = onAuthorClick,
            onAvatarClick = onAvatarClick,
            isLoadingMore = isLoadingMore,
            onLoadMore = onLoadMore,
            hasMore = hasMore,
            emptyMessageRes = if (timedOnly) R.string.no_timed_comments else R.string.no_comments_yet,
            tint = tint,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheetHeader(
    selectedFilter: CommentSortFilter,
    totalText: String?,
    timedOnly: Boolean,
    onTimedChange: ((Boolean) -> Unit)?,
    onFilterChanged: (CommentSortFilter) -> Unit,
    onClose: () -> Unit,
    dragModifier: Modifier,
    showSortChips: Boolean = true,
    sortChipsVisible: Boolean = true,
    showCloseButton: Boolean = true,
) {
    Column(modifier = dragModifier) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            BottomSheetDefaults.DragHandle()
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.comments),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!totalText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showCloseButton) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            }
            if (showSortChips) {
                AnimatedVisibility(
                    visible = sortChipsVisible,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                ) {
                    CommentSortChips(
                        selected = selectedFilter,
                        onSelect = onFilterChanged,
                        timedOnly = timedOnly,
                        onTimedChange = onTimedChange,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        HorizontalDivider(
            color =
                MaterialTheme.colorScheme.outlineVariant
                    .copy(alpha = FlowSheetHeaderDefaults.DividerAlpha),
        )
    }
}
