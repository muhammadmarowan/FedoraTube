@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fedoratube.org.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.shared.FedoraThumbnailsWallpaper

private val TextSpacing = 10.dp

@Composable
internal fun WelcomeStep(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        FedoraThumbnailsWallpaper(
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay: keeps thumbnails visible at top/middle and smoothly blends to background at bottom
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.20f),
                            0.40f to MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                            0.65f to MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
                            0.85f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            1.0f to MaterialTheme.colorScheme.background,
                        ),
                    ),
        )

        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TextSpacing)) {
                Text(
                    text = stringResource(R.string.onboarding_welcome_title),
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.onboarding_welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
