package com.fedoratube.org.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fedoratube.org.R
import com.fedoratube.org.notification.BackgroundWorkPolicy
import com.fedoratube.org.ui.components.shared.FlowChoice
import com.fedoratube.org.ui.components.shared.FlowChoiceDialog
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowSegmentedGap
import com.fedoratube.org.ui.components.shared.FlowSwitchRow
import com.fedoratube.org.ui.components.shared.flowRowGroupShape
import com.fedoratube.org.utils.RegionCatalog

private val FootnotePadding = 16.dp

@Composable
internal fun AlertsStep(
    header: LazyListScope.() -> Unit,
    newVideoAlerts: Boolean,
    onNewVideoAlertsChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    // A new user lands here before background checks can ever run on time, so the exemption is
    // asked for as the page opens — once per onboarding; stepping back never re-prompts.
    promptBackgroundOnEntry: Boolean,
    onBackgroundPromptShown: () -> Unit,
    // Trending region: the same preference Settings writes, offered here so a new user picks
    // their country before ever opening the Explore/Trending tab.
    trendingRegion: String,
    onTrendingRegionChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val asksPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var backgroundAllowed by remember { mutableStateOf(BackgroundWorkPolicy.isBackgroundWorkUnrestricted(context)) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }
    var showRegionDialog by rememberSaveable { mutableStateOf(false) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsAllowed = granted
            askedOnce = true
        }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = notificationsAllowed(context)
        backgroundAllowed = BackgroundWorkPolicy.isBackgroundWorkUnrestricted(context)
    }
    LaunchedEffect(Unit) {
        if (promptBackgroundOnEntry && !BackgroundWorkPolicy.isBackgroundWorkUnrestricted(context)) {
            onBackgroundPromptShown()
            BackgroundWorkPolicy.requestUnrestrictedBackgroundWork(context)
        }
    }
    val allowedText = stringResource(R.string.onboarding_alerts_allowed)
    val rowCount = if (asksPermission) 4 else 3
    val offset = if (asksPermission) 1 else 0

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
        header()
        item(key = "rows") {
            Column(verticalArrangement = Arrangement.spacedBy(FlowSegmentedGap)) {
                if (asksPermission) {
                    FlowNavRow(
                        title = stringResource(R.string.onboarding_alerts_allow),
                        supportingText =
                            if (notificationsAllowed) {
                                allowedText
                            } else {
                                stringResource(
                                    R.string.onboarding_alerts_allow_summary,
                                )
                            },
                        leadingIcon = Icons.Outlined.Notifications,
                        showChevron = !notificationsAllowed,
                        shape = flowRowGroupShape(0, rowCount),
                        onClick = {
                            when {
                                notificationsAllowed -> Unit
                                askedOnce -> openNotificationSettings(context)
                                else -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
                FlowSwitchRow(
                    title = stringResource(R.string.onboarding_alerts_new_videos),
                    supportingText = stringResource(R.string.onboarding_alerts_new_videos_summary),
                    leadingIcon = Icons.Outlined.NewReleases,
                    checked = newVideoAlerts,
                    onCheckedChange = onNewVideoAlertsChange,
                    shape = flowRowGroupShape(offset, rowCount),
                )
                FlowNavRow(
                    title = stringResource(R.string.onboarding_alerts_background),
                    supportingText = if (backgroundAllowed) allowedText else stringResource(R.string.onboarding_alerts_background_summary),
                    leadingIcon = Icons.Outlined.BatterySaver,
                    showChevron = !backgroundAllowed,
                    shape = flowRowGroupShape(offset + 1, rowCount),
                    onClick = { if (!backgroundAllowed) BackgroundWorkPolicy.requestUnrestrictedBackgroundWork(context) },
                )
                FlowNavRow(
                    title = stringResource(R.string.settings_item_region),
                    supportingText = RegionCatalog.displayName(trendingRegion),
                    leadingIcon = Icons.Outlined.Public,
                    shape = flowRowGroupShape(offset + 2, rowCount),
                    onClick = { showRegionDialog = true },
                )
            }
        }
        item(key = "footnote") {
            Text(
                text = stringResource(R.string.onboarding_alerts_footnote),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(FootnotePadding),
            )
        }
    }

    if (showRegionDialog) {
        val regionOptions = remember { RegionCatalog.sorted().map { (code, name) -> FlowChoice(code, name) } }
        FlowChoiceDialog(
            title = stringResource(R.string.settings_region_dialog_title),
            options = regionOptions,
            selected = trendingRegion,
            onSelect = { code ->
                onTrendingRegionChange(code)
                showRegionDialog = false
            },
            onDismiss = { showRegionDialog = false },
        )
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** After a refusal Android stops showing the prompt, so the app's notification page is the only way on. */
private fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
