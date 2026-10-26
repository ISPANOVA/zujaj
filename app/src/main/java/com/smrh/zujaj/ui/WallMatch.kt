package com.smrh.zujaj.ui

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.WidgetConfig

/**
 * "Match my wallpaper": reads the colours Android extracts from the current home-screen wallpaper
 * (no permission needed, Android 8.1+) and dresses the widget in them — a deep tinted card, white text,
 * and the most vivid wallpaper colour as the accent.
 */
object WallMatch {
    fun colours(ctx: Context): List<Int>? = runCatching {
        if (android.os.Build.VERSION.SDK_INT < 27) return null
        val c = WallpaperManager.getInstance(ctx).getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
        listOfNotNull(c.primaryColor, c.secondaryColor, c.tertiaryColor).map { it.toArgb() }
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    private fun hsl(c: Int) = FloatArray(3).also { ColorUtils.colorToHSL(c, it) }
    private fun fromHsl(h: Float, s: Float, l: Float) = ColorUtils.HSLToColor(floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f)))

    fun apply(cfg: WidgetConfig, cols: List<Int>): WidgetConfig {
        val p = cols[0]; val s = cols.getOrElse(1) { p }
        // accent: the most colourful of the three, brought to a readable lightness on a dark card
        val vivid = cols.maxBy { hsl(it)[1] * (1f - kotlin.math.abs(hsl(it)[2] - 0.5f)) }
        val va = hsl(vivid)
        val accent = fromHsl(va[0], maxOf(va[1], 0.55f), va[2].coerceIn(0.58f, 0.72f))
        val ph = hsl(p); val sh = hsl(s)
        val card = fromHsl(ph[0], ph[1] * 0.6f, 0.11f)
        val card2 = fromHsl(sh[0], sh[1] * 0.6f, 0.17f)
        val white = Color.WHITE; val sub = Color.argb(190, 255, 255, 255)
        return cfg.copy(
            bg = true, fill = Fill.GRADIENT, bgColor = card, bgColor2 = card2, bgAlpha = 88,
            color1 = white, color2 = sub, accent = accent, accent2 = accent, nextColor = accent, ornColor = accent,
            hourColor2 = accent, dayColor = white, hijriColor = white, timeColor = white, nameColor = sub,
        )
    }
}
