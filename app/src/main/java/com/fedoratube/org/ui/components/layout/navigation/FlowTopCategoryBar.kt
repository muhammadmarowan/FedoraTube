package com.fedoratube.org.ui.components.layout.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

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
