package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookEntity
import com.example.data.local.NoteEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReaderThemeMode
import com.example.ui.viewmodel.StudyViewModel
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

enum class ReaderDisplayMode {
    GOOGLE_DRIVE_ONLINE,
    OFFLINE_NATIVE
}

/**
 * Thread-safe, memory-efficient PDF multi-page renderer for smooth vertical continuous scrolling.
 */
class SafePdfDocumentRenderer(val file: File) {
    private val lock = Any()
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    var pageCount: Int = 0
        private set

    // In-memory LRU cache for rendered bitmaps
    private val cache = object : LruCache<Int, Bitmap>(24) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    init {
        try {
            if (file.exists() && file.length() > 50) {
                val pfdLocal = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val rLocal = PdfRenderer(pfdLocal)
                this.pfd = pfdLocal
                this.renderer = rLocal
                this.pageCount = rLocal.pageCount
            }
        } catch (t: Throwable) {
            android.util.Log.e("SafePdfRenderer", "Failed to open PDF descriptor", t)
            pageCount = 0
        }
    }

    fun getCachedPage(pageIndex: Int): Bitmap? {
        synchronized(lock) {
            val cached = cache.get(pageIndex)
            return if (cached != null && !cached.isRecycled) cached else null
        }
    }

    suspend fun renderPage(pageIndex: Int): Bitmap? = withContext(Dispatchers.Default) {
        synchronized(lock) {
            if (pageIndex !in 0 until pageCount) return@synchronized null
            val cached = cache.get(pageIndex)
            if (cached != null && !cached.isRecycled) return@synchronized cached

            val r = renderer ?: return@synchronized null
            var page: PdfRenderer.Page? = null
            try {
                page = r.openPage(pageIndex)
                val pw = page.width.coerceAtLeast(1)
                val ph = page.height.coerceAtLeast(1)

                // High-DPI scale for crisp textbook reading
                val scale = 1.6f
                val destW = (pw * scale).toInt().coerceAtLeast(pw)
                val destH = (ph * scale).toInt().coerceAtLeast(ph)

                val bmp = try {
                    val highResBmp = Bitmap.createBitmap(destW, destH, Bitmap.Config.ARGB_8888)
                    highResBmp.eraseColor(AndroidColor.WHITE)
                    val matrix = android.graphics.Matrix().apply {
                        setScale(scale, scale)
                    }
                    page.render(highResBmp, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    highResBmp
                } catch (scaleEx: Throwable) {
                    // Direct 1:1 page dimensions fallback (always works natively)
                    val directBmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
                    directBmp.eraseColor(AndroidColor.WHITE)
                    page.render(directBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    directBmp
                }

                cache.put(pageIndex, bmp)
                bmp
            } catch (t: Throwable) {
                android.util.Log.e("SafePdfRenderer", "Error rendering page $pageIndex", t)
                null
            } finally {
                try {
                    page?.close()
                } catch (ignored: Throwable) {}
            }
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                cache.evictAll()
                renderer?.close()
                renderer = null
                pfd?.close()
                pfd = null
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyReaderScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val book by viewModel.activeReaderBook.collectAsStateWithLifecycle()
    val readerTheme by viewModel.readerThemeMode.collectAsStateWithLifecycle()
    val currentPage by viewModel.readerCurrentPage.collectAsStateWithLifecycle()

    var isFullScreenMode by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isWebLoading by remember { mutableStateOf(true) }
    var webLoadingProgress by remember { mutableStateOf(0) }
    var webErrorOccurred by remember { mutableStateOf(false) }

    // Zoom state for native PDF viewer
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var zoomOffset by remember { mutableStateOf(Offset.Zero) }

    // Automatically select OFFLINE if downloaded, else ONLINE Drive
    var displayMode by remember(book?.id, book?.isDownloaded, book?.localFilePath) {
        mutableStateOf(
            if (book?.isDownloaded == true && !book?.localFilePath.isNullOrBlank() && File(book?.localFilePath ?: "").exists()) {
                ReaderDisplayMode.OFFLINE_NATIVE
            } else {
                ReaderDisplayMode.GOOGLE_DRIVE_ONLINE
            }
        )
    }

    // Handle back button when in full screen: exit full screen first
    BackHandler(enabled = isFullScreenMode) {
        isFullScreenMode = false
    }

    if (book == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate50),
            contentAlignment = Alignment.Center
        ) {
            Text("No book selected", color = Slate500)
        }
        return
    }

    val currentBook = book!!
    val embedUrl = remember(currentBook.fileLink) {
        viewModel.getEmbedViewUrl(currentBook.fileLink)
    }

    // Load SafePdfDocumentRenderer directly and synchronously when local file is present
    val pdfRendererSession = remember(currentBook.localFilePath, currentBook.isDownloaded) {
        val path = currentBook.localFilePath
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists() && file.length() > 50) {
                SafePdfDocumentRenderer(file)
            } else null
        } else null
    }

    DisposableEffect(pdfRendererSession) {
        onDispose {
            pdfRendererSession?.close()
        }
    }

    val pdfTotalPages = pdfRendererSession?.pageCount?.coerceAtLeast(1)
        ?: currentBook.totalPages.coerceAtLeast(1)

    val listState = rememberLazyListState()

    // Sync current page based on list scrolling in vertical multi-page view
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (pdfRendererSession != null && pdfRendererSession!!.pageCount > 0) {
            val page = listState.firstVisibleItemIndex + 1
            if (page != currentPage) {
                viewModel.setReaderPage(page)
            }
        }
    }

    // Scroll to page if page changed externally (e.g. from bookmark)
    LaunchedEffect(currentPage) {
        val targetIdx = (currentPage - 1).coerceIn(0, (pdfTotalPages - 1).coerceAtLeast(0))
        if (listState.firstVisibleItemIndex != targetIdx && !listState.isScrollInProgress) {
            listState.scrollToItem(targetIdx)
        }
    }

    // Colors based on reader theme
    val (readerBg, readerTextColor) = when (readerTheme) {
        ReaderThemeMode.LIGHT -> Pair(Color(0xFFF8FAFC), Slate900)
        ReaderThemeMode.SEPIA -> Pair(ReaderSepiaBg, ReaderSepiaText)
        ReaderThemeMode.DARK -> Pair(Color(0xFF0F172A), Slate100)
    }

    Scaffold(
        topBar = {
            if (!isFullScreenMode) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentBook.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE) {
                                    "Google Drive • Class ${currentBook.classLevel} • ${currentBook.provinceCode.uppercase()}"
                                } else {
                                    "Class ${currentBook.classLevel} • ${currentBook.provinceCode.uppercase()} • Page $currentPage of $pdfTotalPages"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE) Emerald700 else Slate500,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .testTag("reader_back_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Slate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Slate700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        // 1. Create Notes Button
                        IconButton(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.testTag("reader_create_notes_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.EditNote,
                                        contentDescription = "Create Notes",
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 2. Create Bookmark Button (Full Details)
                        IconButton(
                            onClick = { showAddBookmarkDialog = true },
                            modifier = Modifier.testTag("reader_create_bookmark_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkAdd,
                                        contentDescription = "Create Bookmark",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 3. Open in Full Screen Button (Vertical Scroll PDF)
                        IconButton(
                            onClick = { isFullScreenMode = true },
                            modifier = Modifier.testTag("reader_fullscreen_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Emerald50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fullscreen,
                                        contentDescription = "Open in Full Screen",
                                        tint = Emerald700,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Secondary Options Menu (3-dots)
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("reader_more_options_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More Options",
                                        tint = Slate700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Open in Google Drive App") },
                                    leadingIcon = { Icon(Icons.Outlined.OpenInNew, contentDescription = null, tint = Color(0xFF4338CA)) },
                                    onClick = {
                                        showMoreMenu = false
                                        openBookExternally(context, currentBook)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (displayMode == ReaderDisplayMode.OFFLINE_NATIVE) "Switch to Drive Viewer" else "Switch to Native PDF") },
                                    leadingIcon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null, tint = Emerald700) },
                                    onClick = {
                                        showMoreMenu = false
                                        displayMode = if (displayMode == ReaderDisplayMode.OFFLINE_NATIVE) {
                                            ReaderDisplayMode.GOOGLE_DRIVE_ONLINE
                                        } else {
                                            ReaderDisplayMode.OFFLINE_NATIVE
                                        }
                                    }
                                )
                                if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE) {
                                    DropdownMenuItem(
                                        text = { Text("Reload Document") },
                                        leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            isWebLoading = true
                                            webErrorOccurred = false
                                            webViewInstance?.reload()
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Reader Display Settings") },
                                    leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showSettingsSheet = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        },
        bottomBar = {
            if (!isFullScreenMode) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE) {
                            // Online Drive Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Emerald600)
                                    )
                                    Text(
                                        text = "Google Drive PDF View",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800
                                        )
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { isFullScreenMode = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Full Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Offline Native PDF Controls
                            Slider(
                                value = currentPage.toFloat().coerceIn(1f, pdfTotalPages.toFloat()),
                                onValueChange = {
                                    val page = it.toInt().coerceIn(1, pdfTotalPages)
                                    viewModel.setReaderPage(page)
                                    scope.launch {
                                        listState.animateScrollToItem((page - 1).coerceAtLeast(0))
                                    }
                                },
                                valueRange = 1f..pdfTotalPages.toFloat(),
                                steps = (pdfTotalPages - 2).coerceAtLeast(0),
                                colors = SliderDefaults.colors(
                                    thumbColor = Emerald600,
                                    activeTrackColor = Emerald600,
                                    inactiveTrackColor = Slate200
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        if (currentPage > 1) {
                                            val page = currentPage - 1
                                            viewModel.setReaderPage(page)
                                            scope.launch { listState.animateScrollToItem(page - 1) }
                                        }
                                    },
                                    enabled = currentPage > 1,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Slate100,
                                        contentColor = Slate800
                                    )
                                ) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Prev", fontWeight = FontWeight.SemiBold)
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Emerald50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                                    modifier = Modifier.clickable { isFullScreenMode = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Page $currentPage / $pdfTotalPages",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald800
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Emerald700, modifier = Modifier.size(14.dp))
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        if (currentPage < pdfTotalPages) {
                                            val page = currentPage + 1
                                            viewModel.setReaderPage(page)
                                            scope.launch { listState.animateScrollToItem(page - 1) }
                                        }
                                    },
                                    enabled = currentPage < pdfTotalPages,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Emerald600,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Next", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreenMode) PaddingValues(0.dp) else innerPadding)
                .background(readerBg)
        ) {
            if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE && embedUrl.isNotBlank()) {
                // Interactive In-App Google Drive Web Viewer (with vertical scrolling)
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.allowFileAccess = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        webLoadingProgress = newProgress
                                        if (newProgress >= 100) {
                                            isWebLoading = false
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isWebLoading = true
                                        webErrorOccurred = false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isWebLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            webErrorOccurred = true
                                            isWebLoading = false
                                        }
                                    }
                                }

                                loadUrl(embedUrl)
                                webViewInstance = this
                            }
                        },
                        update = { webView ->
                            webViewInstance = webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Loading Progress Indicator
                    if (isWebLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.95f)),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Emerald600, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading Google Drive Document...",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${webLoadingProgress}% completed",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                            )
                        }
                    }

                    // Fallback Error View
                    if (webErrorOccurred) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 6.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Emerald100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Emerald700, modifier = Modifier.size(24.dp))
                                }
                                Text(
                                    text = "Google Drive Book Ready",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                                )
                                Text(
                                    text = "Open directly in Google Drive app or switch to native offline viewer.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate600),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { openBookExternally(context, currentBook) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open in Google Drive", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        webErrorOccurred = false
                                        isWebLoading = true
                                        webViewInstance?.reload()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Loading", color = Slate700)
                                }
                            }
                        }
                    }
                }
            } else {
                // Offline Native PDF Multi-Page Vertical Continuous Scroll Reader
                if (pdfRendererSession != null && pdfRendererSession!!.pageCount > 0) {
                    val renderer = pdfRendererSession!!
                    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        zoomScale = (zoomScale * zoomChange).coerceIn(1f, 3.5f)
                        if (zoomScale > 1f) {
                            zoomOffset += offsetChange
                        } else {
                            zoomOffset = Offset.Zero
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformState)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = zoomScale,
                                    scaleY = zoomScale,
                                    translationX = zoomOffset.x,
                                    translationY = zoomOffset.y
                                ),
                            contentPadding = PaddingValues(
                                start = if (isFullScreenMode) 8.dp else 12.dp,
                                end = if (isFullScreenMode) 8.dp else 12.dp,
                                top = if (isFullScreenMode) 12.dp else 14.dp,
                                bottom = if (isFullScreenMode) 80.dp else 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(renderer.pageCount, key = { it }) { pageIdx ->
                                val pageNum = pageIdx + 1
                                var pageBitmap by remember(pageIdx, renderer) {
                                    mutableStateOf(renderer.getCachedPage(pageIdx))
                                }

                                LaunchedEffect(pageIdx, renderer) {
                                    if (pageBitmap == null) {
                                        val bmp = renderer.renderPage(pageIdx)
                                        if (bmp != null) {
                                            pageBitmap = bmp
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    shadowElevation = if (isFullScreenMode) 2.dp else 4.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Page Header Tag
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Slate100)
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Page $pageNum of ${renderer.pageCount}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Slate600
                                                )
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.EditNote,
                                                    contentDescription = "Note on Page $pageNum",
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            viewModel.setReaderPage(pageNum)
                                                            showAddNoteDialog = true
                                                        }
                                                )
                                                Icon(
                                                    imageVector = Icons.Outlined.BookmarkAdd,
                                                    contentDescription = "Bookmark Page $pageNum",
                                                    tint = Color(0xFFD97706),
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clickable {
                                                            viewModel.setReaderPage(pageNum)
                                                            showAddBookmarkDialog = true
                                                        }
                                                )
                                            }
                                        }

                                        // Rendered Page Image
                                        val renderedBmp = pageBitmap
                                        if (renderedBmp != null) {
                                            Image(
                                                bitmap = renderedBmp.asImageBitmap(),
                                                contentDescription = "PDF Page $pageNum",
                                                contentScale = ContentScale.FillWidth,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(380.dp)
                                                    .background(Slate50),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = Emerald600,
                                                    strokeWidth = 2.5.dp,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Fallback Download / State Card
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Emerald50),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = Emerald700, modifier = Modifier.size(30.dp))
                                }
                                Text(
                                    text = currentBook.title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Class ${currentBook.classLevel} • ${currentBook.provinceCode.uppercase()} Board",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Slate600)
                                )

                                if (currentBook.isDownloading) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { (currentBook.downloadProgress / 100f).coerceIn(0.1f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                            color = Emerald600,
                                            trackColor = Emerald100
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Downloading for Offline Vertical PDF Reading... ${currentBook.downloadProgress}%",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Emerald700, fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.downloadBook(currentBook) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download & Read Offline in App", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { displayMode = ReaderDisplayMode.GOOGLE_DRIVE_ONLINE },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Google Drive Online Viewer", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { openBookExternally(context, currentBook) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open in Google Drive App", color = Color(0xFF4338CA), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Floating Controls HUD for Full Screen Mode
            AnimatedVisibility(
                visible = isFullScreenMode,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.92f),
                    shadowElevation = 10.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Exit Fullscreen Button
                        IconButton(
                            onClick = { isFullScreenMode = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FullscreenExit,
                                contentDescription = "Exit Full Screen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Page indicator badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = "Page $currentPage / $pdfTotalPages",
                                color = Emerald400,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Zoom In/Out controls for PDF
                        if (displayMode == ReaderDisplayMode.OFFLINE_NATIVE) {
                            IconButton(
                                onClick = {
                                    zoomScale = if (zoomScale > 1.1f) 1f else 1.75f
                                    zoomOffset = Offset.Zero
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (zoomScale > 1.1f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                                    contentDescription = "Toggle Zoom",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Quick Note
                        IconButton(
                            onClick = { showAddNoteDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EditNote,
                                contentDescription = "Create Notes",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Quick Bookmark
                        IconButton(
                            onClick = { showAddBookmarkDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkAdd,
                                contentDescription = "Create Bookmark",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 1. Create Note Dialog
    if (showAddNoteDialog) {
        CreateNoteDialog(
            book = currentBook,
            pageNumber = currentPage,
            onDismiss = { showAddNoteDialog = false },
            onSave = { note ->
                viewModel.saveNote(note)
                showAddNoteDialog = false
            }
        )
    }

    // 2. Create Bookmark Dialog (Full Details: Book, Class, Province, Page, Chapter, Memo)
    if (showAddBookmarkDialog) {
        CreateBookmarkDialog(
            book = currentBook,
            initialPage = currentPage,
            totalPages = pdfTotalPages,
            onDismiss = { showAddBookmarkDialog = false },
            onSave = { page, chapterTitle, noteSnippet ->
                viewModel.addPageBookmark(
                    book = currentBook,
                    pageNumber = page,
                    chapterTitle = chapterTitle,
                    noteSnippet = noteSnippet
                )
                showAddBookmarkDialog = false
            }
        )
    }

    // Reader Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "Reader Preferences",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )

                // Display Mode Switcher
                Text(
                    text = "Viewer Mode",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOptionButton(
                        name = "Offline PDF View",
                        bg = if (displayMode == ReaderDisplayMode.OFFLINE_NATIVE) Emerald50 else Color.White,
                        textColor = if (displayMode == ReaderDisplayMode.OFFLINE_NATIVE) Emerald800 else Slate700,
                        isSelected = displayMode == ReaderDisplayMode.OFFLINE_NATIVE,
                        onClick = {
                            displayMode = ReaderDisplayMode.OFFLINE_NATIVE
                            showSettingsSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionButton(
                        name = "Google Drive (Live)",
                        bg = if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE) Emerald50 else Color.White,
                        textColor = if (displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE) Emerald800 else Slate700,
                        isSelected = displayMode == ReaderDisplayMode.GOOGLE_DRIVE_ONLINE,
                        onClick = {
                            displayMode = ReaderDisplayMode.GOOGLE_DRIVE_ONLINE
                            showSettingsSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // External App Launcher
                OutlinedButton(
                    onClick = {
                        openBookExternally(context, currentBook)
                        showSettingsSheet = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4338CA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Launch in Google Drive App", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * 1. Dedicated Note Creation Dialog
 */
@Composable
fun CreateNoteDialog(
    book: BookEntity,
    pageNumber: Int,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit
) {
    var title by remember { mutableStateOf("Note: ${book.subject} (Pg. $pageNumber)") }
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(0) }

    val colorOptions = listOf(
        Pair("Emerald", Color(0xFF10B981)),
        Pair("Blue", Color(0xFF3B82F6)),
        Pair("Amber", Color(0xFFF59E0B)),
        Pair("Purple", Color(0xFF8B5CF6)),
        Pair("Rose", Color(0xFFF43F5E))
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                }
                Text("Create Note", fontWeight = FontWeight.Bold, color = Slate900)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Book metadata tag
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${book.title} • Class ${book.classLevel}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = Slate700),
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Emerald600
                        ) {
                            Text(
                                text = "Page $pageNumber",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Write formulas, concepts, or reminders...") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color selector
                Text("Note Tag Color", style = MaterialTheme.typography.labelSmall.copy(color = Slate600, fontWeight = FontWeight.Bold))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorOptions.forEachIndexed { index, pair ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(pair.second)
                                .border(
                                    width = if (selectedColor == index) 3.dp else 0.dp,
                                    color = if (selectedColor == index) Slate900 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == index) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onSave(
                            NoteEntity(
                                bookId = book.id,
                                bookTitle = book.title,
                                classLevel = book.classLevel,
                                subject = book.subject,
                                title = title.ifBlank { "Study Note (Pg. $pageNumber)" },
                                content = content,
                                colorIndex = selectedColor
                            )
                        )
                    }
                },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Note", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

/**
 * 2. Dedicated Bookmark Creation Dialog with Full Details
 * Captures specific page, book title, class level, province board, chapter/title, and note snippet.
 */
@Composable
fun CreateBookmarkDialog(
    book: BookEntity,
    initialPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (page: Int, chapterTitle: String, noteSnippet: String) -> Unit
) {
    var pageInput by remember { mutableStateOf(initialPage.toString()) }
    var chapterTitle by remember { mutableStateOf("Chapter / Topic on Page $initialPage") }
    var noteSnippet by remember { mutableStateOf("") }

    val selectedPageNumber = pageInput.toIntOrNull()?.coerceIn(1, totalPages.coerceAtLeast(1)) ?: initialPage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                }
                Text("Create Bookmark", fontWeight = FontWeight.Bold, color = Slate900)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Detailed Metadata Summary Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900),
                            maxLines = 1
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Emerald100
                            ) {
                                Text(
                                    text = "Class ${book.classLevel}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Emerald800, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0E7FF)
                            ) {
                                Text(
                                    text = "${book.provinceCode.uppercase()} Board",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF4338CA), fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate200
                            ) {
                                Text(
                                    text = book.subject,
                                    style = MaterialTheme.typography.labelSmall.copy(color = Slate700, fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Page Number Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) {
                                pageInput = input
                            }
                        },
                        label = { Text("Page Number (1-$totalPages)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Emerald600,
                            unfocusedBorderColor = Slate300
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val current = pageInput.toIntOrNull() ?: 1
                            if (current > 1) pageInput = (current - 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Prev Page", tint = Slate700)
                    }

                    IconButton(
                        onClick = {
                            val current = pageInput.toIntOrNull() ?: 1
                            if (current < totalPages) pageInput = (current + 1).toString()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate100)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Next Page", tint = Slate700)
                    }
                }

                // Chapter / Title for Bookmark
                OutlinedTextField(
                    value = chapterTitle,
                    onValueChange = { chapterTitle = it },
                    label = { Text("Chapter / Bookmark Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Memo / Snippet
                OutlinedTextField(
                    value = noteSnippet,
                    onValueChange = { noteSnippet = it },
                    label = { Text("Short Memo / Reminder (Optional)") },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        selectedPageNumber,
                        chapterTitle.ifBlank { "Page $selectedPageNumber Bookmark" },
                        noteSnippet.trim()
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Bookmark", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate600)
            }
        }
    )
}

private fun openBookExternally(context: Context, book: BookEntity) {
    try {
        val path = book.localFilePath
        if (!path.isNullOrBlank()) {
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Open with PDF Reader"))
                return
            }
        }

        if (book.fileLink.isNotBlank()) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(book.fileLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } else {
            Toast.makeText(context, "No web link or local file available", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        try {
            if (book.fileLink.isNotBlank()) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(book.fileLink)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } else {
                Toast.makeText(context, "Could not open file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } catch (e2: Exception) {
            Toast.makeText(context, "Could not open external app", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ThemeOptionButton(
    name: String,
    bg: Color,
    textColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Emerald600 else Slate300
        ),
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            )
        }
    }
}
