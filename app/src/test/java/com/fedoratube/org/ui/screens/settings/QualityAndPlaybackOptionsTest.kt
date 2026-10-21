package com.fedoratube.org.ui.screens.settings

import com.fedoratube.org.data.local.VideoCodec
import com.fedoratube.org.ui.screens.settings.playback.parseSpeedInput
import com.fedoratube.org.ui.screens.settings.playback.parseSpeedPresets
import com.fedoratube.org.ui.screens.settings.playback.serializeSpeedPresets
import com.fedoratube.org.ui.screens.settings.quality.fallbackAfterPreferredChange
import com.fedoratube.org.ui.screens.settings.quality.fallbackCodecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class QualityAndPlaybackOptionsTest {
    @Test
    fun `fallback codec never repeats the preferred one`() {
        assertFalse(VideoCodec.AV1 in fallbackCodecs(VideoCodec.AV1))
        assertEquals(VideoCodec.AUTO, fallbackAfterPreferredChange(VideoCodec.VP9, VideoCodec.VP9))
        assertEquals(VideoCodec.H264, fallbackAfterPreferredChange(VideoCodec.VP9, VideoCodec.H264))
    }

    @Test
    fun `speed presets round trip sorted and deduplicated`() {
        val presets = parseSpeedPresets("1.5, 0.75,abc,1.5,12,0.05")
        assertEquals(listOf(0.75f, 1.5f), presets)
        assertEquals("0.75,1.5", serializeSpeedPresets(presets))
    }

    @Test
    fun `typed speeds accept a comma and reject out of range`() {
        assertEquals(1.35f, parseSpeedInput(" 1,35 "))
        assertNull(parseSpeedInput("11"))
        assertNull(parseSpeedInput("fast"))
    }
}
