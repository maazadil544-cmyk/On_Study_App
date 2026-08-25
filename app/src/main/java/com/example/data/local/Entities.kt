package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val provinceCode: String,
    val classLevel: Int, // 1 to 12
    val subject: String,
    val bookType: String, // TEXTBOOK, GUIDE, NOTES, PAST_PAPERS
    val fileLink: String, // Mediafire, Google Drive, or Direct link
    val coverImage: String = "",
    val fileSize: String = "12.5 MB",
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val downloadProgress: Int = 0, // 0 to 100
    val isDownloading: Boolean = false,
    val uploadDate: String = "2026-08",
    val downloadCount: Int = 120,
    val totalPages: Int = 45,
    val lastReadPage: Int = 1,
    val lastReadTimestamp: Long = 0L,
    val isBookmarked: Boolean = false,
    val sampleContent: String = "" // Structured study text/chapters for instant in-app reading
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val bookTitle: String,
    val subject: String,
    val classLevel: Int,
    val provinceCode: String,
    val pageNumber: Int,
    val chapterTitle: String,
    val noteSnippet: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long? = null,
    val bookTitle: String? = null,
    val classLevel: Int? = null,
    val subject: String? = null,
    val title: String,
    val content: String,
    val colorIndex: Int = 0, // 0: Emerald, 1: Blue, 2: Amber, 3: Purple, 4: Rose
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // Date Sheet, New Guide, Syllabus Update, Board Notice
    val date: String,
    val boardName: String = "All Boards",
    val isUnread: Boolean = true,
    val targetClass: String = "All Classes"
)
