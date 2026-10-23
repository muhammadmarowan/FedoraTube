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
                name = "Cinemeta (Official)",
                description = "Official catalog for IMDb ratings, posters, movies & series metadata",
                manifestUrl = "https://v3-cinemeta.strem.io/manifest.json",
                type = "Catalog & Metadata",
            ),
            PopularAddonPreset(
                name = "Torrentio",
                description = "High-speed torrent & debrid stream provider for movies and TV series",
                manifestUrl = "https://torrentio.strem.fun/manifest.json",
                type = "Streams",
            ),
            PopularAddonPreset(
                name = "Cyberflix Catalog",
                description = "Popular streaming services catalog (Netflix, Disney+, HBO Max, Apple TV+)",
                manifestUrl = "https://cyberflix.kamyroll.tech/manifest.json",
                type = "Catalog",
            ),
            PopularAddonPreset(
                name = "Superflix",
                description = "Direct HTTP video streaming addon for movies and series",
                manifestUrl = "https://superflix.site/manifest.json",
                type = "Streams",
            ),
            PopularAddonPreset(
                name = "Streaming Catalogs",
                description = "Browse content by streaming platforms and popular networks",
                manifestUrl = "https://7a82163c306e-stremio-netflix-catalog-addon.baby-beamup.club/manifest.json",
                type = "Catalog",
            ),
            PopularAddonPreset(
                name = "OpenSubtitles v3",
                description = "Multi-language subtitles from OpenSubtitles.org",
                manifestUrl = "https://opensubtitles-v3.strem.io/manifest.json",
                type = "Subtitles",
            ),
            PopularAddonPreset(
                name = "Anime Kitsu",
                description = "Kitsu anime catalog, episodes and streaming metadata",
                manifestUrl = "https://anime-kitsu.strem.fun/manifest.json",
                type = "Anime",
            ),
        )

        private val DEFAULT_ADDONS = listOf(
            StremioAddon(
                id = "com.linvo.cinemeta",
                name = "Cinemeta",
                version = "3.0.12",
                description = "The official IMDb movie and series catalog addon",
                iconUrl = "https://v3-cinemeta.strem.io/favicon.ico",
                transportUrl = "https://v3-cinemeta.strem.io/manifest.json",
                types = listOf("movie", "series"),
                resources = listOf("catalog", "meta"),
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
        val addons = if (storedJson.isNullOrBlank()) {
            saveAddons(DEFAULT_ADDONS)
            DEFAULT_ADDONS
        } else {
            try {
                json.decodeFromString<List<StremioAddon>>(storedJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed loading addons: ${e.message}")
                DEFAULT_ADDONS
            }
        }
        _installedAddons.value = addons
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

    suspend fun getHomeFeed(
        genre: String? = null,
        typeFilter: String? = null,
    ): MoviesHomeState = withContext(Dispatchers.IO) {
        val catalogAddons = _installedAddons.value.filter { it.enabled && it.resources.contains("catalog") }
        val mainAddon = catalogAddons.firstOrNull { it.id == "com.linvo.cinemeta" } ?: catalogAddons.firstOrNull()

        if (mainAddon == null) {
            return@withContext MoviesHomeState(
                isLoading = false,
                errorMessage = "No catalog addon installed or enabled",
            )
        }

        try {
            coroutineScope {
                val shouldFetchMovies = typeFilter == null || typeFilter == "movie"
                val shouldFetchSeries = typeFilter == null || typeFilter == "series"

                val popularMoviesDeferred = if (shouldFetchMovies) {
                    async { AddonClient.fetchCatalog(mainAddon.transportUrl, "movie", "top", genre = genre) }
                } else null

                val popularSeriesDeferred = if (shouldFetchSeries) {
                    async { AddonClient.fetchCatalog(mainAddon.transportUrl, "series", "top", genre = genre) }
                } else null

                val popularMovies = popularMoviesDeferred?.await().orEmpty()
                val popularSeries = popularSeriesDeferred?.await().orEmpty()

                val sections = mutableListOf<MovieCatalogSection>()

                if (popularMovies.isNotEmpty()) {
                    sections.add(MovieCatalogSection("movies_trending", "Trending Movies", popularMovies.take(15)))
                    if (popularMovies.size > 15) {
                        sections.add(MovieCatalogSection("movies_popular", "Popular Movies", popularMovies.drop(15)))
                    }
                }

                if (popularSeries.isNotEmpty()) {
                    sections.add(MovieCatalogSection("series_popular", "Top TV Series", popularSeries))
                }

                val heroItem = popularMovies.firstOrNull { !it.banner.isNullOrBlank() }
                    ?: popularSeries.firstOrNull { !it.banner.isNullOrBlank() }
                    ?: popularMovies.firstOrNull()
                    ?: popularSeries.firstOrNull()

                MoviesHomeState(
                    isLoading = false,
                    heroItem = heroItem,
                    sections = sections,
                    selectedGenre = genre,
                    selectedType = typeFilter,
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
        val mainAddon = catalogAddons.firstOrNull { it.id == "com.linvo.cinemeta" } ?: catalogAddons.firstOrNull()
            ?: return@withContext emptyList()

        coroutineScope {
            val moviesDeferred = async {
                AddonClient.fetchCatalog(mainAddon.transportUrl, "movie", "top", search = query)
            }
            val seriesDeferred = async {
                AddonClient.fetchCatalog(mainAddon.transportUrl, "series", "top", search = query)
            }

            val movies = moviesDeferred.await()
            val series = seriesDeferred.await()

            (movies + series).distinctBy { it.id }
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
            // Sort streams: 4K first, then 1080p, 720p, etc.
            allStreams.sortedWith(
                compareByDescending<MovieStream> {
                    val q = it.displayQuality
                    when {
                        q.contains("4K") || q.contains("2160") -> 4
                        q.contains("1080") -> 3
                        q.contains("720") -> 2
                        q.contains("480") -> 1
                        else -> 0
                    }
                }.thenByDescending { it.url != null }
            )
        }
    }
}

