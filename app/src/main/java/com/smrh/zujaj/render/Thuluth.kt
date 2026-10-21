package com.smrh.zujaj.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The decorative Arabic heading style (default "calligraphy"): a flowing naskh/thuluth word with a
 * stretched kashida, framed by small calligraphic ornament marks (little hooked "v" strokes and rings)
 * sprinkled in the empty space around the letters — never touching them.
 */
object Thuluth {
    private const val TATWEEL = 'ـ'
    /** Letters that do NOT join to the following letter. */
    private const val NO_JOIN = "اأإآدذرزوؤةىءئ "

    /** Inserts a kashida near the end of the word ("الجمعة" → "الجمـــعة"), the classic calligraphic stretch. */
    fun stretch(s: String): String {
        if (s.length < 4 || ' ' in s || TATWEEL in s) return s
        for (i in (s.length - 3) downTo 1) {
            val c = s[i]
            if (c in 'ء'..'ي' && c !in NO_JOIN && s[i + 1] in 'ء'..'ي') return s.substring(0, i + 1) + "ـــ" + s.substring(i + 1)
        }
        return s
    }

    fun draw(r: RenderCtx, s0: String, cx: Float, cy: Float, size0: Float, color: Int, maxW: Float, maxH: Float) {
        val s = stretch(s0.trim())
        val p = r.paint(color, size0, F.AMIRI, Paint.Align.LEFT)
        val b = Rect()
        p.getTextBounds(s, 0, s.length, b)
        // room for the marks around the word
        val padK = 0.16f
        var k = 1f
        val needW = b.width() + p.textSize * padK * 2; val needH = b.height() + p.textSize * padK * 2
        if (maxW > 0 && needW > maxW) k = min(k, maxW / needW)
        if (maxH > 0 && needH > maxH) k = min(k, maxH / needH)
        if (k < 1f) { p.textSize *= k; p.getTextBounds(s, 0, s.length, b) }
        val x = cx - b.exactCenterX()
        val y = cy - b.exactCenterY()
        r.canvas.drawText(s, x, y, p)
        if (s.none { it in 'ء'..'ي' }) return
        marks(r, s, p, x, y, b, color)
    }

    /** Sprinkles ornament marks in a band around the ink (stable for the same word). */
    private fun marks(r: RenderCtx, s: String, p: Paint, x: Float, y: Float, b: Rect, color: Int) {
        val em = p.textSize
        val ms = em * 0.11f                      // mark size
        val margin = em * 0.2f
        val left = x + b.left - margin; val top = y + b.top - margin
        val wPx = b.width() + 2 * margin; val hPx = b.height() + 2 * margin
        // a small grid of the word's ink
        val cell = (maxOf(wPx, hPx) / 140f).coerceAtLeast(1f)
        val gw = (wPx / cell).toInt().coerceAtLeast(4); val gh = (hPx / cell).toInt().coerceAtLeast(4)
        val bmp = Bitmap.createBitmap(gw, gh, Bitmap.Config.ALPHA_8)
        Canvas(bmp).apply { scale(1f / cell, 1f / cell); translate(-left, -top); drawText(s, x, y, Paint(p).apply { this.color = android.graphics.Color.BLACK }) }
        val a = ByteArray(gw * gh); bmp.copyPixelsToBuffer(java.nio.ByteBuffer.wrap(a)); bmp.recycle()
        // distance (in cells) from every cell to the nearest ink — breadth-first from all ink cells
        val dist = IntArray(gw * gh) { Int.MAX_VALUE }
        val q = IntArray(gw * gh); var qh = 0; var qt = 0
        for (i in a.indices) if ((a[i].toInt() and 0xFF) > 60) { dist[i] = 0; q[qt++] = i }
        while (qh < qt) {
            val i = q[qh++]; val cx = i % gw; val cy = i / gw; val nd = dist[i] + 1
            if (cx > 0 && dist[i - 1] > nd) { dist[i - 1] = nd; q[qt++] = i - 1 }
            if (cx < gw - 1 && dist[i + 1] > nd) { dist[i + 1] = nd; q[qt++] = i + 1 }
            if (cy > 0 && dist[i - gw] > nd) { dist[i - gw] = nd; q[qt++] = i - gw }
            if (cy < gh - 1 && dist[i + gw] > nd) { dist[i + gw] = nd; q[qt++] = i + gw }
        }
        val near = (ms * 1.15f / cell).toInt(); val far = (ms * 3.2f / cell).toInt()
        val cand = ArrayList<Int>()
        for (i in dist.indices) if (dist[i] in near..far) cand.add(i)
        val rnd = java.util.Random(s.hashCode().toLong())
        java.util.Collections.shuffle(cand, rnd)
        val placed = ArrayList<Pair<Float, Float>>()
        val minGap = ms * 3.2f
        val maxMarks = (wPx / (ms * 3.6f)).toInt().coerceIn(5, 16)
        val pen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = ms * 0.2f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        val thin = Paint(pen).apply { strokeWidth = ms * 0.15f }
        for (i in cand) {
            if (placed.size >= maxMarks) break
            val px = left + (i % gw + 0.5f) * cell; val py = top + (i / gw + 0.5f) * cell
            // mostly above the word, a few below
            if (py > y + b.exactCenterY() && rnd.nextFloat() < 0.55f) continue
            if (placed.any { (ax, ay) -> (ax - px) * (ax - px) + (ay - py) * (ay - py) < minGap * minGap }) continue
            placed.add(px to py)
            val ang = (rnd.nextFloat() - 0.5f) * 0.7f
            fun rx(dx: Float, dy: Float) = px + dx * cos(ang) - dy * sin(ang)
            fun ry(dx: Float, dy: Float) = py + dx * sin(ang) + dy * cos(ang)
            if (rnd.nextFloat() < 0.8f) {
                // little "v" whose left arm ends in a hook
                val v = Path().apply {
                    moveTo(rx(-ms * 0.42f, -ms * 0.55f), ry(-ms * 0.42f, -ms * 0.55f))
                    lineTo(rx(0f, ms * 0.55f), ry(0f, ms * 0.55f))
                    lineTo(rx(ms * 0.42f, -ms * 0.65f), ry(ms * 0.42f, -ms * 0.65f))
                }
                r.canvas.drawPath(v, pen)
                val hook = Path().apply {
                    moveTo(rx(-ms * 0.42f, -ms * 0.55f), ry(-ms * 0.42f, -ms * 0.55f))
                    quadTo(rx(-ms * 0.3f, -ms * 0.95f), ry(-ms * 0.3f, -ms * 0.95f), rx(0f, -ms * 0.62f), ry(0f, -ms * 0.62f))
                }
                r.canvas.drawPath(hook, thin)
            } else {
                r.canvas.drawCircle(px, py, ms * 0.28f, thin)
            }
        }
    }
}
