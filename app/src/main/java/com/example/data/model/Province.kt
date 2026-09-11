package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class Province(
    val code: String,
    val monogram: String,
    val title: String,
    val urduName: String,
    val boardName: String,
    val primaryColor: Color,
    val containerColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val description: String
) {
    KPK(
        code = "kpk",
        monogram = "KP",
        title = "KPK",
        urduName = "خیبر پختونخوا",
        boardName = "KPK Textbook Board, Peshawar",
        primaryColor = ProvinceKpkBlue,
        containerColor = ProvinceKpkBlueBg,
        borderColor = ProvinceKpkBlueBorder,
        textColor = ProvinceKpkBlueText,
        description = "Peshawar, Mardan, Swat, Abbottabad, Bannu, Malakand, D.I. Khan Boards"
    ),
    PUNJAB(
        code = "punjab",
        monogram = "PB",
        title = "Punjab",
        urduName = "پنجاب",
        boardName = "Punjab Curriculum & Textbook Board (PCTB), Lahore",
        primaryColor = ProvincePunjabGreen,
        containerColor = ProvincePunjabGreenBg,
        borderColor = ProvincePunjabGreenBorder,
        textColor = ProvincePunjabGreenText,
        description = "Lahore, Rawalpindi, Faisalabad, Multan, Gujranwala, Bahawalpur, Sargodha, Sahiwal Boards"
    ),
    BALOCHISTAN(
        code = "balochistan",
        monogram = "BL",
        title = "Balochistan",
        urduName = "بلوچستان",
        boardName = "Balochistan Textbook Board (BTBB), Quetta",
        primaryColor = ProvinceBalochistanOrange,
        containerColor = ProvinceBalochistanOrangeBg,
        borderColor = ProvinceBalochistanOrangeBorder,
        textColor = ProvinceBalochistanOrangeText,
        description = "Quetta, Turbat, Loralai, Khuzdar, Zhob Boards"
    ),
    SINDH(
        code = "sindh",
        monogram = "SD",
        title = "Sindh",
        urduName = "سندھ",
        boardName = "Sindh Textbook Board (STBB), Jamshoro",
        primaryColor = ProvinceSindhPurple,
        containerColor = ProvinceSindhPurpleBg,
        borderColor = ProvinceSindhPurpleBorder,
        textColor = ProvinceSindhPurpleText,
        description = "Karachi, Hyderabad, Sukkur, Larkana, Mirpurkhas Boards"
    ),
    GENERAL(
        code = "general",
        monogram = "GB",
        title = "General Books",
        urduName = "عام کتب",
        boardName = "Grammar, Health & Reference Library",
        primaryColor = Color(0xFF0D9488),
        containerColor = Color(0xFFF0FDFA),
        borderColor = Color(0xFF99F6E4),
        textColor = Color(0xFF115E59),
        description = "English Grammar, Health Care & First Aid, GK, Science & Computer"
    );

    companion object {
        fun fromCode(code: String): Province {
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) || it.name.equals(code, ignoreCase = true) } ?: PUNJAB
        }
    }
}

enum class BookType(val displayName: String, val badgeColor: Color) {
    TEXTBOOK("Textbook", Color(0xFF0284C7)),
    GUIDE("Guide & Keybook", Color(0xFF10B981)),
    NOTES("Solved Notes", Color(0xFFF97316)),
    PAST_PAPERS("Past Papers", Color(0xFFA855F7));

    companion object {
        fun fromString(value: String): BookType {
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) } ?: TEXTBOOK
        }
    }
}

