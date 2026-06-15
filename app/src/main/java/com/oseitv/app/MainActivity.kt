package com.oseitv.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.os.Handler
import android.os.Looper
import com.oseitv.app.models.Channel
import com.oseitv.app.repository.EPGRepository
import kotlinx.coroutines.delay

val defaultChannels = listOf(
    Channel("1", "News 24", "https://shm.97u.xyz/live/news24.m3u8", "News"),
    Channel("2", "Sports Live", "https://shm.97u.xyz/live/sports.m3u8", "Sports"),
    Channel("3", "Movies Now", "https://shm.97u.xyz/live/movies.m3u8", "Movies"),
    Channel("4", "Action Max", "https://shm.97u.xyz/live/action.m3u8", "Movies"),
    Channel("5", "Discovery Plus", "https://shm.97u.xyz/live/discovery.m3u8", "Documentary"),
    Channel("6", "Cartoon Network", "https://shm.97u.xyz/live/cartoons.m3u8", "Kids")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OseiTVApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OseiTVApp() {
    var selectedChannel by remember { mutableStateOf(defaultChannels[0]) }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    val schedule = remember(selectedChannel) { EPGRepository.generateSchedule(selectedChannel.id) }
    val currentProgram = remember(schedule) { EPGRepository.getCurrentlyPlaying(schedule) }
    
    var currentTab by remember { mutableStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showChannelSwitcher by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Immersive screen orientation and system bar overrides
    LaunchedEffect(isFullScreen) {
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (showChannelSwitcher) {
        ChannelSwitcherBottomSheet(
            selectedChannel = selectedChannel,
            favorites = favorites,
            onChannelSelected = { channel ->
                selectedChannel = channel
                showChannelSwitcher = false
            },
            onDismiss = { showChannelSwitcher = false }
        )
    }

    if (isFullScreen) {
        BackHandler {
            isFullScreen = false
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayer(
                url = selectedChannel.url,
                channelName = selectedChannel.name,
                programTitle = currentProgram?.title,
                isFullScreen = true,
                onToggleFullScreen = { isFullScreen = false },
                onOpenChannelSwitcher = { showChannelSwitcher = true }
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text("OSEI TV", fontWeight = FontWeight.Black, color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { showChannelSwitcher = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Channels Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showChannelSwitcher = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search Channels", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showChannelSwitcher = true },
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.List, contentDescription = "Channel Switcher") },
                    text = { Text("GUIDE", fontWeight = FontWeight.Black) }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.Black) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Tv, contentDescription = null) },
                        label = { Text("Live TV") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Red,
                            selectedTextColor = Color.Red,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Favorites") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Red,
                            selectedTextColor = Color.Red,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            },
            containerColor = Color(0xFF0A0A0A)
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Player Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayer(
                        url = selectedChannel.url,
                        channelName = selectedChannel.name,
                        programTitle = currentProgram?.title,
                        isFullScreen = false,
                        onToggleFullScreen = { isFullScreen = true },
                        onOpenChannelSwitcher = { showChannelSwitcher = true }
                    )
                }

                // Current Program Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedChannel.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            favorites = if (favorites.contains(selectedChannel.id)) {
                                favorites - selectedChannel.id
                            } else {
                                favorites + selectedChannel.id
                            }
                        }) {
                            Icon(
                                imageVector = if (favorites.contains(selectedChannel.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (favorites.contains(selectedChannel.id)) Color.Red else Color.White
                            )
                        }
                    }
                    
                    currentProgram?.let {
                        Text(
                            text = "LIVE: ${it.title}",
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = it.description,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Divider(color = Color(0xFF1E1E1E))

                // Channel List
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = "BROWSE CHANNELS",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val displayChannels = if (currentTab == 1) {
                        defaultChannels.filter { favorites.contains(it.id) }
                    } else {
                        defaultChannels
                    }

                    if (displayChannels.isEmpty() && currentTab == 1) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No favorites yet", color = Color.Gray)
                            }
                        }
                    }

                    items(displayChannels) { channel ->
                        ChannelItem(
                            channel = channel,
                            isSelected = channel.id == selectedChannel.id,
                            onClick = { selectedChannel = channel }
                        )
                    }
                }
            }
        }
    }
}

class WebAppInterface(
    private val onPlayStateChanged: (Boolean) -> Unit,
    private val onBuffering: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPlayStateChanged(isPlaying: Boolean) {
        mainHandler.post { onPlayStateChanged(isPlaying) }
    }

    @JavascriptInterface
    fun onBuffering(isBuffering: Boolean) {
        mainHandler.post { onBuffering(isBuffering) }
    }

    @JavascriptInterface
    fun onError(error: String) {
        mainHandler.post { onError(error) }
    }
}

@Composable
fun VideoPlayer(
    url: String,
    channelName: String,
    programTitle: String?,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onOpenChannelSwitcher: () -> Unit
) {
    var isBuffering by remember { mutableStateOf(true) }
    var isPlayingState by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    var volume by remember { mutableFloatStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    
    var webView: WebView? by remember { mutableStateOf(null) }

    val baseHtmlContent = remember {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, minimal-ui">
            <style>
                body, html {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background-color: #000000;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                video {
                    width: 100%;
                    height: 100%;
                    object-fit: contain;
                    background-color: #000000;
                }
            </style>
            <script src="https://cdn.jsdelivr.net/npm/hls.js@1.4.12/dist/hls.min.js"></script>
        </head>
        <body>
            <video id="video" playsinline autoplay></video>
            <script>
                var video = document.getElementById('video');
                var hls = null;
                var currentUrl = "";

                function initPlayer(url) {
                    if (!url) return;
                    if (currentUrl === url) return;
                    currentUrl = url;
                    
                    if (hls) {
                        hls.destroy();
                        hls = null;
                    }

                    if (Hls.isSupported()) {
                        hls = new Hls({
                            enableWorker: true,
                            lowLatencyMode: true
                        });
                        hls.loadSource(url);
                        hls.attachMedia(video);
                        hls.on(Hls.Events.MANIFEST_PARSED, function() {
                            video.play().catch(function(e) {
                                console.log("Autoplay failed: " + e);
                            });
                        });
                        hls.on(Hls.Events.ERROR, function(event, data) {
                            if (data.fatal) {
                                switch (data.type) {
                                    case Hls.ErrorTypes.NETWORK_ERROR:
                                        console.log("Network error, retrying...");
                                        hls.startLoad();
                                        break;
                                    case Hls.ErrorTypes.MEDIA_ERROR:
                                        console.log("Media error, recovering...");
                                        hls.recoverMediaError();
                                        break;
                                    default:
                                        console.log("Fatal player error, reloading...");
                                        initPlayer(currentUrl);
                                        break;
                                }
                            }
                        });
                    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                        video.src = url;
                        video.addEventListener('loadedmetadata', function() {
                            video.play().catch(function(e) { console.log(e); });
                        });
                    } else {
                        video.src = url;
                        video.play().catch(function(e) { console.log(e); });
                    }
                }

                function playVideo() {
                    video.play().catch(function(e) { console.log(e); });
                }

                function pauseVideo() {
                    video.pause();
                }

                function setVolume(vol) {
                    video.volume = vol;
                }

                function setMuted(muted) {
                    video.muted = muted;
                }

                // Web to App listeners
                video.addEventListener('play', function() {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onPlayStateChanged(true);
                    }
                });
                video.addEventListener('pause', function() {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onPlayStateChanged(false);
                    }
                });
                video.addEventListener('waiting', function() {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onBuffering(true);
                    }
                });
                video.addEventListener('playing', function() {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onBuffering(false);
                    }
                });
                video.addEventListener('error', function() {
                    if (window.AndroidInterface) {
                        window.AndroidInterface.onError("Playback error occurred");
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    LaunchedEffect(url, webView) {
        webView?.let {
            isBuffering = true
            hasError = false
            it.evaluateJavascript("initPlayer('$url')", null)
        }
    }

    LaunchedEffect(isPlayingState, webView) {
        webView?.evaluateJavascript(if (isPlayingState) "playVideo()" else "pauseVideo()", null)
    }

    LaunchedEffect(volume, webView) {
        webView?.evaluateJavascript("setVolume($volume)", null)
    }

    LaunchedEffect(isMuted, webView) {
        webView?.evaluateJavascript("setMuted($isMuted)", null)
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlayingState) {
        if (showControls && isPlayingState) {
            delay(4000L)
            showControls = false
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webView = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, urlStr: String?) {
                            view?.evaluateJavascript("setVolume($volume)", null)
                            view?.evaluateJavascript("setMuted($isMuted)", null)
                            view?.evaluateJavascript("initPlayer('$url')", null)
                        }
                    }
                    addJavascriptInterface(
                        WebAppInterface(
                            onPlayStateChanged = { playing -> isPlayingState = playing },
                            onBuffering = { buffering -> isBuffering = buffering },
                            onError = { err ->
                                hasError = true
                                errorMessage = err
                            }
                        ),
                        "AndroidInterface"
                    )
                    loadDataWithBaseURL("https://localhost", baseHtmlContent, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering loader
        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red,
                strokeWidth = 4.dp
            )
        }

        // Error message layer
        if (hasError) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        hasError = false
                        isBuffering = true
                        webView?.evaluateJavascript("initPlayer('$url')", null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reconnect Stream")
                }
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Header Pane
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isFullScreen) {
                        IconButton(onClick = onToggleFullScreen) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Exit Full Screen",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = channelName,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                        programTitle?.let {
                            Text(
                                text = "LIVE SHOW: $it",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(onClick = onOpenChannelSwitcher) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Switch Channels",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Surface(
                        color = Color.Red,
                        shape = CircleShape,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Central Play/Pause Control
                IconButton(
                    onClick = {
                        isPlayingState = !isPlayingState
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Bottom Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(bottom = if (isFullScreen) 24.dp else 12.dp, start = 16.dp, end = 16.dp, top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Volume layout
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            IconButton(onClick = { isMuted = !isMuted }) {
                                Icon(
                                    imageVector = if (isMuted || volume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = "Mute",
                                    tint = Color.White
                                )
                            }
                            Slider(
                                value = volume,
                                onValueChange = {
                                    volume = it
                                    isMuted = false
                                },
                                modifier = Modifier.width(100.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Red,
                                    activeTrackColor = Color.Red,
                                    inactiveTrackColor = Color.DarkGray
                                )
                            )
                        }

                        // Right actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                hasError = false
                                isBuffering = true
                                webView?.evaluateJavascript("initPlayer('$url')", null)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh stream",
                                    tint = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = onToggleFullScreen) {
                                Icon(
                                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(channel: Channel, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFF1A1A1A) else Color.Transparent)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF252525), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = channel.name.take(1),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = channel.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = channel.category, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Text(
                    text = "PLAYING",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelSwitcherBottomSheet(
    selectedChannel: Channel,
    favorites: Set<String>,
    onChannelSelected: (Channel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        contentColor = Color.White,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.DarkGray
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Channel Guide",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Sheet",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search channel or category...", color = Color.Gray) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red,
                    unfocusedBorderColor = Color(0xFF2E2E2E),
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories list (Horizontal Row)
            val categories = listOf("All", "News", "Sports", "Movies", "Documentary", "Kids", "Favorites")
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isCatSelected = selectedCategory == category
                    val count = if (category == "All") {
                        defaultChannels.size
                    } else if (category == "Favorites") {
                        defaultChannels.count { favorites.contains(it.id) }
                    } else {
                        defaultChannels.count { it.category.equals(category, ignoreCase = true) }
                    }

                    Surface(
                        onClick = { selectedCategory = category },
                        color = if (isCatSelected) Color.Red else Color(0xFF222222),
                        contentColor = Color.White,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCatSelected) Color.Red else Color(0xFF333333)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isCatSelected) Color.White.copy(alpha = 0.2f) else Color(0xFF333333),
                                            CircleShape
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Channels
            val filteredChannels = remember(searchQuery, selectedCategory, favorites) {
                defaultChannels.filter { channel ->
                    val matchesSearch = channel.name.contains(searchQuery, ignoreCase = true) ||
                            channel.category.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = when (selectedCategory) {
                        "All" -> true
                        "Favorites" -> favorites.contains(channel.id)
                        else -> channel.category.equals(selectedCategory, ignoreCase = true)
                    }
                    matchesSearch && matchesCategory
                }
            }

            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "No results",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No channels found",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredChannels) { channel ->
                        val isSelected = channel.id == selectedChannel.id
                        val schedule = remember(channel.id) { EPGRepository.generateSchedule(channel.id) }
                        val currentProgram = remember(schedule) { EPGRepository.getCurrentlyPlaying(schedule) }

                        // Progress calculation
                        val progress = remember(currentProgram) {
                            if (currentProgram != null) {
                                val now = java.util.Calendar.getInstance().timeInMillis
                                val start = currentProgram.startTime.timeInMillis
                                val end = currentProgram.endTime.timeInMillis
                                if (end > start) {
                                    ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
                                } else 0f
                            } else 0f
                        }

                        Surface(
                            onClick = {
                                onChannelSelected(channel)
                            },
                            color = if (isSelected) Color(0xFF221111) else Color(0xFF1A1A1A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color.Red.copy(alpha = 0.5f) else Color(0xFF2C2C2C)
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Logo Placeholder
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                if (isSelected) Color.Red else Color(0xFF2E2E2E),
                                                MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = channel.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = channel.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = Color(0xFF292929),
                                                shape = CircleShape
                                            ) {
                                                Text(
                                                    text = channel.category.uppercase(),
                                                    color = Color.LightGray,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        currentProgram?.let { program ->
                                            Text(
                                                text = "LIVE: ${program.title}",
                                                color = if (isSelected) Color.Red else Color.LightGray,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        } ?: Text(
                                            text = "No Schedule Information",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    if (isSelected) {
                                        Surface(
                                            color = Color.Red,
                                            shape = CircleShape
                                        ) {
                                            Text(
                                                text = "PLAYING",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Show the EPG progress bar if available
                                if (currentProgram != null && progress > 0f) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.dp),
                                            color = Color.Red,
                                            trackColor = Color(0xFF2C2C2C)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val percent = (progress * 100).toInt()
                                        Text(
                                            text = "$percent%",
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.labelSmall
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
}
