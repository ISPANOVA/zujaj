package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.DayOfWeek
import java.time.temporal.IsoFields

/** Calendar & date designs. */
object Cal2R {
    private fun weekStart(r: RenderCtx) = r.today.minusDays(((r.today.dayOfWeek.value % 7)).toLong()) // Sunday

    private fun dayLetter(r: RenderCtx, d: java.time.LocalDate): String {
        val en = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val ar = listOf("اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد")
        return (if (r.ar) ar else en)[d.dayOfWeek.value - 1]
    }

    // Seven — weekday name, date, the week strip (W)
    fun seven(r: RenderCtx) {
        r.text(Kit.dayName(r), r.pad + 4 * r.u, r.h * 0.2f, 26 * r.u * r.s1, r.c1, r.sansBold, maxW = r.w * 0.6f)
        r.text(Kit.greg(r), r.w - r.pad - 4 * r.u, r.h * 0.2f, 11 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT, maxW = r.w * 0.35f)
        val start = weekStart(r)
        val cw = (r.w - 2 * r.pad) / 7
        for (k in 0 until 7) {
            val i = if (r.ar) 6 - k else k
            val d = start.plusDays(i.toLong())
            val cx = r.pad + cw * (k + 0.5f)
            val on = d == r.today
            r.text(dayLetter(r, d).take(3), cx, r.h * 0.5f, 8.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            r.text(r.n(d.dayOfMonth), cx, r.h * 0.7f, 15 * r.u * r.s2, r.c1, if (on) r.sansBold else r.sans, Paint.Align.CENTER)
            if (on) r.circle(cx, r.h * 0.84f, 2 * r.u, r.c1)
        }
    }

    // Progress — week number, month and year bars (W)
    fun progress(r: RenderCtx) {
        val d = r.today
        r.text(r.t("WEEK", "الأسبوع"), r.pad + 4 * r.u, r.h * 0.1f, 8 * r.u, r.c2, r.sansBold, spacing = 0.18f)
        r.text(r.n(d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)), r.pad + 4 * r.u, r.h * 0.27f, 26 * r.u * r.s1, r.c1, r.sansBold)
        r.text(Kit.greg(r), r.w - r.pad - 4 * r.u, r.h * 0.22f, 10.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        val mf = d.dayOfMonth / d.lengthOfMonth().toFloat()
        val yf = d.dayOfYear / d.lengthOfYear().toFloat()
        for ((i, row) in listOf(Triple(Calendars.monthName(d.monthValue, r.ar), mf, r.accent), Triple(r.n(d.year), yf, r.c2)).withIndex()) {
            val y = r.h * (0.5f + i * 0.28f)
            r.text(row.first, r.pad + 4 * r.u, y, 12 * r.u, r.c1, r.sansBold)
            r.text(r.n("${(row.second * 100).toInt()}%"), r.w - r.pad - 4 * r.u, y, 11 * r.u, row.third, r.sansBold, Paint.Align.RIGHT)
            Kit.slider(r, r.pad + 4 * r.u, y + 13 * r.u, r.w - 2 * r.pad - 8 * r.u, row.second, row.third, withAlpha(r.c1, 26), 3 * r.u, 0f)
        }
    }

    // Weekband — time + day, week & day-of-year, week strip with today circled (W)
    fun weekband(r: RenderCtx) {
        Kit.clock(r, r.pad + 2 * r.u, r.h * 0.17f, 22 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.LEFT)
        Kit.calli(r, Kit.dayName(r, true), r.w * 0.78f, r.h * 0.15f, r.h * 0.18f, r.c1, maxW = r.w * 0.4f, maxH = r.h * 0.18f)
        r.text(Kit.greg(r), r.w - r.pad, r.h * 0.33f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        r.text(r.n(r.today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)), r.pad + 4 * r.u, r.h * 0.36f, 10 * r.u, r.c1, r.sansBold)
        r.text(r.t("week", "الأسبوع"), r.pad + 22 * r.u, r.h * 0.36f, 7.5f * r.u, r.accent, r.sans)
        r.text(r.n(r.today.dayOfYear), r.pad + 70 * r.u, r.h * 0.36f, 10 * r.u, r.c1, r.sansBold)
        r.text(r.t("day", "اليوم"), r.pad + 92 * r.u, r.h * 0.36f, 7.5f * r.u, r.accent, r.sans)
        val start = weekStart(r)
        val cw = (r.w - 2 * r.pad) / 7
        for (k in 0 until 7) {
            val i = if (r.ar) 6 - k else k
            val d = start.plusDays(i.toLong()); val cx = r.pad + cw * (k + 0.5f)
            val on = d == r.today
            r.text(dayLetter(r, d), cx, r.h * 0.55f, 7.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            if (on) r.circle(cx, r.h * 0.72f, 10 * r.u, r.accent)
            r.text(r.n(d.dayOfMonth), cx, r.h * 0.72f, 11 * r.u * r.s2, if (on) r.cfg.bgColor or 0xFF000000.toInt() else r.c1, r.sansBold, Paint.Align.CENTER)
        }
        val f = (r.today.dayOfWeek.value % 7 + 1) / 7f
        Kit.slider(r, r.pad + 4 * r.u, r.h * 0.9f, r.w - 2 * r.pad - 8 * r.u, if (r.ar) 1 - f else f, r.accent, withAlpha(r.c1, 30), 2 * r.u, 0f)
    }

    // Fraction — day over month total, time, days left and a dot bar (W)
    fun fraction(r: RenderCtx) {
        val d = r.today
        val cx = r.pad + 30 * r.u
        r.textTight(r.n(d.dayOfMonth), cx, r.h * 0.2f, 24 * r.u * r.s1, r.c1, r.sansBold)
        Kit.line(r, cx - 22 * r.u, r.h * 0.34f, cx + 22 * r.u, r.h * 0.34f, r.c1, 1.2f * r.u)
        r.textTight(r.n(d.lengthOfMonth()), cx, r.h * 0.48f, 24 * r.u * r.s1, r.c1, r.sansBold)
        r.text(r.t("OF THE MONTH", "من الشهر"), cx, r.h * 0.62f, 6.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, spacing = 0.1f)
        Kit.line(r, cx + 34 * r.u, r.h * 0.1f, cx + 34 * r.u, r.h * 0.62f, withAlpha(r.c1, 40), 0.8f * r.u)
        Kit.clock(r, cx + 44 * r.u, r.h * 0.28f, 28 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.LEFT, maxW = r.w * 0.45f,
            pattern = if (r.cfg.hour24) "HH:mm" else "h:mm a")
        r.text(Kit.dayName(r), cx + 44 * r.u, r.h * 0.48f, 11 * r.u, r.c2, r.sansMed)
        val left = d.lengthOfMonth() - d.dayOfMonth
        r.text(r.t("LEFT", "باقي"), r.w - r.pad, r.h * 0.12f, 6.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT, spacing = 0.15f)
        r.text(r.n(left), r.w - r.pad, r.h * 0.26f, 14 * r.u, r.accent, r.sansBold, Paint.Align.RIGHT)
        r.text(r.t("days", "يوم"), r.w - r.pad, r.h * 0.38f, 6.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        r.text(Calendars.monthName(d.monthValue, r.ar), r.w - r.pad, r.h * 0.5f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        val n = d.lengthOfMonth()
        val w = r.w - 2 * r.pad - 8 * r.u
        for (k in 0 until n) {
            val x = r.pad + 4 * r.u + w * (k + 0.5f) / n
            r.circle(x, r.h * 0.76f, minOf(3.2f * r.u, w / n * 0.38f), when { k + 1 == d.dayOfMonth -> r.accent; k + 1 < d.dayOfMonth -> r.c1; else -> withAlpha(r.c1, 50) })
        }
        r.text(r.t("MONTH", "الشهر"), r.pad + 4 * r.u, r.h * 0.9f, 6.5f * r.u, r.c2, r.sans, spacing = 0.15f)
        r.text(r.n("${(d.dayOfMonth * 100 / n)}%"), r.w - r.pad - 4 * r.u, r.h * 0.9f, 7.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    private fun monthGrid(r: RenderCtx, area: RectF, hijri: Boolean) {
        val today = r.today
        val first: java.time.LocalDate
        val len: Int
        val todayN: Int
        if (hijri) {
            val h = Calendars.hijri(today)
            first = today.minusDays((h.day - 1).toLong())
            len = run { var n = 29; if (Calendars.hijri(first.plusDays(29)).month == h.month) n = 30; n }
            todayN = h.day
        } else { first = today.withDayOfMonth(1); len = today.lengthOfMonth(); todayN = today.dayOfMonth }
        val lead = first.dayOfWeek.value % 7 // Sunday first
        val rows = (lead + len + 6) / 7
        val cw = area.width() / 7; val rh = (area.height() - 12 * r.u) / rows
        for (k in 0 until 7) {
            val dd = java.time.LocalDate.of(2023, 1, 1).plusDays(k.toLong()) // a Sunday
            val col = if (r.ar) 6 - k else k
            r.text(dayLetter(r, dd).take(if (r.ar) 2 else 1), area.left + cw * (col + 0.5f), area.top + 5 * r.u, 6.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        }
        for (n in 1..len) {
            val idx = lead + n - 1
            val col = idx % 7; val row = idx / 7
            val x = area.left + cw * ((if (r.ar) 6 - col else col) + 0.5f)
            val y = area.top + 12 * r.u + rh * (row + 0.5f)
            val on = n == todayN
            if (on) r.circle(x, y, minOf(cw, rh) * 0.42f, r.c1)
            val fri = col == 5
            r.text(r.n(n), x, y, minOf(8.5f * r.u, rh * 0.62f), when { on -> r.cfg.bgColor or 0xFF000000.toInt(); fri -> r.accent; else -> r.c1 }, if (on) r.sansBold else r.sans, Paint.Align.CENTER)
        }
    }

    // Hijri calendar — hijri month grid + day, clock, weather and battery (W)
    fun hijriCalendar(r: RenderCtx) {
        val split = r.w * 0.62f
        val h = Calendars.hijri(r.today)
        r.text(Calendars.hijriMonth(h.month, r.ar), r.pad + 2 * r.u, r.h * 0.1f, 9 * r.u, r.c1, r.sansBold)
        r.text(r.t("Hijri", "تقويم هجري"), split - 6 * r.u, r.h * 0.1f, 7.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        monthGrid(r, RectF(r.pad, r.h * 0.17f, split - 4 * r.u, r.h - r.pad * 0.6f), true)
        val cx = (split + r.w - r.pad) / 2
        Kit.calli(r, Kit.dayName(r, true), cx, r.h * 0.16f, r.h * 0.14f, r.c1, maxW = r.w - split - r.pad, maxH = r.h * 0.16f)
        Kit.clock(r, cx, r.h * 0.48f, 36 * r.u * r.s1, LiveFont.BEBAS, maxW = r.w - split - r.pad, maxH = r.h * 0.38f)
        val w = Kit.weather(r)
        Glyphs.sun(r.canvas, split + 14 * r.u, r.h * 0.84f, 5 * r.u, r.accent)
        r.text(Kit.temp(r, w.temp), split + 22 * r.u, r.h * 0.84f, 10 * r.u, r.c1, r.sansMed)
        r.text(r.n("${Kit.battery(r)}%"), r.w - r.pad, r.h * 0.84f, 10 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
    }

    // Rem — big day + hijri, month stats, gregorian grid (W)
    fun remCalendar(r: RenderCtx) {
        val split = r.w * 0.4f
        val d = r.today
        r.textTight(r.n(d.dayOfMonth), split - 18 * r.u, r.h * 0.2f, 22 * r.u * r.s1, r.c1, r.sansBold)
        r.text(Kit.dayName(r, true).let { if (r.ar) it else Kit.dayName(r) }, r.pad + 4 * r.u, r.h * 0.13f, 9 * r.u, r.accent, r.sansBold)
        r.text(Kit.hijriShort(r), r.pad + 4 * r.u, r.h * 0.25f, 7.5f * r.u, r.c2, r.sans)
        val stats = listOf(
            r.t("week", "الأسبوع") to r.n(d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)),
            r.t("of month", "من الشهر") to r.n((d.dayOfMonth + 6) / 7),
            r.t("length", "الطول") to r.n(d.lengthOfMonth()),
            r.t("left", "يتبقى") to r.n(d.lengthOfMonth() - d.dayOfMonth),
        )
        stats.forEachIndexed { i, (k, v) ->
            val col = i % 2; val row = i / 2
            val x = r.pad + 4 * r.u + col * (split - r.pad) / 2
            r.text(k, x, r.h * (0.45f + row * 0.2f), 7 * r.u, r.c2, r.sans)
            r.text(v, x, r.h * (0.54f + row * 0.2f), 10 * r.u, if (i == 3) r.accent else r.c1, r.sansBold)
        }
        Kit.slider(r, r.pad + 4 * r.u, r.h * 0.9f, split - r.pad - 14 * r.u, d.dayOfMonth / d.lengthOfMonth().toFloat(), r.accent, withAlpha(r.c1, 30), 2 * r.u, 0f)
        Kit.line(r, split, r.h * 0.08f, split, r.h * 0.92f, withAlpha(r.c1, 30), 0.7f * r.u)
        monthGrid(r, RectF(split + 6 * r.u, r.h * 0.08f, r.w - r.pad * 0.7f, r.h - r.pad * 0.6f), false)
    }

    // Constellation — the month as dots, today highlighted (W)
    fun constellation(r: RenderCtx) {
        val d = r.today
        val split = r.w * 0.22f
        val n = d.lengthOfMonth()
        val cols = 8; val rows = (n + cols - 1) / cols
        val area = RectF(split + 10 * r.u, r.pad, r.w - r.pad, r.h * 0.72f)
        val cw = area.width() / cols; val rh = area.height() / rows
        for (k in 0 until n) {
            val col = k % cols; val row = k / cols
            val x = area.left + cw * ((if (r.ar) cols - 1 - col else col) + 0.5f); val y = area.top + rh * (row + 0.5f)
            r.circle(x, y, 3.6f * r.u, when { k + 1 == d.dayOfMonth -> r.accent; k + 1 < d.dayOfMonth -> r.c1; else -> withAlpha(r.c1, 60) })
        }
        // the week as a small 2-column block
        for (k in 0 until 7) {
            val x = r.pad + 8 * r.u + (k % 2) * 14 * r.u; val y = r.pad + 8 * r.u + (k / 2) * 14 * r.u
            r.circle(x, y, 3.6f * r.u, if (k < (d.dayOfWeek.value % 7 + 1)) r.c1 else withAlpha(r.c1, 60))
        }
        Kit.line(r, r.pad, r.h * 0.78f, r.w - r.pad, r.h * 0.78f, withAlpha(r.c1, 30), 0.7f * r.u)
        r.text(Calendars.monthName(d.monthValue, false).take(3), r.pad + 6 * r.u, r.h * 0.9f, 11 * r.u, r.c1, r.sansMed)
        r.text(Calendars.monthName(d.monthValue, true), split + 14 * r.u, r.h * 0.9f, 11 * r.u, r.c1, r.sansMed)
        r.text(r.n(d.dayOfMonth), r.w - r.pad, r.h * 0.9f, 15 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.RIGHT)
    }

    // Terminal — two columns, English and Arabic, in a mono green (W)
    fun terminal(r: RenderCtx) {
        val green = r.accent
        val d = r.today
        r.text("zujaj", r.w - r.pad, r.h * 0.08f, 7 * r.u, withAlpha(green, 120), F.SANS, Paint.Align.RIGHT, spacing = 0.2f)
        for ((x, blink) in listOf(r.w * 0.36f to 0, r.w * 0.64f to 1)) r.canvas.drawRect(x - 2 * r.u, r.h * 0.13f, x + 2 * r.u, r.h * 0.23f, r.fillPaint(green))
        val en = listOf(Kit.bakedTime(r, "HH:mm"), "${d.dayOfMonth} ${Calendars.monthName(d.monthValue, false).take(3)}",
            Calendars.hijri(d).let { "${it.day} ${Calendars.hijriMonth(it.month, false).take(3)}" }, "${Kit.battery(r)}%")
        val ar = listOf(Calendars.toArabicDigits(Kit.bakedTime(r, "HH:mm")), "${Calendars.toArabicDigits(d.dayOfMonth.toString())} ${Calendars.monthName(d.monthValue, true)}",
            Calendars.hijri(d).let { "${Calendars.toArabicDigits(it.day.toString())} ${Calendars.hijriMonth(it.month, true)}" }, Calendars.toArabicDigits("${Kit.battery(r)}") + "٪")
        for (i in 0 until 4) {
            val y = r.h * (0.38f + i * 0.16f)
            if (i == 0 && !r.gradTime) {
                r.liveTime("HH:mm", r.w * 0.45f, y, 12 * r.u * r.s1, LiveFont.SANS_BOLD, green, Paint.Align.RIGHT, sample = "00:00")
                r.liveTime("HH:mm", r.w * 0.55f, y, 12 * r.u * r.s1, LiveFont.AR_CAIRO, green, Paint.Align.LEFT, sample = "00:00")
                continue
            }
            r.text(en[i], r.w * 0.45f, y, 12 * r.u * r.s1, green, F.SANS_BOLD, Paint.Align.RIGHT)
            r.text(ar[i], r.w * 0.55f, y, 12 * r.u * r.s1, green, F.CAIRO_BOLD)
        }
    }

    // Arc — week ring + time, day, weather and three stats (W)
    fun arc(r: RenderCtx) {
        val cx = r.pad + r.h * 0.38f; val cy = r.h / 2; val rad = r.h * 0.3f
        val wk = (r.today.dayOfMonth + 6) / 7
        val o = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(o, 135f, 270f, false, r.strokePaint(withAlpha(r.c1, 30), 4 * r.u))
        r.canvas.drawArc(o, 135f, 270f * wk / 5f, false, r.strokePaint(r.accent, 4 * r.u))
        r.textTight(r.n(wk), cx, cy - 4 * r.u, 22 * r.u, r.c1, r.sansBold)
        r.text(r.t("WEEK", "أسبوع"), cx, cy + 14 * r.u, 7 * r.u, r.c2, r.sans, Paint.Align.CENTER, spacing = 0.15f)
        val l = cx + rad + 16 * r.u
        Kit.clock(r, l, r.h * 0.24f, 26 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.LEFT, maxW = r.w * 0.38f, pattern = if (r.cfg.hour24) "HH:mm" else "h:mm a")
        r.text(Kit.dayName(r), l, r.h * 0.42f, 11 * r.u, r.c2, r.sansMed)
        val w = Kit.weather(r)
        r.text(Kit.temp(r, w.temp) + " / " + Kit.temp(r, w.hi), r.w - r.pad, r.h * 0.26f, 10 * r.u, r.c1, r.sans, Paint.Align.RIGHT)
        r.text(Kit.gregShort(r), r.w - r.pad, r.h * 0.4f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        Kit.line(r, l, r.h * 0.55f, r.w - r.pad, r.h * 0.55f, withAlpha(r.c1, 26), 0.7f * r.u)
        val h = Calendars.hijri(r.today)
        val items = listOf(r.t("HIJRI", "هجري") to r.n(h.day), r.t("POWER", "الشحن") to r.n("${Kit.battery(r)}%"),
            r.t("MONTH", "الشهر") to r.n("${r.today.dayOfMonth * 100 / r.today.lengthOfMonth()}%"))
        val cw = (r.w - r.pad - l) / 3
        items.forEachIndexed { i, (k, v) ->
            val x = l + cw * i
            if (i > 0) Kit.line(r, x - 4 * r.u, r.h * 0.62f, x - 4 * r.u, r.h * 0.86f, withAlpha(r.c1, 26), 0.7f * r.u)
            r.text(k, x, r.h * 0.66f, 6.5f * r.u, r.c2, r.sans, spacing = 0.12f)
            r.text(v, x, r.h * 0.8f, 12 * r.u, if (i == 2) r.accent else r.c1, r.sansBold)
        }
    }

    @Suppress("unused") private val dummy = DayOfWeek.MONDAY
}
