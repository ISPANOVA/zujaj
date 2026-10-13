package com.smrh.zujaj.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import kotlin.math.min

object IconGen {
    data class IconStyle(val en: String, val ar: String, val pro: Boolean)

    val styles = listOf(
        IconStyle("Glass", "زجاجي", false), IconStyle("Dark", "داكن", false), IconStyle("Mono", "أحادي", true),
        IconStyle("Gold", "ذهبي", true), IconStyle("Aurora", "شفق", true), IconStyle("Outline", "محدد", true),
    )

    fun make(ctx: Context, app: AppEntry, style: Int, size: Int = 288): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val rad = size * 0.28f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val src = runCatching {
            ctx.packageManager.getActivityIcon(ComponentName(app.ref.pkg, app.ref.cls)).toBitmap(size, size)
        }.getOrNull() ?: app.icon ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        when (style) {
            0 -> { // glass
                p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), 0x55FFFFFF, 0x18FFFFFF, Shader.TileMode.CLAMP)
                c.drawRoundRect(r, rad, rad, p)
                p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = size * 0.015f; p.color = 0x66FFFFFF
                c.drawRoundRect(RectF(2f, 2f, size - 2f, size - 2f), rad, rad, p)
            }
            1 -> { p.color = 0xFF16161C.toInt(); c.drawRoundRect(r, rad, rad, p) }
            2 -> { p.color = 0xFF101014.toInt(); c.drawRoundRect(r, rad, rad, p) }
            3 -> { p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), 0xFF2A2010.toInt(), 0xFF120E07.toInt(), Shader.TileMode.CLAMP); c.drawRoundRect(r, rad, rad, p) }
            4 -> { p.shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), intArrayOf(0xFF4C3BCF.toInt(), 0xFFE8478B.toInt(), 0xFFFFA25C.toInt()), null, Shader.TileMode.CLAMP); c.drawRoundRect(r, rad, rad, p) }
            else -> { p.style = Paint.Style.STROKE; p.strokeWidth = size * 0.03f; p.color = 0xFFFFFFFF.toInt(); c.drawRoundRect(RectF(size * 0.03f, size * 0.03f, size * 0.97f, size * 0.97f), rad, rad, p) }
        }
        val inner = size * 0.62f
        val dst = RectF((size - inner) / 2, (size - inner) / 2, (size + inner) / 2, (size + inner) / 2)
        val ip = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        when (style) {
            2, 5 -> ip.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                0.33f, 0.33f, 0.33f, 0f, 60f, 0.33f, 0.33f, 0.33f, 0f, 60f, 0.33f, 0.33f, 0.33f, 0f, 60f, 0f, 0f, 0f, 1f, 0f)))
            3 -> ip.colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(
                0.45f, 0.45f, 0.1f, 0f, 40f, 0.36f, 0.36f, 0.08f, 0f, 25f, 0.2f, 0.2f, 0.05f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
        }
        // clip the source icon into a circle-ish squircle for consistency
        val clip = Path().apply { addRoundRect(dst, inner * 0.3f, inner * 0.3f, Path.Direction.CW) }
        c.save(); c.clipPath(clip)
        ip.shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also {
            val m = android.graphics.Matrix(); val s = inner / min(src.width, src.height)
            m.setScale(s, s); m.postTranslate(dst.left, dst.top); it.setLocalMatrix(m)
        }
        c.drawRect(dst, ip)
        c.restore()
        return b
    }

    fun pin(ctx: Context, app: AppEntry, label: String, bmp: Bitmap): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.ref.pkg, app.ref.cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val info = ShortcutInfoCompat.Builder(ctx, "icon_${app.ref.pkg}_${System.currentTimeMillis()}")
            .setShortLabel(label.ifBlank { app.ref.label })
            .setIcon(IconCompat.createWithAdaptiveBitmap(padForAdaptive(bmp)))
            .setIntent(launch)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
    }

    /** Adaptive icons crop ~18% each side – pad so nothing gets cut. */
    private fun padForAdaptive(b: Bitmap): Bitmap {
        val s = (b.width * 1.5f).toInt()
        val out = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(b, (s - b.width) / 2f, (s - b.height) / 2f, null)
        return out
    }
}

@Composable
fun IconsTab() {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var style by remember { mutableIntStateOf(0) }
    var q by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<AppEntry?>(null) }
    LaunchedEffect(Unit) { apps = loadApps(ctx) }
    val ar = Prefs.isArabicUi
    Column(Modifier.fillMaxSize()) {
        Text(tr("Restyle any app icon and add it to your home screen.", "غيّر شكل أي أيقونة وأضفها لشاشتك الرئيسية."),
            color = Z.sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(IconGen.styles.size) { i ->
                val s = IconGen.styles[i]
                ZChip((if (ar) s.ar else s.en) + if (s.pro && !Prefs.isPro) " 👑" else "", i == style) { style = i }
            }
        }
        OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text(tr("Search apps", "ابحث عن تطبيق")) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
        LazyVerticalGrid(GridCells.Fixed(4), contentPadding = pagePadding, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
            items(apps.filter { q.isBlank() || it.ref.label.contains(q, true) }, key = { it.ref.pkg + it.ref.cls }) { a ->
                val bmp = remember(a, style) { IconGen.make(ctx, a, style, 160) }
                Column(Modifier.clickable { chosen = a }.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(bmp.asImageBitmap(), null, Modifier.size(58.dp))
                    Text(a.ref.label, fontSize = 10.sp, color = Z.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    chosen?.let { a ->
        var label by remember(a) { mutableStateOf(a.ref.label) }
        val bmp = remember(a, style) { IconGen.make(ctx, a, style) }
        Dialog(onDismissRequest = { chosen = null }) {
            Column(Modifier.clip(RoundedCornerShape(26.dp)).background(Z.surface).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bmp.asImageBitmap(), null, Modifier.size(110.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(label, { label = it }, singleLine = true, label = { Text(tr("Label", "الاسم")) })
                Spacer(Modifier.height(16.dp))
                val locked = IconGen.styles[style].pro && !Prefs.isPro && !Flags.flags.value.freeAll
                PrimaryButton(if (locked) tr("Pro style", "ستايل برو") else tr("Add to Home screen", "أضف للشاشة الرئيسية"), icon = Icons.Rounded.Add) {
                    if (locked) { Toast.makeText(ctx, tr("This style needs Pro", "هذا الستايل يحتاج برو"), Toast.LENGTH_SHORT).show(); return@PrimaryButton }
                    val ok = IconGen.pin(ctx, a, label, bmp)
                    Toast.makeText(ctx, if (ok) tr("Confirm on the home screen ✓", "أكد الإضافة على الشاشة ✓") else tr("Launcher doesn't support shortcuts", "المشغل لا يدعم الاختصارات"), Toast.LENGTH_SHORT).show()
                    chosen = null
                }
                Spacer(Modifier.height(8.dp))
                Text(tr("Some launchers show a small badge on shortcuts.", "بعض المشغلات تُظهر علامة صغيرة على الاختصار."), fontSize = 11.sp, color = Z.sub)
            }
        }
    }
}
