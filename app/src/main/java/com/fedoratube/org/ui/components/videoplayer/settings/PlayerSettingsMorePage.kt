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
import com.fedoratube.org.ui.components.shared.FlowSwitchRow
import com.fedoratube.org.ui.components.shared.flowRowGroupShape
import com.fedoratube.org.utils.formatDurationMillis

/**
 * The settings behind the front page's "More": everything YouTube's six rows leave out — playback
 * toggles, notes, the overlay controls, audio effects and display. No "Cast to TV": casting is out
 * of the app.
 *
 * Laid out as the one continuous grouped list the front page shows — same rows, same group, no
 * section labels splitting it — so entering More reads as a page of the same sheet rather than a
 * different screen.
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
    // The conditional SponsorBlock and note rows shift every row under them, so the group's
    // count and each row's index inside it are worked out first. Eight rows are unconditional:
    // loop, autoplay, PiP, sleep timer, equalizer, skip silence, stable voice and ambient mode.
    val sponsorBlockRows = if (sponsorBlockSegmentCount > 0) 1 else 0
    val noteRows = if (notePositionMs != null) 1 else 0
    val rows = 8 + sponsorBlockRows + noteRows
    val overlayIndex = 2 + sponsorBlockRows + noteRows
    val effectsIndex = overlayIndex + 2
    val displayIndex = effectsIndex + 3

    FlowRowGroup {
        FlowSwitchRow(
            leadingIcon = Icons.Filled.Repeat,
            title = stringResource(R.string.loop_video),
            checked = playerState.isLooping,
            shape = flowRowGroupShape(0, rows),
            onCheckedChange = onLoopToggle,
        )
        FlowSwitchRow(
            leadingIcon = Icons.Filled.SkipNext,
            title = stringResource(R.string.autoplay_next),
            checked = autoplayEnabled,
            enabled = !playerState.isLooping,
            shape = flowRowGroupShape(1, rows),
            onCheckedChange = onAutoplayToggle,
        )
        if (sponsorBlockSegmentCount > 0) {
            FlowSwitchRow(
                leadingIcon = Icons.Filled.Block,
                title = stringResource(R.string.player_settings_sponsorblock_video),
                supportingText =
                    pluralStringResource(R.plurals.sb_video_segments, sponsorBlockSegmentCount, sponsorBlockSegmentCount),
                checked = !sponsorBlockOffForVideo,
                shape = flowRowGroupShape(2, rows),
                onCheckedChange = { on -> onSponsorBlockToggle(!on) },
            )
        }
        if (notePositionMs != null) {
            FlowNavRow(
                leadingIcon = Icons.Filled.MoreTime,
                title = stringResource(R.string.note_at_time, formatDurationMillis(notePositionMs)),
                shape = flowRowGroupShape(2 + sponsorBlockRows, rows),
                onClick = onAddNote,
            )
        }
        FlowNavRow(
            leadingIcon = Icons.Filled.PictureInPicture,
            title = stringResource(R.string.pip_mode),
            shape = flowRowGroupShape(overlayIndex, rows),
            onClick = onPipClick,
        )
        FlowNavRow(
            leadingIcon = Icons.Filled.Bedtime,
            title = stringResource(R.string.sleep_timer),
            shape = flowRowGroupShape(overlayIndex + 1, rows),
            onClick = onSleepTimerClick,
        )
        FlowNavRow(
            leadingIcon = Icons.Filled.Equalizer,
            title = stringResource(R.string.equalizer),
            trailingText = equalizerSummary(),
            shape = flowRowGroupShape(effectsIndex, rows),
            onClick = { onNavigateToPage(PlayerSettingsPage.Equalizer) },
        )
        FlowSwitchRow(
            leadingIcon = Icons.Filled.GraphicEq,
            title = stringResource(R.string.player_settings_skip_silence),
            checked = playerState.isSkipSilenceEnabled,
            shape = flowRowGroupShape(effectsIndex + 1, rows),
            onCheckedChange = onSkipSilenceToggle,
        )
        FlowSwitchRow(
            leadingIcon = Icons.AutoMirrored.Filled.VolumeUp,
            title = stringResource(R.string.player_settings_stable_voice),
            checked = playerState.isStableVolumeEnabled,
            shape = flowRowGroupShape(effectsIndex + 2, rows),
            onCheckedChange = onStableVolumeToggle,
        )
        FlowSwitchRow(
            leadingIcon = Icons.Filled.BlurCircular,
            title = stringResource(R.string.player_settings_ambient_mode),
            checked = ambientModeEnabled,
            shape = flowRowGroupShape(displayIndex, rows),
            onCheckedChange = onAmbientModeToggle,
        )
    }
}
