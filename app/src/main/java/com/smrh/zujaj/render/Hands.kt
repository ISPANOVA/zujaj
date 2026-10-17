package com.smrh.zujaj.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Draws analog clock hands. Shared by previews and by the AnalogClock hand bitmaps. */
object Hands {
    enum class Which { HOUR, MINUTE, SECOND, CENTER }

    fun drawAt(c: Canvas, r: RectF, s: HandStyle, hour: Int, minute: Int, second: Int) {
        val cx = r.centerX(); val cy = r.centerY()
        val hAng = (hour % 12 + minute / 60f) * 30f
        val mAng = (minute + second / 60f) * 6f
        val sAng = second * 6f
        val sh = r.width() * 0.012f
        val shadow = s.copy(hour = 0x55000000, minute = 0x55000000, center = 0x55000000)
        c.save(); c.translate(sh, sh * 1.6f); c.rotate(hAng, cx, cy); drawHand(c, r, shadow, Which.HOUR); c.restore()
        c.save(); c.translate(sh, sh * 1.6f); c.rotate(mAng, cx, cy); drawHand(c, r, shadow, Which.MINUTE); c.restore()
        c.save(); c.rotate(hAng, cx, cy); drawHand(c, r, s, Which.HOUR); c.restore()
        c.save(); c.rotate(mAng, cx, cy); drawHand(c, r, s, Which.MINUTE); c.restore()
        if (s.seconds) { c.save(); c.rotate(sAng, cx, cy); drawHand(c, r, s, Which.SECOND); c.restore() }
        drawHand(c, r, s, Which.CENTER)
    }

    /** Draws one hand pointing to 12 o'clock inside [r]. */
    fun drawHand(c: Canvas, r: RectF, s: HandStyle, which: Which) {
        val cx = r.centerX(); val cy = r.centerY()
        val rad = r.width() / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
        val w = rad * s.width
        when (which) {
            Which.HOUR, Which.MINUTE -> {
                val len = rad * (if (which == Which.HOUR) s.hourLen else s.minLen)
                p.color = if (which == Which.HOUR) s.hour else s.minute
                when (s.kind) {
                    4 -> { // "lume": thin neck from the centre, then a thick rounded blade
                        val neck = len * 0.26f
                        p.strokeWidth = w * 0.7f
                        c.drawLine(cx, cy, cx, cy - neck, p)
                        p.strokeWidth = w * (if (which == Which.HOUR) 2.1f else 1.7f)
                        c.drawLine(cx, cy - neck, cx, cy - len, p)
                    }
                    1 -> { // thick rounded (pixel-like)
                        p.strokeWidth = w * (if (which == Which.HOUR) 2.6f else 2.2f)
                        c.drawLine(cx, cy, cx, cy - len, p)
                    }
                    2 -> { // tapered
                        val path = Path()
                        val bw = w * (if (which == Which.HOUR) 1.5f else 1.1f)
                        path.moveTo(cx - bw, cy + rad * 0.08f)
                        path.lineTo(cx - bw * 0.35f, cy - len)
                        path.lineTo(cx + bw * 0.35f, cy - len)
                        path.lineTo(cx + bw, cy + rad * 0.08f)
                        path.close()
                        p.style = Paint.Style.FILL
                        c.drawPath(path, p)
                    }
                    3 -> { // thin line with hollow tip
                        p.strokeWidth = w * 0.8f
                        c.drawLine(cx, cy, cx, cy - len, p)
                    }
                    else -> { // classic: thin tail + thicker body
                        p.strokeWidth = w * (if (which == Which.HOUR) 1.35f else 1.0f)
                        c.drawLine(cx, cy + rad * 0.06f, cx, cy - len, p)
                    }
                }
            }
            Which.SECOND -> {
                p.color = s.second
                p.strokeWidth = w * 0.42f
                c.drawLine(cx, cy + rad * 0.18f, cx, cy - rad * 0.86f, p)
                p.style = Paint.Style.FILL
                c.drawCircle(cx, cy, w * 0.9f, p)
            }
            Which.CENTER -> {
                p.style = Paint.Style.FILL
                p.color = s.center
                c.drawCircle(cx, cy, w * (if (s.kind == 1) 1.0f else 1.2f), p)
                p.color = 0xFF111111.toInt()
                c.drawCircle(cx, cy, w * 0.45f, p)
            }
        }
    }

    /** Bitmap of a single hand pointing at 12, size x size, for AnalogClock (API 31+). */
    fun handBitmap(size: Int, s: HandStyle, which: Which): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = RectF(0f, 0f, size.toFloat(), size.toFloat())
        if (which == Which.HOUR || which == Which.MINUTE) {
            val sh = size * 0.012f
            c.save(); c.translate(0f, sh * 1.6f)
            drawHand(c, r, s.copy(hour = 0x55000000, minute = 0x55000000), which)
            c.restore()
        }
        drawHand(c, r, s, which)
        if (which == Which.MINUTE || (which == Which.SECOND && s.seconds)) drawHand(c, r, s, Which.CENTER)
        return b
    }
}
