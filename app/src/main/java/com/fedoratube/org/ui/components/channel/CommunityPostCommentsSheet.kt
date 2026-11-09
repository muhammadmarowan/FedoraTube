package com.fedoratube.org.ui.components.channel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.data.model.Comment
import com.fedoratube.org.ui.components.shared.CommentSortFilter
import com.fedoratube.org.ui.components.shared.FlowBottomSheet
import com.fedoratube.org.ui.components.shared.FlowCommentsList
import com.fedoratube.org.ui.components.shared.defaultSheetExpandedHeight
import com.fedoratube.org.ui.components.shared.rememberFlowBottomSheetState

/**
 * YouTube-style post comments: tapping the comment icon (or the Comments preview on the post)
 * slides a bottom sheet up over the post with a "Post comments" header, the comment list and a
 * "Comment..." input bar at the bottom. The post behind dims while the sheet is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPostCommentsSheet(
    comments: List<Comment>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    totalText: String? = null,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    hasMore: Boolean = false,
    onLoadReplies: (Comment) -> Unit = {},
    onLoadMoreReplies: (Comment) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onAvatarClick: (String) -> Unit = {},
) {
    val sheetState = rememberFlowBottomSheetState()

    FlowBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = defaultSheetExpandedHeight(),
        dismissOnOutsideTap = true,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        header = { dragModifier ->
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
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.post_comments),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!totalText.isNullOrBlank()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = totalText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { sheetState.dismiss() },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        },
    ) {
        FlowCommentsList(
            comments = comments,
            isLoading = isLoading,
            listState = rememberLazyListState(),
            selectedFilter = CommentSortFilter.TOP,
            onSeekMs = {},
            onLoadReplies = onLoadReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onAuthorClick = onAuthorClick,
            onAvatarClick = onAvatarClick,
            isLoadingMore = isLoadingMore,
            onLoadMore = onLoadMore,
            hasMore = hasMore,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
