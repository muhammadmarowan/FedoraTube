package com.fedoratube.org.data.video.storage

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

/**
 * Which shared files Flow may read. Files an earlier install or another app saved are visible only
 * with All files access, or with the media permissions for their kind.
 */
data class StorageAccess(
    val allFiles: Boolean,
    val video: Boolean,
    val audio: Boolean,
) {
    /** Whether every video and song an earlier install downloaded can be found. */
    val seesAllDownloads: Boolean get() = allFiles || (video && audio)

    companion object {
        fun read(context: Context): StorageAccess {
            return StorageAccess(
                allFiles = false,
                video = false,
                audio = false,
            )
        }
    }
}
