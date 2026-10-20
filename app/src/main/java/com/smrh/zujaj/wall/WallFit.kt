package com.smrh.zujaj.wall

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint

/**
 * iPhone-style depth fitting: a clock marked [WallLayer.fit] keeps its top edge and grows down to
 * where the subject starts, then a chosen share of it ([WallLayer.hide]) goes behind the subject.
 * A date line marked [WallLayer.stick] rides just above the clock.
 */
object WallFit {
    private const val S = 4
    private var key: List<Any>? = null
    private var cache: IntArray? = null

    /** For every S-px column of the screen, the y (px) where the subject starts; H when there is none. */
    fun tops(d: WallDesign, photo: Bitmap, mask: Bitmap, W: Int, H: Int): IntArray {
        val k = listOf(System.identityHashCode(mask), System.identityHashCode(photo), d.zoom, d.px, d.py, W, H)
        if (k == key) cache?.let { return it }
        val gw = (W / S).coerceAtLeast(1); val gh = (H / S).coerceAtLeast(1)
        val b = Bitmap.createBitmap(gw, gh, Bitmap.Config.ARGB_8888)
        val m = Matrix(WallRender.cropMatrix(photo.width, photo.height, W, H, d)).apply {
            preScale(photo.width / mask.width.toFloat(), photo.height / mask.height.toFloat())
            postScale(1f / S, 1f / S)
        }
        Canvas(b).drawBitmap(mask, m, Paint(Paint.FILTER_BITMAP_FLAG))
        val px = IntArray(gw * gh); b.getPixels(px, 0, gw, 0, 0, gw, gh)
        val out = IntArray(gw) { x ->
            var y = 0
            while (y < gh && Color.alpha(px[y * gw + x]) < 110) y++
            if (y >= gh) H else y * S
        }
        key = k; cache = out
        return out
    }

    fun isClock(l: WallLayer) = l.kind.startsWith("clock_")

    fun apply(d: WallDesign, photo: Bitmap?, mask: Bitmap?, W: Int, H: Int): WallDesign {
        if (W <= 0 || H <= 0) return d
        if (d.layers.none { (it.fit && it.behind) || (it.kind == "date_line" && it.stick) }) return d
        val t = if (photo != null && mask != null) runCatching { tops(d, photo, mask, W, H) }.getOrNull() else null
        var layers = d.layers.map { l -> if (l.fit && l.behind && t != null) fit(l, t, W, H) else l }
        val clock = layers.firstOrNull { isClock(it) && it.fit } ?: layers.firstOrNull { isClock(it) }
        if (clock != null) {
            val rc = WallRender.rect(clock, W, H)
            layers = layers.map { l ->
                if (l.kind != "date_line" || !l.stick) l else {
                    val h = l.w * W / WallRender.aspect(l)
                    l.copy(cx = clock.cx, cy = ((rc.top - h * 0.75f) / H).coerceIn(0.02f, 0.98f))
                }
            }
        }
        return d.copy(layers = layers)
    }

    private fun fit(l: WallLayer, tops: IntArray, W: Int, H: Int): WallLayer {
        val rc = WallRender.rect(l, W, H)
        val top = rc.top
        val x0 = ((rc.left + rc.width() * 0.18f) / S).toInt().coerceIn(0, tops.size - 1)
        val x1 = ((rc.right - rc.width() * 0.18f) / S).toInt().coerceIn(x0, tops.size - 1)
        var subj = H
        for (x in x0..x1) if (tops[x] < subj) subj = tops[x]
        if (subj >= H) return l                 // nothing under the clock
        if (subj < top + H * 0.04f) return l    // the subject is already above the clock's top
        val hide = l.hide.coerceIn(0f, 0.7f)
        val h = ((subj - top) / (1f - hide)).coerceIn(H * 0.06f, H * 0.62f)
        return l.copy(ar = (l.w * W / h).coerceIn(0.25f, 10f), cy = ((top + h / 2f) / H).coerceIn(0.02f, 0.98f))
    }
}
