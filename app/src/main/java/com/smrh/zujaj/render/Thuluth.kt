package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import com.smrh.zujaj.data.Calendars
import kotlin.math.min

/**
 * Decorative Arabic headings (day names, prayer names).
 *
 * 1) If real calligraphy artwork is bundled for the word — res/drawable/calli_<key> (vector XML or PNG/WebP),
 *    keys: day_mon … day_sun, fajr, sunrise, dhuhr, asr, maghrib, isha — that artwork is drawn, tinted
 *    in the widget colour. This is how authentic compound thuluth pieces are used.
 * 2) Otherwise the word is set in Amiri Bold with a kashida and full tashkeel. No generated ornaments.
 */
object Thuluth {
    private const val TATWEEL = 'ـ'
    private const val NO_JOIN = "اأإآدذرزوؤةىءئ "

    private val keys: Map<String, String> = run {
        val days = listOf("day_mon", "day_tue", "day_wed", "day_thu", "day_fri", "day_sat", "day_sun")
        Calendars.daysAr.mapIndexed { i, d -> d to days[i] }.toMap() + mapOf(
            "الفجر" to "fajr", "الشروق" to "sunrise", "الظهر" to "dhuhr", "العصر" to "asr", "المغرب" to "maghrib", "العشاء" to "isha",
        )
    }
    private val resCache = HashMap<String, Int>()

    /** Bundled artwork for this word, or 0. */
    fun artFor(r: RenderCtx, word: String): Int {
        val k = keys[word.trim()] ?: return 0
        return resCache.getOrPut(k) { r.context.resources.getIdentifier("calli_$k", "drawable", r.context.packageName) }
    }

    /** Inserts a kashida near the end of the word ("الجمعة" → "الجمـــعة"); tashkeel marks stay on their letters. */
    fun stretch(s: String): String {
        if (' ' in s || TATWEEL in s) return s
        fun isLetter(c: Char) = c in '\u0621'..'\u064A'
        val letters = s.indices.filter { isLetter(s[it]) }
        if (letters.size < 4) return s
        for (li in (letters.size - 3) downTo 1) {
            val i = letters[li]
            if (s[i] in NO_JOIN) continue
            // put the stretch after this letter's own marks
            var j = i + 1
            while (j < s.length && !isLetter(s[j])) j++
            return s.substring(0, j) + "ـــ" + s.substring(j)
        }
        return s
    }

    fun draw(r: RenderCtx, s0: String, cx: Float, cy: Float, size0: Float, color: Int, maxW: Float, maxH: Float) {
        val word = s0.trim()
        val art = artFor(r, word)
        if (art != 0 && drawArt(r, art, cx, cy, size0, color, maxW, maxH)) return
        // fallback: clean calligraphic type, kashida + tashkeel, nothing generated
        val s = stretch(Calendars.tashkeel(word))
        val p = r.paint(color, size0, F.AMIRI, Paint.Align.LEFT)
        val b = Rect()
        p.getTextBounds(s, 0, s.length, b)
        var k = 1f
        if (maxW > 0 && b.width() > maxW) k = min(k, maxW / b.width())
        if (maxH > 0 && b.height() > maxH) k = min(k, maxH / b.height())
        if (k < 1f) { p.textSize *= k; p.getTextBounds(s, 0, s.length, b) }
        r.canvas.drawText(s, cx - b.exactCenterX(), cy - b.exactCenterY(), p)
    }

    private fun drawArt(r: RenderCtx, res: Int, cx: Float, cy: Float, size: Float, color: Int, maxW: Float, maxH: Float): Boolean {
        val d = runCatching { androidx.core.content.ContextCompat.getDrawable(r.context, res) }.getOrNull()?.mutate() ?: return false
        val iw = d.intrinsicWidth.coerceAtLeast(1).toFloat(); val ih = d.intrinsicHeight.coerceAtLeast(1).toFloat()
        val boxW = if (maxW > 0) maxW else size * 3f
        val boxH = if (maxH > 0) maxH else size
        val k = min(boxW / iw, boxH / ih)
        val w = iw * k; val h = ih * k
        d.setBounds((cx - w / 2).toInt(), (cy - h / 2).toInt(), (cx + w / 2).toInt(), (cy + h / 2).toInt())
        d.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        d.draw(r.canvas)
        return true
    }
}
