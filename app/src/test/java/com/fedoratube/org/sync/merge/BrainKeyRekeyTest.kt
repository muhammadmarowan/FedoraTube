package com.fedoratube.org.sync.merge

import com.google.common.truth.Truth.assertThat
import com.fedoratube.org.data.recommendation.NeuroKeyMigration
import com.fedoratube.org.data.recommendation.NeuroTokenizer
import com.fedoratube.org.sync.canonical.BrainLwwMaps
import com.fedoratube.org.sync.canonical.CanonicalBrain
import com.fedoratube.org.sync.canonical.CanonicalBrainVectors
import com.fedoratube.org.sync.canonical.CanonicalTopicEvidence
import com.fedoratube.org.sync.canonical.CanonicalVector
import com.fedoratube.org.sync.canonical.GCounter
import com.fedoratube.org.sync.canonical.Lww
import com.fedoratube.org.sync.identity.Hlc
import org.junit.Test

/** A peer's or the sidecar's spellings read by key spec 2 before they merge (#907). */
class BrainKeyRekeyTest {
    private val keys = NeuroKeyMigration.keyFunction(NeuroTokenizer())

    private fun hlc(time: Long) = Hlc(time, 0, "node").encode()

    private val older =
        CanonicalBrain(
            deviceId = "b",
            vectors =
                CanonicalBrainVectors(
                    globalVector = CanonicalVector(topics = mapOf("guitars" to 0.2, "guitar" to 0.4, "kayaking" to 0.3)),
                    topicAffinities = mapOf("guitars|kayaking" to 0.5, "guitar|guitars" to 0.9),
                ),
            idfWordFrequency = mapOf("guitars" to GCounter(mapOf("a" to 1, "b" to 4)), "guitar" to GCounter(mapOf("b" to 6))),
            lwwMaps =
                BrainLwwMaps(
                    topicEvidence =
                        mapOf(
                            "guitars" to Lww(CanonicalTopicEvidence(positiveSignals = 2), hlc(2_000)),
                            "guitar" to Lww(CanonicalTopicEvidence(positiveSignals = 7), hlc(1_000)),
                        ),
                ),
        )

    @Test
    fun `spellings of one key merge as their field merges`() {
        val read = BrainKeyRekey.canonical(older, keys)

        assertThat(read.vectors.globalVector.topics).containsExactly("guitar", 0.4, "kayak", 0.3)
        assertThat(read.vectors.topicAffinities).containsExactly("guitar|kayak", 0.5)
        assertThat(read.idfWordFrequency).containsExactly("guitar", GCounter(mapOf("a" to 1L, "b" to 10L)))
        assertThat(
            read.lwwMaps.topicEvidence
                .getValue("guitar")
                .value.positiveSignals,
        ).isEqualTo(2)
    }

    @Test
    fun `keys already read stay as they are`() {
        val once = BrainKeyRekey.canonical(older, keys)

        assertThat(BrainKeyRekey.canonical(once, keys)).isEqualTo(once)
    }

    @Test
    fun `the sidecar read the same way credits no growth to this device`() {
        val state =
            BrainCrdtState(
                idfWords = mapOf("guitars" to mapOf("a" to 4L), "guitar" to mapOf("a" to 2L, "b" to 4L)),
                lastIdfWordScalars = mapOf("guitars" to 4L, "guitar" to 6L),
            )
        // The brain's own counts after migration 20: 4 + 6 under one key.
        val read = BrainKeyRekey.state(state, keys)

        val attributed = BrainCrdtState.attributeLocal(read, "a", 0, 0, mapOf("guitar" to 10L))

        assertThat(attributed.idfWords.getValue("guitar")).containsExactly("a", 6L, "b", 4L)
    }
}
