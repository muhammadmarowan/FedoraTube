package com.fedoratube.org.ui.components.channel

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MenuDefaults
import androidx.compose.ui.unit.sp
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
    onOpenSortSheet: (() -> Unit)? = null,
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

                ChannelSortButton(
                    groups = filterGroups,
                    selection = filterSelection,
                    onFilterSelected = onFilterSelected,
                    onOpenSortSheet = onOpenSortSheet,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp),
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
                ChannelSortButton(
                    groups = filterGroups,
                    selection = filterSelection,
                    onFilterSelected = onFilterSelected,
                    onOpenSortSheet = onOpenSortSheet,
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
 * The tab's filter/sort trigger button in the channel header/banner.
 * Redesigned into a modern glassmorphism pill badge showing the active sort mode,
 * opening the sleek [ChannelSortBottomSheet] on tap.
 */
@Composable
private fun ChannelSortButton(
    groups: List<ChannelFilterGroup>,
    selection: List<Int>,
    onFilterSelected: ((groupIndex: Int, optionIndex: Int) -> Unit)?,
    onOpenSortSheet: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val dropdowns = groups.withIndex().filter { it.value.isDropdown || it.value.options.isNotEmpty() }
    if (dropdowns.isEmpty() || onFilterSelected == null) return

    var openSheet by remember { mutableStateOf(false) }

    val (primaryGroupIndex, primaryGroup) = dropdowns.first()
    val selectedIndex = selection.getOrElse(primaryGroupIndex) { -1 }
    val activeIndex =
        when {
            selectedIndex >= 0 -> selectedIndex
            primaryGroup.marksFirstByDefault -> 0
            else -> -1
        }
    val activeLabel = primaryGroup.options.getOrNull(activeIndex)?.label

    Surface(
        onClick = {
            if (onOpenSortSheet != null) {
                onOpenSortSheet()
            } else {
                openSheet = true
            }
        },
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            if (!activeLabel.isNullOrBlank()) {
                Text(
                    text = activeLabel,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        ),
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.channel_sort_open),
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp),
            )
        }
    }

    if (openSheet) {
        ChannelSortBottomSheet(
            groups = groups,
            selection = selection,
            onFilterSelected = { g, o ->
                openSheet = false
                onFilterSelected(g, o)
            },
            onDismissRequest = { openSheet = false },
        )
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
