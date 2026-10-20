package io.github.aedev.flow.ui.components.shared

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.aedev.flow.R
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.data.video.DownloadStreamPolicy
import io.github.aedev.flow.data.video.downloader.request.DownloadRequest
import io.github.aedev.flow.data.video.downloader.request.DownloadSubtitle
import io.github.aedev.flow.data.video.downloader.request.toDownloadRequest
import io.github.aedev.flow.data.video.downloader.work.DownloadController
import io.github.aedev.flow.innertube.models.response.PlayerResponse

/** Starts the downloads both download dialogs offer, asking for storage access once on the way. */
internal object DownloadLauncher {
    private const val STORAGE_PREFS = "flow_storage_prefs"
    private const val STORAGE_PERMISSION_ASKED = "storage_permission_asked"

    fun promptStoragePermissionIfNeeded(context: Context) {
        // No-op: no storage permissions are requested
    }

    /**
     * Queues the video stream and AAC audio the dialog picked. The queue re-extracts when a slot is
     * free and keeps these itags when they are still offered, so the choice survives a long wait.
     */
    fun startVideoDownload(
        context: Context,
        video: Video,
        videoFormat: PlayerResponse.StreamingData.Format,
        audioFormat: PlayerResponse.StreamingData.Format,
        threads: Int? = null,
        subtitle: DownloadSubtitle? = null,
    ) {
        promptStoragePermissionIfNeeded(context)
        submit(
            context,
            video.toDownloadRequest(
                targetHeight = DownloadStreamPolicy.videoHeight(videoFormat),
                videoCodec = DownloadStreamPolicy.videoCodecKey(videoFormat),
                videoItag = videoFormat.itag,
                audioItag = audioFormat.itag,
                audioTrackId = audioFormat.audioTrack?.id,
                audioLanguage = audioFormat.audioLanguageTag,
                threads = threads,
                subtitle = subtitle,
            ),
        )
        Toast.makeText(context, context.getString(R.string.ui_started_download, video.title), Toast.LENGTH_SHORT).show()
    }

    /** Queues an audio-only download of [format]'s track, saved as an M4A. */
    fun startAudioOnlyDownload(
        context: Context,
        video: Video,
        format: PlayerResponse.StreamingData.Format,
        threads: Int? = null,
    ): Boolean {
        promptStoragePermissionIfNeeded(context)
        submit(
            context,
            video.toDownloadRequest(
                audioOnly = true,
                audioItag = format.itag,
                audioTrackId = format.audioTrack?.id,
                audioLanguage = format.audioLanguageTag,
                threads = threads,
            ),
        )
        return true
    }

    /**
     * The dialog's last resort when it was offered no stream: the queue extracts again when the
     * download starts, and falls back to SABR itself when every direct URL is refused.
     */
    fun startDefaultDownload(
        context: Context,
        video: Video,
    ) {
        promptStoragePermissionIfNeeded(context)
        submit(context, video.toDownloadRequest(targetHeight = 0))
        Toast.makeText(context, context.getString(R.string.ui_started_download, video.title), Toast.LENGTH_SHORT).show()
    }

    private fun submit(
        context: Context,
        request: DownloadRequest,
    ) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, Dependencies::class.java)
            .downloadController()
            .submit(request, replaceExisting = true)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun downloadController(): DownloadController
    }
}
