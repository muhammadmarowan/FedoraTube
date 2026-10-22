package com.fedoratube.org.widget.core.refresh

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetContentSync
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun register() {}
        fun startIfPlaced() {}
    }
