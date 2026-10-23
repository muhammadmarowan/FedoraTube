package com.fedoratube.org.ui.screens.settings.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.R
import com.fedoratube.org.data.recommendation.FlowPersona
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.ui.components.settings.SettingsGroupScope
import com.fedoratube.org.ui.components.settings.SettingsListScope
import com.fedoratube.org.ui.components.settings.SettingsTarget
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.components.settings.switch
import com.fedoratube.org.ui.components.shared.DeepFlowState
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowSwitchRow
import com.fedoratube.org.ui.components.shared.deepFlowStatus
import com.fedoratube.org.ui.screens.settings.index.DestinationIndex
import com.fedoratube.org.ui.screens.settings.index.HomeIndex

@Immutable
internal data class SettingsHomeState(
    val selected: SettingsDestination?,
    val persona: FlowPersona?,
    val deepFlow: DeepFlowState,
    /** Shown on the update row while a check the user started is running; null when idle. */
    val updateCheckingLabel: String?,
    val isFocusModeEnabled: Boolean = false,
    val autoClearCacheHours: Int = 12,
)

internal class SettingsHomeActions(
    val onOpen: (SettingsTarget) -> Unit,
    val onOpenDonations: () -> Unit,
    val onDeepFlowChange: (Boolean) -> Unit,
    val onDurationClick: () -> Unit,
    val onSaveHistoryChange: (Boolean) -> Unit,
    val onScrobbleChange: (Boolean) -> Unit,
    val onShowOnSearchChange: (Boolean) -> Unit,
    val onCheckForUpdates: () -> Unit,
    val onFocusModeChange: (Boolean) -> Unit = {},
    val onAutoClearCacheClick: () -> Unit = {},
)

/** The settings list when no search is running. */
internal fun SettingsListScope.homeContent(
    state: SettingsHomeState,
    actions: SettingsHomeActions,
) {
    fun SettingsGroupScope.page(destination: SettingsDestination) =
        nav(
            entry = DestinationIndex.entry(destination),
            onClick = { actions.onOpen(SettingsTarget(destination)) },
            icon = destinationIcon(destination),
            selected = state.selected == destination,
            showChevron = state.selected == null,
        )

    group(key = "home.engine", header = R.string.settings_flow_engine_header) {
        page(SettingsDestination.TASTE)
        row(HomeIndex.focusMode.key) { shape ->
            FlowSwitchRow(
                title = stringResource(HomeIndex.focusMode.title),
                supportingText = stringResource(HomeIndex.focusMode.summary ?: R.string.focus_mode_subtitle),
                checked = state.isFocusModeEnabled,
                onCheckedChange = actions.onFocusModeChange,
                leadingIcon = Icons.Outlined.CenterFocusStrong,
                shape = shape,
            )
        }
        page(SettingsDestination.TOPICS)
    }
    group(key = "home.watching", header = R.string.settings_group_watching) {
        page(SettingsDestination.QUALITY)
    }
    group(key = "home.movies", header = R.string.settings_group_movies) {
        page(SettingsDestination.ADDONS)
    }
    group(key = "home.data", header = R.string.settings_group_data) {
        page(SettingsDestination.BACKUP)
        page(SettingsDestination.HISTORY)
        row("home.data.auto_clear_cache") { shape ->
            FlowNavRow(
                title = stringResource(R.string.auto_clear_cache_title),
                supportingText = autoClearCacheLabel(state.autoClearCacheHours),
                onClick = actions.onAutoClearCacheClick,
                leadingIcon = Icons.Outlined.CleaningServices,
                shape = shape,
            )
        }
    }
    group(key = "home.system", header = R.string.settings_group_system) {
        page(SettingsDestination.NOTIFICATIONS)
        page(SettingsDestination.WELLBEING)
        page(SettingsDestination.LANGUAGE_REGION)
    }
    if (BuildConfig.UPDATER_ENABLED) {
        group(key = "home.about", header = R.string.settings_header_about) {
            nav(
                HomeIndex.checkForUpdates,
                onClick = actions.onCheckForUpdates,
                value = state.updateCheckingLabel,
                enabled = state.updateCheckingLabel == null,
                showChevron = false,
                icon = HomeRowIcons.Updates,
            )
        }
    }
}

@Composable
internal fun autoClearCacheLabel(hours: Int): String =
    when (hours) {
        0 -> stringResource(R.string.auto_clear_cache_disabled)
        3 -> stringResource(R.string.auto_clear_cache_3h)
        6 -> stringResource(R.string.auto_clear_cache_6h)
        12 -> stringResource(R.string.auto_clear_cache_12h)
        24 -> stringResource(R.string.auto_clear_cache_24h)
        72 -> stringResource(R.string.auto_clear_cache_3d)
        168 -> stringResource(R.string.auto_clear_cache_7d)
        else -> "$hours hours"
    }
