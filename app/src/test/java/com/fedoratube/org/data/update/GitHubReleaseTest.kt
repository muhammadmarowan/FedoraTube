package com.fedoratube.org.data.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.Instant

class GitHubReleaseTest {
    private fun asset(
        name: String,
        size: Long = 1,
        digest: String? = null,
    ) = GitHubAsset(name, "https://example.test/$name", size, digest)

    private val splits = listOf(asset("flow-armeabi-v7a.apk"), asset("flow-foss-arm64-v8a.apk"), asset("flow-arm64-v8a.apk"))

    @Test
    fun `an arm64 device gets the arm64 github build`() {
        assertThat(selectApk(splits, listOf("arm64-v8a", "armeabi-v7a"))?.name).isEqualTo("flow-arm64-v8a.apk")
    }

    @Test
    fun `an arm32 device gets the armv7 build`() {
        assertThat(selectApk(splits, listOf("armeabi-v7a", "armeabi"))?.name).isEqualTo("flow-armeabi-v7a.apk")
    }

    @Test
    fun `an unpublished abi gets nothing when there is no universal build`() {
        assertThat(selectApk(splits, listOf("x86_64", "x86"))).isNull()
    }

    @Test
    fun `an unpublished abi falls back to the universal build`() {
        assertThat(selectApk(splits + asset("flow.apk"), listOf("x86_64"))?.name).isEqualTo("flow.apk")
    }

    @Test
    fun `a foss build is never picked`() {
        assertThat(selectApk(listOf(asset("flow-foss.apk"), asset("flow.apk")), listOf("arm64-v8a"))?.name).isEqualTo("flow.apk")
    }

    @Test
    fun `versions compare numerically and ignore the prefix and suffix`() {
        assertThat(AppVersions.isNewer("v2.10.0", "2.9.9")).isTrue()
        assertThat(AppVersions.isNewer("v2.2.5", "2.2.5-debug")).isFalse()
        assertThat(AppVersions.isNewer("2.2", "2.2.0")).isFalse()
        assertThat(AppVersions.isNewer("v2.2.1", "2.3.0")).isFalse()
    }

    @Test
    fun `a newer release carries its apk size, digest and date`() {
        val json =
            """
            {"tag_name":"v2.3.0","body":"## New features","html_url":"https://github.com/Flow-Tube/Flow/releases/tag/v2.3.0",
             "published_at":"2026-09-28T10:00:00Z","draft":false,
             "assets":[{"name":"flow-arm64-v8a.apk","browser_download_url":"https://example.test/a","size":17613193,
                        "digest":"sha256:F900AB"}]}
            """.trimIndent()

        val release = parseGitHubRelease(json).toAppReleaseIfNewer("2.2.5", listOf("arm64-v8a"))

        assertThat(release?.version).isEqualTo("2.3.0")
        assertThat(release?.publishedAt).isEqualTo(Instant.parse("2026-09-28T10:00:00Z"))
        assertThat(release?.apk).isEqualTo(ReleaseApk("flow-arm64-v8a.apk", "https://example.test/a", 17613193, "f900ab"))
    }

    @Test
    fun `the running version is not an update`() {
        val release = GitHubRelease(tagName = "v2.2.5").toAppReleaseIfNewer("2.2.5", listOf("arm64-v8a"))

        assertThat(release).isNull()
    }

    @Test
    fun `a release tagged nightly is hidden from the updater`() {
        assertThat(GitHubRelease(tagName = "nightly").isNightlyRelease()).isTrue()
    }

    @Test
    fun `a release carrying a nightly apk is hidden from the updater`() {
        val legacy = GitHubRelease(tagName = "v1.6.0", assets = listOf(asset("flow-nightly-1430.apk")))

        assertThat(legacy.isNightlyRelease()).isTrue()
    }

    @Test
    fun `a stable release is not hidden`() {
        val stable = GitHubRelease(tagName = "v1.7.5", assets = listOf(asset("flow.apk")))

        assertThat(stable.isNightlyRelease()).isFalse()
    }

    @Test
    fun `a prerelease never becomes an update`() {
        val pre = GitHubRelease(tagName = "v1.7.7", prerelease = true)

        assertThat(pre.isStableRelease()).isFalse()
        assertThat(pre.toAppReleaseIfNewer("1.7.6", listOf("arm64-v8a"))).isNull()
    }

    @Test
    fun `a draft never becomes an update`() {
        val draft = GitHubRelease(tagName = "v1.7.8", draft = true)

        assertThat(draft.isStableRelease()).isFalse()
        assertThat(draft.toAppReleaseIfNewer("1.7.6", listOf("arm64-v8a"))).isNull()
    }

    @Test
    fun `a stable release stays an update`() {
        val stable = GitHubRelease(tagName = "v1.7.7", prerelease = false, draft = false)

        assertThat(stable.isStableRelease()).isTrue()
        assertThat(stable.toAppReleaseIfNewer("1.7.6", listOf("arm64-v8a"))?.version).isEqualTo("1.7.7")
    }

    @Test
    fun `the stable updater never picks a nightly apk`() {
        assertThat(selectApk(listOf(asset("flow-nightly-1430.apk")), listOf("arm64-v8a"))).isNull()
    }
}
