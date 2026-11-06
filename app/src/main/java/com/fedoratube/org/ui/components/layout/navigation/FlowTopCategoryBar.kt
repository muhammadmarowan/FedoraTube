package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
 * Borderless pills (not boxed/outlined): selected = solid onSurface, rest =
 * surfaceVariant — like YouTube's top filters. Selecting a chip switches the
 * root destination; re-selecting the active chip scrolls that screen to the
 * top via [onCategorySelected]. The row hides on scroll together with the
 * top bar when [scrollBehavior] is in use.
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
    AnimatedVisibility(
        visible = !collapsed,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(tabs, key = { it.id }) { tab ->
                val selected = tab == selectedTab
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            )
                            .clickable { onCategorySelected(tab) }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (selected) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }
        }
    }
}
