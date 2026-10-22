package com.fedoratube.org.widget.core

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import com.fedoratube.org.BuildConfig
import com.fedoratube.org.R
import com.fedoratube.org.widget.core.theme.lastWidgetPreviewStamp
import com.fedoratube.org.widget.core.theme.lastWidgetThemeSignature
import com.fedoratube.org.widget.core.theme.widgetThemeSignatureFlow
import com.fedoratube.org.widget.core.theme.writeWidgetPreviewStamp
import com.fedoratube.org.widget.core.theme.writeWidgetThemeSignature
import com.fedoratube.org.widget.nowplaying.NowPlayingWidget
import com.fedoratube.org.widget.nowplaying.NowPlayingWidgetReceiver
import com.fedoratube.org.widget.quickactions.QuickActionsWidget
import com.fedoratube.org.widget.quickactions.QuickActionsWidgetReceiver
import kotlin.reflect.KClass

/** Registry of every FedoraTube widget: re-renders them on theme changes, publishes previews, lists them in Settings. */
object FedoraWidgets {
    /**
     * Re-renders every placed widget whenever the app's palette actually changes.
     */
    suspend fun observeThemeChanges(context: Context) {
        val appContext = context.applicationContext
        var lastSignature = appContext.lastWidgetThemeSignature()
        widgetThemeSignatureFlow(appContext).collect { signature ->
            val persisted = signature.persistedForm()
            publishPreviewsIfStale(appContext, persisted)
            if (persisted == lastSignature) return@collect
            lastSignature = persisted
            appContext.writeWidgetThemeSignature(persisted)
            updateAll(appContext)
        }
    }

    suspend fun updateAll(context: Context) {
        catalog.forEach { it.widget().updateAll(context) }
    }

    /**
     * Re-renders the picker previews (Android 15+) after an update or a palette change. The platform
     * rate-limits the call, so it runs once per version and theme, never on every launch.
     */
    private suspend fun publishPreviewsIfStale(
        context: Context,
        themeSignature: String,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return
        val stamp = "${BuildConfig.VERSION_CODE}|$themeSignature"
        if (context.lastWidgetPreviewStamp() == stamp) return
        val manager = GlanceAppWidgetManager(context)
        val published =
            catalog.all { entry ->
                manager.setWidgetPreviews(entry.receiver) == GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS
            }
        Log.i(TAG, "Widget previews published: $published")
        if (published) context.writeWidgetPreviewStamp(stamp)
    }

    private const val TAG = "FedoraWidgets"

    val catalog: List<FedoraWidgetEntry> =
        listOf(
            FedoraWidgetEntry(
                id = "now_playing",
                receiver = NowPlayingWidgetReceiver::class,
                widget = ::NowPlayingWidget,
                label = R.string.widget_now_playing_label,
                description = R.string.widget_now_playing_description,
                icon = R.drawable.ic_widget_music,
            ),
            FedoraWidgetEntry(
                id = "quick_actions",
                receiver = QuickActionsWidgetReceiver::class,
                widget = ::QuickActionsWidget,
                label = R.string.widget_quick_actions_label,
                description = R.string.widget_quick_actions_description,
                icon = R.drawable.ic_widget_search,
            ),
        )
}

/** One widget as Settings lists it: what to pin, and how to describe it. */
class FedoraWidgetEntry(
    val id: String,
    val receiver: KClass<out GlanceAppWidgetReceiver>,
    val widget: () -> GlanceAppWidget,
    @StringRes val label: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
)

typealias FlowWidgets = FedoraWidgets
typealias FlowWidgetEntry = FedoraWidgetEntry

