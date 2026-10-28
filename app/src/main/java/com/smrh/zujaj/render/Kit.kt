package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.Prayer
import com.smrh.zujaj.data.PrayerDay
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.data.SystemRepo
import com.smrh.zujaj.data.Weather
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Shared helpers for the newer widget families. */
object Kit {
    fun day(r: RenderCtx): PrayerDay = PrayerRepo.today(r.now.toInstant().toEpochMilli())
    fun pName(r: RenderCtx, p: Prayer) = if (r.ar) p.ar else p.en
    fun pTime(r: RenderCtx, ms: Long, lead: Boolean = true) =
        r.n(SimpleDateFormat(if (r.cfg.hour24) (if (lead) "HH:mm" else "H:mm") else (if (lead) "hh:mm" else "h:mm"), Locale.US).format(Date(ms)))
    /** Next prayer, treating sunrise as "not a prayer" when asked. */
    fun next(r: RenderCtx, skipSunrise: Boolean = false): Prayer {
        val d = day(r)
        if (!skipSunrise || d.next.key != "sunrise") return d.next
        return d.list.firstOrNull { it.key == "dhuhr" } ?: d.next
    }
    fun progress(r: RenderCtx) = day(r).progress

    fun dayName(r: RenderCtx, ar: Boolean = r.ar) = Calendars.dayName(r.today, ar)
    fun hijri(r: RenderCtx): String {
        val h = Calendars.hijri(r.today)
        return if (r.ar) "${r.n(h.day)} ${Calendars.hijriMonth(h.month, true)} ${r.n(h.year)}" else "${h.day} ${Calendars.hijriMonth(h.month, false)} ${h.year}"
    }
    fun hijriShort(r: RenderCtx): String {
        val h = Calendars.hijri(r.today)
        return if (r.ar) "${r.n(h.day)} ${Calendars.hijriMonth(h.month, true)}" else "${h.day} ${Calendars.hijriMonth(h.month, false).take(3)}"
    }
    fun greg(r: RenderCtx): String {
        val d = r.today
        return if (r.ar) "${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)} ${r.n(d.year)}" else "${d.dayOfMonth} ${Calendars.monthName(d.monthValue, false)} ${d.year}"
    }
    fun gregShort(r: RenderCtx): String {
        val d = r.today
        return if (r.ar) "${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)}" else "${d.dayOfMonth} ${Calendars.monthName(d.monthValue, false).take(3)}"
    }

    fun battery(r: RenderCtx): Int = if (r.preview) 85 else runCatching { SystemRepo.read(r.context).battery }.getOrDefault(0)

    fun weather(r: RenderCtx): Weather {
        val s = WeatherRepo.sample
        val w = WeatherRepo.main() ?: return s.copy(city = Prefs.cityName.ifBlank { r.t("Cairo", "القاهرة") })
        return if (w.days.isEmpty() || w.hours.isEmpty()) w.copy(hours = if (w.hours.isEmpty()) s.hours else w.hours, days = if (w.days.isEmpty()) s.days else w.days) else w
    }
    fun temp(r: RenderCtx, t: Double) = r.n(WeatherRepo.fmt(t))

    /** "in 2:15" for the next prayer, as a live countdown. */
    fun countdown(r: RenderCtx, x: Float, cy: Float, size: Float, color: Int, align: Paint.Align = Paint.Align.LEFT, prefix: String = r.t("in ", "باقي "), bold: Boolean = false) =
        r.liveCountdown(day(r).next.time, prefix, x, cy, size, color, align, bold)

    /** The main time as a live clock. */
    fun clock(r: RenderCtx, cx: Float, cy: Float, size: Float, font: LiveFont = LiveFont.SANS_BOLD, color: Int = r.c1,
              align: Paint.Align = Paint.Align.CENTER, maxW: Float = 0f, maxH: Float = 0f, pattern: String = r.hm()) =
        r.liveTime(pattern, cx, cy, size, font, color, align, maxW = maxW, maxH = maxH, sample = if (pattern.length <= 2) "00" else null)

    /** Time drawn into the picture (for gradients/fades). Style must set bakeTime = { true }. */
    fun bakedTime(r: RenderCtx, pattern: String = r.hm()): String =
        r.n(SimpleDateFormat(pattern, Locale.US).format(Date(r.now.toInstant().toEpochMilli())))

    /**
     * The main time of a gradient design: with "gradient time" on it is drawn into the picture with the
     * gradient (redrawn every minute); otherwise the phone draws it live in [c1] — always exact.
     */
    fun timeText(r: RenderCtx, cx: Float, cy: Float, size: Float, live: LiveFont, c1: Int, c2: Int,
                 maxW: Float = 0f, maxH: Float = 0f, pattern: String = r.hm(), font: F? = null) {
        if (r.cfg.gradTime) gradText(r, bakedTime(r, pattern), cx, cy, size, font ?: r.resolveFont(live, 60 * r.u).f, c1, c2, maxW = maxW, maxH = maxH)
        else r.liveTime(pattern, cx, cy, size, live, c1, Paint.Align.CENTER, maxW = maxW, maxH = maxH, sample = pattern.replace(Regex("[a-zA-Z]"), "0"))
    }

    /** Draws [s] with a vertical/horizontal gradient fill, centred on (cx, cy) by ink. Returns ink bounds. */
    fun gradText(r: RenderCtx, s: String, cx: Float, cy: Float, size: Float, font: F, c1: Int, c2: Int,
                 vertical: Boolean = true, maxW: Float = 0f, maxH: Float = 0f, align: Paint.Align = Paint.Align.CENTER): RectF {
        val p = r.paint(c1, size, font, Paint.Align.LEFT)
        r.fitPaint(p, s, maxW, maxH)
        val b = android.graphics.Rect(); p.getTextBounds(s, 0, s.length, b)
        val w = p.measureText(s)
        val x = when (align) { Paint.Align.LEFT -> cx; Paint.Align.RIGHT -> cx - w; else -> cx - w / 2 }
        val y = cy - b.exactCenterY()
        val ink = RectF(x + b.left, y + b.top, x + b.right, y + b.bottom)
        p.shader = if (vertical) LinearGradient(0f, ink.top, 0f, ink.bottom, c1, c2, Shader.TileMode.CLAMP)
        else LinearGradient(ink.left, 0f, ink.right, 0f, c1, c2, Shader.TileMode.CLAMP)
        r.canvas.drawText(s, x, y, p)
        return ink
    }

    /** Arabic display word in the user's calligraphy, optionally faded with a gradient. */
    fun calli(r: RenderCtx, s: String, cx: Float, cy: Float, size: Float, color: Int, maxW: Float, maxH: Float, color2: Int? = null) {
        if (color2 == null) { r.textTight(s, cx, cy, size, color, r.calliFont, maxW = maxW, maxH = maxH); return }
        val box = RectF(cx - maxW / 2 - 4 * r.u, cy - maxH, cx + maxW / 2 + 4 * r.u, cy + maxH)
        r.canvas.saveLayer(box, null)
        r.textTight(s, cx, cy, size, Color.WHITE, r.calliFont, maxW = maxW, maxH = maxH)
        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(box.left, box.top, box.right, box.bottom, color, color2, Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        r.canvas.drawRect(box, gp)
        r.canvas.restore()
    }

    fun line(r: RenderCtx, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, w: Float = r.u) =
        r.canvas.drawLine(x1, y1, x2, y2, r.strokePaint(color, w))

    /** Thin track with a filled part and a knob. */
    fun slider(r: RenderCtx, x: Float, y: Float, w: Float, frac: Float, color: Int, track: Int = withAlpha(r.c1, 34), thick: Float = 3 * r.u, knob: Float = 5 * r.u) {
        r.canvas.drawLine(x, y, x + w, y, r.strokePaint(track, thick))
        val f = frac.coerceIn(0f, 1f)
        if (f > 0f) r.canvas.drawLine(x, y, x + w * f, y, r.strokePaint(color, thick))
        if (knob > 0) { r.circle(x + w * f, y, knob, color); r.circle(x + w * f, y, knob * 0.45f, withAlpha(Color.BLACK, 90)) }
    }

    fun ring(r: RenderCtx, cx: Float, cy: Float, rad: Float, frac: Float, color: Int, track: Int = withAlpha(r.c1, 30), w: Float = 6 * r.u, start: Float = -90f) {
        val o = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(o, 0f, 360f, false, r.strokePaint(track, w))
        r.canvas.drawArc(o, start, 360f * frac.coerceIn(0f, 1f), false, r.strokePaint(color, w))
    }

    fun dotsRing(r: RenderCtx, cx: Float, cy: Float, rad: Float, n: Int, on: Int, color: Int, off: Int, dot: Float) {
        for (i in 0 until n) {
            val a = Math.toRadians((-90 + 360.0 * i / n))
            r.circle(cx + rad * cos(a).toFloat(), cy + rad * sin(a).toFloat(), dot, if (i < on) color else off)
        }
    }

    fun card(r: RenderCtx, rc: RectF, alpha: Int = 16, rad: Float = 14 * r.u) = r.canvas.drawRoundRect(rc, rad, rad, r.fillPaint(withAlpha(r.c1, alpha)))

    /** Moon age in days (0 new … 14.8 full). */
    fun moonAge(r: RenderCtx): Double {
        val jd = r.today.toEpochDay() + 2440587.5
        val a = ((jd - 2451550.1) / 29.530588853) % 1.0
        return (if (a < 0) a + 1 else a) * 29.530588853
    }

    fun moonName(r: RenderCtx): String {
        val a = moonAge(r)
        return when {
            a < 1.8 -> r.t("New moon", "محاق")
            a < 5.5 -> r.t("Waxing crescent", "هلال متزايد")
            a < 9.2 -> r.t("First quarter", "تربيع أول")
            a < 12.9 -> r.t("Waxing gibbous", "أحدب متزايد")
            a < 16.6 -> r.t("Full moon", "بدر")
            a < 20.3 -> r.t("Waning gibbous", "أحدب متناقص")
            a < 24.0 -> r.t("Last quarter", "تربيع أخير")
            a < 27.7 -> r.t("Waning crescent", "هلال متناقص")
            else -> r.t("New moon", "محاق")
        }
    }

    /** Lit disc of the moon. */
    fun moonDisc(r: RenderCtx, cx: Float, cy: Float, rad: Float, lit: Int, dark: Int) {
        val phase = moonAge(r) / 29.530588853 * 2 * Math.PI
        val k = cos(phase).toFloat()
        r.circle(cx, cy, rad, dark)
        val p = Path()
        val waxing = phase < Math.PI
        val o = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        p.arcTo(o, if (waxing) -90f else 90f, 180f)
        val ew = rad * k
        val e = RectF(cx - kotlin.math.abs(ew), cy - rad, cx + kotlin.math.abs(ew), cy + rad)
        p.arcTo(e, if (waxing) 90f else -90f, if (k > 0) 180f else -180f)
        p.close()
        r.canvas.drawPath(p, r.fillPaint(lit))
    }

    /** Small outline icon for each prayer. */
    fun prayerIcon(r: RenderCtx, key: String, cx: Float, cy: Float, s: Float, color: Int) {
        when (key) {
            "isha" -> Glyphs.moon(r.canvas, cx, cy, s, color)
            "fajr", "sunrise", "maghrib" -> {
                r.canvas.save(); r.canvas.clipRect(cx - s, cy - s, cx + s, cy + s * 0.12f)
                Glyphs.sun(r.canvas, cx, cy + s * 0.1f, s * 0.8f, color)
                r.canvas.restore()
                r.canvas.drawLine(cx - s * 0.5f, cy + s * 0.2f, cx + s * 0.5f, cy + s * 0.2f, r.strokePaint(color, s * 0.09f))
            }
            else -> Glyphs.sun(r.canvas, cx, cy, s * (if (key == "asr") 0.72f else 0.9f), color)
        }
    }

    /** Reading order list (right-to-left in Arabic). */
    fun <T> ordered(r: RenderCtx, l: List<T>) = if (r.ar) l.reversed() else l

    fun weekdayShort(r: RenderCtx, dow: Int): String {
        val en = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val ar = listOf("اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت", "أحد")
        return if (r.ar) ar[dow - 1] else en[dow - 1]
    }
}
