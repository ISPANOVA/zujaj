package com.smrh.zujaj.render

import android.content.Intent
import android.graphics.Paint
import android.graphics.RectF
import android.provider.CalendarContract
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.model.Cal
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

object DateR {
    private fun hijri(r: RenderCtx) = Calendars.hijri(r.today)

    fun calendarIntent(): Intent = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build())

    fun fullDate(r: RenderCtx, cal: Cal = r.cfg.calendar): String {
        val d = r.today
        val day = Calendars.dayName(d, r.ar)
        return if (cal == Cal.HIJRI) {
            val h = hijri(r)
            if (r.ar) "$day، ${r.n(h.day)} ${Calendars.hijriMonth(h.month, true)} ${r.n(h.year)}"
            else "$day, ${h.day} ${Calendars.hijriMonth(h.month, false)} ${h.year} AH"
        } else {
            if (r.ar) "$day، ${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)} ${r.n(d.year)}"
            else "$day, ${Calendars.monthName(d.monthValue, false)} ${d.dayOfMonth}, ${d.year}"
        }
    }

    fun bannerSimple(r: RenderCtx) {
        r.text(fullDate(r), r.w / 2, r.h / 2, 18 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad - 8 * r.u)
    }

    fun bannerWeather(r: RenderCtx) {
        val d = r.today
        val w = WeatherRepo.main() ?: WeatherRepo.sample
        val x = r.pad + 8 * r.u
        r.text(Calendars.dayName(d, r.ar), x, r.h / 2 - 9 * r.u, 12 * r.u * r.s2, r.c1, r.sans, maxW = r.w * 0.26f)
        r.text("${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, r.ar)}", x, r.h / 2 + 9 * r.u, 12 * r.u * r.s2, r.c2, r.sans, maxW = r.w * 0.26f)
        r.liveTime(r.hm(), r.w / 2, r.h / 2, 32 * r.u * r.s1, LiveFont.SANS, r.c1, maxW = r.w * 0.4f, sample = "00:00")
        val xr = r.w - r.pad - 8 * r.u
        r.text(r.n(WeatherRepo.fmt(w.temp)), xr, r.h / 2 - 9 * r.u, 12 * r.u * r.s2, r.c1, r.sans, Paint.Align.RIGHT)
        r.text(WeatherRepo.condName(w.cond, r.ar), xr, r.h / 2 + 9 * r.u, 12 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.24f)
    }

    private fun weekHeaders(r: RenderCtx, short: Boolean): List<String> {
        // Week starts Saturday for Arabic, Monday for English
        return if (r.ar) {
            if (short) listOf("س", "ح", "ن", "ث", "ر", "خ", "ج") else listOf("سبت", "أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
        } else {
            if (short) listOf("M", "T", "W", "T", "F", "S", "S") else listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        }
    }

    /** column (0..6) of a date given locale week start */
    private fun col(r: RenderCtx, d: LocalDate): Int {
        val dow = d.dayOfWeek.value // Mon=1..Sun=7
        return if (r.ar) (dow + 1) % 7 /* Sat=0 */ else dow - 1
    }

    private fun monthGrid(r: RenderCtx, area: RectF, short: Boolean) {
        val ym = YearMonth.from(r.today)
        val first = ym.atDay(1)
        val start = first.minusDays(col(r, first).toLong())
        val rows = 7 // header + 6 weeks
        val cw = area.width() / 7
        val rh = area.height() / rows
        val heads = weekHeaders(r, short)
        val fs = min(cw * 0.42f, rh * 0.5f) * r.s1
        for (i in 0 until 7) r.text(heads[i], area.left + cw * (i + 0.5f), area.top + rh * 0.5f, fs * 0.82f, r.c2, r.sans, Paint.Align.CENTER, maxW = cw * 0.9f)
        for (k in 0 until 42) {
            val d = start.plusDays(k.toLong())
            val cx = area.left + cw * (k % 7 + 0.5f)
            val cy = area.top + rh * (k / 7 + 1.5f)
            val inMonth = d.month == first.month
            if (d == r.today) {
                val rr = RectF(cx - cw * 0.42f, cy - rh * 0.44f, cx + cw * 0.42f, cy + rh * 0.44f)
                r.canvas.drawRoundRect(rr, rh * 0.3f, rh * 0.3f, r.fillPaint(withAlpha(r.accentOr(r.c1), 70)))
            }
            r.text(r.n(d.dayOfMonth), cx, cy, fs, if (inMonth) r.c1 else withAlpha(r.c2, 110), r.sans, Paint.Align.CENTER)
        }
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Launch(calendarIntent()))
    }

    fun calendarSmall(r: RenderCtx) {
        val p = r.pad * 0.7f
        monthGrid(r, RectF(p, p, r.w - p, r.h - p), true)
    }

    fun calendarWide(r: RenderCtx) {
        val p = r.pad
        val d = r.today
        r.text("${Calendars.monthName(d.monthValue, r.ar)} ${r.n(d.year)}", p + 6 * r.u, p + 8 * r.u, 15 * r.u * r.s2, r.c1, r.sansBold)
        val h = hijri(r)
        r.text("${r.n(h.day)} ${Calendars.hijriMonth(h.month, r.ar)} ${r.n(h.year)}", r.w - p - 6 * r.u, p + 8 * r.u, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
        monthGrid(r, RectF(p, p + 22 * r.u, r.w - p, r.h - p * 0.6f), false)
    }

    fun dotsMonth(r: RenderCtx) {
        val d = r.today
        val p = r.pad
        val ym = YearMonth.from(d)
        r.text(Calendars.monthName(d.monthValue, r.ar), p + 2 * r.u, p + 6 * r.u, 12 * r.u * r.s2, r.c1, r.sansMed)
        val pct = d.dayOfMonth * 100 / ym.lengthOfMonth()
        r.text(r.n("$pct%"), r.w - p - 2 * r.u, p + 6 * r.u, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
        val area = RectF(p, p + 22 * r.u, r.w - p, r.h - p)
        val cols = 7
        val rows = ceil(ym.lengthOfMonth() / 7f).toInt()
        val cw = area.width() / cols
        val rh = area.height() / rows
        val rad = min(cw, rh) * 0.22f * r.s1
        for (i in 0 until ym.lengthOfMonth()) {
            val cx = area.left + cw * (i % cols + 0.5f)
            val cy = area.top + rh * (i / cols + 0.5f)
            val day = i + 1
            when {
                day < d.dayOfMonth -> r.circle(cx, cy, rad, r.c1)
                day == d.dayOfMonth -> r.circle(cx, cy, rad * 1.25f, r.accentOr(r.c1))
                else -> r.canvas.drawCircle(cx, cy, rad * 0.9f, r.strokePaint(withAlpha(r.c2, 160), rad * 0.35f))
            }
        }
    }

    fun dotsYear(r: RenderCtx) {
        val d = r.today
        val p = r.pad
        r.text(r.n(d.year), p + 2 * r.u, p + 6 * r.u, 12 * r.u * r.s2, r.c1, r.sansMed)
        val len = d.lengthOfYear()
        val pct = d.dayOfYear * 100 / len
        r.text(r.n("$pct%"), r.w - p - 2 * r.u, p + 6 * r.u, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
        val area = RectF(p, p + 20 * r.u, r.w - p, r.h - p)
        val ratio = area.width() / area.height()
        var cols = ceil(sqrt(len * ratio)).toInt()
        val rows = ceil(len / cols.toFloat()).toInt()
        cols = ceil(len / rows.toFloat()).toInt()
        val cw = area.width() / cols
        val rh = area.height() / rows
        val rad = min(cw, rh) * 0.28f * r.s1
        for (i in 0 until len) {
            val cx = area.left + cw * (i % cols + 0.5f)
            val cy = area.top + rh * (i / cols + 0.5f)
            val c = when {
                i + 1 < d.dayOfYear -> r.c1
                i + 1 == d.dayOfYear -> r.accentOr(r.c1)
                else -> withAlpha(r.c2, 70)
            }
            r.circle(cx, cy, if (i + 1 == d.dayOfYear) rad * 1.4f else rad, c)
        }
    }

    fun daysElegant(r: RenderCtx) {
        val name = Calendars.dayName(r.today, r.ar)
        if (r.ar) {
            r.textTight(name, r.w / 2, r.h / 2, r.h * 0.5f * r.s1, r.c1, F.AMIRI, maxW = r.w - 2 * r.pad)
        } else {
            r.textTight(name.uppercase(), r.w / 2, r.h / 2, r.h * 0.32f * r.s1, r.c1, F.BODONI_REG, spacing = 0.32f * r.s2, maxW = r.w - 2 * r.pad - 10 * r.u)
        }
    }

    fun daysMinimal(r: RenderCtx) {
        val name = Calendars.dayName(r.today, r.ar)
        if (r.ar) r.textTight(name, r.w / 2, r.h / 2, r.h * 0.34f * r.s1, r.c1, F.AMIRI, maxW = r.w - 2 * r.pad)
        else r.textTight(name.take(3).uppercase(), r.w / 2, r.h / 2, r.h * 0.26f * r.s1, r.c1, F.BODONI_REG, spacing = 0.35f, maxW = r.w - 2 * r.pad)
    }

    fun daysCalli(r: RenderCtx) {
        val name = Calendars.dayName(r.today, true)
        r.textTight(name, r.w / 2, r.h / 2, r.h * 0.62f * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad - 20 * r.u, maxH = r.h - 2 * r.pad)
    }

    fun daysCalliSmall(r: RenderCtx) {
        val name = Calendars.dayName(r.today, true)
        r.textTight(name, r.w / 2, r.h * 0.46f, r.h * 0.42f * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad, maxH = r.h * 0.5f)
        val d = r.today
        r.text("${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, r.ar)}", r.w / 2, r.h * 0.82f, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
    }

    private fun monthInfo(r: RenderCtx): Triple<Int, String, String> {
        val d = r.today
        return if (r.cfg.calendar == Cal.HIJRI) {
            val h = hijri(r)
            Triple(h.day, Calendars.hijriMonth(h.month, false), Calendars.hijriMonth(h.month, true))
        } else Triple(d.dayOfMonth, Calendars.monthName(d.monthValue, false), Calendars.monthName(d.monthValue, true))
    }

    fun monthsElegant(r: RenderCtx) {
        val (day, en, arName) = monthInfo(r)
        r.textTight(r.n(day), r.w / 2, r.h / 2, r.h * 0.62f * r.s2, withAlpha(r.c2, 120), F.BODONI)
        if (r.ar) r.textTight(arName, r.w / 2, r.h / 2, r.h * 0.42f * r.s1, r.c1, F.AMIRI, maxW = r.w - 2 * r.pad)
        else r.textTight(en.uppercase(), r.w / 2, r.h / 2, r.h * 0.28f * r.s1, r.c1, F.BODONI_REG, spacing = 0.28f, maxW = r.w - 2 * r.pad - 8 * r.u)
    }

    fun monthsCalli(r: RenderCtx) {
        val (day, _, arName) = monthInfo(r)
        r.textTight(r.n(String.format(java.util.Locale.US, "%02d", day)), r.w / 2, r.h * 0.44f, r.h * 0.42f * r.s2, withAlpha(r.c2, 150), F.SANS_BOLD)
        r.textTight(arName, r.w / 2, r.h * 0.44f, r.h * 0.6f * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad - 10 * r.u, maxH = r.h * 0.62f)
        val d = r.today
        r.text(r.n("${d.dayOfMonth.toString().padStart(2, '0')} / ${d.monthValue.toString().padStart(2, '0')} / ${d.year}"), r.w / 2, r.h * 0.85f, 10 * r.u, withAlpha(r.c1, 190), r.sans, Paint.Align.CENTER)
    }

    fun monthsCalliSmall(r: RenderCtx) {
        val (day, _, arName) = monthInfo(r)
        r.textTight(r.n(day), r.w / 2, r.h * 0.4f, r.h * 0.6f * r.s2, withAlpha(r.c2, 110), F.SANS_BOLD)
        r.textTight(arName, r.w / 2, r.h * 0.44f, r.h * 0.36f * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad, maxH = r.h * 0.45f)
        val d = r.today
        r.text("${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, r.ar)}", r.w / 2, r.h * 0.84f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
    }

    fun dualBanner(r: RenderCtx) {
        val d = r.today
        val h = hijri(r)
        val cy = r.h / 2
        val half = r.w / 2
        r.canvas.drawLine(half, r.h * 0.25f, half, r.h * 0.75f, r.strokePaint(withAlpha(r.c1, 50), r.u))
        // left: hijri
        val numSize = r.h * 0.42f * r.s1
        val nL = r.n(h.day)
        val nW = r.measure(nL, numSize, F.SANS_BOLD)
        r.text(nL, r.pad + 6 * r.u, cy, numSize, r.c1, F.SANS_BOLD)
        r.textTight(Calendars.hijriMonth(h.month, true), (r.pad + 6 * r.u + nW + half) / 2 + 4 * r.u, cy, r.h * 0.5f * r.s1, r.c1, r.calliFont, maxW = half - nW - r.pad - 20 * r.u, maxH = r.h * 0.6f)
        // right: gregorian
        val nR = r.n(d.dayOfMonth)
        val nRW = r.measure(nR, numSize, F.SANS_BOLD)
        r.text(nR, r.w - r.pad - 6 * r.u, cy, numSize, r.c1, F.SANS_BOLD, Paint.Align.RIGHT)
        r.textTight(Calendars.monthName(d.monthValue, true), (half + r.w - r.pad - 6 * r.u - nRW) / 2 - 4 * r.u, cy, r.h * 0.5f * r.s1, r.c1, r.calliFont, maxW = half - nRW - r.pad - 20 * r.u, maxH = r.h * 0.6f)
    }

    fun hijriGregorian(r: RenderCtx) {
        r.text(fullDate(r, Cal.HIJRI), r.w / 2, r.h * 0.36f, 14 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        val d = r.today
        val g = if (r.ar) "${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)} ${r.n(d.year)}" else "${Calendars.monthName(d.monthValue, false)} ${d.dayOfMonth}, ${d.year}"
        r.text(g, r.w / 2, r.h * 0.66f, 12 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun compact(r: RenderCtx) {
        val d = r.today
        val (day, month) = if (r.cfg.calendar == Cal.HIJRI) {
            val h = hijri(r); h.day to Calendars.hijriMonth(h.month, r.ar)
        } else d.dayOfMonth to Calendars.monthName(d.monthValue, r.ar)
        r.text(Calendars.dayName(d, r.ar), r.w / 2, r.h * 0.26f, 12 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
        r.textTight(r.n(day), r.w / 2, r.h * 0.52f, 60 * r.u * r.s1, r.c1, F.SANS_MED)
        r.text(month, r.w / 2, r.h * 0.78f, 12 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun weekStrip(r: RenderCtx) {
        val d = r.today
        val start = d.minusDays(col(r, d).toLong())
        val heads = weekHeaders(r, false)
        val cw = (r.w - 2 * r.pad) / 7
        for (i in 0 until 7) {
            val day = start.plusDays(i.toLong())
            val cx = r.pad + cw * (i + 0.5f)
            val isToday = day == d
            if (isToday) {
                val rr = RectF(cx - cw * 0.44f, r.h * 0.14f, cx + cw * 0.44f, r.h * 0.86f)
                r.canvas.drawRoundRect(rr, cw * 0.3f, cw * 0.3f, r.fillPaint(r.accentOr(r.c1)))
            }
            val col = if (isToday) r.bgColorSafe() else r.c1
            r.text(heads[i], cx, r.h * 0.34f, 10 * r.u * r.s2, if (isToday) col else r.c2, r.sans, Paint.Align.CENTER, maxW = cw * 0.9f)
            r.text(r.n(day.dayOfMonth), cx, r.h * 0.64f, 16 * r.u * r.s1, col, r.sansBold, Paint.Align.CENTER)
        }
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Launch(calendarIntent()))
    }

    fun calliFull(r: RenderCtx) {
        val d = r.today
        val h = hijri(r)
        r.textTight(Calendars.dayName(d, true), r.w / 2, r.h * 0.38f, r.h * 0.5f * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad, maxH = r.h * 0.5f)
        val hij = "${r.n(h.day)} ${Calendars.hijriMonth(h.month, true)} ${r.n(h.year)}"
        val greg = "${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)} ${r.n(d.year)}"
        r.text("$hij  ·  $greg", r.w / 2, r.h * 0.82f, 11 * r.u * r.s2, r.c2, F.CAIRO, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }
}

/** Accent unless the user kept the default red (then a softer neutral highlight). */
fun RenderCtx.accentOr(fallback: Int): Int = if (cfg.accent == 0xFFE5484D.toInt()) withAlpha(fallback, 255) else cfg.accent
