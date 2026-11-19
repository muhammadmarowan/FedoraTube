package com.fedoratube.org.ui.screens.onboarding

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fedoratube.org.data.backup.ImportKind
import com.fedoratube.org.data.backup.ImportSource
import com.fedoratube.org.ui.components.shared.FlowCenteredPopupDialog
import com.fedoratube.org.ui.components.shared.FlowNavRow
import com.fedoratube.org.ui.components.shared.FlowRowGroup
import com.fedoratube.org.ui.components.shared.FlowSheetHeader
import com.fedoratube.org.ui.components.shared.flowRowGroupShape

private val SheetBottomPadding = 16.dp

/** Everything [source] can import, the same list Settings shows under that app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportSourceSheet(
    source: ImportSource,
    onImport: (ImportKind) -> Unit,
    onDismiss: () -> Unit,
) {
    FlowCenteredPopupDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        FlowSheetHeader(
            title = stringResource(source.titleRes),
            onClose = onDismiss,
            showDragHandle = false,
        )
        FlowRowGroup(Modifier.verticalScroll(rememberScrollState()).padding(bottom = SheetBottomPadding)) {
            source.kinds.forEachIndexed { index, kind ->
                FlowNavRow(
                    title = stringResource(kind.titleRes),
                    supportingText = stringResource(kind.descriptionRes),
                    leadingIcon = Icons.Outlined.FileOpen,
                    showChevron = false,
                    shape = flowRowGroupShape(index, source.kinds.size),
                    onClick = { onImport(kind) },
                )
            }
        }
    }
}
