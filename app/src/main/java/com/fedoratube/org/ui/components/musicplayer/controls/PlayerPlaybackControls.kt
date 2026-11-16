/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.components.musicplayer.controls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.musicplayer.common.SkipDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class PlaybackButtonType { PREVIOUS, PLAY_PAUSE, NEXT }

/**
 * Modern iOS Apple Music style playback controls:
 * Clean circular layout with prominent centered play/pause button,
 * balanced previous/next controls, frosted glass containers, and signature tactile spring animations.
 */
@Composable
fun PlayerPlaybackControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPreviousClick: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPreviewDirectionChange: (SkipDirection?) -> Unit = {},
) {
    var isPressed by remember { mutableStateOf(false) }
    var isPreviousPressed by remember { mutableStateOf(false) }
    var isNextPressed by remember { mutableStateOf(false) }
    var lastClicked by remember { mutableStateOf<PlaybackButtonType?>(null) }
    var clickTrigger by remember { mutableIntStateOf(0) }
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val isPlayPauseLocked =
        lastClicked == PlaybackButtonType.NEXT || lastClicked == PlaybackButtonType.PREVIOUS
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    var pendingPlayPauseState by remember { mutableStateOf<Boolean?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(lastClicked, clickTrigger) {
        if (lastClicked != null) {
            val releaseDelay = if (lastClicked == PlaybackButtonType.PLAY_PAUSE) 220L else 600L
            delay(releaseDelay)
            lastClicked = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pendingPlayPauseState = true
            return@LaunchedEffect
        }
        if (lastClicked != PlaybackButtonType.PLAY_PAUSE) {
            delay(220L)
        }
        if (!latestIsPlaying) {
            pendingPlayPauseState = false
        }
    }

    LaunchedEffect(isPlayPauseLocked, pendingPlayPauseState) {
        if (!isPlayPauseLocked) {
            pendingPlayPauseState?.let {
                playPauseVisualState = it
                pendingPlayPauseState = null
            }
        }
    }

    val playPauseScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "playPauseScale",
    )
    val previousScale by animateFloatAsState(
        targetValue = if (isPreviousPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "previousScale",
    )
    val nextScale by animateFloatAsState(
        targetValue = if (isNextPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 600f),
        label = "nextScale",
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // iOS Previous Button
        IosControlButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = stringResource(R.string.previous),
            buttonSize = 54.dp,
            iconSize = 34.dp,
            scale = previousScale,
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White.copy(alpha = 0.90f),
            onClick = {
                lastClicked = PlaybackButtonType.PREVIOUS
                clickTrigger++
                scope.launch {
                    delay(120L)
                    onPreviousClick()
                }
            },
            onPressedChange = { isPreviousPressed = it },
            onLongPressStart = { onPreviewDirectionChange(SkipDirection.PREVIOUS) },
            onLongPressEnd = { onPreviewDirectionChange(null) },
        )

        // iOS Play/Pause Button (Prominent centered frosted glass circle)
        IosControlButton(
            icon = if (playPauseVisualState) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription =
                if (playPauseVisualState) stringResource(R.string.pause) else stringResource(R.string.play),
            buttonSize = 72.dp,
            iconSize = 42.dp,
            scale = playPauseScale,
            containerColor = Color.White.copy(alpha = 0.18f),
            contentColor = Color.White,
            borderColor = Color.White.copy(alpha = 0.35f),
            hasShadow = true,
            isBuffering = isBuffering,
            onClick = {
                lastClicked = PlaybackButtonType.PLAY_PAUSE
                clickTrigger++
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPlayPauseToggle()
            },
            onPressedChange = { isPressed = it },
        )

        // iOS Next Button
        IosControlButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.next),
            buttonSize = 54.dp,
            iconSize = 34.dp,
            scale = nextScale,
            containerColor = Color.White.copy(alpha = 0.08f),
            contentColor = Color.White.copy(alpha = 0.90f),
            onClick = {
                lastClicked = PlaybackButtonType.NEXT
                clickTrigger++
                scope.launch {
                    delay(120L)
                    onNextClick()
                }
            },
            onPressedChange = { isNextPressed = it },
            onLongPressStart = { onPreviewDirectionChange(SkipDirection.NEXT) },
            onLongPressEnd = { onPreviewDirectionChange(null) },
        )
    }
}

@Composable
private fun IosControlButton(
    icon: ImageVector,
    contentDescription: String?,
    buttonSize: Dp,
    iconSize: Dp,
    scale: Float,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    hasShadow: Boolean = false,
    isBuffering: Boolean = false,
    onClick: () -> Unit,
    onPressedChange: (Boolean) -> Unit,
    onLongPressStart: (() -> Unit)? = null,
    onLongPressEnd: (() -> Unit)? = null,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnPressedChange by rememberUpdatedState(onPressedChange)
    val currentLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentLongPressEnd by rememberUpdatedState(onLongPressEnd)

    Box(
        modifier =
            Modifier
                .size(buttonSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .then(
                    if (hasShadow) {
                        Modifier.shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                    } else {
                        Modifier
                    },
                )
                .clip(CircleShape)
                .background(containerColor)
                .border(0.75.dp, borderColor, CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        currentOnPressedChange(true)
                        var longPressed = false
                        try {
                            var cancelled = false
                            val upBeforeTimeout =
                                withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    val up = waitForUpOrCancellation()
                                    if (up == null) cancelled = true
                                    up
                                }
                            when {
                                cancelled -> {
                                    Unit
                                }

                                upBeforeTimeout != null -> {
                                    currentOnClick()
                                }

                                currentLongPressStart != null -> {
                                    longPressed = true
                                    currentLongPressStart?.invoke()
                                    waitForUpOrCancellation()
                                }

                                else -> {
                                    if (waitForUpOrCancellation() != null) {
                                        currentOnClick()
                                    }
                                }
                            }
                        } finally {
                            currentOnPressedChange(false)
                            if (longPressed) currentLongPressEnd?.invoke()
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = contentColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
