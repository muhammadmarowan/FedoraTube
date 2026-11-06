package com.fedoratube.org.ui.screens.shorts

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.fedoratube.org.R
import com.fedoratube.org.data.feed.ShortsQueueSource
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.player.config.VideoSizeCap
import com.fedoratube.org.player.datasource.YouTubeHttpDataSource
import com.fedoratube.org.player.factory.PlayerFactory
import com.fedoratube.org.ui.components.layout.flowBottomContentPadding
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.VideoThumbnailImage
import com.fedoratube.org.ui.screens.shorts.ShortsPlayback.resolveMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * YouTube-Shorts-style vertical pager. One shared player plays whichever page is on screen;
 * the other pages show their poster, so swiping is instant and memory stays flat. The queue
 * itself comes from [ShortsViewModel] — algorithmic feed, channel, subscriptions, a shelf
 * snapshot, or saved Shorts. Focus mode never reaches here with content (see the guard below
 * and the navigation layer); if it does, nothing loads.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    source: ShortsQueueSource,
    onBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onOpenVideo: (String) -> Unit = {},
    isTabRoot: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ShortsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val preferences = remember(context) { PlayerPreferences(context) }
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)

    if (isFocusModeEnabled) {
        Scaffold(
            containerColor = Color.Black,
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.shorts), color = Color.White) },
                    navigationIcon = {
                        if (!isTabRoot) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                )
            },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.no_shorts_found), color = Color.White)
            }
        }
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(source) { viewModel.load(source) }

    val playerFactory = remember { PlayerFactory() }
    val player =
        remember {
            val appContext = context.applicationContext
            val dataSourceFactory =
                androidx.media3.datasource.DefaultDataSource
                    .Factory(appContext, YouTubeHttpDataSource.Factory())
            playerFactory.createPlayer(
                appContext,
                playerFactory.createTrackSelector(appContext, VideoSizeCap.forDevice(appContext)),
                playerFactory.createLoadControl(appContext),
                playerFactory.createRenderersFactory(appContext),
                playerFactory.createBandwidthMeter(appContext),
                dataSourceFactory,
            )
        }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var muted by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var playerReady by remember { mutableStateOf(false) }
    val likedIds = remember { mutableStateOf(setOf<String>()) }
    val dislikedIds = remember { mutableStateOf(setOf<String>()) }

    val items = uiState.items
    val exoPlayer = player
    val pagerState = rememberPagerState(pageCount = { items.size.coerceAtLeast(0) })
    val currentIndex = pagerState.currentPage
    val currentItem = items.getOrNull(currentIndex)

    // Load more as the pager nears the end.
    LaunchedEffect(currentIndex, items.size) {
        if (items.isNotEmpty() && currentIndex >= items.size - 3) viewModel.loadMore()
    }

    // (Re)load the current short into the shared player.
    LaunchedEffect(currentItem?.videoId) {
        val item = currentItem ?: return@LaunchedEffect
        playerReady = false
        progress = 0f
        paused = false
        player.volume = if (muted) 0f else 1f
        player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
        val mediaSource =
            withContext(Dispatchers.IO) {
                runCatching { resolveMediaSource(context.applicationContext, preferences, item.videoId) }.getOrNull()
            }
        if (mediaSource != null) {
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true
            playerReady = true
        }
    }

    LaunchedEffect(muted) {
        player.volume = if (muted) 0f else 1f
    }

    // Progress bar ticker for the current short.
    LaunchedEffect(player, currentItem?.videoId) {
        while (true) {
            val duration = player.duration
            progress =
                if (duration > 0) {
                    (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            delay(200)
        }
    }

    LifecycleStartEffect(player) {
        onStopOrDispose { player.pause() }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        when {
            uiState.isLoading && items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            uiState.error != null && items.isEmpty() -> {
                FlowErrorState(
                    error = uiState.error ?: stringResource(R.string.error_failed_to_load_shorts),
                    onRetry = { viewModel.load(source) },
                )
            }

            items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_shorts_found), color = Color.White)
                }
            }

            else -> {
                VerticalPager(
                    state = pagerState,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = flowBottomContentPadding()),
                    beyondViewportPageCount = 1,
                    key = { index -> items[index].videoId },
                ) { page ->
                    val item = items[page]
                    val isCurrent = page == currentIndex
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable {
                                    if (isCurrent) {
                                        paused = !paused
                                        player.playWhenReady = !paused
                                    }
                                },
                    ) {
                        // Poster behind the video (and the whole page when it is not current).
                        VideoThumbnailImage(
                            videoId = item.videoId,
                            model = item.posterUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            portrait = true,
                        )
                        if (isCurrent) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        useController = false
                                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        this.player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (!playerReady) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color.White)
                                }
                            }
                            if (paused) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.play),
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                            }
                            ShortsOverlay(
                                item = item,
                                muted = muted,
                                liked = item.videoId in likedIds.value,
                                disliked = item.videoId in dislikedIds.value,
                                onMuteToggle = { muted = !muted },
                                onLike = {
                                    val nowLiked = item.videoId !in likedIds.value
                                    likedIds.value = if (nowLiked) likedIds.value + item.videoId else likedIds.value - item.videoId
                                    if (nowLiked) dislikedIds.value = dislikedIds.value - item.videoId
                                    viewModel.onLikeToggled(item.videoId, nowLiked)
                                },
                                onDislike = {
                                    val nowDisliked = item.videoId !in dislikedIds.value
                                    dislikedIds.value =
                                        if (nowDisliked) dislikedIds.value + item.videoId else dislikedIds.value - item.videoId
                                    if (nowDisliked) likedIds.value = likedIds.value - item.videoId
                                },
                                onOpenVideo = { onOpenVideo(item.videoId) },
                                onChannelClick = { onChannelClick(item.channelId) },
                                onShare = {
                                    val intent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://www.youtube.com/shorts/${item.videoId}",
                                            )
                                        }
                                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                            // Progress bar at the bottom of the reel.
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(3.dp),
                                color = Color.White,
                                trackColor = Color.White.copy(alpha = 0.25f),
                            )
                        }
                    }
                }
                if (uiState.isLoadingMore) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        // Top bar: back (when this is not the tab), Shorts wordmark.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (!isTabRoot) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                }
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_shorts_filled),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.shorts),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ShortsOverlay(
    item: ShortItem,
    muted: Boolean,
    liked: Boolean,
    disliked: Boolean,
    onMuteToggle: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onOpenVideo: () -> Unit,
    onChannelClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Channel + title at the bottom left, clear of the action rail.
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction = 0.78f)
                    .padding(start = 12.dp, bottom = 28.dp, end = 8.dp),
        ) {
            if (item.channelName.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onChannelClick),
                ) {
                    if (item.channelAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = item.channelAvatarUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.channelName,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!item.soundTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.soundTitle,
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Action rail on the right edge, YouTube-style.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp, bottom = 40.dp),
        ) {
            ShortsRailButton(
                label = item.likeCountText,
                onClick = onLike,
            ) {
                Icon(
                    imageVector = if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = stringResource(R.string.like),
                    tint = if (liked) Color.White else Color.White,
                )
            }
            ShortsRailButton(label = null, onClick = onDislike) {
                Icon(
                    imageVector = if (disliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Dislike",
                    tint = Color.White,
                )
            }
            ShortsRailButton(label = item.commentCountText, onClick = onOpenVideo) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = stringResource(R.string.comments),
                    tint = Color.White,
                )
            }
            ShortsRailButton(label = null, onClick = onShare) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = Color.White,
                )
            }
            ShortsRailButton(label = null, onClick = onOpenVideo) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = "Open video",
                    tint = Color.White,
                )
            }
            ShortsRailButton(label = null, onClick = onMuteToggle) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ShortsRailButton(
    label: String?,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
        ) {
            content()
        }
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

