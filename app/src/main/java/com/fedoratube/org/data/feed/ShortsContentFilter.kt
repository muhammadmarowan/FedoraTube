package com.fedoratube.org.data.feed

import com.fedoratube.org.data.local.PlayerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortsContentFilter(
    val enabled: Flow<Boolean>,
) {
    @Inject
    constructor(preferences: PlayerPreferences) : this(
        combine(preferences.shortsContentEnabled, preferences.focusModeEnabled) { master, focus ->
            master && !focus
        }.distinctUntilChanged(),
    )

    suspend fun isEnabled(): Boolean = enabled.first()
}
