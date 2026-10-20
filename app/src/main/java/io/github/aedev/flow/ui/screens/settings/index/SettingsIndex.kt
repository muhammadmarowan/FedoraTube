package io.github.aedev.flow.ui.screens.settings.index

import io.github.aedev.flow.ui.components.settings.SettingEntry

/** Every option in Settings, in the order the pages show them. */
internal object SettingsIndex {
    val all: List<SettingEntry> by lazy {
        HomeIndex.all +
            TasteIndex.all +
            DestinationIndex.all +
            LanguageRegionIndex.all +
            QualityIndex.all +
            TopicsIndex.all +
            BackupIndex.all +
            NotificationsIndex.all +
            HistoryIndex.all +
            WellbeingIndex.all
    }
}
