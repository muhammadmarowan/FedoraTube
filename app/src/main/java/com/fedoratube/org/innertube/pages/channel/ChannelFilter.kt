package com.fedoratube.org.innertube.pages.channel

import com.fedoratube.org.innertube.pages.arrayOrNull
import com.fedoratube.org.innertube.pages.objectOrNull
import com.fedoratube.org.innertube.pages.renderer.browseParams
import com.fedoratube.org.innertube.pages.renderer.forEachObject
import com.fedoratube.org.innertube.pages.stringOrNull
import com.fedoratube.org.innertube.pages.youtubeText
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * One control inside a tab's filter menu. A tab can carry several — the Videos tab offers its sort
 * entries *and* a visibility choice (Members only & the like) — and they filter independently.
 * Every group is listed in the banner's top-right arrow; none draws its own chip row any more.
 */
data class ChannelFilterGroup(
    val title: String?,
    val isDropdown: Boolean,
    val options: List<ChannelFilterOption>,
    /**
     * Sort controls arrive with nothing flagged: the server's default (Latest) is implicit and the
     * menu marks the first entry itself. Chips flag their own selection, so an empty
     * [selectedIndex] there means nothing was picked rather than the first option.
     */
    val marksFirstByDefault: Boolean = false,
) {
    val selectedIndex: Int get() = options.indexOfFirst { it.selected }
}

/**
 * A filter arrives as one of two commands and they are not interchangeable: chips and dropdown
 * entries carry a [continuation], while the playlists sort menu carries browse [params] and has to be
 * re-requested as a fresh browse.
 */
data class ChannelFilterOption(
    val label: String,
    val selected: Boolean = false,
    val continuation: String? = null,
    val params: String? = null,
)

private val SORT_LIKE_LABELS =
    setOf(
        "latest",
        "oldest",
        "newest",
        "popular",
        "top",
        "views",
        "most views",
        "trending",
        "date added",
        "date added (newest)",
        "date added (oldest)",
        "last video added",
    )

/** A chip row whose every chip is a sort-ish label is the tab's sort control — promote it to a dropdown. */
private fun List<ChannelFilterOption>.looksLikeSort(): Boolean =
    size >= 2 && all { it.label.trim().lowercase() in SORT_LIKE_LABELS }

internal fun JsonElement.channelFilterGroups(
    kind: ChannelTabKind? = null,
): List<ChannelFilterGroup> {
    val groups = mutableListOf<ChannelFilterGroup>()
    val chips = mutableListOf<ChannelFilterOption>()

    findFirstRenderer("chipBarViewModel")
        .objectOrNull()
        ?.get("chips")
        .arrayOrNull()
        .orEmpty()
        .forEach { entry ->
            val chip = entry.objectOrNull()?.get("chipViewModel").objectOrNull() ?: return@forEach
            val dropdown = chip.dropdownOptions()
            if (dropdown.isNotEmpty()) {
                groups +=
                    ChannelFilterGroup(
                        title = null,
                        isDropdown = true,
                        options = dropdown,
                        marksFirstByDefault = true,
                    )
                return@forEach
            }
            chip.asChip()?.let(chips::add)
        }

    if (chips.isNotEmpty()) {
        // The visibility chips join the sort entries in the banner's arrow: the sort control may
        // itself arrive as plain chips, and Members only must not keep a second row under the tabs.
        groups +=
            ChannelFilterGroup(
                title = null,
                isDropdown = true,
                options = chips,
                marksFirstByDefault = chips.looksLikeSort(),
            )
    }

    findFirstRenderer("sortFilterSubMenuRenderer").objectOrNull()?.let { menu ->
        val options =
            menu["subMenuItems"]
                .arrayOrNull()
                .orEmpty()
                .mapNotNull { item ->
                    val value = item.objectOrNull() ?: return@mapNotNull null
                    val label = value["title"].youtubeText()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                    ChannelFilterOption(
                        label = label,
                        selected = value["selected"].stringOrNull() == "true",
                        params = value["navigationEndpoint"].objectOrNull()?.browseParams(),
                    )
                }
        if (options.isNotEmpty()) {
            groups +=
                ChannelFilterGroup(
                    title = menu["title"].youtubeText(),
                    isDropdown = true,
                    options = options,
                    marksFirstByDefault = true,
                )
        }
    }

    // The Live tab's sort bar ships Popular, but a livestream is sorted by recency anyway — keep Latest and Oldest only.
    if (kind == ChannelTabKind.Live) {
        return groups.map { group ->
            if (!group.isDropdown) return@map group
            val filtered = group.options.filterNot { it.label.trim().equals("popular", ignoreCase = true) }
            if (filtered.size == group.options.size) group else group.copy(options = filtered)
        }
    }

    return groups
}

private fun JsonObject.asChip(): ChannelFilterOption? {
    val label = this["text"].youtubeText()?.takeIf(String::isNotBlank) ?: return null
    val token = this["tapCommand"].firstContinuationToken() ?: return null
    return ChannelFilterOption(label = label, selected = this["selected"].stringOrNull() == "true", continuation = token)
}

/**
 * The sort control is a chip that opens a sheet, not a chip per option: its entries live under
 * `showSheetCommand`, and a parser that only reads `tapCommand.continuationCommand` sees the chip's
 * own reload token and no options at all.
 */
private fun JsonObject.dropdownOptions(): List<ChannelFilterOption> =
    this["tapCommand"]
        .objectOrNull()
        ?.get("innertubeCommand")
        .objectOrNull()
        ?.get("showSheetCommand")
        .objectOrNull()
        ?.get("panelLoadingStrategy")
        .objectOrNull()
        ?.get("inlineContent")
        .objectOrNull()
        ?.get("sheetViewModel")
        .objectOrNull()
        ?.get("content")
        .objectOrNull()
        ?.get("listViewModel")
        .objectOrNull()
        ?.get("listItems")
        .arrayOrNull()
        .orEmpty()
        .mapNotNull { entry ->
            val item = entry.objectOrNull()?.get("listItemViewModel").objectOrNull() ?: return@mapNotNull null
            val label = item["title"].youtubeText()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            ChannelFilterOption(
                label = label,
                selected = item["isSelected"].stringOrNull() == "true",
                continuation = item["rendererContext"].firstContinuationToken(),
            )
        }

/** The token sits behind a `commandExecutorCommand` wrapper whose depth varies by control. */
private fun JsonElement?.firstContinuationToken(): String? {
    var token: String? = null
    forEachObject { node ->
        if (token != null) return@forEachObject
        node["continuationCommand"]
            .objectOrNull()
            ?.get("token")
            .stringOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let { token = it }
    }
    return token
}
