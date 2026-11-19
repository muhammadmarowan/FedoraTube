package com.fedoratube.org.ui.screens.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.WatchLater
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.fedoratube.org.R
import com.fedoratube.org.data.model.PlaylistInfo
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.music.DownloadedTrack
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.notes.Note
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
import com.fedoratube.org.ui.components.library.PlaylistVideoRow
import com.fedoratube.org.ui.components.library.VideoDownloadItem
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowSectionHeader
import com.fedoratube.org.ui.components.shared.MediaKind
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
    val pagerState = rememberPagerState(initialPage = 0) { 4 }
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

                // 2. Sliding Category Tabs Row (Playlists, Watch Later, Downloads, Notes)
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
                        0 -> PlaylistsFeed(
                            viewModel = viewModel,
                            onPlaylistClick = { id -> onPlaylistClick(id) },
                            onMusicPlaylistClick = { id -> onMusicPlaylistClick(id) },
                        )
                        1 -> WatchLaterFeed(
                            viewModel = viewModel,
                            onVideoClick = onVideoClick,
                        )
                        2 -> DownloadsFeed(
                            viewModel = viewModel,
                            onDownloadedVideoClick = onDownloadedVideoClick,
                            onDownloadedMusicClick = onDownloadedMusicClick,
                        )
                        3 -> NotesFeed(
                            viewModel = viewModel,
                            onNavigateToNotes = onNavigateToNotes,
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
 * YouTube-style sliding category tabs row: Playlists, Watch Later, Downloads, Notes
 * with animated underline indicator.
 */
@Composable
private fun LibraryCategoryTabsRow(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        stringResource(R.string.library_playlists_label),
        stringResource(R.string.watch_later),
        stringResource(R.string.downloads_title),
        stringResource(R.string.notes_title),
    )
    val tabCenters = remember { mutableStateMapOf<Int, Float>() }
    var rowRootX by remember { mutableFloatStateOf(0f) }

    val currentCenterPx = remember(pagerState.currentPage, pagerState.currentPageOffsetFraction) {
        val page = pagerState.currentPage
        val fraction = pagerState.currentPageOffsetFraction
        val c0 = tabCenters[page] ?: 0f
        val next = (if (fraction > 0f) page + 1 else page - 1).coerceIn(0, tabs.size - 1)
        val c1 = tabCenters[next] ?: c0
        c0 + (c1 - c0) * kotlin.math.abs(fraction)
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TabRowHeight)
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { coordinates ->
                    rowRootX = coordinates.positionInRoot().x
                },
    ) {
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
                            }
                            .onGloballyPositioned { coordinates ->
                                val tabRoot = coordinates.positionInRoot()
                                val tabCenter = (tabRoot.x - rowRootX) + coordinates.size.width / 2f
                                tabCenters[index] = tabCenter
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

        if (currentCenterPx > 0f) {
            val indWidthPx = with(LocalDensity.current) { IndicatorWidth.toPx() }
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset {
                            IntOffset((currentCenterPx - indWidthPx / 2f).roundToInt(), 0)
                        }
                        .width(IndicatorWidth)
                        .height(IndicatorHeight)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(MaterialTheme.colorScheme.primary),
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
    val playlists by viewModel.fullVideoPlaylists.collectAsStateWithLifecycle()
    if (playlists.isNullOrEmpty()) {
        FlowEmptyState(
            title = stringResource(R.string.no_playlists_found),
            icon = Icons.Outlined.PlaylistPlay,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(playlists.orEmpty(), key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    layout = PlaylistCardLayout.LIST,
                )
            }
        }
    }
}

@Composable
private fun WatchLaterFeed(
    viewModel: LibraryViewModel,
    onVideoClick: (Video) -> Unit,
) {
    val watchLaterVideos by viewModel.fullWatchLaterVideos.collectAsStateWithLifecycle()
    if (watchLaterVideos.isNullOrEmpty()) {
        FlowEmptyState(
            title = stringResource(R.string.watch_later),
            subtitle = stringResource(R.string.no_videos_saved_body),
            icon = Icons.Outlined.WatchLater,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(watchLaterVideos.orEmpty(), key = { it.id }) { video ->
                PlaylistVideoRow(
                    video = video,
                    isSelected = false,
                    inSelectionMode = false,
                    canModify = false,
                    reorderModifier = Modifier,
                    dragHandleModifier = Modifier,
                    showDragHandle = false,
                    showAddedDate = false,
                    isWatchLater = true,
                    onRemove = { },
                    onClick = { onVideoClick(video) },
                )
            }
        }
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
    val hasVideos = !downloadedVideos.isNullOrEmpty()
    val hasTracks = !downloadedTracks.isNullOrEmpty()

    if (!hasVideos && !hasTracks) {
        FlowEmptyState(
            title = stringResource(R.string.downloads_title),
            subtitle = stringResource(R.string.library_empty_body),
            icon = Icons.Outlined.Download,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasVideos) {
                item {
                    FlowSectionHeader(
                        text = stringResource(R.string.tab_videos),
                    )
                }
                itemsIndexed(downloadedVideos.orEmpty(), key = { _, v -> "v_${v.video.id}" }) { index, video ->
                    VideoDownloadItem(
                        video = video,
                        asCard = false,
                        selectionMode = false,
                        selected = false,
                        onClick = { onDownloadedVideoClick(downloadedVideos.orEmpty(), index) },
                        onDeleteClick = { },
                    )
                }
            }
            if (hasTracks) {
                item {
                    FlowSectionHeader(
                        text = stringResource(R.string.screen_title_music),
                    )
                }
                itemsIndexed(downloadedTracks.orEmpty(), key = { _, t -> "t_${t.track.videoId}" }) { index, track ->
                    MusicDownloadRow(
                        downloadedTrack = track,
                        selectionMode = false,
                        selected = false,
                        onClick = { onDownloadedMusicClick(downloadedTracks.orEmpty(), index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesFeed(
    viewModel: LibraryViewModel,
    onNavigateToNotes: () -> Unit,
) {
    val notes by viewModel.fullNotes.collectAsStateWithLifecycle()
    if (notes.isNullOrEmpty()) {
        FlowEmptyState(
            title = stringResource(R.string.notes_empty_title),
            subtitle = stringResource(R.string.notes_empty_body),
            icon = Icons.Outlined.StickyNote2,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(notes.orEmpty(), key = { "${it.targetId}_${it.kind}" }) { note ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToNotes() },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        note.subject?.title?.let { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
