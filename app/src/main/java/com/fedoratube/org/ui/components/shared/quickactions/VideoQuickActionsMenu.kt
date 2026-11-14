package com.fedoratube.org.ui.components.shared.quickactions

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.ui.components.shared.SaveVideoSheet
import com.fedoratube.org.ui.components.shared.rememberVideoShareAction

/**
 * The ⋮ menu a video card opens: an anchored popup under the button, styled like YouTube's, rather
 * than a bottom sheet. [title] is the card's resolved title so a DeArrow title stays consistent.
 * "Save to playlist" hands off to [SaveVideoSheet]; every other row runs its action and closes.
 */
@Composable
fun VideoQuickActionsMenu(
    video: Video,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    title: String = video.title,
    viewModel: QuickActionsViewModel = sharedQuickActionsViewModel(),
) {
    var showSave by remember { mutableStateOf(false) }
    val watchLaterIds by viewModel.watchLaterIds.collectAsStateWithLifecycle()
    val share = rememberVideoShareAction()
    val haptics = LocalHapticFeedback.current
    val isWatchLater = video.id in watchLaterIds

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(min = 240.dp),
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.watch_later)) },
            leadingIcon = {
                Icon(
                    imageVector = if (isWatchLater) Icons.Filled.WatchLater else Icons.Outlined.WatchLater,
                    contentDescription = null,
                )
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.toggleWatchLater(video)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.save_to_playlist)) },
            leadingIcon = {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
            },
            onClick = {
                showSave = true
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.download)) },
            leadingIcon = {
                Icon(Icons.Outlined.Download, contentDescription = null)
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.requestDownload(video)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.share)) },
            leadingIcon = {
                Icon(Icons.Outlined.Share, contentDescription = null)
            },
            onClick = {
                share(video.id, title, video.isShort)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.interested)) },
            leadingIcon = {
                Icon(Icons.Outlined.ThumbUp, contentDescription = null)
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                viewModel.markAsInteresting(video)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.not_interested)) },
            leadingIcon = {
                Icon(Icons.Outlined.ThumbDown, contentDescription = null)
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                viewModel.markNotInterested(video)
                onDismissRequest()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dont_show_channel)) },
            leadingIcon = {
                Icon(Icons.Outlined.Block, contentDescription = null)
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                viewModel.blockChannel(video)
                onDismissRequest()
            },
        )
    }

    if (showSave) {
        SaveVideoSheet(
            video = video,
            onDismiss = { showSave = false },
        )
    }
}
