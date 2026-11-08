package com.smrh.zujaj.render

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Signature widgets: Prayer Sky, Real Moon, Tasbeeh beads, Qibla, Daily leaf and the Prayer tracker.
 * Each paints its own full background (they are pictures, not cards).
 */
object SignatureR {
    private fun clip(r: RenderCtx): Path {
        val rad = Renderer.cornerPx(r)
        return Path().apply { addRoundRect(RectF(0f, 0f, r.w, r.h), rad, rad, Path.Direction.CW) }
    }
    private fun mix(a: Int, b: Int, t: Float) = RenderCtx.mix(a, b, t.coerceIn(0f, 1f))

    // =========================================================================================== 1 · Prayer sky
    /** A window on the sky: its colours follow the real time, the sun (or moon) travels an arc with the prayers on it. */
    fun sky(r: RenderCtx) {
        val c = r.canvas
        val d = Kit.day(r)
        val now = r.now.toInstant().toEpochMilli()
        val t = d.list.map { it.time }   // fajr, sunrise, dhuhr, asr, maghrib, isha
        val fajr = t[0]; val rise = t[1]; val set = t[4]; val isha = t[5]
        // sky colours: night → dawn violet → day blue → dusk orange → night
        val night = intArrayOf(0xFF070B1E.toInt(), 0xFF141B3E.toInt())
        val dawn = intArrayOf(0xFF2B2160.toInt(), 0xFFB06AA6.toInt())
        val day = intArrayOf(0xFF2F7FD6.toInt(), 0xFF9BD1F5.toInt())
        val dusk = intArrayOf(0xFF3A1F55.toInt(), 0xFFF08A4B.toInt())
        fun lerp2(a: IntArray, b: IntArray, k: Float) = intArrayOf(mix(a[0], b[0], k), mix(a[1], b[1], k))
        val hour = 3_600_000f
        val sky = when {
            now < fajr -> night
            now < rise -> lerp2(night, dawn, (now - fajr) / (rise - fajr).toFloat().coerceAtLeast(1f))
            now < rise + hour -> lerp2(dawn, day, (now - rise) / hour)
            now < set - hour -> day
            now < set -> lerp2(day, dusk, (now - (set - hour)) / hour)
            now < isha -> lerp2(dusk, night, (now - set) / (isha - set).toFloat().coerceAtLeast(1f))
            else -> night
        }
        val isNight = now < rise || now > set
        c.save(); c.clipPath(clip(r))
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = LinearGradient(0f, 0f, 0f, r.h, sky[0], sky[1], Shader.TileMode.CLAMP) })
        // stars, fading in at dusk and out at dawn
        val starA = when { now < fajr || now > isha -> 1f; now < rise -> 1f - (now - fajr) / (rise - fajr).toFloat(); now > set -> (now - set) / (isha - set).toFloat(); else -> 0f }.coerceIn(0f, 1f)
        if (starA > 0.02f) {
            val rnd = java.util.Random(7)
            repeat(60) { r.circle(rnd.nextFloat() * r.w, rnd.nextFloat() * r.h * 0.7f, (0.5f + rnd.nextFloat() * 1.3f) * r.u, withAlpha(0xFFFFFFFF.toInt(), (starA * (90 + rnd.nextInt(150))).toInt())) }
        }
        // the arc: sunrise on the left, sunset on the right
        val left = r.pad + 14 * r.u; val right = r.w - r.pad - 14 * r.u
        val base = r.h * 0.74f; val top = r.h * 0.2f
        val arcRect = RectF(left, top, right, base + (base - top))
        c.drawArc(arcRect, 180f, 180f, false, r.strokePaint(withAlpha(0xFFFFFFFF.toInt(), 70), 1.4f * r.u).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(4 * r.u, 4 * r.u), 0f) })
        c.drawLine(r.pad, base, r.w - r.pad, base, r.strokePaint(withAlpha(0xFFFFFFFF.toInt(), 50), 1f * r.u))
        fun pos(ms: Long): Pair<Float, Float> {
            val k = ((ms - rise) / (set - rise).toFloat()).coerceIn(0f, 1f)
            val a = PI * (1 - k)
            val cx = (left + right) / 2; val rx = (right - left) / 2; val ry = base - top
            return (cx + (rx * cos(a)).toFloat()) to (base - (ry * sin(a)).toFloat())
        }
        // prayer marks on the arc (dhuhr, asr) and at its ends (sunrise, maghrib)
        val nextKey = d.next.key
        for (i in listOf(1, 2, 3, 4)) {
            val p = d.list[i]; val (x, y) = pos(p.time)
            val on = p.key == nextKey
            r.circle(x, y, (if (on) 4.5f else 3f) * r.u, if (on) r.accent else withAlpha(0xFFFFFFFF.toInt(), 200))
            if (on) r.circle(x, y, 9 * r.u, withAlpha(r.accent, 60))
            r.text(Kit.pName(r, p), x, y - 11 * r.u, 8.5f * r.u, if (on) r.accent else withAlpha(0xFFFFFFFF.toInt(), 220), r.sansBold, Paint.Align.CENTER)
        }
        // the sun by day, the moon by night (riding the same arc from sunset to the next sunrise)
        if (!isNight) {
            val (x, y) = pos(now)
            c.drawCircle(x, y, 26 * r.u, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(x, y, 26 * r.u, 0x88FFE7A0.toInt(), 0x00FFE7A0, Shader.TileMode.CLAMP) })
            r.circle(x, y, 9 * r.u, 0xFFFFE07A.toInt())
        } else {
            val nightStart = if (now > set) set else set - 86_400_000L
            val nightEnd = if (now > set) rise + 86_400_000L else rise
            val k = ((now - nightStart) / (nightEnd - nightStart).toFloat()).coerceIn(0f, 1f)
            val a = PI * (1 - k)
            val x = (left + right) / 2 + ((right - left) / 2 * cos(a)).toFloat(); val y = base - ((base - top) * sin(a)).toFloat()
            Glyphs.moon(c, x, y, 20 * r.u, 0xFFF4EBD0.toInt())
        }
        // the ground: a soft dark band, then the next prayer and its countdown
        c.drawRect(0f, base, r.w, r.h, Paint().apply { shader = LinearGradient(0f, base, 0f, r.h, withAlpha(0xFF000000.toInt(), 70), withAlpha(0xFF000000.toInt(), 150), Shader.TileMode.CLAMP) })
        val ar = r.ar
        val sx = if (ar) r.w - r.pad - 4 * r.u else r.pad + 4 * r.u
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val ty = base + (r.h - base) / 2
        r.text(Kit.pName(r, d.next) + "  " + Kit.pTime(r, d.next.time), sx, ty, 12 * r.u * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, al)
        Kit.countdown(r, if (ar) r.pad + 4 * r.u else r.w - r.pad - 4 * r.u, ty, 11 * r.u, withAlpha(0xFFFFFFFF.toInt(), 220), if (ar) Paint.Align.LEFT else Paint.Align.RIGHT)
        c.restore()
    }

    // =========================================================================================== 2 · Real moon
    /** Days since the last new moon (0–29.5). */
    fun moonAge(ms: Long): Double {
        val synodic = 29.530588853
        val ref = 947182440000.0   // 2000-01-06 18:14 UTC, a new moon
        val days = (ms - ref) / 86_400_000.0
        return ((days % synodic) + synodic) % synodic
    }

    fun moon(r: RenderCtx) {
        val c = r.canvas
        c.save(); c.clipPath(clip(r))
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = RadialGradient(r.w * 0.3f, r.h * 0.4f, maxOf(r.w, r.h), 0xFF1B1E33.toInt(), 0xFF05060C.toInt(), Shader.TileMode.CLAMP) })
        val rnd = java.util.Random(3)
        repeat(40) { r.circle(rnd.nextFloat() * r.w, rnd.nextFloat() * r.h, (0.5f + rnd.nextFloat()) * r.u, withAlpha(0xFFFFFFFF.toInt(), 60 + rnd.nextInt(120))) }
        val ms = r.now.toInstant().toEpochMilli()
        val age = moonAge(ms)
        val wide = r.w > r.h * 1.4f
        val R = minOf(r.h * (if (wide) 0.36f else 0.3f), r.w * 0.3f)
        val cx = if (wide) (if (r.ar) r.w - r.pad - R - 10 * r.u else r.pad + R + 10 * r.u) else r.w / 2
        val cy = if (wide) r.h / 2 else r.pad + R + 8 * r.u
        // glow, dark disc, then the lit part: a half-disc plus or minus an ellipse (the terminator)
        c.drawCircle(cx, cy, R * 1.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx, cy, R * 1.5f, 0x33FFF3D6, 0x00FFF3D6, Shader.TileMode.CLAMP) })
        r.circle(cx, cy, R, 0xFF2A2C38.toInt())
        val phase = age / 29.530588853           // 0 new, 0.5 full
        val lit = Path()
        val waxing = phase < 0.5
        val k = cos(2 * PI * phase).toFloat()     // 1 new → -1 full → 1 new
        val rect = RectF(cx - R, cy - R, cx + R, cy + R)
        // lit half: right half while waxing, left half while waning (northern-hemisphere view)
        lit.arcTo(rect, if (waxing) -90f else 90f, 180f)
        val ex = R * abs(k)
        val ell = RectF(cx - ex, cy - R, cx + ex, cy + R)
        // terminator: bulges into the lit half when crescent (k > 0), away from it when gibbous
        lit.arcTo(ell, if (waxing) 90f else -90f, if (k > 0) -180f else 180f)
        lit.close()
        c.drawPath(lit, Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(cx - R * 0.2f, cy - R * 0.2f, R * 1.3f, 0xFFFFF8E6.toInt(), 0xFFE0D2AE.toInt(), Shader.TileMode.CLAMP) })
        // a few soft craters on the lit side
        for ((dx, dy, s) in listOf(Triple(0.25f, -0.2f, 0.14f), Triple(-0.15f, 0.3f, 0.1f), Triple(0.35f, 0.35f, 0.08f))) {
            c.save(); c.clipPath(lit); r.circle(cx + dx * R, cy + dy * R, s * R, 0x22000000); c.restore()
        }
        // text: Hijri date, phase name, and the countdown to Ramadan / the Eids when close
        val h = Calendars.hijri(r.today)
        val ar = r.ar
        val dayTxt = r.n(h.day) + " " + Calendars.hijriMonth(h.month, ar) + " " + r.n(h.year)
        val phaseName = when {
            age < 1.5 || age > 28.0 -> r.t("New moon", "محاق")
            age < 6.5 -> r.t("Waxing crescent", "هلال")
            age < 8.5 -> r.t("First quarter", "التربيع الأول")
            age < 13.5 -> r.t("Waxing gibbous", "أحدب متزايد")
            age < 16.5 -> r.t("Full moon", "بدر")
            age < 21.5 -> r.t("Waning gibbous", "أحدب متناقص")
            age < 23.5 -> r.t("Last quarter", "التربيع الأخير")
            else -> r.t("Waning crescent", "هلال آخر الشهر")
        }
        val season = when {
            h.month == 8 -> r.t("${30 - h.day} days to Ramadan", "باقي ${r.n(30 - h.day)} يوم على رمضان")
            h.month == 9 -> r.t("${30 - h.day} days to Eid al-Fitr", "باقي ${r.n(30 - h.day)} يوم على عيد الفطر")
            h.month == 12 && h.day < 10 -> r.t("${10 - h.day} days to Eid al-Adha", "باقي ${r.n(10 - h.day)} يوم على عيد الأضحى")
            h.month == 12 && h.day in 10..13 -> r.t("Eid Mubarak", "عيد مبارك")
            h.month == 10 && h.day <= 3 -> r.t("Eid Mubarak", "عيد مبارك")
            else -> r.t("Day ${h.day} of ${Calendars.hijriMonth(h.month, false)}", "اليوم ${r.n(h.day)} من الشهر")
        }
        if (wide) {
            val tx = if (ar) cx - R - 18 * r.u else cx + R + 18 * r.u
            val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
            val room = if (ar) tx - r.pad else r.w - r.pad - tx
            r.text(phaseName, tx, r.h * 0.3f, 10 * r.u, withAlpha(0xFFFFFFFF.toInt(), 180), r.sans, al, maxW = room)
            r.text(dayTxt, tx, r.h * 0.5f, 17 * r.u * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, al, maxW = room)
            r.text(season, tx, r.h * 0.7f, 11 * r.u * r.s2, r.accent, r.sansBold, al, maxW = room)
        } else {
            r.text(dayTxt, r.w / 2, cy + R + 18 * r.u, 12 * r.u * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
            r.text(season, r.w / 2, cy + R + 34 * r.u, 9.5f * r.u * r.s2, r.accent, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        }
        c.restore()
    }

    // =========================================================================================== 3 · Tasbeeh beads
    fun beads(r: RenderCtx) {
        val c = r.canvas
        c.save(); c.clipPath(clip(r))
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = RadialGradient(r.w / 2, r.h / 2, maxOf(r.w, r.h) * 0.75f, 0xFF2A1A12.toInt(), 0xFF0E0806.toInt(), Shader.TileMode.CLAMP) })
        val n = 33
        val count = r.cfg.counter
        val cur = count % n
        val full = count > 0 && cur == 0
        val cx = r.w / 2; val cy = r.h / 2
        val R = minOf(r.w, r.h) / 2 - r.pad - 10 * r.u
        val bead = (2 * PI * R / n).toFloat() * 0.42f
        // the cord
        c.drawCircle(cx, cy, R, r.strokePaint(0x55C9A06B, 1.2f * r.u))
        for (i in 0 until n) {
            // bead 0 at the top, counting clockwise; the done ones are lit, the next one is larger
            val a = -PI / 2 + 2 * PI * i / n
            val x = cx + (R * cos(a)).toFloat(); val y = cy + (R * sin(a)).toFloat()
            val done = full || i < cur
            val next = !full && i == cur
            val col = if (done) r.accent else 0xFF6B4A33.toInt()
            val s = if (next) bead * 1.3f else bead
            c.drawCircle(x, y, s, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(x - s * 0.35f, y - s * 0.35f, s * 1.4f, mix(col, 0xFFFFFFFF.toInt(), 0.45f), mix(col, 0xFF000000.toInt(), 0.35f), Shader.TileMode.CLAMP)
            })
        }
        if (full) c.drawCircle(cx, cy, R, r.strokePaint(withAlpha(r.accent, 120), 6 * r.u))
        // the tassel bead at the top
        r.circle(cx, cy - R - bead * 2.2f, bead * 0.9f, 0xFFC9A06B.toInt())
        // count and the dhikr of this round (33 subhanallah, 33 alhamdulillah, 34 allahu akbar → simplified as rounds of 33)
        val round = (count / n) % 3
        val dhikr = listOf(r.t("SubhanAllah", "سبحان الله"), r.t("Alhamdulillah", "الحمد لله"), r.t("Allahu Akbar", "الله أكبر"))[if (full) (round + 2) % 3 else round]
        r.textTight(r.n(if (full) n else cur), cx, cy - R * 0.12f, R * 0.5f * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, maxW = R * 1.2f)
        r.text(dhikr, cx, cy + R * 0.3f, 10 * r.u * r.s2, r.accent, r.sansBold, Paint.Align.CENTER, maxW = R * 1.5f)
        r.text(r.t("Total ", "المجموع ") + r.n(count), cx, cy + R * 0.52f, 8 * r.u, withAlpha(0xFFFFFFFF.toInt(), 150), r.sans, Paint.Align.CENTER)
        c.restore()
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("tasbeeh", "inc"))
    }

    // =========================================================================================== 4 · Qibla
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LNG = 39.8262

    /** Bearing from north to the Kaaba, in degrees, and the distance in km. */
    fun qibla(lat: Double, lng: Double): Pair<Double, Double> {
        val p1 = Math.toRadians(lat); val p2 = Math.toRadians(KAABA_LAT); val dl = Math.toRadians(KAABA_LNG - lng)
        val b = Math.toDegrees(atan2(sin(dl) * cos(p2), cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)))
        val a = sin((p2 - p1) / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
        val km = 6371.0 * 2 * atan2(sqrt(a), sqrt(1 - a))
        return ((b + 360) % 360) to km
    }

    fun compass(r: RenderCtx) {
        val c = r.canvas
        c.save(); c.clipPath(clip(r))
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = RadialGradient(r.w / 2, r.h / 2, maxOf(r.w, r.h) * 0.7f, 0xFF10261F.toInt(), 0xFF050C0A.toInt(), Shader.TileMode.CLAMP) })
        val (bearing, km) = qibla(Prefs.lat, Prefs.lng)
        val wide = r.w > r.h * 1.4f
        val R = minOf(r.h * 0.4f, (if (wide) r.w * 0.28f else r.w * 0.38f))
        val cx = if (wide) (if (r.ar) r.w - r.pad - R - 8 * r.u else r.pad + R + 8 * r.u) else r.w / 2
        val cy = if (wide) r.h / 2 else r.pad + R + 6 * r.u
        val gold = 0xFFD9B26B.toInt()
        // dial: ring, ticks every 30°, N/E/S/W
        c.drawCircle(cx, cy, R, r.strokePaint(withAlpha(gold, 140), 1.6f * r.u))
        for (i in 0 until 72) {
            val a = Math.toRadians(i * 5.0 - 90)
            val l = if (i % 18 == 0) 8f else if (i % 6 == 0) 5f else 2.5f
            c.drawLine(cx + ((R - l * r.u) * cos(a)).toFloat(), cy + ((R - l * r.u) * sin(a)).toFloat(), cx + (R * cos(a)).toFloat(), cy + (R * sin(a)).toFloat(),
                r.strokePaint(withAlpha(gold, if (i % 6 == 0) 200 else 90), 1f * r.u))
        }
        val dirs = if (r.ar) listOf("ش", "ق", "ج", "غ") else listOf("N", "E", "S", "W")
        dirs.forEachIndexed { i, s ->
            val a = Math.toRadians(i * 90.0 - 90)
            r.text(s, cx + ((R - 16 * r.u) * cos(a)).toFloat(), cy + ((R - 16 * r.u) * sin(a)).toFloat(), 9 * r.u, if (i == 0) 0xFFE5484D.toInt() else withAlpha(0xFFFFFFFF.toInt(), 200), r.sansBold, Paint.Align.CENTER)
        }
        // the needle towards Mecca, with a small Kaaba at its tip
        val a = Math.toRadians(bearing - 90)
        val tipX = cx + ((R * 0.72f) * cos(a)).toFloat(); val tipY = cy + ((R * 0.72f) * sin(a)).toFloat()
        val tailX = cx - ((R * 0.3f) * cos(a)).toFloat(); val tailY = cy - ((R * 0.3f) * sin(a)).toFloat()
        c.drawLine(tailX, tailY, tipX, tipY, r.strokePaint(gold, 3.2f * r.u))
        r.circle(cx, cy, 4 * r.u, gold)
        val k = 7 * r.u
        c.save(); c.translate(tipX, tipY); c.rotate((bearing).toFloat())
        c.drawRect(-k, -k, k, k, r.fillPaint(0xFF111111.toInt()))
        c.drawRect(-k, -k * 0.55f, k, -k * 0.25f, r.fillPaint(gold))
        c.restore()
        // text
        val deg = r.n(bearing.toInt()) + "°"
        val dist = r.n("%,d".format(km.toInt())) + r.t(" km to Mecca", " كم لمكة")
        if (wide) {
            val tx = if (r.ar) cx - R - 18 * r.u else cx + R + 18 * r.u
            val al = if (r.ar) Paint.Align.RIGHT else Paint.Align.LEFT
            val room = if (r.ar) tx - r.pad else r.w - r.pad - tx
            r.text(r.t("Qibla", "القبلة"), tx, r.h * 0.3f, 11 * r.u, gold, r.sansBold, al, maxW = room)
            r.text(deg + r.t(" from north", " من الشمال"), tx, r.h * 0.5f, 16 * r.u * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, al, maxW = room)
            r.text(dist, tx, r.h * 0.7f, 10 * r.u * r.s2, withAlpha(0xFFFFFFFF.toInt(), 190), r.sans, al, maxW = room)
        } else {
            r.text(r.t("Qibla ", "القبلة ") + deg, r.w / 2, cy + R + 14 * r.u, 11 * r.u * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
            r.text(dist, r.w / 2, cy + R + 28 * r.u, 8.5f * r.u * r.s2, withAlpha(0xFFFFFFFF.toInt(), 180), r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        }
        c.restore()
    }

    // =========================================================================================== 5 · Daily leaf
    private data class Saying(val text: String, val src: String)
    private val sayings = listOf(
        Saying("إِنَّ مَعَ الْعُسْرِ يُسْرًا", "الشرح ٦"),
        Saying("فَاذْكُرُونِي أَذْكُرْكُمْ", "البقرة ١٥٢"),
        Saying("وَقُل رَّبِّ زِدْنِي عِلْمًا", "طه ١١٤"),
        Saying("أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ", "الرعد ٢٨"),
        Saying("وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ", "الطلاق ٣"),
        Saying("لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "البقرة ٢٨٦"),
        Saying("إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", "البقرة ١٥٣"),
        Saying("وَلَا تَيْأَسُوا مِن رَّوْحِ اللَّهِ", "يوسف ٨٧"),
        Saying("ادْعُونِي أَسْتَجِبْ لَكُمْ", "غافر ٦٠"),
        Saying("وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ", "الحديد ٤"),
        Saying("رَبِّ اشْرَحْ لِي صَدْرِي", "طه ٢٥"),
        Saying("حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "آل عمران ١٧٣"),
        Saying("إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ", "متفق عليه"),
        Saying("الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ", "متفق عليه"),
        Saying("تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ صَدَقَةٌ", "رواه الترمذي"),
        Saying("خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ", "رواه البخاري"),
        Saying("أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ", "متفق عليه"),
        Saying("الدِّينُ النَّصِيحَةُ", "رواه مسلم"),
    )
    private val wisdom = listOf(
        "من جدّ وجد، ومن زرع حصد", "الوقت كالسيف إن لم تقطعه قطعك", "خير الكلام ما قلّ ودلّ",
        "اصبر تنل", "العلم في الصغر كالنقش على الحجر", "من سار على الدرب وصل", "رُبّ كلمةٍ قالت لصاحبها دعني",
        "الصديق وقت الضيق", "على قدر أهل العزم تأتي العزائم",
    )
    private val monthsAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

    fun leaf(r: RenderCtx) {
        val c = r.canvas
        val d: LocalDate = r.today
        val n = d.toEpochDay().toInt()
        val paper = 0xFFF6F0E2.toInt(); val ink = 0xFF2A2420.toInt(); val red = 0xFFB3261E.toInt()
        val rad = Renderer.cornerPx(r)
        // a little stack of torn sheets behind, then today's sheet
        c.drawRoundRect(RectF(r.pad * 0.6f + 4 * r.u, 6 * r.u, r.w - r.pad * 0.6f + 2 * r.u, r.h), rad, rad, r.fillPaint(0xFFDCD3BF.toInt()))
        val sheet = RectF(r.pad * 0.6f, 0f, r.w - r.pad * 0.6f, r.h - 4 * r.u)
        c.drawRoundRect(sheet, rad, rad, r.fillPaint(paper))
        // red band with the Gregorian month, and the binding holes
        val band = sheet.height() * 0.17f
        c.save(); c.clipRect(sheet.left, sheet.top, sheet.right, sheet.top + band)
        c.drawRoundRect(sheet, rad, rad, r.fillPaint(red)); c.restore()
        for (i in 0 until 2) r.circle(sheet.left + sheet.width() * (0.3f + i * 0.4f), sheet.top + band * 0.28f, 3 * r.u, 0xFF7A1A14.toInt())
        val month = if (r.ar) monthsAr[d.monthValue - 1] + " " + r.n(d.year) else d.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + d.year
        r.text(month, sheet.centerX(), sheet.top + band * 0.66f, 10 * r.u, 0xFFFFFFFF.toInt(), r.sansBold, Paint.Align.CENTER, maxW = sheet.width() * 0.8f)
        // big day number and day name
        val tall = r.h > r.w * 0.9f
        val numY = sheet.top + band + sheet.height() * (if (tall) 0.15f else 0.2f)
        r.textTight(r.n(d.dayOfMonth), sheet.centerX(), numY, sheet.height() * (if (tall) 0.2f else 0.26f) * r.s1, ink, F.BODONI, maxW = sheet.width() * 0.6f)
        val h = Calendars.hijri(d)
        r.text(Calendars.dayName(d, true) + "  ·  " + r.n(h.day) + " " + Calendars.hijriMonth(h.month, true) + " " + r.n(h.year),
            sheet.centerX(), numY + sheet.height() * (if (tall) 0.13f else 0.17f), 9.5f * r.u * r.s2, red, F.CAIRO_BOLD, Paint.Align.CENTER, maxW = sheet.width() - 16 * r.u)
        c.drawLine(sheet.left + 14 * r.u, numY + sheet.height() * (if (tall) 0.19f else 0.24f), sheet.right - 14 * r.u, numY + sheet.height() * (if (tall) 0.19f else 0.24f), r.strokePaint(0x332A2420, 1f * r.u))
        // tap flips between the verse/hadith and a saying
        val showWisdom = (r.cfg.counter % 2) == 1
        val top = numY + sheet.height() * (if (tall) 0.22f else 0.27f)
        val box = RectF(sheet.left + 14 * r.u, top, sheet.right - 14 * r.u, sheet.bottom - 16 * r.u)
        if (!showWisdom) {
            val s = sayings[Math.floorMod(n, sayings.size)]
            r.paragraph(s.text, RectF(box.left, box.top, box.right, box.bottom - 14 * r.u), 12 * r.u * r.s2, ink, F.AMIRI, android.text.Layout.Alignment.ALIGN_CENTER, 1.2f)
            r.text(s.src, box.centerX(), box.bottom - 6 * r.u, 7.5f * r.u, withAlpha(ink, 150), F.CAIRO, Paint.Align.CENTER)
        } else {
            r.text("حكمة اليوم", box.centerX(), box.top + 6 * r.u, 8 * r.u, red, F.CAIRO_BOLD, Paint.Align.CENTER)
            r.paragraph(wisdom[Math.floorMod(n, wisdom.size)], RectF(box.left, box.top + 14 * r.u, box.right, box.bottom), 12 * r.u * r.s2, ink, F.CAIRO_BOLD, android.text.Layout.Alignment.ALIGN_CENTER, 1.2f)
        }
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("leaf", "flip"))
    }

    // =========================================================================================== 6 · Prayer tracker
    fun tracker(r: RenderCtx) {
        val c = r.canvas
        c.save(); c.clipPath(clip(r))
        c.drawRect(0f, 0f, r.w, r.h, Paint().apply { shader = LinearGradient(0f, 0f, r.w, r.h, 0xFF0F1A2E.toInt(), 0xFF162A3F.toInt(), Shader.TileMode.CLAMP) })
        val d = Kit.day(r)
        val now = r.now.toInstant().toEpochMilli()
        val bits = PrayerTrack.bits()
        val done = (0 until 5).count { bits and (1 shl it) != 0 }
        val streak = PrayerTrack.streak()
        val ar = r.ar
        val sx = if (ar) r.w - r.pad - 4 * r.u else r.pad + 4 * r.u
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val ox = if (ar) r.pad + 4 * r.u else r.w - r.pad - 4 * r.u
        val oal = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        r.text(r.t("Today: $done of 5 prayers", "صلّيت النهارده ${r.n(done)} من ${r.n(5)}"), sx, r.h * 0.17f, 12 * r.u * r.s1, 0xFFFFFFFF.toInt(), r.sansBold, al, maxW = r.w * 0.6f)
        // streak flame
        val fire = if (streak > 0) 0xFFFF9F43.toInt() else withAlpha(0xFFFFFFFF.toInt(), 90)
        r.text(r.n(streak) + " " + r.t(if (streak == 1) "day" else "days", "يوم"), ox, r.h * 0.17f, 11 * r.u, fire, r.sansBold, oal)
        val fx = if (ar) ox + r.measure(r.n(streak) + " " + r.t("days", "يوم"), 11 * r.u, r.sansBold) + 10 * r.u else ox - r.measure(r.n(streak) + " " + r.t(if (streak == 1) "day" else "days", "يوم"), 11 * r.u, r.sansBold) - 10 * r.u
        val flame = Path().apply {
            val y = r.h * 0.17f; val s = 6 * r.u
            moveTo(fx, y - s * 1.3f); quadTo(fx + s, y - s * 0.2f, fx + s * 0.6f, y + s * 0.8f); quadTo(fx, y + s * 1.2f, fx - s * 0.6f, y + s * 0.8f); quadTo(fx - s, y - s * 0.2f, fx, y - s * 1.3f); close()
        }
        c.drawPath(flame, r.fillPaint(fire))
        // five prayers: a ring each, ticked when done; tap to tick (only once its time has come)
        val five = d.five
        val left = r.pad + 4 * r.u; val right = r.w - r.pad - 4 * r.u
        val cw = (right - left) / 5
        val cy = r.h * 0.56f
        val R = minOf(cw * 0.34f, r.h * 0.17f)
        five.forEachIndexed { k, p ->
            val i = if (ar) 4 - k else k
            val cx = left + cw * i + cw / 2
            val on = bits and (1 shl k) != 0
            val due = now >= p.time
            if (on) {
                r.circle(cx, cy, R, 0xFF3DDC97.toInt())
                val tick = Path().apply { moveTo(cx - R * 0.45f, cy); lineTo(cx - R * 0.1f, cy + R * 0.35f); lineTo(cx + R * 0.5f, cy - R * 0.35f) }
                c.drawPath(tick, r.strokePaint(0xFF0B2A1C.toInt(), R * 0.2f))
            } else {
                c.drawCircle(cx, cy, R, r.strokePaint(withAlpha(0xFFFFFFFF.toInt(), if (due) 200 else 70), 2f * r.u))
                if (p.key == d.next.key) r.circle(cx, cy, R * 0.3f, withAlpha(r.accent, 200))
            }
            r.text(Kit.pName(r, p), cx, cy + R + 12 * r.u, 9 * r.u * r.s2, if (on) 0xFF3DDC97.toInt() else withAlpha(0xFFFFFFFF.toInt(), if (due) 230 else 120), r.sansBold, Paint.Align.CENTER, maxW = cw)
            r.text(Kit.pTime(r, p.time, false), cx, cy + R + 25 * r.u, 7.5f * r.u, withAlpha(0xFFFFFFFF.toInt(), 130), r.sans, Paint.Align.CENTER, maxW = cw)
            if (due) r.hit(RectF(cx - cw / 2, cy - R - 8 * r.u, cx + cw / 2, cy + R + 30 * r.u), HitAction.Cmd("pray", "$k"))
        }
        c.restore()
    }
}

/** Today's ticked prayers (bits 0–4) and the streak of full days. Shared by every tracker widget. */
object PrayerTrack {
    private fun today() = LocalDate.now().toEpochDay()
    private fun norm() {
        if (Prefs.getString("pt_day").toLongOrNull() != today()) { Prefs.setString("pt_day", today().toString()); Prefs.setString("pt_bits", "0") }
    }
    fun bits(): Int { norm(); return Prefs.getString("pt_bits").toIntOrNull() ?: 0 }
    /** Full days in a row, ending today or yesterday. */
    fun streak(): Int {
        val last = Prefs.getString("pt_last").toLongOrNull() ?: return 0
        return if (last >= today() - 1) Prefs.getString("pt_streak").toIntOrNull() ?: 0 else 0
    }
    /** Ticks or unticks one prayer; returns true when the day just became complete. */
    fun toggle(i: Int): Boolean {
        val before = bits()
        val now = before xor (1 shl i)
        Prefs.setString("pt_bits", now.toString())
        val t = today()
        val last = Prefs.getString("pt_last").toLongOrNull() ?: 0L
        var s = Prefs.getString("pt_streak").toIntOrNull() ?: 0
        if (now == 31 && last != t) {
            s = if (last == t - 1) s + 1 else 1
            Prefs.setString("pt_streak", s.toString()); Prefs.setString("pt_last", t.toString()); return true
        }
        if (now != 31 && last == t) {
            s = (s - 1).coerceAtLeast(0)
            Prefs.setString("pt_streak", s.toString()); Prefs.setString("pt_last", if (s > 0) (t - 1).toString() else "0")
        }
        return false
    }
}
