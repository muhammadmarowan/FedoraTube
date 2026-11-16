package com.fedoratube.org.ui.screens.settings.playback

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.SettingsTarget
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.components.settings.switch
import com.fedoratube.org.ui.screens.settings.index.DestinationIndex
import com.fedoratube.org.ui.screens.settings.index.PlaybackIndex

/**
 * Dedicated settings screen for the Music Player under Watching.
 * Allows configuring equalizer, audio enhancements, background music behavior,
 * and player presentation.
 */
@Composable
internal fun MusicPlayerSettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onNavigate: (SettingsTarget) -> Unit,
    viewModel: PlaybackSettingsViewModel = hiltViewModel(),
) {
    SettingsPage(
        title = stringResource(R.string.settings_music_player_title),
        onBack = onBack,
        highlight = highlight,
    ) {
        // Audio & Processing
        group(key = "music_player.audio", header = R.string.eq_settings_section) {
            nav(
                DestinationIndex.entry(SettingsDestination.EQUALIZER),
                onClick = { onNavigate(SettingsTarget(SettingsDestination.EQUALIZER)) },
            )
            switch(PlaybackIndex.skipSilence, viewModel.skipSilence, viewModel::setSkipSilence)
            switch(PlaybackIndex.stableVoice, viewModel.stableVolume, viewModel::setStableVolume)
        }

        // Music Playback Behavior
        group(key = "music_player.behavior", header = R.string.playback_header) {
            switch(PlaybackIndex.musicNormalSpeed, viewModel.musicNormalSpeed, viewModel::setMusicNormalSpeed)
            switch(PlaybackIndex.openMusicPlayerOnPlay, viewModel.openMusicPlayerOnPlay, viewModel::setOpenMusicPlayerOnPlay)
            switch(PlaybackIndex.restoreMusicMiniPlayer, viewModel.restoreMusicMiniPlayer, viewModel::setRestoreMusicMiniPlayer)
            switch(PlaybackIndex.pauseMusicWhenMuted, viewModel.pauseMusicWhenMuted, viewModel::setPauseMusicWhenMuted)
            switch(PlaybackIndex.musicVideoSwitch, viewModel.musicVideoSwitch, viewModel::setMusicVideoSwitch)
            switch(PlaybackIndex.playDuringCalls, viewModel.playDuringCalls, viewModel::setPlayDuringCalls)
        }
    }
}
