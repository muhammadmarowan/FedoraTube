package com.fedoratube.org.ui.screens.shorts

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Refresh
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
import com.fedoratube.org.ui.components.shared.FlowPullToRefreshBox
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
 * - The next Shorts' media sources are pre-resolved into [sourceCache] while the current one
 *   plays (two ahead, one behind), and overlays are enriched only for the visible Short.
 * - The [PlayerView] stays inside its page (inside the pager's scrollable, where swipes
 *   reach it) with a transparent shutter over the page's poster. A Short is "ready" only when
 *   its first frame actually renders (not when the player is merely prepared), so a swipe
 *   shows poster → video with no black flash and no spinner unless buffering drags past a
 *   short grace period.
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

    // Home-feed style refresh: back to the top Short and a fresh first page, current feed
    // staying on screen until it lands (ViewModel keeps items while isRefreshing).
    fun refreshShortsFeed() {
        if (showComments) return
        viewModel.refresh()
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
    // Set only when the current Short's first frame actually renders — the same rule the main
    // player uses (firstFrameVideoId). "Prepared" is not ready: hiding the poster at prepare()
    // time was the black flash on every swipe, since the surface was still black then.
    var firstFrameVideoId by remember { mutableStateOf<String?>(null) }
    var showBufferSpinner by remember { mutableStateOf(false) }
    val likedIds = remember { mutableStateOf(setOf<String>()) }
    val sourceCache = remember { mutableMapOf<String, MediaSource>() }

    val items = uiState.items
    val pagerState = rememberPagerState(pageCount = { items.size })
    val currentIndex = pagerState.currentPage
    val currentItem = items.getOrNull(currentIndex)
    val playerReady = currentItem != null && firstFrameVideoId == currentItem.videoId

    // The poster stays until this Short's first frame renders; a spinner only appears if
    // buffering drags past a short grace period (YouTube shows the thumbnail, not a spinner,
    // for a normal swipe). A resolved source is usually instant by swipe time, so most
    // swipes never show the spinner at all.
    LaunchedEffect(currentItem?.videoId, playerReady) {
        showBufferSpinner = false
        if (!playerReady) {
            delay(600)
            showBufferSpinner = true
        }
    }

    DisposableEffect(player, currentItem?.videoId) {
        val videoId = currentItem?.videoId ?: return@DisposableEffect onDispose {}
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    firstFrameVideoId = videoId
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Tapping the already-selected Shorts nav icon refreshes the feed and jumps to the top.
    OnTabReselected("shorts") {
        if (pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
        viewModel.load(source, force = true)
    }

    // A manual refresh (pull-to-refresh or the top-bar button) also returns to the top Short.
    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing && pagerState.currentPage != 0) pagerState.animateScrollToPage(0)
    }

    LaunchedEffect(currentIndex, items.size) {
        if (items.isNotEmpty() && currentIndex >= items.size - 3) viewModel.loadMore()
    }

    // Pre-resolve neighbours so a swipe lands on an already-ready source: two ahead (the
    // direction Shorts always scrolls), one behind for swiping back.
    LaunchedEffect(currentIndex, items.size) {
        listOf(currentIndex + 1, currentIndex + 2, currentIndex - 1).forEach { neighbour ->
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
                // playerReady flips via onRenderedFirstFrame — not here, where the screen
                // would still be black behind the poster for a beat.
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
        // The bottom navigation bar stays fixed while comments are open, and the sheet opens
        // above it: the comments area is totalHeight - navigationBottom, split 35% video /
        // 65% sheet. Video height at progress p = totalHeight - p * sheetHeight, so the video's
        // bottom edge always meets the sheet's top edge exactly, with no gap and no overlap.
        val commentsAreaHeight = totalHeight - navigationBottom
        val commentsSheetHeight = commentsAreaHeight * 0.65f
        // 0 when closed, 1 when comments fully open.
        val commentsOpenFraction = commentsProgress.value
        val videoHeightFraction = 1f - commentsOpenFraction * (commentsSheetHeight.value / totalHeight.value)
        // The video keeps its true 9:16 ratio and starts right below the status bar's black
        // strip: its box is the aspect height for the screen's width (or the animated video
        // area when comments shrink it below that), top-anchored — no black gap above the
        // video, the leftover black sits below it, above the navigation bar.
        val videoContentHeight = totalHeight * videoHeightFraction
        val videoBoxHeight = minOf(maxWidth * (16f / 9f), videoContentHeight)

        // Home-feed style pull-to-refresh around the video area (not the comments sheet):
        // pulling down on the first Short reloads the feed with a top indicator.
        FlowPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { refreshShortsFeed() },
            modifier = Modifier.fillMaxSize(),
        ) {
        // Video container: fills the screen normally (below the status bar — that strip
        // stays black, video plays under it, never behind the status icons); shrinks to the
        // top 35% as the same commentsProgress value that slides the sheet up, keeping
        // playback active letterboxed in the top view. fillMaxHeight fraction (not an
        // animated Dp) keeps the per-frame work to one layout fraction, and overlays below
        // fade on the GPU instead of re-laying-out.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(videoHeightFraction)
                    .statusBarsPadding()
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
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(videoBoxHeight)
                                .align(Alignment.TopCenter),
                    ) {
                        // The surface fades in over the poster only once its first frame
                        // renders (playerReady) — see the listener above.
                        val playerAlpha by animateFloatAsState(
                            targetValue = if (playerReady) 1f else 0f,
                            animationSpec = tween(durationMillis = 200),
                            label = "shorts_player_alpha",
                        )
                        VerticalPager(
                            state = pagerState,
                            userScrollEnabled = !showComments,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 2,
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
                                // Poster behind the surface until the video has faded in
                                // over it: the surface is a TextureView with a transparent
                                // shutter, invisible until its first frame, so a swipe shows
                                // poster → video with no black frame and no shutter teardown.
                                // Once the fade completes the poster unmounts, so a letterboxed
                                // video (comments open) sits on pure black — never a picture
                                // peeking out around the video. Two extra pages stay composed,
                                // so the next Short's poster is already cached before the
                                // swipe lands.
                                if (!playerReady || playerAlpha < 1f) {
                                    VideoThumbnailImage(
                                        videoId = item.videoId,
                                        model = item.posterUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        // Same FIT ratio as the video, so poster → first
                                        // frame does not jump or crop either.
                                        contentScale = ContentScale.Fit,
                                        portrait = true,
                                    )
                                }
                                // The PlayerView lives INSIDE its page, under the overlay in
                                // the same page — inside the pager's scrollable, where swipes
                                // and taps reach it (a surface hoisted above the pager blocks
                                // the pager's drag gestures, which froze Shorts scrolling).
                                // resizeMode is FIT in BOTH states: the whole frame always
                                // shows, never cropped. Switching FIT→ZOOM when comments
                                // closed cropped the video and made it jump/bounce back —
                                // one ratio for open and close means the height animation
                                // alone moves the video, smoothly, to its normal position.
                                if (isCurrent) {
                                    AndroidView(
                                        factory = { ctx ->
                                            (LayoutInflater.from(ctx).inflate(R.layout.video_player_view, null) as PlayerView).apply {
                                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                                subtitleView?.visibility = ViewGroup.GONE
                                                this.player = player
                                            }
                                        },
                                        update = { playerView ->
                                            playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                                            playerView.player = player
                                        },
                                        onRelease = { view -> view.player = null },
                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { alpha = playerAlpha },
                                    )
                                    if (showBufferSpinner && !playerReady) {
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
                                }
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
                                        // No statusBarsPadding here: the video container
                                        // already starts below the status bar's black strip.
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                if (!isTabRoot) {
                                    IconButton(onClick = onBack) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                // Same refresh as pull-to-refresh, as a button for discoverability.
                                IconButton(onClick = { refreshShortsFeed() }) {
                                    if (uiState.isRefreshing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh), tint = Color.White)
                                    }
                                }
                                IconButton(onClick = onSearchClick) {
                                    Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search), tint = Color.White)
                                }
                            }
                            }
                        }
                        // No bottom "loading more" spinner: entering Shorts with a short
                        // first page fires loadMore immediately, and that spinner sitting
                        // above the navigation bar on every entry was pure noise — pages
                        // append silently underneath.
                    }
                }
            }
            // Overlay + timeline live at the OUTER container level (not the aspect box),
            // bottom-anchored to the navigation bar: timeline at the bar's top edge, caption
            // above it, channel name + logo above the caption, like/comment/save/share rail
            // low at the bottom. They fade on the same GPU alpha as the sheet/video.
            val overlayItem = currentItem
            if (items.isNotEmpty() && overlayItem != null && commentsOpenFraction < 1f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 1f - commentsOpenFraction },
                ) {
                    ShortsOverlay(
                        item = overlayItem,
                        liked = overlayItem.videoId in likedIds.value,
                        saved = overlayItem.videoId in savedIds,
                        onLike = {
                            val nowLiked = overlayItem.videoId !in likedIds.value
                            likedIds.value = if (nowLiked) likedIds.value + overlayItem.videoId else likedIds.value - overlayItem.videoId
                            viewModel.onLikeToggled(overlayItem.videoId, nowLiked)
                        },
                        onSave = { viewModel.toggleSave(overlayItem) },
                        onComments = { openCommentsSheet(overlayItem) },
                        onChannelClick = { onChannelClick(overlayItem.channelId) },
                        onShare = {
                            val intent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://www.youtube.com/shorts/${overlayItem.videoId}",
                                    )
                                }
                            context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                        },
                        navigationBottom = navigationBottom,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
        }
        }

        // YouTube-Shorts style: comments sheet occupies bottom ~65% of the area ABOVE the
        // fixed navigation bar while video continues playing in the top 35%. The sheet opens
        // from behind/below the navigation bar (its resting bottom edge sits on the bar's top),
        // and the bar never hides. Same progress value drives both, so opening AND closing
        // (drag-down / back) move video + sheet together frame-by-frame.
        // No sort-category chips and no cross button here — drag down or press back to close.
        if (showComments && commentsState.videoId.isNotBlank()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = navigationBottom),
            ) {
                FlowCommentsBottomSheet(
                    comments = commentsState.comments,
                    isLoading = commentsState.isLoading,
                    onDismiss = { closeCommentsSheet() },
                    onLoadReplies = { viewModel.loadCommentReplies(it) },
                    onLoadMoreReplies = { viewModel.loadMoreCommentReplies(it) },
                    isLoadingMore = commentsState.isLoadingMore,
                    onLoadMore = viewModel::loadMoreComments,
                    hasMore = commentsState.hasMore,
                    totalText = commentsState.totalText,
                    onAuthorClick = { channelId -> onChannelClick(channelId) },
                    onAvatarClick = { channelId -> onChannelClick(channelId) },
                    expandedHeight = commentsSheetHeight,
                    dismissOnOutsideTap = false,
                    showSortChips = false,
                    showCloseButton = false,
                    externalProgress = commentsProgress,
                    animationSpec = commentsAnimationSpec,
                )
            }
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
            // Icon only, like share — no "Save" text under the bookmark.
            ShortsRailButton(
                label = null,
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
