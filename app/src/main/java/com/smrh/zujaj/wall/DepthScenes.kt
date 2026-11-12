package com.smrh.zujaj.wall

import android.graphics.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 200 ready-made depth wallpapers, drawn in code: 20 scenes × 10 colour moods.
 * Each one is painted as two layers — the sky/backdrop and the foreground shape — so the clock can
 * sit exactly between them (no guessing a cut-out). Every one opens in the studio fully editable.
 */
object DepthScenes {
    class Mood(val en: String, val ar: String, val skyTop: Int, val skyBot: Int, val glow: Int, val dark: Int, val lit: Int, val rim: Int, val clock1: Int, val clock2: Int, val date: Int)
    class Scene(val en: String, val ar: String, val top: Float, val kind: String = "clock_tall", val width: Float = 0.84f)
    class Item(val id: Int, val scene: Int, val mood: Int) {
        fun name(ar: Boolean) = if (ar) "${scenes[scene].ar} · ${moods[mood].ar}" else "${scenes[scene].en} · ${moods[mood].en}"
    }

    private fun c(v: Long) = v.toInt()
    val moods = listOf(
        Mood("Aqua", "أكوا", c(0xFF020608), c(0xFF06222A), c(0xFF19E3E0), c(0xFF0A1420), c(0xFF1FA6B8), c(0xFFFF4F8B), c(0xFF19D3CF), c(0xFF06353A), c(0xFFFF4F8B)),
        Mood("Violet", "بنفسجي", c(0xFF05030C), c(0xFF1A0E3A), c(0xFFB18CFF), c(0xFF120A2A), c(0xFF6E48D8), c(0xFFE5D9FF), c(0xFFE0D2FF), c(0xFF7A4FF0), c(0xFFC9B6FF)),
        Mood("Sunset", "غروب", c(0xFF14060A), c(0xFF7A2418), c(0xFFFFB347), c(0xFF1B0B10), c(0xFFB8452B), c(0xFFFFD08A), c(0xFFFFE3B8), c(0xFFFF5A2E), c(0xFFFFC27A)),
        Mood("Emerald", "زمردي", c(0xFF020A06), c(0xFF0B3324), c(0xFF8DF5B5), c(0xFF06140D), c(0xFF1F8A5B), c(0xFFC9FFD9), c(0xFFB9F7A5), c(0xFF19C48A), c(0xFF9EF0C2)),
        Mood("Crimson", "قرمزي", c(0xFF070102), c(0xFF3A0610), c(0xFFFF3B4E), c(0xFF0E0205), c(0xFF8E1424), c(0xFFFFB3BB), c(0xFFFFFFFF), c(0xFFE0182E), c(0xFFFF6B78)),
        Mood("Gold", "دهبي", c(0xFF050403), c(0xFF2A1E0A), c(0xFFF5C451), c(0xFF0E0A04), c(0xFF8A6A22), c(0xFFFFE6A3), c(0xFFFFE08A), c(0xFFC88A12), c(0xFFF5C451)),
        Mood("Ice", "تلجي", c(0xFF060A12), c(0xFF1C3352), c(0xFFBFE3FF), c(0xFF0C1626), c(0xFF5B8FC4), c(0xFFFFFFFF), c(0xFFFFFFFF), c(0xFF7FB6EA), c(0xFFCFE6FF)),
        Mood("Neon", "نيون", c(0xFF04020A), c(0xFF180630), c(0xFFFF2BD6), c(0xFF07031A), c(0xFF3B1FA8), c(0xFF29F0FF), c(0xFF29F0FF), c(0xFFFF2BD6), c(0xFFFF7AE8)),
        Mood("Silver", "فضي", c(0xFF050505), c(0xFF26282C), c(0xFFE8E8E8), c(0xFF0B0B0C), c(0xFF5A5D63), c(0xFFFFFFFF), c(0xFFF2F2F2), c(0xFF6B6F76), c(0xFFDADADA)),
        Mood("Dawn", "فجر", c(0xFF0C0716), c(0xFF4B2A5A), c(0xFFFFB0C8), c(0xFF140C22), c(0xFFA0507A), c(0xFFFFE0EA), c(0xFFFFE4EE), c(0xFFFF6FA0), c(0xFFFFB0C8)),
    )
    val scenes = listOf(
        Scene("Peak", "قمة", 0.30f), Scene("Twin peaks", "قمتين", 0.33f), Scene("Planet", "كوكب", 0.56f, "clock_thin", 0.62f),
        Scene("Dunes", "كثبان", 0.44f), Scene("Pyramids", "أهرامات", 0.36f), Scene("Mosque", "مسجد", 0.34f, "clock_stack", 0.5f),
        Scene("Skyline", "مدينة", 0.34f), Scene("Palms", "نخيل", 0.34f, "clock_thin", 0.62f), Scene("Lighthouse", "فنار", 0.30f, "clock_stack", 0.5f),
        Scene("Sail", "مركب", 0.42f), Scene("Pines", "صنوبر", 0.36f), Scene("Crystal", "كريستال", 0.32f, "clock_thin", 0.62f),
        Scene("Orb", "كرة", 0.36f), Scene("Arch", "قوس", 0.30f, "clock_stack", 0.48f), Scene("Iceberg", "جبل تلج", 0.38f),
        Scene("Volcano", "بركان", 0.36f), Scene("Canyon", "وادي", 0.36f, "clock_stack", 0.5f), Scene("Lantern", "فانوس", 0.36f, "clock_thin", 0.62f),
        Scene("Lone tree", "شجرة", 0.30f), Scene("Wave", "موجة", 0.38f),
    )
    /** each scene's 10 moods, in a pleasing order (first ones first in the grid) */
    val all: List<Item> = (0 until 10).flatMap { m -> scenes.indices.map { s -> Item(m * scenes.size + s, s, (s * 3 + m) % moods.size) } }

    // ---------------------------------------------------------------------------------------------

    class Painted(val full: Bitmap, val fg: Bitmap)

    fun paint(it: Item, w: Int, h: Int): Painted {
        val bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val fg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val m = moods[it.mood]
        val P = Pen(Canvas(bg), Canvas(fg), w.toFloat(), h.toFloat(), m, Random(it.id * 7919 + 13))
        P.sky()
        when (it.scene) {
            0 -> P.peak(single = true)
            1 -> P.peak(single = false)
            2 -> P.planet()
            3 -> P.dunes()
            4 -> P.pyramids()
            5 -> P.mosque()
            6 -> P.skyline()
            7 -> P.palms()
            8 -> P.lighthouse()
            9 -> P.sail()
            10 -> P.pines()
            11 -> P.crystal()
            12 -> P.orb()
            13 -> P.arch()
            14 -> P.iceberg()
            15 -> P.volcano()
            16 -> P.canyon()
            17 -> P.lantern()
            18 -> P.tree()
            else -> P.wave()
        }
        val full = bg.copy(Bitmap.Config.ARGB_8888, true)
        Canvas(full).drawBitmap(fg, 0f, 0f, null)
        bg.recycle()
        return Painted(full, fg)
    }

    /** The studio layers for a ready-made wallpaper: big clock tucked behind the shape, date line above it. */
    fun design(it: Item, image: String, mask: String, W: Int, H: Int): WallDesign {
        val s = scenes[it.scene]; val m = moods[it.mood]
        val asp = WallRender.naturalAspect(WallLayer(kind = s.kind))
        val hFrac = s.width * (W / H.toFloat()) / asp
        val cy = s.top + 0.02f * hFrac  // a good half of the clock hides behind the shape
        val fills = intArrayOf(4, 2, 3, 0, 5, 1)
        val n = System.nanoTime()
        val clock = WallLayer(id = n, kind = s.kind, cx = 0.5f, cy = cy, w = s.width, behind = true,
            color = m.clock1, color2 = m.clock2, fill = fills[it.id % fills.size], font = (it.id / 7) % WallRender.CLOCK_FONTS.size.coerceAtMost(9),
            shadow = false)
        val date = WallLayer(id = n + 1, kind = "date_line", cx = 0.5f, cy = (cy - hFrac / 2 - 0.035f).coerceAtLeast(0.06f), w = 0.46f, color = m.date, stick = true, shadow = false)
        return WallDesign(image = image, mask = mask, layers = listOf(clock, date), depthId = it.id, parallax = 1, anim = 1)
    }

    // ---------------------------------------------------------------------------------------------

    private class Pen(val b: Canvas, val f: Canvas, val W: Float, val H: Float, val m: Mood, val r: Random) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun a(col: Int, al: Int) = (col and 0x00FFFFFF) or (al.coerceIn(0, 255) shl 24)
        fun mix(x: Int, y: Int, t: Float): Int {
            fun ch(s: Int) = (((x shr s) and 255) * (1 - t) + ((y shr s) and 255) * t).toInt().coerceIn(0, 255)
            return Color.argb(255, ch(16), ch(8), ch(0))
        }
        fun lin(x0: Float, y0: Float, x1: Float, y1: Float, vararg cols: Int) = LinearGradient(x0, y0, x1, y1, cols, null, Shader.TileMode.CLAMP)

        fun glow(cv: Canvas, x: Float, y: Float, rad: Float, col: Int) {
            p.shader = RadialGradient(x, y, rad, col, a(col, 0), Shader.TileMode.CLAMP); cv.drawCircle(x, y, rad, p); p.shader = null
        }

        fun sky() {
            p.shader = lin(0f, 0f, 0f, H, m.skyTop, m.skyTop, m.skyBot); b.drawRect(0f, 0f, W, H, p); p.shader = null
            // soft beams of the mood colour from below, like a stage light
            glow(b, W * 0.5f, H * 0.62f, W * 0.95f, a(m.glow, 60))
            glow(b, W * 0.12f, H * 0.78f, W * 0.6f, a(m.glow, 40))
            glow(b, W * 0.9f, H * 0.7f, W * 0.55f, a(m.rim, 26))
            repeat(160) {
                val y = r.nextFloat() * H * 0.6f
                p.color = a(Color.WHITE, (40 + r.nextInt(170)) * (1f - y / (H * 0.6f)).let { k -> k.toDouble() }.toInt().coerceAtLeast(20))
                b.drawCircle(r.nextFloat() * W, y, 0.6f + r.nextFloat() * 1.6f * W / 1080f, p)
            }
        }

        /** fills a foreground path: dark body, lit side, rim light along the top and a soft bottom fade */
        fun body(path: Path, lightX: Float = W * 0.35f, rim: Boolean = true) {
            p.shader = lin(0f, H * 0.3f, 0f, H, m.lit, m.dark, m.dark); f.drawPath(path, p)
            p.shader = RadialGradient(lightX, H * 0.35f, W * 0.7f, a(m.glow, 120), a(m.glow, 0), Shader.TileMode.CLAMP)
            f.save(); f.clipPath(path); f.drawRect(0f, 0f, W, H, p); f.restore(); p.shader = null
            if (rim) {
                p.style = Paint.Style.STROKE; p.strokeWidth = W * 0.004f; p.color = a(m.rim, 190)
                p.maskFilter = BlurMaskFilter(W * 0.006f, BlurMaskFilter.Blur.NORMAL)
                f.save(); f.clipRect(0f, 0f, W, H * 0.8f); f.drawPath(path, p); f.restore()
                p.maskFilter = null; p.style = Paint.Style.FILL
            }
            // fade into black at the very bottom so the dock area stays calm
            p.shader = lin(0f, H * 0.82f, 0f, H, a(Color.BLACK, 0), a(Color.BLACK, 230))
            f.save(); f.clipPath(path); f.drawRect(0f, H * 0.8f, W, H, p); f.restore(); p.shader = null
        }

        fun ridge(top: Float, peaks: List<Pair<Float, Float>>): Path {
            val path = Path(); path.moveTo(0f, H)
            path.lineTo(0f, H * (top + 0.25f))
            var px = 0f
            for ((x, y) in peaks) {
                // jagged climb towards each summit
                val steps = 7
                val sx = px; val sy = path.let { H * (top + 0.25f) }
                for (i in 1..steps) {
                    val t = i / steps.toFloat()
                    val jx = sx + (x * W - sx) * t
                    val jy = sy + (y * H - sy) * t + (if (i < steps) (r.nextFloat() - 0.5f) * H * 0.025f else 0f)
                    path.lineTo(jx, jy)
                }
                px = x * W
            }
            path.lineTo(W, H * (top + 0.27f)); path.lineTo(W, H); path.close()
            return path
        }

        fun peak(single: Boolean) {
            // a hazy far range first, then the hero summit(s)
            val far = Path().apply {
                moveTo(0f, H); lineTo(0f, H * 0.6f)
                var x = 0f
                while (x < W) { x += W * (0.08f + r.nextFloat() * 0.1f); lineTo(x, H * (0.5f + r.nextFloat() * 0.1f)) }
                lineTo(W, H); close()
            }
            p.color = a(mix(m.skyBot, m.lit, 0.35f), 200); f.drawPath(far, p)
            val main = if (single) Path().apply {
                moveTo(-W * 0.1f, H); lineTo(-W * 0.05f, H * 0.62f); lineTo(W * 0.18f, H * 0.5f); lineTo(W * 0.3f, H * 0.44f)
                lineTo(W * 0.42f, H * 0.36f); lineTo(W * 0.47f, H * 0.32f); lineTo(W * 0.5f, H * 0.3f); lineTo(W * 0.55f, H * 0.34f)
                lineTo(W * 0.64f, H * 0.4f); lineTo(W * 0.72f, H * 0.39f); lineTo(W * 0.86f, H * 0.5f); lineTo(W * 1.1f, H * 0.58f); lineTo(W * 1.1f, H); close()
            } else Path().apply {
                moveTo(-W * 0.1f, H); lineTo(0f, H * 0.5f); lineTo(W * 0.2f, H * 0.4f); lineTo(W * 0.3f, H * 0.33f); lineTo(W * 0.42f, H * 0.42f)
                lineTo(W * 0.52f, H * 0.38f); lineTo(W * 0.66f, H * 0.33f); lineTo(W * 0.74f, H * 0.37f); lineTo(W * 0.9f, H * 0.46f); lineTo(W * 1.1f, H * 0.52f); lineTo(W * 1.1f, H); close()
            }
            body(main, W * 0.32f)
            // facets: lighter streaks down the lit faces
            f.save(); f.clipPath(main)
            repeat(14) {
                val x0 = W * (0.15f + r.nextFloat() * 0.7f); val y0 = H * (0.32f + r.nextFloat() * 0.18f)
                p.shader = lin(x0, y0, x0 - W * 0.12f, y0 + H * 0.3f, a(m.rim, 90), a(m.rim, 0))
                val fp = Path().apply { moveTo(x0, y0); lineTo(x0 - W * (0.02f + r.nextFloat() * 0.05f), y0 + H * 0.32f); lineTo(x0 - W * 0.12f, y0 + H * 0.32f); close() }
                f.drawPath(fp, p)
            }
            p.shader = null; f.restore()
        }

        fun planet() {
            val cx = W * 0.5f; val cy = H * 1.0f; val rad = W * 0.95f
            glow(f, cx, cy, rad * 1.12f, a(m.glow, 110))
            val path = Path().apply { addCircle(cx, cy, rad, Path.Direction.CW) }
            p.shader = RadialGradient(cx - rad * 0.3f, cy - rad * 0.6f, rad * 1.4f, intArrayOf(m.lit, m.dark, Color.BLACK), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            f.drawPath(path, p); p.shader = null
            f.save(); f.clipPath(path)
            repeat(9) { i ->
                p.color = a(if (i % 2 == 0) m.rim else m.glow, 22); p.style = Paint.Style.STROKE; p.strokeWidth = H * (0.006f + r.nextFloat() * 0.01f)
                f.drawCircle(cx + W * 0.2f, cy + H * 0.2f, rad * (0.5f + i * 0.07f), p)
            }
            p.style = Paint.Style.FILL; f.restore()
            p.style = Paint.Style.STROKE; p.strokeWidth = W * 0.006f; p.color = a(m.rim, 220); p.maskFilter = BlurMaskFilter(W * 0.008f, BlurMaskFilter.Blur.NORMAL)
            f.drawArc(RectF(cx - rad, cy - rad, cx + rad, cy + rad), 200f, 140f, false, p)
            p.maskFilter = null; p.style = Paint.Style.FILL
            // a small moon off to the side
            glow(b, W * 0.82f, H * 0.18f, W * 0.12f, a(m.glow, 90)); p.color = a(m.rim, 230); b.drawCircle(W * 0.82f, H * 0.18f, W * 0.035f, p)
        }

        fun dunes() {
            glow(b, W * 0.5f, H * 0.42f, W * 0.34f, a(m.glow, 160)); p.color = a(m.rim, 240); b.drawCircle(W * 0.5f, H * 0.42f, W * 0.13f, p)
            for (k in 0..2) {
                val y0 = H * (0.42f + k * 0.12f)
                val path = Path().apply {
                    moveTo(0f, H); lineTo(0f, y0 + H * 0.05f)
                    cubicTo(W * 0.3f, y0 - H * 0.04f, W * 0.55f, y0 + H * 0.08f, W, y0 - H * 0.01f * k); lineTo(W, H); close()
                }
                p.color = mix(m.dark, m.lit, 0.45f - k * 0.18f); f.drawPath(path, p)
                p.shader = lin(0f, y0 - H * 0.05f, 0f, y0 + H * 0.2f, a(m.glow, 90), a(m.glow, 0)); f.drawPath(path, p); p.shader = null
            }
        }

        fun pyramids() {
            glow(b, W * 0.62f, H * 0.3f, W * 0.3f, a(m.glow, 140)); p.color = a(m.rim, 235); b.drawCircle(W * 0.62f, H * 0.3f, W * 0.08f, p)
            fun pyr(cx: Float, top: Float, half: Float) {
                val base = H * 0.72f
                val left = Path().apply { moveTo(cx, top); lineTo(cx - half, base); lineTo(cx + half * 0.15f, base); close() }
                val right = Path().apply { moveTo(cx, top); lineTo(cx + half * 0.15f, base); lineTo(cx + half, base); close() }
                p.shader = lin(0f, top, 0f, base, m.lit, mix(m.lit, m.dark, 0.5f)); f.drawPath(left, p)
                p.shader = lin(0f, top, 0f, base, mix(m.dark, m.lit, 0.25f), m.dark); f.drawPath(right, p); p.shader = null
            }
            pyr(W * 0.18f, H * 0.5f, W * 0.22f); pyr(W * 0.82f, H * 0.46f, W * 0.26f); pyr(W * 0.5f, H * 0.36f, W * 0.4f)
            p.shader = lin(0f, H * 0.7f, 0f, H, mix(m.dark, m.lit, 0.3f), Color.BLACK); f.drawRect(0f, H * 0.715f, W, H, p); p.shader = null
        }

        fun mosque() {
            glow(b, W * 0.5f, H * 0.36f, W * 0.55f, a(m.glow, 110))
            p.color = a(m.rim, 230); b.drawCircle(W * 0.78f, H * 0.16f, W * 0.05f, p); p.color = m.skyTop; b.drawCircle(W * 0.8f, H * 0.15f, W * 0.045f, p)
            val path = Path()
            val base = H * 0.66f
            path.addRect(W * 0.18f, H * 0.5f, W * 0.82f, H, Path.Direction.CW)
            // main dome with finial
            path.addOval(RectF(W * 0.28f, H * 0.36f, W * 0.72f, H * 0.56f), Path.Direction.CW)
            path.addRect(W * 0.495f, H * 0.32f, W * 0.505f, H * 0.37f, Path.Direction.CW)
            path.addCircle(W * 0.5f, H * 0.33f, W * 0.012f, Path.Direction.CW)
            // side domes
            path.addOval(RectF(W * 0.16f, H * 0.47f, W * 0.34f, H * 0.55f), Path.Direction.CW)
            path.addOval(RectF(W * 0.66f, H * 0.47f, W * 0.84f, H * 0.55f), Path.Direction.CW)
            // minarets
            for (x in listOf(W * 0.08f, W * 0.92f)) {
                path.addRect(x - W * 0.025f, H * 0.36f, x + W * 0.025f, H, Path.Direction.CW)
                path.addRect(x - W * 0.04f, H * 0.42f, x + W * 0.04f, H * 0.43f, Path.Direction.CW)
                path.moveTo(x - W * 0.03f, H * 0.36f); path.lineTo(x, H * 0.3f); path.lineTo(x + W * 0.03f, H * 0.36f); path.close()
            }
            path.addRect(0f, base + H * 0.06f, W, H, Path.Direction.CW)
            body(path, W * 0.5f)
            // glowing windows
            p.color = a(m.glow, 200)
            for (i in 0..4) { val x = W * (0.26f + i * 0.12f); f.drawRoundRect(RectF(x - W * 0.018f, H * 0.58f, x + W * 0.018f, H * 0.64f), W * 0.02f, W * 0.02f, p) }
        }

        fun skyline() {
            val path = Path()
            var x = -W * 0.02f
            while (x < W) {
                val bw = W * (0.07f + r.nextFloat() * 0.1f)
                val center = 1f - abs((x + bw / 2) / W - 0.5f) * 1.4f
                val top = H * (0.62f - center * 0.26f - r.nextFloat() * 0.06f)
                path.addRect(x, top, x + bw, H, Path.Direction.CW)
                if (r.nextFloat() < 0.35f) path.addRect(x + bw * 0.45f, top - H * 0.04f, x + bw * 0.55f, top, Path.Direction.CW)
                x += bw * (0.92f + r.nextFloat() * 0.1f)
            }
            body(path, W * 0.5f, rim = true)
            f.save(); f.clipPath(path)
            repeat(420) {
                p.color = a(if (r.nextBoolean()) m.glow else m.rim, 60 + r.nextInt(150))
                f.drawRect(RectF(r.nextFloat() * W, H * (0.36f + r.nextFloat() * 0.5f), 0f, 0f).apply { right = left + W * 0.008f; bottom = top + H * 0.005f }, p)
            }
            f.restore()
        }

        fun palms() {
            glow(b, W * 0.5f, H * 0.55f, W * 0.4f, a(m.glow, 170)); p.color = a(m.rim, 230); b.drawCircle(W * 0.5f, H * 0.56f, W * 0.18f, p)
            p.color = a(m.skyBot, 255)
            for (i in 0..6) b.drawRect(0f, H * (0.56f + i * 0.022f), W, H * (0.56f + i * 0.022f) + H * 0.006f, p)
            val path = Path()
            fun palm(bx: Float, lean: Float, top: Float) {
                val tx = bx + lean * W; val ty = top * H
                val trunk = Path().apply {
                    moveTo(bx - W * 0.018f, H); quadTo(bx + lean * W * 0.3f, H * 0.8f, tx - W * 0.008f, ty)
                    lineTo(tx + W * 0.008f, ty); quadTo(bx + lean * W * 0.3f + W * 0.02f, H * 0.8f, bx + W * 0.018f, H); close()
                }
                path.addPath(trunk)
                for (k in 0 until 8) {
                    val ang = (-170f + k * 23f) * PI.toFloat() / 180f
                    val len = W * (0.2f + r.nextFloat() * 0.06f)
                    val ex = tx + cos(ang) * len; val ey = ty + sin(ang) * len * 0.5f + len * 0.25f
                    val leaf = Path().apply {
                        moveTo(tx, ty); quadTo((tx + ex) / 2, ty - len * 0.25f, ex, ey)
                        quadTo((tx + ex) / 2, ty - len * 0.05f, tx, ty + W * 0.01f); close()
                    }
                    path.addPath(leaf)
                }
            }
            palm(W * 0.2f, 0.06f, 0.36f); palm(W * 0.84f, -0.08f, 0.42f); palm(W * 0.68f, 0.05f, 0.56f)
            path.addRect(0f, H * 0.82f, W, H, Path.Direction.CW)
            p.color = m.dark; f.drawPath(path, p)
            p.shader = lin(0f, H * 0.8f, 0f, H, a(Color.BLACK, 0), a(Color.BLACK, 220)); f.drawRect(0f, H * 0.8f, W, H, p); p.shader = null
        }

        fun lighthouse() {
            // beam
            p.shader = lin(W * 0.55f, H * 0.33f, -W * 0.2f, H * 0.2f, a(m.glow, 140), a(m.glow, 0))
            b.drawPath(Path().apply { moveTo(W * 0.55f, H * 0.33f); lineTo(-W * 0.2f, H * 0.12f); lineTo(-W * 0.2f, H * 0.36f); close() }, p); p.shader = null
            val path = Path()
            path.moveTo(W * 0.48f, H * 0.72f); path.lineTo(W * 0.51f, H * 0.36f); path.lineTo(W * 0.59f, H * 0.36f); path.lineTo(W * 0.62f, H * 0.72f); path.close()
            path.addRect(W * 0.5f, H * 0.31f, W * 0.6f, H * 0.36f, Path.Direction.CW)
            path.moveTo(W * 0.49f, H * 0.31f); path.lineTo(W * 0.55f, H * 0.28f); path.lineTo(W * 0.61f, H * 0.31f); path.close()
            // rocks
            path.moveTo(0f, H); path.lineTo(0f, H * 0.78f); path.lineTo(W * 0.2f, H * 0.74f); path.lineTo(W * 0.4f, H * 0.7f); path.lineTo(W * 0.7f, H * 0.69f)
            path.lineTo(W * 0.85f, H * 0.74f); path.lineTo(W, H * 0.76f); path.lineTo(W, H); path.close()
            body(path, W * 0.5f)
            glow(f, W * 0.55f, H * 0.335f, W * 0.08f, a(m.glow, 240))
            p.color = a(m.rim, 70)
            for (i in 0..2) f.drawRect(W * 0.505f + i * W * 0.005f, H * (0.45f + i * 0.09f), W * 0.6f - i * W * 0.005f, H * (0.47f + i * 0.09f), p)
        }

        fun sail() {
            glow(b, W * 0.5f, H * 0.5f, W * 0.5f, a(m.glow, 120))
            val path = Path()
            // boat: hull + two sails
            path.moveTo(W * 0.3f, H * 0.6f); path.lineTo(W * 0.72f, H * 0.6f); path.lineTo(W * 0.64f, H * 0.64f); path.lineTo(W * 0.36f, H * 0.64f); path.close()
            path.moveTo(W * 0.5f, H * 0.42f); path.lineTo(W * 0.5f, H * 0.59f); path.lineTo(W * 0.33f, H * 0.59f); path.close()
            path.moveTo(W * 0.52f, H * 0.45f); path.lineTo(W * 0.66f, H * 0.59f); path.lineTo(W * 0.52f, H * 0.59f); path.close()
            // waves
            for (k in 0..3) {
                val y0 = H * (0.62f + k * 0.06f)
                path.moveTo(0f, H); path.lineTo(0f, y0)
                var x = 0f
                while (x <= W) { path.quadTo(x + W * 0.06f, y0 - H * 0.02f, x + W * 0.12f, y0); x += W * 0.12f }
                path.lineTo(W, H); path.close()
            }
            body(path, W * 0.5f)
        }

        fun pines() {
            val path = Path()
            fun tree(cx: Float, top: Float, hgt: Float) {
                val wd = hgt * 0.42f
                for (k in 0..5) {
                    val ty = top + hgt * k * 0.13f; val bw = wd * (0.3f + k * 0.15f)
                    path.moveTo(cx, ty); path.lineTo(cx - bw, ty + hgt * 0.22f); path.lineTo(cx + bw, ty + hgt * 0.22f); path.close()
                }
                path.addRect(cx - wd * 0.04f, top + hgt * 0.8f, cx + wd * 0.04f, H, Path.Direction.CW)
            }
            for (i in 0..9) {
                val cx = W * (i / 9f) + (r.nextFloat() - 0.5f) * W * 0.05f
                val hgt = H * (0.32f + (1f - abs(i / 9f - 0.5f) * 2f) * 0.18f + r.nextFloat() * 0.04f)
                tree(cx, H * 0.86f - hgt, hgt)
            }
            path.addRect(0f, H * 0.84f, W, H, Path.Direction.CW)
            body(path, W * 0.5f)
            // fog between the trees
            p.shader = lin(0f, H * 0.6f, 0f, H * 0.8f, a(m.glow, 0), a(m.glow, 50), a(m.glow, 0)); f.drawRect(0f, H * 0.6f, W, H * 0.8f, p); p.shader = null
        }

        fun crystal() {
            glow(b, W * 0.5f, H * 0.5f, W * 0.6f, a(m.glow, 120))
            val pts = listOf(0.5f to 0.32f, 0.66f to 0.46f, 0.6f to 0.78f, 0.42f to 0.8f, 0.34f to 0.48f)
            val path = Path().apply { moveTo(W * pts[0].first, H * pts[0].second); pts.drop(1).forEach { (x, y) -> lineTo(W * x, H * y) }; close() }
            // ground shards
            path.moveTo(0f, H); path.lineTo(W * 0.1f, H * 0.7f); path.lineTo(W * 0.22f, H); path.close()
            path.moveTo(W * 0.78f, H); path.lineTo(W * 0.88f, H * 0.66f); path.lineTo(W, H * 0.9f); path.lineTo(W, H); path.close()
            path.addRect(0f, H * 0.86f, W, H, Path.Direction.CW)
            body(path, W * 0.4f)
            val cx = W * 0.5f; val cy = H * 0.56f
            for (i in pts.indices) {
                val (x0, y0) = pts[i]; val (x1, y1) = pts[(i + 1) % pts.size]
                p.color = a(if (i % 2 == 0) m.rim else m.glow, 40 + i * 18)
                f.drawPath(Path().apply { moveTo(cx, cy); lineTo(W * x0, H * y0); lineTo(W * x1, H * y1); close() }, p)
            }
        }

        fun orb() {
            val cx = W * 0.5f; val cy = H * 0.56f; val rad = W * 0.36f
            glow(f, cx, cy, rad * 1.5f, a(m.glow, 90))
            p.shader = RadialGradient(cx - rad * 0.35f, cy - rad * 0.4f, rad * 1.5f, intArrayOf(m.rim, m.lit, m.dark, Color.BLACK), floatArrayOf(0f, 0.25f, 0.7f, 1f), Shader.TileMode.CLAMP)
            f.drawCircle(cx, cy, rad, p); p.shader = null
            p.shader = RadialGradient(cx - rad * 0.4f, cy - rad * 0.45f, rad * 0.35f, a(Color.WHITE, 170), a(Color.WHITE, 0), Shader.TileMode.CLAMP)
            f.drawCircle(cx - rad * 0.4f, cy - rad * 0.45f, rad * 0.35f, p); p.shader = null
            // reflection pool
            p.shader = lin(0f, H * 0.82f, 0f, H, mix(m.dark, m.lit, 0.2f), Color.BLACK); f.drawRect(0f, H * 0.83f, W, H, p); p.shader = null
            glow(f, cx, H * 0.84f, rad, a(m.glow, 70))
        }

        fun arch() {
            // the sky shows through an arched window; the wall around it is the foreground
            glow(b, W * 0.5f, H * 0.62f, W * 0.4f, a(m.glow, 170)); p.color = a(m.rim, 235); b.drawCircle(W * 0.5f, H * 0.6f, W * 0.1f, p)
            val wall = Path().apply { addRect(0f, 0f, W, H, Path.Direction.CW) }
            val hole = Path().apply {
                moveTo(W * 0.24f, H * 0.86f); lineTo(W * 0.24f, H * 0.44f)
                quadTo(W * 0.24f, H * 0.3f, W * 0.5f, H * 0.24f); quadTo(W * 0.76f, H * 0.3f, W * 0.76f, H * 0.44f)
                lineTo(W * 0.76f, H * 0.86f); close()
            }
            wall.op(hole, Path.Op.DIFFERENCE)
            // the arch frame only fills the lower part so the clock area above stays open sky… keep top open
            val clip = Path().apply { addRect(0f, H * 0.3f, W, H, Path.Direction.CW) }
            wall.op(clip, Path.Op.INTERSECT)
            body(wall, W * 0.5f, rim = false)
            p.style = Paint.Style.STROKE; p.strokeWidth = W * 0.01f; p.color = a(m.rim, 160); f.drawPath(hole, p); p.style = Paint.Style.FILL
            // geometric star tiles on the wall
            p.color = a(m.rim, 30)
            for (yy in 0..8) for (xx in 0..4) { val x = W * (xx * 0.25f); val y = H * (0.34f + yy * 0.08f); f.drawCircle(x, y, W * 0.02f, p) }
        }

        fun iceberg() {
            p.color = a(m.skyBot, 255); b.drawRect(0f, H * 0.62f, W, H, p)
            val path = Path().apply {
                moveTo(W * 0.08f, H * 0.62f); lineTo(W * 0.22f, H * 0.5f); lineTo(W * 0.36f, H * 0.42f); lineTo(W * 0.46f, H * 0.36f)
                lineTo(W * 0.56f, H * 0.4f); lineTo(W * 0.7f, H * 0.38f); lineTo(W * 0.84f, H * 0.52f); lineTo(W * 0.94f, H * 0.62f)
                // under water, wider and fading
                lineTo(W * 1.05f, H * 0.92f); lineTo(-W * 0.05f, H * 0.95f); close()
            }
            body(path, W * 0.4f)
            p.shader = lin(0f, H * 0.62f, 0f, H, a(m.skyBot, 120), a(Color.BLACK, 230)); f.drawRect(0f, H * 0.62f, W, H, p); p.shader = null
            p.color = a(m.rim, 140); f.drawRect(0f, H * 0.618f, W, H * 0.622f, p)
        }

        fun volcano() {
            glow(b, W * 0.5f, H * 0.34f, W * 0.5f, a(m.glow, 140))
            repeat(40) { p.color = a(m.glow, 80 + r.nextInt(150)); b.drawCircle(W * (0.35f + r.nextFloat() * 0.3f), H * (0.1f + r.nextFloat() * 0.25f), W * 0.004f, p) }
            val path = Path().apply {
                moveTo(-W * 0.1f, H); lineTo(W * 0.1f, H * 0.62f); lineTo(W * 0.36f, H * 0.4f); lineTo(W * 0.44f, H * 0.36f)
                lineTo(W * 0.56f, H * 0.36f); lineTo(W * 0.64f, H * 0.4f); lineTo(W * 0.9f, H * 0.62f); lineTo(W * 1.1f, H); close()
            }
            body(path, W * 0.5f)
            f.save(); f.clipPath(path)
            p.color = a(m.glow, 200); p.style = Paint.Style.STROKE; p.strokeWidth = W * 0.008f; p.maskFilter = BlurMaskFilter(W * 0.01f, BlurMaskFilter.Blur.NORMAL)
            f.drawPath(Path().apply { moveTo(W * 0.5f, H * 0.36f); quadTo(W * 0.46f, H * 0.5f, W * 0.4f, H * 0.66f) }, p)
            f.drawPath(Path().apply { moveTo(W * 0.52f, H * 0.36f); quadTo(W * 0.58f, H * 0.52f, W * 0.62f, H * 0.7f) }, p)
            p.maskFilter = null; p.style = Paint.Style.FILL; f.restore()
            glow(f, W * 0.5f, H * 0.36f, W * 0.1f, a(m.glow, 230))
        }

        fun canyon() {
            glow(b, W * 0.5f, H * 0.5f, W * 0.3f, a(m.glow, 160))
            val left = Path().apply { moveTo(0f, 0f); lineTo(W * 0.22f, H * 0.3f); lineTo(W * 0.3f, H * 0.45f); lineTo(W * 0.24f, H * 0.6f); lineTo(W * 0.4f, H * 0.8f); lineTo(W * 0.3f, H); lineTo(0f, H); close() }
            val right = Path().apply { moveTo(W, H * 0.1f); lineTo(W * 0.78f, H * 0.32f); lineTo(W * 0.7f, H * 0.5f); lineTo(W * 0.78f, H * 0.64f); lineTo(W * 0.6f, H * 0.84f); lineTo(W * 0.7f, H); lineTo(W, H); close() }
            val floor = Path().apply { addRect(0f, H * 0.9f, W, H, Path.Direction.CW) }
            val all = Path().apply { addPath(left); addPath(right); addPath(floor) }
            // keep the top of the canyon walls below the clock area
            all.op(Path().apply { addRect(0f, H * 0.34f, W, H, Path.Direction.CW) }, Path.Op.INTERSECT)
            body(all, W * 0.5f)
        }

        fun lantern() {
            val cx = W * 0.5f
            glow(b, cx, H * 0.6f, W * 0.55f, a(m.glow, 120))
            val path = Path()
            path.addRect(cx - W * 0.003f, 0f, cx + W * 0.003f, H * 0.38f, Path.Direction.CW)
            path.moveTo(cx - W * 0.1f, H * 0.44f); path.lineTo(cx, H * 0.38f); path.lineTo(cx + W * 0.1f, H * 0.44f); path.close()
            path.addRect(cx - W * 0.12f, H * 0.44f, cx + W * 0.12f, H * 0.46f, Path.Direction.CW)
            path.moveTo(cx - W * 0.11f, H * 0.46f); path.lineTo(cx - W * 0.15f, H * 0.62f); path.lineTo(cx - W * 0.09f, H * 0.7f)
            path.lineTo(cx + W * 0.09f, H * 0.7f); path.lineTo(cx + W * 0.15f, H * 0.62f); path.lineTo(cx + W * 0.11f, H * 0.46f); path.close()
            path.moveTo(cx - W * 0.09f, H * 0.7f); path.lineTo(cx, H * 0.76f); path.lineTo(cx + W * 0.09f, H * 0.7f); path.close()
            p.color = m.dark; f.drawPath(path, p)
            // glowing glass panes
            val glass = Path().apply { moveTo(cx - W * 0.095f, H * 0.475f); lineTo(cx - W * 0.13f, H * 0.62f); lineTo(cx - W * 0.08f, H * 0.68f); lineTo(cx + W * 0.08f, H * 0.68f); lineTo(cx + W * 0.13f, H * 0.62f); lineTo(cx + W * 0.095f, H * 0.475f); close() }
            p.shader = RadialGradient(cx, H * 0.59f, W * 0.16f, intArrayOf(a(Color.WHITE, 255), m.glow, mix(m.glow, m.dark, 0.5f)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            f.drawPath(glass, p); p.shader = null
            p.color = m.dark; p.strokeWidth = W * 0.006f; p.style = Paint.Style.STROKE
            f.drawLine(cx, H * 0.475f, cx, H * 0.68f, p); f.drawLine(cx - W * 0.13f, H * 0.6f, cx + W * 0.13f, H * 0.6f, p); p.style = Paint.Style.FILL
            glow(f, cx, H * 0.6f, W * 0.3f, a(m.glow, 70))
            // floor
            p.shader = lin(0f, H * 0.86f, 0f, H, m.dark, Color.BLACK); f.drawRect(0f, H * 0.88f, W, H, p); p.shader = null
        }

        fun tree() {
            glow(b, W * 0.5f, H * 0.42f, W * 0.55f, a(m.glow, 130))
            val path = Path()
            fun branch(x: Float, y: Float, ang: Float, len: Float, wdt: Float, depth: Int) {
                val ex = x + cos(ang) * len; val ey = y + sin(ang) * len
                val nx = -sin(ang) * wdt / 2; val ny = cos(ang) * wdt / 2
                path.moveTo(x + nx, y + ny); path.lineTo(ex + nx * 0.6f, ey + ny * 0.6f); path.lineTo(ex - nx * 0.6f, ey - ny * 0.6f); path.lineTo(x - nx, y - ny); path.close()
                if (depth == 0) { path.addCircle(ex, ey, len * 0.5f, Path.Direction.CW); return }
                val spread = 0.45f + r.nextFloat() * 0.2f
                branch(ex, ey, ang - spread, len * 0.74f, wdt * 0.62f, depth - 1)
                branch(ex, ey, ang + spread, len * 0.74f, wdt * 0.62f, depth - 1)
                if (r.nextFloat() < 0.4f) branch(ex, ey, ang + (r.nextFloat() - 0.5f) * 0.3f, len * 0.6f, wdt * 0.5f, depth - 1)
            }
            branch(W * 0.5f, H * 0.86f, -PI.toFloat() / 2, H * 0.17f, W * 0.07f, 6)
            path.moveTo(0f, H); path.lineTo(0f, H * 0.86f); path.quadTo(W * 0.5f, H * 0.82f, W, H * 0.88f); path.lineTo(W, H); path.close()
            p.color = m.dark; f.drawPath(path, p)
            p.shader = lin(0f, H * 0.82f, 0f, H, a(Color.BLACK, 0), a(Color.BLACK, 200)); f.drawRect(0f, H * 0.8f, W, H, p); p.shader = null
        }

        fun wave() {
            glow(b, W * 0.7f, H * 0.3f, W * 0.3f, a(m.glow, 140)); p.color = a(m.rim, 230); b.drawCircle(W * 0.7f, H * 0.3f, W * 0.07f, p)
            val path = Path().apply {
                moveTo(-W * 0.05f, H); lineTo(-W * 0.05f, H * 0.62f)
                cubicTo(W * 0.2f, H * 0.6f, W * 0.3f, H * 0.36f, W * 0.56f, H * 0.38f)
                cubicTo(W * 0.74f, H * 0.39f, W * 0.8f, H * 0.48f, W * 0.7f, H * 0.52f)
                cubicTo(W * 0.62f, H * 0.55f, W * 0.66f, H * 0.62f, W * 0.8f, H * 0.64f)
                lineTo(W * 1.05f, H * 0.66f); lineTo(W * 1.05f, H); close()
            }
            body(path, W * 0.45f)
            // foam along the crest
            p.color = a(m.rim, 150)
            repeat(70) {
                val t = r.nextFloat()
                val x = W * (0.3f + t * 0.42f); val y = H * (0.37f + (t - 0.6f).let { it * it } * 0.6f) + (r.nextFloat() - 0.5f) * H * 0.015f
                f.drawCircle(x, y, W * (0.003f + r.nextFloat() * 0.006f), p)
            }
        }
    }
}
