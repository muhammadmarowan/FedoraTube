package com.fedoratube.org.ui.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fedoratube.org.R
import com.fedoratube.org.data.model.PlaylistInfo
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.music.DownloadedTrack
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.stats.RecapPeriod
import com.fedoratube.org.data.video.DownloadedVideo
import com.fedoratube.org.ui.OnTabReselected
import com.fedoratube.org.ui.components.PlaylistCard
import com.fedoratube.org.ui.components.PlaylistCardLayout
import com.fedoratube.org.ui.components.dialogs.ShareAppQrDialog
import com.fedoratube.org.ui.components.layout.flowBottomContentPadding
import com.fedoratube.org.ui.components.layout.navigation.FlowTab
import com.fedoratube.org.ui.components.layout.topbar.FlowGlobalActionsMode
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import com.fedoratube.org.ui.components.layout.topbar.LocalFlowGlobalActions
import com.fedoratube.org.ui.components.library.LibrarySection
import com.fedoratube.org.ui.components.library.MusicDownloadRow
import com.fedoratube.org.ui.components.library.CircularSelectBadge
import com.fedoratube.org.ui.components.library.PlaylistCreationTarget
import com.fedoratube.org.ui.components.library.PlaylistVideoRow
import com.fedoratube.org.ui.components.library.VideoDownloadItem
import com.fedoratube.org.ui.components.shared.ChannelAvatarImage
import com.fedoratube.org.ui.components.shared.CollectionEditDialog
import com.fedoratube.org.ui.components.shared.ExplicitBadge
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowSectionHeader
import com.fedoratube.org.ui.components.shared.FlowSelectionAction
import com.fedoratube.org.ui.components.shared.FlowSelectionToolbar
import com.fedoratube.org.ui.components.shared.MediaKind
import com.fedoratube.org.ui.components.shared.MediaThumbnail
import com.fedoratube.org.ui.components.stats.RecapEntryCard
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val TabRowHeight = 42.dp
private val IndicatorHeight = 2.5.dp
private val IndicatorWidth = 28.dp

@Composable
fun LibraryScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToPlaylists: (MediaKind?) -> Unit,
    onNavigateToLikedVideos: () -> Unit,
    onNavigateToLikedMusic: () -> Unit,
    onNavigateToWatchLater: () -> Unit,
    onNavigateToLocalMedia: () -> Unit = {},
    onNavigateToNotes: () -> Unit,
    onNavigateToDownloads: () -> Unit = {},
    onManageData: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenRecap: (RecapPeriod?) -> Unit,
    onVideoClick: (Video) -> Unit,
    onMusicClick: (MusicTrack, List<MusicTrack>, String) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    onMusicPlaylistClick: (String) -> Unit = {},
    onDownloadedVideoClick: (List<DownloadedVideo>, Int) -> Unit = { _, _ -> },
    onDownloadedMusicClick: (List<DownloadedTrack>, Int) -> Unit = { _, _ -> },
    topCategoryTabs: List<FlowTab> = emptyList(),
    onCategorySelected: (FlowTab) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    var showQrDialog by rememberSaveable { mutableStateOf(false) }

    var historyShelfHeightPx by remember { mutableFloatStateOf(0f) }
    var scrollOffsetPx by remember { mutableFloatStateOf(0f) }

    OnTabReselected(FlowTab.Library.route) {
        coroutineScope.launch {
            scrollOffsetPx = 0f
            pagerState.scrollToPage(0)
        }
    }

    val nestedScrollConnection = remember(historyShelfHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0 && historyShelfHeightPx > 0f) {
                    val oldOffset = scrollOffsetPx
                    val newOffset = (scrollOffsetPx + delta).coerceIn(-historyShelfHeightPx, 0f)
                    val consumed = newOffset - oldOffset
                    scrollOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0 && historyShelfHeightPx > 0f) {
                    val oldOffset = scrollOffsetPx
                    val newOffset = (scrollOffsetPx + delta).coerceIn(-historyShelfHeightPx, 0f)
                    val consumed = newOffset - oldOffset
                    scrollOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.library),
                globalActions = FlowGlobalActionsMode.None,
                actions = {
                    IconButton(onClick = { showQrDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = stringResource(R.string.share_app_qr),
                        )
                    }
                    val globalActions = LocalFlowGlobalActions.current
                    if (globalActions != null) {
                        IconButton(onClick = globalActions.onOpenSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        BoxWithConstraints(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .nestedScroll(nestedScrollConnection),
        ) {
            val totalHeight = maxHeight
            val historyShelfHeightDp = with(LocalDensity.current) { historyShelfHeightPx.toDp() }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(totalHeight + historyShelfHeightDp)
                        .offset { IntOffset(0, scrollOffsetPx.roundToInt()) },
            ) {
                // 1. Watch History Carousel Shelf
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { size ->
                                historyShelfHeightPx = size.height.toFloat()
                            },
                ) {
                    LibraryMediaShelfRoute(
                        section = LibrarySection.HISTORY,
                        itemsFlow = viewModel.history,
                        onTitleClick = onNavigateToHistory,
                        onVideoClick = onVideoClick,
                        onMusicClick = onMusicClick,
                        onDownloadedVideoClick = onDownloadedVideoClick,
                        onDownloadedMusicClick = onDownloadedMusicClick,
                    )
                }

                // 2. Sliding Category Tabs Row: Downloads, Watch later, Playlists, Notes
                LibraryCategoryTabsRow(
                    pagerState = pagerState,
                    onTabSelected = { page ->
                        coroutineScope.launch { pagerState.animateScrollToPage(page) }
                    },
                )

                // 3. Category Feeds (Horizontal Pager)
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) { page ->
                    when (page) {
                        0 -> DownloadsFeed(
                            viewModel = viewModel,
                            onDownloadedVideoClick = onDownloadedVideoClick,
                            onDownloadedMusicClick = onDownloadedMusicClick,
                        )
                        1 -> WatchLaterFeed(
                            viewModel = viewModel,
                            onVideoClick = onVideoClick,
                        )
                        2 -> PlaylistsFeed(
                            viewModel = viewModel,
                            onPlaylistClick = { id -> onPlaylistClick(id) },
                            onMusicPlaylistClick = { id -> onMusicPlaylistClick(id) },
                        )
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        ShareAppQrDialog(onDismiss = { showQrDialog = false })
    }
}

/**
 * YouTube-style sliding category tabs row: Downloads, Watch later, Playlists, Notes
 * with animated underline indicator.
 */
@Composable
private fun LibraryCategoryTabsRow(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        stringResource(R.string.downloads_title),
        stringResource(R.string.watch_later),
        stringResource(R.string.library_playlists_label),
    )

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TabRowHeight)
                .background(MaterialTheme.colorScheme.background),
    ) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset = tabWidth * (pagerState.currentPage + pagerState.currentPageOffsetFraction) + (tabWidth - IndicatorWidth) / 2
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = pagerState.currentPage == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    animationSpec = tween(150),
                    label = "tabTextColor",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                onTabSelected(index)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorOffset)
                    .width(IndicatorWidth)
                    .height(IndicatorHeight)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun DownloadsFeed(
    viewModel: LibraryViewModel,
    onDownloadedVideoClick: (List<DownloadedVideo>, Int) -> Unit,
    onDownloadedMusicClick: (List<DownloadedTrack>, Int) -> Unit,
) {
    val downloadedVideos by viewModel.fullDownloadedVideos.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.fullDownloadedTracks.collectAsStateWithLifecycle()
    val isFocusMode by viewModel.isFocusModeEnabled.collectAsStateWithLifecycle()
    val hasVideos = !downloadedVideos.isNullOrEmpty()
    val hasTracks = !downloadedTracks.isNullOrEmpty()

    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedVideoIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedTrackIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    val exitSelection = {
        selectionMode = false
        selectedVideoIds = emptySet()
        selectedTrackIds = emptySet()
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    if (!hasVideos && !hasTracks) {
        FlowEmptyState(
            title = stringResource(R.string.downloads_title),
            subtitle = stringResource(R.string.library_empty_body),
            icon = Icons.Outlined.Download,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = if (isFocusMode) 4.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 1. Downloaded Music Section: placed on top, side-by-side horizontally
                if (hasTracks) {
                    if (!isFocusMode) {
                        item(key = "header_music") {
                            FlowSectionHeader(
                                text = stringResource(R.string.screen_title_music),
                            )
                        }
                    }
                    item(key = "row_music") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                        ) {
                            items(downloadedTracks.orEmpty(), key = { it.track.videoId }) { track ->
                                val isSelected = track.track.videoId in selectedTrackIds
                                DownloadedMusicCard(
                                    track = track,
                                    selected = isSelected,
                                    selectionMode = selectionMode,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedTrackIds =
                                                if (isSelected) selectedTrackIds - track.track.videoId
                                                else selectedTrackIds + track.track.videoId
                                        } else {
                                            val idx = downloadedTracks.orEmpty().indexOf(track)
                                            onDownloadedMusicClick(downloadedTracks.orEmpty(), if (idx >= 0) idx else 0)
                                        }
                                    },
                                    onLongClick = {
                                        selectionMode = true
                                        selectedTrackIds = selectedTrackIds + track.track.videoId
                                    },
                                )
                            }
                        }
                    }
                }

                // 2. Downloaded Videos Section: placed below music
                if (hasVideos) {
                    if (!isFocusMode) {
                        item(key = "header_videos") {
                            FlowSectionHeader(
                                text = stringResource(R.string.tab_videos),
                            )
                        }
                    }
                    itemsIndexed(downloadedVideos.orEmpty(), key = { _, v -> "v_${v.video.id}" }) { index, video ->
                        val isSelected = video.video.id in selectedVideoIds
                        VideoDownloadItem(
                            video = video,
                            asCard = false,
                            selectionMode = selectionMode,
                            selected = isSelected,
                            onClick = {
                                if (selectionMode) {
                                    selectedVideoIds =
                                        if (isSelected) selectedVideoIds - video.video.id
                                        else selectedVideoIds + video.video.id
                                } else {
                                    onDownloadedVideoClick(downloadedVideos.orEmpty(), index)
                                }
                            },
                            onLongClick = {
                                selectionMode = true
                                selectedVideoIds = selectedVideoIds + video.video.id
                            },
                            onDeleteClick = { viewModel.deleteDownloadedVideo(video.video.id) },
                        )
                    }
                }
            }

            val totalSelected = selectedVideoIds.size + selectedTrackIds.size
            FlowSelectionToolbar(
                visible = selectionMode && totalSelected > 0,
                summary = pluralStringResource(R.plurals.selected_count_template, totalSelected, totalSelected),
                actions = listOf(
                    FlowSelectionAction(Icons.Outlined.Delete, stringResource(R.string.action_delete)) {
                        selectedVideoIds.forEach { viewModel.deleteDownloadedVideo(it) }
                        selectedTrackIds.forEach { viewModel.deleteDownloadedMusic(it) }
                        exitSelection()
                    }
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun WatchLaterFeed(
    viewModel: LibraryViewModel,
    onVideoClick: (Video) -> Unit,
) {
    val watchLaterVideos by viewModel.fullWatchLaterVideos.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedVideoIds by rememberSaveable { mutableStateOf(setOf<String>()) }

    val exitSelection = {
        selectionMode = false
        selectedVideoIds = emptySet()
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    if (watchLaterVideos.isNullOrEmpty()) {
        FlowEmptyState(
            title = stringResource(R.string.watch_later),
            subtitle = stringResource(R.string.no_videos_saved_body),
            icon = Icons.Outlined.WatchLater,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(watchLaterVideos.orEmpty(), key = { it.id }) { video ->
                    val isSelected = video.id in selectedVideoIds
                    PlaylistVideoRow(
                        video = video,
                        isSelected = isSelected,
                        inSelectionMode = selectionMode,
                        canModify = false,
                        reorderModifier = Modifier,
                        dragHandleModifier = Modifier,
                        showDragHandle = false,
                        showAddedDate = false,
                        isWatchLater = true,
                        onRemove = { viewModel.removeFromWatchLater(video.id) },
                        onClick = {
                            if (selectionMode) {
                                selectedVideoIds =
                                    if (isSelected) selectedVideoIds - video.id
                                    else selectedVideoIds + video.id
                            } else {
                                onVideoClick(video)
                            }
                        },
                        onLongClick = {
                            selectionMode = true
                            selectedVideoIds = selectedVideoIds + video.id
                        },
                    )
                }
            }

            FlowSelectionToolbar(
                visible = selectionMode && selectedVideoIds.isNotEmpty(),
                summary = pluralStringResource(R.plurals.selected_count_template, selectedVideoIds.size, selectedVideoIds.size),
                actions = listOf(
                    FlowSelectionAction(Icons.Outlined.Delete, stringResource(R.string.action_delete)) {
                        selectedVideoIds.forEach { viewModel.removeFromWatchLater(it) }
                        exitSelection()
                    }
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun PlaylistsFeed(
    viewModel: LibraryViewModel,
    onPlaylistClick: (String) -> Unit,
    onMusicPlaylistClick: (String) -> Unit,
) {
    val playlists by viewModel.combinedPlaylists.collectAsStateWithLifecycle()
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylistIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var creationTarget by remember { mutableStateOf<PlaylistCreationTarget?>(null) }

    val exitSelection = {
        selectionMode = false
        selectedPlaylistIds = emptySet()
    }

    BackHandler(enabled = selectionMode) { exitSelection() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Playlist creation system below playlist category tabs
            item(key = "create_playlists_row") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FlowGlassButton(
                        icon = Icons.Outlined.VideoLibrary,
                        label = stringResource(R.string.create_new_playlist),
                        onClick = { creationTarget = PlaylistCreationTarget.Video },
                        modifier = Modifier.weight(1f),
                    )
                    FlowGlassButton(
                        icon = Icons.Outlined.MusicNote,
                        label = stringResource(R.string.new_playlist_button),
                        onClick = { creationTarget = PlaylistCreationTarget.Music },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (playlists.isNullOrEmpty()) {
                item(key = "empty_playlists") {
                    FlowEmptyState(
                        title = stringResource(R.string.no_playlists_found),
                        icon = Icons.Outlined.PlaylistPlay,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    )
                }
            } else {
                items(playlists.orEmpty(), key = { it.first.id }) { (playlist, isMusic) ->
                    val isSelected = playlist.id in selectedPlaylistIds
                    PlaylistCard(
                        playlist = playlist,
                        isMusic = isMusic,
                        selected = isSelected,
                        selectionMode = selectionMode,
                        onClick = {
                            if (selectionMode) {
                                selectedPlaylistIds =
                                    if (isSelected) selectedPlaylistIds - playlist.id
                                    else selectedPlaylistIds + playlist.id
                            } else {
                                if (isMusic) {
                                    onMusicPlaylistClick(playlist.id)
                                } else {
                                    onPlaylistClick(playlist.id)
                                }
                            }
                        },
                        onLongClick = {
                            selectionMode = true
                            selectedPlaylistIds = selectedPlaylistIds + playlist.id
                        },
                        layout = PlaylistCardLayout.LIST,
                    )
                }
            }
        }

        FlowSelectionToolbar(
            visible = selectionMode && selectedPlaylistIds.isNotEmpty(),
            summary = pluralStringResource(R.plurals.selected_count_template, selectedPlaylistIds.size, selectedPlaylistIds.size),
            actions = listOf(
                FlowSelectionAction(Icons.Outlined.Delete, stringResource(R.string.action_delete)) {
                    selectedPlaylistIds.forEach { viewModel.deletePlaylist(it) }
                    exitSelection()
                }
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    creationTarget?.let { target ->
        val isVideo = target == PlaylistCreationTarget.Video
        CollectionEditDialog(
            title = stringResource(if (isVideo) R.string.create_new_playlist else R.string.new_playlist_button),
            confirmLabel = stringResource(R.string.create),
            icon = if (isVideo) Icons.Outlined.VideoLibrary else Icons.Outlined.MusicNote,
            onDismiss = { creationTarget = null },
            onConfirm = { name, description ->
                if (isVideo) {
                    viewModel.createVideoPlaylist(name, description)
                } else {
                    viewModel.createMusicPlaylist(name, description)
                }
                creationTarget = null
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadedMusicCard(
    track: DownloadedTrack,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkSize = 120.dp
    Column(
        modifier =
            modifier
                .width(artworkSize)
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Surface(
            modifier = Modifier.size(artworkSize),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
            ),
        ) {
            Box {
                AsyncImage(
                    model = track.track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                if (selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    )
                }

                if (selectionMode) {
                    CircularSelectBadge(
                        selected = selected,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.track.isExplicit == true) {
                    ExplicitBadge(modifier = Modifier.padding(end = 4.dp))
                }
                Text(
                    text = track.track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Modern iOS liquid glass-style action button with subtle translucent backdrop and outline border.
 */
@Composable
private fun FlowGlassButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier.height(42.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val Note.key: String get() = kind.idFor(targetId)
