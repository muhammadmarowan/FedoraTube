/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Full-surface loading state with sleek YouTube-style circular indicator.
 */
@Composable
fun FlowLoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun FedoraLoadingIndicator(modifier: Modifier = Modifier) = FlowLoadingIndicator(modifier)

/**
 * Inline loading row for a feed continuation or a paged list tail.
 */
@Composable
fun FlowFeedProgress(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(26.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun FedoraFeedProgress(modifier: Modifier = Modifier) = FlowFeedProgress(modifier)
