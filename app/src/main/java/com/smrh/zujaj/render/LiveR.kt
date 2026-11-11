package com.smrh.zujaj.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.Cond
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.data.WeatherRepo
import com.smrh.zujaj.render.RenderCtx.Companion.mix
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live widgets. Each one is a still part (texts, drawn sharp and kept in place) plus a moving layer
 * drawn for a phase t in 0..1 that loops seamlessly: every motion moves by exactly one period of
 * its own pattern (a ray spacing, a bead spacing, one wavelength…), so 12 frames read as smooth.
 *
 * Layout: the moving object lives in [zone] (the end side of a wide widget, the top of a small or large
 * one) and the texts in [box], so the animation never covers what you read. Ambient effects (stars,
 * rain, aurora…) cross the whole widget but stay faint.
 */
object LiveR {
    private const val TAU = (2 * PI).toFloat()
    private fun a(c: Int, f: Float) = withAlpha(c, (f.coerceIn(0f, 1f) * 255).toInt())
    /** How much effect: subtle / normal / rich. */
    private fun amt(r: RenderCtx) = when (r.cfg.liveFx) { 0 -> 0.55f; 2 -> 1.6f; else -> 1f }
    private fun count(r: RenderCtx, n: Int) = max(3, (n * amt(r)).toInt())
    private fun wave(t: Float, k: Int = 1, ph: Float = 0f) = sin((t * k + ph) * TAU)
    private fun w01(t: Float, k: Int = 1, ph: Float = 0f) = (wave(t, k, ph) + 1f) / 2f
    private val Float.d get() = this

    // ------------------------------------------------------------------------------------------ geometry
    fun zone(r: RenderCtx): RectF = when (r.style.shape) {
        Shape.SMALL -> RectF(r.w * 0.2f, r.h * 0.05f, r.w * 0.8f, r.h * 0.55f)
        Shape.WIDE, Shape.BANNER -> {
            val s = r.h * 0.84f; val m = r.h * 0.08f
            if (r.ar) RectF(m, (r.h - s) / 2, m + s, (r.h + s) / 2) else RectF(r.w - m - s, (r.h - s) / 2, r.w - m, (r.h + s) / 2)
        }
        else -> { val s = r.h * 0.48f; RectF(r.w / 2 - s / 2, r.h * 0.05f, r.w / 2 + s / 2, r.h * 0.05f + s) }
    }

    fun box(r: RenderCtx): RectF {
        val z = zone(r); val p = max(r.pad, 12 * r.u)
        return when (r.style.shape) {
            Shape.SMALL -> RectF(p * 0.7f, r.h * 0.55f, r.w - p * 0.7f, r.h - p * 0.5f)
            Shape.WIDE, Shape.BANNER -> if (r.ar) RectF(z.right + 6 * r.u, p * 0.8f, r.w - p, r.h - p * 0.8f) else RectF(p, p * 0.8f, z.left - 6 * r.u, r.h - p * 0.8f)
            else -> RectF(p, r.h * 0.55f, r.w - p, r.h - p * 0.7f)
        }
    }

    private fun startX(r: RenderCtx, b: RectF) = if (r.ar) b.right else b.left
    private fun al(r: RenderCtx) = if (r.ar) Paint.Align.RIGHT else Paint.Align.LEFT
    private fun small(r: RenderCtx) = r.style.shape == Shape.SMALL
    private fun large(r: RenderCtx) = r.style.shape == Shape.LARGE || r.style.shape == Shape.XL || r.style.shape == Shape.TALL

    // ------------------------------------------------------------------------------------------ paint helpers
    private fun glow(c: Canvas, x: Float, y: Float, rad: Float, color: Int, alpha: Float) {
        if (rad <= 0f || alpha <= 0f) return
        c.drawCircle(x, y, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x, y, rad, intArrayOf(a(color, alpha * 0.75f), a(color, alpha * 0.22f), 0), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        })
    }

    /** A shaded 3D ball: light from the top-left, a rim of reflected light, a soft shadow under it. */
    private fun sphere(c: Canvas, x: Float, y: Float, rad: Float, color: Int, light: Int = Color.WHITE) {
        c.drawCircle(x, y, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x - rad * 0.38f, y - rad * 0.42f, rad * 1.5f,
                intArrayOf(mix(color, light, 0.55f), color, mix(color, Color.BLACK, 0.55f), mix(color, Color.BLACK, 0.8f)), floatArrayOf(0f, 0.35f, 0.8f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(x, y, rad * 0.97f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = rad * 0.06f
            shader = LinearGradient(x, y - rad, x, y + rad, 0, a(light, 0.35f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(x - rad * 0.36f, y - rad * 0.4f, rad * 0.16f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x - rad * 0.36f, y - rad * 0.4f, rad * 0.16f, a(Color.WHITE, 0.85f), 0, Shader.TileMode.CLAMP)
        })
    }

    private fun star4(c: Canvas, x: Float, y: Float, s: Float, color: Int) {
        val p = Path().apply {
            moveTo(x, y - s); quadTo(x, y, x + s, y); quadTo(x, y, x, y + s); quadTo(x, y, x - s, y); quadTo(x, y, x, y - s); close()
        }
        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun rnd(seed: Long) = java.util.Random(seed)

    // ========================================================================================== AMBIENT (whole widget)
    fun stars(r: RenderCtx, t: Float, seed: Long = 7) {
        val g = rnd(seed); val n = count(r, 34)
        repeat(n) {
            val x = g.nextFloat() * r.w; val y = g.nextFloat() * r.h
            val ph = g.nextFloat(); val k = 1 + g.nextInt(2)
            val tw = w01(t, k, ph); val s = (0.7f + g.nextFloat() * 1.3f) * r.u
            r.circle(x, y, s, a(Color.WHITE, 0.15f + 0.6f * tw * tw))
            if (tw > 0.85f && g.nextInt(3) == 0) star4(r.canvas, x, y, s * 4f, a(r.accent, 0.5f * (tw - 0.85f) / 0.15f))
        }
    }

    fun aurora(r: RenderCtx, t: Float) {
        val c = r.canvas
        val cols = intArrayOf(r.accent, r.cfg.accent2, mix(r.accent, r.cfg.accent2, 0.5f))
        for (k in 0 until 3) {
            val base = r.h * (0.22f + k * 0.12f); val amp = r.h * 0.07f
            val path = Path(); val steps = 28
            for (i in 0..steps) {
                val x = r.w * i / steps
                val y = base + amp * sin(x / r.w * TAU * (1 + k * 0.5f) + 0.6f * sin(t * TAU) + k * 1.7f) + amp * 0.4f * sin(x / r.w * TAU * 2.3f - 0.5f * sin(t * TAU + k))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val band = r.h * (0.16f + 0.06f * amt(r))
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = band; strokeCap = Paint.Cap.ROUND
                shader = LinearGradient(0f, base - band, 0f, base + band, intArrayOf(0, a(cols[k], 0.14f), 0), null, Shader.TileMode.CLAMP)
            })
        }
    }

    fun fireflies(r: RenderCtx, t: Float, seed: Long = 3) {
        val g = rnd(seed)
        repeat(count(r, 12)) {
            val x0 = g.nextFloat() * r.w; val y0 = g.nextFloat() * r.h; val ax = (6 + g.nextFloat() * 10) * r.u; val ay = (4 + g.nextFloat() * 8) * r.u
            val ph = g.nextFloat()
            val x = x0 + ax * cos((t + ph) * TAU); val y = y0 + ay * sin((t * 2 + ph) * TAU)
            val b = w01(t, 1, ph * 2)
            glow(r.canvas, x, y, (5 + 4 * b) * r.u, if (g.nextBoolean()) r.accent else r.cfg.accent2, 0.35f + 0.45f * b)
            r.circle(x, y, 1.2f * r.u, a(Color.WHITE, 0.7f + 0.3f * b))
        }
    }

    /** Soft circles rising: a lattice that moves up by exactly one gap per loop. */
    fun bokeh(r: RenderCtx, t: Float, seed: Long = 11) {
        val g = rnd(seed); val cols = count(r, 7); val gap = r.h * 0.55f
        for (i in 0 until cols) {
            val x = (i + 0.5f) / cols * r.w + (g.nextFloat() - 0.5f) * r.w / cols
            val off = g.nextFloat(); val s = (6 + g.nextFloat() * 12) * r.u; val col = if (i % 2 == 0) r.accent else r.cfg.accent2
            var y = g.nextFloat() * gap + 6 * r.u * sin((t + off) * TAU)
            while (y < r.h + gap) { glow(r.canvas, x + 4 * r.u * cos((t + off) * TAU), y, s * 1.6f, col, 0.1f + 0.05f * w01(t, 1, off)); y += gap }
        }
    }

    fun rainStreaks(r: RenderCtx, t: Float, seed: Long = 5) {
        val g = rnd(seed); val cols = count(r, 16); val gap = r.h * 0.45f; val len = 9 * r.u
        val p = r.strokePaint(a(Color.WHITE, 0.28f), 1.1f * r.u)
        for (i in 0 until cols) {
            val x0 = g.nextFloat() * r.w; val off = g.nextFloat()
            var y = -gap + ((t + off) % 1f) * gap
            while (y < r.h + len) { r.canvas.drawLine(x0 - y * 0.12f, y, x0 - (y + len) * 0.12f, y + len, p); y += gap }
        }
    }

    fun snow(r: RenderCtx, t: Float, area: RectF = RectF(0f, 0f, r.w, r.h), seed: Long = 9) {
        val g = rnd(seed); val cols = count(r, 12); val gap = area.height() * 0.5f
        for (i in 0 until cols) {
            val x0 = area.left + g.nextFloat() * area.width(); val off = g.nextFloat(); val s = (1f + g.nextFloat() * 1.6f) * r.u; val ph = g.nextFloat()
            var y = area.top - gap + ((t + off) % 1f) * gap
            var k = 0
            while (y < area.bottom + s) {
                if (y > area.top - s) r.circle(x0 + 3 * r.u * sin((t + ph + k * 0.3f) * TAU), y, s, a(Color.WHITE, 0.75f))
                y += gap; k++
            }
        }
    }

    fun meteors(r: RenderCtx, t: Float, seed: Long = 13) {
        val g = rnd(seed); val n = 1
        repeat(n) {
            val ph = g.nextFloat(); val x0 = r.w * (0.3f + g.nextFloat() * 0.8f); val y0 = -r.h * 0.1f + g.nextFloat() * r.h * 0.3f
            val p = (t + ph) % 1f
            if (p < 0.3f) {
                val q = p / 0.3f; val dx = -r.w * 0.55f * q; val dy = r.h * 0.45f * q
                val hx = x0 + dx; val hy = y0 + dy; val tail = 30 * r.u
                r.canvas.drawLine(hx, hy, hx + tail, hy - tail * 0.82f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    strokeWidth = 1.6f * r.u; strokeCap = Paint.Cap.ROUND
                    shader = LinearGradient(hx, hy, hx + tail, hy - tail * 0.82f, a(Color.WHITE, 0.9f * (1 - q * 0.6f)), 0, Shader.TileMode.CLAMP)
                })
                glow(r.canvas, hx, hy, 4 * r.u, r.accent, 0.8f * (1 - q * 0.6f))
            }
        }
    }

    fun sparkles(r: RenderCtx, t: Float, seed: Long = 17) {
        val g = rnd(seed)
        repeat(count(r, 14)) {
            val x = g.nextFloat() * r.w; val y = g.nextFloat() * r.h; val ph = g.nextFloat()
            val b = w01(t, 1, ph).let { it * it * it }
            if (b > 0.05f) star4(r.canvas, x, y, (2 + 4 * b) * r.u, a(if (g.nextBoolean()) r.accent else Color.WHITE, b * 0.7f))
        }
    }

    fun bubbles(r: RenderCtx, t: Float, seed: Long = 19) {
        val g = rnd(seed); val cols = count(r, 8); val gap = r.h * 0.6f
        for (i in 0 until cols) {
            val x0 = (i + 0.5f) / cols * r.w; val off = g.nextFloat(); val s = (3 + g.nextFloat() * 6) * r.u
            var y = r.h + gap - ((t + off) % 1f) * gap; var k = 0
            while (y > -s) {
                val x = x0 + 4 * r.u * sin((t + off + k * 0.37f) * TAU)
                r.canvas.drawCircle(x, y, s, r.strokePaint(a(Color.WHITE, 0.35f), 1f * r.u))
                r.circle(x - s * 0.35f, y - s * 0.35f, s * 0.22f, a(Color.WHITE, 0.55f))
                y -= gap; k++
            }
        }
    }

    fun wind(r: RenderCtx, t: Float, seed: Long = 23) {
        val g = rnd(seed); val n = count(r, 5); val gap = r.w * 0.45f
        repeat(n) {
            val y = r.h * (0.12f + g.nextFloat() * 0.76f); val off = g.nextFloat(); val len = r.w * (0.18f + g.nextFloat() * 0.15f)
            var x = -gap + ((t + off) % 1f) * gap
            while (x < r.w) {
                val p = Path().apply { moveTo(x, y); cubicTo(x + len * 0.3f, y - 5 * r.u, x + len * 0.7f, y + 5 * r.u, x + len, y) }
                r.canvas.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 1.4f * r.u; strokeCap = Paint.Cap.ROUND
                    shader = LinearGradient(x, y, x + len, y, intArrayOf(0, a(Color.WHITE, 0.22f), 0), null, Shader.TileMode.CLAMP)
                })
                x += gap
            }
        }
    }

    /** A bright segment running round the widget's edge (four of them, each moving a quarter turn per loop). */
    fun neon(r: RenderCtx, t: Float) {
        val rad = Renderer.cornerPx(r); val inset = 2.2f * r.u
        val path = Path().apply { addRoundRect(RectF(inset, inset, r.w - inset, r.h - inset), rad - inset, rad - inset, Path.Direction.CW) }
        val per = android.graphics.PathMeasure(path, false).length
        val seg = per / 8f
        fun pass(width: Float, alpha: Float, col: Int) = r.canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = width; strokeCap = Paint.Cap.ROUND; color = a(col, alpha)
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(seg * 0.45f, seg * 0.55f), -t * seg)
        })
        pass(8 * r.u, 0.07f * amt(r), r.accent); pass(4 * r.u, 0.16f, r.accent); pass(1.4f * r.u, 0.6f, mix(r.accent, Color.WHITE, 0.35f))
        r.canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 1.6f * r.u; color = a(r.cfg.accent2, 0.35f)
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(seg * 0.2f, seg * 0.8f), -t * seg + seg * 0.5f)
        })
    }

    // ========================================================================================== OBJECTS (inside the zone)
    /** A ringed planet: shaded ball, a tilted ring passing in front and behind, four moons orbiting. */
    fun planet(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.27f
        val rx = R * 1.75f; val ry = R * 0.42f
        glow(c, cx, cy, R * 2.2f, r.accent, 0.3f * amt(r))
        fun moon(i: Int, front: Boolean) {
            val th = (t / 6f + i / 6f) * TAU
            val s = sin(th); if ((s > 0) != front) return
            val mx = cx + rx * 1.12f * cos(th); val my = cy + ry * 1.12f * s
            c.save(); c.rotate(-16f, cx, cy)
            sphere(c, mx, my, R * (0.1f + 0.05f * (s + 1)), if (i % 2 == 0) r.cfg.accent2 else mix(r.c1, r.accent, 0.3f))
            c.restore()
        }
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = R * 0.16f
            shader = LinearGradient(cx - rx, cy, cx + rx, cy, intArrayOf(a(r.cfg.accent2, 0.25f), a(mix(r.cfg.accent2, Color.WHITE, 0.4f), 0.9f), a(r.cfg.accent2, 0.25f)), null, Shader.TileMode.CLAMP)
        }
        c.save(); c.rotate(-16f, cx, cy)
        c.drawArc(RectF(cx - rx, cy - ry, cx + rx, cy + ry), 180f, 180f, false, ring)
        c.restore()
        for (i in 0 until 6) moon(i, false)
        sphere(c, cx, cy, R, r.accent)
        // drifting cloud bands on the ball, clipped to it (they move one band spacing per loop)
        c.save(); c.clipPath(Path().apply { addCircle(cx, cy, R, Path.Direction.CW) })
        val bandGap = R * 0.9f
        for (k in -2..3) {
            val bx = cx - R * 1.5f + (k + t) * bandGap
            c.drawOval(RectF(bx - R * 0.5f, cy - R * 0.32f, bx + R * 0.5f, cy - R * 0.18f), r.fillPaint(a(Color.WHITE, 0.12f)))
            c.drawOval(RectF(bx - R * 0.3f, cy + R * 0.18f, bx + R * 0.6f, cy + R * 0.3f), r.fillPaint(a(Color.BLACK, 0.12f)))
        }
        c.restore()
        c.save(); c.rotate(-16f, cx, cy)
        c.drawArc(RectF(cx - rx, cy - ry, cx + rx, cy + ry), 0f, 180f, false, ring)
        c.restore()
        for (i in 0 until 6) moon(i, true)
    }

    /** A sun with slowly turning rays (12 rays → one ray spacing per loop) and a breathing glow. */
    fun sun(r: RenderCtx, z: RectF, t: Float, cx: Float = z.centerX(), cy: Float = z.centerY(), scale: Float = 1f) {
        val c = r.canvas; val R = min(z.width(), z.height()) * 0.2f * scale
        val col = r.accent
        glow(c, cx, cy, R * (3.1f + 0.3f * wave(t)), col, 0.45f * amt(r).coerceAtMost(1.2f))
        sphere(c, cx, cy, R, mix(col, 0xFFFFE08A.toInt(), 0.4f), 0xFFFFF6D8.toInt())
    }

    /** A crescent with a halo that breathes and little stars blinking round it. */
    fun crescent(r: RenderCtx, z: RectF, t: Float, cx: Float = z.centerX(), cy: Float = z.centerY(), scale: Float = 1f) {
        val c = r.canvas; val R = min(z.width(), z.height()) * 0.27f * scale
        glow(c, cx, cy, R * (2.3f + 0.25f * wave(t)), r.accent, 0.42f * amt(r).coerceAtMost(1.2f))
        val l = c.saveLayer(cx - R * 1.2f, cy - R * 1.2f, cx + R * 1.2f, cy + R * 1.2f, null)
        sphere(c, cx, cy, R, mix(r.accent, 0xFFFFF3D6.toInt(), 0.55f), Color.WHITE)
        c.drawCircle(cx + R * 0.48f, cy - R * 0.22f, R * 0.88f, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) })
        c.restoreToCount(l)
        val g = rnd(31)
        repeat(count(r, 5)) {
            val ang = g.nextFloat() * TAU; val d = R * (1.45f + g.nextFloat() * 0.6f)
            val b = w01(t, 1, g.nextFloat())
            star4(c, cx + d * cos(ang), cy + d * sin(ang), (1.5f + 3.5f * b) * r.u, a(Color.WHITE, 0.3f + 0.7f * b))
        }
    }

    /** A Ramadan lantern swinging on its chain, its flame flickering. */
    fun lantern(r: RenderCtx, cx: Float, top: Float, size: Float, t: Float, ph: Float = 0f) {
        val c = r.canvas
        val ang = 7f * wave(t, 1, ph)
        val gold = r.cfg.accent2; val light = r.accent
        c.save(); c.rotate(ang, cx, top)
        c.drawLine(cx, top, cx, top + size * 0.22f, r.strokePaint(a(gold, 0.9f), size * 0.025f))
        val y0 = top + size * 0.22f
        c.drawCircle(cx, y0, size * 0.04f, r.strokePaint(gold, size * 0.02f))
        // cap
        val cap = Path().apply { moveTo(cx, y0 + size * 0.02f); lineTo(cx + size * 0.2f, y0 + size * 0.17f); lineTo(cx - size * 0.2f, y0 + size * 0.17f); close() }
        c.drawPath(cap, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(cx - size * 0.2f, 0f, cx + size * 0.2f, 0f, mix(gold, Color.BLACK, 0.3f), mix(gold, Color.WHITE, 0.35f), Shader.TileMode.MIRROR) })
        // glass body with a warm flickering light
        val bt = y0 + size * 0.17f; val bb = bt + size * 0.42f
        val body = Path().apply {
            moveTo(cx - size * 0.2f, bt); lineTo(cx + size * 0.2f, bt); lineTo(cx + size * 0.26f, bt + size * 0.2f); lineTo(cx + size * 0.16f, bb)
            lineTo(cx - size * 0.16f, bb); lineTo(cx - size * 0.26f, bt + size * 0.2f); close()
        }
        val fl = 0.75f + 0.25f * w01(t, 3, ph) * w01(t, 2, ph + 0.3f)
        glow(c, cx, bt + size * 0.2f, size * 0.75f * fl, light, 0.55f * fl)
        c.drawPath(body, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, bt + size * 0.22f, size * 0.32f, intArrayOf(a(mix(light, Color.WHITE, 0.6f), fl), a(light, 0.85f * fl), a(mix(light, Color.BLACK, 0.5f), 0.9f)), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        })
        c.drawPath(body, r.strokePaint(gold, size * 0.022f))
        c.drawLine(cx, bt, cx, bb, r.strokePaint(a(gold, 0.8f), size * 0.016f))
        c.drawLine(cx - size * 0.26f, bt + size * 0.2f, cx + size * 0.26f, bt + size * 0.2f, r.strokePaint(a(gold, 0.8f), size * 0.016f))
        // base and drop
        c.drawRoundRect(RectF(cx - size * 0.19f, bb, cx + size * 0.19f, bb + size * 0.06f), size * 0.02f, size * 0.02f, r.fillPaint(gold))
        val drop = Path().apply { moveTo(cx - size * 0.08f, bb + size * 0.06f); lineTo(cx + size * 0.08f, bb + size * 0.06f); lineTo(cx, bb + size * 0.16f); close() }
        c.drawPath(drop, r.fillPaint(mix(gold, Color.BLACK, 0.2f)))
        c.restore()
    }

    /** A mosque on the horizon: dome and two minarets, windows that glow on and off softly. */
    fun mosque(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val w = z.width(); val bx = z.centerX(); val by = z.bottom - z.height() * 0.06f
        val ink = mix(r.cfg.bgColor, Color.BLACK, 0.55f)
        val dome = w * 0.2f
        glow(c, bx, by - dome * 1.4f, w * 0.55f, r.accent, 0.22f * amt(r))
        val p = Path().apply {
            addRect(bx - w * 0.36f, by - dome * 0.9f, bx + w * 0.36f, by, Path.Direction.CW)
            addOval(RectF(bx - dome, by - dome * 2.4f, bx + dome, by - dome * 0.4f), Path.Direction.CW)
            for (s in listOf(-1, 1)) {
                val mx = bx + s * w * 0.38f
                addRect(mx - w * 0.035f, by - dome * 3f, mx + w * 0.035f, by, Path.Direction.CW)
                addOval(RectF(mx - w * 0.05f, by - dome * 3.15f, mx + w * 0.05f, by - dome * 2.85f), Path.Direction.CW)
                moveTo(mx - w * 0.04f, by - dome * 3.05f); lineTo(mx, by - dome * 3.6f); lineTo(mx + w * 0.04f, by - dome * 3.05f); close()
            }
            addRect(bx - w * 0.008f, by - dome * 2.8f, bx + w * 0.008f, by - dome * 2.35f, Path.Direction.CW)
        }
        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, by - dome * 3.6f, 0f, by, mix(ink, r.accent, 0.12f), ink, Shader.TileMode.CLAMP) })
        // crescent finial
        r.circle(bx, by - dome * 2.92f, dome * 0.13f, r.accent)
        r.circle(bx + dome * 0.06f, by - dome * 2.95f, dome * 0.11f, ink)
        // windows
        for (i in -2..2) {
            val wx = bx + i * w * 0.11f; val on = w01(t, 1, i * 0.21f + 0.1f)
            val wr = RectF(wx - w * 0.025f, by - dome * 0.62f, wx + w * 0.025f, by - dome * 0.18f)
            glow(c, wr.centerX(), wr.centerY(), w * 0.07f, r.accent, 0.35f * on)
            c.drawRoundRect(wr, w * 0.025f, w * 0.025f, r.fillPaint(a(mix(r.accent, Color.WHITE, 0.3f), 0.45f + 0.5f * on)))
        }
    }

    /** Weather in motion: the sky object for the condition, bobbing, with rain, snow or fog when there is. */
    fun weatherObj(r: RenderCtx, z: RectF, t: Float, cond: Cond, night: Boolean) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY() - z.height() * 0.06f; val s = min(z.width(), z.height())
        val bob = wave(t) * s * 0.025f
        fun cloud(x: Float, y: Float, k: Float, col: Int) {
            val w = s * 0.5f * k
            c.drawCircle(x + 6 * r.u * k, y + 8 * r.u * k, 0f, r.fillPaint(0))
            val p = Path().apply {
                addCircle(x - w * 0.28f, y + w * 0.06f, w * 0.26f, Path.Direction.CW)
                addCircle(x + w * 0.02f, y - w * 0.1f, w * 0.34f, Path.Direction.CW)
                addCircle(x + w * 0.32f, y + w * 0.08f, w * 0.24f, Path.Direction.CW)
                addRoundRect(RectF(x - w * 0.52f, y + w * 0.02f, x + w * 0.56f, y + w * 0.32f), w * 0.15f, w * 0.15f, Path.Direction.CW)
            }
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, y - w * 0.45f, 0f, y + w * 0.32f, Color.WHITE, mix(col, 0xFF8E96A8.toInt(), 0.5f), Shader.TileMode.CLAMP) })
        }
        when (cond) {
            Cond.CLEAR -> if (night) crescent(r, z, t) else sun(r, z, t, scale = 1.1f)
            Cond.PARTLY -> {
                if (night) crescent(r, z, t, cx + s * 0.13f, cy - s * 0.12f, 0.75f) else sun(r, z, t, cx + s * 0.12f, cy - s * 0.12f, 0.8f)
                cloud(cx - s * 0.06f + wave(t) * s * 0.04f, cy + s * 0.08f, 1f, Color.WHITE)
            }
            Cond.CLOUDY, Cond.FOG -> {
                cloud(cx + s * 0.12f - wave(t) * s * 0.03f, cy - s * 0.08f, 0.75f, 0xFFB8BCC6.toInt())
                cloud(cx - s * 0.05f + wave(t) * s * 0.04f, cy + s * 0.06f, 1f, Color.WHITE)
                if (cond == Cond.FOG) for (k in 0 until 3) {
                    val y = cy + s * (0.28f + k * 0.08f); val dx = wave(t, 1, k * 0.3f) * s * 0.06f
                    c.drawRoundRect(RectF(z.left + s * 0.1f + dx, y, z.right - s * 0.1f + dx, y + s * 0.035f), s * 0.02f, s * 0.02f, r.fillPaint(a(Color.WHITE, 0.35f)))
                }
            }
            Cond.RAIN, Cond.STORM -> {
                cloud(cx, cy - s * 0.04f + bob, 1.05f, 0xFF9AA3B5.toInt())
                val area = RectF(cx - s * 0.26f, cy + s * 0.14f, cx + s * 0.28f, z.bottom)
                val gap = area.height() * 0.55f; val n = count(r, 7)
                val p = r.strokePaint(a(0xFF9AD0FF.toInt(), 0.9f), 1.8f * r.u)
                for (i in 0 until n) {
                    val x = area.left + (i + 0.5f) / n * area.width(); val off = (i * 0.37f) % 1f
                    var y = area.top - gap + ((t + off) % 1f) * gap
                    while (y < area.bottom) { if (y > area.top) c.drawLine(x, y, x - 2 * r.u, y + 7 * r.u, p); y += gap }
                }
                if (cond == Cond.STORM) {
                    val f = w01(t, 1).let { it * it * it * it }
                    val bolt = Path().apply { moveTo(cx + s * 0.02f, cy + s * 0.12f); lineTo(cx - s * 0.06f, cy + s * 0.28f); lineTo(cx + s * 0.01f, cy + s * 0.28f); lineTo(cx - s * 0.05f, cy + s * 0.44f) }
                    c.drawPath(bolt, r.strokePaint(a(0xFFFFE27A.toInt(), 0.35f + 0.65f * f), 2.4f * r.u))
                    glow(c, cx - s * 0.02f, cy + s * 0.28f, s * 0.3f, 0xFFFFE27A.toInt(), 0.35f * f)
                }
            }
            Cond.SNOW -> {
                cloud(cx, cy - s * 0.04f + bob, 1.05f, 0xFFC9D2E2.toInt())
                snow(r, t, RectF(cx - s * 0.28f, cy + s * 0.16f, cx + s * 0.28f, z.bottom))
            }
        }
    }

    /** Three tilted rings spinning round a glowing core (a gyroscope). */
    fun gyro(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.38f
        glow(c, cx, cy, R * 1.3f, r.accent, 0.35f * amt(r))
        val cols = intArrayOf(r.accent, r.cfg.accent2, mix(r.c1, r.accent, 0.4f))
        for (k in 0 until 3) {
            val ry = R * (0.32f + 0.08f * sin((t + k / 3f) * TAU))
            c.save(); c.rotate(k * 60f + t * 60f, cx, cy)
            c.drawOval(RectF(cx - R, cy - max(ry, 1f), cx + R, cy + max(ry, 1f)), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2.6f * r.u
                shader = LinearGradient(cx, cy - R, cx, cy + R, a(cols[k], 0.95f), a(cols[k], 0.35f), Shader.TileMode.CLAMP)
            })
            c.restore()
        }
        sphere(c, cx, cy, R * (0.24f + 0.02f * wave(t, 2)), mix(r.accent, Color.WHITE, 0.2f))
    }

    /** Ripples spreading from a glowing dot; three at once, a third of a cycle apart. */
    fun pulse(r: RenderCtx, z: RectF, t: Float, cx: Float = z.centerX(), cy: Float = z.centerY()) {
        val c = r.canvas; val R = min(z.width(), z.height()) * 0.46f
        for (k in 0 until 3) {
            val p = (t + k / 3f) % 1f
            c.drawCircle(cx, cy, R * (0.15f + 0.85f * p), r.strokePaint(a(if (k == 1) r.cfg.accent2 else r.accent, (1 - p) * 0.8f), (2.5f - 1.5f * p) * r.u))
        }
        glow(c, cx, cy, R * 0.55f, r.accent, 0.5f)
        sphere(c, cx, cy, R * 0.16f, r.accent)
    }

    /** A glass orb filled with liquid up to [level], its surface rolling. */
    fun orb(r: RenderCtx, z: RectF, t: Float, level: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.4f
        glow(c, cx, cy, R * 1.45f, r.accent, 0.28f * amt(r))
        c.drawCircle(cx, cy, R, r.fillPaint(a(Color.WHITE, 0.06f)))
        c.save(); c.clipPath(Path().apply { addCircle(cx, cy, R * 0.94f, Path.Direction.CW) })
        val top = cy + R - 2 * R * level.coerceIn(0.04f, 0.96f)
        val lam = R * 1.2f
        for (k in 0 until 2) {
            val amp = R * (0.07f - k * 0.025f); val ph = if (k == 0) t else -t + 0.5f
            val p = Path().apply {
                moveTo(cx - R * 1.2f, cy + R * 1.2f)
                var x = cx - R * 1.2f
                while (x <= cx + R * 1.2f) { lineTo(x, top + amp * sin(((x - cx) / lam + ph) * TAU)); x += R * 0.05f }
                lineTo(cx + R * 1.2f, cy + R * 1.2f); close()
            }
            val col = if (k == 0) r.accent else r.cfg.accent2
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, top, 0f, cy + R, a(mix(col, Color.WHITE, 0.2f), if (k == 0) 0.9f else 0.55f), a(mix(col, Color.BLACK, 0.4f), 0.95f), Shader.TileMode.CLAMP) })
        }
        // bubbles in the liquid
        val g = rnd(41)
        repeat(count(r, 5)) {
            val bx = cx + (g.nextFloat() - 0.5f) * R * 1.2f; val off = g.nextFloat(); val span = cy + R - top
            val by = cy + R - ((t + off) % 1f) * span
            if (by > top) c.drawCircle(bx, by, (1 + g.nextFloat() * 1.5f) * r.u, r.strokePaint(a(Color.WHITE, 0.6f), 0.9f * r.u))
        }
        c.restore()
        c.drawCircle(cx, cy, R, r.strokePaint(a(Color.WHITE, 0.4f), 1.6f * r.u))
        c.drawArc(RectF(cx - R * 0.8f, cy - R * 0.8f, cx + R * 0.8f, cy + R * 0.8f), 200f, 50f, false, r.strokePaint(a(Color.WHITE, 0.55f), 2.4f * r.u))
    }

    /** A cube turning on its vertical axis (a quarter turn per loop — a cube looks the same after that). */
    fun cube(r: RenderCtx, z: RectF, t: Float, octa: Boolean = false) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val S = min(z.width(), z.height()) * (if (octa) 0.36f else 0.24f)
        glow(c, cx, cy, S * 2.4f, r.accent, 0.3f * amt(r))
        val th = (t * 90f + 20f) * PI.toFloat() / 180f; val tilt = 0.42f
        fun proj(x: Float, y: Float, z0: Float): Triple<Float, Float, Float> {
            val x1 = x * cos(th) + z0 * sin(th); val z1 = -x * sin(th) + z0 * cos(th)
            val y2 = y * cos(tilt) - z1 * sin(tilt); val z2 = y * sin(tilt) + z1 * cos(tilt)
            val k = 3.2f / (3.2f + z2 * 0.35f)
            return Triple(cx + x1 * S * k, cy + y2 * S * k, z2)
        }
        val verts: List<Triple<Float, Float, Float>>
        val faces: List<IntArray>
        if (!octa) {
            verts = listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).flatMap { (x, y) -> listOf(Triple(x, y, -1f)) } + listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).map { (x, y) -> Triple(x, y, 1f) }
            faces = listOf(intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6, 7), intArrayOf(0, 1, 5, 4), intArrayOf(2, 3, 7, 6), intArrayOf(1, 2, 6, 5), intArrayOf(0, 3, 7, 4))
        } else {
            verts = listOf(Triple(1f, 0f, 0f), Triple(-1f, 0f, 0f), Triple(0f, -1.3f, 0f), Triple(0f, 1.3f, 0f), Triple(0f, 0f, 1f), Triple(0f, 0f, -1f))
            faces = listOf(intArrayOf(0, 2, 4), intArrayOf(4, 2, 1), intArrayOf(1, 2, 5), intArrayOf(5, 2, 0), intArrayOf(0, 3, 4), intArrayOf(4, 3, 1), intArrayOf(1, 3, 5), intArrayOf(5, 3, 0))
        }
        val p = verts.map { proj(it.first, it.second, it.third) }
        faces.sortedByDescending { f -> f.sumOf { p[it].third.toDouble() } }.forEach { f ->
            val path = Path().apply { moveTo(p[f[0]].first, p[f[0]].second); for (i in 1 until f.size) lineTo(p[f[i]].first, p[f[i]].second); close() }
            // light from the top-left: shade by the face's facing (2D cross product sign & size)
            val ax = p[f[1]].first - p[f[0]].first; val ay = p[f[1]].second - p[f[0]].second
            val bx = p[f[2]].first - p[f[0]].first; val by = p[f[2]].second - p[f[0]].second
            val cross = ax * by - ay * bx
            val avgY = f.map { p[it].second }.average().toFloat()
            val lit = ((cy - avgY) / S * 0.5f + 0.5f).coerceIn(0f, 1f)
            val base = if (cross > 0) mix(r.accent, r.cfg.accent2, 0.5f - lit * 0.5f) else mix(r.cfg.accent2, Color.BLACK, 0.3f)
            c.drawPath(path, r.fillPaint(a(mix(base, Color.WHITE, lit * 0.35f), 0.82f)))
            c.drawPath(path, r.strokePaint(a(mix(r.accent, Color.WHITE, 0.6f), 0.85f), 1.3f * r.u))
        }
    }

    /** An hourglass: the sand stream falls (a lattice moving one gap per loop); the piles breathe slightly. */
    fun hourglass(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val h = z.height() * 0.8f; val w = h * 0.55f; val top = z.centerY() - h / 2; val bot = top + h
        glow(c, cx, z.centerY(), h * 0.7f, r.accent, 0.25f * amt(r))
        val gold = r.cfg.accent2
        c.drawRoundRect(RectF(cx - w * 0.62f, top - h * 0.05f, cx + w * 0.62f, top + h * 0.02f), h * 0.02f, h * 0.02f, r.fillPaint(gold))
        c.drawRoundRect(RectF(cx - w * 0.62f, bot - h * 0.02f, cx + w * 0.62f, bot + h * 0.05f), h * 0.02f, h * 0.02f, r.fillPaint(gold))
        val glass = Path().apply {
            moveTo(cx - w / 2, top + h * 0.02f); cubicTo(cx - w / 2, top + h * 0.3f, cx - w * 0.06f, top + h * 0.42f, cx - w * 0.06f, top + h / 2)
            cubicTo(cx - w * 0.06f, top + h * 0.58f, cx - w / 2, top + h * 0.7f, cx - w / 2, bot - h * 0.02f)
            lineTo(cx + w / 2, bot - h * 0.02f); cubicTo(cx + w / 2, top + h * 0.7f, cx + w * 0.06f, top + h * 0.58f, cx + w * 0.06f, top + h / 2)
            cubicTo(cx + w * 0.06f, top + h * 0.42f, cx + w / 2, top + h * 0.3f, cx + w / 2, top + h * 0.02f); close()
        }
        c.drawPath(glass, r.fillPaint(a(Color.WHITE, 0.08f)))
        c.save(); c.clipPath(glass)
        val sand = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, top, 0f, bot, r.accent, mix(r.accent, Color.BLACK, 0.35f), Shader.TileMode.CLAMP) }
        c.drawRect(cx - w, top + h * 0.26f, cx + w, top + h / 2, sand)
        val pile = Path().apply { moveTo(cx - w * 0.5f, bot); quadTo(cx, bot - h * 0.32f, cx + w * 0.5f, bot); close() }
        c.drawPath(pile, sand)
        val gap = h * 0.06f
        var y = top + h / 2 + (t * gap)
        while (y < bot - h * 0.2f) { r.circle(cx, y, 1.3f * r.u, r.accent); y += gap }
        c.drawLine(cx, top + h / 2, cx, bot - h * 0.2f, r.strokePaint(a(r.accent, 0.5f), 1f * r.u))
        c.restore()
        c.drawPath(glass, r.strokePaint(a(Color.WHITE, 0.45f), 1.4f * r.u))
    }

    /** A compass whose needle settles on the Qibla, swaying a little either side of it. */
    fun compass(r: RenderCtx, z: RectF, t: Float, bearing: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.44f
        glow(c, cx, cy, R * 1.3f, r.accent, 0.22f * amt(r))
        c.drawCircle(cx, cy, R, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, cy - R * 0.3f, R * 1.3f, a(Color.WHITE, 0.12f), a(Color.BLACK, 0.25f), Shader.TileMode.CLAMP) })
        c.drawCircle(cx, cy, R, r.strokePaint(a(r.cfg.accent2, 0.8f), 1.8f * r.u))
        for (i in 0 until 36) {
            c.save(); c.rotate(i * 10f, cx, cy)
            val l = if (i % 9 == 0) R * 0.16f else R * 0.07f
            c.drawLine(cx, cy - R + 3 * r.u, cx, cy - R + 3 * r.u + l, r.strokePaint(a(r.c1, if (i % 9 == 0) 0.9f else 0.35f), 1.2f * r.u))
            c.restore()
        }
        c.drawPath(starPath(cx, cy, R * 0.62f, R * 0.26f, 8), r.fillPaint(a(r.cfg.accent2, 0.14f)))
        c.drawPath(starPath(cx, cy, R * 0.62f, R * 0.26f, 8), r.strokePaint(a(r.cfg.accent2, 0.45f), 0.9f * r.u))
        val ang = bearing + 4f * wave(t)
        c.save(); c.rotate(ang, cx, cy)
        val n = Path().apply { moveTo(cx, cy - R * 0.78f); lineTo(cx + R * 0.1f, cy); lineTo(cx, cy + R * 0.12f); lineTo(cx - R * 0.1f, cy); close() }
        glow(c, cx, cy - R * 0.55f, R * 0.4f, r.accent, 0.5f)
        c.drawPath(n, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(cx - R * 0.1f, 0f, cx + R * 0.1f, 0f, r.accent, mix(r.accent, Color.BLACK, 0.4f), Shader.TileMode.CLAMP) })
        val s = Path().apply { moveTo(cx, cy + R * 0.6f); lineTo(cx + R * 0.07f, cy); lineTo(cx - R * 0.07f, cy); close() }
        c.drawPath(s, r.fillPaint(a(r.c1, 0.35f)))
        // a tiny Kaaba cube at the tip
        c.drawRect(cx - R * 0.07f, cy - R * 0.92f, cx + R * 0.07f, cy - R * 0.78f, r.fillPaint(0xFF141414.toInt()))
        c.drawRect(cx - R * 0.07f, cy - R * 0.89f, cx + R * 0.07f, cy - R * 0.87f, r.fillPaint(r.cfg.accent2))
        c.restore()
        sphere(c, cx, cy, R * 0.08f, r.cfg.accent2)
    }

    /** 33 prayer beads on a tilted loop, sliding round one bead per loop (front beads bigger and brighter). */
    fun beadsRing(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val rx = min(z.width(), z.height()) * 0.42f; val ry = rx * 0.45f
        glow(c, cx, cy, rx * 1.2f, r.accent, 0.2f * amt(r))
        data class B(val x: Float, val y: Float, val d: Float, val i: Int)
        val beads = (0 until 33).map { i ->
            val th = (i + t) / 33f * TAU
            B(cx + rx * cos(th), cy + ry * sin(th), sin(th), i)
        }.sortedBy { it.d }
        c.drawOval(RectF(cx - rx, cy - ry, cx + rx, cy + ry), r.strokePaint(a(r.cfg.accent2, 0.35f), 1f * r.u))
        beads.forEach { b ->
            val k = 0.65f + 0.35f * (b.d + 1) / 2
            sphere(c, b.x, b.y, rx * 0.075f * k, if (b.i == 0) r.cfg.accent2 else mix(r.accent, Color.BLACK, 0.25f * (1 - k)))
        }
        // the tassel at the bottom
        val tx = cx; val ty = cy + ry
        c.drawLine(tx, ty, tx, ty + rx * 0.3f, r.strokePaint(r.cfg.accent2, 1.6f * r.u))
        sphere(c, tx, ty + rx * 0.34f, rx * 0.08f, r.cfg.accent2)
    }

    /** Fireworks: three bursts a third of a cycle apart, sparks fading as they spread and fall. */
    fun fireworks(r: RenderCtx, area: RectF, t: Float) {
        val c = r.canvas
        val cols = intArrayOf(r.accent, r.cfg.accent2, mix(r.c1, r.accent, 0.3f))
        val g = rnd(53)
        for (k in 0 until 2) {
            val bx = area.left + area.width() * (0.2f + g.nextFloat() * 0.6f); val by = area.top + area.height() * (0.2f + g.nextFloat() * 0.45f)
            val p = (t + k / 2f) % 1f
            val R = min(area.width(), area.height()) * (0.28f + 0.1f * g.nextFloat())
            val n = count(r, 14)
            if (p < 0.18f) {
                // the rocket going up
                val q = p / 0.18f
                val ry = area.bottom - (area.bottom - by) * q
                c.drawLine(bx, ry, bx, ry + 10 * r.u, r.strokePaint(a(cols[k], 0.8f), 1.4f * r.u))
                continue
            }
            val q = (p - 0.18f) / 0.82f
            val rad = R * (1 - (1 - q) * (1 - q))
            val al = (1 - q).let { it * it }
            glow(c, bx, by, R * 0.6f * (1 - q), cols[k], 0.5f * (1 - q))
            for (i in 0 until n) {
                val th = i.toFloat() / n * TAU
                val x = bx + rad * cos(th); val y = by + rad * sin(th) + R * 0.25f * q * q
                val x2 = bx + rad * 0.82f * cos(th); val y2 = by + rad * 0.82f * sin(th) + R * 0.25f * q * q * 0.8f
                c.drawLine(x2, y2, x, y, r.strokePaint(a(cols[k], al), 1.5f * r.u))
                r.circle(x, y, 1.6f * r.u, a(Color.WHITE, al))
            }
        }
    }

    fun balloons(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val s = min(z.width(), z.height())
        val cols = intArrayOf(r.accent, r.cfg.accent2, mix(r.accent, r.cfg.accent2, 0.5f))
        val pos = listOf(-0.26f to 0.06f, 0f to -0.06f, 0.26f to 0.08f)
        pos.forEachIndexed { i, (dx, dy) ->
            val x = z.centerX() + dx * s + wave(t, 1, i * 0.3f) * s * 0.02f; val y = z.centerY() + dy * s + wave(t, 1, i * 0.27f + 0.2f) * s * 0.04f - s * 0.08f
            val br = s * 0.15f
            val str = Path().apply { moveTo(x, y + br * 1.15f); quadTo(x + br * 0.3f * wave(t, 1, i * 0.2f), y + br * 1.8f, z.centerX(), z.bottom - s * 0.04f) }
            c.drawPath(str, r.strokePaint(a(r.c1, 0.45f), 0.9f * r.u))
            c.save(); c.scale(1f, 1.18f, x, y)
            sphere(c, x, y, br, cols[i])
            c.restore()
            c.drawPath(Path().apply { moveTo(x - br * 0.12f, y + br * 1.22f); lineTo(x + br * 0.12f, y + br * 1.22f); lineTo(x, y + br * 1.1f); close() }, r.fillPaint(cols[i]))
        }
    }

    /** A spiral galaxy turning (two arms → half a turn per loop looks continuous). */
    fun galaxy(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.46f
        glow(c, cx, cy, R, r.accent, 0.35f * amt(r))
        val g = rnd(61); val n = 0
        c.save()
        repeat(n) {
            val arm = it % 4; val d = g.nextFloat().let { q -> q * q } * R
            val th = d / R * 4.2f + arm * PI.toFloat() / 2 + (g.nextFloat() - 0.5f) * 0.45f
            val x = cx + d * cos(th); val y = cy + d * sin(th)
            val col = mix(r.cfg.accent2, r.accent, d / R)
            r.circle(x, y, (0.6f + g.nextFloat() * 1.2f) * r.u * (1.3f - d / R), a(col, 0.4f + 0.6f * (1 - d / R)))
        }
        c.restore()
        glow(c, cx, cy, R * 0.25f, Color.WHITE, 0.7f)
    }

    /** A DNA-like double helix of light drifting along (one wavelength per loop). */
    fun helix(r: RenderCtx, area: RectF, t: Float) {
        val c = r.canvas; val n = count(r, 22); val amp = area.height() * 0.32f; val cy = area.centerY()
        val lam = area.width() / 1.5f
        for (i in 0 until n) {
            val x = area.left + area.width() * i / (n - 1)
            val ph = (x - area.left) / lam - t
            val y1 = cy + amp * sin(ph * TAU); val y2 = cy - amp * sin(ph * TAU)
            val d = cos(ph * TAU)
            c.drawLine(x, y1, x, y2, r.strokePaint(a(Color.WHITE, 0.12f), 1f * r.u))
            r.circle(x, y1, (1.6f + 1.2f * d) * r.u, a(r.accent, 0.5f + 0.4f * d))
            r.circle(x, y2, (1.6f - 1.2f * d) * r.u, a(r.cfg.accent2, 0.5f - 0.4f * d))
        }
    }

    /** The Earth (or any globe) turning: meridians slide round, one spacing per loop. */
    fun globe(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.38f
        glow(c, cx, cy, R * 1.5f, r.accent, 0.3f * amt(r))
        sphere(c, cx, cy, R, mix(r.accent, 0xFF1C4E8A.toInt(), 0.45f))
        c.save(); c.clipPath(Path().apply { addCircle(cx, cy, R, Path.Direction.CW) })
        val line = r.strokePaint(a(Color.WHITE, 0.3f), 1f * r.u)
        for (k in 0 until 6) {
            val phi = ((k + t) * 30f) * PI.toFloat() / 180f
            val rx = R * abs(cos(phi))
            c.drawOval(RectF(cx - rx, cy - R, cx + rx, cy + R), line)
        }
        for (k in -2..2) { val yy = cy + k * R * 0.38f; val hw = sqrt(max(0f, R * R - (yy - cy) * (yy - cy))); c.drawLine(cx - hw, yy, cx + hw, yy, line) }
        // a couple of "continents" drifting across
        val g = rnd(71)
        repeat(4) {
            val x0 = (g.nextFloat() + t / 6f * 2f) % 1f; val y0 = cy + (g.nextFloat() - 0.5f) * R * 1.2f
            val x = cx - R + x0 * 2 * R; val ww = R * (0.25f + g.nextFloat() * 0.25f)
            c.drawOval(RectF(x - ww, y0 - ww * 0.4f, x + ww, y0 + ww * 0.4f), r.fillPaint(a(r.cfg.accent2, 0.35f)))
        }
        c.restore()
    }

    fun snowGlobe(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val R = min(z.width(), z.height()) * 0.38f; val cy = z.centerY() - R * 0.12f
        glow(c, cx, cy, R * 1.4f, r.accent, 0.25f * amt(r))
        c.drawRoundRect(RectF(cx - R * 0.8f, cy + R * 0.78f, cx + R * 0.8f, cy + R * 1.18f), R * 0.12f, R * 0.12f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, cy + R * 0.78f, 0f, cy + R * 1.18f, r.cfg.accent2, mix(r.cfg.accent2, Color.BLACK, 0.5f), Shader.TileMode.CLAMP)
        })
        c.drawCircle(cx, cy, R, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, cy - R * 0.3f, R * 1.2f, a(r.accent, 0.35f), a(mix(r.accent, Color.BLACK, 0.6f), 0.7f), Shader.TileMode.CLAMP) })
        c.save(); c.clipPath(Path().apply { addCircle(cx, cy, R * 0.96f, Path.Direction.CW) })
        // a little tree and a snow hill inside
        c.drawOval(RectF(cx - R * 1.1f, cy + R * 0.45f, cx + R * 1.1f, cy + R * 1.4f), r.fillPaint(a(Color.WHITE, 0.9f)))
        val tree = Path().apply { moveTo(cx, cy - R * 0.45f); lineTo(cx + R * 0.28f, cy + R * 0.5f); lineTo(cx - R * 0.28f, cy + R * 0.5f); close() }
        c.drawPath(tree, r.fillPaint(mix(0xFF2E7D5B.toInt(), r.accent, 0.2f)))
        snow(r, t, RectF(cx - R, cy - R, cx + R, cy + R), seed = 77)
        c.restore()
        c.drawCircle(cx, cy, R, r.strokePaint(a(Color.WHITE, 0.45f), 1.5f * r.u))
        c.drawArc(RectF(cx - R * 0.78f, cy - R * 0.78f, cx + R * 0.78f, cy + R * 0.78f), 205f, 45f, false, r.strokePaint(a(Color.WHITE, 0.6f), 2.4f * r.u))
    }

    /** The sun sitting on the sea, its reflection shimmering in moving lines. */
    fun sunset(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val hy = z.top + z.height() * 0.62f; val R = min(z.width(), z.height()) * 0.24f
        glow(c, cx, hy, R * 3f, r.accent, 0.45f * amt(r).coerceAtMost(1.2f))
        c.save(); c.clipRect(z.left - R, z.top - R, z.right + R, hy)
        sphere(c, cx, hy + R * 0.15f + wave(t) * R * 0.04f, R, mix(r.accent, 0xFFFF7A45.toInt(), 0.5f), 0xFFFFE9B0.toInt())
        c.restore()
        for (k in 0 until 7) {
            val y = hy + (k + 1) * R * 0.18f; val half = R * (1.1f - k * 0.12f) * (0.8f + 0.2f * w01(t, 1, k * 0.17f))
            val dx = wave(t, 1, k * 0.23f) * R * 0.15f
            c.drawLine(cx - half + dx, y, cx + half + dx, y, r.strokePaint(a(mix(r.accent, Color.WHITE, 0.3f), 0.7f - k * 0.08f), 1.8f * r.u))
        }
    }

    /** A month-progress ring with a spark at its head and six motes running round the track. */
    fun progressRing(r: RenderCtx, z: RectF, t: Float, f: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.42f
        val rect = RectF(cx - R, cy - R, cx + R, cy + R)
        c.drawCircle(cx, cy, R, r.strokePaint(a(r.c1, 0.12f), 6 * r.u))
        c.drawArc(rect, -90f, 360f * f, false, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6 * r.u; strokeCap = Paint.Cap.ROUND
            shader = android.graphics.SweepGradient(cx, cy, intArrayOf(r.cfg.accent2, r.accent, r.cfg.accent2), null)
        })
        val hd = 360f * f - 90f; val hx = cx + R * cos(Math.toRadians(hd.toDouble())).toFloat(); val hy = cy + R * sin(Math.toRadians(hd.toDouble())).toFloat()
        glow(c, hx, hy, (9 + 4 * wave(t)) * r.u, r.accent, 0.9f)
        r.circle(hx, hy, 3 * r.u, Color.WHITE)
    }

    // ========================================================================================== CONTENT (still, in the box)
    private fun dateLine(r: RenderCtx): String {
        val d = r.today; val h = Calendars.hijri(d)
        return Calendars.dayName(d, r.ar) + " · " + r.n(h.day) + " " + Calendars.hijriMonth(h.month, r.ar)
    }

    fun prayerNext(r: RenderCtx, label: String = r.t("Next prayer", "الصلاة الجاية")) {
        val b = box(r); val d = Kit.day(r); val p = d.next
        if (small(r)) {
            r.text(Kit.pName(r, p), b.centerX(), b.top + b.height() * 0.2f, 12.5f * r.u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            r.textTight(Kit.pTime(r, p.time), b.centerX(), b.top + b.height() * 0.55f, b.height() * 0.36f * r.s1, r.c1, r.sansBold, maxW = b.width())
            Kit.countdown(r, b.centerX(), b.top + b.height() * 0.87f, 9 * r.u, r.c2, Paint.Align.CENTER)
            return
        }
        val x = startX(r, b); val al = al(r)
        if (large(r)) {
            r.text(label, x, b.top + b.height() * 0.08f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width() * 0.5f)
            r.text(Kit.pName(r, p) + "  " + Kit.pTime(r, p.time), x, b.top + b.height() * 0.3f, 17 * r.u * r.s1, r.c1, r.sansBold, al, maxW = b.width() * 0.62f)
            Kit.countdown(r, if (r.ar) b.left else b.right, b.top + b.height() * 0.3f, 11 * r.u, r.c2, if (r.ar) Paint.Align.LEFT else Paint.Align.RIGHT)
            // the five prayers in a row
            val five = Kit.ordered(r, d.five); val cw = b.width() / five.size; val y = b.top + b.height() * 0.72f
            five.forEachIndexed { i, q ->
                val cx = b.left + cw * (i + 0.5f); val on = q.key == p.key
                if (on) r.canvas.drawRoundRect(RectF(cx - cw / 2 + 2 * r.u, y - b.height() * 0.26f, cx + cw / 2 - 2 * r.u, y + b.height() * 0.26f), 10 * r.u, 10 * r.u, r.fillPaint(a(r.accent, 0.22f)))
                r.text(Kit.pName(r, q), cx, y - b.height() * 0.1f, 9 * r.u * r.s2, if (on) r.accent else r.c2, r.sansBold, Paint.Align.CENTER, maxW = cw - 4 * r.u)
                r.text(Kit.pTime(r, q.time, false), cx, y + b.height() * 0.12f, 10.5f * r.u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = cw - 4 * r.u)
            }
            return
        }
        r.text(label, x, b.top + b.height() * 0.12f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
        r.textTight(Kit.pName(r, p), if (r.ar) b.right - b.width() / 2 else b.left + b.width() / 2, b.top + b.height() * 0.42f, b.height() * 0.3f * r.s1, r.c1,
            if (r.ar) r.calliFont else r.sansBold, maxW = b.width(), maxH = b.height() * 0.34f)
        r.text(Kit.pTime(r, p.time), x, b.top + b.height() * 0.7f, 14 * r.u * r.s2, r.c1, r.sansBold, al, maxW = b.width())
        Kit.countdown(r, x, b.top + b.height() * 0.9f, 10 * r.u, r.c2, al)
    }

    fun weatherNow(r: RenderCtx) {
        val b = box(r); val w = Kit.weather(r)
        val city = w.city.ifBlank { Prefs.cityName }.let { if (r.ar) Kit.arCity(it) else it }
        if (small(r)) {
            r.textTight(Kit.temp(r, w.temp), b.centerX(), b.top + b.height() * 0.4f, b.height() * 0.46f * r.s1, r.c1, r.sansBold, maxW = b.width())
            r.text(city, b.centerX(), b.top + b.height() * 0.84f, 9 * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            return
        }
        val x = startX(r, b); val al = al(r)
        if (large(r)) {
            r.text(city, x, b.top + b.height() * 0.1f, 10 * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width() * 0.5f)
            r.text(Kit.temp(r, w.temp) + "  " + WeatherRepo.condName(w.cond, r.ar), x, b.top + b.height() * 0.32f, 17 * r.u * r.s1, r.c1, r.sansBold, al, maxW = b.width() * 0.7f)
            r.text(Kit.temp(r, w.lo) + " / " + Kit.temp(r, w.hi), if (r.ar) b.left else b.right, b.top + b.height() * 0.32f, 11 * r.u, r.c2, r.sansBold, if (r.ar) Paint.Align.LEFT else Paint.Align.RIGHT)
            val days = Kit.ordered(r, w.days.take(5)); if (days.isEmpty()) return
            val cw = b.width() / days.size; val y = b.top + b.height() * 0.74f
            days.forEachIndexed { i, df ->
                val cx = b.left + cw * (i + 0.5f); val ld = LocalDate.ofEpochDay(df.epochDay)
                r.text(Calendars.dayName(ld, r.ar).take(if (r.ar) 6 else 3), cx, y - b.height() * 0.16f, 8.5f * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = cw - 2 * r.u)
                Glyphs.weather(r.canvas, df.cond, false, cx, y + b.height() * 0.02f, 13 * r.u)
                r.text(Kit.temp(r, df.hi), cx, y + b.height() * 0.2f, 9.5f * r.u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = cw - 2 * r.u)
            }
            return
        }
        r.text(city, x, b.top + b.height() * 0.12f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
        r.textTight(Kit.temp(r, w.temp), if (r.ar) b.right - b.width() * 0.3f else b.left + b.width() * 0.3f, b.top + b.height() * 0.46f, b.height() * 0.42f * r.s1, r.c1, r.sansBold, maxW = b.width() * 0.6f)
        r.text(WeatherRepo.condName(w.cond, r.ar), x, b.top + b.height() * 0.76f, 11 * r.u * r.s2, r.c1, r.sansBold, al, maxW = b.width())
        r.text(Kit.temp(r, w.lo) + " / " + Kit.temp(r, w.hi) + if (w.wind > 0) "   ·  " + r.n(w.wind.toInt()) + r.t(" km/h", " كم/س") else "", x, b.top + b.height() * 0.93f, 9 * r.u * r.s2, r.c2, r.sans, al, maxW = b.width())
    }

    fun clock(r: RenderCtx) {
        val b = box(r)
        if (small(r)) {
            Kit.clock(r, b.centerX(), b.top + b.height() * 0.42f, b.height() * 0.5f * r.s1, maxW = b.width())
            r.text(Calendars.dayName(r.today, r.ar) + " " + r.n(r.today.dayOfMonth), b.centerX(), b.top + b.height() * 0.88f, 9 * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            return
        }
        if (large(r)) {
            Kit.clock(r, b.centerX(), b.top + b.height() * 0.4f, b.height() * 0.5f * r.s1, maxW = b.width() * 0.9f)
            r.text(Calendars.dayName(r.today, r.ar) + " " + r.n(r.today.dayOfMonth) + " " + Calendars.monthName(r.today.monthValue, r.ar) + "  ·  " + dateLine(r).substringAfter(" · "),
                b.centerX(), b.top + b.height() * 0.86f, 10.5f * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            return
        }
        val x = startX(r, b); val al = al(r)
        Kit.clock(r, if (r.ar) b.right - b.width() / 2 else b.left + b.width() / 2, b.top + b.height() * 0.44f, b.height() * 0.46f * r.s1, maxW = b.width())
        r.text(Calendars.dayName(r.today, r.ar) + " " + r.n(r.today.dayOfMonth) + " " + Calendars.monthName(r.today.monthValue, r.ar), x, b.top + b.height() * 0.8f, 10.5f * r.u * r.s2, r.c1, r.sansBold, al, maxW = b.width())
        r.text(dateLine(r).substringAfter(" · "), x, b.top + b.height() * 0.95f, 9 * r.u * r.s2, r.c2, r.sans, al, maxW = b.width())
    }

    fun dateCard(r: RenderCtx, hijri: Boolean) {
        val b = box(r); val d = r.today; val h = Calendars.hijri(d)
        val big = if (hijri) r.n(h.day) else r.n(d.dayOfMonth)
        val month = if (hijri) Calendars.hijriMonth(h.month, r.ar) else Calendars.monthName(d.monthValue, r.ar)
        val year = if (hijri) r.n(h.year) + r.t(" AH", " هـ") else r.n(d.year)
        val other = if (hijri) r.n(d.dayOfMonth) + " " + Calendars.monthName(d.monthValue, r.ar) else r.n(h.day) + " " + Calendars.hijriMonth(h.month, r.ar)
        if (small(r)) {
            r.text(Calendars.dayName(d, r.ar), b.centerX(), b.top + b.height() * 0.14f, 9.5f * r.u * r.s2, r.accent, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            r.textTight(big, b.centerX(), b.top + b.height() * 0.5f, b.height() * 0.42f * r.s1, r.c1, r.sansBold, maxW = b.width())
            r.text(month, b.centerX(), b.top + b.height() * 0.88f, 10 * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            return
        }
        val x = startX(r, b); val al = al(r)
        r.text(Calendars.dayName(d, r.ar) + "  ·  " + year, x, b.top + b.height() * 0.12f, 10 * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
        val numW = r.measure(big, b.height() * 0.48f, r.sansBold).coerceAtMost(b.width() * 0.4f)
        val numX = if (r.ar) b.right - numW / 2 else b.left + numW / 2
        r.textTight(big, numX, b.top + b.height() * 0.5f, b.height() * 0.48f * r.s1, r.c1, r.sansBold, maxW = b.width() * 0.4f)
        val mx = if (r.ar) b.right - numW - 10 * r.u else b.left + numW + 10 * r.u
        r.textTight(month, if (r.ar) (b.left + mx) / 2 else (mx + b.right) / 2, b.top + b.height() * 0.5f, b.height() * 0.28f * r.s1, r.c1, if (r.ar) r.calliFont else F.PLAYFAIR, maxW = abs(mx - (if (r.ar) b.left else b.right)) - 4 * r.u, maxH = b.height() * 0.32f)
        r.text(other, x, b.top + b.height() * 0.9f, 10 * r.u * r.s2, r.c2, r.sansBold, al, maxW = b.width())
    }

    /** Ramadan & Eid: during Ramadan the iftar / suhoor countdown, on Eid "Eid Mubarak", otherwise days to the next one. */
    fun season(r: RenderCtx) {
        val b = box(r); val now = r.now.toInstant().toEpochMilli(); val h = Calendars.hijri(r.today)
        val x = startX(r, b); val al = al(r); val sm = small(r)
        val cx = if (sm) b.centerX() else x; val align = if (sm) Paint.Align.CENTER else al
        val midX = if (sm) b.centerX() else if (r.ar) b.right - b.width() / 2 else b.left + b.width() / 2
        val eid = when { h.month == 10 && h.day in 1..3 -> r.t("Eid al-Fitr", "عيد الفطر"); h.month == 12 && h.day in 10..13 -> r.t("Eid al-Adha", "عيد الأضحى"); else -> null }
        if (eid != null) {
            r.text(eid, cx, b.top + b.height() * 0.15f, 10 * r.u * r.s2, r.accent, r.sansBold, align, maxW = b.width())
            r.textTight(r.t("Eid Mubarak", "عيد مبارك"), midX, b.top + b.height() * 0.52f, b.height() * 0.36f * r.s1, r.c1, r.calliFont, maxW = b.width(), maxH = b.height() * 0.42f)
            r.text(r.t("Taqabbal Allahu minna wa minkum", "تقبّل الله منا ومنكم"), cx, b.top + b.height() * 0.88f, 9 * r.u * r.s2, r.c2, r.sansBold, align, maxW = b.width())
            return
        }
        val pd = PrayerRepo.today(now); val fajr = pd.list[0].time; val maghrib = pd.list[4].time
        val ramadan = h.month == 9 || (now >= maghrib && Calendars.hijri(r.today.plusDays(1)).month == 9)
        if (ramadan) {
            val iftar = now in fajr until maghrib
            val target = when { now < fajr -> fajr; now < maghrib -> maghrib; else -> PrayerRepo.today(now + 86_400_000L).list[0].time }
            val day = if (now >= maghrib) Calendars.hijri(r.today.plusDays(1)).day else h.day
            r.text(r.t("Ramadan · day ", "رمضان · اليوم ") + r.n(day), cx, b.top + b.height() * 0.13f, 10 * r.u * r.s2, r.accent, r.sansBold, align, maxW = b.width())
            r.text(if (iftar) r.t("Iftar in", "الفطار بعد") else r.t("Suhoor ends in", "السحور ينتهي بعد"), cx, b.top + b.height() * 0.34f, 9.5f * r.u * r.s2, r.c2, r.sans, align, maxW = b.width())
            r.liveCountdown(target, "", cx, b.top + b.height() * 0.6f, (if (sm) 18f else 24f) * r.u * r.s1, r.c1, align, bold = true)
            r.text(r.t("Suhoor ", "السحور ") + Kit.pTime(r, fajr) + "   " + r.t("Iftar ", "الفطار ") + Kit.pTime(r, maghrib), cx, b.top + b.height() * 0.9f, 9 * r.u * r.s2, r.c2, r.sansBold, align, maxW = b.width())
            return
        }
        val ev = SeasonR.events(r.today); val next = ev.firstOrNull() ?: return
        if (sm) {
            r.text(r.t(next.en, next.ar), b.centerX(), b.top + b.height() * 0.15f, 10.5f * r.u * r.s2, r.accent, r.sansBold, Paint.Align.CENTER, maxW = b.width())
            r.textTight(r.n(next.days), b.centerX(), b.top + b.height() * 0.52f, b.height() * 0.42f * r.s1, r.c1, r.sansBold, maxW = b.width())
            r.text(r.t("days left", "يوم باقي"), b.centerX(), b.top + b.height() * 0.88f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = b.width())
            return
        }
        r.text(r.t("Coming up", "المناسبة الجاية"), x, b.top + b.height() * 0.1f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
        r.textTight(r.t(next.en, next.ar), midX, b.top + b.height() * 0.36f, b.height() * 0.24f * r.s1, r.c1, r.calliFont, maxW = b.width(), maxH = b.height() * 0.28f)
        r.text(r.n(next.days) + " " + r.t(if (next.days == 1) "day left" else "days left", "يوم باقي"), x, b.top + b.height() * 0.64f, 15 * r.u * r.s1, r.c1, r.sansBold, al, maxW = b.width())
        val others = ev.drop(1).joinToString("   ·   ") { r.t(it.en, it.ar) + " " + r.n(it.days) }
        r.text(others, x, b.top + b.height() * 0.9f, 9 * r.u * r.s2, r.c2, r.sans, al, maxW = b.width())
    }

    fun qiblaInfo(r: RenderCtx) {
        val b = box(r); val (deg, km) = SignatureR.qibla(Prefs.lat, Prefs.lng)
        val x = if (small(r)) b.centerX() else startX(r, b); val al = if (small(r)) Paint.Align.CENTER else al(r)
        r.text(r.t("Qibla", "القبلة"), x, b.top + b.height() * 0.16f, 10.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
        r.text(r.n(deg.toInt()) + "°", x, b.top + b.height() * 0.52f, (if (small(r)) 20f else 26f) * r.u * r.s1, r.c1, r.sansBold, al, maxW = b.width())
        r.text(r.n("%,d".format(km.toInt())) + r.t(" km to Makkah", " كم لمكة"), x, b.top + b.height() * 0.86f, 9 * r.u * r.s2, r.c2, r.sans, al, maxW = b.width())
    }

    private val dhikr = listOf(
        "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ" to "SubhanAllahi wa bihamdih",
        "لَا إِلَٰهَ إِلَّا اللَّهُ" to "La ilaha illa Allah",
        "أَسْتَغْفِرُ اللَّهَ" to "Astaghfirullah",
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ" to "Allahumma salli ala Muhammad",
        "سُبْحَانَ اللَّهِ الْعَظِيمِ" to "SubhanAllahil Azeem",
        "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ" to "La hawla wa la quwwata illa billah",
        "الْحَمْدُ لِلَّهِ" to "Alhamdulillah",
    )

    fun dhikrCard(r: RenderCtx) {
        val b = box(r); val (arT, en) = dhikr[Math.floorMod(r.today.toEpochDay().toInt(), dhikr.size)]
        val midX = if (small(r)) b.centerX() else if (r.ar) b.right - b.width() / 2 else b.left + b.width() / 2
        r.text(r.t("Today's dhikr", "ذكر اليوم"), if (small(r)) b.centerX() else startX(r, b), b.top + b.height() * 0.12f, 9.5f * r.u * r.s2, r.accent, r.sansBold, if (small(r)) Paint.Align.CENTER else al(r), maxW = b.width())
        r.textTight(arT, midX, b.top + b.height() * 0.5f, b.height() * 0.3f * r.s1, r.c1, F.AMIRI, maxW = b.width(), maxH = b.height() * 0.4f)
        r.text(if (r.ar) r.t("", "اضغط وسبّح") else en, midX, b.top + b.height() * 0.88f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = b.width())
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("tasbeeh", "inc"))
        if (r.cfg.counter > 0) r.text(r.n(r.cfg.counter), if (r.ar) b.left else b.right, b.top + b.height() * 0.12f, 10 * r.u, r.accent, r.sansBold, if (r.ar) Paint.Align.LEFT else Paint.Align.RIGHT)
    }

    fun friday(r: RenderCtx) {
        val b = box(r); val d = r.today
        val left = Math.floorMod(java.time.DayOfWeek.FRIDAY.value - d.dayOfWeek.value, 7)
        val x = startX(r, b); val al = al(r); val midX = if (r.ar) b.right - b.width() / 2 else b.left + b.width() / 2
        if (left == 0) {
            r.text(r.t("Today", "النهارده"), x, b.top + b.height() * 0.12f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
            r.textTight(r.t("Jumu'ah Mubarak", "جمعة مباركة"), midX, b.top + b.height() * 0.46f, b.height() * 0.32f * r.s1, r.c1, r.calliFont, maxW = b.width(), maxH = b.height() * 0.36f)
            r.text(r.t("Surat Al-Kahf · send salawat", "سورة الكهف · وأكثروا من الصلاة على النبي ﷺ"), x, b.top + b.height() * 0.84f, 9 * r.u * r.s2, r.c2, r.sansBold, al, maxW = b.width())
        } else {
            r.text(r.t("Friday", "الجمعة"), x, b.top + b.height() * 0.12f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = b.width())
            r.text(r.n(left) + " " + r.t(if (left == 1) "day left" else "days left", if (left == 1) "يوم" else "أيام"), x, b.top + b.height() * 0.46f, 22 * r.u * r.s1, r.c1, r.sansBold, al, maxW = b.width())
            r.text(dateLine(r), x, b.top + b.height() * 0.84f, 9 * r.u * r.s2, r.c2, r.sans, al, maxW = b.width())
        }
    }

    fun newYear(r: RenderCtx) {
        val b = box(r); val d = r.today; val ny = LocalDate.of(d.year + 1, 1, 1)
        val days = java.time.temporal.ChronoUnit.DAYS.between(d, ny).toInt()
        r.text(r.t("New Year ", "السنة الجديدة ") + r.n(d.year + 1), b.centerX(), b.top + b.height() * 0.14f, 10 * r.u * r.s2, r.accent, r.sansBold, Paint.Align.CENTER, maxW = b.width())
        r.textTight(r.n(days), b.centerX(), b.top + b.height() * 0.52f, b.height() * 0.42f * r.s1, r.c1, r.sansBold, maxW = b.width())
        r.text(r.t("days to go", "يوم باقي"), b.centerX(), b.top + b.height() * 0.88f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = b.width())
    }

    private val wisdom = listOf(
        "إن مع العسر يسرا" to "With hardship comes ease",
        "وقل رب زدني علما" to "My Lord, increase me in knowledge",
        "فاذكروني أذكركم" to "Remember Me; I will remember you",
        "ولسوف يعطيك ربك فترضى" to "Your Lord will give you, and you will be pleased",
        "إن الله مع الصابرين" to "Allah is with the patient",
        "ومن يتوكل على الله فهو حسبه" to "Whoever relies on Allah, He is enough",
        "لا تحزن إن الله معنا" to "Do not grieve; Allah is with us",
    )

    fun verse(r: RenderCtx) {
        val b = box(r); val (arT, en) = wisdom[Math.floorMod(r.today.toEpochDay().toInt(), wisdom.size)]
        val midX = if (r.ar) b.right - b.width() / 2 else b.left + b.width() / 2
        r.text(r.t("Verse of the day", "آية اليوم"), startX(r, b), b.top + b.height() * 0.1f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al(r), maxW = b.width())
        r.textTight(if (r.ar) arT else en, midX, b.top + b.height() * 0.52f, b.height() * 0.24f * r.s1, r.c1, if (r.ar) F.AMIRI else F.PLAYFAIR, maxW = b.width(), maxH = b.height() * 0.5f)
        r.text(dateLine(r), startX(r, b), b.top + b.height() * 0.92f, 9 * r.u * r.s2, r.c2, r.sans, al(r), maxW = b.width())
    }

    fun monthProgressInfo(r: RenderCtx): Float {
        val d = r.today; return (d.dayOfMonth - 1 + r.now.hour / 24f) / d.lengthOfMonth()
    }

    fun monthProgress(r: RenderCtx) {
        val b = box(r); val f = monthProgressInfo(r)
        r.text(r.n((f * 100).toInt()) + "%", b.centerX(), b.top + b.height() * 0.32f, 18 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER, maxW = b.width())
        r.text(r.t("of ", "من ") + Calendars.monthName(r.today.monthValue, r.ar), b.centerX(), b.top + b.height() * 0.72f, 9.5f * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = b.width())
    }

    // ------------------------------------------------------------------------------------------ helpers for designs
    fun isNight(r: RenderCtx): Boolean {
        val d = Kit.day(r); val now = r.now.toInstant().toEpochMilli()
        return now < d.list[1].time || now > d.list[4].time
    }
    fun qiblaBearing(): Float = SignatureR.qibla(Prefs.lat, Prefs.lng).first.toFloat()

    // ========================================================================================== ISLAMIC ART
    /** An n-point star polygon (alternating outer / inner radius). */
    fun starPath(cx: Float, cy: Float, R: Float, inner: Float, n: Int, rot: Float = 0f): Path = Path().apply {
        for (i in 0 until n * 2) {
            val rr = if (i % 2 == 0) R else inner
            val th = rot + i * PI.toFloat() / n - PI.toFloat() / 2
            val x = cx + rr * cos(th); val y = cy + rr * sin(th)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Khatam lattice: 8-point stars with the small crosses between them, as in old mosque tiles. */
    fun pattern(r: RenderCtx, area: RectF, color: Int, alpha: Float, cell: Float = 34 * r.u) {
        val c = r.canvas; val p = r.strokePaint(a(color, alpha), 0.9f * r.u)
        c.save(); c.clipRect(area)
        var y = area.top - cell; var row = 0
        while (y < area.bottom + cell) {
            var x = area.left - cell + (if (row % 2 == 0) 0f else 0f)
            while (x < area.right + cell) {
                c.drawPath(starPath(x, y, cell * 0.42f, cell * 0.3f, 8, PI.toFloat() / 8), p)
                c.drawPath(starPath(x, y, cell * 0.2f, cell * 0.14f, 8, PI.toFloat() / 8), p)
                c.drawPath(starPath(x + cell / 2, y + cell / 2, cell * 0.16f, cell * 0.07f, 4), p)
                x += cell
            }
            y += cell; row++
        }
        c.restore()
    }

    /** A pointed (ogee) arch — the mihrab — with a double gold frame and a warm light at its top. */
    fun mihrabPath(rect: RectF): Path = Path().apply {
        val cx = rect.centerX(); val spring = rect.top + rect.height() * 0.38f; val w = rect.width()
        moveTo(rect.left, rect.bottom); lineTo(rect.left, spring)
        cubicTo(rect.left, rect.top + rect.height() * 0.12f, cx - w * 0.12f, rect.top + rect.height() * 0.08f, cx, rect.top)
        cubicTo(cx + w * 0.12f, rect.top + rect.height() * 0.08f, rect.right, rect.top + rect.height() * 0.12f, rect.right, spring)
        lineTo(rect.right, rect.bottom); close()
    }

    fun mihrab(r: RenderCtx, rect: RectF) {
        val c = r.canvas; val gold = r.cfg.accent2
        val path = mihrabPath(rect)
        c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, rect.top, 0f, rect.bottom, a(mix(r.accent, Color.BLACK, 0.55f), 0.55f), a(Color.BLACK, 0.35f), Shader.TileMode.CLAMP)
        })
        c.save(); c.clipPath(path); pattern(r, rect, gold, 0.12f, rect.width() * 0.34f); c.restore()
        c.drawPath(path, r.strokePaint(a(gold, 0.9f), 1.6f * r.u))
        val inset = rect.width() * 0.07f
        c.drawPath(mihrabPath(RectF(rect.left + inset, rect.top + inset * 1.6f, rect.right - inset, rect.bottom)), r.strokePaint(a(gold, 0.45f), 0.9f * r.u))
        // a small star at the apex
        c.drawPath(starPath(rect.centerX(), rect.top - rect.width() * 0.06f, rect.width() * 0.06f, rect.width() * 0.03f, 8), r.fillPaint(gold))
    }

    /** A rosette: 16-point outer star, 8-point inner one turning slowly (45° per loop), gold on dark. */
    fun rosette(r: RenderCtx, z: RectF, t: Float) {
        val c = r.canvas; val cx = z.centerX(); val cy = z.centerY(); val R = min(z.width(), z.height()) * 0.44f
        val gold = r.cfg.accent2
        glow(c, cx, cy, R * 1.25f, r.accent, 0.22f + 0.06f * wave(t))
        c.drawPath(starPath(cx, cy, R, R * 0.82f, 16), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, cy, R, a(mix(gold, Color.BLACK, 0.6f), 0.5f), a(mix(gold, Color.BLACK, 0.3f), 0.25f), Shader.TileMode.CLAMP)
        })
        c.drawPath(starPath(cx, cy, R, R * 0.82f, 16), r.strokePaint(a(gold, 0.9f), 1.4f * r.u))
        c.drawCircle(cx, cy, R * 0.74f, r.strokePaint(a(gold, 0.5f), 0.9f * r.u))
        val rot = 0f
        c.drawPath(starPath(cx, cy, R * 0.68f, R * 0.4f, 8, rot), Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx - R, cy - R, cx + R, cy + R, mix(gold, Color.WHITE, 0.35f), mix(gold, Color.BLACK, 0.35f), Shader.TileMode.CLAMP)
        })
        c.drawPath(starPath(cx, cy, R * 0.4f, R * 0.28f, 8, -rot + PI.toFloat() / 8), r.fillPaint(mix(r.cfg.bgColor, Color.BLACK, 0.3f)))
        c.drawPath(starPath(cx, cy, R * 0.4f, R * 0.28f, 8, -rot + PI.toFloat() / 8), r.strokePaint(gold, 1.1f * r.u))
        sphere(c, cx, cy, R * 0.12f, r.accent)
        // a glint running round the outer star
        val gth = t * TAU; glow(c, cx + R * 0.9f * cos(gth), cy + R * 0.9f * sin(gth), R * 0.18f, Color.WHITE, 0.35f)
    }

    /** Mosque skyline along the bottom: domes and minarets, crescents on top (still). */
    fun skyline(r: RenderCtx, baseY: Float, color: Int) {
        val c = r.canvas; val u = r.u; val w = r.w
        val p = Path()
        fun dome(cx: Float, rad: Float, body: Float) {
            p.addRect(cx - rad * 1.1f, baseY - body, cx + rad * 1.1f, baseY + 2 * u, Path.Direction.CW)
            p.addOval(RectF(cx - rad, baseY - body - rad * 1.5f, cx + rad, baseY - body + rad * 0.5f), Path.Direction.CW)
            p.moveTo(cx - rad * 0.12f, baseY - body - rad * 1.35f); p.lineTo(cx, baseY - body - rad * 1.9f); p.lineTo(cx + rad * 0.12f, baseY - body - rad * 1.35f); p.close()
        }
        fun minaret(cx: Float, h: Float) {
            p.addRect(cx - 3.2f * u, baseY - h, cx + 3.2f * u, baseY + 2 * u, Path.Direction.CW)
            p.addRect(cx - 5 * u, baseY - h * 0.72f, cx + 5 * u, baseY - h * 0.68f, Path.Direction.CW)
            p.moveTo(cx - 3.6f * u, baseY - h); p.lineTo(cx, baseY - h - 9 * u); p.lineTo(cx + 3.6f * u, baseY - h); p.close()
        }
        dome(w * 0.5f, 22 * u, 10 * u); dome(w * 0.32f, 12 * u, 6 * u); dome(w * 0.68f, 12 * u, 6 * u)
        dome(w * 0.1f, 8 * u, 4 * u); dome(w * 0.9f, 8 * u, 4 * u)
        minaret(w * 0.2f, 52 * u); minaret(w * 0.8f, 52 * u); minaret(w * 0.42f, 34 * u); minaret(w * 0.58f, 34 * u)
        p.addRect(0f, baseY, w, baseY + 3 * u, Path.Direction.CW)
        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, baseY - 60 * u, 0f, baseY, mix(color, r.accent, 0.08f), color, Shader.TileMode.CLAMP) })
    }

    /** Warm windows along the skyline, glowing softly at different times. */
    fun skylineLights(r: RenderCtx, baseY: Float, t: Float) {
        val g = rnd(91)
        for (i in 0 until 14) {
            val x = r.w * (0.06f + i * 0.068f) + g.nextFloat() * 4 * r.u; val y = baseY - (3 + g.nextFloat() * 7) * r.u
            val b = 0.45f + 0.55f * w01(t, 1, g.nextFloat())
            glow(r.canvas, x, y, 6 * r.u, r.accent, 0.4f * b)
            r.canvas.drawRoundRect(RectF(x - 1.3f * r.u, y - 2.4f * r.u, x + 1.3f * r.u, y + 2.4f * r.u), 1.3f * r.u, 1.3f * r.u, r.fillPaint(a(mix(r.accent, Color.WHITE, 0.3f), 0.5f + 0.45f * b)))
        }
    }

    /** A rope of hanging stars and crescents across the top, each swinging gently. */
    fun garland(r: RenderCtx, t: Float) {
        val c = r.canvas; val gold = r.cfg.accent2; val sag = r.h * 0.1f
        val rope = Path().apply { moveTo(-4 * r.u, 4 * r.u); quadTo(r.w / 2, 4 * r.u + sag * 2, r.w + 4 * r.u, 4 * r.u) }
        c.drawPath(rope, r.strokePaint(a(gold, 0.6f), 1f * r.u))
        val n = 7
        for (i in 0 until n) {
            val f = (i + 0.5f) / n; val x = r.w * f; val y = 4 * r.u + sag * 2 * 2 * f * (1 - f)
            val len = (if (i % 2 == 0) 16f else 24f) * r.u; val ang = 6f * wave(t, 1, i * 0.13f)
            c.save(); c.rotate(ang, x, y)
            c.drawLine(x, y, x, y + len, r.strokePaint(a(gold, 0.6f), 0.8f * r.u))
            val oy = y + len + 5 * r.u
            glow(c, x, oy, 9 * r.u, r.accent, 0.3f + 0.15f * w01(t, 1, i * 0.21f))
            if (i % 2 == 0) c.drawPath(starPath(x, oy, 5.5f * r.u, 2.6f * r.u, 8), r.fillPaint(gold))
            else {
                val l = c.saveLayer(x - 8 * r.u, oy - 8 * r.u, x + 8 * r.u, oy + 8 * r.u, null)
                r.circle(x, oy, 5.5f * r.u, gold)
                c.drawCircle(x + 2.6f * r.u, oy - 1.2f * r.u, 4.8f * r.u, Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) })
                c.restoreToCount(l)
            }
            c.restore()
        }
    }

    /** Light falling through a mashrabiya screen: a soft beam that sways and dust motes drifting in it. */
    fun beam(r: RenderCtx, t: Float, fromX: Float) {
        val c = r.canvas; val sway = 0.06f * r.w * wave(t)
        val p = Path().apply { moveTo(fromX - r.w * 0.06f, 0f); lineTo(fromX + r.w * 0.06f, 0f); lineTo(fromX + r.w * 0.28f + sway, r.h); lineTo(fromX - r.w * 0.12f + sway, r.h); close() }
        c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, r.h, a(r.accent, 0.16f), 0, Shader.TileMode.CLAMP) })
        val g = rnd(97)
        repeat(count(r, 10)) {
            val y = g.nextFloat() * r.h; val k = y / r.h; val x = fromX + (g.nextFloat() - 0.3f) * r.w * 0.2f * (0.3f + k) + sway * k + 5 * r.u * sin((t + g.nextFloat()) * TAU)
            r.circle(x, y + 4 * r.u * cos((t + g.nextFloat()) * TAU), (0.6f + g.nextFloat()) * r.u, a(Color.WHITE, 0.25f + 0.35f * w01(t, 1, g.nextFloat())))
        }
    }

    // ========================================================================================== WEATHER SCENES
    /** True while the widget keeps its own scene background (the user hasn't picked another one). */
    const val SCENE = 0xFF0B1730.toInt()
    fun sceneOn(r: RenderCtx) = r.cfg.bg && r.cfg.bgColor == SCENE

    /** The sky for the real weather and time of day, with far hills — painted under the text (still). */
    fun skyScene(r: RenderCtx) {
        if (!sceneOn(r)) return
        val w = Kit.weather(r); val night = w.night || isNight(r)
        val (top, bottom) = when (w.cond) {
            Cond.CLEAR, Cond.PARTLY -> if (night) 0xFF0A1330.toInt() to 0xFF27305E.toInt() else 0xFF2D6CB5.toInt() to 0xFF8DC2E8.toInt()
            Cond.CLOUDY, Cond.FOG -> if (night) 0xFF151B2B.toInt() to 0xFF2E3647.toInt() else 0xFF5D7390.toInt() to 0xFFA9B8C9.toInt()
            Cond.RAIN, Cond.STORM -> if (night) 0xFF0E141F.toInt() to 0xFF26303F.toInt() else 0xFF3F4F63.toInt() to 0xFF7C8C9E.toInt()
            Cond.SNOW -> if (night) 0xFF1A2236.toInt() to 0xFF3C4A66.toInt() else 0xFF8FA6C0.toInt() to 0xFFDCE6F0.toInt()
        }
        val c = r.canvas; val rad = Renderer.cornerPx(r)
        c.save(); c.clipPath(Path().apply { addRoundRect(RectF(0f, 0f, r.w, r.h), rad, rad, Path.Direction.CW) })
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, r.h, top, bottom, Shader.TileMode.CLAMP) })
        // a gentle dark veil on the text side so the words always read well
        val veil = if (r.ar) LinearGradient(r.w, 0f, r.w * 0.35f, 0f, a(Color.BLACK, 0.28f), 0, Shader.TileMode.CLAMP) else LinearGradient(0f, 0f, r.w * 0.65f, 0f, a(Color.BLACK, 0.28f), 0, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = veil })
        // two layers of hills
        val h1 = Path().apply { moveTo(0f, r.h); lineTo(0f, r.h * 0.8f); cubicTo(r.w * 0.25f, r.h * 0.7f, r.w * 0.45f, r.h * 0.86f, r.w * 0.7f, r.h * 0.76f); cubicTo(r.w * 0.85f, r.h * 0.7f, r.w * 0.95f, r.h * 0.78f, r.w, r.h * 0.74f); lineTo(r.w, r.h); close() }
        c.drawPath(h1, r.fillPaint(a(mix(bottom, Color.BLACK, 0.35f), 0.55f)))
        val h2 = Path().apply { moveTo(0f, r.h); lineTo(0f, r.h * 0.9f); cubicTo(r.w * 0.3f, r.h * 0.82f, r.w * 0.6f, r.h * 0.96f, r.w, r.h * 0.86f); lineTo(r.w, r.h); close() }
        c.drawPath(h2, r.fillPaint(a(mix(bottom, Color.BLACK, 0.55f), 0.7f)))
        c.restore()
    }

    /** Soft, layered clouds across the whole sky, drifting a little back and forth. */
    fun cloudsWide(r: RenderCtx, t: Float, dark: Boolean, n: Int = 4) {
        val c = r.canvas; val g = rnd(101)
        repeat(n) { i ->
            // on a wide widget the clouds stay over the sky side, never over the temperature and city
            val zz = zone(r); val wideW = r.style.shape == Shape.WIDE || r.style.shape == Shape.BANNER
            val x0 = if (wideW) zz.left - zz.width() * 0.15f + g.nextFloat() * zz.width() * 1.3f else r.w * g.nextFloat()
            val w0 = (if (wideW) zz.width() * (0.45f + g.nextFloat() * 0.3f) else r.w * (0.28f + g.nextFloat() * 0.22f)); val x = x0 + (if (i % 2 == 0) 1 else -1) * r.w * 0.025f * wave(t, 1, i * 0.2f)
            val y = r.h * (if (wideW) 0.12f + g.nextFloat() * 0.4f else 0.08f + g.nextFloat() * 0.22f)
            val col = if (dark) mix(0xFF59657A.toInt(), Color.WHITE, 0.15f * i) else Color.WHITE
            val al = (if (dark) 0.55f else 0.75f) * (0.6f + 0.4f * (i.toFloat() / n))
            val p = Path().apply {
                addCircle(x - w0 * 0.25f, y + w0 * 0.05f, w0 * 0.2f, Path.Direction.CW)
                addCircle(x + w0 * 0.02f, y - w0 * 0.08f, w0 * 0.27f, Path.Direction.CW)
                addCircle(x + w0 * 0.28f, y + w0 * 0.06f, w0 * 0.19f, Path.Direction.CW)
                addRoundRect(RectF(x - w0 * 0.46f, y + w0 * 0.02f, x + w0 * 0.48f, y + w0 * 0.24f), w0 * 0.12f, w0 * 0.12f, Path.Direction.CW)
            }
            c.drawPath(p, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, y - w0 * 0.35f, 0f, y + w0 * 0.24f, a(col, al), a(mix(col, 0xFF7D8799.toInt(), 0.45f), al), Shader.TileMode.CLAMP) })
        }
    }

    /** Everything that moves in a weather scene, chosen by the real weather. */
    fun weatherScene(r: RenderCtx, t: Float) {
        val w = Kit.weather(r); val night = w.night || isNight(r); val z = zone(r)
        val sx = z.centerX(); val sy = z.top + z.height() * 0.38f
        when (w.cond) {
            Cond.CLEAR -> if (night) { stars(r, t); crescent(r, z, t, sx, sy, 0.8f) } else sun(r, z, t, sx, sy, 0.95f)
            Cond.PARTLY -> { if (night) { stars(r, t); crescent(r, z, t, sx, sy, 0.7f) } else sun(r, z, t, sx, sy, 0.8f); cloudsWide(r, t, false, 3) }
            Cond.CLOUDY -> cloudsWide(r, t, night, 5)
            Cond.FOG -> {
                cloudsWide(r, t, true, 3)
                for (k in 0 until 4) {
                    val y = r.h * (0.45f + k * 0.12f); val dx = wave(t, 1, k * 0.27f) * r.w * 0.04f
                    r.canvas.drawRoundRect(RectF(-r.w * 0.1f + dx, y, r.w * 1.1f + dx, y + r.h * 0.05f), r.h * 0.03f, r.h * 0.03f, r.fillPaint(a(Color.WHITE, 0.16f)))
                }
            }
            Cond.RAIN, Cond.STORM -> {
                cloudsWide(r, t, true, 4); rainStreaks(r, t)
                if (w.cond == Cond.STORM) {
                    val f = ((t * 20f).toInt() == 3).let { if (it) 1f else 0f }
                    if (f > 0f) r.canvas.drawRect(0f, 0f, r.w, r.h, r.fillPaint(a(Color.WHITE, 0.12f)))
                }
            }
            Cond.SNOW -> { cloudsWide(r, t, false, 3); snow(r, t) }
        }
    }

    // ========================================================================================== SMOOTH SPINNERS
    /**
     * A shape the launcher itself spins at the screen's refresh rate (60–120 Hz): an indeterminate ProgressBar
     * placed over the widget. Under an animation (BASE) it is handed to the widget as a live slot; for a still
     * picture (lock screen, motion off) the same shape is simply drawn in place.
     */
    fun spin(r: RenderCtx, kind: String, cx: Float, cy: Float, half: Float, color: Int) {
        val rect = RectF(cx - half, cy - half, cx + half, cy + half)
        if ((LiveFx.mode.get() ?: LiveFx.ALL) == LiveFx.BASE) { r.lives += LiveSpin(rect, kind, color); return }
        val id = spinRes(kind) ?: return
        val d = r.context.getDrawable(id)?.mutate() ?: return
        d.setTint(color); d.setBounds(rect.left.toInt(), rect.top.toInt(), rect.right.toInt(), rect.bottom.toInt()); d.draw(r.canvas)
    }
    fun spinRes(kind: String): Int? = when (kind) {
        "rays" -> com.smrh.zujaj.R.drawable.v_spin_rays; "star8" -> com.smrh.zujaj.R.drawable.v_spin_star8
        "gyro" -> com.smrh.zujaj.R.drawable.v_spin_gyro; "dots" -> com.smrh.zujaj.R.drawable.v_spin_dots
        "sparks" -> com.smrh.zujaj.R.drawable.v_spin_sparks; "galaxy" -> com.smrh.zujaj.R.drawable.v_spin_galaxy
        else -> null
    }
    /** Milliseconds per full turn, as in res/drawable/spin_*.xml (for the in-app preview). */
    fun spinPeriod(kind: String) = when (kind) { "rays" -> 30000; "star8" -> 36000; "gyro" -> 14000; "dots" -> 16000; "sparks" -> 40000; else -> 30000 }

    fun sunRays(r: RenderCtx, cx: Float, cy: Float, scale: Float, z: RectF = zone(r)) {
        val R = min(z.width(), z.height()) * 0.2f * scale
        spin(r, "rays", cx, cy, R * 2.05f, a(mix(r.accent, Color.WHITE, 0.3f), 0.85f))
    }
    fun crescentSparks(r: RenderCtx, cx: Float, cy: Float, scale: Float, z: RectF = zone(r)) {
        val R = min(z.width(), z.height()) * 0.27f * scale
        spin(r, "sparks", cx, cy, R * 1.75f, a(Color.WHITE, 0.75f))
    }
    fun rosetteRing(r: RenderCtx) { val z = zone(r); val R = min(z.width(), z.height()) * 0.44f; spin(r, "star8", z.centerX(), z.centerY(), R * 0.8f, a(r.cfg.accent2, 0.95f)) }
    fun galaxySpin(r: RenderCtx) { val z = zone(r); val R = min(z.width(), z.height()) * 0.46f; spin(r, "galaxy", z.centerX(), z.centerY(), R, a(mix(r.accent, Color.WHITE, 0.25f), 0.95f)) }
    fun ringDots(r: RenderCtx) { val z = zone(r); val R = min(z.width(), z.height()) * 0.42f; spin(r, "dots", z.centerX(), z.centerY(), R * 1.087f, a(Color.WHITE, 0.7f)) }

    /** The smooth parts of a weather scene (sun rays by day, sparks round the moon at night). */
    fun weatherSpins(r: RenderCtx) {
        val w = Kit.weather(r); val night = w.night || isNight(r); val z = zone(r)
        val sx = z.centerX(); val sy = z.top + z.height() * 0.38f
        when (w.cond) {
            Cond.CLEAR -> if (night) crescentSparks(r, sx, sy, 0.8f) else sunRays(r, sx, sy, 0.95f)
            Cond.PARTLY -> if (night) crescentSparks(r, sx, sy, 0.7f) else sunRays(r, sx, sy, 0.8f)
            else -> {}
        }
    }
}
