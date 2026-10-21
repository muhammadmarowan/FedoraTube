package com.fedoratube.org.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.music.DownloadedTrack
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.stats.RecapPeriod
import com.fedoratube.org.data.video.DownloadedVideo
import com.fedoratube.org.ui.OnTabReselected
import com.fedoratube.org.ui.components.layout.flowBottomContentPadding
import com.fedoratube.org.ui.components.layout.navigation.FlowTab
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import com.fedoratube.org.ui.components.library.LibraryNavigationRow
import com.fedoratube.org.ui.components.library.LibrarySection
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowMaxContentWidth
import com.fedoratube.org.ui.components.shared.MediaKind
import com.fedoratube.org.ui.components.stats.RecapEntryCard
import java.time.format.TextStyle

private val ListVerticalPadding = 12.dp
private val ShelfSpacing = 24.dp
private val RecapCardPadding = 16.dp

@Composable
fun LibraryScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToPlaylists: (MediaKind?) -> Unit,
    onNavigateToLikedVideos: () -> Unit,
    onNavigateToLikedMusic: () -> Unit,
    onNavigateToWatchLater: () -> Unit,
    onNavigateToSavedShorts: () -> Unit = {},
    onNavigateToDownloads: () -> Unit,
    onNavigateToLocalMedia: () -> Unit = {},
    onNavigateToNotes: () -> Unit,
    onManageData: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onOpenRecap: (RecapPeriod?) -> Unit,
    onVideoClick: (Video) -> Unit,
    onMusicClick: (MusicTrack, List<MusicTrack>, String) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    onMusicPlaylistClick: (String) -> Unit = {},
    onDownloadedVideoClick: (List<DownloadedVideo>, Int) -> Unit = { _, _ -> },
    onDownloadedMusicClick: (List<DownloadedTrack>, Int) -> Unit = { _, _ -> },
    onSavedShortClick: (Video) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val recapReady by viewModel.recapReady.collectAsStateWithLifecycle()
    val notesCount by viewModel.notesCount.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0]
    val listState = rememberLazyListState()
    OnTabReselected(FlowTab.Library.route) { listState.animateScrollToItem(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.library),
                globalActionsShowSettings = true,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(top = ListVerticalPadding, bottom = flowBottomContentPadding(ListVerticalPadding)),
            verticalArrangement = Arrangement.spacedBy(ShelfSpacing),
        ) {
            item(key = LibrarySection.HISTORY.name, contentType = "media-shelf") {
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

            item(key = "sections", contentType = "navigation-section") {
                val counts by viewModel.counts.collectAsStateWithLifecycle()
                val notes = notesCount
                Column(modifier = Modifier.widthIn(max = FlowMaxContentWidth).padding(horizontal = 16.dp)) {
                    LibraryNavigationRow(
                        icon = LibrarySection.PLAYLISTS.icon,
                        title = LibrarySection.PLAYLISTS.title,
                        subtitle = LibrarySection.PLAYLISTS.subtitle(counts),
                        onClick = { onNavigateToPlaylists(null) },
                    )
                    LibraryNavigationRow(
                        icon = LibrarySection.WATCH_LATER.icon,
                        title = LibrarySection.WATCH_LATER.title,
                        subtitle = LibrarySection.WATCH_LATER.subtitle(counts),
                        onClick = onNavigateToWatchLater,
                    )
                    LibraryNavigationRow(
                        icon = LibrarySection.DOWNLOADS.icon,
                        title = LibrarySection.DOWNLOADS.title,
                        subtitle = LibrarySection.DOWNLOADS.subtitle(counts),
                        onClick = onNavigateToDownloads,
                    )
                    if (notes != null && notes > 0) {
                        LibraryNavigationRow(
                            icon = LibrarySection.NOTES.icon,
                            title = LibrarySection.NOTES.title,
                            subtitle = pluralStringResource(R.plurals.notes_count, notes, notes),
                            onClick = onNavigateToNotes,
                        )
                    } else {
                        LibraryNavigationRow(
                            icon = LibrarySection.NOTES.icon,
                            title = LibrarySection.NOTES.title,
                            subtitle = null,
                            onClick = onNavigateToNotes,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
