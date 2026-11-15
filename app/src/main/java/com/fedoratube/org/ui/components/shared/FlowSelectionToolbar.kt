package com.fedoratube.org.ui.components.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets
import com.fedoratube.org.ui.components.layout.floatAboveBottomChrome

/** One button in a select mode's toolbar. */
class FlowSelectionAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

/**
 * The actions for what is selected in a list, floating over its bottom edge so they stay
 * under the thumb and don't depend on how wide the top bar is.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlowSelectionToolbar(
    visible: Boolean,
    summary: String,
    actions: List<FlowSelectionAction>,
    modifier: Modifier = Modifier,
) {
    val bottomInsets = LocalFlowBottomInsets.current
    AnimatedVisibility(
        visible = visible,
        modifier =
            modifier
                .floatAboveBottomChrome(bottomInsets)
                .padding(bottom = FloatingToolbarDefaults.ScreenOffset),
        enter =
            slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight * 2 },
                animationSpec =
                    spring(
                        dampingRatio = 0.82f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
            ) + fadeIn(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)),
        exit =
            slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight * 2 },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
    ) {
        // Inverse, like a snackbar: the standard container is the page's surface and the vibrant one
        // matches a selected row in several palettes, so either would melt into the list under it.
        HorizontalFloatingToolbar(
            expanded = true,
            colors =
                FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = MaterialTheme.colorScheme.inverseSurface,
                    toolbarContentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ),
            modifier = Modifier,
            leadingContent = {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            },
        ) {
            actions.forEach { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = LocalContentColor.current,
                    )
                }
            }
        }
    }
}
