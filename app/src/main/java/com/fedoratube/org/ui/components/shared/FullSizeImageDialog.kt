package com.fedoratube.org.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.fedoratube.org.R

@Composable
fun FullSizeImageDialog(
    imageUrls: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
) {
    if (imageUrls.isEmpty()) return
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex),
        pageCount = { imageUrls.size }
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { onDismiss() },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(vertical = 32.dp),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(toHighQualityAvatarUrl(imageUrls[page]))
                                    .crossfade(true)
                                    .size(1600, 1600)
                                    .scale(coil3.size.Scale.FIT)
                                    .allowHardware(false)
                                    .build(),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxWidth(0.92f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit,
                            filterQuality = FilterQuality.High,
                        )
                    }
                }

                if (imageUrls.size > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(imageUrls.size) { iteration ->
                            val isCurrent = pagerState.currentPage == iteration
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) Color.White else Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.tap_to_close),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun FullSizeImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    FullSizeImageDialog(
        imageUrls = listOf(imageUrl),
        initialIndex = 0,
        onDismiss = onDismiss,
    )
}
