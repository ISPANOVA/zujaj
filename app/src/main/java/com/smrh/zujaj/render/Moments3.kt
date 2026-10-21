package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.Prayer
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha

/**
 * Two complete home-screen boards (4×5):
 *  · Rose glass board — next prayer + weather, the day's prayer times, and a music player, each card with a round notch.
 *  · Dark calligraphy board — day, months, next prayer and the prayer row on dark cards, headings in decorative script.
 * Typography: decorative script only for big Arabic headings (r.calliFont); everything informational uses r.sans*.
 */
object Moments3R {
    // ------------------------------------------------------------------ shared helpers
    private fun nowMs(r: RenderCtx) = r.now.toInstant().toEpochMilli()

    /** "1 س 41 د" / "1h 41m" until the next prayer. */
    fun left(r: RenderCtx, p: Prayer): String {
        val m = ((p.time - nowMs(r)) / 60_000L).coerceAtLeast(0)
        val h = m / 60; val mm = m % 60
        return if (r.ar) (if (h > 0) "${r.n(h.toInt())} س ${r.n(mm.toInt())} د" else "${r.n(mm.toInt())} د") else (if (h > 0) "${h}h ${mm}m" else "${mm}m")
    }

    /** Prayer time with ص/م (AM/PM) when the clock is 12-hour. */
    private fun timeAmPm(r: RenderCtx, p: Prayer): String {
        val t = Kit.pTime(r, p.time)
        if (r.cfg.hour24) return t
        val pm = java.util.Calendar.getInstance().apply { timeInMillis = p.time }.get(java.util.Calendar.AM_PM) == java.util.Calendar.PM
        return if (r.ar) "$t ${if (pm) "م" else "ص"}" else "$t ${if (pm) "PM" else "AM"}"
    }

    private fun gregDay(r: RenderCtx): String {
        val d = r.today
        return if (r.ar) "${Calendars.dayName(d, true)} ${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)}"
        else "${Calendars.dayName(d, false)}, ${d.dayOfMonth} ${Calendars.monthName(d.monthValue, false)}"
    }

    private fun hijriFull(r: RenderCtx): String {
        val h = Calendars.hijri(r.today)
        return if (r.ar) "${r.n(h.day)} ${Calendars.hijriMonth(h.month, true)} ${r.n(h.year)} هـ" else "${h.day} ${Calendars.hijriMonth(h.month, false)} ${h.year} AH"
    }

    // =================================================================== 1 · rose glass board
    /** A card with a round bite taken out of its top edge, filled with the card gradient. */
    /** The card body follows the editor: background on/off, fill (solid · gradient · glass · aurora), colours and opacity. */
    private fun bodyPaint(r: RenderCtx, rc: RectF): Paint? {
        val c = r.cfg
        if (!c.bg || c.fill == com.smrh.zujaj.model.Fill.NONE) return null
        val a = c.bgAlpha.coerceIn(0, 100) * 255 / 100
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            when (c.fill) {
                com.smrh.zujaj.model.Fill.SOLID -> color = withAlpha(c.bgColor, a)
                com.smrh.zujaj.model.Fill.GLASS -> shader = LinearGradient(rc.left, rc.top, rc.right, rc.bottom,
                    withAlpha(RenderCtx.mix(c.bgColor, Color.WHITE, 0.55f), a * 45 / 100), withAlpha(c.bgColor, a * 30 / 100), Shader.TileMode.CLAMP)
                else -> shader = LinearGradient(rc.left, rc.top, rc.right, rc.bottom, withAlpha(c.bgColor, a), withAlpha(c.bgColor2, a), Shader.TileMode.CLAMP)
            }
        }
    }

    private fun notchedCard(r: RenderCtx, rc: RectF, rad: Float, notchX: Float, notchY: Float, notchR: Float, c1: Int, c2: Int) {
        val card = Path().apply { addRoundRect(rc, rad, rad, Path.Direction.CW) }
        val bite = Path().apply { addCircle(notchX, notchY, notchR, Path.Direction.CW) }
        card.op(bite, Path.Op.DIFFERENCE)
        val body = bodyPaint(r, rc) ?: return   // background turned off: only the content shows
        // soft shadow, then the body, then a faint light edge on top
        r.canvas.drawPath(card, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.BLACK, 70); maskFilter = android.graphics.BlurMaskFilter(6 * r.u, android.graphics.BlurMaskFilter.Blur.NORMAL)
        })
        r.canvas.drawPath(card, body)
        r.canvas.drawPath(card, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1f * r.u
            shader = LinearGradient(0f, rc.top, 0f, rc.bottom, withAlpha(Color.WHITE, 60), withAlpha(Color.WHITE, 8), Shader.TileMode.CLAMP)
        })
    }

    /** The little round bubble that sits in a notch. */
    private fun bubble(r: RenderCtx, cx: Float, cy: Float, rad: Float, c: Int) {
        r.canvas.drawCircle(cx, cy + 1.5f * r.u, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.BLACK, 80); maskFilter = android.graphics.BlurMaskFilter(4 * r.u, android.graphics.BlurMaskFilter.Blur.NORMAL)
        })
        r.canvas.drawCircle(cx, cy, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx, cy - rad, cx, cy + rad, RenderCtx.mix(c, Color.WHITE, 0.12f), RenderCtx.mix(c, Color.BLACK, 0.25f), Shader.TileMode.CLAMP)
        })
    }

    fun roseBoard(r: RenderCtx) {
        val ar = r.ar
        val u = r.u
        val c1 = r.cfg.bgColor; val c2 = r.cfg.bgColor2
        val bub = r.accent
        val ink = r.c1; val sub = withAlpha(r.c1, 200)
        val W = r.w; val H = r.h
        val gap = 10 * u; val rad = 22 * u
        val R = 21 * u; val bite = R + 6 * u
        val d = Kit.day(r)
        val nx = d.next
        val w = Kit.weather(r)
        val y0 = R * 0.5f
        val topH = H * 0.30f
        val midTop = y0 + topH + gap + R * 0.85f
        val midH = H * 0.28f
        val botTop = midTop + midH + gap + R * 0.85f
        val half = (W - gap) / 2f
        // in Arabic the weather sits on the right, the prayer on the left (like the reference)
        val weatherRc = if (ar) RectF(half + gap, y0, W, y0 + topH) else RectF(0f, y0, half, y0 + topH)
        val prayerRc = if (ar) RectF(0f, y0, half, y0 + topH) else RectF(half + gap, y0, W, y0 + topH)
        val pad = 16 * u

        // ---- weather card
        val wnX = if (ar) weatherRc.left + R + 12 * u else weatherRc.right - R - 12 * u
        notchedCard(r, weatherRc, rad, wnX, weatherRc.top + R * 0.55f, bite, c1, c2)
        bubble(r, wnX, weatherRc.top + R * 0.55f, R, bub)
        Glyphs.weather(r.canvas, w.cond, w.night, wnX, weatherRc.top + R * 0.55f, R * 1.15f, color = false)
        val wx = if (ar) weatherRc.right - pad else weatherRc.left + pad
        val wal = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        r.text(w.city.ifBlank { r.t("My city", "مدينتي") }, wx, weatherRc.top + 22 * u, 12 * u * r.s2, sub, r.sans, wal, maxW = half - 2 * R - 40 * u)
        r.text(Kit.temp(r, w.temp), wx, weatherRc.top + topH * 0.55f, 34 * u * r.s1, ink, r.sansBold, wal)
        r.text(WeatherRepo.condName(w.cond, ar), wx, weatherRc.top + topH * 0.75f, 13 * u * r.s2, ink, r.sansMed, wal, maxW = half - 2 * pad)
        r.text(Kit.temp(r, w.lo) + " / " + Kit.temp(r, w.hi), wx, weatherRc.top + topH * 0.89f, 11 * u * r.s2, sub, r.sans, wal)

        // ---- next prayer card
        val pnX = if (ar) prayerRc.right - R - 12 * u else prayerRc.left + R + 12 * u
        notchedCard(r, prayerRc, rad, pnX, prayerRc.top + R * 0.55f, bite, c1, c2)
        bubble(r, pnX, prayerRc.top + R * 0.55f, R, bub)
        Kit.prayerIcon(r, nx.key, pnX, prayerRc.top + R * 0.55f, R * 0.55f, Color.WHITE)
        val outerX = if (ar) prayerRc.left + pad else prayerRc.right - pad
        val oal = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        r.text(r.t("Next prayer", "الصلاة القادمة"), outerX, prayerRc.top + 22 * u, 11 * u * r.s2, sub, r.sans, oal, maxW = half - 2 * R - 40 * u)
        val px = if (ar) prayerRc.right - pad else prayerRc.left + pad
        val pal = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        r.text(Kit.pName(r, nx), px, prayerRc.top + topH * 0.55f, 24 * u * r.s1, ink, r.sansBold, pal, maxW = half - 2 * pad)
        r.text(timeAmPm(r, nx), px, prayerRc.top + topH * 0.75f, 16 * u * r.s2, ink, r.sansBold, pal)
        r.text(r.t("in ", "بعد ") + left(r, nx), px, prayerRc.top + topH * 0.89f, 11 * u * r.s2, sub, r.sans, pal)

        // ---- prayer times card
        val mid = RectF(0f, midTop, W, midTop + midH)
        val mnY = mid.top + R * 0.15f
        notchedCard(r, mid, rad, W / 2, mnY, bite, c1, c2)
        bubble(r, W / 2, mnY, R, bub)
        Kit.prayerIcon(r, nx.key, W / 2, mnY, R * 0.55f, Color.WHITE)
        val ix = if (ar) mid.right - pad else mid.left + pad
        val ox = if (ar) mid.left + pad else mid.right - pad
        val ial = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val oal2 = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        r.text(gregDay(r), ix, mid.top + 24 * u, 14 * u * r.s1, ink, r.sansBold, ial, maxW = W / 2 - R - 2 * pad)
        r.text(hijriFull(r), ix, mid.top + 42 * u, 11 * u * r.s2, sub, r.sans, ial, maxW = W / 2 - R - 2 * pad)
        r.text(Kit.pName(r, nx), ox, mid.top + 24 * u, 14 * u * r.s1, ink, r.sansBold, oal2, maxW = W / 2 - R - 2 * pad)
        r.text(r.t("in ", "بعد ") + left(r, nx), ox, mid.top + 42 * u, 11 * u * r.s2, sub, r.sans, oal2)
        val list = Kit.ordered(r, d.list)
        val cw = (W - 2 * pad) / list.size
        val rowName = mid.top + midH * 0.56f; val rowIcon = mid.top + midH * 0.71f; val rowTime = mid.top + midH * 0.86f
        list.forEachIndexed { i, p ->
            val cx = pad + cw * (i + 0.5f)
            val on = p.key == nx.key
            val col = if (on) ink else withAlpha(ink, 170)
            r.text(Kit.pName(r, p), cx, rowName, 11 * u * r.s2, col, if (on) r.sansBold else r.sans, Paint.Align.CENTER, maxW = cw - 4 * u)
            Kit.prayerIcon(r, p.key, cx, rowIcon, 7.5f * u, col)
            r.text(Kit.pTime(r, p.time), cx, rowTime, 12.5f * u * r.s2, col, if (on) r.sansBold else r.sans, Paint.Align.CENTER, maxW = cw - 2 * u)
            if (on) {
                val ty = rowTime + 11 * u
                val tri = Path().apply { moveTo(cx - 3.5f * u, ty + 3 * u); lineTo(cx + 3.5f * u, ty + 3 * u); lineTo(cx, ty - 2 * u); close() }
                r.canvas.drawPath(tri, r.fillPaint(ink))
            }
        }

        // ---- music card
        val bot = RectF(0f, botTop, W, H)
        val bnY = bot.top + R * 0.15f
        notchedCard(r, bot, rad, W / 2, bnY, bite, c1, c2)
        val m = MediaR.st(r)
        // album art in the notch, with a small play mark
        val artRc = RectF(W / 2 - R, bnY - R, W / 2 + R, bnY + R)
        bubble(r, W / 2, bnY, R, bub)
        MediaR.art(r, m, artRc, R)
        r.circle(W / 2, bnY, R, withAlpha(Color.BLACK, 70))
        Glyphs.playPause(r.canvas, m.playing, W / 2 + if (m.playing) 0f else R * 0.06f, bnY, R * 0.7f, Color.WHITE)
        MediaR.openHit(r, artRc)
        val bh = bot.height()
        r.text(MediaR.title(r, m), ix, bot.top + 24 * u, 14 * u * r.s1, ink, r.sansBold, ial, maxW = W / 2 - R - 2 * pad)
        r.text(MediaR.artist(r, m), ix, bot.top + 42 * u, 11.5f * u * r.s2, sub, r.sans, ial, maxW = W / 2 - R - 2 * pad)
        r.text(MediaR.time(r, m.position), ox, bot.top + 24 * u, 14 * u * r.s1, ink, r.sansBold, oal2)
        r.text(MediaR.time(r, m.duration), ox, bot.top + 42 * u, 11.5f * u * r.s2, sub, r.sans, oal2)
        // progress (fills from the reading start)
        val py = bot.top + bh * 0.60f
        val pl = bot.left + pad; val pr = bot.right - pad
        r.canvas.drawLine(pl, py, pr, py, r.strokePaint(withAlpha(ink, 60), 3 * u))
        val pf = (pr - pl) * m.progress
        r.canvas.drawLine(pl, py, pl + pf, py, r.strokePaint(ink, 3 * u)) // media always runs left → right
        // controls: skip · white play pill · skip
        val cy = bot.top + bh * 0.82f
        val pillW = 44 * u; val pillH = 28 * u
        val pill = RectF(W / 2 - pillW / 2, cy - pillH / 2, W / 2 + pillW / 2, cy + pillH / 2)
        r.canvas.drawRoundRect(pill, 11 * u, 11 * u, r.fillPaint(Color.WHITE))
        Glyphs.playPause(r.canvas, m.playing, W / 2 + if (m.playing) 0f else 1.5f * u, cy, 13 * u, RenderCtx.mix(c1, Color.BLACK, 0.55f))
        r.hit(RectF(pill.left - 6 * u, pill.top - 6 * u, pill.right + 6 * u, pill.bottom + 6 * u), MediaR.cmd("play_pause"))
        MediaR.btn(r, "prev", W / 2 - 50 * u, cy, 15 * u, m, filled = false, glyphColor = ink)
        MediaR.btn(r, "next", W / 2 + 50 * u, cy, 15 * u, m, filled = false, glyphColor = ink)
    }

    // =================================================================== 2 · dark calligraphy board
    private fun darkCard(r: RenderCtx, rc: RectF, lit: Boolean = false) {
        val rad = 18 * r.u
        val base = r.cfg.bgColor
        r.canvas.drawRoundRect(rc, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, rc.top, 0f, rc.bottom,
                if (lit) RenderCtx.mix(base, Color.WHITE, 0.22f) else RenderCtx.mix(base, Color.WHITE, 0.05f),
                if (lit) RenderCtx.mix(base, Color.WHITE, 0.14f) else base, Shader.TileMode.CLAMP)
        })
        r.canvas.drawRoundRect(rc, rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 0.8f * r.u; color = withAlpha(Color.WHITE, 22)
        })
    }

    private fun pill(r: RenderCtx, s: String, cx: Float, cy: Float, size: Float) {
        val p = r.paint(r.c1, size, r.sansMed)
        val w = p.measureText(s) + size * 1.4f; val h = size * 1.7f
        val rc = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        r.canvas.drawRoundRect(rc, h / 2, h / 2, r.fillPaint(withAlpha(r.c1, 34)))
        r.text(s, cx, cy, size, r.c1, r.sansMed, Paint.Align.CENTER)
    }

    fun darkBoard(r: RenderCtx) {
        val ar = r.ar
        val u = r.u
        val W = r.w; val H = r.h
        val g = 8 * u
        val ink = r.c1; val sub = r.c2
        val deco = r.calliFont
        val d = Kit.day(r)
        val nx = d.next
        val hd = Calendars.hijri(r.today)
        val greg = r.today
        // rows
        val aH = H * 0.27f; val bH = H * 0.16f; val cH = H * 0.18f; val dH = H * 0.17f
        val aTop = 0f; val bTop = aTop + aH + g; val cTop = bTop + bH + g; val dTop = cTop + cH + g; val eTop = dTop + dH + g
        fun xs(l: Float, rr: Float) = if (ar) RectF(W - rr, 0f, W - l, 0f) else RectF(l, 0f, rr, 0f) // mirrors a horizontal span

        // ---- A: the day in big decorative script, months and years under a thin rule
        val a = RectF(0f, aTop, W, aTop + aH)
        darkCard(r, a)
        r.textTight(Calendars.dayName(r.today, ar), W / 2, a.top + aH * 0.38f, aH * 0.62f * r.s1, ink, if (ar) deco else F.PLAYFAIR, maxW = W - 44 * u, maxH = aH * 0.58f)
        r.canvas.drawLine(W * 0.16f, a.top + aH * 0.73f, W * 0.84f, a.top + aH * 0.73f, r.strokePaint(withAlpha(ink, 40), 0.8f * u))
        val l1 = if (ar) "${Calendars.hijriMonth(hd.month, true)} • ${r.n(hd.year)}" else "${Calendars.hijriMonth(hd.month, false)} • ${hd.year}"
        val l2 = if (ar) "${Calendars.monthName(greg.monthValue, true)} • ${r.n(greg.year)}" else "${Calendars.monthName(greg.monthValue, false)} • ${greg.year}"
        r.text(l1, W / 2, a.top + aH * 0.835f, 11f * u * r.s2, ink, r.sansBold, Paint.Align.CENTER)
        r.text(l2, W / 2, a.top + aH * 0.945f, 11f * u * r.s2, sub, r.sansBold, Paint.Align.CENTER)

        // ---- B: time of the next prayer + countdown | its name in script, with a progress line under both
        val bSmall = xs(0f, W * 0.42f); val bBig = xs(W * 0.42f + g, W)
        val b1 = RectF(bSmall.left, bTop, bSmall.right, bTop + bH); val b2 = RectF(bBig.left, bTop, bBig.right, bTop + bH)
        darkCard(r, b1); darkCard(r, b2)
        val inX = if (ar) b1.right - 14 * u else b1.left + 14 * u
        r.text(r.t("in", "بعد"), inX, b1.top + 13 * u, 9.5f * u * r.s2, sub, r.sans, if (ar) Paint.Align.RIGHT else Paint.Align.LEFT)
        r.textTight(Kit.pTime(r, nx.time), b1.centerX(), b1.top + bH * 0.45f, bH * 0.36f * r.s1, ink, r.sansBold, maxW = b1.width() - 24 * u)
        pill(r, left(r, nx), b1.centerX(), b1.top + bH * 0.78f, 10 * u * r.s2)
        r.textTight(Kit.pName(r, nx), b2.centerX(), b2.top + bH * 0.44f, bH * 0.6f * r.s1, ink, if (ar) deco else F.PLAYFAIR, maxW = b2.width() - 30 * u, maxH = bH * 0.62f)
        // progress since the last prayer, drawn under the script
        val ly = b2.bottom - 12 * u
        val lx0 = b2.left + 16 * u; val lx1 = b2.right - 16 * u
        r.canvas.drawLine(lx0, ly, lx1, ly, r.strokePaint(withAlpha(ink, 50), 2 * u))
        val f = d.progress.coerceIn(0f, 1f)
        val dotX = if (ar) lx1 - (lx1 - lx0) * f else lx0 + (lx1 - lx0) * f
        if (ar) r.canvas.drawLine(lx1, ly, dotX, ly, r.strokePaint(ink, 2 * u)) else r.canvas.drawLine(lx0, ly, dotX, ly, r.strokePaint(ink, 2 * u))
        r.circle(dotX, ly, 3.5f * u, ink)

        // ---- C: today's date — huge faint number behind the month in script
        val c = RectF(0f, cTop, W, cTop + cH)
        darkCard(r, c)
        r.textTight(r.n(greg.dayOfMonth), W / 2, c.centerY(), cH * 0.9f, withAlpha(ink, 46), r.sansBold, maxH = cH * 0.78f)
        r.textTight(Calendars.monthName(greg.monthValue, ar), W / 2, c.centerY(), cH * 0.7f * r.s1, ink, if (ar) deco else F.PLAYFAIR, maxW = W - 60 * u, maxH = cH * 0.72f)

        // ---- D: hijri month | next prayer | day + temperature
        val sideW = W * 0.29f
        val dHij = xs(0f, sideW); val dMid = xs(sideW + g, W - sideW - g); val dDay = xs(W - sideW, W)
        val r1 = RectF(dHij.left, dTop, dHij.right, dTop + dH); val r2 = RectF(dMid.left, dTop, dMid.right, dTop + dH); val r3 = RectF(dDay.left, dTop, dDay.right, dTop + dH)
        darkCard(r, r1); darkCard(r, r2); darkCard(r, r3)
        r.text(r.n(hd.year), r1.centerX(), r1.top + dH * 0.18f, 10 * u * r.s2, sub, r.sansBold, Paint.Align.CENTER)
        r.textTight(Calendars.hijriMonth(hd.month, ar), r1.centerX(), r1.top + dH * 0.52f, dH * 0.42f * r.s1, ink, if (ar) deco else F.PLAYFAIR, maxW = r1.width() - 14 * u, maxH = dH * 0.42f)
        r.text(r.n(hd.day), r1.centerX(), r1.top + dH * 0.85f, 11 * u * r.s2, ink, r.sansBold, Paint.Align.CENTER)
        r.text(Kit.pName(r, nx), r2.centerX(), r2.top + dH * 0.2f, 12 * u * r.s2, ink, r.sansBold, Paint.Align.CENTER)
        r.textTight(Kit.pTime(r, nx.time), r2.centerX(), r2.top + dH * 0.48f, dH * 0.3f * r.s1, ink, r.sansBold, maxW = r2.width() - 20 * u)
        pill(r, left(r, nx), r2.centerX(), r2.top + dH * 0.8f, 9.5f * u * r.s2)
        val w = Kit.weather(r)
        val tx = if (ar) r3.left + 12 * u else r3.right - 12 * u
        r.text(Kit.temp(r, w.temp) + r.t("", "م"), tx, r3.top + dH * 0.2f, 10 * u * r.s2, ink, r.sansBold, if (ar) Paint.Align.LEFT else Paint.Align.RIGHT)
        r.textTight(Calendars.dayName(r.today, ar), r3.centerX(), r3.top + dH * 0.6f, dH * 0.42f * r.s1, ink, if (ar) deco else F.PLAYFAIR, maxW = r3.width() - 14 * u, maxH = dH * 0.46f)

        // ---- E: the five prayers, the next one lit
        val five = Kit.ordered(r, d.five)
        val eH = H - eTop
        val cw = (W - g * (five.size - 1)) / five.size
        five.forEachIndexed { i, p ->
            val x0 = i * (cw + g)
            val rc = RectF(x0, eTop, x0 + cw, eTop + eH)
            val on = p.key == nx.key
            darkCard(r, rc, lit = on)
            r.text(Kit.pName(r, p), rc.centerX(), rc.top + eH * 0.33f, 9.5f * u * r.s2, if (on) ink else sub, r.sans, Paint.Align.CENTER, maxW = cw - 6 * u)
            r.text(Kit.pTime(r, p.time), rc.centerX(), rc.top + eH * 0.68f, 12 * u * r.s2, ink, r.sansBold, Paint.Align.CENTER, maxW = cw - 4 * u)
        }
    }

    // =================================================================== separate widgets (as in the references)
    private fun mirror(r: RenderCtx, x: Float) = if (r.ar) x else r.w - x

    // ---------------------------------------------------- rose · next prayer (small)
    fun roseNext(r: RenderCtx) {
        val u = r.u; val R = 19 * u; val ar = r.ar
        val card = RectF(0f, R * 0.45f, r.w, r.h)
        val nx = r.w - R - 10 * u
        val bx = if (ar) nx else r.w - nx
        val by = card.top + R * 0.55f
        notchedCard(r, card, 22 * u, bx, by, R + 5 * u, r.cfg.bgColor, r.cfg.bgColor2)
        bubble(r, bx, by, R, r.accent)
        val d = Kit.day(r); val p = d.next
        Kit.prayerIcon(r, p.key, bx, by, R * 0.5f, Color.WHITE)
        val pad = 14 * u
        val lx = if (ar) pad else r.w - pad
        r.text(r.t("Next prayer", "الصلاة القادمة"), lx, card.top + 18 * u, 10 * u * r.s2, withAlpha(r.c1, 200), r.sans, if (ar) Paint.Align.LEFT else Paint.Align.RIGHT, maxW = r.w - 2 * R - 34 * u)
        val tx = if (ar) r.w - pad else pad
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val h = card.height()
        r.text(Kit.pName(r, p), tx, card.top + h * 0.56f, 22 * u * r.s1, r.c1, r.sansBold, al, maxW = r.w - 2 * pad)
        r.text(timeAmPm(r, p), tx, card.top + h * 0.75f, 15 * u * r.s2, r.c1, r.sansBold, al)
        r.text(r.t("in ", "بعد ") + left(r, p), tx, card.top + h * 0.89f, 10.5f * u * r.s2, withAlpha(r.c1, 200), r.sans, al)
    }

    // ---------------------------------------------------- rose · weather (small)
    fun roseWeather(r: RenderCtx) {
        val u = r.u; val R = 19 * u; val ar = r.ar
        val card = RectF(0f, R * 0.45f, r.w, r.h)
        val bx = if (ar) R + 10 * u else r.w - R - 10 * u
        val by = card.top + R * 0.55f
        notchedCard(r, card, 22 * u, bx, by, R + 5 * u, r.cfg.bgColor, r.cfg.bgColor2)
        bubble(r, bx, by, R, r.accent)
        val w = Kit.weather(r)
        Glyphs.weather(r.canvas, w.cond, w.night, bx, by, R * 1.1f, color = false)
        val pad = 14 * u
        val tx = if (ar) r.w - pad else pad
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val h = card.height()
        r.text(w.city.ifBlank { r.t("My city", "مدينتي") }, tx, card.top + 18 * u, 11 * u * r.s2, withAlpha(r.c1, 200), r.sans, al, maxW = r.w - 2 * R - 34 * u)
        r.text(Kit.temp(r, w.temp), tx, card.top + h * 0.56f, 32 * u * r.s1, r.c1, r.sansBold, al)
        r.text(WeatherRepo.condName(w.cond, ar), tx, card.top + h * 0.75f, 12 * u * r.s2, r.c1, r.sansMed, al, maxW = r.w - 2 * pad)
        r.text(Kit.temp(r, w.lo) + " / " + Kit.temp(r, w.hi), tx, card.top + h * 0.89f, 10.5f * u * r.s2, withAlpha(r.c1, 200), r.sans, al)
    }

    // ---------------------------------------------------- rose · prayer times (wide)
    fun rosePrayers(r: RenderCtx) {
        val u = r.u; val R = 18 * u; val ar = r.ar
        val card = RectF(0f, R * 0.95f, r.w, r.h)
        val by = card.top + R * 0.05f
        notchedCard(r, card, 22 * u, r.w / 2, by, R + 5 * u, r.cfg.bgColor, r.cfg.bgColor2)
        bubble(r, r.w / 2, by, R, r.accent)
        val d = Kit.day(r); val nx = d.next
        Kit.prayerIcon(r, nx.key, r.w / 2, by, R * 0.5f, Color.WHITE)
        val pad = 16 * u; val h = card.height()
        val ix = if (ar) r.w - pad else pad; val ox = if (ar) pad else r.w - pad
        val ial = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT; val oal = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        val sub = withAlpha(r.c1, 200)
        r.text(gregDay(r), ix, card.top + h * 0.17f, 13 * u * r.s1, r.c1, r.sansBold, ial, maxW = r.w / 2 - R - 2 * pad)
        r.text(hijriFull(r), ix, card.top + h * 0.33f, 10 * u * r.s2, sub, r.sans, ial, maxW = r.w / 2 - R - 2 * pad)
        r.text(Kit.pName(r, nx), ox, card.top + h * 0.17f, 13 * u * r.s1, r.c1, r.sansBold, oal)
        r.text(r.t("in ", "بعد ") + left(r, nx), ox, card.top + h * 0.33f, 10 * u * r.s2, sub, r.sans, oal)
        val list = Kit.ordered(r, d.list)
        val cw = (r.w - 2 * pad) / list.size
        list.forEachIndexed { i, p ->
            val cx = pad + cw * (i + 0.5f); val on = p.key == nx.key
            val col = if (on) r.c1 else withAlpha(r.c1, 175)
            r.text(Kit.pName(r, p), cx, card.top + h * 0.55f, 10.5f * u * r.s2, col, if (on) r.sansBold else r.sans, Paint.Align.CENTER, maxW = cw - 4 * u)
            Kit.prayerIcon(r, p.key, cx, card.top + h * 0.70f, 7 * u, col)
            r.text(Kit.pTime(r, p.time), cx, card.top + h * 0.85f, 12 * u * r.s2, col, if (on) r.sansBold else r.sans, Paint.Align.CENTER, maxW = cw - 2 * u)
            if (on) {
                val ty = card.top + h * 0.95f
                r.canvas.drawPath(Path().apply { moveTo(cx - 3.5f * u, ty + 2.5f * u); lineTo(cx + 3.5f * u, ty + 2.5f * u); lineTo(cx, ty - 2.5f * u); close() }, r.fillPaint(r.c1))
            }
        }
    }

    // ---------------------------------------------------- rose · music (wide)
    fun roseMusic(r: RenderCtx) {
        val u = r.u; val R = 18 * u; val ar = r.ar
        val card = RectF(0f, R * 0.95f, r.w, r.h)
        val by = card.top + R * 0.05f
        val W = r.w
        notchedCard(r, card, 22 * u, W / 2, by, R + 5 * u, r.cfg.bgColor, r.cfg.bgColor2)
        val m = MediaR.st(r)
        val artRc = RectF(W / 2 - R, by - R, W / 2 + R, by + R)
        bubble(r, W / 2, by, R, r.accent)
        MediaR.art(r, m, artRc, R)
        r.circle(W / 2, by, R, withAlpha(Color.BLACK, 70))
        Glyphs.playPause(r.canvas, m.playing, W / 2 + if (m.playing) 0f else R * 0.06f, by, R * 0.7f, Color.WHITE)
        MediaR.openHit(r, artRc)
        val pad = 16 * u; val h = card.height()
        val ix = if (ar) W - pad else pad; val ox = if (ar) pad else W - pad
        val ial = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT; val oal = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        val sub = withAlpha(r.c1, 200)
        r.text(MediaR.title(r, m), ix, card.top + h * 0.16f, 13 * u * r.s1, r.c1, r.sansBold, ial, maxW = W / 2 - R - 2 * pad)
        r.text(MediaR.artist(r, m), ix, card.top + h * 0.32f, 10.5f * u * r.s2, sub, r.sans, ial, maxW = W / 2 - R - 2 * pad)
        r.text(MediaR.time(r, m.position), ox, card.top + h * 0.16f, 13 * u * r.s1, r.c1, r.sansBold, oal)
        r.text(MediaR.time(r, m.duration), ox, card.top + h * 0.32f, 10.5f * u * r.s2, sub, r.sans, oal)
        val py = card.top + h * 0.56f
        r.canvas.drawLine(pad, py, W - pad, py, r.strokePaint(withAlpha(r.c1, 60), 3 * u))
        r.canvas.drawLine(pad, py, pad + (W - 2 * pad) * m.progress, py, r.strokePaint(r.c1, 3 * u))
        val cy = card.top + h * 0.80f
        val pill = RectF(W / 2 - 22 * u, cy - 14 * u, W / 2 + 22 * u, cy + 14 * u)
        r.canvas.drawRoundRect(pill, 11 * u, 11 * u, r.fillPaint(Color.WHITE))
        Glyphs.playPause(r.canvas, m.playing, W / 2 + if (m.playing) 0f else 1.5f * u, cy, 13 * u, RenderCtx.mix(r.cfg.bgColor, Color.BLACK, 0.55f))
        r.hit(RectF(pill.left - 6 * u, pill.top - 6 * u, pill.right + 6 * u, pill.bottom + 6 * u), MediaR.cmd("play_pause"))
        MediaR.btn(r, "prev", W / 2 - 50 * u, cy, 15 * u, m, filled = false, glyphColor = r.c1)
        MediaR.btn(r, "next", W / 2 + 50 * u, cy, 15 * u, m, filled = false, glyphColor = r.c1)
    }

    // =================================================================== dark calligraphy set
    private fun plate(r: RenderCtx, rc: RectF, rad: Float = 20 * r.u) {
        val body = bodyPaint(r, rc) ?: return
        val rr = if (r.cfg.corner > 0) minOf(r.cfg.corner * r.u, rad * 1.6f) else rad
        r.canvas.drawRoundRect(rc, rr, rr, body)
        r.canvas.drawRoundRect(rc, rr, rr, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f * r.u; color = withAlpha(Color.WHITE, 18) })
    }

    // ---------------------------------------------------- 1 · day + months (wide)
    fun darkDay(r: RenderCtx) {
        val u = r.u; val ar = r.ar
        val c = RectF(0f, 0f, r.w, r.h)
        plate(r, c)
        val h = r.h
        r.textTight(Calendars.dayName(r.today, ar), r.w / 2, h * 0.38f, h * 0.62f * r.s1, r.c1, if (ar) r.calliFont else F.PLAYFAIR, maxW = r.w - 50 * u, maxH = h * 0.56f)
        r.canvas.drawLine(r.w * 0.18f, h * 0.72f, r.w * 0.82f, h * 0.72f, r.strokePaint(withAlpha(r.c1, 40), 0.8f * u))
        val hd = Calendars.hijri(r.today); val g = r.today
        val l1 = if (ar) "${Calendars.hijriMonth(hd.month, true)} • ${r.n(hd.year)}" else "${Calendars.hijriMonth(hd.month, false)} • ${hd.year}"
        val l2 = if (ar) "${Calendars.monthName(g.monthValue, true)} • ${r.n(g.year)}" else "${Calendars.monthName(g.monthValue, false)} • ${g.year}"
        r.text(l1, r.w / 2, h * 0.81f, 11 * u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(l2, r.w / 2, h * 0.92f, 11 * u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER)
    }

    // ---------------------------------------------------- 2 · next prayer + big date (large, one piece)
    fun darkPrayerDate(r: RenderCtx) {
        val u = r.u; val ar = r.ar
        val W = r.w; val H = r.h
        plate(r, RectF(0f, 0f, W, H), 22 * u)
        val d = Kit.day(r); val nx = d.next
        val topH = H * 0.46f
        // time block on the reading-end side, a slightly lifted panel
        val bw = W * 0.38f
        val blk = if (ar) RectF(10 * u, 10 * u, 10 * u + bw, topH - 6 * u) else RectF(W - 10 * u - bw, 10 * u, W - 10 * u, topH - 6 * u)
        r.canvas.drawRoundRect(blk, 16 * u, 16 * u, r.fillPaint(withAlpha(Color.WHITE, 14)))
        val inX = if (ar) blk.right - 12 * u else blk.left + 12 * u
        r.text(r.t("in", "بعد"), inX, blk.top + 12 * u, 9 * u * r.s2, r.c2, r.sans, if (ar) Paint.Align.RIGHT else Paint.Align.LEFT)
        r.textTight(Kit.pTime(r, nx.time), blk.centerX(), blk.top + blk.height() * 0.48f, blk.height() * 0.34f * r.s1, r.c1, r.sansBold, maxW = blk.width() - 20 * u)
        pill(r, left(r, nx), blk.centerX(), blk.top + blk.height() * 0.8f, 9.5f * u * r.s2)
        val nameL = if (ar) blk.right + 8 * u else 10 * u
        val nameR = if (ar) W - 10 * u else blk.left - 8 * u
        r.textTight(Kit.pName(r, nx), (nameL + nameR) / 2, topH * 0.48f, topH * 0.55f * r.s1, r.c1, if (ar) r.calliFont else F.PLAYFAIR, maxW = nameR - nameL - 16 * u, maxH = topH * 0.6f)
        // the line with a dot: how far we are towards the next prayer
        val ly = topH + 6 * u
        val x0 = 18 * u; val x1 = W - 18 * u
        r.canvas.drawLine(x0, ly, x1, ly, r.strokePaint(withAlpha(r.c1, 45), 2 * u))
        val f = d.progress.coerceIn(0f, 1f)
        val dot = if (ar) x1 - (x1 - x0) * f else x0 + (x1 - x0) * f
        r.canvas.drawLine(if (ar) x1 else x0, ly, dot, ly, r.strokePaint(r.c1, 2 * u))
        r.circle(dot, ly, 4 * u, r.c1)
        // big date: faint number behind the month in script
        val g = r.today
        val cy = (ly + H) / 2 + 2 * u
        val bh = H - ly - 12 * u
        r.textTight(r.n(g.dayOfMonth), W / 2, cy, bh * 0.85f, withAlpha(r.c1, 42), r.sansBold, maxH = bh * 0.8f)
        r.textTight(Calendars.monthName(g.monthValue, ar), W / 2, cy, bh * 0.75f * r.s1, r.c1, if (ar) r.calliFont else F.PLAYFAIR, maxW = W - 60 * u, maxH = bh * 0.72f)
    }

    // ---------------------------------------------------- 3 · one connected grid (large)
    fun darkGrid(r: RenderCtx) {
        val u = r.u; val ar = r.ar
        val W = r.w; val H = r.h
        plate(r, RectF(0f, 0f, W, H), 22 * u)
        val line = r.strokePaint(withAlpha(Color.WHITE, 22), 0.8f * u, false)
        val topH = H * 0.6f
        val cw = W / 3f
        // grid lines inside the one card
        r.canvas.drawLine(0f, topH, W, topH, line)
        r.canvas.drawLine(cw, 8 * u, cw, topH - 8 * u, line)
        r.canvas.drawLine(2 * cw, 8 * u, 2 * cw, topH - 8 * u, line)
        val d = Kit.day(r); val nx = d.next
        val hd = Calendars.hijri(r.today)
        // reading order: day (start) · next prayer (middle) · hijri month (end)
        val dayCx = mirror(r, W - cw / 2); val hijCx = mirror(r, cw / 2)
        val w = Kit.weather(r)
        val tx = if (ar) dayCx - cw / 2 + 12 * u else dayCx + cw / 2 - 12 * u
        r.text(Kit.temp(r, w.temp) + r.t("", "م"), tx, topH * 0.16f, 10 * u * r.s2, r.c1, r.sansBold, if (ar) Paint.Align.LEFT else Paint.Align.RIGHT)
        r.textTight(Calendars.dayName(r.today, ar), dayCx, topH * 0.58f, topH * 0.38f * r.s1, r.c1, if (ar) r.calliFont else F.PLAYFAIR, maxW = cw - 18 * u, maxH = topH * 0.42f)
        r.text(Kit.pName(r, nx), W / 2, topH * 0.24f, 12 * u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER)
        r.textTight(Kit.pTime(r, nx.time), W / 2, topH * 0.5f, topH * 0.22f * r.s1, r.c1, r.sansBold, maxW = cw - 16 * u)
        pill(r, left(r, nx), W / 2, topH * 0.78f, 9.5f * u * r.s2)
        r.text(r.n(hd.year), hijCx, topH * 0.18f, 10 * u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER)
        r.textTight(Calendars.hijriMonth(hd.month, ar), hijCx, topH * 0.52f, topH * 0.34f * r.s1, r.c1, if (ar) r.calliFont else F.PLAYFAIR, maxW = cw - 16 * u, maxH = topH * 0.36f)
        r.text(r.n(hd.day), hijCx, topH * 0.84f, 11 * u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER)
        // bottom row: the five prayers, the next one on a lighter tile
        val five = Kit.ordered(r, d.five)
        val pw = W / five.size
        five.forEachIndexed { i, p ->
            val x0 = i * pw
            val on = p.key == nx.key
            if (on) r.canvas.drawRoundRect(RectF(x0 + 5 * u, topH + 6 * u, x0 + pw - 5 * u, H - 6 * u), 14 * u, 14 * u, r.fillPaint(withAlpha(Color.WHITE, 30)))
            else if (i > 0 && five[i - 1].key != nx.key) r.canvas.drawLine(x0, topH + 10 * u, x0, H - 10 * u, line)
            val bh = H - topH
            r.text(Kit.pName(r, p), x0 + pw / 2, topH + bh * 0.36f, 9.5f * u * r.s2, if (on) r.c1 else r.c2, r.sans, Paint.Align.CENTER, maxW = pw - 6 * u)
            r.text(Kit.pTime(r, p.time), x0 + pw / 2, topH + bh * 0.66f, 12 * u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = pw - 4 * u)
        }
    }
}
