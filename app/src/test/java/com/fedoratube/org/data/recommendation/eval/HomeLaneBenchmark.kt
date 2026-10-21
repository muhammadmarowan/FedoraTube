/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 * Test-source-set only — never shipped in the APK.
 */

package com.fedoratube.org.data.recommendation.eval

import com.fedoratube.org.data.model.Video
import com.fedoratube.org.data.recommendation.FeedExclusions
import com.fedoratube.org.data.recommendation.FlowNeuroEngine
import com.fedoratube.org.data.recommendation.GraphSeedInput
import com.fedoratube.org.data.recommendation.GraphSeedSelector
import com.fedoratube.org.data.recommendation.GraphSeedSource
import com.fedoratube.org.data.recommendation.NeuroScoring
import com.fedoratube.org.data.recommendation.UserBrain
import com.fedoratube.org.data.recommendation.eval.NeuroLearningBenchmark.Family
import com.fedoratube.org.ui.screens.home.DURATION_COMFORT_DEFAULT_SEC
import com.fedoratube.org.ui.screens.home.FeedTasteProfile
import com.fedoratube.org.ui.screens.home.GraphCandidate
import com.fedoratube.org.ui.screens.home.assembleHomeFeed
import com.fedoratube.org.ui.screens.home.buildHomeFeedLanes
import kotlinx.coroutines.runBlocking

/**
 * Home as the viewer scrolls it: the real lanes, quotas and blend around the real ranker. The
 * viewer has watched comedy for months (long specials, about half of each) and then two guitar
 * playalongs in full. What YouTube returns is modelled: a search gives mostly its subject plus a
 * quarter of unrelated videos, a related list mostly its seed's subject plus 15 % unrelated. The
 * searches are drawn at random, so the shares are averaged over [REFRESHES] refreshes.
 */
internal object HomeLaneBenchmark {
    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val RESULTS_PER_SEARCH = 12
    private const val UNRELATED_PER_SEARCH = 3
    private const val RESULTS_PER_SEED = 20
    private const val UNRELATED_PER_SEED = 3
    private const val SEEDS = 4
    private const val REFRESHES = 8

    data class Result(
        val unrelatedIn5to20: Double,
        val guitarInTop20: Double,
        val comedyInTop20: Double,
        val guitarSeeds: Int,
        val searches: List<String>,
    )

    private class Supply(
        families: Map<Family, List<Video>>,
    ) {
        private val remaining = families.mapValues { (_, videos) -> ArrayDeque(videos) }

        fun take(
            family: Family,
            count: Int,
        ): List<Video> = List(count) { remaining.getValue(family).removeFirstOrNull() }.filterNotNull()
    }

    // Search words of each family as the learned keys spell them, song and comedian names included.
    private val guitarWords =
        listOf("guitar", "playalong", "tabs", "child", "mine", "wonderwall", "california", "spirit", "sandman", "nation", "highway")
    private val comedyWords =
        listOf(
            "comedy",
            "stand",
            "special",
            "comedian",
            "funny",
            "mulaney",
            "gaffigan",
            "wong",
            "burr",
            "birbiglia",
            "tomlinson",
            "airport",
            "dentist",
            "wedding",
            "kid",
            "parent",
            "hotel",
            "road",
            "gym",
        )

    private fun familyOfQuery(query: String): Family {
        val words = query.lowercase().split(' ')
        return when {
            words.any { word -> guitarWords.any { word.startsWith(it) } } -> Family.GUITAR
            words.any { word -> comedyWords.any { word.startsWith(it) } } -> Family.COMEDY
            else -> Family.OTHER
        }
    }

    // YouTube never runs out of a subject, so past the pool each family continues with more of its own videos.
    private fun supply(): Supply {
        val byFamily = NeuroLearningBenchmark.pool().groupBy { NeuroLearningBenchmark.familyOf(it.id) }
        return Supply(
            byFamily +
                mapOf(
                    Family.COMEDY to byFamily.getValue(Family.COMEDY) + (100 until 300).map { NeuroLearningBenchmark.comedy(it) },
                    Family.GUITAR to byFamily.getValue(Family.GUITAR) + (100 until 300).map { NeuroLearningBenchmark.guitar(it) },
                    Family.OTHER to byFamily.getValue(Family.OTHER) + (200 until 300).map { NeuroLearningBenchmark.other(it) },
                ),
        )
    }

    private fun history(now: Long): List<GraphSeedInput> {
        val comedy =
            (0 until 38).map { i ->
                val video = NeuroLearningBenchmark.comedy(i)
                GraphSeedInput(
                    id = video.id,
                    title = video.title,
                    channelId = video.channelId,
                    source = GraphSeedSource.WATCH_HISTORY,
                    engagementWeight = 0.45,
                    timestamp = now - (1 + i % 7) * DAY_MS,
                    durationSec = 3600,
                    percentWatched = 45.0,
                )
            }
        val guitar =
            (0 until 2).map { i ->
                val video = NeuroLearningBenchmark.guitar(i)
                GraphSeedInput(
                    id = video.id,
                    title = video.title,
                    channelId = video.channelId,
                    source = GraphSeedSource.WATCH_HISTORY,
                    engagementWeight = 1.0,
                    timestamp = now - DAY_MS,
                    durationSec = 240,
                    percentWatched = 100.0,
                )
            }
        return (guitar + comedy).sortedByDescending { it.timestamp }
    }

    private suspend fun FlowNeuroEngine.watchHistory() =
        with(NeuroLearningBenchmark) {
            for (i in 0 until 38) watch(comedy(i), 0.45f)
            watch(guitar(0), 1f)
            watch(guitar(1), 1f)
        }

    fun comedyThenGuitar(now: Long = System.currentTimeMillis()): Result =
        runBlocking {
            val learner = NeuroLearningBenchmark.engine()
            learner.watchHistory()
            learner.resetSession()
            val brain = learner.getBrainSnapshot()
            learner.shutdown()
            val refreshes = List(REFRESHES) { refresh(brain, now) }
            Result(
                unrelatedIn5to20 = refreshes.map { it.unrelatedIn5to20 }.average(),
                guitarInTop20 = refreshes.map { it.guitarInTop20 }.average(),
                comedyInTop20 = refreshes.map { it.comedyInTop20 }.average(),
                guitarSeeds = refreshes.first().guitarSeeds,
                searches = refreshes.first().searches,
            )
        }

    private suspend fun refresh(
        brain: UserBrain,
        now: Long,
    ): Result =
        run {
            val engine = NeuroLearningBenchmark.engine(brain, NeuroLearningBenchmark.NO_JITTER)
            val supply = supply()

            val searches = engine.generateDiscoveryQueries(resetDepth = true)
            val discovery =
                searches.flatMap { query ->
                    supply.take(familyOfQuery(query), RESULTS_PER_SEARCH - UNRELATED_PER_SEARCH) +
                        supply.take(Family.OTHER, UNRELATED_PER_SEARCH)
                }

            val seedInputs = history(now)
            val mass = brain.globalVector.topics
            val seeds =
                GraphSeedSelector.selectWithLongTerm(
                    seedInputs,
                    SEEDS,
                    emptyList(),
                    mass,
                    { key -> if ("guitar" in key || "playalong" in key) "guitar" else "comedy" },
                    now,
                    topicScores = mass,
                )
            val related =
                seeds.flatMap { seedId ->
                    val family = NeuroLearningBenchmark.familyOf(seedId)
                    (supply.take(family, RESULTS_PER_SEED - UNRELATED_PER_SEED) + supply.take(Family.OTHER, UNRELATED_PER_SEED))
                        .mapIndexed { rank, video ->
                            GraphCandidate(
                                video = video,
                                seedId = seedId,
                                seedScore = 1.0,
                                graphRank = rank,
                                seedCluster = family.name,
                                seedResultCount = RESULTS_PER_SEED,
                            )
                        }
                }

            val lanes =
                buildHomeFeedLanes(
                    rawSubs = emptyList(),
                    rawDiscovery = discovery,
                    rawMemory = emptyList(),
                    rawRelated = related,
                    rssFeed = emptyList(),
                    watched = emptySet(),
                    exclusions = FeedExclusions.NONE,
                    isRecentlyShown = { false },
                    taste = FeedTasteProfile(DURATION_COMFORT_DEFAULT_SEC, emptySet()),
                    now = now,
                    freshSlotTarget = 3,
                    subAvatarMap = emptyMap(),
                    rank = { engine.rank(it, emptySet()) },
                    rankRelevant = { engine.rank(it, emptySet(), NeuroScoring.LANE_RELEVANCE_FLOOR) },
                    share = engine::shareByInterest,
                )
            val feed = assembleHomeFeed(lanes, emptySet(), subCount = 0, totalInteractions = brain.totalInteractions).videos
            engine.shutdown()

            fun share(
                videos: List<Video>,
                family: Family,
            ) = if (videos.isEmpty()) 0.0 else videos.count { NeuroLearningBenchmark.familyOf(it.id) == family }.toDouble() / videos.size
            val top20 = feed.take(20)
            Result(
                unrelatedIn5to20 = share(feed.drop(4).take(16), Family.OTHER),
                guitarInTop20 = share(top20, Family.GUITAR),
                comedyInTop20 = share(top20, Family.COMEDY),
                guitarSeeds = seeds.count { NeuroLearningBenchmark.familyOf(it) == Family.GUITAR },
                searches = searches,
            )
        }
}
