package com.smrh.zujaj.render

import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

// =====================================================================================  PHOTO + VERSE
/** Daily photo with a verse, a dhikr or the user's own words over it. */
object VerseR {
    /** Short, well-known ayat with their place in the Qur'an. */
    val ayat = listOf(
        "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا" to "الشرح ٥",
        "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ" to "الرعد ٢٨",
        "فَاذْكُرُونِي أَذْكُرْكُمْ" to "البقرة ١٥٢",
        "وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ" to "الطلاق ٣",
        "لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا" to "التوبة ٤٠",
        "وَقُل رَّبِّ زِدْنِي عِلْمًا" to "طه ١١٤",
        "رَبِّ اشْرَحْ لِي صَدْرِي" to "طه ٢٥",
        "إِنَّ اللَّهَ مَعَ الصَّابِرِينَ" to "البقرة ١٥٣",
        "وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ" to "الحديد ٤",
        "ادْعُونِي أَسْتَجِبْ لَكُمْ" to "غافر ٦٠",
        "وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ" to "الضحى ٥",
        "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ" to "آل عمران ١٧٣",
        "وَمَا تَوْفِيقِي إِلَّا بِاللَّهِ" to "هود ٨٨",
        "إِنَّ رَبِّي قَرِيبٌ مُّجِيبٌ" to "هود ٦١",
    )

    /** (text, source) for today, by the widget's choice: 0 dhikr · 1 ayah · 2 own text · 3 nothing. */
    fun words(r: RenderCtx): Pair<String, String>? = when (r.cfg.variant) {
        1 -> ayat[(r.today.toEpochDay() % ayat.size).toInt()]
        2 -> r.cfg.text.takeIf { it.isNotBlank() }?.let { it to "" }
        3 -> null
        else -> IslamicR.adhkar[(r.today.toEpochDay() % IslamicR.adhkar.size).toInt()] to ""
    }

    private fun photo(r: RenderCtx): android.graphics.Bitmap? {
        val ps = r.cfg.photos
        if (ps.isEmpty()) return null
        return AppIcons.loadFile(ps[(r.today.toEpochDay() % ps.size).toInt()], 1000)
    }

    private fun base(r: RenderCtx, rect: RectF, rad: Float): Boolean {
        val b = photo(r)
        Renderer.photoLayer(r, b, rect, rad)
        if (b == null) { r.hit(rect, HitAction.Cmd("edit")); return false }
        return true
    }

    private fun label(r: RenderCtx) = when (r.cfg.variant) { 1 -> r.t("Verse of the day", "آية اليوم"); 2 -> ""; else -> r.t("Dhikr of the day", "ذكر اليوم") }

    /** Wide: photo, deep fade at the bottom, the words centred in Amiri. */
    fun wide(r: RenderCtx) {
        val rect = RectF(0f, 0f, r.w, r.h)
        val rad = Renderer.cornerPx(r)
        val has = base(r, rect, rad)
        val w = words(r) ?: return
        val g = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, r.h * 0.25f, 0f, r.h, intArrayOf(Color.TRANSPARENT, withAlpha(Color.BLACK, 120), withAlpha(Color.BLACK, 205)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        }
        if (r.cfg.bg) r.canvas.drawRoundRect(rect, rad, rad, g)
        val top = r.h * 0.42f
        val lbl = label(r)
        if (lbl.isNotBlank()) r.text(lbl, r.w / 2, top, 9.5f * r.u, withAlpha(Color.WHITE, 200), r.sansMed, Paint.Align.CENTER)
        r.paragraph(w.first, RectF(18 * r.u, top + 8 * r.u, r.w - 18 * r.u, r.h - (if (w.second.isNotBlank()) 24 else 12) * r.u), 22 * r.u * r.s1, Color.WHITE, F.AMIRI, lineMult = 1.2f)
        if (w.second.isNotBlank()) r.text(w.second, r.w / 2, r.h - 13 * r.u, 9 * r.u, withAlpha(Color.WHITE, 170), r.sans, Paint.Align.CENTER)
        if (!has) return
    }

    /** Small: photo with a frosted glass caption card at the bottom. */
    fun square(r: RenderCtx) {
        val rect = RectF(0f, 0f, r.w, r.h)
        val rad = Renderer.cornerPx(r)
        base(r, rect, rad)
        val w = words(r) ?: return
        val m = 8 * r.u
        val card = RectF(m, r.h * 0.52f, r.w - m, r.h - m)
        val cr = min(16 * r.u, rad - m * 0.5f).coerceAtLeast(6 * r.u)
        Renderer.liquidGlass(r.canvas, card, cr, r.u, Color.BLACK, 255)
        r.canvas.drawRoundRect(card, cr, cr, r.fillPaint(withAlpha(Color.BLACK, 70)))
        r.paragraph(w.first, RectF(card.left + 8 * r.u, card.top + 6 * r.u, card.right - 8 * r.u, card.bottom - 6 * r.u), 15 * r.u * r.s1, Color.WHITE, F.AMIRI, lineMult = 1.15f)
    }

    /** Large: photo on top, words and today's date on a soft panel below. */
    fun large(r: RenderCtx) {
        val rad = Renderer.cornerPx(r)
        val photoR = RectF(0f, 0f, r.w, r.h * 0.62f)
        r.canvas.save()
        r.canvas.clipPath(Path().roundRect(RectF(0f, 0f, r.w, r.h), rad))
        base(r, photoR, 0f)
        val fade = Paint().apply { shader = LinearGradient(0f, photoR.bottom - 40 * r.u, 0f, photoR.bottom, Color.TRANSPARENT, r.cfg.bgColor or 0xFF000000.toInt(), Shader.TileMode.CLAMP) }
        r.canvas.drawRect(0f, photoR.bottom - 40 * r.u, r.w, photoR.bottom + 1, fade)
        r.canvas.restore()
        val w = words(r)
        val lbl = label(r)
        val y0 = photoR.bottom + 6 * r.u
        if (lbl.isNotBlank()) r.text(lbl, r.w / 2, y0, 9 * r.u, r.c2, r.sansMed, Paint.Align.CENTER)
        if (w != null) r.paragraph(w.first, RectF(r.pad + 8 * r.u, y0 + 8 * r.u, r.w - r.pad - 8 * r.u, r.h - 22 * r.u), 20 * r.u * r.s1, r.c1, F.AMIRI, lineMult = 1.2f)
        val foot = if (w != null && w.second.isNotBlank()) w.second + "  ·  " + Kit.gregShort(r) else Kit.greg(r)
        r.text(foot, r.w / 2, r.h - 12 * r.u, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
    }
}

// =====================================================================================  EVENTS
/** Countdowns to Ramadan, the Eids, a birthday, exams, New Year or any date. */
object EventR {
    data class Event(val title: String, val date: LocalDate, val icon: Int)

    private fun nextHijri(from: LocalDate, month: Int, day: Int): LocalDate {
        var d = from
        repeat(400) {
            val h = Calendars.hijri(d)
            if (h.month == month && h.day == day) return d
            d = d.plusDays(1)
        }
        return from
    }

    private fun nextYearly(from: LocalDate, month: Int, day: Int): LocalDate {
        val dd = min(day, java.time.YearMonth.of(from.year, month).lengthOfMonth())
        val t = LocalDate.of(from.year, month, dd)
        return if (t.isBefore(from)) LocalDate.of(from.year + 1, month, min(day, java.time.YearMonth.of(from.year + 1, month).lengthOfMonth())) else t
    }

    /** icon: 0 crescent · 1 lantern · 2 cake · 3 book · 4 star · 5 sparkle */
    fun event(r: RenderCtx): Event {
        val today = r.today
        val picked = if (r.cfg.targetMillis > 0) Instant.ofEpochMilli(r.cfg.targetMillis).atZone(ZoneId.systemDefault()).toLocalDate() else null
        return when (r.cfg.event) {
            "ramadan" -> Event(r.cfg.title.ifBlank { r.t("Ramadan", "رمضان") }, nextHijri(today, 9, 1), 1)
            "fitr" -> Event(r.cfg.title.ifBlank { r.t("Eid al-Fitr", "عيد الفطر") }, nextHijri(today, 10, 1), 0)
            "adha" -> Event(r.cfg.title.ifBlank { r.t("Eid al-Adha", "عيد الأضحى") }, nextHijri(today, 12, 10), 0)
            "newyear" -> Event(r.cfg.title.ifBlank { r.t("New Year", "السنة الجديدة") }, LocalDate.of(today.year + 1, 1, 1), 5)
            "birthday" -> Event(r.cfg.title.ifBlank { r.t("Birthday", "عيد الميلاد") }, picked?.let { nextYearly(today, it.monthValue, it.dayOfMonth) } ?: nextYearly(today, today.monthValue, today.dayOfMonth), 2)
            "exams" -> Event(r.cfg.title.ifBlank { r.t("Exams", "الامتحانات") }, picked ?: today.plusDays(30), 3)
            else -> Event(r.cfg.title.ifBlank { if (picked != null) r.t("My day", "يومي") else r.t("New Year", "السنة الجديدة") },
                picked ?: LocalDate.of(today.year + 1, 1, 1), 4)
        }
    }

    fun daysLeft(r: RenderCtx, e: Event) = ChronoUnit.DAYS.between(r.today, e.date).coerceAtLeast(0)

    /** Progress through the waiting time (a year for yearly events, else from when it was set). */
    private fun progress(r: RenderCtx, e: Event): Float {
        val left = daysLeft(r, e).toFloat()
        val span = when (r.cfg.event) { "exams", "" -> 120f; else -> 365f }
        return (1f - left / span).coerceIn(0.02f, 1f)
    }

    private fun dateLine(r: RenderCtx, d: LocalDate) =
        if (r.ar) "${Calendars.dayName(d, true)} ${r.n(d.dayOfMonth)} ${Calendars.monthName(d.monthValue, true)}"
        else "${Calendars.dayName(d, false).take(3)}, ${d.dayOfMonth} ${Calendars.monthName(d.monthValue, false).take(3)}"

    private fun daysWord(r: RenderCtx, n: Long) = if (!r.ar) (if (n == 1L) "day" else "days") else when {
        n == 0L -> "اليوم"; n == 1L -> "يوم"; n == 2L -> "يومين"; n in 3..10 -> "أيام"; else -> "يوم"
    }

    fun icon(r: RenderCtx, kind: Int, cx: Float, cy: Float, s: Float, color: Int) {
        val c = r.canvas
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        when (kind) {
            0 -> Glyphs.moon(c, cx, cy, s, color)
            1 -> { // lantern
                val st = r.strokePaint(color, s * 0.07f)
                c.drawLine(cx, cy - s * 0.48f, cx, cy - s * 0.36f, st)
                val body = Path().apply {
                    moveTo(cx - s * 0.14f, cy - s * 0.34f); lineTo(cx + s * 0.14f, cy - s * 0.34f)
                    lineTo(cx + s * 0.26f, cy - s * 0.1f); lineTo(cx + s * 0.2f, cy + s * 0.3f)
                    lineTo(cx - s * 0.2f, cy + s * 0.3f); lineTo(cx - s * 0.26f, cy - s * 0.1f); close()
                }
                c.drawPath(body, p)
                c.drawRect(cx - s * 0.12f, cy + s * 0.3f, cx + s * 0.12f, cy + s * 0.4f, p)
                c.drawCircle(cx, cy - s * 0.02f, s * 0.08f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(Color.BLACK, 90) })
            }
            2 -> { // cake
                c.drawRoundRect(RectF(cx - s * 0.34f, cy - s * 0.04f, cx + s * 0.34f, cy + s * 0.34f), s * 0.06f, s * 0.06f, p)
                c.drawRoundRect(RectF(cx - s * 0.26f, cy - s * 0.24f, cx + s * 0.26f, cy - s * 0.02f), s * 0.05f, s * 0.05f, p)
                c.drawRect(cx - s * 0.025f, cy - s * 0.4f, cx + s * 0.025f, cy - s * 0.24f, p)
                c.drawOval(RectF(cx - s * 0.05f, cy - s * 0.52f, cx + s * 0.05f, cy - s * 0.4f), p)
            }
            3 -> { // open book
                val l = Path().apply { moveTo(cx, cy - s * 0.22f); quadTo(cx - s * 0.2f, cy - s * 0.32f, cx - s * 0.42f, cy - s * 0.26f); lineTo(cx - s * 0.42f, cy + s * 0.26f); quadTo(cx - s * 0.2f, cy + s * 0.2f, cx, cy + s * 0.3f); close() }
                val rr = Path().apply { moveTo(cx, cy - s * 0.22f); quadTo(cx + s * 0.2f, cy - s * 0.32f, cx + s * 0.42f, cy - s * 0.26f); lineTo(cx + s * 0.42f, cy + s * 0.26f); quadTo(cx + s * 0.2f, cy + s * 0.2f, cx, cy + s * 0.3f); close() }
                c.drawPath(l, p); c.drawPath(rr, Paint(p).apply { alpha = (Color.alpha(color) * 0.75f).toInt() })
            }
            5 -> Glyphs.sparkle(c, cx, cy, s, color)
            else -> { // star
                val path = Path()
                for (i in 0 until 10) {
                    val rad = if (i % 2 == 0) s * 0.42f else s * 0.18f
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val x = cx + (rad * Math.cos(a)).toFloat(); val y = cy + (rad * Math.sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close(); c.drawPath(path, p)
            }
        }
    }

    /** Small: a glowing progress ring around the number of days. */
    fun ring(r: RenderCtx) {
        val e = event(r)
        val n = daysLeft(r, e)
        val cx = r.w / 2; val cy = r.h * 0.53f
        val rad = min(r.w, r.h) * 0.34f
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        val th = 7 * r.u
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 30), th))
        val sweep = 360f * progress(r, e)
        val grad = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = th; strokeCap = Paint.Cap.ROUND
            shader = SweepGradient(cx, cy, intArrayOf(r.cfg.accent2, r.accent, r.cfg.accent2), floatArrayOf(0f, 0.5f, 1f)).apply {
                setLocalMatrix(android.graphics.Matrix().apply { setRotate(-90f, cx, cy) })
            }
        }
        val glow = Paint(grad).apply { maskFilter = BlurMaskFilter(th * 1.2f, BlurMaskFilter.Blur.NORMAL); alpha = 120 }
        r.canvas.drawArc(rect, -90f, sweep, false, glow)
        r.canvas.drawArc(rect, -90f, sweep, false, grad)
        r.text(e.title, r.w / 2, r.pad + 3 * r.u, 11 * r.u * r.s2, r.c2, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        if (n == 0L) {
            r.textTight(r.t("Today", "النهارده"), cx, cy, 20 * r.u * r.s1, r.c1, r.sansBold, maxW = rad * 1.5f)
        } else {
            r.textTight(r.n(n.toString()), cx, cy - 4 * r.u, 34 * r.u * r.s1, r.c1, F.SANS_BOLD, maxW = rad * 1.45f, maxH = rad * 0.75f)
            r.text(daysWord(r, n), cx, cy + rad * 0.5f, 9.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        }
    }

    /** Wide: icon + title + date on the side, big liquid number on a glass pill. */
    fun card(r: RenderCtx) {
        val e = event(r)
        val n = daysLeft(r, e)
        val left = r.pad + 4 * r.u
        val ar = r.ar
        val tx = if (ar) r.w - left else left
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        icon(r, e.icon, if (ar) r.w - left - 14 * r.u else left + 14 * r.u, r.h * 0.26f, 28 * r.u, r.accent)
        r.text(e.title, tx, r.h * 0.5f, 19 * r.u * r.s1, r.c1, if (ar) F.CAIRO_BOLD else F.SANS_BOLD, al, maxW = r.w * 0.5f)
        r.text(dateLine(r, e.date), tx, r.h * 0.68f, 10.5f * r.u * r.s2, r.c2, r.sans, al, maxW = r.w * 0.5f)
        // progress
        val bw = r.w * 0.42f
        val bx = if (ar) r.w - left - bw else left
        val by = r.h * 0.84f
        r.canvas.drawLine(bx, by, bx + bw, by, r.strokePaint(withAlpha(r.c1, 34), 4 * r.u))
        val f = progress(r, e)
        val pp = r.strokePaint(r.accent, 4 * r.u).apply { shader = LinearGradient(bx, 0f, bx + bw, 0f, r.cfg.accent2, r.accent, Shader.TileMode.CLAMP) }
        if (ar) r.canvas.drawLine(bx + bw, by, bx + bw - bw * f, by, pp) else r.canvas.drawLine(bx, by, bx + bw * f, by, pp)
        // number pill
        val pw = r.w * 0.38f; val ph = r.h - 2 * r.pad
        val pl = if (ar) r.pad else r.w - r.pad - pw
        val pill = RectF(pl, r.pad, pl + pw, r.pad + ph)
        Renderer.liquidGlass(r.canvas, pill, 22 * r.u, r.u, r.c1, 255)
        if (n == 0L) r.textTight(r.t("Today", "النهارده"), pill.centerX(), pill.centerY(), 22 * r.u, r.c1, r.sansBold, maxW = pw * 0.8f)
        else {
            r.textTight(r.n(n.toString()), pill.centerX(), pill.centerY() - 7 * r.u, 46 * r.u * r.s1, r.c1, F.SANS_BOLD, maxW = pw * 0.82f, maxH = ph * 0.5f)
            r.text(daysWord(r, n), pill.centerX(), pill.bottom - 16 * r.u, 10 * r.u, r.c2, r.sansMed, Paint.Align.CENTER)
        }
    }

    /** Wide: flip-clock tiles for the days, label underneath. */
    fun flip(r: RenderCtx) {
        val e = event(r)
        val n = daysLeft(r, e)
        val digits = r.n(n.toString().padStart(2, '0'))
        val tiles = digits.length
        val gap = 8 * r.u
        val th = r.h * 0.5f
        val tw = min(th * 0.74f, (r.w * 0.62f - gap * (tiles - 1)) / tiles)
        val total = tw * tiles + gap * (tiles - 1)
        val x0 = (r.w - total) / 2
        val ty = r.h * 0.12f
        for (i in 0 until tiles) {
            val rc = RectF(x0 + i * (tw + gap), ty, x0 + i * (tw + gap) + tw, ty + th)
            val rad = 9 * r.u
            r.canvas.drawRoundRect(rc, rad, rad, r.fillPaint(withAlpha(Color.BLACK, 110)))
            val top = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, rc.top, 0f, rc.bottom, withAlpha(Color.WHITE, 26), withAlpha(Color.WHITE, 6), Shader.TileMode.CLAMP) }
            r.canvas.drawRoundRect(rc, rad, rad, top)
            r.textTight(digits[i].toString(), rc.centerX(), rc.centerY(), th * 0.78f, r.c1, F.SANS_BOLD, maxW = tw * 0.8f)
            // the hinge
            r.canvas.drawLine(rc.left, rc.centerY(), rc.right, rc.centerY(), r.strokePaint(withAlpha(Color.BLACK, 120), 1.6f * r.u, false))
            r.canvas.drawCircle(rc.left + 1.5f * r.u, rc.centerY(), 2 * r.u, r.fillPaint(withAlpha(Color.BLACK, 140)))
            r.canvas.drawCircle(rc.right - 1.5f * r.u, rc.centerY(), 2 * r.u, r.fillPaint(withAlpha(Color.BLACK, 140)))
        }
        r.text(r.t("days until ", "يوم على ") + e.title, r.w / 2, ty + th + 16 * r.u, 13 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.text(dateLine(r, e.date), r.w / 2, ty + th + 33 * r.u, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    /** Banner: icon, title and a glowing pill with the days. */
    fun banner(r: RenderCtx) {
        val e = event(r)
        val n = daysLeft(r, e)
        val ar = r.ar
        val cy = r.h / 2
        val ix = if (ar) r.w - r.pad - 16 * r.u else r.pad + 16 * r.u
        r.circle(ix, cy, 17 * r.u, withAlpha(r.accent, 46))
        icon(r, e.icon, ix, cy, 22 * r.u, r.accent)
        val tx = if (ar) ix - 26 * r.u else ix + 26 * r.u
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        r.text(e.title, tx, cy - 8 * r.u, 15 * r.u * r.s1, r.c1, if (ar) F.CAIRO_BOLD else F.SANS_BOLD, al, maxW = r.w * 0.45f)
        r.text(dateLine(r, e.date), tx, cy + 10 * r.u, 9.5f * r.u, r.c2, r.sans, al, maxW = r.w * 0.45f)
        val label = if (n == 0L) r.t("Today", "النهارده") else r.n(n.toString()) + " " + daysWord(r, n)
        val p = r.paint(Color.WHITE, 14 * r.u, r.sansBold)
        val pw = p.measureText(label) + 26 * r.u
        val pl = if (ar) r.pad else r.w - r.pad - pw
        val pill = RectF(pl, cy - 15 * r.u, pl + pw, cy + 15 * r.u)
        val g = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(pill.left, 0f, pill.right, 0f, r.cfg.accent2, r.accent, Shader.TileMode.CLAMP) }
        r.canvas.drawRoundRect(RectF(pill.left, pill.top + 3 * r.u, pill.right, pill.bottom + 3 * r.u), pill.height() / 2, pill.height() / 2,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(r.accent, 90); maskFilter = BlurMaskFilter(6 * r.u, BlurMaskFilter.Blur.NORMAL) })
        r.canvas.drawRoundRect(pill, pill.height() / 2, pill.height() / 2, g)
        r.text(label, pill.centerX(), cy, 14 * r.u, Color.WHITE, r.sansBold, Paint.Align.CENTER)
    }
}

// =====================================================================================  TIME OF DAY
/** Background colours that follow the sky: dawn, day, golden hour, sunset, night. */
object DayTint {
    fun palette(r: RenderCtx): Pair<Int, Int> {
        val now = r.now.toInstant().toEpochMilli()
        val d = runCatching { Kit.day(r) }.getOrNull() ?: return 0xFF0F2027.toInt() to 0xFF2C5364.toInt()
        fun at(k: String) = d.list.firstOrNull { it.key == k }?.time
        val fajr = at("fajr"); val sunrise = at("sunrise"); val asr = at("asr"); val maghrib = at("maghrib"); val isha = at("isha")
        if (fajr == null || sunrise == null || asr == null || maghrib == null || isha == null) return 0xFF1E3C72.toInt() to 0xFF2A5298.toInt()
        return when {
            now < fajr -> 0xFF0B1026.toInt() to 0xFF1D2B53.toInt()              // night
            now < sunrise -> 0xFF2B2D5C.toInt() to 0xFFC0607A.toInt()          // dawn
            now < asr -> 0xFF1F6FD1.toInt() to 0xFF5EB6F0.toInt()              // day
            now < maghrib - 45 * 60_000L -> 0xFFD9822B.toInt() to 0xFFF2B15A.toInt() // afternoon gold
            now < maghrib + 25 * 60_000L -> 0xFF6A2C70.toInt() to 0xFFF08A5D.toInt() // sunset
            now < isha -> 0xFF2E1A47.toInt() to 0xFF4B2A6B.toInt()            // dusk
            else -> 0xFF0B1026.toInt() to 0xFF1D2B53.toInt()                   // night
        }
    }
}
