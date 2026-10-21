package com.fedoratube.org.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.shared.FlowMorphingPortrait

private val PortraitSize = 56.dp
private val ChipSpacing = 8.dp
private const val PORTRAIT_DELAY = 2
private val StageTopPadding = 24.dp
private val HeroStageSize = 300.dp
private val TextSpacing = 10.dp

/** Where subscribed channels land around the hero, in the order they were added. */
private val PortraitOrbit =
    listOf(
        DpOffset((-114).dp, (-78).dp),
        DpOffset(116.dp, (-92).dp),
        DpOffset((-120).dp, 70.dp),
        DpOffset(120.dp, 58.dp),
        DpOffset(0.dp, (-124).dp),
    )

@Composable
internal fun ReadyStep(
    state: OnboardingUiState,
    artistCount: Int,
    isFocusMode: Boolean,
    hero: HeroSlot,
    onEdit: (OnboardingStep) -> Unit,
    contentPadding: PaddingValues,
) {
    BookendLayout(
        contentPadding = contentPadding,
        stage = {
            hero(HeroReady)
            state.subscribed.take(PortraitOrbit.size).forEachIndexed { index, channel ->
                val offset = PortraitOrbit[index]
                FlowMorphingPortrait(
                    imageUrl = channel.thumbnailUrl,
                    fallback = channel.name,
                    diameter = PortraitSize,
                    accent = MaterialTheme.colorScheme.secondaryContainer,
                    onAccent = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.offset(offset.x, offset.y),
                    delayIndex = index + PORTRAIT_DELAY,
                )
            }
        },
    ) {
        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.displaySmallEmphasized,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.onboarding_ready_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(ChipSpacing)) {
            SummaryChip(
                text = if (isFocusMode) stringResource(R.string.focus_mode_title) else stringResource(R.string.onboarding_mode_normal_title),
                icon = if (isFocusMode) Icons.Outlined.CenterFocusStrong else Icons.Outlined.SmartDisplay,
            ) { onEdit(OnboardingStep.MODE) }
            SummaryChip(
                text = pluralStringResource(R.plurals.onboarding_ready_interests, state.topics.size, state.topics.size),
                icon = Icons.Outlined.Interests,
            ) { onEdit(OnboardingStep.INTERESTS) }
        }
    }
}

@Composable
private fun SummaryChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(SuggestionChipDefaults.IconSize)) },
        colors =
            SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                iconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        border = null,
    )
}

@Composable
internal fun BookendLayout(
    contentPadding: PaddingValues,
    stage: @Composable () -> Unit,
    text: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(contentPadding),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = StageTopPadding),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(HeroStageSize), contentAlignment = Alignment.Center) { stage() }
            }
            Column(verticalArrangement = Arrangement.spacedBy(TextSpacing)) { text() }
        }
    }
}
