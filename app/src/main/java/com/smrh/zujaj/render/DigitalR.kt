package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.model.City
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

object DigitalR {
    fun cities(r: RenderCtx, n: Int): List<City> {
        val local = City(Prefs.cityName.ifBlank { r.t("Local", "محلي") }, 0.0, 0.0, TimeZone.getDefault().id)
        val defaults = listOf(local, City(r.t("Dubai", "دبي"), 25.2, 55.27, "Asia/Dubai"), City(r.t("London", "لندن"), 51.5, -0.12, "Europe/London"))
        return (r.cfg.cities + defaults.drop(r.cfg.cities.size)).take(n)
    }

    private fun zoneOk(z: String) = runCatching { ZoneId.of(z) }.isSuccess
    private fun z(c: City) = if (c.zone.isNotBlank() && zoneOk(c.zone)) c.zone else TimeZone.getDefault().id

    private fun dateLine(r: RenderCtx): String {
        val d = r.today
        val day = com.smrh.zujaj.data.Calendars.dayName(d, r.ar)
        val mon = com.smrh.zujaj.data.Calendars.monthName(d.monthValue, r.ar)
        return if (r.ar) "$day، ${r.n(d.dayOfMonth)} $mon" else "$day, ${r.n(d.dayOfMonth)} $mon"
    }

    /** Clean wide clock: big bold time, a quiet date line under it. */
    fun clock(r: RenderCtx) {
        val size = r.h * 0.56f * r.s1
        r.liveTime(r.hm(), r.w / 2, r.h * 0.44f, size, LiveFont.SANS_BOLD, r.c1, maxW = r.w - 2 * r.pad - 16 * r.u, spacing = -0.03f, sample = "00:00")
        r.text(dateLine(r), r.w / 2, r.h * 0.8f, 12 * r.u * r.s2, r.c2, r.sansMed, Paint.Align.CENTER, spacing = 0.02f, maxW = r.w - 2 * r.pad)
    }

    /** Hours over minutes in a heavy condensed face; the minutes melt into the background. */
    fun stacked(r: RenderCtx) {
        val size = r.h * 0.56f * r.s1
        val maxW = r.w - 2 * r.pad
        r.liveTime(r.hh(), r.w / 2, r.h * 0.28f, size, LiveFont.ANTON, r.c1, maxW = maxW, spacing = -0.02f, sample = "00", maxH = r.h * 0.4f * r.s1)
        val bg = r.bgColorAt(r.h)
        r.liveTime("mm", r.w / 2, r.h * 0.72f, size, LiveFont.ANTON, if (bg != null) r.c1 else r.c2, maxW = maxW, spacing = -0.02f, sample = "00", maxH = r.h * 0.4f * r.s1)
        if (bg != null) r.fade(RectF(0f, r.h * 0.46f, r.w, r.h), bg, from = 0f, until = 1.04f)
    }

    fun tall(r: RenderCtx) {
        val size = r.h * 0.82f * r.s1
        r.liveTime(r.hm(), r.w / 2, r.h / 2, size, LiveFont.SHOULDERS, r.c1, maxW = r.w - 2 * r.pad, sample = "00:00")
    }

    /** Wide: tiny date on top, huge heavy time that fades towards the bottom edge. */
    fun fadeWide(r: RenderCtx) {
        val x = r.pad + 6 * r.u
        r.text(dateLine(r).uppercase(r.locale), x, r.pad + 8 * r.u, 11 * r.u * r.s2, r.c2, r.sansBold, spacing = 0.12f, maxW = r.w * 0.7f)
        r.circle(r.w - r.pad - 10 * r.u, r.pad + 8 * r.u, 3.5f * r.u, r.accent)
        r.liveTime(r.hm(), r.w / 2, r.h * 0.6f, r.h * 0.78f * r.s1, LiveFont.ANTON, r.c1, maxW = r.w - 2 * r.pad, spacing = -0.01f, sample = "00:00", maxH = r.h * 0.56f * r.s1)
        r.bgColorAt(r.h)?.let { r.fade(RectF(0f, r.h * 0.52f, r.w, r.h), it, until = 1.08f) }
    }

    /** Small: hours big on the left column, minutes in the accent colour, day name label. */
    fun duo(r: RenderCtx) {
        val x = r.pad + 4 * r.u
        val d = r.today
        r.text(com.smrh.zujaj.data.Calendars.dayName(d, r.ar), x, r.pad + 6 * r.u, 12 * r.u * r.s2, r.c2, r.sansBold, maxW = r.w - 2 * x)
        val size = r.h * 0.46f * r.s1
        r.liveTime(r.hh(), x - 2 * r.u, r.h * 0.44f, size, LiveFont.ARCHIVO, r.c1, Paint.Align.LEFT, maxW = r.w * 0.7f, spacing = -0.04f, sample = "00", maxH = r.h * 0.28f * r.s1)
        r.liveTime("mm", r.w - x + 2 * r.u, r.h * 0.76f, size, LiveFont.ARCHIVO, r.accent, Paint.Align.RIGHT, maxW = r.w * 0.7f, spacing = -0.04f, sample = "00", maxH = r.h * 0.28f * r.s1)
    }

    /** Arabic-Indic numerals with the day written in calligraphy. */
    fun arabic(r: RenderCtx) {
        val d = r.today
        val split = r.w * 0.52f
        r.liveTime(r.hm(), split / 2 + r.pad / 2, r.h * 0.5f, r.h * 0.5f * r.s1, LiveFont.AR_AMIRI, r.c1, maxW = split - r.pad - 6 * r.u, sample = "00:00")
        r.canvas.drawLine(split, r.h * 0.26f, split, r.h * 0.74f, r.strokePaint(withAlpha(r.c1, 50), r.u))
        val right = r.w - r.pad
        val cx = (split + right) / 2
        r.textTight(com.smrh.zujaj.data.Calendars.dayName(d, true), cx, r.h * 0.42f, r.h * 0.42f * r.s2, r.c1, r.calliFont, maxW = right - split - 14 * r.u, maxH = r.h * 0.36f)
        val h = com.smrh.zujaj.data.Calendars.hijri(d)
        val sub = "${r.n(h.day)} ${com.smrh.zujaj.data.Calendars.hijriMonth(h.month, true)}"
        r.text(sub, cx, r.h * 0.76f, 12 * r.u * r.s2, r.c2, F.CAIRO_BOLD, Paint.Align.CENTER, maxW = right - split - 10 * r.u)
    }

    private fun offsetLabel(a: City, b: City): String {
        val now = ZonedDateTime.now()
        val oa = now.withZoneSameInstant(ZoneId.of(z(a))).offset.totalSeconds
        val ob = now.withZoneSameInstant(ZoneId.of(z(b))).offset.totalSeconds
        val diff = (ob - oa) / 3600f
        val s = if (diff == diff.toInt().toFloat()) diff.toInt().toString() else String.format(java.util.Locale.US, "%.1f", diff)
        return (if (diff >= 0) "+" else "") + s
    }

    fun worldBanner(r: RenderCtx) {
        val (a, b) = cities(r, 2)
        val cy = r.h / 2
        val left = r.pad + 12 * r.u
        val right = r.w - r.pad - 12 * r.u
        r.text(a.name, left, cy - 14 * r.u, 11 * r.u * r.s2, r.c2, r.sans, maxW = r.w * 0.3f)
        r.liveTime(r.hm(), left, cy + 9 * r.u, 22 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.LEFT, z(a), sample = "00:00")
        r.text(b.name, right, cy - 14 * r.u, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.3f)
        r.liveTime(r.hm(), right, cy + 9 * r.u, 22 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.RIGHT, z(b), sample = "00:00")
        val cr = 20 * r.u
        r.circle(r.w / 2, cy, cr, withAlpha(r.c1, 34))
        r.text(r.n(offsetLabel(a, b)), r.w / 2, cy, 14 * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
    }

    fun worldSplit(r: RenderCtx) {
        val (a, b) = cities(r, 2)
        val cy = r.h / 2
        r.canvas.drawLine(r.w / 2, r.h * 0.22f, r.w / 2, r.h * 0.78f, r.strokePaint(withAlpha(r.c1, 40), r.u))
        for ((i, c) in listOf(a, b).withIndex()) {
            val cx = r.w * (0.25f + 0.5f * i)
            r.text(c.name, cx, cy - 34 * r.u, 13 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w * 0.4f)
            r.liveTime(r.hm(), cx, cy + 2 * r.u, 34 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.CENTER, z(c), maxW = r.w * 0.42f, sample = "00:00")
            if (!r.cfg.hour24) r.liveTime("a", cx, cy + 32 * r.u, 11 * r.u, LiveFont.SANS, r.c2, Paint.Align.CENTER, z(c))
        }
    }

    fun worldStack(r: RenderCtx) {
        val (a, b) = cities(r, 2)
        val x = r.pad + 4 * r.u
        r.text(a.name, x, r.h * 0.17f, 10 * r.u * r.s2, r.c2, r.sans, maxW = r.w - 2 * x)
        r.liveTime(r.hm(), x, r.h * 0.34f, 30 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.LEFT, z(a), sample = "00:00")
        r.canvas.drawLine(x, r.h * 0.52f, r.w - x, r.h * 0.52f, r.strokePaint(withAlpha(r.c1, 34), r.u))
        r.text(b.name, x, r.h * 0.64f, 10 * r.u * r.s2, r.c2, r.sans, maxW = r.w - 2 * x)
        r.liveTime(r.hm(), x, r.h * 0.81f, 30 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.LEFT, z(b), sample = "00:00")
    }

    fun worldTrio(r: RenderCtx) {
        val cs = cities(r, 3)
        val colW = (r.w - 2 * r.pad) / 3
        for ((i, c) in cs.withIndex()) {
            val cx = r.pad + colW * (i + 0.5f)
            val tile = RectF(cx - colW / 2 + 4 * r.u, r.h * 0.16f, cx + colW / 2 - 4 * r.u, r.h * 0.84f)
            r.tile(tile, 18 * r.u, 16)
            r.text(c.name, cx, r.h * 0.32f, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = colW - 14 * r.u)
            r.liveTime(r.hm(), cx, r.h * 0.54f, 26 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.CENTER, z(c), maxW = colW - 16 * r.u, sample = "00:00")
            r.liveTime("EEE", cx, r.h * 0.72f, 10 * r.u, LiveFont.SANS, r.c2, Paint.Align.CENTER, z(c))
        }
    }

    fun weather(r: RenderCtx) {
        val w = WeatherRepo.main() ?: WeatherRepo.sample
        r.liveTime(r.hm(), r.w / 2, r.h * 0.42f, r.h * 0.42f * r.s1, LiveFont.DMSERIF, r.c1, maxW = r.w - 2 * r.pad - 20 * r.u, sample = "00:00")
        val d = r.today
        val date = r.n("${d.dayOfMonth.toString().padStart(2, '0')} / ${d.monthValue.toString().padStart(2, '0')} / ${d.year}")
        val y = r.h * 0.78f
        val dw = r.measure(date, 12 * r.u * r.s2)
        val temp = r.n(WeatherRepo.fmt(w.temp))
        val tw = r.measure(temp, 12 * r.u * r.s2)
        val total = dw + 34 * r.u + tw
        var x = r.w / 2 - total / 2
        r.text(date, x, y, 12 * r.u * r.s2, r.c2)
        x += dw + 17 * r.u
        Glyphs.weather(r.canvas, w.cond, w.night, x, y, 22 * r.u)
        x += 17 * r.u
        r.text(temp, x, y, 12 * r.u * r.s2, r.c2)
    }

    fun neon(r: RenderCtx) {
        r.liveTime(r.hm(), r.w / 2, r.h * 0.44f, r.h * 0.5f * r.s1, LiveFont.SHOULDERS, r.c1, maxW = r.w - 2 * r.pad, sample = "00:00")
        val d = r.today
        val label = "${com.smrh.zujaj.data.Calendars.dayName(d, r.ar)} · ${r.n(d.dayOfMonth)} ${com.smrh.zujaj.data.Calendars.monthName(d.monthValue, r.ar)}"
        r.text(label, r.w / 2, r.h * 0.8f, 12 * r.u * r.s2, withAlpha(r.c1, 200), r.sansMed, Paint.Align.CENTER, spacing = 0.08f)
    }
}
