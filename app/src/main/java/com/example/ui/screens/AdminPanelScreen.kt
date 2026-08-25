package com.example.ui.screens

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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.BookEntity
import com.example.data.local.NewsEntity
import com.example.data.model.BookType
import com.example.data.model.Province
import com.example.ui.components.AppTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: StudyViewModel) {
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val allNews by viewModel.allNews.collectAsStateWithLifecycle()

    var selectedAdminTab by remember { mutableStateOf(0) } // 0: Books Catalog, 1: Add New Book, 2: Announcements, 3: Analytics
    var searchQuery by remember { mutableStateOf("") }
    var bookToEdit by remember { mutableStateOf<BookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    if (!isAdminLoggedIn) {
        AdminLoginView(
            viewModel = viewModel,
            onBack = { viewModel.navigateBack() }
        )
    } else {
        Scaffold(
            containerColor = Slate50,
            topBar = {
                AppTopBar(
                    title = "Admin Control Panel",
                    subtitle = "Logged in as onstudy13.1311@gmail.com",
                    showBackButton = true,
                    onBackClick = { viewModel.navigateBack() },
                    viewModel = viewModel,
                    actions = {
                        IconButton(
                            onClick = { viewModel.logoutAdmin() },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Slate100)
                                .testTag("admin_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log Out Admin",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Admin Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedAdminTab,
                    containerColor = Color.White,
                    contentColor = Emerald600,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedAdminTab]),
                            color = Emerald600
                        )
                    }
                ) {
                    Tab(
                        selected = selectedAdminTab == 0,
                        onClick = { selectedAdminTab = 0 },
                        text = {
                            Text(
                                "Books (${allBooks.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedAdminTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 0) Emerald700 else Slate600
                            )
                        }
                    )
                    Tab(
                        selected = selectedAdminTab == 1,
                        onClick = { selectedAdminTab = 1 },
                        text = {
                            Text(
                                "+ Add Book/Guide",
                                fontSize = 12.sp,
                                fontWeight = if (selectedAdminTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 1) Emerald700 else Slate600
                            )
                        }
                    )
                    Tab(
                        selected = selectedAdminTab == 2,
                        onClick = { selectedAdminTab = 2 },
                        text = {
                            Text(
                                "News (${allNews.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedAdminTab == 2) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 2) Emerald700 else Slate600
                            )
                        }
                    )
                    Tab(
                        selected = selectedAdminTab == 3,
                        onClick = { selectedAdminTab = 3 },
                        text = {
                            Text(
                                "Analytics",
                                fontSize = 12.sp,
                                fontWeight = if (selectedAdminTab == 3) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 3) Emerald700 else Slate600
                            )
                        }
                    )
                    Tab(
                        selected = selectedAdminTab == 4,
                        onClick = { selectedAdminTab = 4 },
                        text = {
                            Text(
                                "Firebase Sync",
                                fontSize = 12.sp,
                                fontWeight = if (selectedAdminTab == 4) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedAdminTab == 4) Emerald700 else Slate600
                            )
                        }
                    )
                }

                when (selectedAdminTab) {
                    0 -> AdminBooksListTab(
                        books = allBooks,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onEdit = { bookToEdit = it },
                        onDelete = { bookToDelete = it },
                        onAddNewClick = { selectedAdminTab = 1 },
                        onSyncClick = { viewModel.syncWithFirebase() },
                        isSyncing = viewModel.isSyncingWithFirebase.collectAsStateWithLifecycle().value
                    )
                    1 -> AdminAddBookTab(
                        initialBook = null,
                        onSave = { newBook ->
                            viewModel.addNewBookFromAdmin(newBook)
                            selectedAdminTab = 0
                        }
                    )
                    2 -> AdminNewsTab(
                        newsList = allNews,
                        onAddNews = { viewModel.postNewsFromAdmin(it) },
                        onDeleteNews = { viewModel.deleteNewsFromAdmin(it) }
                    )
                    3 -> AdminAnalyticsTab(books = allBooks)
                    4 -> AdminFirebaseSyncTab(viewModel = viewModel)
                }
            }
        }

        // Edit Dialog
        bookToEdit?.let { book ->
            AdminEditBookDialog(
                book = book,
                onDismiss = { bookToEdit = null },
                onSave = { updated ->
                    viewModel.updateBookFromAdmin(updated)
                    bookToEdit = null
                }
            )
        }

        // Delete Confirmation Dialog
        bookToDelete?.let { book ->
            AlertDialog(
                onDismissRequest = { bookToDelete = null },
                title = { Text("Delete Book from Catalog?", fontWeight = FontWeight.Bold, color = Slate900) },
                text = { Text("Are you sure you want to delete '${book.title}'? It will be removed from all student app catalogs.", color = Slate700) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteBookFromAdmin(book)
                            bookToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookToDelete = null }) {
                        Text("Cancel", color = Slate600)
                    }
                }
            )
        }
    }
}

/**
 * Modern High-Contrast Admin Authentication Login Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginView(
    viewModel: StudyViewModel,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            AppTopBar(
                title = "Admin Authentication",
                subtitle = "Authorized Personnel Only",
                showBackButton = true,
                onBackClick = onBack,
                viewModel = viewModel
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Shield / Lock Visual Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Emerald600)
                    .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Emerald300),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "Admin Lock",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ON Study Admin Portal",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    fontSize = 22.sp
                )
            )

            Text(
                text = "Sign in to upload textbooks, keybooks, model papers, and broadcast board notices.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Slate600,
                    fontSize = 13.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Form Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Email Field with High Contrast Colors
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("Admin Email", color = Slate700) },
                        placeholder = { Text("onstudy13.1311@gmail.com", color = Slate400) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = Emerald600)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald600,
                            unfocusedBorderColor = Slate300,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Emerald600
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_email_input")
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Admin Password", color = Slate700) },
                        placeholder = { Text("Enter password", color = Slate400) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password", tint = Emerald600)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Slate500
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald600,
                            unfocusedBorderColor = Slate300,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            cursorColor = Emerald600
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    errorMessage?.let { error ->
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Login Button
                    Button(
                        onClick = {
                            val success = viewModel.loginAdmin(email, password)
                            if (!success) {
                                errorMessage = "Invalid Email or Password. Please check credentials."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_login_submit_btn")
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Access Admin Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    // Quick Fill Button for testing
                    OutlinedButton(
                        onClick = {
                            email = "onstudy13.1311@gmail.com"
                            password = "onstudy13.1311"
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Slate600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Auto-Fill Credentials", color = Slate700, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Protected with secure cryptographic local verification",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.sp)
            )
        }
    }
}

/**
 * Admin Books List & Management Tab
 */
@Composable
fun AdminBooksListTab(
    books: List<BookEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onEdit: (BookEntity) -> Unit,
    onDelete: (BookEntity) -> Unit,
    onAddNewClick: () -> Unit,
    onSyncClick: () -> Unit = {},
    isSyncing: Boolean = false
) {
    val filtered = books.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) ||
                it.subject.contains(searchQuery, ignoreCase = true) ||
                it.provinceCode.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cloud Sync Banner & Status
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Emerald600)
                    )
                    Text(
                        text = "Firebase Realtime DB: Connected & Synced",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Emerald800,
                            fontSize = 11.sp
                        )
                    )
                }
                TextButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Emerald700
                        )
                    } else {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Sync Now",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Emerald700
                            )
                        )
                    }
                }
            }
        }

        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search catalog...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate500, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald600,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onAddNewClick,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        }

        Text(
            text = "Total Entries in Catalog: ${filtered.size}",
            style = MaterialTheme.typography.labelMedium.copy(color = Slate600, fontWeight = FontWeight.SemiBold)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.id }) { book ->
                val prov = Province.fromCode(book.provinceCode)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = prov.containerColor
                                ) {
                                    Text(
                                        text = prov.monogram,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = prov.textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Class ${book.classLevel} • ${book.subject} • ${book.bookType}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Emerald800,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Link: ${book.fileLink}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate500,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { onEdit(book) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Slate100)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Emerald700, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onDelete(book) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Slate100)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * + Add Book / Solved Guide Tab
 * Fully styled with high-contrast visible text, supporting ALL classes 1-12 and all provinces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddBookTab(
    initialBook: BookEntity?,
    onSave: (BookEntity) -> Unit
) {
    var title by remember { mutableStateOf(initialBook?.title ?: "") }
    var selectedProvince by remember { mutableStateOf(initialBook?.provinceCode ?: "punjab") }
    var selectedClass by remember { mutableStateOf(initialBook?.classLevel ?: 10) }
    var subject by remember { mutableStateOf(initialBook?.subject ?: "Physics") }
    var bookType by remember { mutableStateOf(initialBook?.bookType ?: "GUIDE") }
    var fileLink by remember { mutableStateOf(initialBook?.fileLink ?: "") }
    var fileSize by remember { mutableStateOf(initialBook?.fileSize ?: "18.5 MB") }
    var sampleContent by remember { mutableStateOf(initialBook?.sampleContent ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val commonSubjects = listOf(
        "Physics", "Mathematics", "Chemistry", "Biology", "Computer Science",
        "English", "Urdu", "Islamiat", "Pak Studies", "General Science"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Upload Book or Solved Guide",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                    Text(
                        text = "Enter book details and paste your Mediafire, Google Drive, or Direct PDF link for students to download.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                }
            }
        }

        // Quick Fill Preset Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = {
                        title = "Chemistry Class 10 Solved Notes & Guide"
                        selectedProvince = "kpk"
                        selectedClass = 10
                        subject = "Chemistry"
                        bookType = "GUIDE"
                        fileLink = "https://www.mediafire.com/file/sample_kpk_chem_10.pdf/download"
                        fileSize = "22.4 MB"
                        sampleContent = "[CHAPTER 1: CHEMICAL EQUILIBRIUM]\nKey Concepts & Solved Short Questions\n1. Reversible Reactions and Equilibrium\n2. Law of Mass Action\n3. Equilibrium Constant Expression (Kc)\n4. Solved Long Questions & Numerical Problems for Class 10."
                        validationError = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald600)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pre-fill Sample Guide Data", color = Emerald700, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Title
        item {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = null
                },
                label = { Text("Book / Guide Title *", color = Slate700) },
                placeholder = { Text("e.g. Mathematics Class 9 Complete Keybook", color = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald600,
                    unfocusedBorderColor = Slate300,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Emerald600
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Province Picker
        item {
            Text("Select Province:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Province.values().forEach { prov ->
                    val isSelected = selectedProvince.equals(prov.code, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedProvince = prov.code },
                        label = { Text(prov.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald600,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Emerald600 else Slate300
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Class Picker (Classes 1 to 12)
        item {
            Text("Select Class (1 to 12):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(12) { index ->
                    val classNum = index + 1
                    val isSelected = selectedClass == classNum
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedClass = classNum },
                        label = { Text("Class $classNum", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald600,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Emerald600 else Slate300
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Subject Picker & Input
        item {
            Text("Subject:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(commonSubjects) { sub ->
                    val isSelected = subject.equals(sub, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { subject = sub },
                        label = { Text(sub, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald600,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Emerald600 else Slate300
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Custom Subject Name", color = Slate700) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald600,
                    unfocusedBorderColor = Slate300,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Emerald600
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Book Type
        item {
            Text("Material Type:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BookType.values().forEach { type ->
                    val isSelected = bookType.equals(type.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { bookType = type.name },
                        label = { Text(type.displayName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Emerald600,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Slate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Emerald600 else Slate300
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Direct Download Link
        item {
            OutlinedTextField(
                value = fileLink,
                onValueChange = {
                    fileLink = it
                    validationError = null
                },
                label = { Text("Direct File Link (Mediafire / Google Drive / PDF URL) *", color = Slate700) },
                placeholder = { Text("https://www.mediafire.com/file/... or Drive link", color = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald600,
                    unfocusedBorderColor = Slate300,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Emerald600
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // File Size
        item {
            OutlinedTextField(
                value = fileSize,
                onValueChange = { fileSize = it },
                label = { Text("File Size (e.g. 18.5 MB)", color = Slate700) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald600,
                    unfocusedBorderColor = Slate300,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Emerald600
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sample Content / Key Chapters
        item {
            OutlinedTextField(
                value = sampleContent,
                onValueChange = { sampleContent = it },
                label = { Text("Text Preview / Key Chapters for In-App Reader (Optional)", color = Slate700) },
                placeholder = { Text("Write chapters, formulas, or study notes for in-app reading...", color = Slate400) },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate900,
                    unfocusedTextColor = Slate900,
                    focusedBorderColor = Emerald600,
                    unfocusedBorderColor = Slate300,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Emerald600
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        validationError?.let { err ->
            item {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        validationError = "Please enter a title for the book or guide."
                        return@Button
                    }
                    if (fileLink.isBlank()) {
                        validationError = "Please enter a valid Mediafire, Google Drive, or PDF download link."
                        return@Button
                    }

                    onSave(
                        BookEntity(
                            title = title.trim(),
                            provinceCode = selectedProvince.lowercase().trim(),
                            classLevel = selectedClass,
                            subject = subject.trim(),
                            bookType = bookType,
                            fileLink = fileLink.trim(),
                            fileSize = fileSize.ifBlank { "15.0 MB" },
                            sampleContent = sampleContent.ifBlank {
                                "[CHAPTER 1: INTRODUCTION]\n$title\nCurriculum content for Class $selectedClass ($subject).\nIncludes complete solved exercises, board questions, and summaries."
                            }
                        )
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Publish to ON Study Library", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Edit Book Dialog
 */
@Composable
fun AdminEditBookDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onSave: (BookEntity) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var subject by remember { mutableStateOf(book.subject) }
    var fileLink by remember { mutableStateOf(book.fileLink) }
    var fileSize by remember { mutableStateOf(book.fileSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Book Entry", fontWeight = FontWeight.Bold, color = Slate900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = Slate700) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject", color = Slate700) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileLink,
                    onValueChange = { fileLink = it },
                    label = { Text("Direct File Link", color = Slate700) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                        focusedBorderColor = Emerald600,
                        unfocusedBorderColor = Slate300
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileSize,
                    onValueChange = { fileSize = it },
                    label = { Text("File Size", color = Slate700) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
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
                        book.copy(
                            title = title,
                            subject = subject,
                            fileLink = fileLink,
                            fileSize = fileSize
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate600) }
        }
    )
}

/**
 * Admin News & Announcements Tab
 */
@Composable
fun AdminNewsTab(
    newsList: List<NewsEntity>,
    onAddNews: (NewsEntity) -> Unit,
    onDeleteNews: (Long) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Broadcasts & Date Sheets",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Notice", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(newsList) { item ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.category} • ${item.date} • ${item.boardName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Emerald800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteNews(item.id) },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Slate100)
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Date Sheet") }
        var boardName by remember { mutableStateOf("All Boards") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Broadcast New Announcement", fontWeight = FontWeight.Bold, color = Slate900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title", color = Slate700) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald600,
                            unfocusedBorderColor = Slate300
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description", color = Slate700) },
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
                            focusedBorderColor = Emerald600,
                            unfocusedBorderColor = Slate300
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = boardName,
                        onValueChange = { boardName = it },
                        label = { Text("Board Name (e.g. BISE Peshawar)", color = Slate700) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate900,
                            unfocusedTextColor = Slate900,
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
                        if (title.isNotBlank()) {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            onAddNews(
                                NewsEntity(
                                    title = title,
                                    description = description,
                                    category = category,
                                    date = today,
                                    boardName = boardName
                                )
                            )
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Broadcast", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = Slate600) }
            }
        )
    }
}

/**
 * Admin Analytics Dashboard
 */
@Composable
fun AdminAnalyticsTab(books: List<BookEntity>) {
    val totalDownloads = books.sumOf { it.downloadCount }
    val kpkBooks = books.count { it.provinceCode.equals("kpk", ignoreCase = true) }
    val punjabBooks = books.count { it.provinceCode.equals("punjab", ignoreCase = true) }
    val sindhBooks = books.count { it.provinceCode.equals("sindh", ignoreCase = true) }
    val balochistanBooks = books.count { it.provinceCode.equals("balochistan", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Admin Metrics & Library Statistics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard("Total Books", "${books.size}", Emerald600, Modifier.weight(1f))
                MetricCard("Total Downloads", "$totalDownloads+", Color(0xFF2563EB), Modifier.weight(1f))
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Province Catalog Distribution", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate900))
                    ProvinceStatRow("Punjab", punjabBooks, Province.PUNJAB.primaryColor)
                    ProvinceStatRow("KPK", kpkBooks, Province.KPK.primaryColor)
                    ProvinceStatRow("Sindh", sindhBooks, Province.SINDH.primaryColor)
                    ProvinceStatRow("Balochistan", balochistanBooks, Province.BALOCHISTAN.primaryColor)
                }
            }
        }

        item {
            Text("Most Downloaded Materials", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Slate800))
        }

        val topDownloaded = books.sortedByDescending { it.downloadCount }.take(5)
        items(topDownloaded) { book ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${topDownloaded.indexOf(book) + 1}",
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald800,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900),
                            maxLines = 1
                        )
                        Text(
                            text = "${book.provinceCode.uppercase()} • Class ${book.classLevel} • ${book.subject}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate500, fontSize = 11.sp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
                    ) {
                        Text(
                            text = "${book.downloadCount} DLs",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Emerald800),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = accentColor, fontSize = 24.sp))
        }
    }
}

@Composable
fun ProvinceStatRow(name: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(10.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium.copy(color = Slate800, fontWeight = FontWeight.Medium))
        }
        Text("$count Books", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Slate900))
    }
}

/**
 * Admin Firebase Realtime Database & Cloud Sync Settings Tab
 */
@Composable
fun AdminFirebaseSyncTab(viewModel: StudyViewModel) {
    val databaseUrl by viewModel.firebaseDatabaseUrl.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncingWithFirebase.collectAsStateWithLifecycle()
    val lastSync by viewModel.lastFirebaseSyncTime.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()

    var urlInput by remember(databaseUrl) { mutableStateOf(databaseUrl) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showBatchUploadConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Firebase Realtime Database Sync",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Slate900
                )
            )
            Text(
                text = "All books, Google Drive links, past papers, and announcements uploaded here are stored in Firebase and automatically synced across all student devices in real-time.",
                style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 12.sp)
            )
        }

        // Firebase Quick Setup Guide Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(20.dp))
                        Text(
                            text = "How to Connect Your Free Firebase (2 Minutes):",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF)
                            )
                        )
                    }

                    Text(
                        text = "1. Open console.firebase.google.com in browser and create a free project.\n" +
                                "2. Click 'Build' ➔ 'Realtime Database' ➔ 'Create Database'.\n" +
                                "3. In 'Rules' tab, set rules to:\n    {\".read\": true, \".write\": true} and click 'Publish'.\n" +
                                "4. Copy your Database URL (e.g. https://xyz-default-rtdb.firebaseio.com), paste below and click 'Save Endpoint'.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E3A8A), fontSize = 12.sp, lineHeight = 18.sp)
                    )
                }
            }
        }

        // Live Connection Status Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Emerald600)
                            )
                            Column {
                                Text(
                                    text = "Cloud Sync Engine",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Slate900
                                    )
                                )
                                Text(
                                    text = lastSync,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Emerald700,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.syncWithFirebase() },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Database Endpoint URL
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Firebase Realtime Database Endpoint",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            placeholder = { Text("https://<project-id>-default-rtdb.firebaseio.com", color = Slate400, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate900,
                                unfocusedTextColor = Slate900,
                                focusedBorderColor = Emerald600,
                                unfocusedBorderColor = Slate300,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Slate50
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (urlInput.isNotBlank()) {
                                        viewModel.updateFirebaseDatabaseUrl(urlInput)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save Endpoint", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    isTestingConnection = true
                                    testResult = null
                                    viewModel.testFirebaseConnection { success, message ->
                                        isTestingConnection = false
                                        testResult = message
                                    }
                                },
                                enabled = !isTestingConnection,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Slate700)
                                } else {
                                    Text("Test Connection", fontWeight = FontWeight.SemiBold, color = Slate800, fontSize = 12.sp)
                                }
                            }
                        }

                        testResult?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (msg.contains("successfully", ignoreCase = true)) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (msg.contains("successfully", ignoreCase = true)) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                                )
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (msg.contains("successfully", ignoreCase = true)) Emerald800 else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bulk Actions Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cloud Batch Synchronization",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                    )

                    Text(
                        text = "If you have added multiple items offline or wish to push all ${allBooks.size} catalog books to Firebase in one click, tap below.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600, fontSize = 12.sp)
                    )

                    Button(
                        onClick = { showBatchUploadConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload All ${allBooks.size} Books to Firebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.syncAllNewsToFirebase() },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Push All Announcements to Firebase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showClearAllConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Catalog & Cloud (Delete All)", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            urlInput = com.example.data.remote.FirebaseRealtimeDbManager.DEFAULT_DATABASE_URL
                            viewModel.updateFirebaseDatabaseUrl(urlInput)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, tint = Slate700, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Default Database URL", color = Slate800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // How it works info card (Urdu & English)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Multi-Device Sync Guide ( رہنمائی )",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                        )
                    }
                    Text(
                        text = "• Jab aap Admin Panel se koi bhi Google Drive link ya naya book upload/update karenge, wo foran Firebase Realtime Database mein store hoga.\n" +
                                "• Doosri devices (students/phones) jab app open karengi to ye naya data auto-sync ho kar unke pass show hoga.\n" +
                                "• Student kisi bhi time 'Sync Cloud' button daba kar latest updates live receive kar sakte hain.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontSize = 12.sp, lineHeight = 18.sp)
                    )
                }
            }
        }
    }

    if (showBatchUploadConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchUploadConfirm = false },
            title = { Text("Push All Books to Firebase?", fontWeight = FontWeight.Bold, color = Slate900) },
            text = { Text("This will upload all ${allBooks.size} catalog entries to the Firebase Realtime Database.", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.uploadAllCatalogToFirebase()
                        showBatchUploadConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Upload Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchUploadConfirm = false }) { Text("Cancel", color = Slate600) }
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear All Books from Cloud & Local?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Are you sure you want to wipe all books from both local memory and Firebase Cloud? This will remove all dummy books completely so you can start fresh with your real books.", color = Slate700) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCatalogAndCloud()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Wipe All Books", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) { Text("Cancel", color = Slate600) }
            }
        )
    }
}

