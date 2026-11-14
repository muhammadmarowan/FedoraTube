package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.R
import com.fedoratube.org.ui.utils.LocalWindowSizeClass
import com.fedoratube.org.ui.utils.isMediumWidth

private const val MAX_BAR_TABS = 5

/** Modern YouTube icon size: 24 dp with clear label underneath. */
private val NavIconSize = 24.dp

/** Unselected tabs sit back subtly so the selected one reads with high clarity. */
private const val UNSELECTED_ICON_ALPHA = 0.70f

/**
 * Modern YouTube-style bottom navigation bar with subtle top divider, icon + label layout,
 * smooth spring transitions, and refined elevation.
 */
@Composable
internal fun FedoraNavigationBar(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalItems = LocalWindowSizeClass.current.isMediumWidth
    val iconPosition = if (horizontalItems) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top
    val barTabs = if (tabs.size <= MAX_BAR_TABS) tabs else tabs.take(MAX_BAR_TABS - 1)
    val overflowTabs = tabs.drop(barTabs.size)
    val topBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    ShortNavigationBar(
        modifier =
            modifier.drawBehind {
                drawLine(
                    color = topBorderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
        containerColor = MaterialTheme.colorScheme.surface,
        arrangement =
            if (horizontalItems) ShortNavigationBarArrangement.Centered else ShortNavigationBarArrangement.EqualWeight,
    ) {
        barTabs.forEach { tab ->
            val selected = tab == selectedTab
            ShortNavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = { AnimatedNavIcon(tab = tab, selected = selected) },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                iconPosition = iconPosition,
                colors =
                    ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
                        selectedTextColorStartIconPosition = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        selectedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
            )
        }
        if (overflowTabs.isNotEmpty()) {
            OverflowItem(
                tabs = overflowTabs,
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                iconPosition = iconPosition,
            )
        }
    }
}

@Composable
internal fun FlowNavigationBar(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
) = FedoraNavigationBar(tabs, selectedTab, onTabSelected, modifier)

@Composable
private fun AnimatedNavIcon(
    tab: FlowTab,
    selected: Boolean,
) {
    val progress by
        animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            label = "navIconMorph",
        )
    // The icon that just won lands oversized and springs back to rest, so selecting a tab
    // reads as a pop rather than a quiet swap of two glyphs.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(0.84f)
            pop.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            pop.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
        }
    }
    Box(
        modifier = Modifier.size(NavIconSize),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.icon(selected = false),
            contentDescription = stringResource(tab.labelRes),
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = (UNSELECTED_ICON_ALPHA * (1f - progress)).coerceIn(0f, 1f)
                        scaleX = 1f - 0.10f * progress
                        scaleY = 1f - 0.10f * progress
                    }
                    .size(NavIconSize),
        )
        Icon(
            imageVector = tab.icon(selected = true),
            contentDescription = null,
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = progress.coerceIn(0f, 1f)
                        val scale = (0.84f + 0.16f * progress) * pop.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .size(NavIconSize),
        )
    }
}

@Composable
private fun OverflowItem(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    iconPosition: NavigationItemIconPosition,
) {
    var expanded by remember { mutableStateOf(false) }
    // One Box per bar slot: the bar measures every direct child as an item, and the menu's
    // popup anchor would otherwise count as one.
    Box(propagateMinConstraints = true) {
        ShortNavigationBarItem(
            selected = selectedTab in tabs,
            onClick = { expanded = true },
            icon = {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = stringResource(R.string.nav_more),
                    modifier = Modifier.size(NavIconSize),
                )
            },
            label = null,
            iconPosition = iconPosition,
            colors =
                ShortNavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColorTopIconPosition = MaterialTheme.colorScheme.onSurface,
                    selectedTextColorStartIconPosition = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                    selectedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                tabs.forEachIndexed { index, tab ->
                    DropdownMenuItem(
                        selected = tab == selectedTab,
                        onClick = {
                            expanded = false
                            onTabSelected(tab)
                        },
                        text = { Text(text = stringResource(tab.labelRes)) },
                        shapes = MenuDefaults.itemShape(index = index, count = tabs.size),
                        leadingIcon = {
                            Icon(
                                imageVector = tab.icon(selected = false),
                                contentDescription = null,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            )
                        },
                        selectedLeadingIcon = {
                            Icon(
                                imageVector = tab.icon(selected = true),
                                contentDescription = null,
                                modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                            )
                        },
                    )
                }
            }
        }
    }
}
