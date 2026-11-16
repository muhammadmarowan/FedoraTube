/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.components.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.innertube.pages.channel.ChannelFilterGroup
import com.fedoratube.org.ui.components.shared.FlowDropdownFilterChip
import com.fedoratube.org.ui.components.shared.FlowFilterChip

/**
 * Modern YouTube-style horizontal filter chips row placed directly below the channel tab row.
 * Gives 1-tap switching between Latest / Popular / Oldest and other channel tab filters.
 */
@Composable
fun ChannelFilterChipBar(
    groups: List<ChannelFilterGroup>,
    selection: List<Int>,
    onFilterSelected: (groupIndex: Int, optionIndex: Int) -> Unit,
    onOpenSortSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validGroups = groups.filter { it.options.isNotEmpty() }
    if (validGroups.isEmpty()) return

    val defaultSortLabel = stringResource(R.string.filter_sort_by)

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val primaryGroup = groups.firstOrNull { it.options.isNotEmpty() } ?: return@LazyRow
        val primaryIndex = groups.indexOf(primaryGroup)
        val selectedIndex = selection.getOrElse(primaryIndex) { -1 }
        val activeIndex =
            when {
                selectedIndex >= 0 -> selectedIndex
                primaryGroup.marksFirstByDefault -> 0
                else -> -1
            }

        // If the primary filter has <= 4 options (e.g. Latest, Popular, Oldest), render direct chips
        if (primaryGroup.options.size <= 4) {
            items(primaryGroup.options.size) { optIndex ->
                val option = primaryGroup.options[optIndex]
                val isSelected = optIndex == activeIndex
                FlowFilterChip(
                    label = option.label,
                    selected = isSelected,
                    onClick = { onFilterSelected(primaryIndex, optIndex) },
                )
            }
        } else {
            // For longer lists (e.g. Playlists sort), show a dropdown chip that opens the bottom sheet
            val activeLabel = primaryGroup.options.getOrNull(activeIndex)?.label ?: defaultSortLabel
            item {
                FlowDropdownFilterChip(
                    label = activeLabel,
                    selected = true,
                    onClick = onOpenSortSheet,
                )
            }
        }

        // Additional filter groups (e.g. visibility, members only)
        groups.forEachIndexed { groupIndex, group ->
            if (groupIndex != primaryIndex && group.options.isNotEmpty()) {
                val groupSelIndex = selection.getOrElse(groupIndex) { -1 }
                val groupActiveIndex =
                    when {
                        groupSelIndex >= 0 -> groupSelIndex
                        group.marksFirstByDefault -> 0
                        else -> -1
                    }
                items(group.options.size) { optIndex ->
                    val option = group.options[optIndex]
                    val isSelected = optIndex == groupActiveIndex
                    FlowFilterChip(
                        label = option.label,
                        selected = isSelected,
                        onClick = { onFilterSelected(groupIndex, optIndex) },
                    )
                }
            }
        }
    }
}
