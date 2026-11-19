package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Centered popup dialog with smooth entrance animation, modern rounded corners,
 * and responsive constraints. Replaces bottom sheets with an elegant centered presentation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowCenteredPopupDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    maxHeight: Dp? = null,
    maxWidth: Dp = 480.dp,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
    ),
    content: @Composable ColumnScope.() -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val effectiveMaxHeight = maxHeight ?: (screenHeight * 0.85f).coerceAtLeast(320.dp)

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = properties,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = maxWidth)
                .fillMaxWidth(0.92f)
                .heightIn(max = effectiveMaxHeight),
            shape = shape,
            color = containerColor,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                content()
            }
        }
    }
}
