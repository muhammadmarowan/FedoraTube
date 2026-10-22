@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.fedoratube.org.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R

private val CardWidth = 148.dp
private val CardHeight = 90.dp
private val CardSpacing = 8.dp
private val RowSpacing = 8.dp
private val TextSpacing = 10.dp
private val BrandSpacing = 8.dp

private val Row0Thumbs =
    listOf(
        R.drawable.onboarding_thumb_0,
        R.drawable.onboarding_thumb_1,
        R.drawable.onboarding_thumb_2,
        R.drawable.onboarding_thumb_3,
        R.drawable.onboarding_thumb_4,
    )
private val Row1Thumbs =
    listOf(
        R.drawable.onboarding_thumb_5,
        R.drawable.onboarding_thumb_6,
        R.drawable.onboarding_thumb_7,
        R.drawable.onboarding_thumb_8,
        R.drawable.onboarding_thumb_9,
    )
private val Row2Thumbs =
    listOf(
        R.drawable.onboarding_thumb_10,
        R.drawable.onboarding_thumb_11,
        R.drawable.onboarding_thumb_12,
        R.drawable.onboarding_thumb_13,
        R.drawable.onboarding_thumb_14,
    )
private val Row3Thumbs =
    listOf(
        R.drawable.onboarding_thumb_15,
        R.drawable.onboarding_thumb_2,
        R.drawable.onboarding_thumb_4,
        R.drawable.onboarding_thumb_6,
        R.drawable.onboarding_thumb_8,
    )
private val Row4Thumbs =
    listOf(
        R.drawable.onboarding_thumb_1,
        R.drawable.onboarding_thumb_3,
        R.drawable.onboarding_thumb_5,
        R.drawable.onboarding_thumb_7,
        R.drawable.onboarding_thumb_9,
    )
private val Row5Thumbs =
    listOf(
        R.drawable.onboarding_thumb_11,
        R.drawable.onboarding_thumb_13,
        R.drawable.onboarding_thumb_15,
        R.drawable.onboarding_thumb_0,
        R.drawable.onboarding_thumb_12,
    )

private val AllRows =
    listOf(
        Row0Thumbs,
        Row1Thumbs,
        Row2Thumbs,
        Row3Thumbs,
        Row4Thumbs,
        Row5Thumbs,
        Row0Thumbs,
        Row1Thumbs,
        Row2Thumbs,
    )

@Composable
internal fun WelcomeStep(
    hero: HeroSlot,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        OnboardingThumbnailsWallpaper(
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay to keep text and bottom controls readable
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.40f),
                            0.30f to MaterialTheme.colorScheme.background.copy(alpha = 0.50f),
                            0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                            0.80f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            1.0f to MaterialTheme.colorScheme.background,
                        ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(TextSpacing),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(BrandSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_fedora_tube_wordmark),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.height(34.dp),
                        contentScale = ContentScale.FillHeight,
                    )
                }
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

@Composable
private fun OnboardingThumbnailsWallpaper(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wallpaperSlide")
    val slideRightProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 32000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "slideRight",
    )
    val slideLeftProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "slideLeft",
    )

    val density = LocalDensity.current
    val cycleWidthDp = (CardWidth + CardSpacing) * 5
    val cycleWidthPx = with(density) { cycleWidthDp.toPx() }

    Box(
        modifier = modifier.clipToBounds(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
        ) {
            AllRows.forEachIndexed { rowIndex, thumbs ->
                val slideRight = (rowIndex % 2 == 0)
                val progress = if (slideRight) slideRightProgress else slideLeftProgress
                val xOffset =
                    if (slideRight) {
                        -cycleWidthPx + (progress * cycleWidthPx)
                    } else {
                        -(progress * cycleWidthPx)
                    }

                // Repeat items 4 times so there's always plenty of cards visible on either side
                val repeatedThumbs = thumbs + thumbs + thumbs + thumbs

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = xOffset
                            },
                    horizontalArrangement = Arrangement.spacedBy(CardSpacing),
                ) {
                    repeatedThumbs.forEach { resId ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 2.dp,
                            shadowElevation = 3.dp,
                            modifier = Modifier.size(width = CardWidth, height = CardHeight),
                        ) {
                            Image(
                                painter = painterResource(resId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}
