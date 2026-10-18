package com.smrh.zujaj.render

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.Gravity
import com.smrh.zujaj.catalog.WidgetStyle
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.model.AppRef
import com.smrh.zujaj.model.ContactRef
import com.smrh.zujaj.model.Lang
import com.smrh.zujaj.model.Numerals
import com.smrh.zujaj.model.WidgetConfig
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

/**
 * Fonts that exist as live TextClock slots in the widget layout (res/font/clk_*).
 * Latin faces always draw 0-9 (even when the phone is in Arabic); AR_* faces always draw ٠-٩.
 * [kind]: 0 sans · 1 serif · 2 condensed · 3 display — used to pick the matching Arabic face.
 */
enum class LiveFont(val f: F, val res: String, val label: String, val kind: Int, val pro: Boolean = false, val slots: Int = 2, val arabic: Boolean = false) {
    SANS(F.CLK_LABEL, "clk_label", "Manrope", 0, slots = 6),
    SANS_BOLD(F.CLK_MANROPE, "clk_manrope", "Manrope Bold", 0, slots = 3),
    ARCHIVO(F.CLK_ARCHIVO, "clk_archivo", "Archivo Black", 0),
    ANTON(F.CLK_ANTON, "clk_anton", "Anton", 2),
    BEBAS(F.CLK_BEBAS, "clk_bebas", "Bebas", 2),
    SHOULDERS(F.CLK_SHOULDERS, "clk_shoulders", "Shoulders", 2),
    OSWALD(F.CLK_OSWALD, "clk_oswald", "Oswald Light", 2),
    PLAYFAIR(F.CLK_PLAYFAIR, "clk_playfair", "Playfair", 1),
    BODONI(F.CLK_BODONI, "clk_bodoni", "Bodoni", 1),
    DMSERIF(F.CLK_DMSERIF, "clk_dmserif", "DM Serif", 1),
    ABRIL(F.CLK_ABRIL, "clk_abril", "Abril", 1, pro = true),
    COMFORTAA(F.CLK_COMFORTAA, "clk_comfortaa", "Comfortaa", 0),
    RIGHTEOUS(F.CLK_RIGHTEOUS, "clk_righteous", "Righteous", 3),
    SYNE(F.CLK_SYNE, "clk_syne", "Syne", 3, pro = true),
    UNBOUNDED(F.CLK_UNBOUNDED, "clk_unbounded", "Unbounded", 3, pro = true),
    ORBITRON(F.CLK_ORBITRON, "clk_orbitron", "Orbitron", 3, pro = true),
    MICHROMA(F.CLK_MICHROMA, "clk_michroma", "Michroma", 3, pro = true),
    POIRET(F.CLK_POIRET, "clk_poiret", "Poiret", 3, pro = true),
    SIXCAPS(F.CLK_SIXCAPS, "clk_sixcaps", "Six Caps", 2, pro = true),
    AR_CAIRO(F.CLK_AR_CAIRO, "clk_ar_cairo", "القاهرة", 0, slots = 8, arabic = true),
    AR_AMIRI(F.CLK_AR_AMIRI, "clk_ar_amiri", "أميري", 1, arabic = true),
    AR_KUFI(F.CLK_AR_KUFI, "clk_ar_kufi", "كوفي", 2, arabic = true),
    AR_RAKKAS(F.CLK_AR_RAKKAS, "clk_ar_rakkas", "رقاص", 3, pro = true, arabic = true);

    fun toArabic(): LiveFont = if (arabic) this else when (kind) { 1 -> AR_AMIRI; 2 -> AR_KUFI; 3 -> AR_RAKKAS; else -> AR_CAIRO }
    fun toLatin(): LiveFont = if (!arabic) this else when (this) { AR_AMIRI -> PLAYFAIR; AR_KUFI -> OSWALD; AR_RAKKAS -> RIGHTEOUS; else -> SANS_BOLD }

    companion object {
        fun byName(n: String): LiveFont? = entries.firstOrNull { it.name == n }
        val latinChoices get() = entries.filter { !it.arabic && it != SANS }
        val arabicChoices get() = entries.filter { it.arabic }
    }
}

data class HandStyle(
    val kind: Int = 0, // 0 thin classic, 1 thick rounded, 2 tapered, 3 line+ring
    val hour: Int = Color.WHITE,
    val minute: Int = Color.WHITE,
    val second: Int = 0xFFE5484D.toInt(),
    val seconds: Boolean = true,
    val hourLen: Float = 0.50f,
    val minLen: Float = 0.78f,
    val width: Float = 0.045f,
    val center: Int = Color.WHITE,
)

sealed class Live {
    abstract val rect: RectF
}

data class LiveClock(
    override val rect: RectF,
    val font: LiveFont,
    val sizePx: Float,
    val color: Int,
    val gravity: Int,
    val pattern: String,
    val zone: String?,
    val spacing: Float = 0f,
) : Live()

data class LiveChrono(
    override val rect: RectF,
    val sizePx: Float,
    val color: Int,
    val gravity: Int,
    val target: Long,
    val prefix: String,
    val bold: Boolean = false,
    val arabic: Boolean = false,
) : Live()

data class LiveAnalog(override val rect: RectF, val hands: HandStyle, val zone: String?) : Live()

sealed class HitAction {
    data class App(val app: AppRef) : HitAction()
    data class Cmd(val cmd: String, val arg: String = "") : HitAction()
    data class Launch(val intent: Intent) : HitAction()
    data class Contact(val contact: ContactRef) : HitAction()
}

data class Hit(val rect: RectF, val action: HitAction)

/** Data snapshot handed to renderers. Everything is optional / lazily read. */
class RenderData(
    val now: ZonedDateTime = ZonedDateTime.now(),
)

class RenderCtx(
    val context: Context,
    val bitmap: Bitmap,
    val style: WidgetStyle,
    val cfg: WidgetConfig,
    val preview: Boolean,
    /** px per dp on the device */
    val density: Float,
    val data: RenderData = RenderData(),
) {
    val canvas = Canvas(bitmap)
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    val lives = mutableListOf<Live>()
    /**
     * Draw live texts into the picture instead of using TextClock/Chronometer. Used where the
     * host ignores the app's fonts (Samsung lock screen) but Arabic-Indic digits are wanted;
     * those widgets are then redrawn every minute.
     */
    var bakeLive = false
    val hits = mutableListOf<Hit>()

    /** One "design unit": scales the nominal design (dp) to the real widget size. */
    val u: Float = min(w / style.shape.dw, h / style.shape.dh)
    val pad: Float = cfg.padding * u
    val now: ZonedDateTime get() = data.now
    val today: LocalDate get() = now.toLocalDate()

    val ar: Boolean = when (cfg.language) {
        Lang.AR -> true
        Lang.EN -> false
        Lang.AUTO -> com.smrh.zujaj.data.Prefs.isArabicUi
    }
    val arDigits: Boolean = when (cfg.numerals) {
        Numerals.ARABIC -> true
        Numerals.LATIN -> false
        Numerals.AUTO -> false
    }
    val locale: Locale = if (ar) Locale.forLanguageTag("ar-EG-u-nu-latn") else Locale.ENGLISH

    val c1 get() = cfg.color1
    val c2 get() = cfg.color2
    val accent get() = cfg.accent
    val s1 get() = cfg.size1 / 100f
    val s2 get() = cfg.size2 / 100f

    /** Main UI font for normal labels depending on language. */
    val sans: F get() = if (ar || arDigits) F.CAIRO else F.SANS
    val sansBold: F get() = if (ar || arDigits) F.CAIRO_BOLD else F.SANS_BOLD
    val sansMed: F get() = if (ar || arDigits) F.CAIRO_BOLD else F.SANS_MED

    fun t(en: String, arS: String) = if (ar) arS else en
    fun n(s: String) = if (arDigits) Calendars.toArabicDigits(s) else s
    fun n(i: Int) = n(i.toString())
    fun two(i: Int) = n(i.toString().padStart(2, '0'))

    fun tf(f: F): Typeface = Fonts.get(context, f)

    /** Calligraphy face chosen by the user for Arabic display words. */
    val calliFont: F get() = when (cfg.calli) { 1 -> F.RUQAA; 2 -> F.THULUTH; 3 -> F.AMIRI; else -> F.KUFI_BOLD }
    /** Thuluth look = bold naskh + full tashkeel + the small decorative marks of the script. */
    private val thuluthDeco get() = cfg.calli == 0
    private fun withTashkeel(s: String, font: F) =
        if (font == F.THULUTH || font == F.AMIRI) Calendars.tashkeel(s) else s

    // ------------------------------------------------------------------ paint helpers
    fun paint(color: Int = c1, size: Float = 14 * u, font: F = sans, align: Paint.Align = Paint.Align.LEFT, spacing: Float = 0f) =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            textSize = size
            typeface = tf(font)
            textAlign = align
            letterSpacing = spacing + cfg.spacing / 100f
        }

    fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    fun strokePaint(color: Int, width: Float, round: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = width
        if (round) strokeCap = Paint.Cap.ROUND
    }

    /** Draw text vertically centred on [cy]. */
    fun text(s: String, x: Float, cy: Float, size: Float, color: Int = c1, font: F = sans, align: Paint.Align = Paint.Align.LEFT, spacing: Float = 0f, maxW: Float = 0f): Float {
        val p = paint(color, size, font, align, spacing)
        if (maxW > 0) {
            val mw = p.measureText(s)
            if (mw > maxW) p.textSize = p.textSize * maxW / mw
        }
        val fm = p.fontMetrics
        canvas.drawText(s, x, cy - (fm.ascent + fm.descent) / 2f, p)
        return p.measureText(s)
    }

    /** Text using its real glyph bounds to centre (better for big numerals). */
    fun textTight(s0: String, cx: Float, cy: Float, size: Float, color: Int = c1, font: F = sans, spacing: Float = 0f, maxW: Float = 0f, maxH: Float = 0f) {
        if (font == F.KUFI_BOLD && thuluthDeco && font == calliFont && s0.any { it in '\u0621'..'\u064A' }) {
            Thuluth.draw(this, s0, cx, cy, size, color, maxW, maxH)
            return
        }
        val s = withTashkeel(s0, font)
        val p = paint(color, size, font, Paint.Align.LEFT, spacing)
        fitPaint(p, s, maxW, maxH)
        val b = android.graphics.Rect()
        p.getTextBounds(s, 0, s.length, b)
        val width = p.measureText(s)
        canvas.drawText(s, cx - width / 2f, cy - b.exactCenterY(), p)
    }

    fun fitPaint(p: Paint, s: String, maxW: Float, maxH: Float) {
        if (maxW > 0) {
            val mw = p.measureText(s)
            if (mw > maxW) p.textSize *= maxW / mw
        }
        if (maxH > 0) {
            val b = android.graphics.Rect()
            p.getTextBounds(s, 0, s.length, b)
            if (b.height() > maxH) p.textSize *= maxH / b.height()
        }
    }

    fun measure(s: String, size: Float, font: F = sans, spacing: Float = 0f) = paint(size = size, font = font, spacing = spacing).measureText(s)

    /** Largest text size (<= max) making [s] fit in [maxW] x [maxH]. */
    fun fit(s: String, font: F, maxW: Float, maxH: Float, max: Float, spacing: Float = 0f): Float {
        val p = paint(size = max, font = font, spacing = spacing)
        fitPaint(p, s, maxW, maxH)
        return p.textSize
    }

    fun roundRect(r: RectF, radius: Float, color: Int) = canvas.drawRoundRect(r, radius, radius, fillPaint(color))
    fun circle(cx: Float, cy: Float, r: Float, color: Int) = canvas.drawCircle(cx, cy, r, fillPaint(color))

    fun progressBar(x: Float, y: Float, width: Float, frac: Float, color: Int = c1, track: Int = withAlpha(c1, 40), thick: Float = 3 * u) {
        val p = strokePaint(track, thick)
        canvas.drawLine(x, y, x + width, y, p)
        p.color = color
        val f = frac.coerceIn(0f, 1f)
        if (f > 0.005f) canvas.drawLine(x, y, x + width * f, y, p)
    }

    fun pill(r: RectF, color: Int) = canvas.drawRoundRect(r, r.height() / 2, r.height() / 2, fillPaint(color))

    /** Inner glass "tile" used inside widgets. */
    fun tile(r: RectF, radius: Float = 16 * u, alpha: Int = 22) {
        canvas.drawRoundRect(r, radius, radius, fillPaint(withAlpha(c1, alpha)))
    }

    fun sheen(r: RectF, radius: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(r.left, r.top, r.left, r.bottom, withAlpha(Color.WHITE, 30), withAlpha(Color.WHITE, 0), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(r, radius, radius, p)
    }

    // ------------------------------------------------------------------ live overlays
    private fun fmt(pattern: String, zone: String?): String {
        val f = SimpleDateFormat(pattern, locale)
        if (!zone.isNullOrBlank()) f.timeZone = TimeZone.getTimeZone(zone)
        return n(f.format(Date()))
    }

    /** Final face for a live text: user's font for the main time, then Arabic/Latin digits. */
    fun resolveFont(font: LiveFont, size: Float): LiveFont {
        var f = font
        if (size >= 18 * u) LiveFont.byName(cfg.font)?.let { f = it }
        return if (arDigits) f.toArabic() else f.toLatin()
    }

    fun liveFontToF(f: LiveFont) = f.f

    /** Time pattern honoring the 12/24h setting. */
    fun hm(): String = if (cfg.hour24) "HH:mm" else "hh:mm"
    fun hh(): String = if (cfg.hour24) "HH" else "hh"

    /**
     * A live, self-updating time text. In preview it is drawn directly; on the
     * home screen it becomes a TextClock positioned over the bitmap.
     * [cy] is the vertical centre; size is fitted to [maxW].
     */
    fun liveTime(
        pattern: String, cx: Float, cy: Float, size: Float, font: LiveFont,
        color: Int = c1, align: Paint.Align = Paint.Align.CENTER, zone: String? = null,
        maxW: Float = 0f, spacing: Float = 0f, sample: String? = null, maxH: Float = 0f,
    ) {
        val lf = resolveFont(font, size)
        val f = lf.f
        val shown = fmt(pattern, zone)
        val measureStr = sample ?: shown
        val numeric = measureStr.all { it.isDigit() || it == ':' || it == ' ' || it == '.' }
        // widest rendering of the sample (digits are not equally wide in every face)
        fun widest(pp: Paint): Float = if (!numeric) pp.measureText(measureStr)
            else ('0'..'9').maxOf { d -> pp.measureText(measureStr.map { if (it.isDigit()) d else it }.joinToString("")) }
        var sz = size
        if (maxW > 0) {
            val mw = widest(paint(size = sz, font = f, spacing = spacing))
            if (mw > maxW) sz *= maxW / mw
        }
        if (maxH > 0) {
            val b = android.graphics.Rect()
            paint(size = sz, font = f).getTextBounds("0123456789", 0, 10, b)
            if (b.height() > maxH) sz *= maxH / b.height()
        }
        val p = paint(color, sz, f, align, spacing)
        val fm = p.fontMetrics
        var width = widest(p) * 1.12f + 4 * u
        if (!numeric) width = maxOf(width, sz * 3.2f)
        val left = when (align) {
            Paint.Align.LEFT -> cx
            Paint.Align.RIGHT -> cx - width
            else -> cx - width / 2
        }
        // Numbers are centred on their real ink (lining digits), not on the font's line box,
        // so the time sits exactly in the middle instead of drifting down.
        val baseline = if (numeric) {
            val b = android.graphics.Rect()
            p.getTextBounds("0123456789", 0, 10, b)
            cy - b.exactCenterY()
        } else cy - (fm.ascent + fm.descent) / 2f
        val lineH = (fm.descent - fm.ascent)
        // TextView (includeFontPadding=false, centre gravity) puts the baseline at boxTop + (boxH - lineH)/2 - ascent
        val boxH = lineH * 1.06f
        val top = baseline + fm.ascent - (boxH - lineH) / 2f
        val rect = RectF(left, top, left + width, top + boxH)
        if (preview || bakeLive) {
            canvas.drawText(shown, cx, baseline, p)
        } else {
            val g = when (align) {
                Paint.Align.LEFT -> Gravity.START
                Paint.Align.RIGHT -> Gravity.END
                else -> Gravity.CENTER_HORIZONTAL
            } or Gravity.CENTER_VERTICAL
            lives += LiveClock(rect, lf, sz, color, g, pattern, zone, spacing + cfg.spacing / 100f)
        }
    }

    // ------------------------------------------------------------------ foreground effects
    /** Overlay drawn above live text (e.g. a fade). Only created when a widget asks for it. */
    var fx: Bitmap? = null
        private set
    var fxClip: Path? = null

    /**
     * Fades whatever is under [r] (including live clocks) towards [to] from top to bottom.
     * Works because the overlay sits above the TextClocks in the widget layout.
     */
    fun fade(r: RectF, to: Int, from: Float = 0f, until: Float = 1f) {
        val c = if (preview) canvas else {
            val b = fx ?: Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { fx = it }
            Canvas(b).also { cv -> fxClip?.let { cv.clipPath(it) } }
        }
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val y0 = r.top + r.height() * from
        val y1 = r.top + r.height() * until
        p.shader = LinearGradient(0f, y0, 0f, y1, withAlpha(to, 0), to, Shader.TileMode.CLAMP)
        c.drawRect(r.left, y0, r.right, r.bottom, p)
    }

    /** The colour of the widget background behind y, when it is an opaque-ish fill; null otherwise. */
    fun bgColorAt(y: Float): Int? {
        if (!cfg.bg) return null
        val a = cfg.bgAlpha.coerceIn(0, 100) * 255 / 100
        return when (cfg.fill) {
            com.smrh.zujaj.model.Fill.SOLID -> withAlpha(cfg.bgColor, a)
            com.smrh.zujaj.model.Fill.GRADIENT -> withAlpha(mix(cfg.bgColor, cfg.bgColor2, (y / h).coerceIn(0f, 1f) * 0.5f + 0.25f), a)
            com.smrh.zujaj.model.Fill.AURORA -> withAlpha(cfg.bgColor, a)
            else -> null
        }
    }

    /** Live countdown to [target] (epoch ms) e.g. "in 02:15:07". */
    fun liveCountdown(target: Long, prefix: String, x: Float, cy: Float, size: Float, color: Int = c1, align: Paint.Align = Paint.Align.LEFT, bold: Boolean = false) {
        val f = if (bold) sansBold else sansMed
        val p = paint(color, size, f, align)
        val left = (target - System.currentTimeMillis()).coerceAtLeast(0) / 1000
        if (left >= 86400) {
            // Long countdowns: static "in 80d 4h" (refreshed periodically)
            val s = prefix + n("${left / 86400}") + t("d ", "ي ") + n("${(left % 86400) / 3600}") + t("h", "س")
            val fm = p.fontMetrics
            canvas.drawText(s, x, cy - (fm.ascent + fm.descent) / 2f, p)
            return
        }
        val shown = if (bakeLive) ((left + 59) / 60).let { m -> prefix + n(String.format(Locale.US, "%d:%02d", m / 60, m % 60)) }
            else prefix + n(String.format(Locale.US, "%d:%02d:%02d", left / 3600, (left % 3600) / 60, left % 60))
        val fm = p.fontMetrics
        if (preview || bakeLive) {
            canvas.drawText(shown, x, cy - (fm.ascent + fm.descent) / 2f, p)
        } else {
            val width = p.measureText(shown) * 1.2f + 6 * u
            val l = when (align) {
                Paint.Align.LEFT -> x
                Paint.Align.RIGHT -> x - width
                else -> x - width / 2
            }
            val lineH = (fm.descent - fm.ascent) * 1.05f
            val g = when (align) {
                Paint.Align.LEFT -> Gravity.START
                Paint.Align.RIGHT -> Gravity.END
                else -> Gravity.CENTER_HORIZONTAL
            } or Gravity.CENTER_VERTICAL
            lives += LiveChrono(RectF(l, cy - lineH / 2, l + width, cy + lineH / 2), size, color, g, target, prefix, bold, arDigits)
        }
    }

    /** Live analog hands in the square [r]. */
    fun liveAnalog(r: RectF, hands: HandStyle, zone: String? = null) {
        if (preview) {
            val t = if (zone.isNullOrBlank()) now else now.withZoneSameInstant(java.time.ZoneId.of(zone))
            Hands.drawAt(canvas, r, hands, t.hour % 12, t.minute, t.second)
        } else {
            lives += LiveAnalog(r, hands, zone)
        }
    }

    fun hit(r: RectF, a: HitAction) { hits += Hit(RectF(r), a) }

    companion object {
        fun withAlpha(c: Int, a: Int) = Color.argb((a.coerceIn(0, 255) * Color.alpha(c) / 255), Color.red(c), Color.green(c), Color.blue(c))
        fun mix(a: Int, b: Int, t: Float): Int {
            val it = 1 - t
            return Color.argb(
                (Color.alpha(a) * it + Color.alpha(b) * t).toInt(),
                (Color.red(a) * it + Color.red(b) * t).toInt(),
                (Color.green(a) * it + Color.green(b) * t).toInt(),
                (Color.blue(a) * it + Color.blue(b) * t).toInt(),
            )
        }
    }
}

fun Path.roundRect(r: RectF, rad: Float): Path { addRoundRect(r, rad, rad, Path.Direction.CW); return this }
