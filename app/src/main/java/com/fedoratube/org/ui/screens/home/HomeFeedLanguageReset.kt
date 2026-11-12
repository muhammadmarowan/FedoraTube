package com.fedoratube.org.ui.screens.home

import com.fedoratube.org.data.local.HomeFeedCacheRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube translates titles into the request language and ranks results by the request country,
 * so both Home caches hold the old locale's content after a switch and would keep showing it
 * until they expire (#1277). A trending-region switch orphans them exactly like a language one:
 * every discovery search, related lane and saved feed was fetched for the wrong `gl`.
 *
 * The caller updates `YouTube.locale` first, then reports the switch here; [resets] tells Home
 * to rebuild once the caches are gone, so the reload can never race the locale write.
 */
@Singleton
class HomeFeedLanguageReset
    @Inject
    constructor(
        private val storedFeed: HomeFeedCacheRepository,
    ) {
        private val _resets =
            MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )

        /** Emitted after a real language or region switch cleared the caches. */
        val resets: SharedFlow<Unit> = _resets.asSharedFlow()

        @Volatile private var lastLanguage: String? = null

        @Volatile private var lastRegion: String? = null

        suspend fun onContentLanguage(hl: String) {
            val previous = lastLanguage
            lastLanguage = hl
            if (!isLanguageSwitch(previous, hl)) return
            clearForSwitch()
        }

        /** The geolocation every InnerTube request carries: a switch orphans all cached feeds. */
        suspend fun onTrendingRegion(gl: String) {
            val previous = lastRegion
            lastRegion = gl
            if (!isLanguageSwitch(previous, gl)) return
            clearForSwitch()
        }

        private suspend fun clearForSwitch() {
            HomeFeedCache.clear()
            storedFeed.clearAll()
            _resets.tryEmit(Unit)
        }
    }

/** The first language of a process is where the caches came from, not a switch. */
internal fun isLanguageSwitch(
    previous: String?,
    current: String,
): Boolean = previous != null && previous != current
