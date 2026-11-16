package com.fedoratube.org.ui.screens.settings.playback

import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.ui.components.settings.SettingsListScope
import com.fedoratube.org.ui.components.settings.SettingsTarget
import com.fedoratube.org.ui.components.settings.choice
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.components.settings.switch
import com.fedoratube.org.ui.screens.settings.index.DestinationIndex
import com.fedoratube.org.ui.screens.settings.index.PlaybackIndex

/**
 * Player settings matching the exact controls used in video and music playback:
 * Background play, autoplay, loop, PiP, equalizer, skip silence, stable voice,
 * playback speed, double tap seek, captions, audio language, and ambient mode.
 */
internal fun SettingsListScope.playbackSections(
    viewModel: PlaybackSettingsViewModel,
    state: PlaybackStructure,
    openDialog: (PlaybackDialog) -> Unit,
    onNavigate: (SettingsTarget) -> Unit,
) {
    // 1. General Playback (Background play, Autoplay, Loop, Picture-in-Picture)
    group(key = "playback.general", header = R.string.playback_header) {
        switch(PlaybackIndex.backgroundPlay, viewModel.backgroundPlay, viewModel::setBackgroundPlay)
        switch(PlaybackIndex.autoplay, viewModel.autoplay, viewModel::setAutoplay, enabled = !state.loopAll)
        switch(PlaybackIndex.loopAll, viewModel.loopAll, viewModel::setLoopAll)
        switch(PlaybackIndex.autoPip, viewModel.autoPip, viewModel::setAutoPip)
    }

    // 2. Audio & Effects (Equalizer, Skip silence, Stable voice)
    group(key = "playback.sound", header = R.string.eq_settings_section) {
        nav(DestinationIndex.entry(SettingsDestination.EQUALIZER), onClick = { onNavigate(SettingsTarget(SettingsDestination.EQUALIZER)) })
        switch(PlaybackIndex.skipSilence, viewModel.skipSilence, viewModel::setSkipSilence)
        switch(PlaybackIndex.stableVoice, viewModel.stableVolume, viewModel::setStableVolume)
    }

    // 3. Speed & Gestures (Remember speed, Double tap seek)
    group(key = "playback.speed", header = R.string.settings_section_speed) {
        switch(PlaybackIndex.rememberSpeed, viewModel.rememberSpeed, viewModel::setRememberSpeed)
        choice(PlaybackIndex.doubleTapSeek, onClick = { openDialog(PlaybackDialog.DOUBLE_TAP_SEEK) }) {
            val seconds by viewModel.doubleTapSeek.collectAsStateWithLifecycle()
            if (seconds <= 0) {
                stringResource(R.string.player_settings_double_tap_seek_off)
            } else {
                pluralStringResource(R.plurals.player_settings_double_tap_seek_subtitle, seconds, seconds)
            }
        }
    }

    // 4. Captions & Audio Tracks (Auto captions, Subtitle language, Audio track)
    group(key = "playback.languages", header = R.string.settings_section_language_captions) {
        switch(PlaybackIndex.autoCaptions, viewModel.autoCaptions, viewModel::setAutoCaptions)
        choice(PlaybackIndex.subtitleLanguage, onClick = { openDialog(PlaybackDialog.SUBTITLE_LANGUAGE) }) {
            val code by viewModel.subtitleLanguage.collectAsStateWithLifecycle()
            subtitleLanguageLabel(code)
        }
        choice(PlaybackIndex.audioLanguage, onClick = { openDialog(PlaybackDialog.AUDIO_LANGUAGE) }) {
            val code by viewModel.audioLanguage.collectAsStateWithLifecycle()
            audioLanguageLabel(code)
        }
    }

    // 5. Display & Ambient Mode
    group(key = "playback.display", header = R.string.settings_section_watch_page) {
        switch(PlaybackIndex.ambientMode, viewModel.ambientMode, viewModel::setAmbientMode)
    }
}
