package com.fedoratube.org.ui.screens.settings.index

import com.fedoratube.org.R
import com.fedoratube.org.ui.components.settings.SettingAvailability
import com.fedoratube.org.ui.components.settings.SettingEntry
import com.fedoratube.org.ui.components.settings.SettingsDestination

/** Options that live directly on the settings list rather than on a page of their own. */
internal object HomeIndex {
    private val home = SettingsDestination.HOME

    val checkForUpdates =
        SettingEntry(
            key = "home.updates",
            title = R.string.check_for_updates,
            summary = R.string.check_for_updates_subtitle,
            section = R.string.settings_header_about,
            destination = home,
            availability = SettingAvailability.GithubOnly,
        )

    val focusMode =
        SettingEntry(
            key = "home.focus_mode",
            title = R.string.focus_mode_title,
            summary = R.string.focus_mode_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )

    val all = listOf(focusMode, checkForUpdates)
}
