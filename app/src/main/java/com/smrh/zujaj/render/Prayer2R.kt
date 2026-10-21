package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.render.RenderCtx.Companion.mix
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha

/** More prayer-time designs. */
object Prayer2R {
    private fun nextIdx(r: RenderCtx) = Kit.day(r).let { d -> d.list.indexOfFirst { it.key == d.next.key } }

    // Journey — next prayer + countdown + moon, progress and a 6-dot timeline (W)
    fun journey(r: RenderCtx) {
        val d = Kit.day(r); val nx = d.next
        val l = r.pad + 6 * r.u
        r.text(r.t("NEXT PRAYER", "الصلاة القادمة"), l, r.h * 0.13f, 8.5f * r.u, r.c2, r.sansBold, spacing = 0.12f)
        val nw = r.text(Kit.pName(r, nx), l, r.h * 0.3f, 22 * r.u * r.s1, r.c1, r.sansBold, maxW = r.w * 0.36f)
        Kit.countdown(r, l + nw + 10 * r.u, r.h * 0.3f, 15 * r.u, r.accent, prefix = "")
        Kit.moonDisc(r, r.w - r.pad - 22 * r.u, r.h * 0.23f, 15 * r.u, withAlpha(Color.WHITE, 230), withAlpha(r.c1, 30))
        r.text(Kit.hijri(r), r.w - r.pad - 44 * r.u, r.h * 0.17f, 10 * r.u, r.c1, r.sansBold, Paint.Align.RIGHT)
        r.text(Kit.moonName(r), r.w - r.pad - 44 * r.u, r.h * 0.3f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        Kit.slider(r, l, r.h * 0.47f, r.w - 2 * l, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 3 * r.u, 4.5f * r.u)
        val list = Kit.ordered(r, d.list)
        val cw = (r.w - 2 * l) / list.size
        for ((i, p) in list.withIndex()) {
            val cx = l + cw * (i + 0.5f); val on = p.key == nx.key
            r.text(Kit.pName(r, p), cx, r.h * 0.63f, 9.5f * r.u, if (on) r.accent else r.c1, r.sansMed, Paint.Align.CENTER, maxW = cw * 0.95f)
            r.circle(cx, r.h * 0.74f, 3 * r.u, if (on) r.accent else withAlpha(r.c1, if (p.time < System.currentTimeMillis()) 220 else 120))
            r.text(Kit.pTime(r, p.time, false), cx, r.h * 0.87f, 11.5f * r.u * r.s2, if (on) r.accent else r.c1, r.sansBold, Paint.Align.CENTER, maxW = cw * 0.95f)
        }
        if (list.size > 1) Kit.line(r, l + cw / 2, r.h * 0.74f, r.w - l - cw / 2, r.h * 0.74f, withAlpha(r.c1, 50), 0.8f * r.u)
    }

    // Mashrabiya — warm card with a lattice panel, day in calligraphy and countdown (W)
    fun mashrabiya(r: RenderCtx) {
        val panel = RectF(r.w * 0.76f, r.pad * 0.6f, r.w - r.pad * 0.6f, r.h - r.pad * 0.6f)
        val cell = 7 * r.u
        r.canvas.save(); r.canvas.clipRect(panel)
        var y = panel.top; var row = 0
        while (y < panel.bottom) {
            var x = panel.left + if (row % 2 == 0) 0f else cell / 2
            while (x < panel.right) { r.canvas.drawRect(x + 1.2f * r.u, y + 1.2f * r.u, x + cell - 1.2f * r.u, y + cell - 1.2f * r.u, r.fillPaint(withAlpha(r.accent, 170))); x += cell }
            y += cell; row++
        }
        r.canvas.restore()
        val w = r.w * 0.74f
        Kit.calli(r, Kit.dayName(r, true), w / 2 + r.pad / 2, r.h * 0.3f, r.h * 0.28f * r.s2, r.accent, maxW = w - 2 * r.pad, maxH = r.h * 0.28f)
        val nx = Kit.day(r).next
        val l = r.pad + 6 * r.u
        r.text(r.t("NEXT PRAYER", "الصلاة القادمة"), l, r.h * 0.58f, 8 * r.u, r.c2, r.sansBold, spacing = 0.14f)
        r.text(Kit.pName(r, nx), l, r.h * 0.71f, 17 * r.u * r.s1, r.c1, r.sansBold, maxW = w * 0.5f)
        Kit.countdown(r, w - 8 * r.u, r.h * 0.7f, 18 * r.u * r.s1, r.accent, Paint.Align.RIGHT, prefix = "")
        Kit.slider(r, l, r.h * 0.82f, w - l - 8 * r.u, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 2 * r.u, 3 * r.u)
        r.text(Kit.pTime(r, nx.time), l, r.h * 0.92f, 9 * r.u, r.c2, r.sans)
        r.text(Kit.hijriShort(r), w - 8 * r.u, r.h * 0.92f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    private fun mosque(r: RenderCtx, left: Float, base: Float, width: Float, color: Int) {
        val p = Path(); val h = width * 0.28f
        p.moveTo(left, base)
        p.lineTo(left, base - h * 0.4f); p.lineTo(left + width * 0.08f, base - h * 0.4f); p.lineTo(left + width * 0.08f, base - h * 0.95f)
        p.lineTo(left + width * 0.1f, base - h * 1.1f); p.lineTo(left + width * 0.12f, base - h * 0.95f); p.lineTo(left + width * 0.12f, base - h * 0.5f)
        p.quadTo(left + width * 0.3f, base - h * 1.25f, left + width * 0.48f, base - h * 0.5f)
        p.lineTo(left + width * 0.48f, base - h * 0.6f)
        p.quadTo(left + width * 0.62f, base - h * 1.45f, left + width * 0.76f, base - h * 0.6f)
        p.lineTo(left + width * 0.86f, base - h * 0.6f); p.lineTo(left + width * 0.86f, base - h * 1.1f)
        p.lineTo(left + width * 0.88f, base - h * 1.25f); p.lineTo(left + width * 0.9f, base - h * 1.1f); p.lineTo(left + width * 0.9f, base - h * 0.4f)
        p.lineTo(left + width, base - h * 0.4f); p.lineTo(left + width, base); p.close()
        r.canvas.drawPath(p, r.fillPaint(color))
    }

    // Mint box — header, time over a mosque skyline, sunrise chip, progress (S)
    fun mintBox(r: RenderCtx) {
        val l = r.pad
        r.text(r.t("Prayer times", "أوقات الصلاة"), r.w - l, r.h * 0.1f, 9.5f * r.u, r.c1, r.sansBold, Paint.Align.RIGHT)
        mosque(r, l, r.h * 0.6f, r.w - 2 * l, withAlpha(r.c1, 18))
        Kit.clock(r, r.w / 2, r.h * 0.33f, 30 * r.u * r.s1, LiveFont.SANS_BOLD, maxW = r.w - 2 * l)
        val nx = Kit.day(r).next
        val chip = "${Kit.pName(r, nx)}  ${Kit.pTime(r, nx.time)}"
        val cw = r.measure(chip, 8.5f * r.u, r.sansMed) + 14 * r.u
        r.pill(RectF(r.w / 2 - cw / 2, r.h * 0.5f, r.w / 2 + cw / 2, r.h * 0.6f), withAlpha(r.accent, 60))
        r.text(chip, r.w / 2, r.h * 0.55f, 8.5f * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
        Kit.line(r, l, r.h * 0.7f, r.w - l, r.h * 0.7f, withAlpha(r.c1, 26))
        Kit.countdown(r, l, r.h * 0.8f, 10 * r.u, r.c1, prefix = "")
        r.text(r.n("${(Kit.progress(r) * 100).toInt()}%"), r.w - l, r.h * 0.8f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        Kit.slider(r, l, r.h * 0.9f, r.w - 2 * l, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 2.5f * r.u, 3.5f * r.u)
    }

    // Column — six rows with icons, next one boxed (S)
    fun column(r: RenderCtx) {
        val d = Kit.day(r)
        val rowH = (r.h - 2 * r.pad * 0.6f) / 6
        for ((i, p) in d.list.withIndex()) {
            val y = r.pad * 0.6f + rowH * (i + 0.5f)
            val on = p.key == d.next.key
            if (on) {
                val rc = RectF(r.pad * 0.6f, y - rowH * 0.46f, r.w - r.pad * 0.6f, y + rowH * 0.46f)
                r.canvas.drawRoundRect(rc, 8 * r.u, 8 * r.u, r.fillPaint(withAlpha(r.accent, 40)))
                r.canvas.drawRoundRect(rc, 8 * r.u, 8 * r.u, r.strokePaint(r.accent, 1 * r.u))
            }
            val col = if (on) r.accent else r.c1
            Kit.prayerIcon(r, p.key, r.w - r.pad - 4 * r.u, y, 6 * r.u, col)
            r.text(Kit.pName(r, p), r.w - r.pad - 14 * r.u, y, 11 * r.u * r.s2, col, r.sansMed, Paint.Align.RIGHT, maxW = r.w * 0.45f)
            r.text(Kit.pTime(r, p.time, false), r.pad + 2 * r.u, y, 11.5f * r.u * r.s1, col, r.sansBold)
        }
    }

    // Midnight — time on the left, six columns with icons on the right (W)
    fun midnight(r: RenderCtx) {
        val split = r.w * 0.4f
        mosque(r, r.pad, r.h * 0.95f, split, withAlpha(r.c1, 16))
        Kit.clock(r, (r.pad + split) / 2, r.h * 0.36f, 30 * r.u * r.s1, LiveFont.SANS_BOLD, maxW = split - r.pad)
        val nx = Kit.day(r).next
        val chip = Kit.pName(r, nx) + "  " + r.t("next", "القادمة")
        val cw = r.measure(chip, 8.5f * r.u, r.sansMed) + 14 * r.u
        val cx = (r.pad + split) / 2
        r.pill(RectF(cx - cw / 2, r.h * 0.55f, cx + cw / 2, r.h * 0.67f), withAlpha(r.accent, 60))
        r.text(chip, cx, r.h * 0.61f, 8.5f * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
        val d = Kit.day(r)
        val list = Kit.ordered(r, d.list)
        val cwid = (r.w - r.pad - split) / list.size
        for ((i, p) in list.withIndex()) {
            val x = split + cwid * (i + 0.5f); val on = p.key == nx.key
            if (on) {
                r.canvas.drawRect(x - cwid * 0.45f, r.h * 0.2f, x + cwid * 0.45f, r.h * 0.8f, r.fillPaint(withAlpha(r.accent, 40)))
                r.canvas.drawRect(x - cwid * 0.45f, r.h * 0.19f, x + cwid * 0.45f, r.h * 0.21f, r.fillPaint(r.accent))
            }
            Kit.prayerIcon(r, p.key, x, r.h * 0.3f, 5 * r.u, if (on) r.accent else r.c2)
            r.text(Kit.pName(r, p), x, r.h * 0.5f, 8.5f * r.u, if (on) r.accent else r.c1, r.sansMed, Paint.Align.CENTER, maxW = cwid * 0.95f)
            r.text(Kit.pTime(r, p.time, false), x, r.h * 0.68f, 9 * r.u * r.s2, if (on) r.accent else r.c2, r.sans, Paint.Align.CENTER, maxW = cwid * 0.95f)
        }
    }

    // Ahead — two panels: the next three prayers, and today's remaining ones (W)
    fun ahead(r: RenderCtx) {
        val d = Kit.day(r); val i0 = nextIdx(r).coerceAtLeast(0)
        val gap = 8 * r.u
        val a = RectF(r.pad * 0.6f, r.pad * 0.6f, r.w / 2 - gap / 2, r.h - r.pad * 0.6f)
        val b = RectF(r.w / 2 + gap / 2, r.pad * 0.6f, r.w - r.pad * 0.6f, r.h - r.pad * 0.6f)
        for (rc in listOf(a, b)) Kit.card(r, rc, 14)
        fun panel(rc: RectF, title: String, items: List<com.smrh.zujaj.data.Prayer>) {
            r.text(title, rc.left + 10 * r.u, rc.top + 13 * r.u, 7.5f * r.u, r.c2, r.sansBold, spacing = 0.15f)
            val rowH = (rc.height() - 34 * r.u) / 3
            items.take(3).forEachIndexed { k, p ->
                val y = rc.top + 28 * r.u + rowH * (k + 0.5f)
                val on = p.key == d.next.key
                if (on) r.canvas.drawRoundRect(RectF(rc.left + 4 * r.u, y - rowH * 0.42f, rc.right - 4 * r.u, y + rowH * 0.42f), 8 * r.u, 8 * r.u, r.fillPaint(withAlpha(r.c1, 26)))
                r.text(Kit.pName(r, p), rc.left + 10 * r.u, y, 11 * r.u, r.c1, r.sansMed, maxW = rc.width() * 0.5f)
                r.text(Kit.pTime(r, p.time), rc.right - 10 * r.u, y, 11 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.RIGHT)
            }
        }
        val all = d.list
        panel(a, r.t("AHEAD", "القادمة"), (all.drop(i0) + all).take(3))
        panel(b, r.t("EARLIER", "السابقة"), (all + all).drop((i0 + 3) % 6).take(3))
        r.text(Kit.hijriShort(r), a.right - 10 * r.u, a.bottom - 8 * r.u, 7.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    // Timeline — vertical list with a dotted rail and a progress bar (L)
    fun timeline(r: RenderCtx) {
        val d = Kit.day(r)
        val top = r.pad; val bottom = r.h * 0.8f
        val rowH = (bottom - top) / 6
        val railX = r.pad + 10 * r.u
        Kit.line(r, railX, top + rowH * 0.5f, railX, top + rowH * 5.5f, withAlpha(r.c1, 40), 0.8f * r.u)
        for ((i, p) in d.list.withIndex()) {
            val y = top + rowH * (i + 0.5f); val on = p.key == d.next.key
            r.circle(railX, y, 4 * r.u, r.cfg.bgColor.let { withAlpha(it, 255) })
            r.canvas.drawCircle(railX, y, 3.5f * r.u, r.strokePaint(if (on) r.accent else withAlpha(r.c1, 120), 1.2f * r.u))
            if (on) r.circle(railX, y, 1.8f * r.u, r.accent)
            r.text(Kit.pName(r, p), railX + 16 * r.u, y, 15 * r.u * r.s1, if (on) r.accent else r.c1, r.sansMed, maxW = r.w * 0.5f)
            r.text(Kit.pTime(r, p.time), r.w - r.pad - 4 * r.u, y, 15 * r.u * r.s1, if (on) r.accent else r.c1, r.sansMed, Paint.Align.RIGHT)
        }
        Kit.slider(r, railX + 16 * r.u, r.h * 0.9f, r.w - railX - 16 * r.u - r.pad - 4 * r.u, (nextIdx(r) - 1 + Kit.progress(r)) / 5f, r.accent, withAlpha(r.c1, 30), 4 * r.u, 5 * r.u)
    }

    // Mint full — time, sunrise chip, list of six and small stats (L)
    fun mintFull(r: RenderCtx) {
        val split = r.w * 0.55f
        r.text(r.t("Prayer times", "أوقات الصلاة"), r.pad + 4 * r.u, r.h * 0.08f, 11 * r.u, r.c1, r.sansBold)
        r.text(Kit.greg(r), r.w - r.pad, r.h * 0.08f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        mosque(r, r.pad, r.h * 0.72f, split - r.pad, withAlpha(r.c1, 16))
        Kit.clock(r, (r.pad + split) / 2, r.h * 0.33f, 40 * r.u * r.s1, LiveFont.SANS_BOLD, maxW = split - r.pad)
        val nx = Kit.day(r).next
        val chip = Kit.pName(r, nx) + "  " + Kit.pTime(r, nx.time)
        val cw = r.measure(chip, 9 * r.u, r.sansMed) + 16 * r.u; val cx = (r.pad + split) / 2
        r.pill(RectF(cx - cw / 2, r.h * 0.47f, cx + cw / 2, r.h * 0.56f), withAlpha(r.accent, 60))
        r.text(chip, cx, r.h * 0.515f, 9 * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
        val d = Kit.day(r)
        val rowH = r.h * 0.62f / 6
        for ((i, p) in d.list.withIndex()) {
            val y = r.h * 0.17f + rowH * (i + 0.5f); val on = p.key == nx.key
            if (on) r.canvas.drawRect(split + 6 * r.u, y - rowH * 0.45f, split + 8 * r.u, y + rowH * 0.45f, r.fillPaint(r.accent))
            Kit.prayerIcon(r, p.key, split + 18 * r.u, y, 4.5f * r.u, if (on) r.accent else r.c2)
            r.text(Kit.pName(r, p), split + 28 * r.u, y, 9.5f * r.u, if (on) r.accent else r.c1, r.sansMed, maxW = r.w * 0.2f)
            r.text(Kit.pTime(r, p.time), r.w - r.pad, y, 9.5f * r.u * r.s2, if (on) r.accent else r.c1, r.sansMed, Paint.Align.RIGHT)
        }
        Kit.line(r, r.pad, r.h * 0.8f, r.w - r.pad, r.h * 0.8f, withAlpha(r.c1, 24))
        Kit.countdown(r, r.pad + 4 * r.u, r.h * 0.9f, 12 * r.u, r.c1, prefix = "")
        Kit.slider(r, r.w * 0.3f, r.h * 0.9f, r.w * 0.3f, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 2.5f * r.u, 3.5f * r.u)
        val w = Kit.weather(r)
        r.text(Kit.temp(r, w.temp), r.w * 0.76f, r.h * 0.9f, 11 * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
        r.text(r.n("${Kit.battery(r)}%"), r.w - r.pad - 4 * r.u, r.h * 0.9f, 11 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
    }

    // Serene — soft light card with a slider and a short list (W)
    fun serene(r: RenderCtx) {
        val d = Kit.day(r); val nx = d.next
        Kit.slider(r, r.pad + 4 * r.u, r.h * 0.2f, r.w * 0.45f, Kit.progress(r), r.accent, withAlpha(r.c1, 40), 7 * r.u, 7 * r.u)
        Kit.clock(r, r.w - r.pad - 16 * r.u, r.h * 0.2f, 18 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.RIGHT)
        Glyphs.moon(r.canvas, r.w - r.pad - 4 * r.u, r.h * 0.2f, 6 * r.u, r.accent)
        val i0 = nextIdx(r).coerceAtLeast(0)
        val items = (d.list + d.list).drop((i0 + 5) % 6).take(3)
        items.forEachIndexed { k, p ->
            val y = r.h * (0.42f + k * 0.16f); val on = p.key == nx.key
            r.circle(r.w * 0.5f, y, 2.4f * r.u, if (on) r.accent else withAlpha(r.c1, 90))
            r.text(Kit.pName(r, p), r.w * 0.5f - 8 * r.u, y, 10 * r.u, if (on) r.c1 else r.c2, r.sansMed, Paint.Align.RIGHT)
            r.text(Kit.pTime(r, p.time), r.pad + 4 * r.u, y, 10 * r.u * r.s2, if (on) r.c1 else r.c2, r.sansBold)
        }
        Kit.countdown(r, r.pad + 4 * r.u, r.h * 0.9f, 10 * r.u, r.c1, prefix = "")
        r.text(Kit.hijri(r), r.w - r.pad - 4 * r.u, r.h * 0.9f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    // Ledger — paper table of the five prayers, next one tinted (W)
    fun ledger(r: RenderCtx) {
        r.text(com.smrh.zujaj.data.Prefs.cityName.ifBlank { r.t("My city", "مدينتي") }, r.pad + 2 * r.u, r.h * 0.12f, 8.5f * r.u, r.c2, r.sans)
        Kit.clock(r, r.w - r.pad - 14 * r.u, r.h * 0.12f, 15 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.RIGHT)
        Glyphs.moon(r.canvas, r.w - r.pad - 3 * r.u, r.h * 0.12f, 5 * r.u, r.accent)
        r.text(Kit.hijriShort(r), r.w - r.pad, r.h * 0.24f, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        Kit.line(r, r.pad, r.h * 0.32f, r.w - r.pad, r.h * 0.32f, withAlpha(r.c1, 30), 0.7f * r.u)
        val d = Kit.day(r)
        val cells = d.list
        val colW = (r.w - 2 * r.pad) / 3
        val rowH = r.h * 0.6f / 2
        cells.forEachIndexed { i, p ->
            val col = if (r.ar) 2 - i % 3 else i % 3; val row = i / 3
            val x0 = r.pad + colW * col; val y = r.h * 0.38f + rowH * (row + 0.5f)
            val on = p.key == d.next.key
            if (on) r.canvas.drawRoundRect(RectF(x0 + 2 * r.u, y - rowH * 0.36f, x0 + colW - 2 * r.u, y + rowH * 0.36f), 6 * r.u, 6 * r.u, r.fillPaint(withAlpha(r.accent, 40)))
            r.canvas.drawCircle(x0 + colW - 8 * r.u, y, 2.6f * r.u, if (on) r.fillPaint(r.accent) else r.strokePaint(withAlpha(r.c1, 120), 0.8f * r.u))
            r.text(Kit.pName(r, p), x0 + colW - 14 * r.u, y, 9 * r.u, if (on) r.accent else r.c1, r.sansMed, Paint.Align.RIGHT, maxW = colW * 0.5f)
            r.text(Kit.pTime(r, p.time), x0 + 6 * r.u, y, 9 * r.u * r.s2, if (on) r.accent else r.c1, r.sansBold, maxW = colW * 0.42f)
        }
    }

    // Ruler — names and times in columns over a ruler with a moving marker (W)
    fun ruler(r: RenderCtx) {
        val d = Kit.day(r)
        val list = Kit.ordered(r, d.list)
        val l = r.pad + 2 * r.u; val w = r.w - 2 * l
        val cw = w / list.size
        for ((i, p) in list.withIndex()) {
            val cx = l + cw * (i + 0.5f); val on = p.key == d.next.key
            r.text(Kit.pName(r, p), cx, r.h * 0.18f, 9.5f * r.u, if (on) r.accent else r.c2, r.sans, Paint.Align.CENTER, maxW = cw * 0.95f)
            r.text(Kit.pTime(r, p.time, false), cx, r.h * 0.36f, 12 * r.u * r.s1, if (on) r.accent else r.c1, r.sansMed, Paint.Align.CENTER, maxW = cw * 0.95f)
        }
        val y = r.h * 0.62f
        for (k in 0..48) {
            val x = l + w * k / 48f
            val big = k % 8 == 0
            Kit.line(r, x, y - (if (big) 7 else 3) * r.u, x, y, withAlpha(r.c1, if (big) 110 else 50), 0.7f * r.u)
        }
        val frac = (d.list.indexOfFirst { it.key == d.next.key } - 1 + Kit.progress(r)).coerceAtLeast(0f) / 5f
        Kit.slider(r, l, r.h * 0.74f, w, if (r.ar) 1 - frac else frac, r.accent, withAlpha(r.c1, 30), 2 * r.u, 3.5f * r.u)
        Kit.countdown(r, if (r.ar) l else r.w - l, r.h * 0.88f, 10 * r.u, r.c2, if (r.ar) Paint.Align.LEFT else Paint.Align.RIGHT)
    }

    // Flip — two flip tiles, day and the next prayer with its bar (W)
    fun flip(r: RenderCtx) {
        val th = r.h * 0.74f; val tw = r.w * 0.27f
        val top = (r.h - th) / 2
        val a = RectF(r.pad, top, r.pad + tw, top + th)
        val b = RectF(a.right + 12 * r.u, top, a.right + 12 * r.u + tw, top + th)
        for (rc in listOf(a, b)) {
            r.canvas.drawRoundRect(rc, 12 * r.u, 12 * r.u, r.fillPaint(withAlpha(r.c1, 22)))
            Kit.line(r, rc.left, rc.centerY(), rc.right, rc.centerY(), withAlpha(Color.BLACK, 120), 1.2f * r.u)
        }
        Kit.clock(r, a.centerX(), a.centerY(), th * 0.7f * r.s1, LiveFont.SANS_BOLD, maxW = tw * 0.9f, maxH = th * 0.62f, pattern = r.hh())
        Kit.clock(r, b.centerX(), b.centerY(), th * 0.7f * r.s1, LiveFont.SANS_BOLD, maxW = tw * 0.9f, maxH = th * 0.62f, pattern = "mm")
        r.circle(a.right + 6 * r.u, a.centerY() - 8 * r.u, 2 * r.u, r.c2); r.circle(a.right + 6 * r.u, a.centerY() + 8 * r.u, 2 * r.u, r.c2)
        val x = r.w - r.pad
        Kit.calli(r, Kit.dayName(r, true), (b.right + x) / 2 + 6 * r.u, r.h * 0.2f, r.h * 0.16f, r.c1, maxW = x - b.right - 14 * r.u, maxH = r.h * 0.18f)
        r.text(Kit.gregShort(r), x, r.h * 0.34f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        Kit.line(r, b.right + 12 * r.u, r.h * 0.42f, x, r.h * 0.42f, withAlpha(r.c1, 26), 0.7f * r.u)
        val nx = Kit.day(r).next
        r.text(r.t("Next prayer", "الصلاة القادمة"), x, r.h * 0.5f, 7.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        r.text(Kit.pName(r, nx), x, r.h * 0.6f, 12 * r.u * r.s2, r.accent, r.sansBold, Paint.Align.RIGHT)
        r.text(Kit.pTime(r, nx.time), x, r.h * 0.7f, 9.5f * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
        Kit.slider(r, b.right + 14 * r.u, r.h * 0.8f, x - b.right - 14 * r.u, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 2.5f * r.u, 3.5f * r.u)
        Kit.countdown(r, x, r.h * 0.9f, 8 * r.u, r.c2, Paint.Align.RIGHT)
    }

    // Hilal — prayers on top, hour scale, big hijri day on the side (W)
    fun hilal(r: RenderCtx) {
        val split = r.w * 0.72f
        val d = Kit.day(r)
        val list = Kit.ordered(r, d.list)
        val cw = (split - r.pad) / list.size
        for ((i, p) in list.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f); val on = p.key == d.next.key
            r.text(Kit.pName(r, p), cx, r.h * 0.17f, 8 * r.u, if (on) r.accent else r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            r.text(Kit.pTime(r, p.time, false), cx, r.h * 0.29f, 9 * r.u * r.s2, if (on) r.accent else r.c1, r.sansMed, Paint.Align.CENTER, maxW = cw)
        }
        Kit.line(r, r.pad, r.h * 0.42f, split - 6 * r.u, r.h * 0.42f, withAlpha(r.c1, 26), 0.7f * r.u)
        val hours = 7
        val hw = (split - r.pad) / hours
        val now = r.now.hour
        for (k in 0 until hours) {
            val h = (now - 3 + k + 24) % 24
            val cx = r.pad + hw * (k + 0.5f)
            val on = k == 3
            r.text(Kit.weekdayShort(r, ((r.today.dayOfWeek.value + 0) % 7) + 1).take(1), cx, r.h * 0.56f, 8 * r.u, withAlpha(r.c2, 0), r.sans, Paint.Align.CENTER)
            r.text(r.n(h.let { if (r.cfg.hour24) it else (if (it % 12 == 0) 12 else it % 12) }.toString()), cx, r.h * 0.74f, 14 * r.u * r.s1, if (on) r.accent else withAlpha(r.c1, 170), r.sansMed, Paint.Align.CENTER)
            if (on) r.circle(cx, r.h * 0.86f, 2 * r.u, r.accent)
        }
        Kit.line(r, split, r.h * 0.12f, split, r.h * 0.88f, withAlpha(r.c1, 26), 0.7f * r.u)
        val h = com.smrh.zujaj.data.Calendars.hijri(r.today)
        val cx = (split + r.w - r.pad) / 2
        r.text(com.smrh.zujaj.data.Calendars.hijriMonth(h.month, r.ar), cx, r.h * 0.2f, 11 * r.u, r.accent, r.sansBold, Paint.Align.CENTER, maxW = r.w - split - r.pad)
        r.textTight(r.n(h.day), cx, r.h * 0.46f, 44 * r.u * r.s1, r.c1, r.sansBold, maxW = r.w - split - r.pad - 4 * r.u)
        r.text(r.n("${h.year}") + r.t(" AH", " هـ"), cx, r.h * 0.68f, 8.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        Kit.countdown(r, cx, r.h * 0.82f, 9 * r.u, r.accent, Paint.Align.CENTER)
    }

    // Day pass — a ticket from the last prayer to the next (W)
    fun dayPass(r: RenderCtx) {
        val stub = RectF(r.pad * 0.5f, r.pad * 0.5f, r.w * 0.2f, r.h - r.pad * 0.5f)
        r.canvas.drawRoundRect(stub, 12 * r.u, 12 * r.u, r.fillPaint(withAlpha(r.c1, 230)))
        val ink = if (androidx.core.graphics.ColorUtils.calculateLuminance(r.c1) > 0.5) Color.BLACK else Color.WHITE
        r.text(Kit.weekdayShort(r, r.today.dayOfWeek.value), stub.centerX(), stub.top + 14 * r.u, 8 * r.u, withAlpha(ink, 160), r.sans, Paint.Align.CENTER)
        r.textTight(r.n(r.today.dayOfMonth), stub.centerX(), stub.top + stub.height() * 0.4f, 30 * r.u, ink, r.sansBold, maxW = stub.width() * 0.9f)
        r.text(com.smrh.zujaj.data.Calendars.monthName(r.today.monthValue, r.ar).take(if (r.ar) 10 else 3), stub.centerX(), stub.top + stub.height() * 0.66f, 9 * r.u, withAlpha(ink, 170), r.sans, Paint.Align.CENTER)
        r.text(r.n(r.today.year), stub.centerX(), stub.bottom - 12 * r.u, 7.5f * r.u, withAlpha(ink, 120), r.sans, Paint.Align.CENTER)
        // perforation
        for (k in 0..8) r.circle(stub.right + 6 * r.u, stub.top + 8 * r.u + k * (stub.height() - 16 * r.u) / 8, 1.2f * r.u, withAlpha(r.c1, 70))
        val d = Kit.day(r)
        val l = stub.right + 16 * r.u; val rr = r.w - r.pad
        r.text(Kit.hijri(r), l, r.h * 0.12f, 8 * r.u, r.c2, r.sans)
        r.text(r.t("Day pass", "تذكرة اليوم"), rr, r.h * 0.12f, 8 * r.u, r.c1, r.sansBold, Paint.Align.RIGHT)
        r.text(r.t("from", "من"), l, r.h * 0.3f, 7.5f * r.u, r.c2, r.sans)
        r.text(r.t("to", "إلى"), rr, r.h * 0.3f, 7.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        r.text(Kit.pName(r, d.prev), l, r.h * 0.45f, 17 * r.u * r.s1, r.c1, r.sansBold, maxW = (rr - l) * 0.4f)
        r.text(Kit.pName(r, d.next), rr, r.h * 0.45f, 17 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.RIGHT, maxW = (rr - l) * 0.4f)
        r.text(Kit.pTime(r, d.prev.time), l, r.h * 0.6f, 10 * r.u, r.accent, r.sansMed)
        r.text(Kit.pTime(r, d.next.time), rr, r.h * 0.6f, 10 * r.u, r.accent, r.sansMed, Paint.Align.RIGHT)
        val mid = (l + rr) / 2
        for (k in -3..3) r.circle(mid + k * 5 * r.u, r.h * 0.45f, 1 * r.u, withAlpha(r.c1, 90))
        Kit.slider(r, l, r.h * 0.76f, rr - l, d.progress, r.accent, withAlpha(r.c1, 30), 2 * r.u, 3.5f * r.u)
        Kit.countdown(r, l, r.h * 0.89f, 8.5f * r.u, r.c1, prefix = "")
        r.text(r.t("until next prayer", "على الصلاة القادمة"), rr, r.h * 0.89f, 7.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    // Relay — "now" and "next" cards with the countdown between them (W)
    fun relay(r: RenderCtx) {
        val d = Kit.day(r)
        r.text(r.t("PRAYER RELAY", "تتابع الصلاة"), r.pad + 4 * r.u, r.h * 0.12f, 8.5f * r.u, r.c1, r.sansBold, spacing = 0.14f)
        r.text(com.smrh.zujaj.data.Prefs.cityName, r.w - r.pad - 4 * r.u, r.h * 0.12f, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        val cw = r.w * 0.3f
        val a = RectF(r.pad, r.h * 0.24f, r.pad + cw, r.h * 0.78f)
        val b = RectF(r.w - r.pad - cw, a.top, r.w - r.pad, a.bottom)
        for ((rc, p, lbl) in listOf(Triple(a, d.prev, r.t("NOW", "الآن")), Triple(b, d.next, r.t("NEXT", "التالية")))) {
            Kit.card(r, rc, 18, 12 * r.u)
            r.text(lbl, rc.left + 10 * r.u, rc.top + 12 * r.u, 7.5f * r.u, r.c2, r.sansBold, spacing = 0.14f)
            r.text(Kit.pName(r, p), rc.left + 10 * r.u, rc.top + rc.height() * 0.48f, 14 * r.u * r.s1, r.c1, r.sansBold, maxW = rc.width() - 16 * r.u)
            r.text(Kit.pTime(r, p.time), rc.left + 10 * r.u, rc.top + rc.height() * 0.78f, 14 * r.u * r.s1, r.c1, r.sansBold)
        }
        Kit.countdown(r, r.w / 2, r.h * 0.42f, 17 * r.u * r.s2, r.c1, Paint.Align.CENTER, prefix = "", bold = true)
        r.text(r.n("${(d.progress * 100).toInt()}%"), r.w / 2, r.h * 0.55f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        Kit.slider(r, a.right + 8 * r.u, r.h * 0.65f, b.left - a.right - 16 * r.u, d.progress, r.c1, withAlpha(r.c1, 40), 2.5f * r.u, 4 * r.u)
        Kit.line(r, r.pad + 4 * r.u, r.h * 0.86f, r.w - r.pad - 4 * r.u, r.h * 0.86f, withAlpha(r.c1, 26), 0.7f * r.u)
        r.text(r.t("NOW · NEXT", "الآن · التالية"), r.w / 2, r.h * 0.93f, 7 * r.u, r.c2, r.sans, Paint.Align.CENTER, spacing = 0.12f)
    }

    // Mosaic — bento tiles: time & day, next prayer, hijri, battery (L)
    fun mosaic(r: RenderCtx) {
        val g = 7 * r.u; val l = r.pad * 0.6f; val t = r.pad * 0.6f
        val W = r.w - 2 * l; val H = r.h - 2 * t
        val top = RectF(l, t, l + W, t + H * 0.4f)
        val bl = RectF(l, top.bottom + g, l + W * 0.55f, t + H)
        val br1 = RectF(bl.right + g, bl.top, l + W, bl.top + (bl.height() - g) / 2)
        val br2 = RectF(br1.left, br1.bottom + g, l + W, bl.bottom)
        for (rc in listOf(top, bl, br1, br2)) Kit.card(r, rc, 18, 14 * r.u)
        Kit.clock(r, top.left + 12 * r.u, top.centerY(), 34 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.LEFT, maxW = top.width() * 0.55f)
        r.text(Kit.dayName(r), top.right - 12 * r.u, top.centerY() - 8 * r.u, 11 * r.u, r.c1, r.sansBold, Paint.Align.RIGHT)
        r.text(Kit.gregShort(r), top.right - 12 * r.u, top.centerY() + 9 * r.u, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        val nx = Kit.day(r).next
        r.text(r.t("NEXT PRAYER", "الصلاة القادمة"), bl.left + 10 * r.u, bl.top + 14 * r.u, 7.5f * r.u, r.c2, r.sansBold, spacing = 0.12f)
        r.text(Kit.pName(r, nx), bl.left + 10 * r.u, bl.top + bl.height() * 0.42f, 16 * r.u * r.s2, r.c1, r.sansBold, maxW = bl.width() - 20 * r.u)
        r.text(Kit.pTime(r, nx.time), bl.left + 10 * r.u, bl.top + bl.height() * 0.6f, 11 * r.u, r.accent, r.sansBold)
        Kit.countdown(r, bl.left + 10 * r.u, bl.top + bl.height() * 0.74f, 8 * r.u, r.c2)
        Kit.slider(r, bl.left + 10 * r.u, bl.bottom - 10 * r.u, bl.width() - 20 * r.u, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 2.5f * r.u, 3.5f * r.u)
        val h = com.smrh.zujaj.data.Calendars.hijri(r.today)
        r.textTight(r.n(h.day), br1.left + 18 * r.u, br1.centerY(), 20 * r.u, r.accent, r.sansBold)
        r.text(com.smrh.zujaj.data.Calendars.hijriMonth(h.month, r.ar), br1.left + 34 * r.u, br1.centerY() - 6 * r.u, 9.5f * r.u, r.c1, r.sansBold, maxW = br1.width() - 40 * r.u)
        r.text(r.n("${h.year}"), br1.left + 34 * r.u, br1.centerY() + 8 * r.u, 8 * r.u, r.c2, r.sans)
        r.text("⚡", br2.left + 14 * r.u, br2.centerY() - 4 * r.u, 11 * r.u, r.accent, r.sans, Paint.Align.CENTER)
        r.text(r.n("${Kit.battery(r)}%"), br2.left + 30 * r.u, br2.centerY() - 4 * r.u, 15 * r.u, r.c1, r.sansBold)
        r.text(r.t("Battery", "البطارية"), br2.left + 30 * r.u, br2.centerY() + 12 * r.u, 7.5f * r.u, r.c2, r.sans)
    }

    // Aurora grid — 3 × 2 grid of names and times, next one in accent (W)
    fun auroraGrid(r: RenderCtx) {
        val d = Kit.day(r)
        val inner = RectF(r.pad * 0.6f, r.pad * 0.6f, r.w - r.pad * 0.6f, r.h - r.pad * 0.6f)
        Kit.card(r, inner, 12, 16 * r.u)
        val colW = inner.width() / 2; val rowH = inner.height() / 3
        d.list.forEachIndexed { i, p ->
            val col = if (r.ar) 1 - i / 3 else i / 3; val row = i % 3
            val x0 = inner.left + colW * col; val y = inner.top + rowH * (row + 0.5f)
            val on = p.key == d.next.key
            r.text(Kit.pName(r, p), x0 + colW - 12 * r.u, y, 15 * r.u * r.s2, if (on) r.accent else r.c1, r.sansMed, Paint.Align.RIGHT, maxW = colW * 0.48f)
            r.text(Kit.pTime(r, p.time, false), x0 + 12 * r.u, y, 15 * r.u * r.s1, if (on) r.accent else r.c1, r.sansBold, maxW = colW * 0.4f)
        }
    }

    // Temple — hijri date, countdown slider and a 3 × 2 grid with check boxes (W)
    fun temple(r: RenderCtx) {
        val d = Kit.day(r)
        r.text(Kit.hijri(r), r.w - r.pad - 4 * r.u, r.h * 0.16f, 15 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.RIGHT, maxW = r.w * 0.5f)
        Kit.countdown(r, r.pad + 4 * r.u, r.h * 0.1f, 8 * r.u, r.accent)
        Kit.slider(r, r.pad + 4 * r.u, r.h * 0.22f, r.w * 0.4f, 1 - d.progress, r.c1, withAlpha(r.c1, 30), 2 * r.u, 5 * r.u)
        val colW = (r.w - 2 * r.pad) / 3; val rowH = r.h * 0.6f / 2
        d.list.forEachIndexed { i, p ->
            val col = if (r.ar) 2 - i % 3 else i % 3; val row = i / 3
            val x0 = r.pad + colW * col; val y = r.h * 0.38f + rowH * (row + 0.5f)
            val on = p.key == d.next.key
            if (on) r.canvas.drawRoundRect(RectF(x0 + 2 * r.u, y - rowH * 0.36f, x0 + colW - 2 * r.u, y + rowH * 0.36f), 8 * r.u, 8 * r.u, r.fillPaint(withAlpha(r.accent, 40)))
            val bx = x0 + colW - 8 * r.u
            val box = RectF(bx - 3 * r.u, y - 3 * r.u, bx + 3 * r.u, y + 3 * r.u)
            if (on) r.canvas.drawRect(box, r.fillPaint(r.accent)) else r.canvas.drawRect(box, r.strokePaint(withAlpha(r.c1, 120), 0.8f * r.u, false))
            r.text(Kit.pName(r, p), bx - 7 * r.u, y, 10.5f * r.u, if (on) r.accent else r.c2, r.sansMed, Paint.Align.RIGHT, maxW = colW * 0.45f)
            r.text(Kit.pTime(r, p.time, false), x0 + 6 * r.u, y, 10.5f * r.u * r.s2, if (on) r.accent else r.c1, r.sansBold, maxW = colW * 0.4f)
        }
    }

    // Next small — time, prayer name and remaining (S)
    fun nextSmall(r: RenderCtx) {
        val nx = Kit.day(r).next
        r.text(Kit.pName(r, nx), r.w - r.pad, r.h * 0.38f, 22 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.RIGHT, maxW = r.w * 0.5f)
        r.text(Kit.pTime(r, nx.time), r.pad, r.h * 0.38f, 20 * r.u * r.s1, r.c1, r.sansBold, maxW = r.w * 0.45f)
        r.text(r.t("next", "القادمة"), r.w - r.pad, r.h * 0.18f, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        Kit.countdown(r, r.w - r.pad, r.h * 0.7f, 11 * r.u, r.accent, Paint.Align.RIGHT)
        Kit.slider(r, r.pad, r.h * 0.86f, r.w - 2 * r.pad, Kit.progress(r), r.accent, withAlpha(r.c1, 30), 2.5f * r.u, 0f)
    }

    // Horizon — temperature, condition and the sun's path from sunrise to sunset (W)
    fun horizon(r: RenderCtx) {
        val w = Kit.weather(r)
        r.textTight(Kit.temp(r, w.temp), r.pad + 34 * r.u, r.h * 0.26f, 34 * r.u * r.s1, r.c1, r.sansBold)
        r.text(com.smrh.zujaj.data.WeatherRepo.condName(w.cond, r.ar), r.pad + 4 * r.u, r.h * 0.5f, 14 * r.u, r.c1, r.sansMed, spacing = if (r.ar) 0f else 0.12f)
        r.text(w.city.ifBlank { com.smrh.zujaj.data.Prefs.cityName }, r.w - r.pad - 4 * r.u, r.h * 0.16f, 15 * r.u, withAlpha(r.c1, 120), r.sansMed, Paint.Align.RIGHT, maxW = r.w * 0.4f)
        val d = Kit.day(r)
        val rise = d.list.first { it.key == "sunrise" }.time
        val set = d.list.first { it.key == "maghrib" }.time
        val now = System.currentTimeMillis()
        val frac = ((now - rise).toFloat() / (set - rise)).coerceIn(0f, 1f)
        val y = r.h * 0.64f
        Kit.line(r, r.pad, y, r.w - r.pad, y, withAlpha(r.c1, 50), 1 * r.u)
        val x = r.pad + (r.w - 2 * r.pad) * frac
        r.canvas.drawLine(r.pad, y, x, y, r.strokePaint(r.accent, 1.6f * r.u))
        r.circle(x, y, 3.2f * r.u, r.c1)
        for (f in listOf(0.22f, 0.78f)) Kit.line(r, r.pad + (r.w - 2 * r.pad) * f, y - 4 * r.u, r.pad + (r.w - 2 * r.pad) * f, y + 4 * r.u, withAlpha(r.c1, 60), 0.7f * r.u)
        r.text(Kit.pTime(r, rise, false), r.w * 0.25f, r.h * 0.78f, 13 * r.u * r.s2, r.c1, r.sans, Paint.Align.CENTER)
        r.text(Kit.pTime(r, set, false), r.w * 0.75f, r.h * 0.78f, 13 * r.u * r.s2, r.c1, r.sans, Paint.Align.CENTER)
        r.text(r.t("SUNRISE", "الشروق"), r.w * 0.25f, r.h * 0.9f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, spacing = 0.15f)
        r.text(r.t("SUNSET", "الغروب"), r.w * 0.75f, r.h * 0.9f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, spacing = 0.15f)
    }

    @Suppress("unused") private fun unused(a: Int, b: Int) = mix(a, b, 0.5f) + LinearGradient(0f, 0f, 1f, 1f, 0, 0, Shader.TileMode.CLAMP).hashCode()
}
