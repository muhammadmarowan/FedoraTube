package com.fedoratube.org.ui.components.layout.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.fedoratube.org.R
import com.fedoratube.org.data.local.DEFAULT_NAV_TAB_ORDER

/**
 * A root destination of the app's primary navigation.
 *
 * [id] is what `navTabOrder` and `defaultNavTabIndex` persist, so it must never be renumbered.
 */
enum class FlowTab(
    val id: Int,
    val route: String,
    @StringRes val labelRes: Int,
) {
    Home(0, "home", R.string.nav_home),
    Music(2, "music", R.string.nav_music),
    Subscriptions(3, "subscriptions", R.string.nav_subs),
    Library(4, "library", R.string.nav_library),
    Search(5, "search", R.string.nav_search),
    Explore(6, "categories", R.string.nav_explore),
    Downloads(7, "downloads", R.string.nav_downloads),
    ;

    companion object {
        fun fromId(id: Int): FlowTab? = entries.firstOrNull { it.id == id }
    }
}

typealias FedoraTab = FlowTab

@Composable
fun FlowTab.icon(selected: Boolean): ImageVector =
    when (this) {
        FlowTab.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        FlowTab.Music -> if (selected) Icons.Filled.MusicNote else Icons.Outlined.MusicNote
        FlowTab.Subscriptions -> if (selected) Icons.Filled.Subscriptions else Icons.Outlined.Subscriptions
        FlowTab.Library -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
        FlowTab.Search -> if (selected) Icons.Filled.Search else Icons.Outlined.Search
        FlowTab.Explore -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
        FlowTab.Downloads -> if (selected) Icons.Filled.Download else Icons.Outlined.Download
    }

data class NavigationVisibility(
    val home: Boolean = true,
    val music: Boolean = true,
    val search: Boolean = false,
    val categories: Boolean = false,
)

/**
 * The bottom-bar tabs, in the user's order. Library cannot be hidden: every root
 * destination carries Notifications and Settings in its top bar, so one must always exist.
 * Downloads lives in the Library section, YouTube-style. Music lives in the bottom navigation bar.
 */
fun visibleFlowTabs(
    order: List<Int>,
    visibility: NavigationVisibility,
): List<FlowTab> {
    val enabled =
        buildSet {
            if (visibility.home) add(FlowTab.Home)
            if (visibility.music) add(FlowTab.Music)
            add(FlowTab.Library)
            if (visibility.search) add(FlowTab.Search)
            if (visibility.categories) add(FlowTab.Explore)
        }
    return (order + DEFAULT_NAV_TAB_ORDER)
        .distinct()
        .mapNotNull(FlowTab::fromId)
        .filter { enabled.contains(it) }
}

/**
 * The top chips under the app wordmark, YouTube-style: Home and Subscriptions.
 * Music now lives in the bottom navigation bar.
 */
fun topCategoryTabs(visibility: NavigationVisibility): List<FlowTab> =
    buildList {
        if (visibility.home) add(FlowTab.Home)
        add(FlowTab.Subscriptions)
    }

fun resolveDefaultFlowTab(
    preferredId: Int,
    order: List<Int>,
    visibility: NavigationVisibility,
): FlowTab {
    val visible = visibleFlowTabs(order, visibility)
    return visible.firstOrNull { it.id == preferredId } ?: visible.first()
}
