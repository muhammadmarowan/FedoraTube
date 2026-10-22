package com.fedoratube.org.ui.components.videoplayer.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.player.EnhancedPlayerState
import com.fedoratube.org.player.QualityOption
import com.fedoratube.org.ui.components.equalizer.EqQuickPanel
import com.fedoratube.org.ui.components.layout.navigation.LocalMediaNavigator
import com.fedoratube.org.ui.components.shared.FlowBottomSheet
import com.fedoratube.org.ui.components.shared.FlowSheetHeader
import com.fedoratube.org.ui.components.shared.defaultSheetExpandedHeight
import com.fedoratube.org.ui.components.shared.rememberFlowBottomSheetState
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
    onCastClick: () -> Unit = {},
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
    val sheetState = rememberFlowBottomSheetState()
    var currentPage by remember { mutableStateOf(initialPage) }
    var subtitleStyleReturnPage by remember { mutableStateOf(PlayerSettingsPage.Main) }
    val currentTitle =
        when (currentPage) {
            PlayerSettingsPage.Main -> stringResource(R.string.player_settings)
            PlayerSettingsPage.Quality -> stringResource(R.string.video_quality_title)
            PlayerSettingsPage.Speed -> stringResource(R.string.playback_speed)
            PlayerSettingsPage.Audio -> stringResource(R.string.audio_track)
            PlayerSettingsPage.Subtitles -> stringResource(R.string.filter_subtitles)
            PlayerSettingsPage.SubtitleStyle -> stringResource(R.string.subtitle_style)
            PlayerSettingsPage.Equalizer -> stringResource(R.string.equalizer)
            PlayerSettingsPage.More -> stringResource(R.string.more_header)
        }
    val backPage =
        when (currentPage) {
            PlayerSettingsPage.Main -> null
            PlayerSettingsPage.SubtitleStyle -> subtitleStyleReturnPage
            PlayerSettingsPage.Audio, PlayerSettingsPage.Equalizer -> PlayerSettingsPage.More
            else -> PlayerSettingsPage.Main
        }
    // A panel host swaps the panel's content in place, so a row that opens another surface must not
    // run the exit animation there: the host's onDismiss would close the panel under the new surface.
    val leaveFor: (() -> Unit) -> Unit =
        if (enableVerticalDismiss) {
            { after -> sheetState.dismiss(after) }
        } else {
            { after -> after() }
        }

    LaunchedEffect(initialPage) {
        currentPage = initialPage
    }

    FlowBottomSheet(
        onDismiss = onDismiss,
        modifier = modifier,
        state = sheetState,
        expandedHeight = expandedHeight ?: defaultSheetExpandedHeight(),
        collapsedHeight = collapsedHeight,
        dismissible = enableVerticalDismiss,
        dismissOnOutsideTap = false,
        shape = RectangleShape,
        containerColor = MaterialTheme.colorScheme.surface,
        onBack = backPage?.let { page -> { currentPage = page } },
        onProgressChange = onSheetProgressChange,
        header = { dragModifier ->
            if (currentPage == PlayerSettingsPage.Main && enableVerticalDismiss) {
                Box(
                    modifier = dragModifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                    )
                }
            } else {
                FlowSheetHeader(
                    inSidePane = !enableVerticalDismiss,
                    title = currentTitle,
                    onClose = { sheetState.dismiss() },
                    modifier = dragModifier,
                    onBack = backPage?.let { page -> { currentPage = page } },
                    contentPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 8.dp),
                    closeButtonSize = null,
                    dividerAlpha = 0.4f,
                )
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = SheetContentVerticalPadding),
        ) {
            when (currentPage) {
                PlayerSettingsPage.Main -> {
                    PlayerSettingsMainPage(
                        playerState = playerState,
                        autoplayEnabled = autoplayEnabled,
                        subtitlesEnabled = subtitlesEnabled,
                        ambientModeEnabled = ambientModeEnabled,
                        onNavigateToPage = { currentPage = it },
                        onShowSubtitleStyle = {
                            subtitleStyleReturnPage = PlayerSettingsPage.Main
                            currentPage = PlayerSettingsPage.SubtitleStyle
                        },
                        onCastClick = { leaveFor(onCastClick) },
                        onPipClick = { sheetState.dismiss(onPipClick) },
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
                            sheetState.dismiss()
                        },
                    )
                }

                PlayerSettingsPage.Speed -> {
                    PlayerSettingsSpeedPage(
                        currentSpeed = playerState.playbackSpeed,
                        onSpeedSelected = onSpeedSelected,
                        onSpeedSelectionFinished = { sheetState.dismiss() },
                    )
                }

                PlayerSettingsPage.Audio -> {
                    PlayerSettingsAudioPage(
                        availableAudioTracks = playerState.availableAudioTracks,
                        currentAudioTrack = playerState.currentAudioTrack,
                        onTrackSelected = {
                            onAudioTrackSelected(it)
                            sheetState.dismiss()
                        },
                    )
                }

                PlayerSettingsPage.Equalizer -> {
                    val navigator = LocalMediaNavigator.current
                    EqQuickPanel(
                        onEdit = { sheetState.dismiss { navigator.openEqualizer() } },
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
                            sheetState.dismiss()
                        },
                        onDisableSubtitles = {
                            onDisableSubtitles()
                            sheetState.dismiss()
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

                PlayerSettingsPage.More -> {
                    PlayerSettingsMorePage(
                        playerState = playerState,
                        autoplayEnabled = autoplayEnabled,
                        onNavigateToPage = { currentPage = it },
                        onCastClick = { leaveFor(onCastClick) },
                        onPipClick = { sheetState.dismiss(onPipClick) },
                        onSleepTimerClick = { leaveFor(onSleepTimerClick) },
                        onAutoplayToggle = onAutoplayToggle,
                        onSkipSilenceToggle = onSkipSilenceToggle,
                        onShowSubtitleStyle = {
                            subtitleStyleReturnPage = PlayerSettingsPage.More
                            currentPage = PlayerSettingsPage.SubtitleStyle
                        },
                        sponsorBlockSegmentCount = sponsorBlockSegmentCount,
                        sponsorBlockOffForVideo = sponsorBlockOffForVideo,
                        onSponsorBlockToggle = onSponsorBlockToggle,
                        notePositionMs = notePositionMs,
                        onAddNote = onAddNote,
                    )
                }
            }
        }
    }
}

enum class PlayerSettingsPage {
    Main,
    Quality,
    Speed,
    Audio,
    Subtitles,
    SubtitleStyle,
    Equalizer,
    More,
}

private val SheetContentVerticalPadding = 12.dp
