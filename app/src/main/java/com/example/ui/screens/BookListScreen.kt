package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookEntity
import com.example.data.model.BookType
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(viewModel: StudyViewModel) {
    val selectedProvince by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val selectedClassLevel by viewModel.selectedClassLevel.collectAsStateWithLifecycle()
    val books by viewModel.currentClassBooks.collectAsStateWithLifecycle()
    val availableSubjects by viewModel.availableSubjects.collectAsStateWithLifecycle()
    val selectedSubject by viewModel.selectedSubjectFilter.collectAsStateWithLifecycle()
    val selectedBookType by viewModel.selectedBookTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showDetailsDialogForBook by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "Class $selectedClassLevel (${selectedProvince.title})",
                subtitle = "${selectedProvince.boardName} • ${books.size} Materials",
                showBackButton = true,
                onBackClick = { viewModel.navigateBack() },
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Geometric Search & Filter Container
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
                    placeholder = { Text("Search Math, Physics, Notes, Guides...", color = Slate400, fontSize = 14.sp) },
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
                        .testTag("book_list_search_field")
                )

                // Subject Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(availableSubjects) { subject ->
                        val isSelected = selectedSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedSubjectFilter.value = subject },
                            label = {
                                Text(
                                    subject,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald600,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Emerald600 else Slate200
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("subject_chip_$subject")
                        )
                    }
                }

                // Book Type Chips (All, Textbook, Guide & Keybook, Solved Notes, Past Papers)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedBookType == null,
                            onClick = { viewModel.selectedBookTypeFilter.value = null },
                            label = { Text("All Types", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Slate800,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedBookType == null,
                                borderColor = if (selectedBookType == null) Slate800 else Slate200
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    items(BookType.values()) { type ->
                        val isSelected = selectedBookType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.selectedBookTypeFilter.value = if (isSelected) null else type
                            },
                            label = { Text(type.displayName, fontSize = 12.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else type.badgeColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = type.badgeColor,
                                selectedLabelColor = Color.White,
                                containerColor = Slate100,
                                labelColor = Slate700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) type.badgeColor else Slate200
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate200)

            // Book List
            if (books.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Outlined.MenuBook,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No study material found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing filters or search for another subject",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.selectedSubjectFilter.value = "All"
                                viewModel.selectedBookTypeFilter.value = null
                                viewModel.searchQuery.value = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald600),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald600)
                        ) {
                            Text("Reset Filters", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        BookCardItem(
                            book = book,
                            provinceColor = selectedProvince.primaryColor,
                            onOpenReader = { viewModel.openReader(book) },
                            onDownload = { viewModel.downloadBook(book) },
                            onToggleBookmark = { viewModel.toggleBookmark(book) },
                            onShowDetails = { showDetailsDialogForBook = book }
                        )
                    }
                }
            }
        }
    }

    // Detail/Action Modal dialog if requested
    showDetailsDialogForBook?.let { book ->
        BookDetailsDialog(
            book = book,
            onDismiss = { showDetailsDialogForBook = null },
            onRead = {
                showDetailsDialogForBook = null
                viewModel.openReader(book)
            },
            onDownload = {
                showDetailsDialogForBook = null
                viewModel.downloadBook(book)
            }
        )
    }
}

@Composable
fun BookCardItem(
    book: BookEntity,
    provinceColor: Color,
    onOpenReader: () -> Unit,
    onDownload: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShowDetails: () -> Unit
) {
    val bookType = BookType.fromString(book.bookType)

    Surface(
        onClick = onOpenReader,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (book.isDownloaded) Emerald200 else Slate200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Slate300
            )
            .testTag("book_card_${book.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Book Cover / Subject Icon Box
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bookType.badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = when (book.subject.lowercase()) {
                                "mathematics", "maths" -> Icons.Default.Calculate
                                "physics" -> Icons.Default.Bolt
                                "chemistry" -> Icons.Default.Science
                                "biology" -> Icons.Default.Spa
                                "computer science", "computer" -> Icons.Default.Computer
                                "english" -> Icons.Default.Translate
                                "urdu" -> Icons.Default.MenuBook
                                "sindhi" -> Icons.Default.MenuBook
                                "pakistan studies" -> Icons.Default.Public
                                else -> Icons.Default.AutoStories
                            },
                            contentDescription = book.subject,
                            tint = bookType.badgeColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = book.subject.take(6),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = bookType.badgeColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bookType.badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = bookType.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = bookType.badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleBookmark,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Slate50)
                        ) {
                            Icon(
                                imageVector = if (book.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (book.isBookmarked) Color(0xFFEAB308) else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900,
                            letterSpacing = (-0.3).sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Size: ${book.fileSize}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                        )
                        Text(
                            text = "${book.totalPages} Pages",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate500,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // Download Progress if downloading
            if (book.isDownloading) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading to offline storage...",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Emerald600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            text = "${book.downloadProgress}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Emerald600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    LinearProgressIndicator(
                        progress = { book.downloadProgress / 100f },
                        color = Emerald600,
                        trackColor = Emerald100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: Open (Read) / Download
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Downloaded status indicator or download button
                if (book.isDownloaded) {
                    Surface(
                        color = Emerald50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Saved Offline",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Emerald800,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onDownload,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Emerald600
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald600),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_button_${book.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Read / Open Book Button
                Button(
                    onClick = onOpenReader,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Emerald600,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("read_button_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Open In-App Reader",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Read Now",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Info / Link icon button
                IconButton(
                    onClick = onShowDetails,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Book Details & Links",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BookDetailsDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = book.title, fontWeight = FontWeight.Bold, color = Slate900)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Subject: ${book.subject} • Class ${book.classLevel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
                Text(
                    text = "Province: ${book.provinceCode.uppercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
                Text(
                    text = "Type: ${book.bookType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
                Text(
                    text = "File Size: ${book.fileSize}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate700
                )
                Text(
                    text = "Direct Link: ${book.fileLink}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate500)
                )

                if (book.fileLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(book.fileLink)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4338CA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4338CA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Direct Link in Google Drive / Browser", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Admin Note: Files can be read in-app immediately or downloaded to internal app storage for 100% offline study without internet.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = Slate600
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
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Open Reader")
            }
        },
        dismissButton = {
            if (!book.isDownloaded) {
                OutlinedButton(
                    onClick = onDownload,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald600),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600)
                ) {
                    Text("Download PDF")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Slate600)
                }
            }
        }
    )
}
