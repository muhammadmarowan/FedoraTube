package com.fedoratube.org.ui.screens.channel

import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.fedoratube.org.R
import com.fedoratube.org.data.model.SubscriptionGroup
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.innertube.pages.channel.ChannelHeader
import com.fedoratube.org.innertube.pages.channel.ChannelTabKind
import com.fedoratube.org.innertube.pages.renderer.CommunityPost
import com.fedoratube.org.ui.components.channel.ChannelBanner
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import com.fedoratube.org.ui.components.shared.ChannelAvatarImage
import com.fedoratube.org.ui.components.shared.CollectionEditDialog
import com.fedoratube.org.ui.components.shared.CollectionSheetEntry
import com.fedoratube.org.ui.components.shared.CommentSortFilter
import com.fedoratube.org.ui.components.shared.FlowCommentsBottomSheet
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.FlowLoadingIndicator
import com.fedoratube.org.ui.components.shared.FlowNoteEditorDialog
import com.fedoratube.org.ui.components.shared.FlowSubscribeButton
import com.fedoratube.org.ui.components.shared.FullSizeImageDialog
import com.fedoratube.org.ui.components.shared.SaveToCollectionSheet
import com.fedoratube.org.ui.components.shared.sortCommentsByFilter
import com.fedoratube.org.ui.theme.extendedColors
import com.fedoratube.org.ui.youtubeChannelUrl
import com.fedoratube.org.utils.ThumbnailUrlResolver
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(
    channelUrl: String,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    onShortClick: (videoId: String, sortIndex: Int) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences =
        remember(context) {
            com.fedoratube.org.data.local
                .PlayerPreferences(context)
        }
    val isGridView by preferences.channelIsGridView.collectAsState(initial = false)
    val uiState by viewModel.uiState.collectAsState()
    val communityUiState by viewModel.communityUiState.collectAsState()
    val tabStates by viewModel.tabStates.collectAsStateWithLifecycle()
    val subscribedChannelIds by viewModel.subscribedChannelIds.collectAsStateWithLifecycle()
    val channelNote by viewModel.channelNote.collectAsStateWithLifecycle()
    val notesEnabled by viewModel.notesEnabled.collectAsStateWithLifecycle()
    val showShortsTab by viewModel.showShortsTab.collectAsStateWithLifecycle()
    var showNoteEditor by rememberSaveable { mutableStateOf(false) }
    val subscriptionGroups by viewModel.subscriptionGroups.collectAsStateWithLifecycle()
    var showGroupSheet by rememberSaveable { mutableStateOf(false) }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }

    // Shelves and posts have their own layout; only the item grids can switch.
    val showLayoutToggle =
        uiState.selectedTab != null &&
            uiState.selectedTab !in setOf(ChannelTabKind.Home, ChannelTabKind.Posts)

    LaunchedEffect(channelUrl) { viewModel.loadChannel(channelUrl) }

    var showCollapsedChannelTitle by remember(channelUrl) { mutableStateOf(false) }
    val collapsedChannelTitle = uiState.header?.title.orEmpty()
    var communityCommentSort by rememberSaveable { mutableStateOf(CommentSortFilter.TOP) }
    val sortedCommunityComments =
        remember(communityUiState.comments, communityCommentSort) {
            sortCommentsByFilter(communityUiState.comments, communityCommentSort)
        }

    LaunchedEffect(communityUiState.activePost?.id) {
        communityCommentSort = CommentSortFilter.TOP
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            FlowTopBar(
                title = {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showCollapsedChannelTitle && collapsedChannelTitle.isNotBlank(),
                    ) {
                        Text(
                            text = collapsedChannelTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                onBack = onBackClick,
                actions = {},
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        FlowLoadingIndicator()
                    }

                    uiState.error != null -> {
                        FlowErrorState(
                            error = uiState.error ?: stringResource(R.string.failed_to_load_channel),
                            onRetry = { viewModel.loadChannel(channelUrl) },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    uiState.header != null -> {
                        ChannelContent(
                            uiState = uiState,
                            communityUiState = communityUiState,
                            tabStates = tabStates,
                            showShortsTab = showShortsTab,
                            onFilterSelected = viewModel::selectTabFilter,
                            subscribedChannelIds = subscribedChannelIds,
                            channelNote = channelNote.takeIf { notesEnabled },
                            onEditNote = { showNoteEditor = true }.takeIf { notesEnabled },
                            onSubscribeChannel = viewModel::setChannelSubscription,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onShortClick = { videoId ->
                                onShortClick(videoId, tabStates[ChannelTabKind.Shorts]?.selected?.firstOrNull() ?: 0)
                            },
                            onPlaylistClick = onPlaylistClick,
                            onSubscribeClick = { viewModel.toggleSubscription() },
                            onUnsubscribeClick = { viewModel.unsubscribe() },
                            onNotificationChange = { viewModel.setNotificationState(it) },
                            onManageGroups = { showGroupSheet = true },
                            onTabSelected = { viewModel.selectTab(it) },
                            onSearchToggle = { viewModel.setSearchActive(!uiState.searchActive) },
                            onSearchQueryChange = { viewModel.searchInChannel(it) },
                            onCommunityPostComments = viewModel::openCommunityPostComments,
                            onCloseCommunityPostComments = viewModel::closeCommunityPostComments,
                            onLoadMoreCommunityPostComments = viewModel::loadMoreCommunityPostComments,
                            onLoadCommunityCommentReplies = { comment, append ->
                                if (append) {
                                    viewModel.loadMoreCommunityCommentReplies(comment)
                                } else {
                                    viewModel.loadCommunityCommentReplies(comment)
                                }
                            },
                            communityComments = sortedCommunityComments,
                            onCommunityPostShare = { post ->
                                val shareIntent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/post/${post.id}")
                                    }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_community_post),
                                    ),
                                )
                            },
                            onLoadMoreCommunityPosts = viewModel::loadMoreCommunityPosts,
                            onRetryCommunityPosts = viewModel::retryCommunityPosts,
                            initialScrollIndex = viewModel.listScrollIndex,
                            initialScrollOffset = viewModel.listScrollOffset,
                            onScrollChanged = { idx, off -> viewModel.saveScrollPosition(idx, off) },
                            onCollapsedTitleVisibilityChange = { showCollapsedChannelTitle = it },
                        )
                    }
                }
            }
        }

        BackHandler(enabled = communityUiState.activePost != null) {
            viewModel.closeCommunityPostComments()
        }
    }

    val channelId = uiState.header?.id.orEmpty()
    if (showGroupSheet && channelId.isNotBlank()) {
        ChannelGroupSheet(
            groups = subscriptionGroups,
            channelId = channelId,
            onToggle = { groupName, inGroup -> viewModel.setChannelInGroup(groupName, channelId, inGroup) },
            onCreateNew = {
                showGroupSheet = false
                showCreateGroupDialog = true
            },
            onDismiss = { showGroupSheet = false },
        )
    }

    if (showNoteEditor && notesEnabled) {
        FlowNoteEditorDialog(
            initialText = channelNote.orEmpty(),
            title = stringResource(R.string.note_channel_title),
            onSave = viewModel::saveChannelNote,
            onDismiss = { showNoteEditor = false },
        )
    }

    if (showCreateGroupDialog && channelId.isNotBlank()) {
        CollectionEditDialog(
            title = stringResource(R.string.new_group),
            confirmLabel = stringResource(R.string.save),
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name, _ ->
                viewModel.createGroupWithChannel(name, channelId)
                showCreateGroupDialog = false
            },
            icon = Icons.Rounded.Folder,
            showDescription = false,
        )
    }
}
