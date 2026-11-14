/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.data.recommendation

import java.util.concurrent.ConcurrentHashMap

/** The step of [NeuroScoring.applySmartDiversity] that placed a video. */
enum class DiversityPhase { STRICT, EXPLORATION, DEFERRED, RELAXED }

/**
 * Why each candidate of the rank passes of one Home load made it into the result, so a Home
 * snapshot can tell a video the viewer's taste chose from one that only filled a short lane. Lanes
 * rank concurrently while the snapshot reads, hence the concurrent collections.
 */
class RankTrace {
    val unrelated: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val phases: MutableMap<String, DiversityPhase> = ConcurrentHashMap()

    /** Admitted although it shares nothing but common words with the viewer, by a rank pass without the lane floor. */
    fun isPadding(videoId: String): Boolean = videoId in unrelated
}
