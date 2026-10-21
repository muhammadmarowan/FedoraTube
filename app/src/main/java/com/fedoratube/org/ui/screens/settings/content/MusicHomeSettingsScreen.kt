package com.fedoratube.org.ui.screens.settings.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.music.section.MusicHomeShelf
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.switch
import com.fedoratube.org.ui.screens.settings.index.MusicHomeIndex

/** One switch per music home section; a hidden section is left out of the feed. */
@Composable
internal fun MusicHomeSettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    viewModel: MusicHomeSettingsViewModel = hiltViewModel(),
) {
    val hidden by viewModel.hidden.collectAsStateWithLifecycle()
    SettingsPage(title = stringResource(R.string.settings_music_home_title), onBack = onBack, highlight = highlight) {
        group(key = "music_home.sections", header = R.string.settings_music_home_sections) {
            MusicHomeShelf.entries.forEach { shelf ->
                switch(MusicHomeIndex.entry(shelf), checked = shelf !in hidden, onCheckedChange = { viewModel.setShown(shelf, it) })
            }
        }
    }
}
