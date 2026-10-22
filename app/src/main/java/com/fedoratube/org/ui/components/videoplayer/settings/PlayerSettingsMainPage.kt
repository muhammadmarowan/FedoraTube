package com.fedoratube.org.ui.components.videoplayer.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.R
import com.fedoratube.org.player.EnhancedPlayerState
import com.fedoratube.org.ui.components.shared.playbackSpeedLabel

@Composable
internal fun PlayerSettingsMainPage(
    playerState: EnhancedPlayerState,
    autoplayEnabled: Boolean,
    subtitlesEnabled: Boolean,
    ambientModeEnabled: Boolean,
    onNavigateToPage: (PlayerSettingsPage) -> Unit,
    onShowSubtitleStyle: () -> Unit,
    onCastClick: () -> Unit,
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
    val qualityLabel =
        if (playerState.currentQuality == 0) {
            stringResource(R.string.quality_auto)
        } else {
            "${playerState.currentQuality}p"
        }

    val captionLabel =
        if (subtitlesEnabled) {
            playerState.availableSubtitles.find { it.url == playerState.selectedSubtitleUrl }?.label?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.on)
        } else {
            null
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        // 1. Quality • Auto (720p60)
        YouTubeSettingsRow(
            icon = Icons.Outlined.Settings,
            title = stringResource(R.string.quality),
            trailingText = qualityLabel,
            onClick = { onNavigateToPage(PlayerSettingsPage.Quality) },
        )

        // 2. Captions
        YouTubeSettingsRow(
            icon = Icons.Outlined.ClosedCaption,
            title = stringResource(R.string.captions),
            trailingText = captionLabel,
            onClick = { onNavigateToPage(PlayerSettingsPage.Subtitles) },
        )

        // 3. Loop video
        YouTubeSettingsRow(
            icon = Icons.Rounded.Repeat,
            title = stringResource(R.string.loop_video),
            trailingText = if (playerState.isLooping) stringResource(R.string.on) else null,
            onClick = { onLoopToggle(!playerState.isLooping) },
        )

        // 4. Stable volume
        YouTubeSettingsRow(
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
            title = stringResource(R.string.player_settings_stable_voice),
            trailingText = if (playerState.isStableVolumeEnabled) stringResource(R.string.on) else null,
            onClick = { onStableVolumeToggle(!playerState.isStableVolumeEnabled) },
        )

        // 5. Help & feedback / Sleep timer
        YouTubeSettingsRow(
            icon = Icons.Outlined.HelpOutline,
            title = stringResource(R.string.sleep_timer),
            onClick = onSleepTimerClick,
        )

        // 6. Playback speed • 1x
        YouTubeSettingsRow(
            icon = Icons.Filled.Speed,
            title = stringResource(R.string.playback_speed),
            trailingText = playbackSpeedLabel(playerState.playbackSpeed),
            onClick = { onNavigateToPage(PlayerSettingsPage.Speed) },
        )

        // 7. Ambient mode / Watch in VR
        YouTubeSettingsRow(
            icon = ImageVector.vectorResource(R.drawable.ic_ambient_mode),
            title = stringResource(R.string.player_settings_ambient_mode),
            trailingText = if (ambientModeEnabled) stringResource(R.string.on) else null,
            onClick = { onAmbientModeToggle(!ambientModeEnabled) },
        )

        // 8. More >
        YouTubeSettingsRow(
            icon = Icons.Outlined.Settings,
            title = stringResource(R.string.more_header),
            showChevron = true,
            onClick = { onNavigateToPage(PlayerSettingsPage.More) },
        )
    }
}

@Composable
private fun YouTubeSettingsRow(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    showChevron: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (trailingText != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "•  $trailingText",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
