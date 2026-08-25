package com.example.data.local

object SampleDataProvider {

    fun getInitialBooks(): List<BookEntity> {
        val books = mutableListOf<BookEntity>()
        val provinces = listOf("kpk", "punjab", "sindh", "balochistan")
        val googleDriveLink = "https://drive.google.com/file/d/1Oo5pIMwXYg6KvfoGlumpxxBrCbpmK6Bl/view?usp=drive_link"

        // Add "Test" book for Class 1 to 12 across all provinces
        for (prov in provinces) {
            for (lvl in 1..12) {
                books.add(
                    BookEntity(
                        title = "Test",
                        provinceCode = prov,
                        classLevel = lvl,
                        subject = if (lvl == 1) "Primary Studies" else "General Knowledge & Subject Guide",
                        bookType = "TEXTBOOK",
                        fileLink = googleDriveLink,
                        fileSize = "4.2 MB",
                        uploadDate = "2026-08",
                        downloadCount = 1050,
                        totalPages = 24,
                        sampleContent = """
                            Test Book - Class $lvl
                            Province: ${prov.uppercase()} Textbook Board
                            Google Drive Resource: $googleDriveLink
                        """.trimIndent()
                    )
                )
            }
        }

        return books
    }

    fun getInitialNews(): List<NewsEntity> {
        return listOf(
            NewsEntity(
                title = "New Academic Curriculum & Guide Released",
                description = "Updated textbooks and solved guides are now available for all classes (1 to 12) across KPK, Punjab, Sindh, and Balochistan. Access textbooks with direct cloud links.",
                category = "Syllabus Update",
                date = "Aug 2026",
                boardName = "National Academic Council",
                isUnread = true,
                targetClass = "All Classes"
            ),
            NewsEntity(
                title = "Board Exams Schedule & Solved Papers",
                description = "Annual examination date sheets and practice test books have been released for primary, middle, matric, and intermediate levels.",
                category = "Date Sheet",
                date = "Aug 2026",
                boardName = "All Pakistan BISE Board Network",
                isUnread = true,
                targetClass = "All Classes"
            )
        )
    }

    fun getInitialNotes(): List<NoteEntity> {
        return emptyList()
    }
}
