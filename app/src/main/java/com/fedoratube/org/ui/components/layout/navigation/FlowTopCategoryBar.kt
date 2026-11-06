package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Full-screen YouTube-style swipe: drag left = next category, drag right = previous. */
@Composable
fun rememberCategorySwipe(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onCategorySelected: (FlowTab) -> Unit,
    enabled: Boolean = true,
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }
    if (!enabled || tabs.size < 2) return Modifier
    return Modifier.pointerInput(tabs, selectedTab, thresholdPx) {
        var totalDragPx = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount -> totalDragPx += dragAmount },
            onDragEnd = {
                val dragPx = totalDragPx
                totalDragPx = 0f
                if (dragPx < -thresholdPx) {
                    tabs.getOrNull(tabs.indexOf(selectedTab) + 1)?.let(onCategorySelected)
                } else if (dragPx > thresholdPx) {
                    tabs.getOrNull(tabs.indexOf(selectedTab) - 1)?.let(onCategorySelected)
                }
            },
        )
    }
}

/**
 * YouTube-style top chips under the app wordmark: Home, Subscriptions, Music.
 * Selecting a chip switches the root destination; re-selecting the active chip
 * scrolls that screen to the top via [onCategorySelected].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowTopCategoryBar(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onCategorySelected: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size < 2) return
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs, key = { it.id }) { tab ->
            FilterChip(
                selected = tab == selectedTab,
                onClick = { onCategorySelected(tab) },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}
