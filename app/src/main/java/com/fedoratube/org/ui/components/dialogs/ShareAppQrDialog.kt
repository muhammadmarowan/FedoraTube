package com.fedoratube.org.ui.components.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.R
import com.fedoratube.org.data.update.parseGitHubReleases
import com.fedoratube.org.ui.screens.sync.QrCodeImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** The stable link, used only when no release can be reached at all. */
private const val APP_DOWNLOAD_URL = "https://github.com/muhammadmarowan/FedoraTube/releases/latest/download/app-github-universal-release.apk"
private const val RELEASES_API = "https://api.github.com/repos/muhammadmarowan/FedoraTube/releases"
private const val UNIVERSAL_APK_ASSET = "app-github-universal-release.apk"
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val APK_CACHE_DIR = "apk"

private data class LatestRelease(
    val tag: String,
    val url: String,
)

/** Resolved once per process so reopening the dialog does not ask GitHub again. */
@Volatile
private var cachedRelease: LatestRelease? = null

/**
 * The newest published release — prereleases included, because GitHub's `/releases/latest` link
 * skips them and that is exactly how FedoraTube updates ship — and the universal APK it carries.
 */
private fun fetchLatestRelease(): LatestRelease? =
    runCatching {
        val connection = URL(RELEASES_API).openConnection() as HttpURLConnection
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
        val release = parseGitHubReleases(body).firstOrNull { !it.draft } ?: return@runCatching null
        val apk =
            release.assets.firstOrNull { it.name == UNIVERSAL_APK_ASSET }
                ?: release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.name.contains("universal", ignoreCase = true) }
        LatestRelease(
            tag = release.tagName,
            url = apk?.downloadUrl?.takeIf { it.isNotBlank() } ?: APP_DOWNLOAD_URL,
        )
    }.getOrNull()?.takeIf { it.tag.isNotBlank() }

/**
 * Copies the installed APK (and any split APKs beside it) into the FileProvider's cache folder and
 * returns content URIs for the Sharesheet. Nearby/Quick Share then carries the file over to the
 * other phone with no internet on either side.
 */
private suspend fun prepareApkUris(context: Context): List<Uri> =
    withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val sources =
            buildList {
                app.applicationInfo.sourceDir?.let { add(File(it)) }
                app.applicationInfo.splitSourceDirs?.forEach { add(File(it)) }
            }.filter(File::exists)
        if (sources.isEmpty()) return@withContext emptyList()
        val apkDir = File(app.cacheDir, APK_CACHE_DIR).apply { mkdirs() }
        val names = sources.map(File::getName).toSet()
        val shared =
            sources.map { source ->
                val target = File(apkDir, source.name)
                if (!target.exists() || target.length() != source.length()) {
                    source.copyTo(target, overwrite = true)
                } else {
                    target
                }
            }
        // Copies of older builds would pile up in the cache otherwise.
        apkDir.listFiles()?.forEach { file -> if (file.name !in names) file.delete() }
        shared.map { FileProvider.getUriForFile(app, "${app.packageName}.files", it) }
    }

private fun startShare(
    context: Context,
    uris: List<Uri>,
) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = APK_MIME_TYPE
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            putExtra(Intent.EXTRA_SUBJECT, "FedoraTube ${BuildConfig.VERSION_NAME}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    val chooser =
        Intent
            .createChooser(send, context.getString(R.string.share_app_qr_share_chooser))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(chooser) }
        .onFailure { Toast.makeText(context, R.string.share_app_qr_share_failed, Toast.LENGTH_SHORT).show() }
}

/**
 * Share sheet for the app itself: the newest-release QR under the app's badge, and a Quick Share
 * button that hands the installed APK to the system Sharesheet for an offline transfer.
 */
@Composable
fun ShareAppQrDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadUrl by remember { mutableStateOf(APP_DOWNLOAD_URL) }
    var latestTag by remember { mutableStateOf<String?>(null) }
    var resolved by remember { mutableStateOf(false) }
    var preparing by remember { mutableStateOf(false) }

    // The QR must always carry the newest build, so ask GitHub for the newest release — including
    // prereleases — every time the dialog opens, showing the static link only until the answer lands.
    LaunchedEffect(Unit) {
        cachedRelease?.let { release ->
            downloadUrl = release.url
            latestTag = release.tag
            resolved = true
        }
        withContext(Dispatchers.IO) { fetchLatestRelease() }?.let { release ->
            cachedRelease = release
            downloadUrl = release.url
            latestTag = release.tag
        }
        resolved = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // App badge
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notification_logo),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.share_app_qr_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.share_app_qr_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LatestReleasePill(tag = latestTag, resolved = resolved)
                Spacer(modifier = Modifier.height(18.dp))

                // QR card
                Box(
                    modifier =
                        Modifier
                            .size(236.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White)
                            .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    QrCodeImage(
                        text = downloadUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                // Full-width buttons: side by side squeezed "Quick Share" onto two lines.
                Button(
                    onClick = {
                        if (preparing) return@Button
                        preparing = true
                        scope.launch {
                            val uris = runCatching { prepareApkUris(context) }.getOrDefault(emptyList())
                            preparing = false
                            if (uris.isEmpty()) {
                                Toast.makeText(context, R.string.share_app_qr_share_failed, Toast.LENGTH_SHORT).show()
                            } else {
                                startShare(context, uris)
                            }
                        }
                    },
                    enabled = !preparing,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (preparing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            stringResource(
                                if (preparing) R.string.share_app_qr_preparing else R.string.share_app_qr_share,
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

/** The chip above the QR: a spinner until GitHub answers, then the release it pointed at. */
@Composable
private fun LatestReleasePill(
    tag: String?,
    resolved: Boolean,
) {
    val text =
        when {
            !resolved -> stringResource(R.string.share_app_qr_checking_latest)
            tag != null -> stringResource(R.string.share_app_qr_latest_template, tag)
            else -> return
        }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (resolved) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
