package com.fedoratube.org.ui.screens.movies

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.fedoratube.org.data.movies.MovieDetails
import com.fedoratube.org.data.movies.MovieEpisode
import com.fedoratube.org.data.movies.MovieRepository
import com.fedoratube.org.data.movies.MovieStream
import com.fedoratube.org.data.movies.TorrentEngineManager
import com.fedoratube.org.data.movies.TorrentPlaybackStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@kotlin.OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoviePlayerScreen(
    initialStreamUrl: String,
    initialIsTorrent: Boolean,
    initialMagnetUri: String?,
    initialFileIdx: Int?,
    title: String,
    subtitle: String? = null,
    poster: String? = null,
    headers: Map<String, String>? = null,
    availableStreams: List<MovieStream> = emptyList(),
    episodes: List<MovieEpisode> = emptyList(),
    currentEpisode: MovieEpisode? = null,
    movieDetails: MovieDetails? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var activeStreamUrl by remember { mutableStateOf(initialStreamUrl) }
    var activeIsTorrent by remember { mutableStateOf(initialIsTorrent) }
    var activeMagnetUri by remember { mutableStateOf(initialMagnetUri) }
    var activeFileIdx by remember { mutableStateOf(initialFileIdx) }
    var activeTitle by remember { mutableStateOf(title) }
    var activeSubtitle by remember { mutableStateOf(subtitle) }
    var currentStreamList by remember { mutableStateOf(availableStreams) }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    var torrentLoading by remember { mutableStateOf(false) }
    var torrentStats by remember { mutableStateOf(TorrentPlaybackStats()) }

    var showSourceSheet by remember { mutableStateOf(false) }
    var showEpisodesSheet by remember { mutableStateOf(false) }
    var showTrackSheet by remember { mutableStateOf(false) }
    var tracksSnapshot by remember { mutableStateOf<Tracks?>(null) }

    // Resize modes: FIT, ZOOM, FILL, 16:9
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var resizeModeLabel by remember { mutableStateOf("Fit") }

    // Gestures state
    var gestureOverlayType by remember { mutableStateOf<String?>(null) } // "brightness", "volume", "seek_fwd", "seek_rew"
    var gestureLevel by remember { mutableFloatStateOf(0.5f) }

    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .apply {
                if (!headers.isNullOrEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = true
            }
    }

    // Function to play URL in ExoPlayer
    fun playUrlInPlayer(url: String) {
        hasError = false
        isBuffering = true
        val uri = Uri.parse(url)
        val isHls = url.contains(".m3u8", ignoreCase = true)
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .apply {
                if (isHls) setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    // Torrent stream startup
    LaunchedEffect(activeIsTorrent, activeMagnetUri, activeFileIdx) {
        if (activeIsTorrent && !activeMagnetUri.isNullOrBlank()) {
            torrentLoading = true
            isBuffering = true
            val result = TorrentEngineManager.startTorrentStream(context, activeMagnetUri!!, activeFileIdx)
            result.fold(
                onSuccess = { res ->
                    torrentLoading = false
                    activeStreamUrl = res.playbackUrl
                    playUrlInPlayer(res.playbackUrl)
                },
                onFailure = { err ->
                    torrentLoading = false
                    hasError = true
                    errorMessage = "Torrent stream initialization failed: ${err.message}"
                }
            )
        } else if (!activeIsTorrent && activeStreamUrl.isNotBlank()) {
            playUrlInPlayer(activeStreamUrl)
        }
    }

    // Torrent stats polling
    LaunchedEffect(activeIsTorrent) {
        if (!activeIsTorrent) return@LaunchedEffect
        while (true) {
            delay(1000)
            TorrentEngineManager.updateStats()
            torrentStats = TorrentEngineManager.stats.value
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    hasError = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: Tracks) {
                tracksSnapshot = tracks
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                hasError = true
                isBuffering = false
                errorMessage = error.localizedMessage ?: "Playback error"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            if (activeIsTorrent) {
                scope.launch {
                    TorrentEngineManager.stopStream()
                }
            }
        }
    }

    // Update progress loop
    LaunchedEffect(exoPlayer, isPlaying) {
        while (true) {
            if (exoPlayer.isPlaying && !isSeeking) {
                currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Auto-hide controls
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(4000)
            controlsVisible = false
        }
    }

    // Auto-hide gesture feedback overlay
    LaunchedEffect(gestureOverlayType) {
        if (gestureOverlayType != null) {
            delay(1200)
            gestureOverlayType = null
        }
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x > (size.width / 2f)
                        if (isRightSide) {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            gestureOverlayType = "seek_fwd"
                        } else {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                            gestureOverlayType = "seek_rew"
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val isLeft = offset.x < (size.width / 2f)
                        if (isLeft) {
                            gestureOverlayType = "brightness"
                            val lp = activity?.window?.attributes
                            gestureLevel = lp?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                        } else {
                            gestureOverlayType = "volume"
                            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                            gestureLevel = (cur / max).coerceIn(0f, 1f)
                        }
                    },
                    onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                        change.consume()
                        val delta = -dragAmount / 500f
                        if (gestureOverlayType == "brightness") {
                            gestureLevel = (gestureLevel + delta).coerceIn(0.01f, 1.0f)
                            activity?.let { act ->
                                val lp = act.window.attributes
                                lp.screenBrightness = gestureLevel
                                act.window.attributes = lp
                            }
                        } else if (gestureOverlayType == "volume") {
                            gestureLevel = (gestureLevel + delta).coerceIn(0f, 1f)
                            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val targetVol = (gestureLevel * max).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                        }
                    },
                    onDragEnd = {
                        gestureOverlayType = null
                    },
                )
            },
    ) {
        // Video View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { playerView ->
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Gesture Overlay: Brightness / Volume vertical indicators
        AnimatedVisibility(
            visible = gestureOverlayType == "brightness" || gestureOverlayType == "volume",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val isBrightness = gestureOverlayType == "brightness"
                Column(
                    modifier = Modifier
                        .align(if (isBrightness) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 28.dp)
                        .width(42.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isBrightness) Icons.Filled.BrightnessMedium else Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "${(gestureLevel * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(gestureLevel)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }

        // Gesture Overlay: Seek Forward / Rewind indicator
        AnimatedVisibility(
            visible = gestureOverlayType == "seek_fwd" || gestureOverlayType == "seek_rew",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(72.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (gestureOverlayType == "seek_fwd") Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                        Text(
                            text = if (gestureOverlayType == "seek_fwd") "+10s" else "-10s",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Buffering / Torrent Loading
        if ((isBuffering || torrentLoading) && !hasError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.5.dp,
                )
                if (torrentLoading || activeIsTorrent) {
                    Text(
                        text = if (torrentStats.connectedPeers > 0) "Buffering Torrent..." else "Connecting to peers...",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (activeIsTorrent && (torrentStats.connectedSeeds > 0 || torrentStats.connectedPeers > 0)) {
                        Text(
                            text = "⚡ ${torrentStats.downloadSpeedFormatted}  •  👥 ${torrentStats.connectedSeeds} seeds (${torrentStats.connectedPeers} peers)",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Error message overlay
        if (hasError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Stream Playback Error",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = errorMessage ?: "Unable to stream media from this source.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        hasError = false
                        isBuffering = true
                        if (activeIsTorrent && !activeMagnetUri.isNullOrBlank()) {
                            scope.launch {
                                TorrentEngineManager.startTorrentStream(context, activeMagnetUri!!, activeFileIdx)
                            }
                        } else {
                            exoPlayer.prepare()
                            exoPlayer.play()
                        }
                    }) {
                        Text("Retry")
                    }
                    if (currentStreamList.isNotEmpty()) {
                        FilledTonalButton(onClick = { showSourceSheet = true }) {
                            Text("Switch Source")
                        }
                    }
                    OutlinedButton(onClick = {
                        val launchUrl = activeMagnetUri ?: activeStreamUrl
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val u = Uri.parse(launchUrl)
                                if (activeIsTorrent) {
                                    data = u
                                } else {
                                    setDataAndType(u, "video/*")
                                }
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, "Play stream with"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open external player", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Open External")
                    }
                }
            }
        }

        // Nuvio-style Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = activeTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        activeSubtitle?.let { sub ->
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Aspect ratio switcher
                    IconButton(onClick = {
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                                resizeModeLabel = "Zoom"
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                                resizeModeLabel = "Fill"
                                AspectRatioFrameLayout.RESIZE_MODE_FILL
                            }
                            else -> {
                                resizeModeLabel = "Fit"
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }
                        Toast.makeText(context, "Aspect: $resizeModeLabel", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Audio & Subtitles
                    IconButton(onClick = { showTrackSheet = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Subtitles,
                            contentDescription = "Subtitles & Audio",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Open external
                    IconButton(onClick = {
                        val launchUrl = activeMagnetUri ?: activeStreamUrl
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val u = Uri.parse(launchUrl)
                                if (activeIsTorrent) data = u else setDataAndType(u, "video/*")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(intent, "Play stream with"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open external player", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "External Player",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                // Center Play / Rewind / FastForward
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                            currentPosition = newPos
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }

                // Bottom Bar Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // Torrent P2P Stats HUD pill
                    if (activeIsTorrent) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "⚡ ${torrentStats.downloadSpeedFormatted}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "👥 ${torrentStats.connectedSeeds} seeds  (${torrentStats.connectedPeers} peers)",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }

                    // Seek Slider + Timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = formatTime(if (isSeeking) (seekPosition * duration).toLong() else currentPosition),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        Slider(
                            value = if (duration > 0) {
                                if (isSeeking) seekPosition else (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { frac ->
                                isSeeking = true
                                seekPosition = frac
                            },
                            onValueChangeFinished = {
                                isSeeking = false
                                val target = (seekPosition * duration).toLong()
                                exoPlayer.seekTo(target)
                                currentPosition = target
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                        )

                        Text(
                            text = formatTime(duration),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Bottom action shortcuts (Source Switcher, Episodes switcher)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Switch Stream Source button
                        if (currentStreamList.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showSourceSheet = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Sources (${currentStreamList.size})", fontSize = 12.sp)
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        // Episodes Switcher button (for series)
                        if (episodes.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showEpisodesSheet = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Episodes (${episodes.size})", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Source Switcher Modal Sheet
    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Select Stream Source",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(currentStreamList) { stream ->
                        val isCurrent = (activeIsTorrent && stream.isTorrent && stream.infoHash == activeMagnetUri) ||
                            (!activeIsTorrent && stream.playbackUrl == activeStreamUrl)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSourceSheet = false
                                    activeTitle = title
                                    activeSubtitle = stream.displayTitle
                                    if (stream.isTorrent) {
                                        activeIsTorrent = true
                                        activeMagnetUri = stream.getMagnetUri(title)
                                        activeFileIdx = stream.fileIdx
                                    } else {
                                        activeIsTorrent = false
                                        activeStreamUrl = stream.playbackUrl ?: ""
                                        playUrlInPlayer(activeStreamUrl)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (stream.isDirectPlayable) Color(0xFF2E7D32) else Color(0xFFE65100),
                                ) {
                                    Text(
                                        text = if (stream.isDirectPlayable) "DIRECT" else "TORRENT",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    )
                                }
                                Text(
                                    text = stream.displayQuality,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stream.displayTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = stream.addonName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Episodes Switcher Modal Sheet (for series)
    if (showEpisodesSheet && movieDetails != null) {
        ModalBottomSheet(
            onDismissRequest = { showEpisodesSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Episodes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(episodes, key = { "ep_sheet_${it.id}" }) { ep ->
                        val isCurrent = currentEpisode?.id == ep.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showEpisodesSheet = false
                                    activeTitle = "${movieDetails.name} - S${ep.season ?: 1}E${ep.episode ?: 1} ${ep.title}"
                                    scope.launch {
                                        val repo = MovieRepository.getInstance(context)
                                        val newStreams = repo.getStreams("series", ep.id)
                                        currentStreamList = newStreams
                                        val chosen = newStreams.firstOrNull()
                                        if (chosen != null) {
                                            activeSubtitle = chosen.displayTitle
                                            if (chosen.isTorrent) {
                                                activeIsTorrent = true
                                                activeMagnetUri = chosen.getMagnetUri(activeTitle)
                                                activeFileIdx = chosen.fileIdx
                                            } else {
                                                activeIsTorrent = false
                                                activeStreamUrl = chosen.playbackUrl ?: ""
                                                playUrlInPlayer(activeStreamUrl)
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "E${ep.episode ?: ""}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ep.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    ep.overview?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Audio & Subtitle Tracks Sheet
    if (showTrackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTrackSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Audio & Subtitles",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )

                val audioTracks = remember(tracksSnapshot) {
                    tracksSnapshot?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList()
                }
                val textTracks = remember(tracksSnapshot) {
                    tracksSnapshot?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList()
                }

                Text(
                    text = "Audio Track",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (audioTracks.isEmpty()) {
                    Text("Default Audio", style = MaterialTheme.typography.bodyMedium)
                } else {
                    audioTracks.forEach { group ->
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                                            .build()
                                        showTrackSheet = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = format.label ?: format.language ?: "Audio ${i + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Subtitles",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                // Option to disable subtitles
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                .build()
                            showTrackSheet = false
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Off", style = MaterialTheme.typography.bodyMedium)
                }

                if (textTracks.isNotEmpty()) {
                    textTracks.forEach { group ->
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            val isSelected = group.isTrackSelected(i)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, i))
                                            .build()
                                        showTrackSheet = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = format.label ?: format.language ?: "Subtitle ${i + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (isSelected) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
