package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R

private val ChipSpacing = 8.dp

/**
 * Whether the chips above [listState]'s comments are on screen.
 *
 * The row reads fine at the top and gets in the way once the thread is under the thumb, so it
 * steps aside while the list scrolls down and comes back on the way up or at the top. Positions
 * are tracked outside the scroll too, so comments loading or a reply expanding cannot masquerade
 * as a scroll and hide the row unasked.
 */
@Composable
fun rememberCommentSortChipsVisible(listState: LazyListState): Boolean {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
            )
        }.collect { (scrolling, index, offset) ->
            val previousIndex = lastIndex
            val previousOffset = lastOffset
            lastIndex = index
            lastOffset = offset
            if (!scrolling) return@collect
            val scrolledDown =
                if (index == previousIndex) offset > previousOffset else index > previousIndex
            visible = !scrolledDown
        }
    }
    return visible
}

/**
 * The orders a comment section offers, and the one filter the app adds on top.
 *
 * Top and Newest are the two orders YouTube serves, each behind its own continuation. Oldest has no
 * server equivalent, so it reverses the chronological pages already loaded. Timed keeps only the
 * comments whose text points at a position in this video.
 */
@Composable
fun CommentSortChips(
    selected: CommentSortFilter,
    onSelect: (CommentSortFilter) -> Unit,
    modifier: Modifier = Modifier,
    timedOnly: Boolean = false,
    onTimedChange: ((Boolean) -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
    ) {
        CommentSortFilter.entries.forEach { filter ->
            FlowFilterChip(
                label = stringResource(filter.labelRes),
                selected = selected == filter,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSelect(filter)
                },
            )
        }
        if (onTimedChange != null) {
            FlowFilterChip(
                label = stringResource(R.string.filter_timed),
                selected = timedOnly,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onTimedChange(!timedOnly)
                },
            )
        }
    }
}
