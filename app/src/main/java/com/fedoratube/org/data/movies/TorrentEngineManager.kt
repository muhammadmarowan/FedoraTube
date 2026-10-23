package com.fedoratube.org.data.movies

import android.content.Context
import android.util.Log
import com.nuvio.engine.NuvioEngine
import com.nuvio.engine.NuvioEngineConfig
import com.nuvio.engine.NuvioTorrentProfile
import com.nuvio.engine.NuvioUploadMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class TorrentPlaybackStats(
    val downloadSpeedBytes: Long = 0L,
    val uploadSpeedBytes: Long = 0L,
    val connectedPeers: Int = 0,
    val connectedSeeds: Int = 0,
    val totalDownloadBytes: Long = 0L,
) {
    val downloadSpeedFormatted: String
        get() {
            val mb = downloadSpeedBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.1f MB/s", mb)
            } else {
                val kb = downloadSpeedBytes / 1024.0
                String.format(java.util.Locale.US, "%.0f KB/s", kb)
            }
        }
}

data class TorrentStreamResult(
    val playbackUrl: String,
    val streamId: String,
)

object TorrentEngineManager {
    private const val TAG = "TorrentEngineManager"
    private val mutex = Mutex()

    @Volatile
    private var engine: NuvioEngine? = null
    private var activeStreamId: String? = null

    private val _stats = MutableStateFlow(TorrentPlaybackStats())
    val stats: StateFlow<TorrentPlaybackStats> = _stats.asStateFlow()

    private suspend fun ensureEngine(context: Context): NuvioEngine = withContext(Dispatchers.IO) {
        val current = engine
        if (current != null) return@withContext current

        val stateDir = File(context.noBackupFilesDir, "torrent_stream/state").apply { mkdirs() }
        val cacheDir = File(context.cacheDir, "torrent_stream/cache").apply { mkdirs() }

        val config = NuvioEngineConfig(
            dataDirectory = stateDir,
            cacheDirectory = cacheDir,
            diskCacheCapacityBytes = 8L * 1024L * 1024L * 1024L, // 8 GB cache
            torrentProfile = NuvioTorrentProfile.Fast,
            uploadMode = NuvioUploadMode.Unlimited,
            streamInactivityTimeoutMilliseconds = 0,
        )

        try {
            val created = NuvioEngine.create(config)
            engine = created
            Log.i(TAG, "Torrent streaming engine initialized")
            created
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create torrent streaming engine: ${e.message}", e)
            throw e
        }
    }

    suspend fun startTorrentStream(
        context: Context,
        magnetUri: String,
        fileIndex: Int? = null,
    ): Result<TorrentStreamResult> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val eng = ensureEngine(context.applicationContext)

                // If previous stream is active, stop it
                activeStreamId?.let { prevId ->
                    try {
                        eng.stopStream(prevId)
                    } catch (_: Exception) {}
                    activeStreamId = null
                }

                val torrentId = eng.addMagnet(magnetUri)
                val stream = eng.prepareStream(
                    torrentId = torrentId,
                    fileIndex = fileIndex,
                    filenameHint = null,
                )

                activeStreamId = stream.id
                Log.i(TAG, "Torrent stream ready at: ${stream.url}")
                Result.success(TorrentStreamResult(playbackUrl = stream.url, streamId = stream.id))
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting torrent stream: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun updateStats() = withContext(Dispatchers.IO) {
        val eng = engine ?: return@withContext
        try {
            val s = eng.stats.value
            _stats.value = TorrentPlaybackStats(
                downloadSpeedBytes = s.downloadRateBytesPerSecond,
                uploadSpeedBytes = s.uploadRateBytesPerSecond,
                connectedPeers = s.connectedPeers,
                connectedSeeds = s.connectedSeeds,
                totalDownloadBytes = s.totalPayloadDownloadBytes,
            )
        } catch (_: Exception) {}
    }

    suspend fun stopStream(streamId: String? = null) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val targetId = streamId ?: activeStreamId
            if (targetId != null) {
                try {
                    engine?.stopStream(targetId)
                    Log.i(TAG, "Torrent stream stopped: $targetId")
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping torrent stream: ${e.message}")
                }
                if (targetId == activeStreamId) {
                    activeStreamId = null
                }
            }
            _stats.value = TorrentPlaybackStats()
        }
    }
}

