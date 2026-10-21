package com.fedoratube.org.discord

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscordFossIsolationTest {
    @Test
    fun `foss classpath excludes functional Discord implementation`() {
        val forbiddenClasses = listOf(
            "com.fedoratube.org.discord.DiscordTokenStore",
            "com.fedoratube.org.discord.DiscordAuthTokens",
            "com.fedoratube.org.discord.DiscordPlaybackSource",
            "com.fedoratube.org.discord.DiscordPresenceCoordinator",
            "com.fedoratube.org.discord.KizzyDiscordPresenceTransport",
            "com.fedoratube.org.discord.KizzyGatewayProtocol",
        )

        forbiddenClasses.forEach { className ->
            assertThat(runCatching { Class.forName(className) }.isFailure).isTrue()
        }
    }

    @Test
    fun `foss runtime reports Discord unavailable`() {
        assertThat(DiscordPresenceRuntime.settingsState.value.isAvailable).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.isEnabled).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.summary)
            .isEqualTo(DiscordSettingsSummary.UNAVAILABLE)
    }
}
