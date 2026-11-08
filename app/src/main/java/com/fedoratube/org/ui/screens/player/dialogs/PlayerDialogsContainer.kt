package com.fedoratube.org.ui.screens.player.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.player.state.EnhancedPlayerState
import com.fedoratube.org.ui.components.shared.MediaDownloadDialog
import com.fedoratube.org.ui.components.shared.MediaDownloadDialogCompact
import com.fedoratube.org.ui.screens.player.VideoPlayerViewModel
import com.fedoratube.org.ui.screens.player.state.PlayerScreenState
import com.fedoratube.org.ui.screens.player.state.PlayerSheet
import com.fedoratube.org.ui.screens.player.state.VideoPlayerPreferencesState
import com.fedoratube.org.ui.screens.player.state.VideoPlayerUiState
import kotlinx.coroutines.launch

@Composable
internal fun PlayerDialogsContainer(
    screenState: PlayerScreenState,
    playerState: EnhancedPlayerState,
    uiState: VideoPlayerUiState,
    video: Video,
    viewModel: VideoPlayerViewModel,
    prefs: VideoPlayerPreferencesState,
    hostedInSidePanel: Boolean = false,
    mediaSheetExpandedHeight: Dp? = null,
    mediaSheetCollapsedHeight: Dp = 0.dp,
    onMediaSheetProgressChange: (Float) -> Unit = {},
) {
    val playerPreferences = prefs.preferences
    val coroutineScope = rememberCoroutineScope()

    // Download Quality Dialog
    if (screenState.activeSheet == PlayerSheet.Download) {
        when (prefs.downloadDialogStyle) {
            com.fedoratube.org.data.local.DownloadDialogStyle.COMPACT -> {
                MediaDownloadDialogCompact(
                    streamSizes = uiState.streamSizes,
                    innerTubeVideoFormats = uiState.innerTubeVideoFormats,
                    innerTubeAudioFormats = uiState.innerTubeAudioFormats,
                    video = video,
                    currentPlayingHeight = playerState.effectiveQuality,
                    subtitles = playerState.availableSubtitles,
                    onDismiss = { screenState.closeSheet() },
                )
            }

            com.fedoratube.org.data.local.DownloadDialogStyle.FULL -> {
                MediaDownloadDialog(
                    streamSizes = uiState.streamSizes,
                    innerTubeVideoFormats = uiState.innerTubeVideoFormats,
                    innerTubeAudioFormats = uiState.innerTubeAudioFormats,
                    video = video,
                    subtitles = playerState.availableSubtitles,
                    onDismiss = { screenState.closeSheet() },
                )
            }

            null -> { }
        }
    }

    if (screenState.isSettingsOpen && !hostedInSidePanel) {
        PlayerSettingsSheetHost(
            screenState = screenState,
            playerState = playerState,
            uiState = uiState,
            viewModel = viewModel,
            playerPreferences = playerPreferences,
            scope = coroutineScope,
            rememberPlaybackSpeed = prefs.rememberPlaybackSpeed,
            ambientModeEnabled = prefs.ambientModeEnabled,
            groupedQualitySelectorEnabled = prefs.groupedQualitySelectorEnabled,
            rememberSubtitleLanguage = { language ->
                coroutineScope.launch { playerPreferences.setPreferredSubtitleLanguage(language) }
            },
            asSidePanel = false,
            expandedHeight = mediaSheetExpandedHeight,
            collapsedHeight = mediaSheetCollapsedHeight,
            pipAspectRatio = null,
            onSheetProgressChange = onMediaSheetProgressChange,
            onDismiss = { screenState.closeSheet() },
        )
    }
}
