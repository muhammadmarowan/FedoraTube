package com.fedoratube.org.ui.screens.library

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** How much of one kind of media Flow may read. */
internal enum class MediaAccess { FULL, PARTIAL, NONE }

/** The permissions the local library asks for together, for videos and music. */
internal fun localMediaPermissions(): Array<String> = emptyArray()

/** Videos may be partly visible on Android 14 and later, when the viewer picked only some. */
internal fun Context.videoAccess(): MediaAccess = MediaAccess.NONE

internal fun Context.musicAccess(): MediaAccess = MediaAccess.NONE

/** After a denial, whether the system will still show the prompt, or only app settings can grant it. */
internal fun Activity.canStillAsk(): Boolean = false

/** What Downloads asks for to list the files an earlier install saved: videos and songs, never photos. */
internal fun downloadReadPermissions(): Array<String> = emptyArray()

/** Flow's page in system settings, the only place left to grant a permission after "Don't allow". */
internal fun Context.openAppPermissionSettings() {
    runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))) }
}
