package com.smrh.zujaj.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.smrh.zujaj.data.Cond
import kotlin.math.cos
import kotlin.math.sin

/** Simple hand-drawn vector glyphs (no third party artwork). Each fits a box of [s] centred on (cx, cy). */
object Glyphs {
    private fun fill(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c; style = Paint.Style.FILL }
    private fun stroke(c: Int, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = c; style = Paint.Style.STROKE; strokeWidth = w; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    // ---------------------------------------------------------------- weather
    fun weather(c: Canvas, cond: Cond, night: Boolean, cx: Float, cy: Float, s: Float, color: Boolean = true) {
        val sunC = if (color) 0xFFFFC53D.toInt() else Color.WHITE
        val cloudC = Color.WHITE
        when (cond) {
            Cond.CLEAR -> if (night) moon(c, cx, cy, s * 0.8f, 0xFFF3F0E0.toInt()) else sun(c, cx, cy, s * 0.8f, sunC)
            Cond.PARTLY -> {
                if (night) moon(c, cx + s * 0.16f, cy - s * 0.14f, s * 0.55f, 0xFFF3F0E0.toInt())
                else sun(c, cx + s * 0.16f, cy - s * 0.14f, s * 0.55f, sunC, rays = false)
                cloud(c, cx - s * 0.06f, cy + s * 0.1f, s * 0.8f, cloudC)
            }
            Cond.CLOUDY -> {
                cloud(c, cx + s * 0.14f, cy - s * 0.1f, s * 0.6f, 0xFFB8BCC6.toInt())
                cloud(c, cx - s * 0.05f, cy + s * 0.08f, s * 0.82f, cloudC)
            }
            Cond.RAIN -> {
                cloud(c, cx, cy - s * 0.1f, s * 0.85f, cloudC)
                val p = stroke(if (color) 0xFF5AB0FF.toInt() else Color.WHITE, s * 0.06f)
                for (i in -1..1) c.drawLine(cx + i * s * 0.2f, cy + s * 0.22f, cx + i * s * 0.2f - s * 0.06f, cy + s * 0.38f, p)
            }
            Cond.SNOW -> {
                cloud(c, cx, cy - s * 0.1f, s * 0.85f, cloudC)
                val p = fill(Color.WHITE)
                for (i in -1..1) c.drawCircle(cx + i * s * 0.2f, cy + s * 0.32f, s * 0.045f, p)
            }
            Cond.STORM -> {
                cloud(c, cx, cy - s * 0.12f, s * 0.85f, 0xFFB8BCC6.toInt())
                val path = Path().apply {
                    moveTo(cx + s * 0.02f, cy + s * 0.08f); lineTo(cx - s * 0.12f, cy + s * 0.28f)
                    lineTo(cx, cy + s * 0.28f); lineTo(cx - s * 0.08f, cy + s * 0.46f)
                    lineTo(cx + s * 0.14f, cy + s * 0.22f); lineTo(cx + s * 0.02f, cy + s * 0.22f); close()
                }
                c.drawPath(path, fill(if (color) 0xFFFFC53D.toInt() else Color.WHITE))
            }
            Cond.FOG -> {
                val p = stroke(Color.WHITE, s * 0.07f)
                for (i in 0..2) c.drawLine(cx - s * 0.36f + i * s * 0.05f, cy - s * 0.18f + i * s * 0.18f, cx + s * 0.36f - i * s * 0.03f, cy - s * 0.18f + i * s * 0.18f, p)
            }
        }
    }

    fun sun(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, rays: Boolean = true) {
        c.drawCircle(cx, cy, s * 0.26f, fill(color))
        if (rays) {
            val p = stroke(color, s * 0.07f)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0)
                c.drawLine(cx + (cos(a) * s * 0.38f).toFloat(), cy + (sin(a) * s * 0.38f).toFloat(), cx + (cos(a) * s * 0.48f).toFloat(), cy + (sin(a) * s * 0.48f).toFloat(), p)
            }
        }
    }

    fun moon(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val path = Path()
        path.addCircle(cx, cy, s * 0.36f, Path.Direction.CW)
        val cut = Path().apply { addCircle(cx + s * 0.18f, cy - s * 0.12f, s * 0.3f, Path.Direction.CW) }
        path.op(cut, Path.Op.DIFFERENCE)
        c.drawPath(path, fill(color))
    }

    fun cloud(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = fill(color)
        val path = Path()
        path.addCircle(cx - s * 0.18f, cy + s * 0.04f, s * 0.2f, Path.Direction.CW)
        path.addCircle(cx + s * 0.04f, cy - s * 0.08f, s * 0.27f, Path.Direction.CW)
        path.addCircle(cx + s * 0.26f, cy + s * 0.06f, s * 0.18f, Path.Direction.CW)
        path.addRoundRect(RectF(cx - s * 0.38f, cy + s * 0.0f, cx + s * 0.44f, cy + s * 0.24f), s * 0.12f, s * 0.12f, Path.Direction.CW)
        c.drawPath(path, p)
    }

    // ---------------------------------------------------------------- media
    fun play(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val path = Path().apply {
            moveTo(cx - s * 0.28f, cy - s * 0.36f); lineTo(cx + s * 0.38f, cy); lineTo(cx - s * 0.28f, cy + s * 0.36f); close()
        }
        val p = fill(color).apply { pathEffect = android.graphics.CornerPathEffect(s * 0.08f) }
        c.drawPath(path, p)
    }

    fun pause(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val r = s * 0.06f
        c.drawRoundRect(RectF(cx - s * 0.28f, cy - s * 0.34f, cx - s * 0.08f, cy + s * 0.34f), r, r, fill(color))
        c.drawRoundRect(RectF(cx + s * 0.08f, cy - s * 0.34f, cx + s * 0.28f, cy + s * 0.34f), r, r, fill(color))
    }

    fun playPause(c: Canvas, playing: Boolean, cx: Float, cy: Float, s: Float, color: Int) =
        if (playing) pause(c, cx, cy, s, color) else play(c, cx, cy, s, color)

    fun skip(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, next: Boolean) {
        val d = if (next) 1 else -1
        val path = Path().apply {
            moveTo(cx - d * s * 0.3f, cy - s * 0.3f); lineTo(cx + d * s * 0.18f, cy); lineTo(cx - d * s * 0.3f, cy + s * 0.3f); close()
        }
        c.drawPath(path, fill(color).apply { pathEffect = android.graphics.CornerPathEffect(s * 0.06f) })
        val bx = cx + d * s * 0.26f
        c.drawRoundRect(RectF(bx - s * 0.05f, cy - s * 0.3f, bx + s * 0.05f, cy + s * 0.3f), s * 0.04f, s * 0.04f, fill(color))
    }

    fun note(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = fill(color)
        c.drawOval(RectF(cx - s * 0.36f, cy + s * 0.12f, cx - s * 0.08f, cy + s * 0.36f), p)
        c.drawOval(RectF(cx + s * 0.06f, cy + s * 0.04f, cx + s * 0.34f, cy + s * 0.28f), p)
        val st = stroke(color, s * 0.07f)
        c.drawLine(cx - s * 0.1f, cy + s * 0.24f, cx - s * 0.1f, cy - s * 0.3f, st)
        c.drawLine(cx + s * 0.32f, cy + s * 0.16f, cx + s * 0.32f, cy - s * 0.38f, st)
        c.drawLine(cx - s * 0.1f, cy - s * 0.3f, cx + s * 0.32f, cy - s * 0.38f, stroke(color, s * 0.12f))
    }

    fun shuffle(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.08f)
        val a = Path().apply { moveTo(cx - s * 0.36f, cy - s * 0.22f); cubicTo(cx, cy - s * 0.22f, cx, cy + s * 0.22f, cx + s * 0.3f, cy + s * 0.22f) }
        val b = Path().apply { moveTo(cx - s * 0.36f, cy + s * 0.22f); cubicTo(cx, cy + s * 0.22f, cx, cy - s * 0.22f, cx + s * 0.3f, cy - s * 0.22f) }
        c.drawPath(a, p); c.drawPath(b, p)
        arrowHead(c, cx + s * 0.36f, cy + s * 0.22f, s, color)
        arrowHead(c, cx + s * 0.36f, cy - s * 0.22f, s, color)
    }

    private fun arrowHead(c: Canvas, x: Float, y: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.08f)
        c.drawLine(x, y, x - s * 0.12f, y - s * 0.1f, p)
        c.drawLine(x, y, x - s * 0.12f, y + s * 0.1f, p)
    }

    fun repeat(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.08f)
        c.drawLine(cx - s * 0.34f, cy + s * 0.06f, cx - s * 0.34f, cy - s * 0.18f, p)
        c.drawLine(cx - s * 0.34f, cy - s * 0.18f, cx + s * 0.32f, cy - s * 0.18f, p)
        arrowHead(c, cx + s * 0.36f, cy - s * 0.18f, s, color)
        c.drawLine(cx + s * 0.34f, cy - s * 0.06f, cx + s * 0.34f, cy + s * 0.18f, p)
        c.drawLine(cx + s * 0.34f, cy + s * 0.18f, cx - s * 0.32f, cy + s * 0.18f, p)
        c.save(); c.rotate(180f, cx - s * 0.34f, cy + s * 0.18f); arrowHead(c, cx - s * 0.30f, cy + s * 0.18f, s, color); c.restore()
    }

    fun volume(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, waves: Int) {
        val path = Path().apply {
            moveTo(cx - s * 0.36f, cy - s * 0.12f); lineTo(cx - s * 0.2f, cy - s * 0.12f); lineTo(cx, cy - s * 0.32f)
            lineTo(cx, cy + s * 0.32f); lineTo(cx - s * 0.2f, cy + s * 0.12f); lineTo(cx - s * 0.36f, cy + s * 0.12f); close()
        }
        c.drawPath(path, fill(color))
        val p = stroke(color, s * 0.07f)
        for (i in 1..waves) {
            val r = s * (0.12f + i * 0.12f)
            c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -45f, 90f, false, p)
        }
    }

    fun headphones(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.08f)
        c.drawArc(RectF(cx - s * 0.34f, cy - s * 0.36f, cx + s * 0.34f, cy + s * 0.3f), 180f, 180f, false, p)
        c.drawRoundRect(RectF(cx - s * 0.38f, cy, cx - s * 0.18f, cy + s * 0.34f), s * 0.06f, s * 0.06f, fill(color))
        c.drawRoundRect(RectF(cx + s * 0.18f, cy, cx + s * 0.38f, cy + s * 0.34f), s * 0.06f, s * 0.06f, fill(color))
    }

    // ---------------------------------------------------------------- misc
    fun search(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.09f)
        c.drawCircle(cx - s * 0.06f, cy - s * 0.06f, s * 0.24f, p)
        c.drawLine(cx + s * 0.12f, cy + s * 0.12f, cx + s * 0.32f, cy + s * 0.32f, p)
    }

    fun mic(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        c.drawRoundRect(RectF(cx - s * 0.12f, cy - s * 0.38f, cx + s * 0.12f, cy + s * 0.08f), s * 0.12f, s * 0.12f, fill(color))
        val p = stroke(color, s * 0.07f)
        c.drawArc(RectF(cx - s * 0.24f, cy - s * 0.2f, cx + s * 0.24f, cy + s * 0.2f), 0f, 180f, false, p)
        c.drawLine(cx, cy + s * 0.2f, cx, cy + s * 0.36f, p)
    }

    fun lens(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.08f)
        val r = s * 0.34f
        val path = Path()
        path.moveTo(cx - r, cy - r * 0.2f); path.lineTo(cx - r, cy - r + s * 0.1f)
        path.quadTo(cx - r, cy - r, cx - r + s * 0.1f, cy - r); path.lineTo(cx - r * 0.2f, cy - r)
        c.drawPath(path, p)
        c.save(); c.rotate(90f, cx, cy); c.drawPath(path, p); c.rotate(90f, cx, cy); c.drawPath(path, p); c.restore()
        c.drawCircle(cx, cy, s * 0.12f, fill(color))
        c.drawCircle(cx + r * 0.62f, cy + r * 0.62f, s * 0.06f, fill(color))
    }

    fun sparkle(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val r = s * 0.4f
        val path = Path().apply {
            moveTo(cx, cy - r)
            quadTo(cx + r * 0.12f, cy - r * 0.12f, cx + r, cy)
            quadTo(cx + r * 0.12f, cy + r * 0.12f, cx, cy + r)
            quadTo(cx - r * 0.12f, cy + r * 0.12f, cx - r, cy)
            quadTo(cx - r * 0.12f, cy - r * 0.12f, cx, cy - r)
            close()
        }
        c.drawPath(path, fill(color))
    }

    fun translate(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; textSize = s * 0.5f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        c.drawText("A", cx + s * 0.16f, cy + s * 0.32f, p)
        p.textSize = s * 0.42f
        c.drawText("文", cx - s * 0.16f, cy + s * 0.02f, p)
    }

    fun pin(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val path = Path()
        path.addCircle(cx, cy - s * 0.1f, s * 0.26f, Path.Direction.CW)
        val tip = Path().apply { moveTo(cx - s * 0.22f, cy - s * 0.0f); lineTo(cx, cy + s * 0.4f); lineTo(cx + s * 0.22f, cy); close() }
        path.op(tip, Path.Op.UNION)
        path.op(Path().apply { addCircle(cx, cy - s * 0.1f, s * 0.1f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
        c.drawPath(path, fill(color))
    }

    fun video(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        c.drawRoundRect(RectF(cx - s * 0.4f, cy - s * 0.28f, cx + s * 0.4f, cy + s * 0.28f), s * 0.12f, s * 0.12f, fill(color))
        play(c, cx + s * 0.02f, cy, s * 0.36f, 0xFF1A1A1F.toInt())
    }

    fun incognito(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val f = fill(color)
        c.drawRoundRect(RectF(cx - s * 0.4f, cy - s * 0.06f, cx + s * 0.4f, cy + s * 0.02f), s * 0.04f, s * 0.04f, f)
        val hat = Path().apply { moveTo(cx - s * 0.26f, cy - s * 0.06f); lineTo(cx - s * 0.16f, cy - s * 0.36f); lineTo(cx + s * 0.16f, cy - s * 0.36f); lineTo(cx + s * 0.26f, cy - s * 0.06f); close() }
        c.drawPath(hat, f)
        val p = stroke(color, s * 0.06f)
        c.drawCircle(cx - s * 0.18f, cy + s * 0.2f, s * 0.11f, p)
        c.drawCircle(cx + s * 0.18f, cy + s * 0.2f, s * 0.11f, p)
    }

    fun battery(c: Canvas, cx: Float, cy: Float, s: Float, color: Int, level: Float = 0.6f) {
        val p = stroke(color, s * 0.07f)
        val r = RectF(cx - s * 0.36f, cy - s * 0.2f, cx + s * 0.28f, cy + s * 0.2f)
        c.drawRoundRect(r, s * 0.06f, s * 0.06f, p)
        c.drawRoundRect(RectF(cx + s * 0.3f, cy - s * 0.08f, cx + s * 0.38f, cy + s * 0.08f), s * 0.03f, s * 0.03f, fill(color))
        c.drawRoundRect(RectF(r.left + s * 0.07f, r.top + s * 0.07f, r.left + s * 0.07f + (r.width() - s * 0.14f) * level, r.bottom - s * 0.07f), s * 0.02f, s * 0.02f, fill(color))
    }

    fun storage(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.07f)
        c.drawRoundRect(RectF(cx - s * 0.3f, cy - s * 0.32f, cx + s * 0.3f, cy + s * 0.32f), s * 0.06f, s * 0.06f, p)
        c.drawLine(cx - s * 0.3f, cy + s * 0.1f, cx + s * 0.3f, cy + s * 0.1f, p)
        c.drawCircle(cx + s * 0.16f, cy + s * 0.21f, s * 0.035f, fill(color))
    }

    fun chip(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.07f)
        c.drawRoundRect(RectF(cx - s * 0.24f, cy - s * 0.24f, cx + s * 0.24f, cy + s * 0.24f), s * 0.05f, s * 0.05f, p)
        c.drawRect(RectF(cx - s * 0.09f, cy - s * 0.09f, cx + s * 0.09f, cy + s * 0.09f), fill(color))
        for (i in -1..1) {
            val o = i * s * 0.13f
            c.drawLine(cx + o, cy - s * 0.24f, cx + o, cy - s * 0.36f, p); c.drawLine(cx + o, cy + s * 0.24f, cx + o, cy + s * 0.36f, p)
            c.drawLine(cx - s * 0.24f, cy + o, cx - s * 0.36f, cy + o, p); c.drawLine(cx + s * 0.24f, cy + o, cx + s * 0.36f, cy + o, p)
        }
    }

    fun plus(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val p = stroke(color, s * 0.09f)
        c.drawLine(cx - s * 0.26f, cy, cx + s * 0.26f, cy, p)
        c.drawLine(cx, cy - s * 0.26f, cx, cy + s * 0.26f, p)
    }

    fun minus(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        c.drawLine(cx - s * 0.26f, cy, cx + s * 0.26f, cy, stroke(color, s * 0.09f))
    }

    fun share(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val a = floatArrayOf(cx + s * 0.24f, cy - s * 0.28f)
        val b = floatArrayOf(cx - s * 0.24f, cy)
        val d = floatArrayOf(cx + s * 0.24f, cy + s * 0.28f)
        val p = stroke(color, s * 0.07f)
        c.drawLine(a[0], a[1], b[0], b[1], p); c.drawLine(b[0], b[1], d[0], d[1], p)
        val f = fill(color)
        c.drawCircle(a[0], a[1], s * 0.11f, f); c.drawCircle(b[0], b[1], s * 0.11f, f); c.drawCircle(d[0], d[1], s * 0.11f, f)
    }

    fun playCircle(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        c.drawCircle(cx, cy, s * 0.42f, stroke(color, s * 0.07f))
        play(c, cx + s * 0.04f, cy, s * 0.42f, color)
    }

    fun phone(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val path = Path().apply {
            moveTo(cx - s * 0.3f, cy - s * 0.32f)
            quadTo(cx - s * 0.38f, cy + s * 0.1f, cx + s * 0.28f, cy + s * 0.34f)
            lineTo(cx + s * 0.36f, cy + s * 0.16f); lineTo(cx + s * 0.16f, cy + s * 0.04f)
            lineTo(cx + s * 0.06f, cy + s * 0.14f); quadTo(cx - s * 0.12f, cy + s * 0.04f, cx - s * 0.16f, cy - s * 0.08f)
            lineTo(cx - s * 0.06f, cy - s * 0.18f); lineTo(cx - s * 0.16f, cy - s * 0.38f); close()
        }
        c.drawPath(path, fill(color))
    }

    fun chat(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val r = RectF(cx - s * 0.34f, cy - s * 0.28f, cx + s * 0.34f, cy + s * 0.18f)
        c.drawRoundRect(r, s * 0.12f, s * 0.12f, fill(color))
        val t = Path().apply { moveTo(cx - s * 0.16f, cy + s * 0.14f); lineTo(cx - s * 0.22f, cy + s * 0.36f); lineTo(cx + s * 0.04f, cy + s * 0.16f); close() }
        c.drawPath(t, fill(color))
    }

    fun person(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        c.drawCircle(cx, cy - s * 0.14f, s * 0.17f, fill(color))
        c.drawArc(RectF(cx - s * 0.32f, cy + s * 0.08f, cx + s * 0.32f, cy + s * 0.6f), 180f, 180f, true, fill(color))
    }

    fun crown(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val path = Path().apply {
            moveTo(cx - s * 0.4f, cy + s * 0.24f); lineTo(cx - s * 0.44f, cy - s * 0.22f); lineTo(cx - s * 0.18f, cy + s * 0.0f)
            lineTo(cx, cy - s * 0.32f); lineTo(cx + s * 0.18f, cy); lineTo(cx + s * 0.44f, cy - s * 0.22f); lineTo(cx + s * 0.4f, cy + s * 0.24f); close()
        }
        c.drawPath(path, fill(color).apply { pathEffect = android.graphics.CornerPathEffect(s * 0.05f) })
    }

    fun beads(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val f = fill(color)
        for (i in 0 until 11) {
            val a = Math.toRadians(-90.0 + i * 30.0)
            c.drawCircle(cx + (cos(a) * s * 0.32f).toFloat(), cy + (sin(a) * s * 0.32f).toFloat(), s * 0.06f, f)
        }
        c.drawLine(cx, cy + s * 0.32f, cx, cy + s * 0.48f, stroke(color, s * 0.05f))
    }

    fun kaaba(c: Canvas, cx: Float, cy: Float, s: Float, color: Int) {
        val r = RectF(cx - s * 0.3f, cy - s * 0.26f, cx + s * 0.3f, cy + s * 0.3f)
        c.drawRoundRect(r, s * 0.03f, s * 0.03f, fill(color))
        c.drawRect(RectF(r.left, cy - s * 0.12f, r.right, cy - s * 0.04f), fill(0xFFE5B65A.toInt()))
    }
}
