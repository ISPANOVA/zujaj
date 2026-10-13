package com.smrh.zujaj.render

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object ClockR {
    private class Dial(val cx: Float, val cy: Float, val r: Float) {
        val rect get() = RectF(cx - r, cy - r, cx + r, cy + r)
        fun at(deg: Float, rad: Float): Pair<Float, Float> {
            val a = Math.toRadians((deg - 90).toDouble())
            return (cx + cos(a) * rad).toFloat() to (cy + sin(a) * rad).toFloat()
        }
    }

    private fun dial(r: RenderCtx, inset: Float = 8f): Dial {
        val s = min(r.w, r.h) / 2f - inset * r.u
        return Dial(r.w / 2f, r.h / 2f, s)
    }

    private fun hands(r: RenderCtx, kind: Int = 0, hourLen: Float = 0.5f, minLen: Float = 0.78f, width: Float = 0.045f) =
        HandStyle(kind, r.c1, r.c1, r.accent, r.cfg.seconds, hourLen, minLen, width, r.c1)

    /** small time + weekday inside the dial */
    private fun innerLabels(r: RenderCtx, d: Dial, topY: Float = 0.42f, botY: Float = 0.42f, size: Float = 0.11f) {
        val sz = d.r * size * r.s2
        r.liveTime(r.hm(), d.cx, d.cy - d.r * topY, sz, LiveFont.SANS, r.c2)
        r.liveTime("EEE", d.cx, d.cy + d.r * botY, sz * 0.92f, LiveFont.SANS, r.c2)
    }

    private fun scallop(cx: Float, cy: Float, rad: Float, waves: Int = 9, depth: Float = 0.055f): Path {
        val p = Path()
        val steps = 360
        for (i in 0..steps) {
            val a = Math.toRadians(i.toDouble())
            val rr = rad * (1 - depth + depth * cos(waves * a)).toFloat()
            val x = (cx + cos(a) * rr).toFloat(); val y = (cy + sin(a) * rr).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    fun dots(r: RenderCtx) {
        val d = dial(r)
        for (i in 0 until 12) {
            val (x, y) = d.at(i * 30f, d.r * 0.86f)
            r.circle(x, y, d.r * (if (i % 3 == 0) 0.04f else 0.028f), withAlpha(r.c1, if (i % 3 == 0) 230 else 150))
        }
        r.liveAnalog(d.rect, hands(r))
    }

    fun fill(r: RenderCtx) {
        val d = dial(r)
        r.circle(d.cx, d.cy, d.r * 0.92f, withAlpha(r.c1, 18))
        for (i in 0 until 4) {
            val (x, y) = d.at(i * 90f, d.r * 0.8f)
            r.circle(x, y, d.r * 0.035f, r.c1)
        }
        innerLabels(r, d)
        r.liveAnalog(d.rect, hands(r))
    }

    fun minimal(r: RenderCtx) {
        val d = dial(r)
        r.circle(d.cx, d.cy, d.r * 0.55f, withAlpha(r.c1, 22))
        for (i in 0 until 4) {
            val (x, y) = d.at(i * 90f, d.r * 0.85f)
            r.circle(x, y, d.r * 0.03f, r.c2)
        }
        innerLabels(r, d, 0.36f, 0.36f, 0.1f)
        r.liveAnalog(d.rect, hands(r))
    }

    fun modern(r: RenderCtx) {
        val d = dial(r)
        // striped disc
        val save = r.canvas.save()
        val clip = Path().apply { addCircle(d.cx, d.cy, d.r * 0.62f, Path.Direction.CW) }
        r.canvas.clipPath(clip)
        val sp = r.strokePaint(withAlpha(r.c1, 18), d.r * 0.035f, false)
        var x = d.cx - d.r * 3
        while (x < d.cx + d.r) { r.canvas.drawLine(x, d.cy + d.r, x + d.r * 2, d.cy - d.r, sp); x += d.r * 0.09f }
        r.canvas.restoreToCount(save)
        r.circle(d.cx, d.cy, d.r * 0.36f, withAlpha(r.bgColorSafe(), 255))
        r.text(r.n("9"), d.cx - d.r * 0.8f, d.cy, d.r * 0.3f * r.s1, r.c1, F.SANS_MED, Paint.Align.CENTER)
        r.text(r.n("3"), d.cx + d.r * 0.8f, d.cy, d.r * 0.3f * r.s1, r.c1, F.SANS_MED, Paint.Align.CENTER)
        innerLabels(r, d, 0.6f, 0.5f)
        r.liveAnalog(d.rect, hands(r))
    }

    fun flat(r: RenderCtx) {
        val d = dial(r, 6f)
        r.canvas.drawPath(scallop(d.cx, d.cy, d.r * 0.98f), r.fillPaint(withAlpha(r.c1, 26)))
        val (x, y) = d.at(180f, d.r * 0.72f)
        r.circle(x, y, d.r * 0.045f, r.accent)
        r.liveAnalog(d.rect, HandStyle(1, r.c1, r.c2, r.accent, r.cfg.seconds, 0.42f, 0.66f, 0.06f, r.c1))
    }

    fun relief(r: RenderCtx) {
        val d = dial(r, 6f)
        r.canvas.drawPath(scallop(d.cx, d.cy, d.r * 0.98f, 10, 0.045f), r.fillPaint(withAlpha(r.c1, 26)))
        r.text(r.n("9"), d.cx - d.r * 0.66f, d.cy, d.r * 0.32f * r.s1, r.c1, F.SANS_MED, Paint.Align.CENTER)
        r.text(r.n("3"), d.cx + d.r * 0.66f, d.cy, d.r * 0.32f * r.s1, r.c1, F.SANS_MED, Paint.Align.CENTER)
        val (x, y) = d.at(180f, d.r * 0.74f)
        r.circle(x, y, d.r * 0.04f, r.accent)
        innerLabels(r, d, 0.5f, 0.42f, 0.1f)
        r.liveAnalog(d.rect, HandStyle(1, r.c1, r.c2, r.accent, r.cfg.seconds, 0.4f, 0.62f, 0.055f, r.c1))
    }

    fun lines(r: RenderCtx) {
        val d = dial(r)
        for (i in 0 until 60) {
            val major = i % 5 == 0
            val (x1, y1) = d.at(i * 6f, d.r * (if (major) 0.8f else 0.86f))
            val (x2, y2) = d.at(i * 6f, d.r * 0.95f)
            r.canvas.drawLine(x1, y1, x2, y2, r.strokePaint(withAlpha(r.c1, if (major) 240 else 110), d.r * (if (major) 0.022f else 0.011f)))
        }
        innerLabels(r, d, 0.42f, 0.42f)
        r.liveAnalog(d.rect, hands(r))
    }

    fun depth(r: RenderCtx) {
        val d = dial(r)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(d.cx, d.cy - d.r, d.cx, d.cy + d.r, withAlpha(r.c1, 60), withAlpha(r.c1, 14), Shader.TileMode.CLAMP)
        r.canvas.drawCircle(d.cx, d.cy, d.r * 0.96f, p)
        r.circle(d.cx, d.cy, d.r * 0.6f, withAlpha(r.bgColorSafe(), 235))
        for (i in 0 until 12) {
            val (x1, y1) = d.at(i * 30f, d.r * 0.74f)
            val (x2, y2) = d.at(i * 30f, d.r * 0.86f)
            r.canvas.drawLine(x1, y1, x2, y2, r.strokePaint(withAlpha(r.c1, 220), d.r * 0.035f))
        }
        val (x, y) = d.at(180f, d.r * 0.68f)
        r.circle(x, y, d.r * 0.035f, r.accent)
        innerLabels(r, d, 0.3f, 0.3f, 0.1f)
        r.liveAnalog(d.rect, hands(r, 0, 0.45f, 0.72f))
    }

    private fun radialNumerals(r: RenderCtx, d: Dial, font: F, labels: List<String>, rad: Float, size: Float, colorFn: (Int) -> Int) {
        for (i in 0 until 12) {
            val ang = i * 30f
            val (x, y) = d.at(ang, rad)
            r.canvas.save()
            r.canvas.rotate(ang, x, y)
            r.text(labels[i], x, y, size, colorFn(i), font, Paint.Align.CENTER)
            r.canvas.restore()
        }
    }

    fun styled(r: RenderCtx) {
        val d = dial(r)
        val labels = (0 until 12).map { r.n(if (it == 0) "12" else it.toString()) }
        radialNumerals(r, d, F.BODONI, labels, d.r * 0.84f, d.r * 0.2f * r.s1) { withAlpha(r.c1, 235) }
        r.liveAnalog(d.rect, hands(r))
    }

    fun gradient(r: RenderCtx) {
        val d = dial(r)
        val labels = (0 until 12).map { r.n(if (it == 0) "12" else it.toString()) }
        radialNumerals(r, d, F.BODONI, labels, d.r * 0.84f, d.r * 0.2f * r.s1) { i -> withAlpha(r.c1, 70 + i * 15) }
        r.liveAnalog(d.rect, hands(r))
    }

    fun roman(r: RenderCtx) {
        val d = dial(r)
        val rom = listOf("XII", "I", "II", "III", "IIII", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        radialNumerals(r, d, F.CORMORANT, rom, d.r * 0.8f, d.r * 0.17f * r.s1) { r.c1 }
        r.canvas.drawCircle(d.cx, d.cy, d.r * 0.96f, r.strokePaint(withAlpha(r.c1, 60), d.r * 0.012f))
        r.liveAnalog(d.rect, hands(r, 2))
    }

    fun arabicNumerals(r: RenderCtx) {
        val d = dial(r)
        for (i in 0 until 12) {
            val (x, y) = d.at(i * 30f, d.r * 0.8f)
            val lab = Calendars.toArabicDigits(if (i == 0) "12" else i.toString())
            r.text(lab, x, y, d.r * 0.2f * r.s1, if (i % 3 == 0) r.c1 else r.c2, F.AMIRI, Paint.Align.CENTER)
        }
        r.liveAnalog(d.rect, hands(r, 2))
    }

    fun orbit(r: RenderCtx) {
        val d = dial(r)
        val p = r.strokePaint(withAlpha(r.c1, 40), d.r * 0.012f)
        r.canvas.drawCircle(d.cx, d.cy, d.r * 0.92f, p)
        r.canvas.drawCircle(d.cx, d.cy, d.r * 0.62f, p)
        r.canvas.drawCircle(d.cx, d.cy, d.r * 0.32f, p)
        for (i in 0 until 12) {
            val (x, y) = d.at(i * 30f, d.r * 0.92f)
            r.circle(x, y, d.r * (if (i % 3 == 0) 0.035f else 0.018f), if (i % 3 == 0) r.accent else r.c1)
        }
        r.liveAnalog(d.rect, hands(r, 3, 0.55f, 0.88f))
    }

    /** Wide rectangle with radiating ticks, big hour/minute digits and hands in the middle. */
    fun wide(r: RenderCtx) {
        val cx = r.w / 2; val cy = r.h / 2
        val hw = r.w / 2 - 10 * r.u
        val hh = r.h / 2 - 10 * r.u
        for (i in 0 until 60) {
            val a = Math.toRadians((i * 6 - 90).toDouble())
            val c = cos(a).toFloat(); val s = sin(a).toFloat()
            val t = min(if (abs(c) > 1e-4) hw / abs(c) else Float.MAX_VALUE, if (abs(s) > 1e-4) hh / abs(s) else Float.MAX_VALUE)
            val major = i % 5 == 0
            val len = (if (major) 14f else 8f) * r.u
            val x2 = cx + c * t; val y2 = cy + s * t
            val x1 = cx + c * (t - len); val y1 = cy + s * (t - len)
            r.canvas.drawLine(x1, y1, x2, y2, r.strokePaint(withAlpha(r.c1, if (major) 230 else 110), (if (major) 2.4f else 1.2f) * r.u))
        }
        val big = 70 * r.u * r.s1
        r.liveTime(r.hh(), cx - 60 * r.u, cy, big, LiveFont.BODONI, r.c1, Paint.Align.RIGHT, sample = "00")
        r.liveTime("mm", cx + 60 * r.u, cy, big, LiveFont.BODONI, r.c1, Paint.Align.LEFT, sample = "00")
        val hr = 46 * r.u
        r.liveAnalog(RectF(cx - hr, cy - hr, cx + hr, cy + hr), hands(r, 0, 0.55f, 0.95f, 0.05f))
    }
}

/** Background colour (opaque) used to "cut" shapes inside widgets. */
fun RenderCtx.bgColorSafe(): Int = if (cfg.bg) (cfg.bgColor or 0xFF000000.toInt()) else 0xFF141418.toInt()
