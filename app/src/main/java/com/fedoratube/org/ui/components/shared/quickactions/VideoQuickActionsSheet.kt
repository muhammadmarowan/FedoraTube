package com.fedoratube.org.ui.components.shared.quickactions

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.QueuePlayNext
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.model.VideoCollaborator
import com.fedoratube.org.ui.components.layout.navigation.LocalMediaNavigator
import com.fedoratube.org.ui.components.shared.ChannelAvatarStack
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowSubscribeButton
import com.fedoratube.org.ui.components.shared.FlowSubscribeButtonSize
import com.fedoratube.org.ui.components.shared.MediaThumbnail
import com.fedoratube.org.ui.components.shared.SaveVideoSheet
import com.fedoratube.org.ui.components.shared.card.LocalVideoCardPreferences
import com.fedoratube.org.ui.components.shared.card.channelAvatarUrls
import com.fedoratube.org.ui.components.shared.card.isWatchedProgress
import com.fedoratube.org.ui.components.shared.card.rememberCardTitle
import com.fedoratube.org.ui.components.shared.card.rememberCollaboratorChannelDisplayName
import com.fedoratube.org.ui.components.shared.card.rememberCollaboratorItems
import com.fedoratube.org.ui.components.shared.card.rememberWatchProgress
import com.fedoratube.org.ui.components.shared.collaboratorRows
import com.fedoratube.org.ui.components.shared.rememberVideoShareAction
import com.fedoratube.org.utils.youtubeWatchUrl
import kotlinx.coroutines.launch

private enum class VideoMenuPage { Actions, Details, Collaborators, Save }

/**
 * A video's menu. [title] and [thumbnailUrl] are what the card shows, so a DeArrow title stays the
 * same in the menu. [showChannel] is false where the screen already is the channel. A playlist that
 * can drop the video passes [onRemoveFromCollection] with its [removeFromCollectionLabel] and icon;
 * it is the last row of the Options group. [whyShown] says why a feed shows
 * the video, on the Details page. [onPlayNow] puts Play now first in the Playback group, for a video
 * that is not open yet, such as one shared to Flow.
 */
@Composable
fun VideoQuickActionsBottomSheet(
    video: Video,
    onDismiss: () -> Unit,
    title: String = rememberCardTitle(video),
    thumbnailUrl: String = video.thumbnailUrl,
    showChannel: Boolean = true,
    whyShown: String? = null,
    onRemoveFromCollection: (() -> Unit)? = null,
    removeFromCollectionLabel: String? = null,
    removeFromCollectionIcon: ImageVector = Icons.Outlined.PlaylistRemove,
    onPlayNow: (() -> Unit)? = null,
    viewModel: QuickActionsViewModel = sharedQuickActionsViewModel(),
) {
    var page by rememberSaveable(video.id) { mutableStateOf(VideoMenuPage.Actions) }
    val collaborators = rememberCollaboratorItems(video)
    val toActions = { page = VideoMenuPage.Actions }

    if (page == VideoMenuPage.Save) {
        SaveVideoSheet(video = video, onDismiss = onDismiss)
        return
    }

    QuickActionsSheet(
        onDismiss = onDismiss,
        page = page,
        onBack = if (page == VideoMenuPage.Actions) null else toActions,
    ) { sheet ->
        val close = sheet::close
        when (page) {
            VideoMenuPage.Details -> {
                QuickActionsPageHeader(title = stringResource(R.string.details_metadata), onBack = toActions, onClose = close)
                MediaDetailsPage(subject = video.toDetailsSubject(title, whyShown))
            }

            VideoMenuPage.Collaborators -> {
                QuickActionsPageHeader(title = stringResource(R.string.collaborators), onBack = toActions, onClose = close)
                QuickActionsGroup(title = null, rows = collaboratorRows(collaborators, onOpened = close, viewModel = viewModel))
            }

            else -> {
                VideoPrimaryActions(video, viewModel, onSave = { sheet.hideThen { page = VideoMenuPage.Save } }, onDismiss = close)

                Spacer(modifier = Modifier.height(16.dp))

                val download = downloadRow(video, viewModel, close)
                val removeRow =
                    if (onRemoveFromCollection != null && removeFromCollectionLabel != null) {
                        actionRow("remove", removeFromCollectionIcon, removeFromCollectionLabel, destructive = true) {
                            onRemoveFromCollection()
                            close()
                        }
                    } else {
                        null
                    }
                QuickActionsGroup(
                    title = null,
                    rows = listOfNotNull(download, removeRow),
                )

                QuickActionsGroup(title = stringResource(R.string.section_algorithm), rows = feedRows(video, viewModel, close))
            }
        }
    }
}

@Composable
private fun VideoPrimaryActions(
    video: Video,
    viewModel: QuickActionsViewModel,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val watchLaterIds by viewModel.watchLaterIds.collectAsStateWithLifecycle()
    val share = rememberVideoShareAction()
    QuickActionsPrimaryGroup(
        listOf(
            QuickPrimaryAction(icon = Icons.AutoMirrored.Outlined.PlaylistAdd, label = stringResource(R.string.save), onClick = onSave),
            QuickPrimaryAction(
                icon = Icons.Outlined.WatchLater,
                checkedIcon = Icons.Filled.WatchLater,
                label = stringResource(R.string.watch_later),
                checked = video.id in watchLaterIds,
                onClick = { viewModel.toggleWatchLater(video) },
            ),
            QuickPrimaryAction(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.share),
                onClick = {
                    share(video.id, video.title, video.isShort)
                    onDismiss()
                },
            ),
        ),
    )
}



@Composable
private fun feedRows(
    video: Video,
    viewModel: QuickActionsViewModel,
    onDismiss: () -> Unit,
): List<QuickActionRow> {
    val haptics = LocalHapticFeedback.current
    val isWatched = isWatchedProgress(rememberWatchProgress(video.id))
    return listOf(
        actionRow("interested", Icons.Outlined.ThumbUp, stringResource(R.string.i_like_this)) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.markAsInteresting(video)
            onDismiss()
        },
        actionRow("not_interested", Icons.Outlined.ThumbDown, stringResource(R.string.not_interested)) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            viewModel.markNotInterested(video)
            onDismiss()
        },
        QuickActionRow("watched") { shape ->
            FlowNavRow(
                title = stringResource(if (isWatched) R.string.mark_as_unwatched else R.string.mark_as_watched),
                leadingIcon = if (isWatched) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    if (isWatched) viewModel.markAsUnwatched(video) else viewModel.markAsWatched(video)
                    onDismiss()
                },
                enabled = true,
                showChevron = false,
                shape = shape,
            )
        },
        actionRow(
            key = "block",
            icon = Icons.Outlined.Block,
            title = stringResource(R.string.dont_show_channel),
            supporting = stringResource(R.string.dont_show_channel_desc),
            destructive = true,
        ) {
            viewModel.blockChannel(video)
            onDismiss()
        },
    )
}

@Composable
private fun downloadRow(
    video: Video,
    viewModel: QuickActionsViewModel,
    onDismiss: () -> Unit,
): QuickActionRow {
    val downloadedIds by viewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val isDownloaded = video.id in downloadedIds
    val downloadedLabel = stringResource(R.string.downloaded)
    return QuickActionRow("download") { shape ->
        FlowNavRow(
            title = if (isDownloaded) downloadedLabel else stringResource(R.string.download),
            leadingIcon = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Outlined.Download,
            onClick = {
                viewModel.requestDownload(video)
                onDismiss()
            },
            enabled = !isDownloaded,
            showChevron = false,
            stateDescription = if (isDownloaded) downloadedLabel else null,
            shape = shape,
        )
    }
}

/** A row that runs an action and closes the menu, so it carries no chevron. */
internal fun actionRow(
    key: String,
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    destructive: Boolean = false,
    onClick: () -> Unit,
): QuickActionRow =
    QuickActionRow(key) { shape ->
        FlowNavRow(
            title = title,
            supportingText = supporting,
            leadingIcon = icon,
            onClick = onClick,
            showChevron = false,
            destructive = destructive,
            shape = shape,
        )
    }

private fun Video.toDetailsSubject(
    displayTitle: String,
    whyShown: String?,
) = MediaDetailsSubject(
    videoId = id,
    title = displayTitle,
    author = channelName,
    channelId = channelId,
    viewCount = viewCount,
    likeCount = likeCount,
    uploadDate = uploadDate,
    timestamp = timestamp,
    timestampIsExact = timestampIsExact,
    durationSeconds = duration,
    whyShown = whyShown,
)
