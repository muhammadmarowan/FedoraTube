package com.fedoratube.org.ui.components.videoplayer.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurCircular
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.fedoratube.org.R
import com.fedoratube.org.player.EnhancedPlayerState
import com.fedoratube.org.ui.components.equalizer.equalizerSummary
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowRowGroup
import com.fedoratube.org.ui.components.shared.FlowSectionHeader
import com.fedoratube.org.ui.components.shared.FlowSwitchRow
import com.fedoratube.org.ui.components.shared.flowRowGroupShape
import com.fedoratube.org.utils.formatDurationMillis

private const val PLAYBACK_ROWS = 3
private const val OVERLAY_ROWS = 2
private const val EFFECT_ROWS = 3
private const val DISPLAY_ROWS = 1

/**
 * The settings behind the front page's "More": everything YouTube's six rows leave out — playback
 * toggles, notes, the overlay controls, audio effects and display. No "Cast to TV": casting is out
 * of the app.
 */
@Composable
internal fun PlayerSettingsMorePage(
    playerState: EnhancedPlayerState,
    autoplayEnabled: Boolean,
    ambientModeEnabled: Boolean,
    onNavigateToPage: (PlayerSettingsPage) -> Unit,
    onPipClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLoopToggle: (Boolean) -> Unit,
    onAutoplayToggle: (Boolean) -> Unit,
    onSkipSilenceToggle: (Boolean) -> Unit,
    onStableVolumeToggle: (Boolean) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    sponsorBlockSegmentCount: Int = 0,
    sponsorBlockOffForVideo: Boolean = false,
    onSponsorBlockToggle: (off: Boolean) -> Unit = {},
    notePositionMs: Long? = null,
    onAddNote: () -> Unit = {},
) {
    val playbackRows = if (sponsorBlockSegmentCount > 0) PLAYBACK_ROWS + 1 else PLAYBACK_ROWS
    FlowSectionHeader(stringResource(R.string.playback_header))
    FlowRowGroup {
        FlowSwitchRow(
            leadingIcon = Icons.Filled.Repeat,
            title = stringResource(R.string.loop_video),
            checked = playerState.isLooping,
            shape = flowRowGroupShape(0, playbackRows),
            onCheckedChange = onLoopToggle,
        )
        FlowSwitchRow(
            leadingIcon = Icons.Filled.SkipNext,
            title = stringResource(R.string.autoplay_next),
            checked = autoplayEnabled,
            enabled = !playerState.isLooping,
            shape = flowRowGroupShape(1, playbackRows),
            onCheckedChange = onAutoplayToggle,
        )
        if (sponsorBlockSegmentCount > 0) {
            FlowSwitchRow(
                leadingIcon = Icons.Filled.Block,
                title = stringResource(R.string.player_settings_sponsorblock_video),
                supportingText =
                    pluralStringResource(R.plurals.sb_video_segments, sponsorBlockSegmentCount, sponsorBlockSegmentCount),
                checked = !sponsorBlockOffForVideo,
                shape = flowRowGroupShape(2, playbackRows),
                onCheckedChange = { on -> onSponsorBlockToggle(!on) },
            )
        }
    }

    if (notePositionMs != null) {
        FlowSectionHeader(stringResource(R.string.note_title))
        FlowRowGroup {
            FlowNavRow(
                leadingIcon = Icons.Filled.MoreTime,
                title = stringResource(R.string.note_at_time, formatDurationMillis(notePositionMs)),
                shape = flowRowGroupShape(0, 1),
                onClick = onAddNote,
            )
        }
    }

    FlowSectionHeader(stringResource(R.string.player_settings_overlay_controls))
    FlowRowGroup {
        FlowNavRow(
            leadingIcon = Icons.Filled.PictureInPicture,
            title = stringResource(R.string.pip_mode),
            shape = flowRowGroupShape(0, OVERLAY_ROWS),
            onClick = onPipClick,
        )
        FlowNavRow(
            leadingIcon = Icons.Filled.Bedtime,
            title = stringResource(R.string.sleep_timer),
            shape = flowRowGroupShape(1, OVERLAY_ROWS),
            onClick = onSleepTimerClick,
        )
    }

    FlowSectionHeader(stringResource(R.string.audio_effects))
    FlowRowGroup {
        FlowNavRow(
            leadingIcon = Icons.Filled.Equalizer,
            title = stringResource(R.string.equalizer),
            trailingText = equalizerSummary(),
            shape = flowRowGroupShape(0, EFFECT_ROWS),
            onClick = { onNavigateToPage(PlayerSettingsPage.Equalizer) },
        )
        FlowSwitchRow(
            leadingIcon = Icons.Filled.GraphicEq,
            title = stringResource(R.string.player_settings_skip_silence),
            checked = playerState.isSkipSilenceEnabled,
            shape = flowRowGroupShape(1, EFFECT_ROWS),
            onCheckedChange = onSkipSilenceToggle,
        )
        FlowSwitchRow(
            leadingIcon = Icons.AutoMirrored.Filled.VolumeUp,
            title = stringResource(R.string.player_settings_stable_voice),
            checked = playerState.isStableVolumeEnabled,
            shape = flowRowGroupShape(2, EFFECT_ROWS),
            onCheckedChange = onStableVolumeToggle,
        )
    }

    FlowSectionHeader(stringResource(R.string.player_settings_display))
    FlowRowGroup {
        FlowSwitchRow(
            leadingIcon = Icons.Filled.BlurCircular,
            title = stringResource(R.string.player_settings_ambient_mode),
            checked = ambientModeEnabled,
            shape = flowRowGroupShape(0, DISPLAY_ROWS),
            onCheckedChange = onAmbientModeToggle,
        )
    }
}
