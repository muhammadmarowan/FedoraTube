/*
 * Copyright (C) 2025-2026 Flow | A-EDev
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.screens.home

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeSubscriptionScopeTest {
    private val subs = setOf("UCa", "UCb")

    @Test
    fun `with subscriptions on Home they are boosted and nothing is hidden`() {
        val scope = HomeSubscriptionScope.of(subs, showOnHome = true)

        assertThat(scope.boosted).isEqualTo(subs)
        assertThat(scope.hidden).isEmpty()
    }

    @Test
    fun `with subscriptions off Home hides them and boosts nothing`() {
        val scope = HomeSubscriptionScope.of(subs, showOnHome = false)

        assertThat(scope.boosted).isEmpty()
        assertThat(scope.hidden).isEqualTo(subs)
    }
}
