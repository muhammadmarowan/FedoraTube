package com.fedoratube.org.widget.onrepeat

import androidx.glance.appwidget.GlanceAppWidget
import com.fedoratube.org.data.music.model.MusicTrack
import com.fedoratube.org.data.recommendation.music.MusicBrainEngine
import com.fedoratube.org.data.recommendation.music.onRepeatShelf
import com.fedoratube.org.widget.core.refresh.WidgetContentKey
import com.fedoratube.org.widget.core.refresh.WidgetContentSource
import javax.inject.Inject
import javax.inject.Singleton

/** The Music page's On Repeat shelf. The brain has no change stream, so a finished listen requests a refresh. */
@Singleton
class OnRepeatSource
    @Inject
    constructor(
        private val musicBrain: MusicBrainEngine,
    ) : WidgetContentSource {
        override val key = WidgetContentKey.ON_REPEAT
        override val widget: GlanceAppWidget get() = OnRepeatWidget()

        suspend fun tracks(): List<MusicTrack> = musicBrain.onRepeatShelf().take(MAX_ITEMS)

        override suspend fun signature(): String = tracks().joinToString(",") { it.videoId }

        companion object {
            const val MAX_ITEMS = 8
        }
    }
