package com.smrh.zujaj.wall

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Lang
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.F
import com.smrh.zujaj.render.Kit
import com.smrh.zujaj.render.RenderCtx
import com.smrh.zujaj.render.RenderData
import com.smrh.zujaj.render.Renderer
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** Draws a wallpaper design: photo → layers behind the subject → the subject → layers in front. */
object WallRender {
    /** Layer kinds offered in the picker, with their width:height. */
    val kinds = listOf(
        "clock_tall" to 1.15f, "clock_thin" to 1.0f, "clock_stack" to 0.62f,
        "day_calli" to 2.2f, "month_greg" to 2.2f, "month_hijri" to 2.2f, "date_pair" to 4.2f,
        "date_greg" to 6f, "date_hijri" to 6f, "hijri_full" to 6f, "dual_full" to 4f,
        "prayer_next" to 4f, "prayer_in" to 4f, "prayer_row" to 3.6f, "location" to 4f, "text" to 3.4f,
    )

    fun aspect(l: WallLayer): Float = if (l.ar > 0f) l.ar else naturalAspect(l)
    fun naturalAspect(l: WallLayer): Float = if (l.kind == "widget") Catalog.byId(l.styleId)?.shape?.let { it.dw / it.dh } ?: 2f
        else kinds.firstOrNull { it.first == l.kind }?.second ?: 3f

    fun loadImage(path: String, maxSide: Int = 2400, trim: Boolean = true): Bitmap? = runCatching {
        if (path.isBlank() || !File(path).exists()) return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1
        while (max(o.outWidth, o.outHeight) / (s * 2) >= maxSide) s *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })?.let { if (trim) trimBars(it) else it }
    }.getOrNull()

    /** Cuts off flat black (or white) bars around a photo — e.g. a screenshot of a picture. */
    fun trimBars(b: Bitmap): Bitmap {
        val w = b.width; val h = b.height
        if (w < 40 || h < 40) return b
        fun flat(px: IntArray): Boolean {
            val first = px[0]
            val dark = Color.red(first) < 28 && Color.green(first) < 28 && Color.blue(first) < 28
            val light = Color.red(first) > 235 && Color.green(first) > 235 && Color.blue(first) > 235
            if (!dark && !light) return false
            return px.all { abs(Color.red(it) - Color.red(first)) < 14 && abs(Color.green(it) - Color.green(first)) < 14 && abs(Color.blue(it) - Color.blue(first)) < 14 }
        }
        val step = 4
        fun row(y: Int) = IntArray(w / step) { b.getPixel(it * step, y) }
        fun col(x: Int) = IntArray(h / step) { b.getPixel(x, it * step) }
        var top = 0; while (top < h / 2 && flat(row(top))) top++
        var bot = h - 1; while (bot > top + h / 4 && flat(row(bot))) bot--
        var left = 0; while (left < w / 2 && flat(col(left))) left++
        var right = w - 1; while (right > left + w / 4 && flat(col(right))) right--
        val nw = right - left + 1; val nh = bot - top + 1
        if (nw >= w - 4 && nh >= h - 4) return b
        if (nw < w * 0.3f || nh < h * 0.3f) return b
        return Bitmap.createBitmap(b, left, top, nw, nh)
    }

    /** Matrix placing the photo into W×H: fills the screen, then the user's zoom / move on top. */
    fun cropMatrix(iw: Int, ih: Int, W: Int, H: Int, d: WallDesign? = null): Matrix {
        val sc = max(W / iw.toFloat(), H / ih.toFloat()) * (d?.zoom ?: 1f)
        return Matrix().apply { setScale(sc, sc); postTranslate((W - iw * sc) / 2f + (d?.px ?: 0f) * W, (H - ih * sc) / 2f + (d?.py ?: 0f) * H) }
    }

    /** Smooth (not blocky) blur: shrink in halves, then grow back in doubles. */
    fun smoothBlur(src: Bitmap, factor: Int): Bitmap {
        var b = src; var f = 1
        while (f < factor && b.width > 4 && b.height > 4) { b = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true); f *= 2 }
        while (b.width * 2 <= src.width && b.height * 2 <= src.height) b = Bitmap.createScaledBitmap(b, b.width * 2, b.height * 2, true)
        return Bitmap.createScaledBitmap(b, src.width, src.height, true)
    }

    private fun photoRect(photo: Bitmap, d: WallDesign, W: Int, H: Int) =
        RectF(0f, 0f, photo.width.toFloat(), photo.height.toFloat()).also { cropMatrix(photo.width, photo.height, W, H, d).mapRect(it) }

    private fun covers(r: RectF, W: Int, H: Int) = r.left <= 0.5f && r.top <= 0.5f && r.right >= W - 0.5f && r.bottom >= H - 0.5f

    /** Fills the empty space around a photo that is smaller than the screen, so it looks like the photo continues. */
    private fun fill(c: Canvas, photo: Bitmap, d: WallDesign, W: Int, H: Int) {
        val r = photoRect(photo, d, W, H)
        if (covers(r, W, H)) return
        val ext = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val e = Canvas(ext)
        val p = Paint(Paint.FILTER_BITMAP_FLAG)
        val pw = photo.width; val ph = photo.height
        val sw = (pw * 0.035f).toInt().coerceAtLeast(2); val shh = (ph * 0.035f).toInt().coerceAtLeast(2)
        when (d.fillMode) {
            1 -> { // mirror the photo around its edges
                fun flip(sx: Float, sy: Float, dx: Float, dy: Float) {
                    val m = Matrix().apply { setScale(r.width() / pw * sx, r.height() / ph * sy); postTranslate(dx, dy) }
                    e.drawBitmap(photo, m, p)
                }
                flip(1f, -1f, r.left, r.top); flip(1f, -1f, r.left, r.bottom + r.height())
                flip(-1f, 1f, r.left, r.top); flip(-1f, 1f, r.right + r.width(), r.top)
                flip(-1f, -1f, r.left, r.top); flip(-1f, -1f, r.right + r.width(), r.top)
                flip(-1f, -1f, r.left, r.bottom + r.height()); flip(-1f, -1f, r.right + r.width(), r.bottom + r.height())
            }
            2 -> { // big soft blur of the whole photo
                val cover = cropMatrix(pw, ph, W, H)
                e.drawBitmap(photo, cover, p)
            }
            3 -> { // smooth colour gradient from the photo's edges
                val t = Bitmap.createScaledBitmap(Bitmap.createBitmap(photo, 0, 0, pw, shh), 1, 1, true).getPixel(0, 0)
                val bt = Bitmap.createScaledBitmap(Bitmap.createBitmap(photo, 0, ph - shh, pw, shh), 1, 1, true).getPixel(0, 0)
                e.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, 0f, H.toFloat(), t, bt, android.graphics.Shader.TileMode.CLAMP) })
            }
            else -> { // extend: stretch the outermost rows / columns outwards
                val src = android.graphics.Rect()
                if (r.top > 0) { src.set(0, 0, pw, shh); e.drawBitmap(photo, src, RectF(r.left, 0f, r.right, r.top + 2), p) }
                if (r.bottom < H) { src.set(0, ph - shh, pw, ph); e.drawBitmap(photo, src, RectF(r.left, r.bottom - 2, r.right, H.toFloat()), p) }
                if (r.left > 0) { src.set(0, 0, sw, ph); e.drawBitmap(photo, src, RectF(0f, r.top, r.left + 2, r.bottom), p) }
                if (r.right < W) { src.set(pw - sw, 0, pw, ph); e.drawBitmap(photo, src, RectF(r.right - 2, r.top, W.toFloat(), r.bottom), p) }
                // corners: the corner pixels' colours
                fun corner(px: Int, py: Int, rc: RectF) { if (rc.width() > 0 && rc.height() > 0) e.drawRect(rc, Paint().apply { color = photo.getPixel(px, py) }) }
                corner(0, 0, RectF(0f, 0f, r.left, r.top)); corner(pw - 1, 0, RectF(r.right, 0f, W.toFloat(), r.top))
                corner(0, ph - 1, RectF(0f, r.bottom, r.left, H.toFloat())); corner(pw - 1, ph - 1, RectF(r.right, r.bottom, W.toFloat(), H.toFloat()))
            }
        }
        // the photo itself too, so the blur never mixes in empty (dark) pixels at the seam
        if (d.fillMode < 2) e.drawBitmap(photo, null, r, p)
        val blurred = smoothBlur(ext, when (d.fillMode) { 2 -> 32; 1 -> 8; 3 -> 1; else -> 6 })
        c.drawBitmap(blurred, 0f, 0f, p)
    }

    /** Draws the photo; where it meets a filled gap its edge is feathered so the seam disappears. */
    private fun drawPhoto(c: Canvas, b: Bitmap, m: Matrix, r: RectF, W: Int, H: Int, p: Paint) {
        if (covers(r, W, H)) { c.drawBitmap(b, m, p); return }
        val layer = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val lc = Canvas(layer)
        lc.drawBitmap(b, m, p)
        val f = minOf(W, H) * 0.06f
        val out = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
        fun fade(x0: Float, y0: Float, x1: Float, y1: Float, rc: RectF) {
            out.shader = android.graphics.LinearGradient(x0, y0, x1, y1, Color.BLACK, Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP)
            lc.drawRect(rc, out)
        }
        if (r.top > 0.5f) fade(0f, r.top, 0f, r.top + f, RectF(r.left, r.top, r.right, r.top + f))
        if (r.bottom < H - 0.5f) fade(0f, r.bottom, 0f, r.bottom - f, RectF(r.left, r.bottom - f, r.right, r.bottom))
        if (r.left > 0.5f) fade(r.left, 0f, r.left + f, 0f, RectF(r.left, r.top, r.left + f, r.bottom))
        if (r.right < W - 0.5f) fade(r.right, 0f, r.right - f, 0f, RectF(r.right - f, r.top, r.right, r.bottom))
        c.drawBitmap(layer, 0f, 0f, null)
    }

    /** Frosted glass plate for the area [rc] of an already-drawn wallpaper [under]. */
    fun glassPlate(under: Bitmap, rc: RectF): Bitmap? {
        val pad = minOf(rc.width(), rc.height()) * 0.12f
        val r = RectF(rc.left - pad, rc.top - pad, rc.right + pad, rc.bottom + pad)
        val x0 = r.left.toInt().coerceIn(0, under.width - 2); val y0 = r.top.toInt().coerceIn(0, under.height - 2)
        val x1 = r.right.toInt().coerceIn(x0 + 1, under.width); val y1 = r.bottom.toInt().coerceIn(y0 + 1, under.height)
        val w = (r.width().toInt()).coerceAtLeast(2); val h = (r.height().toInt()).coerceAtLeast(2)
        val crop = Bitmap.createBitmap(under, x0, y0, x1 - x0, y1 - y0)
        val small = Bitmap.createScaledBitmap(crop, (crop.width / 14).coerceAtLeast(2), (crop.height / 14).coerceAtLeast(2), true)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val rad = minOf(w, h) * 0.24f
        val path = android.graphics.Path().apply { addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), rad, rad, android.graphics.Path.Direction.CW) }
        c.save(); c.clipPath(path)
        c.drawBitmap(small, null, RectF(x0 - r.left, y0 - r.top, x1 - r.left, y1 - r.top), Paint(Paint.FILTER_BITMAP_FLAG))
        c.drawColor(Color.argb(46, 255, 255, 255))
        c.drawRect(0f, 0f, w.toFloat(), h * 0.5f, Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, 0f, h * 0.5f, Color.argb(34, 255, 255, 255), Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP) })
        c.restore()
        val st = minOf(w, h) * 0.012f + 1f
        c.drawRoundRect(RectF(st / 2, st / 2, w - st / 2, h - st / 2), rad, rad, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = st; color = Color.argb(90, 255, 255, 255) })
        return out
    }
    /** Turns a layer into glass: its shapes are filled with the blurred wallpaper behind them, with a soft shine and a bright rim. */
    fun glassify(layer: Bitmap, under: Bitmap, dst: RectF): Bitmap {
        val w = layer.width; val h = layer.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val x0 = dst.left.toInt().coerceIn(0, under.width - 2); val y0 = dst.top.toInt().coerceIn(0, under.height - 2)
        val x1 = dst.right.toInt().coerceIn(x0 + 1, under.width); val y1 = dst.bottom.toInt().coerceIn(y0 + 1, under.height)
        val crop = Bitmap.createBitmap(under, x0, y0, x1 - x0, y1 - y0)
        val small = Bitmap.createScaledBitmap(crop, (crop.width / 10).coerceAtLeast(2), (crop.height / 10).coerceAtLeast(2), true)
        val sx = w / dst.width(); val sy = h / dst.height()
        c.drawBitmap(small, null, RectF((x0 - dst.left) * sx, (y0 - dst.top) * sy, (x1 - dst.left) * sx, (y1 - dst.top) * sy), Paint(Paint.FILTER_BITMAP_FLAG))
        // milky frost + light from the top
        c.drawColor(Color.argb(70, 255, 255, 255))
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, 0f, h.toFloat(), Color.argb(80, 255, 255, 255), Color.argb(0, 255, 255, 255), android.graphics.Shader.TileMode.CLAMP) })
        // keep only the shapes of the layer
        c.drawBitmap(layer, 0f, 0f, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        // glossy inner rim
        val a = layer.extractAlpha()
        val r = (minOf(w, h) * 0.035f).coerceIn(1.5f, 14f)
        c.drawBitmap(a, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 255, 255, 255); maskFilter = BlurMaskFilter(r, BlurMaskFilter.Blur.INNER) })
        return out
    }

    fun glassRect(rc: RectF): RectF { val pad = minOf(rc.width(), rc.height()) * 0.12f; return RectF(rc.left - pad, rc.top - pad, rc.right + pad, rc.bottom + pad) }

    /** Box of a layer in px. */
    fun rect(l: WallLayer, W: Int, H: Int): RectF {
        val w = l.w * W; val h = w / aspect(l)
        return RectF(l.cx * W - w / 2, l.cy * H - h / 2, l.cx * W + w / 2, l.cy * H + h / 2)
    }

    private fun ctxFor(ctx: Context, b: Bitmap, l: WallLayer, now: ZonedDateTime): RenderCtx {
        val st = Catalog.byId("calli_big_day") ?: Catalog.all.first()
        val cfg = WidgetConfig(st.id, bg = false, color1 = l.color, color2 = Color.argb(200, Color.red(l.color), Color.green(l.color), Color.blue(l.color)),
            accent = l.color, calli = 3, padding = 0, language = if (Prefs.isArabicUi) Lang.AR else Lang.EN)
        return RenderCtx(ctx, b, st, cfg, true, 1f, RenderData(now))
    }

    private fun fitText(c: Canvas, s: String, box: RectF, p: Paint) {
        p.textAlign = Paint.Align.LEFT
        val b = Rect(); p.textSize = 100f; p.getTextBounds(s, 0, s.length, b)
        val k = minOf(box.width() / p.measureText(s), box.height() / b.height().coerceAtLeast(1))
        p.textSize = 100f * k
        p.getTextBounds(s, 0, s.length, b)
        val w = p.measureText(s)
        c.drawText(s, box.centerX() - w / 2, box.centerY() - b.exactCenterY(), p)
    }

    /** Renders one layer into its own bitmap (transparent background). */
    fun layerBitmap(ctx: Context, l: WallLayer, wPx: Int, now: ZonedDateTime = ZonedDateTime.now()): Bitmap {
        val w = wPx.coerceIn(8, 2400); val h = (w / aspect(l)).toInt().coerceAtLeast(8)
        if (l.kind == "widget") {
            val st = Catalog.byId(l.styleId)
            if (st != null) {
                val cfg = st.defaultConfig().copy(bg = false, color1 = l.color)
                val d = w / st.shape.dw
                val dh = if (l.ar > 0f) st.shape.dw / l.ar else st.shape.dh
                return runCatching { Renderer.render(ctx, cfg, st.shape.dw, dh, true, d, RenderData(now)).bitmap }.getOrElse { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
            }
        }
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val box = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val r = ctxFor(ctx, b, l, now)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = l.color }
        val date = Date(now.toInstant().toEpochMilli())
        fun t(pat: String) = r.n(SimpleDateFormat(pat, Locale.US).format(date))
        val hm = if (Prefs.isArabicUi && false) "" else if (android.text.format.DateFormat.is24HourFormat(ctx)) "HH:mm" else "hh:mm"
        when (l.kind) {
            "clock_tall" -> { p.typeface = r.tf(F.CLK_BEBAS); fitText(c, t(hm), box, p) }
            "clock_thin" -> { p.typeface = r.tf(F.CLK_SIXCAPS); fitText(c, t(hm), box, p) }
            "clock_stack" -> {
                p.typeface = r.tf(F.CLK_SIXCAPS)
                fitText(c, t(hm.take(2)), RectF(0f, 0f, w.toFloat(), h * 0.49f), p)
                fitText(c, t("mm"), RectF(0f, h * 0.51f, w.toFloat(), h.toFloat()), p)
            }
            "day_calli" -> r.textTight(Kit.dayName(r, true), w / 2f, h / 2f, h * 0.9f, l.color, r.calliFont, maxW = w * 0.98f, maxH = h * 0.92f)
            "month_greg", "month_hijri" -> {
                val (n, m) = if (l.kind == "month_greg") now.dayOfMonth to com.smrh.zujaj.data.Calendars.monthName(now.monthValue, true)
                    else com.smrh.zujaj.data.Calendars.hijri(now.toLocalDate()).let { it.day to com.smrh.zujaj.data.Calendars.hijriMonth(it.month, true) }
                p.typeface = r.tf(F.SANS_BOLD); p.alpha = 70
                fitText(c, r.n(n), RectF(w * 0.25f, 0f, w * 0.75f, h.toFloat()), p)
                r.textTight(m, w / 2f, h / 2f, h * 0.8f, l.color, r.calliFont, maxW = w * 0.98f, maxH = h * 0.85f)
            }
            "date_pair" -> {
                val hd = com.smrh.zujaj.data.Calendars.hijri(now.toLocalDate())
                p.typeface = r.tf(F.SANS_BOLD)
                fitText(c, r.n(now.dayOfMonth), RectF(w * 0.8f, h * 0.2f, w * 0.98f, h * 0.8f), p)
                r.textTight(com.smrh.zujaj.data.Calendars.monthName(now.monthValue, true), w * 0.64f, h / 2f, h * 0.7f, l.color, r.calliFont, maxW = w * 0.28f, maxH = h * 0.8f)
                c.drawRect(w * 0.495f, h * 0.25f, w * 0.505f, h * 0.75f, Paint().apply { color = l.color; alpha = 120 })
                r.textTight(com.smrh.zujaj.data.Calendars.hijriMonth(hd.month, true), w * 0.34f, h / 2f, h * 0.7f, l.color, r.calliFont, maxW = w * 0.28f, maxH = h * 0.8f)
                fitText(c, r.n(hd.day.toString().padStart(2, '0')), RectF(w * 0.02f, h * 0.2f, w * 0.18f, h * 0.8f), p)
            }
            "date_greg" -> { p.typeface = r.tf(F.SANS_MED); p.letterSpacing = 0.18f; fitText(c, if (r.ar) Kit.dayName(r, true) + "، " + Kit.gregShort(r) else SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(date).uppercase(), box, p) }
            "date_hijri" -> { p.typeface = r.tf(F.SANS_MED); p.letterSpacing = 0.18f; fitText(c, Kit.dayName(r) + "  " + Kit.hijriShort(r), box, p) }
            "hijri_full" -> { p.typeface = r.tf(F.SANS); fitText(c, Kit.dayName(r) + "، " + Kit.hijri(r), box, p) }
            "dual_full" -> {
                p.typeface = r.tf(F.SANS)
                fitText(c, Kit.dayName(r) + "، " + Kit.hijri(r), RectF(0f, 0f, w.toFloat(), h * 0.48f), p)
                fitText(c, Kit.greg(r), RectF(0f, h * 0.52f, w.toFloat(), h.toFloat()), p)
            }
            "prayer_next" -> { val nx = Kit.day(r).next; p.typeface = r.tf(F.SANS_MED); p.letterSpacing = 0.12f; fitText(c, Kit.pName(r, nx) + "  " + Kit.pTime(r, nx.time), box, p) }
            "prayer_in" -> {
                val nx = Kit.day(r).next
                val m = ((nx.time - now.toInstant().toEpochMilli()) / 60000).coerceAtLeast(0)
                p.typeface = r.tf(F.SANS_MED); p.letterSpacing = 0.12f
                fitText(c, Kit.pName(r, nx) + r.t(" in ", " بعد ") + r.n("${m / 60}:${(m % 60).toString().padStart(2, '0')}"), box, p)
            }
            "prayer_row" -> {
                val d = Kit.day(r); val list = Kit.ordered(r, d.five); val cw = w / list.size.toFloat()
                list.forEachIndexed { i, pr ->
                    val cx = cw * (i + 0.5f)
                    p.typeface = r.tf(F.SANS_MED); fitText(c, Kit.pName(r, pr), RectF(cx - cw * 0.45f, h * 0.18f, cx + cw * 0.45f, h * 0.42f), p)
                    p.typeface = r.tf(F.SANS_BOLD); fitText(c, Kit.pTime(r, pr.time), RectF(cx - cw * 0.42f, h * 0.55f, cx + cw * 0.42f, h * 0.85f), p)
                }
            }
            "location" -> { p.typeface = r.tf(F.SANS_MED); fitText(c, "⌖ " + Prefs.cityName.ifBlank { r.t("My location", "موقعي") }, box, p) }
            else -> { p.typeface = r.tf(if (r.ar) F.CAIRO_BOLD else F.SANS_BOLD); fitText(c, l.text.ifBlank { r.t("Type your text", "اكتب كلامك") }, box, p) }
        }
        return b
    }

    /** Layer bitmap with its solid/gradient colour applied. */
    fun styled(ctx: Context, l: WallLayer, wPx: Int, now: ZonedDateTime = ZonedDateTime.now()): Bitmap {
        val b = layerBitmap(ctx, l, wPx, now)
        if (l.color2 == 0) return b
        val out = if (b.isMutable) b else b.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(out)
        val (x1, y1) = when (l.gDir) { 1 -> out.width.toFloat() to 0f; 2 -> out.width.toFloat() to out.height.toFloat(); else -> 0f to out.height.toFloat() }
        val p = Paint().apply {
            shader = android.graphics.LinearGradient(0f, 0f, x1, y1, l.color, l.color2, android.graphics.Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        c.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), p)
        return out
    }

    /** The photo (blurred/darkened as set), cropped to W×H. */
    fun background(d: WallDesign, photo: Bitmap?, W: Int, H: Int): Bitmap {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.rgb(16, 20, 30))
        if (photo == null) return out
        fill(c, photo, d, W, H)
        val m = cropMatrix(photo.width, photo.height, W, H, d)
        val bp = Paint(Paint.FILTER_BITMAP_FLAG)
        if (d.blur > 0) {
            val k = (1 + d.blur / 4).coerceAtMost(24)
            val small = Bitmap.createScaledBitmap(photo, (photo.width / k).coerceAtLeast(2), (photo.height / k).coerceAtLeast(2), true)
            drawPhoto(c, small, Matrix(m).apply { preScale(photo.width / small.width.toFloat(), photo.height / small.height.toFloat()) }, photoRect(photo, d, W, H), W, H, bp)
        } else drawPhoto(c, photo, m, photoRect(photo, d, W, H), W, H, bp)
        if (d.dim > 0) c.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0))
        return out
    }

    /** Only the subject (sharp), transparent elsewhere. */
    fun subject(d: WallDesign, photo: Bitmap, mask: Bitmap, W: Int, H: Int): Bitmap {
        val m = cropMatrix(photo.width, photo.height, W, H, d)
        val sub = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val sc = Canvas(sub)
        sc.drawBitmap(photo, m, Paint(Paint.FILTER_BITMAP_FLAG))
        if (d.dim > 0) sc.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        val mp = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        sc.drawBitmap(mask, Matrix(m).apply { preScale(photo.width / mask.width.toFloat(), photo.height / mask.height.toFloat()) }, mp)
        return sub
    }

    /** A soft glowing outline of the subject (for the "found it" animation). */
    fun outline(photo: Bitmap, mask: Bitmap, W: Int, H: Int, color: Int, d: WallDesign? = null): Bitmap {
        val m = cropMatrix(photo.width, photo.height, W, H, d)
        val full = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        Canvas(full).drawBitmap(mask, Matrix(m).apply { preScale(photo.width / mask.width.toFloat(), photo.height / mask.height.toFloat()) }, Paint(Paint.FILTER_BITMAP_FLAG))
        val a = full.extractAlpha()
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; maskFilter = BlurMaskFilter(W * 0.012f, BlurMaskFilter.Blur.OUTER) }
        c.drawBitmap(a, 0f, 0f, glow)
        val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE; maskFilter = BlurMaskFilter(W * 0.003f, BlurMaskFilter.Blur.OUTER) }
        c.drawBitmap(a, 0f, 0f, rim)
        return out
    }

    /**
     * Mask of everything that is NOT sky: flood-fills from the top edge over pixels that look
     * like the sky. Great for mountains, domes, skylines and trees.
     */
    fun skyMask(photo: Bitmap): Bitmap? {
        val w = 380; val h = (photo.height * w / photo.width.toFloat()).toInt().coerceAtLeast(10)
        val s = Bitmap.createScaledBitmap(photo, w, h, true)
        val px = IntArray(w * h); s.getPixels(px, 0, w, 0, 0, w, h)
        fun d(a: Int, b: Int): Int { val dr = Color.red(a) - Color.red(b); val dg = Color.green(a) - Color.green(b); val db = Color.blue(a) - Color.blue(b); return dr * dr + dg * dg + db * db }
        val sky = BooleanArray(w * h)
        val q = ArrayDeque<Int>()
        for (x in 0 until w) { sky[x] = true; q.add(x) }
        val local = 18 * 18 * 3
        // the sky's colour drifts from top to horizon; allow that, but not into the subject
        var mr = 0L; var mg = 0L; var mb = 0L; val band = (h / 20).coerceAtLeast(1)
        for (i in 0 until w * band) { mr += Color.red(px[i]); mg += Color.green(px[i]); mb += Color.blue(px[i]) }
        val mean = Color.rgb((mr / (w * band)).toInt(), (mg / (w * band)).toInt(), (mb / (w * band)).toInt())
        val global = 85 * 85 * 3
        while (q.isNotEmpty()) {
            val i = q.removeFirst(); val x = i % w; val y = i / w
            for ((nx, ny) in listOf(x - 1 to y, x + 1 to y, x to y + 1, x to y - 1)) {
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                val j = ny * w + nx
                if (sky[j]) continue
                if (d(px[i], px[j]) < local && d(px[j], mean) < global) { sky[j] = true; q.add(j) }
            }
        }
        val count = sky.count { it }
        if (count < w * h / 12 || count > w * h * 0.92) return null
        val out = IntArray(w * h) { if (sky[it]) 0 else Color.BLACK }
        val small = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        // smooth edges
        val big = Bitmap.createScaledBitmap(small, w * 4, h * 4, true)
        return Bitmap.createScaledBitmap(Bitmap.createScaledBitmap(big, w * 2, h * 2, true), photo.width.coerceAtMost(1600), (photo.height * (photo.width.coerceAtMost(1600) / photo.width.toFloat())).toInt(), true)
    }

    /** Fraction of the photo the mask covers. */
    fun coverage(mask: Bitmap): Float {
        val s = Bitmap.createScaledBitmap(mask, 64, 64, true)
        var n = 0
        for (y in 0 until 64) for (x in 0 until 64) if (Color.alpha(s.getPixel(x, y)) > 128) n++
        return n / 4096f
    }

    /** k-means on a pixel list → (colour, share) sorted by share. */
    private fun clusters(px: IntArray, k: Int): List<Pair<Int, Float>> {
        if (px.isEmpty()) return emptyList()
        val cent = Array(k) { i -> px[(i * px.size / k).coerceAtMost(px.size - 1)] }.map { floatArrayOf(Color.red(it).toFloat(), Color.green(it).toFloat(), Color.blue(it).toFloat()) }.toMutableList()
        val assign = IntArray(px.size)
        repeat(8) {
            for ((i, c) in px.withIndex()) {
                val r = Color.red(c); val g = Color.green(c); val b = Color.blue(c)
                var best = 0; var bd = Float.MAX_VALUE
                for ((j, m) in cent.withIndex()) { val d = (r - m[0]) * (r - m[0]) + (g - m[1]) * (g - m[1]) + (b - m[2]) * (b - m[2]); if (d < bd) { bd = d; best = j } }
                assign[i] = best
            }
            for (j in 0 until k) {
                var n = 0; var sr = 0f; var sg = 0f; var sb = 0f
                for ((i, c) in px.withIndex()) if (assign[i] == j) { n++; sr += Color.red(c); sg += Color.green(c); sb += Color.blue(c) }
                if (n > 0) cent[j] = floatArrayOf(sr / n, sg / n, sb / n)
            }
        }
        return (0 until k).map { j -> Color.rgb(cent[j][0].toInt(), cent[j][1].toInt(), cent[j][2].toInt()) to assign.count { it == j } / px.size.toFloat() }
            .filter { it.second > 0f }.sortedByDescending { it.second }
    }

    private fun hsl(c: Int) = FloatArray(3).also { androidx.core.graphics.ColorUtils.colorToHSL(c, it) }

    /** Moves a colour's lightness until it reads clearly on [bg] (contrast ≥ [target]). */
    private fun readable(c: Int, bg: Int, target: Double = 3.2): Int {
        val h = hsl(c)
        val darkBg = androidx.core.graphics.ColorUtils.calculateLuminance(bg) < 0.4
        var out = c
        var steps = 0
        while (androidx.core.graphics.ColorUtils.calculateContrast(out, bg or 0xFF000000.toInt()) < target && steps < 40) {
            h[2] = (h[2] + if (darkBg) 0.025f else -0.025f).coerceIn(0.03f, 0.97f)
            out = androidx.core.graphics.ColorUtils.HSLToColor(h); steps++
        }
        return out
    }

    /**
     * Two colours that suit the wallpaper: looks at what is actually behind the layer (its main colour)
     * and at the photo's palette, then picks two harmonious accents and adjusts them to stay readable there.
     */
    fun autoColors(photo: Bitmap, l: WallLayer, W: Int, H: Int, dim: Int = 0, d: WallDesign? = null): Pair<Int, Int> {
        val rc = rect(l, W, H)
        val mv = FloatArray(9).also { cropMatrix(photo.width, photo.height, W, H, d).getValues(it) }
        val sc = mv[Matrix.MSCALE_X]
        val ox = mv[Matrix.MTRANS_X]; val oy = mv[Matrix.MTRANS_Y]
        val x0 = ((rc.left - ox) / sc).toInt().coerceIn(0, photo.width - 2); val x1 = ((rc.right - ox) / sc).toInt().coerceIn(x0 + 1, photo.width)
        val y0 = ((rc.top - oy) / sc).toInt().coerceIn(0, photo.height - 2); val y1 = ((rc.bottom - oy) / sc).toInt().coerceIn(y0 + 1, photo.height)
        // what's right behind the layer
        val area = Bitmap.createScaledBitmap(Bitmap.createBitmap(photo, x0, y0, x1 - x0, y1 - y0), 40, 40, true)
        val ap = IntArray(1600); area.getPixels(ap, 0, 40, 0, 0, 40, 40)
        val area3 = clusters(ap, 3)
        val bg = area3.first().first.let { if (dim > 0) androidx.core.graphics.ColorUtils.blendARGB(it, Color.BLACK, dim / 100f) else it }
        // the hue to build on: the most colourful tone right behind the layer, else the photo's most vivid colour
        var src = area3.maxByOrNull { hsl(it.first)[1] * (0.5f + it.second) }!!.first
        if (hsl(src)[1] < 0.15f) {
            val whole = Bitmap.createScaledBitmap(photo, 64, 64, true)
            val wp = IntArray(4096); whole.getPixels(wp, 0, 64, 0, 0, 64, 64)
            clusters(wp, 6).maxByOrNull { hsl(it.first)[1] * (0.4f + it.second) }?.first?.let { if (hsl(it)[1] >= 0.15f) src = it }
        }
        val sh = hsl(src)
        // light or dark text: whichever stays readable against every big tone behind the layer, not just the first
        val big = area3.filter { it.second > 0.18f }.map { it.first.let { c -> if (dim > 0) androidx.core.graphics.ColorUtils.blendARGB(c, Color.BLACK, dim / 100f) else c } }
        fun worst(t: Int) = big.minOf { androidx.core.graphics.ColorUtils.calculateContrast(t, it or 0xFF000000.toInt()) }
        val dark = worst(Color.WHITE) >= worst(0xFF151515.toInt())
        val sat = if (sh[1] < 0.15f) 0.08f else sh[1]
        // tone-on-tone: a pale tint on top fading into a richer shade of the same hue, both readable on the spot
        val top = readable(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(sh[0], (sat * 0.7f).coerceIn(0.05f, 0.55f), if (dark) 0.9f else 0.14f)), bg, 3.6)
        val bottom = readable(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(sh[0], sat.coerceIn(0.08f, 0.85f).let { if (sat > 0.15f) maxOf(it, 0.6f) else it }, if (dark) 0.74f else 0.3f)), bg, 2.4)
        return top to bottom
    }

    private fun drawLayer(ctx: Context, c: Canvas, l: WallLayer, W: Int, H: Int, now: ZonedDateTime, under: Bitmap? = null) {
        val rc = rect(l, W, H)
        if (l.glass && under != null) glassPlate(under, rc)?.let { c.drawBitmap(it, null, glassRect(rc), Paint(Paint.FILTER_BITMAP_FLAG)) }
        val b = styled(ctx, l, rc.width().toInt(), now)
        val dst = RectF(rc.left, rc.centerY() - rc.width() / (b.width / b.height.toFloat()) / 2, rc.right, rc.centerY() + rc.width() / (b.width / b.height.toFloat()) / 2)
        if (l.shadow && !l.glassFill) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { maskFilter = BlurMaskFilter(rc.width() * 0.02f + 2f, BlurMaskFilter.Blur.NORMAL); alpha = 90 }
            val sh = b.extractAlpha()
            c.drawBitmap(sh, null, RectF(dst.left, dst.top + rc.width() * 0.006f, dst.right, dst.bottom + rc.width() * 0.006f), sp.apply { color = Color.BLACK })
        }
        val fin = if (l.glassFill && under != null) runCatching { glassify(b, under, dst) }.getOrDefault(b) else b
        c.drawBitmap(fin, null, dst, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = l.alpha })
    }

    /**
     * Full wallpaper at W×H. [photo] is the picked image, [mask] the subject mask (same size as the photo,
     * alpha = subject). Layers marked "behind" are drawn under the subject.
     */
    fun compose(ctx: Context, d: WallDesign, photo: Bitmap?, mask: Bitmap?, W: Int, H: Int, now: ZonedDateTime = ZonedDateTime.now(), reveal: Float = 1f): Bitmap {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.rgb(16, 20, 30))
        if (photo != null) fill(c, photo, d, W, H)
        val m = photo?.let { cropMatrix(it.width, it.height, W, H, d) }
        if (photo != null) {
            val bp = Paint(Paint.FILTER_BITMAP_FLAG)
            if (d.blur > 0) {
                val k = (1 + d.blur / 4).coerceAtMost(24)
                val small = Bitmap.createScaledBitmap(photo, (photo.width / k).coerceAtLeast(2), (photo.height / k).coerceAtLeast(2), true)
                drawPhoto(c, small, Matrix(m).apply { preScale(photo.width / small.width.toFloat(), photo.height / small.height.toFloat()) }, photoRect(photo, d, W, H), W, H, bp)
            } else drawPhoto(c, photo, m!!, photoRect(photo, d, W, H), W, H, bp)
            if (d.dim > 0) c.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0))
        }
        for (l in d.layers.filter { it.behind && mask != null }) drawLayer(ctx, c, l, W, H, now, out)
        if (photo != null && mask != null && d.layers.any { it.behind }) {
            // the subject, cut out with the mask, drawn over the "behind" layers
            val sub = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
            val sc = Canvas(sub)
            sc.drawBitmap(photo, m!!, Paint(Paint.FILTER_BITMAP_FLAG))
            if (d.dim > 0) sc.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
            val mp = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            sc.drawBitmap(mask, Matrix(m).apply { preScale(photo.width / mask.width.toFloat(), photo.height / mask.height.toFloat()) }, mp)
            c.drawBitmap(sub, 0f, 0f, Paint().apply { alpha = (255 * reveal).toInt() })
        }
        for (l in d.layers.filter { !it.behind || mask == null }) drawLayer(ctx, c, l, W, H, now, out)
        return out
    }
}
