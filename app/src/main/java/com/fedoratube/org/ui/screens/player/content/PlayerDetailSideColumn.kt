package com.fedoratube.org.ui.screens.player.content

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.data.local.PlayerRelatedCardStyle
import com.fedoratube.org.data.model.Comment
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.player.EnhancedPlayerState
import com.fedoratube.org.ui.components.videoplayer.sheet.LiveChatPreview
import com.fedoratube.org.ui.screens.player.VideoPlayerViewModel
import com.fedoratube.org.ui.screens.player.dialogs.PlayerChaptersSheetHost
import com.fedoratube.org.ui.screens.player.dialogs.PlayerCommentsPanelHost
import com.fedoratube.org.ui.screens.player.dialogs.PlayerDescriptionSheetHost
import com.fedoratube.org.ui.screens.player.dialogs.PlayerLiveChatColumn
import com.fedoratube.org.ui.screens.player.dialogs.PlayerQueueSheetHost
import com.fedoratube.org.ui.screens.player.dialogs.PlayerSettingsSheetHost
import com.fedoratube.org.ui.screens.player.dialogs.PlayerSleepTimerSheetHost
import com.fedoratube.org.ui.screens.player.dialogs.PlayerTranscriptSheetHost
import com.fedoratube.org.ui.screens.player.state.PlayerScreenState
import com.fedoratube.org.ui.screens.player.state.PlayerSheet
import com.fedoratube.org.ui.screens.player.state.VideoPlayerPreferencesState
import com.fedoratube.org.ui.screens.player.state.VideoPlayerUiState
import com.fedoratube.org.ui.screens.player.state.rememberPlayerCommentsUiState
import com.fedoratube.org.ui.screens.player.state.transcriptTrackUrl
import kotlinx.coroutines.launch

private val QueueCardHorizontalPadding = 12.dp
private val QueueCardVerticalPadding = 8.dp

/**
 * Supporting pane of the wide player layout. Comments, the description, the chapters, the queue, the
 * settings sheet with every page it owns and the sleep timer take the pane over when opened, so the video
 * stays visible instead of being covered by a bottom sheet (#918); otherwise it shows live chat,
 * falling back to the related-videos list.
 */
@Composable
internal fun PlayerDetailSideColumn(
    video: Video,
    uiState: VideoPlayerUiState,
    playerState: EnhancedPlayerState,
    viewModel: VideoPlayerViewModel,
    screenState: PlayerScreenState,
    prefs: VideoPlayerPreferencesState,
    commentsEnabled: Boolean,
    showRelatedVideos: Boolean,
    relatedCardStyle: PlayerRelatedCardStyle,
    onVideoClick: (Video) -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    queueCard: (@Composable () -> Unit)? = null,
) {
    val commentsUiState = rememberPlayerCommentsUiState(viewModel)
    val scope = rememberCoroutineScope()
    val playerPreferences = prefs.preferences
    val closeSheet = { screenState.closeSheet() }

    BoxWithConstraints(modifier) {
        val paneHeight = maxHeight
        val paneModifier = Modifier.fillMaxSize()
        when {
            screenState.activeSheet == PlayerSheet.Comments() && commentsEnabled -> {
                BackHandler(onBack = closeSheet)
                PlayerCommentsPanelHost(
                    videoId = video.id,
                    screenState = screenState,
                    viewModel = viewModel,
                    commentsUiState = commentsUiState,
                    artworkUrl = video.thumbnailUrl,
                    onNavigateToChannel = onChannelClick,
                    onClose = closeSheet,
                    modifier = paneModifier,
                )
            }

            screenState.isSettingsOpen -> {
                BackHandler(onBack = closeSheet)
                PlayerSettingsSheetHost(
                    screenState = screenState,
                    playerState = playerState,
                    uiState = uiState,
                    viewModel = viewModel,
                    playerPreferences = playerPreferences,
                    scope = scope,
                    rememberPlaybackSpeed = prefs.rememberPlaybackSpeed,
                    ambientModeEnabled = prefs.ambientModeEnabled,
                    groupedQualitySelectorEnabled = prefs.groupedQualitySelectorEnabled,
                    rememberSubtitleLanguage = { language ->
                        scope.launch { playerPreferences.setPreferredSubtitleLanguage(language) }
                    },
                    asSidePanel = true,
                    expandedHeight = paneHeight,
                    onDismiss = closeSheet,
                )
            }

            screenState.activeSheet == PlayerSheet.Description -> {
                BackHandler(onBack = closeSheet)
                PlayerDescriptionSheetHost(
                    video = video,
                    uiState = uiState,
                    viewModel = viewModel,
                    asSidePanel = true,
                    expandedHeight = paneHeight,
                    onDismiss = closeSheet,
                    hasTranscriptTrack = transcriptTrackUrl(playerState, screenState) != null,
                    onChaptersClick = { screenState.open(PlayerSheet.Chapters) },
                    onTranscriptClick = { screenState.open(PlayerSheet.Transcript) },
                    onChannelClick = onChannelClick,
                )
            }

            screenState.activeSheet == PlayerSheet.Transcript -> {
                BackHandler(onBack = closeSheet)
                PlayerTranscriptSheetHost(
                    viewModel = viewModel,
                    screenState = screenState,
                    trackUrl = transcriptTrackUrl(playerState, screenState),
                    tracks = playerState.availableSubtitles,
                    chapters = uiState.chapters,
                    artworkUrl = video.thumbnailUrl,
                    asSidePanel = true,
                    expandedHeight = paneHeight,
                    onDismiss = closeSheet,
                )
            }

            screenState.activeSheet == PlayerSheet.Chapters -> {
                BackHandler(onBack = closeSheet)
                PlayerChaptersSheetHost(
                    screenState = screenState,
                    chapters = uiState.chapters,
                    thumbnailUrl = video.thumbnailUrl,
                    asSidePanel = true,
                    expandedHeight = paneHeight,
                    onDismiss = closeSheet,
                )
            }

            screenState.activeSheet == PlayerSheet.Queue -> {
                BackHandler(onBack = closeSheet)
                PlayerQueueSheetHost(
                    asSidePanel = true,
                    expandedHeight = paneHeight,
                    onDismiss = closeSheet,
                )
            }

            screenState.activeSheet == PlayerSheet.SleepTimer -> {
                BackHandler(onBack = closeSheet)
                PlayerSleepTimerSheetHost(
                    asSidePanel = true,
                    expandedHeight = paneHeight,
                    onDismiss = closeSheet,
                )
            }

            else -> {
                Column(paneModifier) {
                    if (queueCard != null) {
                        Box(Modifier.padding(horizontal = QueueCardHorizontalPadding, vertical = QueueCardVerticalPadding)) {
                            queueCard()
                        }
                    }
                    BoxWithConstraints(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        val restingHeight = maxHeight
                        when {
                            uiState.isLiveChatAvailable && screenState.showLiveChatPanel -> {
                                PlayerLiveChatColumn(
                                    messages = uiState.liveChatMessages,
                                    isLoading = uiState.isLiveChatLoading,
                                    onClose = { screenState.showLiveChatPanel = false },
                                    modifier = paneModifier,
                                )
                            }

                            // With related videos switched off the resting pane had nothing in it, leaving a
                            // column of empty space beside the video (#1022). It keeps its place and shows the next
                            // most useful thing instead: the comments, or the description when those are off too.
                            !showRelatedVideos && commentsEnabled -> {
                                PlayerCommentsPanelHost(
                                    videoId = video.id,
                                    screenState = screenState,
                                    viewModel = viewModel,
                                    commentsUiState = commentsUiState,
                                    artworkUrl = video.thumbnailUrl,
                                    onNavigateToChannel = onChannelClick,
                                    onClose = closeSheet,
                                    modifier = paneModifier,
                                )
                            }

                            !showRelatedVideos -> {
                                PlayerDescriptionSheetHost(
                                    video = video,
                                    uiState = uiState,
                                    viewModel = viewModel,
                                    asSidePanel = true,
                                    expandedHeight = restingHeight,
                                    onDismiss = closeSheet,
                                    hasTranscriptTrack = transcriptTrackUrl(playerState, screenState) != null,
                                    onChaptersClick = { screenState.open(PlayerSheet.Chapters) },
                                    onTranscriptClick = { screenState.open(PlayerSheet.Transcript) },
                                    onChannelClick = onChannelClick,
                                )
                            }

                            else -> {
                                LazyColumn(
                                    modifier = paneModifier,
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                ) {
                                    if (uiState.isLiveChatAvailable) {
                                        item {
                                            LiveChatPreview(onClick = { screenState.showLiveChatPanel = true })
                                        }
                                    }
                                    relatedVideosContent(
                                        relatedVideos = uiState.relatedVideos,
                                        onVideoClick = onVideoClick,
                                        cardStyle = relatedCardStyle,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
