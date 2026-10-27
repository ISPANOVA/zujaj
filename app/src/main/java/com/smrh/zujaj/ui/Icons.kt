package com.smrh.zujaj.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon as MIcon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glass icons: any installed app gets a glass-styled icon on the home screen. Android doesn't let one app
 * change another app's icon, so — like other icon apps — each one is a home-screen shortcut that opens that
 * app. The icon is drawn here: a glass tile with the app's own symbol in white (or in its colours).
 */
object GlassIcons {
    enum class Style(val en: String, val ar: String) {
        CLEAR("Clear glass", "زجاج شفاف"), FROST("Frosted dark", "زجاج غامق"), TINT("Tinted", "زجاج ملوّن"), COLOR("Glass + colours", "زجاج بألوانه"),
    }

    /** The app's symbol on transparent: the adaptive monochrome or foreground layer when there is one. */
    private fun symbol(d: Drawable, size: Int): Pair<Bitmap, Boolean> {
        if (android.os.Build.VERSION.SDK_INT >= 26 && d is AdaptiveIconDrawable) {
            val mono = if (android.os.Build.VERSION.SDK_INT >= 33) d.monochrome else null
            val layer = mono ?: d.foreground
            if (layer != null) {
                // adaptive layers are 108dp with the visible part in the middle 72dp
                val big = Bitmap.createBitmap(size * 3 / 2, size * 3 / 2, Bitmap.Config.ARGB_8888)
                layer.setBounds(0, 0, big.width, big.height); layer.draw(Canvas(big))
                val off = (big.width - size) / 2
                return Bitmap.createBitmap(big, off, off, size, size) to true
            }
        }
        return d.toBitmap(size, size, Bitmap.Config.ARGB_8888) to false
    }

    fun render(ctx: Context, d: Drawable, style: Style, tint: Int, px: Int = 256): Bitmap {
        val out = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(out); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = RectF(px * 0.04f, px * 0.04f, px * 0.96f, px * 0.96f); val rad = px * 0.26f
        // glass tile
        val (top, bottom) = when (style) {
            Style.CLEAR -> android.graphics.Color.argb(120, 255, 255, 255) to android.graphics.Color.argb(50, 255, 255, 255)
            Style.FROST -> android.graphics.Color.argb(230, 40, 40, 52) to android.graphics.Color.argb(230, 16, 16, 22)
            Style.TINT, Style.COLOR -> androidx.core.graphics.ColorUtils.setAlphaComponent(tint, 210) to androidx.core.graphics.ColorUtils.setAlphaComponent(androidx.core.graphics.ColorUtils.blendARGB(tint, android.graphics.Color.BLACK, 0.45f), 220)
        }
        p.shader = LinearGradient(0f, r.top, 0f, r.bottom, top, bottom, Shader.TileMode.CLAMP); c.drawRoundRect(r, rad, rad, p); p.shader = null
        // top sheen + crisp rim: what makes it read as glass
        p.shader = LinearGradient(0f, r.top, 0f, r.top + r.height() * 0.5f, android.graphics.Color.argb(70, 255, 255, 255), 0, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = px * 0.012f
        p.shader = LinearGradient(r.left, r.top, r.right, r.bottom, android.graphics.Color.argb(170, 255, 255, 255), android.graphics.Color.argb(40, 255, 255, 255), Shader.TileMode.CLAMP)
        c.drawRoundRect(r, rad, rad, p); p.shader = null; p.style = Paint.Style.FILL
        // the symbol
        val s = (px * 0.62f).toInt()
        val (sym, adaptive) = symbol(d, s)
        val left = (px - s) / 2f; val topY = (px - s) / 2f
        if (style == Style.COLOR || !adaptive) {
            // keep the app's colours (also for old non-adaptive icons, whose silhouette would be a plain square)
            c.drawBitmap(sym, null, RectF(left + s * 0.1f, topY + s * 0.1f, left + s * 0.9f, topY + s * 0.9f), p)
        } else {
            // white silhouette with a soft shadow
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { maskFilter = BlurMaskFilter(px * 0.03f, BlurMaskFilter.Blur.NORMAL); colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0f,0f,0f,0f,0f, 0f,0f,0f,0f,0f, 0f,0f,0f,0f,0f, 0f,0f,0f,0.35f,0f))) }
            c.drawBitmap(sym.extractAlpha(), left, topY + px * 0.015f, shadow)
            val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (style == Style.CLEAR) android.graphics.Color.WHITE else android.graphics.Color.argb(245, 255, 255, 255) }
            c.drawBitmap(sym.extractAlpha(), left, topY, white)
        }
        return out
    }

    fun supported(ctx: Context) = android.os.Build.VERSION.SDK_INT >= 26 && ctx.getSystemService(ShortcutManager::class.java)?.isRequestPinShortcutSupported == true

    /** Asks the launcher to place the shortcut. Returns false when the launcher can't. */
    fun pin(ctx: Context, app: com.smrh.zujaj.model.AppRef, icon: Bitmap, label: String): Boolean {
        if (!supported(ctx)) return false
        val sm = ctx.getSystemService(ShortcutManager::class.java)
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName(app.pkg, app.cls))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val info = ShortcutInfo.Builder(ctx, "glass_${app.pkg}_${System.currentTimeMillis()}")
            .setShortLabel(label.take(20)).setIcon(Icon.createWithBitmap(icon)).setIntent(launch).build()
        return runCatching { sm.requestPinShortcut(info, null) }.getOrDefault(false)
    }
}

@Composable
fun IconsScreen(nav: Nav) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    LaunchedEffect(Unit) { apps = loadApps(ctx) }
    var style by rememberSaveable { mutableStateOf(GlassIcons.Style.CLEAR) }
    var tint by rememberSaveable { mutableIntStateOf(0xFF6C5CE7.toInt()) }
    var q by rememberSaveable { mutableStateOf("") }
    val pane = LocalPaneWidth.current
    val gut = Sp.gutter(pane)
    val locked = !Prefs.isPro && style != GlassIcons.Style.CLEAR
    // the app icons themselves are loaded once; glass versions are drawn per style
    val pm = ctx.packageManager
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Glass icons", "أيقونات زجاج"), { nav.back() })
        Text(tr("Tap an app to put a glass version of its icon on your home screen. It opens the same app.",
            "اضغط على أي تطبيق عشان تحط نسخة زجاج من أيقونته على الشاشة الرئيسية. بتفتح نفس التطبيق."),
            color = Z.sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = gut))
        Spacer(Modifier.height(Sp.m))
        LazyRow(contentPadding = PaddingValues(horizontal = gut), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(GlassIcons.Style.values().size) { i -> val s = GlassIcons.Style.values()[i]
                ZChip(tr(s.en, s.ar) + if (s != GlassIcons.Style.CLEAR && !Prefs.isPro) " · Pro" else "", style == s) { style = s } }
        }
        if (style == GlassIcons.Style.TINT || style == GlassIcons.Style.COLOR) {
            LazyRow(contentPadding = PaddingValues(horizontal = gut, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val cols = listOf(0xFF6C5CE7, 0xFFE7B75F, 0xFFFF6B9A, 0xFF3DD9B4, 0xFF4DB5FF, 0xFFFF8A4C, 0xFF2A2A33, 0xFF8B6B4A).map { it.toInt() }
                items(cols.size) { i -> val c = cols[i]
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(c)).border(2.dp, if (c == tint) Z.text else Color.Transparent, CircleShape).clickable { tint = c }) }
            }
        }
        ZField(q, { q = it }, tr("Search apps", "ابحث عن تطبيق"), Icons.Rounded.Search, modifier = Modifier.padding(horizontal = gut, vertical = 8.dp))
        if (!GlassIcons.supported(ctx)) Text(tr("Your launcher doesn't allow adding shortcuts.", "اللانشر بتاعك مش بيسمح بإضافة اختصارات."), color = Color(0xFFFF8A4C), fontSize = 13.sp, modifier = Modifier.padding(horizontal = gut))
        val list = apps?.filter { q.isBlank() || it.ref.label.contains(q, true) }
        if (list == null) Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalAccent.current) }
        else LazyVerticalGrid(GridCells.Adaptive(84.dp), contentPadding = PaddingValues(start = gut, end = gut, top = 8.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.ref.pkg + it.ref.cls }) { a ->
                val bmp by produceState<Bitmap?>(null, a.ref, style, tint) {
                    value = withContext(Dispatchers.Default) {
                        runCatching { GlassIcons.render(ctx, pm.getActivityIcon(ComponentName(a.ref.pkg, a.ref.cls)), style, tint, 192) }.getOrNull()
                    }
                }
                Column(Modifier.clip(RoundedCornerShape(Rad.m)).clickable {
                    if (locked) { nav.go(Screen.Paywall); return@clickable }
                    val b = bmp ?: return@clickable
                    Haptics.click(ctx)
                    if (!GlassIcons.pin(ctx, a.ref, b, a.ref.label)) Toast.makeText(ctx, tr("Your launcher doesn't allow adding shortcuts", "اللانشر مش بيسمح بإضافة اختصارات"), Toast.LENGTH_LONG).show()
                }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // the glass icon drawn over a wallpaper-like tile so it looks like on a home screen
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(Backdrops.brush(a.ref.label.length)), contentAlignment = Alignment.Center) {
                        bmp?.let { Image(it.asImageBitmap(), a.ref.label, Modifier.size(60.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(a.ref.label, fontSize = 11.sp, color = Z.text, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
