package com.fedoratube.org.data.movies

import java.net.URLEncoder

internal fun normalizeManifestUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""

    val normalizedScheme = when {
        trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
        trimmed.startsWith("stremio://", ignoreCase = true) -> "https://${trimmed.substring(10)}"
        trimmed.startsWith("nuvio://", ignoreCase = true) -> "https://${trimmed.substring(8)}"
        else -> "https://$trimmed"
    }

    val withoutFragment = normalizedScheme.substringBefore("#")
    val query = withoutFragment.substringAfter("?", "")
    val path = withoutFragment.substringBefore("?").trimEnd('/')
    val manifestPath = if (path.endsWith("/manifest.json", ignoreCase = true)) {
        path
    } else {
        "$path/manifest.json"
    }

    return if (query.isEmpty()) manifestPath else "$manifestPath?$query"
}

internal fun addonTransportBaseUrl(manifestUrl: String): String =
    manifestUrl.substringBefore("?").removeSuffix("/manifest.json").trimEnd('/')

internal fun buildAddonResourceUrl(
    manifestUrl: String,
    resource: String,
    type: String,
    id: String,
    extraPath: String? = null,
): String {
    val baseUrl = addonTransportBaseUrl(manifestUrl)
    val encodedId = try {
        URLEncoder.encode(id, "UTF-8").replace("+", "%20")
    } catch (_: Exception) {
        id
    }
    val query = manifestUrl.substringAfter("?", "").let { if (it.isBlank()) "" else "?$it" }
    val resourceUrl = if (extraPath.isNullOrEmpty()) {
        "$baseUrl/$resource/$type/$encodedId.json"
    } else {
        "$baseUrl/$resource/$type/$encodedId/$extraPath.json"
    }
    return resourceUrl + query
}

