package com.fedoratube.org.data.movies.nuvio

import android.util.Log
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.asyncFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val MAX_FETCH_HEADER_VALUE_CHARS = 8 * 1024
private const val FETCH_TRUNCATION_SUFFIX = "\n...[truncated]"

internal class FetchBridge : HostModule {
    private val tag = "FetchBridge"
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override fun register(runtime: QuickJs) {
        runtime.asyncFunction("__native_fetch") { args ->
            val url = args.getOrNull(0)?.toString() ?: ""
            val method = args.getOrNull(1)?.toString() ?: "GET"
            val headersJson = args.getOrNull(2)?.toString() ?: "{}"
            val bodyKind = args.getOrNull(3)?.toString() ?: "none"
            val body = args.getOrNull(4)?.toString() ?: ""
            val followRedirects = args.getOrNull(5) as? Boolean ?: true
            try {
                performNativeFetch(url, method, headersJson, bodyKind, body, followRedirects)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                Log.e(tag, "Fetch bridge error for $method $url: ${t.message}")
                JsonObject(
                    mapOf(
                        "ok" to JsonPrimitive(false),
                        "status" to JsonPrimitive(0),
                        "statusText" to JsonPrimitive(t.message ?: "Fetch failed"),
                        "url" to JsonPrimitive(url),
                        "body" to JsonPrimitive(""),
                        "bodyBase64" to JsonPrimitive(""),
                        "headers" to JsonObject(emptyMap()),
                    ),
                ).toString()
            }
        }
    }

    private suspend fun performNativeFetch(
        url: String,
        method: String,
        headersJson: String,
        bodyKind: String,
        body: String,
        followRedirects: Boolean,
    ): String = withContext(Dispatchers.IO) {
        val headers = parseHeaders(headersJson).toMutableMap()
        if (!headers.containsKey("User-Agent") && !headers.containsKey("user-agent")) {
            headers["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        }

        val normalizedMethod = method.uppercase()
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) ->
            if (!k.equals("Host", ignoreCase = true) && !k.equals("Content-Length", ignoreCase = true)) {
                builder.header(k, v)
            }
        }

        if (normalizedMethod == "POST" || normalizedMethod == "PUT" || normalizedMethod == "PATCH") {
            val contentType = headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
                ?: if (normalizedMethod == "POST") "application/x-www-form-urlencoded" else "application/json"
            val bodyBytes = decodeBinaryBody(bodyKind, body) ?: body.toByteArray(Charsets.UTF_8)
            builder.method(normalizedMethod, bodyBytes.toRequestBody(contentType.toMediaType()))
        } else {
            builder.method(normalizedMethod, null)
        }

        val client = if (followRedirects) httpClient else httpClient.newBuilder().followRedirects(false).followSslRedirects(false).build()

        client.newCall(builder.build()).execute().use { response ->
            val bytes = response.body?.bytes() ?: ByteArray(0)
            val decodedBody = try {
                String(bytes, Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }

            val responseHeaders = response.headers.toMultimap().mapValues { (_, values) ->
                truncateString(values.joinToString(","), MAX_FETCH_HEADER_VALUE_CHARS)
            }.mapKeys { (name, _) -> name.lowercase() }

            val result = JsonObject(
                mapOf(
                    "ok" to JsonPrimitive(response.isSuccessful),
                    "status" to JsonPrimitive(response.code),
                    "url" to JsonPrimitive(response.request.url.toString()),
                    "statusText" to JsonPrimitive(response.message),
                    "body" to JsonPrimitive(decodedBody),
                    "bodyBase64" to JsonPrimitive(encodeBase64(bytes)),
                    "headers" to JsonObject(responseHeaders.mapValues { JsonPrimitive(it.value) }),
                ),
            )
            result.toString()
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeBinaryBody(bodyKind: String, body: String): ByteArray? = when (bodyKind) {
        "base64" -> Base64.decode(body)
        "none", "text" -> null
        else -> null
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun encodeBase64(bytes: ByteArray): String = Base64.encode(bytes)

    private fun parseHeaders(headersJson: String): Map<String, String> {
        return runCatching {
            val obj = json.parseToJsonElement(headersJson) as? JsonObject ?: JsonObject(emptyMap())
            obj.entries.mapNotNull { (key, value) ->
                value.jsonPrimitive.contentOrNull?.let { key to it }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    private fun truncateString(value: String, maxChars: Int): String {
        if (value.length <= maxChars) return value
        val end = maxChars - FETCH_TRUNCATION_SUFFIX.length
        if (end <= 0) return FETCH_TRUNCATION_SUFFIX.take(maxChars)
        return value.substring(0, end) + FETCH_TRUNCATION_SUFFIX
    }
}

