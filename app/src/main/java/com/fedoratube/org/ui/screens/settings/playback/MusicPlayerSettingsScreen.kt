package com.fedoratube.org.ui.screens.settings.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.local.LYRICS_ALIGN_LEFT
import com.fedoratube.org.data.local.LYRICS_ALIGN_RIGHT
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.SettingsTarget
import com.fedoratube.org.ui.components.settings.choice
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.components.settings.switch
import com.fedoratube.org.ui.screens.settings.index.DestinationIndex
import com.fedoratube.org.ui.screens.settings.index.PlaybackIndex

/**
 * Dedicated settings screen for the Music Player under Watching.
 * Allows configuring equalizer, audio enhancements, background music behavior,
 * and comprehensive lyrics settings (translation, romanization, alignment, providers).
 */
@Composable
internal fun MusicPlayerSettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onNavigate: (SettingsTarget) -> Unit,
    viewModel: PlaybackSettingsViewModel = hiltViewModel(),
) {
    var dialog by remember { mutableStateOf<PlaybackDialog?>(null) }

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

        // Lyrics Settings
        group(key = "music_player.lyrics", header = R.string.lyrics) {
            nav(
                PlaybackIndex.lyricsProviders,
                onClick = { dialog = PlaybackDialog.LYRICS },
            )
            choice(
                PlaybackIndex.lyricsAlignment,
                onClick = { dialog = PlaybackDialog.LYRICS_ALIGNMENT },
                valueText = {
                    val align by viewModel.lyricsTextAlign.collectAsStateWithLifecycle()
                    when (align) {
                        LYRICS_ALIGN_LEFT -> stringResource(R.string.lyrics_align_left)
                        LYRICS_ALIGN_RIGHT -> stringResource(R.string.lyrics_align_right)
                        else -> stringResource(R.string.lyrics_align_center)
                    }
                },
            )
            switch(PlaybackIndex.lyricsTranslation, viewModel.lyricsShowTranslation, viewModel::setLyricsShowTranslation)
            switch(PlaybackIndex.lyricsRomanization, viewModel.lyricsShowRomanization, viewModel::setLyricsShowRomanization)
            switch(PlaybackIndex.lyricsAutoRomanize, viewModel.lyricsAutoRomanize, viewModel::setLyricsAutoRomanize)
            switch(PlaybackIndex.lyricsKeepScreenOn, viewModel.lyricsKeepScreenOn, viewModel::setLyricsKeepScreenOn)
        }
    }

    dialog?.let { PlaybackDialogs(it, viewModel, onDismiss = { dialog = null }) }
}
