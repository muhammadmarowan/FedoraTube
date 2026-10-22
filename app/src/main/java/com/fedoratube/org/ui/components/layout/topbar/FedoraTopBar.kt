package com.fedoratube.org.ui.components.layout.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.fedoratube.org.R

/**
 * Which screens show the app-shell actions ([FlowGlobalActions]).
 */
enum class FlowGlobalActionsMode {
    Auto,
    Always,
    None,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FedoraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    globalActions: FlowGlobalActionsMode = FlowGlobalActionsMode.Auto,
    globalActionsShowSettings: Boolean = false,
    windowInsets: WindowInsets = FlowTopBarDefaults.WindowInsets,
) {
    FedoraTopBar(
        modifier = modifier,
        title = {
            FedoraTopBarTitle(title = title, subtitle = subtitle)
        },
        onBack = onBack,
        leading = leading,
        actions = actions,
        globalActions = globalActions,
        globalActionsShowSettings = globalActionsShowSettings,
        windowInsets = windowInsets,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FedoraTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    globalActions: FlowGlobalActionsMode = FlowGlobalActionsMode.Auto,
    globalActionsShowSettings: Boolean = false,
    windowInsets: WindowInsets = FlowTopBarDefaults.WindowInsets,
) {
    val showGlobalActions =
        when (globalActions) {
            FlowGlobalActionsMode.Always -> true
            FlowGlobalActionsMode.None -> false
            FlowGlobalActionsMode.Auto -> onBack == null
        }

    TopAppBar(
        modifier = modifier,
        title = title,
        navigationIcon = {
            when {
                leading != null -> {
                    leading()
                }

                onBack != null -> {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                        )
                    }
                }
            }
        },
        actions = {
            actions()
            if (showGlobalActions) {
                FlowGlobalActionsRow(showSettings = globalActionsShowSettings)
            }
        },
        colors = FlowTopBarDefaults.colors(),
        windowInsets = windowInsets,
    )
}

@Composable
fun FlowTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    globalActions: FlowGlobalActionsMode = FlowGlobalActionsMode.Auto,
    globalActionsShowSettings: Boolean = false,
    windowInsets: WindowInsets = FlowTopBarDefaults.WindowInsets,
) = FedoraTopBar(title, modifier, subtitle, onBack, leading, actions, globalActions, globalActionsShowSettings, windowInsets)

@Composable
fun FlowTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    globalActions: FlowGlobalActionsMode = FlowGlobalActionsMode.Auto,
    globalActionsShowSettings: Boolean = false,
    windowInsets: WindowInsets = FlowTopBarDefaults.WindowInsets,
) = FedoraTopBar(title, modifier, onBack, leading, actions, globalActions, globalActionsShowSettings, windowInsets)

@Composable
private fun FedoraTopBarTitle(
    title: String,
    subtitle: String?,
) {
    if (subtitle == null) {
        Text(
            text = title,
            style = FlowTopBarDefaults.titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else {
        Column {
            Text(
                text = title,
                style = FlowTopBarDefaults.titleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = FlowTopBarDefaults.subtitleStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

