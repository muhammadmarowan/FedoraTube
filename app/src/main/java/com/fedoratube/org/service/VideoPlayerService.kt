package com.fedoratube.org.service

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.fedoratube.org.R
import com.fedoratube.org.notification.NotificationHelper
import com.fedoratube.org.player.EnhancedPlayerManager
import com.fedoratube.org.player.GlobalPlayerState
import com.fedoratube.org.player.PopupPlayerWindow
import com.fedoratube.org.player.error.PlayerDiagnostics
import com.fedoratube.org.utils.FlowCrashHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@UnstableApi
class VideoPlayerService : MediaSessionService() {
    companion object {
        private const val TAG = "VideoPlayerService"
        private const val LOCK_RELEASE_DELAY_MS = 30_000L
        const val ACTION_SHOW_POPUP = "com.fedoratube.org.action.SHOW_POPUP_PLAYER"
        const val ACTION_HIDE_POPUP = "com.fedoratube.org.action.HIDE_POPUP_PLAYER"

        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_VIDEO_CHANNEL = "video_channel"
        const val EXTRA_VIDEO_THUMBNAIL = "video_thumbnail"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private var lockReleaseJob: Job? = null
    private var popupPlayerWindow: PopupPlayerWindow? = null

    private fun serviceSnapshot(): String {
        val player = EnhancedPlayerManager.getInstance().getPlayer()
        val state =
            when (player?.playbackState) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                null -> "NO_PLAYER"
                else -> "UNKNOWN(${player.playbackState})"
            }
        return "exo=$state pwr=${player?.playWhenReady} playing=${player?.isPlaying} " +
            "pos=${player?.currentPosition}/${player?.duration} " +
            "idx=${player?.currentMediaItemIndex} count=${player?.mediaItemCount} " +
            "wakeHeld=${wakeLock?.isHeld} wifiHeld=${wifiLock?.isHeld} " +
            "ongoing=${runCatching { isPlaybackOngoing() }.getOrDefault(false)} " +
            "activeForLocks=${isPlaybackActiveForLocks()}"
    }

    private fun serviceLog(message: String) {
        val full = "$message | ${serviceSnapshot()}"
        Log.w(TAG, full)
        PlayerDiagnostics.logWarning(TAG, full)
    }

    override fun onCreate() {
        super.onCreate()
        val notificationProvider =
            VideoNotificationProvider().apply {
                setSmallIcon(R.drawable.ic_notification_logo)
            }
        setMediaNotificationProvider(notificationProvider)
        recordForegroundStartFailures("video-service")

        FlowCrashHandler.recordPhase("video-service", "onCreate")
        serviceLog("onCreate")

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FedoraTube:VideoPlayerWakeLock")
            wakeLock?.setReferenceCounted(false)

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "FedoraTube:VideoPlayerWifiLock")
            wifiLock?.setReferenceCounted(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create locks", e)
        }

        EnhancedPlayerManager.getInstance().initialize(applicationContext)

        lifecycleScope.launch {
            EnhancedPlayerManager.getInstance().playerState.collectLatest {
                serviceLog("playerState collect update")
                updateLocks(isPlaybackActiveForLocks())
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        val manager = EnhancedPlayerManager.getInstance()
        manager.initialize(applicationContext)
        serviceLog("onGetSession controller=${controllerInfo.packageName}")
        if (controllerInfo.declinesMediaButtonStart(hasMediaItem = manager.getPlayer()?.currentMediaItem != null)) {
            serviceLog("onGetSession declined: media button with nothing to play")
            return null
        }
        return manager.getVideoMediaSession()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val startResult = super.onStartCommand(intent, flags, startId)
        FlowCrashHandler.recordPhase(
            "video-service",
            "onStartCommand action=${intent?.action} startId=$startId",
        )
        serviceLog("onStartCommand action=${intent?.action}")

        when (intent?.action) {
            ACTION_SHOW_POPUP -> showPopupPlayer()
            ACTION_HIDE_POPUP -> popupPlayerWindow?.dismiss()
        }

        updateLocks(isPlaybackActiveForLocks())
        return startResult
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        serviceLog("onTaskRemoved pip=${GlobalPlayerState.isInPipMode.value}")
        if (GlobalPlayerState.isInPipMode.value) {
            GlobalPlayerState.requestDismiss()
            EnhancedPlayerManager.getInstance().stop()
            releaseLocks()
            stopSelf()
            return
        }

        val player = EnhancedPlayerManager.getInstance().getPlayer()
        if (player?.keepsPlayingOnTaskRemoved(isPlaybackOngoing()) == true) return

        EnhancedPlayerManager.getInstance().stop()
        releaseLocks()
        // Unlike a bare stopSelf(), this clears Media3's user-engaged state, so the pause cannot put
        // the stopped service back into the foreground (#1025).
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        serviceLog("onDestroy")
        popupPlayerWindow?.dismiss()
        popupPlayerWindow = null
        lockReleaseJob?.cancel()
        lockReleaseJob = null
        releaseLocks()
        // lifecycleScope is cancelled by super.onDestroy() dispatching ON_DESTROY.
        super.onDestroy()
    }

    private fun showPopupPlayer() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !android.provider.Settings.canDrawOverlays(this)) return
        val player = EnhancedPlayerManager.getInstance().getPlayer() ?: return
        if (popupPlayerWindow == null) {
            popupPlayerWindow =
                PopupPlayerWindow(this) {
                    popupPlayerWindow?.dismiss()
                    EnhancedPlayerManager.getInstance().stop()
                    stopSelf()
                }
        }
        popupPlayerWindow?.show(player)
    }

    private fun acquireLocks() {
        serviceLog("acquireLocks")
        if (wakeLock?.isHeld != true) wakeLock?.acquire()
        if (wifiLock?.isHeld != true) wifiLock?.acquire()
    }

    private fun updateLocks(isPlaybackActive: Boolean) {
        serviceLog("updateLocks active=$isPlaybackActive")
        lockReleaseJob?.cancel()
        lockReleaseJob = null

        if (isPlaybackActive) {
            acquireLocks()
            return
        }

        lockReleaseJob =
            lifecycleScope.launch {
                delay(LOCK_RELEASE_DELAY_MS)
                releaseLocks()
            }
    }

    private fun isPlaybackActiveForLocks(): Boolean {
        val player = EnhancedPlayerManager.getInstance().getPlayer() ?: return false
        return player.isPlaying ||
            (
                player.playWhenReady &&
                    player.playbackState != Player.STATE_IDLE &&
                    player.playbackState != Player.STATE_ENDED
            )
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock", e)
        }
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wifi lock", e)
        }
    }

    private inner class VideoNotificationProvider :
        DefaultMediaNotificationProvider(
            this@VideoPlayerService,
            { NotificationHelper.NOTIFICATION_PLAYBACK },
            NotificationHelper.CHANNEL_PLAYBACK,
            R.string.notification_channel_video_playback,
        ) {
        override fun addNotificationActions(
            session: MediaSession,
            mediaButtons: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
            builder: androidx.core.app.NotificationCompat.Builder,
            actionFactory: androidx.media3.session.MediaNotification.ActionFactory,
        ): IntArray {
            builder.setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            builder.setCategory(androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT)
            builder.setShowWhen(false)
            if (session.player.isPlaying) {
                builder.setOngoing(true)
            }
            return super.addNotificationActions(session, mediaButtons, builder, actionFactory)
        }
    }
}
