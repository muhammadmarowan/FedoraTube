package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fedoratube.org.ui.utils.LocalWindowSizeClass
import com.fedoratube.org.ui.utils.isExpandedWidth
import com.fedoratube.org.ui.utils.isMediumHeight

const val FLOW_NAV_BAR_TAG = "flow_nav_bar"
const val FLOW_NAV_RAIL_TAG = "flow_nav_rail"

object FlowNavigationDefaults {
    /**
     * The bar's height above the system navigation inset: Material's short navigation bar
     * container. The bar reports its real height, which grows past this at large font scales.
     */
    val BarHeight: Dp = 64.dp
}

/**
 * Whether this window is wide enough to navigate from the start edge instead of the bottom. The
 * decision belongs to the window's size class, so a phone in a narrow split window keeps the bar
 * even on a large display.
 */
@Composable
fun flowUsesNavigationRail(): Boolean = LocalWindowSizeClass.current.let { it.isExpandedWidth && it.isMediumHeight }

/**
 * The app's primary navigation around [content]: the bottom bar over it on compact and medium
 * windows, the wide navigation rail beside it once the window is expanded.
 *
 * The bar overlays the content, so callers reserve [onBarHeightChanged] themselves. The rail takes
 * its own column; [onRailWidthChanged] is only for overlays drawn outside this layout.
 *
 * [showGestureScrim] paints the gesture (home indicator) strip in the page's background colour
 * over the content: the bar covers that strip while it is shown, and without this the content
 * would slide under the white home line the moment the bar hides on scroll — YouTube keeps that
 * strip opaque so nothing ever appears below the line.
 */
@Composable
fun FlowNavigationChrome(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    barVisible: Boolean,
    railVisible: Boolean,
    modifier: Modifier = Modifier,
    showGestureScrim: Boolean = false,
    onBarHeightChanged: (Dp) -> Unit = {},
    onRailWidthChanged: (Dp) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val usesRail = flowUsesNavigationRail()
    val showRail = usesRail && railVisible
    val density = LocalDensity.current
    val railInsets = WideNavigationRailDefaults.windowInsets

    Row(modifier = modifier.fillMaxSize()) {
        if (showRail) {
            FlowNavigationRail(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                modifier =
                    Modifier
                        .testTag(FLOW_NAV_RAIL_TAG)
                        .onSizeChanged { onRailWidthChanged(with(density) { it.width.toDp() }) },
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (showRail) {
                            Modifier.consumeWindowInsets(railInsets.only(WindowInsetsSides.Start))
                        } else {
                            Modifier
                        },
                    ),
        ) {
            content()
            if (!usesRail) {
                if (showGestureScrim) {
                    // Opaque gesture (home indicator) strip over the content: while the bar is
                    // shown it sits under the bar, and once the bar hides on scroll it keeps the
                    // band below the white home line clear, the way YouTube does.
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                                .background(MaterialTheme.colorScheme.background),
                    )
                }
                NavigationBarSlot(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    visible = barVisible,
                    onHeightChanged = onBarHeightChanged,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.NavigationBarSlot(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    visible: Boolean,
    onHeightChanged: (Dp) -> Unit,
) {
    val density = LocalDensity.current
    val bottomInsetPx = ShortNavigationBarDefaults.windowInsets.getBottom(density)
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(animationSpec = androidx.compose.animation.core.tween(durationMillis = 240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it },
        exit = slideOutVertically(animationSpec = androidx.compose.animation.core.tween(durationMillis = 200, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { it },
    ) {
        FlowNavigationBar(
            tabs = tabs,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier =
                Modifier
                    .testTag(FLOW_NAV_BAR_TAG)
                    .onSizeChanged { onHeightChanged(with(density) { (it.height - bottomInsetPx).toDp() }) },
        )
    }
}
