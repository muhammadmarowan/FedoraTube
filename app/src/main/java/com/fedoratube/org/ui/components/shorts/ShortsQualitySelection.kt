package com.fedoratube.org.ui.components.shorts

import com.fedoratube.org.data.shorts.ShortVideoQuality
import com.fedoratube.org.player.QualityOption
import com.fedoratube.org.player.stream.VideoCodecUtils

internal fun findActiveShortQuality(
    qualities: List<ShortVideoQuality>,
    currentVideoUrl: String?,
    activeVideoWidth: Int,
    activeVideoHeight: Int,
    activeCodecKey: String?,
): ShortVideoQuality? {
    currentVideoUrl
        ?.takeIf { it.isNotBlank() }
        ?.let { url -> qualities.firstOrNull { it.videoUrl == url } }
        ?.let { return it }

    val dimensions = listOf(activeVideoWidth, activeVideoHeight).filter { it > 0 }
    val activeHeightClass =
        dimensions
            .minOrNull()
            ?.let(VideoCodecUtils::normalizeQualityHeight)
            ?.takeIf { it > 0 }
            ?: return null

    return qualities.firstOrNull { quality ->
        quality.heightClass == activeHeightClass &&
            activeCodecKey?.takeIf { it.isNotBlank() } == quality.codecKey
    } ?: qualities.firstOrNull { it.heightClass == activeHeightClass }
}

/** The codec rides in the label as the main player's options do, so list mode can tell "2160p AV1" from "2160p VP9". */
internal fun ShortVideoQuality.toQualityOption(): QualityOption =
    QualityOption(
        height = heightClass,
        label = listOf(label, codecLabel).filter(String::isNotBlank).joinToString(" "),
        bitrate = 0L,
        codecKey = codecKey,
        streamKey = videoUrl,
    )
