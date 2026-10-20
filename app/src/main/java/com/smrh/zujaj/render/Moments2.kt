package com.smrh.zujaj.render

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.min

/** Embossed / puffy text helpers and three new designs (bubble clock, words-of-the-day card, prayer board). */
object Moments2R {
    /** Text with depth: soft drop shadow, a top-light gradient fill and a thin bright rim. */
    fun emboss(r: RenderCtx, s: String, cx: Float, cy: Float, size: Float, font: F, top: Int, bottom: Int, maxW: Float, maxH: Float, puffy: Float = 0f) {
        if (s.isBlank()) return
        val p = r.paint(top, size, font, Paint.Align.LEFT)
        r.fitPaint(p, s, maxW, maxH)
        val b = android.graphics.Rect(); p.getTextBounds(s, 0, s.length, b)
        val w = p.measureText(s)
        val x = cx - w / 2; val y = cy - b.exactCenterY()
        val ink = RectF(x + b.left, y + b.top, x + b.right, y + b.bottom)
        val stroke = p.textSize * puffy
        fun base(pp: Paint) {
            if (stroke > 0f) { pp.style = Paint.Style.FILL_AND_STROKE; pp.strokeWidth = stroke; pp.strokeJoin = Paint.Join.ROUND; pp.strokeCap = Paint.Cap.ROUND }
        }
        // shadow
        val sh = Paint(p).apply { shader = null; color = withAlpha(Color.BLACK, 110); maskFilter = BlurMaskFilter(p.textSize * 0.05f + 1f, BlurMaskFilter.Blur.NORMAL); base(this) }
        r.canvas.drawText(s, x + p.textSize * 0.015f, y + p.textSize * 0.04f, sh)
        // dark lower edge (thickness)
        val edge = Paint(p).apply { shader = null; color = RenderCtx.mix(bottom, Color.BLACK, 0.35f); base(this) }
        r.canvas.drawText(s, x, y + p.textSize * 0.02f, edge)
        // body
        val body = Paint(p).apply { shader = LinearGradient(0f, ink.top, 0f, ink.bottom, top, bottom, Shader.TileMode.CLAMP); base(this) }
        r.canvas.drawText(s, x, y, body)
        // light from above
        val hl = Paint(p).apply {
            shader = LinearGradient(0f, ink.top, 0f, ink.top + ink.height() * 0.45f, withAlpha(Color.WHITE, 150), withAlpha(Color.WHITE, 0), Shader.TileMode.CLAMP); base(this)
        }
        r.canvas.drawText(s, x, y - p.textSize * 0.01f, hl)
    }

    private fun hijriLine(r: RenderCtx): String {
        val h = Calendars.hijri(r.today)
        return if (r.ar) "${r.n(h.day)} ${Calendars.hijriMonth(h.month, true)} ${r.n(h.year)}" else "${h.day} ${Calendars.hijriMonth(h.month, false)} ${h.year}"
    }

    private fun gregLine(r: RenderCtx): String {
        val d = r.today
        return if (r.ar) "${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)} ${r.n(d.year)}" else "${d.dayOfMonth} ${Calendars.monthName(d.monthValue, false)} ${d.year}"
    }

    // =============================================================================  1 · bubble clock
    /** Big puffy hours over minutes, the day in silver calligraphy, then dates and weather. */
    fun bubble(r: RenderCtx) {
        val cx = r.w / 2
        val hh = Kit.bakedTime(r, r.hh()); val mm = Kit.bakedTime(r, "mm")
        val top = Color.WHITE; val bot = RenderCtx.mix(r.c1, r.accent, 0.55f)
        val dh = r.h * 0.2f
        emboss(r, hh, cx, r.h * 0.15f, dh * 1.25f, F.CLK_RIGHTEOUS, top, bot, r.w * 0.6f, dh, puffy = 0.09f)
        emboss(r, mm, cx, r.h * 0.37f, dh * 1.25f, F.CLK_RIGHTEOUS, top, bot, r.w * 0.6f, dh, puffy = 0.09f)
        val day = Calendars.dayName(r.today, r.ar)
        emboss(r, day, cx, r.h * 0.6f, r.h * 0.17f, if (r.ar) r.calliFont else F.PLAYFAIR, Color.WHITE, 0xFFB9B9C6.toInt(), r.w * 0.66f, r.h * 0.15f)
        // info row
        val y0 = r.h * 0.74f; val y1 = r.h * 0.9f
        r.canvas.drawLine(r.w * 0.16f, y0, r.w * 0.84f, y0, r.strokePaint(withAlpha(Color.WHITE, 60), r.u * 0.8f))
        r.canvas.drawLine(r.w * 0.16f, y1 + 6 * r.u, r.w * 0.84f, y1 + 6 * r.u, r.strokePaint(withAlpha(Color.WHITE, 60), r.u * 0.8f))
        r.canvas.drawLine(cx, y0 + 4 * r.u, cx, y1 + 2 * r.u, r.strokePaint(withAlpha(Color.WHITE, 90), r.u))
        val mid = (y0 + y1) / 2 + 3 * r.u
        val dateX = if (r.ar) cx - 8 * r.u else cx + 8 * r.u
        val al = if (r.ar) Paint.Align.RIGHT else Paint.Align.LEFT
        r.text(hijriLine(r), dateX, mid - 8 * r.u, 10.5f * r.u * r.s2, r.c1, r.sans, al, maxW = r.w * 0.34f)
        r.text(gregLine(r), dateX, mid + 8 * r.u, 10.5f * r.u * r.s2, r.c1, r.sans, al, maxW = r.w * 0.34f)
        val w = Kit.weather(r)
        val wx = if (r.ar) cx + 22 * r.u else cx - 22 * r.u
        Glyphs.weather(r.canvas, w.cond, w.night, wx, mid, 26 * r.u)
        val tx = if (r.ar) cx + 44 * r.u else cx - 44 * r.u
        val tal = if (r.ar) Paint.Align.LEFT else Paint.Align.RIGHT
        r.text(Kit.temp(r, w.temp), tx, mid - 7 * r.u, 15 * r.u * r.s1, r.c1, r.sansBold, tal)
        r.text(WeatherRepo.condName(w.cond, r.ar), tx, mid + 9 * r.u, 10 * r.u, r.c2, r.sans, tal, maxW = r.w * 0.2f)
    }

    // =============================================================================  2 · words of the day
    /** Short supplications for the card (rotate with the verses and dhikr). */
    val duas = listOf(
        "اللهم صيبًا نافعًا", "اللهم بارك لنا في يومنا", "رب اغفر لي ولوالدي", "اللهم إني أسألك العفو والعافية",
        "حسبي الله ونعم الوكيل", "اللهم اجعله يومًا مباركًا", "لا إله إلا أنت سبحانك",
    )

    /** What the card shows now: the pick changes every cfg.counter minutes (default 6 h). */
    private fun words(r: RenderCtx): String {
        val every = (if (r.cfg.counter > 0) r.cfg.counter else 360).toLong()
        val slot = (r.now.toInstant().toEpochMilli() / 60_000L) / every
        val pool: List<String> = when (r.cfg.variant) {
            1 -> VerseR.ayat.map { it.first }
            2 -> IslamicR.adhkar
            3 -> listOf(r.cfg.text.ifBlank { "اللهم صيبًا نافعًا" })
            else -> duas
        }
        return pool[(slot % pool.size).toInt()]
    }

    /** Sky-blue card: dates and the time on top, the day in big calligraphy, rotating words, prayer row. */
    fun dayCard(r: RenderCtx) {
        val cx = r.w / 2
        // top row: hijri · time pill · gregorian
        val ty = r.pad + 12 * r.u
        val time = Kit.bakedTime(r, r.hm())
        val tp = r.paint(Color.BLACK, 13 * r.u, r.sansBold)
        val pw = tp.measureText(time) + 22 * r.u
        val pill = RectF(cx - pw / 2, ty - 11 * r.u, cx + pw / 2, ty + 11 * r.u)
        r.canvas.drawRoundRect(pill, pill.height() / 2, pill.height() / 2, r.fillPaint(withAlpha(Color.WHITE, 235)))
        r.text(time, cx, ty, 13 * r.u, 0xFF1A1A22.toInt(), r.sansBold, Paint.Align.CENTER)
        val leftTxt = if (r.ar) gregLine(r) else hijriLine(r)
        val rightTxt = if (r.ar) hijriLine(r) else gregLine(r)
        r.text(leftTxt, pill.left - 10 * r.u, ty, 11 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.RIGHT, maxW = pill.left - r.pad - 10 * r.u)
        r.text(rightTxt, pill.right + 10 * r.u, ty, 11 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.LEFT, maxW = r.w - pill.right - r.pad - 10 * r.u)
        // day + words
        val day = Calendars.dayName(r.today, r.ar)
        emboss(r, day, cx, r.h * 0.4f, r.h * 0.34f * r.s1, if (r.ar) r.calliFont else F.PLAYFAIR, Color.WHITE, 0xFFE4EAF4.toInt(), r.w - 2 * r.pad, r.h * 0.3f)
        emboss(r, words(r), cx, r.h * 0.67f, r.h * 0.15f, r.calliFont, Color.WHITE, 0xFFE4EAF4.toInt(), r.w - 2 * r.pad - 10 * r.u, r.h * 0.12f)
        // prayers
        val d = Kit.day(r)
        val list = d.five
        val slotW = (r.w - 2 * r.pad) / list.size
        val py = r.h - r.pad - 12 * r.u
        list.forEachIndexed { i, p ->
            val idx = if (r.ar) list.size - 1 - i else i
            val x = r.pad + slotW * idx + slotW / 2
            val next = p.key == d.next.key
            if (next) r.canvas.drawRoundRect(RectF(x - slotW / 2 + 3 * r.u, py - 13 * r.u, x + slotW / 2 - 3 * r.u, py + 13 * r.u), 9 * r.u, 9 * r.u, r.fillPaint(withAlpha(Color.WHITE, 70)))
            val label = "${Kit.pName(r, p)} ${Kit.pTime(r, p.time)}"
            r.text(label, x, py, 10 * r.u, r.c1, r.sansBold, Paint.Align.CENTER, maxW = slotW - 8 * r.u)
        }
    }

    // =============================================================================  3 · prayer board
    /** Glass card with the next prayer and its countdown, and a row of all prayers with little icons. */
    fun board(r: RenderCtx) {
        val d = Kit.day(r)
        val gap = 8 * r.u
        val topH = r.h * 0.62f
        val card = RectF(0f, 0f, r.w, topH)
        val row = RectF(0f, topH + gap, r.w, r.h)
        val rad = Renderer.cornerPx(r)
        Renderer.liquidGlass(r.canvas, card, rad, r.u, r.cfg.bgColor, 255)
        Renderer.liquidGlass(r.canvas, row, rad * 0.8f, r.u, r.cfg.bgColor, 255)
        val ar = r.ar
        val p = 16 * r.u
        val sx = if (ar) card.right - p else card.left + p
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val ox = if (ar) card.left + p else card.right - p
        val oal = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        r.text(r.t("Next prayer", "الصلاة القادمة"), sx, card.top + 20 * r.u, 11 * r.u * r.s2, r.c1, r.sansBold, al)
        r.text("⌖ " + com.smrh.zujaj.data.Prefs.cityName.ifBlank { r.t("My city", "مدينتي") }, if (ar) sx - 100 * r.u else sx + 100 * r.u, card.top + 20 * r.u, 10 * r.u, r.c2, r.sans, al, maxW = 90 * r.u)
        // big prayer name
        val nameCx = if (ar) card.right - card.width() * 0.25f else card.left + card.width() * 0.25f
        r.textTight(Kit.pName(r, d.next), nameCx, card.top + topH * 0.5f, 40 * r.u * r.s1, r.c1, if (ar) r.calliFont else r.sansBold, maxW = card.width() * 0.42f, maxH = topH * 0.3f)
        r.text(r.t("in", "بعد"), sx, card.top + topH * 0.72f, 11 * r.u, r.c2, r.sans, al)
        Kit.countdown(r, sx, card.top + topH * 0.87f, 12 * r.u, r.c1, al, prefix = "", bold = true)
        // weather + dates on the other side
        val w = Kit.weather(r)
        Glyphs.moon(r.canvas, if (ar) ox + 18 * r.u else ox - 18 * r.u, card.top + 26 * r.u, 46 * r.u, r.accent)
        r.text(Kit.temp(r, w.temp), ox, card.top + topH * 0.43f, 18 * r.u * r.s1, r.c1, r.sansBold, oal)
        r.text(WeatherRepo.condName(w.cond, ar), if (ar) ox + 46 * r.u else ox - 46 * r.u, card.top + topH * 0.43f, 9 * r.u, r.c2, r.sans, oal, maxW = 70 * r.u)
        r.text(Calendars.dayName(r.today, ar) + "، " + gregLine(r), ox, card.top + topH * 0.6f, 11 * r.u, r.c1, r.sans, oal, maxW = card.width() * 0.48f)
        r.text(hijriLine(r), ox, card.top + topH * 0.73f, 11 * r.u, r.c1, r.sans, oal, maxW = card.width() * 0.48f)
        // progress to the next prayer
        val bw = card.width() * 0.4f
        val bx = if (ar) ox else ox - bw
        val by = card.top + topH * 0.87f
        r.canvas.drawLine(bx, by, bx + bw, by, r.strokePaint(withAlpha(r.c1, 50), 4 * r.u))
        val f = d.progress.coerceIn(0f, 1f)
        if (ar) r.canvas.drawLine(bx + bw, by, bx + bw - bw * f, by, r.strokePaint(r.c1, 4 * r.u)) else r.canvas.drawLine(bx, by, bx + bw * f, by, r.strokePaint(r.c1, 4 * r.u))
        r.circle(if (ar) bx + bw - bw * f else bx + bw * f, by, 5 * r.u, r.c1)
        // row of all six
        val list = d.list
        val sw = row.width() / list.size
        list.forEachIndexed { i, pr ->
            val idx = if (ar) list.size - 1 - i else i
            val x = row.left + sw * idx + sw / 2
            val next = pr.key == d.next.key
            val ic = RectF(x - 17 * r.u, row.top + row.height() * 0.32f, x + 17 * r.u, row.top + row.height() * 0.32f + 26 * r.u)
            r.canvas.drawRoundRect(ic, 9 * r.u, 9 * r.u, r.fillPaint(withAlpha(Color.WHITE, if (next) 70 else 26)))
            if (next) r.canvas.drawRoundRect(ic, 9 * r.u, 9 * r.u, r.strokePaint(withAlpha(Color.WHITE, 200), r.u, false))
            val gc = ic.centerY()
            when (pr.key) {
                "fajr" -> { Glyphs.moon(r.canvas, x, gc, 22 * r.u, 0xFFE9E3FF.toInt()) }
                "sunrise" -> Glyphs.sun(r.canvas, x, gc + 3 * r.u, 20 * r.u, 0xFFFFC34D.toInt(), false)
                "dhuhr" -> Glyphs.sun(r.canvas, x, gc, 20 * r.u, 0xFFFFC34D.toInt())
                "asr" -> { Glyphs.sun(r.canvas, x - 3 * r.u, gc - 2 * r.u, 16 * r.u, 0xFFFFC34D.toInt()); Glyphs.cloud(r.canvas, x + 3 * r.u, gc + 3 * r.u, 18 * r.u, Color.WHITE) }
                "maghrib" -> Glyphs.sun(r.canvas, x, gc + 5 * r.u, 20 * r.u, 0xFFFF9F5A.toInt(), false)
                else -> Glyphs.moon(r.canvas, x, gc, 22 * r.u, 0xFFDCDCE6.toInt())
            }
            r.text(Kit.pName(r, pr), x, row.top + row.height() * 0.17f, 10.5f * r.u, r.c1, r.sansBold, Paint.Align.CENTER, maxW = sw - 4 * r.u)
            r.text(Kit.pTime(r, pr.time, lead = false), x, row.bottom - row.height() * 0.17f, 11 * r.u, r.c1, r.sansBold, Paint.Align.CENTER, maxW = sw - 4 * r.u)
        }
    }

    // =============================================================================  4 · sport card (tall)
    /** Tall card: slanted two-tone time, the day standing up in big 3D letters with a battery bar beside it,
     *  then slanted 3D pills for the hijri and gregorian dates and the next prayer with its progress. */
    fun sport(r: RenderCtx) {
        val ar = r.ar
        val acc = r.accent; val ink = r.c1; val grey = r.c2
        val p0 = r.pad
        val L = p0; val R = r.w - p0; val W = R - L
        val c = r.canvas
        // ---- time
        val hh = Kit.bakedTime(r, r.hh()); val mm = Kit.bakedTime(r, "mm")
        val tp = r.paint(ink, 100f, F.CLK_BEBAS).apply { textSkewX = -0.24f; isFakeBoldText = true }
        val tb = android.graphics.Rect(); tp.getTextBounds("00", 0, 2, tb)
        val maxTH = r.h * 0.15f * r.s1
        tp.textSize = 100f * minOf(maxTH / tb.height(), W * 0.98f / tp.measureText(hh + mm))
        tp.getTextBounds("00", 0, 2, tb)
        val ty = p0 + tb.height()
        val tw = tp.measureText(hh + mm)
        val tx = if (ar) R - tw else L
        c.drawText(hh, tx + r.u, ty + 2 * r.u, Paint(tp).apply { color = withAlpha(Color.BLACK, 120) })
        c.drawText(hh, tx, ty, tp)
        c.drawText(mm, tx + tp.measureText(hh), ty, Paint(tp).apply { color = acc })
        // ---- lower block: pills + prayer
        val gap = r.h * 0.018f
        val barH = 4 * r.u
        val prH = r.h * 0.15f; val pillH = r.h * 0.065f
        val barY = r.h - p0 - barH / 2
        val prBot = barY - barH / 2 - gap
        val prTop = prBot - prH
        val gBot = prTop - gap; val gTop = gBot - pillH
        val hBot = gTop - gap * 0.8f; val hTop = hBot - pillH
        val pw = W * 0.86f
        val px0 = if (ar) R - pw else L
        fun slab(top: Float, bot: Float, x0: Float, w: Float, cTop: Int, cBot: Int) {
            val rc = RectF(x0, top, x0 + w, bot)
            val rad = (bot - top) * 0.22f
            c.save()
            c.skew(-0.14f, 0f); val dx = rc.centerY() * 0.14f
            val rr = RectF(rc.left + dx, rc.top, rc.right + dx, rc.bottom)
            c.drawRoundRect(RectF(rr.left + 2.5f * r.u, rr.top + 3.5f * r.u, rr.right + 2.5f * r.u, rr.bottom + 3.5f * r.u), rad, rad,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(Color.BLACK, 150); maskFilter = BlurMaskFilter(3 * r.u, BlurMaskFilter.Blur.NORMAL) })
            c.drawRoundRect(RectF(rr.left, rr.top + 2 * r.u, rr.right, rr.bottom + 2 * r.u), rad, rad, r.fillPaint(RenderCtx.mix(cBot, Color.BLACK, 0.45f)))
            c.drawRoundRect(rr, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, rr.top, 0f, rr.bottom, cTop, cBot, Shader.TileMode.CLAMP) })
            c.drawRoundRect(RectF(rr.left + r.u, rr.top + r.u, rr.right - r.u, rr.top + rr.height() * 0.45f), rad, rad,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, rr.top, 0f, rr.top + rr.height() * 0.45f, withAlpha(Color.WHITE, 90), withAlpha(Color.WHITE, 0), Shader.TileMode.CLAMP) })
            c.restore()
        }
        fun slText(s: String, cx: Float, cy: Float, size: Float, color: Int, font: F, maxW: Float, maxH: Float) {
            c.save(); c.skew(-0.14f, 0f); val dx = cy * 0.14f
            r.textTight(s, cx + dx, cy, size, color, font, maxW = maxW, maxH = maxH)
            c.restore()
        }
        slab(hTop, hBot, px0, pw, RenderCtx.mix(acc, Color.WHITE, 0.18f), RenderCtx.mix(acc, Color.BLACK, 0.3f))
        val hs = Calendars.hijri(r.today)
        val hTxt = if (ar) "${r.n(hs.day)} ${Calendars.hijriMonth(hs.month, true)}" else "${hs.day} ${Calendars.hijriMonth(hs.month, false)}"
        slText(hTxt, px0 + pw / 2, (hTop + hBot) / 2, pillH * 0.62f * r.s2, ink, if (ar) r.calliFont else r.sansBold, pw * 0.86f, pillH * 0.66f)
        slab(gTop, gBot, px0 - (if (ar) W * 0.03f else -W * 0.03f), pw, RenderCtx.mix(grey, Color.WHITE, 0.55f), RenderCtx.mix(grey, Color.BLACK, 0.3f))
        val gTxt = (if (ar) r.n(r.today.dayOfMonth.toString().padStart(2, '0')) else r.today.dayOfMonth.toString().padStart(2, '0')) + " " + Calendars.monthName(r.today.monthValue, ar)
        slText(gTxt, px0 + pw / 2 - (if (ar) W * 0.03f else -W * 0.03f), (gTop + gBot) / 2, pillH * 0.62f * r.s2, 0xFF141414.toInt(), if (ar) r.calliFont else r.sansBold, pw * 0.86f, pillH * 0.66f)
        val d = Kit.day(r)
        val prW = W * 0.8f
        val prX = if (ar) px0 - W * 0.06f else px0 + W * 0.06f + pw - prW
        slab(prTop, prBot, prX, prW, RenderCtx.mix(acc, Color.WHITE, 0.12f), RenderCtx.mix(acc, Color.BLACK, 0.45f))
        slText(Kit.pName(r, d.next), prX + prW / 2, prTop + prH * 0.32f, prH * 0.36f, ink, if (ar) r.calliFont else r.sansBold, prW * 0.8f, prH * 0.36f)
        slText(Kit.pTime(r, d.next.time), prX + prW / 2, prTop + prH * 0.72f, prH * 0.34f, ink, F.CLK_BEBAS, prW * 0.8f, prH * 0.32f)
        // progress
        val bx0 = prX; val bx1 = prX + prW
        c.drawLine(bx0 + barH, barY, bx1 - barH, barY, r.strokePaint(withAlpha(ink, 60), barH))
        val f = d.progress.coerceIn(0f, 1f); val len = (bx1 - bx0 - 2 * barH) * f
        if (ar) c.drawLine(bx1 - barH, barY, bx1 - barH - len, barY, r.strokePaint(ink, barH))
        else c.drawLine(bx0 + barH, barY, bx0 + barH + len, barY, r.strokePaint(ink, barH))
        // ---- the day, standing up (reads bottom → top), 3D with an accent extrusion
        val top = ty + r.h * 0.03f; val bot = hTop - gap * 1.2f
        val batW = W * 0.2f
        val dayL = if (ar) L + batW else L; val dayR = if (ar) R else R - batW
        val dcx = (dayL + dayR) / 2; val dcy = (top + bot) / 2
        val day = Calendars.dayName(r.today, ar)
        val font = if (ar) r.calliFont else F.PLAYFAIR
        val len2 = bot - top; val thick = dayR - dayL
        c.save(); c.rotate(-90f, dcx, dcy)
        for (k in 5 downTo 1) {
            val o = k * 0.8f * r.u
            r.textTight(day, dcx - o, dcy + o, thick * 1.2f * r.s1, RenderCtx.mix(acc, Color.BLACK, k / 12f), font, maxW = len2 * 0.98f, maxH = thick * 0.98f)
        }
        r.textTight(day, dcx, dcy, thick * 1.2f * r.s1, ink, font, maxW = len2 * 0.98f, maxH = thick * 0.98f)
        c.restore()
        // ---- battery: thin standing bar with a bolt and the % turned sideways
        val bat = Kit.battery(r)
        val bx = if (ar) L + batW * 0.62f else R - batW * 0.62f
        val bTop = top + len2 * 0.32f; val bBot = bot - len2 * 0.14f
        c.drawLine(bx, bTop, bx, bBot, r.strokePaint(withAlpha(ink, 70), 3.2f * r.u))
        c.drawLine(bx, bBot, bx, bBot - (bBot - bTop) * bat / 100f, r.strokePaint(acc, 3.2f * r.u))
        val bolt = android.graphics.Path().apply {
            val s0 = 5 * r.u; val ox = bx - (if (ar) 9 else -9) * r.u; val oy = bBot - (bBot - bTop) * 0.42f
            moveTo(ox + s0 * 0.2f, oy - s0); lineTo(ox - s0 * 0.5f, oy + s0 * 0.1f); lineTo(ox, oy + s0 * 0.1f)
            lineTo(ox - s0 * 0.2f, oy + s0); lineTo(ox + s0 * 0.5f, oy - s0 * 0.1f); lineTo(ox, oy - s0 * 0.1f); close()
        }
        c.drawPath(bolt, r.fillPaint(ink))
        c.save(); val pcx = bx - (if (ar) 9 else -9) * r.u; val pcy = bBot - (bBot - bTop) * 0.12f
        c.rotate(-90f, pcx, pcy)
        r.text(r.n("$bat%"), pcx, pcy, 9 * r.u * r.s2, acc, F.CLK_BEBAS, Paint.Align.CENTER)
        c.restore()
    }

    @Suppress("unused") private fun keep() = min(1, 2)
}
