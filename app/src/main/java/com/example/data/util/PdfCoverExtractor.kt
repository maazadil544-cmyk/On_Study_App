package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object PdfCoverExtractor {

    private const val TAG = "PdfCoverExtractor"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Extracts Google Drive file ID from various Drive and Docs link formats.
     */
    fun extractGoogleDriveFileId(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null

        val driveFileMatch = Regex("""drive\.google\.com/file/d/([a-zA-Z0-9_-]{20,})""").find(trimmed)
        if (driveFileMatch != null) return driveFileMatch.groupValues[1]

        val driveIdMatch = Regex("""drive\.google\.com/.*[?&]id=([a-zA-Z0-9_-]{20,})""").find(trimmed)
        if (driveIdMatch != null) return driveIdMatch.groupValues[1]

        val docMatch = Regex("""docs\.google\.com/document/d/([a-zA-Z0-9_-]{20,})""").find(trimmed)
        if (docMatch != null) return docMatch.groupValues[1]

        val openMatch = Regex("""drive\.google\.com/open\?id=([a-zA-Z0-9_-]{20,})""").find(trimmed)
        if (openMatch != null) return openMatch.groupValues[1]

        val ucMatch = Regex("""drive\.google\.com/uc\?.*id=([a-zA-Z0-9_-]{20,})""").find(trimmed)
        if (ucMatch != null) return ucMatch.groupValues[1]

        return null
    }

    /**
     * Returns Google's high-res thumbnail endpoint for the first page of a Drive PDF.
     */
    fun getDriveThumbnailUrl(fileId: String): String {
        return "https://lh3.googleusercontent.com/d/$fileId=w800"
    }

    /**
     * Extracts or generates the cover photo (Page 1) for a given PDF link or file.
     * Returns a local file path (e.g. /data/user/0/.../covers/cover_xxx.jpg) or image URL.
     */
    suspend fun extractCoverFromPdf(
        context: Context,
        rawUrl: String,
        fallbackTitle: String = "",
        fallbackSubject: String = "",
        fallbackClass: Int = 10,
        fallbackProvince: String = "punjab"
    ): String = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return@withContext generateStyledFallbackCover(
                context,
                fallbackTitle.ifBlank { "Textbook Guide" },
                fallbackSubject.ifBlank { "General Studies" },
                fallbackClass,
                fallbackProvince
            )
        }

        // 1. Check if it's already an existing local PDF file on device
        val localFile = File(trimmed)
        if (localFile.exists() && localFile.length() > 0) {
            val localCover = renderPdfFirstPageToImage(context, localFile)
            if (!localCover.isNullOrBlank()) {
                return@withContext localCover
            }
        }

        // 2. Google Drive Link: Extract fileId and get first page thumbnail
        val driveId = extractGoogleDriveFileId(trimmed)
        if (driveId != null) {
            val thumbUrl = getDriveThumbnailUrl(driveId)
            // Attempt to cache the thumbnail image locally so it is instant and offline-ready
            val cachedFile = cacheRemoteImage(context, thumbUrl, "cover_drive_${driveId}.jpg")
            if (cachedFile != null && cachedFile.exists()) {
                return@withContext cachedFile.absolutePath
            }
            return@withContext thumbUrl
        }

        // 3. Direct PDF download or online link
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val tempPdf = File(context.cacheDir, "temp_extract_${System.currentTimeMillis()}.pdf")
            try {
                val request = Request.Builder()
                    .url(trimmed)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body
                    if (body != null) {
                        FileOutputStream(tempPdf).use { out ->
                            body.byteStream().copyTo(out)
                        }
                        if (tempPdf.exists() && tempPdf.length() > 500) {
                            val renderedCover = renderPdfFirstPageToImage(context, tempPdf)
                            if (!renderedCover.isNullOrBlank()) {
                                return@withContext renderedCover
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct PDF cover extraction failed: ${e.message}")
            } finally {
                if (tempPdf.exists()) {
                    tempPdf.delete()
                }
            }
        }

        // 4. Fallback: generate a clean, elegant textbook cover bitmap
        return@withContext generateStyledFallbackCover(
            context,
            fallbackTitle.ifBlank { "Textbook Guide" },
            fallbackSubject.ifBlank { "General Studies" },
            fallbackClass,
            fallbackProvince
        )
    }

    /**
     * Renders Page 0 (the first page) of a local PDF file into a high-quality JPEG image.
     */
    fun renderPdfFirstPageToImage(context: Context, pdfFile: File): String? {
        if (!pdfFile.exists() || pdfFile.length() < 100) return null

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        return try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount <= 0) return null

            page = renderer.openPage(0) // First page of the PDF!

            val targetWidth = 540
            val aspect = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspect).toInt().coerceIn(380, 1080)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(AndroidColor.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
            val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}_${(100..999).random()}.jpg")

            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
            }
            bitmap.recycle()

            coverFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering PDF first page: ${e.message}", e)
            null
        } finally {
            try { page?.close() } catch (ignored: Throwable) {}
            try { renderer?.close() } catch (ignored: Throwable) {}
            try { pfd?.close() } catch (ignored: Throwable) {}
        }
    }

    /**
     * Downloads an image from a URL and saves it into the app's covers directory.
     */
    private fun cacheRemoteImage(context: Context, imageUrl: String, filename: String): File? {
        return try {
            val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
            val targetFile = File(coversDir, filename)
            if (targetFile.exists() && targetFile.length() > 500) {
                return targetFile
            }

            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    FileOutputStream(targetFile).use { fos ->
                        fos.write(bytes)
                        fos.flush()
                    }
                    return targetFile
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache remote image: ${e.message}")
            null
        }
    }

    /**
     * Generates a beautifully styled textbook cover bitmap with realistic book spine and title typography.
     */
    fun generateStyledFallbackCover(
        context: Context,
        title: String,
        subject: String,
        classLevel: Int,
        provinceCode: String
    ): String {
        val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
        val coverFile = File(coversDir, "cover_fallback_${System.currentTimeMillis()}_${(100..999).random()}.jpg")

        val width = 480
        val height = 680
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Palette based on subject/province
        val primaryColor = when (subject.lowercase()) {
            "physics", "mathematics", "maths" -> AndroidColor.rgb(14, 116, 144) // Cyan/Blue
            "chemistry", "biology", "science" -> AndroidColor.rgb(15, 118, 110) // Teal
            "english", "urdu", "sindhi" -> AndroidColor.rgb(180, 83, 9) // Amber/Brown
            "computer science", "computer" -> AndroidColor.rgb(79, 70, 229) // Indigo
            else -> AndroidColor.rgb(5, 150, 105) // Emerald green
        }

        // Background
        val bgPaint = Paint().apply { color = AndroidColor.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Primary colored header & pattern
        val headerPaint = Paint().apply { color = primaryColor }
        canvas.drawRect(0f, 0f, width.toFloat(), 180f, headerPaint)

        // Board & Province Header
        val subHeaderPaint = Paint().apply {
            color = AndroidColor.WHITE
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val provName = when (provinceCode.lowercase()) {
            "kpk" -> "KPK TEXTBOOK BOARD PESHAWAR"
            "punjab" -> "PUNJAB CURRICULUM & TEXTBOOK BOARD"
            "sindh" -> "SINDH TEXTBOOK BOARD JAMSHORO"
            "balochistan" -> "BALOCHISTAN TEXTBOOK BOARD QUETTA"
            else -> "NATIONAL CURRICULUM COUNCIL PAKISTAN"
        }
        canvas.drawText(provName, 26f, 50f, subHeaderPaint)

        // Class Level Badge
        val classBadgePaint = Paint().apply {
            color = AndroidColor.rgb(255, 255, 255)
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val classText = if (classLevel > 0) "CLASS $classLevel" else "GENERAL REFERENCE"
        canvas.drawText(classText, 26f, 100f, classBadgePaint)

        // Subject Badge bar
        val barPaint = Paint().apply {
            color = AndroidColor.rgb(254, 243, 199)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawRect(0f, 175f, width.toFloat(), 180f, Paint().apply { color = AndroidColor.rgb(245, 158, 11) })

        // Main Title in Center Body
        val titlePaint = Paint().apply {
            color = AndroidColor.rgb(15, 23, 42)
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subjectPaint = Paint().apply {
            color = primaryColor
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Draw title wrapped lines
        var yPos = 240f
        canvas.drawText(subject.uppercase(), 30f, yPos, subjectPaint)
        yPos += 34f

        val words = title.split(" ")
        var line = ""
        for (w in words) {
            val testLine = if (line.isEmpty()) w else "$line $w"
            if (titlePaint.measureText(testLine) > (width - 60)) {
                canvas.drawText(line, 30f, yPos, titlePaint)
                yPos += 30f
                line = w
            } else {
                line = testLine
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, 30f, yPos, titlePaint)
            yPos += 30f
        }

        // Decorative Book emblem
        val emblemPaint = Paint().apply {
            color = primaryColor
            alpha = 40
        }
        canvas.drawRoundRect(RectF(30f, height - 190f, width - 30f, height - 80f), 16f, 16f, emblemPaint)

        val footerNotePaint = Paint().apply {
            color = primaryColor
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("STANDARD CURRICULUM SYLLABUS", 50f, height - 145f, footerNotePaint)
        canvas.drawText("Complete Solved Guide, Past Papers & Textbook", 50f, height - 115f, Paint().apply {
            color = AndroidColor.rgb(71, 85, 105)
            textSize = 11f
            isAntiAlias = true
        })

        // Book Spine Shadow on left edge
        val spinePaint = Paint().apply {
            color = AndroidColor.argb(50, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, 12f, height.toFloat(), spinePaint)

        // Outer border
        val borderPaint = Paint().apply {
            color = AndroidColor.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        try {
            FileOutputStream(coverFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing fallback cover: ${e.message}")
        } finally {
            bitmap.recycle()
        }

        return coverFile.absolutePath
    }
}
