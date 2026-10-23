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
    private val prefs = appContext.getSharedPreferences("fedora_movies_addons_v1", Context.MODE_PRIVATE)
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

        val POPULAR_PRESETS = listOf(
            PopularAddonPreset(
                name = "PlavioHD (Direct Streams)",
                description = "Direct HTTPS & HLS video streams for movies and series (no torrents needed)",
                manifestUrl = "https://hd.plavio.site/manifest.json",
                type = "Direct Streams",
            ),
            PopularAddonPreset(
                name = "Cinemeta (Official)",
                description = "Official catalog for IMDb ratings, posters, movies & series metadata",
                manifestUrl = "https://v3-cinemeta.strem.io/manifest.json",
                type = "Catalog & Metadata",
            ),
            PopularAddonPreset(
                name = "Torrentio",
                description = "High-speed torrent & debrid stream provider for movies and TV series",
                manifestUrl = "https://torrentio.strem.fun/manifest.json",
                type = "Torrents & Debrid",
            ),
            PopularAddonPreset(
                name = "Yoru Nuvio Providers",
                description = "Popular Nuvio community scraper repository for direct streams",
                manifestUrl = "https://raw.githubusercontent.com/yoruix/nuvio-providers/refs/heads/main/manifest.json",
                type = "Nuvio Plugin",
            ),
            PopularAddonPreset(
                name = "All-in-One Nuvio",
                description = "Comprehensive anime, movie and drama scrapers bundle",
                manifestUrl = "https://raw.githubusercontent.com/NuvioPlugin/All-in-One-Nuvio/refs/heads/main/manifest.json",
                type = "Nuvio Plugin",
            ),
            PopularAddonPreset(
                name = "OpenSubtitles v3",
                description = "Multi-language subtitles from OpenSubtitles.org",
                manifestUrl = "https://opensubtitles-v3.strem.io/manifest.json",
                type = "Subtitles",
            ),
            PopularAddonPreset(
                name = "Streaming Catalogs",
                description = "Browse content by streaming platforms and popular networks",
                manifestUrl = "https://7a82163c306e-stremio-netflix-catalog-addon.baby-beamup.club/manifest.json",
                type = "Catalog",
            ),
            PopularAddonPreset(
                name = "Anime Kitsu",
                description = "Kitsu anime catalog, episodes and streaming metadata",
                manifestUrl = "https://anime-kitsu.strem.fun/manifest.json",
                type = "Anime",
            ),
        )

        val DEFAULT_ADDONS = listOf(
            StremioAddon(
                id = "com.linvo.cinemeta",
                name = "Cinemeta",
                version = "3.0.12",
                description = "The official IMDb movie and series catalog addon",
                iconUrl = "https://v3-cinemeta.strem.io/favicon.ico",
                transportUrl = "https://v3-cinemeta.strem.io/manifest.json",
                types = listOf("movie", "series"),
                resources = listOf("catalog", "meta"),
                catalogs = listOf(
                    StremioCatalog("movie", "top", "Trending Movies"),
                    StremioCatalog("series", "top", "Top TV Series"),
                ),
                enabled = true,
                isDeletable = false,
            ),
            StremioAddon(
                id = "community.plaviohd",
                name = "PlavioHD (Direct Streams)",
                version = "1.9.49",
                description = "Direct HTTPS & HLS movies & TV series streams",
                iconUrl = "https://hd.plavio.site/logo.png",
                transportUrl = "https://hd.plavio.site/manifest.json",
                types = listOf("movie", "series", "tv"),
                resources = listOf("stream", "catalog", "meta"),
                catalogs = listOf(
                    StremioCatalog("tv", "plaviohd-live-tv", "Live TV"),
                ),
                enabled = true,
                isDeletable = false,
            ),
            StremioAddon(
                id = "torrentio",
                name = "Torrentio",
                version = "1.0.0",
                description = "Torrent and streaming provider",
                iconUrl = null,
                transportUrl = "https://torrentio.strem.fun/manifest.json",
                types = listOf("movie", "series"),
                resources = listOf("stream"),
                enabled = true,
                isDeletable = true,
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

        val updated = _installedAddons.value + fetched
        saveAddons(updated)
        Result.success(fetched)
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
                    if (addon.id == "com.linvo.cinemeta" || addon.catalogs.isEmpty()) {
                        // Cinemeta or standard addon default catalogs
                        val moviesDef = async { AddonClient.fetchCatalog(addon.transportUrl, "movie", "top") }
                        val seriesDef = async { AddonClient.fetchCatalog(addon.transportUrl, "series", "top") }
                        sectionDeferredList.add("Trending Movies" to moviesDef)
                        sectionDeferredList.add("Top TV Series" to seriesDef)
                    } else {
                        // Custom addon catalogs (e.g. Cyberflix, Streaming Catalogs, Live TV)
                        for (catalog in addon.catalogs.take(4)) {
                            val def = async {
                                AddonClient.fetchCatalog(addon.transportUrl, catalog.type, catalog.id)
                            }
                            val sectionTitle = if (catalog.name.contains(addon.name, ignoreCase = true)) {
                                catalog.name
                            } else {
                                "${catalog.name} (${addon.name})"
                            }
                            sectionDeferredList.add(sectionTitle to def)
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
