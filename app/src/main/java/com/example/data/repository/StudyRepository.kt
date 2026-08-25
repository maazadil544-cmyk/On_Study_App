package com.example.data.repository

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class StudyRepository(
    private val context: Context,
    private val dao: AppDao
) {

    val firebaseManager = com.example.data.remote.FirebaseRealtimeDbManager(context)

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val booksSyncedCount: Int = 0,
        val newsSyncedCount: Int = 0
    )

    suspend fun initializeDataIfNeeded() = withContext(Dispatchers.IO) {
        val booksCount = dao.getBooksCount()
        if (booksCount == 0) {
            val defaultBooks = SampleDataProvider.getInitialBooks()
            dao.insertBooks(defaultBooks)
        }
        val currentNews = dao.getAllNewsSync()
        if (currentNews.isEmpty()) {
            val defaultNews = SampleDataProvider.getInitialNews()
            dao.insertNews(defaultNews)
        }
        // Auto sync with Firebase if internet is active
        if (com.example.data.util.NetworkUtils.isOnline(context)) {
            syncWithFirebase()
        }
    }

    /**
     * Synchronize local Room database with Firebase Realtime Database
     */
    suspend fun syncWithFirebase(): SyncResult = withContext(Dispatchers.IO) {
        if (!com.example.data.util.NetworkUtils.isOnline(context)) {
            return@withContext SyncResult(
                success = true,
                message = "Offline mode: Loaded catalog from on-device storage.",
                booksSyncedCount = 0,
                newsSyncedCount = 0
            )
        }

        try {
            var booksCount = 0
            var newsCount = 0

            // 1. Fetch and Sync Books from Firebase
            try {
                val remoteBooks = firebaseManager.fetchRemoteBooks()
                if (remoteBooks.isNotEmpty()) {
                    val existingLocalBooks = dao.getAllBooksSync()
                    val existingProgressMap = existingLocalBooks.associateBy { it.id }

                    val mergedBooks = remoteBooks.map { remote ->
                        val local = existingProgressMap[remote.id]
                        if (local != null) {
                            remote.copy(
                                lastReadPage = if (local.lastReadPage > 0) local.lastReadPage else remote.lastReadPage,
                                lastReadTimestamp = if (local.lastReadTimestamp > 0) local.lastReadTimestamp else remote.lastReadTimestamp,
                                isDownloaded = local.isDownloaded,
                                localFilePath = local.localFilePath,
                                isBookmarked = local.isBookmarked
                            )
                        } else {
                            remote
                        }
                    }

                    dao.deleteAllBooks()
                    dao.insertBooks(mergedBooks)
                    booksCount = mergedBooks.size
                }
            } catch (e: Exception) {
                android.util.Log.e("StudyRepository", "Error syncing books", e)
            }

            // 2. Fetch and Sync News & Announcements from Firebase
            try {
                val remoteNews = firebaseManager.fetchRemoteNews()
                if (remoteNews.isNotEmpty()) {
                    val existingNews = dao.getAllNewsSync()
                    val readNewsIds = existingNews.filter { !it.isUnread }.map { it.id }.toSet()

                    val mergedNews = remoteNews.map { rn ->
                        if (readNewsIds.contains(rn.id)) {
                            rn.copy(isUnread = false)
                        } else {
                            rn
                        }
                    }

                    dao.deleteAllNews()
                    dao.insertNews(mergedNews)
                    newsCount = mergedNews.size
                }
            } catch (e: Exception) {
                android.util.Log.e("StudyRepository", "Error syncing news", e)
            }

            SyncResult(
                success = true,
                message = "Synced $booksCount books and $newsCount announcements from Firebase Cloud!",
                booksSyncedCount = booksCount,
                newsSyncedCount = newsCount
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Sync failed: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    suspend fun resetCatalogToDefault() = withContext(Dispatchers.IO) {
        dao.deleteAllBooks()
        val defaultBooks = SampleDataProvider.getInitialBooks()
        dao.insertBooks(defaultBooks)
        firebaseManager.uploadAllBooksBatch(defaultBooks)
    }

    suspend fun deleteAllBooksFromEverywhere() = withContext(Dispatchers.IO) {
        dao.deleteAllBooks()
        firebaseManager.clearAllRemoteBooks()
    }

    // --- Books ---
    fun getAllBooks(): Flow<List<BookEntity>> = dao.getAllBooks()

    fun getBooksByProvinceAndClass(provinceCode: String, classLevel: Int): Flow<List<BookEntity>> =
        dao.getBooksByProvinceAndClass(provinceCode, classLevel)

    fun observeBookById(id: Long): Flow<BookEntity?> = dao.observeBookById(id)

    suspend fun getBookById(id: Long): BookEntity? = dao.getBookById(id)

    fun getDownloadedBooks(): Flow<List<BookEntity>> = dao.getDownloadedBooks()

    fun getRecentReads(): Flow<List<BookEntity>> = dao.getRecentReads()

    fun getBookmarkedBooks(): Flow<List<BookEntity>> = dao.getBookmarkedBooks()

    suspend fun addNewBook(book: BookEntity): Long = withContext(Dispatchers.IO) {
        val insertedId = dao.insertBook(book)
        val finalBook = book.copy(id = insertedId)
        // Upload immediately to Firebase Realtime Database for all devices
        firebaseManager.uploadBook(finalBook)
        insertedId
    }

    suspend fun updateBook(book: BookEntity) = withContext(Dispatchers.IO) {
        dao.updateBook(book)
        // Push update to Firebase Realtime Database
        firebaseManager.uploadBook(book)
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        // Delete local file if present
        book.localFilePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dao.deleteBook(book)
        // Remove from Firebase Realtime Database
        firebaseManager.deleteBook(book.id)
    }

    suspend fun toggleBookBookmark(bookId: Long, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        dao.updateBookBookmark(bookId, !isBookmarked)
    }

    suspend fun recordReadingProgress(bookId: Long, page: Int) = withContext(Dispatchers.IO) {
        dao.updateReadingProgress(bookId, page, System.currentTimeMillis())
    }

    private val pdfDownloader = com.example.data.util.PdfDownloader(context)

    // --- Google Drive & Cloud URL Converter ---
    fun resolveDirectDownloadUrl(rawUrl: String): String {
        return pdfDownloader.extractGoogleDriveFileId(rawUrl)?.let { id ->
            "https://drive.google.com/uc?export=download&id=$id&confirm=t"
        } ?: rawUrl.trim()
    }

    fun extractGoogleDriveFileId(rawUrl: String): String? {
        return pdfDownloader.extractGoogleDriveFileId(rawUrl)
    }

    fun resolveEmbedViewUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""
        val driveFileMatch = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (driveFileMatch != null) {
            val fileId = driveFileMatch.groupValues[1]
            return "https://drive.google.com/file/d/$fileId/preview"
        }
        val driveIdMatch = Regex("""drive\.google\.com/.*[?&]id=([a-zA-Z0-9_-]+)""").find(trimmed)
        if (driveIdMatch != null) {
            val fileId = driveIdMatch.groupValues[1]
            return "https://drive.google.com/file/d/$fileId/preview"
        }
        val docMatch = Regex("""docs\.google\.com/document/d/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (docMatch != null) {
            val docId = docMatch.groupValues[1]
            return "https://docs.google.com/document/d/$docId/preview"
        }
        return if (trimmed.endsWith(".pdf", ignoreCase = true)) {
            "https://docs.google.com/viewer?embedded=true&url=$trimmed"
        } else {
            trimmed
        }
    }

    // --- File Download Engine ---
    suspend fun downloadBook(book: BookEntity, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        dao.updateDownloadProgress(book.id, true, 5)
        onProgress(5)

        val downloadsDir = File(context.filesDir, "downloads").apply { if (!exists()) mkdirs() }
        val sanitizedSubject = book.subject.lowercase().replace("[^a-zA-Z0-9]".toRegex(), "_")
        val fileName = "book_${book.id}_${sanitizedSubject}.pdf"
        val targetFile = File(downloadsDir, fileName)

        // Check if already downloaded and valid
        if (targetFile.exists() && pdfDownloader.isValidPdfFile(targetFile)) {
            val pageCount = pdfDownloader.getActualPdfPageCount(targetFile, book.totalPages)
            dao.updateBook(
                book.copy(
                    isDownloaded = true,
                    isDownloading = false,
                    downloadProgress = 100,
                    localFilePath = targetFile.absolutePath,
                    totalPages = pageCount
                )
            )
            onProgress(100)
            return@withContext true
        }

        var isDownloadedSuccessfully = false
        if (book.fileLink.isNotBlank() && book.fileLink.startsWith("http", ignoreCase = true)) {
            isDownloadedSuccessfully = pdfDownloader.downloadPdf(book.fileLink, targetFile) { prog ->
                kotlinx.coroutines.runBlocking {
                    dao.updateDownloadProgress(book.id, true, prog)
                }
                onProgress(prog)
            }
        }

        if (isDownloadedSuccessfully && targetFile.exists() && pdfDownloader.isValidPdfFile(targetFile)) {
            val actualPages = pdfDownloader.getActualPdfPageCount(targetFile, book.totalPages)
            val updatedBook = book.copy(
                isDownloaded = true,
                isDownloading = false,
                downloadProgress = 100,
                localFilePath = targetFile.absolutePath,
                totalPages = actualPages
            )
            dao.updateBook(updatedBook)
            onProgress(100)
            return@withContext true
        } else {
            // Fallback: Generate full multi-page offline study PDF guide
            val generated = pdfDownloader.generateOfflineStudyPdf(
                title = book.title,
                subject = book.subject,
                classLevel = book.classLevel,
                provinceCode = book.provinceCode,
                totalPages = book.totalPages.coerceAtLeast(16),
                targetFile = targetFile
            )

            if (generated && targetFile.exists() && pdfDownloader.isValidPdfFile(targetFile)) {
                val actualPages = pdfDownloader.getActualPdfPageCount(targetFile, book.totalPages)
                val updatedBook = book.copy(
                    isDownloaded = true,
                    isDownloading = false,
                    downloadProgress = 100,
                    localFilePath = targetFile.absolutePath,
                    totalPages = actualPages
                )
                dao.updateBook(updatedBook)
                onProgress(100)
                return@withContext true
            } else {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                dao.updateBook(
                    book.copy(
                        isDownloaded = false,
                        isDownloading = false,
                        downloadProgress = 0,
                        localFilePath = null
                    )
                )
                onProgress(0)
                return@withContext false
            }
        }
    }

    suspend fun removeDownloadedFile(book: BookEntity) = withContext(Dispatchers.IO) {
        book.localFilePath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        dao.removeBookDownload(book.id)
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = File(context.filesDir, "downloads")
            if (downloadsDir.exists()) {
                downloadsDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        dao.clearAllDownloads()
    }

    fun getTotalStorageUsedBytes(): Long {
        return try {
            val downloadsDir = File(context.filesDir, "downloads")
            if (downloadsDir.exists()) {
                downloadsDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    // --- Bookmarks ---
    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = dao.getAllBookmarks()

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> = dao.getBookmarksForBook(bookId)

    suspend fun addBookmark(bookmark: BookmarkEntity): Long = withContext(Dispatchers.IO) {
        dao.insertBookmark(bookmark)
    }

    suspend fun removeBookmark(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkById(id)
    }

    suspend fun removeBookmarkForPage(bookId: Long, page: Int) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkForPage(bookId, page)
    }

    // --- Notes ---
    fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()

    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>> = dao.getNotesForBook(bookId)

    suspend fun saveNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        if (note.id == 0L) {
            dao.insertNote(note)
        } else {
            dao.updateNote(note)
            note.id
        }
    }

    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteNoteById(id)
    }

    // --- News ---
    fun getAllNews(): Flow<List<NewsEntity>> = dao.getAllNews()

    fun getUnreadNewsCount(): Flow<Int> = dao.getUnreadNewsCount()

    suspend fun addNews(news: NewsEntity): Long = withContext(Dispatchers.IO) {
        val insertedId = dao.insertSingleNews(news)
        val finalNews = news.copy(id = insertedId)
        firebaseManager.uploadNews(finalNews)
        insertedId
    }

    suspend fun markNewsAsRead(id: Long) = withContext(Dispatchers.IO) {
        dao.markNewsAsRead(id)
    }

    suspend fun markAllNewsAsRead() = withContext(Dispatchers.IO) {
        dao.markAllNewsAsRead()
    }

    suspend fun deleteNews(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteNewsById(id)
        firebaseManager.deleteNews(id)
    }
}
