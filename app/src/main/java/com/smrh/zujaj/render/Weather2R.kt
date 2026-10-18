package com.smrh.zujaj.render

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.DayF
import com.smrh.zujaj.data.HourF
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

/** Forecast designs (hourly & daily). */
object Weather2R {
    private fun hours(r: RenderCtx, n: Int): List<HourF> {
        val now = System.currentTimeMillis() - 3_600_000
        return Kit.weather(r).hours.filter { it.time >= now }.take(n).ifEmpty { WeatherRepo.sample.hours.take(n) }
    }
    private fun days(r: RenderCtx, n: Int): List<DayF> {
        val today = LocalDate.now().toEpochDay()
        return Kit.weather(r).days.filter { it.epochDay >= today }.take(n).ifEmpty { WeatherRepo.sample.days.take(n) }
    }
    private fun hourLabel(r: RenderCtx, t: Long, first: Boolean) =
        if (first) r.t("Now", "الآن") else r.n(SimpleDateFormat(if (r.cfg.hour24) "HH" else "h a", Locale.US).format(Date(t)).lowercase())
    private fun dayLabel(r: RenderCtx, d: DayF, first: Boolean) =
        if (first) r.t("Today", "اليوم") else Kit.weekdayShort(r, LocalDate.ofEpochDay(d.epochDay).dayOfWeek.value)
    private fun icon(r: RenderCtx, cond: com.smrh.zujaj.data.Cond, night: Boolean, cx: Float, cy: Float, s: Float) =
        Glyphs.weather(r.canvas, cond, night, cx, cy, s)
    private fun t(r: RenderCtx, v: Double) = Kit.temp(r, v)

    /** Smooth line through points, with a soft fill under it. */
    private fun curve(r: RenderCtx, pts: List<Pair<Float, Float>>, color: Int, fillTo: Float? = null) {
        if (pts.size < 2) return
        val p = Path(); p.moveTo(pts[0].first, pts[0].second)
        for (i in 1 until pts.size) {
            val (x0, y0) = pts[i - 1]; val (x1, y1) = pts[i]
            val mx = (x0 + x1) / 2
            p.cubicTo(mx, y0, mx, y1, x1, y1)
        }
        if (fillTo != null) {
            val f = Path(p); f.lineTo(pts.last().first, fillTo); f.lineTo(pts[0].first, fillTo); f.close()
            val fp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, pts.minOf { it.second }, 0f, fillTo, withAlpha(color, 90), withAlpha(color, 0), Shader.TileMode.CLAMP) }
            r.canvas.drawPath(f, fp)
        }
        r.canvas.drawPath(p, r.strokePaint(color, 2 * r.u))
    }

    // Today & tomorrow — two halves on a sky-blue card (W)
    fun todayTomorrow(r: RenderCtx) {
        val ds = days(r, 2)
        for ((i, d) in ds.withIndex()) {
            val col = if (r.ar) 1 - i else i
            val x0 = r.pad + (r.w - 2 * r.pad) / 2 * col
            val cx = x0 + (r.w - 2 * r.pad) / 4
            icon(r, d.cond, false, cx - 30 * r.u, r.h * 0.3f, 22 * r.u)
            r.text(dayLabel(r, d, i == 0), cx + 30 * r.u, r.h * 0.3f, 10 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
            r.text("${t(r, d.hi)} / ${t(r, d.lo)}", cx, r.h * 0.62f, 18 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
            r.text(WeatherRepo.condName(d.cond, r.ar), cx, r.h * 0.82f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        }
        Kit.line(r, r.w / 2, r.h * 0.2f, r.w / 2, r.h * 0.85f, withAlpha(r.c1, 50), 0.8f * r.u)
    }

    // Next hours — icons with temperature, small labels (B / W)
    fun nextHours(r: RenderCtx) {
        val hs = Kit.ordered(r, hours(r, 6))
        val cw = (r.w - 2 * r.pad) / hs.size
        val first = hours(r, 1).firstOrNull()
        for ((i, h) in hs.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.text(hourLabel(r, h.time, h == first), cx, r.h * 0.2f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            icon(r, h.cond, isNight(h.time), cx, r.h * 0.5f, 18 * r.u)
            r.text(t(r, h.temp), cx, r.h * 0.8f, 11 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        }
    }

    private fun isNight(t: Long): Boolean {
        val h = java.time.Instant.ofEpochMilli(t).atZone(java.time.ZoneId.systemDefault()).hour
        return h < 6 || h >= 19
    }

    // Temperature curve — next hours as a smooth line (S / W)
    fun tempCurve(r: RenderCtx) {
        val hs = hours(r, 8)
        val l = r.pad; val w = r.w - 2 * r.pad
        val top = r.h * 0.3f; val bot = r.h * 0.55f
        val mn = hs.minOf { it.temp }; val mx = hs.maxOf { it.temp }.coerceAtLeast(mn + 1)
        val pts = hs.mapIndexed { i, h ->
            val x = l + w * (i + 0.5f) / hs.size
            (if (r.ar) r.w - x else x) to (bot - (h.temp - mn).toFloat() / (mx - mn).toFloat() * (bot - top))
        }
        curve(r, pts, r.accent, r.h * 0.62f)
        for ((i, h) in hs.withIndex()) {
            val (x, y) = pts[i]
            r.circle(x, y, 2 * r.u, r.accent)
            r.text(r.n("${h.temp.toInt()}"), x, y - 9 * r.u, 8 * r.u, r.c1, r.sans, Paint.Align.CENTER)
            icon(r, h.cond, isNight(h.time), x, r.h * 0.74f, 10 * r.u)
            r.text(hourLabel(r, h.time, i == 0), x, r.h * 0.9f, 7 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = w / hs.size)
        }
    }

    // Rain this week — rows with rain bars (S / W)
    fun rainWeek(r: RenderCtx) {
        val ds = days(r, 5)
        val rowH = (r.h - 2 * r.pad * 0.7f) / ds.size
        val mx = ds.maxOf { it.rain }.coerceAtLeast(1.0)
        for ((i, d) in ds.withIndex()) {
            val y = r.pad * 0.7f + rowH * (i + 0.5f)
            r.text(dayLabel(r, d, i == 0), r.w - r.pad, y, 9 * r.u, r.c1, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.22f)
            r.text(r.n("${(d.rain * 10).toInt() / 10.0}"), r.w * 0.66f, y, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
            Kit.slider(r, r.w * 0.24f, y, r.w * 0.3f, (d.rain / mx).toFloat().coerceAtLeast(0.04f), r.accent, withAlpha(r.c1, 26), 3 * r.u, 0f)
            r.text("${t(r, d.hi)}", r.pad, y, 9 * r.u, r.c1, r.sansMed)
        }
    }

    // Week columns — thermometer bars per day (W)
    fun weekColumns(r: RenderCtx) {
        val ds = Kit.ordered(r, days(r, 7))
        val cw = (r.w - 2 * r.pad) / ds.size
        val mn = ds.minOf { it.lo }; val mx = ds.maxOf { it.hi }.coerceAtLeast(mn + 1)
        val top = r.h * 0.22f; val bot = r.h * 0.66f
        for ((i, d) in ds.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.text(t(r, d.hi), cx, r.h * 0.1f, 8 * r.u, r.c1, r.sans, Paint.Align.CENTER)
            val y1 = bot - ((d.hi - mn) / (mx - mn)).toFloat() * (bot - top)
            val y0 = bot - ((d.lo - mn) / (mx - mn)).toFloat() * (bot - top)
            r.canvas.drawLine(cx, top, cx, bot, r.strokePaint(withAlpha(r.c1, 24), 4 * r.u))
            val p = r.strokePaint(r.accent, 4 * r.u)
            p.shader = LinearGradient(0f, y1, 0f, y0, r.accent, r.c2, Shader.TileMode.CLAMP)
            r.canvas.drawLine(cx, y1, cx, y0, p)
            icon(r, d.cond, false, cx, r.h * 0.78f, 9 * r.u)
            r.text(dayLabel(r, d, false).take(3), cx, r.h * 0.92f, 7 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
        }
    }

    // Week at a glance — day, icon, high (W / B)
    fun weekGlance(r: RenderCtx) {
        val ds = Kit.ordered(r, days(r, 7))
        val cw = (r.w - 2 * r.pad) / ds.size
        for ((i, d) in ds.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.text(dayLabel(r, d, false).take(3), cx, r.h * 0.22f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            icon(r, d.cond, false, cx, r.h * 0.5f, 15 * r.u)
            r.text(t(r, d.hi), cx, r.h * 0.8f, 10 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        }
    }

    // Five days — day, icon, high/low (S / W)
    fun fiveDays(r: RenderCtx) {
        val ds = Kit.ordered(r, days(r, 5))
        val cw = (r.w - 2 * r.pad) / ds.size
        for ((i, d) in ds.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.text(dayLabel(r, d, false).take(3), cx, r.h * 0.18f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            icon(r, d.cond, false, cx, r.h * 0.45f, 15 * r.u)
            r.text(t(r, d.hi), cx, r.h * 0.7f, 10 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
            r.text(t(r, d.lo), cx, r.h * 0.86f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        }
    }

    // Hourly curve — now on the left, a temperature line through the next hours (W)
    fun hourlyCurve(r: RenderCtx) {
        val w0 = Kit.weather(r)
        icon(r, w0.cond, w0.night, r.pad + 14 * r.u, r.h * 0.2f, 18 * r.u)
        r.text(t(r, w0.temp), r.pad + 28 * r.u, r.h * 0.2f, 18 * r.u * r.s1, r.c1, r.sansBold)
        val hs = hours(r, 6)
        val l = r.pad; val w = r.w - 2 * r.pad
        val mn = hs.minOf { it.temp }; val mx = hs.maxOf { it.temp }.coerceAtLeast(mn + 1)
        val pts = hs.mapIndexed { i, h -> (l + w * (i + 0.5f) / hs.size) to (r.h * 0.62f - ((h.temp - mn) / (mx - mn)).toFloat() * r.h * 0.12f) }
        curve(r, pts, r.c1)
        for ((i, h) in hs.withIndex()) {
            val (x, y) = pts[i]
            r.text(t(r, h.temp), x, r.h * 0.42f, 8 * r.u, r.c1, r.sans, Paint.Align.CENTER)
            r.circle(x, y, 2.4f * r.u, r.c1)
            icon(r, h.cond, isNight(h.time), x, r.h * 0.76f, 8 * r.u)
            r.text(hourLabel(r, h.time, i == 0), x, r.h * 0.9f, 7 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = w / hs.size)
        }
    }

    // Hours & days — two rows: next hours on top, next days below (L / W)
    fun hoursDays(r: RenderCtx) {
        val hs = Kit.ordered(r, hours(r, 5)); val ds = Kit.ordered(r, days(r, 5))
        val cw = (r.w - 2 * r.pad) / 5
        val first = hours(r, 1).firstOrNull()
        for ((i, h) in hs.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.text(hourLabel(r, h.time, h == first), cx, r.h * 0.1f, 7.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            icon(r, h.cond, isNight(h.time), cx, r.h * 0.24f, 10 * r.u)
            r.text(t(r, h.temp), cx, r.h * 0.38f, 9.5f * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        }
        Kit.line(r, r.pad, r.h * 0.5f, r.w - r.pad, r.h * 0.5f, withAlpha(r.c1, 26), 0.7f * r.u)
        for ((i, d) in ds.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.text(dayLabel(r, d, false).take(3), cx, r.h * 0.6f, 7.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            icon(r, d.cond, false, cx, r.h * 0.74f, 10 * r.u)
            r.text(t(r, d.hi), cx, r.h * 0.88f, 9.5f * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        }
    }

    // Now & days ahead — current big on the left, list of days on the right (W)
    fun nowAhead(r: RenderCtx) {
        val w0 = Kit.weather(r)
        icon(r, w0.cond, w0.night, r.pad + 20 * r.u, r.h * 0.26f, 26 * r.u)
        r.text(t(r, w0.temp), r.pad + 4 * r.u, r.h * 0.58f, 24 * r.u * r.s1, r.c1, r.sansBold)
        r.text(WeatherRepo.condName(w0.cond, r.ar), r.pad + 4 * r.u, r.h * 0.74f, 8.5f * r.u, r.c2, r.sans)
        r.text(w0.city.ifBlank { com.smrh.zujaj.data.Prefs.cityName }, r.pad + 4 * r.u, r.h * 0.86f, 8.5f * r.u, r.c1, r.sansBold, maxW = r.w * 0.35f)
        val ds = days(r, 5).drop(1).take(4)
        val l = r.w * 0.42f
        val rowH = (r.h - 2 * r.pad * 0.7f) / ds.size
        for ((i, d) in ds.withIndex()) {
            val y = r.pad * 0.7f + rowH * (i + 0.5f)
            r.text(dayLabel(r, d, false), l, y, 9 * r.u, r.c2, r.sans, maxW = r.w * 0.16f)
            icon(r, d.cond, false, l + (r.w - l) * 0.42f, y, 9 * r.u)
            r.text(t(r, d.hi), r.w - r.pad - 28 * r.u, y, 9.5f * r.u * r.s2, r.c1, r.sansMed, Paint.Align.RIGHT)
            r.text(t(r, d.lo), r.w - r.pad, y, 9.5f * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
        }
    }
}
