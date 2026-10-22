package com.fedoratube.org.ui.screens.settings.quality

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fedoratube.org.R
import com.fedoratube.org.data.local.MusicAudioQuality
import com.fedoratube.org.data.local.ThumbnailQuality
import com.fedoratube.org.data.local.VideoCodec
import com.fedoratube.org.data.local.VideoQuality
import com.fedoratube.org.ui.components.settings.SettingsPage
import com.fedoratube.org.ui.components.settings.SettingsTabs
import com.fedoratube.org.ui.components.settings.nav
import com.fedoratube.org.ui.components.settings.notice
import com.fedoratube.org.ui.components.settings.option
import com.fedoratube.org.ui.components.shared.FlowChoice
import com.fedoratube.org.ui.components.shared.FlowChoiceDialog
import com.fedoratube.org.ui.components.shared.FlowConnectedToggleGroup
import com.fedoratube.org.ui.components.shared.FlowToggleOption
import com.fedoratube.org.ui.screens.settings.index.QualityIndex

private val TabsBottomPadding = 8.dp

private enum class QualityPicker {
    VIDEO_WIFI,
    VIDEO_MOBILE,
    CODEC,
    FALLBACK_CODEC,
    THUMBNAILS_WIFI,
    THUMBNAILS_MOBILE,
}

/** Default quality for regular videos, Shorts and music, one media kind at a time. */
@Composable
internal fun QualitySettingsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    tab: String?,
    viewModel: QualitySettingsViewModel = hiltViewModel(),
) {
    val videoWifi by viewModel.videoWifi.collectAsStateWithLifecycle()
    val videoMobile by viewModel.videoMobile.collectAsStateWithLifecycle()
    val shortsWifi by viewModel.shortsWifi.collectAsStateWithLifecycle()
    val shortsMobile by viewModel.shortsMobile.collectAsStateWithLifecycle()
    val music by viewModel.music.collectAsStateWithLifecycle()
    val codec by viewModel.codec.collectAsStateWithLifecycle()
    val fallbackCodec by viewModel.fallbackCodec.collectAsStateWithLifecycle()
    val thumbnailsWifi by viewModel.thumbnailsWifi.collectAsStateWithLifecycle()
    val thumbnailsMobile by viewModel.thumbnailsMobile.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableStateOf(tab ?: SettingsTabs.QUALITY_VIDEO) }
    var picker by rememberSaveable { mutableStateOf<QualityPicker?>(null) }

    val tabs =
        listOf(
            FlowToggleOption(SettingsTabs.QUALITY_VIDEO, stringResource(R.string.settings_quality_tab_video), Icons.Outlined.SmartDisplay),
            FlowToggleOption(SettingsTabs.QUALITY_MUSIC, stringResource(R.string.settings_quality_tab_music), Icons.Outlined.MusicNote),
        )
    val videoWifiLabel = stringResource(videoQualityLabel(videoWifi))
    val videoMobileLabel = stringResource(videoQualityLabel(videoMobile))
    val shortsWifiLabel = stringResource(videoQualityLabel(shortsWifi))
    val shortsMobileLabel = stringResource(videoQualityLabel(shortsMobile))
    val codecLabel = codecLabel(codec)
    val fallbackLabel = codecLabel(fallbackCodec)
    val thumbnailsWifiLabel = stringResource(thumbnailQualityLabel(thumbnailsWifi))
    val thumbnailsMobileLabel = stringResource(thumbnailQualityLabel(thumbnailsMobile))
    val musicLabels = MusicQualities.associateWith { stringResource(musicQualityLabel(it)) }
    val musicAutoSummary = stringResource(R.string.settings_music_quality_auto_summary)

    SettingsPage(
        title = stringResource(R.string.quality),
        onBack = onBack,
        highlight = highlight,
        header = {
            FlowConnectedToggleGroup(
                options = tabs,
                selected = selectedTab,
                onSelected = { selectedTab = it },
                modifier = Modifier.padding(bottom = TabsBottomPadding),
            )
        },
    ) {
        when (selectedTab) {
            SettingsTabs.QUALITY_MUSIC -> {
                group(key = QualityIndex.music.key, header = R.string.music_quality_header) {
                    MusicQualities.forEach { quality ->
                        option(
                            key = "quality.music.${quality.name}",
                            label = musicLabels.getValue(quality),
                            supportingText = musicAutoSummary.takeIf { quality == MusicAudioQuality.AUTO },
                            selected = music == quality,
                            onClick = { viewModel.setMusic(quality) },
                        )
                    }
                }
            }

            else -> {
                group(key = "quality.video.resolution", header = R.string.settings_section_resolution) {
                    nav(
                        QualityIndex.videoWifi,
                        value = videoWifiLabel,
                        showChevron = false,
                        onClick = { picker = QualityPicker.VIDEO_WIFI },
                    )
                    nav(QualityIndex.videoMobile, value = videoMobileLabel, showChevron = false, onClick = {
                        picker =
                            QualityPicker.VIDEO_MOBILE
                    })
                }
                group(key = "quality.video.codec", header = R.string.settings_section_codec) {
                    nav(QualityIndex.codec, value = codecLabel, showChevron = false, onClick = { picker = QualityPicker.CODEC })
                    if (codec != VideoCodec.AUTO) {
                        nav(QualityIndex.fallbackCodec, value = fallbackLabel, showChevron = false, onClick = {
                            picker =
                                QualityPicker.FALLBACK_CODEC
                        })
                    }
                }
                notice("quality.video.notice", text = { stringResource(R.string.video_quality_warning) }, icon = Icons.Outlined.DataUsage)
                group(key = "quality.thumbnails", header = R.string.settings_section_thumbnails) {
                    nav(QualityIndex.thumbnailsWifi, value = thumbnailsWifiLabel, showChevron = false, onClick = {
                        picker = QualityPicker.THUMBNAILS_WIFI
                    })
                    nav(QualityIndex.thumbnailsMobile, value = thumbnailsMobileLabel, showChevron = false, onClick = {
                        picker = QualityPicker.THUMBNAILS_MOBILE
                    })
                }
            }
        }
    }

    picker?.let { open ->
        QualityPickerDialog(
            picker = open,
            state =
                QualityPickerState(
                    videoWifi,
                    videoMobile,
                    shortsWifi,
                    shortsMobile,
                    codec,
                    fallbackCodec,
                    thumbnailsWifi,
                    thumbnailsMobile,
                ),
            viewModel = viewModel,
            onDismiss = { picker = null },
        )
    }
}

private data class QualityPickerState(
    val videoWifi: VideoQuality,
    val videoMobile: VideoQuality,
    val shortsWifi: VideoQuality,
    val shortsMobile: VideoQuality,
    val codec: VideoCodec,
    val fallbackCodec: VideoCodec,
    val thumbnailsWifi: ThumbnailQuality,
    val thumbnailsMobile: ThumbnailQuality,
)

@Composable
private fun QualityPickerDialog(
    picker: QualityPicker,
    state: QualityPickerState,
    viewModel: QualitySettingsViewModel,
    onDismiss: () -> Unit,
) {
    @Composable
    fun resolution(
        title: Int,
        options: List<VideoQuality>,
        selected: VideoQuality,
        onSelect: (VideoQuality) -> Unit,
    ) = FlowChoiceDialog(
        title = stringResource(title),
        options = options.map { FlowChoice(it, stringResource(videoQualityLabel(it))) },
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
    )

    @Composable
    fun thumbnails(
        title: Int,
        selected: ThumbnailQuality,
        onSelect: (ThumbnailQuality) -> Unit,
    ) = FlowChoiceDialog(
        title = stringResource(title),
        options =
            ThumbnailQualities.map {
                FlowChoice(it, stringResource(thumbnailQualityLabel(it)), stringResource(thumbnailQualityDescription(it)))
            },
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
    )

    when (picker) {
        QualityPicker.VIDEO_WIFI -> {
            resolution(R.string.settings_quality_wifi, VideoQualities, state.videoWifi, viewModel::setVideoWifi)
        }

        QualityPicker.VIDEO_MOBILE -> {
            resolution(R.string.settings_quality_mobile, VideoQualities, state.videoMobile, viewModel::setVideoMobile)
        }

        QualityPicker.THUMBNAILS_WIFI -> {
            thumbnails(R.string.settings_quality_wifi, state.thumbnailsWifi, viewModel::setThumbnailsWifi)
        }

        QualityPicker.THUMBNAILS_MOBILE -> {
            thumbnails(R.string.settings_quality_mobile, state.thumbnailsMobile, viewModel::setThumbnailsMobile)
        }

        QualityPicker.CODEC -> {
            val autoDescription = stringResource(R.string.player_settings_video_codec_auto_desc)
            FlowChoiceDialog(
                title = stringResource(R.string.player_settings_video_codec_dialog_title),
                description = stringResource(R.string.player_settings_video_codec_dialog_body),
                options = VideoCodec.entries.map { FlowChoice(it, codecLabel(it), autoDescription.takeIf { _ -> it == VideoCodec.AUTO }) },
                selected = state.codec,
                onSelect = viewModel::setCodec,
                onDismiss = onDismiss,
            )
        }

        QualityPicker.FALLBACK_CODEC -> {
            val autoDescription = stringResource(R.string.player_settings_video_codec_fallback_auto_desc)
            FlowChoiceDialog(
                title = stringResource(R.string.player_settings_video_codec_fallback),
                description = stringResource(R.string.player_settings_video_codec_fallback_dialog_body),
                options =
                    fallbackCodecs(state.codec).map {
                        FlowChoice(
                            it,
                            codecLabel(it),
                            autoDescription.takeIf { _ ->
                                it ==
                                    VideoCodec.AUTO
                            },
                        )
                    },
                selected = state.fallbackCodec,
                onSelect = viewModel::setFallbackCodec,
                onDismiss = onDismiss,
            )
        }
    }
}
