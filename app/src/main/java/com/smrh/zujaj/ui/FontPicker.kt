package com.smrh.zujaj.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Numerals
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.LiveFont

private val families = HashMap<LiveFont, FontFamily>()
private fun family(f: LiveFont) = families.getOrPut(f) { FontFamily(Font(f.f.res)) }

/** "123 / ١٢٣" switch, always shown first in the Content tab. */
@Composable
fun NumeralsSwitch(cfg: WidgetConfig, onChange: (WidgetConfig) -> Unit) {
    val arabic = cfg.numerals == Numerals.ARABIC
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(false to ("123" to tr("English numerals", "أرقام إنجليزية")), true to ("١٢٣" to tr("Arabic numerals", "أرقام عربية"))).forEach { (isAr, t) ->
            val sel = isAr == arabic
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) accent.copy(alpha = 0.16f) else Color(0x0FFFFFFF))
                    .border(1.5.dp, if (sel) accent else Color(0x14FFFFFF), RoundedCornerShape(16.dp))
                    .clickable { onChange(cfg.copy(numerals = if (isAr) Numerals.ARABIC else Numerals.LATIN)) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(t.first, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (sel) accent else Z.text,
                    fontFamily = family(if (isAr) LiveFont.AR_CAIRO else LiveFont.SANS_BOLD))
                Text(t.second, fontSize = 12.sp, color = Z.sub)
            }
        }
    }
}

/** Grid of clock faces, each showing the time in that face. */
@Composable
fun ClockFontGrid(cfg: WidgetConfig, onChange: (WidgetConfig) -> Unit, onPro: () -> Unit) {
    val arabic = cfg.numerals == Numerals.ARABIC
    val accent = LocalAccent.current
    val choices: List<LiveFont?> = remember(arabic) { listOf<LiveFont?>(null) + if (arabic) LiveFont.arabicChoices else LiveFont.latinChoices }
    val current = LiveFont.byName(cfg.font)?.let { if (arabic) it.toArabic() else it.toLatin() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { f ->
                    val sel = f == current
                    val locked = f != null && f.pro && !Prefs.isPro
                    Box(
                        Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) accent.copy(alpha = 0.14f) else Color(0x0DFFFFFF))
                            .border(1.5.dp, if (sel) accent else Color(0x12FFFFFF), RoundedCornerShape(16.dp))
                            .clickable { if (locked) onPro() else onChange(cfg.copy(font = f?.name ?: "")) },
                    ) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (f == null) {
                                Text(tr("Original", "الأصلي"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (sel) accent else Z.text)
                            } else {
                                Text(if (arabic) "١٢:٤٥" else "12:45", fontFamily = family(f), fontSize = 27.sp, maxLines = 1,
                                    color = if (sel) accent else Z.text, textAlign = TextAlign.Center, lineHeight = 30.sp)
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(f?.label ?: tr("design's face", "خط التصميم"), fontSize = 9.sp, color = Z.sub, maxLines = 1)
                        }
                        if (locked) ProBadge(Modifier.align(Alignment.TopEnd).padding(5.dp))
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
