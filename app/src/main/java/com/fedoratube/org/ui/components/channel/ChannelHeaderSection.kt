package com.fedoratube.org.ui.components.channel

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.innertube.pages.channel.ChannelFilterGroup
import com.fedoratube.org.innertube.pages.channel.ChannelHeader
import com.fedoratube.org.ui.components.shared.ChannelAvatarImage
import com.fedoratube.org.ui.components.shared.FlowNoteCard
import com.fedoratube.org.ui.components.shared.FlowSubscribeButton
import com.fedoratube.org.ui.components.shared.FullSizeImageDialog
import com.fedoratube.org.ui.theme.extendedColors
import com.fedoratube.org.utils.ThumbnailUrlResolver
import com.fedoratube.org.utils.copyPlainText
import kotlinx.coroutines.launch

@Composable
internal fun ChannelHeaderSection(
    header: ChannelHeader,
    isSubscribed: Boolean,
    isNotificationsEnabled: Boolean,
    onSubscribeClick: () -> Unit,
    onUnsubscribeClick: () -> Unit,
    onNotificationChange: (Boolean) -> Unit,
    onManageGroups: (() -> Unit)?,
    note: String?,
    onEditNote: (() -> Unit)?,
    // Focus mode hides the whole subscribe system on channel pages.
    showSubscribe: Boolean = true,
    // The tab's sort/filter dropdown (e.g. Latest/Popular/Oldest) shown at the banner's top-right.
    filterGroups: List<ChannelFilterGroup> = emptyList(),
    filterSelection: List<Int> = emptyList(),
    onFilterSelected: ((groupIndex: Int, optionIndex: Int) -> Unit)? = null,
) {
    val bannerUrl = remember(header.bannerUrl) { ThumbnailUrlResolver.resolveChannelBanner(header.bannerUrl, targetWidth = 2048) }
    var showFullSizeAvatar by remember(header.id) { mutableStateOf(false) }

    if (showFullSizeAvatar && header.avatarUrl.isNotEmpty()) {
        FullSizeImageDialog(
            imageUrl = header.avatarUrl,
            onDismiss = { showFullSizeAvatar = false },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background),
    ) {
        if (!bannerUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ChannelBanner(imageUrl = bannerUrl)

                ChannelSortArrow(
                    groups = filterGroups,
                    selection = filterSelection,
                    onFilterSelected = onFilterSelected,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp),
                )

                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(y = 38.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ChannelAvatarImage(
                        url = header.avatarUrl,
                        contentDescription = stringResource(R.string.channel_avatar),
                        modifier =
                            Modifier
                                .size(76.dp)
                                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = header.avatarUrl.isNotEmpty()) { showFullSizeAvatar = true },
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (showSubscribe) {
                        FlowSubscribeButton(
                            isSubscribed = isSubscribed,
                            isNotificationsEnabled = isNotificationsEnabled,
                            onSubscribeClick = onSubscribeClick,
                            onUnsubscribeClick = onUnsubscribeClick,
                            onNotificationChange = onNotificationChange,
                            onManageGroups = onManageGroups,
                            onAddNote = onEditNote,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ChannelAvatarImage(
                    url = header.avatarUrl,
                    contentDescription = stringResource(R.string.channel_avatar),
                    modifier =
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(enabled = header.avatarUrl.isNotEmpty()) { showFullSizeAvatar = true },
                )

                Spacer(modifier = Modifier.weight(1f))

                if (showSubscribe) {
                    FlowSubscribeButton(
                        isSubscribed = isSubscribed,
                        isNotificationsEnabled = isNotificationsEnabled,
                        onSubscribeClick = onSubscribeClick,
                        onUnsubscribeClick = onUnsubscribeClick,
                        onNotificationChange = onNotificationChange,
                        onManageGroups = onManageGroups,
                        onAddNote = onEditNote,
                    )
                }
                ChannelSortArrow(
                    groups = filterGroups,
                    selection = filterSelection,
                    onFilterSelected = onFilterSelected,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            ChannelName(header.title)

            val metadata = remember(header) { listOfNotNull(header.handle, header.subscriberCountText, header.videoCountText) }
            if (metadata.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    metadata.forEach { entry ->
                        Text(
                            text = entry,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extendedColors.textSecondary,
                        )
                    }
                }
            }

            if (!note.isNullOrBlank() && onEditNote != null) {
                FlowNoteCard(
                    text = note,
                    onEdit = onEditNote,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * The tab's filter menu (Latest / Popular / Oldest… and the visibility chips, Members only & the
 * like) as one arrow in the banner's top-right corner. [groups] is the tab's full filter list:
 * every group is listed here — a chip row under the tabs would only duplicate it — with a divider
 * where one control ends and the next begins. "Latest" is the server's default and stays selected
 * until the viewer picks something else.
 */
@Composable
private fun ChannelSortArrow(
    groups: List<ChannelFilterGroup>,
    selection: List<Int>,
    onFilterSelected: ((groupIndex: Int, optionIndex: Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dropdowns = groups.withIndex().filter { it.value.isDropdown }
    if (dropdowns.isEmpty() || onFilterSelected == null) return

    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { open = true },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.channel_sort_open),
                tint = Color.White,
                modifier = Modifier.size(22.dp).align(Alignment.Center),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            dropdowns.forEachIndexed { position, (groupIndex, group) ->
                if (position > 0) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                group.title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
                    )
                }
                val selectedIndex = selection.getOrElse(groupIndex) { -1 }
                val activeIndex =
                    when {
                        selectedIndex >= 0 -> selectedIndex
                        group.marksFirstByDefault -> 0
                        else -> -1
                    }
                group.options.forEachIndexed { index, option ->
                    val isActive = index == activeIndex
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            open = false
                            onFilterSelected(groupIndex, index)
                        },
                        leadingIcon =
                            if (isActive) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}

// Subscribe button

/**
 * The channel's name, copied on long-press. Not a text selection: the header slides away as the
 * page scrolls, which would leave selection handles floating.
 */
@Composable
private fun ChannelName(title: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val clipLabel = stringResource(R.string.channel_name_clip_label)
    val copyLabel = stringResource(R.string.copy_channel_name)

    fun copy() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            clipboard.copyPlainText(clipLabel, title)
            Toast.makeText(context, context.getString(R.string.channel_name_copied), Toast.LENGTH_SHORT).show()
        }
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .pointerInput(title) { detectTapGestures(onLongPress = { copy() }) }
                .semantics {
                    onLongClick(label = copyLabel) {
                        copy()
                        true
                    }
                },
    )
}
