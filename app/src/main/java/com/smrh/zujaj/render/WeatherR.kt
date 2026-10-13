package com.smrh.zujaj.render

import android.graphics.Paint
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.data.Weather
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.text.SimpleDateFormat
import java.util.Date

object WeatherR {
    fun main(r: RenderCtx): Weather = WeatherRepo.main() ?: WeatherRepo.sample.copy(city = Prefs.cityName.ifBlank { WeatherRepo.sample.city })

    fun second(r: RenderCtx): Weather {
        val c = r.cfg.cities.firstOrNull() ?: return WeatherRepo.sample2.copy(city = r.t("Dubai", "دبي"))
        return WeatherRepo.cached(c.lat, c.lng) ?: WeatherRepo.sample2.copy(city = c.name, symbol = "cloudy")
    }

    private fun city(r: RenderCtx, w: Weather) = w.city.ifBlank { r.t("My location", "موقعي") }
    private fun temp(r: RenderCtx, t: Double) = r.n(WeatherRepo.fmt(t))

    fun compactBanner(r: RenderCtx) {
        val w = main(r)
        val x = r.pad + 8 * r.u
        Glyphs.weather(r.canvas, w.cond, w.night, x + 16 * r.u, r.h / 2, 34 * r.u)
        r.text(city(r, w), x + 42 * r.u, r.h / 2 - 9 * r.u, 13 * r.u * r.s2, r.c1, r.sansMed, maxW = r.w * 0.45f)
        r.text(WeatherRepo.condName(w.cond, r.ar), x + 42 * r.u, r.h / 2 + 10 * r.u, 11 * r.u * r.s2, r.c2, r.sans, maxW = r.w * 0.45f)
        r.text(temp(r, w.temp), r.w - r.pad - 8 * r.u, r.h / 2, 26 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.RIGHT)
    }

    fun dual(r: RenderCtx) {
        val a = main(r); val b = second(r)
        r.canvas.drawLine(r.w / 2, r.h * 0.25f, r.w / 2, r.h * 0.75f, r.strokePaint(withAlpha(r.c1, 40), r.u))
        for ((i, w) in listOf(a, b).withIndex()) {
            val cx = r.w * (0.25f + 0.5f * i)
            r.text(city(r, w), cx, r.h * 0.33f, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w * 0.4f)
            r.text(temp(r, w.temp), cx, r.h * 0.64f, 22 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        }
    }

    fun compactSmall(r: RenderCtx) {
        val w = main(r)
        val x = r.pad + 4 * r.u
        r.text(city(r, w), x, r.pad + 10 * r.u, 12 * r.u * r.s2, r.c1, r.sansMed, maxW = r.w * 0.55f)
        r.text(WeatherRepo.condName(w.cond, r.ar), x, r.pad + 27 * r.u, 10 * r.u * r.s2, r.c2, r.sans, maxW = r.w * 0.55f)
        Glyphs.weather(r.canvas, w.cond, w.night, r.w - r.pad - 18 * r.u, r.pad + 18 * r.u, 34 * r.u)
        r.text(temp(r, w.temp), x - 2 * r.u, r.h * 0.68f, 40 * r.u * r.s1, r.c1, r.sansMed)
        r.text(r.n("H:${WeatherRepo.fmt(w.hi)} L:${WeatherRepo.fmt(w.lo)}"), x, r.h - r.pad - 4 * r.u, 10 * r.u * r.s2, r.c2, r.sans)
    }

    fun simple(r: RenderCtx) {
        val w = main(r)
        Glyphs.weather(r.canvas, w.cond, w.night, r.w / 2, r.h * 0.32f, 48 * r.u)
        r.text(temp(r, w.temp), r.w / 2, r.h * 0.62f, 30 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        r.text(WeatherRepo.condName(w.cond, r.ar), r.w / 2, r.h * 0.8f, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
    }

    fun detail(r: RenderCtx) {
        val w = main(r)
        val x = r.pad + 6 * r.u
        r.text(city(r, w), x, r.pad + 8 * r.u, 12 * r.u * r.s2, r.c2, r.sans)
        r.text(temp(r, w.temp), x - 2 * r.u, r.pad + 40 * r.u, 40 * r.u * r.s1, r.c1, r.sansMed)
        Glyphs.weather(r.canvas, w.cond, w.night, r.w - r.pad - 28 * r.u, r.pad + 26 * r.u, 52 * r.u)
        r.text(r.n("H:${WeatherRepo.fmt(w.hi)}  L:${WeatherRepo.fmt(w.lo)}"), x, r.h - r.pad - 22 * r.u, 11 * r.u * r.s2, r.c1, r.sans)
        val mins = ((System.currentTimeMillis() - w.updated) / 60000).coerceAtLeast(0)
        r.text(r.t("Updated ${r.n(mins.toInt())}m ago", "تحديث منذ ${r.n(mins.toInt())} د"), x, r.h - r.pad - 6 * r.u, 9 * r.u, r.c2, r.sans)
        r.liveTime(r.hm(), r.w - r.pad - 6 * r.u, r.h - r.pad - 22 * r.u, 18 * r.u * r.s1, LiveFont.SANS, r.c1, Paint.Align.RIGHT, sample = "00:00")
        val d = r.today
        r.text("${Calendars.dayName(d, r.ar).take(if (r.ar) 10 else 3)}, ${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, r.ar).take(if (r.ar) 10 else 3)} ${r.n(d.year)}",
            r.w - r.pad - 6 * r.u, r.h - r.pad - 6 * r.u, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    fun split(r: RenderCtx) {
        val a = main(r); val b = second(r)
        for ((i, w) in listOf(a, b).withIndex()) {
            val cx = r.w * (0.25f + 0.5f * i)
            r.text(city(r, w), cx, r.h * 0.22f, 12 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w * 0.42f)
            r.text(temp(r, w.temp), cx, r.h * 0.5f, 34 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
            r.text(WeatherRepo.condName(w.cond, r.ar), cx, r.h * 0.76f, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
        }
        r.canvas.drawLine(r.w / 2, r.h * 0.2f, r.w / 2, r.h * 0.8f, r.strokePaint(withAlpha(r.c1, 40), r.u))
    }

    fun big(r: RenderCtx) {
        val w = main(r)
        Glyphs.weather(r.canvas, w.cond, w.night, r.w * 0.24f, r.h / 2, 92 * r.u)
        r.text(temp(r, w.temp), r.w * 0.5f, r.h * 0.42f, 56 * r.u * r.s1, r.c1, F.SANS_MED)
        r.text("${city(r, w)} · ${WeatherRepo.condName(w.cond, r.ar)}", r.w * 0.5f, r.h * 0.72f, 12 * r.u * r.s2, withAlpha(r.c1, 210), r.sans, maxW = r.w * 0.46f)
        val upd = SimpleDateFormat("HH:mm", r.locale).format(Date(w.updated))
        r.text(r.n("H ${WeatherRepo.fmt(w.hi)} · L ${WeatherRepo.fmt(w.lo)} · $upd"), r.w * 0.5f, r.h * 0.86f, 9 * r.u, r.c2, r.sans, maxW = r.w * 0.46f)
    }
}
