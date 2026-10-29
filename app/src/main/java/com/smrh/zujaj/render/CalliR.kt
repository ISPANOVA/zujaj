package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arabic-calligraphy clocks: the weekday written large (in the user's chosen script) paired with
 * the time, dates and the next prayer. All colours come from Colour 1 / Colour 2 / Accent.
 */
object CalliR {
    private fun dayAr(r: RenderCtx) = Kit.dayName(r, true)

    /** Prayer row: names over times, next one in accent. */
    private fun prayerRow(r: RenderCtx, top: Float, bottom: Float, left: Float = r.pad, right: Float = r.w - r.pad, six: Boolean = true) {
        val d = Kit.day(r)
        val list = Kit.ordered(r, if (six) d.list else d.five)
        val cw = (right - left) / list.size
        val h = bottom - top
        for ((i, p) in list.withIndex()) {
            val cx = left + cw * (i + 0.5f)
            val on = p.key == d.next.key
            r.text(Kit.pName(r, p), cx, top + h * 0.3f, 10 * r.u, if (on) r.accent else r.c2, r.sans, Paint.Align.CENTER, maxW = cw * 0.95f)
            r.text(Kit.pTime(r, p.time, false), cx, top + h * 0.75f, 12.5f * r.u * r.s2, if (on) r.accent else r.c1, r.sansBold, Paint.Align.CENTER, maxW = cw * 0.95f)
        }
    }

    // 1 — big clock, minutes in accent, the day written across it, prayer row below (W)
    fun lawn(r: RenderCtx) {
        val c = r.cfg
        val top = r.h * 0.06f; val bot = r.h * 0.62f
        val hh = Kit.bakedTime(r, r.hh()); val mm = Kit.bakedTime(r, "mm")
        val size = (bot - top) * 1.25f * r.s1
        val p = r.paint(c.color1, size, r.resolveFont(LiveFont.BEBAS, size).f)
        val ink = android.graphics.Rect(); p.getTextBounds("0123456789", 0, 10, ink)
        val base = (top + bot) / 2 - ink.exactCenterY()
        var x = r.pad + 2 * r.u
        if (r.gradTime) {
            r.canvas.drawText("$hh:", x, base, p); x += p.measureText("$hh:")
            p.shader = LinearGradient(0f, base + ink.top, 0f, base + ink.bottom, c.accent, withAlpha(c.accent, 60), Shader.TileMode.CLAMP)
            r.canvas.drawText(mm, x, base, p); x += p.measureText(mm)
            p.shader = null
        } else {
            val tcy = (top + bot) / 2
            r.liveTime(r.hh() + ":", x, tcy, p.textSize, LiveFont.BEBAS, c.color1, Paint.Align.LEFT, sample = "00:"); x += p.measureText("$hh:")
            r.liveTime("mm", x, tcy, p.textSize, LiveFont.BEBAS, c.accent, Paint.Align.LEFT, sample = "00"); x += p.measureText(mm)
        }
        Kit.calli(r, dayAr(r), x - 4 * r.u, r.h * 0.42f, r.h * 0.32f * r.s2, r.c1, maxW = (r.w - r.pad - x) * 1.6f + 60 * r.u, maxH = r.h * 0.3f)
        r.text(Kit.hijri(r), r.w - r.pad, r.h * 0.13f, 10 * r.u, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.4f)
        r.text(Kit.greg(r), r.w - r.pad, r.h * 0.25f, 10 * r.u, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.4f)
        prayerRow(r, r.h * 0.66f, r.h - r.pad * 0.6f)
    }

    // 2 — fading tall clock with the day across it, prayer list on the side (W)
    fun laiha(r: RenderCtx) {
        val split = r.w * 0.58f
        val cx = (r.pad + split) / 2
        r.text(Kit.hijri(r), cx, r.h * 0.13f, 10 * r.u, r.c1, r.sans, Paint.Align.CENTER, maxW = split - r.pad)
        Kit.timeText(r, cx, r.h * 0.5f, r.h * 0.6f * r.s1, LiveFont.BEBAS,
            r.c1, withAlpha(r.c1, 30), maxW = split - r.pad - 6 * r.u, maxH = r.h * 0.5f)
        Kit.calli(r, dayAr(r), cx, r.h * 0.66f, r.h * 0.22f * r.s2, r.accent, maxW = split - r.pad, maxH = r.h * 0.22f)
        r.text(Kit.greg(r), cx, r.h * 0.88f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = split - r.pad)
        Kit.line(r, split + 6 * r.u, r.h * 0.15f, split + 6 * r.u, r.h * 0.85f, withAlpha(r.c1, 40))
        val d = Kit.day(r)
        val x0 = split + 16 * r.u
        val rowH = (r.h - 2 * r.pad) / 6
        for ((i, p) in d.list.withIndex()) {
            val y = r.pad + rowH * (i + 0.5f)
            val on = p.key == d.next.key
            r.circle(x0 + 3 * r.u, y, 2.6f * r.u, if (on) r.accent else withAlpha(r.c1, 0))
            if (!on) r.canvas.drawCircle(x0 + 3 * r.u, y, 2.6f * r.u, r.strokePaint(withAlpha(r.c1, 110), 0.8f * r.u))
            r.text(Kit.pName(r, p), x0 + 10 * r.u, y, 10 * r.u, if (on) r.c1 else r.c2, r.sans, maxW = r.w * 0.2f)
            r.text(Kit.pTime(r, p.time), r.w - r.pad, y, 10.5f * r.u, if (on) r.accent else r.c1, r.sansBold, Paint.Align.RIGHT)
        }
    }

    // 3 — day in calligraphy, a progress line, clock and "Dhuhr • in 0:23" (W)
    fun countdown(r: RenderCtx) {
        Kit.calli(r, dayAr(r), r.w / 2, r.h * 0.25f, r.h * 0.3f * r.s2, r.c1, maxW = r.w - 2 * r.pad, maxH = r.h * 0.3f)
        val y = r.h * 0.45f
        Kit.slider(r, r.pad + 4 * r.u, y, r.w - 2 * r.pad - 8 * r.u, Kit.progress(r), r.accent, withAlpha(r.c1, 40), 1.6f * r.u, 2.6f * r.u)
        Kit.clock(r, r.w / 2, r.h * 0.66f, 40 * r.u * r.s1, LiveFont.BEBAS, maxW = r.w * 0.6f, maxH = r.h * 0.26f)
        val nx = Kit.day(r).next
        val name = Kit.pName(r, nx)
        val nw = r.measure(name, 11 * r.u, r.sansBold)
        r.text(name, r.w / 2 - 4 * r.u, r.h * 0.88f, 11 * r.u, r.accent, r.sansBold, Paint.Align.RIGHT)
        r.circle(r.w / 2 + 2 * r.u, r.h * 0.88f, 1.6f * r.u, r.c2)
        Kit.countdown(r, r.w / 2 + 8 * r.u, r.h * 0.88f, 11 * r.u, r.c1)
        if (nw < 0) Unit
    }

    // 4 — time over a giant faded day, weekday + date bottom-left, calligraphy bottom-right (W)
    fun ascend(r: RenderCtx) {
        Kit.calli(r, dayAr(r), r.w / 2, r.h * 0.34f, r.h * 0.6f, withAlpha(r.c1, 26), maxW = r.w * 1.1f, maxH = r.h * 0.62f)
        Kit.clock(r, r.w / 2, r.h * 0.34f, 44 * r.u * r.s1, LiveFont.SANS_BOLD, maxW = r.w * 0.8f, maxH = r.h * 0.3f,
            pattern = if (r.cfg.hour24) "HH:mm" else "hh:mm a")
        r.text(if (r.ar) Kit.hijri(r) else Kit.dayName(r, false),
            r.pad + 6 * r.u, r.h * 0.72f, 15 * r.u, r.c1, r.sansMed, spacing = 0.08f, maxW = r.w * 0.45f)
        r.text(Kit.greg(r), r.pad + 6 * r.u, r.h * 0.86f, 11 * r.u, r.c2, r.sans, maxW = r.w * 0.45f)
        Kit.calli(r, dayAr(r), r.w * 0.75f, r.h * 0.78f, r.h * 0.26f * r.s2, r.c1, maxW = r.w * 0.42f, maxH = r.h * 0.26f)
    }

    // 5 — hours | day in accent calligraphy | minutes (W)
    fun between(r: RenderCtx) {
        r.text(Kit.greg(r) + "   |   " + Kit.hijri(r), r.w / 2, r.h * 0.13f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        val cy = r.h * 0.5f
        Kit.clock(r, r.pad + 2 * r.u, cy, 62 * r.u * r.s1, LiveFont.BEBAS, align = Paint.Align.LEFT, maxH = r.h * 0.5f, pattern = r.hh())
        Kit.clock(r, r.w - r.pad - 2 * r.u, cy, 62 * r.u * r.s1, LiveFont.BEBAS, align = Paint.Align.RIGHT, maxH = r.h * 0.5f, pattern = "mm")
        Kit.calli(r, dayAr(r), r.w / 2, cy, r.h * 0.3f * r.s2, r.accent, maxW = r.w * 0.4f, maxH = r.h * 0.28f)
        val nx = Kit.day(r).next
        r.text(Kit.pTime(r, nx.time) + "  •  " + Kit.pName(r, nx), r.w / 2, r.h * 0.87f, 10.5f * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
    }

    // 6 — square: day, hijri month, next prayer (S)
    fun aura(r: RenderCtx) {
        Kit.calli(r, dayAr(r), r.w / 2, r.h * 0.32f, r.h * 0.3f * r.s1, r.c1, maxW = r.w - 2 * r.pad, maxH = r.h * 0.3f)
        r.text(Kit.hijri(r), r.w / 2, r.h * 0.6f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        val nx = Kit.day(r).next
        r.text(Kit.pName(r, nx), r.w - r.pad - 2 * r.u, r.h * 0.82f, 11 * r.u, r.accent, r.sansBold, Paint.Align.RIGHT)
        r.text(Kit.pTime(r, nx.time), r.pad + 2 * r.u, r.h * 0.82f, 11 * r.u, r.c1, r.sansBold)
    }

    // 7 — square with a gold corner frame (S)
    fun rukn(r: RenderCtx) {
        val g = r.accent
        val s = r.strokePaint(g, 2 * r.u, round = false)
        val x = r.w - r.pad; val y = r.pad
        r.canvas.drawLine(x - 34 * r.u, y + 6 * r.u, x - 4 * r.u, y + 6 * r.u, s)
        r.canvas.drawLine(x - 4 * r.u, y + 6 * r.u, x - 4 * r.u, y + 36 * r.u, s)
        Kit.calli(r, dayAr(r), r.w / 2, r.h * 0.5f, r.h * 0.3f * r.s1, r.c1, maxW = r.w - 2 * r.pad - 8 * r.u, maxH = r.h * 0.3f)
        r.text(Kit.hijri(r), r.w - r.pad - 4 * r.u, r.h * 0.8f, 10 * r.u, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w - 2 * r.pad)
    }

    // 8 — time + rows on the left, big day on the right (W)
    fun khatt(r: RenderCtx) {
        val split = r.w * 0.42f
        Kit.clock(r, split - 6 * r.u, r.h * 0.18f, 22 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.RIGHT, maxW = split - r.pad)
        val d = Kit.day(r); val nx = d.next
        val rows = listOf(
            r.t("Next", "القادمة") to Kit.pName(r, nx),
            r.t("Time", "الوقت") to Kit.pTime(r, nx.time),
            r.t("Hijri", "هجري") to Kit.hijriShort(r),
            r.t("Battery", "الشحن") to r.n("${Kit.battery(r)}%"),
        )
        rows.forEachIndexed { i, (k, v) ->
            val y = r.h * (0.38f + i * 0.12f)
            r.text(k, split - 6 * r.u, y, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
            r.text(v, r.pad + 2 * r.u, y, 10 * r.u, if (i == 0) r.accent else r.c1, r.sansBold)
        }
        Kit.slider(r, r.pad + 2 * r.u, r.h * 0.88f, split - r.pad - 8 * r.u, Kit.battery(r) / 100f, r.accent, knob = 0f, thick = 2.5f * r.u)
        Kit.line(r, split + 6 * r.u, r.h * 0.12f, split + 6 * r.u, r.h * 0.88f, withAlpha(r.c1, 34))
        val cx = (split + r.w - r.pad) / 2 + 4 * r.u
        Kit.calli(r, dayAr(r), cx, r.h * 0.42f, r.h * 0.42f * r.s2, r.c1, maxW = r.w - r.pad - split - 14 * r.u, maxH = r.h * 0.42f)
        r.text(Kit.hijri(r), r.w - r.pad, r.h * 0.78f, 9.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        r.text(Kit.greg(r), r.w - r.pad, r.h * 0.88f, 9.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    // 9 — tall stretched digits beside the day and date (S)
    fun obelisk(r: RenderCtx) {
        val left = r.pad
        Kit.timeText(r, left + r.w * 0.27f, r.h * 0.42f, r.h * 0.6f * r.s1, LiveFont.SIXCAPS,
            r.c1, withAlpha(r.c1, 70), maxW = r.w * 0.5f, maxH = r.h * 0.6f)
        Kit.calli(r, dayAr(r), r.w * 0.76f, r.h * 0.3f, r.h * 0.16f, r.c1, maxW = r.w * 0.4f, maxH = r.h * 0.16f)
        r.textTight(r.n(r.today.dayOfMonth), r.w * 0.76f, r.h * 0.5f, 30 * r.u * r.s2, r.c1, r.sansBold)
        r.text(Kit.hijriShort(r), r.w * 0.76f, r.h * 0.64f, 8.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w * 0.3f)
        Kit.line(r, left, r.h * 0.77f, r.w - left, r.h * 0.77f, withAlpha(r.c1, 30))
        val nx = Kit.day(r).next
        Kit.countdown(r, left, r.h * 0.88f, 11 * r.u, r.c1, prefix = "")
        r.text(Kit.pTime(r, nx.time), r.w - left, r.h * 0.88f, 11 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
    }

    // 10 — hours / minutes stacked with the day underneath (S)
    fun column(r: RenderCtx) {
        Kit.clock(r, r.w / 2, r.h * 0.2f, 26 * r.u * r.s1, LiveFont.SANS_BOLD, pattern = if (r.cfg.hour24) "H" else "h")
        Kit.clock(r, r.w / 2, r.h * 0.42f, 26 * r.u * r.s1, LiveFont.SANS_BOLD, color = r.c2, pattern = "mm")
        Kit.line(r, r.w / 2, r.h * 0.55f, r.w / 2, r.h * 0.64f, withAlpha(r.c1, 60))
        Kit.calli(r, dayAr(r), r.w / 2, r.h * 0.78f, r.h * 0.16f * r.s2, r.c1, maxW = r.w - 2 * r.pad, maxH = r.h * 0.2f)
    }

    // 11 — the day alone, very large (W)
    fun bigDay(r: RenderCtx) =
        Kit.calli(r, dayAr(r), r.w / 2, r.h / 2, r.h * 0.7f * r.s1, r.c1, maxW = r.w - 2 * r.pad, maxH = r.h - 2 * r.pad)

    // 12 — the day standing vertically (S)
    fun vertical(r: RenderCtx) {
        r.canvas.save()
        r.canvas.rotate(-90f, r.w / 2, r.h / 2)
        Kit.calli(r, dayAr(r), r.w / 2, r.h / 2, r.w * 0.36f * r.s1, r.c1, maxW = r.h - 2 * r.pad, maxH = r.w * 0.5f)
        r.canvas.restore()
    }

    // 13 — phrase + day, with "remaining" and the prayer underneath (W)
    fun satr(r: RenderCtx) {
        val text = r.cfg.text.ifBlank { "صلِّ على النبي" }
        Kit.calli(r, text, r.w * 0.28f, r.h * 0.38f, r.h * 0.3f * r.s1, r.c1, maxW = r.w * 0.42f, maxH = r.h * 0.34f)
        Kit.calli(r, dayAr(r), r.w * 0.74f, r.h * 0.38f, r.h * 0.3f * r.s1, r.c1, maxW = r.w * 0.4f, maxH = r.h * 0.34f)
        val nx = Kit.day(r).next
        r.text(Kit.pName(r, nx), r.w - r.pad - 4 * r.u, r.h * 0.8f, 15 * r.u * r.s2, r.c1, if (r.ar) F.AMIRI else r.sansMed, Paint.Align.RIGHT)
        Kit.countdown(r, r.pad + 4 * r.u, r.h * 0.8f, 13 * r.u * r.s2, r.c2)
    }

    // 14 — the day over a big date number (S)
    fun calendarDay(r: RenderCtx) {
        Kit.calli(r, dayAr(r), r.w * 0.6f, r.h * 0.22f, r.h * 0.2f, r.c1, maxW = r.w * 0.7f, maxH = r.h * 0.2f)
        r.textTight(r.n(r.today.dayOfMonth), r.w * 0.6f, r.h * 0.55f, 60 * r.u * r.s1, r.c1, r.sansBold, maxH = r.h * 0.4f)
        r.text(Kit.hijri(r), r.w * 0.6f, r.h * 0.86f, 9.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w * 0.8f)
    }

    // 15 — the day repeated as fading echoes behind the clock (W)
    fun echo(r: RenderCtx) {
        r.text(Kit.hijri(r), r.pad + 6 * r.u, r.h * 0.12f, 10 * r.u, r.c1, r.sans, maxW = r.w * 0.5f)
        Kit.clock(r, r.pad + 4 * r.u, r.h * 0.5f, 60 * r.u * r.s1, LiveFont.BEBAS, align = Paint.Align.LEFT, maxW = r.w * 0.5f, maxH = r.h * 0.52f)
        val cx = r.w * 0.76f
        for (i in 2 downTo 0) {
            val a = listOf(255, 70, 22)[i]
            Kit.calli(r, dayAr(r), cx + i * 4 * r.u, r.h * (0.52f - i * 0.2f), r.h * 0.24f * r.s2, withAlpha(r.accent, a), maxW = r.w * 0.42f, maxH = r.h * 0.24f)
        }
        val nx = Kit.day(r).next
        r.text(Kit.pName(r, nx), r.w * 0.5f, r.h * 0.88f, 11 * r.u, r.accent, r.sansBold, Paint.Align.LEFT)
        Kit.countdown(r, r.w * 0.48f, r.h * 0.88f, 11 * r.u, r.c1, Paint.Align.RIGHT)
    }

    // 16 — fading clock with the day in accent over it and blossoms (W)
    fun blossom(r: RenderCtx) {
        r.text(Kit.greg(r) + "  |  " + Kit.hijri(r), r.w / 2, r.h * 0.12f, 10 * r.u, r.c1, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        Kit.timeText(r, r.w / 2, r.h * 0.48f, r.h * 0.5f * r.s1, LiveFont.BEBAS,
            r.c1, withAlpha(r.c1, 20), maxW = r.w * 0.6f, maxH = r.h * 0.48f)
        Kit.calli(r, dayAr(r), r.w * 0.58f, r.h * 0.6f, r.h * 0.26f * r.s2, r.accent, maxW = r.w * 0.5f, maxH = r.h * 0.26f)
        // blossoms
        fun flower(cx: Float, cy: Float, s: Float, a: Int) {
            for (k in 0 until 5) {
                val ang = Math.toRadians(k * 72.0 - 90)
                r.circle(cx + s * 0.55f * cos(ang).toFloat(), cy + s * 0.55f * sin(ang).toFloat(), s * 0.5f, withAlpha(r.accent, a))
            }
            r.circle(cx, cy, s * 0.28f, withAlpha(Color.WHITE, a))
        }
        flower(r.w * 0.12f, r.h * 0.62f, 16 * r.u, 230); flower(r.w * 0.2f, r.h * 0.8f, 10 * r.u, 200)
        flower(r.w * 0.9f, r.h * 0.18f, 14 * r.u, 230); flower(r.w * 0.82f, r.h * 0.1f, 8 * r.u, 180)
        for ((x, y) in listOf(0.08f to 0.35f, 0.3f to 0.9f, 0.86f to 0.42f, 0.7f to 0.88f)) r.circle(r.w * x, r.h * y, 2 * r.u, withAlpha(r.accent, 140))
        val nx = Kit.day(r).next
        r.text(Kit.pName(r, nx), r.w / 2 + 4 * r.u, r.h * 0.86f, 11 * r.u, r.accent, r.sansBold)
        Kit.countdown(r, r.w / 2 - 4 * r.u, r.h * 0.86f, 11 * r.u, r.c1, Paint.Align.RIGHT)
    }

    // 17 — clock fading into the background with the day written through it, starry streaks (W)
    fun mirage(r: RenderCtx) {
        val nx = Kit.day(r).next
        r.text(Kit.pTime(r, nx.time) + "  •  " + Kit.pName(r, nx), r.w / 2, r.h * 0.11f, 10.5f * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
        for (i in 0 until 7) {
            val x = r.w * (0.08f + i * 0.14f); val y = r.h * (0.2f + (i % 3) * 0.18f)
            Kit.line(r, x, y, x + 18 * r.u, y - 8 * r.u, withAlpha(r.c1, 60), 0.7f * r.u)
            r.circle(x + 18 * r.u, y - 8 * r.u, 1.2f * r.u, withAlpha(r.c1, 160))
        }
        Kit.timeText(r, r.w / 2, r.h * 0.5f, r.h * 0.6f * r.s1, LiveFont.BEBAS,
            r.c1, withAlpha(r.c1, 10), maxW = r.w * 0.6f, maxH = r.h * 0.56f)
        Kit.calli(r, dayAr(r), r.w * 0.62f, r.h * 0.68f, r.h * 0.3f * r.s2, withAlpha(r.accent, 230), maxW = r.w * 0.6f, maxH = r.h * 0.3f, color2 = r.c2)
        r.text(Kit.greg(r) + "  |  " + Kit.hijri(r), r.w / 2, r.h * 0.9f, 10 * r.u, r.c1, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    // 18 — the day written over a big gradient date (S)
    fun dayNumber(r: RenderCtx) {
        Kit.gradText(r, r.n(r.today.dayOfMonth), r.w / 2, r.h * 0.52f, r.h * 0.62f * r.s1, r.sansBold, r.accent, withAlpha(r.accent, 70), maxH = r.h * 0.5f)
        Kit.calli(r, dayAr(r), r.w / 2, r.h * 0.18f, r.h * 0.16f, r.accent, maxW = r.w * 0.7f, maxH = r.h * 0.16f)
        r.text(Kit.hijri(r), r.w / 2, r.h * 0.88f, 9.5f * r.u, r.accent, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    // 19 — Arabic hour above Arabic minutes in two colours (S)
    fun noqta(r: RenderCtx) {
        Kit.clock(r, r.w / 2, r.h * 0.3f, 40 * r.u * r.s1, LiveFont.AR_CAIRO, r.c1, maxH = r.h * 0.3f, pattern = r.hh())
        Kit.clock(r, r.w / 2, r.h * 0.62f, 40 * r.u * r.s1, LiveFont.AR_CAIRO, r.accent, maxH = r.h * 0.3f, pattern = "mm")
        r.text(Kit.hijri(r), r.w / 2, r.h * 0.88f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }
}
