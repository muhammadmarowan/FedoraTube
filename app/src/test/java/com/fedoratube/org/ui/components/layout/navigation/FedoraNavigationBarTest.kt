package com.fedoratube.org.ui.components.layout.navigation

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.fedoratube.org.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class FedoraNavigationBarTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val selections = mutableListOf<FlowTab>()

    private fun label(tab: FlowTab) = context.getString(tab.labelRes)

    private fun setBar(
        tabs: List<FlowTab>,
        selected: FlowTab,
    ) {
        rule.setContent {
            MaterialTheme {
                FlowNavigationBar(tabs = tabs, selectedTab = selected, onTabSelected = { selections += it })
            }
        }
    }

    @Test
    fun `the selected tab is announced as selected and the others are not`() {
        setBar(FlowTab.entries.take(5), selected = FlowTab.Music)

        rule.onNodeWithContentDescription(label(FlowTab.Music), useUnmergedTree = true).assertIsSelected()
        rule.onNodeWithContentDescription(label(FlowTab.Home), useUnmergedTree = true).assertIsNotSelected()
    }

    @Test
    fun `five tabs fit the bar without an overflow`() {
        setBar(FlowTab.entries.take(5), selected = FlowTab.Home)

        rule.onAllNodesWithContentDescription(context.getString(R.string.nav_more), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `tabs past the fourth move behind More and still navigate`() {
        setBar(FlowTab.entries, selected = FlowTab.Home)

        rule.onAllNodesWithContentDescription(label(FlowTab.Library), useUnmergedTree = true).assertCountEquals(0)
        rule.onNodeWithContentDescription(context.getString(R.string.nav_more), useUnmergedTree = true).performClick()
        // Overflow menu items still use visible text labels.
        rule.onAllNodesWithText(label(FlowTab.Explore)).fetchSemanticsNodes().getOrNull(0)?.let {
            rule.onNodeWithText(label(FlowTab.Explore)).performClick()
        }
        assertThat(selections).containsExactly(FlowTab.Explore)
    }

    @Test
    fun `More reads as selected while an overflow tab is current`() {
        setBar(FlowTab.entries, selected = FlowTab.Search)

        rule.onNodeWithContentDescription(context.getString(R.string.nav_more), useUnmergedTree = true).assertIsSelected()
    }
}
