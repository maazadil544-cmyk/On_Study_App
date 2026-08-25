package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.BookEntity
import com.example.data.local.NewsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FirebaseRealtimeDbManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("firebase_rtdb_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val TAG = "FirebaseRtdb"
        private const val KEY_DB_URL = "firebase_database_url"
        // Default public Firebase Realtime Database URL for ON Study app
        const val DEFAULT_DATABASE_URL = "https://on-study-4ad19-default-rtdb.firebaseio.com"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    var databaseUrl: String
        get() {
            val saved = prefs.getString(KEY_DB_URL, DEFAULT_DATABASE_URL) ?: DEFAULT_DATABASE_URL
            return cleanUrl(saved)
        }
        set(value) {
            prefs.edit().putString(KEY_DB_URL, cleanUrl(value)).apply()
        }

    private fun cleanUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }

    /**
     * Test connection to Firebase Realtime Database
     */
    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/.json?shallow=true"
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "Connected successfully to Firebase Realtime Database!")
                } else if (response.code == 404) {
                    Pair(false, "Server returned HTTP 404 (Database Not Found):\nThis Firebase URL does not exist. Please create your free Realtime Database at console.firebase.google.com and paste your database URL here.")
                } else if (response.code == 401 || response.code == 403) {
                    Pair(false, "Server returned HTTP ${response.code} (Permission Denied):\nIn Firebase Console -> Realtime Database -> Rules tab, change rules to {\".read\": true, \".write\": true} and click Publish.")
                } else {
                    Pair(false, "Server returned HTTP ${response.code}: ${response.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed", e)
            Pair(false, "Connection error: ${e.localizedMessage ?: "Unknown network error"}")
        }
    }

    /**
     * Fetch all books from Firebase Realtime Database
     */
    suspend fun fetchRemoteBooks(): List<BookEntity> = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/books.json"
        val books = mutableListOf<BookEntity>()
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorMsg = when (response.code) {
                    404 -> "Firebase Database Not Found (HTTP 404).\nCheck database URL: $databaseUrl"
                    401, 403 -> "Permission Denied (HTTP ${response.code}).\nMake sure Realtime Database rules have {\".read\": true, \".write\": true}."
                    else -> "Firebase returned HTTP ${response.code}: ${response.message}"
                }
                Log.w(TAG, errorMsg)
                throw RuntimeException(errorMsg)
            }
            val body = response.body?.string() ?: return@withContext emptyList()
            if (body.isBlank() || body == "null") return@withContext emptyList()

            // Firebase may return an Object with keys or an Array
            if (body.startsWith("{")) {
                val rootObj = JSONObject(body)
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = rootObj.optJSONObject(key)
                    if (item != null) {
                        parseBookJson(item, key.toLongOrNull())?.let { books.add(it) }
                    }
                }
            } else if (body.startsWith("[")) {
                val rootArr = JSONArray(body)
                for (i in 0 until rootArr.length()) {
                    val item = rootArr.optJSONObject(i)
                    if (item != null) {
                        parseBookJson(item, i.toLong())?.let { books.add(it) }
                    }
                }
            }
        }
        return@withContext books
    }

    private fun parseBookJson(obj: JSONObject, fallbackId: Long?): BookEntity? {
        val title = obj.optString("title")
        if (title.isBlank()) return null

        val id = if (obj.has("id")) obj.optLong("id") else (fallbackId ?: System.currentTimeMillis())
        val provinceCode = obj.optString("provinceCode", "kpk")
        val classLevel = obj.optInt("classLevel", 1)
        val subject = obj.optString("subject", "General Knowledge")
        val bookType = obj.optString("bookType", "TEXTBOOK")
        val fileLink = obj.optString("fileLink", "")
        val coverImage = obj.optString("coverImage", "")
        val fileSize = obj.optString("fileSize", "12.5 MB")
        val uploadDate = obj.optString("uploadDate", "2026-08")
        val downloadCount = obj.optInt("downloadCount", 100)
        val totalPages = obj.optInt("totalPages", 45)
        val sampleContent = obj.optString("sampleContent", "")

        return BookEntity(
            id = id,
            title = title,
            provinceCode = provinceCode,
            classLevel = classLevel,
            subject = subject,
            bookType = bookType,
            fileLink = fileLink,
            coverImage = coverImage,
            fileSize = fileSize,
            uploadDate = uploadDate,
            downloadCount = downloadCount,
            totalPages = totalPages,
            sampleContent = sampleContent
        )
    }

    /**
     * Upload or update a book in Firebase Realtime Database
     */
    suspend fun uploadBook(book: BookEntity): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/books/${book.id}.json"
        try {
            val json = JSONObject().apply {
                put("id", book.id)
                put("title", book.title)
                put("provinceCode", book.provinceCode)
                put("classLevel", book.classLevel)
                put("subject", book.subject)
                put("bookType", book.bookType)
                put("fileLink", book.fileLink)
                put("coverImage", book.coverImage)
                put("fileSize", book.fileSize)
                put("uploadDate", book.uploadDate)
                put("downloadCount", book.downloadCount)
                put("totalPages", book.totalPages)
                put("sampleContent", book.sampleContent)
            }

            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading book to Firebase: ${book.title}", e)
            false
        }
    }

    /**
     * Delete a book from Firebase Realtime Database
     */
    suspend fun deleteBook(bookId: Long): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/books/$bookId.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting book $bookId from Firebase", e)
            false
        }
    }

    /**
     * Fetch all announcements / news from Firebase Realtime Database
     */
    suspend fun fetchRemoteNews(): List<NewsEntity> = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/news.json"
        val newsList = mutableListOf<NewsEntity>()
        try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                if (body.isBlank() || body == "null") return@withContext emptyList()

                if (body.startsWith("{")) {
                    val rootObj = JSONObject(body)
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val item = rootObj.optJSONObject(key)
                        if (item != null) {
                            parseNewsJson(item, key.toLongOrNull())?.let { newsList.add(it) }
                        }
                    }
                } else if (body.startsWith("[")) {
                    val rootArr = JSONArray(body)
                    for (i in 0 until rootArr.length()) {
                        val item = rootArr.optJSONObject(i)
                        if (item != null) {
                            parseNewsJson(item, i.toLong())?.let { newsList.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching news from Firebase", e)
        }
        return@withContext newsList
    }

    private fun parseNewsJson(obj: JSONObject, fallbackId: Long?): NewsEntity? {
        val title = obj.optString("title")
        if (title.isBlank()) return null
        val id = if (obj.has("id")) obj.optLong("id") else (fallbackId ?: System.currentTimeMillis())
        val description = obj.optString("description", "")
        val category = obj.optString("category", "General")
        val date = obj.optString("date", "2026-08")
        val boardName = obj.optString("boardName", "All Boards")
        val targetClass = obj.optString("targetClass", "All Classes")

        return NewsEntity(
            id = id,
            title = title,
            description = description,
            category = category,
            date = date,
            boardName = boardName,
            isUnread = true,
            targetClass = targetClass
        )
    }

    /**
     * Upload an announcement / news to Firebase
     */
    suspend fun uploadNews(news: NewsEntity): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/news/${news.id}.json"
        try {
            val json = JSONObject().apply {
                put("id", news.id)
                put("title", news.title)
                put("description", news.description)
                put("category", news.category)
                put("date", news.date)
                put("boardName", news.boardName)
                put("targetClass", news.targetClass)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading news to Firebase", e)
            false
        }
    }

    /**
     * Delete an announcement / news from Firebase
     */
    suspend fun deleteNews(newsId: Long): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/news/$newsId.json"
        try {
            val request = Request.Builder().url(url).delete().build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting news $newsId from Firebase", e)
            false
        }
    }

    /**
     * Syncs a batch of local books to Firebase
     */
    suspend fun uploadAllBooksBatch(books: List<BookEntity>): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/books.json"
        try {
            val rootObj = JSONObject()
            for (b in books) {
                val bJson = JSONObject().apply {
                    put("id", b.id)
                    put("title", b.title)
                    put("provinceCode", b.provinceCode)
                    put("classLevel", b.classLevel)
                    put("subject", b.subject)
                    put("bookType", b.bookType)
                    put("fileLink", b.fileLink)
                    put("coverImage", b.coverImage)
                    put("fileSize", b.fileSize)
                    put("uploadDate", b.uploadDate)
                    put("downloadCount", b.downloadCount)
                    put("totalPages", b.totalPages)
                    put("sampleContent", b.sampleContent)
                }
                rootObj.put(b.id.toString(), bJson)
            }
            val body = rootObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing all books batch to Firebase", e)
            false
        }
    }

    /**
     * Clears all books node from Firebase Realtime Database
     */
    suspend fun clearAllRemoteBooks(): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/books.json"
        try {
            val body = "{}".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing remote books", e)
            false
        }
    }

    /**
     * Uploads a batch of news to Firebase Realtime Database
     */
    suspend fun uploadAllNewsBatch(newsList: List<NewsEntity>): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/news.json"
        try {
            val rootObj = JSONObject()
            for (n in newsList) {
                val nJson = JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("description", n.description)
                    put("category", n.category)
                    put("date", n.date)
                    put("boardName", n.boardName)
                    put("targetClass", n.targetClass)
                }
                rootObj.put(n.id.toString(), nJson)
            }
            val body = rootObj.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing news batch to Firebase", e)
            false
        }
    }

    /**
     * Clears all news from Firebase Realtime Database
     */
    suspend fun clearAllRemoteNews(): Boolean = withContext(Dispatchers.IO) {
        val url = "$databaseUrl/news.json"
        try {
            val body = "{}".toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder().url(url).put(body).build()
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing remote news", e)
            false
        }
    }
}
