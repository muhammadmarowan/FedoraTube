package com.fedoratube.org.ui.components.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fedoratube.org.innertube.pages.channel.ChannelFilterGroup
import com.fedoratube.org.ui.components.shared.FlowFilterChip

/**
 * A tab's chip filter bar. The sort dropdown is not rendered here — it lives in the banner's
 * top-right arrow. What is left here is the chips YouTube emits (Members only & the like), or an
 * empty bar worth hiding entirely.
 */
@Composable
internal fun ChannelFilterBar(
    filterGroups: List<ChannelFilterGroup>,
    selected: List<Int>,
    onFilterSelected: (groupIndex: Int, optionIndex: Int) -> Unit,
) {
    val hasChips = filterGroups.any { !it.isDropdown }
    if (!hasChips) return

    // One lazy item per control: a group emitted as a single item would draw its chips with no
    // gap, because the row's spacing only separates items.
    val controls =
        remember(filterGroups) {
            filterGroups.flatMapIndexed { groupIndex, group ->
                if (group.isDropdown) {
                    emptyList()
                } else {
                    group.options.indices.map { optionIndex -> groupIndex to optionIndex }
                }
            }
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = controls,
                    key = { (groupIndex, optionIndex) -> "f_${groupIndex}_$optionIndex" },
                ) { (groupIndex, optionIndex) ->
                    val group = filterGroups[groupIndex]
                    val chosen = selected.getOrElse(groupIndex) { -1 }
                    val option = group.options[optionIndex]
                    FlowFilterChip(
                        label = option.label,
                        selected = optionIndex == chosen,
                        onClick = { onFilterSelected(groupIndex, optionIndex) },
                    )
                }
            }
        }
    }
}


