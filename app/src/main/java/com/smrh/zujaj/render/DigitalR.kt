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

    fun clock(r: RenderCtx) {
        val size = r.h * 0.62f * r.s1
        r.liveTime(r.hm(), r.w / 2, r.h / 2, size, LiveFont.PLAYFAIR, r.c1, maxW = r.w - 2 * r.pad - 10 * r.u, sample = "00:00")
    }

    fun stacked(r: RenderCtx) {
        val size = r.h * 0.5f * r.s1
        r.liveTime(r.hh(), r.w / 2, r.h * 0.3f, size, LiveFont.SHOULDERS, r.c1, sample = "00")
        r.liveTime("mm", r.w / 2, r.h * 0.7f, size, LiveFont.SHOULDERS, r.c2, sample = "00")
    }

    fun tall(r: RenderCtx) {
        val size = r.h * 0.82f * r.s1
        r.liveTime(r.hm(), r.w / 2, r.h / 2, size, LiveFont.SHOULDERS, r.c1, maxW = r.w - 2 * r.pad, sample = "00:00")
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
        r.liveTime(r.hm(), r.w / 2, r.h * 0.42f, r.h * 0.42f * r.s1, LiveFont.PLAYFAIR, r.c1, maxW = r.w - 2 * r.pad - 20 * r.u, sample = "00:00")
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
