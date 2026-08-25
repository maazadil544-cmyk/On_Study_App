package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.model.BookType
import com.example.data.model.Province
import com.example.data.repository.StudyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    LOGIN,            // First-time & returning authentication screen
    HOME,             // Province Selection
    CLASS_SELECT,     // Class 1 to 12
    BOOK_LIST,        // Books & Guides for Province + Class
    READER,           // In-App PDF / Study Reader
    BOOKMARKS,        // Bookmarks Hub
    NOTES,            // Notes Hub
    NEWS,             // News & Announcements
    SETTINGS,         // Settings & Storage
    ADMIN_PANEL       // Admin Content Management
}

enum class ReaderThemeMode {
    LIGHT,
    SEPIA,
    DARK
}

data class UiMessage(val text: String, val isError: Boolean = false)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository
    private val prefs = application.getSharedPreferences("onstudy_session_prefs", Context.MODE_PRIVATE)

    // Navigation and screen state
    val currentScreen = MutableStateFlow(Screen.SPLASH)
    val screenBackStack = mutableListOf<Screen>()

    // User Authentication state
    val isUserLoggedIn = MutableStateFlow(false)
    val loggedInUsername = MutableStateFlow("")
    val loggedInEmail = MutableStateFlow("")

    // Selection filters
    val selectedProvince = MutableStateFlow(Province.PUNJAB)
    val selectedClassLevel = MutableStateFlow(10)
    val selectedSubjectFilter = MutableStateFlow<String?>("All")
    val selectedBookTypeFilter = MutableStateFlow<BookType?>(null)
    val searchQuery = MutableStateFlow("")

    // Active Reader State
    val activeReaderBook = MutableStateFlow<BookEntity?>(null)
    val readerThemeMode = MutableStateFlow(ReaderThemeMode.LIGHT)
    val readerFontSize = MutableStateFlow(16f)
    val readerCurrentPage = MutableStateFlow(1)

    // Admin & UI Sheets
    val isAdminLoggedIn = MutableStateFlow(false)
    val isAdminMode = MutableStateFlow(false)
    val showMoreBottomSheet = MutableStateFlow(false)
    val currentMoreTab = MutableStateFlow(0) // 0: Bookmarks, 1: Notes, 2: News, 3: Settings

    // Storage info
    val storageUsedMb = MutableStateFlow("0.0 MB")

    // Network & Connectivity State
    val isOnline: StateFlow<Boolean> = com.example.data.util.NetworkUtils.observeNetworkState(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.example.data.util.NetworkUtils.isOnline(application))

    // Firebase Sync State
    val isSyncingWithFirebase = MutableStateFlow(false)
    val lastFirebaseSyncTime = MutableStateFlow("Auto-sync enabled")
    val firebaseDatabaseUrl = MutableStateFlow(com.example.data.remote.FirebaseRealtimeDbManager.DEFAULT_DATABASE_URL)

    // Notifications / Snackbars
    val uiMessage = MutableSharedFlow<UiMessage>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyRepository(application, database.appDao())
        firebaseDatabaseUrl.value = repository.firebaseManager.databaseUrl

        // Restore saved session if any
        val savedLoggedIn = prefs.getBoolean(PREF_IS_LOGGED_IN, false)
        val savedAdmin = prefs.getBoolean(PREF_IS_ADMIN, false)
        val savedUsername = prefs.getString(PREF_USERNAME, "") ?: ""
        val savedEmail = prefs.getString(PREF_EMAIL, "") ?: ""

        isUserLoggedIn.value = savedLoggedIn
        isAdminLoggedIn.value = savedAdmin
        loggedInUsername.value = savedUsername
        loggedInEmail.value = savedEmail

        viewModelScope.launch {
            repository.initializeDataIfNeeded()
            updateStorageUsage()
            if (com.example.data.util.NetworkUtils.isOnline(application)) {
                syncWithFirebase(silent = true)
            }
        }

        // Auto-sync when internet reconnects
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    syncWithFirebase(silent = true)
                }
            }
        }
    }

    // Repository Flows
    val allBooks: StateFlow<List<BookEntity>> = repository.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedBooks: StateFlow<List<BookEntity>> = repository.getDownloadedBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentReads: StateFlow<List<BookEntity>> = repository.getRecentReads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookmarks: StateFlow<List<BookmarkEntity>> = repository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNews: StateFlow<List<NewsEntity>> = repository.getAllNews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNewsCount: StateFlow<Int> = repository.getUnreadNewsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private data class FilterCriteria(
        val province: Province,
        val classLevel: Int,
        val subject: String?,
        val bookType: BookType?,
        val query: String
    )

    private val filterCriteria = combine(
        selectedProvince,
        selectedClassLevel,
        selectedSubjectFilter,
        selectedBookTypeFilter,
        searchQuery
    ) { prov, cls, subject, type, query ->
        FilterCriteria(prov, cls, subject, type, query)
    }

    // Filtered books for current province & class
    val currentClassBooks: StateFlow<List<BookEntity>> = combine(
        allBooks,
        filterCriteria
    ) { books, filter ->
        books.filter { book ->
            val matchProvince = book.provinceCode.equals(filter.province.code, ignoreCase = true)
            val matchClass = book.classLevel == filter.classLevel
            val matchSubject = filter.subject == null || filter.subject == "All" || book.subject.equals(filter.subject, ignoreCase = true)
            val matchType = filter.bookType == null || book.bookType.equals(filter.bookType.name, ignoreCase = true)
            val matchQuery = filter.query.isBlank() || book.title.contains(filter.query, ignoreCase = true) || book.subject.contains(filter.query, ignoreCase = true)

            matchProvince && matchClass && matchSubject && matchType && matchQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unique subjects list for filter chips
    val availableSubjects: StateFlow<List<String>> = combine(allBooks, selectedProvince, selectedClassLevel) { books, prov, cls ->
        val subjects = books.filter { it.provinceCode.equals(prov.code, ignoreCase = true) && it.classLevel == cls }
            .map { it.subject }
            .distinct()
            .sorted()
        listOf("All") + subjects
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // Navigation Methods
    fun navigateTo(screen: Screen, addToBackStack: Boolean = true) {
        if (addToBackStack && currentScreen.value != screen) {
            screenBackStack.add(currentScreen.value)
        }
        currentScreen.value = screen
        showMoreBottomSheet.value = false
    }

    fun navigateBack(): Boolean {
        if (screenBackStack.isNotEmpty()) {
            val previous = screenBackStack.removeAt(screenBackStack.size - 1)
            currentScreen.value = previous
            return true
        }
        return false
    }

    fun selectProvince(province: Province) {
        selectedProvince.value = province
        navigateTo(Screen.CLASS_SELECT)
    }

    fun selectClass(classLevel: Int) {
        selectedClassLevel.value = classLevel
        selectedSubjectFilter.value = "All"
        selectedBookTypeFilter.value = null
        searchQuery.value = ""
        navigateTo(Screen.BOOK_LIST)
    }

    fun openReader(book: BookEntity, page: Int = 1) {
        activeReaderBook.value = book
        readerCurrentPage.value = if (page > 0) page else (if (book.lastReadPage > 0) book.lastReadPage else 1)
        navigateTo(Screen.READER)
        viewModelScope.launch {
            repository.recordReadingProgress(book.id, readerCurrentPage.value)
            val localPath = book.localFilePath
            val fileExists = !localPath.isNullOrBlank() && java.io.File(localPath).exists() && java.io.File(localPath).length() > 500
            if (!book.isDownloaded || !fileExists) {
                val success = repository.downloadBook(book)
                val refreshed = repository.getBookById(book.id)
                if (refreshed != null) {
                    activeReaderBook.value = refreshed
                }
            }
        }
    }

    fun getEmbedViewUrl(rawUrl: String): String = repository.resolveEmbedViewUrl(rawUrl)

    // Actions
    fun downloadBook(book: BookEntity) {
        viewModelScope.launch {
            uiMessage.emit(UiMessage("Downloading '${book.title}' for offline reading..."))
            val success = repository.downloadBook(book)
            updateStorageUsage()
            val refreshed = repository.getBookById(book.id)
            if (refreshed != null && activeReaderBook.value?.id == book.id) {
                activeReaderBook.value = refreshed
            }
            if (success) {
                val pages = refreshed?.totalPages ?: book.totalPages
                uiMessage.emit(UiMessage("Downloaded '${book.title}' ($pages pages) successfully! Ready offline."))
            } else {
                uiMessage.emit(
                    UiMessage(
                        "Could not download '${book.title}'. Please ensure the Google Drive file permission is set to 'Anyone with the link' and check your internet.",
                        isError = true
                    )
                )
            }
        }
    }

    fun removeDownload(book: BookEntity) {
        viewModelScope.launch {
            repository.removeDownloadedFile(book)
            updateStorageUsage()
            uiMessage.emit(UiMessage("Removed download for '${book.title}'"))
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
            updateStorageUsage()
            uiMessage.emit(UiMessage("All downloaded books cleared from storage."))
        }
    }

    fun updateStorageUsage() {
        viewModelScope.launch {
            val bytes = repository.getTotalStorageUsedBytes()
            val mb = bytes / (1024.0 * 1024.0)
            storageUsedMb.value = String.format("%.2f MB", mb)
        }
    }

    fun toggleBookmark(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleBookBookmark(book.id, book.isBookmarked)
            if (!book.isBookmarked) {
                repository.addBookmark(
                    BookmarkEntity(
                        bookId = book.id,
                        bookTitle = book.title,
                        subject = book.subject,
                        classLevel = book.classLevel,
                        provinceCode = book.provinceCode,
                        pageNumber = 1,
                        chapterTitle = "Chapter 1",
                        noteSnippet = "Bookmarked from catalog"
                    )
                )
                uiMessage.emit(UiMessage("Added to Bookmarks"))
            } else {
                repository.removeBookmarkForPage(book.id, 1)
                uiMessage.emit(UiMessage("Removed from Bookmarks"))
            }
        }
    }

    fun addPageBookmark(book: BookEntity, pageNumber: Int, chapterTitle: String, noteSnippet: String = "") {
        viewModelScope.launch {
            repository.addBookmark(
                BookmarkEntity(
                    bookId = book.id,
                    bookTitle = book.title,
                    subject = book.subject,
                    classLevel = book.classLevel,
                    provinceCode = book.provinceCode,
                    pageNumber = pageNumber,
                    chapterTitle = chapterTitle,
                    noteSnippet = noteSnippet
                )
            )
            uiMessage.emit(UiMessage("Page $pageNumber bookmarked!"))
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
            uiMessage.emit(UiMessage("Bookmark deleted"))
        }
    }

    fun saveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.saveNote(note)
            uiMessage.emit(UiMessage("Note saved successfully!"))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repository.deleteNote(id)
            uiMessage.emit(UiMessage("Note deleted"))
        }
    }

    fun markNewsAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNewsAsRead(id)
        }
    }

    fun markAllNewsAsRead() {
        viewModelScope.launch {
            repository.markAllNewsAsRead()
            uiMessage.emit(UiMessage("All updates marked as read"))
        }
    }

    // Admin & User Authentication
    companion object {
        private const val PREF_IS_LOGGED_IN = "pref_is_logged_in"
        private const val PREF_IS_ADMIN = "pref_is_admin"
        private const val PREF_USERNAME = "pref_username"
        private const val PREF_EMAIL = "pref_email"

        const val ADMIN_USERNAME = "onstudy13"
        const val ADMIN_EMAIL = "onstudy13.1311@gmail.com"
        const val ADMIN_PASSWORD = "onstudy13.1311"
    }

    private fun saveSession(isLoggedIn: Boolean, isAdmin: Boolean, username: String, email: String) {
        prefs.edit()
            .putBoolean(PREF_IS_LOGGED_IN, isLoggedIn)
            .putBoolean(PREF_IS_ADMIN, isAdmin)
            .putString(PREF_USERNAME, username)
            .putString(PREF_EMAIL, email)
            .apply()
    }

    private fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun loginUser(username: String, email: String, pass: String): Boolean {
        val u = username.trim()
        val e = email.trim()
        val p = pass.trim()

        if (u.isBlank() || e.isBlank() || p.isBlank()) {
            viewModelScope.launch {
                uiMessage.emit(UiMessage("Please enter Username, Email, and Password.", isError = true))
            }
            return false
        }

        // Check Admin Credentials
        if (u.equals(ADMIN_USERNAME, ignoreCase = true) &&
            e.equals(ADMIN_EMAIL, ignoreCase = true) &&
            p == ADMIN_PASSWORD
        ) {
            isAdminLoggedIn.value = true
            isUserLoggedIn.value = true
            loggedInUsername.value = "Admin"
            loggedInEmail.value = e
            saveSession(isLoggedIn = true, isAdmin = true, username = u, email = e)
            navigateTo(Screen.ADMIN_PANEL, addToBackStack = false)
            viewModelScope.launch {
                uiMessage.emit(UiMessage("Access granted."))
            }
            return true
        }

        // Regular Student Login
        isAdminLoggedIn.value = false
        isUserLoggedIn.value = true
        loggedInUsername.value = u
        loggedInEmail.value = e
        saveSession(isLoggedIn = true, isAdmin = false, username = u, email = e)
        navigateTo(Screen.HOME, addToBackStack = false)
        viewModelScope.launch {
            uiMessage.emit(UiMessage("Welcome, $u!"))
        }
        return true
    }

    fun loginAdmin(email: String, pass: String): Boolean {
        val trimmedEmail = email.trim()
        val trimmedPass = pass.trim()
        if (trimmedEmail.equals(ADMIN_EMAIL, ignoreCase = true) && trimmedPass == ADMIN_PASSWORD) {
            isAdminLoggedIn.value = true
            isUserLoggedIn.value = true
            loggedInUsername.value = "Admin"
            loggedInEmail.value = trimmedEmail
            saveSession(isLoggedIn = true, isAdmin = true, username = ADMIN_USERNAME, email = trimmedEmail)
            viewModelScope.launch {
                uiMessage.emit(UiMessage("Access granted."))
            }
            return true
        } else {
            viewModelScope.launch {
                uiMessage.emit(UiMessage("Invalid credentials. Access denied.", isError = true))
            }
            return false
        }
    }

    fun logoutAdmin() {
        isAdminLoggedIn.value = false
        isUserLoggedIn.value = false
        loggedInUsername.value = ""
        loggedInEmail.value = ""
        clearSession()
        navigateTo(Screen.LOGIN, addToBackStack = false)
        viewModelScope.launch {
            uiMessage.emit(UiMessage("Logged out."))
        }
    }

    fun logoutUser() {
        isAdminLoggedIn.value = false
        isUserLoggedIn.value = false
        loggedInUsername.value = ""
        loggedInEmail.value = ""
        clearSession()
        navigateTo(Screen.LOGIN, addToBackStack = false)
        viewModelScope.launch {
            uiMessage.emit(UiMessage("Signed out."))
        }
    }

    fun addNewBookFromAdmin(book: BookEntity) {
        viewModelScope.launch {
            repository.addNewBook(book)
            uiMessage.emit(UiMessage("Book '${book.title}' saved & synced to Firebase Realtime Database!"))
        }
    }

    fun updateBookFromAdmin(book: BookEntity) {
        viewModelScope.launch {
            repository.updateBook(book)
            uiMessage.emit(UiMessage("Book updated & synced across all devices!"))
        }
    }

    fun deleteBookFromAdmin(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            uiMessage.emit(UiMessage("Book deleted from Firebase & local catalog"))
        }
    }

    fun postNewsFromAdmin(news: NewsEntity) {
        viewModelScope.launch {
            repository.addNews(news)
            uiMessage.emit(UiMessage("Announcement posted & synced to all devices!"))
        }
    }

    fun deleteNewsFromAdmin(id: Long) {
        viewModelScope.launch {
            repository.deleteNews(id)
            uiMessage.emit(UiMessage("Announcement deleted from Firebase"))
        }
    }

    fun syncWithFirebase(silent: Boolean = false) {
        viewModelScope.launch {
            isSyncingWithFirebase.value = true
            val result = repository.syncWithFirebase()
            isSyncingWithFirebase.value = false
            val time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            lastFirebaseSyncTime.value = "Synced at $time"
            if (!silent) {
                uiMessage.emit(
                    UiMessage(
                        result.message,
                        isError = !result.success
                    )
                )
            }
        }
    }

    fun updateFirebaseDatabaseUrl(newUrl: String) {
        repository.firebaseManager.databaseUrl = newUrl
        firebaseDatabaseUrl.value = repository.firebaseManager.databaseUrl
        viewModelScope.launch {
            uiMessage.emit(UiMessage("Firebase Database URL updated!"))
            syncWithFirebase()
        }
    }

    fun testFirebaseConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.firebaseManager.testConnection()
            onResult(result.first, result.second)
        }
    }

    fun uploadAllCatalogToFirebase() {
        viewModelScope.launch {
            isSyncingWithFirebase.value = true
            val currentBooks = allBooks.value
            val success = repository.firebaseManager.uploadAllBooksBatch(currentBooks)
            isSyncingWithFirebase.value = false
            if (success) {
                uiMessage.emit(UiMessage("Uploaded all ${currentBooks.size} books to Firebase Realtime Database!"))
            } else {
                uiMessage.emit(UiMessage("Failed to upload batch to Firebase.", isError = true))
            }
        }
    }

    fun syncAllNewsToFirebase() {
        viewModelScope.launch {
            isSyncingWithFirebase.value = true
            val currentNews = allNews.value
            val success = repository.firebaseManager.uploadAllNewsBatch(currentNews)
            isSyncingWithFirebase.value = false
            if (success) {
                uiMessage.emit(UiMessage("Uploaded ${currentNews.size} announcements to Firebase!"))
            } else {
                uiMessage.emit(UiMessage("Failed to upload announcements to Firebase.", isError = true))
            }
        }
    }

    fun clearAllCatalogAndCloud() {
        viewModelScope.launch {
            isSyncingWithFirebase.value = true
            repository.deleteAllBooksFromEverywhere()
            isSyncingWithFirebase.value = false
            uiMessage.emit(UiMessage("All dummy books cleared from both Local & Firebase Cloud!"))
        }
    }

    fun setReaderPage(page: Int) {
        readerCurrentPage.value = page
        activeReaderBook.value?.let { book ->
            viewModelScope.launch {
                repository.recordReadingProgress(book.id, page)
            }
        }
    }
}
