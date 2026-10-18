package com.smrh.zujaj.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.WidgetStyle
import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.Texture
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class RenderResult(val bitmap: Bitmap, val lives: List<Live>, val hits: List<Hit>, val scale: Float, val fx: Bitmap? = null)

object Renderer {
    /** Max bitmap side in px pushed to the launcher (keeps RemoteViews memory small). */
    private const val MAX_SIDE = 1000

    /**
     * Renders a widget at [wDp] x [hDp].
     * [scale] = output px per dp (may be smaller than device density for big widgets).
     */
    fun render(
        ctx: Context, cfg: WidgetConfig, wDp: Float, hDp: Float, preview: Boolean,
        density: Float = ctx.resources.displayMetrics.density, data: RenderData = RenderData(), bake: Boolean = false,
    ): RenderResult {
        val style = Catalog.byId(cfg.styleId) ?: Catalog.all.first()
        var scale = density
        val longest = max(wDp, hDp) * scale
        if (longest > MAX_SIDE) scale *= MAX_SIDE / longest
        val bw = max(1, (wDp * scale).toInt())
        val bh = max(1, (hDp * scale).toInt())
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val rc = RenderCtx(ctx, bmp, style, cfg, preview, scale, data)
        rc.bakeLive = bake
        drawBackground(rc)
        val save = rc.canvas.save()
        rc.fxClip = Path().roundRect(RectF(0f, 0f, rc.w, rc.h), cornerPx(rc))
        rc.canvas.clipPath(rc.fxClip!!)
        runCatching { style.draw(rc) }.onFailure { e ->
            rc.text("⚠ ${e.javaClass.simpleName}", rc.w / 2, rc.h / 2, 12 * rc.u, Color.WHITE, F.SANS, Paint.Align.CENTER)
        }
        rc.canvas.restoreToCount(save)
        return RenderResult(bmp, rc.lives, rc.hits, scale, rc.fx)
    }

    /**
     * Renders for a placed widget of any size: the background always fills the whole widget
     * (stretch it tall or wide and the card grows with it), while the design itself keeps its
     * own proportions, as big as fits, centred on that background.
     */
    fun renderFitted(
        ctx: Context, cfg: WidgetConfig, wDp: Float, hDp: Float,
        density: Float = ctx.resources.displayMetrics.density, bake: Boolean = false,
    ): RenderResult {
        val style = Catalog.byId(cfg.styleId) ?: Catalog.all.first()
        val aspect = style.shape.dw / style.shape.dh
        val slot = wDp / hDp
        if (kotlin.math.abs(kotlin.math.ln(aspect / slot)) <= 0.18f) return render(ctx, cfg, wDp, hDp, false, density, bake = bake)
        var scale = density
        val longest = max(wDp, hDp) * scale
        if (longest > MAX_SIDE) scale *= MAX_SIDE / longest
        // 1) full-size card: background, texture and border only
        val bw = max(1, (wDp * scale).toInt()); val bh = max(1, (hDp * scale).toInt())
        val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
        val bgCtx = RenderCtx(ctx, bmp, style, cfg, false, scale)
        drawBackground(bgCtx)
        // 2) the design at its own proportions, without its own card
        val pad = 6f
        val availW = wDp - 2 * pad; val availH = hDp - 2 * pad
        val rw: Float; val rh: Float
        if (availW / availH > aspect) { rh = availH; rw = availH * aspect } else { rw = availW; rh = availW / aspect }
        val inner = render(ctx, cfg.copy(bg = false), rw, rh, false, scale, bake = bake)
        val ox = (bw - inner.bitmap.width) / 2f
        val oy = (bh - inner.bitmap.height) / 2f
        Canvas(bmp).drawBitmap(inner.bitmap, ox, oy, Paint(Paint.FILTER_BITMAP_FLAG))
        val fx = inner.fx?.let { f ->
            Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888).also { Canvas(it).drawBitmap(f, ox, oy, null) }
        }
        fun RectF.off() = RectF(left + ox, top + oy, right + ox, bottom + oy)
        val lives = inner.lives.map { l ->
            when (l) {
                is LiveClock -> l.copy(rect = l.rect.off())
                is LiveChrono -> l.copy(rect = l.rect.off())
                is LiveAnalog -> l.copy(rect = l.rect.off())
            }
        }
        val hits = inner.hits.map { it.copy(rect = it.rect.off()) }
        return RenderResult(bmp, lives, hits, scale, fx)
    }

    fun cornerPx(rc: RenderCtx) = min(rc.cfg.corner * rc.u, min(rc.w, rc.h) / 2f)

    private var noise: Bitmap? = null
    private fun noiseBitmap(): Bitmap = noise ?: run {
        val n = 96
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val r = Random(7)
        val px = IntArray(n * n) {
            val v = r.nextInt(256)
            Color.argb(r.nextInt(18, 46), v, v, v)
        }
        b.setPixels(px, 0, n, 0, 0, n, n)
        noise = b
        b
    }

    fun drawBackground(rc: RenderCtx) {
        val cfg = rc.cfg
        if (!cfg.bg || cfg.fill == Fill.NONE) return
        val c = rc.canvas
        val r = RectF(0f, 0f, rc.w, rc.h)
        val rad = cornerPx(rc)
        val a = (cfg.bgAlpha.coerceIn(0, 100) * 255 / 100)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        when (cfg.fill) {
            Fill.SOLID -> {
                p.color = withAlpha(cfg.bgColor, a)
                c.drawRoundRect(r, rad, rad, p)
                p.color = Color.WHITE
                p.shader = LinearGradient(0f, 0f, 0f, rc.h * 0.6f, withAlpha(Color.WHITE, (a * 0.06f).toInt()), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                c.drawRoundRect(r, rad, rad, p)
            }
            Fill.GRADIENT -> {
                p.shader = LinearGradient(0f, 0f, rc.w, rc.h, withAlpha(cfg.bgColor, a), withAlpha(cfg.bgColor2, a), Shader.TileMode.CLAMP)
                c.drawRoundRect(r, rad, rad, p)
            }
            Fill.GLASS -> {
                p.color = withAlpha(cfg.bgColor, (a * 0.42f).toInt())
                c.drawRoundRect(r, rad, rad, p)
                p.color = Color.WHITE
                p.shader = LinearGradient(0f, 0f, rc.w, rc.h,
                    intArrayOf(withAlpha(Color.WHITE, (a * 0.22f).toInt()), withAlpha(Color.WHITE, (a * 0.06f).toInt()), withAlpha(Color.WHITE, (a * 0.12f).toInt())),
                    floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
                c.drawRoundRect(r, rad, rad, p)
            }
            Fill.AURORA -> {
                p.color = withAlpha(cfg.bgColor, a)
                c.drawRoundRect(r, rad, rad, p)
                val save = c.save()
                c.clipPath(Path().roundRect(r, rad))
                fun blob(x: Float, y: Float, radius: Float, col: Int) {
                    val bp = Paint(Paint.ANTI_ALIAS_FLAG)
                    bp.shader = RadialGradient(x, y, radius, withAlpha(col, (a * 0.85f).toInt()), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                    c.drawCircle(x, y, radius, bp)
                }
                val m = max(rc.w, rc.h)
                blob(rc.w * 0.15f, rc.h * 0.1f, m * 0.7f, cfg.bgColor2)
                blob(rc.w * 0.95f, rc.h * 0.9f, m * 0.6f, cfg.accent)
                blob(rc.w * 0.7f, rc.h * 0.0f, m * 0.4f, RenderCtx.mix(cfg.bgColor2, cfg.accent, 0.5f))
                c.restoreToCount(save)
            }
            Fill.NONE -> {}
        }
        // texture
        if (cfg.texture != Texture.OFF) {
            val save = c.save()
            c.clipPath(Path().roundRect(r, rad))
            val tp = Paint(Paint.ANTI_ALIAS_FLAG)
            val tc = withAlpha(Color.WHITE, 22)
            when (cfg.texture) {
                Texture.NOISE, Texture.GRAIN -> {
                    val sh = BitmapShader(noiseBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                    if (cfg.texture == Texture.GRAIN) sh.setLocalMatrix(Matrix().apply { setScale(rc.u * 1.4f, rc.u * 1.4f) })
                    tp.shader = sh
                    c.drawRect(r, tp)
                }
                Texture.LINES -> {
                    tp.color = tc; tp.strokeWidth = rc.u
                    var x = -rc.h
                    while (x < rc.w) { c.drawLine(x, rc.h, x + rc.h, 0f, tp); x += 9 * rc.u }
                }
                Texture.DOTS -> {
                    tp.color = tc
                    var y = 6 * rc.u
                    while (y < rc.h) { var x = 6 * rc.u; while (x < rc.w) { c.drawCircle(x, y, rc.u * 0.9f, tp); x += 10 * rc.u }; y += 10 * rc.u }
                }
                Texture.GRID -> {
                    tp.color = withAlpha(Color.WHITE, 14); tp.strokeWidth = rc.u * 0.6f
                    var x = 0f; while (x < rc.w) { c.drawLine(x, 0f, x, rc.h, tp); x += 14 * rc.u }
                    var y = 0f; while (y < rc.h) { c.drawLine(0f, y, rc.w, y, tp); y += 14 * rc.u }
                }
                Texture.WAVES -> {
                    tp.color = tc; tp.style = Paint.Style.STROKE; tp.strokeWidth = rc.u
                    var y = 0f
                    while (y < rc.h + 20 * rc.u) {
                        val path = Path(); path.moveTo(0f, y)
                        var x = 0f
                        while (x < rc.w) { path.rQuadTo(10 * rc.u, -6 * rc.u, 20 * rc.u, 0f); path.rQuadTo(10 * rc.u, 6 * rc.u, 20 * rc.u, 0f); x += 40 * rc.u }
                        c.drawPath(path, tp); y += 12 * rc.u
                    }
                }
                Texture.OFF -> {}
            }
            c.restoreToCount(save)
        }
        if (cfg.border) {
            val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = max(1f, rc.u * 0.9f)
                shader = LinearGradient(0f, 0f, rc.w, rc.h,
                    withAlpha(Color.WHITE, if (cfg.fill == Fill.GLASS) 90 else 34),
                    withAlpha(Color.WHITE, if (cfg.fill == Fill.GLASS) 24 else 8), Shader.TileMode.CLAMP)
            }
            val i = bp.strokeWidth / 2
            c.drawRoundRect(RectF(i, i, rc.w - i, rc.h - i), rad - i, rad - i, bp)
        }
    }

    /** Draws [src] centre-cropped into [dst] with rounded corners. */
    fun drawImage(c: Canvas, src: Bitmap, dst: RectF, radius: Float, alpha: Int = 255) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val sh = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val s = max(dst.width() / src.width, dst.height() / src.height)
        val m = Matrix()
        m.setScale(s, s)
        m.postTranslate(dst.centerX() - src.width * s / 2, dst.centerY() - src.height * s / 2)
        sh.setLocalMatrix(m)
        p.shader = sh
        p.alpha = alpha
        c.drawRoundRect(dst, radius, radius, p)
    }

    /** A colourful placeholder "album art" gradient (no copyrighted art). */
    fun drawArtPlaceholder(c: Canvas, dst: RectF, radius: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(dst.left, dst.top, dst.right, dst.bottom,
            intArrayOf(0xFF6A5CFF.toInt(), 0xFFE8478B.toInt(), 0xFFFFA25C.toInt(), 0xFF38C6D9.toInt()),
            floatArrayOf(0f, 0.4f, 0.7f, 1f), Shader.TileMode.CLAMP)
        c.drawRoundRect(dst, radius, radius, p)
        val g = Paint(Paint.ANTI_ALIAS_FLAG)
        g.shader = RadialGradient(dst.left + dst.width() * 0.3f, dst.top + dst.height() * 0.25f, dst.width() * 0.7f,
            withAlpha(Color.WHITE, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        c.drawRoundRect(dst, radius, radius, g)
    }
}
