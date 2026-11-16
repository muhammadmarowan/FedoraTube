package com.fedoratube.org.ui.screens.settings.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.fedoratube.org.R
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.data.update.UpdateEntryPoint
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.screens.settings.index.AboutIndex
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class AboutDialog { CHANGELOG, DEVICE, NOTICES }

private const val WEBSITE_URL = "https://github.com/muhammadmarowan/FedoraTube"
private const val GITHUB_URL = "https://github.com/muhammadmarowan/FedoraTube"
private const val REDDIT_URL = "https://github.com/muhammadmarowan/FedoraTube/discussions"
private const val CREATOR_URL = "https://github.com/muhammadmarowan"
private const val NEWPIPE_URL = "https://github.com/TeamNewPipe/NewPipeExtractor"
private const val LICENSE_URL = "https://github.com/muhammadmarowan/FedoraTube/blob/main/License"
private const val CREATOR_AVATAR_URL = "https://github.com/muhammadmarowan.png?size=144"
private const val REPORT_EMAIL = "muhammadmarowan2007@gmail.com"

private val LogoSize = 72.dp
private val HeaderPadding = 24.dp
private val HeaderSpacing = 8.dp
private val AvatarSize = 48.dp

/** The app's version, where to find and reach the project, and its licences. */
@Composable
internal fun AboutScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onOpenUpdate: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { PlayerPreferences(context) }
    val isBetaEnabled by preferences.betaProgramEnabled.collectAsState(initial = false)
    var dialog by rememberSaveable { mutableStateOf<AboutDialog?>(null) }
    val open = { url: String -> runCatching { uriHandler.openUri(url) } }

    SettingsPage(
        title = stringResource(R.string.settings_item_about_flow),
        onBack = onBack,
        highlight = highlight,
    ) {
        item("about.header") { AboutHeader() }
        group(key = "about.beta") {
            row("about.beta_program") { shape ->
                BetaProgramRow(
                    isBeta = isBetaEnabled,
                    onToggle = { enabled ->
                        scope.launch {
                            preferences.setBetaProgramEnabled(enabled)
                            if (enabled) {
                                Toast.makeText(context, "Joined Beta Program! Checking for pre-releases...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Switched to Stable channel", Toast.LENGTH_SHORT).show()
                            }
                            launch(Dispatchers.IO) {
                                runCatching {
                                    EntryPointAccessors.fromApplication(context.applicationContext, UpdateEntryPoint::class.java)
                                        .updateRepository().fetch(forceBeta = enabled)
                                }
                            }
                        }
                    },
                    onOpenUpdate = onOpenUpdate,
                    shape = shape,
                )
            }
        }
        group(key = "about.creator") {
            row(AboutIndex.creator.key) { shape -> CreatorRow(shape = shape, onClick = { open(CREATOR_URL) }) }
        }
        group(key = "about.app", header = R.string.section_app) {
            nav(AboutIndex.changelog, icon = Icons.Outlined.History, showChevron = false, onClick = { dialog = AboutDialog.CHANGELOG })
        }
        group(key = "about.contact", header = R.string.section_contact) {
            nav(AboutIndex.website, icon = Icons.Outlined.Public, showChevron = false, onClick = { open(WEBSITE_URL) })
            nav(AboutIndex.github, iconRes = R.drawable.ic_github, showChevron = false, onClick = { open(GITHUB_URL) })
            nav(AboutIndex.reddit, icon = IconReddit, showChevron = false, onClick = { open(REDDIT_URL) })
        }
        group(key = "about.report", header = R.string.section_report) {
            nav(AboutIndex.report, icon = Icons.Outlined.Email, showChevron = false, onClick = { openReport(context) })
        }
        group(key = "about.legal", header = R.string.section_legal) {
            nav(AboutIndex.license, icon = Icons.Outlined.Description, showChevron = false, onClick = { open(LICENSE_URL) })
            nav(AboutIndex.newPipe, icon = Icons.Outlined.Extension, showChevron = false, onClick = { open(NEWPIPE_URL) })
            nav(AboutIndex.notices, icon = Icons.Outlined.Gavel, showChevron = false, onClick = { dialog = AboutDialog.NOTICES })
        }
        group(key = "about.device", header = R.string.section_device) {
            nav(
                AboutIndex.deviceInfo,
                value = "${Build.MANUFACTURER} ${Build.MODEL}",
                icon = Icons.Outlined.Smartphone,
                showChevron = false,
                onClick = { dialog = AboutDialog.DEVICE },
            )
        }
    }

    when (dialog) {
        AboutDialog.CHANGELOG -> ChangelogSheet(onDismiss = { dialog = null })
        AboutDialog.DEVICE -> DeviceInfoDialog(onDismiss = { dialog = null })
        AboutDialog.NOTICES -> NoticesDialog(onDismiss = { dialog = null })
        null -> Unit
    }
}

/** Hands the report straight to the viewer's email client, addressed to the developer. */
internal fun openReport(context: Context) {
    val intent =
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$REPORT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.report_email_subject))
        }
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, R.string.report_no_email_app, Toast.LENGTH_SHORT).show() }
}

@Composable
private fun AboutHeader() {
    val context = LocalContext.current
    val unknown = stringResource(R.string.unknown)
    val version =
        remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }
                .getOrNull()
                ?.let { it.versionName to it.longVersionCode.toString() }
        }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = HeaderPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeaderSpacing),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_notification_logo),
            contentDescription = null,
            modifier = Modifier.size(LogoSize),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.v_version_template, version?.first ?: unknown, version?.second ?: "0"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreatorRow(
    shape: Shape,
    onClick: () -> Unit,
) {
    var avatarLoaded by remember { mutableStateOf(false) }
    SegmentedListItem(
        onClick = onClick,
        verticalAlignment = Alignment.CenterVertically,
        shapes = ListItemDefaults.shapes(shape = shape),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        leadingContent = {
            Box(
                modifier = Modifier.size(AvatarSize).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarLoaded) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AsyncImage(
                    model = CREATOR_AVATAR_URL,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { avatarLoaded = true },
                    modifier = Modifier.matchParentSize(),
                )
            }
        },
        overlineContent = { Text(stringResource(R.string.about_creator)) },
        supportingContent = { Text(stringResource(R.string.settings_creator_role)) },
        trailingContent = { Icon(painterResource(R.drawable.ic_github), contentDescription = null) },
    ) {
        Text(stringResource(R.string.about_creator_name), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BetaProgramRow(
    isBeta: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenUpdate: () -> Unit,
    shape: Shape,
) {
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                .size(44.dp)
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
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Beta Program",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                                    "Joined · Receiving GitHub pre-release updates with newest features."
                                } else {
                                    "Stable Channel · Receiving official public releases only."
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

            OutlinedButton(
                onClick = onOpenUpdate,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(40.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check for Updates",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}
