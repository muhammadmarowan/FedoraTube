package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.fedoratube.org.R

/** The app's badge/icon. Size it by width. */
@Composable
fun FlowLogo(modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f)) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground_image),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
