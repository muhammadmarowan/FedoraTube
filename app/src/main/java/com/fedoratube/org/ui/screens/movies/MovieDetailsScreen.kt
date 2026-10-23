package com.fedoratube.org.ui.screens.movies

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.fedoratube.org.data.movies.MovieDetails
import com.fedoratube.org.data.movies.MovieEpisode
import com.fedoratube.org.data.movies.MovieRepository
import com.fedoratube.org.data.movies.MovieStream
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsScreen(
    type: String,
    id: String,
    onPlayStream: (streamUrl: String, title: String, subtitle: String?, poster: String?, headers: Map<String, String>?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val repository = remember(context) { MovieRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var details by remember { mutableStateOf<MovieDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(true) }

    var selectedSeason by remember { mutableStateOf(1) }
    var selectedEpisode by remember { mutableStateOf<MovieEpisode?>(null) }

    var streams by remember { mutableStateOf<List<MovieStream>>(emptyList()) }
    var isLoadingStreams by remember { mutableStateOf(false) }

    fun loadStreamsForId(targetId: String) {
        isLoadingStreams = true
        scope.launch {
            streams = repository.getStreams(type, targetId)
            isLoadingStreams = false
        }
    }

    LaunchedEffect(type, id) {
        isLoadingDetails = true
        val meta = repository.getMovieDetails(type, id)
        details = meta
        isLoadingDetails = false

        if (meta != null) {
            if (meta.type == "series" && meta.videos.isNotEmpty()) {
                val s1Episodes = meta.videos.filter { (it.season ?: 1) == 1 }
                val initialEp = s1Episodes.firstOrNull() ?: meta.videos.first()
                selectedSeason = initialEp.season ?: 1
                selectedEpisode = initialEp
                loadStreamsForId(initialEp.id)
            } else {
                loadStreamsForId(meta.id)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            when {
                isLoadingDetails -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                details == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Could not load title details",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
                else -> {
                    val meta = details!!
                    val availableSeasons = remember(meta.videos) {
                        meta.videos.mapNotNull { it.season }.distinct().sorted()
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                    ) {
                        // Header with Backdrop & Gradient
                        item(key = "header") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                            ) {
                                val backdropUrl = meta.background ?: meta.poster
                                if (!backdropUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = backdropUrl,
                                        contentDescription = meta.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Black.copy(alpha = 0.45f),
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                                    MaterialTheme.colorScheme.background,
                                                ),
                                            ),
                                        ),
                                )

                                // Back button
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .statusBarsPadding()
                                        .padding(8.dp)
                                        .align(Alignment.TopStart),
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }

                                // Header info overlay at bottom
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    // Small Poster
                                    if (!meta.poster.isNullOrBlank()) {
                                        AsyncImage(
                                            model = meta.poster,
                                            contentDescription = meta.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .width(80.dp)
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = meta.name,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            meta.imdbRating?.let { rating ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF5C518),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Star,
                                                            contentDescription = null,
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(11.dp),
                                                        )
                                                        Text(
                                                            text = rating,
                                                            color = Color.Black,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                            }

                                            meta.releaseInfo?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            meta.runtime?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        if (meta.genres.isNotEmpty()) {
                                            Text(
                                                text = meta.genres.joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Synopsis / Overview
                        meta.description?.takeIf { it.isNotBlank() }?.let { overview ->
                            item(key = "overview") {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Overview",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = overview,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // If Series: Season Picker and Episode List
                        if (meta.type == "series" && meta.videos.isNotEmpty()) {
                            item(key = "seasons_title") {
                                Text(
                                    text = "Episodes",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                                )
                            }

                            if (availableSeasons.size > 1) {
                                item(key = "seasons_tabs") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    ) {
                                        items(availableSeasons) { seasonNum ->
                                            FilterChip(
                                                selected = selectedSeason == seasonNum,
                                                onClick = {
                                                    selectedSeason = seasonNum
                                                    val ep = meta.videos.firstOrNull { it.season == seasonNum }
                                                    if (ep != null) {
                                                        selectedEpisode = ep
                                                        loadStreamsForId(ep.id)
                                                    }
                                                },
                                                label = { Text("Season $seasonNum") },
                                                shape = RoundedCornerShape(20.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // Episode Cards for selected season
                            val currentSeasonEpisodes = meta.videos.filter { (it.season ?: 1) == selectedSeason }
                            items(currentSeasonEpisodes, key = { "ep_${it.id}" }) { episode ->
                                val isSelected = selectedEpisode?.id == episode.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clickable {
                                            selectedEpisode = episode
                                            loadStreamsForId(episode.id)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        },
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Episode thumbnail
                                        Box(
                                            modifier = Modifier
                                                .width(96.dp)
                                                .height(56.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!episode.thumbnail.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = episode.thumbnail,
                                                    contentDescription = episode.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            } else {
                                                Text(
                                                    text = "E${episode.episode ?: ""}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Text(
                                                text = "${episode.episode ?: ""}. ${episode.title}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onBackground,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            episode.overview?.takeIf { it.isNotBlank() }?.let { overview ->
                                                Text(
                                                    text = overview,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Streams Section
                        item(key = "streams_header") {
                            Text(
                                text = "Available Streams",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                            )
                        }

                        when {
                            isLoadingStreams -> {
                                item(key = "streams_loading") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            streams.isEmpty() -> {
                                item(key = "streams_empty") {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                text = "No Streams Found",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "Add or configure stream addons (e.g. Torrentio or Superflix) in Settings -> Addons & Plugins.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                items(streams, key = { "${it.addonName}_${it.playbackUrl ?: it.infoHash}_${it.displayTitle}" }) { stream ->
                                    StreamCardItem(
                                        stream = stream,
                                        movieTitle = meta.name,
                                        episodeTitle = selectedEpisode?.title,
                                        posterUrl = meta.poster,
                                        onPlayStream = { url, headers ->
                                            val fullTitle = if (selectedEpisode != null) {
                                                "${meta.name} - S${selectedEpisode?.season ?: 1}E${selectedEpisode?.episode ?: 1} ${selectedEpisode?.title}"
                                            } else {
                                                meta.name
                                            }
                                            onPlayStream(url, fullTitle, stream.displayTitle, meta.poster, headers)
                                        },
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

@Composable
private fun StreamCardItem(
    stream: MovieStream,
    movieTitle: String,
    episodeTitle: String?,
    posterUrl: String?,
    onPlayStream: (url: String, headers: Map<String, String>?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val quality = stream.displayQuality
    val badgeColor = when {
        quality.contains("4K") || quality.contains("2160") -> Color(0xFFE53935)
        quality.contains("1080") -> Color(0xFF1E88E5)
        quality.contains("720") -> Color(0xFF43A047)
        else -> MaterialTheme.colorScheme.secondary
    }

    val magnetOrUrl = if (stream.isTorrent) stream.getMagnetUri(movieTitle) else stream.playbackUrl

    fun handlePlayOrLaunch() {
        if (stream.isDirectPlayable && !stream.playbackUrl.isNullOrBlank()) {
            onPlayStream(stream.playbackUrl!!, stream.proxyHeaders)
        } else {
            val magnet = stream.getMagnetUri(movieTitle) ?: stream.playbackUrl
            if (!magnet.isNullOrBlank()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open Torrent With"))
                } catch (e: Exception) {
                    Toast.makeText(context, "No torrent app found. Please install Flud, LibreTorrent or TorrServe.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "No stream URL found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { handlePlayOrLaunch() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Quality Badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor,
            ) {
                Text(
                    text = quality,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }

            // Direct vs Torrent Badge
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

            // Stream Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stream.displayTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stream.addonName.ifBlank { "Addon Stream" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // External open button
            if (!magnetOrUrl.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        try {
                            val uri = Uri.parse(magnetOrUrl)
                            val intent = if (stream.isTorrent) {
                                Intent(Intent.ACTION_VIEW, uri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            } else {
                                Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "video/*")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            }
                            context.startActivity(Intent.createChooser(intent, if (stream.isTorrent) "Open Magnet With" else "Play stream with"))
                        } catch (e: Exception) {
                            Toast.makeText(context, if (stream.isTorrent) "No torrent app found (install Flud / LibreTorrent)" else "Could not open external player", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = "Open externally",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Copy URL button
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Stream URL", magnetOrUrl)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, if (stream.isTorrent) "Magnet link copied" else "Stream link copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy stream link",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Play button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(38.dp)
                    .clickable { handlePlayOrLaunch() },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

