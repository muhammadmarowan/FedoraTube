package com.fedoratube.org.ui.components.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import com.fedoratube.org.ui.components.shared.FlowSearchField

/**
 * Modern Search bar: Clean pill field carrying the query and its integrated clear button.
 * Back button appears when in results mode or when onBack is explicitly passed.
 */
@Composable
fun SearchTopBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.search_videos_channels_placeholder),
    onVoiceSearch: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    onFieldFocused: () -> Unit = {},
    actions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        FlowSearchField(
            state = textFieldState,
            placeholder = placeholder,
            modifier = Modifier.weight(1f),
            onSearch = onSearch,
            onClear = { textFieldState.clearText() },
            showClearButton = true,
            focusRequester = focusRequester,
            onFieldFocused = onFieldFocused,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            },
        )

        actions?.invoke()
    }
}

/** The filter and layout affordances, shown only once results are on screen. Filter option removed. */
@Composable
fun SearchTopBarActions(
    activeFilterCount: Int,
    isGridMode: Boolean = false,
    onOpenFilters: () -> Unit,
    onToggleGridMode: () -> Unit = {},
) {
    // Filter option removed per user request
}
