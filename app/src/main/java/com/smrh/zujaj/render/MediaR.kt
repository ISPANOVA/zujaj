package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.MediaRepo
import com.smrh.zujaj.data.MediaState
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

object MediaR {
    fun st(r: RenderCtx): MediaState = MediaRepo.current(r.context)

    fun art(r: RenderCtx, m: MediaState, dst: RectF, radius: Float) {
        val b = m.art ?: (if (m.real) MediaRepo.cachedArt(r.context) else null)
        Renderer.photoLayer(r, b, dst, radius)
    }

    fun title(r: RenderCtx, m: MediaState) = if (m.real) m.title else r.t("Midnight Drive", "رحلة منتصف الليل")
    fun artist(r: RenderCtx, m: MediaState) = if (m.real) m.artist.ifBlank { appLabel(r, m.pkg) } else r.t("Tap to connect music", "اضغط لتفعيل الميديا")

    private fun appLabel(r: RenderCtx, pkg: String): String = runCatching {
        val pm = r.context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault("")

    fun time(r: RenderCtx, ms: Long): String {
        val s = ms / 1000
        return r.n(String.format(java.util.Locale.US, "%02d:%02d", s / 60, s % 60))
    }

    fun cmd(c: String) = HitAction.Cmd("media", c)

    /** circular button with glyph */
    fun btn(r: RenderCtx, kind: String, cx: Float, cy: Float, rad: Float, m: MediaState, filled: Boolean = true, glyphColor: Int = r.c1, bg: Int = withAlpha(r.c1, 34)) {
        if (filled) r.circle(cx, cy, rad, bg)
        val g = rad * 1.0f
        when (kind) {
            "play_pause" -> Glyphs.playPause(r.canvas, m.playing, cx + if (m.playing) 0f else rad * 0.06f, cy, g * 0.9f, glyphColor)
            "prev" -> Glyphs.skip(r.canvas, cx, cy, g * 0.8f, glyphColor, false)
            "next" -> Glyphs.skip(r.canvas, cx, cy, g * 0.8f, glyphColor, true)
            "open" -> Glyphs.note(r.canvas, cx, cy, g * 0.9f, glyphColor)
            "shuffle" -> Glyphs.shuffle(r.canvas, cx, cy, g * 0.9f, glyphColor)
            "repeat" -> Glyphs.repeat(r.canvas, cx, cy, g * 0.9f, glyphColor)
            "vol_down" -> Glyphs.volume(r.canvas, cx, cy, g * 0.9f, glyphColor, 1)
            "vol_up" -> Glyphs.volume(r.canvas, cx, cy, g * 0.9f, glyphColor, 2)
        }
        r.hit(RectF(cx - rad * 1.1f, cy - rad * 1.1f, cx + rad * 1.1f, cy + rad * 1.1f), cmd(kind))
    }

    private fun trio(r: RenderCtx, m: MediaState, cx: Float, cy: Float, rad: Float, gap: Float, bigPlay: Float = 1f, filled: Boolean = true) {
        btn(r, "prev", cx - gap, cy, rad, m, filled)
        btn(r, "play_pause", cx, cy, rad * bigPlay, m, filled)
        btn(r, "next", cx + gap, cy, rad, m, filled)
    }

    fun openHit(r: RenderCtx, rect: RectF) = r.hit(rect, cmd("open"))

    private fun labels(r: RenderCtx, m: MediaState, x: Float, y: Float, maxW: Float, size: Float = 13f, color: Int = r.c1, sub: Int = r.c2, align: Paint.Align = Paint.Align.LEFT) {
        r.text(title(r, m), x, y - size * 0.55f * r.u, size * r.u * r.s1, color, r.sansBold, align, maxW = maxW)
        r.text(artist(r, m), x, y + size * 0.6f * r.u, (size - 3) * r.u * r.s2, sub, r.sans, align, maxW = maxW)
    }

    private fun newBadge(r: RenderCtx) {}

    // -------------------------------------------------------------- banners
    fun bar(r: RenderCtx) {
        val m = st(r)
        val p = r.pad * 0.7f
        val s = r.h - 2 * p
        val a = RectF(p, p, p + s, p + s)
        art(r, m, a, 14 * r.u)
        openHit(r, a)
        labels(r, m, a.right + 12 * r.u, r.h / 2, r.w * 0.36f)
        trio(r, m, r.w - p - 62 * r.u, r.h / 2, 14 * r.u, 38 * r.u)
    }

    fun strip(r: RenderCtx) {
        val m = st(r)
        labels(r, m, r.pad + 8 * r.u, r.h / 2, r.w * 0.4f)
        openHit(r, RectF(0f, 0f, r.w * 0.45f, r.h))
        trio(r, m, r.w - r.pad - 64 * r.u, r.h / 2, 17 * r.u, 42 * r.u)
    }

    fun capsule(r: RenderCtx) {
        val m = st(r)
        btn(r, "play_pause", r.pad + 26 * r.u, r.h / 2, 22 * r.u, m)
        labels(r, m, r.pad + 60 * r.u, r.h / 2, r.w * 0.5f)
        Glyphs.note(r.canvas, r.w - r.pad - 20 * r.u, r.h / 2, 28 * r.u, r.c1)
        openHit(r, RectF(r.w - r.pad - 44 * r.u, 0f, r.w, r.h))
    }

    // -------------------------------------------------------------- small
    fun cover(r: RenderCtx) {
        val m = st(r)
        art(r, m, RectF(0f, 0f, r.w, r.h), 0f)
        val g = Paint(); g.shader = LinearGradient(0f, r.h * 0.45f, 0f, r.h, Color.TRANSPARENT, withAlpha(Color.BLACK, 190), Shader.TileMode.CLAMP)
        r.canvas.drawRect(0f, 0f, r.w, r.h, g)
        openHit(r, RectF(0f, 0f, r.w, r.h * 0.6f))
        labels(r, m, r.pad + 2 * r.u, r.h - r.pad - 12 * r.u, r.w * 0.6f, 12f, Color.WHITE, withAlpha(Color.WHITE, 190))
        btn(r, "play_pause", r.w - r.pad - 14 * r.u, r.h - r.pad - 12 * r.u, 15 * r.u, m, bg = withAlpha(Color.WHITE, 60), glyphColor = Color.WHITE)
    }

    fun disc(r: RenderCtx) {
        val m = st(r)
        val rad = min(r.w, r.h) * 0.3f
        val cx = r.w / 2; val cy = r.h * 0.42f
        val save = r.canvas.save()
        r.canvas.clipPath(Path().apply { addCircle(cx, cy, rad, Path.Direction.CW) })
        art(r, m, RectF(cx - rad, cy - rad, cx + rad, cy + rad), 0f)
        r.canvas.restoreToCount(save)
        btn(r, "play_pause", cx, cy, rad * 0.32f, m, bg = withAlpha(Color.WHITE, 70), glyphColor = Color.WHITE)
        r.text(title(r, m), cx, r.h * 0.82f, 11 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.text(artist(r, m), cx, r.h * 0.92f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun controls(r: RenderCtx) {
        val m = st(r)
        labels(r, m, r.w / 2, r.h * 0.26f, r.w - 2 * r.pad, 12f, align = Paint.Align.CENTER)
        openHit(r, RectF(0f, 0f, r.w, r.h * 0.4f))
        trio(r, m, r.w / 2, r.h * 0.64f, 13 * r.u, 42 * r.u, 1.5f)
    }

    fun pad(r: RenderCtx) {
        val m = st(r)
        val rad = min(r.w, r.h) * 0.17f
        val ox = r.w / 2; val oy = r.h / 2
        val d = rad * 1.25f
        btn(r, "open", ox - d, oy - d, rad, m)
        btn(r, "play_pause", ox + d, oy - d, rad, m)
        btn(r, "prev", ox - d, oy + d, rad, m)
        btn(r, "next", ox + d, oy + d, rad, m)
    }

    fun ring(r: RenderCtx) {
        val m = st(r)
        art(r, m, RectF(0f, 0f, r.w, r.h), 0f)
        if (r.cfg.bg) r.canvas.drawRect(0f, 0f, r.w, r.h, r.fillPaint(withAlpha(Color.BLACK, 70)))
        val cx = r.w / 2; val cy = r.h * 0.42f
        val rad = min(r.w, r.h) * 0.2f
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(Color.WHITE, 60), 3 * r.u))
        r.canvas.drawArc(rect, -90f, 360f * m.progress, false, r.strokePaint(Color.WHITE, 3 * r.u))
        btn(r, "play_pause", cx, cy, rad * 0.62f, m, bg = withAlpha(Color.WHITE, 50), glyphColor = Color.WHITE)
        btn(r, "prev", cx - rad * 1.8f, cy, rad * 0.4f, m, false, Color.WHITE)
        btn(r, "next", cx + rad * 1.8f, cy, rad * 0.4f, m, false, Color.WHITE)
        labels(r, m, cx, r.h * 0.82f, r.w - 2 * r.pad, 11f, Color.WHITE, withAlpha(Color.WHITE, 200), Paint.Align.CENTER)
    }

    fun card(r: RenderCtx) {
        val m = st(r)
        val p = r.pad * 0.7f
        val a = RectF(p, p, r.w - p, r.h * 0.48f)
        art(r, m, a, 14 * r.u)
        openHit(r, a)
        labels(r, m, r.w / 2, r.h * 0.62f, r.w - 2 * p, 11f, align = Paint.Align.CENTER)
        trio(r, m, r.w / 2, r.h * 0.84f, 12 * r.u, 34 * r.u)
    }

    // -------------------------------------------------------------- wide
    private fun progressRow(r: RenderCtx, m: MediaState, x: Float, y: Float, width: Float, color: Int = r.c1) {
        r.progressBar(x, y, width, m.progress, color, withAlpha(color, 50), 3 * r.u)
        r.text(time(r, m.position), x, y + 11 * r.u, 8.5f * r.u, withAlpha(color, 170), r.sans)
        r.text(time(r, m.duration), x + width, y + 11 * r.u, 8.5f * r.u, withAlpha(color, 170), r.sans, Paint.Align.RIGHT)
    }

    fun player(r: RenderCtx) {
        val m = st(r)
        val p = r.pad * 0.8f
        val s = r.h - 2 * p
        val a = RectF(p, p, p + s, p + s)
        art(r, m, a, 18 * r.u)
        openHit(r, a)
        val x = a.right + 14 * r.u
        val right = r.w - p - 6 * r.u
        labels(r, m, x, p + 22 * r.u, right - x - 28 * r.u)
        Glyphs.note(r.canvas, right - 8 * r.u, p + 18 * r.u, 18 * r.u, r.c1)
        progressRow(r, m, x, r.h * 0.56f, right - x)
        trio(r, m, (x + right) / 2, r.h - p - 18 * r.u, 13 * r.u, 40 * r.u)
    }

    fun deck(r: RenderCtx) {
        val m = st(r)
        art(r, m, RectF(0f, 0f, r.w * 0.62f, r.h), 0f)
        val g = Paint(); g.shader = LinearGradient(r.w * 0.25f, 0f, r.w * 0.62f, 0f, Color.TRANSPARENT, r.bgColorSafe(), Shader.TileMode.CLAMP)
        r.canvas.drawRect(0f, 0f, r.w * 0.62f + 2, r.h, g)
        openHit(r, RectF(0f, 0f, r.w * 0.45f, r.h))
        val x = r.w * 0.5f
        labels(r, m, x, r.pad + 20 * r.u, r.w * 0.36f)
        Glyphs.note(r.canvas, r.w - r.pad - 10 * r.u, r.pad + 16 * r.u, 18 * r.u, r.c1)
        trio(r, m, r.w * 0.75f, r.h - r.pad - 22 * r.u, 13 * r.u, 40 * r.u)
    }

    fun bigPlay(r: RenderCtx) {
        val m = st(r)
        val p = r.pad * 0.8f
        val a = RectF(p, p, p + 44 * r.u, p + 44 * r.u)
        art(r, m, a, 12 * r.u)
        openHit(r, a)
        labels(r, m, a.right + 10 * r.u, a.centerY(), r.w * 0.28f, 11f)
        progressRow(r, m, p + 4 * r.u, r.h - p - 18 * r.u, r.w * 0.44f)
        val rad = 22 * r.u
        val gx = r.w - p - rad - 4 * r.u
        val gy = r.h / 2
        btn(r, "open", gx - rad * 2.3f, gy - rad * 1.15f, rad, m)
        btn(r, "play_pause", gx, gy - rad * 1.15f, rad, m)
        btn(r, "prev", gx - rad * 2.3f, gy + rad * 1.15f, rad, m)
        btn(r, "next", gx, gy + rad * 1.15f, rad, m)
    }

    fun orbit(r: RenderCtx) {
        val m = st(r)
        val x = r.pad + 8 * r.u
        labels(r, m, x, r.pad + 20 * r.u, r.w * 0.5f)
        r.text(r.n("${time(r, m.position)} / ${time(r, m.duration)}"), x, r.pad + 46 * r.u, 9 * r.u, r.c2, r.sans)
        trio(r, m, x + 52 * r.u, r.h - r.pad - 24 * r.u, 13 * r.u, 40 * r.u)
        val rad = min(r.h / 2 - r.pad, r.w * 0.2f)
        val cx = r.w - r.pad - rad - 6 * r.u; val cy = r.h / 2
        val save = r.canvas.save()
        r.canvas.clipPath(Path().apply { addCircle(cx, cy, rad * 0.84f, Path.Direction.CW) })
        art(r, m, RectF(cx - rad, cy - rad, cx + rad, cy + rad), 0f)
        r.canvas.restoreToCount(save)
        val ring = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(ring, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 40), 3 * r.u))
        r.canvas.drawArc(ring, -90f, 360f * m.progress, false, r.strokePaint(r.c1, 3 * r.u))
        openHit(r, ring)
    }

    fun wave(r: RenderCtx) {
        val m = st(r)
        val p = r.pad * 0.8f
        val a = RectF(p, p, p + 34 * r.u, p + 34 * r.u)
        art(r, m, a, 9 * r.u)
        openHit(r, a)
        labels(r, m, a.right + 10 * r.u, a.centerY(), r.w * 0.5f, 11f)
        Glyphs.headphones(r.canvas, r.w - p - 34 * r.u, a.centerY(), 16 * r.u, r.c1)
        Glyphs.note(r.canvas, r.w - p - 10 * r.u, a.centerY(), 16 * r.u, r.c1)
        // waveform
        val top = a.bottom + 10 * r.u
        val wh = r.h * 0.22f
        val n = 64
        val bw = (r.w - 2 * p - 8 * r.u) / n
        for (i in 0 until n) {
            val amp = (0.25f + 0.75f * abs(sin(i * 0.55f) * sin(i * 0.17f + 1))).coerceIn(0.15f, 1f)
            val x = p + 4 * r.u + bw * (i + 0.5f)
            val played = i.toFloat() / n <= m.progress
            r.canvas.drawLine(x, top + wh / 2 - wh * amp / 2, x, top + wh / 2 + wh * amp / 2, r.strokePaint(if (played) r.c1 else withAlpha(r.c1, 80), bw * 0.5f))
        }
        r.text(time(r, m.position), p + 4 * r.u, top + wh + 8 * r.u, 8 * r.u, r.c2, r.sans)
        r.text(time(r, m.duration), r.w - p - 4 * r.u, top + wh + 8 * r.u, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        val cy = r.h - p - 14 * r.u
        val step = (r.w - 2 * p) / 7
        listOf("vol_down", "shuffle", "prev", "play_pause", "next", "repeat", "vol_up").forEachIndexed { i, k ->
            btn(r, k, p + step * (i + 0.5f), cy, if (k == "play_pause" || k == "prev" || k == "next") 12 * r.u else 9 * r.u, m, k == "play_pause" || k == "prev" || k == "next")
        }
    }

    fun book(r: RenderCtx) {
        val m = st(r)
        val p = r.pad * 0.8f
        val s = r.h - 2 * p
        val a = RectF(p, p, p + s, p + s)
        art(r, m, a, 16 * r.u)
        openHit(r, a)
        val x = a.right + 16 * r.u
        labels(r, m, x, r.h * 0.42f, r.w - x - p - 30 * r.u)
        Glyphs.note(r.canvas, r.w - p - 12 * r.u, p + 16 * r.u, 18 * r.u, r.c1)
        btn(r, "play_pause", x + 14 * r.u, r.h * 0.7f, 14 * r.u, m)
        r.text(time(r, m.position), x + 36 * r.u, r.h * 0.7f, 10 * r.u, r.c2, r.sans)
    }

    fun poster(r: RenderCtx) {
        val m = st(r)
        art(r, m, RectF(0f, 0f, r.w, r.h), 0f)
        val g = Paint(); g.shader = LinearGradient(0f, r.h * 0.35f, 0f, r.h, Color.TRANSPARENT, withAlpha(Color.BLACK, 210), Shader.TileMode.CLAMP)
        if (r.cfg.bg) r.canvas.drawRect(0f, 0f, r.w, r.h, g)
        openHit(r, RectF(0f, 0f, r.w, r.h * 0.5f))
        Glyphs.note(r.canvas, r.w - r.pad - 12 * r.u, r.pad + 14 * r.u, 18 * r.u, Color.WHITE)
        val x = r.pad + 4 * r.u
        labels(r, m, x, r.h * 0.62f, r.w - 2 * x, 13f, Color.WHITE, withAlpha(Color.WHITE, 200))
        progressRow(r, m, x, r.h * 0.74f, r.w - 2 * x, Color.WHITE)
        val cy = r.h - r.pad - 16 * r.u
        val step = (r.w - 2 * r.pad) / 7
        listOf("vol_down", "shuffle", "prev", "play_pause", "next", "repeat", "vol_up").forEachIndexed { i, k ->
            val big = k == "play_pause" || k == "prev" || k == "next"
            btn(r, k, r.pad + step * (i + 0.5f), cy, if (big) 14 * r.u else 10 * r.u, m, big, Color.WHITE, withAlpha(Color.WHITE, 50))
        }
    }
}
