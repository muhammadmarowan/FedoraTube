package com.fedoratube.org.ui.screens.settings.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.R
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.recommendation.FlowPersona
import com.fedoratube.org.data.update.UpdateEntryPoint
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
import com.fedoratube.org.ui.screens.settings.about.openReport
import com.fedoratube.org.ui.screens.settings.index.AboutIndex
import com.fedoratube.org.ui.screens.settings.index.DestinationIndex
import com.fedoratube.org.ui.screens.settings.index.HomeIndex
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    val onDeepFlowChange: (Boolean) -> Unit,
    val onDurationClick: () -> Unit,
    val onSaveHistoryChange: (Boolean) -> Unit,
    val onScrobbleChange: (Boolean) -> Unit,
    val onShowOnSearchChange: (Boolean) -> Unit,
    val onCheckForUpdates: () -> Unit,
    val onFocusModeChange: (Boolean) -> Unit = {},
    val onAutoClearCacheClick: () -> Unit = {},
    val onToggleBetaProgram: (Boolean) -> Unit = {},
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
        page(SettingsDestination.TASTE)
    }
    group(key = "home.watching", header = R.string.settings_group_watching) {
        page(SettingsDestination.PLAYBACK)
        page(SettingsDestination.MUSIC_PLAYER)
        page(SettingsDestination.QUALITY)
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
    group(key = "home.about", header = R.string.settings_header_about) {
        page(SettingsDestination.ABOUT)
        row("home.about.beta_program") { shape ->
            val context = LocalContext.current
            val preferences = remember(context) { PlayerPreferences(context) }
            val isBetaEnabled by preferences.betaProgramEnabled.collectAsState(initial = true)
            BetaProgramSettingsRow(
                isBeta = isBetaEnabled,
                onToggle = { enabled ->
                    if (enabled) {
                        Toast.makeText(context, "Joined Beta Program! Checking for pre-releases...", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Switched to Stable channel! Checking for stable releases...", Toast.LENGTH_SHORT).show()
                    }
                    actions.onToggleBetaProgram(enabled)
                },
                shape = shape,
            )
        }
        if (BuildConfig.UPDATER_ENABLED) {
            nav(
                HomeIndex.checkForUpdates,
                onClick = actions.onCheckForUpdates,
                value = state.updateCheckingLabel,
                enabled = state.updateCheckingLabel == null,
                showChevron = false,
                icon = HomeRowIcons.Updates,
            )
        }
        row(AboutIndex.report.key) { shape ->
            val context = LocalContext.current
            FlowNavRow(
                title = stringResource(AboutIndex.report.title),
                supportingText = AboutIndex.report.summary?.let { stringResource(it) },
                onClick = { openReport(context) },
                leadingIcon = Icons.Outlined.Email,
                showChevron = false,
                shape = shape,
            )
        }
    }
}

@Composable
private fun BetaProgramSettingsRow(
    isBeta: Boolean,
    onToggle: (Boolean) -> Unit,
    shape: Shape,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isBeta) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isBeta) Icons.Rounded.Science else Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = if (isBeta) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Beta Program",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color =
                                if (isBeta) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                },
                        ) {
                            Text(
                                text = if (isBeta) "BETA" else "STABLE",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    ),
                                color =
                                    if (isBeta) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text =
                            if (isBeta) {
                                "Receiving GitHub pre-release updates with newest features."
                            } else {
                                "Receiving official stable releases only."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isBeta,
                onCheckedChange = onToggle,
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
