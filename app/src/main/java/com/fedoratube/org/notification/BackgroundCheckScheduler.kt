package com.fedoratube.org.notification

import android.content.Context
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.data.local.PlayerPreferences
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Keeps the periodic background checks in step with the notification switches, wherever they are
 * changed (settings, onboarding, a restored backup). A check whose notifications are off is
 * cancelled, so Flow is never woken for a result it would throw away.
 */
internal object BackgroundCheckScheduler {
    fun subscriptionCheckWanted(
        notificationsEnabled: Boolean,
        newVideosEnabled: Boolean,
        focusModeEnabled: Boolean = false,
    ): Boolean = notificationsEnabled && newVideosEnabled && !focusModeEnabled

    fun updateCheckWanted(
        notificationsEnabled: Boolean,
        updatesEnabled: Boolean,
        focusModeEnabled: Boolean = false,
    ): Boolean = notificationsEnabled && updatesEnabled && !focusModeEnabled

    /**
     * Runs for the life of the process. The first value only confirms the schedule a previous
     * process left (KEEP, so its timer is not reset); later changes replace it.
     */
    suspend fun follow(
        context: Context,
        preferences: PlayerPreferences,
    ) = coroutineScope {
        launch {
            preferences.focusModeEnabled
                .distinctUntilChanged()
                .collect { enabled -> if (enabled) NotificationHelper.cancelNonPlaybackNotifications(context) }
        }
        launch {
            combine(
                preferences.notificationsEnabled,
                preferences.notifNewVideosEnabled,
                preferences.subscriptionCheckIntervalMinutes,
                preferences.focusModeEnabled,
            ) { enabled, newVideos, minutes, focusMode ->
                if (subscriptionCheckWanted(enabled, newVideos, focusMode)) minutes else null
            }
                .distinctUntilChanged()
                .collectIndexed { index, minutes ->
                    if (minutes == null) {
                        SubscriptionCheckWorker.cancelScheduledChecks(context)
                    } else {
                        SubscriptionCheckWorker.schedulePeriodicCheck(context, minutes.toLong(), reschedule = index > 0)
                    }
                }
        }
        if (BuildConfig.UPDATER_ENABLED) {
            launch {
                combine(
                    preferences.notificationsEnabled,
                    preferences.notifUpdatesEnabled,
                    preferences.focusModeEnabled,
                    ::updateCheckWanted,
                ).distinctUntilChanged()
                    .collectIndexed { index, wanted ->
                        if (wanted) {
                            UpdateCheckWorker.schedulePeriodicCheck(context, reschedule = index > 0)
                        } else {
                            UpdateCheckWorker.cancelScheduledChecks(context)
                        }
                    }
            }
        }
    }
}
