package com.fedoratube.org.ui.screens.player

import com.fedoratube.org.data.local.PlayerPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Saves a speed the viewer picked on the player, from the speed sheet or a speed swipe, everywhere the
 * settings keep one: the remembered speed and the channel's own speed.
 */
internal fun CoroutineScope.saveChosenPlaybackSpeed(
    playerPreferences: PlayerPreferences,
    speed: Float,
    rememberPlaybackSpeed: Boolean,
    channelId: String?,
) {
    launch {
        if (rememberPlaybackSpeed) playerPreferences.setPlaybackSpeed(speed)
        if (!channelId.isNullOrBlank() && playerPreferences.speedPerChannel.first()) {
            playerPreferences.setChannelPlaybackSpeed(channelId, speed)
        }
    }
}
