package com.fedoratube.org.ui.screens.settings.index

import com.fedoratube.org.R
import com.fedoratube.org.ui.components.library.LibrarySection
import com.fedoratube.org.ui.components.settings.SettingEntry
import com.fedoratube.org.ui.components.settings.SettingsDestination

internal object LibrarySectionsIndex {
    private val entries: Map<LibrarySection, SettingEntry> =
        LibrarySection.entries.associateWith { section ->
            SettingEntry(
                key = "library.${section.name.lowercase()}",
                title = section.titleRes,
                section =
                    if (section in
                        LibrarySection.settingsAndData
                    ) {
                        R.string.library_settings_data_header
                    } else {
                        R.string.library_section_header
                    },
                destination = SettingsDestination.LIBRARY,
            )
        }

    fun entry(section: LibrarySection): SettingEntry = entries.getValue(section)

    val all: List<SettingEntry> = LibrarySection.entries.map(::entry)
}
