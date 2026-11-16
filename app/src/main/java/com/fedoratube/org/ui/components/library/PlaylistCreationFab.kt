package com.fedoratube.org.ui.components.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R

internal enum class PlaylistCreationTarget {
    Video,
    Music,
}

@Composable
internal fun PlaylistCreationFabMenu(
    onCreateVideo: () -> Unit,
    onCreateMusic: () -> Unit,
    onImport: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "fab_rotation",
    )

    val fabScale by animateFloatAsState(
        targetValue = if (expanded) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "fab_scale",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Video Playlist item
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                initialOffsetY = { it / 2 },
            ) + fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            ),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                targetOffsetY = { it / 3 },
            ) + fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            PlaylistFabMenuItem(
                title = stringResource(R.string.video),
                icon = Icons.Rounded.VideoLibrary,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                onClick = {
                    expanded = false
                    onCreateVideo()
                },
            )
        }

        // Music Playlist item
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                initialOffsetY = { it / 3 },
            ) + fadeIn(spring(stiffness = Spring.StiffnessMedium)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
            ),
            exit = slideOutVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
                targetOffsetY = { it / 4 },
            ) + fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            PlaylistFabMenuItem(
                title = stringResource(R.string.tab_music),
                icon = Icons.Rounded.MusicNote,
                iconTint = MaterialTheme.colorScheme.tertiary,
                iconBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                onClick = {
                    expanded = false
                    onCreateMusic()
                },
            )
        }

        // Main FAB
        FloatingActionButton(
            onClick = { expanded = !expanded },
            shape = CircleShape,
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.primary,
            contentColor = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 2.dp,
            ),
            modifier = Modifier
                .scale(fabScale)
                .size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(
                    if (expanded) R.string.playlist_creation_menu_close else R.string.playlist_creation_menu_open,
                ),
                modifier = Modifier
                    .size(26.dp)
                    .rotate(rotation),
            )
        }
    }
}

@Composable
private fun PlaylistFabMenuItem(
    title: String,
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconBackground,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
