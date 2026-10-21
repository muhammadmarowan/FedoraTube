package com.fedoratube.org.innertube.pages

import com.fedoratube.org.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
