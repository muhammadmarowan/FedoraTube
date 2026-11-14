package com.fedoratube.org.ui.components.layout.navigation

import com.fedoratube.org.data.local.DEFAULT_NAV_TAB_ORDER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FedoraTabTest {
    @Test
    fun hiddenHomeFallsBackToFirstVisibleDestination() {
        val visibility = NavigationVisibility(home = false, music = false)

        val resolved =
            resolveDefaultFlowTab(
                preferredId = FlowTab.Home.id,
                order = listOf(0, 4, 2, 5, 6),
                visibility = visibility,
            )

        assertEquals(FlowTab.Library, resolved)
        assertFalse(visibleFlowTabs(listOf(0, 4), visibility).contains(FlowTab.Home))
    }

    @Test
    fun reEnabledHomeRestoresAHomeDefault() {
        val resolved =
            resolveDefaultFlowTab(
                preferredId = FlowTab.Home.id,
                order = listOf(2, 0, 4),
                visibility = NavigationVisibility(home = true),
            )

        assertEquals(FlowTab.Home, resolved)
    }

    @Test
    fun tabsFollowTheSavedOrder() {
        val tabs =
            visibleFlowTabs(
                order = listOf(4, 2, 0),
                visibility = NavigationVisibility(),
            )

        assertEquals(
            listOf(FlowTab.Library, FlowTab.Music, FlowTab.Home),
            tabs,
        )
    }

    @Test
    fun aSavedOrderMissingNewerTabsAppendsThemInDefaultOrder() {
        val tabs =
            visibleFlowTabs(
                order = listOf(0),
                visibility = NavigationVisibility(search = true, categories = true),
            )

        assertEquals(
            listOf(
                FlowTab.Home,
                FlowTab.Music,
                FlowTab.Library,
                FlowTab.Search,
                FlowTab.Explore,
            ),
            tabs,
        )
    }

    @Test
    fun unknownPersistedIdsAreIgnored() {
        val tabs = visibleFlowTabs(order = listOf(42, 4, -1), visibility = NavigationVisibility())

        assertEquals(FlowTab.Library, tabs.first())
        assertEquals(tabs.distinct(), tabs)
    }

    @Test
    fun persistedIdsNeverChange() {
        assertEquals(DEFAULT_NAV_TAB_ORDER, FlowTab.entries.map(FlowTab::id))
        FlowTab.entries.forEach { tab -> assertEquals(tab, FlowTab.fromId(tab.id)) }
    }

    @Test
    fun everyVisibilityCombinationKeepsAnUnhideableRootDestination() {
        val orders =
            listOf(
                DEFAULT_NAV_TAB_ORDER,
                listOf(6, 5, 4, 2, 0),
                listOf(4, 2),
                emptyList(),
            )

        for (bits in 0 until 16) {
            val visibility =
                NavigationVisibility(
                    home = bits and 1 != 0,
                    music = bits and 2 != 0,
                    search = bits and 4 != 0,
                    categories = bits and 8 != 0,
                )

            for (order in orders) {
                val visible = visibleFlowTabs(order, visibility)
                assertTrue(
                    "no unhideable root destination for $visibility / $order",
                    visible.contains(FlowTab.Library),
                )

                val resolved = resolveDefaultFlowTab(FlowTab.Home.id, order, visibility)
                assertTrue(
                    "resolved default $resolved is not visible for $visibility / $order",
                    visible.contains(resolved),
                )
            }
        }
    }
}
