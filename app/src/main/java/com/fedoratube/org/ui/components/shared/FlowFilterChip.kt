/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * The single filter chip for every surface: home feed chips, search filters, browse tabs,
 * library content filters. Styled like modern YouTube pills: rounded 8.dp corners,
 * high-contrast selected surface without a leading checkmark, and tactile press-scale feedback.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlowFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val chipShape = RoundedCornerShape(8.dp)
    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        label = {
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shape = chipShape,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                selectedLabelColor = MaterialTheme.colorScheme.surface,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = Color.Transparent,
                selectedBorderColor = Color.Transparent,
            ),
        modifier = modifier.pressScale(interactionSource, pressedScale = 0.95f),
        leadingIcon = null,
    )
}

/**
 * A filter chip that opens a menu instead of toggling. [selected] marks that a non-default option is
 * active; the chevron replaces the check so the two kinds read differently side by side.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlowDropdownFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val chipShape = RoundedCornerShape(8.dp)
    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        label = {
            Text(
                text = label,
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        shape = chipShape,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                selectedLabelColor = MaterialTheme.colorScheme.surface,
                selectedTrailingIconColor = MaterialTheme.colorScheme.surface,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = Color.Transparent,
                selectedBorderColor = Color.Transparent,
            ),
        modifier = modifier.pressScale(interactionSource, pressedScale = 0.95f),
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
    )
}
