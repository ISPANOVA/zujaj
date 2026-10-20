package com.smrh.zujaj.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Cat
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.Texture
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.Renderer
import com.smrh.zujaj.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One look for every widget at once. Applying it rewrites each placed widget; each can still be edited after. */
object WidgetThemes {
    class Theme(val id: String, val en: String, val ar: String, val apply: (WidgetConfig) -> WidgetConfig)

    val all = listOf(
        Theme("liquid", "Liquid glass", "زجاج سائل") {
            it.copy(bg = true, fill = Fill.GLASS, bgColor = 0xFF20202A.toInt(), bgAlpha = 100, texture = Texture.OFF, border = true,
                color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), dayTint = false)
        },
        Theme("neon", "Neon", "نيون") {
            it.copy(bg = true, fill = Fill.SOLID, bgColor = 0xFF0A0A14.toInt(), bgAlpha = 100, texture = Texture.OFF, border = true,
                color1 = 0xFF7DF9FF.toInt(), color2 = 0xFFB98CFF.toInt(), accent = 0xFFFF4FD8.toInt(), accent2 = 0xFF7DF9FF.toInt(), dayTint = false)
        },
        Theme("minimal", "Minimal", "بسيط") {
            it.copy(bg = false, fill = Fill.NONE, texture = Texture.OFF, border = false,
                color1 = 0xFFFFFFFF.toInt(), color2 = 0xD9FFFFFF.toInt(), accent = 0xFFFFFFFF.toInt(), dayTint = false)
        },
        Theme("paper", "Paper", "ورقي") {
            it.copy(bg = true, fill = Fill.SOLID, bgColor = 0xFFF4F1EA.toInt(), bgAlpha = 100, texture = Texture.GRAIN, border = false,
                color1 = 0xFF1B1B1B.toInt(), color2 = 0xFF6B6B6B.toInt(), accent = 0xFFC0392B.toInt(), dayTint = false)
        },
        Theme("gold", "Midnight gold", "ذهبي ليلي") {
            it.copy(bg = true, fill = Fill.GRADIENT, bgColor = 0xFF14110B.toInt(), bgColor2 = 0xFF2B2213.toInt(), bgAlpha = 100, texture = Texture.OFF, border = true,
                color1 = 0xFFF2D49A.toInt(), color2 = 0xFFB8995E.toInt(), accent = 0xFFE7B75F.toInt(), accent2 = 0xFFF2865E.toInt(), dayTint = false)
        },
        Theme("sky", "Follow the sky", "ألوان السما") {
            it.copy(bg = true, fill = Fill.GRADIENT, bgColor = 0xFF1F6FD1.toInt(), bgColor2 = 0xFF5EB6F0.toInt(), bgAlpha = 100, texture = Texture.OFF, border = false,
                color1 = 0xFFFFFFFF.toInt(), color2 = 0xCCFFFFFF.toInt(), dayTint = true)
        },
    )

    /** Photos and sticky notes keep their own look. */
    fun skip(cfg: WidgetConfig): Boolean {
        val st = Catalog.byId(cfg.styleId) ?: return true
        return st.cat == Cat.PHOTO || st.id == "sticky_note"
    }

    /** Applies [t] to every widget on the home screen; returns how many changed. */
    fun applyAll(ctx: android.content.Context, t: Theme): Int {
        var n = 0
        WidgetUpdater.allIds(ctx).forEach { id ->
            val cfg = Prefs.widgetConfig(id) ?: return@forEach
            if (skip(cfg)) return@forEach
            Prefs.saveWidgetConfig(id, t.apply(cfg)); n++
        }
        WidgetUpdater.updateAll(ctx)
        return n
    }
}

@Composable
fun ThemesDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var confirm by remember { mutableStateOf<WidgetThemes.Theme?>(null) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Z.surface,
        title = { Text(tr("Widget themes", "ثيمات الويدجت")) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("One look for all the widgets on your home screen. You can still edit each one after.", "شكل واحد لكل الويدجت اللي على الشاشة. وتقدر تعدّل أي واحدة بعدها."),
                    color = Z.sub, fontSize = 13.sp)
                WidgetThemes.all.forEach { t ->
                    val img by produceState<ImageBitmap?>(null, t.id) {
                        value = withContext(Dispatchers.Default) { runCatching {
                            val st = Catalog.byId("prayer_hub") ?: Catalog.all.first { it.shape == com.smrh.zujaj.catalog.Shape.WIDE }
                            Renderer.render(ctx, t.apply(st.defaultConfig()), st.shape.dw, st.shape.dh, true, 2f).bitmap.asImageBitmap()
                        }.getOrNull() }
                    }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { confirm = t }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(340f / 160f).clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF3B6EA8), Color(0xFFB0567A), Color(0xFFE9A15B))))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            img?.let { Image(it, null, Modifier.fillMaxWidth().padding(10.dp)) }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(tr(t.en, t.ar), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close", "إغلاق")) } })
    confirm?.let { t ->
        AlertDialog(onDismissRequest = { confirm = null }, containerColor = Z.surface,
            title = { Text(tr(t.en, t.ar)) },
            text = { Text(tr("Apply this look to all your widgets?", "تطبّق الشكل ده على كل الويدجت؟")) },
            confirmButton = { TextButton(onClick = {
                val n = WidgetThemes.applyAll(ctx, t)
                confirm = null; onDismiss()
                Toast.makeText(ctx, if (n == 0) tr("Add a widget to your home screen first", "ضيف ويدجت على الشاشة الأول") else tr("Applied to $n widgets ✓", "اتطبق على $n ويدجت ✓"), Toast.LENGTH_SHORT).show()
            }) { Text(tr("Apply", "تطبيق"), color = LocalAccent.current, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(tr("Cancel", "إلغاء")) } })
    }
}
