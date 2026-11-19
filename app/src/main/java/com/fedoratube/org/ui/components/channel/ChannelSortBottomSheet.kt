/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.components.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.R
import com.fedoratube.org.innertube.pages.channel.ChannelFilterGroup
import com.fedoratube.org.ui.components.shared.FlowCenteredPopupDialog
import com.fedoratube.org.ui.theme.extendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSortBottomSheet(
    groups: List<ChannelFilterGroup>,
    selection: List<Int>,
    onFilterSelected: (groupIndex: Int, optionIndex: Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val dropdowns = groups.withIndex().filter { it.value.isDropdown || it.value.options.isNotEmpty() }
    if (dropdowns.isEmpty()) return

    FlowCenteredPopupDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
        ) {
            val mainTitle = dropdowns.firstNotNullOfOrNull { it.value.title } ?: stringResource(R.string.filter_sort_by)
            Text(
                text = mainTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )

            dropdowns.forEachIndexed { position, (groupIndex, group) ->
                if (position > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
                val groupTitle = group.title
                if (dropdowns.size > 1 && groupTitle != null && groupTitle != mainTitle) {
                    Text(
                        text = groupTitle,
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    )
                }

                val selectedIndex = selection.getOrElse(groupIndex) { -1 }
                val activeIndex =
                    when {
                        selectedIndex >= 0 -> selectedIndex
                        group.marksFirstByDefault -> 0
                        else -> -1
                    }

                group.options.forEachIndexed { optIndex, option ->
                    val isActive = optIndex == activeIndex
                    Surface(
                        onClick = {
                            onFilterSelected(groupIndex, optIndex)
                            onDismissRequest()
                        },
                        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Spacer(modifier = Modifier.size(20.dp))
                            }

                            Text(
                                text = option.label,
                                style =
                                    MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.extendedColors.textSecondary,
                                    ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}
