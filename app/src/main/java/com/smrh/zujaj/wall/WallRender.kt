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

    fun aspect(l: WallLayer): Float = if (l.kind == "widget") Catalog.byId(l.styleId)?.shape?.let { it.dw / it.dh } ?: 2f
        else kinds.firstOrNull { it.first == l.kind }?.second ?: 3f

    fun loadImage(path: String, maxSide: Int = 2400): Bitmap? = runCatching {
        if (path.isBlank() || !File(path).exists()) return null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var s = 1
        while (max(o.outWidth, o.outHeight) / (s * 2) >= maxSide) s *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = s })
    }.getOrNull()

    /** Center-crop matrix of an image into W×H. */
    fun cropMatrix(iw: Int, ih: Int, W: Int, H: Int): Matrix {
        val sc = max(W / iw.toFloat(), H / ih.toFloat())
        return Matrix().apply { setScale(sc, sc); postTranslate((W - iw * sc) / 2f, (H - ih * sc) / 2f) }
    }

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
                return runCatching { Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, true, d, RenderData(now)).bitmap }.getOrElse { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
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
        val m = cropMatrix(photo.width, photo.height, W, H)
        val bp = Paint(Paint.FILTER_BITMAP_FLAG)
        if (d.blur > 0) {
            val k = (1 + d.blur / 4).coerceAtMost(24)
            val small = Bitmap.createScaledBitmap(photo, (photo.width / k).coerceAtLeast(2), (photo.height / k).coerceAtLeast(2), true)
            c.drawBitmap(small, Matrix(m).apply { preScale(photo.width / small.width.toFloat(), photo.height / small.height.toFloat()) }, bp)
        } else c.drawBitmap(photo, m, bp)
        if (d.dim > 0) c.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0))
        return out
    }

    /** Only the subject (sharp), transparent elsewhere. */
    fun subject(d: WallDesign, photo: Bitmap, mask: Bitmap, W: Int, H: Int): Bitmap {
        val m = cropMatrix(photo.width, photo.height, W, H)
        val sub = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val sc = Canvas(sub)
        sc.drawBitmap(photo, m, Paint(Paint.FILTER_BITMAP_FLAG))
        if (d.dim > 0) sc.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        val mp = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
        sc.drawBitmap(mask, Matrix(m).apply { preScale(photo.width / mask.width.toFloat(), photo.height / mask.height.toFloat()) }, mp)
        return sub
    }

    /** A soft glowing outline of the subject (for the "found it" animation). */
    fun outline(photo: Bitmap, mask: Bitmap, W: Int, H: Int, color: Int): Bitmap {
        val m = cropMatrix(photo.width, photo.height, W, H)
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

    /** Two colours that suit the photo behind a layer: a light one and a vivid one. */
    fun autoColors(photo: Bitmap, l: WallLayer, W: Int, H: Int): Pair<Int, Int> {
        val rc = rect(l, W, H)
        val sc = maxOf(W / photo.width.toFloat(), H / photo.height.toFloat())
        val ox = (W - photo.width * sc) / 2; val oy = (H - photo.height * sc) / 2
        val x0 = ((rc.left - ox) / sc).toInt().coerceIn(0, photo.width - 2); val x1 = ((rc.right - ox) / sc).toInt().coerceIn(x0 + 1, photo.width)
        val y0 = ((rc.top - oy) / sc).toInt().coerceIn(0, photo.height - 2); val y1 = ((rc.bottom - oy) / sc).toInt().coerceIn(y0 + 1, photo.height)
        val crop = Bitmap.createBitmap(photo, x0, y0, x1 - x0, y1 - y0)
        val small = Bitmap.createScaledBitmap(crop, 24, 24, true)
        val whole = Bitmap.createScaledBitmap(photo, 32, 32, true)
        var best = Color.WHITE; var bestSat = -1f
        val hsv = FloatArray(3)
        for (y in 0 until 32) for (x in 0 until 32) {
            Color.colorToHSV(whole.getPixel(x, y), hsv)
            val score = hsv[1] * (0.4f + hsv[2])
            if (score > bestSat) { bestSat = score; best = whole.getPixel(x, y) }
        }
        var sum = FloatArray(3)
        for (y in 0 until 24) for (x in 0 until 24) { Color.colorToHSV(small.getPixel(x, y), hsv); sum[0] += hsv[0]; sum[1] += hsv[1]; sum[2] += hsv[2] }
        sum = sum.map { it / 576f }.toFloatArray()
        val darkBg = sum[2] < 0.55f
        // light tint of the area + vivid accent from the photo, kept readable on the area
        val light = Color.HSVToColor(floatArrayOf(sum[0], (sum[1] * 0.35f).coerceAtMost(0.3f), if (darkBg) 0.98f else 0.18f))
        Color.colorToHSV(best, hsv)
        val vivid = Color.HSVToColor(floatArrayOf(hsv[0], hsv[1].coerceIn(0.45f, 0.9f), if (darkBg) 0.95f else 0.35f))
        return light to vivid
    }

    private fun drawLayer(ctx: Context, c: Canvas, l: WallLayer, W: Int, H: Int, now: ZonedDateTime) {
        val rc = rect(l, W, H)
        val b = styled(ctx, l, rc.width().toInt(), now)
        val dst = RectF(rc.left, rc.centerY() - rc.width() / (b.width / b.height.toFloat()) / 2, rc.right, rc.centerY() + rc.width() / (b.width / b.height.toFloat()) / 2)
        if (l.shadow) {
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { maskFilter = BlurMaskFilter(rc.width() * 0.02f + 2f, BlurMaskFilter.Blur.NORMAL); alpha = 90 }
            val sh = b.extractAlpha()
            c.drawBitmap(sh, null, RectF(dst.left, dst.top + rc.width() * 0.006f, dst.right, dst.bottom + rc.width() * 0.006f), sp.apply { color = Color.BLACK })
        }
        c.drawBitmap(b, null, dst, Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = l.alpha })
    }

    /**
     * Full wallpaper at W×H. [photo] is the picked image, [mask] the subject mask (same size as the photo,
     * alpha = subject). Layers marked "behind" are drawn under the subject.
     */
    fun compose(ctx: Context, d: WallDesign, photo: Bitmap?, mask: Bitmap?, W: Int, H: Int, now: ZonedDateTime = ZonedDateTime.now(), reveal: Float = 1f): Bitmap {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(Color.rgb(16, 20, 30))
        val m = photo?.let { cropMatrix(it.width, it.height, W, H) }
        if (photo != null) {
            val bp = Paint(Paint.FILTER_BITMAP_FLAG)
            if (d.blur > 0) {
                val k = (1 + d.blur / 4).coerceAtMost(24)
                val small = Bitmap.createScaledBitmap(photo, (photo.width / k).coerceAtLeast(2), (photo.height / k).coerceAtLeast(2), true)
                c.drawBitmap(small, Matrix(m).apply { preScale(photo.width / small.width.toFloat(), photo.height / small.height.toFloat()) }, bp)
            } else c.drawBitmap(photo, m!!, bp)
            if (d.dim > 0) c.drawColor(Color.argb(d.dim * 255 / 100, 0, 0, 0))
        }
        for (l in d.layers.filter { it.behind && mask != null }) drawLayer(ctx, c, l, W, H, now)
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
        for (l in d.layers.filter { !it.behind || mask == null }) drawLayer(ctx, c, l, W, H, now)
        return out
    }
}
