package io.github.aedev.flow.data.update

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private val ReleaseJson = Json { ignoreUnknownKeys = true }
private const val SHA256_PREFIX = "sha256:"
private const val FOSS_PREFIX = "flow-foss"
private const val UNIVERSAL_APK = "flow.apk"
private const val APK_SUFFIX = ".apk"
private val NightlyApk = Regex("""flow-nightly-(\d+)\.apk""", RegexOption.IGNORE_CASE)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
    val digest: String? = null,
)

internal fun parseGitHubRelease(json: String): GitHubRelease = ReleaseJson.decodeFromString(GitHubRelease.serializer(), json)

internal fun parseGitHubReleases(json: String): List<GitHubRelease> =
    runCatching {
        ReleaseJson.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(GitHubRelease.serializer()),
            json,
        )
    }.getOrDefault(emptyList())

/** [release] as an [AppRelease] when it is newer than [currentVersion], otherwise null. */
internal fun GitHubRelease.toAppReleaseIfNewer(
    currentVersion: String,
    supportedAbis: List<String>,
): AppRelease? {
    if (tagName.isBlank() || !AppVersions.isNewer(tagName, currentVersion)) return null
    return AppRelease(
        version = AppVersions.normalize(tagName),
        tag = tagName,
        notes = body.orEmpty(),
        publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pageUrl = htmlUrl,
        apk = selectApk(assets, supportedAbis)?.toReleaseApk(),
    )
}

/**
 * The rolling nightly release as an [AppRelease] when its build is newer than [currentRun]. Nightly
 * builds are told apart by their CI run number, which the APK's name carries, so a local build
 * (run 0) never offers one.
 */
internal fun GitHubRelease.toNightlyReleaseIfNewer(currentRun: Int): AppRelease? {
    if (currentRun <= 0) return null
    val (apk, run) =
        assets
            .firstNotNullOfOrNull { asset -> NightlyApk.matchEntire(asset.name)?.let { asset to it.groupValues[1].toInt() } }
            ?: return null
    if (run <= currentRun) return null
    return AppRelease(
        version = "nightly.$run",
        tag = tagName,
        notes = body.orEmpty(),
        publishedAt = publishedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        pageUrl = htmlUrl,
        apk = apk.toReleaseApk(),
    )
}

/**
 * The github-flavour APK for this device: the split build for its preferred ABI, else the universal
 * `flow.apk`, else any github APK. Foss builds are never picked; they are signed for other stores.
 */
internal fun selectApk(
    assets: List<GitHubAsset>,
    supportedAbis: List<String>,
): GitHubAsset? {
    val github =
        assets.filter {
            it.name.endsWith(APK_SUFFIX, ignoreCase = true) && !it.name.startsWith(FOSS_PREFIX, ignoreCase = true)
        }
    val matchesAbi = { asset: GitHubAsset, abi: String ->
        asset.name.equals("flow-$abi.apk", ignoreCase = true) ||
            asset.name.contains("-$abi.apk", ignoreCase = true) ||
            asset.name.contains("_$abi.apk", ignoreCase = true)
    }
    val splits = github.filter { asset -> supportedAbis.any { abi -> matchesAbi(asset, abi) } }
    if (splits.isNotEmpty()) {
        return supportedAbis.firstNotNullOfOrNull { abi -> splits.firstOrNull { matchesAbi(it, abi) } }
    }
    val publishesSplits = github.any {
        it.name.startsWith("flow-", ignoreCase = true) ||
            it.name.contains("-arm", ignoreCase = true) ||
            it.name.contains("-x86", ignoreCase = true)
    }
    val isUniversal = { name: String -> name.equals(UNIVERSAL_APK, ignoreCase = true) || name.contains("universal", ignoreCase = true) }
    if (publishesSplits && github.none { isUniversal(it.name) }) return null
    return github.firstOrNull { isUniversal(it.name) } ?: github.firstOrNull()
}

private fun GitHubAsset.toReleaseApk(): ReleaseApk =
    ReleaseApk(
        name = name,
        url = downloadUrl,
        sizeBytes = size,
        sha256 = digest?.takeIf { it.startsWith(SHA256_PREFIX) }?.removePrefix(SHA256_PREFIX)?.lowercase(),
    )
