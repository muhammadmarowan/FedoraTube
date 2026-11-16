/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.components.musicplayer.mini

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.fedoratube.org.R
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.player.EnhancedMusicPlayerManager
import com.fedoratube.org.ui.components.PlayingWaveform
import com.fedoratube.org.ui.theme.ArtworkScrimContent
import com.fedoratube.org.ui.theme.ArtworkScrimNowPlaying

private val ArtworkSize = 40.dp
private val ArtworkCorner = 10.dp

/**
 * Modern iOS Apple Music / Dynamic Island style mini player pill:
 * Features squircle album art, SF Pro-styled typography, crisp iOS transport buttons,
 * and a sleek continuous progress line along the bottom of the pill.
 */
@Composable
internal fun MiniPlayerContent(
    track: MusicTrack,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = true,
    showPrevious: Boolean = false,
) {
    val playerState by EnhancedMusicPlayerManager.playerState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val animatedProgress by animateFloatAsState(
        targetValue =
            if (playerState.duration > 0) {
                (playerState.position.toFloat() / playerState.duration.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            },
        animationSpec = if (animationsEnabled) tween(900, easing = LinearEasing) else snap(),
        label = "miniProgress",
    )

    val progressTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val progressFillColor = MaterialTheme.colorScheme.primary

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .drawBehind {
                    val y = size.height - 1.5.dp.toPx()
                    val startX = 20.dp.toPx()
                    val endX = size.width - 20.dp.toPx()
                    val strokeW = 2.dp.toPx()

                    drawLine(
                        color = progressTrackColor,
                        start = Offset(startX, y),
                        end = Offset(endX, y),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round,
                    )
                    if (animatedProgress > 0f) {
                        drawLine(
                            color = progressFillColor,
                            start = Offset(startX, y),
                            end = Offset(startX + (endX - startX) * animatedProgress, y),
                            strokeWidth = strokeW,
                            cap = StrokeCap.Round,
                        )
                    }
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // iOS-style Squircle Album Art with soft shadow
            val artworkShape = RoundedCornerShape(ArtworkCorner)
            Box(
                modifier =
                    Modifier
                        .size(ArtworkSize)
                        .shadow(elevation = 3.dp, shape = artworkShape, clip = false)
                        .clip(artworkShape)
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), artworkShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = track.listThumbnailUrl,
                    contentDescription = stringResource(R.string.album_art),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (playerState.isPlaying && animationsEnabled) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(ArtworkScrimNowPlaying),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayingWaveform(
                            color = ArtworkScrimContent,
                            barCount = 3,
                            barWidth = 2.dp,
                            barSpacing = 1.2.dp,
                            staggerMillis = 120,
                        )
                    }
                }
            }

            // Track info: Title and Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = track.title,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        if (animationsEnabled) {
                            Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 2500,
                            )
                        } else {
                            Modifier
                        },
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = track.artist,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // iOS-style transport controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showPrevious) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            EnhancedMusicPlayerManager.playPrevious()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = stringResource(R.string.previous),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // iOS Play/Pause Button
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        EnhancedMusicPlayerManager.togglePlayPause()
                    },
                    modifier = Modifier.size(38.dp),
                ) {
                    if (playerState.isBuffering && animationsEnabled) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription =
                                if (playerState.isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        EnhancedMusicPlayerManager.playNext()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
