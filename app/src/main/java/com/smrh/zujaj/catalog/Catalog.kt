package com.smrh.zujaj.catalog

import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.Texture
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.Moments2R
import com.smrh.zujaj.render.Moments3R
import com.smrh.zujaj.render.AppsR
import com.smrh.zujaj.render.CalliR
import com.smrh.zujaj.render.Prayer2R
import com.smrh.zujaj.render.Cal2R
import com.smrh.zujaj.render.Weather2R
import com.smrh.zujaj.render.Tools2R
import com.smrh.zujaj.render.Tasbih2R
import com.smrh.zujaj.render.Fancy2R
import com.smrh.zujaj.render.Life2R
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
import com.smrh.zujaj.render.VerseR
import com.smrh.zujaj.render.EventR

/** Grid footprint and the design size (dp) every renderer lays out against. */
enum class Shape(val cols: Int, val rows: Int, val dw: Float, val dh: Float) {
    SMALL(2, 2, 160f, 160f),
    WIDE(4, 2, 340f, 160f),
    BANNER(4, 1, 340f, 84f),
    LARGE(4, 3, 340f, 250f),
    TALL(2, 4, 160f, 340f),
    XL(4, 5, 340f, 420f),
}

enum class Cat(val en: String, val ar: String, val tags: String = "") {
    CALLI("Calligraphy", "الخط العربي", "calligraphy thuluth arabic day kufi خط عربي ثلث كوفي يوم مزخرف"),
    CLOCK("Clock", "ساعات", "clock time watch analog hour ساعه ساعة وقت الوقت عقارب"),
    DIGITAL("Digital", "رقمية", "clock time digital hour ساعه ساعة رقمي رقميه الوقت"),
    DATE("Date", "التاريخ", "date calendar day month year hijri تاريخ تقويم يوم شهر سنه هجري ميلادي"),
    WEATHER("Weather", "الطقس", "weather temperature rain sun forecast طقس الطقس جو حراره حرارة مطر"),
    PRAYER("Prayer", "الصلاة", "prayer salah salat namaz adhan azan athan صلاه صلاة الصلاه اذان أذان مواقيت"),
    ISLAMIC("Islamic", "إسلامي", "islamic quran dhikr azkar tasbih islam اسلامي قران قرآن اذكار أذكار تسبيح ذكر"),
    MEDIA("Media", "الميديا", "music media song songs player audio spotify youtube anghami sound podcast موسيقى موسيقي مزيكا اغاني أغاني اغنيه أغنية مشغل ميديا صوت"),
    PHOTO("Photo", "صور", "photo photos picture pictures image gallery صور صوره صورة معرض"),
    COUNTERS("Counters", "عدادات", "counter countdown days timer event عداد عد تنازلي ايام أيام مناسبه"),
    APPS("Apps", "تطبيقات", "apps app shortcut shortcuts launcher تطبيقات تطبيق اختصارات"),
    PEOPLE("People", "أشخاص", "people contacts contact call friends اشخاص أشخاص جهات اتصال مكالمه"),
    SEARCH("Search", "بحث", "search google bar بحث جوجل"),
    SYSTEM("System", "النظام", "system battery storage ram نظام بطاريه بطارية ذاكره مساحه"),
    QUOTES("Quotes", "اقتباسات", "quote quotes text note اقتباس اقتباسات نص كلام ملاحظه"),
    HABITS("Habits", "العادات", "habit habits streak tracker routine عادات عاده متابعة"),
    NOTES("Notes", "ملاحظات", "note notes sticky memo todo ملاحظه ملاحظات مذكره"),
    FOCUS("Focus", "تركيز", "focus pomodoro timer study تركيز مذاكره مؤقت"),
    TOOLS("Tools", "أدوات", "tools torch flashlight calculator wifi camera alarm ادوات أدوات كشاف حاسبه واي فاي كاميرا منبه"),
}

/** Extra editor controls a style needs (beyond the common ones). */
enum class Feat { ROTATE, VERSE, EVENT, HUB, COLOR2, ACCENT, SIZE2, CALENDAR, HOUR24, SECONDS, TITLE, TEXT, AUTHOR, TARGET_DATE, GOAL, APPS, CONTACTS, PHOTOS, CITIES, GAP }

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
    /** Time drawn into the picture (redrawn each minute) — e.g. for gradient digits. */
    val bakeTime: (WidgetConfig) -> Boolean = { false },
    val draw: (RenderCtx) -> Unit,
) {
    fun name(ar: Boolean) = if (ar) this.ar else en
    fun defaultConfig(): WidgetConfig = defaults(WidgetConfig(styleId = id))
}

/** Loose search: English/Arabic names, category and keywords (e.g. "music" → media widgets). */
object WidgetSearch {
    private fun norm(s: String) = s.lowercase().replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ة', 'ه').replace('ى', 'ي').replace("\u0640", "").filter { it !in '\u064B'..'\u0652' }

    fun matches(st: WidgetStyle, q0: String): Boolean {
        val q = norm(q0.trim())
        if (q.isEmpty()) return true
        val hay = norm("${st.en} ${st.ar} ${st.id.replace('_', ' ')} ${st.cat.en} ${st.cat.ar} ${st.cat.tags}")
        return q.split(' ').filter { it.isNotBlank() }.all { w -> hay.contains(w) || hay.split(' ').any { it.startsWith(w) } }
    }
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
    private val cF = setOf(Feat.ACCENT, Feat.COLOR2, Feat.HOUR24, Feat.SIZE2)
    private val cD: (WidgetConfig) -> WidgetConfig = { it.copy(calli = 3, accent = 0xFFE7B75F.toInt()) }
    private val pF = setOf(Feat.COLOR2, Feat.HOUR24, Feat.ACCENT)
    private val mint: (WidgetConfig) -> WidgetConfig = { it.copy(fill = Fill.GRADIENT, bgColor = 0xFF0E2420.toInt(), bgColor2 = 0xFF143A33.toInt(), accent = 0xFF3DDCB5.toInt()) }
    private val tF = setOf(Feat.GOAL, Feat.TEXT, Feat.ACCENT)
    private val hF = setOf(Feat.TITLE, Feat.ACCENT)
    private val hD: (WidgetConfig) -> WidgetConfig = { it.copy(title = "", accent = 0xFF34C759.toInt()) }
    private val nF = setOf(Feat.TITLE, Feat.TEXT, Feat.ACCENT)
    private val fF = setOf(Feat.TITLE, Feat.GOAL, Feat.ACCENT)
    private val fD: (WidgetConfig) -> WidgetConfig = { it.copy(goal = 25, accent = 0xFFE5484D.toInt()) }

    private fun glass(c: WidgetConfig) = c.copy(fill = Fill.GLASS, texture = Texture.OFF)
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
        WidgetStyle("digital_fade", Cat.DIGITAL, "Digital Fade", "ساعة متلاشية", W, pro = true, isNew = true, popular = true, feats = timeF + Feat.ACCENT, upd = none) { DigitalR.fadeWide(it) },
        WidgetStyle("digital_duo", Cat.DIGITAL, "Digital Duo", "ساعة ثنائية", S, isNew = true, feats = timeF + Feat.ACCENT, upd = none) { DigitalR.duo(it) },
        WidgetStyle("digital_arabic", Cat.DIGITAL, "Arabic Numerals", "ساعة بالأرقام العربية", W, isNew = true, popular = true, feats = timeF, upd = setOf(Upd.DAILY),
            defaults = { it.copy(numerals = com.smrh.zujaj.model.Numerals.ARABIC, language = com.smrh.zujaj.model.Lang.AR) }) { DigitalR.arabic(it) },
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
        WidgetStyle("prayer_hub", Cat.PRAYER, "Prayer Hub", "لوحة الصلاة", L, isNew = true, popular = true,
            feats = setOf(Feat.HUB, Feat.HOUR24), upd = pU,
            // same graphite look as the rest of the app; gold → coral minutes
            defaults = { it.copy(accent = 0xFFE7B75F.toInt(), calli = 3) },
            bakeTime = { it.minGrad || it.hourGrad || it.timeMerge }) { PrayerR.hub(it) },
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
        WidgetStyle("photo_verse", Cat.PHOTO, "Photo & Verse", "صورة وآية", W, isNew = true, popular = true, feats = setOf(Feat.PHOTOS, Feat.VERSE), maxItems = 20, upd = setOf(Upd.PHOTO, Upd.DAILY), defaults = { it.copy(padding = 0, variant = 1) }) { VerseR.wide(it) },
        WidgetStyle("photo_verse_sq", Cat.PHOTO, "Photo & Dhikr", "صورة وذكر", S, isNew = true, feats = setOf(Feat.PHOTOS, Feat.VERSE), maxItems = 20, upd = setOf(Upd.PHOTO, Upd.DAILY), defaults = { it.copy(padding = 0) }) { VerseR.square(it) },
        WidgetStyle("photo_verse_lg", Cat.PHOTO, "Photo Journal", "صورة ويومية", L, pro = true, isNew = true, feats = setOf(Feat.PHOTOS, Feat.VERSE), maxItems = 20, upd = setOf(Upd.PHOTO, Upd.DAILY), defaults = { it.copy(padding = 12, variant = 1, bgColor = 0xFF15131A.toInt()) }) { VerseR.large(it) },
        WidgetStyle("bubble_clock", Cat.CLOCK, "Bubble Clock", "ساعة الفقاعات", L, isNew = true, popular = true, feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.WEATHER), bakeTime = { true }, defaults = { it.copy(fill = Fill.NONE, accent = 0xFFB9A6F2.toInt(), calli = 2) }) { Moments2R.bubble(it) },
        WidgetStyle("words_day_card", Cat.CALLI, "Words of the Day", "كارت اليوم", L, isNew = true, popular = true, feats = setOf(Feat.ROTATE, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER), bakeTime = { true }, defaults = { it.copy(fill = Fill.GRADIENT, bgColor = 0xFF8FB8E6.toInt(), bgColor2 = 0xFF6E9BD3.toInt(), color1 = 0xFFFFFFFF.toInt(), counter = 360, calli = 2) }) { Moments2R.dayCard(it) },
        WidgetStyle("rose_next", Cat.PRAYER, "Rose · Next Prayer", "الورد · الصلاة القادمة", S, isNew = true, popular = true, feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xE67A3E68.toInt(), bgColor2 = 0xE6B4566E.toInt(), accent = 0xFF5A2C50.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), padding = 0) }) { Moments3R.roseNext(it) },
        WidgetStyle("rose_weather", Cat.WEATHER, "Rose · Weather", "الورد · الطقس", S, isNew = true, popular = true, feats = setOf(Feat.ACCENT), upd = setOf(Upd.WEATHER), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xE67A3E68.toInt(), bgColor2 = 0xE6B4566E.toInt(), accent = 0xFF5A2C50.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), padding = 0) }) { Moments3R.roseWeather(it) },
        WidgetStyle("rose_prayers", Cat.PRAYER, "Rose · Prayer Times", "الورد · مواقيت الصلاة", W, isNew = true, popular = true, feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xE67A3E68.toInt(), bgColor2 = 0xE6B4566E.toInt(), accent = 0xFF5A2C50.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), padding = 0) }) { Moments3R.rosePrayers(it) },
        WidgetStyle("rose_music", Cat.MEDIA, "Rose · Music", "الورد · الموسيقى", W, isNew = true, popular = true, feats = setOf(Feat.ACCENT), upd = setOf(Upd.MEDIA), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xE67A3E68.toInt(), bgColor2 = 0xE6B4566E.toInt(), accent = 0xFF5A2C50.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), padding = 0) }) { Moments3R.roseMusic(it) },
        WidgetStyle("dark_day", Cat.CALLI, "Night · Day & Months", "الليل · اليوم والشهور", W, isNew = true, popular = true, upd = setOf(Upd.DAILY), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xF5141416.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xB3FFFFFF.toInt(), padding = 0) }) { Moments3R.darkDay(it) },
        WidgetStyle("dark_prayer_date", Cat.CALLI, "Night · Prayer & Date", "الليل · الصلاة والتاريخ", L, isNew = true, popular = true, feats = setOf(Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xF5141416.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xB3FFFFFF.toInt(), padding = 0) }) { Moments3R.darkPrayerDate(it) },
        WidgetStyle("dark_grid", Cat.PRAYER, "Night · Prayer Grid", "الليل · شبكة الصلاة", L, isNew = true, popular = true, feats = setOf(Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER, Upd.WEATHER), defaults = { it.copy(fill = Fill.NONE, bgColor = 0xF5141416.toInt(), color1 = 0xFFFFFFFF.toInt(), color2 = 0xB3FFFFFF.toInt(), padding = 0) }) { Moments3R.darkGrid(it) },
        WidgetStyle("sport_card", Cat.PRAYER, "Sport Card", "الكارت الرياضي", Shape.TALL, isNew = true, popular = true, feats = setOf(Feat.ACCENT, Feat.COLOR2, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER, Upd.SYSTEM), bakeTime = { true }, defaults = { it.copy(fill = Fill.SOLID, bgColor = 0xFF0E0E10.toInt(), texture = Texture.GRAIN, color1 = 0xFFFFFFFF.toInt(), color2 = 0xFFD9D9D9.toInt(), accent = 0xFFE5322D.toInt(), calli = 0) }) { Moments2R.sport(it) },
        WidgetStyle("prayer_board", Cat.PRAYER, "Prayer Board", "لوحة الصلاة", L, isNew = true, popular = true, feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.PRAYER, Upd.WEATHER), bakeTime = { true }, defaults = { it.copy(fill = Fill.NONE, color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), accent = 0xFFF2E6B8.toInt()) }) { Moments2R.board(it) },
        WidgetStyle("event_ring", Cat.COUNTERS, "Event Ring", "حلقة المناسبة", S, isNew = true, popular = true, feats = setOf(Feat.EVENT, Feat.ACCENT), defaults = { it.copy(event = "ramadan", fill = Fill.GRADIENT, bgColor = 0xFF14102A.toInt(), bgColor2 = 0xFF2A1B4D.toInt(), accent = 0xFFE7B75F.toInt(), accent2 = 0xFFB07BF0.toInt()) }) { EventR.ring(it) },
        WidgetStyle("event_card", Cat.COUNTERS, "Event Card", "كارت المناسبة", W, isNew = true, feats = setOf(Feat.EVENT, Feat.ACCENT), defaults = { it.copy(event = "fitr", fill = Fill.GRADIENT, bgColor = 0xFF0E2420.toInt(), bgColor2 = 0xFF1C4A3F.toInt(), accent = 0xFF3DDCB5.toInt(), accent2 = 0xFFB7E34A.toInt()) }) { EventR.card(it) },
        WidgetStyle("event_flip", Cat.COUNTERS, "Event Flip", "عدّاد قلّاب", W, pro = true, isNew = true, feats = setOf(Feat.EVENT), defaults = { it.copy(event = "exams", fill = Fill.GRADIENT, bgColor = 0xFF1A1A1F.toInt(), bgColor2 = 0xFF2E2E38.toInt()) }) { EventR.flip(it) },
        WidgetStyle("event_banner", Cat.COUNTERS, "Event Banner", "شريط المناسبة", B, isNew = true, feats = setOf(Feat.EVENT, Feat.ACCENT), defaults = { it.copy(event = "birthday", accent = 0xFFFF6B9A.toInt(), accent2 = 0xFFB07BF0.toInt()) }) { EventR.banner(it) },
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
        // ------------------------------------------------------------------ CALLIGRAPHY
        WidgetStyle("calli_lawn", Cat.CALLI, "Lawn", "لون", W, isNew = true, popular = true, feats = cF, defaults = cD, bakeTime = { true }) { CalliR.lawn(it) },
        WidgetStyle("calli_laiha", Cat.CALLI, "Laiha", "لائحة", W, pro = true, isNew = true, feats = cF, defaults = cD, bakeTime = { true }) { CalliR.laiha(it) },
        WidgetStyle("calli_countdown", Cat.CALLI, "Calligraphy Countdown", "عد الصلاة بالخط", W, isNew = true, feats = cF, defaults = cD) { CalliR.countdown(it) },
        WidgetStyle("calli_ascend", Cat.CALLI, "Ascend", "صعود", W, pro = true, isNew = true, feats = cF, defaults = cD) { CalliR.ascend(it) },
        WidgetStyle("calli_between", Cat.CALLI, "Between the Digits", "بين الأرقام", W, isNew = true, popular = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFE5674F.toInt()) }) { CalliR.between(it) },
        WidgetStyle("calli_aura", Cat.CALLI, "Aura", "هالة", S, isNew = true, feats = cF, defaults = cD) { CalliR.aura(it) },
        WidgetStyle("calli_rukn", Cat.CALLI, "Rukn", "ركن", S, pro = true, isNew = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFE7B75F.toInt()) }) { CalliR.rukn(it) },
        WidgetStyle("calli_khatt", Cat.CALLI, "Khatt", "خط", W, pro = true, isNew = true, feats = cF, defaults = cD) { CalliR.khatt(it) },
        WidgetStyle("calli_obelisk", Cat.CALLI, "Obelisk", "مسلّة", S, pro = true, isNew = true, feats = cF, defaults = cD, bakeTime = { true }) { CalliR.obelisk(it) },
        WidgetStyle("calli_column", Cat.CALLI, "Column", "عمود", S, isNew = true, feats = cF, defaults = cD) { CalliR.column(it) },
        WidgetStyle("calli_big_day", Cat.CALLI, "Big Day", "اليوم الكبير", W, isNew = true, feats = cF, defaults = cD) { CalliR.bigDay(it) },
        WidgetStyle("calli_vertical", Cat.CALLI, "Vertical Day", "اليوم العمودي", S, pro = true, isNew = true, feats = cF, defaults = cD) { CalliR.vertical(it) },
        WidgetStyle("calli_satr", Cat.CALLI, "Satr", "سطر", W, pro = true, isNew = true, feats = cF + Feat.TEXT, defaults = { it.copy(calli = 3, text = "صلِّ على النبي") }) { CalliR.satr(it) },
        WidgetStyle("calli_calendar_day", Cat.CALLI, "Calendar Day", "يوم التقويم", S, isNew = true, feats = cF, defaults = cD) { CalliR.calendarDay(it) },
        WidgetStyle("calli_echo", Cat.CALLI, "Echo", "صدى", W, pro = true, isNew = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFE58A3C.toInt()) }) { CalliR.echo(it) },
        WidgetStyle("calli_blossom", Cat.CALLI, "Blossom", "زهر", W, pro = true, isNew = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFF27BA7.toInt()) }, bakeTime = { true }) { CalliR.blossom(it) },
        WidgetStyle("calli_mirage", Cat.CALLI, "Mirage", "سراب", W, pro = true, isNew = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFB9A6E0.toInt()) }, bakeTime = { true }) { CalliR.mirage(it) },
        WidgetStyle("calli_day_number", Cat.CALLI, "Day Number", "رقم اليوم", S, isNew = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFE7B75F.toInt()) }) { CalliR.dayNumber(it) },
        WidgetStyle("calli_noqta", Cat.CALLI, "Noqta", "نقطة", S, isNew = true, feats = cF, defaults = { it.copy(numerals = com.smrh.zujaj.model.Numerals.ARABIC, accent = 0xFF2FC4A5.toInt()) }) { CalliR.noqta(it) },
        WidgetStyle("calli_desert", Cat.CALLI, "Desert", "الصحراء", W, pro = true, isNew = true, feats = cF, defaults = { it.copy(calli = 3, accent = 0xFFD9A441.toInt(), color1 = 0xFFF3E3C2.toInt()) }) { Fancy2R.desert(it) },
        // ------------------------------------------------------------------ PRAYER (more)
        WidgetStyle("prayer_journey", Cat.PRAYER, "Prayer Journey", "رحلة الصلاة", W, isNew = true, popular = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFF3DDCB5.toInt()) }) { Prayer2R.journey(it) },
        WidgetStyle("prayer_mashrabiya", Cat.PRAYER, "Mashrabiya", "مشربية", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(fill = Fill.GRADIENT, bgColor = 0xFF1A120B.toInt(), bgColor2 = 0xFF3A2614.toInt(), accent = 0xFFD9A05B.toInt(), calli = 3) }) { Prayer2R.mashrabiya(it) },
        WidgetStyle("prayer_mint_box", Cat.PRAYER, "Mint Box", "صندوق النعناع", S, isNew = true, feats = pF, upd = pU, defaults = mint) { Prayer2R.mintBox(it) },
        WidgetStyle("prayer_column", Cat.PRAYER, "Prayer Column", "عمود الصلاة", S, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFF3DDC97.toInt()) }) { Prayer2R.column(it) },
        WidgetStyle("prayer_midnight", Cat.PRAYER, "Midnight", "منتصف الليل", W, isNew = true, feats = pF, upd = pU, defaults = mint) { Prayer2R.midnight(it) },
        WidgetStyle("prayer_ahead", Cat.PRAYER, "Ahead", "القادمة", W, isNew = true, feats = pF, upd = pU) { Prayer2R.ahead(it) },
        WidgetStyle("prayer_timeline", Cat.PRAYER, "Timeline", "الخط الزمني", L, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { Prayer2R.timeline(it) },
        WidgetStyle("prayer_mint_full", Cat.PRAYER, "Mint Full", "النعناع الكامل", L, pro = true, isNew = true, feats = pF, upd = pU + Upd.WEATHER, defaults = mint) { Prayer2R.mintFull(it) },
        WidgetStyle("prayer_serene", Cat.PRAYER, "Serene", "هدوء", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(fill = Fill.GRADIENT, bgColor = 0xFFC9CCD1.toInt(), bgColor2 = 0xFF9DA2A9.toInt(), color1 = 0xFF1B1B1F.toInt(), color2 = 0xFF4A4D55.toInt(), accent = 0xFF1F8A70.toInt()) }) { Prayer2R.serene(it) },
        WidgetStyle("prayer_ledger", Cat.PRAYER, "Ledger", "الدفتر", W, isNew = true, feats = pF, upd = pU, defaults = { it.copy(bgColor = 0xFFF7F3EC.toInt(), color1 = 0xFF2A2420.toInt(), color2 = 0xFF8A8079.toInt(), accent = 0xFFD8654A.toInt(), border = false) }) { Prayer2R.ledger(it) },
        WidgetStyle("prayer_ruler", Cat.PRAYER, "Ruler", "المسطرة", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { Prayer2R.ruler(it) },
        WidgetStyle("prayer_flip", Cat.PRAYER, "Flip", "قلّاب", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFFE5484D.toInt(), calli = 3) }) { Prayer2R.flip(it) },
        WidgetStyle("prayer_hilal", Cat.PRAYER, "Hilal", "هلال", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { Prayer2R.hilal(it) },
        WidgetStyle("prayer_day_pass", Cat.PRAYER, "Day Pass", "تذكرة اليوم", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFF5B8DEF.toInt()) }) { Prayer2R.dayPass(it) },
        WidgetStyle("prayer_relay", Cat.PRAYER, "Relay", "تتابع", W, isNew = true, feats = pF, upd = pU) { Prayer2R.relay(it) },
        WidgetStyle("prayer_mosaic", Cat.PRAYER, "Mosaic", "فسيفساء", L, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFF3DB4DC.toInt()) }) { Prayer2R.mosaic(it) },
        WidgetStyle("prayer_aurora_grid", Cat.PRAYER, "Prayer Aurora", "شفق الصلاة", W, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFF3DDCB5.toInt()) }) { Prayer2R.auroraGrid(it) },
        WidgetStyle("prayer_temple", Cat.PRAYER, "Temple of Hours", "معبد الساعات", W, pro = true, isNew = true, feats = pF, upd = pU, defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { Prayer2R.temple(it) },
        WidgetStyle("prayer_next_small", Cat.PRAYER, "Next Prayer", "الصلاة التالية", S, isNew = true, feats = pF, upd = pU) { Prayer2R.nextSmall(it) },
        // ------------------------------------------------------------------ DATE (more)
        WidgetStyle("cal_seven", Cat.DATE, "Seven", "سبعة", W, isNew = true, feats = setOf(Feat.COLOR2)) { Cal2R.seven(it) },
        WidgetStyle("cal_progress", Cat.DATE, "Progress", "التقدم", W, isNew = true, feats = setOf(Feat.ACCENT), defaults = { it.copy(accent = 0xFF6E8BF0.toInt(), color2 = 0xFFD9A05B.toInt()) }) { Cal2R.progress(it) },
        WidgetStyle("cal_weekband", Cat.DATE, "Weekband", "شريط الأسبوع", W, pro = true, isNew = true, feats = setOf(Feat.ACCENT), defaults = { it.copy(accent = 0xFFB7E34A.toInt(), calli = 3) }) { Cal2R.weekband(it) },
        WidgetStyle("cal_fraction", Cat.DATE, "Fraction", "كسر", W, isNew = true, feats = setOf(Feat.ACCENT), defaults = { it.copy(accent = 0xFF3DDCB5.toInt()) }) { Cal2R.fraction(it) },
        WidgetStyle("cal_hijri_grid", Cat.DATE, "Hijri Calendar", "التقويم الهجري", W, pro = true, isNew = true, popular = true, feats = setOf(Feat.ACCENT), upd = setOf(Upd.DAILY, Upd.WEATHER), defaults = { it.copy(accent = 0xFF5B8DEF.toInt(), calli = 3) }) { Cal2R.hijriCalendar(it) },
        WidgetStyle("cal_rem", Cat.DATE, "Month Grid", "شبكة الشهر", W, isNew = true, feats = setOf(Feat.ACCENT), defaults = { it.copy(accent = 0xFFE5484D.toInt()) }) { Cal2R.remCalendar(it) },
        WidgetStyle("cal_constellation", Cat.DATE, "Constellation", "كوكبة", W, pro = true, isNew = true, feats = setOf(Feat.ACCENT), defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { Cal2R.constellation(it) },
        WidgetStyle("cal_terminal", Cat.DATE, "Terminal", "الطرفية", W, isNew = true, feats = setOf(Feat.ACCENT), defaults = { it.copy(accent = 0xFF3DDC6B.toInt()) }, bakeTime = { true }) { Cal2R.terminal(it) },
        WidgetStyle("cal_arc", Cat.DATE, "Arc", "قوس", W, pro = true, isNew = true, feats = setOf(Feat.ACCENT, Feat.HOUR24), upd = setOf(Upd.DAILY, Upd.WEATHER), defaults = { it.copy(accent = 0xFF7B7BF0.toInt()) }) { Cal2R.arc(it) },
        // ------------------------------------------------------------------ CLOCK (graphic)
        WidgetStyle("clock_overlap", Cat.DIGITAL, "Overlap", "تقاطع", W, pro = true, isNew = true, feats = timeF + Feat.ACCENT, upd = none, defaults = { it.copy(bgColor = 0xFFF4EFE6.toInt(), color1 = 0xFF1B1B1F.toInt(), color2 = 0xFF4C6EF5.toInt(), accent = 0xFFF06A5B.toInt(), border = false) }) { Fancy2R.overlap(it) },
        WidgetStyle("clock_chrome", Cat.DIGITAL, "Chrome", "كروم", S, isNew = true, feats = timeF, upd = setOf(Upd.WEATHER), bakeTime = { true }) { Fancy2R.chrome(it) },
        WidgetStyle("clock_forms", Cat.DIGITAL, "Forms", "أشكال", S, pro = true, isNew = true, feats = timeF + Feat.ACCENT, upd = none, defaults = { it.copy(color2 = 0xFFF2C14E.toInt(), accent = 0xFFE5674F.toInt(), color1 = 0xFFF4EFE6.toInt()) }) { Fancy2R.forms(it) },
        WidgetStyle("clock_neon", Cat.DIGITAL, "Neon Pulse", "نبض نيون", S, isNew = true, popular = true, feats = timeF + Feat.ACCENT, upd = none, defaults = { it.copy(accent = 0xFFFF3DAE.toInt(), color2 = 0xFF8B5CF6.toInt()) }, bakeTime = { true }) { Fancy2R.neon(it) },
        WidgetStyle("clock_dot_rings", Cat.DIGITAL, "Dot Rings", "حلقات النقاط", W, pro = true, isNew = true, feats = timeF + Feat.ACCENT, upd = none, defaults = { it.copy(bgColor = 0xFF0C0C0E.toInt()) }, bakeTime = { true }) { Fancy2R.dotRings(it) },
        WidgetStyle("clock_galaxy", Cat.DIGITAL, "Galaxy", "مجرة", S, isNew = true, feats = timeF + Feat.ACCENT, upd = none) { Fancy2R.galaxy(it) },
        // ------------------------------------------------------------------ WEATHER (forecast)
        WidgetStyle("w_today_tomorrow", Cat.WEATHER, "Today & Tomorrow", "اليوم وبكرة", W, isNew = true, upd = wU, defaults = { it.copy(fill = Fill.GRADIENT, bgColor = 0xFF3D8BDB.toInt(), bgColor2 = 0xFF7BB6EE.toInt(), color2 = 0xFFE3EEFA.toInt(), border = false) }) { Weather2R.todayTomorrow(it) },
        WidgetStyle("w_next_hours", Cat.WEATHER, "Next Hours", "الساعات الجاية", B, isNew = true, upd = wU) { Weather2R.nextHours(it) },
        WidgetStyle("w_temp_curve", Cat.WEATHER, "Temperature Curve", "منحنى الحرارة", W, isNew = true, feats = setOf(Feat.ACCENT), upd = wU, defaults = { it.copy(accent = 0xFFB7E34A.toInt()) }) { Weather2R.tempCurve(it) },
        WidgetStyle("w_rain_week", Cat.WEATHER, "Rain This Week", "أمطار الأسبوع", S, pro = true, isNew = true, feats = setOf(Feat.ACCENT), upd = wU, defaults = { it.copy(accent = 0xFFF2A93B.toInt()) }) { Weather2R.rainWeek(it) },
        WidgetStyle("w_week_columns", Cat.WEATHER, "Week Columns", "أعمدة الأسبوع", W, pro = true, isNew = true, feats = setOf(Feat.ACCENT), upd = wU, defaults = { it.copy(accent = 0xFFF2C14E.toInt(), color2 = 0xFF3DDC97.toInt()) }) { Weather2R.weekColumns(it) },
        WidgetStyle("w_week_glance", Cat.WEATHER, "Week at a Glance", "الأسبوع بنظرة", B, isNew = true, upd = wU) { Weather2R.weekGlance(it) },
        WidgetStyle("w_five_days", Cat.WEATHER, "Five Days", "خمس أيام", S, isNew = true, upd = wU) { Weather2R.fiveDays(it) },
        WidgetStyle("w_hourly_curve", Cat.WEATHER, "Hourly Curve", "منحنى الساعات", W, pro = true, isNew = true, upd = wU, defaults = { it.copy(fill = Fill.GRADIENT, bgColor = 0xFF3D8BDB.toInt(), bgColor2 = 0xFF7BB6EE.toInt(), color2 = 0xFFE3EEFA.toInt(), border = false) }) { Weather2R.hourlyCurve(it) },
        WidgetStyle("w_hours_days", Cat.WEATHER, "Hours & Days", "ساعات وأيام", W, isNew = true, upd = wU) { Weather2R.hoursDays(it) },
        WidgetStyle("w_now_ahead", Cat.WEATHER, "Now & Days Ahead", "الآن والأيام الجاية", W, pro = true, isNew = true, upd = wU) { Weather2R.nowAhead(it) },
        WidgetStyle("w_horizon", Cat.WEATHER, "Horizon", "الأفق", W, isNew = true, feats = setOf(Feat.ACCENT), upd = wU + pU, defaults = { it.copy(accent = 0xFFE7B75F.toInt()) }) { Prayer2R.horizon(it) },
        // ------------------------------------------------------------------ ISLAMIC (tasbih)
        WidgetStyle("tasbih_beads", Cat.ISLAMIC, "Tasbih Beads", "سبحة الخرز", S, isNew = true, popular = true, feats = tF, upd = none, defaults = { it.copy(goal = 33, text = "سبحان الله", accent = 0xFFE58A3C.toInt(), calli = 3) }) { Tasbih2R.beads(it) },
        WidgetStyle("tasbih_ring", Cat.ISLAMIC, "Tasbih Ring", "حلقة التسبيح", S, isNew = true, feats = tF, upd = none, defaults = { it.copy(goal = 99, text = "سبحان الله", accent = 0xFF2FC4A5.toInt(), calli = 3) }) { Tasbih2R.ring(it) },
        WidgetStyle("tasbih_calendar", Cat.ISLAMIC, "Tasbih Calendar", "تقويم التسبيح", W, pro = true, isNew = true, feats = tF, upd = none, defaults = { it.copy(goal = 99, text = "سبحان الله", accent = 0xFF5B9BF0.toInt(), calli = 3) }) { Tasbih2R.calendar(it) },
        WidgetStyle("tasbih_stats", Cat.ISLAMIC, "Tasbih Stats", "إحصاء التسبيح", W, pro = true, isNew = true, feats = tF, upd = none, defaults = { it.copy(goal = 33, text = "سبحان الله", accent = 0xFFB07BF0.toInt(), calli = 3) }) { Tasbih2R.stats(it) },
        WidgetStyle("tasbih_bars", Cat.ISLAMIC, "Tasbih Bars", "أعمدة التسبيح", W, pro = true, isNew = true, feats = tF, upd = none, defaults = { it.copy(goal = 33, text = "سبحان الله", accent = 0xFF8B6CF0.toInt(), calli = 3) }) { Tasbih2R.bars(it) },
        // ------------------------------------------------------------------ HABITS
        WidgetStyle("habit_card", Cat.HABITS, "Habit", "عادة", S, isNew = true, popular = true, feats = hF, defaults = hD) { Life2R.habit(it) },
        WidgetStyle("habit_ring", Cat.HABITS, "Habit Ring", "حلقة العادة", S, isNew = true, feats = hF, defaults = hD) { Life2R.habitRing(it) },
        WidgetStyle("habit_mini", Cat.HABITS, "Habit Mini", "عادة صغيرة", S, isNew = true, feats = hF, defaults = hD) { Life2R.habitMini(it) },
        WidgetStyle("habit_strip", Cat.HABITS, "Habit Strip", "شريط العادة", B, isNew = true, feats = hF, defaults = hD) { Life2R.habitStrip(it) },
        WidgetStyle("habit_month", Cat.HABITS, "Habit Month", "شهر العادة", S, pro = true, isNew = true, feats = hF, defaults = hD) { Life2R.habitWeek(it) },
        WidgetStyle("habit_pills", Cat.HABITS, "Habit Pills", "كبسولة العادة", W, pro = true, isNew = true, feats = hF, defaults = hD) { Life2R.habitPills(it) },
        // ------------------------------------------------------------------ NOTES
        WidgetStyle("note_card", Cat.NOTES, "Note", "ملاحظة", S, isNew = true, feats = nF, upd = none, defaults = { it.copy(accent = 0xFFF2C14E.toInt(), tap = com.smrh.zujaj.model.Tap.EDIT) }) { Life2R.note(it) },
        WidgetStyle("note_sticky", Cat.NOTES, "Sticky Note", "ورقة لاصقة", S, isNew = true, popular = true, feats = nF, upd = none, defaults = { it.copy(accent = 0xFFF2C14E.toInt(), tap = com.smrh.zujaj.model.Tap.EDIT) }) { Life2R.sticky(it) },
        WidgetStyle("note_strip", Cat.NOTES, "Note Strip", "شريط ملاحظة", B, isNew = true, feats = nF, upd = none, defaults = { it.copy(accent = 0xFFF2C14E.toInt(), tap = com.smrh.zujaj.model.Tap.EDIT) }) { Life2R.noteStrip(it) },
        WidgetStyle("note_big", Cat.NOTES, "Big Note", "ملاحظة كبيرة", W, pro = true, isNew = true, feats = nF, upd = none, defaults = { it.copy(tap = com.smrh.zujaj.model.Tap.EDIT) }) { Life2R.bigNote(it) },
        WidgetStyle("note_list", Cat.NOTES, "Notes List", "قائمة ملاحظات", W, isNew = true, feats = nF, upd = none, defaults = { it.copy(accent = 0xFFF2C14E.toInt(), tap = com.smrh.zujaj.model.Tap.EDIT) }) { Life2R.notesList(it) },
        // ------------------------------------------------------------------ FOCUS
        WidgetStyle("focus_card", Cat.FOCUS, "Focus", "تركيز", S, isNew = true, feats = fF, upd = none, defaults = fD) { Life2R.focus(it) },
        WidgetStyle("focus_centre", Cat.FOCUS, "Focus Centre", "مركز التركيز", S, isNew = true, feats = fF, upd = none, defaults = fD) { Life2R.focusCentre(it) },
        WidgetStyle("focus_board", Cat.FOCUS, "Focus Board", "لوحة التركيز", W, pro = true, isNew = true, feats = fF, upd = none, defaults = fD) { Life2R.focusBoard(it) },
        // ------------------------------------------------------------------ TOOLS (more)
        WidgetStyle("t_volume_slider", Cat.TOOLS, "Volume Slider", "شريط الصوت", S, isNew = true, upd = sysU) { Tools2R.volumeSlider(it) },
        WidgetStyle("t_volume_keys", Cat.TOOLS, "Volume Keys", "أزرار الصوت", S, isNew = true, upd = sysU) { Tools2R.volumeKeys(it) },
        WidgetStyle("t_sound_mode", Cat.TOOLS, "Sound Mode", "وضع الصوت", S, isNew = true, popular = true, feats = setOf(Feat.ACCENT), upd = sysU, defaults = { it.copy(accent = 0xFF4C8DF6.toInt()) }) { Tools2R.soundMode(it) },
        WidgetStyle("t_sound_buttons", Cat.TOOLS, "Sound Buttons", "أزرار الرنين", B, pro = true, isNew = true, feats = setOf(Feat.ACCENT), upd = sysU, defaults = { it.copy(accent = 0xFF4C8DF6.toInt()) }) { Tools2R.soundButtons(it) },
        WidgetStyle("t_floating_keys", Cat.TOOLS, "Floating Keys", "مفاتيح عائمة", B, isNew = true, feats = setOf(Feat.ACCENT), upd = sysU, defaults = { it.copy(accent = 0xFF4C8DF6.toInt()) }) { Tools2R.floatingKeys(it) },
        WidgetStyle("t_torch", Cat.TOOLS, "Torch", "الكشاف", S, isNew = true, upd = none) { Tools2R.torch(it) },
        WidgetStyle("t_quick_tools", Cat.TOOLS, "Quick Tools", "أدوات سريعة", B, isNew = true, upd = none) { Tools2R.quickTools(it) },
        WidgetStyle("t_search_tools", Cat.SEARCH, "Search & Tools", "البحث والأدوات", W, pro = true, isNew = true, feats = setOf(Feat.ACCENT), upd = none, defaults = { it.copy(accent = 0xFFE5674F.toInt()) }) { Tools2R.searchTools(it) },
        WidgetStyle("t_around_me", Cat.TOOLS, "Around Me", "حولي", W, pro = true, isNew = true, upd = none) { Tools2R.aroundMe(it) },
    )

    private val map = all.associateBy { it.id }
    fun byId(id: String) = map[id]
}
