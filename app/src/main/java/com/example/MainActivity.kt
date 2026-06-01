package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.TvViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_layout"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    TvConsoleDashboard(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(Color(0xFF0F1115)) // "Sophisticated Dark" background
                    )
                }
            }
        }
    }
}

@Composable
fun TvConsoleDashboard(
    modifier: Modifier = Modifier,
    viewModel: TvViewModel = viewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    if (isLandscape) {
        // Landscape Mode: Left Screen Display Simulator, Right Developer SDK panel and virtual Remote.
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TV Screen Column (60% Width)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvOperatingHeader(viewModel)
                TvScreenSimulator(viewModel, modifier = Modifier.weight(1f))
                TvTelemetryBar(viewModel)
            }

            // Developer Tooling & Remote Column (40% Width)
            Row(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Remote Controller Space
                RemoteControllerPanel(viewModel, modifier = Modifier.width(140.dp))
                // Developer Diagnostic Suite
                DeveloperDiagnosticSuite(viewModel, modifier = Modifier.weight(1f))
            }
        }
    } else {
        // Portrait Mode: Top-to-Bottom scrolling layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvOperatingHeader(viewModel)
            
            // Fixed height TV screen mock for vertical viewports to fit everything elegantly
            TvScreenSimulator(
                viewModel, 
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
            
            TvTelemetryBar(viewModel)
            
            // Parallel components stack
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RemoteControllerPanel(viewModel, modifier = Modifier.width(130.dp))
                DeveloperDiagnosticSuite(viewModel, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ------------------------------------------------------------------------
// HEADER MODULE: System status information bar
// ------------------------------------------------------------------------
@Composable
fun TvOperatingHeader(viewModel: TvViewModel) {
    val selectedProf by viewModel.selectedProfile.collectAsState()
    val syncText by viewModel.syncStatusText.collectAsState()
    val isSynced by viewModel.syncedOnMobileDevice.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1D23), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34D399)) // System active emerald color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AetherOS v2.4.0",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF2D2F36), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "CORE LAYER ACTIVE",
                    color = Color(0xFF818CF8), // Sophisticated light indigo text
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Handoff indication
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        if (isSynced) Color(0x3010B981) else Color(0xFF2D2F36),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isSynced) Color(0xFF10B981) else Color(0x0DFFFFFF),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .clickable { viewModel.toggleDeviceSync() }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Device Handoff",
                    tint = if (isSynced) Color(0xFF34D399) else Color(0xFF94A3B8),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = syncText,
                    color = if (isSynced) Color(0xFF34D399) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Simple Profile Selector Header
            selectedProf?.let { prof ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF2D2F36), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(prof.avatarColorHex)))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = prof.profileName.substringBefore(" "),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// TV SCREEN SIMULATOR FRAME: Renders the active layout of the television OS.
// Rules apply (Home screen grid vs custom video screen with buffering overlays)
// ------------------------------------------------------------------------
@Composable
fun TvScreenSimulator(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    val activePkg by viewModel.activeAppPackage.collectAsState()
    val activeMed by viewModel.activeMedia.collectAsState()
    val pbState by viewModel.playbackStatus.collectAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(4.dp, Color(0xFF1B1D23), RoundedCornerShape(24.dp)) // Smart TV Bezel Rim in Sophisticated Dark
            .testTag("tv_screen_simulator"),
        contentAlignment = Alignment.Center
    ) {
        when {
            activePkg != null -> {
                // An Installed App has captured TV display focus
                SimulatedAppViewport(packageName = activePkg!!, viewModel = viewModel)
            }
            activeMed != null -> {
                // Media catalog is currently rendering active feed
                SimulatedMediaViewport(mediaItem = activeMed!!, playbackState = pbState, viewModel = viewModel)
            }
            else -> {
                // Smart TV Launcher Home Screen Core UI
                SimulatedTVLauncherHome(viewModel = viewModel)
            }
        }

        // Static overlay demonstrating the physical TV frame brand label of the API layer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .background(Color(0xE0111827), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 1.dp)
        ) {
            Text(
                text = "AETHER DYNAMIC MODEL",
                color = Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ------------------------------------------------------------------------
// THE TV LAUNCHER: Beautiful custom operating system Home Screen Grid
// ------------------------------------------------------------------------
@Composable
fun SimulatedTVLauncherHome(viewModel: TvViewModel) {
    val appsList by viewModel.apps.collectAsState()
    val mediaCatalog by viewModel.mediaCatalog.collectAsState()
    val profile by viewModel.selectedProfile.collectAsState()

    // Filter items based on Recommendation Algorithm (genre matching, parental limits)
    val recommendedItems = remember(mediaCatalog, profile) {
        val prefGenre = profile?.preferredGenre ?: "Sci-Fi"
        val isKids = profile?.parentalControlsEnabled ?: false
        
        mediaCatalog.filter {
            if (isKids) it.rating != "R" else true
        }.sortedByDescending {
            val score = (if (it.genre == prefGenre) 5 else 0) + (if (it.category == "Live Channel") 2 else 0)
            score
        }
    }

    val channelsAndStreams = remember(mediaCatalog) {
        mediaCatalog.filter { it.category == "Live Channel" }
    }

    val onDemandMovies = remember(mediaCatalog, profile) {
        val isKids = profile?.parentalControlsEnabled ?: false
        mediaCatalog.filter { (it.category == "Movie" || it.category == "TV Show") && (!isKids || it.rating != "R") }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1B1D23), Color(0xFF0F1115)),
                    radius = 900f
                )
            )
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Welcome Header Greeting
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Aether Home Hub",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Unified OS Application Core & Metadata Layer",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34D399))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TV HDMI-1 READY",
                    color = Color(0xFF34D399),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Recommendation Highlighting Banner (Dynamic recommended model item)
        recommendedItems.firstOrNull()?.let { topRecommendation ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1B1D23))
                    .border(1.dp, Color(0x19FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { viewModel.playMedia(topRecommendation) }
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4F46E5), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "AI SELECTION",
                                    color = Color.White,
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Based on profile settings",
                                color = Color(0xFF818CF8),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topRecommendation.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = topRecommendation.description,
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4F46E5))
                            .clickable { viewModel.playMedia(topRecommendation) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "PLAY", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Launcher Segment: Custom Registered Applications sandbox
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "INSTALLED OPERATING SYSTEM APPLICATIONS",
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                 items(appsList) { app ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1D23)),
                        modifier = Modifier
                            .width(82.dp)
                            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                            .clickable { viewModel.launchAppPkg(app) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Draw an abstract App Icon with primary colors
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = when (app.packageName) {
                                                "com.netflix.tv" -> listOf(Color(0xFFE50914), Color(0xFF7A0006))
                                                "com.youtube.tv" -> listOf(Color(0xFFFF0000), Color(0xFF990000))
                                                "com.spotify.tv" -> listOf(Color(0xFF1DB954), Color(0xFF0F5E2B))
                                                "com.twitch.tv" -> listOf(Color(0xFF9146FF), Color(0xFF4C1D95))
                                                else -> listOf(Color(0xFF06B6D4), Color(0xFF0891B2))
                                            }
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (app.packageName) {
                                        "com.spotify.tv" -> Icons.Default.Refresh
                                        "com.youtube.tv" -> Icons.Default.PlayArrow
                                        else -> Icons.Default.Settings
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = app.appName,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "v" + app.version,
                                color = Color.Gray,
                                fontSize = 6.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Channels Module
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "LIVE TV CHANNELS (TUNER INTEGRATION)",
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(channelsAndStreams) { chan ->
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1D23))
                            .border(1.dp, Color(0xFF34D399).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.playMedia(chan) }
                            .padding(6.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF34D399), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("LIVE", color = Color.Black, fontSize = 6.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text("FM: " + chan.rating, color = Color.Gray, fontSize = 7.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chan.title,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = chan.genre,
                                color = Color(0xFF34D399),
                                fontSize = 7.sp
                            )
                        }
                    }
                }
            }
        }

        // Movies & TV Shows On-Demand
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ON-DEMAND STREAMING CATALOG (DRM TOKENS COMPLIANT)",
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(onDemandMovies) { item ->
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B1D23))
                            .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.playMedia(item) }
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = item.category + " | " + item.genre,
                                color = Color(0xFF818CF8),
                                fontSize = 7.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Rating: " + item.rating,
                                color = Color.Gray,
                                fontSize = 7.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// THE TV MEDIA SCREEN VIEWPORT: Renders active video streams with custom state
// ------------------------------------------------------------------------
@Composable
fun SimulatedMediaViewport(
    mediaItem: TvMediaItem,
    playbackState: String,
    viewModel: TvViewModel
) {
    val timelineSecs by viewModel.playbackPosition.collectAsState()
    val isMute by viewModel.isMuted.collectAsState()
    val volumeLevel by viewModel.volume.collectAsState()
    val subtitlesOn by viewModel.subtitlesEnabled.collectAsState()
    val rawBufPercent by viewModel.bufferPercent.collectAsState()
    val currentTrk by viewModel.audioTrack.collectAsState()

    // Interactive custom color gradients for ambient movie backgrounds
    val streamBgGradient = remember(mediaItem.id) {
        Brush.verticalGradient(
            colors = when (mediaItem.id) {
                "movie_1" -> listOf(Color(0xFF1F2937), Color(0xFF0F172A), Color(0xFF030712))
                "show_1" -> listOf(Color(0xFF1E1B4B), Color(0xFF0F172A), Color(0xFF020617))
                "live_1" -> listOf(Color(0xFF064E3B), Color(0xFF111827), Color(0xFF022C22))
                else -> listOf(Color(0xFF4C1D95), Color(0xFF111827), Color(0xFF0F172A))
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(streamBgGradient)
            .padding(12.dp)
    ) {
        // Top Ticker / Metadata Details
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AETHER VIDEO CONTAINER RX-MODE",
                    color = Color(0xFF38BDF8),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = mediaItem.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .background(Color(0xBA1F2937), RoundedCornerShape(4.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "RATING: ${mediaItem.rating}", color = Color.White, fontSize = 7.sp)
            }
        }

        // Center rendering feed based on pause/playing/buffering state
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (playbackState) {
                "BUFFERING" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Buffering Stream ($rawBufPercent%)...",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                "VIDEO_PAUSED" -> {
                    Row(
                        modifier = Modifier.size(28.dp).padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color.White.copy(alpha = 0.7f)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color.White.copy(alpha = 0.7f)))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PAUSED",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                "VIDEO_PLAYING" -> {
                    // Simulating a moving cinematic visual canvas
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseFactor by infiniteTransition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ), label = "pAnim"
                    )

                    Canvas(
                        modifier = Modifier
                            .size(110.dp, 40.dp)
                            .testTag("cinematic_canvas")
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val pointsCount = 12
                        val step = canvasWidth / (pointsCount - 1)
                        for (i in 0 until pointsCount) {
                            val h = (bounceHeight(i, timelineSecs) * canvasHeight * 0.75f * pulseFactor).coerceIn(4f, canvasHeight)
                            drawRect(
                                color = if (isMute) Color(0xFF94A3B8) else Color(0xFF06B6D4),
                                size = androidx.compose.ui.geometry.Size(12f, h),
                                topLeft = androidx.compose.ui.geometry.Offset(i * step, (canvasHeight - h) / 2)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DECRYPTING LIVE DECODER FEED",
                        color = Color(0xFF10B981),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Subtitles Overlays Engine
        if (subtitlesOn && playbackState == "VIDEO_PLAYING") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = getSimulatedSubtitleLine(timelineSecs, mediaItem.title),
                    color = Color.Yellow,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Bottom Ticker Progress Bar & System control parameters
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Audio track indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Track: $currentTrk",
                    color = Color(0xFF94A3B8),
                    fontSize = 7.sp
                )
                if (isMute) {
                    Text("MUTED", color = Color(0xFFEF4444), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("VOL: $volumeLevel%", color = Color(0xFFE2E8F0), fontSize = 7.sp)
                }
            }

            // Seek Timeline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formatTimelineText(timelineSecs),
                    color = Color.White,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Seek bar
                val currentPercent = if (mediaItem.durationSeconds > 0) {
                    timelineSecs.toFloat() / mediaItem.durationSeconds.toFloat()
                } else {
                    1f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(currentPercent)
                            .background(Color(0xFF818CF8))
                    )
                }

                Text(
                    text = if (mediaItem.durationSeconds > 0) formatTimelineText(mediaItem.durationSeconds) else "LIVE TUNER",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ------------------------------------------------------------------------
// COMPONENTIAL VIEWS: APP SECTORS FOR CUSTOM APP RECONSTRUCTIONS
// ------------------------------------------------------------------------
@Composable
fun SimulatedAppViewport(
    packageName: String,
    viewModel: TvViewModel
) {
    val appBrush = remember(packageName) {
        Brush.verticalGradient(
            colors = when (packageName) {
                "com.netflix.tv" -> listOf(Color(0xFF1F0C10), Color(0xFF111827))
                "com.youtube.tv" -> listOf(Color(0xFF3B0707), Color(0xFF030712))
                else -> listOf(Color(0xFF022C22), Color(0xFF020617))
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBrush)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "App Launched",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SANDBOX CONTAINER CONTAINER",
                color = Color(0xFF38BDF8),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Active Application Package: $packageName",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Simulating dynamic rendering bridge... Click remote BACK / HOME to return to general OS shell.",
                color = Color.Gray,
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { viewModel.sendRemoteInput("HOME") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Text("CLOSE APPLICATION", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------
// THE TELEMETRY BAR: Hardware state inspector
// ------------------------------------------------------------------------
@Composable
fun TvTelemetryBar(viewModel: TvViewModel) {
    val cpuT by viewModel.cpuTemp.collectAsState()
    val ramU by viewModel.ramUsedPercent.collectAsState()
    val netQ by viewModel.networkQuality.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1D23), RoundedCornerShape(24.dp))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Temperature Info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Temperature Sensor",
                tint = if (cpuT > 60.0) Color(0xFFEF4444) else Color(0xFF818CF8),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "CPU: ${String.format("%.1f", cpuT)}°C",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // RAM Load Info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "RAM Usage",
                tint = Color(0xFFA855F7),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "RAM: ${String.format("%.1f", ramU)}%",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Network throughput
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Network Link Quality",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = netQ.substringAfter(": "),
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ------------------------------------------------------------------------
// VIRTUAL REMOTE CONTROLLER PANEL: Provides physical key simulators in OS
// ------------------------------------------------------------------------
@Composable
fun RemoteControllerPanel(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1B1D23))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Remote Brand Tag
        Text(
            text = "E-REMOTE RX",
            color = Color(0xFF6B7280),
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )

        // Row 1: Power & Mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { viewModel.sendRemoteInput("POWER") },
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Power", tint = Color.White, modifier = Modifier.size(12.dp))
            }

            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFF2D2F36), CircleShape)
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Mute Toggle", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        // D-Pad Circular Box with Directional buttons
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(Color(0xFF0F1115), CircleShape)
                .border(1.dp, Color(0x19FFFFFF), CircleShape)
        ) {
            // Directional Buttons nested inside
            IconButton(
                onClick = { viewModel.sendRemoteInput("NAVIGATE", "UP") },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(26.dp)
                    .testTag("remote_up")
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "UP", tint = Color.White)
            }

            IconButton(
                onClick = { viewModel.sendRemoteInput("NAVIGATE", "DOWN") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(26.dp)
                    .testTag("remote_down")
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "DOWN", tint = Color.White)
            }

            IconButton(
                onClick = { viewModel.sendRemoteInput("NAVIGATE", "LEFT") },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(26.dp)
                    .testTag("remote_left")
            ) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "LEFT", tint = Color.White)
            }

            IconButton(
                onClick = { viewModel.sendRemoteInput("NAVIGATE", "RIGHT") },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(26.dp)
                    .testTag("remote_right")
            ) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "RIGHT", tint = Color.White)
            }

            // SELECT BUTTON In center
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4F46E5))
                    .clickable { viewModel.sendRemoteInput("SELECT") }
                    .testTag("remote_select"),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "OK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Row 3: Back & Home Keyboard triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(
                onClick = { viewModel.sendRemoteInput("BACK") },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF2D2F36), RoundedCornerShape(12.dp))
                    .testTag("remote_back")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "BACK", tint = Color.White, modifier = Modifier.size(12.dp))
            }

            IconButton(
                onClick = { viewModel.sendRemoteInput("HOME") },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF2D2F36), RoundedCornerShape(12.dp))
                    .testTag("remote_home")
            ) {
                Icon(Icons.Default.Home, contentDescription = "HOME", tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        // Row 4: Channel & Vol controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VOL", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { viewModel.adjustVolume(increase = false) },
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2D2F36), CircleShape)
                    ) {
                        Text("-", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { viewModel.adjustVolume(increase = true) },
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2D2F36), CircleShape)
                    ) {
                        Text("+", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CH", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = { viewModel.sendRemoteInput("CHANNEL_DOWN") },
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2D2F36), CircleShape)
                    ) {
                        Text("‹", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { viewModel.sendRemoteInput("CHANNEL_UP") },
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2D2F36), CircleShape)
                    ) {
                        Text("›", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// DEVELOPER INSPECTION TOOLING SUITE: Exposes system logs, profiles, IoT settings
// ------------------------------------------------------------------------
@Composable
fun DeveloperDiagnosticSuite(
    viewModel: TvViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = API Console, 1 = IoT / Smart Home, 2 = Household, 3 = Metrics
    val tabNames = listOf("API Console", "Smart Home", "Users Hub", "Analytics")

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1B1D23))
            .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
            .padding(8.dp)
    ) {
        // Multi-tab headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F1115), RoundedCornerShape(12.dp))
                .padding(3.dp)
        ) {
            tabNames.forEachIndexed { idx, name ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeTab == idx) Color(0xFF2D2F36) else Color.Transparent)
                        .clickable { activeTab = idx }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (activeTab == idx) Color.White else Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tab Content Router
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> ApiConsoleTab(viewModel)
                1 -> SmartHomeTab(viewModel)
                2 -> UsersHubTab(viewModel)
                3 -> AnalyticsTab(viewModel)
            }
        }
    }
}

// ------------------------------------------------------------------------
// DIAGNOSTIC SUITE TAB 0: Real-time SDK console logs
// ------------------------------------------------------------------------
@Composable
fun ApiConsoleTab(viewModel: TvViewModel) {
    val sdkLogs by viewModel.sdkConsoleLogs.collectAsState()
    val voiceCmd by viewModel.voiceCommandText.collectAsState()
    val voiceFeed by viewModel.voiceFeedbackText.collectAsState()
    var voiceTxt by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Natural Language Voice command simulator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B2030), RoundedCornerShape(6.dp))
                .padding(4.dp)
        ) {
            BasicTextField(
                value = voiceTxt,
                onValueChange = { voiceTxt = it },
                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 9.sp),
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0B0F19), RoundedCornerShape(4.dp))
                    .padding(6.dp),
                decorationBox = { innerTextField ->
                    if (voiceTxt.isEmpty()) {
                        Text("Send Voice Command (e.g. 'Play Aether')...", color = Color.Gray, fontSize = 9.sp)
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = {
                    if (voiceTxt.isNotEmpty()) {
                        viewModel.compileVoiceCommand(voiceTxt)
                        voiceTxt = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("SEND", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Voice Command State Feedback overlay
        if (voiceFeed.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x3038BDF8), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF06B6D4)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SDK Feedback: $voiceFeed",
                        color = Color(0xFF38BDF8),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // SDK Console Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SDK INTERFACING TELEMETRY (WSS/REST LOGS)",
                color = Color.Gray,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "CLEAR LOGS",
                color = Color(0xFFEF4444),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { viewModel.toggleMute() } // resets or clear locally
            )
        }

        // Console Log Stream output
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF090D1A))
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                .padding(6.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sdkLogs) { log ->
                    Text(
                        text = log,
                        color = when {
                            log.contains("[Socket") || log.contains("[Socket") -> Color(0xFFEAB308)
                            log.contains("[REST") -> Color(0xFF38BDF8)
                            log.contains("[Media Engine") || log.contains("[Media") -> Color(0xFF22C55E)
                            else -> Color.White
                        },
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                if (sdkLogs.isEmpty()) {
                    item {
                        Text(
                            text = "Socket closed or awaiting command inputs...",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// DIAGNOSTIC SUITE TAB 1: Smart Home / Home Automation IoT log
// ------------------------------------------------------------------------
@Composable
fun SmartHomeTab(viewModel: TvViewModel) {
    val logs by viewModel.recentLogs.collectAsState()
    val isBlindsClosed by viewModel.windowBlindsClosed.collectAsState()
    val roomLightLvl by viewModel.currentRoomLightIntensity.collectAsState()

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Physical IoT Simulator state cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Light Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(6.dp)
            ) {
                Column {
                    Text("Living Room Lights", color = Color.Gray, fontSize = 8.sp)
                    Text("$roomLightLvl% Dimmer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(roomLightLvl / 100f)
                                .background(Color(0xFFFBBF24))
                        )
                    }
                }
            }

            // Blinds Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(6.dp)
            ) {
                Column {
                    Text("Window Blinds", color = Color.Gray, fontSize = 8.sp)
                    Text(
                        text = if (isBlindsClosed) "ROLLERS CLOSED" else "ROLLERS OPEN",
                        color = if (isBlindsClosed) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Automatic cinema override",
                        color = Color.Gray,
                        fontSize = 7.sp
                    )
                }
            }
        }

        // Direct Testing Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { viewModel.customIoTTrigger("LIGHTING", "Manual Smart Home: Dimmed to 5% instantly.") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("MODE: CINEMA", color = Color.White, fontSize = 8.sp)
            }

            Button(
                onClick = { viewModel.customIoTTrigger("LIGHTING", "Manual Smart Home: Increased to 100% full glow.") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("MODE: BRIGHT", color = Color.White, fontSize = 8.sp)
            }

            Button(
                onClick = { viewModel.clearAllIoTLogs() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x30EF4444)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.weight(0.8f),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("RESET LOGS", color = Color(0xFFEF4444), fontSize = 8.sp)
            }
        }

        Text(
            text = "IOT AUTOMATION EVENT LOG HISTORY (MAPPED TRIGGERS)",
            color = Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // Event log list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0A0E1A))
                .padding(6.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.logMessage,
                            color = Color.White,
                            fontSize = 8.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    when (log.actionType) {
                                        "LIGHTING" -> Color(0x50FBBF24)
                                        "VOICE" -> Color(0x5038BDF8)
                                        "APP" -> Color(0x50A855F7)
                                        else -> Color(0x5022C55E)
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = log.actionType, color = Color.White, fontSize = 6.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (logs.isEmpty()) {
                    item {
                        Text("No home automation logs captured yet.", color = Color.Gray, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// DIAGNOSTIC SUITE TAB 2: Household Profiles & Watch History
// ------------------------------------------------------------------------
@Composable
fun UsersHubTab(viewModel: TvViewModel) {
    val profiles by viewModel.userProfiles.collectAsState()
    val activeProf by viewModel.selectedProfile.collectAsState()
    var appHistoryList by remember { mutableStateOf<List<WatchHistory>>(emptyList()) }

    // Read history flow reactively for selected profile
    LaunchedEffect(activeProf) {
        activeProf?.let {
            viewModel.getHistory(it.id).collect { history ->
                appHistoryList = history
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "SELECT REGISTERED HOUSEHOLD USER PROFILE",
            color = Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // Profiles row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            profiles.forEach { prof ->
                val isSelected = activeProf?.id == prof.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF1E293B) else Color(0x131E293B))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569).copy(alpha = 0.5f),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { viewModel.selectUserProfile(prof) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(prof.avatarColorHex)))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = prof.profileName.substringBefore(" "),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (prof.parentalControlsEnabled) "Kid Limit: ${prof.viewingLimitMinutes}m" else "Admin Access",
                            color = if (prof.parentalControlsEnabled) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontSize = 6.5.sp
                        )
                    }
                }
            }
        }

        // Active Profile Rules & Parental limits display
        activeProf?.let { prof ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131A30), RoundedCornerShape(6.dp))
                    .padding(6.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SECURITY ISOLATION RULES FOR: " + prof.profileName.uppercase(),
                            color = Color(0xFF38BDF8),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ERASE PROFILE DATA",
                            color = Color(0xFFEF4444),
                            fontSize = 7.sp,
                            modifier = Modifier.clickable { viewModel.clearAllHistory() }
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Preferred Genre Matching: ${prof.preferredGenre} | Strict Content Restriction: ${if (prof.parentalControlsEnabled) "Locked: R-Rating Restrictions apply" else "None"}",
                        color = Color.LightGray,
                        fontSize = 8.sp
                    )
                }
            }
        }

        Text(
            text = "PROFILE WATCH HISTORY CONTINUATION LOGS",
            color = Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // History logs
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF090D1A))
                .padding(6.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(appHistoryList) { hist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = hist.mediaTitle,
                            color = Color.White,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Position: " + formatTimelineText(hist.watchedDurationSeconds),
                            color = Color(0xFF818CF8),
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                if (appHistoryList.isEmpty()) {
                    item {
                        Text("No continuous watch logs recorded yet.", color = Color.Gray, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// DIAGNOSTIC SUITE TAB 3: Analytics dashboard metrics
// ------------------------------------------------------------------------
@Composable
fun AnalyticsTab(viewModel: TvViewModel) {
    val analyticsMetrics by viewModel.analyticsMetrics.collectAsState()

    val aggTotalSeconds = remember(analyticsMetrics) {
        analyticsMetrics.sumOf { it.viewDurationSeconds }
    }

    val appsPopularity = remember(analyticsMetrics) {
        analyticsMetrics.groupBy { it.mediaTitle }.mapValues { it.value.size }.toList().sortedByDescending { it.second }.take(3)
    }

    val dropOffIndex = remember(analyticsMetrics) {
        val total = analyticsMetrics.size
        if (total == 0) 0 else {
            val partialCount = analyticsMetrics.count { it.completionStatus == "PARTIAL" }
            (partialCount * 100) / total
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "HARDWARE ENGINE TELEMETRY INTEL (BIG DATA)",
            color = Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        // Key stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Aggregate Duration Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(6.dp)
            ) {
                Column {
                    Text("Total Streams Watched", color = Color.Gray, fontSize = 7.5.sp)
                    Text(
                        text = "${aggTotalSeconds / 60} min",
                        color = Color(0xFF10B981),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Drop-off rate Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B))
                    .padding(6.dp)
            ) {
                Column {
                    Text("Session Drop-Off", color = Color.Gray, fontSize = 7.5.sp)
                    Text(
                        text = "$dropOffIndex% partial",
                        color = Color(0xFFFF5555),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Popular Content Ranking List
        Text(
            text = "RECOMMENDED ALGORITHM POPULAR CONTENT RATINGS",
            color = Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF090D1A))
                .padding(6.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(appsPopularity) { ranking ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ranking.first,
                            color = Color.White,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Hits: " + ranking.second,
                            color = Color(0xFFFBBF24),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (appsPopularity.isEmpty()) {
                    item {
                        Text("Awaiting video stream playback metrics compilation...", color = Color.Gray, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------
// AUXILIARY UTILITY HELPER LOGIC SECTOR
// ------------------------------------------------------------------------
fun formatTimelineText(totalSecs: Int): String {
    val hrs = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val secs = totalSecs % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun bounceHeight(step: Int, timeline: Int): Float {
    // Generate different sinus wave shapes determined by time and step index to animate canvas
    val rad = (timeline + (step * 30)).toDouble() * Math.PI / 180.0
    val wave = Math.sin(rad * 4.0)
    return ((wave + 1.2) / 2.2).toFloat()
}

fun getSimulatedSubtitleLine(timeline: Int, title: String): String {
    val captions = if (title.contains("Space")) {
        listOf(
            0 to "CC: [Spaceship alarm beeping faintly in background]",
            4 to "CC: Captain: Prepare the final telemetry links...",
            8 to "CC: First Officer: Checking standard TCP cluster...",
            14 to "CC: [Loud explosion shakes the command deck!]",
            19 to "CC: Captain: We have lost buffering capacity!",
            25 to "CC: [Thrusters engaging with high-density output]"
        )
    } else {
        listOf(
            0 to "CC: [Dramatic cosmic string synthesizers crescendo]",
            5 to "CC: Presenter: Systems developers must synchronize APIs...",
            11 to "CC: Prototyping Smart TV systems requires low latency.",
            17 to "CC: [Smart Home automated controls dimming overhead lights]",
            24 to "CC: Ensure security credentials stay safely stored."
        )
    }

    val matched = captions.lastOrNull { timeline >= it.first }
    return matched?.second ?: "CC: [Background audio stream playing...]"
}
