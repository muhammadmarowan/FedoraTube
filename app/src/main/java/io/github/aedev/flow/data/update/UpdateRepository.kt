package io.github.aedev.flow.data.update

import android.os.Build
import io.github.aedev.flow.BuildConfig
import io.github.aedev.flow.data.local.LocalDataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val STABLE_RELEASE_API = "https://api.github.com/repos/muhammadmarowan/FedoraTube/releases/latest"
private const val ALL_RELEASES_API = "https://api.github.com/repos/muhammadmarowan/FedoraTube/releases"
private const val NIGHTLY_RELEASE_API = "https://api.github.com/repos/muhammadmarowan/FedoraTube/releases/tags/nightly"
private const val NIGHTLY_CHANNEL = "nightly"
private const val GITHUB_JSON = "application/vnd.github+json"

/** Where an automatic check may surface a release; each announces a version once. */
enum class UpdateAnnouncement { LAUNCH_PAGE, NOTIFICATION }

/** How the automatic checks space themselves out and which releases they may show. */
internal object UpdateSchedule {
    val COOLDOWN_MS: Long = TimeUnit.MINUTES.toMillis(5)

    fun isDue(
        lastCheckMs: Long,
        nowMs: Long,
    ): Boolean = nowMs < lastCheckMs || nowMs - lastCheckMs >= COOLDOWN_MS

    fun mayAnnounce(
        version: String,
        skippedVersion: String?,
        announcedVersion: String?,
    ): Boolean = version != skippedVersion && version != announcedVersion
}

/**
 * The one place Flow asks GitHub about releases. The launch check, the background worker and the
 * Settings button all come through here, so a cold start never fetches twice and a skipped version
 * stays skipped everywhere.
 */
@Singleton
class UpdateRepository
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dataManager: LocalDataManager,
    ) {
        private val mutex = Mutex()
        private val isNightly = BuildConfig.UPDATE_CHANNEL == NIGHTLY_CHANNEL
        private val _latest = MutableStateFlow<AppRelease?>(null)
        private val _allReleases = MutableStateFlow<List<GitHubRelease>>(emptyList())

        /** The newest release found this process, or null when none is known or this build is current. */
        val latest: StateFlow<AppRelease?> = _latest.asStateFlow()

        /** All releases fetched from GitHub. */
        val allReleases: StateFlow<List<GitHubRelease>> = _allReleases.asStateFlow()

        /** Asks GitHub now. Throws when the request fails, so a check the user started can say so. */
        suspend fun fetch(): AppRelease? = mutex.withLock { fetchLocked() }

        /**
         * For the automatic checks: fetches only once the cooldown has passed, and returns a release
         * only if [announcement] has not shown this version before and the user has not skipped it.
         */
        suspend fun releaseToAnnounce(
            announcement: UpdateAnnouncement,
            nowMs: Long = System.currentTimeMillis(),
        ): AppRelease? =
            mutex.withLock {
                if (!UpdateSchedule.isDue(dataManager.lastUpdateCheck.first(), nowMs)) return@withLock null
                val release = runCatching { fetchLocked() }.getOrNull() ?: return@withLock null
                val announced =
                    when (announcement) {
                        UpdateAnnouncement.LAUNCH_PAGE -> dataManager.promptedUpdateVersion.first()
                        UpdateAnnouncement.NOTIFICATION -> dataManager.notifiedUpdateVersion.first()
                    }
                if (!UpdateSchedule.mayAnnounce(release.version, dataManager.skippedUpdateVersion.first(), announced)) {
                    return@withLock null
                }
                when (announcement) {
                    UpdateAnnouncement.LAUNCH_PAGE -> dataManager.setPromptedUpdateVersion(release.version)
                    UpdateAnnouncement.NOTIFICATION -> dataManager.setNotifiedUpdateVersion(release.version)
                }
                release
            }

        suspend fun skip(version: String) = dataManager.setSkippedUpdateVersion(version)

        private suspend fun fetchLocked(): AppRelease? {
            val parsed =
                withContext(Dispatchers.IO) {
                    if (!isNightly) {
                        val allReleasesRequest =
                            Request
                                .Builder()
                                .url(ALL_RELEASES_API)
                                .header("Accept", GITHUB_JSON)
                                .cacheControl(CacheControl.FORCE_NETWORK)
                                .build()
                        val allResponse = runCatching { client.newCall(allReleasesRequest).execute() }.getOrNull()
                        if (allResponse != null && allResponse.isSuccessful) {
                            val body = allResponse.body.string()
                            allResponse.close()
                            val releases = parseGitHubReleases(body)
                            if (releases.isNotEmpty()) {
                                _allReleases.value = releases
                                return@withContext releases.first()
                            }
                        } else {
                            allResponse?.close()
                        }
                    }

                    val primaryUrl = if (isNightly) NIGHTLY_RELEASE_API else STABLE_RELEASE_API
                    val request =
                        Request
                            .Builder()
                            .url(primaryUrl)
                            .header("Accept", GITHUB_JSON)
                            .cacheControl(CacheControl.FORCE_NETWORK)
                            .build()
                    val response = runCatching { client.newCall(request).execute() }.getOrNull()
                    if (response != null && response.isSuccessful) {
                        response.use { parseGitHubRelease(it.body.string()) }
                    } else {
                        val code = response?.code ?: "Unknown"
                        response?.close()
                        throw IOException("Update check failed: HTTP $code")
                    }
                }
            val release =
                if (isNightly) {
                    parsed.toNightlyReleaseIfNewer(BuildConfig.NIGHTLY_RUN)
                } else {
                    parsed.toAppReleaseIfNewer(BuildConfig.VERSION_NAME, Build.SUPPORTED_ABIS.toList())
                }
            dataManager.setLastUpdateCheck(System.currentTimeMillis())
            _latest.value = release
            return release
        }
    }
