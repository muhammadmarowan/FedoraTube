package com.fedoratube.org.ui.components.layout.navigation

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.ui.utils.LocalWindowSizeClass
import com.fedoratube.org.ui.utils.isMediumWidth

private const val MAX_BAR_TABS = 5

/**
 * The bottom navigation bar. YouTube-style: icon-only, no pill indicator,
 * selected icon morphs outline → filled with a smooth cross-fade + scale.
 * Material allows at most five destinations in a bar, so a sixth and
 * seventh enabled tab move behind a "More" item; the rail has room for all of them.
 */
@Composable
internal fun FlowNavigationBar(
    tabs: List<FlowTab>,
    selectedTab: FlowTab?,
    onTabSelected: (FlowTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalItems = LocalWindowSizeClass.current.isMediumWidth
    val iconPosition = if (horizontalItems) NavigationItemIconPosition.Start else NavigationItemIconPosition.Top
    val barTabs = if (tabs.size <= MAX_BAR_TABS) tabs else tabs.take(MAX_BAR_TABS - 1)
    val overflowTabs = tabs.drop(barTabs.size)

    ShortNavigationBar(
        modifier = modifier,
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
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = tab.icon(selected = false),
            contentDescription = stringResource(tab.labelRes),
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = 1f - progress
                        scaleX = 1f - 0.08f * progress
                        scaleY = 1f - 0.08f * progress
                    }
                    .size(24.dp),
        )
        Icon(
            imageVector = tab.icon(selected = true),
            contentDescription = null,
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = progress
                        scaleX = 0.88f + 0.12f * progress
                        scaleY = 0.88f + 0.12f * progress
                    }
                    .size(24.dp),
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
            icon = { Icon(imageVector = Icons.Filled.MoreHoriz, contentDescription = stringResource(R.string.nav_more)) },
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
