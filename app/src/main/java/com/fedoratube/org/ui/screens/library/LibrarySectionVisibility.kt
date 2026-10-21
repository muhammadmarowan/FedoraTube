package com.fedoratube.org.ui.screens.library

import com.fedoratube.org.ui.components.library.LibrarySection

/** Saved Shorts goes with the Shorts content switch as well as its own. */
internal fun visibleContentSections(
    hidden: Set<LibrarySection>,
    separatePlaylistKinds: Boolean,
    shortsEnabled: Boolean,
): List<LibrarySection> =
    LibrarySection
        .content(separatePlaylistKinds)
        .filter { it !in hidden && (shortsEnabled || it != LibrarySection.SAVED_SHORTS) }

/** Notes goes with the notes switch in Content settings as well as its own. */
internal fun visibleSettingsSections(
    hidden: Set<LibrarySection>,
    notesEnabled: Boolean,
): List<LibrarySection> = LibrarySection.settingsAndData.filter { it !in hidden && (notesEnabled || it != LibrarySection.NOTES) }
