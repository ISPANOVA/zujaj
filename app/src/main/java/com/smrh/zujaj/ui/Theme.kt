package com.smrh.zujaj.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.smrh.zujaj.R
import com.smrh.zujaj.data.Prefs

data class Accent(val en: String, val ar: String, val dark: Color, val lightC: Color) {
    /** deeper, richer version on light backgrounds so it never looks washed out */
    val color: Color get() = if (Z.light) lightC else dark
}

object Z {
    val accents = listOf(
        Accent("Gold", "ذهبي", Color(0xFFE7B75F), Color(0xFFC08A1E)),
        Accent("Violet", "بنفسجي", Color(0xFF8B7CFF), Color(0xFF5B4BE0)),
        Accent("Rose", "وردي", Color(0xFFFF6B9A), Color(0xFFE03C72)),
        Accent("Mint", "نعناعي", Color(0xFF3DD9B4), Color(0xFF0E9E80)),
        Accent("Sky", "سماوي", Color(0xFF4DB5FF), Color(0xFF1C7FD8)),
        Accent("Ember", "برتقالي", Color(0xFFFF8A4C), Color(0xFFE5621E)),
    )
    /** the chosen colour, observed by the UI so a change animates instead of rebuilding the app */
    var accentIdx by androidx.compose.runtime.mutableIntStateOf(-1)
    fun pickAccent(i: Int) { Prefs.accentIndex = i; accentIdx = i }
    /** Light look (set from Settings: system / dark / light). Every colour below follows it. */
    var light by androidx.compose.runtime.mutableStateOf(false)
    val bg get() = if (light) Color(0xFFF3F2F7) else Color(0xFF07070C)
    val surface get() = if (light) Color(0xFFFFFFFF) else Color(0xFF15151B)
    val surface2 get() = if (light) Color(0xFFEDECF3) else Color(0xFF1D1D25)
    val line get() = if (light) Color(0x14000000) else Color(0x1AFFFFFF)
    val text get() = if (light) Color(0xFF16151C) else Color(0xFFF4F3F0)
    val sub get() = if (light) Color(0xFF6B6977) else Color(0xFF9C9BA6)
    val pro get() = if (light) Color(0xFFB8852A) else Color(0xFFE7B75F)
    /** A soft wash for cards and chips: white on dark, black on light. */
    fun tint(a: Float) = (if (light) Color.Black else Color.White).copy(alpha = if (light) a * 0.75f else a)

    /** The phone's "remove animations" setting: decorative looping motion is skipped when it is on. */
    fun reducedMotion(ctx: android.content.Context): Boolean = runCatching {
        android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

    /** 0 = follow the phone, 1 = dark, 2 = light. */
    fun mode() = Prefs.getString("theme_mode", "1").toIntOrNull() ?: 1
    fun refresh(ctx: android.content.Context) {
        light = when (mode()) {
            2 -> true
            0 -> (ctx.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) != android.content.res.Configuration.UI_MODE_NIGHT_YES
            else -> false
        }
    }

    val manrope = FontFamily(
        Font(R.font.manrope_regular, FontWeight.Normal),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_bold, FontWeight.Bold),
    )
    val cairo = FontFamily(
        Font(R.font.cairo_regular, FontWeight.Normal),
        Font(R.font.cairo_bold, FontWeight.SemiBold),
        Font(R.font.cairo_bold, FontWeight.Bold),
    )
    val ruqaa = FontFamily(Font(R.font.aref_ruqaa_bold))
    val bodoni = FontFamily(Font(R.font.bodoni_bold))
    val kufi = FontFamily(Font(R.font.reem_kufi_bold))
}

val LocalAccent = androidx.compose.runtime.compositionLocalOf { Z.accents[0].color }

fun tr(en: String, ar: String) = when (val l = Prefs.uiLang) {
    "ar" -> ar
    "en" -> en
    else -> com.smrh.zujaj.data.I18n.t(l, en)
}

@Composable
fun ZujajTheme(content: @Composable () -> Unit) {
    val ar = Prefs.isArabicUi
    val idx = if (Z.accentIdx >= 0) Z.accentIdx else Prefs.accentIndex
    val accent by androidx.compose.animation.animateColorAsState(Z.accents.getOrElse(idx) { Z.accents[0] }.color, androidx.compose.animation.core.tween(500), label = "accent")
    val family = if (Prefs.isRtlUi) Z.cairo else Z.manrope
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    val typo = Typography(
        displayLarge = base.displayLarge.f(), displayMedium = base.displayMedium.f(), displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(), headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f().copy(fontWeight = FontWeight.Bold), titleMedium = base.titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.f(), bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f().copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = base.labelLarge.f().copy(fontWeight = FontWeight.SemiBold), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f().copy(fontSize = 12.sp),
    )
    val scheme = if (Z.light) androidx.compose.material3.lightColorScheme(
        primary = accent, onPrimary = Color(0xFF14110A), secondary = accent,
        background = Z.bg, surface = Z.surface, surfaceVariant = Z.surface2, onBackground = Z.text, onSurface = Z.text,
        onSurfaceVariant = Z.sub, outline = Z.line, surfaceContainer = Z.surface, surfaceContainerHigh = Z.surface2,
        surfaceContainerLow = Z.surface, surfaceContainerLowest = Z.surface, surfaceContainerHighest = Z.surface2,
    ) else darkColorScheme(
        primary = accent, onPrimary = Color(0xFF14110A), secondary = accent,
        background = Z.bg, surface = Z.surface, surfaceVariant = Z.surface2, onBackground = Z.text, onSurface = Z.text,
        onSurfaceVariant = Z.sub, outline = Z.line, surfaceContainer = Z.surface, surfaceContainerHigh = Z.surface2,
    )
    CompositionLocalProvider(
        LocalLayoutDirection provides if (Prefs.isRtlUi) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalAccent provides accent,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typo) {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Z.text, content = content)
        }
    }
}

/** Small, tasteful vibrations so taps feel physical. */
object Haptics {
    private fun v(ctx: android.content.Context) = (ctx as? android.app.Activity)?.window?.decorView
    fun on() = Prefs.getString("haptics", "1") == "1"
    /** light tick: chips, tabs, toggles */
    fun tick(ctx: android.content.Context) { if (on()) v(ctx)?.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) }
    /** firm click: main buttons */
    fun click(ctx: android.content.Context) { if (on()) v(ctx)?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY) }
    /** success: added, saved, unlocked */
    fun success(ctx: android.content.Context) {
        if (!on()) return
        val view = v(ctx) ?: return
        if (android.os.Build.VERSION.SDK_INT >= 30) view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
        else view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }
}

// =============================================================================== design tokens
/** Spacing scale (4-pt grid). Screens use these instead of one-off numbers. */
object Sp {
    val xxs = 2.dp; val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 24.dp; val xxl = 32.dp
    /** Edge gutter that grows with the window so wide screens are not crammed against the bezel. */
    fun gutter(widthDp: Int) = when { widthDp >= 1200 -> 40.dp; widthDp >= 840 -> 32.dp; widthDp >= 600 -> 24.dp; else -> 16.dp }
}

/** Corner radii — small controls, medium fields, large surfaces, extra-large hero/sheets. */
object Rad { val s = 12.dp; val m = 16.dp; val l = 20.dp; val xl = 28.dp }

/** Window size classes from the usable width (not the device model). */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED;
    companion object { fun of(widthDp: Int) = when { widthDp >= 840 -> EXPANDED; widthDp >= 600 -> MEDIUM; else -> COMPACT } }
}

/** One motion language: standard easing, three durations, and no bounce on UI chrome. */
object Motion {
    val ease = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val FAST = 140; const val MED = 240; const val SLOW = 360
    fun <T> std(ms: Int = MED) = androidx.compose.animation.core.tween<T>(ms, easing = ease)
    fun color() = androidx.compose.animation.core.tween<Color>(FAST + 40, easing = ease)
}

/** Width (dp) of the pane a screen is drawn in — the window minus rails/cutouts — so layouts follow the real space. */
val LocalPaneWidth = androidx.compose.runtime.compositionLocalOf { 400 }
