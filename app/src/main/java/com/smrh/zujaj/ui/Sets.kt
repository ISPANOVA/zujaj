package com.smrh.zujaj.ui

import androidx.compose.runtime.saveable.rememberSaveable
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ready-made "sets": a wallpaper drawn for the theme + 3–4 widgets dressed in the same colours.
 * One screen previews the whole home screen; applying sets the wallpaper and then places each widget
 * (Android asks for every widget separately, so it's a short checklist the user taps through).
 */
data class WidgetSet(
    val id: String, val en: String, val ar: String, val tagEn: String, val tagAr: String,
    val pro: Boolean, val wall: String, val items: List<String>,
    val accent: Int, val card: Int, val card2: Int, val text: Int = 0xFFFFFFFF.toInt(), val sub: Int = 0xCCFFFFFF.toInt(),
) {
    fun name(ar: Boolean) = if (ar) this.ar else en
    fun tag(ar: Boolean) = if (ar) tagAr else tagEn
    /** Dress a widget in the set's colours; designs with their own art (Rose, Night) keep theirs. */
    fun dress(styleId: String): WidgetConfig {
        val base = Catalog.byId(styleId)!!.defaultConfig()
        val own = styleId.startsWith("rose_") || styleId.startsWith("dark_")
        val c = if (own) base else base.copy(
            fill = Fill.GRADIENT, bgColor = card, bgColor2 = card2, bgAlpha = 86, border = true,
            color1 = text, color2 = sub, accent = accent, accent2 = accent, nextColor = accent, ornColor = accent,
            hourColor2 = accent, dayColor = text, hijriColor = text, timeColor = text, nameColor = sub,
        )
        return if (styleId == "event_ring") c.copy(event = "ramadan") else c
    }
}

object WidgetSets {
    val all = listOf(
        WidgetSet("midnight", "Midnight", "منتصف الليل", "Black glass, violet light", "زجاج أسود ونور بنفسجي", false, "midnight",
            listOf("dark_day", "digital_stacked", "athkar_small"), 0xFFA78BFA.toInt(), 0xFF120F1F.toInt(), 0xFF1C1631.toInt()),
        WidgetSet("desert", "Desert", "الصحراء", "Warm sand and gold", "رمل دافي ودهب", true, "desert",
            listOf("calli_desert", "prayer_journey", "clock_dots", "athkar_small"), 0xFFE7B75F.toInt(), 0xFF2A1A10.toInt(), 0xFF3D2616.toInt()),
        WidgetSet("ramadan", "Ramadan", "رمضان", "Crescent nights", "ليالي الهلال", true, "ramadan",
            listOf("prayer_spotlight", "athkar_card", "event_ring"), 0xFFF2C14E.toInt(), 0xFF0E1530.toInt(), 0xFF1A2550.toInt()),
        WidgetSet("rose", "Rose", "الورد", "Soft pink everything", "كل حاجة وردي", true, "rose",
            listOf("rose_next", "rose_weather", "rose_prayers", "rose_music"), 0xFFFF7BA9.toInt(), 0xFF2A0F1E.toInt(), 0xFF3A1428.toInt()),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Paints the set's wallpaper at any size (thumbnail or full screen). Pure drawing — no assets. */
    fun paint(kind: String, w: Int, h: Int): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        fun vgrad(vararg cols: Int) { p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), cols, null, Shader.TileMode.CLAMP); c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p); p.shader = null }
        fun glow(x: Float, y: Float, r: Float, col: Int) { p.shader = RadialGradient(x, y, r, col, 0, Shader.TileMode.CLAMP); c.drawCircle(x, y, r, p); p.shader = null }
        val rnd = java.util.Random(kind.hashCode().toLong())
        when (kind) {
            "desert" -> {
                vgrad(0xFF1B1020.toInt(), 0xFF5A2E2A.toInt(), 0xFFC77B3C.toInt(), 0xFFE9B067.toInt())
                glow(w * 0.68f, h * 0.48f, w * 0.55f, 0x66FFD08A)
                p.color = 0xFFFFE2A8.toInt(); c.drawCircle(w * 0.68f, h * 0.48f, w * 0.09f, p)
                val dunes = listOf(0xFF8A4B26.toInt() to 0.62f, 0xFF6B3720.toInt() to 0.72f, 0xFF4A2516.toInt() to 0.82f, 0xFF2E160D.toInt() to 0.92f)
                dunes.forEachIndexed { i, (col, y0) ->
                    val path = Path(); path.moveTo(0f, h.toFloat())
                    path.lineTo(0f, h * y0)
                    path.cubicTo(w * (0.25f + 0.1f * i), h * (y0 - 0.07f), w * (0.55f - 0.05f * i), h * (y0 + 0.05f), w.toFloat(), h * (y0 - 0.03f))
                    path.lineTo(w.toFloat(), h.toFloat()); path.close()
                    p.color = col; c.drawPath(path, p)
                }
            }
            "ramadan" -> {
                vgrad(0xFF04061A.toInt(), 0xFF0E1638.toInt(), 0xFF1E2A5C.toInt())
                glow(w * 0.7f, h * 0.2f, w * 0.6f, 0x33F2C14E)
                repeat(140) { val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h * 0.75f; val s = (rnd.nextFloat() * 1.8f + 0.4f) * w / 1080f
                    p.color = android.graphics.Color.argb((80 + rnd.nextInt(170)), 255, 248, 225); c.drawCircle(x, y, s * 2f, p) }
                // crescent: a lit disc minus an offset dark disc
                val cx = w * 0.7f; val cy = h * 0.2f; val r = w * 0.11f
                val l = c.saveLayer(null, null)
                p.color = 0xFFF8E3A3.toInt(); c.drawCircle(cx, cy, r, p)
                p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR); c.drawCircle(cx + r * 0.42f, cy - r * 0.18f, r * 0.9f, p); p.xfermode = null
                c.restoreToCount(l)
                // a soft skyline of domes
                p.color = 0xFF050816.toInt()
                val sky = Path(); sky.moveTo(0f, h.toFloat()); sky.lineTo(0f, h * 0.86f)
                var x = 0f
                while (x < w) { val dw = w * (0.12f + rnd.nextFloat() * 0.1f); val dh = h * (0.04f + rnd.nextFloat() * 0.05f)
                    sky.lineTo(x, h * 0.86f); sky.quadTo(x + dw / 2, h * 0.86f - dh * 2, x + dw, h * 0.86f); x += dw }
                sky.lineTo(w.toFloat(), h.toFloat()); sky.close(); c.drawPath(sky, p)
            }
            "rose" -> {
                vgrad(0xFF2A0F1E.toInt(), 0xFF7A2C52.toInt(), 0xFFE0789F.toInt(), 0xFFF6B9CF.toInt())
                repeat(26) { val x = rnd.nextFloat() * w; val y = rnd.nextFloat() * h; val r = w * (0.03f + rnd.nextFloat() * 0.09f)
                    p.maskFilter = BlurMaskFilter(r * 0.6f, BlurMaskFilter.Blur.NORMAL); p.color = android.graphics.Color.argb(40 + rnd.nextInt(60), 255, 220, 235); c.drawCircle(x, y, r, p) }
                p.maskFilter = null
            }
            else -> { // midnight: near-black with violet/teal aurora ribbons
                vgrad(0xFF050508.toInt(), 0xFF0B0A16.toInt(), 0xFF07070C.toInt())
                glow(w * 0.2f, h * 0.15f, w * 0.9f, 0x666C5CE7)
                glow(w * 0.9f, h * 0.55f, w * 0.7f, 0x3313B5A8)
                glow(w * 0.5f, h * 1.0f, w * 0.8f, 0x33A78BFA)
                p.style = Paint.Style.STROKE; p.strokeWidth = w * 0.004f
                repeat(5) { i -> p.color = android.graphics.Color.argb(26, 200, 190, 255)
                    val path = Path(); path.moveTo(0f, h * (0.3f + i * 0.05f))
                    for (k in 1..20) { val t = k / 20f; path.lineTo(w * t, h * (0.3f + i * 0.05f) + sin(t * 6.28f + i) * h * 0.03f) }
                    c.drawPath(path, p) }
                p.style = Paint.Style.FILL
            }
        }
        return b
    }

    /** Sets the wallpaper on the home and lock screen. Returns false if the phone refused. */
    suspend fun applyWallpaper(ctx: android.content.Context, set: WidgetSet): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val dm = ctx.resources.displayMetrics
            val w = minOf(dm.widthPixels, dm.heightPixels); val h = maxOf(dm.widthPixels, dm.heightPixels)
            val bmp = paint(set.wall, w, h)
            val wm = WallpaperManager.getInstance(ctx)
            if (android.os.Build.VERSION.SDK_INT >= 24) wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            else wm.setBitmap(bmp)
            true
        }.getOrDefault(false)
    }
}

/** Grid units of each widget size on the 4-column preview home screen. */
private fun span(s: Shape): Pair<Int, Int> = when (s) {
    Shape.SMALL -> 2 to 2; Shape.WIDE -> 4 to 2; Shape.BANNER -> 4 to 1; Shape.TALL -> 2 to 4; Shape.LARGE -> 4 to 3; Shape.XL -> 4 to 5
}

/** The set's home screen in miniature: its wallpaper with the widgets laid out on a 4-column grid. */
@Composable
fun SetPhone(set: WidgetSet, width: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val wall by produceState<ImageBitmap?>(null, set.id) { value = withContext(Dispatchers.Default) { WidgetSets.paint(set.wall, 360, 780).asImageBitmap() } }
    val cfgs = remember(set.id) { set.items.map { set.dress(it) } }
    val radius = width * 0.12f
    Box(modifier.width(width).aspectRatio(360f / 780f).clip(RoundedCornerShape(radius)).background(Color(0xFF0B0B10))
        .border(3.dp, Color(0xFF2A2A33), RoundedCornerShape(radius))) {
        wall?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        // flow the widgets into a 4-column grid, row by row
        val cell = (width - width * 0.1f) / 4
        Column(Modifier.fillMaxSize().padding(start = width * 0.05f, end = width * 0.05f, top = width * 0.16f), verticalArrangement = Arrangement.spacedBy(cell * 0.12f)) {
            var row = mutableListOf<WidgetConfig>(); var used = 0
            val rows = mutableListOf<List<WidgetConfig>>()
            cfgs.forEach { c -> val w = span(Catalog.byId(c.styleId)!!.shape).first
                if (used + w > 4) { rows += row; row = mutableListOf(); used = 0 }; row += c; used += w }
            if (row.isNotEmpty()) rows += row
            rows.forEach { r ->
                Row(horizontalArrangement = Arrangement.spacedBy(cell * 0.12f)) {
                    r.forEach { c -> val (cw, _) = span(Catalog.byId(c.styleId)!!.shape)
                        WidgetPreview(c, Modifier.width(cell * cw - cell * 0.12f), density = 1.4f) }
                }
            }
        }
    }
}

/** Home row: a strip of sets with their miniature home screens. */
@Composable
fun SetsRow(nav: Nav, gutter: Dp) {
    Column(Modifier.padding(bottom = Sp.s)) {
        Row(Modifier.fillMaxWidth().padding(start = gutter, end = gutter, top = Sp.s, bottom = Sp.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Ready-made sets", "سِتات جاهزة"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Z.text)
                Text(tr("Wallpaper + matching widgets, in one go", "خلفية وويدجت متناسقين مرة واحدة"), fontSize = 13.sp, color = Z.sub)
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = gutter), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(WidgetSets.all, key = { it.id }) { s ->
                Column(Modifier.width(150.dp).clip(RoundedCornerShape(Rad.l)).clickable { nav.go(Screen.SetDetail(s.id)) }) {
                    Box { SetPhone(s, 150.dp)
                        if (s.pro && !Prefs.isPro) ProBadge(Modifier.align(Alignment.TopEnd).padding(10.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Text(s.name(Prefs.isArabicUi), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Z.text, modifier = Modifier.padding(horizontal = 4.dp))
                    Text(s.tag(Prefs.isArabicUi), fontSize = 12.sp, color = Z.sub, maxLines = 1, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

/** One set: big preview, what's inside, and the apply checklist. */
@Composable
fun SetDetailScreen(nav: Nav, id: String) {
    val set = WidgetSets.byId(id) ?: run { LaunchedEffect(Unit) { nav.back() }; return }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ar = Prefs.isArabicUi
    val locked = set.pro && !Prefs.isPro
    var wallDone by rememberSaveable(id) { mutableStateOf<Boolean>(false) }
    var busy by remember { mutableStateOf(false) }
    // a widget counts as added when one more of that design appears on the home screen than when we opened
    val startCounts: IntArray = rememberSaveable(id) { set.items.map { st -> WidgetUpdater.allIds(ctx).count { Prefs.widgetConfig(it)?.styleId == st } }.toIntArray() }
    var counts by remember { mutableStateOf(startCounts.toList()) }
    LaunchedEffect(id) { while (true) { delay(900)
        counts = set.items.map { st -> WidgetUpdater.allIds(ctx).count { Prefs.widgetConfig(it)?.styleId == st } } } }
    val pane = LocalPaneWidth.current
    val wide = pane >= 760
    val preview: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().padding(vertical = Sp.l), contentAlignment = Alignment.Center) {
            SetPhone(set, if (wide) 300.dp else 240.dp)
        }
    }
    val steps: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = Sp.l)) {
            Text(set.name(ar), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Z.text)
            Text(set.tag(ar), fontSize = 15.sp, color = Z.sub)
            Spacer(Modifier.height(Sp.l))
            if (locked) {
                Text(tr("Part of Zujaj Pro — every set, every widget, no ads.", "جزء من زجاج برو — كل السِتات وكل الويدجت ومن غير إعلانات."), color = Z.sub, fontSize = 14.sp)
                Spacer(Modifier.height(Sp.l))
                PrimaryButton(tr("Unlock with Pro", "افتح مع برو"), Modifier.fillMaxWidth(), icon = Icons.Rounded.Lock) { nav.go(Screen.Paywall) }
                return@Column
            }
            Text(tr("Apply in ${set.items.size + 1} taps", "طبّقه في ${set.items.size + 1} ضغطات"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Z.text)
            Text(tr("Android asks once for each widget — tap them in order.", "أندرويد بيسأل مرة لكل ويدجت — اضغطهم بالترتيب."), fontSize = 13.sp, color = Z.sub)
            Spacer(Modifier.height(Sp.m))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.l)).background(Z.surface).border(1.dp, Z.line, RoundedCornerShape(Rad.l))) {
                StepRow(Icons.Rounded.Wallpaper, tr("Set the wallpaper", "حط الخلفية"), tr("Home & lock screen", "الشاشة الرئيسية وشاشة القفل"), wallDone, busy) {
                    busy = true
                    scope.launch {
                        val ok = WidgetSets.applyWallpaper(ctx, set)
                        busy = false; wallDone = ok
                        if (ok) Haptics.success(ctx)
                        Toast.makeText(ctx, if (ok) tr("Wallpaper set ✓", "اتحطت الخلفية ✓") else tr("Your phone didn't allow changing the wallpaper", "تليفونك مسمحش بتغيير الخلفية"), Toast.LENGTH_SHORT).show()
                    }
                }
                set.items.forEachIndexed { i, st ->
                    val style = Catalog.byId(st)!!
                    val done = counts.getOrElse(i) { 0 } > startCounts.getOrElse(i) { 0 }
                    Divider()
                    StepRow(Icons.Rounded.Widgets, style.name(ar), sizeLabel(style.shape), done, false) {
                        Haptics.click(ctx); pin(ctx, set.dress(st), style.shape)
                    }
                }
            }
            Spacer(Modifier.height(Sp.l))
            Text(tr("Tip: each widget can still be changed later from My widgets.", "تقدر تعدّل أي ويدجت بعدين من «ويدجتاتي»."), fontSize = 12.sp, color = Z.sub)
            Spacer(Modifier.height(Sp.xxl))
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("", { nav.back() })
        if (wide) Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { preview() }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding()) { steps() }
        } else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding()) { preview(); steps() }
    }
}

@Composable
private fun StepRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, done: Boolean, busy: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Row(Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onClick).heightIn(min = 64.dp).padding(horizontal = Sp.l, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, if (done) Color(0xFF34C77B) else accent)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Z.text, maxLines = 1)
            Text(sub, fontSize = 12.sp, color = Z.sub)
        }
        if (done) Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF34C77B)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        } else Text(if (busy) "…" else tr("Add", "إضافة"), color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End)
    }
}
