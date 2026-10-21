package com.fedoratube.org.innertube.models.body

import com.fedoratube.org.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
