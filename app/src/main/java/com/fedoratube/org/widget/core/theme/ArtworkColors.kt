package com.fedoratube.org.widget.core.theme

import android.content.Context
import androidx.glance.color.ColorProviders
import com.fedoratube.org.data.local.MusicPlayerBackgroundStyle
import com.fedoratube.org.ui.components.musicplayer.common.paletteColorScheme
import com.fedoratube.org.ui.components.shared.mediaPaletteOf
import com.fedoratube.org.widget.core.image.WidgetImageLoader

/**
 * The music player's own artwork scheme, for player widgets while the player is set to take its
 * colours from the cover. Null keeps the app theme: the Default style, or no cover to read.
 */
internal suspend fun artworkColorProviders(
    context: Context,
    artworkUrl: String?,
    style: MusicPlayerBackgroundStyle,
    appColors: ColorProviders,
): ColorProviders? {
    if (style == MusicPlayerBackgroundStyle.DEFAULT) return null
    val bitmap = WidgetImageLoader.load(context, artworkUrl, PALETTE_SAMPLE_PX) ?: return null
    val palette = mediaPaletteOf(bitmap, appColors.surface.getColor(context), appColors.primary.getColor(context))
    val scheme = paletteColorScheme(palette, appColors.error.getColor(context))
    return androidx.glance.material3.ColorProviders(scheme)
}

// The same sample size the in-app player reads its palette from.
private const val PALETTE_SAMPLE_PX = 128
