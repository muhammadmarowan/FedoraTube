package com.fedoratube.org.ui.screens.settings.index

import com.fedoratube.org.R
import com.fedoratube.org.ui.components.settings.SettingEntry
import com.fedoratube.org.ui.components.settings.SettingsDestination
import com.fedoratube.org.widget.core.FlowWidgetEntry
import com.fedoratube.org.widget.core.FlowWidgets

internal object WidgetsIndex {
    fun entryFor(widget: FlowWidgetEntry) =
        SettingEntry(
            key = "widgets.${widget.id}",
            title = widget.label,
            summary = widget.description,
            section = R.string.settings_widgets_add_header,
            keywords = R.string.settings_keywords_widgets,
            destination = SettingsDestination.WIDGETS,
        )

    val all = FlowWidgets.catalog.map(::entryFor)
}
