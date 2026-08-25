package com.example.data.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PdfDownloader(private val context: Context) {

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val domain = url.topPrivateDomain() ?: url.host
            val list = cookieStore.getOrPut(domain) { mutableListOf() }
            synchronized(list) {
                // Remove expired or existing with same name
                list.removeAll { existing -> cookies.any { it.name == existing.name } }
                list.addAll(cookies)
            }
            // Also store under general ".google.com" if google host
            if (url.host.contains("google.com")) {
                val gList = cookieStore.getOrPut("google.com") { mutableListOf() }
                synchronized(gList) {
                    gList.removeAll { existing -> cookies.any { it.name == existing.name } }
                    gList.addAll(cookies)
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val domain = url.topPrivateDomain() ?: url.host
            val result = mutableListOf<Cookie>()
            cookieStore[domain]?.let { list ->
                synchronized(list) {
                    result.addAll(list)
                }
            }
            if (url.host.contains("google.com") && domain != "google.com") {
                cookieStore["google.com"]?.let { list ->
                    synchronized(list) {
                        for (c in list) {
                            if (result.none { it.name == c.name }) {
                                result.add(c)
                            }
                        }
                    }
                }
            }
            return result
        }
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val TAG = "PdfDownloader"
    }

    suspend fun downloadPdf(
        rawUrl: String,
        targetFile: File,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || !trimmed.startsWith("http", ignoreCase = true)) {
            return@withContext false
        }

        val tempFile = File(targetFile.parentFile, "${targetFile.name}.downloading")
        if (tempFile.exists()) tempFile.delete()

        try {
            val driveId = extractGoogleDriveFileId(trimmed)
            var downloaded = false

            if (driveId != null) {
                // Try Google Drive Multi-Step Download
                downloaded = downloadGoogleDriveFile(driveId, tempFile, onProgress)
            } else if (trimmed.contains("mediafire.com", ignoreCase = true)) {
                downloaded = downloadMediafireFile(trimmed, tempFile, onProgress)
            } else {
                val directUrl = resolveGeneralDirectUrl(trimmed)
                downloaded = downloadDirectUrl(directUrl, tempFile, onProgress)
            }

            if (downloaded && isValidPdfFile(tempFile)) {
                if (targetFile.exists()) targetFile.delete()
                val renamed = tempFile.renameTo(targetFile)
                if (renamed) {
                    onProgress(100)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error for $rawUrl", e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
        return@withContext false
    }

    private suspend fun downloadGoogleDriveFile(
        fileId: String,
        tempFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        // Strategy 1: Call uc?export=download&id=...
        val primaryUrl = "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
        var success = executeDownloadAndHandleDriveHtml(primaryUrl, fileId, tempFile, onProgress)
        if (success && isValidPdfFile(tempFile)) return@withContext true

        // Strategy 2: Call drive.usercontent.google.com
        val usercontentUrl = "https://drive.usercontent.google.com/download?id=$fileId&export=download&authuser=0&confirm=t"
        success = executeDownloadAndHandleDriveHtml(usercontentUrl, fileId, tempFile, onProgress)
        if (success && isValidPdfFile(tempFile)) return@withContext true

        // Strategy 3: Docs UC export
        val docsUrl = "https://docs.google.com/uc?export=download&id=$fileId"
        success = executeDownloadAndHandleDriveHtml(docsUrl, fileId, tempFile, onProgress)
        if (success && isValidPdfFile(tempFile)) return@withContext true

        // Strategy 4: Google Docs / Presentation PDF export formats
        val exportDocUrl = "https://docs.google.com/document/d/$fileId/export?format=pdf"
        success = downloadDirectUrl(exportDocUrl, tempFile, onProgress)
        if (success && isValidPdfFile(tempFile)) return@withContext true

        val exportPresUrl = "https://docs.google.com/presentation/d/$fileId/export/pdf"
        success = downloadDirectUrl(exportPresUrl, tempFile, onProgress)
        if (success && isValidPdfFile(tempFile)) return@withContext true

        return@withContext false
    }

    private suspend fun executeDownloadAndHandleDriveHtml(
        url: String,
        fileId: String,
        tempFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/pdf,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "identity")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false

                val body = response.body ?: return@withContext false
                val contentType = body.contentType()?.toString() ?: ""

                // Check if response is HTML warning page
                val source = body.source()
                source.request(1024)
                val buffer = source.buffer

                val isPdf = buffer.size >= 4 &&
                        buffer[0] == 0x25.toByte() &&
                        buffer[1] == 0x50.toByte() &&
                        buffer[2] == 0x44.toByte() &&
                        buffer[3] == 0x46.toByte()

                if (!isPdf && (contentType.contains("text/html", ignoreCase = true) || buffer.indexOf('<'.toByte()) in 0..10)) {
                    val htmlContent = body.string()
                    // Extract download form or confirm link from HTML
                    val confirmUrl = extractDriveConfirmUrlFromHtml(htmlContent, fileId)
                    if (confirmUrl != null) {
                        return@withContext downloadDirectUrl(confirmUrl, tempFile, onProgress)
                    }
                    return@withContext false
                }

                // Stream response into tempFile
                val totalLength = body.contentLength()
                val outputStream = FileOutputStream(tempFile)
                val input = body.byteStream()
                val chunk = ByteArray(16384)
                var bytesRead: Int
                var totalRead: Long = 0

                while (input.read(chunk).also { bytesRead = it } != -1) {
                    outputStream.write(chunk, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalLength > 0) {
                        val percent = ((totalRead * 100) / totalLength).toInt().coerceIn(5, 98)
                        onProgress(percent)
                    } else {
                        val est = (10 + (totalRead / 50000).toInt()).coerceIn(10, 95)
                        onProgress(est)
                    }
                }
                outputStream.flush()
                outputStream.close()
                input.close()

                return@withContext tempFile.exists() && tempFile.length() > 500 && isValidPdfFile(tempFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing download for $url", e)
            return@withContext false
        }
    }

    private fun extractDriveConfirmUrlFromHtml(html: String, fileId: String): String? {
        // 1. Look for form action
        val formMatch = Regex("""<form[^>]*action="([^"]+)"[^>]*>([\s\S]*?)</form>""").find(html)
        if (formMatch != null) {
            val action = formMatch.groupValues[1].replace("&amp;", "&")
            val formBody = formMatch.groupValues[2]
            val params = mutableMapOf<String, String>()

            val inputRegex = Regex("""<input[^>]+>""")
            for (match in inputRegex.findAll(formBody)) {
                val inputTag = match.value
                val nameMatch = Regex("""name=["']([^"']+)["']""").find(inputTag)
                val valMatch = Regex("""value=["']([^"']+)["']""").find(inputTag)
                if (nameMatch != null && valMatch != null) {
                    params[nameMatch.groupValues[1]] = valMatch.groupValues[1]
                }
            }

            if (!params.containsKey("id")) params["id"] = fileId
            if (!params.containsKey("export")) params["export"] = "download"

            val queryString = params.entries.joinToString("&") { "${it.key}=${it.value}" }
            return if (action.contains("?")) "$action&$queryString" else "$action?$queryString"
        }

        // 2. Look for uc-download-link or a href with confirm
        val linkMatch = Regex("""<a[^>]*id=["']uc-download-link["'][^>]*href=["']([^"']+)["']""").find(html)
            ?: Regex("""<a[^>]*href=["']([^"']*(?:confirm=|export=download|drive\.usercontent)[^"']*)["']""").find(html)

        if (linkMatch != null) {
            var href = linkMatch.groupValues[1].replace("&amp;", "&")
            if (href.startsWith("/")) {
                href = "https://drive.google.com$href"
            }
            return href
        }

        // 3. Look for confirm token directly
        val confirmTokenMatch = Regex("""confirm=([0-9A-Za-z_-]+)""").find(html)
            ?: Regex("""name=["']confirm["']\s+value=["']([^"']+)["']""").find(html)
            ?: Regex("""value=["']([^"']+)["']\s+name=["']confirm["']""").find(html)

        if (confirmTokenMatch != null) {
            val token = confirmTokenMatch.groupValues[1]
            return "https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=$token"
        }

        return null
    }

    private suspend fun downloadMediafireFile(
        pageUrl: String,
        tempFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(pageUrl).header("User-Agent", USER_AGENT).build()
            val html = client.newCall(request).execute().use { it.body?.string() } ?: return@withContext false
            val directMatch = Regex("""aria-label="Download file"[^>]*href="([^"]+)"""").find(html)
                ?: Regex("""id="downloadButton"[^>]*href="([^"]+)"""").find(html)
                ?: Regex("""href="(https?://download\d+\.mediafire\.com/[^"]+)"""").find(html)

            if (directMatch != null) {
                return@withContext downloadDirectUrl(directMatch.groupValues[1], tempFile, onProgress)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Mediafire download error", e)
        }
        return@withContext false
    }

    private suspend fun downloadDirectUrl(
        url: String,
        tempFile: File,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .header("Accept-Encoding", "identity")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val body = response.body ?: return@withContext false

                val totalLength = body.contentLength()
                val outputStream = FileOutputStream(tempFile)
                val input = body.byteStream()
                val chunk = ByteArray(16384)
                var bytesRead: Int
                var totalRead: Long = 0

                while (input.read(chunk).also { bytesRead = it } != -1) {
                    outputStream.write(chunk, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalLength > 0) {
                        val percent = ((totalRead * 100) / totalLength).toInt().coerceIn(5, 98)
                        onProgress(percent)
                    } else {
                        val est = (10 + (totalRead / 50000).toInt()).coerceIn(10, 95)
                        onProgress(est)
                    }
                }
                outputStream.flush()
                outputStream.close()
                input.close()

                return@withContext tempFile.exists() && tempFile.length() > 500 && isValidPdfFile(tempFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct download error for $url", e)
            return@withContext false
        }
    }

    fun extractGoogleDriveFileId(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val match1 = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (match1 != null) return match1.groupValues[1]
        val match2 = Regex("""[?&]id=([a-zA-Z0-9_-]+)""").find(trimmed)
        if (match2 != null) return match2.groupValues[1]
        val match3 = Regex("""docs\.google\.com/[a-z]+/d/([a-zA-Z0-9_-]+)""").find(trimmed)
        if (match3 != null) return match3.groupValues[1]
        val match4 = Regex("""drive\.google\.com/open\?id=([a-zA-Z0-9_-]+)""").find(trimmed)
        if (match4 != null) return match4.groupValues[1]
        return null
    }

    private fun resolveGeneralDirectUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.contains("dropbox.com", ignoreCase = true)) {
            return trimmed.replace("?dl=0", "?dl=1").replace("&dl=0", "&dl=1")
        }
        if (trimmed.contains("github.com", ignoreCase = true) && trimmed.contains("/blob/")) {
            return trimmed.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
        }
        return trimmed
    }

    fun isValidPdfFile(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            val header = ByteArray(4)
            FileInputStream(file).use { it.read(header) }
            header[0] == 0x25.toByte() && header[1] == 0x50.toByte() &&
                    header[2] == 0x44.toByte() && header[3] == 0x46.toByte()
        } catch (e: Exception) {
            false
        }
    }

    fun getActualPdfPageCount(file: File, fallbackPages: Int): Int {
        if (!file.exists() || !isValidPdfFile(file)) return fallbackPages
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count.coerceAtLeast(1)
        } catch (e: Exception) {
            fallbackPages
        }
    }

    fun generateOfflineStudyPdf(
        title: String,
        subject: String,
        classLevel: Int,
        provinceCode: String,
        totalPages: Int,
        targetFile: File
    ): Boolean {
        val doc = android.graphics.pdf.PdfDocument()
        try {
            val pagesToCreate = totalPages.coerceIn(8, 36)
            val pw = 595 // Standard A4 width in points
            val ph = 842 // Standard A4 height in points

            val titlePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(15, 23, 42)
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(5, 150, 105)
                textSize = 13f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val bodyPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(51, 65, 85)
                textSize = 11.5f
                isAntiAlias = true
            }
            val headerBgPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(241, 245, 249)
            }
            val linePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(226, 232, 240)
                strokeWidth = 1f
            }
            val bannerPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(16, 185, 129)
            }
            val pageNumPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.rgb(100, 116, 139)
                textSize = 10f
                isAntiAlias = true
            }

            for (p in 1..pagesToCreate) {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pw, ph, p).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(android.graphics.Color.WHITE)

                canvas.drawRect(0f, 0f, pw.toFloat(), 60f, headerBgPaint)
                canvas.drawRect(0f, 58f, pw.toFloat(), 60f, bannerPaint)

                canvas.drawText("${provinceCode.uppercase()} TEXTBOOK BOARD • CLASS $classLevel", 30f, 32f, subPaint)
                canvas.drawText("Curriculum & Solved Reference Guide", 30f, 48f, pageNumPaint)

                val chapterNum = ((p - 1) / 3) + 1
                val chapterTitle = when (chapterNum) {
                    1 -> "Chapter 1: Fundamentals & Core Concepts"
                    2 -> "Chapter 2: Principles, Theories & Methodologies"
                    3 -> "Chapter 3: Analytical Problem Solving & Examples"
                    4 -> "Chapter 4: Comprehensive Review & Key Formulas"
                    5 -> "Chapter 5: Board Exam Solved Questions & MCQs"
                    6 -> "Chapter 6: Practical Exercises & Model Test Papers"
                    else -> "Chapter $chapterNum: Advanced Topics & Summary"
                }

                var y = 100f
                canvas.drawText("$title — $subject", 30f, y, titlePaint)
                y += 26f
                canvas.drawText(chapterTitle, 30f, y, subPaint)
                y += 15f
                canvas.drawLine(30f, y, pw - 30f, y, linePaint)
                y += 28f

                val pageType = (p - 1) % 3
                val lines = when (pageType) {
                    0 -> listOf(
                        "1. Overview & Learning Objectives:",
                        "• Understand the primary axioms, definitions, and foundational concepts.",
                        "• Master the standard terminology approved by the Board of Intermediate & Secondary Education.",
                        "• Review practical real-world applications relevant to student coursework.",
                        "",
                        "2. Key Definitions & Core Terminology:",
                        "• Concept Definition A: Fundamental law governing the primary state and interactions.",
                        "• Concept Definition B: Empirical observations and theoretical derivations.",
                        "• Application Framework: Step-by-step methodologies used in standard evaluations.",
                        "",
                        "3. Theoretical Background & Explanations:",
                        "In this section, students examine the foundational principles with clear explanations.",
                        "Every topic is structured according to the latest national curriculum syllabus guidelines,",
                        "providing in-depth conceptual clarity and examination preparation strategies."
                    )
                    1 -> listOf(
                        "1. Solved Examples & Step-by-Step Calculations:",
                        "• Example 1: Calculate the standard value using formula [X = (A × B) / C].",
                        "  Step 1: Identify known values from given problem statement.",
                        "  Step 2: Substitute parameters into the standard formula.",
                        "  Step 3: Solve algebraic operations and verify unit consistency.",
                        "  Result: Verified against BISE standard marking scheme.",
                        "",
                        "2. Board Examination Tips & Important Notes:",
                        "• Always draw neat, labeled diagrams where applicable.",
                        "• State all assumptions clearly in subjective questions.",
                        "• Write units for every calculated final answer to secure full marks."
                    )
                    else -> listOf(
                        "1. Exercise Questions & Self-Assessment:",
                        "Q1: Define the core principles of $subject and explain their importance.",
                        "Q2: Differentiate between standard theoretical derivations and practical experiments.",
                        "Q3: Solve the short questions typically asked in Section-B of BISE examinations.",
                        "",
                        "2. Multiple Choice Questions (MCQs):",
                        "• Q(i): Which of the following best describes the primary state? (A/B/C/D)",
                        "• Q(ii): Identify the correct SI unit for the calculated parameter.",
                        "• Q(iii): The standard formula was formulated in which year of study?",
                        "",
                        "3. Summary & Quick Revision Formula Sheet:",
                        "• Remember the key relationships and practice the past 5 years' board papers."
                    )
                }

                for (line in lines) {
                    if (line.startsWith("1.") || line.startsWith("2.") || line.startsWith("3.")) {
                        canvas.drawText(line, 30f, y, subPaint)
                    } else {
                        canvas.drawText(line, 30f, y, bodyPaint)
                    }
                    y += 18f
                }

                canvas.drawLine(30f, ph - 45f, pw - 30f, ph - 45f, linePaint)
                canvas.drawText("OnStudy Pakistan • All Boards Study Guide", 30f, ph - 28f, pageNumPaint)
                canvas.drawText("Page $p of $pagesToCreate", pw - 100f, ph - 28f, pageNumPaint)

                doc.finishPage(page)
            }

            if (targetFile.exists()) targetFile.delete()
            val fos = FileOutputStream(targetFile)
            doc.writeTo(fos)
            fos.flush()
            fos.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                doc.close()
            } catch (ignored: Throwable) {}
        }
    }
}
