package com.smrh.zujaj.catalog

import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.Texture
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.AppsR
import com.smrh.zujaj.render.ClockR
import com.smrh.zujaj.render.CounterR
import com.smrh.zujaj.render.DateR
import com.smrh.zujaj.render.DigitalR
import com.smrh.zujaj.render.IslamicR
import com.smrh.zujaj.render.MediaR
import com.smrh.zujaj.render.PeopleR
import com.smrh.zujaj.render.PhotoR
import com.smrh.zujaj.render.PrayerR
import com.smrh.zujaj.render.QuoteR
import com.smrh.zujaj.render.RenderCtx
import com.smrh.zujaj.render.SearchR
import com.smrh.zujaj.render.SystemR
import com.smrh.zujaj.render.ToolsR
import com.smrh.zujaj.render.WeatherR

/** Grid footprint and the design size (dp) every renderer lays out against. */
enum class Shape(val cols: Int, val rows: Int, val dw: Float, val dh: Float) {
    SMALL(2, 2, 160f, 160f),
    WIDE(4, 2, 340f, 160f),
    BANNER(4, 1, 340f, 84f),
    LARGE(4, 3, 340f, 250f),
}

enum class Cat(val en: String, val ar: String) {
    CLOCK("Clock", "ساعات"),
    DIGITAL("Digital", "رقمية"),
    DATE("Date", "التاريخ"),
    WEATHER("Weather", "الطقس"),
    PRAYER("Prayer", "الصلاة"),
    ISLAMIC("Islamic", "إسلامي"),
    MEDIA("Media", "الميديا"),
    PHOTO("Photo", "صور"),
    COUNTERS("Counters", "عدادات"),
    APPS("Apps", "تطبيقات"),
    PEOPLE("People", "أشخاص"),
    SEARCH("Search", "بحث"),
    SYSTEM("System", "النظام"),
    QUOTES("Quotes", "اقتباسات"),
    TOOLS("Tools", "أدوات"),
}

/** Extra editor controls a style needs (beyond the common ones). */
enum class Feat { COLOR2, ACCENT, SIZE2, CALENDAR, HOUR24, SECONDS, TITLE, TEXT, AUTHOR, TARGET_DATE, GOAL, APPS, CONTACTS, PHOTOS, CITIES, GAP }

/** What makes a widget need refreshing. */
enum class Upd { DAILY, PRAYER, WEATHER, MEDIA, SYSTEM, PHOTO, NONE }

class WidgetStyle(
    val id: String,
    val cat: Cat,
    val en: String,
    val ar: String,
    val shape: Shape,
    val pro: Boolean = false,
    val isNew: Boolean = false,
    val popular: Boolean = false,
    val feats: Set<Feat> = emptySet(),
    val maxItems: Int = 0,
    val upd: Set<Upd> = setOf(Upd.DAILY),
    val defaults: (WidgetConfig) -> WidgetConfig = { it },
    val draw: (RenderCtx) -> Unit,
) {
    fun name(ar: Boolean) = if (ar) this.ar else en
    fun defaultConfig(): WidgetConfig = defaults(WidgetConfig(styleId = id))
}

object Catalog {
    private val S = Shape.SMALL
    private val W = Shape.WIDE
    private val B = Shape.BANNER
    private val L = Shape.LARGE

    private val clockF = setOf(Feat.ACCENT, Feat.COLOR2, Feat.SECONDS, Feat.HOUR24)
    private val timeF = setOf(Feat.COLOR2, Feat.HOUR24, Feat.SIZE2)
    private val prayerF = setOf(Feat.COLOR2, Feat.HOUR24, Feat.ACCENT)
    private val pU = setOf(Upd.PRAYER, Upd.DAILY)
    private val wU = setOf(Upd.WEATHER, Upd.DAILY)
    private val mU = setOf(Upd.MEDIA)
    private val sysU = setOf(Upd.SYSTEM)
    private val none = setOf(Upd.NONE)

    private fun glass(c: WidgetConfig) = c.copy(fill = Fill.GLASS, texture = Texture.NOISE)
    private fun aurora(c: WidgetConfig) = c.copy(fill = Fill.AURORA, bgColor = 0xFF0E0E16.toInt(), bgColor2 = 0xFF4C3BCF.toInt(), accent = 0xFFE8478B.toInt())

    val all: List<WidgetStyle> = listOf(
        // ------------------------------------------------------------------ CLOCK
        WidgetStyle("clock_dots", Cat.CLOCK, "Clock Dots", "ساعة النقاط", S, popular = true, feats = clockF, upd = none) { ClockR.dots(it) },
        WidgetStyle("clock_fill", Cat.CLOCK, "Clock Fill", "ساعة ممتلئة", S, feats = clockF, upd = none) { ClockR.fill(it) },
        WidgetStyle("clock_minimal", Cat.CLOCK, "Clock Minimal", "ساعة بسيطة", S, feats = clockF, upd = none) { ClockR.minimal(it) },
        WidgetStyle("clock_modern", Cat.CLOCK, "Clock Modern", "ساعة عصرية", S, popular = true, feats = clockF, upd = none) { ClockR.modern(it) },
        WidgetStyle("clock_flat", Cat.CLOCK, "Clock Flat", "ساعة مسطحة", S, feats = clockF, upd = none) { ClockR.flat(it) },
        WidgetStyle("clock_relief", Cat.CLOCK, "Clock Relief", "ساعة بارزة", S, pro = true, feats = clockF, upd = none) { ClockR.relief(it) },
        WidgetStyle("clock_lines", Cat.CLOCK, "Clock Lines", "ساعة الخطوط", S, pro = true, feats = clockF, upd = none) { ClockR.lines(it) },
        WidgetStyle("clock_depth", Cat.CLOCK, "Clock Depth", "ساعة العمق", S, pro = true, feats = clockF, upd = none) { ClockR.depth(it) },
        WidgetStyle("clock_style", Cat.CLOCK, "Clock Style", "ساعة أنيقة", S, pro = true, feats = clockF, upd = none) { ClockR.styled(it) },
        WidgetStyle("clock_gradient", Cat.CLOCK, "Clock Gradient", "ساعة متدرجة", S, pro = true, feats = clockF, upd = none) { ClockR.gradient(it) },
        WidgetStyle("clock_roman", Cat.CLOCK, "Clock Roman", "ساعة رومانية", S, pro = true, isNew = true, feats = clockF, upd = none) { ClockR.roman(it) },
        WidgetStyle("clock_arabic", Cat.CLOCK, "Clock Arabic", "ساعة عربية", S, isNew = true, feats = clockF, upd = none) { ClockR.arabicNumerals(it) },
        WidgetStyle("clock_orbit", Cat.CLOCK, "Clock Orbit", "ساعة المدار", S, pro = true, isNew = true, feats = clockF, upd = none) { ClockR.orbit(it) },
        WidgetStyle("clock_classic", Cat.CLOCK, "Clock Classic", "ساعة كلاسيك", S, popular = true, feats = clockF, upd = none) { ClockR.classic(it) },
        WidgetStyle("clock_bold", Cat.CLOCK, "Clock Bold", "ساعة عريضة الأرقام", S, feats = clockF, upd = none) { ClockR.bold(it) },
        WidgetStyle("clock_arabesque", Cat.CLOCK, "Clock Arabesque", "ساعة أرابيسك", S, pro = true, isNew = true, popular = true, feats = clockF, upd = none) { ClockR.arabesque(it) },
        WidgetStyle("clock_damascene", Cat.CLOCK, "Clock Damascene", "ساعة دمشقية", S, pro = true, isNew = true, feats = clockF, upd = none) { ClockR.damascene(it) },
        WidgetStyle("clock_radiant", Cat.CLOCK, "Clock Radiant", "ساعة النجمة", S, pro = true, isNew = true, feats = clockF, upd = none) { ClockR.radiant(it) },
        WidgetStyle("clock_calligraphy", Cat.CLOCK, "Clock Calligraphy", "ساعة الخط", S, pro = true, isNew = true, feats = clockF, upd = none) { ClockR.calligraphy(it) },
        WidgetStyle("clock_sunburst", Cat.CLOCK, "Clock Sunburst", "ساعة الإشعاع", S, pro = true, isNew = true, feats = clockF, upd = none, defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { ClockR.sunburst(it) },
        WidgetStyle("clock_hindi", Cat.CLOCK, "Clock Hindi", "ساعة أرقام هندية", S, isNew = true, feats = clockF, upd = none) { ClockR.hindi(it) },
        WidgetStyle("clock_wide_date", Cat.CLOCK, "Clock Wide Date", "ساعة عريضة بالتاريخ", W, pro = true, isNew = true, feats = clockF) { ClockR.wideDate(it) },
        WidgetStyle("clock_wide", Cat.CLOCK, "Clock Wide Digital", "ساعة عريضة", W, pro = true, popular = true, feats = clockF, upd = none) { ClockR.wide(it) },

        // ------------------------------------------------------------------ DIGITAL
        WidgetStyle("digital_clock", Cat.DIGITAL, "Digital Clock", "ساعة رقمية", W, popular = true, feats = timeF, upd = none) { DigitalR.clock(it) },
        WidgetStyle("digital_stacked", Cat.DIGITAL, "Digital Stacked", "ساعة مكدسة", S, isNew = true, feats = timeF, upd = none) { DigitalR.stacked(it) },
        WidgetStyle("digital_tall", Cat.DIGITAL, "Digital Tall", "ساعة طويلة", S, feats = timeF, upd = none) { DigitalR.tall(it) },
        WidgetStyle("digital_world", Cat.DIGITAL, "Digital World", "توقيت العالم", B, pro = true, feats = timeF + Feat.CITIES, maxItems = 2, upd = none) { DigitalR.worldBanner(it) },
        WidgetStyle("digital_world_split", Cat.DIGITAL, "World Split", "توقيت مزدوج", W, feats = timeF + Feat.CITIES, maxItems = 2, upd = none) { DigitalR.worldSplit(it) },
        WidgetStyle("digital_world_stack", Cat.DIGITAL, "World Compact", "توقيت مختصر", S, pro = true, feats = timeF + Feat.CITIES, maxItems = 2, upd = none) { DigitalR.worldStack(it) },
        WidgetStyle("digital_world3", Cat.DIGITAL, "World Trio", "ثلاث مدن", W, pro = true, isNew = true, feats = timeF + Feat.CITIES, maxItems = 3, upd = none) { DigitalR.worldTrio(it) },
        WidgetStyle("digital_weather", Cat.DIGITAL, "Digital Weather", "ساعة وطقس", L, pro = true, popular = true, feats = timeF, upd = wU) { DigitalR.weather(it) },
        WidgetStyle("digital_neon", Cat.DIGITAL, "Digital Neon", "ساعة نيون", W, pro = true, isNew = true, feats = timeF + Feat.ACCENT, upd = none, defaults = ::aurora) { DigitalR.neon(it) },

        // ------------------------------------------------------------------ DATE
        WidgetStyle("banner_simple", Cat.DATE, "Banner Simple", "شريط التاريخ", B, feats = setOf(Feat.CALENDAR)) { DateR.bannerSimple(it) },
        WidgetStyle("banner_weather", Cat.DATE, "Banner Weather", "شريط وطقس", B, pro = true, feats = timeF, upd = wU) { DateR.bannerWeather(it) },
        WidgetStyle("date_calendar", Cat.DATE, "Date Calendar", "التقويم", S, popular = true, feats = setOf(Feat.COLOR2, Feat.ACCENT)) { DateR.calendarSmall(it) },
        WidgetStyle("date_calendar_wide", Cat.DATE, "Calendar Wide", "تقويم عريض", L, feats = setOf(Feat.COLOR2, Feat.ACCENT)) { DateR.calendarWide(it) },
        WidgetStyle("dots_month", Cat.DATE, "Dots Month", "نقاط الشهر", S, pro = true, feats = setOf(Feat.COLOR2)) { DateR.dotsMonth(it) },
        WidgetStyle("dots_year", Cat.DATE, "Dots Year", "نقاط السنة", W, feats = setOf(Feat.COLOR2)) { DateR.dotsYear(it) },
        WidgetStyle("days_elegant", Cat.DATE, "Days Elegant", "اليوم الأنيق", W, popular = true, feats = setOf(Feat.SIZE2)) { DateR.daysElegant(it) },
        WidgetStyle("days_minimal", Cat.DATE, "Days Minimal", "اليوم المختصر", S) { DateR.daysMinimal(it) },
        WidgetStyle("days_thuluth", Cat.DATE, "Days Calligraphy", "اليوم بالخط العربي", W, isNew = true, popular = true) { DateR.daysCalli(it) },
        WidgetStyle("days_thuluth_small", Cat.DATE, "Day Calligraphy", "اليوم خط صغير", S) { DateR.daysCalliSmall(it) },
        WidgetStyle("months_elegant", Cat.DATE, "Months Elegant", "الشهر الأنيق", W, feats = setOf(Feat.CALENDAR, Feat.COLOR2)) { DateR.monthsElegant(it) },
        WidgetStyle("months_thuluth", Cat.DATE, "Months Calligraphy", "الشهر بالخط العربي", W, isNew = true, feats = setOf(Feat.CALENDAR, Feat.COLOR2, Feat.SIZE2), defaults = { it.copy(calendar = com.smrh.zujaj.model.Cal.HIJRI) }) { DateR.monthsCalli(it) },
        WidgetStyle("months_thuluth_small", Cat.DATE, "Month Calligraphy", "الشهر خط صغير", S, pro = true, feats = setOf(Feat.CALENDAR, Feat.COLOR2)) { DateR.monthsCalliSmall(it) },
        WidgetStyle("date_calli_full", Cat.DATE, "Date Calligraphy", "التاريخ بالخط", W, isNew = true, popular = true) { DateR.calliFull(it) },
        WidgetStyle("dual_banner", Cat.DATE, "Dual Banner", "هجري وميلادي", B, pro = true, popular = true) { DateR.dualBanner(it) },
        WidgetStyle("hijri_gregorian", Cat.DATE, "Hijri & Gregorian", "التاريخين", B) { DateR.hijriGregorian(it) },
        WidgetStyle("date_compact", Cat.DATE, "Date Compact", "تاريخ مختصر", S, feats = setOf(Feat.CALENDAR, Feat.SIZE2)) { DateR.compact(it) },
        WidgetStyle("date_week", Cat.DATE, "Week Strip", "شريط الأسبوع", B, isNew = true, feats = setOf(Feat.ACCENT)) { DateR.weekStrip(it) },

        // ------------------------------------------------------------------ WEATHER
        WidgetStyle("weather_compact", Cat.WEATHER, "Weather Compact", "طقس مختصر", B, upd = wU) { WeatherR.compactBanner(it) },
        WidgetStyle("weather_dual", Cat.WEATHER, "Weather Dual", "طقس مدينتين", B, pro = true, feats = setOf(Feat.CITIES), maxItems = 1, upd = wU) { WeatherR.dual(it) },
        WidgetStyle("weather_small", Cat.WEATHER, "Weather Card", "كارت الطقس", S, upd = wU) { WeatherR.compactSmall(it) },
        WidgetStyle("weather_simple", Cat.WEATHER, "Weather Simple", "طقس بسيط", S, pro = true, upd = wU) { WeatherR.simple(it) },
        WidgetStyle("weather_detail", Cat.WEATHER, "Weather Detail", "تفاصيل الطقس", W, pro = true, popular = true, feats = setOf(Feat.HOUR24), upd = wU) { WeatherR.detail(it) },
        WidgetStyle("weather_split", Cat.WEATHER, "Weather Split", "طقس منقسم", W, pro = true, feats = setOf(Feat.CITIES), maxItems = 1, upd = wU) { WeatherR.split(it) },
        WidgetStyle("weather_big", Cat.WEATHER, "Weather Big", "طقس كبير", W, isNew = true, upd = wU, defaults = ::aurora) { WeatherR.big(it) },

        // ------------------------------------------------------------------ PRAYER
        WidgetStyle("prayer_simple", Cat.PRAYER, "Prayer Simple", "الصلاة القادمة", B, popular = true, feats = prayerF, upd = pU) { PrayerR.simple(it) },
        WidgetStyle("prayer_strip", Cat.PRAYER, "Prayer Strip", "شريط الصلوات", B, feats = prayerF, upd = pU) { PrayerR.strip(it, 0) },
        WidgetStyle("prayer_pill", Cat.PRAYER, "Prayer Pill", "مواقيت كبسولة", B, feats = prayerF, upd = pU) { PrayerR.strip(it, 1) },
        WidgetStyle("prayer_dots", Cat.PRAYER, "Prayer Dots", "مواقيت بالنقاط", B, pro = true, feats = prayerF, upd = pU) { PrayerR.strip(it, 2) },
        WidgetStyle("prayer_pair", Cat.PRAYER, "Prayer Calligraphy", "الصلاة بالخط", W, popular = true, feats = prayerF, upd = pU) { PrayerR.pair(it) },
        WidgetStyle("prayer_minimal", Cat.PRAYER, "Prayer Minimal", "صلاة مختصرة", W, feats = prayerF, upd = pU) { PrayerR.minimal(it) },
        WidgetStyle("prayer_progress", Cat.PRAYER, "Prayer Progress", "تقدم الصلوات", B, pro = true, feats = prayerF, upd = pU) { PrayerR.progressStrip(it) },
        WidgetStyle("prayer_countdown", Cat.PRAYER, "Prayer Countdown", "العد التنازلي", B, pro = true, feats = prayerF, upd = pU) { PrayerR.countdown(it) },
        WidgetStyle("prayer_dual", Cat.PRAYER, "Prayer Dual", "ثلاث صلوات", B, feats = prayerF, upd = pU) { PrayerR.dual(it) },
        WidgetStyle("prayer_banner", Cat.PRAYER, "Prayer Banner", "بانر الصلاة", B, feats = prayerF, upd = pU) { PrayerR.banner(it) },
        WidgetStyle("prayer_compact", Cat.PRAYER, "Prayer Ring", "حلقة الصلاة", S, pro = true, feats = prayerF, upd = pU) { PrayerR.ring(it, false) },
        WidgetStyle("prayer_ring_calli", Cat.PRAYER, "Prayer Ring Calligraphy", "حلقة بالخط", S, pro = true, feats = prayerF, upd = pU) { PrayerR.ring(it, true) },
        WidgetStyle("prayer_table", Cat.PRAYER, "Prayer Table", "جدول الصلاة", L, pro = true, feats = prayerF, upd = wU + pU) { PrayerR.table(it) },
        WidgetStyle("prayer_next", Cat.PRAYER, "Prayer Next", "الصلاة التالية", L, feats = prayerF, upd = pU) { PrayerR.next(it) },
        WidgetStyle("prayer_grid", Cat.PRAYER, "Prayer Day", "يوم الصلاة", L, pro = true, popular = true, feats = prayerF, upd = pU) { PrayerR.dayGrid(it) },
        WidgetStyle("prayer_detail", Cat.PRAYER, "Prayer Detail", "تفاصيل الصلاة", W, feats = prayerF, upd = pU) { PrayerR.detail(it) },
        WidgetStyle("prayer_list", Cat.PRAYER, "Prayer List", "قائمة الصلوات", L, pro = true, feats = prayerF, upd = pU) { PrayerR.list(it) },
        WidgetStyle("prayer_spotlight", Cat.PRAYER, "Prayer Spotlight", "الصلاة بالخط الكبير", W, pro = true, isNew = true, popular = true, feats = prayerF, upd = pU) { PrayerR.spotlight(it) },
        WidgetStyle("prayer_tiles", Cat.PRAYER, "Prayer Tiles", "مربعات الصلاة", L, pro = true, isNew = true, feats = prayerF, upd = pU) { PrayerR.tiles(it) },

        // ------------------------------------------------------------------ ISLAMIC (new)
        WidgetStyle("tasbeeh", Cat.ISLAMIC, "Tasbeeh Counter", "السبحة", S, isNew = true, popular = true, feats = setOf(Feat.GOAL, Feat.TEXT, Feat.ACCENT), upd = none, defaults = { it.copy(goal = 33, text = "سبحان الله") }) { IslamicR.tasbeeh(it) },
        WidgetStyle("tasbeeh_wide", Cat.ISLAMIC, "Tasbeeh Wide", "سبحة عريضة", W, pro = true, isNew = true, feats = setOf(Feat.GOAL, Feat.TEXT, Feat.ACCENT), upd = none, defaults = { it.copy(goal = 100, text = "أستغفر الله") }) { IslamicR.tasbeehWide(it) },
        WidgetStyle("dhikr_daily", Cat.ISLAMIC, "Daily Dhikr", "ذكر اليوم", W, isNew = true) { IslamicR.dhikr(it) },
        WidgetStyle("moon_phase", Cat.ISLAMIC, "Moon Phase", "أطوار القمر", S, isNew = true) { IslamicR.moon(it) },
        WidgetStyle("qibla", Cat.ISLAMIC, "Qibla", "اتجاه القبلة", S, isNew = true) { IslamicR.qibla(it) },
        WidgetStyle("ramadan_countdown", Cat.ISLAMIC, "Ramadan Countdown", "العد لرمضان", B, pro = true, isNew = true) { IslamicR.ramadan(it) },
        WidgetStyle("hijri_big", Cat.ISLAMIC, "Hijri Day", "اليوم الهجري", S, isNew = true) { IslamicR.hijriBig(it) },

        // ------------------------------------------------------------------ MEDIA
        WidgetStyle("media_bar", Cat.MEDIA, "Media Bar", "شريط الميديا", B, popular = true, upd = mU) { MediaR.bar(it) },
        WidgetStyle("media_strip", Cat.MEDIA, "Media Strip", "شريط تحكم", B, upd = mU) { MediaR.strip(it) },
        WidgetStyle("media_capsule", Cat.MEDIA, "Media Capsule", "كبسولة ميديا", B, pro = true, upd = mU) { MediaR.capsule(it) },
        WidgetStyle("media_cover", Cat.MEDIA, "Media Cover", "غلاف الميديا", S, pro = true, isNew = true, upd = mU) { MediaR.cover(it) },
        WidgetStyle("media_disc", Cat.MEDIA, "Media Disc", "أسطوانة", S, pro = true, isNew = true, upd = mU) { MediaR.disc(it) },
        WidgetStyle("media_controls", Cat.MEDIA, "Media Controls", "أزرار التحكم", S, upd = mU) { MediaR.controls(it) },
        WidgetStyle("media_pad", Cat.MEDIA, "Media Pad", "لوحة الميديا", S, pro = true, upd = mU) { MediaR.pad(it) },
        WidgetStyle("media_ring", Cat.MEDIA, "Media Ring", "حلقة الميديا", S, pro = true, isNew = true, upd = mU) { MediaR.ring(it) },
        WidgetStyle("media_card", Cat.MEDIA, "Media Card", "كارت الميديا", S, pro = true, upd = mU) { MediaR.card(it) },
        WidgetStyle("media_player", Cat.MEDIA, "Media Player", "المشغل", W, popular = true, upd = mU) { MediaR.player(it) },
        WidgetStyle("media_deck", Cat.MEDIA, "Media Deck", "منصة الميديا", W, pro = true, isNew = true, upd = mU) { MediaR.deck(it) },
        WidgetStyle("media_bigplay", Cat.MEDIA, "Media Big Play", "تشغيل كبير", W, pro = true, upd = mU) { MediaR.bigPlay(it) },
        WidgetStyle("media_orbit", Cat.MEDIA, "Media Orbit", "مدار الميديا", W, pro = true, isNew = true, upd = mU) { MediaR.orbit(it) },
        WidgetStyle("media_wave", Cat.MEDIA, "Media Wave", "موجة الميديا", W, pro = true, isNew = true, upd = mU) { MediaR.wave(it) },
        WidgetStyle("media_book", Cat.MEDIA, "Media Book", "كتاب الميديا", W, pro = true, upd = mU) { MediaR.book(it) },
        WidgetStyle("media_poster", Cat.MEDIA, "Media Poster", "بوستر الميديا", L, pro = true, isNew = true, upd = mU) { MediaR.poster(it) },

        // ------------------------------------------------------------------ PHOTO
        WidgetStyle("photo_small", Cat.PHOTO, "Photo Frame", "إطار صورة", S, popular = true, feats = setOf(Feat.PHOTOS), maxItems = 10, upd = setOf(Upd.PHOTO), defaults = { it.copy(padding = 0) }) { PhotoR.frame(it) },
        WidgetStyle("photo_wide", Cat.PHOTO, "Photo Wide", "صورة عريضة", W, feats = setOf(Feat.PHOTOS), maxItems = 10, upd = setOf(Upd.PHOTO), defaults = { it.copy(padding = 0) }) { PhotoR.frame(it) },
        WidgetStyle("photo_large", Cat.PHOTO, "Photo Large", "صورة كبيرة", L, pro = true, feats = setOf(Feat.PHOTOS), maxItems = 10, upd = setOf(Upd.PHOTO), defaults = { it.copy(padding = 0) }) { PhotoR.frame(it) },
        WidgetStyle("photo_polaroid", Cat.PHOTO, "Polaroid", "بولارويد", S, pro = true, isNew = true, feats = setOf(Feat.PHOTOS, Feat.TEXT), maxItems = 10, upd = setOf(Upd.PHOTO)) { PhotoR.polaroid(it) },
        WidgetStyle("photo_caption", Cat.PHOTO, "Photo Caption", "صورة بتعليق", W, isNew = true, feats = setOf(Feat.PHOTOS, Feat.TEXT), maxItems = 10, upd = setOf(Upd.PHOTO), defaults = { it.copy(padding = 0) }) { PhotoR.caption(it) },
        WidgetStyle("share_app", Cat.PHOTO, "Share the app", "شارك التطبيق", S, upd = none) { ToolsR.share(it) },

        // ------------------------------------------------------------------ COUNTERS
        WidgetStyle("countdown_simple", Cat.COUNTERS, "Countdown Simple", "عد تنازلي", B, popular = true, feats = setOf(Feat.TITLE, Feat.TARGET_DATE, Feat.COLOR2)) { CounterR.countdownBanner(it) },
        WidgetStyle("countdown_small", Cat.COUNTERS, "Countdown Circle", "عد تنازلي دائري", S, feats = setOf(Feat.TITLE, Feat.TARGET_DATE, Feat.COLOR2)) { CounterR.countdownSmall(it) },
        WidgetStyle("counter", Cat.COUNTERS, "Counter", "عداد", S, feats = setOf(Feat.TITLE, Feat.COLOR2), upd = none) { CounterR.counter(it) },
        WidgetStyle("countdown_line", Cat.COUNTERS, "Countdown Line", "عد تنازلي بخط", W, pro = true, feats = setOf(Feat.TITLE, Feat.TARGET_DATE, Feat.COLOR2)) { CounterR.countdownLine(it) },
        WidgetStyle("counter_wide", Cat.COUNTERS, "Counter Wide", "عداد عريض", W, pro = true, feats = setOf(Feat.TITLE, Feat.COLOR2), upd = none) { CounterR.counterWide(it) },
        WidgetStyle("habit_week", Cat.COUNTERS, "Habit Tracker", "متتبع العادات", B, isNew = true, feats = setOf(Feat.TITLE, Feat.ACCENT), defaults = { it.copy(title = "Habit") }) { CounterR.habit(it) },
        WidgetStyle("year_progress", Cat.COUNTERS, "Year Progress", "تقدم السنة", S, isNew = true, feats = setOf(Feat.ACCENT)) { CounterR.yearProgress(it) },

        // ------------------------------------------------------------------ APPS
        WidgetStyle("apps_row", Cat.APPS, "Apps Row", "صف التطبيقات", B, popular = true, feats = setOf(Feat.APPS), maxItems = 5, upd = none) { AppsR.grid(it, 5, 1) },
        WidgetStyle("apps_small9", Cat.APPS, "Apps Folder 9", "مجلد ٩", S, pro = true, feats = setOf(Feat.APPS), maxItems = 9, upd = none) { AppsR.grid(it, 3, 3) },
        WidgetStyle("apps_small4", Cat.APPS, "Apps Folder 4", "مجلد ٤", S, pro = true, feats = setOf(Feat.APPS), maxItems = 4, upd = none) { AppsR.grid(it, 2, 2) },
        WidgetStyle("apps_wide", Cat.APPS, "Apps Folder", "مجلد التطبيقات", W, feats = setOf(Feat.APPS), maxItems = 8, upd = none) { AppsR.grid(it, 4, 2) },
        WidgetStyle("apps_large", Cat.APPS, "Apps Large", "مجلد كبير", L, pro = true, feats = setOf(Feat.APPS), maxItems = 12, upd = none) { AppsR.grid(it, 4, 3) },
        WidgetStyle("apps_dock", Cat.APPS, "Glass Dock", "دوك زجاجي", B, isNew = true, feats = setOf(Feat.APPS), maxItems = 4, upd = none, defaults = ::glass) { AppsR.dock(it) },

        // ------------------------------------------------------------------ PEOPLE
        WidgetStyle("people_row", Cat.PEOPLE, "People Row", "صف الأشخاص", B, feats = setOf(Feat.CONTACTS), maxItems = 5, upd = none) { PeopleR.grid(it, 5, 1) },
        WidgetStyle("people_small9", Cat.PEOPLE, "People Folder 9", "أشخاص ٩", S, pro = true, feats = setOf(Feat.CONTACTS), maxItems = 9, upd = none) { PeopleR.grid(it, 3, 3) },
        WidgetStyle("people_small4", Cat.PEOPLE, "People Folder 4", "أشخاص ٤", S, pro = true, feats = setOf(Feat.CONTACTS), maxItems = 4, upd = none) { PeopleR.grid(it, 2, 2) },
        WidgetStyle("people_wide", Cat.PEOPLE, "People Folder", "مجلد الأشخاص", W, popular = true, feats = setOf(Feat.CONTACTS), maxItems = 8, upd = none) { PeopleR.grid(it, 4, 2) },
        WidgetStyle("people_favorite", Cat.PEOPLE, "Favorite Person", "الشخص المفضل", S, isNew = true, feats = setOf(Feat.CONTACTS), maxItems = 1, upd = none) { PeopleR.favorite(it) },

        // ------------------------------------------------------------------ SEARCH
        WidgetStyle("search_bar", Cat.SEARCH, "Search Bar", "شريط البحث", B, popular = true, upd = none) { SearchR.bar(it, 0) },
        WidgetStyle("search_brave", Cat.SEARCH, "Search Bar Brave", "بحث Brave", B, upd = none) { SearchR.bar(it, 1) },
        WidgetStyle("search_samsung", Cat.SEARCH, "Search Bar Samsung", "بحث سامسونج", B, upd = none) { SearchR.bar(it, 2) },
        WidgetStyle("search_plus", Cat.SEARCH, "Search Bar Plus", "بحث بلس", B, pro = true, upd = none) { SearchR.plus(it) },
        WidgetStyle("search_actions", Cat.SEARCH, "Search Actions", "أزرار البحث", S, pro = true, upd = none) { SearchR.actions(it) },
        WidgetStyle("search_actions_grid", Cat.SEARCH, "Search Actions Grid", "شبكة البحث", S, pro = true, upd = none) { SearchR.actionsGrid(it) },
        WidgetStyle("search_big", Cat.SEARCH, "Search Big", "بحث كبير", W, pro = true, upd = none) { SearchR.big(it) },

        // ------------------------------------------------------------------ SYSTEM
        WidgetStyle("system_storage", Cat.SYSTEM, "System Storage", "التخزين", B, upd = sysU) { SystemR.single(it, 1) },
        WidgetStyle("system_battery", Cat.SYSTEM, "System Battery", "البطارية", B, upd = sysU) { SystemR.single(it, 0) },
        WidgetStyle("system_ram", Cat.SYSTEM, "System RAM", "الذاكرة", B, upd = sysU) { SystemR.single(it, 2) },
        WidgetStyle("system_info", Cat.SYSTEM, "System Info", "معلومات النظام", W, popular = true, upd = sysU) { SystemR.info(it) },
        WidgetStyle("system_device", Cat.SYSTEM, "Device Info", "معلومات الجهاز", L, pro = true, upd = sysU) { SystemR.device(it) },
        WidgetStyle("battery_ring", Cat.SYSTEM, "Battery Ring", "حلقة البطارية", S, isNew = true, feats = setOf(Feat.ACCENT), upd = sysU) { SystemR.ring(it) },

        // ------------------------------------------------------------------ QUOTES
        WidgetStyle("quotes_custom", Cat.QUOTES, "Quotes Custom", "اقتباس", W, feats = setOf(Feat.TEXT, Feat.AUTHOR, Feat.COLOR2), upd = none) { QuoteR.custom(it) },
        WidgetStyle("quotes_text", Cat.QUOTES, "Quotes Text", "نص حر", W, feats = setOf(Feat.TEXT), upd = none) { QuoteR.text(it) },
        WidgetStyle("quotes_calli", Cat.QUOTES, "Calligraphy Text", "نص بالخط العربي", W, pro = true, isNew = true, feats = setOf(Feat.TEXT), upd = none, defaults = { it.copy(text = "وَقُل رَّبِّ زِدْنِي عِلْمًا") }) { QuoteR.calli(it) },
        WidgetStyle("sticky_note", Cat.QUOTES, "Sticky Note", "ملاحظة", S, isNew = true, feats = setOf(Feat.TEXT, Feat.TITLE), upd = none, defaults = { it.copy(fill = Fill.SOLID, bgColor = 0xFFF4D35E.toInt(), color1 = 0xFF2B2B2B.toInt(), color2 = 0xFF5A4A1A.toInt(), border = false) }) { QuoteR.sticky(it) },

        // ------------------------------------------------------------------ TOOLS
        WidgetStyle("tutorials", Cat.TOOLS, "Tutorials", "الشرح", S, upd = none) { ToolsR.tutorials(it) },
        WidgetStyle("sun_times", Cat.TOOLS, "Sun Arc", "الشروق والغروب", W, isNew = true, feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = pU) { ToolsR.sunArc(it) },
        WidgetStyle("quick_toggles", Cat.TOOLS, "Quick Actions", "اختصارات سريعة", B, isNew = true, upd = none) { ToolsR.quick(it) },
    )

    private val map = all.associateBy { it.id }
    fun byId(id: String) = map[id]
}
