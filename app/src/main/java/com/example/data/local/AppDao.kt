package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Books ---
    @Query("SELECT * FROM books ORDER BY id ASC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY id ASC")
    suspend fun getAllBooksSync(): List<BookEntity>

    @Query("SELECT * FROM books WHERE provinceCode = :provinceCode AND classLevel = :classLevel ORDER BY id ASC")
    fun getBooksByProvinceAndClass(provinceCode: String, classLevel: Int): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE isDownloaded = 1 ORDER BY lastReadTimestamp DESC")
    fun getDownloadedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastReadTimestamp > 0 ORDER BY lastReadTimestamp DESC LIMIT 5")
    fun getRecentReads(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isBookmarked = 1 ORDER BY id DESC")
    fun getBookmarkedBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("UPDATE books SET isDownloaded = :isDownloaded, localFilePath = :filePath, downloadProgress = 100, isDownloading = 0, downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, isDownloaded: Boolean, filePath: String?)

    @Query("UPDATE books SET isDownloading = :isDownloading, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadProgress(id: Long, isDownloading: Boolean, progress: Int)

    @Query("UPDATE books SET lastReadPage = :page, lastReadTimestamp = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: Long, page: Int, timestamp: Long)

    @Query("UPDATE books SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookBookmark(id: Long, isBookmarked: Boolean)

    @Query("UPDATE books SET isDownloaded = 0, localFilePath = NULL, downloadProgress = 0 WHERE id = :id")
    suspend fun removeBookDownload(id: Long)

    @Query("UPDATE books SET isDownloaded = 0, localFilePath = NULL, downloadProgress = 0")
    suspend fun clearAllDownloads()

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBooksCount(): Int

    // --- Bookmarks ---
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageNumber = :page")
    suspend fun deleteBookmarkForPage(bookId: Long, page: Int)

    // --- Notes ---
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY updatedAt DESC")
    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    // --- News / Announcements ---
    @Query("SELECT * FROM news ORDER BY id DESC")
    fun getAllNews(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news ORDER BY id DESC")
    suspend fun getAllNewsSync(): List<NewsEntity>

    @Query("SELECT COUNT(*) FROM news WHERE isUnread = 1")
    fun getUnreadNewsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<NewsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleNews(news: NewsEntity): Long

    @Query("UPDATE news SET isUnread = 0 WHERE id = :id")
    suspend fun markNewsAsRead(id: Long)

    @Query("UPDATE news SET isUnread = 0")
    suspend fun markAllNewsAsRead()

    @Query("DELETE FROM news WHERE id = :id")
    suspend fun deleteNewsById(id: Long)

    @Query("DELETE FROM news")
    suspend fun deleteAllNews()
}
