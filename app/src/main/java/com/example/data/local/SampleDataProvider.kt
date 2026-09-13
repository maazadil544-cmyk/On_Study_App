package com.example.data.local

object SampleDataProvider {

    fun getInitialBooks(): List<BookEntity> {
        val books = mutableListOf<BookEntity>()
        val provinces = listOf("kpk", "punjab", "sindh", "balochistan")
        val googleDriveLink = "https://drive.google.com/file/d/1Oo5pIMwXYg6KvfoGlumpxxBrCbpmK6Bl/view?usp=drive_link"
        val driveCoverImage = "https://lh3.googleusercontent.com/d/1Oo5pIMwXYg6KvfoGlumpxxBrCbpmK6Bl=w800"

        // 1. Add General Reference Books (Grammar, Health Care, GK, Science, Computer, Urdu Qawaid)
        books.addAll(getGeneralBooks())

        // 2. Add "Test" book for Class 1 to 12 across all provinces
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
                        coverImage = driveCoverImage,
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

    fun getGeneralBooks(): List<BookEntity> {
        val googleDriveLink = "https://drive.google.com/file/d/1Oo5pIMwXYg6KvfoGlumpxxBrCbpmK6Bl/view?usp=drive_link"
        val driveCoverImage = "https://lh3.googleusercontent.com/d/1Oo5pIMwXYg6KvfoGlumpxxBrCbpmK6Bl=w800"

        return listOf(
            BookEntity(
                title = "English Grammar & Composition",
                provinceCode = "general",
                classLevel = 0,
                subject = "English Grammar",
                bookType = "GUIDE",
                fileLink = googleDriveLink,
                coverImage = driveCoverImage,
                fileSize = "5.8 MB",
                uploadDate = "2026-08",
                downloadCount = 2480,
                totalPages = 85,
                sampleContent = """
                    ENGLISH GRAMMAR & COMPOSITION - COMPLETE HANDBOOK
                    Comprehensive Study Guide for All Classes & Competitive Exams

                    ========================================
                    CHAPTER 1: THE PARTS OF SPEECH
                    ========================================
                    1. NOUN (اسم):
                       A noun is the name of a person, place, thing, or idea.
                       • Proper Noun: Specific name (e.g., Pakistan, Allama Iqbal, Lahore).
                       • Common Noun: General name (e.g., boy, city, book).
                       • Collective Noun: Group of individuals (e.g., team, flock, class, jury).
                       • Abstract Noun: Concept or feeling (e.g., honesty, wisdom, bravery, freedom).

                    2. PRONOUN (ضمير):
                       A word used in place of a noun.
                       • Personal: I, we, you, he, she, it, they.
                       • Demonstrative: This, that, these, those.
                       • Relative: Who, whom, which, whose, that.

                    3. VERB & ADVERB:
                       • Verb: Word expressing action or state of being (run, write, is, become).
                       • Adverb: Modifies a verb, adjective, or another adverb (quickly, very, gracefully).

                    4. PREPOSITIONS, CONJUNCTIONS & INTERJECTIONS:
                       • Prepositions show position and relationship: in, on, at, under, between, among.
                       • Conjunctions connect words or clauses: and, but, because, although, therefore.
                       • Interjections express sudden emotion: Hurrah!, Alas!, Wow!, Ouch!

                    ========================================
                    CHAPTER 2: MASTERING TENSES WITH FORMULAS
                    ========================================
                    1. PRESENT TENSES:
                       • Present Indefinite: Subject + V1(s/es) + Object.
                         Example: "He studies diligently every evening."
                       • Present Continuous: Subject + is/am/are + V1-ing + Object.
                         Example: "Students are preparing for their board exams."
                       • Present Perfect: Subject + has/have + V3 + Object.
                         Example: "She has completed her research paper."
                       • Present Perfect Continuous: Subject + has/have been + V1-ing + since/for.
                         Example: "They have been reading since morning."

                    2. PAST TENSES:
                       • Past Indefinite: Subject + V2 + Object.
                         Example: "The team won the science exhibition contest."
                       • Past Continuous: Subject + was/were + V1-ing + Object.
                       • Past Perfect: Subject + had + V3 + Object.

                    3. FUTURE TENSES:
                       • Future Indefinite: Subject + will/shall + V1 + Object.
                       • Future Continuous: Subject + will be + V1-ing + Object.
                       • Future Perfect: Subject + will have + V3 + Object.

                    ========================================
                    CHAPTER 3: ACTIVE & PASSIVE VOICE RULES
                    ========================================
                    Golden Conversion Rules:
                    1. Object of the Active sentence becomes the Subject of the Passive sentence.
                    2. Always use the 3rd form of the verb (V3 / Past Participle).
                    3. Use appropriate auxiliary helping verbs (is/am/are/was/were/been/being).
                    4. Add the preposition "by" before the agent.

                    Examples:
                    • Active: "The teacher solved the difficult equation."
                    • Passive: "The difficult equation was solved by the teacher."
                    • Active: "They are planting trees across the campus."
                    • Passive: "Trees are being planted across the campus by them."

                    ========================================
                    CHAPTER 4: DIRECT AND INDIRECT SPEECH
                    ========================================
                    Changes in Narration:
                    • Present Indefinite changes to Past Indefinite.
                    • Present Continuous changes to Past Continuous.
                    • Direct: He said, "I am writing an essay."
                    • Indirect: He said that he was writing an essay.
                    • Direct: Teacher said, "Honesty is the best policy."
                    • Indirect: Teacher said that honesty is the best policy (Universal truth remains unchanged).
                """.trimIndent()
            ),
            BookEntity(
                title = "Health Care & First Aid Guide",
                provinceCode = "general",
                classLevel = 0,
                subject = "Health & First Aid",
                bookType = "GUIDE",
                fileLink = googleDriveLink,
                coverImage = driveCoverImage,
                fileSize = "4.9 MB",
                uploadDate = "2026-08",
                downloadCount = 1890,
                totalPages = 68,
                sampleContent = """
                    HEALTH CARE & FIRST AID GUIDE (صحت اور ابتدائی طبی امداد)
                    Essential Emergency Procedures, Daily Hygiene & Wellness for Students

                    ========================================
                    باب اول: ابتدائی طبی امداد (FIRST AID ESSENTIALS)
                    ========================================
                    1. DRABC PROTOCOL FOR EMERGENCIES:
                       • D - Danger: Check the surroundings for safety.
                       • R - Response: Tap shoulders and ask "Are you okay?".
                       • A - Airway: Ensure the airway is clear of obstruction.
                       • B - Breathing: Look, listen, and feel for breathing.
                       • C - Circulation / CPR: Chest compressions if unresponsive.

                    2. WOUNDS & BLEEDING MANAGEMENT:
                       • Wash hands before touching open wounds.
                       • Apply firm, direct pressure with a clean sterile gauze or cloth.
                       • Elevate the injured limb above heart level to reduce blood flow.
                       • Clean gently with antiseptic lotion and secure with a sterile bandage.

                    3. BURNS TREATMENT (جلنے کی صورت میں):
                       • Cool the burn immediately under cool running tap water for at least 10 to 15 minutes.
                       • DO NOT apply ice, butter, oil, or toothpaste on fresh burns.
                       • Cover loosely with a clean non-stick dressing.

                    4. HEAT STROKE & DEHYDRATION (لو لگنا اور پانی کی کمی):
                       • Move patient immediately to a shaded, cool, well-ventilated area.
                       • Loosen tight clothing and apply cool damp cloths to neck and forehead.
                       • Administer Oral Rehydration Salts (ORS) or electrolyte water in small sips.

                    ========================================
                    باب دوم: متوازن غذا اور غذائیت (BALANCED NUTRITION)
                    ========================================
                    1. Essential Food Groups:
                       • Carbohydrates: Whole wheat, brown rice, oats for sustained energy.
                       • Proteins: Eggs, lentils (دالیں), milk, chicken, fish for muscle repair.
                       • Healthy Fats: Nuts (بادام، اخروٹ), seeds, olive oil for brain health.
                       • Vitamins & Minerals: Fresh green vegetables and citrus fruits for immune defense.

                    2. Daily Water Intake (پانی کا مناسب استعمال):
                       • Minimum 8 to 10 glasses of clean, filtered or boiled water daily.
                       • Avoid excessive sugary drinks, packaged juices, and caffeine.

                    ========================================
                    باب سوم: ذہنی صحت اور امتحانات کا دباؤ (MENTAL WELLNESS)
                    ========================================
                    1. Study Breaks & Pomodoro Technique:
                       • Study with focus for 25-30 minutes, then take a 5-minute restorative break.
                    2. Sleep Hygiene (نیند کی اہمیت):
                       • 7 to 8 hours of uninterrupted sleep every night is vital for memory consolidation.
                    3. Physical Activity:
                       • 30 minutes of brisk walking, light exercise, or outdoor sports daily.
                """.trimIndent()
            ),
            BookEntity(
                title = "General Knowledge & Everyday Science",
                provinceCode = "general",
                classLevel = 0,
                subject = "General Knowledge",
                bookType = "GUIDE",
                fileLink = googleDriveLink,
                coverImage = driveCoverImage,
                fileSize = "6.4 MB",
                uploadDate = "2026-08",
                downloadCount = 3120,
                totalPages = 95,
                sampleContent = """
                    GENERAL KNOWLEDGE & EVERYDAY SCIENCE (معلومات عامہ و سائنس)
                    Quick Reference Factbook for Competitive Exams & School Quizzes

                    ========================================
                    CHAPTER 1: EVERYDAY SCIENCE FUNDAMENTALS
                    ========================================
                    1. THE SOLAR SYSTEM & UNIVERSE:
                       • Closest Planet to Sun: Mercury.
                       • Hottest Planet: Venus (due to dense Greenhouse atmosphere).
                       • Red Planet: Mars (due to iron oxide dust).
                       • Largest Planet: Jupiter.
                       • Speed of Light: Approximately 300,000 km/second (3 × 10⁸ m/s).

                    2. HUMAN BIOLOGY & PHYSIOLOGY:
                       • Normal Human Body Temperature: 37°C (98.6°F).
                       • Normal Blood Pressure: 120/80 mmHg.
                       • Largest Organ of Human Body: Skin.
                       • Largest Internal Gland: Liver.
                       • Number of Bones in Adult Skeleton: 206 bones.
                       • Universal Blood Donor: O Negative (O-).
                       • Universal Blood Recipient: AB Positive (AB+).

                    3. PHYSICS & CHEMISTRY IN DAILY LIFE:
                       • Why is Sky Blue? Rayleigh scattering of shorter blue wavelengths by gas molecules.
                       • Chemical Formula of Water: H₂O.
                       • Chemical Formula of Common Salt: NaCl (Sodium Chloride).
                       • Most Abundant Gas in Earth's Atmosphere: Nitrogen (~78%), followed by Oxygen (~21%).

                    ========================================
                    CHAPTER 2: PAKISTAN AFFAIRS & GEOGRAPHY
                    ========================================
                    1. Geographic Landmarks:
                       • Highest Peak: K2 (Godwin-Austen) - 8,611 meters (2nd highest on Earth).
                       • Longest River: Indus River (دریائے سندھ) - ~3,180 km.
                       • Largest Desert: Thar Desert (Sindh & Punjab border).
                       • Deep Sea Port: Gwadar Port (Balochistan).

                    2. Key Historic Milestones:
                       • Lahore Resolution: March 23, 1940.
                       • Independence Day: August 14, 1947.
                       • First Governor-General: Quaid-e-Azam Muhammad Ali Jinnah.
                       • National Poet & Philosopher: Allama Muhammad Iqbal.
                       • Current Constitution Promulgated: 1973.

                    ========================================
                    CHAPTER 3: INTERNATIONAL ORGANIZATIONS
                    ========================================
                    • United Nations (UN): Founded on October 24, 1945. Headquarters: New York, USA.
                    • World Health Organization (WHO): Headquarters: Geneva, Switzerland.
                    • UNESCO: United Nations Educational, Scientific and Cultural Organization. HQ: Paris.
                    • OIC: Organization of Islamic Cooperation. HQ: Jeddah, Saudi Arabia.
                """.trimIndent()
            ),
            BookEntity(
                title = "Computer Literacy & Digital Skills",
                provinceCode = "general",
                classLevel = 0,
                subject = "Computer Skills",
                bookType = "GUIDE",
                fileLink = googleDriveLink,
                coverImage = driveCoverImage,
                fileSize = "5.2 MB",
                uploadDate = "2026-08",
                downloadCount = 1750,
                totalPages = 72,
                sampleContent = """
                    COMPUTER LITERACY & DIGITAL SKILLS (کمپیوٹر اور ڈیجیٹل مہارتیں)
                    Practical Handbook for Modern Students and Beginners

                    ========================================
                    CHAPTER 1: COMPUTING HARDWARE & ARCHITECTURE
                    ========================================
                    1. Central Processing Unit (CPU):
                       • Known as the "Brain of the Computer".
                       • Components: ALU (Arithmetic Logic Unit), CU (Control Unit), and Registers.

                    2. Primary vs Secondary Memory:
                       • RAM (Random Access Memory): Fast, Volatile temporary storage.
                       • ROM (Read Only Memory): Non-volatile, stores BIOS firmware.
                       • SSD vs HDD: Solid State Drives offer 5-10x faster read/write speeds than mechanical HDDs.

                    3. Input and Output Devices:
                       • Input: Keyboard, Optical Mouse, Microphone, Document Scanner.
                       • Output: Monitor (LCD/OLED), Laser Printer, Speakers, Projector.

                    ========================================
                    CHAPTER 2: ESSENTIAL PRODUCTIVITY SOFTWARE
                    ========================================
                    1. Microsoft Word & Document Editing:
                       • Formatting: Bold (Ctrl+B), Italic (Ctrl+I), Underline (Ctrl+U).
                       • Quick Alignments: Left (Ctrl+L), Center (Ctrl+E), Justify (Ctrl+J).
                       • Save (Ctrl+S), Print (Ctrl+P), Find & Replace (Ctrl+H).

                    2. Microsoft Excel & Spreadsheets:
                       • SUM: =SUM(A1:A10) to add numbers in a range.
                       • AVERAGE: =AVERAGE(B1:B20) to compute mean.
                       • IF Condition: =IF(C2>=50, "Pass", "Fail").

                    ========================================
                    CHAPTER 3: CYBER SECURITY & INTERNET SAFETY
                    ========================================
                    1. Strong Password Practices:
                       • Minimum 12 characters combining uppercase, lowercase, numbers, and symbols (@#$%).
                       • Never share One-Time Passwords (OTP) or banking credentials with anyone.
                    2. Phishing Awareness:
                       • Verify sender email addresses before clicking suspicious download links.
                       • Always check for "https://" lock icon before submitting private information online.
                """.trimIndent()
            ),
            BookEntity(
                title = "Urdu Qawaid-o-Insha",
                provinceCode = "general",
                classLevel = 0,
                subject = "Urdu Grammar",
                bookType = "GUIDE",
                fileLink = googleDriveLink,
                coverImage = driveCoverImage,
                fileSize = "4.5 MB",
                uploadDate = "2026-08",
                downloadCount = 2190,
                totalPages = 80,
                sampleContent = """
                    اردو قواعد و انشاء پردازی (URDU QAWAID-O-INSHA)
                    جامع اردو گرامر، خطوط نویسی اور مضامین برائے طلباء

                    ========================================
                    حصہ اول: کلمہ اور اس کی اقسام
                    ========================================
                    1. کلمہ:
                       بامعنی لفظ کو کلمہ کہتے ہیں (مثلاً: قلم، کتاب، پانی)۔
                       بےمعنی لفظ کو مہمل کہتے ہیں (مثلاً: روٹی ووٹی میں 'ووٹی')۔

                    2. کلمہ کی بنیادی اقسام:
                       • اسم: کسی شخص، جگہ یا چیز کے نام کو اسم کہتے ہیں (احمد، لاہور، درخت)۔
                       • فعل: وہ کلمہ جس میں کسی کام کا کرنا یا ہونا کسی زمانے کے تعلق سے پایا جائے (پڑھا، لکھتا ہے، جائے گا)۔
                       • حرف: وہ کلمہ جو تنہا کوئی مفہوم نہیں دیتا مگر دوسرے کلمات کو آپس میں ملاتا ہے (کا، کے، کی، پر، تک، سے)۔

                    3. اسم معرفہ اور اسم نکرہ:
                       • اسم معرفہ (خاص نام): جیسے قائد اعظم، دریائے سندھ، مینارِ پاکستان۔
                       • اسم نکرہ (عام نام): جیسے لڑکا، دریا، پہاڑ، شہر۔

                    ========================================
                    حصہ دوم: تذکیر و تانیث اور واحد جمع
                    ========================================
                    1. تذکیر و تانیث کے اہم اصول:
                       • تمام دنوں اور مہینوں کے نام مذکر بولے جاتے ہیں (سوائے جمعرات کے جو مؤنث ہے)۔
                       • دھاتوں کے نام مذکر ہیں (سونا، تانبا، لوہا) سوائے چاندی کے جو مؤنث ہے۔
                       • زبانوں اور کتابوں کے نام عموماً مؤنث ہوتے ہیں (اردو، انگریزی، تاریخ)۔

                    2. واحد سے جمع بنانے کے طریقے:
                       • الف یا ہ پر ختم ہونے والے مذکر الفاظ کو بڑی ے سے بدل دیں (لڑکا -> لڑکے، پرندہ -> پرندے)۔
                       • مؤنث الفاظ کے آخر میں الف اور نون غنہ کا اضافہ (کتاب -> کتابیں، رات -> راتیں)۔

                    ========================================
                    حصہ سوم: روزمرہ اور ضرب الامثال
                    ========================================
                    • آب آب ہونا -> بہت شرمندہ ہونا۔
                    • باغ باغ ہونا -> بے حد خوش ہونا۔
                    • چراغ پا ہونا -> سخت غصے میں آنا۔
                    • ناچ نہ جانے آنگن ٹیڑھا -> اپنی کمزوری کو چھپانے کے لیے دوسروں پر عیب لگانا۔
                    • اونٹ کے منہ میں زیرہ -> ضرورت سے بہت کم ملنا۔
                """.trimIndent()
            ),
            BookEntity(
                title = "Islamic Studies & Moral Ethics",
                provinceCode = "general",
                classLevel = 0,
                subject = "Islamic & Ethics",
                bookType = "GUIDE",
                fileLink = googleDriveLink,
                coverImage = driveCoverImage,
                fileSize = "5.0 MB",
                uploadDate = "2026-08",
                downloadCount = 2050,
                totalPages = 78,
                sampleContent = """
                    اسلامیات اور اخلاقی اقدار (ISLAMIC STUDIES & MORAL ETHICS)
                    سیرت النبی ﷺ، اسلامی تعلیمات اور حسن اخلاق کا رہنما مجموعہ

                    ========================================
                    باب اول: بنیادی ارکان و عقائد
                    ========================================
                    1. عقیدہ توحید (ONENESS OF ALLAH):
                       اللہ تعالیٰ ایک ہے، اس کا کوئی شریک نہیں۔ وہی خالق، مالک اور رازق کائنات ہے۔

                    2. عقیدہ رسالت اور ختم نبوت:
                       حضرت محمد مصطفیٰ ﷺ اللہ کے آخری نبی اور رسول ہیں۔ آپ ﷺ پر نبوت کا سلسلہ مکمل ہو چکا ہے۔

                    3. ارکانِ اسلام:
                       • کلمہ طیبہ (شہادت)
                       • نماز (قائم کرنا)
                       • زکوٰۃ (ادا کرنا)
                       • روزہ (رمضان المبارک کے روزے رکھنا)
                       • حج (استطاعت رکھنے والوں کے لیے بیت اللہ کا حج)

                    ========================================
                    باب دوم: سیرتِ طیبہ ﷺ اور حسنِ اخلاق
                    ========================================
                    1. صادق اور امین کا لقب:
                       نبی کریم ﷺ اعلانِ نبوت سے قبل ہی مکہ مکرمہ میں سچائی اور امانت داری کی علامت سمجھے جاتے تھے۔

                    2. عفو و درگزر اور حسنِ سلوک:
                       فتح مکہ کے موقع پر رسول اللہ ﷺ نے اپنے سخت ترین دشمنوں کو معاف فرما کر تاریخ میں بے مثال اخلاق کی مثال قائم کی۔

                    ========================================
                    باب سوم: حقوق العباد اور معاشرتی ذمہ داریاں
                    ========================================
                    1. والدین کی خدمت اور اطاعت:
                       قرآن مجید میں اللہ تعالیٰ کی عبادت کے ساتھ والدین کے ساتھ حسن سلوک کا حکم دیا گیا ہے۔
                    2. اساتذہ اور علم کا احترام:
                       استاد کو روحانی باپ کا درجہ حاصل ہے۔ علم حاصل کرنا ہر مسلمان مرد اور عورت پر فرض ہے۔
                    3. پڑوسیوں کے حقوق اور کمزوروں کی مدد:
                       پڑوسی خواہ رشتہ دار ہو یا اجنبی، اس کے آرام اور دکھ سکھ کا خیال رکھنا اسلامی تعلیمات کا لازمی حصہ ہے۔
                """.trimIndent()
            )
        )
    }

    fun getInitialNews(): List<NewsEntity> {
        return listOf(
            NewsEntity(
                title = "New Academic Curriculum & General Guides Released",
                description = "Updated textbooks, English Grammar handbook, Health Care & First Aid guide, and solved notes are now available with direct cloud links.",
                category = "New Guide",
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

