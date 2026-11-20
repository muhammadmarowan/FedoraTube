package com.fedoratube.org.ui.screens.home.chips

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fedoratube.org.R
import kotlin.math.roundToInt

private val TabRowHeight = 38.dp
private val IndicatorHeight = 2.5.dp
private val IndicatorWidth = 28.dp

/**
 * YouTube-style top tabs under the app wordmark: All and Subs.
 * Plain text tabs with an animated underline indicator on the selected tab
 * (YouTube-style line, themed here) — no pill/box backgrounds.
 */
@Composable
internal fun HomeChipRow(
    state: HomeChipsState,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onReselect: ((String) -> Unit)? = null,
    pagerState: PagerState? = null,
) {
    if (state.chips.isEmpty()) return

    val density = LocalDensity.current
    val indicatorWidthPx = with(density) { IndicatorWidth.toPx() }

    val tabCenters = remember { mutableStateMapOf<String, Float>() }
    var rowRootX by remember { mutableFloatStateOf(0f) }

    val selectedKey = state.selected
    val targetCenterX = tabCenters[selectedKey]

    val animatedCenterX = remember { Animatable(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(targetCenterX) {
        if (pagerState == null && targetCenterX != null) {
            if (!isInitialized) {
                animatedCenterX.snapTo(targetCenterX)
                isInitialized = true
            } else {
                animatedCenterX.animateTo(
                    targetValue = targetCenterX,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = 380f,
                        ),
                )
            }
        }
    }

    val currentCenterPx =
        if (pagerState != null && state.chips.size >= 2) {
            val page = pagerState.currentPage
            val offset = pagerState.currentPageOffsetFraction
            val fromKey = state.chips.getOrNull(page)?.key ?: ""
            val toPage = if (offset > 0f) page + 1 else if (offset < 0f) page - 1 else page
            val toKey = state.chips.getOrNull(toPage)?.key ?: fromKey
            val cFrom = tabCenters[fromKey] ?: 0f
            val cTo = tabCenters[toKey] ?: cFrom
            if (cFrom > 0f && cTo > 0f) {
                cFrom + (cTo - cFrom) * kotlin.math.abs(offset)
            } else {
                tabCenters[selectedKey] ?: 0f
            }
        } else {
            animatedCenterX.value
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TabRowHeight)
                .background(MaterialTheme.colorScheme.surface)
                .onGloballyPositioned { coordinates ->
                    rowRootX = coordinates.positionInRoot().x
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.chips.forEach { chip ->
                val label = chip.label()
                val isSelected = chip.key == state.selected
                val textColor by
                    animateColorAsState(
                        targetValue =
                            if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                            },
                        animationSpec = tween(180),
                        label = "tabTextColor",
                    )
                val description = stringResource(R.string.home_chip_description, label)

                Column(
                    modifier =
                        Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (isSelected) {
                                        onReselect?.invoke(chip.key)
                                    } else {
                                        onSelect(chip.key)
                                    }
                                },
                            )
                            .semantics {
                                role = Role.Tab
                                contentDescription = description
                            }
                            .padding(vertical = 4.dp)
                            .onGloballyPositioned { coordinates ->
                                val centerXInRoot = coordinates.positionInRoot().x + coordinates.size.width / 2f
                                val localCenterX = centerXInRoot - rowRootX
                                tabCenters[chip.key] = localCenterX
                            },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        color = textColor,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        // Sliding indicator line
        val showIndicator = if (pagerState != null) currentCenterPx > 0f else (isInitialized && currentCenterPx > 0f)
        if (showIndicator) {
            val leftOffsetPx = (currentCenterPx - indicatorWidthPx / 2f).coerceAtLeast(0f)
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset { IntOffset(leftOffsetPx.roundToInt(), 0) }
                        .padding(bottom = 2.dp)
                        .width(IndicatorWidth)
                        .height(IndicatorHeight)
                        .clip(RoundedCornerShape(IndicatorHeight / 2))
                        .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** The name of a theme chip's YouTube category ([com.fedoratube.org.data.recommendation.NeuroCategories]). */
@StringRes
private fun themeLabel(category: String): Int? =
    when (category) {
        "music" -> R.string.category_music
        "gaming" -> R.string.category_gaming
        "sports" -> R.string.category_sports
        "education" -> R.string.category_education
        "news" -> R.string.category_news
        "science" -> R.string.home_theme_science
        "comedy" -> R.string.home_theme_comedy
        "film" -> R.string.home_theme_film
        "autos" -> R.string.home_theme_autos
        "pets" -> R.string.home_theme_pets
        "travel" -> R.string.home_theme_travel
        "howto" -> R.string.home_theme_howto
        "nonprofits" -> R.string.home_theme_nonprofits
        else -> null
    }

@Composable
private fun HomeChip.label(): String =
    when (this) {
        HomeChip.All -> stringResource(R.string.home_chip_all)
        HomeChip.Subscriptions -> "Subs"
        HomeChip.Music -> "Music"
        is HomeChip.Interest -> interest.category?.let(::themeLabel)?.let { stringResource(it) } ?: interest.label
        HomeChip.NewToYou -> stringResource(R.string.home_chip_new_to_you)
        HomeChip.RecentlyUploaded -> stringResource(R.string.home_chip_recently_uploaded)
        HomeChip.Mixes -> stringResource(R.string.home_chip_mixes)
        HomeChip.Live -> stringResource(R.string.home_chip_live)
        HomeChip.Watched -> stringResource(R.string.home_chip_watched)
    }
