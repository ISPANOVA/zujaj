package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.Prayer
import com.smrh.zujaj.data.PrayerDay
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.text.SimpleDateFormat
import java.util.Date

object PrayerR {
    private fun day(r: RenderCtx): PrayerDay = PrayerRepo.today(r.now.toInstant().toEpochMilli())
    private fun tm(r: RenderCtx, ms: Long) = r.n(SimpleDateFormat(if (r.cfg.hour24) "HH:mm" else "hh:mm", java.util.Locale.US).format(Date(ms)))
    private fun nm(r: RenderCtx, p: Prayer) = if (r.ar) p.ar else p.en
    private fun inWord(r: RenderCtx) = r.t("in ", "بعد ")
    private fun hl(r: RenderCtx) = withAlpha(r.c1, 34)

    /** Day progress from Fajr (0) to next Fajr (1) */
    private fun dayProgress(d: PrayerDay): Float {
        val idx = d.nextIndex.coerceIn(0, 6)
        return ((idx - 1).coerceAtLeast(0) + d.progress) / 6f
    }

    private fun icon(r: RenderCtx, key: String, cx: Float, cy: Float, s: Float, color: Int) {
        when (key) {
            "isha" -> Glyphs.moon(r.canvas, cx, cy, s, color)
            "fajr", "sunrise", "maghrib" -> {
                r.canvas.save()
                r.canvas.clipRect(cx - s, cy - s, cx + s, cy + s * 0.12f)
                Glyphs.sun(r.canvas, cx, cy + s * 0.1f, s * 0.8f, color)
                r.canvas.restore()
                r.canvas.drawLine(cx - s * 0.45f, cy + s * 0.18f, cx + s * 0.45f, cy + s * 0.18f, r.strokePaint(color, s * 0.08f))
            }
            else -> Glyphs.sun(r.canvas, cx, cy, s * (if (key == "asr") 0.75f else 0.9f), color)
        }
    }

    // ------------------------------------------------------------------ banners
    fun simple(r: RenderCtx) {
        val d = day(r)
        val s = "${nm(r, d.next)}  ${tm(r, d.next.time)}"
        r.text(s, r.w / 2, r.h * 0.38f, 17 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.liveCountdown(d.next.time, inWord(r), r.w / 2, r.h * 0.68f, 12 * r.u * r.s2, r.c2, Paint.Align.CENTER)
    }

    /** 0 = highlighted time pill, 1 = highlighted column, 2 = dots */
    fun strip(r: RenderCtx, variant: Int, area: RectF = RectF(r.pad, 0f, r.w - r.pad, r.h)) {
        val d = day(r)
        val five = if (r.ar) d.five.reversed() else d.five
        val cw = area.width() / 5
        val nextKey = d.next.key.let { if (it == "sunrise") "dhuhr" else it }
        for ((i, p) in five.withIndex()) {
            val cx = area.left + cw * (i + 0.5f)
            val isNext = p.key == nextKey
            val nameY = area.top + area.height() * (if (variant == 2) 0.5f else 0.36f)
            val timeY = area.top + area.height() * (if (variant == 2) 0.74f else 0.66f)
            if (variant == 1 && isNext) {
                r.canvas.drawRoundRect(RectF(cx - cw * 0.46f, area.top + area.height() * 0.14f, cx + cw * 0.46f, area.bottom - area.height() * 0.14f), 14 * r.u, 14 * r.u, r.fillPaint(hl(r)))
            }
            if (variant == 2) {
                r.circle(cx, area.top + area.height() * 0.26f, 3 * r.u, if (isNext) r.c1 else withAlpha(r.c1, 90))
            }
            r.text(nm(r, p), cx, nameY, 9.5f * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = cw * 0.95f)
            if (variant != 1 && isNext) {
                val tw = r.measure(tm(r, p.time), 11 * r.u * r.s1) + 12 * r.u
                r.pill(RectF(cx - tw / 2, timeY - 8 * r.u, cx + tw / 2, timeY + 8 * r.u), hl(r))
            }
            r.text(tm(r, p.time), cx, timeY, 11 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER, maxW = cw * 0.95f)
        }
    }

    fun progressStrip(r: RenderCtx) {
        strip(r, 0, RectF(r.pad, -6 * r.u, r.w - r.pad, r.h - 6 * r.u))
        r.progressBar(r.pad + 8 * r.u, r.h - r.pad * 0.7f - 4 * r.u, r.w - 2 * r.pad - 16 * r.u, dayProgress(day(r)), r.c1, withAlpha(r.c1, 40), 3 * r.u)
    }

    fun countdown(r: RenderCtx) {
        val d = day(r)
        val line = "${nm(r, d.next)}  ${tm(r, d.next.time)}"
        r.text(line, r.w / 2 - 36 * r.u, r.h * 0.28f, 12 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.liveCountdown(d.next.time, inWord(r), r.w / 2 + 22 * r.u, r.h * 0.28f, 11 * r.u * r.s2, r.c2, Paint.Align.LEFT)
        r.progressBar(r.pad + 8 * r.u, r.h * 0.52f, r.w - 2 * r.pad - 16 * r.u, d.progress, r.c1, withAlpha(r.c1, 40), 3 * r.u)
        r.text(DateR.fullDate(r), r.w / 2, r.h * 0.76f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun dual(r: RenderCtx) {
        val d = day(r)
        val x = r.pad + 8 * r.u
        val xr = r.w - r.pad - 8 * r.u
        r.text(nm(r, d.prev), x, r.h * 0.3f, 10 * r.u * r.s2, r.c2, r.sans)
        r.text(tm(r, d.prev.time), x, r.h * 0.55f, 17 * r.u * r.s1, r.c1, r.sansMed)
        r.text(r.t("Next", "القادمة"), r.w / 2, r.h * 0.3f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
        r.liveCountdown(d.next.time, "", r.w / 2, r.h * 0.55f, 17 * r.u * r.s1, r.c1, Paint.Align.CENTER, bold = true)
        r.text(nm(r, d.next), xr, r.h * 0.3f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
        r.text(tm(r, d.next.time), xr, r.h * 0.55f, 17 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.RIGHT)
        r.progressBar(x, r.h * 0.8f, xr - x, d.progress, r.c1, withAlpha(r.c1, 40), 3 * r.u)
    }

    fun banner(r: RenderCtx) {
        val d = day(r)
        val gap = 8 * r.u
        val p = r.pad * 0.6f
        val half = (r.w - 2 * p - gap) / 2
        val a = RectF(p, p, p + half, r.h - p)
        val b = RectF(p + half + gap, p, r.w - p, r.h - p)
        r.canvas.drawRoundRect(a, 16 * r.u, 16 * r.u, r.fillPaint(withAlpha(r.c1, 14)))
        r.canvas.drawRoundRect(b, 16 * r.u, 16 * r.u, r.fillPaint(withAlpha(r.c1, 50)))
        for ((rect, pr) in listOf(a to d.prev, b to d.next)) {
            r.text(nm(r, pr), rect.centerX(), rect.centerY() - 9 * r.u, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
            r.text(tm(r, pr.time), rect.centerX(), rect.centerY() + 9 * r.u, 15 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        }
    }

    // ------------------------------------------------------------------ wide
    fun pair(r: RenderCtx) {
        val d = day(r)
        val x = r.pad + 8 * r.u
        r.text(tm(r, d.next.time), x, r.h * 0.36f, 26 * r.u * r.s1, r.c1, r.sansBold)
        r.liveCountdown(d.next.time, inWord(r), x, r.h * 0.6f, 13 * r.u * r.s2, r.c2)
        r.textTight(d.next.ar, r.w * 0.68f, r.h * 0.48f, r.h * 0.5f * r.s1, r.c1, r.calliFont, maxW = r.w * 0.5f, maxH = r.h * 0.6f)
    }

    fun minimal(r: RenderCtx) {
        val d = day(r)
        val x = r.pad + 8 * r.u
        val xr = r.w - r.pad - 8 * r.u
        val rows = listOf(d.prev to r.h * 0.34f, d.next to r.h * 0.66f)
        for ((i, pair) in rows.withIndex()) {
            val (p, y) = pair
            r.circle(x + 3 * r.u, y, 3 * r.u, if (i == 1) r.c1 else withAlpha(r.c1, 110))
            r.text(nm(r, p), x + 14 * r.u, y, 13 * r.u * r.s2, if (i == 1) r.c1 else r.c2, r.sansMed)
            r.text(tm(r, p.time), xr, y, 17 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.RIGHT)
        }
        r.liveCountdown(d.next.time, inWord(r), r.w * 0.5f, r.h * 0.66f, 11 * r.u * r.s2, r.c2, Paint.Align.CENTER)
    }

    fun detail(r: RenderCtx) {
        val d = day(r)
        val x = r.pad + 6 * r.u
        val xr = r.w - r.pad - 6 * r.u
        val h = Calendars.hijri(r.today)
        r.text("${Calendars.dayName(r.today, r.ar)}, ${r.n(h.day)} ${Calendars.hijriMonth(h.month, r.ar)}", x, r.pad + 2 * r.u, 10 * r.u * r.s2, r.c2, r.sans, maxW = r.w * 0.55f)
        r.text("${r.n(r.today.dayOfMonth)} ${Calendars.monthName(r.today.monthValue, r.ar)}", x, r.pad + 20 * r.u, 15 * r.u * r.s1, r.c1, r.sansMed)
        r.text(nm(r, d.next), xr, r.pad + 2 * r.u, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
        r.liveCountdown(d.next.time, inWord(r), xr, r.pad + 20 * r.u, 13 * r.u * r.s1, r.c1, Paint.Align.RIGHT, bold = true)
        strip(r, 2, RectF(r.pad, r.h * 0.38f, r.w - r.pad, r.h * 0.86f))
        r.progressBar(x + 6 * r.u, r.h - r.pad * 0.7f, xr - x - 12 * r.u, dayProgress(d), r.c1, withAlpha(r.c1, 40), 3 * r.u)
    }

    // ------------------------------------------------------------------ small
    fun ring(r: RenderCtx, calli: Boolean) {
        val d = day(r)
        val cx = r.w / 2; val cy = r.h / 2
        val rad = minOf(r.w, r.h) / 2 - r.pad - 4 * r.u
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 40), 7 * r.u))
        r.canvas.drawArc(rect, -90f, 360f * d.progress, false, r.strokePaint(r.c1, 7 * r.u))
        if (calli) {
            r.text(tm(r, d.next.time), cx, cy - rad * 0.5f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
            r.textTight(d.next.ar, cx, cy, rad * 0.62f * r.s1, r.c1, r.calliFont, maxW = rad * 1.4f, maxH = rad * 0.7f)
        } else {
            r.text(nm(r, d.next), cx, cy - rad * 0.42f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
            r.text(tm(r, d.next.time), cx, cy, 26 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        }
        r.liveCountdown(d.next.time, "", cx, cy + rad * 0.45f, 10 * r.u * r.s2, r.c2, Paint.Align.CENTER)
    }

    // ------------------------------------------------------------------ large
    fun table(r: RenderCtx) {
        val d = day(r)
        val w = WeatherRepo.main() ?: WeatherRepo.sample
        val x = r.pad + 6 * r.u
        Glyphs.weather(r.canvas, w.cond, w.night, x + 12 * r.u, r.pad + 14 * r.u, 26 * r.u)
        r.text(r.n(WeatherRepo.fmt(w.temp)), x + 30 * r.u, r.pad + 14 * r.u, 14 * r.u, r.c1, r.sansMed)
        r.text(tm(r, d.next.time), x, r.pad + 46 * r.u, 22 * r.u * r.s1, r.c1, r.sansBold)
        r.textTight(Calendars.dayName(r.today, true), r.w * 0.68f, r.pad + 34 * r.u, 60 * r.u * r.s1, r.c1, r.calliFont, maxW = r.w * 0.5f, maxH = 64 * r.u)
        strip(r, 0, RectF(r.pad, r.h * 0.5f, r.w - r.pad, r.h * 0.86f))
        r.progressBar(x + 6 * r.u, r.h - r.pad * 0.8f, r.w - 2 * x - 12 * r.u, dayProgress(d), r.c1, withAlpha(r.c1, 40), 3 * r.u)
    }

    fun next(r: RenderCtx) {
        val d = day(r)
        val x = r.pad + 6 * r.u
        r.text(tm(r, d.next.time), x, r.pad + 14 * r.u, 22 * r.u * r.s1, r.c1, r.sansBold)
        r.text(nm(r, d.next), x, r.pad + 36 * r.u, 11 * r.u * r.s2, r.c2, r.sans)
        r.liveCountdown(d.next.time, "", x, r.pad + 54 * r.u, 15 * r.u * r.s1, r.c1)
        r.textTight(d.next.ar, r.w * 0.68f, r.pad + 36 * r.u, 70 * r.u * r.s1, r.c1, r.calliFont, maxW = r.w * 0.5f, maxH = 76 * r.u)
        strip(r, 0, RectF(r.pad, r.h * 0.5f, r.w - r.pad, r.h * 0.86f))
        r.progressBar(x + 6 * r.u, r.h - r.pad * 0.8f, r.w - 2 * x - 12 * r.u, dayProgress(d), r.c1, withAlpha(r.c1, 40), 3 * r.u)
    }

    fun dayGrid(r: RenderCtx) {
        val d = day(r)
        val h = Calendars.hijri(r.today)
        val t = r.today
        r.text(r.n("${h.day.toString().padStart(2, '0')}.${h.month.toString().padStart(2, '0')}.${h.year}  |  ${t.dayOfMonth.toString().padStart(2, '0')}.${t.monthValue.toString().padStart(2, '0')}.${t.year}"),
            r.w / 2, r.pad + 6 * r.u, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        val name = Calendars.dayName(t, r.ar)
        if (r.ar) r.textTight(name, r.w / 2, r.h * 0.3f, 56 * r.u * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad, maxH = r.h * 0.3f)
        else r.textTight(name.uppercase(), r.w / 2, r.h * 0.3f, 46 * r.u * r.s1, r.c1, F.BODONI_REG, spacing = 0.3f, maxW = r.w - 2 * r.pad - 10 * r.u)
        strip(r, 2, RectF(r.pad, r.h * 0.48f, r.w - r.pad, r.h * 0.86f))
        r.progressBar(r.pad + 12 * r.u, r.h - r.pad * 0.8f, r.w - 2 * r.pad - 24 * r.u, dayProgress(d), r.c1, withAlpha(r.c1, 40), 3 * r.u)
    }

    fun list(r: RenderCtx) {
        val d = day(r)
        val p = r.pad * 0.8f
        val card = RectF(p, p, r.w - p, p + r.h * 0.26f)
        r.canvas.drawRoundRect(card, 18 * r.u, 18 * r.u, r.fillPaint(withAlpha(r.c1, 40)))
        r.text(nm(r, d.next), card.left + 14 * r.u, card.top + card.height() * 0.32f, 11 * r.u * r.s2, r.c2, r.sans)
        r.liveCountdown(d.next.time, inWord(r), card.left + 14 * r.u, card.top + card.height() * 0.68f, 14 * r.u * r.s1, r.c1)
        r.text(tm(r, d.next.time), card.right - 14 * r.u, card.centerY(), 26 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.RIGHT)
        val top = card.bottom + 6 * r.u
        val rowH = (r.h - p - top) / 6
        for ((i, pr) in d.list.withIndex()) {
            val cy = top + rowH * (i + 0.5f)
            if (pr.key == d.next.key && pr.time == d.next.time) {
                r.canvas.drawRoundRect(RectF(p, cy - rowH * 0.46f, r.w - p, cy + rowH * 0.46f), 12 * r.u, 12 * r.u, r.fillPaint(withAlpha(r.c1, 26)))
            }
            if (r.ar) {
                icon(r, pr.key, r.w - p - 16 * r.u, cy, 14 * r.u, r.c2)
                r.text(nm(r, pr), r.w - p - 32 * r.u, cy, 11 * r.u * r.s2, r.c1, r.sans, Paint.Align.RIGHT)
                r.text(tm(r, pr.time), p + 12 * r.u, cy, 11 * r.u * r.s2, r.c1, r.sans)
            } else {
                icon(r, pr.key, p + 16 * r.u, cy, 14 * r.u, r.c2)
                r.text(nm(r, pr), p + 32 * r.u, cy, 11 * r.u * r.s2, r.c1, r.sans)
                r.text(tm(r, pr.time), r.w - p - 12 * r.u, cy, 11 * r.u * r.s2, r.c1, r.sans, Paint.Align.RIGHT)
            }
        }
    }

    fun tiles(r: RenderCtx) {
        val d = day(r)
        val p = r.pad * 0.7f
        val g = 8 * r.u
        val cw = (r.w - 2 * p - g) / 2
        val ch = (r.h - 2 * p - 2 * g) / 3
        for ((i, pr) in d.list.withIndex()) {
            val col = i % 2; val row = i / 2
            val rect = RectF(p + col * (cw + g), p + row * (ch + g), p + col * (cw + g) + cw, p + row * (ch + g) + ch)
            val isNext = pr.key == d.next.key && pr.time == d.next.time
            r.canvas.drawRoundRect(rect, 16 * r.u, 16 * r.u, r.fillPaint(withAlpha(r.c1, if (isNext) 70 else 18)))
            icon(r, pr.key, rect.centerX(), rect.top + ch * 0.24f, 14 * r.u, r.c1)
            r.text(nm(r, pr), rect.centerX(), rect.top + ch * 0.52f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
            r.text(tm(r, pr.time), rect.centerX(), rect.top + ch * 0.78f, 14 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        }
    }

    fun spotlight(r: RenderCtx) {
        val d = day(r)
        r.textTight(d.next.ar, r.w / 2, r.h * 0.42f, r.h * 0.62f * r.s1, r.c1, r.calliFont, maxW = r.w - 2 * r.pad - 10 * r.u, maxH = r.h * 0.6f)
        val line = tm(r, d.next.time)
        r.text(line, r.w / 2 - 6 * r.u, r.h * 0.84f, 13 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.RIGHT)
        r.liveCountdown(d.next.time, inWord(r), r.w / 2 + 6 * r.u, r.h * 0.84f, 12 * r.u * r.s2, r.c2, Paint.Align.LEFT)
    }
}
