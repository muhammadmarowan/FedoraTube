package com.fedoratube.org.ui.components.videoplayer.overlay

import com.fedoratube.org.data.local.SponsorBlockAction
import com.fedoratube.org.data.model.SponsorBlockCategories
import com.fedoratube.org.data.model.SponsorBlockSegment

internal fun findActiveManualSponsorSegment(
    sponsorSegments: List<SponsorBlockSegment>,
    currentPositionMs: Long,
    skippedUuids: Set<String>,
    categoryActions: Map<String, SponsorBlockAction>,
    playbackEnded: Boolean,
): SponsorBlockSegment? {
    if (playbackEnded) return null

    val positionSeconds = currentPositionMs / 1000f
    return sponsorSegments.find { segment ->
        positionSeconds >= segment.startTime &&
            positionSeconds < segment.endTime &&
            segment.uuid !in skippedUuids &&
            (categoryActions[segment.category] ?: SponsorBlockCategories.defaultAction(segment.category)) !=
            SponsorBlockAction.SKIP
    }
}
