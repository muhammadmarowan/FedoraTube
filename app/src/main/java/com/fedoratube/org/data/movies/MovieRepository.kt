package com.fedoratube.org.data.movies

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PopularAddonPreset(
    val name: String,
    val description: String,
    val manifestUrl: String,
    val type: String = "Streams & Metadata",
)

class MovieRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("fedora_movies_addons_v3", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _installedAddons = MutableStateFlow<List<StremioAddon>>(emptyList())
    val installedAddons: StateFlow<List<StremioAddon>> = _installedAddons.asStateFlow()

    companion object {
        private const val KEY_ADDONS = "installed_addons_json"
        private const val TAG = "MovieRepository"

        @Volatile
        private var INSTANCE: MovieRepository? = null

        fun getInstance(context: Context): MovieRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MovieRepository(context).also { INSTANCE = it }
            }
        }

        val POPULAR_PRESETS = emptyList<PopularAddonPreset>()

        val DEFAULT_ADDONS = listOf(
            StremioAddon(
                id = "com.soluserv.streaming-catalog-plus",
                name = "fedoratube addon",
                version = "5.1.11",
                description = "Official FedoraTube movies and series catalogs",
                iconUrl = "https://scplus.duckdns.org/assets/scplus-stremio-neon-nova-5111.png",
                transportUrl = "https://scplus.duckdns.org/c.4ac87f9234298a5314277c44b63dd13221c25f82/manifest.json",
                types = listOf("movie", "series"),
                resources = listOf("catalog", "meta"),
                catalogs = listOf(
                    StremioCatalog("movie", "global_movie_in_theaters_50d_global_cv24_da431fd45f", "Movies · Now Playing · GLOBAL"),
                    StremioCatalog("movie", "global_movie_popular_global_cv24_da431fd45f", "Movies · Popular · GLOBAL"),
                    StremioCatalog("movie", "global_movie_top_global_cv24_da431fd45f", "Movies · Top Rated · GLOBAL"),
                    StremioCatalog("series", "global_series_airing_today_global_cv24_da431fd45f", "Series · Airing Today · GLOBAL"),
                    StremioCatalog("series", "global_series_popular_global_cv24_da431fd45f", "Series · Popular · GLOBAL"),
                    StremioCatalog("series", "global_series_top_global_cv24_da431fd45f", "Series · Top Rated · GLOBAL"),
                    StremioCatalog("movie", "local_movie_in_theaters_50d_local_cv24_da431fd45f", "Movies · Now Playing · LOCAL"),
                    StremioCatalog("movie", "local_movie_popular_local_cv24_da431fd45f", "Movies · Popular · LOCAL"),
                    StremioCatalog("movie", "local_movie_top_local_cv24_da431fd45f", "Movies · Top Rated · LOCAL"),
                    StremioCatalog("series", "local_series_airing_today_local_cv24_da431fd45f", "Series · Airing Today · LOCAL"),
                    StremioCatalog("series", "local_series_popular_local_cv24_da431fd45f", "Series · Popular · LOCAL"),
                    StremioCatalog("series", "local_series_top_local_cv24_da431fd45f", "Series · Top Rated · LOCAL"),
                    StremioCatalog("series", "anime_series_airing_today_anime_cv24_da431fd45f", "Series · Airing Today · ANIME"),
                    StremioCatalog("series", "anime_series_airing_week_anime_cv24_da431fd45f", "Series · On Air This Week · ANIME"),
                    StremioCatalog("series", "anime_series_new_recent_anime_cv24_da431fd45f", "Series · New Releases · ANIME"),
                    StremioCatalog("series", "anime_series_miniseries_recent_anime_cv24_da431fd45f", "Series · Recent Miniseries · ANIME"),
                    StremioCatalog("series", "anime_series_finished_recent_anime_cv24_da431fd45f", "Series · Completed Series · ANIME"),
                    StremioCatalog("series", "anime_series_popular_anime_cv24_da431fd45f", "Series · Popular · ANIME"),
                    StremioCatalog("series", "anime_series_top_anime_cv24_da431fd45f", "Series · Top Rated · ANIME"),
                    StremioCatalog("movie", "provider_nfx_popular_cv24_da431fd45f", "Netflix · Movies · Popular"),
                    StremioCatalog("movie", "provider_nfx_top_cv24_da431fd45f", "Netflix · Movies · Top Rated"),
                    StremioCatalog("movie", "provider_nfx_recent_cv24_da431fd45f", "Netflix · Movies · Most Recent"),
                    StremioCatalog("series", "provider_nfx_popular_cv24_da431fd45f", "Netflix · Series · Popular"),
                    StremioCatalog("series", "provider_nfx_top_cv24_da431fd45f", "Netflix · Series · Top Rated"),
                    StremioCatalog("series", "provider_nfx_recent_cv24_da431fd45f", "Netflix · Series · Most Recent"),
                    StremioCatalog("movie", "provider_nfx_fresh_cv24_da431fd45f", "Netflix · Movies · New This Year"),
                    StremioCatalog("movie", "provider_nfx_hidden_gems_cv24_da431fd45f", "Netflix · Movies · Hidden Gems"),
                    StremioCatalog("movie", "provider_nfx_family_cv24_da431fd45f", "Netflix · Movies · Family"),
                    StremioCatalog("movie", "provider_prv_popular_cv24_da431fd45f", "Amazon Prime Video · Movies · Popular"),
                    StremioCatalog("movie", "provider_prv_top_cv24_da431fd45f", "Amazon Prime Video · Movies · Top Rated"),
                    StremioCatalog("series", "provider_prv_popular_cv24_da431fd45f", "Amazon Prime Video · Series · Popular"),
                    StremioCatalog("movie", "provider_p315_popular_cv24_da431fd45f", "Hoichoi · Movies · Popular"),
                    StremioCatalog("series", "provider_p315_popular_cv24_da431fd45f", "Hoichoi · Series · Popular"),
                    StremioCatalog("movie", "provider_p2336_popular_cv24_da431fd45f", "JioHotstar · Movies · Popular"),
                    StremioCatalog("series", "provider_p2336_popular_cv24_da431fd45f", "JioHotstar · Series · Popular"),
                ),
                enabled = true,
                isDeletable = false,
            ),
        )
    }

    init {
        loadAddons()
    }

    private fun loadAddons() {
        val storedJson = prefs.getString(KEY_ADDONS, null)
        var addons = if (storedJson.isNullOrBlank()) {
            DEFAULT_ADDONS
        } else {
            try {
                json.decodeFromString<List<StremioAddon>>(storedJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading addons: ${e.message}")
                DEFAULT_ADDONS
            }
        }

        // Ensure key defaults (Cinemeta and PlavioHD for direct streams) are always present
        val missingDefaults = DEFAULT_ADDONS.filter { def -> addons.none { it.id == def.id } }
        if (missingDefaults.isNotEmpty()) {
            addons = addons + missingDefaults
            saveAddons(addons)
        } else {
            _installedAddons.value = addons
        }
    }

    private fun saveAddons(addons: List<StremioAddon>) {
        try {
            val encoded = json.encodeToString(addons)
            prefs.edit().putString(KEY_ADDONS, encoded).apply()
            _installedAddons.value = addons
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving addons: ${e.message}")
        }
    }

    suspend fun addAddon(rawUrl: String): Result<StremioAddon> = withContext(Dispatchers.IO) {
        val normalized = normalizeManifestUrl(rawUrl)
        if (normalized.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty URL"))
        }

        val existing = _installedAddons.value.firstOrNull { it.transportUrl == normalized }
        if (existing != null) {
            return@withContext Result.success(existing)
        }

        val fetched = AddonClient.fetchManifest(normalized)
            ?: return@withContext Result.failure(Exception("Could not fetch addon manifest from $normalized"))

        val fetchedAddon = if (normalized.contains("scplus.duckdns.org") || normalized.contains("4ac87f9234298a5314277c44b63dd13221c25f82")) {
            fetched.copy(name = "fedoratube addon")
        } else {
            fetched
        }

        val updated = _installedAddons.value + fetchedAddon
        saveAddons(updated)
        Result.success(fetchedAddon)
    }

    fun removeAddon(addonId: String) {
        val updated = _installedAddons.value.filter { it.id != addonId }
        saveAddons(updated)
    }

    fun toggleAddon(addonId: String, enabled: Boolean) {
        val updated = _installedAddons.value.map {
            if (it.id == addonId) it.copy(enabled = enabled) else it
        }
        saveAddons(updated)
    }

    fun restoreAddons(addons: List<StremioAddon>) {
        val current = _installedAddons.value
        val merged = (current.filter { !it.isDeletable } + addons).distinctBy { it.id }
        saveAddons(merged)
    }

    suspend fun getHomeFeed(): MoviesHomeState = withContext(Dispatchers.IO) {
        val enabledAddons = _installedAddons.value.filter { it.enabled }
        val catalogAddons = enabledAddons.filter { it.resources.contains("catalog") || it.catalogs.isNotEmpty() }

        if (catalogAddons.isEmpty()) {
            return@withContext MoviesHomeState(
                isLoading = false,
                errorMessage = "No catalog addon installed or enabled",
            )
        }

        try {
            coroutineScope {
                val sectionDeferredList = mutableListOf<Pair<String, kotlinx.coroutines.Deferred<List<MovieItem>>>>()

                for (addon in catalogAddons) {
                    if (addon.catalogs.isEmpty()) {
                        val moviesDef = async { AddonClient.fetchCatalog(addon.transportUrl, "movie", "top") }
                        val seriesDef = async { AddonClient.fetchCatalog(addon.transportUrl, "series", "top") }
                        sectionDeferredList.add("Trending Movies" to moviesDef)
                        sectionDeferredList.add("Top TV Series" to seriesDef)
                    } else {
                        val browsableCatalogs = addon.catalogs.filter {
                            !it.id.startsWith("search", ignoreCase = true) &&
                            !it.id.startsWith("last-", ignoreCase = true) &&
                            !it.id.startsWith("calendar-", ignoreCase = true)
                        }
                        for (catalog in browsableCatalogs.take(35)) {
                            val def = async {
                                AddonClient.fetchCatalog(addon.transportUrl, catalog.type, catalog.id)
                            }
                            sectionDeferredList.add(catalog.name to def)
                        }
                    }
                }

                val sections = mutableListOf<MovieCatalogSection>()
                val allItems = mutableListOf<MovieItem>()

                for ((title, deferred) in sectionDeferredList) {
                    val items = deferred.await()
                    if (items.isNotEmpty()) {
                        allItems.addAll(items)
                        if (items.size > 15 && title == "Trending Movies") {
                            sections.add(MovieCatalogSection("sec_${sections.size}", "Trending Movies", items.take(15)))
                            sections.add(MovieCatalogSection("sec_${sections.size}", "Popular Movies", items.drop(15)))
                        } else {
                            sections.add(MovieCatalogSection("sec_${sections.size}", title, items))
                        }
                    }
                }

                val heroItem = allItems.firstOrNull { !it.banner.isNullOrBlank() }
                    ?: allItems.firstOrNull()

                MoviesHomeState(
                    isLoading = false,
                    heroItem = heroItem,
                    sections = sections,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed loading home feed: ${e.message}")
            MoviesHomeState(
                isLoading = false,
                errorMessage = e.message ?: "Failed to load movies",
            )
        }
    }

    suspend fun searchMovies(query: String): List<MovieItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val catalogAddons = _installedAddons.value.filter { it.enabled && it.resources.contains("catalog") }
        if (catalogAddons.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val deferredList = catalogAddons.flatMap { addon ->
                listOf(
                    async { AddonClient.fetchCatalog(addon.transportUrl, "movie", "top", search = query) },
                    async { AddonClient.fetchCatalog(addon.transportUrl, "series", "top", search = query) },
                )
            }

            val allResults = deferredList.awaitAll().flatten()
            allResults.distinctBy { it.id }
        }
    }

    suspend fun getMovieDetails(type: String, id: String): MovieDetails? = withContext(Dispatchers.IO) {
        val metaAddons = _installedAddons.value.filter { it.enabled && it.resources.contains("meta") }
        for (addon in metaAddons) {
            val details = AddonClient.fetchMeta(addon.transportUrl, type, id)
            if (details != null) return@withContext details
        }
        null
    }

    suspend fun getStreams(type: String, id: String): List<MovieStream> = withContext(Dispatchers.IO) {
        val streamAddons = _installedAddons.value.filter { it.enabled && it.resources.contains("stream") }
        if (streamAddons.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val deferredList = streamAddons.map { addon ->
                async {
                    AddonClient.fetchStreams(addon.transportUrl, type, id, addon.name, addon.iconUrl)
                }
            }

            val allStreams = deferredList.awaitAll().flatten()
            // Prioritize direct playable HTTPS streams over torrents, and sort by resolution (4K > 1080p > 720p)
            allStreams.sortedWith(
                compareByDescending<MovieStream> { it.isDirectPlayable }
                    .thenByDescending {
                        val q = it.displayQuality
                        when {
                            q.contains("4K") || q.contains("2160") -> 4
                            q.contains("1080") -> 3
                            q.contains("720") -> 2
                            q.contains("480") -> 1
                            else -> 0
                        }
                    }
                    .thenByDescending { it.url != null }
            )
        }
    }
}
