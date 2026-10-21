package com.fedoratube.org.ui.screens.settings.taste

import com.google.common.truth.Truth.assertThat
import com.fedoratube.org.data.recommendation.ContentVector
import com.fedoratube.org.data.recommendation.NeuroDiscovery
import com.fedoratube.org.data.recommendation.UserBrain
import org.junit.Test

class EngineDetailsTest {
    @Test
    fun `a new or reset brain shows the starter searches`() {
        assertThat(EngineDetails.of(UserBrain()).starters).containsExactlyElementsIn(NeuroDiscovery.STARTER_QUERIES)
    }

    @Test
    fun `learned topics or picks hide the starter searches`() {
        assertThat(EngineDetails.of(UserBrain(globalVector = ContentVector(topics = mapOf("comedy" to 0.3)))).starters).isEmpty()
        assertThat(EngineDetails.of(UserBrain(preferredTopics = setOf("Cooking"))).starters).isEmpty()
    }
}
