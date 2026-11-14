@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fedoratube.org.ui.screens.update

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.R
import com.fedoratube.org.data.update.AppRelease
import com.fedoratube.org.data.update.AppVersions
import com.fedoratube.org.data.update.GitHubRelease
import com.fedoratube.org.data.update.UpdateFailure
import com.fedoratube.org.ui.components.layout.topbar.FlowTopBar
import com.fedoratube.org.ui.components.shared.FlowEmptyState
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.FlowLoadingIndicator
import com.fedoratube.org.ui.components.shared.FlowMaxContentWidth
import com.fedoratube.org.ui.components.shared.FlowSectionHeader
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ContentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
private val HeaderSpacing = 16.dp
private val FactSpacing = 8.dp
private val NoticePadding = 16.dp

/**
 * Redesigned, modern Check for Updates screen with sleek card hierarchy,
 * clean version transitions, rich release notes, and GitHub release history.
 */
@Composable
fun UpdateScreen(
    onClose: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    val release = state.release

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            FlowTopBar(
                title = stringResource(R.string.check_for_updates),
                onBack = onClose,
                actions = {
                    IconButton(onClick = viewModel::retryFetch) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                    if (release != null) {
                        UpdateMenu(
                            onSkip = {
                                viewModel.skip()
                                onClose()
                            },
                            onOpenGitHub = { uriHandler.openUri(release.pageUrl) },
                        )
                    } else {
                        IconButton(onClick = { uriHandler.openUri("https://github.com/muhammadmarowan/FedoraTube/releases") }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = stringResource(R.string.update_open_github),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (release != null) {
                UpdateActionBar(
                    stage = state.stage,
                    onPrimary = {
                        if (state.stage == UpdateStage.NeedsPermission) {
                            runCatching { context.startActivity(viewModel.installPermissionIntent()) }
                        } else {
                            viewModel.primaryAction()
                        }
                    },
                    onSecondary = {
                        if (state.stage is UpdateStage.Downloading) viewModel.cancelDownload() else onClose()
                    },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                state.loading -> {
                    FlowLoadingIndicator()
                }

                state.fetchFailed && state.allReleases.isEmpty() && release == null -> {
                    FlowErrorState(
                        error = stringResource(R.string.update_check_failed),
                        onRetry = viewModel::retryFetch,
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = FlowMaxContentWidth).fillMaxWidth(),
                        contentPadding = ContentPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (release != null) {
                            item(key = "update_hero") {
                                UpdateAvailableHeroCard(
                                    release = release,
                                    currentVersion = state.currentVersion,
                                )
                            }
                            item(key = "notice") { StageNotice(state.stage) }
                            state.notes?.let { releaseNotes(it) }
                        } else {
                            item(key = "uptodate_hero") {
                                UpToDateHeroCard(
                                    currentVersion = state.currentVersion,
                                    onCheckAgain = viewModel::retryFetch,
                                )
                            }
                        }

                        if (state.allReleases.isNotEmpty()) {
                            item(key = "all_releases_header") {
                                FlowSectionHeader(
                                    text = "All Releases",
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                )
                            }
                            items(
                                items = state.allReleases,
                                key = { "rel-${it.tagName}" },
                            ) { rel ->
                                ReleaseHistoryCard(rel, state.currentVersion, uriHandler)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpToDateHeroCard(
    currentVersion: String,
    onCheckAgain: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E3A2F)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(42.dp),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "fTube is up to date",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You are currently running the latest version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50)),
                    )
                    Text(
                        text = "Installed: v$currentVersion",
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = onCheckAgain,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check again")
            }
        }
    }
}

@Composable
private fun UpdateAvailableHeroCard(
    release: AppRelease,
    currentVersion: String,
) {
    val context = LocalContext.current
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, Color(0xFF3EA6FF).copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3EA6FF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = Color(0xFF3EA6FF),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = "New Update Available",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3EA6FF).copy(alpha = 0.15f),
                ) {
                    Text(
                        text = "NEW",
                        style =
                            MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3EA6FF),
                            ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // Version transition badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        text = "v${AppVersions.normalize(currentVersion)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF3EA6FF),
                    modifier = Modifier.size(16.dp),
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF3EA6FF).copy(alpha = 0.2f),
                ) {
                    Text(
                        text = "v${release.version}",
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3EA6FF),
                            ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // Meta chips row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(FactSpacing),
                verticalArrangement = Arrangement.spacedBy(FactSpacing),
            ) {
                release.apk?.sizeBytes?.takeIf { it > 0 }?.let {
                    ReleaseFact(
                        Icons.Outlined.Download,
                        Formatter.formatShortFileSize(context, it),
                    )
                }
                release.publishedAt?.let {
                    ReleaseFact(
                        Icons.Outlined.CalendarToday,
                        it.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseHistoryCard(
    release: GitHubRelease,
    currentVersion: String,
    uriHandler: UriHandler,
) {
    val isCurrent = AppVersions.normalize(release.tagName) == AppVersions.normalize(currentVersion)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = release.tagName,
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E3A2F),
                        ) {
                            Text(
                                text = "Current",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50),
                                    ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                release.publishedAt?.take(10)?.let { dateStr ->
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!release.body.isNullOrBlank()) {
                Text(
                    text = release.body.trim(),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun StageNotice(stage: UpdateStage) {
    val (icon, text) =
        when (stage) {
            UpdateStage.NeedsPermission -> Icons.Outlined.Shield to R.string.update_permission_notice
            is UpdateStage.Failed -> Icons.Outlined.ErrorOutline to stage.reason.messageRes()
            else -> return
        }
    val failed = stage is UpdateStage.Failed
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (failed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(bottom = HeaderSpacing),
    ) {
        Row(
            modifier = Modifier.padding(NoticePadding),
            horizontalArrangement = Arrangement.spacedBy(HeaderSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun UpdateFailure.messageRes(): Int =
    when (this) {
        UpdateFailure.NETWORK -> R.string.update_failed_network
        UpdateFailure.STORAGE -> R.string.update_failed_storage
        UpdateFailure.CHECKSUM -> R.string.update_failed_checksum
        UpdateFailure.PACKAGE -> R.string.update_failed_package
        UpdateFailure.INSTALL -> R.string.update_failed_install
    }

@Composable
private fun UpdateMenu(
    onSkip: () -> Unit,
    onOpenGitHub: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.update_skip_version)) },
                leadingIcon = { Icon(Icons.Outlined.NotificationsOff, contentDescription = null) },
                onClick = {
                    open = false
                    onSkip()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.update_open_github)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null) },
                onClick = {
                    open = false
                    onOpenGitHub()
                },
            )
        }
    }
}
