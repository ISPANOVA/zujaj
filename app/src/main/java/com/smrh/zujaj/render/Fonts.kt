package com.smrh.zujaj.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.smrh.zujaj.R

enum class F(val res: Int) {
    /** Simple Arabic: the phone's own clean system font (One UI on Samsung, Noto on Pixel) for data,
     *  times, temperatures, countdowns and labels. res = 0 means "system font", weight in [AR_W]. */
    AR_UI(0),
    AR_UI_MED(0),
    AR_UI_BOLD(0),
    SANS(R.font.manrope_regular),
    SANS_MED(R.font.manrope_semibold),
    SANS_BOLD(R.font.manrope_bold),
    PLAYFAIR(R.font.playfair_bold),
    BODONI(R.font.bodoni_bold),
    BODONI_REG(R.font.bodoni_regular),
    SHOULDERS(R.font.shoulders_medium),
    SHOULDERS_LIGHT(R.font.shoulders_light),
    OSWALD(R.font.oswald_regular),
    OSWALD_LIGHT(R.font.oswald_light),
    CORMORANT(R.font.cormorant_medium),
    RUQAA(R.font.aref_ruqaa_bold),
    THULUTH(R.font.gulzar),
    AMIRI(R.font.amiri_bold),
    KUFI(R.font.reem_kufi_medium),
    KUFI_BOLD(R.font.reem_kufi_bold),
    RAKKAS(R.font.rakkas),
    CAIRO(R.font.cairo_regular),
    CAIRO_BOLD(R.font.cairo_bold),
    // clock faces (digit-locked, see tools/gen_clock_fonts.py)
    CLK_LABEL(R.font.clk_label),
    CLK_MANROPE(R.font.clk_manrope),
    CLK_PLAYFAIR(R.font.clk_playfair),
    CLK_BODONI(R.font.clk_bodoni),
    CLK_SHOULDERS(R.font.clk_shoulders),
    CLK_OSWALD(R.font.clk_oswald),
    CLK_ANTON(R.font.clk_anton),
    CLK_BEBAS(R.font.clk_bebas),
    CLK_ARCHIVO(R.font.clk_archivo),
    CLK_ORBITRON(R.font.clk_orbitron),
    CLK_MICHROMA(R.font.clk_michroma),
    CLK_DMSERIF(R.font.clk_dmserif),
    CLK_ABRIL(R.font.clk_abril),
    CLK_POIRET(R.font.clk_poiret),
    CLK_SIXCAPS(R.font.clk_sixcaps),
    CLK_RIGHTEOUS(R.font.clk_righteous),
    CLK_COMFORTAA(R.font.clk_comfortaa),
    CLK_UNBOUNDED(R.font.clk_unbounded),
    CLK_SYNE(R.font.clk_syne),
    CLK_AR_CAIRO(R.font.clk_ar_cairo),
    CLK_AR_AMIRI(R.font.clk_ar_amiri),
    CLK_AR_RAKKAS(R.font.clk_ar_rakkas),
    CLK_AR_KUFI(R.font.clk_ar_kufi),
}

object Fonts {
    private val cache = HashMap<F, Typeface>()

    @Synchronized
    fun get(ctx: Context, f: F): Typeface = cache.getOrPut(f) {
        when (f) {
            // the old simple Arabic face now follows the same clean system font, so every widget matches
            F.AR_UI, F.CAIRO -> system(400)
            F.AR_UI_MED -> system(500)
            F.AR_UI_BOLD, F.CAIRO_BOLD -> system(700)
            else -> runCatching { ResourcesCompat.getFont(ctx, f.res) }.getOrNull() ?: Typeface.DEFAULT
        }
    }

    private fun system(weight: Int): Typeface =
        if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.create("sans-serif", Typeface.NORMAL), weight, false)
        else Typeface.create(if (weight >= 600) "sans-serif" else if (weight >= 500) "sans-serif-medium" else "sans-serif", if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)

    /** Decorative faces (headings): calligraphy. Everything else is the simple face. */
    fun isSimple(f: F) = f == F.AR_UI || f == F.AR_UI_MED || f == F.AR_UI_BOLD || f == F.CAIRO || f == F.CAIRO_BOLD ||
        f == F.SANS || f == F.SANS_MED || f == F.SANS_BOLD
}
