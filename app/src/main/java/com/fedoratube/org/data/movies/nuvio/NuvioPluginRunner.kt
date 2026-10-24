package com.fedoratube.org.data.movies.nuvio

import android.content.Context
import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.quickJs
import com.fedoratube.org.data.movies.MovieStream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object NuvioPluginRunner {
    private const val TAG = "NuvioPluginRunner"
    private const val SCRAPER_TIMEOUT_MS = 15_000L

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val pluginDispatcher: CoroutineDispatcher =
        Executors.newFixedThreadPool(4) { runnable ->
            Thread({
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            }, "nuvio-plugin-worker").apply { isDaemon = true }
        }.asCoroutineDispatcher()

    private val scraperCodeCache = ConcurrentHashMap<String, String>()
    private val tmdbIdCache = ConcurrentHashMap<String, String>()

    suspend fun fetchStreamsForNuvioPlugin(
        context: Context,
        manifestUrl: String,
        pluginName: String,
        type: String,
        id: String,
    ): List<MovieStream> = withContext(Dispatchers.IO) {
        try {
            val manifestBody = httpGet(manifestUrl) ?: return@withContext emptyList()
            val root = json.parseToJsonElement(manifestBody).jsonObject
            val scrapersArray = (root["scrapers"] as? JsonArray)
                ?: (root["providers"] as? JsonArray)
                ?: return@withContext emptyList()

            val baseUrl = manifestUrl.substringBefore("?").removeSuffix("/manifest.json").trimEnd('/')

            // Resolve TMDB ID, season, episode from id
            val (resolvedTmdbId, season, episode) = resolveMediaTarget(type, id)
            if (resolvedTmdbId.isBlank()) {
                Log.w(TAG, "Could not resolve TMDB ID for target type=$type id=$id")
                return@withContext emptyList()
            }

            val targetType = if (type.equals("movie", ignoreCase = true)) "movie" else "tv"

            // Filter enabled scrapers that support this media type
            val candidateScrapers = scrapersArray.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val enabled = obj["enabled"]?.jsonPrimitive?.contentOrNull != "false"
                if (!enabled) return@mapNotNull null

                val supportedTypes = (obj["supportedTypes"] as? JsonArray)?.mapNotNull {
                    it.jsonPrimitive.contentOrNull?.lowercase()
                } ?: listOf("movie", "tv", "series")

                val matches = if (targetType == "movie") {
                    supportedTypes.contains("movie")
                } else {
                    supportedTypes.contains("tv") || supportedTypes.contains("series")
                }

                if (!matches) return@mapNotNull null

                val scraperId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val scraperName = obj["name"]?.jsonPrimitive?.contentOrNull ?: scraperId
                val filename = obj["filename"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                Triple(scraperId, scraperName, filename)
            }

            if (candidateScrapers.isEmpty()) return@withContext emptyList()

            // Run scrapers concurrently (up to 8 in parallel)
            coroutineScope {
                val deferredList = candidateScrapers.take(12).map { (scraperId, scraperName, filename) ->
                    async {
                        val codeUrl = if (filename.startsWith("http://") || filename.startsWith("https://")) {
                            filename
                        } else {
                            "$baseUrl/${filename.trimStart('/')}"
                        }

                        val code = getScraperCode(context, scraperId, codeUrl) ?: return@async emptyList()

                        executeSingleScraper(
                            code = code,
                            scraperId = scraperId,
                            scraperName = scraperName,
                            pluginName = pluginName,
                            tmdbId = resolvedTmdbId,
                            mediaType = targetType,
                            season = season,
                            episode = episode,
                        )
                    }
                }

                deferredList.awaitAll().flatten()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching streams for Nuvio plugin $manifestUrl: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun executeSingleScraper(
        code: String,
        scraperId: String,
        scraperName: String,
        pluginName: String,
        tmdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
    ): List<MovieStream> = withContext(pluginDispatcher) {
        withTimeoutOrNull(SCRAPER_TIMEOUT_MS) {
            try {
                val deferred = CompletableDeferred<String>()
                val callArgsJson = JsonObject(
                    mapOf(
                        "tmdbId" to JsonPrimitive(tmdbId),
                        "mediaType" to JsonPrimitive(mediaType),
                        "season" to (season?.let(::JsonPrimitive) ?: JsonNull),
                        "episode" to (episode?.let(::JsonPrimitive) ?: JsonNull),
                    ),
                ).toString()

                val domBridge = DomBridge()
                val hostRegistry = HostApiRegistry().apply {
                    addModule(
                        HostFunctions(
                            scraperId = scraperId,
                            scraperSettingsJson = "{}",
                            callArgsJson = callArgsJson,
                            onResult = { deferred.complete(it) },
                        ),
                    )
                    addModule(FetchBridge())
                    addModule(UrlBridge())
                    addModule(CryptoBridge())
                    addModule(WasmBridge())
                    addModule(domBridge)
                }

                quickJs {
                    evaluationTimeoutMillis = SCRAPER_TIMEOUT_MS
                    hostRegistry.registerAll(this)

                    // Execute polyfills
                    evaluate<Any?>(JsBindings.staticPolyfillCode)

                    // Wrap and execute scraper code
                    val wrapped = """
                        var module = { exports: {} };
                        var exports = module.exports;
                        (function() {
                            $code
                        })();
                    """.trimIndent()
                    evaluate<Any?>(wrapped)

                    // Execute runner
                    evaluate<Any?>(JsBindings.staticCallCode)

                    val resultJson = deferred.await()
                    parseStreams(resultJson, scraperName, pluginName)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Scraper $scraperId execution failed: ${e.message}")
                emptyList()
            }
        } ?: emptyList()
    }

    private fun parseStreams(rawJson: String, scraperName: String, pluginName: String): List<MovieStream> {
        return try {
            val rootArray = json.parseToJsonElement(rawJson) as? JsonArray ?: return emptyList()
            rootArray.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null

                var url = obj["url"]?.jsonPrimitive?.contentOrNull
                    ?: obj["link"]?.jsonPrimitive?.contentOrNull
                    ?: obj["streamUrl"]?.jsonPrimitive?.contentOrNull
                val externalUrl = obj["externalUrl"]?.jsonPrimitive?.contentOrNull
                var infoHash = obj["infoHash"]?.jsonPrimitive?.contentOrNull

                // Handle magnet URLs
                if (!url.isNullOrBlank() && url.startsWith("magnet:", ignoreCase = true)) {
                    val xt = url.substringAfter("xt=urn:btih:", "").substringBefore("&").trim()
                    if (xt.isNotBlank()) infoHash = xt
                }

                if (url.isNullOrBlank() && externalUrl.isNullOrBlank() && infoHash.isNullOrBlank()) {
                    return@mapNotNull null
                }

                val server = obj["server"]?.jsonPrimitive?.contentOrNull
                val filename = obj["filename"]?.jsonPrimitive?.contentOrNull
                val quality = obj["quality"]?.jsonPrimitive?.contentOrNull
                val size = obj["size"]?.jsonPrimitive?.contentOrNull

                val baseName = obj["name"]?.jsonPrimitive?.contentOrNull ?: server ?: scraperName
                val baseTitle = obj["title"]?.jsonPrimitive?.contentOrNull ?: filename ?: baseName
                val streamTitle = buildString {
                    append(baseTitle)
                    if (!size.isNullOrBlank()) append(" • $size")
                }
                val description = obj["description"]?.jsonPrimitive?.contentOrNull

                val hints = obj["behaviorHints"] as? JsonObject
                val reqHeaders = (hints?.get("proxyHeaders") as? JsonObject)?.get("request") as? JsonObject
                val proxyHeaders = reqHeaders?.entries?.associate {
                    it.key to (it.value.jsonPrimitive.contentOrNull.orEmpty())
                }

                MovieStream(
                    name = quality?.let { "$baseName $it" } ?: baseName,
                    title = streamTitle,
                    description = description,
                    url = url,
                    externalUrl = externalUrl,
                    infoHash = infoHash,
                    addonName = "$pluginName • $scraperName",
                    proxyHeaders = proxyHeaders,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing streams from JSON: ${e.message}")
            emptyList()
        }
    }

    private suspend fun getScraperCode(context: Context, scraperId: String, codeUrl: String): String? {
        scraperCodeCache[scraperId]?.let { return it }

        val cacheFile = File(context.cacheDir, "nuvio_scrapers/$scraperId.js")
        if (cacheFile.exists() && System.currentTimeMillis() - cacheFile.lastModified() < 24 * 60 * 60 * 1000L) {
            val cached = cacheFile.readText(Charsets.UTF_8)
            if (cached.isNotBlank()) {
                scraperCodeCache[scraperId] = cached
                return cached
            }
        }

        val downloaded = httpGet(codeUrl) ?: return null
        try {
            cacheFile.parentFile?.mkdirs()
            cacheFile.writeText(downloaded, Charsets.UTF_8)
        } catch (_: Exception) {}

        scraperCodeCache[scraperId] = downloaded
        return downloaded
    }

    private suspend fun resolveMediaTarget(type: String, id: String): Triple<String, Int?, Int?> {
        val trimmed = id.trim()
        val parts = trimmed.split(":")

        // Episode format: tt12345:season:episode or 12345:season:episode
        val baseId = parts.getOrNull(0).orEmpty()
        val season = parts.getOrNull(1)?.toIntOrNull()
        val episode = parts.getOrNull(2)?.toIntOrNull()

        val normalized = baseId
            .removePrefix("tmdb:")
            .removePrefix("series:")
            .removePrefix("movie:")
            .trim()

        if (normalized.all(Char::isDigit)) {
            return Triple(normalized, season, episode)
        }

        if (normalized.startsWith("tt", ignoreCase = true)) {
            tmdbIdCache[normalized]?.let { return Triple(it, season, episode) }

            // Resolve IMDb ID to TMDB ID
            val tmdbId = resolveImdbToTmdb(normalized, type)
            if (tmdbId != null) {
                tmdbIdCache[normalized] = tmdbId
                return Triple(tmdbId, season, episode)
            }

            return Triple(normalized, season, episode)
        }

        return Triple(normalized, season, episode)
    }

    private suspend fun resolveImdbToTmdb(imdbId: String, type: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.themoviedb.org/3/find/$imdbId?api_key=$NUVIO_DEFAULT_TMDB_API_KEY&external_source=imdb_id"
            val body = httpGet(url) ?: return@withContext null
            val root = json.parseToJsonElement(body).jsonObject

            val isSeries = type.equals("series", ignoreCase = true) || type.equals("tv", ignoreCase = true)
            val resultsKey = if (isSeries) "tv_results" else "movie_results"
            val fallbackKey = if (isSeries) "movie_results" else "tv_results"

            val results = (root[resultsKey] as? JsonArray)?.takeIf { it.isNotEmpty() }
                ?: (root[fallbackKey] as? JsonArray)

            val first = results?.firstOrNull()?.jsonObject
            first?.get("id")?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            Log.w(TAG, "Failed resolving IMDb $imdbId to TMDB: ${e.message}")
            null
        }
    }

    private fun httpGet(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "*/*")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "HTTP GET failed for $url: ${e.message}")
            null
        }
    }
}

