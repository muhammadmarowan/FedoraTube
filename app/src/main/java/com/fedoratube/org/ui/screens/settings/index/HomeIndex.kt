package com.fedoratube.org.ui.screens.settings.index

import com.fedoratube.org.R
import com.fedoratube.org.ui.components.settings.SettingAvailability
import com.fedoratube.org.ui.components.settings.SettingEntry
import com.fedoratube.org.ui.components.settings.SettingsDestination

/** Options that live directly on the settings list rather than on a page of their own. */
internal object HomeIndex {
    private val home = SettingsDestination.HOME

    val persona =
        SettingEntry(
            key = "home.persona",
            title = R.string.taste_title,
            summary = R.string.taste_summary,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFlow =
        SettingEntry(
            key = "home.deep_flow",
            title = R.string.deep_flow_mode_title,
            summary = R.string.deep_flow_mode_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFlowDuration =
        SettingEntry(
            key = "home.deep_flow_duration",
            title = R.string.deep_flow_expire_duration_title,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFlowHistory =
        SettingEntry(
            key = "home.deep_flow_history",
            title = R.string.deep_flow_save_history_title,
            summary = R.string.deep_flow_save_history_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFlowScrobble =
        SettingEntry(
            key = "home.deep_flow_scrobble",
            title = R.string.deep_flow_scrobble_title,
            summary = R.string.deep_flow_scrobble_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
    val deepFlowOnSearch =
        SettingEntry(
            key = "home.deep_flow_on_search",
            title = R.string.deep_flow_on_search_title,
            summary = R.string.deep_flow_on_search_subtitle,
            section = R.string.settings_flow_engine_header,
            destination = home,
        )
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
