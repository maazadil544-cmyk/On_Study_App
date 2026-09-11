package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adsterra.AdsterraBannerAd
import com.example.adsterra.AdsterraManager
import com.example.data.local.BookEntity
import com.example.data.model.Province
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun HomeScreen(viewModel: StudyViewModel) {
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val generalBooks by viewModel.generalBooks.collectAsStateWithLifecycle()
    val downloadedBooks by viewModel.downloadedBooks.collectAsStateWithLifecycle()
    val recentReads by viewModel.recentReads.collectAsStateWithLifecycle()
    val allNews by viewModel.allNews.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "ON Study",
                subtitle = "Academic Guides & Textbooks",
                showBackButton = false,
                viewModel = viewModel,
                actions = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.BOOKMARKS) },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                            .testTag("home_bookmarks_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmarks",
                            tint = Slate800,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            GeometricBottomNavigation(viewModel = viewModel, currentScreen = Screen.HOME)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search guides, textbooks, subjects...",
                            color = Slate400,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Emerald600,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Slate200)
                        .testTag("home_search_input"),
                    singleLine = true
                )
            }

            // If user searched in Home, show instant search results
            if (searchQuery.isNotBlank()) {
                val searchResults = allBooks.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                            it.subject.contains(searchQuery, ignoreCase = true) ||
                            it.provinceCode.contains(searchQuery, ignoreCase = true)
                }

                item {
                    Text(
                        text = "Search Results (${searchResults.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )
                }

                if (searchResults.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No study material found for \"$searchQuery\"",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Slate600,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(searchResults) { book ->
                        QuickBookItemCard(
                            book = book,
                            onOpen = { viewModel.openReader(book) },
                            onDownload = { viewModel.downloadBook(book) }
                        )
                    }
                }
            } else {
                // Section 1: Geometric Header Title
                item {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                        Text(
                            text = "SELECT YOUR PROVINCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                letterSpacing = 1.8.sp,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("Explore ")
                                withStyle(SpanStyle(color = Emerald600)) {
                                    append("Books")
                                }
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                letterSpacing = (-0.8).sp,
                                color = Slate900
                            )
                        )
                    }
                }

                // Section 2: 2x2 Balanced Geometric Grid for 4 Provinces
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Row 1: KPK & Punjab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GeometricProvinceCard(
                                province = Province.KPK,
                                bookCount = allBooks.count { it.provinceCode.equals("kpk", ignoreCase = true) },
                                modifier = Modifier.weight(1f),
                                testTag = "province_card_kpk",
                                onClick = { viewModel.selectProvince(Province.KPK) }
                            )

                            GeometricProvinceCard(
                                province = Province.PUNJAB,
                                bookCount = allBooks.count { it.provinceCode.equals("punjab", ignoreCase = true) },
                                modifier = Modifier.weight(1f),
                                testTag = "province_card_punjab",
                                onClick = { viewModel.selectProvince(Province.PUNJAB) }
                            )
                        }

                        // Row 2: Balochistan & Sindh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GeometricProvinceCard(
                                province = Province.BALOCHISTAN,
                                bookCount = allBooks.count { it.provinceCode.equals("balochistan", ignoreCase = true) },
                                modifier = Modifier.weight(1f),
                                testTag = "province_card_balochistan",
                                onClick = { viewModel.selectProvince(Province.BALOCHISTAN) }
                            )

                            GeometricProvinceCard(
                                province = Province.SINDH,
                                bookCount = allBooks.count { it.provinceCode.equals("sindh", ignoreCase = true) },
                                modifier = Modifier.weight(1f),
                                testTag = "province_card_sindh",
                                onClick = { viewModel.selectProvince(Province.SINDH) }
                            )
                        }
                    }
                }

                // Section 3: General & Skill Books (Grammar, Health Care, GK, Computer, etc.)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        // Section Header with Urdu subtitle and "View All" button
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
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D9488))
                                )
                                Column {
                                    Text(
                                        text = "GENERAL BOOKS & GUIDES",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate600,
                                            letterSpacing = 1.4.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                    Text(
                                        text = "عام اور معلوماتی کتب (Grammar, Health & GK)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF0D9488),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    AdsterraManager.triggerPopunder(context)
                                    viewModel.openGeneralBooks()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("home_view_all_general_books")
                            ) {
                                Text(
                                    text = "View All (${generalBooks.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D9488),
                                        fontSize = 11.sp
                                    )
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Subject Filter Chips on Home
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            val topics = listOf(
                                "English Grammar" to Icons.Default.Spellcheck,
                                "Health & First Aid" to Icons.Default.MedicalServices,
                                "General Knowledge" to Icons.Default.Lightbulb,
                                "Computer Skills" to Icons.Default.Computer,
                                "Urdu Grammar" to Icons.Default.MenuBook,
                                "Islamic & Ethics" to Icons.Default.Mosque
                            )

                            items(topics) { (topic, icon) ->
                                Surface(
                                    onClick = {
                                        AdsterraManager.triggerPopunder(context)
                                        viewModel.openGeneralBooks(topic)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.testTag("home_topic_chip_$topic")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color(0xFF0D9488),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = topic,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Slate800,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Horizontal Carousel of Featured General Books
                        if (generalBooks.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(generalBooks) { book ->
                                    FeaturedGeneralBookCard(
                                        book = book,
                                        onOpen = { viewModel.openReader(book) },
                                        onDownload = { viewModel.downloadBook(book) },
                                        onCardClick = { viewModel.openReader(book) }
                                    )
                                }
                            }
                        } else {
                            // Fallback card if loading
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF0FDFA),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        AdsterraManager.triggerPopunder(context)
                                        viewModel.openGeneralBooks()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = Color(0xFF0D9488),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Explore General Books Library", fontWeight = FontWeight.Bold, color = Color(0xFF115E59))
                                        Text("English Grammar, Health Care & First Aid, GK", fontSize = 11.sp, color = Color(0xFF134E4A))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Recent Openings / Resume Section (Enhanced Dedicated Section)
                if (recentReads.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
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
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Emerald600)
                                    )
                                    Text(
                                        text = "RECENTLY OPENED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Slate600,
                                            letterSpacing = 1.4.sp,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Text(
                                    text = "${recentReads.size} in history",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Slate400,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Hero Banner for most recent book with direct page resume
                            GeometricHeroRecentCard(
                                recentBook = recentReads.firstOrNull(),
                                totalBooks = allBooks.size,
                                downloadedCount = downloadedBooks.size,
                                latestNews = allNews.firstOrNull()?.title ?: "2026 Board Exam Syllabus & Guides Published",
                                onNewsClick = { viewModel.navigateTo(Screen.NEWS) },
                                onResume = { book ->
                                    viewModel.openReader(book, if (book.lastReadPage > 0) book.lastReadPage else 1)
                                },
                                onExploreClick = {
                                    viewModel.selectProvince(Province.PUNJAB)
                                }
                            )

                            // Additional recent items list if user opened more books
                            if (recentReads.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    recentReads.drop(1).forEach { book ->
                                        QuickBookItemCard(
                                            book = book,
                                            onOpen = { viewModel.openReader(book, if (book.lastReadPage > 0) book.lastReadPage else 1) },
                                            onDownload = { viewModel.downloadBook(book) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Default Explore Hero Card when no book has been opened yet
                    item {
                        GeometricHeroRecentCard(
                            recentBook = null,
                            totalBooks = allBooks.size,
                            downloadedCount = downloadedBooks.size,
                            latestNews = allNews.firstOrNull()?.title ?: "2026 Board Exam Syllabus & Guides Published",
                            onNewsClick = { viewModel.navigateTo(Screen.NEWS) },
                            onResume = {},
                            onExploreClick = {
                                viewModel.selectProvince(Province.PUNJAB)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeometricProvinceCard(
    province: Province,
    bookCount: Int,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = province.primaryColor.copy(alpha = 0.15f),
                ambientColor = Slate200
            )
            .testTag(testTag),
        shape = RoundedCornerShape(32.dp),
        color = province.containerColor.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(2.dp, province.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top Row: Monogram Badge & Urdu Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Monogram Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            spotColor = province.primaryColor.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(province.primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = province.monogram,
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp
                        )
                    )
                }

                // Urdu Badge
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, province.borderColor),
                    shadowElevation = 1.dp
                ) {
                    Text(
                        text = province.urduName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = province.primaryColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Province Name
            Text(
                text = province.title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Slate800,
                    letterSpacing = (-0.3).sp
                )
            )

            // Board label
            Text(
                text = "${province.title} Board",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate500,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Book count micro badge
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, province.borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$bookCount Guides",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = province.textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GeometricHeroRecentCard(
    recentBook: BookEntity?,
    totalBooks: Int,
    downloadedCount: Int,
    latestNews: String,
    onNewsClick: () -> Unit,
    onResume: (BookEntity) -> Unit,
    onExploreClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Emerald600,
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Emerald900.copy(alpha = 0.3f)
            )
            .testTag("hero_recent_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (recentBook != null) "RECENT ACTIVITY" else "ACADEMIC PORTAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$totalBooks Books Available",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Book action row
            if (recentBook != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recentBook.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.4).sp
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Page ${recentBook.lastReadPage} of ${recentBook.totalPages} • ${recentBook.subject} (Class ${recentBook.classLevel})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Emerald100,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Circular Resume button
                    IconButton(
                        onClick = { onResume(recentBook) },
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape, spotColor = Emerald900)
                            .clip(CircleShape)
                            .background(Color.White)
                            .testTag("resume_reading_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume Reading",
                            tint = Emerald600,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Physics Grade 10",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.4).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Chapter 3: Geometric Optics • PCTB Guide",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Emerald100,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = onExploreClick,
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(6.dp, CircleShape, spotColor = Emerald900)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Explore",
                            tint = Emerald600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick News Ticker inside hero
            Surface(
                onClick = onNewsClick,
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = latestNews,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickBookItemCard(
    book: BookEntity,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Emerald50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${book.provinceCode.uppercase()} • Class ${book.classLevel} • ${book.fileSize}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                )
            }
            if (book.isDownloaded) {
                IconButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Emerald50)
                ) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = "Read", tint = Emerald600, modifier = Modifier.size(18.dp))
                }
            } else {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Download", tint = Emerald600, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun GeometricBottomNavigation(
    viewModel: StudyViewModel,
    currentScreen: Screen
) {
    val unreadNewsCount by viewModel.unreadNewsCount.collectAsStateWithLifecycle()
    val hasUnreadNews = unreadNewsCount > 0

    Column(modifier = Modifier.fillMaxWidth()) {
        AdsterraBannerAd()
        Surface(
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GeometricNavItem(
                icon = Icons.Default.Home,
                label = "HOME",
                isSelected = currentScreen == Screen.HOME,
                onClick = { viewModel.navigateTo(Screen.HOME) },
                testTag = "bottom_nav_home"
            )

            GeometricNavItem(
                icon = Icons.Outlined.BookmarkBorder,
                label = "SAVED",
                isSelected = currentScreen == Screen.BOOKMARKS,
                onClick = { viewModel.navigateTo(Screen.BOOKMARKS) },
                testTag = "bottom_nav_saved"
            )

            GeometricNavItem(
                icon = Icons.Outlined.Campaign,
                label = "NEWS",
                isSelected = currentScreen == Screen.NEWS,
                onClick = { viewModel.navigateTo(Screen.NEWS) },
                testTag = "bottom_nav_news",
                showBadge = hasUnreadNews,
                badgeCount = unreadNewsCount
            )

            GeometricNavItem(
                icon = Icons.Outlined.Widgets,
                label = "MORE",
                isSelected = false,
                onClick = { viewModel.showMoreBottomSheet.value = true },
                testTag = "bottom_nav_more"
            )
        }
    }
}
}

@Composable
private fun GeometricNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    showBadge: Boolean = false,
    badgeCount: Int = 0
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Emerald600 else Slate400,
                modifier = Modifier.size(22.dp)
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-3).dp)
                        .size(if (badgeCount in 1..99) 16.dp else 8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)), // Alert badge
                    contentAlignment = Alignment.Center
                ) {
                    if (badgeCount in 1..99) {
                        Text(
                            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = if (isSelected) Emerald600 else Slate400
            )
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(Emerald600)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun FeaturedGeneralBookCard(
    book: BookEntity,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onCardClick: () -> Unit
) {
    val (themeColor, containerBg, icon) = when {
        book.subject.contains("Grammar", ignoreCase = true) -> Triple(Color(0xFF2563EB), Color(0xFFEFF6FF), Icons.Default.Spellcheck)
        book.subject.contains("Health", ignoreCase = true) -> Triple(Color(0xFFDC2626), Color(0xFFFEF2F2), Icons.Default.MedicalServices)
        book.subject.contains("Knowledge", ignoreCase = true) || book.subject.contains("Science", ignoreCase = true) -> Triple(Color(0xFFD97706), Color(0xFFFFFBEB), Icons.Default.Lightbulb)
        book.subject.contains("Computer", ignoreCase = true) -> Triple(Color(0xFF7C3AED), Color(0xFFF5F3FF), Icons.Default.Computer)
        book.subject.contains("Islamic", ignoreCase = true) -> Triple(Color(0xFF059669), Color(0xFFECFDF5), Icons.Default.Mosque)
        else -> Triple(Color(0xFF0D9488), Color(0xFFF0FDFA), Icons.Default.MenuBook)
    }

    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(220.dp)
            .testTag("featured_general_book_${book.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header with Icon and Subject Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    color = containerBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = book.subject.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Book Title
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${book.totalPages} Pages • ${book.fileSize}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Slate500,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("featured_read_btn_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Read", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (book.isDownloaded) {
                    Surface(
                        shape = CircleShape,
                        color = Emerald50,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = Emerald600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Slate100)
                            .testTag("featured_download_btn_${book.id}")
                    ) {
                        if (book.isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = themeColor
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.CloudDownload,
                                contentDescription = "Download",
                                tint = Slate700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
