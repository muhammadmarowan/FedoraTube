package com.fedoratube.org.widget.core.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.layout.height
import androidx.glance.layout.width

/** Steps across the bar; fine enough that one step is under a pixel on any widget. */
internal const val WIDGET_PROGRESS_MAX = 1000

/**
 * Where a track is now, as a bar value. While playing it runs on from the moment the position was
 * read, at the playback speed, so a render long after the last player event is still right.
 */
internal fun widgetProgressAt(
    positionMs: Long,
    durationMs: Long,
    capturedAtElapsedMs: Long,
    isPlaying: Boolean,
    speed: Float,
    nowElapsedMs: Long,
): Int {
    if (durationMs <= 0L) return 0
    val ran = if (isPlaying && capturedAtElapsedMs > 0L) ((nowElapsedMs - capturedAtElapsedMs).coerceAtLeast(0L) * speed).toLong() else 0L
    val position = (positionMs + ran).coerceIn(0L, durationMs)
    return (position * WIDGET_PROGRESS_MAX / durationMs).toInt()
}

/**
 * The M3 Expressive progress bar. Playing, the played part is a scrolling wave the launcher animates
 * on its own; paused, it is a flat line. The remaining track and its stop dot are flat either way.
 * [width] is the bar's real width, which sizes the wave to the played length. Before Android 12 a
 * platform view cannot take a theme tint, so it falls back to Glance's flat bar.
 */
@Composable
internal fun WidgetWavyProgress(
    progress: Int,
    playing: Boolean,
    width: Dp,
) {
    LinearProgressIndicator(
        progress = (progress.toFloat() / WIDGET_PROGRESS_MAX).coerceIn(0f, 1f),
        modifier = GlanceModifier.width(width).height(4.dp),
        color = GlanceTheme.colors.primary,
        backgroundColor = GlanceTheme.colors.secondaryContainer,
    )
}

private val BarHeight = 12.dp
private val FallbackHeight = 4.dp
