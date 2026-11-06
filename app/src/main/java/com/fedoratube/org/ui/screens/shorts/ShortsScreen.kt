package com.fedoratube.org.ui.screens.shorts

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.fedoratube.org.R
import com.fedoratube.org.data.feed.ShortsQueueSource
import com.fedoratube.org.data.local.PlayerPreferences
import com.fedoratube.org.player.config.VideoSizeCap
import com.fedoratube.org.player.datasource.YouTubeHttpDataSource
import com.fedoratube.org.player.factory.PlayerFactory
import com.fedoratube.org.ui.components.shared.FlowCommentsBottomSheet
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.VideoThumbnailImage
import com.fedoratube.org.ui.screens.shorts.ShortsPlayback.resolveMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * YouTube-Shorts-style vertical pager.
 *
 * What keeps it from stuttering (atke-atke jawa):
 * - The progress ticker owns its own state inside [ShortsProgressBar], so it never
 *   recomposes the pager.
 * - The next/previous Short's media source is pre-resolved into [sourceCache] while the
 *   current one plays, and overlays are enriched only for the visible Short.
 * - The [PlayerView] sits inside its page with the overlay above it in the same page, so
 *   like/dislike/comment/share are always visible and tappable — never under the surface.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortsScreen(
    source: ShortsQueueSource,
    onBack: () -> Unit,
    onChannelClick: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    isTabRoot: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ShortsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val preferences = remember(context) { PlayerPreferences(context) }
    val isFocusModeEnabled by preferences.focusModeEnabled.collectAsStateWithLifecycle(initialValue = false)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val commentsState by viewModel.commentsState.collectAsStateWithLifecycle()
    var showComments by remember { mutableStateOf(false) }

    if (isFocusModeEnabled) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.no_shorts_found), color = Color.White)
        }
        return
    }

    LaunchedEffect(source) { viewModel.load(source) }

    // One player for the whole pager.
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
    LifecycleStartEffect(player) {
        onStopOrDispose { player.pause() }
    }

    var paused by remember { mutableStateOf(false) }
    var playerReady by remember { mutableStateOf(false) }
    val likedIds = remember { mutableStateOf(setOf<String>()) }
    val dislikedIds = remember { mutableStateOf(setOf<String>()) }
    val sourceCache = remember { mutableMapOf<String, MediaSource>() }

    val items = uiState.items
    val pagerState = rememberPagerState(pageCount = { items.size })
    val currentIndex = pagerState.currentPage
    val currentItem = items.getOrNull(currentIndex)

    LaunchedEffect(currentIndex, items.size) {
        if (items.isNotEmpty() && currentIndex >= items.size - 3) viewModel.loadMore()
    }

    // Pre-resolve neighbours so a swipe lands on an already-ready source.
    LaunchedEffect(currentIndex, items.size) {
        listOf(currentIndex + 1, currentIndex - 1).forEach { neighbour ->
            val item = items.getOrNull(neighbour) ?: return@forEach
            if (!sourceCache.containsKey(item.videoId)) {
                val resolved =
                    withContext(Dispatchers.IO) {
                        runCatching { resolveMediaSource(context.applicationContext, preferences, item.videoId) }.getOrNull()
                    }
                if (resolved != null) sourceCache[item.videoId] = resolved
            }
        }
    }

    // (Re)load the current short into the shared player.
    LaunchedEffect(currentItem?.videoId) {
        val item = currentItem ?: return@LaunchedEffect
        viewModel.enrichItem(item.videoId)
        playerReady = false
        paused = false
        player.volume = 1f
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.setVideoScalingMode(androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
        val cached = sourceCache[item.videoId]
        val mediaSource =
            cached
                ?: withContext(Dispatchers.IO) {
                    runCatching { resolveMediaSource(context.applicationContext, preferences, item.videoId) }.getOrNull()
                }
        if (mediaSource != null) {
            sourceCache[item.videoId] = mediaSource
            runCatching {
                player.setMediaSource(mediaSource)
                player.prepare()
                player.playWhenReady = true
                playerReady = true
            }
        }
    }

    // Full-bleed video: from the very top of the screen down to the navigation bar.
    // No app-name bar here; only an overlay search (and back, when not the tab) floats
    // over the video at the top, like YouTube Shorts.
    val navigationBottom = com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets.current.navigationBottom
    Box(
        modifier =
            Modifier
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        VerticalPager(
                            state = pagerState,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(bottom = navigationBottom),
                            beyondViewportPageCount = 1,
                            // Index-safe: the pager can ask for a key/content while a fresh
                            // page list is still settling (1-item first page -> append).
                            key = { index -> items.getOrNull(index)?.videoId ?: "page-$index" },
                        ) { page ->
                            val item = items.getOrNull(page) ?: return@VerticalPager
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
                                // Poster only until the video actually renders underneath it.
                                if (!(isCurrent && playerReady)) {
                                    VideoThumbnailImage(
                                        videoId = item.videoId,
                                        model = item.posterUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        portrait = true,
                                    )
                                }
                                // The PlayerView lives INSIDE its page, under the overlay in
                                // the same page — a hoisted PlayerView sat above every page's
                                // overlay, hiding the like rail and going black on scroll.
                                if (isCurrent) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                useController = false
                                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                setShutterBackgroundColor(Color.Black.toArgb())
                                                this.player = player
                                            }
                                        },
                                        onRelease = { view -> view.player = null },
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    if (!playerReady) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(color = Color.White)
                                        }
                                    }
                                    if (paused && playerReady) {
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
                                        liked = item.videoId in likedIds.value,
                                        disliked = item.videoId in dislikedIds.value,
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
                                        onComments = {
                                            viewModel.openComments(item.videoId)
                                            showComments = true
                                        },
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
                                }
                            }
                        }
                        ShortsProgressBar(
                            player = player,
                            modifier =
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(bottom = navigationBottom),
                        )
                        // Floating top controls over the video (no app-name bar).
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                                        ),
                                    )
                                    .statusBarsPadding()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            if (!isTabRoot) {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = onSearchClick) {
                                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search), tint = Color.White)
                            }
                        }
                        if (uiState.isLoadingMore) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

    if (showComments && commentsState.videoId.isNotBlank()) {
        FlowCommentsBottomSheet(
            comments = commentsState.comments,
            isLoading = commentsState.isLoading,
            onDismiss = {
                showComments = false
                viewModel.closeComments()
            },
            isLoadingMore = commentsState.isLoadingMore,
            onLoadMore = viewModel::loadMoreComments,
            hasMore = commentsState.hasMore,
            totalText = commentsState.totalText,
            onAuthorClick = { channelId -> onChannelClick(channelId) },
            onAvatarClick = { channelId -> onChannelClick(channelId) },
        )
    }
}

/** Owns its ticker state, so the pager above never recomposes for the progress bar. */
@Composable
private fun ShortsProgressBar(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    var progress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            progress =
                if (duration > 0) {
                    (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
            delay(150)
        }
    }
    LinearProgressIndicator(
        progress = { progress },
        modifier =
            modifier
                .fillMaxWidth()
                .height(3.dp),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.25f),
    )
}

@Composable
private fun ShortsOverlay(
    item: ShortItem,
    liked: Boolean,
    disliked: Boolean,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onComments: () -> Unit,
    onChannelClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Bottom scrim: keeps title, channel and rail readable on any bright video.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        ),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction = 0.78f)
                    .padding(start = 12.dp, bottom = 16.dp, end = 8.dp),
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
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .padding(1.5.dp)
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

        // Rail sits at the bottom, directly above the timeline progress bar.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 16.dp),
        ) {
            ShortsRailButton(
                label = item.likeCountText,
                selected = liked,
                onClick = onLike,
            ) {
                Icon(
                    imageVector = if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = stringResource(R.string.like),
                    tint = Color.White,
                )
            }
            ShortsRailButton(label = null, selected = disliked, onClick = onDislike) {
                Icon(
                    imageVector = if (disliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Dislike",
                    tint = Color.White,
                )
            }
            ShortsRailButton(label = item.commentCountText, onClick = onComments) {
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
        }
    }
}

/**
 * Professional rail button matching the app theme: a frosted dark circle with a hairline
 * border that reads on any video, the theme primary filling it when selected (liked /
 * disliked), and a bold label underneath with its own scrim-darkened pill.
 */
@Composable
private fun ShortsRailButton(
    label: String?,
    selected: Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier =
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.52f)
                        },
                    ),
        ) {
            content()
        }
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier =
                    Modifier
                        .padding(top = 2.dp, bottom = 2.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}
