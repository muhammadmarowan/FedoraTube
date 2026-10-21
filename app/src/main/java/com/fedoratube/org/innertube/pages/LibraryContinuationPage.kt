package com.fedoratube.org.innertube.pages

import com.fedoratube.org.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
