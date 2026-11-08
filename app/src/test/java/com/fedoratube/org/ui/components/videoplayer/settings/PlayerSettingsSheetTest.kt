package com.fedoratube.org.ui.components.videoplayer.settings

import android.app.Application
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.fedoratube.org.R
import com.fedoratube.org.player.state.EnhancedPlayerState
import com.fedoratube.org.player.state.QualityOption
import com.fedoratube.org.ui.components.videoplayer.subtitle.SubtitleStyle
import com.fedoratube.org.ui.screens.player.FIXTURE_QUALITY_720
import com.fedoratube.org.ui.screens.player.fakePlayerState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Pins what each [SettingsMenuDialog] page renders and which callback each row fires, so the
 * settings sheet can be moved onto the shared sheet primitive without changing its contents.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h891dp")
class PlayerSettingsSheetTest {
    @get:Rule
    val rule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun string(id: Int) = context.getString(id)

    private fun speedLabel(value: String) = context.getString(R.string.playback_speed_multiplier, value)

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private class Callbacks {
        var dismissed = false
        var quality: QualityOption? = null
        var audioTrack: Int? = null
        var speed: Float? = null
        var subtitlesDisabled = false
        var sleepTimer = false
        var locked = false
    }

    private fun setMenu(
        initialPage: PlayerSettingsPage = PlayerSettingsPage.Main,
        playerState: EnhancedPlayerState = fakePlayerState(),
        subtitlesEnabled: Boolean = false,
        enableVerticalDismiss: Boolean = true,
    ): Callbacks {
        val callbacks = Callbacks()
        rule.setContent {
            MaterialTheme {
                SettingsMenuDialog(
                    playerState = playerState,
                    autoplayEnabled = true,
                    subtitlesEnabled = subtitlesEnabled,
                    onDismiss = { callbacks.dismissed = true },
                    initialPage = initialPage,
                    onQualitySelected = { callbacks.quality = it },
                    onAudioTrackSelected = { callbacks.audioTrack = it },
                    onSpeedSelected = { callbacks.speed = it },
                    onDisableSubtitles = { callbacks.subtitlesDisabled = true },
                    onAutoplayToggle = {},
                    onSkipSilenceToggle = {},
                    onStableVolumeToggle = {},
                    subtitleStyle = SubtitleStyle(),
                    onSubtitleStyleChange = {},
                    onLoopToggle = {},
                    onLockScreen = { callbacks.locked = true },
                    onSleepTimerClick = { callbacks.sleepTimer = true },
                    enableVerticalDismiss = enableVerticalDismiss,
                )
            }
        }
        rule.waitForIdle()
        return callbacks
    }

    @Test
    fun mainPageRendersTheYouTubeStyleRows() {
        setMenu()

        rule.onNodeWithText(string(R.string.player_settings)).assertIsDisplayed()
        listOf(
            R.string.quality,
            R.string.playback_speed,
            R.string.captions,
            R.string.audio_track,
            R.string.lock_screen,
            R.string.more_header,
        ).forEach { id -> rule.onNodeWithText(string(id)).assertExists() }
        // Everything else lives behind More now, and casting is out of the app entirely.
        listOf(
            R.string.cast_to_tv,
            R.string.loop_video,
            R.string.pip_mode,
        ).forEach { id -> rule.onNodeWithText(string(id)).assertDoesNotExist() }
    }

    @Test
    fun morePageKeepsTheRestOfTheSettings() {
        setMenu(initialPage = PlayerSettingsPage.More)

        rule.onNodeWithText(string(R.string.more_header)).assertIsDisplayed()
        listOf(
            R.string.loop_video,
            R.string.autoplay_next,
            R.string.pip_mode,
            R.string.sleep_timer,
            R.string.equalizer,
            R.string.player_settings_skip_silence,
            R.string.player_settings_stable_voice,
            R.string.player_settings_ambient_mode,
        ).forEach { id -> rule.onNodeWithText(string(id)).assertExists() }
    }

    @Test
    fun morePageShowsThePlaybackHeaderOnce() {
        setMenu(initialPage = PlayerSettingsPage.More)

        rule.onAllNodesWithText(string(R.string.playback_header)).assertCountEquals(1)
    }

    @Test
    fun qualityPageListsOptionsAndReportsTheTappedOne() {
        val callbacks = setMenu(initialPage = PlayerSettingsPage.Quality)

        rule.onNodeWithText(string(R.string.video_quality_title)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.quality_auto)).assertExists()
        rule.onNodeWithText("1080p").assertExists()

        rule.onNodeWithText("720p").performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(callbacks.quality).isEqualTo(FIXTURE_QUALITY_720)
        assertThat(callbacks.dismissed).isTrue()
    }

    @Test
    fun audioPageListsTracksAndReportsTheTappedIndex() {
        val callbacks = setMenu(initialPage = PlayerSettingsPage.Audio)

        rule.onNodeWithText(string(R.string.audio_track)).assertIsDisplayed()
        rule.onNodeWithText("English").assertExists()

        rule.onNodeWithText("Deutsch").performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(callbacks.audioTrack).isEqualTo(1)
    }

    @Test
    fun speedPageListsPresetsAndReportsTheTappedSpeed() {
        val callbacks = setMenu(initialPage = PlayerSettingsPage.Speed)

        rule.onNodeWithText(string(R.string.playback_speed)).assertIsDisplayed()
        rule.onNodeWithText(string(R.string.normal)).assertExists()

        // The speed rows now render through the shared `playback_speed_multiplier` resource, so the
        // label carries a multiplication sign rather than the literal "x" the page used to build.
        rule.onNodeWithText(speedLabel("1.5")).performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(callbacks.speed).isEqualTo(1.5f)
        assertThat(callbacks.dismissed).isTrue()
    }

    @Test
    fun speedRowsAreRadioButtonsAndTheCurrentSpeedIsSelected() {
        setMenu(initialPage = PlayerSettingsPage.Speed)

        rule
            .onNode(hasText(speedLabel("1.5")) and hasRole(Role.RadioButton))
            .assertIsNotSelected()
        rule
            .onNode(hasText(string(R.string.normal)) and hasRole(Role.RadioButton))
            .assertIsSelected()
    }

    @Test
    fun qualityRowsAreRadioButtons() {
        setMenu(initialPage = PlayerSettingsPage.Quality)

        rule.onNode(hasText("720p") and hasRole(Role.RadioButton)).assertExists()
    }

    @Test
    fun morePageTogglesAreSwitchesThatReportTheirState() {
        setMenu(initialPage = PlayerSettingsPage.More)

        rule
            .onNode(hasText(string(R.string.loop_video)) and hasRole(Role.Switch))
            .performScrollTo()
            .assertIsOff()
    }

    @Test
    fun autoplayToggleIsDisabledWhileLooping() {
        setMenu(initialPage = PlayerSettingsPage.More, playerState = fakePlayerState().copy(isLooping = true))

        rule
            .onNode(hasText(string(R.string.autoplay_next)) and hasRole(Role.Switch))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun subtitlesPageOffRowDisablesSubtitles() {
        val callbacks = setMenu(initialPage = PlayerSettingsPage.Subtitles, subtitlesEnabled = true)

        rule.onNodeWithText(string(R.string.filter_subtitles)).assertIsDisplayed()
        rule.onNodeWithText("English").assertExists()

        rule.onNodeWithText(string(R.string.off)).performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(callbacks.subtitlesDisabled).isTrue()
        assertThat(callbacks.dismissed).isTrue()
    }

    @Test
    fun sleepTimerRowFiresAfterTheSheetDismisses() {
        val callbacks = setMenu(initialPage = PlayerSettingsPage.More)

        rule.onNodeWithText(string(R.string.sleep_timer)).performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(callbacks.sleepTimer).isTrue()
        assertThat(callbacks.dismissed).isTrue()
    }

    @Test
    fun lockScreenRowFiresAfterTheSheetDismisses() {
        val callbacks = setMenu()

        rule.onNodeWithText(string(R.string.lock_screen)).performClick()
        rule.waitForIdle()

        assertThat(callbacks.locked).isTrue()
        assertThat(callbacks.dismissed).isTrue()
    }

    @Test
    fun sleepTimerRowInAPanelHostFiresWithoutDismissing() {
        val callbacks = setMenu(initialPage = PlayerSettingsPage.More, enableVerticalDismiss = false)

        rule.onNodeWithText(string(R.string.sleep_timer)).performScrollTo().performClick()
        rule.waitForIdle()

        assertThat(callbacks.sleepTimer).isTrue()
        assertThat(callbacks.dismissed).isFalse()
    }

    @Test
    fun backFromTheSubtitlesPageReturnsToTheMainPage() {
        setMenu(initialPage = PlayerSettingsPage.Subtitles, subtitlesEnabled = true)

        rule.onNodeWithContentDescription(string(R.string.back)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.player_settings)).assertIsDisplayed()
    }

    @Test
    fun subtitleStyleFromTheSubtitlesPageReturnsToTheSubtitlesPage() {
        setMenu(initialPage = PlayerSettingsPage.Subtitles, subtitlesEnabled = true)

        rule.onNodeWithText(string(R.string.subtitle_style)).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.subtitle_customization_title)).assertExists()

        rule.onNodeWithContentDescription(string(R.string.back)).performClick()
        rule.waitForIdle()

        rule.onNodeWithText(string(R.string.filter_subtitles)).assertIsDisplayed()
    }

    @Test
    fun closeButtonDismissesAfterTheExitAnimation() {
        val callbacks = setMenu()

        rule.onNodeWithContentDescription(string(R.string.close)).performClick()
        rule.waitForIdle()

        assertThat(callbacks.dismissed).isTrue()
    }
}
