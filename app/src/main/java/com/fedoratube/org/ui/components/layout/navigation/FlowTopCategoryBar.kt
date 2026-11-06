package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Swipe on any content: drag left = next category, drag right = previous (Home→Subs→Music). */
fun categorySwipe(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onCategorySelected: (FlowTab) -> Unit,
    thresholdPx: Float,
): Modifier =
    Modifier.pointerInput(tabs, selectedTab, thresholdPx) {
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

@Composable
fun rememberCategorySwipe(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onCategorySelected: (FlowTab) -> Unit,
): Modifier {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }
    return categorySwipe(tabs, selectedTab, onCategorySelected, thresholdPx)
}

/**
 * YouTube-style top tabs under the app wordmark: Home, Subscriptions, Music.
 * Plain text tabs with a thick theme-blue underline on the selected tab
 * (YouTube's red line, themed here) — no pill/box backgrounds. Selecting a
 * tab switches the root destination; re-selecting the active tab scrolls
 * that screen to the top via [onCategorySelected]. The row hides on scroll
 * together with the top bar when [scrollBehavior] is in use.
 */
@Composable
fun FlowTopCategoryBar(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onCategorySelected: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    if (tabs.size < 2) return
    // Unified header: the tabs collapse in exact sync with the wordmark bar.
    // fraction = 1 expanded, 0 collapsed; height + alpha follow it so
    // one shows only when the other shows and one hides only when the other hides.
    val limit = scrollBehavior?.state?.heightOffsetLimit ?: 0f
    val fraction =
        if (scrollBehavior == null || limit == 0f) {
            1f
        } else {
            (1f - (scrollBehavior.state.heightOffset / limit)).coerceIn(0f, 1f)
        }
    if (fraction <= 0.01f) return
    val fullHeight = 46.dp
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(fullHeight * fraction)
                .alpha(fraction)
                .clip(androidx.compose.ui.graphics.RectangleShape),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(tabs, key = { it.id }) { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier =
                        Modifier
                            .clickable { onCategorySelected(tab) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.titleSmall,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Box(
                        modifier =
                            Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    },
                                ),
                    ) {
                        Box(modifier = Modifier.width(28.dp).height(3.dp))
                    }
                }
            }
        }
    }
}
