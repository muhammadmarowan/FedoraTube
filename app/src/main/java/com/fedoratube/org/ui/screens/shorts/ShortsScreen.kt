package com.fedoratube.org.ui.screens.shorts

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.fedoratube.org.player.GlobalPlayerState
import com.fedoratube.org.player.config.VideoSizeCap
import com.fedoratube.org.player.datasource.YouTubeHttpDataSource
import com.fedoratube.org.player.factory.PlayerFactory
import com.fedoratube.org.ui.OnTabReselected
import com.fedoratube.org.ui.components.shared.FlowCommentsBottomSheet
import com.fedoratube.org.ui.components.shared.FlowErrorState
import com.fedoratube.org.ui.components.shared.VideoThumbnailImage
import com.fedoratube.org.ui.screens.shorts.ShortsPlayback.resolveMediaSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    var showComments by remember { mutableStateOf(false) }

    // One animation drives BOTH the video shrinking up and the comments sheet sliding up — and
    // back, in perfect sync. Previously the video used its own Dp spring while the sheet used a
    // second spring (and on close the sheet was unmounted instantly with no exit animation at
    // all), so the two drifted apart and dropped frames. Now:
    // - a single Animatable (0 = closed, 1 = open) is shared with FlowBottomSheet via
    //   externalProgress, whose slide is a GPU graphicsLayer translation (no recomposition), and
    // - the video container height is a plain fillMaxHeight fraction of that same value, with the
    //   overlays fading through graphicsLayer alpha instead of separate AnimatedVisibility layers.
    // A spring (not a fixed tween) keeps velocity when the user reverses mid-animation — re-tapping
    // comment, or dragging the sheet half-way and letting go, turns around smoothly instead of
    // restarting. The same spec is handed to the sheet, so drag-snap-back matches too.
    val commentsProgress = remember { Animatable(0f) }
    val commentsAnimationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
        }
    val commentsScope = rememberCoroutineScope()
    fun openCommentsSheet(item: ShortItem) {
        viewModel.openComments(item.videoId)
        if (!showComments) {
            showComments = true
            GlobalPlayerState.setShortsCommentsOpen(true)
        }
        commentsScope.launch { commentsProgress.animateTo(1f, commentsAnimationSpec) }
    }

    // Called by the sheet only AFTER it has animated externalProgress back to 0 (drag-down or
    // back press), so the video is already full-height again when the sheet unmounts — no jump.
    fun closeCommentsSheet() {
        showComments = false
        viewModel.closeComments()
        GlobalPlayerState.setShortsCommentsOpen(false)
    }

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
    val sourceCache = remember { mutableMapOf<String, MediaSource>() }

    val items = uiState.items
    val pagerState = rememberPagerState(pageCount = { items.size })
    val currentIndex = pagerState.currentPage
    val currentItem = items.getOrNull(currentIndex)

    // Tapping the already-selected Shorts nav icon refreshes the feed and jumps to the top.
    OnTabReselected("shorts") {
        if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
        viewModel.load(source, force = true)
    }

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
        viewModel.refreshSaved(item.videoId)
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
    // In Shorts, content runs edge-to-edge behind the bottom navigation bar.
    val insets = com.fedoratube.org.ui.components.layout.LocalFlowBottomInsets.current
    val navigationBottom = insets.navigationBottom

    DisposableEffect(Unit) {
        onDispose {
            GlobalPlayerState.setShortsCommentsOpen(false)
        }
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        val totalHeight = maxHeight
        // 0 when closed, 1 when comments fully open; video occupies the top 35% at 1.
        val commentsOpenFraction = commentsProgress.value
        val videoHeightFraction = 1f - commentsOpenFraction * 0.65f

        // Video container: fills the full screen normally; shrinks to the top 35% as the same
        // commentsProgress value that slides the sheet up, keeping playback active letterboxed
        // in the top view. fillMaxHeight fraction (not an animated Dp) keeps the per-frame work
        // to one layout fraction, and overlays below fade on the GPU instead of re-laying-out.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(videoHeightFraction)
                    .align(Alignment.TopCenter)
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
                            userScrollEnabled = !showComments,
                            modifier = Modifier.fillMaxSize(),
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
                                        contentScale = if (showComments) ContentScale.Fit else ContentScale.Crop,
                                        portrait = true,
                                    )
                                }
                                // The PlayerView lives INSIDE its page, under the overlay in
                                // the same page. When comments are open, resizeMode shifts
                                // to FIT so the 9:16 video letterboxes neatly in the top section.
                                if (isCurrent) {
                                    AndroidView(
                                        factory = { ctx ->
                                            PlayerView(ctx).apply {
                                                useController = false
                                                resizeMode =
                                                    if (showComments) {
                                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                    } else {
                                                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                    }
                                                setShutterBackgroundColor(Color.Black.toArgb())
                                                this.player = player
                                            }
                                        },
                                        update = { playerView ->
                                            playerView.resizeMode =
                                                if (showComments) {
                                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                } else {
                                                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                                }
                                            playerView.player = player
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
                                                modifier = Modifier.size(if (showComments) 40.dp else 72.dp),
                                            )
                                        }
                                    }
                                    // Fades with the same progress as the sheet/video (GPU alpha),
                                    // so it never pops or lags a frame behind the resize.
                                    if (commentsOpenFraction < 1f) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer { alpha = 1f - commentsOpenFraction },
                                        ) {
                                        ShortsOverlay(
                                            item = item,
                                            liked = item.videoId in likedIds.value,
                                            saved = item.videoId in savedIds,
                                            onLike = {
                                                val nowLiked = item.videoId !in likedIds.value
                                                likedIds.value = if (nowLiked) likedIds.value + item.videoId else likedIds.value - item.videoId
                                                viewModel.onLikeToggled(item.videoId, nowLiked)
                                            },
                                            onSave = { viewModel.toggleSave(item) },
                                            onComments = { openCommentsSheet(item) },
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
                                            navigationBottom = navigationBottom,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                        }
                                    }
                                }
                            }
                        }
                        // Progress bar + top controls fade on the same GPU alpha as the overlay,
                        // instead of their own fade animations lagging the video resize.
                        if (commentsOpenFraction < 1f) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(bottom = navigationBottom)
                                        .graphicsLayer { alpha = 1f - commentsOpenFraction },
                            ) {
                                ShortsProgressBar(
                                    player = player,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        // Floating top controls over the video (no app-name bar).
                        if (commentsOpenFraction < 1f) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .graphicsLayer { alpha = 1f - commentsOpenFraction },
                            ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier
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
                            }
                        }
                        if (uiState.isLoadingMore && !showComments) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = navigationBottom + 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }

        // YouTube-Shorts style: comments sheet occupies bottom ~65% while video
        // continues playing smoothly in the top 35%. Same progress value drives both, so
        // opening AND closing (drag-down / back) move video + sheet together frame-by-frame.
        // No sort-category chips and no cross button here — drag down or press back to close.
        if (showComments && commentsState.videoId.isNotBlank()) {
            FlowCommentsBottomSheet(
                comments = commentsState.comments,
                isLoading = commentsState.isLoading,
                onDismiss = { closeCommentsSheet() },
                isLoadingMore = commentsState.isLoadingMore,
                onLoadMore = viewModel::loadMoreComments,
                hasMore = commentsState.hasMore,
                totalText = commentsState.totalText,
                onAuthorClick = { channelId -> onChannelClick(channelId) },
                onAvatarClick = { channelId -> onChannelClick(channelId) },
                expandedHeight = totalHeight * 0.65f,
                dismissOnOutsideTap = false,
                showSortChips = false,
                showCloseButton = false,
                externalProgress = commentsProgress,
                animationSpec = commentsAnimationSpec,
            )
        }
    }
}

/**
 * The timeline, seekable like YouTube Shorts: press or drag anywhere on it to jump
 * forward/backward, with a time tooltip while scrubbing. Owns its ticker state, so the
 * pager above never recomposes for the progress bar.
 */
@Composable
private fun ShortsProgressBar(
    player: ExoPlayer,
    modifier: Modifier = Modifier,
) {
    var progress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    LaunchedEffect(player) {
        while (true) {
            val duration = player.duration
            if (!scrubbing) {
                progress =
                    if (duration > 0) {
                        (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
            }
            delay(150)
        }
    }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(player) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val duration = player.duration
                        if (duration > 0) player.seekTo((duration * fraction).toLong())
                    }
                }
                .pointerInput(player) {
                    detectDragGestures(
                        onDragStart = {
                            scrubbing = true
                            scrubFraction = (it.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            progress = scrubFraction
                        },
                        onDragEnd = {
                            scrubbing = false
                            val duration = player.duration
                            if (duration > 0) player.seekTo((duration * scrubFraction).toLong())
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            scrubFraction = (change.position.x / size.width.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
                            progress = scrubFraction
                        },
                    )
                },
        // The bar sits at the very bottom of its touch area, so the line always
        // touches the navigation bar — no gap above it.
        contentAlignment = Alignment.BottomCenter,
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(if (scrubbing) 5.dp else 3.dp),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.25f),
        )
    }
}

@Composable
private fun ShortsOverlay(
    item: ShortItem,
    liked: Boolean,
    saved: Boolean,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComments: () -> Unit,
    onChannelClick: () -> Unit,
    onShare: () -> Unit,
    navigationBottom: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // The shadow runs from behind the caption all the way down INTO the
        // navigation bar (its darkest end hides behind the bar), so there is no
        // hard cut line where the shadow meets the bar.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.95f),
                            ),
                        ),
                    ),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fraction = 0.78f)
                    .padding(start = 12.dp, bottom = navigationBottom + 6.dp, end = 8.dp),
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
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                shadow =
                                    androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.8f),
                                        blurRadius = 6f,
                                        offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                    ),
                            ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            shadow =
                                androidx.compose.ui.graphics.Shadow(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    blurRadius = 6f,
                                    offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                ),
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Rail sits at the bottom, directly above the timeline progress bar.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = navigationBottom + 8.dp),
        ) {
            // YouTube-Shorts style: plain outline icons, no circles. Love (heart) means
            // like; below the comment bubble sits the Save bookmark for Watch Later.
            ShortsRailButton(
                label = item.likeCountText,
                selected = liked,
                onClick = onLike,
            ) {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.like),
                    tint = if (liked) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
            ShortsRailButton(label = item.commentCountText, onClick = onComments) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = stringResource(R.string.comments),
                    tint = Color.White,
                )
            }
            ShortsRailButton(
                label = if (saved) stringResource(R.string.saved) else stringResource(R.string.save),
                selected = saved,
                onClick = onSave,
            ) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.save),
                    tint = if (saved) MaterialTheme.colorScheme.primary else Color.White,
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
 * YouTube-Shorts rail button: a plain large outline icon with its count in bold white
 * below it — no circle. The bottom scrim behind the rail keeps it readable on any video.
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
            modifier = Modifier.size(44.dp),
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
            )
        }
    }
}
