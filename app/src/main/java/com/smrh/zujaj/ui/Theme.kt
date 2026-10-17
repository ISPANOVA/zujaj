package com.smrh.zujaj.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import com.smrh.zujaj.R
import com.smrh.zujaj.data.Prefs

data class Accent(val en: String, val ar: String, val color: Color)

object Z {
    val accents = listOf(
        Accent("Gold", "ذهبي", Color(0xFFE7B75F)),
        Accent("Violet", "بنفسجي", Color(0xFF8B7CFF)),
        Accent("Rose", "وردي", Color(0xFFFF6B9A)),
        Accent("Mint", "نعناعي", Color(0xFF3DD9B4)),
        Accent("Sky", "سماوي", Color(0xFF4DB5FF)),
        Accent("Ember", "برتقالي", Color(0xFFFF8A4C)),
    )
    val bg = Color(0xFF07070C)
    val surface = Color(0xFF15151B)
    val surface2 = Color(0xFF1D1D25)
    val line = Color(0x1AFFFFFF)
    val text = Color(0xFFF4F3F0)
    val sub = Color(0xFF9C9BA6)
    val pro = Color(0xFFE7B75F)

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
}

val LocalAccent = staticCompositionLocalOf { Z.accents[0].color }

fun tr(en: String, ar: String) = if (Prefs.isArabicUi) ar else en

@Composable
fun ZujajTheme(content: @Composable () -> Unit) {
    val ar = Prefs.isArabicUi
    val accent = Z.accents.getOrElse(Prefs.accentIndex) { Z.accents[0] }.color
    val family = if (ar) Z.cairo else Z.manrope
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    val typo = Typography(
        displayLarge = base.displayLarge.f(), displayMedium = base.displayMedium.f(), displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(), headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f().copy(fontWeight = FontWeight.Bold), titleMedium = base.titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.f(), bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f().copy(fontWeight = FontWeight.SemiBold), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f().copy(fontSize = 11.sp),
    )
    val scheme = darkColorScheme(
        primary = accent, onPrimary = Color(0xFF14110A), secondary = accent,
        background = Z.bg, surface = Z.surface, surfaceVariant = Z.surface2, onBackground = Z.text, onSurface = Z.text,
        onSurfaceVariant = Z.sub, outline = Z.line, surfaceContainer = Z.surface, surfaceContainerHigh = Z.surface2,
    )
    CompositionLocalProvider(
        LocalLayoutDirection provides if (ar) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalAccent provides accent,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typo) {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Z.text, content = content)
        }
    }
}
