package com.fedoratube.org.ui.components.videoplayer.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.player.EnhancedPlayerState
import com.fedoratube.org.player.QualityOption
import com.fedoratube.org.ui.components.equalizer.EqQuickPanel
import com.fedoratube.org.ui.components.layout.navigation.LocalMediaNavigator
import com.fedoratube.org.ui.components.shared.FlowCenteredPopupDialog
import com.fedoratube.org.ui.components.shared.FlowSheetHeader
import com.fedoratube.org.ui.components.videoplayer.subtitle.SubtitleCustomizer
import com.fedoratube.org.ui.components.videoplayer.subtitle.SubtitleStyle

@Composable
fun SettingsMenuDialog(
    playerState: EnhancedPlayerState,
    autoplayEnabled: Boolean,
    subtitlesEnabled: Boolean,
    onDismiss: () -> Unit,
    initialPage: PlayerSettingsPage = PlayerSettingsPage.Main,
    onQualitySelected: (QualityOption) -> Unit = {},
    onAudioTrackSelected: (Int) -> Unit = {},
    onSpeedSelected: (Float) -> Unit = {},
    selectedSubtitleUrl: String? = null,
    onSubtitleSelected: (Int) -> Unit = {},
    onDisableSubtitles: () -> Unit = {},
    onAddSubtitleFile: (() -> Unit)? = null,
    onSubtitleOffsetChange: ((Long) -> Unit)? = null,
    onAutoplayToggle: (Boolean) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit,
    onStableVolumeToggle: (Boolean) -> Unit,
    subtitleStyle: SubtitleStyle,
    onSubtitleStyleChange: (SubtitleStyle) -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    ambientModeEnabled: Boolean = false,
    onAmbientModeToggle: (Boolean) -> Unit = {},
    onLockScreen: () -> Unit = {},
    onPipClick: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    sponsorBlockSegmentCount: Int = 0,
    sponsorBlockOffForVideo: Boolean = false,
    onSponsorBlockToggle: (off: Boolean) -> Unit = {},
    notePositionMs: Long? = null,
    onAddNote: () -> Unit = {},
    expandedHeight: Dp? = null,
    collapsedHeight: Dp = 0.dp,
    enableVerticalDismiss: Boolean = true,
    useGroupedQualitySelector: Boolean = false,
    onSheetProgressChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentPage by remember { mutableStateOf(initialPage) }
    var subtitleStyleReturnPage by remember { mutableStateOf(PlayerSettingsPage.Subtitles) }
    // Which page the current subpage was opened from, so back from e.g. Quality returns to the
    // page that led there (front page or "More") rather than always the front page.
    var subpageReturnPage by remember { mutableStateOf(PlayerSettingsPage.Main) }
    val currentTitle =
        when (currentPage) {
            PlayerSettingsPage.Main -> stringResource(R.string.player_settings)
            PlayerSettingsPage.More -> stringResource(R.string.more_header)
            PlayerSettingsPage.Quality -> stringResource(R.string.video_quality_title)
            PlayerSettingsPage.Speed -> stringResource(R.string.playback_speed)
            PlayerSettingsPage.Audio -> stringResource(R.string.audio_track)
            PlayerSettingsPage.Subtitles -> stringResource(R.string.filter_subtitles)
            PlayerSettingsPage.SubtitleStyle -> stringResource(R.string.subtitle_style)
            PlayerSettingsPage.Equalizer -> stringResource(R.string.equalizer)
        }
    val backPage =
        when (currentPage) {
            PlayerSettingsPage.Main -> null
            PlayerSettingsPage.SubtitleStyle -> subtitleStyleReturnPage
            PlayerSettingsPage.More -> PlayerSettingsPage.Main
            else -> subpageReturnPage
        }
    // A panel host swaps the panel's content in place, so a row that opens another surface must not
    // run the exit animation there: the host's onDismiss would close the panel under the new surface.
    val leaveFor: (() -> Unit) -> Unit = { after ->
        onDismiss()
        after()
    }

    LaunchedEffect(initialPage) {
        currentPage = initialPage
    }

    val settingsContent: @Composable () -> Unit = {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = SheetContentVerticalPadding),
        ) {
            when (currentPage) {
                PlayerSettingsPage.Main -> {
                    PlayerSettingsMainPage(
                        playerState = playerState,
                        subtitlesEnabled = subtitlesEnabled,
                        onNavigateToPage = { page ->
                            subpageReturnPage = PlayerSettingsPage.Main
                            currentPage = page
                        },
                        onLockScreen = { leaveFor(onLockScreen) },
                    )
                }

                PlayerSettingsPage.More -> {
                    PlayerSettingsMorePage(
                        playerState = playerState,
                        autoplayEnabled = autoplayEnabled,
                        ambientModeEnabled = ambientModeEnabled,
                        onNavigateToPage = { page ->
                            subpageReturnPage = PlayerSettingsPage.More
                            currentPage = page
                        },
                        onPipClick = {
                            onDismiss()
                            onPipClick()
                        },
                        onSleepTimerClick = { leaveFor(onSleepTimerClick) },
                        onLoopToggle = onLoopToggle,
                        onAutoplayToggle = onAutoplayToggle,
                        onSkipSilenceToggle = onSkipSilenceToggle,
                        onStableVolumeToggle = onStableVolumeToggle,
                        onAmbientModeToggle = onAmbientModeToggle,
                        sponsorBlockSegmentCount = sponsorBlockSegmentCount,
                        sponsorBlockOffForVideo = sponsorBlockOffForVideo,
                        onSponsorBlockToggle = onSponsorBlockToggle,
                        notePositionMs = notePositionMs,
                        onAddNote = onAddNote,
                    )
                }

                PlayerSettingsPage.Quality -> {
                    PlayerSettingsQualityPage(
                        availableQualities = playerState.availableQualities,
                        currentQuality = playerState.currentQuality,
                        currentQualityKey = playerState.currentQualityKey,
                        useGroupedQualitySelector = useGroupedQualitySelector,
                        onQualitySelected = {
                            onQualitySelected(it)
                            onDismiss()
                        },
                    )
                }

                PlayerSettingsPage.Speed -> {
                    PlayerSettingsSpeedPage(
                        currentSpeed = playerState.playbackSpeed,
                        onSpeedSelected = onSpeedSelected,
                        onSpeedSelectionFinished = { onDismiss() },
                    )
                }

                PlayerSettingsPage.Audio -> {
                    PlayerSettingsAudioPage(
                        availableAudioTracks = playerState.availableAudioTracks,
                        currentAudioTrack = playerState.currentAudioTrack,
                        onTrackSelected = {
                            onAudioTrackSelected(it)
                            onDismiss()
                        },
                    )
                }

                PlayerSettingsPage.Equalizer -> {
                    val navigator = LocalMediaNavigator.current
                    EqQuickPanel(
                        onEdit = {
                            onDismiss()
                            navigator.openEqualizer()
                        },
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                }

                PlayerSettingsPage.Subtitles -> {
                    PlayerSettingsSubtitlesPage(
                        availableSubtitles = playerState.availableSubtitles,
                        selectedSubtitleUrl = selectedSubtitleUrl,
                        subtitlesEnabled = subtitlesEnabled,
                        onSubtitleSelected = { index ->
                            onSubtitleSelected(index)
                            onDismiss()
                        },
                        onDisableSubtitles = {
                            onDisableSubtitles()
                            onDismiss()
                        },
                        // Stays open: the picker's result comes back to this composition.
                        onAddSubtitleFile = onAddSubtitleFile,
                        subtitleOffsetMs = playerState.subtitleOffsetMs,
                        onSubtitleOffsetChange = onSubtitleOffsetChange,
                        onShowStyleCustomizer = {
                            subtitleStyleReturnPage = PlayerSettingsPage.Subtitles
                            currentPage = PlayerSettingsPage.SubtitleStyle
                        },
                    )
                }

                PlayerSettingsPage.SubtitleStyle -> {
                    SubtitleCustomizer(
                        currentStyle = subtitleStyle,
                        onStyleChange = onSubtitleStyleChange,
                    )
                }
            }
        }
    }

    if (enableVerticalDismiss) {
        FlowCenteredPopupDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = modifier,
        ) {
            FlowSheetHeader(
                inSidePane = true,
                title = currentTitle,
                onClose = onDismiss,
                onBack = backPage?.let { page -> { currentPage = page } },
                contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
                closeButtonSize = null,
                dividerAlpha = 0.4f,
                showDragHandle = false,
            )
            settingsContent()
        }
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                FlowSheetHeader(
                    inSidePane = true,
                    title = currentTitle,
                    onClose = onDismiss,
                    onBack = backPage?.let { page -> { currentPage = page } },
                    contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
                    closeButtonSize = null,
                    dividerAlpha = 0.4f,
                    showDragHandle = false,
                )
                Box(modifier = Modifier.weight(1f)) {
                    settingsContent()
                }
            }
        }
    }
}

enum class PlayerSettingsPage {
    Main,
    More,
    Quality,
    Speed,
    Audio,
    Subtitles,
    SubtitleStyle,
    Equalizer,
}

private val SheetContentVerticalPadding = 12.dp
