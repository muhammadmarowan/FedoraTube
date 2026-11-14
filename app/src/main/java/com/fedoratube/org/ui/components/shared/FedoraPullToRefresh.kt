package com.fedoratube.org.ui.components.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Modern YouTube-style pull-to-refresh container with a sleek, elevated circular floating indicator
 * and smooth circular progress spinner.
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

        val distanceFraction = state.distanceFraction
        val showIndicator = isRefreshing || distanceFraction > 0.05f

        val animatedScale by animateFloatAsState(
            targetValue = if (showIndicator) 1f else 0.4f,
            animationSpec = tween(150),
            label = "ptrScale",
        )
        val animatedAlpha by animateFloatAsState(
            targetValue = if (showIndicator) 1f else 0f,
            animationSpec = tween(150),
            label = "ptrAlpha",
        )

        if (showIndicator) {
            val yOffset = ((distanceFraction.coerceIn(0f, 1.25f) * 44f) + if (isRefreshing) 16f else 0f).dp
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = yOffset)
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
                        }
                        .size(38.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 4.dp,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        CircularProgressIndicator(
                            progress = { (distanceFraction * 0.75f).coerceIn(0.1f, 0.85f) },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
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
