package com.smrh.zujaj.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.max
import kotlin.math.min

/**
 * Thuluth-style lettering built on a bold naskh face:
 *  - a long, swelling kashida stretches the word at its most graceful joint
 *  - the free space around the word is filled with the small marks calligraphers use
 *    (curled "v" marks, hooks, a long slanted fatha and small muhmal letters below),
 *    drawn a shade lighter than the word.
 */
object Thuluth {
    private const val ZWJ = '‍'

    // letters that never connect to the following letter
    private const val NON_JOIN = "اأإآدذرزوؤةءى"
    private fun isLetter(c: Char) = c in 'ء'..'ي'

    /** Index i such that the stroke between s[i] and s[i+1] is stretched, or -1. */
    fun kashidaAt(s: String): Int {
        var best = -1
        var bestScore = -1
        for (i in 0 until s.length - 1) {
            val a = s[i]; val b = s[i + 1]
            if (!isLetter(a) || !isLetter(b) || a in NON_JOIN || b == 'ء') continue
            if (a == 'ل' && b in "اأإآ") continue // keep the lam-alef ligature
            if (i > 0 && s[i - 1] == 'ل' && a == 'ل') continue
            var score = i * 2 + 10
            if (b in "اأإآ") score -= 40 // stretching into an alef looks weak
            if (b in "ةهـ") score -= 6
            if (a in "سشصضطظ") score += 6 // these love a long tail
            if (a in "حجخعغ" ) score += 3
            if (score > bestScore) { bestScore = score; best = i }
        }
        return best
    }

    private class Layout(
        val p: Paint, val right: String, val left: String?, val wR: Float, val wL: Float,
        val kash: Float, val bounds: RectF, val joinTop: Float, val joinBottom: Float,
    )

    private fun layout(r: RenderCtx, s: String, size: Float, color: Int): Layout {
        val p = r.paint(color, size, F.AMIRI, Paint.Align.LEFT)
        val k = kashidaAt(s)
        val tb = Rect(); p.getTextBounds("ـ", 0, 1, tb)
        val right: String; val left: String?
        if (k >= 0) { right = s.substring(0, k + 1) + ZWJ; left = ZWJ + s.substring(k + 1) } else { right = s; left = null }
        val wR = p.measureText(right)
        val wL = left?.let { p.measureText(it) } ?: 0f
        val kash = if (left != null) size * (if (s.length <= 4) 1.05f else 0.8f) else 0f
        // bounds with the right part drawn at x = wL + kash (its left edge), left part at x = 0
        val bR = Rect(); p.getTextBounds(right, 0, right.length, bR)
        val bounds = RectF(bR.left + wL + kash, bR.top.toFloat(), bR.right + wL + kash, bR.bottom.toFloat())
        if (left != null) {
            val bL = Rect(); p.getTextBounds(left, 0, left.length, bL)
            bounds.union(RectF(bL.left.toFloat(), bL.top.toFloat(), bL.right.toFloat(), bL.bottom.toFloat()))
        }
        return Layout(p, right, left, wR, wL, kash, bounds, tb.top.toFloat(), tb.bottom.toFloat())
    }

    /** Draws the word (and kashida) with its origin (left part's left edge, baseline) at (x, y). */
    private fun drawWord(c: Canvas, l: Layout, x: Float, y: Float, p: Paint) {
        c.drawText(l.right, x + l.wL + l.kash, y, p)
        if (l.left != null) {
            c.drawText(l.left, x, y, p)
            // the swelling kashida: thin where it leaves the letters, fuller and slightly sagging in the middle
            val t = (l.joinBottom - l.joinTop)
            val x0 = x + l.wL + l.kash + t * 0.3f
            val x1 = x + l.wL - t * 0.3f
            val top = y + l.joinTop
            val mid = (x0 + x1) / 2
            val path = Path()
            path.moveTo(x0, top)
            path.cubicTo(x0 - (x0 - x1) * 0.3f, top + t * 0.55f, mid, top + t * 0.6f, x1, top + t * 0.05f)
            path.lineTo(x1, top + t * 1.05f)
            path.cubicTo(mid, top + t * 2.05f, x0 - (x0 - x1) * 0.25f, top + t * 1.9f, x0, top + t)
            path.close()
            val fp = Paint(p).apply { style = Paint.Style.FILL }
            c.drawPath(path, fp)
        }
    }

    fun draw(r: RenderCtx, s0: String, cx: Float, cy: Float, size0: Float, color: Int, maxW: Float, maxH: Float) {
        val s = s0.trim()
        var l = layout(r, s, size0, color)
        // fit (the marks need ~12% headroom above and below)
        var k = 1f
        if (maxW > 0 && l.bounds.width() > maxW) k = min(k, maxW / l.bounds.width())
        if (maxH > 0 && l.bounds.height() * 1.18f > maxH) k = min(k, maxH / (l.bounds.height() * 1.18f))
        if (k < 1f) l = layout(r, s, size0 * k, color)
        val size = l.p.textSize
        val x = cx - l.bounds.centerX()
        val y = cy - l.bounds.centerY()
        drawWord(r.canvas, l, x, y, l.p)
        ornaments(r, l, x, y, size, color, s)
    }

    // ------------------------------------------------------------------------------- marks
    private fun ornaments(r: RenderCtx, l: Layout, x: Float, y: Float, size: Float, color: Int, s: String) {
        val m = size * 0.15f
        val cell = max(m * 0.45f, 2f)
        val padX = m * 0.9f; val padY = m * 1.5f
        val area = RectF(x + l.bounds.left - padX, y + l.bounds.top - padY, x + l.bounds.right + padX, y + l.bounds.bottom + padY * 0.6f)
        val cols = (area.width() / cell).toInt() + 1
        val rows = (area.height() / cell).toInt() + 1
        if (cols < 4 || rows < 4 || cols * rows > 200_000) return
        // ink mask (letters fattened a little so marks keep a respectful distance)
        val mask = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        val mc = Canvas(mask)
        mc.scale(1f / cell, 1f / cell)
        mc.translate(-area.left, -area.top)
        val mp = Paint(l.p).apply { this.color = Color.BLACK; style = Paint.Style.FILL_AND_STROKE; strokeWidth = m * 0.45f }
        drawWord(mc, l, x, y, mp)
        val px = IntArray(cols * rows)
        mask.getPixels(px, 0, cols, 0, 0, cols, rows)
        mask.recycle()
        val ink = BooleanArray(cols * rows) { (px[it] ushr 24) > 24 }
        fun inkAt(cx: Int, cy: Int, rad: Int): Boolean {
            for (yy in cy - rad..cy + rad) for (xx in cx - rad..cx + rad) {
                if (xx !in 0 until cols || yy !in 0 until rows) continue
                if (ink[yy * cols + xx]) return true
            }
            return false
        }
        fun cx(i: Int) = area.left + (i + 0.5f) * cell
        fun cy(j: Int) = area.top + (j + 0.5f) * cell

        val gray = withAlpha(color, 170)
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = gray; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val c = r.canvas
        val placed = mutableListOf<Pair<Float, Float>>()
        val baseY = y
        val rnd = java.util.Random(s.hashCode().toLong() * 31 + 7)

        // 1) one long slanted fatha above the stretched joint (or the word's middle)
        run {
            val midX = if (l.left != null) x + l.wL + l.kash / 2 else x + l.bounds.centerX()
            val len = max(l.kash * 0.95f, size * 0.55f)
            var bestY = Float.NaN
            var yy = baseY - size * 0.32f
            while (yy > y + l.bounds.top - padY * 0.4f) {
                val x0 = midX + len / 2; val y0 = yy + len * 0.1f
                val x1 = midX - len / 2; val y1 = yy - len * 0.1f
                var free = true
                for (t in 0..12) {
                    val qx = x0 + (x1 - x0) * t / 12f; val qy = y0 + (y1 - y0) * t / 12f
                    val i = ((qx - area.left) / cell).toInt(); val j = ((qy - area.top) / cell).toInt()
                    if (inkAt(i, j, 1)) { free = false; break }
                }
                if (free) { bestY = yy; break }
                yy -= cell
            }
            if (!bestY.isNaN()) {
                val path = Path()
                val x0 = midX + len / 2; val x1 = midX - len / 2
                // a calligraphic fatha: thin at its ends, full in the middle
                path.moveTo(x0, bestY + len * 0.1f)
                path.quadTo(midX, bestY - m * 0.16f, x1, bestY - len * 0.1f)
                path.quadTo(midX, bestY + m * 0.2f, x0, bestY + len * 0.1f)
                path.close()
                c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = gray; style = Paint.Style.FILL })
                c.drawLine(x0, bestY + len * 0.1f, x1, bestY - len * 0.1f, Paint(pen).apply { strokeWidth = m * 0.12f })
                var t = 0f
                while (t <= 1f) { placed += (x0 + (x1 - x0) * t) to (bestY + len * 0.1f - len * 0.2f * t); t += 0.25f }
            }
        }

        // 2) scattered marks in the free space close to the letters
        val cand = ArrayList<Pair<Int, Int>>()
        for (j in 1 until rows - 1) for (i in 1 until cols - 1) {
            if (inkAt(i, j, 1)) continue
            if (!inkAt(i, j, (size * 0.3f / cell).toInt().coerceAtLeast(3))) continue
            cand += i to j
        }
        cand.shuffle(rnd)
        val want = (s.count { isLetter(it) } * 3f).toInt().coerceIn(8, 18)
        val minD = m * 1.3f
        var n = 0
        for ((i, j) in cand) {
            if (n >= want) break
            val mx = cx(i); val my = cy(j)
            if (placed.any { (ax, ay) -> (ax - mx) * (ax - mx) + (ay - my) * (ay - my) < minD * minD }) continue
            // keep the whole mark off the letters
            if (inkAt(i, j, (m * 0.7f / cell).toInt().coerceAtLeast(1))) continue
            placed += mx to my
            n++
            val below = my > baseY + size * 0.02f
            c.save()
            c.rotate(-8f + rnd.nextFloat() * 8f, mx, my)
            when {
                below && n % 2 == 0 -> smallLetter(c, if (n % 4 == 0) "س" else "ح", mx, my, m, gray, l.p)
                n % 5 == 2 -> hook(c, mx, my, m, pen)
                else -> vee(c, mx, my, m, pen)
            }
            c.restore()
        }
    }

    /** Curled "v": a hairline left arm and a fuller right arm, both ending in little curls. */
    private fun vee(c: Canvas, x: Float, y: Float, m: Float, pen: Paint) {
        val h = m * 0.8f
        val left = Path().apply {
            moveTo(x - m * 0.34f, y - h * 0.62f)
            quadTo(x - m * 0.46f, y - h * 0.95f, x - m * 0.28f, y - h * 0.9f)
            moveTo(x - m * 0.34f, y - h * 0.62f)
            quadTo(x - m * 0.2f, y, x - m * 0.02f, y + h * 0.55f)
        }
        c.drawPath(left, Paint(pen).apply { strokeWidth = m * 0.1f })
        val right = Path().apply {
            moveTo(x - m * 0.02f, y + h * 0.55f)
            quadTo(x + m * 0.18f, y - h * 0.2f, x + m * 0.34f, y - h * 0.82f)
            quadTo(x + m * 0.42f, y - h * 1.02f, x + m * 0.52f, y - h * 0.78f)
        }
        c.drawPath(right, Paint(pen).apply { strokeWidth = m * 0.19f })
    }

    /** A small hooked stroke, like a tiny raised comma. */
    private fun hook(c: Canvas, x: Float, y: Float, m: Float, pen: Paint) {
        val path = Path().apply {
            moveTo(x + m * 0.05f, y + m * 0.42f)
            quadTo(x - m * 0.08f, y - m * 0.05f, x + m * 0.02f, y - m * 0.36f)
            quadTo(x + m * 0.1f, y - m * 0.55f, x + m * 0.3f, y - m * 0.5f)
        }
        c.drawPath(path, Paint(pen).apply { strokeWidth = m * 0.17f })
    }

    /** A small letter written under the line (the muhmal sign). */
    private fun smallLetter(c: Canvas, ch: String, x: Float, y: Float, m: Float, color: Int, base: Paint) {
        val p = Paint(base).apply { this.color = color; textSize = m * 1.25f; textAlign = Paint.Align.CENTER; style = Paint.Style.FILL }
        val b = Rect(); p.getTextBounds(ch, 0, ch.length, b)
        c.drawText(ch, x, y - b.exactCenterY(), p)
    }
}
