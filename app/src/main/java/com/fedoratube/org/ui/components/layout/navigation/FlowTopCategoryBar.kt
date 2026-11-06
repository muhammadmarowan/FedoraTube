package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset as ComposeIntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
 * The underline is a single pill that spring-slides between tabs (YouTube-style)
 * instead of popping in place.
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
    val density = LocalDensity.current
    val indicatorWidthPx = with(density) { 28.dp.toPx() }
    val indicatorHeight = 3.dp
    // tab id -> distance (px) from bar left to indicator left
    val indicatorLeft = remember { mutableStateMapOf<Int, Float>() }
    var barRootX by remember { mutableFloatStateOf(0f) }
    val targetLeft = indicatorLeft[selectedTab?.id] ?: 0f
    val animatedLeft by
        animateFloatAsState(
            targetValue = targetLeft,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            label = "categoryIndicatorLeft",
        )

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(fullHeight * fraction)
                .alpha(fraction)
                .onGloballyPositioned { barRootX = it.positionInRoot().x }
                .clip(androidx.compose.ui.graphics.RectangleShape),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier =
                        Modifier
                            .clickable { onCategorySelected(tab) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .onGloballyPositioned { coordinates ->
                                val centerX = coordinates.positionInRoot().x + coordinates.size.width / 2f
                                indicatorLeft[tab.id] = centerX - barRootX - indicatorWidthPx / 2f
                            },
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                    // Keep the same visual footprint as the old per-tab box so the row height doesn't jump.
                    Box(modifier = Modifier.padding(top = 6.dp).width(28.dp).height(indicatorHeight))
                }
            }
        }
        // Sliding indicator (single pill, YouTube-style)
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset {
                        ComposeIntOffset(animatedLeft.roundToInt(), 0)
                    }
                    .padding(bottom = 8.dp)
                    .width(28.dp)
                    .height(indicatorHeight)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary),
        )
    }
}
