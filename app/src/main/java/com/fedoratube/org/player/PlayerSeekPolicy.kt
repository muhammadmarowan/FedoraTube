package com.fedoratube.org.player

import androidx.media3.common.C

internal fun isEndBoundarySeek(requestedPositionMs: Long, durationMs: Long): Boolean =
    durationMs > 0L &&
        durationMs != C.TIME_UNSET &&
        requestedPositionMs >= durationMs
