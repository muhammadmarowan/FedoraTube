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
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.screens.settings.index.AboutIndex

private enum class AboutDialog { DEVICE, NOTICES }

private const val WEBSITE_URL = "https://github.com/muhammadmarowan/FedoraTube"
private const val CREATOR_URL = "https://github.com/muhammadmarowan"
private const val NEWPIPE_URL = "https://github.com/TeamNewPipe/NewPipeExtractor"
private const val LICENSE_URL = "https://github.com/muhammadmarowan/FedoraTube/blob/main/License"
private const val CREATOR_AVATAR_URL = "https://github.com/muhammadmarowan.png?size=144"
private const val REPORT_EMAIL = "muhammadmarowan2007@gmail.com"

private val LogoSize = 76.dp
private val AvatarSize = 48.dp

/** Redesigned modern About screen for FedoraTube. */
@Composable
internal fun AboutScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onOpenUpdate: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    var dialog by rememberSaveable { mutableStateOf<AboutDialog?>(null) }
    val open = { url: String -> runCatching { uriHandler.openUri(url) } }

    SettingsPage(
        title = stringResource(R.string.settings_item_about_flow),
        onBack = onBack,
        highlight = highlight,
    ) {
        item("about.header") { AboutHeader() }

        group(key = "about.creator") {
            row(AboutIndex.creator.key) { shape ->
                CreatorCard(shape = shape, onClick = { open(CREATOR_URL) })
            }
        }

        group(key = "about.contact", header = R.string.section_contact) {
            nav(AboutIndex.website, icon = Icons.Outlined.Public, showChevron = false, onClick = { open(WEBSITE_URL) })
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
    val versionName =
        remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }
                .getOrNull()
                ?.versionName ?: unknown
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shadowElevation = 6.dp,
            modifier = Modifier.size(LogoSize),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_logo),
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        ) {
            Text(
                text = "v$versionName",
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreatorCard(
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
                modifier =
                    Modifier
                        .size(AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarLoaded) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        overlineContent = {
            Text(
                text = stringResource(R.string.about_creator),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        supportingContent = {
            Text(
                text = stringResource(R.string.settings_creator_role),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(36.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_github),
                        contentDescription = "GitHub",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    ) {
        Text(
            text = stringResource(R.string.about_creator_name),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}
