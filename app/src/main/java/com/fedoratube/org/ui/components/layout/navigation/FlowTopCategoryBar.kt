package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Category swipe modifier for the tabs row only (cheap — no full-screen pointer input). */
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
    val collapsed by remember(scrollBehavior) {
        derivedStateOf { (scrollBehavior?.state?.contentOffset ?: 0f) < -8f }
    }
    // Fade-only hide to avoid expand/shrink layout thrash on every scroll frame (lag).
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 72.dp.toPx() }
    AnimatedVisibility(
        visible = !collapsed,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        LazyRow(
            modifier = categorySwipe(tabs, selectedTab, onCategorySelected, swipeThresholdPx),
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
