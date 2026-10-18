package com.smrh.zujaj.render

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.smrh.zujaj.R

enum class F(val res: Int) {
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
        runCatching { ResourcesCompat.getFont(ctx, f.res) }.getOrNull() ?: Typeface.DEFAULT
    }
}
