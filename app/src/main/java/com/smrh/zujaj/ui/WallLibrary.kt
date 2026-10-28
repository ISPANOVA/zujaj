package com.smrh.zujaj.ui

import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.AddPhotoAlternate
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smrh.zujaj.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * 50 free wallpapers, all drawn in code (no image files): 10 calm scenes × 5 soft palettes.
 * Each palette is 4 colours from deep to light so every scene stays easy on the eyes.
 */
object WallLibrary {
    data class Pal(val en: String, val ar: String, val c: IntArray)
    data class Wall(val id: Int, val scene: Int, val pal: Pal) {
        fun name(ar: Boolean) = if (ar) "${scenes[scene].second} · ${pal.ar}" else "${scenes[scene].first} · ${pal.en}"
    }

    private fun c(v: Long) = v.toInt()
    val palettes = listOf(
        Pal("Midnight", "منتصف الليل", intArrayOf(c(0xFF07070F), c(0xFF17153A), c(0xFF4B3F9E), c(0xFFA99BFF))),
        Pal("Lagoon", "البحيرة", intArrayOf(c(0xFF041417), c(0xFF0B3B42), c(0xFF1F8A8A), c(0xFF9CE6DA))),
        Pal("Dusk", "الغروب", intArrayOf(c(0xFF140B1C), c(0xFF4A1F3D), c(0xFFC0566B), c(0xFFF7B79A))),
        Pal("Sand", "الرمل", intArrayOf(c(0xFF1A120B), c(0xFF5C3A22), c(0xFFC28A52), c(0xFFF1D6A8))),
        Pal("Forest", "الغابة", intArrayOf(c(0xFF07120C), c(0xFF163A27), c(0xFF4E8A5E), c(0xFFC4E3B5))),
        Pal("Rose", "الورد", intArrayOf(c(0xFF1C0A14), c(0xFF5E2342), c(0xFFD07197), c(0xFFF8C9DA))),
        Pal("Ocean", "المحيط", intArrayOf(c(0xFF050B1A), c(0xFF0E2A5A), c(0xFF2F6FC0), c(0xFFA6CCF5))),
        Pal("Ember", "الجمر", intArrayOf(c(0xFF120806), c(0xFF4A1A0E), c(0xFFD0643A), c(0xFFFFC98F))),
        Pal("Lavender", "اللافندر", intArrayOf(c(0xFF100C1A), c(0xFF35295A), c(0xFF8C79C9), c(0xFFE2D8FA))),
        Pal("Mist", "الضباب", intArrayOf(c(0xFF0D1014), c(0xFF2B333D), c(0xFF6F7E8E), c(0xFFDCE3EA))),
    )
    val scenes = listOf(
        "Silk" to "حرير", "Aurora" to "شفق", "Dunes" to "كثبان", "Peaks" to "جبال", "Tide" to "موج",
        "Bokeh" to "أضواء", "Night" to "ليل", "Blobs" to "سائل", "Arcs" to "أقواس", "Glass" to "زجاج",
    )
    /** Each scene gets the 5 palettes that suit it best. */
    private val pick = listOf(
        listOf(0, 2, 5, 1, 8), listOf(0, 1, 6, 8, 4), listOf(3, 2, 7, 5, 0), listOf(9, 6, 2, 4, 8), listOf(6, 1, 9, 2, 0),
        listOf(0, 5, 7, 1, 8), listOf(0, 6, 8, 1, 2), listOf(8, 5, 1, 2, 6), listOf(3, 0, 4, 9, 7), listOf(9, 8, 1, 5, 6),
    )
    val all: List<Wall> = scenes.indices.flatMap { s -> pick[s].map { p -> s to p } }.mapIndexed { i, (s, p) -> Wall(i, s, palettes[p]) }

    fun paint(wp: Wall, w: Int, h: Int): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val k = wp.pal.c; val W = w.toFloat(); val H = h.toFloat(); val u = W / 1080f
        val rnd = java.util.Random(wp.id * 7919L + 11)
        fun a(col: Int, al: Int) = (col and 0x00FFFFFF) or (al shl 24)
        fun vgrad(vararg cols: Int) { p.shader = LinearGradient(0f, 0f, 0f, H, cols, null, Shader.TileMode.CLAMP); cv.drawRect(0f, 0f, W, H, p); p.shader = null }
        fun glow(x: Float, y: Float, r: Float, col: Int) { p.shader = RadialGradient(x, y, r, col, a(col, 0), Shader.TileMode.CLAMP); cv.drawCircle(x, y, r, p); p.shader = null }
        fun soft(x: Float, y: Float, r: Float, col: Int, blur: Float) { p.maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL); p.color = col; cv.drawCircle(x, y, r, p); p.maskFilter = null }
        fun stars(n: Int, maxY: Float) = repeat(n) {
            p.color = android.graphics.Color.argb(60 + rnd.nextInt(150), 255, 250, 235)
            cv.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H * maxY, (0.8f + rnd.nextFloat() * 2.2f) * u, p)
        }
        fun ridge(y0: Float, amp: Float, col: Int, jag: Boolean) {
            val path = Path(); path.moveTo(0f, H); path.lineTo(0f, y0)
            val n = if (jag) 9 else 4
            for (i in 1..n) {
                val x = W * i / n; val y = y0 + (rnd.nextFloat() - 0.5f) * amp * 2
                if (jag) path.lineTo(x, y) else path.quadTo(x - W / n / 2, y0 + (rnd.nextFloat() - 0.6f) * amp * 2, x, y)
            }
            path.lineTo(W, H); path.close(); p.color = col; cv.drawPath(path, p)
        }
        when (wp.scene) {
            0 -> { // silk: smooth diagonal gradient with soft folds
                p.shader = LinearGradient(0f, 0f, W, H, intArrayOf(k[0], k[1], k[2], k[1]), floatArrayOf(0f, 0.4f, 0.75f, 1f), Shader.TileMode.CLAMP)
                cv.drawRect(0f, 0f, W, H, p); p.shader = null
                repeat(4) { i ->
                    val path = Path(); val y = H * (0.25f + i * 0.17f)
                    path.moveTo(-W * 0.1f, y); path.cubicTo(W * 0.3f, y - H * 0.18f, W * 0.6f, y + H * 0.2f, W * 1.1f, y - H * 0.05f)
                    p.style = Paint.Style.STROKE; p.strokeWidth = W * (0.16f - i * 0.025f)
                    p.maskFilter = BlurMaskFilter(W * 0.07f, BlurMaskFilter.Blur.NORMAL); p.color = a(k[3], 34 - i * 5); cv.drawPath(path, p)
                    p.maskFilter = null; p.style = Paint.Style.FILL
                }
            }
            1 -> { // aurora ribbons over a starry sky
                vgrad(k[0], k[0], k[1])
                stars(90, 0.7f)
                repeat(3) { i ->
                    val path = Path(); val y = H * (0.28f + i * 0.09f)
                    path.moveTo(-W * 0.1f, y)
                    for (s in 1..24) { val t = s / 24f; path.lineTo(W * 1.2f * t - W * 0.1f, y + sin(t * 5.2f + i * 1.3f) * H * 0.06f) }
                    p.style = Paint.Style.STROKE; p.strokeWidth = H * 0.09f
                    p.maskFilter = BlurMaskFilter(H * 0.05f, BlurMaskFilter.Blur.NORMAL); p.color = a(if (i == 1) k[3] else k[2], 70)
                    cv.drawPath(path, p); p.maskFilter = null; p.style = Paint.Style.FILL
                }
                ridge(H * 0.86f, H * 0.02f, k[0], false)
            }
            2 -> { // dunes under a low sun
                vgrad(k[1], k[2], k[3])
                glow(W * 0.62f, H * 0.5f, W * 0.6f, a(k[3], 140))
                p.color = a(0xFFFFFFFF.toInt(), 200); cv.drawCircle(W * 0.62f, H * 0.5f, W * 0.08f, p)
                listOf(0.62f, 0.72f, 0.82f, 0.92f).forEachIndexed { i, y ->
                    val path = Path(); path.moveTo(0f, H); path.lineTo(0f, H * y)
                    path.cubicTo(W * (0.25f + 0.1f * i), H * (y - 0.07f), W * (0.55f - 0.05f * i), H * (y + 0.05f), W, H * (y - 0.03f))
                    path.lineTo(W, H); path.close()
                    p.color = blend(k[2], k[0], 0.25f + i * 0.22f); cv.drawPath(path, p)
                }
            }
            3 -> { // layered mountain ridges with haze
                vgrad(k[3], k[2], k[1])
                glow(W * 0.3f, H * 0.3f, W * 0.5f, a(0xFFFFFFFF.toInt(), 60))
                for (i in 0 until 5) ridge(H * (0.5f + i * 0.1f), H * 0.05f, blend(k[2], k[0], 0.2f + i * 0.2f), true)
            }
            4 -> { // calm tide: stacked sine bands
                vgrad(k[3], k[2], k[1], k[0])
                for (i in 0 until 7) {
                    val y0 = H * (0.42f + i * 0.085f); val path = Path(); path.moveTo(0f, H); path.lineTo(0f, y0)
                    for (s in 1..30) { val t = s / 30f; path.lineTo(W * t, y0 + sin(t * 6.28f * 1.3f + i) * H * 0.012f) }
                    path.lineTo(W, H); path.close()
                    p.color = a(blend(k[2], k[0], i / 6f), 200); cv.drawPath(path, p)
                }
            }
            5 -> { // bokeh lights
                vgrad(k[0], k[1], k[0])
                repeat(34) {
                    val r = W * (0.03f + rnd.nextFloat() * 0.11f)
                    soft(rnd.nextFloat() * W, rnd.nextFloat() * H, r, a(if (rnd.nextBoolean()) k[2] else k[3], 40 + rnd.nextInt(60)), r * 0.35f)
                }
            }
            6 -> { // night: moon, stars and a soft horizon
                vgrad(k[0], k[1], k[2])
                stars(160, 0.8f)
                glow(W * 0.7f, H * 0.22f, W * 0.45f, a(k[3], 70))
                p.color = blend(k[3], 0xFFFFFFFF.toInt(), 0.6f); cv.drawCircle(W * 0.7f, H * 0.22f, W * 0.075f, p)
                ridge(H * 0.84f, H * 0.03f, blend(k[0], k[1], 0.4f), false)
                ridge(H * 0.9f, H * 0.02f, k[0], false)
            }
            7 -> { // liquid blobs melting into each other
                vgrad(k[1], k[0])
                repeat(5) { i ->
                    val r = W * (0.35f + rnd.nextFloat() * 0.3f)
                    soft(W * (0.1f + rnd.nextFloat() * 0.8f), H * (0.1f + i * 0.2f), r, a(k[2 + (i % 2)], 120), r * 0.55f)
                }
            }
            8 -> { // concentric arcs, like a soft arch
                vgrad(k[0], k[1])
                p.style = Paint.Style.STROKE
                for (i in 0 until 14) {
                    p.strokeWidth = W * 0.018f; p.color = a(blend(k[2], k[3], i / 13f), 30 + i * 6)
                    val r = W * (0.15f + i * 0.09f)
                    cv.drawArc(W / 2 - r, H * 0.95f - r, W / 2 + r, H * 0.95f + r, 180f, 180f, false, p)
                }
                p.style = Paint.Style.FILL
                glow(W / 2, H * 0.95f, W * 0.5f, a(k[3], 80))
            }
            else -> { // frosted glass panes
                p.shader = LinearGradient(0f, 0f, W, H, intArrayOf(k[1], k[0], k[2]), null, Shader.TileMode.CLAMP); cv.drawRect(0f, 0f, W, H, p); p.shader = null
                glow(W * 0.2f, H * 0.25f, W * 0.7f, a(k[3], 90)); glow(W * 0.85f, H * 0.75f, W * 0.6f, a(k[2], 110))
                repeat(6) { i ->
                    val cx = W * (0.15f + rnd.nextFloat() * 0.7f); val cy = H * (0.12f + i * 0.15f)
                    val ww = W * (0.3f + rnd.nextFloat() * 0.35f); val hh = ww * (0.55f + rnd.nextFloat() * 0.6f)
                    val rot = (rnd.nextFloat() - 0.5f) * 30f
                    cv.save(); cv.rotate(rot, cx, cy)
                    p.color = a(0xFFFFFFFF.toInt(), 16); cv.drawRoundRect(cx - ww / 2, cy - hh / 2, cx + ww / 2, cy + hh / 2, 60 * u, 60 * u, p)
                    p.style = Paint.Style.STROKE; p.strokeWidth = 2.2f * u; p.color = a(0xFFFFFFFF.toInt(), 38)
                    cv.drawRoundRect(cx - ww / 2, cy - hh / 2, cx + ww / 2, cy + hh / 2, 60 * u, 60 * u, p); p.style = Paint.Style.FILL
                    cv.restore()
                }
            }
        }
        // a whisper of grain hides gradient banding
        repeat((W * H / 900).toInt().coerceAtMost(6000)) {
            p.color = if (rnd.nextBoolean()) 0x08FFFFFF else 0x08000000
            val x = rnd.nextFloat() * W; val y = rnd.nextFloat() * H
            cv.drawRect(x, y, x + 1.5f * u + 1f, y + 1.5f * u + 1f, p)
        }
        return b
    }

    private fun blend(x: Int, y: Int, t: Float): Int {
        fun ch(s: Int) = (((x shr s) and 255) * (1 - t) + ((y shr s) and 255) * t).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }

    /** where: 1 = home, 2 = lock, 3 = both. */
    suspend fun apply(ctx: android.content.Context, wp: Wall, where: Int): Boolean = withContext(Dispatchers.Default) {
        runCatching {
            val dm = ctx.resources.displayMetrics
            val w = minOf(dm.widthPixels, dm.heightPixels); val h = maxOf(dm.widthPixels, dm.heightPixels)
            val bmp = paint(wp, w, h)
            val wm = WallpaperManager.getInstance(ctx)
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                val f = (if (where and 1 != 0) WallpaperManager.FLAG_SYSTEM else 0) or (if (where and 2 != 0) WallpaperManager.FLAG_LOCK else 0)
                wm.setBitmap(bmp, null, true, f)
            } else wm.setBitmap(bmp)
            true
        }.getOrDefault(false)
    }

    /** Colour families for the filter: 0 all, 1 dark, 2 blue, 3 purple, 4 pink, 5 warm, 6 green. */
    val families = listOf("All" to "الكل", "Dark" to "غامق", "Blue" to "أزرق", "Purple" to "بنفسجي", "Pink" to "وردي", "Warm" to "دافي", "Green" to "أخضر")
    val familyDots = listOf(0xFF888888.toInt(), 0xFF1A1A22.toInt(), 0xFF2F6FC0.toInt(), 0xFF7B5CD6.toInt(), 0xFFD0618C.toInt(), 0xFFD0843A.toInt(), 0xFF3C9A72.toInt())
    fun family(argb: Int): Int {
        val hsv = FloatArray(3); android.graphics.Color.colorToHSV(argb, hsv)
        val (h, s, v) = Triple(hsv[0], hsv[1], hsv[2])
        if (s < 0.2f || v < 0.22f) return 1
        return when {
            h < 20 || h >= 330 -> 4
            h < 65 -> 5
            h < 170 -> 6
            h < 245 -> 2
            h < 290 -> 3
            else -> 4
        }
    }
    fun family(wp: Wall) = family(wp.pal.c[2])
    /** Average colour of a picture (its thumbnail), for the filter. */
    fun average(b: Bitmap): Int {
        val s = Bitmap.createScaledBitmap(b, 1, 1, true); return s.getPixel(0, 0)
    }

    private val thumbs = android.util.LruCache<Int, ImageBitmap>(60)
    suspend fun thumb(wp: Wall): ImageBitmap = thumbs.get(wp.id) ?: withContext(Dispatchers.Default) {
        paint(wp, 270, 585).asImageBitmap().also { thumbs.put(wp.id, it) }
    }
}

/** Hands a picture to the wallpaper editor (photo + clocks/widgets on top). */
object WallHandoff { @Volatile var path: String? = null }

/** One card in the Wallpapers tab: a built-in drawing or an uploaded picture. */
sealed class WallSrc {
    abstract val key: String; abstract val by: String
    data class Built(val wp: WallLibrary.Wall) : WallSrc() { override val key = "b${wp.id}"; override val by = "SMRH" }
    data class Remote(val item: com.smrh.zujaj.remote.Walls.Item) : WallSrc() { override val key = "r${item.id}"; override val by = item.by }
}

/** Centre-crops [src] to the phone's screen shape and sets it. where: 1 home, 2 lock, 3 both. */
suspend fun applyBitmap(ctx: android.content.Context, src: Bitmap, where: Int): Boolean = withContext(Dispatchers.Default) {
    runCatching {
        val dm = ctx.resources.displayMetrics
        val w = minOf(dm.widthPixels, dm.heightPixels); val h = maxOf(dm.widthPixels, dm.heightPixels)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val k = maxOf(w / src.width.toFloat(), h / src.height.toFloat())
        val dw = src.width * k; val dh = src.height * k
        Canvas(out).drawBitmap(src, null, android.graphics.RectF((w - dw) / 2, (h - dh) / 2, (w + dw) / 2, (h + dh) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        val wm = WallpaperManager.getInstance(ctx)
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            val f = (if (where and 1 != 0) WallpaperManager.FLAG_SYSTEM else 0) or (if (where and 2 != 0) WallpaperManager.FLAG_LOCK else 0)
            wm.setBitmap(out, null, true, f)
        } else wm.setBitmap(out)
        true
    }.getOrDefault(false)
}

/** Bottom-bar tab: Home (SMRH's wallpapers) and Community (approved uploads), plus the photo + widgets editor. */
@Composable
fun WallLibraryTab(nav: Nav) {
    val ctx = LocalContext.current
    val ar = Prefs.isArabicUi
    var page by rememberSaveable { mutableIntStateOf(0) }
    var open by remember { mutableStateOf<WallSrc?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var homeRemote by remember { mutableStateOf<List<com.smrh.zujaj.remote.Walls.Item>>(emptyList()) }
    var hidden by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var community by remember { mutableStateOf<List<com.smrh.zujaj.remote.Walls.Item>?>(null) }
    var err by remember { mutableStateOf(false) }
    LaunchedEffect(reload) {
        runCatching { com.smrh.zujaj.remote.Walls.hiddenBuiltIn() }.onSuccess { hidden = it }
        runCatching { com.smrh.zujaj.remote.Walls.approved("home") }.onSuccess { homeRemote = it }
        runCatching { com.smrh.zujaj.remote.Walls.approved("community") }.onSuccess { community = it; err = false }.onFailure { err = true; community = community ?: emptyList() }
    }
    // community upload: pick → name → send for review
    var picked by remember { mutableStateOf<Bitmap?>(null) }
    var name by rememberSaveable { mutableStateOf(Prefs.getString("wall_name")) }
    var sending by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { picked = withContext(Dispatchers.IO) { com.smrh.zujaj.remote.Walls.readPicked(ctx, uri) }
            if (picked == null) Toast.makeText(ctx, tr("Couldn't open this picture", "الصورة دي مش نافعة"), Toast.LENGTH_SHORT).show() }
    }
    val pane = LocalPaneWidth.current
    val cols = when { pane >= 840 -> 4; pane >= 600 -> 3; else -> 2 }
    var fam by rememberSaveable { mutableIntStateOf(0) }
    // colour family of uploaded pictures, worked out from their thumbnails
    var remoteFam by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(homeRemote, community) {
        remoteFam = withContext(Dispatchers.Default) {
            (homeRemote + community.orEmpty()).associate { it.id to (com.smrh.zujaj.remote.Walls.decode(it.thumb)?.let { b -> WallLibrary.family(WallLibrary.average(b)) } ?: 1) }
        }
    }
    fun famOf(s: WallSrc) = when (s) { is WallSrc.Built -> WallLibrary.family(s.wp); is WallSrc.Remote -> remoteFam[s.item.id] ?: -1 }
    val gut = if (pane >= 600) Sp.xl else Sp.l
    val list: List<WallSrc>? = (if (page == 0) homeRemote.map { WallSrc.Remote(it) } + WallLibrary.all.filter { it.id !in hidden }.map { WallSrc.Built(it) }
        else community?.map { WallSrc.Remote(it) })?.filter { fam == 0 || famOf(it) == fam }
    LazyVerticalGrid(GridCells.Fixed(cols), Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = gut, end = gut, top = Sp.l, bottom = LocalDockSpace.current),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "head") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tr("Wallpapers", "خلفيات"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Z.text)
                // the old "clock behind your photo" studio lives here now
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.l))
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(LocalAccent.current.copy(alpha = 0.32f), LocalAccent.current.copy(alpha = 0.12f), Z.surface)))
                    .border(1.dp, Z.line, RoundedCornerShape(Rad.l)).clickable { nav.go(Screen.Wallpapers) }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AddPhotoAlternate, null, tint = Z.text, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Your photo + widgets", "صورتك + ويدجت"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Z.text)
                        Text(tr("Put a clock behind your photo, add widgets, and your saved designs", "حط الساعة ورا صورتك وضيف ويدجت، وتصميماتك المحفوظة"), fontSize = 13.sp, color = Z.sub)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Z.sub)
                }
                Segmented(listOf(tr("Home", "الرئيسية"), tr("Community", "المجتمع")), page) { page = it }
                // colour filter
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WallLibrary.families.size) { i ->
                        val sel = i == fam
                        Row(Modifier.clip(RoundedCornerShape(50)).background(if (sel) LocalAccent.current.copy(alpha = 0.2f) else Z.surface)
                            .border(1.dp, if (sel) LocalAccent.current else Z.line, RoundedCornerShape(50))
                            .clickable { Haptics.tick(ctx); fam = i }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (i > 0) { Box(Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(WallLibrary.familyDots[i])).border(1.dp, Z.tint(0.3f), androidx.compose.foundation.shape.CircleShape)); Spacer(Modifier.width(6.dp)) }
                            Text(WallLibrary.families[i].let { if (ar) it.second else it.first }, fontSize = 13.sp, color = if (sel) Z.text else Z.sub, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                if (page == 1) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Wallpapers shared by people, after review.", "خلفيات شاركها الناس، بعد المراجعة."), fontSize = 13.sp, color = Z.sub, modifier = Modifier.weight(1f))
                    PrimaryButton(tr("Share yours", "شارك خلفيتك"), icon = Icons.Rounded.Upload) {
                        if (!com.smrh.zujaj.remote.Walls.canSubmit()) Toast.makeText(ctx, tr("You can share 3 a day — try again tomorrow", "تقدر تشارك 3 في اليوم — جرب بكره"), Toast.LENGTH_LONG).show()
                        else picker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                }
                if (page == 1 && err) Text(tr("Couldn't load — check the internet.", "معرفناش نحمّل — اتأكد من النت."), color = Color(0xFFE5484D), fontSize = 13.sp)
                if (page == 1 && community?.isEmpty() == true && !err) Text(tr("Nothing here yet — be the first to share!", "لسه مفيش حاجة — كن أول واحد يشارك!"), color = Z.sub, fontSize = 13.sp)
            }
        }
        if (list == null) items(6) { Box(Modifier.fillMaxWidth().aspectRatio(270f / 585f).clip(RoundedCornerShape(Rad.l)).shimmer()) }
        if (list?.isEmpty() == true && fam != 0) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(tr("No wallpapers in this colour here yet", "مفيش خلفيات باللون ده هنا لسه"), color = Z.sub, fontSize = 14.sp, modifier = Modifier.padding(vertical = 30.dp))
        }
        items(list.orEmpty(), key = { it.key }) { src ->
            Column {
                WallThumb(src, Modifier.fillMaxWidth().aspectRatio(270f / 585f).clickable { open = src })
                Text(src.by, fontSize = 12.sp, color = Z.sub, maxLines = 1, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp))
            }
        }
    }
    open?.let { WallSheet(it, nav) { open = null } }
    picked?.let { bmp ->
        ZDialog(Icons.Rounded.Upload, tr("Share this wallpaper", "شارك الخلفية دي"), tr("It shows in Community after review. Your name appears under it.", "هتظهر في المجتمع بعد المراجعة، واسمك هيتكتب تحتها."), onDismiss = { if (!sending) picked = null }) {
            Image(bmp.asImageBitmap(), null, Modifier.height(200.dp).aspectRatio(270f / 585f).clip(RoundedCornerShape(Rad.m)).align(Alignment.CenterHorizontally), contentScale = ContentScale.Crop)
            ZField(name, { name = it.take(30) }, tr("Your name (required)", "اسمك (لازم)"))
            PrimaryButton(if (sending) tr("Sending…", "بنبعت…") else tr("Send", "ابعت"), Modifier.fillMaxWidth()) {
                if (sending) return@PrimaryButton
                if (name.isBlank()) { Toast.makeText(ctx, tr("Write your name first", "اكتب اسمك الأول"), Toast.LENGTH_SHORT).show(); return@PrimaryButton }
                sending = true
                scope.launch(kotlinx.coroutines.NonCancellable) {
                    Prefs.setString("wall_name", name.trim())
                    val r = runCatching { com.smrh.zujaj.remote.Walls.upload(bmp, name, admin = false) }
                    sending = false
                    Toast.makeText(ctx, if (r.isSuccess) tr("Sent ✓ It appears after review", "اتبعتت ✓ هتظهر بعد المراجعة") else tr("Couldn't send — check the internet", "معرفناش نبعت — اتأكد من النت"), Toast.LENGTH_LONG).show()
                    if (r.isSuccess) picked = null
                }
            }
        }
    }
}

@Composable
fun WallThumb(src: WallSrc, modifier: Modifier) {
    val img by produceState<ImageBitmap?>(null, src.key) {
        value = when (src) {
            is WallSrc.Built -> WallLibrary.thumb(src.wp)
            is WallSrc.Remote -> withContext(Dispatchers.Default) { com.smrh.zujaj.remote.Walls.decode(src.item.thumb)?.asImageBitmap() }
        }
    }
    Box(modifier.clip(RoundedCornerShape(Rad.l)).then(if (img == null) Modifier.shimmer() else Modifier.background(Z.surface)).border(1.dp, Z.line, RoundedCornerShape(Rad.l))) {
        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun WallSheet(src: WallSrc, nav: Nav, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val big by produceState<Bitmap?>(null, src.key) {
        value = when (src) {
            is WallSrc.Built -> withContext(Dispatchers.Default) {
                val dm = ctx.resources.displayMetrics
                WallLibrary.paint(src.wp, minOf(dm.widthPixels, dm.heightPixels, 1440), maxOf(dm.widthPixels, dm.heightPixels).coerceAtMost(3120))
            }
            is WallSrc.Remote -> runCatching { com.smrh.zujaj.remote.Walls.fullImage(src.item.id) }.getOrNull()
        }
    }
    val preview = remember(big) { big?.asImageBitmap() }
    Dialog(onClose, DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(Rad.xl)).background(Z.surface).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxHeight(0.5f).aspectRatio(540f / 1170f).clip(RoundedCornerShape(Rad.l)).background(Z.surface2), contentAlignment = Alignment.Center) {
                if (preview != null) Image(preview, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else androidx.compose.material3.CircularProgressIndicator(color = LocalAccent.current)
            }
            Spacer(Modifier.height(10.dp))
            Text(tr("By ", "من ") + src.by, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Z.text)
            Spacer(Modifier.height(12.dp))
            fun go(where: Int) {
                val b = big ?: return
                if (busy) return; busy = true
                scope.launch {
                    val ok = applyBitmap(ctx, b, where)
                    busy = false
                    if (ok) { Haptics.success(ctx); com.smrh.zujaj.remote.Stats.wallpaperUsed() }
                    Toast.makeText(ctx, if (ok) tr("Wallpaper set ✓", "اتحطت الخلفية ✓") else tr("Your phone didn't allow changing the wallpaper", "تليفونك مسمحش بتغيير الخلفية"), Toast.LENGTH_SHORT).show()
                    if (ok) onClose()
                }
            }
            PrimaryButton(tr("Add clock & widgets on it", "ضيف ساعة وويدجت عليها"), Modifier.fillMaxWidth(), icon = Icons.Rounded.Widgets) {
                val b = big ?: return@PrimaryButton
                if (busy) return@PrimaryButton; busy = true
                scope.launch {
                    val f = withContext(Dispatchers.IO) {
                        java.io.File(com.smrh.zujaj.wall.WallDesign.dir(ctx), "photo_${System.currentTimeMillis()}.jpg").also { f -> f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
                    }
                    busy = false
                    WallHandoff.path = f.path
                    onClose(); nav.go(Screen.Wallpapers)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionBtn(Icons.Rounded.Smartphone, tr("Both", "الاتنين"), Modifier.weight(1f)) { go(3) }
                OptionBtn(Icons.Rounded.Home, tr("Home", "الرئيسية"), Modifier.weight(1f)) { go(1) }
                OptionBtn(Icons.Rounded.Lock, tr("Lock", "القفل"), Modifier.weight(1f)) { go(2) }
            }
            if (busy) Text(tr("Working…", "ثانية…"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun OptionBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(50)).border(1.dp, Z.line, RoundedCornerShape(50)).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Z.text, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
        Text(label, color = Z.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
    }
}
