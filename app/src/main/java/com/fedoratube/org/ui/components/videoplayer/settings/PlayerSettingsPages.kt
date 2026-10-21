package com.fedoratube.org.ui.components.videoplayer.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.player.AudioTrackOption
import com.fedoratube.org.player.QualityOption
import com.fedoratube.org.player.SubtitleOption
import com.fedoratube.org.player.state.SubtitleOrigin
import com.fedoratube.org.player.stream.VideoCodecUtils
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowRowGroup
import com.fedoratube.org.ui.components.shared.FlowSelectionRow
import com.fedoratube.org.ui.components.shared.MediaAudioTrackRow
import com.fedoratube.org.ui.components.shared.MediaPlaybackSpeedPicker
import com.fedoratube.org.ui.components.shared.MediaQualitySelectorContent
import com.fedoratube.org.ui.components.shared.MediaQualitySelectorOption
import com.fedoratube.org.ui.components.shared.audioTrackFallbackLabel
import com.fedoratube.org.ui.components.shared.flowRowGroupShape

@Composable
internal fun PlayerSettingsQualityPage(
    availableQualities: List<QualityOption>,
    currentQuality: Int,
    currentQualityKey: String?,
    useGroupedQualitySelector: Boolean,
    onQualitySelected: (QualityOption) -> Unit,
) {
    val autoLabel = stringResource(R.string.quality_auto)
    val selectorOptions =
        availableQualities.map { quality ->
            MediaQualitySelectorOption(
                item = quality,
                height = quality.height,
                label = if (quality.height == 0) autoLabel else quality.displayLabel(),
                codecKey = quality.codecKey,
                codecLabel =
                    quality.codecKey
                        .takeIf { it.isNotBlank() }
                        ?.let(VideoCodecUtils::codecLabelFromKey)
                        .orEmpty(),
                selected = quality.isSelected(currentQuality, currentQualityKey),
            )
        }

    MediaQualitySelectorContent(
        options = selectorOptions,
        groupedByResolution = useGroupedQualitySelector,
        onOptionSelected = onQualitySelected,
    )
}

private fun QualityOption.displayLabel(): String = label.takeIf { it.isNotBlank() } ?: "${height}p"

private fun QualityOption.isSelected(
    currentQuality: Int,
    currentQualityKey: String?,
): Boolean =
    if (height == 0) {
        currentQuality == 0
    } else {
        streamKey != null && streamKey == currentQualityKey
    }

@Composable
internal fun PlayerSettingsSpeedPage(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onSpeedSelectionFinished: () -> Unit,
) {
    val context = LocalContext.current
    val playerPrefs =
        remember {
            com.fedoratube.org.data.local
                .PlayerPreferences(context)
        }
    val customSpeedsEnabled by playerPrefs.customSpeedsEnabled.collectAsState(initial = false)
    val customSpeedPresetsRaw by playerPrefs.customSpeedPresets.collectAsState(initial = "")
    val speedSliderEnabled by playerPrefs.speedSliderEnabled.collectAsState(initial = false)

    MediaPlaybackSpeedPicker(
        currentSpeed = currentSpeed,
        sliderEnabled = speedSliderEnabled,
        customSpeedsEnabled = customSpeedsEnabled,
        customSpeedPresetsRaw = customSpeedPresetsRaw,
        onSpeedSelected = onSpeedSelected,
        onSpeedRowSelected = { onSpeedSelectionFinished() },
    )
}

@Composable
internal fun PlayerSettingsAudioPage(
    availableAudioTracks: List<AudioTrackOption>,
    currentAudioTrack: Int,
    onTrackSelected: (Int) -> Unit,
) {
    FlowRowGroup {
        availableAudioTracks.forEachIndexed { index, track ->
            MediaAudioTrackRow(
                label = audioTrackDisplayLabel(track, index),
                supportingText = track.language.takeIf { it.isNotBlank() },
                selected = index == currentAudioTrack,
                shape = flowRowGroupShape(index, availableAudioTracks.size),
                onClick = { onTrackSelected(index) },
            )
        }
    }
}

@Composable
internal fun PlayerSettingsSubtitlesPage(
    availableSubtitles: List<SubtitleOption>,
    selectedSubtitleUrl: String?,
    subtitlesEnabled: Boolean,
    onSubtitleSelected: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onShowStyleCustomizer: () -> Unit,
    onAddSubtitleFile: (() -> Unit)? = null,
    subtitleOffsetMs: Long = 0L,
    onSubtitleOffsetChange: ((Long) -> Unit)? = null,
) {
    val rowCount = availableSubtitles.size + 1
    FlowRowGroup {
        FlowSelectionRow(
            title = stringResource(R.string.off),
            selected = !subtitlesEnabled,
            shape = flowRowGroupShape(0, rowCount),
            onClick = onDisableSubtitles,
        )
        availableSubtitles.forEachIndexed { index, subtitle ->
            FlowSelectionRow(
                title = subtitleRowTitle(subtitle, index),
                supportingText =
                    when (subtitle.origin) {
                        SubtitleOrigin.FILE -> {
                            subtitle.detail?.takeIf { it != subtitle.label }
                                ?: stringResource(R.string.subtitle_origin_file)
                        }

                        SubtitleOrigin.EMBEDDED -> {
                            stringResource(R.string.subtitle_origin_embedded)
                        }

                        SubtitleOrigin.ONLINE -> {
                            subtitle.language.takeIf { it.isNotBlank() }
                        }
                    },
                selected = subtitle.url == selectedSubtitleUrl && subtitlesEnabled,
                shape = flowRowGroupShape(index + 1, rowCount),
                onClick = { onSubtitleSelected(index) },
            )
        }
    }
    if (onSubtitleOffsetChange != null && subtitlesEnabled) {
        Spacer(modifier = Modifier.height(SubtitleStyleRowSpacing))
        SubtitleTimingCard(offsetMs = subtitleOffsetMs, onOffsetChange = onSubtitleOffsetChange)
    }
    Spacer(modifier = Modifier.height(SubtitleStyleRowSpacing))
    val navRows = if (onAddSubtitleFile != null) 2 else 1
    FlowRowGroup {
        if (onAddSubtitleFile != null) {
            FlowNavRow(
                title = stringResource(R.string.subtitle_add_file),
                leadingIcon = Icons.Filled.FileOpen,
                shape = flowRowGroupShape(0, navRows),
                onClick = onAddSubtitleFile,
            )
        }
        FlowNavRow(
            title = stringResource(R.string.subtitle_style),
            leadingIcon = Icons.Filled.Tune,
            shape = flowRowGroupShape(navRows - 1, navRows),
            onClick = onShowStyleCustomizer,
        )
    }
}

/** The track's name, with what kind of track it is when that is not a plain authored one. */
@Composable
private fun subtitleRowTitle(
    subtitle: SubtitleOption,
    index: Int,
): String {
    val name = subtitle.label.ifBlank { stringResource(R.string.subtitle_track_number, index + 1) }
    val kind =
        when {
            subtitle.isTranslated -> stringResource(R.string.subtitle_translated)
            subtitle.isAutoGenerated -> stringResource(R.string.quality_auto)
            subtitle.isForced -> stringResource(R.string.subtitle_forced)
            else -> return name
        }
    return stringResource(R.string.subtitle_auto_generated_template, name, kind)
}

private val SubtitleStyleRowSpacing = 12.dp

@Composable
internal fun audioTrackDisplayLabel(
    track: AudioTrackOption?,
    fallbackIndex: Int,
): String = track?.label?.takeIf { it.isNotBlank() } ?: audioTrackFallbackLabel(fallbackIndex)
