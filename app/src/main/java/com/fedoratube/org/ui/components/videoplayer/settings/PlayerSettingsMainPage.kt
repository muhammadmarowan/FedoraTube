package com.fedoratube.org.ui.components.videoplayer.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fedoratube.org.R
import com.fedoratube.org.player.EnhancedPlayerState
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowRowGroup
import com.fedoratube.org.ui.components.shared.flowRowGroupShape
import com.fedoratube.org.ui.components.shared.playbackSpeedLabel

private const val MAIN_ROWS = 6

/**
 * The sheet's front page, kept to YouTube's six rows: Quality, Playback speed, Captions,
 * Audio Track, Lock screen, More. Everything else lives behind [PlayerSettingsMorePage] so the
 * first view stays a single glance instead of a scroll; "Cast to TV" is gone from the app.
 */
@Composable
internal fun PlayerSettingsMainPage(
    playerState: EnhancedPlayerState,
    subtitlesEnabled: Boolean,
    onNavigateToPage: (PlayerSettingsPage) -> Unit,
    onLockScreen: () -> Unit,
) {
    FlowRowGroup {
        FlowNavRow(
            leadingIcon = Icons.Filled.HighQuality,
            title = stringResource(R.string.quality),
            trailingText =
                if (playerState.currentQuality == 0) {
                    stringResource(R.string.quality_auto)
                } else {
                    "${playerState.currentQuality}p"
                },
            shape = flowRowGroupShape(0, MAIN_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Quality) },
        )
        FlowNavRow(
            leadingIcon = Icons.Filled.Speed,
            title = stringResource(R.string.playback_speed),
            trailingText = playbackSpeedLabel(playerState.playbackSpeed),
            shape = flowRowGroupShape(1, MAIN_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Speed) },
        )
        FlowNavRow(
            leadingIcon = Icons.Filled.Subtitles,
            title = stringResource(R.string.captions),
            trailingText =
                if (subtitlesEnabled) stringResource(R.string.on) else stringResource(R.string.off),
            shape = flowRowGroupShape(2, MAIN_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Subtitles) },
        )
        FlowNavRow(
            leadingIcon = Icons.Filled.AudioFile,
            title = stringResource(R.string.audio_track),
            trailingText =
                audioTrackDisplayLabel(
                    playerState.availableAudioTracks.getOrNull(playerState.currentAudioTrack),
                    playerState.currentAudioTrack,
                ),
            shape = flowRowGroupShape(3, MAIN_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Audio) },
        )
        FlowNavRow(
            leadingIcon = Icons.Rounded.Lock,
            title = stringResource(R.string.lock_screen),
            showChevron = false,
            shape = flowRowGroupShape(4, MAIN_ROWS),
            onClick = onLockScreen,
        )
        FlowNavRow(
            leadingIcon = Icons.Rounded.Settings,
            title = stringResource(R.string.more_header),
            shape = flowRowGroupShape(5, MAIN_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.More) },
        )
    }
}
