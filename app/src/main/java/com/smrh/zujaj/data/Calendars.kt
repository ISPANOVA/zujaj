package com.smrh.zujaj.data

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/** Day / month names and Hijri conversion used by the widgets. */
object Calendars {
    val daysEn = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val daysAr = listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")
    val monthsEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    /** Egyptian-style Gregorian month names */
    val monthsAr = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    val hijriEn = listOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Ula", "Jumada al-Akhirah",
        "Rajab", "Shaban", "Ramadan", "Shawwal", "Dhu al-Qadah", "Dhu al-Hijjah"
    )
    val hijriAr = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    data class HDate(val day: Int, val month: Int /*1..12*/, val year: Int)

    fun hijri(date: LocalDate, offset: Int = Prefs.hijriOffset): HDate {
        return try {
            val h = HijrahDate.from(date.plusDays(offset.toLong()))
            HDate(h.get(ChronoField.DAY_OF_MONTH), h.get(ChronoField.MONTH_OF_YEAR), h.get(ChronoField.YEAR))
        } catch (e: Exception) {
            // Outside the supported Umm al-Qura range: fall back to arithmetic (tabular) calendar
            tabular(date.plusDays(offset.toLong()))
        }
    }

    private fun tabular(d: LocalDate): HDate {
        val jd = d.toEpochDay() + 2440588 // Julian day number
        val l0 = jd - 1948440 + 10632
        val n = (l0 - 1) / 10631
        val l1 = l0 - 10631 * n + 354
        val j = ((10985 - l1) / 5316) * ((50 * l1) / 17719) + (l1 / 5670) * ((43 * l1) / 15238)
        val l2 = l1 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val m = (24 * l2) / 709
        val day = l2 - (709 * m) / 24
        val y = 30 * n + j - 30
        return HDate(day.toInt(), m.toInt(), y.toInt())
    }

    fun dayName(d: LocalDate, ar: Boolean) = (if (ar) daysAr else daysEn)[d.dayOfWeek.value - 1]
    fun monthName(m: Int, ar: Boolean) = (if (ar) monthsAr else monthsEn)[m - 1]
    fun hijriMonth(m: Int, ar: Boolean) = (if (ar) hijriAr else hijriEn)[(m - 1).coerceIn(0, 11)]

    /** Fully vowelled forms used by the calligraphy fonts. */
    private val tashkeelMap: Map<String, String> = mapOf(
        "الأحد" to "الأَحَد", "الإثنين" to "الإِثْنَيْن", "الثلاثاء" to "الثُّلَاثَاء", "الأربعاء" to "الأَرْبِعَاء",
        "الخميس" to "الخَمِيس", "الجمعة" to "الجُمُعَة", "السبت" to "السَّبْت",
        "محرم" to "مُحَرَّم", "صفر" to "صَفَر", "ربيع الأول" to "رَبِيع الأَوَّل", "ربيع الآخر" to "رَبِيع الآخِر",
        "جمادى الأولى" to "جُمَادَى الأُولَى", "جمادى الآخرة" to "جُمَادَى الآخِرَة", "رجب" to "رَجَب", "شعبان" to "شَعْبَان",
        "رمضان" to "رَمَضَان", "شوال" to "شَوَّال", "ذو القعدة" to "ذُو القَعْدَة", "ذو الحجة" to "ذُو الحِجَّة",
        "يناير" to "يَنَايِر", "فبراير" to "فِبْرَايِر", "مارس" to "مَارِس", "أبريل" to "أَبْرِيل", "مايو" to "مَايُو",
        "يونيو" to "يُونْيُو", "يوليو" to "يُولْيُو", "أغسطس" to "أَغُسْطُس", "سبتمبر" to "سِبْتَمْبِر",
        "أكتوبر" to "أُكْتُوبَر", "نوفمبر" to "نُوفَمْبِر", "ديسمبر" to "دِيسَمْبِر",
        "الفجر" to "الفَجْر", "الشروق" to "الشُّرُوق", "الظهر" to "الظُّهْر", "العصر" to "العَصْر",
        "المغرب" to "المَغْرِب", "العشاء" to "العِشَاء", "رمضان كريم" to "رَمَضَان كَرِيم",
    )

    fun tashkeel(s: String): String = tashkeelMap[s] ?: s

    fun toArabicDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') ('٠' + (c - '0')) else c)
        return sb.toString()
    }
}
