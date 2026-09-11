package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.adsterra.AdsterraBannerAd
import com.example.adsterra.AdsterraManager
import com.example.data.local.BookEntity
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun GeneralBooksScreen(viewModel: StudyViewModel) {
    val generalBooks by viewModel.generalBooks.collectAsStateWithLifecycle()
    val availableSubjects by viewModel.generalSubjects.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Trigger Adsterra Popunder when opening General Books screen
    LaunchedEffect(Unit) {
        AdsterraManager.triggerPopunder(context)
    }

    var selectedBookForDialog by remember { mutableStateOf<BookEntity?>(null) }

    val filteredBooks = remember(generalBooks, selectedSubject, searchQuery) {
        generalBooks.filter { book ->
            val matchSubject = selectedSubject == null || selectedSubject == "All" || book.subject.equals(selectedSubject, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    book.title.contains(searchQuery, ignoreCase = true) ||
                    book.subject.contains(searchQuery, ignoreCase = true) ||
                    book.sampleContent.contains(searchQuery, ignoreCase = true)
            matchSubject && matchQuery
        }
    }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "General Books Library",
                subtitle = "Grammar, Health Care, GK & Skills",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                viewModel = viewModel
            )
        },
        bottomBar = {
            GeometricBottomNavigation(viewModel = viewModel, currentScreen = Screen.GENERAL_BOOKS)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search and Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = {
                        Text(
                            "Search Grammar, Health, GK, Science, Tenses...",
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
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Slate500,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("general_books_search_input")
                )

                // Subject Filter Tabs
                if (availableSubjects.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(availableSubjects) { subject ->
                            val isSelected = (selectedSubject == subject) || (selectedSubject == null && subject == "All")
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedSubjectFilter.value = if (subject == "All") "All" else subject },
                                label = {
                                    Text(
                                        text = subject,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald600,
                                    selectedLabelColor = Color.White,
                                    containerColor = Slate100,
                                    labelColor = Slate700
                                ),
                                border = null,
                                modifier = Modifier.testTag("filter_subject_$subject")
                            )
                        }
                    }
                }
            }

            // Books List or Empty State
            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Slate200),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No General Books Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try adjusting your search query or subject filters.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp)
                ) {
                    // Header Summary Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF0FDFA),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF99F6E4)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFF0D9488)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Essential Reference & Knowledge Library",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF115E59),
                                            fontSize = 14.sp
                                        )
                                    )
                                    Text(
                                        text = "Comprehensive grammar, emergency health care, and general knowledge for daily learning and competitive tests.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF134E4A),
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    items(filteredBooks, key = { it.id }) { book ->
                        GeneralBookCard(
                            book = book,
                            onOpen = { viewModel.openReader(book) },
                            onDownload = { viewModel.downloadBook(book) },
                            onDetails = { selectedBookForDialog = book },
                            onExternalLink = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.fileLink))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.uiMessage.tryEmit(com.example.ui.viewmodel.UiMessage("Cannot open browser link", true))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Details Dialog
        selectedBookForDialog?.let { book ->
            GeneralBookDetailDialog(
                book = book,
                onDismiss = { selectedBookForDialog = null },
                onRead = {
                    selectedBookForDialog = null
                    viewModel.openReader(book)
                },
                onDownload = {
                    selectedBookForDialog = null
                    viewModel.downloadBook(book)
                }
            )
        }
    }
}

@Composable
fun GeneralBookCard(
    book: BookEntity,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onDetails: () -> Unit,
    onExternalLink: () -> Unit
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
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("general_book_card_${book.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row: Icon, Title, Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = containerBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = book.subject.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (book.isDownloaded) {
                            Surface(
                                color = Emerald50,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald600,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Saved Offline",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Emerald700,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${book.totalPages} Pages • ${book.fileSize} • ${book.downloadCount}+ Reads",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate500,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action: Open Reader
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("read_general_book_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Read In-App",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                // Download Button
                OutlinedButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (book.isDownloaded) Emerald600 else Slate200),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (book.isDownloaded) Emerald50 else Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("download_general_book_${book.id}")
                ) {
                    if (book.isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = themeColor
                        )
                    } else {
                        Icon(
                            imageVector = if (book.isDownloaded) Icons.Default.Check else Icons.Outlined.CloudDownload,
                            contentDescription = "Download",
                            tint = if (book.isDownloaded) Emerald600 else Slate700,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Details Button
                IconButton(
                    onClick = onDetails,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                        .testTag("details_general_book_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Details",
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralBookDetailDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column {
                Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = book.subject.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Emerald700,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Pages:", color = Slate500, fontSize = 13.sp)
                    Text("${book.totalPages} Pages", fontWeight = FontWeight.SemiBold, color = Slate800, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("File Size:", color = Slate500, fontSize = 13.sp)
                    Text(book.fileSize, fontWeight = FontWeight.SemiBold, color = Slate800, fontSize = 13.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Category:", color = Slate500, fontSize = 13.sp)
                    Text("General Reference Guide", fontWeight = FontWeight.SemiBold, color = Slate800, fontSize = 13.sp)
                }

                HorizontalDivider(color = Slate200, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Structured Study Preview:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Slate700
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = book.sampleContent.take(220) + "...\n\n(Complete handbook available in In-App Reader)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Slate600,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRead,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Read Now", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Slate600)
            }
        }
    )
}
