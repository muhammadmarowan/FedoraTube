package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fedoratube.org.R
import kotlinx.coroutines.flow.drop

@Composable
fun FedoraSearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFieldFocused: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    releaseFocusWithKeyboard: Boolean = false,
) {
    if (releaseFocusWithKeyboard) ReleaseFocusWhenKeyboardCloses()
    Surface(
        modifier = modifier.height(PillHeight),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    start = if (leadingIcon != null) PillLeadingPadding else PillStartPadding,
                    end = PillEndPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(PillItemSpacing))
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    state = state,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                            .onFocusChanged { if (it.isFocused) onFieldFocused() },
                    textStyle =
                        LocalTextStyle.current.merge(
                            MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        ),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    onKeyboardAction = { onSearch(state.text.toString()) },
                )
            }

            if (showClearButton && state.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        state.clearText()
                        onClear?.invoke()
                    },
                    modifier = Modifier.size(ClearButtonSize),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.clear),
                    )
                }
            }

            trailingContent?.invoke(this)
        }
    }
}

@Composable
fun FedoraSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFieldFocused: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    releaseFocusWithKeyboard: Boolean = false,
) {
    val state = remember { TextFieldState(initialText = query) }
    val echoes = remember { QueryEchoFilter() }
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1)
            .collect {
                echoes.sent(it)
                currentOnQueryChange(it)
            }
    }

    LaunchedEffect(query) {
        if (echoes.shouldApply(query) && query != state.text.toString()) state.setTextAndPlaceCursorAtEnd(query)
    }

    FedoraSearchField(
        state = state,
        placeholder = placeholder,
        modifier = modifier,
        onSearch = { onSearch() },
        onClear = onClear,
        showClearButton = showClearButton,
        focusRequester = focusRequester,
        onFieldFocused = onFieldFocused,
        leadingIcon = leadingIcon,
        trailingContent = trailingContent,
        releaseFocusWithKeyboard = releaseFocusWithKeyboard,
    )
}

@Composable
fun FlowSearchField(
    state: TextFieldState,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFieldFocused: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    releaseFocusWithKeyboard: Boolean = false,
) = FedoraSearchField(
    state = state,
    placeholder = placeholder,
    modifier = modifier,
    onSearch = onSearch,
    onClear = onClear,
    showClearButton = showClearButton,
    focusRequester = focusRequester,
    onFieldFocused = onFieldFocused,
    leadingIcon = leadingIcon,
    trailingContent = trailingContent,
    releaseFocusWithKeyboard = releaseFocusWithKeyboard,
)

@Composable
fun FlowSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFieldFocused: () -> Unit = {},
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
    releaseFocusWithKeyboard: Boolean = false,
) = FedoraSearchField(
    query = query,
    onQueryChange = onQueryChange,
    placeholder = placeholder,
    modifier = modifier,
    onSearch = onSearch,
    onClear = onClear,
    showClearButton = showClearButton,
    focusRequester = focusRequester,
    onFieldFocused = onFieldFocused,
    leadingIcon = leadingIcon,
    trailingContent = trailingContent,
    releaseFocusWithKeyboard = releaseFocusWithKeyboard,
)

internal class QueryEchoFilter {
    private val pending = ArrayDeque<String>()

    fun sent(query: String) {
        pending.addLast(query)
        if (pending.size > MAX_PENDING) pending.removeFirst()
    }

    fun shouldApply(query: String): Boolean {
        if (query == pending.lastOrNull()) {
            pending.clear()
            return false
        }
        if (query in pending) return false
        pending.clear()
        return true
    }

    private companion object {
        const val MAX_PENDING = 64
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReleaseFocusWhenKeyboardCloses() {
    val imeVisible = WindowInsets.isImeVisible
    val focusManager = LocalFocusManager.current
    LaunchedEffect(imeVisible) { if (!imeVisible) focusManager.clearFocus() }
}

private val PillHeight = 40.dp
private val PillStartPadding = 16.dp
private val PillLeadingPadding = 12.dp
private val PillEndPadding = 4.dp
private val PillItemSpacing = 8.dp
private val ClearButtonSize = 32.dp

