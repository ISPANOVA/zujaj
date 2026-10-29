package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.cos
import kotlin.math.sin

/** Graphic clocks: bands, Bauhaus forms, neon, dot-matrix, galaxy, desert. */
object Fancy2R {
    // Overlap — two crossing colour bands behind a big time (W, light by default)
    fun overlap(r: RenderCtx) {
        val a = Path().apply { moveTo(0f, r.h * 0.5f); lineTo(r.w, r.h * 0.82f); lineTo(r.w, r.h * 0.94f); lineTo(0f, r.h * 0.62f); close() }
        val b = Path().apply { moveTo(0f, r.h * 0.96f); lineTo(r.w, r.h * 0.52f); lineTo(r.w, r.h * 0.64f); lineTo(0f, r.h * 1.08f); close() }
        r.canvas.drawPath(a, r.fillPaint(withAlpha(r.c2, 200)))
        r.canvas.drawPath(b, r.fillPaint(withAlpha(r.accent, 200)))
        r.text(r.t("Time", "الوقت"), r.w - r.pad, r.h * 0.12f, 9 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
        r.text(Kit.greg(r), r.pad, r.h * 0.12f, 8 * r.u, withAlpha(r.c1, 140), r.sans)
        Kit.clock(r, r.w - r.pad, r.h * 0.32f, 34 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.RIGHT, maxW = r.w * 0.6f)
    }

    // Chrome — temperature, day, big metallic time, ISO date (S)
    fun chrome(r: RenderCtx) {
        val w = Kit.weather(r)
        r.text(Kit.temp(r, w.temp), r.pad, r.h * 0.14f, 12 * r.u, r.c1, r.sansBold)
        r.text(Kit.dayName(r), r.w - r.pad, r.h * 0.14f, 11 * r.u, withAlpha(r.c1, 160), r.sansBold, Paint.Align.RIGHT, maxW = r.w * 0.5f)
        Kit.timeText(r, r.w / 2, r.h * 0.48f, r.h * 0.34f * r.s1, LiveFont.SANS_BOLD,
            Color.WHITE, 0xFF8A8A92.toInt(), maxW = r.w - 2 * r.pad, maxH = r.h * 0.3f, pattern = if (r.cfg.hour24) "HH:mm" else "h:mm", font = r.sansBold)
        r.text(r.n(r.today.toString()), r.w / 2, r.h * 0.82f, 11 * r.u, withAlpha(r.c1, 180), r.sansBold, Paint.Align.CENTER, spacing = 0.06f)
    }

    // Forms — Bauhaus shapes with the time (S)
    fun forms(r: RenderCtx) {
        val tri = Path().apply { moveTo(r.pad + 4 * r.u, r.h * 0.2f); lineTo(r.pad + 14 * r.u, r.h * 0.06f); lineTo(r.pad + 24 * r.u, r.h * 0.2f); close() }
        r.canvas.drawPath(tri, r.fillPaint(r.c2))
        r.text(Kit.dayName(r), r.pad + 30 * r.u, r.h * 0.13f, 8.5f * r.u, r.c1, r.sansBold, spacing = 0.1f)
        Kit.clock(r, r.pad + 4 * r.u, r.h * 0.38f, 20 * r.u * r.s1, LiveFont.SANS_BOLD, align = Paint.Align.LEFT, maxW = r.w * 0.66f,
            pattern = if (r.cfg.hour24) "HH:mm" else "h:mm a")
        r.text(Kit.gregShort(r), r.pad + 4 * r.u, r.h * 0.52f, 8 * r.u, r.c2, r.sans)
        val bar = RectF(r.w - r.pad - 16 * r.u, r.pad * 0.6f, r.w - r.pad, r.h * 0.62f)
        r.canvas.drawRoundRect(bar, bar.width() / 2, bar.width() / 2, r.fillPaint(r.c2))
        val half = RectF(r.pad - 4 * r.u, r.h * 0.62f, r.pad + 54 * r.u, r.h * 1.0f + 20 * r.u)
        r.canvas.drawArc(half, 180f, 180f, true, r.fillPaint(r.accent))
        r.text(Kit.hijriShort(r), r.w - r.pad, r.h * 0.9f, 7 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
    }

    // Neon — glowing gradient time with AM/PM and the date (S)
    fun neon(r: RenderCtx) {
        r.text(Kit.weekdayShort(r, r.today.dayOfWeek.value), r.w / 2, r.h * 0.16f, 11 * r.u, withAlpha(r.c1, 150), r.sansBold, Paint.Align.CENTER, spacing = 0.2f)
        val s = Kit.bakedTime(r, if (r.cfg.hour24) "HH:mm" else "hh:mm")
        if (!r.gradTime) {
            r.liveTime(if (r.cfg.hour24) "HH:mm" else "hh:mm", r.w / 2, r.h * 0.46f, r.h * 0.3f * r.s1, LiveFont.SANS_BOLD, r.accent, Paint.Align.CENTER, maxW = r.w - 2 * r.pad, maxH = r.h * 0.3f, sample = "00:00")
            if (!r.cfg.hour24) r.liveTime("a", r.w / 2, r.h * 0.68f, 11 * r.u, LiveFont.SANS_BOLD, r.c2, Paint.Align.CENTER)
            r.text(Kit.greg(r), r.w / 2, r.h * 0.86f, 9 * r.u, withAlpha(r.c1, 170), r.sans, Paint.Align.CENTER)
            return
        }
        // glow
        val gp = r.paint(r.accent, r.h * 0.3f * r.s1, r.sansBold, Paint.Align.CENTER)
        r.fitPaint(gp, s, r.w - 2 * r.pad, r.h * 0.3f)
        gp.maskFilter = android.graphics.BlurMaskFilter(8 * r.u, android.graphics.BlurMaskFilter.Blur.NORMAL)
        val b = android.graphics.Rect(); gp.getTextBounds(s, 0, s.length, b)
        r.canvas.drawText(s, r.w / 2, r.h * 0.46f - b.exactCenterY(), gp)
        Kit.gradText(r, s, r.w / 2, r.h * 0.46f, r.h * 0.3f * r.s1, r.sansBold, r.accent, r.c2, vertical = false, maxW = r.w - 2 * r.pad, maxH = r.h * 0.3f)
        if (!r.cfg.hour24) r.text(Kit.bakedTime(r, "a"), r.w / 2, r.h * 0.68f, 11 * r.u, r.c2, r.sansBold, Paint.Align.CENTER, spacing = 0.2f)
        r.text(Kit.greg(r), r.w / 2, r.h * 0.86f, 9 * r.u, withAlpha(r.c1, 170), r.sans, Paint.Align.CENTER)
    }

    private fun dotDigits(r: RenderCtx, s: String, x0: Float, cy: Float, cell: Float, color: Int, off: Int): Float {
        val font = mapOf(
            '0' to listOf("111", "101", "101", "101", "111"), '1' to listOf("010", "110", "010", "010", "111"),
            '2' to listOf("111", "001", "111", "100", "111"), '3' to listOf("111", "001", "111", "001", "111"),
            '4' to listOf("101", "101", "111", "001", "001"), '5' to listOf("111", "100", "111", "001", "111"),
            '6' to listOf("111", "100", "111", "101", "111"), '7' to listOf("111", "001", "010", "010", "010"),
            '8' to listOf("111", "101", "111", "101", "111"), '9' to listOf("111", "101", "111", "001", "111"),
            ':' to listOf("0", "1", "0", "1", "0"),
        )
        var x = x0
        for (ch in s) {
            val g = font[ch] ?: continue
            val sub = 3 // each pixel = 3×3 tiny dots
            for ((row, line) in g.withIndex()) for ((col, bit) in line.withIndex()) {
                for (a in 0 until sub) for (b in 0 until sub) {
                    val px = x + (col * sub + a) * cell; val py = cy - 2.5f * sub * cell + (row * sub + b) * cell
                    r.circle(px + cell / 2, py + cell / 2, cell * 0.36f, if (bit == '1') color else off)
                }
            }
            x += (g[0].length * sub + 2) * cell
        }
        return x
    }

    // Dot rings — LED dot-matrix time with a concentric dotted target (W)
    fun dotRings(r: RenderCtx) {
        val bgDots = withAlpha(r.c1, 14)
        val step = 4 * r.u
        var yy = step / 2
        while (yy < r.h) { var xx = step / 2; while (xx < r.w) { r.circle(xx, yy, 0.7f * r.u, bgDots); xx += step }; yy += step }
        val cx = r.pad + r.h * 0.34f; val cy = r.h / 2
        for (k in 1..5) {
            val rad = k * r.h * 0.065f
            val n = (2 * Math.PI * rad / (3.2f * r.u)).toInt()
            for (i in 0 until n) {
                val a = 2 * Math.PI * i / n
                val col = if (k == 2 && i < n / 3) r.accent else withAlpha(r.c1, 200)
                r.circle(cx + rad * cos(a).toFloat(), cy + rad * sin(a).toFloat(), 1.1f * r.u, col)
            }
        }
        r.circle(cx, cy, 2.4f * r.u, r.accent)
        val t = Kit.bakedTime(r, r.hm())
        val cell = (r.w - cx - r.h * 0.38f - r.pad) / ((3 * 3 + 2) * 4 + (1 * 3 + 2))
        if (r.gradTime) dotDigits(r, t, cx + r.h * 0.42f, cy, cell.coerceAtMost(r.h * 0.6f / 15), r.c1, withAlpha(r.c1, 18))
        else r.liveTime(r.hm(), cx + r.h * 0.42f, cy, r.h * 0.4f, LiveFont.ORBITRON, r.c1, Paint.Align.LEFT, maxW = r.w - cx - r.h * 0.42f - r.pad, maxH = r.h * 0.4f, sample = "00:00")
    }

    // Galaxy — dotted spiral and the time (S)
    fun galaxy(r: RenderCtx) {
        val cx = r.w * 0.28f; val cy = r.h / 2
        for (i in 0 until 90) {
            val t = i / 90f * 4 * Math.PI
            val rad = r.h * 0.04f + i * r.h * 0.0032f
            val col = if (i % 9 == 0) r.accent else withAlpha(r.c1, 120 + (i % 3) * 40)
            r.circle(cx + rad * cos(t).toFloat(), cy + rad * sin(t).toFloat(), 0.9f * r.u, col)
        }
        r.circle(cx, cy, 2 * r.u, r.accent)
        Kit.clock(r, r.w * 0.74f, cy, 30 * r.u * r.s1, LiveFont.BEBAS, maxW = r.w * 0.46f, maxH = r.h * 0.4f)
    }

    // Desert — line-art pyramids, sun disk and palms with the day and time (W)
    fun desert(r: RenderCtx) {
        val gold = r.accent
        val line = r.strokePaint(withAlpha(gold, 200), 1.1f * r.u)
        val base = r.h * 0.86f
        fun pyramid(cx: Float, w: Float, h: Float) {
            val p = Path().apply { moveTo(cx - w / 2, base); lineTo(cx, base - h); lineTo(cx + w / 2, base) }
            r.canvas.drawPath(p, line)
            r.canvas.drawLine(cx, base - h, cx + w * 0.12f, base, r.strokePaint(withAlpha(gold, 90), 0.8f * r.u))
            for (k in 1..4) { val y = base - h * k / 5f; val hw = w / 2 * (1 - k / 5f); r.canvas.drawLine(cx - hw, y, cx + hw, y, r.strokePaint(withAlpha(gold, 50), 0.6f * r.u)) }
        }
        pyramid(r.w * 0.2f, r.w * 0.26f, r.h * 0.42f); pyramid(r.w * 0.36f, r.w * 0.2f, r.h * 0.3f)
        r.canvas.drawLine(r.pad, base, r.w - r.pad, base, r.strokePaint(withAlpha(gold, 120), 0.8f * r.u))
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(r.w * 0.27f, r.h * 0.26f, 18 * r.u, withAlpha(gold, 160), withAlpha(gold, 0), Shader.TileMode.CLAMP) }
        r.canvas.drawCircle(r.w * 0.27f, r.h * 0.26f, 18 * r.u, sp)
        r.canvas.drawCircle(r.w * 0.27f, r.h * 0.26f, 7 * r.u, line)
        // palm
        val px = r.w * 0.06f
        r.canvas.drawLine(px, base, px + 3 * r.u, base - r.h * 0.28f, line)
        for (a in listOf(-70f, -30f, 10f, 40f, -110f)) {
            val rad = Math.toRadians(a.toDouble())
            r.canvas.drawLine(px + 3 * r.u, base - r.h * 0.28f, px + 3 * r.u + 14 * r.u * cos(rad).toFloat(), base - r.h * 0.28f + 8 * r.u * sin(rad).toFloat() + 4 * r.u, line)
        }
        val right = r.w - r.pad
        Kit.calli(r, Kit.dayName(r, true), r.w * 0.72f, r.h * 0.3f, r.h * 0.3f * r.s2, r.c1, maxW = r.w * 0.5f, maxH = r.h * 0.32f)
        Kit.clock(r, right, r.h * 0.62f, 26 * r.u * r.s1, LiveFont.SANS_BOLD, gold, Paint.Align.RIGHT, maxW = r.w * 0.45f)
        r.text(Kit.greg(r), r.w * 0.72f, r.h * 0.84f, 8.5f * r.u, withAlpha(gold, 200), r.sans, Paint.Align.CENTER)
    }

    @Suppress("unused") private val unused = LinearGradient(0f, 0f, 1f, 1f, 0, 0, Shader.TileMode.CLAMP)
}
