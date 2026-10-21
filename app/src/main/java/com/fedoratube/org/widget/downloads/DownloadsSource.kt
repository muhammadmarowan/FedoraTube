package com.fedoratube.org.widget.downloads

import androidx.glance.appwidget.GlanceAppWidget
import com.fedoratube.org.data.local.entity.DownloadItemStatus
import com.fedoratube.org.data.local.entity.DownloadWithItems
import com.fedoratube.org.data.music.DownloadManager
import com.fedoratube.org.data.video.VideoDownloadManager
import com.fedoratube.org.widget.core.refresh.WidgetContentKey
import com.fedoratube.org.widget.core.refresh.WidgetContentSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** One finished download as the widget draws it. */
data class WidgetDownload(
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val isMusic: Boolean,
    val downloadedAt: Long,
)

data class WidgetDownloads(
    val items: List<WidgetDownload>,
    val inProgress: Int,
)

@Singleton
class DownloadsSource
    @Inject
    constructor(
        private val videoDownloads: VideoDownloadManager,
        private val musicDownloads: DownloadManager,
    ) : WidgetContentSource {
        override val key = WidgetContentKey.DOWNLOADS
        override val widget: GlanceAppWidget get() = DownloadsWidget()

        suspend fun load(): WidgetDownloads {
            val videos =
                videoDownloads.downloadedVideos.first().map {
                    WidgetDownload(
                        id = it.video.id,
                        title = it.video.title,
                        subtitle = it.video.channelName,
                        thumbnailUrl = it.video.thumbnailUrl,
                        durationSeconds = it.video.duration.toLong(),
                        isMusic = false,
                        downloadedAt = it.downloadedAt,
                    )
                }
            val songs =
                musicDownloads.downloadedTracks.first().map {
                    WidgetDownload(
                        id = it.track.videoId,
                        title = it.track.title,
                        subtitle = it.track.artist,
                        thumbnailUrl = it.track.listThumbnailUrl,
                        durationSeconds = it.track.duration.toLong(),
                        isMusic = true,
                        downloadedAt = it.downloadedAt,
                    )
                }
            val items = (videos + songs).sortedByDescending { it.downloadedAt }.distinctBy { it.id }.take(MAX_ITEMS)
            return WidgetDownloads(items, inProgressCount(videoDownloads.allDownloads.first()))
        }

        // Rows are written on every progress tick; only a finished, removed or started download counts.
        override fun changes(): Flow<Unit> =
            videoDownloads.allDownloads
                .map { downloads ->
                    val finished = downloads.filter { it.overallStatus == DownloadItemStatus.COMPLETED }.map { it.download.videoId }
                    finished.sorted() to inProgressCount(downloads)
                }.distinctUntilChanged()
                .map { }

        override suspend fun signature(): String =
            load().let { downloads -> downloads.items.joinToString(",") { it.id } + "|" + downloads.inProgress }

        companion object {
            const val MAX_ITEMS = 8

            internal fun inProgressCount(downloads: List<DownloadWithItems>): Int =
                downloads.count {
                    it.overallStatus == DownloadItemStatus.DOWNLOADING ||
                        it.overallStatus == DownloadItemStatus.PENDING ||
                        it.overallStatus == DownloadItemStatus.PAUSED
                }
        }
    }
