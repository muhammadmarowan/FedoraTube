package com.fedoratube.org.data.movies

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AddonClient {
    private const val TAG = "AddonClient"

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

    private suspend fun getJson(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.w(TAG, "Request to $url returned HTTP ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed request to $url: ${e.message}")
            null
        }
    }

    suspend fun fetchManifest(manifestUrl: String): StremioAddon? = withContext(Dispatchers.IO) {
        val normalized = normalizeManifestUrl(manifestUrl)
        if (normalized.isBlank()) return@withContext null

        val body = getJson(normalized) ?: return@withContext null
        try {
            val root = json.parseToJsonElement(body).jsonObject

            // Check if this is a Nuvio Plugin Manifest (which has "scrapers")
            val scrapersArray = root["scrapers"] as? JsonArray
            if (scrapersArray != null) {
                val name = root["name"]?.jsonPrimitive?.contentOrNull ?: "Nuvio Plugin"
                val version = root["version"]?.jsonPrimitive?.contentOrNull ?: "1.0.0"
                val id = "nuvio_" + name.lowercase().replace(Regex("[^a-z0-9]"), "_")
                val scraperCount = scrapersArray.size
                return@withContext StremioAddon(
                    id = id,
                    name = name,
                    version = version,
                    description = "Nuvio Plugin ($scraperCount scrapers)",
                    iconUrl = null,
                    transportUrl = normalized,
                    types = listOf("movie", "series"),
                    resources = listOf("stream"),
                    catalogs = emptyList(),
                    enabled = true,
                    isDeletable = true,
                )
            }

            // Stremio Addon Manifest
            val id = root["id"]?.jsonPrimitive?.contentOrNull ?: "addon_${System.currentTimeMillis()}"
            val name = root["name"]?.jsonPrimitive?.contentOrNull ?: id
            val version = root["version"]?.jsonPrimitive?.contentOrNull ?: "1.0.0"
            val description = root["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val logo = root["logo"]?.jsonPrimitive?.contentOrNull

            val types = (root["types"] as? JsonArray)?.mapNotNull {
                it.jsonPrimitive.contentOrNull
            } ?: listOf("movie", "series")

            val resources = (root["resources"] as? JsonArray)?.mapNotNull { el ->
                when (el) {
                    is JsonObject -> el["name"]?.jsonPrimitive?.contentOrNull
                    else -> el.jsonPrimitive.contentOrNull
                }
            } ?: listOf("catalog", "meta", "stream")

            val catalogs = (root["catalogs"] as? JsonArray)?.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val cType = obj["type"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val cId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val cName = obj["name"]?.jsonPrimitive?.contentOrNull ?: cId
                StremioCatalog(cType, cId, cName)
            }.orEmpty()

            StremioAddon(
                id = id,
                name = name,
                version = version,
                description = description,
                iconUrl = logo,
                transportUrl = normalized,
                types = types,
                resources = resources,
                catalogs = catalogs,
                enabled = true,
                isDeletable = true,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing manifest from $manifestUrl: ${e.message}")
            null
        }
    }

    suspend fun fetchCatalog(
        manifestUrl: String,
        type: String,
        id: String = "top",
        genre: String? = null,
        search: String? = null,
        skip: Int = 0,
    ): List<MovieItem> = withContext(Dispatchers.IO) {
        val baseUrl = addonTransportBaseUrl(manifestUrl)
        val query = manifestUrl.substringAfter("?", "").let { if (it.isBlank()) "" else "?$it" }

        val url = when {
            !search.isNullOrBlank() -> {
                val encodedSearch = try {
                    URLEncoder.encode(search.trim(), "UTF-8").replace("+", "%20")
                } catch (_: Exception) {
                    search.trim()
                }
                "$baseUrl/catalog/$type/$id/search=$encodedSearch.json$query"
            }
            !genre.isNullOrBlank() -> {
                val encodedGenre = try {
                    URLEncoder.encode(genre.trim(), "UTF-8").replace("+", "%20")
                } catch (_: Exception) {
                    genre.trim()
                }
                if (skip > 0) {
                    "$baseUrl/catalog/$type/$id/genre=$encodedGenre&skip=$skip.json$query"
                } else {
                    "$baseUrl/catalog/$type/$id/genre=$encodedGenre.json$query"
                }
            }
            skip > 0 -> "$baseUrl/catalog/$type/$id/skip=$skip.json$query"
            else -> "$baseUrl/catalog/$type/$id.json$query"
        }

        val body = getJson(url) ?: return@withContext emptyList()
        try {
            val root = json.parseToJsonElement(body).jsonObject
            val metasArray = root["metas"] as? JsonArray ?: return@withContext emptyList()
            metasArray.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val itemId = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val itemName = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val itemType = obj["type"]?.jsonPrimitive?.contentOrNull ?: type

                MovieItem(
                    id = itemId,
                    type = itemType,
                    name = itemName,
                    poster = obj["poster"]?.jsonPrimitive?.contentOrNull,
                    banner = obj["background"]?.jsonPrimitive?.contentOrNull ?: obj["banner"]?.jsonPrimitive?.contentOrNull,
                    imdbRating = obj["imdbRating"]?.jsonPrimitive?.contentOrNull,
                    releaseInfo = obj["releaseInfo"]?.jsonPrimitive?.contentOrNull ?: obj["year"]?.jsonPrimitive?.contentOrNull,
                    genres = (obj["genres"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                    description = obj["description"]?.jsonPrimitive?.contentOrNull,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing catalog from $url: ${e.message}")
            emptyList()
        }
    }

    suspend fun fetchMeta(
        manifestUrl: String,
        type: String,
        id: String,
    ): MovieDetails? = withContext(Dispatchers.IO) {
        val url = buildAddonResourceUrl(manifestUrl, "meta", type, id)
        val body = getJson(url) ?: return@withContext null
        try {
            val root = json.parseToJsonElement(body).jsonObject
            val meta = root["meta"] as? JsonObject ?: return@withContext null

            val metaId = meta["id"]?.jsonPrimitive?.contentOrNull ?: id
            val metaType = meta["type"]?.jsonPrimitive?.contentOrNull ?: type
            val name = meta["name"]?.jsonPrimitive?.contentOrNull ?: metaId

            val videos = (meta["videos"] as? JsonArray)?.mapNotNull { el ->
                val v = el as? JsonObject ?: return@mapNotNull null
                val vId = v["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val vTitle = v["title"]?.jsonPrimitive?.contentOrNull
                    ?: v["name"]?.jsonPrimitive?.contentOrNull
                    ?: "Episode ${v["episode"]?.jsonPrimitive?.intOrNull ?: ""}"

                MovieEpisode(
                    id = vId,
                    title = vTitle,
                    season = v["season"]?.jsonPrimitive?.intOrNull,
                    episode = v["episode"]?.jsonPrimitive?.intOrNull,
                    thumbnail = v["thumbnail"]?.jsonPrimitive?.contentOrNull,
                    overview = v["overview"]?.jsonPrimitive?.contentOrNull ?: v["description"]?.jsonPrimitive?.contentOrNull,
                    released = v["released"]?.jsonPrimitive?.contentOrNull,
                )
            }.orEmpty()

            MovieDetails(
                id = metaId,
                type = metaType,
                name = name,
                // Parse imdbId: prefer explicit "imdb_id" field, then check if the meta id itself is a tt-format IMDb ID
                imdbId = meta["imdb_id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("tt") }
                    ?: meta["imdbId"]?.jsonPrimitive?.contentOrNull?.takeIf { it.startsWith("tt") }
                    ?: metaId.takeIf { it.startsWith("tt") },
                poster = meta["poster"]?.jsonPrimitive?.contentOrNull,
                background = meta["background"]?.jsonPrimitive?.contentOrNull,
                logo = meta["logo"]?.jsonPrimitive?.contentOrNull,
                description = meta["description"]?.jsonPrimitive?.contentOrNull,
                releaseInfo = meta["releaseInfo"]?.jsonPrimitive?.contentOrNull ?: meta["year"]?.jsonPrimitive?.contentOrNull,
                imdbRating = meta["imdbRating"]?.jsonPrimitive?.contentOrNull,
                runtime = meta["runtime"]?.jsonPrimitive?.contentOrNull,
                genres = (meta["genres"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                videos = videos,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing meta details from $url: ${e.message}")
            null
        }
    }

    suspend fun fetchStreams(
        manifestUrl: String,
        type: String,
        id: String,
        addonName: String,
        addonLogo: String? = null,
    ): List<MovieStream> = withContext(Dispatchers.IO) {
        val url = buildAddonResourceUrl(manifestUrl, "stream", type, id)
        val body = getJson(url) ?: return@withContext emptyList()
        try {
            val root = json.parseToJsonElement(body).jsonObject
            val streams = root["streams"] as? JsonArray ?: return@withContext emptyList()

            streams.mapNotNull { el ->
                val obj = el as? JsonObject ?: return@mapNotNull null
                val streamUrl = obj["url"]?.jsonPrimitive?.contentOrNull
                val externalUrl = obj["externalUrl"]?.jsonPrimitive?.contentOrNull
                val infoHash = obj["infoHash"]?.jsonPrimitive?.contentOrNull

                if (streamUrl.isNullOrBlank() && externalUrl.isNullOrBlank() && infoHash.isNullOrBlank()) {
                    return@mapNotNull null
                }

                val hints = obj["behaviorHints"] as? JsonObject
                val reqHeaders = (hints?.get("proxyHeaders") as? JsonObject)?.get("request") as? JsonObject
                val proxyHeaders = reqHeaders?.entries?.associate {
                    it.key to (it.value.jsonPrimitive.contentOrNull.orEmpty())
                }

                val streamName = obj["name"]?.jsonPrimitive?.contentOrNull
                val streamTitle = obj["title"]?.jsonPrimitive?.contentOrNull
                val description = obj["description"]?.jsonPrimitive?.contentOrNull

                MovieStream(
                    name = streamName,
                    title = streamTitle,
                    description = description,
                    url = streamUrl,
                    externalUrl = externalUrl,
                    infoHash = infoHash,
                    fileIdx = obj["fileIdx"]?.jsonPrimitive?.intOrNull,
                    addonName = addonName,
                    addonLogo = addonLogo,
                    proxyHeaders = proxyHeaders,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing streams from $url: ${e.message}")
            emptyList()
        }
    }
}

