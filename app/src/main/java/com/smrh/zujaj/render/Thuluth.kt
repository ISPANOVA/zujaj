package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Shader
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The default Arabic display style: the word in bold Reem Kufi, framed by a calm geometric
 * ornament — a small eight-pointed star on each side and, underneath, a fine rule that fades
 * out at both ends with a star at its centre and two small diamonds. Nothing touches the letters.
 */
object Thuluth {
    private val GOLD = 0xFFE7B75F.toInt()

    fun draw(r: RenderCtx, s0: String, cx: Float, cy: Float, size0: Float, color: Int, maxW: Float, maxH: Float) {
        val s = s0.trim()
        val p = r.paint(color, size0, F.KUFI_BOLD, Paint.Align.LEFT)
        val b = Rect()
        // room for the side stars (≈ 0.55 em each side) and the rule underneath (≈ 0.42 em)
        fun measure() { p.getTextBounds(s, 0, s.length, b) }
        measure()
        val fullW = { b.width() + p.textSize * 1.1f }
        val fullH = { b.height() + p.textSize * 0.42f }
        var k = 1f
        if (maxW > 0 && fullW() > maxW) k = min(k, maxW / fullW())
        if (maxH > 0 && fullH() > maxH) k = min(k, maxH / fullH())
        if (k < 1f) { p.textSize *= k; measure() }
        val size = p.textSize
        val rule = size * 0.42f
        // centre the whole composition (word + rule) on cy
        val wordCy = cy - rule / 2f
        val x = cx - b.exactCenterX()
        val y = wordCy - b.exactCenterY()
        r.canvas.drawText(s, x, y, p)

        val left = x + b.left
        val right = x + b.right
        val bottom = y + b.bottom
        val gold = withAlpha(GOLD, 235)
        val soft = withAlpha(color, 150)

        // side stars, level with the middle of the word
        val sr = size * 0.085f
        star(r, left - size * 0.3f, wordCy, sr, soft)
        star(r, right + size * 0.3f, wordCy, sr, soft)

        // the rule: fades in from both ends, broken in the middle by a star
        val ry = bottom + rule * 0.55f
        val half = (right - left) / 2f + size * 0.15f
        val mid = (left + right) / 2f
        val gap = size * 0.16f
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = (size * 0.018f).coerceAtLeast(1f); strokeCap = Paint.Cap.ROUND }
        lp.shader = LinearGradient(mid - half, ry, mid - gap, ry, Color.TRANSPARENT, gold, Shader.TileMode.CLAMP)
        r.canvas.drawLine(mid - half, ry, mid - gap, ry, lp)
        lp.shader = LinearGradient(mid + gap, ry, mid + half, ry, gold, Color.TRANSPARENT, Shader.TileMode.CLAMP)
        r.canvas.drawLine(mid + gap, ry, mid + half, ry, lp)
        val st = size * 0.13f
        star(r, mid, ry, st, gold)
        val dd = size * 0.042f
        for (sx in floatArrayOf(-1f, 1f)) diamond(r, mid + sx * half * 0.55f, ry, dd, gold)
    }

    private fun star(r: RenderCtx, cx: Float, cy: Float, rad: Float, color: Int) {
        val path = Path()
        for (i in 0 until 16) {
            val a = Math.PI / 8 * i - Math.PI / 2
            val rr = if (i % 2 == 0) rad else rad * 0.42f
            val px = (cx + rr * cos(a)).toFloat(); val py = (cy + rr * sin(a)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        r.canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun diamond(r: RenderCtx, cx: Float, cy: Float, d: Float, color: Int) {
        val path = Path().apply { moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close() }
        r.canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }
}
