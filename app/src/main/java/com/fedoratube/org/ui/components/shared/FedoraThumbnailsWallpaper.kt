package com.fedoratube.org.ui.components.shared

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R

private val CardWidth = 144.dp
private val CardHeight = 84.dp
private val CardSpacing = 8.dp
private val RowSpacing = 8.dp

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
        Row3Thumbs,
    )

@Composable
fun FedoraThumbnailsWallpaper(modifier: Modifier = Modifier) {
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

                // 5 repetitions = 25 cards, ensuring the row extends far beyond any screen width
                val repeatedThumbs = thumbs + thumbs + thumbs + thumbs + thumbs

                Row(
                    modifier =
                        Modifier
                            .wrapContentWidth(align = Alignment.Start, unbounded = true)
                            .graphicsLayer {
                                translationX = xOffset
                            },
                    horizontalArrangement = Arrangement.spacedBy(CardSpacing),
                ) {
                    repeatedThumbs.forEach { resId ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 1.dp,
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

