package com.fedoratube.org.ui.components.shared.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.shared.MediaIconBadge
import com.fedoratube.org.ui.components.shared.MediaThumbnail
import com.fedoratube.org.ui.components.shared.MediaThumbnailDefaults
import com.fedoratube.org.ui.components.shared.VideoStatusBadge

/**
 * A card's thumbnail: the shared [MediaThumbnail] with the status, reminder and DeArrow badges on it.
 * The image is decorative because the card's title already names the video.
 *
 * [width] is unspecified for a stacked card, whose [modifier] fills the width instead.
 */
@Composable
internal fun VideoCardThumbnail(
    state: VideoCardState,
    isUpcoming: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    isFeedPlaybackEligible: Boolean = false,
) {
    val video = state.video
    MediaThumbnail(
        videoId = video.id,
        thumbnailUrl = state.thumbnailUrl,
        width = width,
        shape = shape,
        showWatchProgress = LocalVideoCardPreferences.current.showWatchProgress,
        modifier = modifier,
    ) {
        val badgeModifier = Modifier.padding(MediaThumbnailDefaults.BadgePadding)
        VideoStatusBadge(
            isLive = video.isLive,
            isUpcoming = isUpcoming,
            durationSeconds = video.duration,
            modifier = badgeModifier.align(Alignment.BottomEnd),
        )
        if (state.showReminderBadge) {
            MediaIconBadge(
                icon = Icons.Rounded.NotificationsActive,
                contentDescription = stringResource(R.string.upcoming_video_reminder_badge),
                modifier = badgeModifier.align(Alignment.TopStart),
            )
        }
        if (state.showDeArrowBadge) {
            MediaIconBadge(
                icon = Icons.Outlined.AutoFixHigh,
                contentDescription = stringResource(R.string.dearrow_badge),
                modifier = badgeModifier.align(Alignment.TopEnd),
            )
        }

        if (isFeedPlaybackEligible && !isUpcoming && !video.isLive) {
            var isMuted by rememberSaveable(video.id) { mutableStateOf(true) }
            var isCaptionsEnabled by rememberSaveable(video.id) { mutableStateOf(false) }

            // YouTube-style feed playback controls: Mute & CC
            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.65f),
                    onClick = { isCaptionsEnabled = !isCaptionsEnabled },
                    modifier = Modifier.size(32.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ClosedCaption,
                            contentDescription = "Captions",
                            tint = if (isCaptionsEnabled) Color(0xFF3EA6FF) else Color.White,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            // Bottom scrub line (YouTube red line)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.35f)
                            .fillMaxHeight()
                            .background(Color(0xFFFF0000)),
                )
            }
        }
    }
}
