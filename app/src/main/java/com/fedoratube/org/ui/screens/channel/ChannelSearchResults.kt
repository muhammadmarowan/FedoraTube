package com.fedoratube.org.ui.screens.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.data.local.HomeFeedColumns
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.ui.components.rememberFeedGridLayout
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowLoadingIndicator
import com.fedoratube.org.ui.components.shared.card.MediaVideoCard
import com.fedoratube.org.ui.components.shared.card.VideoCardLayout
import com.fedoratube.org.ui.components.shared.rememberFeedGridPlan

@Composable
internal fun ChannelSearchResults(
    uiState: ChannelUiState,
    listState: LazyGridState,
    contentPadding: PaddingValues,
    topInset: Dp,
    isGridView: Boolean,
    columnPreference: HomeFeedColumns,
    onVideoClick: (Video) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.isSearching -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = topInset),
                contentAlignment = Alignment.Center,
            ) { FlowLoadingIndicator() }
        }

        uiState.searchErrorLog != null -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = topInset),
                contentAlignment = Alignment.Center,
            ) {
                ChannelRequestErrorState(
                    message = stringResource(R.string.channel_search_failed),
                    errorLog = uiState.searchErrorLog,
                    onRetry = onRetry,
                )
            }
        }

        uiState.searchResults.isEmpty() -> {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(top = topInset),
                contentAlignment = Alignment.Center,
            ) {
                FlowEmptyState(title = stringResource(R.string.channel_search_no_results, uiState.searchQuery))
            }
        }

        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val feedLayout = rememberFeedGridLayout(maxWidth, columnPreference)
                val plan =
                    rememberFeedGridPlan(
                        layout = feedLayout,
                        listMode = !isGridView,
                        itemCount = uiState.searchResults.size,
                        spansOwnRow = { false },
                        includeLastRun = true,
                        itemsKey = uiState.searchResults,
                    )
                val padding =
                    remember(contentPadding, plan.horizontalPadding) {
                        plan.contentPadding(
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding(),
                        )
                    }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items = uiState.searchResults,
                        key = { _, video -> "search_${video.id}" },
                        contentType = { _, _ -> "video" },
                        span = { _, _ -> GridItemSpan(1) },
                    ) { index, video ->
                        MediaVideoCard(
                            video = video,
                            layout = VideoCardLayout.Stacked,
                            showChannel = false,
                            onClick = { onVideoClick(video) },
                            useInternalPadding = false,
                        )
                    }
                    item(key = "bottom_gap", span = { GridItemSpan(1) }) { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
