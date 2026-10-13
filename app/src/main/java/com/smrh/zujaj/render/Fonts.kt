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
    AMIRI(R.font.amiri_bold),
    KUFI(R.font.reem_kufi_medium),
    RAKKAS(R.font.rakkas),
    CAIRO(R.font.cairo_regular),
    CAIRO_BOLD(R.font.cairo_bold),
}

object Fonts {
    private val cache = HashMap<F, Typeface>()

    @Synchronized
    fun get(ctx: Context, f: F): Typeface = cache.getOrPut(f) {
        runCatching { ResourcesCompat.getFont(ctx, f.res) }.getOrNull() ?: Typeface.DEFAULT
    }
}
