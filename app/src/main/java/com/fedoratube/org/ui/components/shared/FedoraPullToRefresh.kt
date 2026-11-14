package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Modern YouTube-style pull-to-refresh container with the official Material 3 circular elevated indicator
 * and smooth progress spinner, sliding smoothly from below the top app bar and cleanly retracting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.pullToRefresh(
                isRefreshing = isRefreshing,
                state = state,
                enabled = enabled,
                onRefresh = onRefresh,
            ),
        contentAlignment = contentAlignment,
    ) {
        content()
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = Color(0xFF282828),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FedoraPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    enabled: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) = FlowPullToRefreshBox(isRefreshing, onRefresh, modifier, state, enabled, contentAlignment, content)

