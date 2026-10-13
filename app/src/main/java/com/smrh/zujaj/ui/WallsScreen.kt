package com.smrh.zujaj.ui

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object WallGen {
    data class Kind(val en: String, val ar: String, val pro: Boolean)

    val kinds = listOf(
        Kind("Aurora", "شفق", false), Kind("Mesh", "تدرج", false), Kind("Dunes", "كثبان", false), Kind("Arabesque", "زخرفة", true),
        Kind("Glass", "زجاج", true), Kind("Bokeh", "أضواء", false), Kind("Minimal", "بسيط", false), Kind("Night", "ليل", true),
    )

    private val palettes = listOf(
        intArrayOf(0xFF0B0B14.toInt(), 0xFF4C3BCF.toInt(), 0xFFE8478B.toInt(), 0xFFFFA25C.toInt()),
        intArrayOf(0xFF07131F.toInt(), 0xFF0E5068.toInt(), 0xFF38C6D9.toInt(), 0xFF3DD9B4.toInt()),
        intArrayOf(0xFF120D06.toInt(), 0xFF5A3D12.toInt(), 0xFFE7B75F.toInt(), 0xFFF6E0B0.toInt()),
        intArrayOf(0xFF140A12.toInt(), 0xFF5C1F3A.toInt(), 0xFFFF6B9A.toInt(), 0xFFFFC3D6.toInt()),
        intArrayOf(0xFF0A0F0C.toInt(), 0xFF1F3B2D.toInt(), 0xFF2EB67D.toInt(), 0xFFB9F5D0.toInt()),
        intArrayOf(0xFF0D0D10.toInt(), 0xFF2A2A33.toInt(), 0xFF8B7CFF.toInt(), 0xFFE0DCFF.toInt()),
        intArrayOf(0xFF100808.toInt(), 0xFF6A1F12.toInt(), 0xFFFF8A4C.toInt(), 0xFFFFD29A.toInt()),
    )

    fun make(kind: Int, seed: Long, w: Int, h: Int): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val rnd = Random(seed)
        val pal = palettes[(seed % palettes.size).toInt().let { if (it < 0) -it else it }]
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        c.drawColor(pal[0])
        val W = w.toFloat(); val H = h.toFloat()
        fun blob(x: Float, y: Float, r: Float, col: Int, a: Int) {
            p.shader = RadialGradient(x, y, r, Color.argb(a, Color.red(col), Color.green(col), Color.blue(col)), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, r, p); p.shader = null
        }
        when (kind) {
            0 -> repeat(6) { blob(rnd.nextFloat() * W, rnd.nextFloat() * H, W * (0.5f + rnd.nextFloat() * 0.6f), pal[1 + it % 3], 200) }
            1 -> {
                blob(0f, 0f, W * 1.2f, pal[1], 255); blob(W, 0f, W * 1.1f, pal[2], 230)
                blob(0f, H, W * 1.1f, pal[3], 220); blob(W, H, W * 1.2f, pal[1], 255)
                blob(W * 0.5f, H * 0.5f, W * 0.7f, pal[2], 160)
            }
            2 -> {
                for (i in 0 until 7) {
                    val y0 = H * (0.3f + i * 0.1f)
                    val path = Path(); path.moveTo(0f, H); path.lineTo(0f, y0)
                    val amp = H * 0.04f * (1 + rnd.nextFloat()); val ph = rnd.nextFloat() * 6f
                    var x = 0f
                    while (x <= W) { path.lineTo(x, y0 + sin(x / W * 6.28f * 1.3f + ph) * amp); x += W / 60 }
                    path.lineTo(W, H); path.close()
                    val t = i / 7f
                    p.shader = LinearGradient(0f, y0, 0f, H, RenderMix.mix(pal[2], pal[1], t), pal[0], Shader.TileMode.CLAMP)
                    p.alpha = 230
                    c.drawPath(path, p); p.shader = null; p.alpha = 255
                }
            }
            3 -> {
                blob(W * 0.5f, H * 0.3f, W, pal[1], 120)
                p.style = Paint.Style.STROKE; p.strokeWidth = W / 360f; p.color = pal[2]; p.alpha = 140
                val s = W / 6f
                var y = -s
                while (y < H + s) {
                    var x = -s
                    while (x < W + s) { star(c, x, y, s * 0.48f, p); x += s }
                    y += s
                }
                p.style = Paint.Style.FILL
            }
            4 -> {
                blob(W * 0.2f, H * 0.2f, W * 0.9f, pal[1], 230); blob(W * 0.9f, H * 0.8f, W * 0.9f, pal[2], 200)
                repeat(5) {
                    val rw = W * (0.35f + rnd.nextFloat() * 0.4f); val rh = rw * (0.6f + rnd.nextFloat())
                    val x = rnd.nextFloat() * (W - rw); val y = rnd.nextFloat() * (H - rh)
                    val r = RectF(x, y, x + rw, y + rh)
                    p.shader = LinearGradient(r.left, r.top, r.right, r.bottom, 0x40FFFFFF, 0x0AFFFFFF, Shader.TileMode.CLAMP)
                    c.drawRoundRect(r, rw * 0.12f, rw * 0.12f, p); p.shader = null
                    p.style = Paint.Style.STROKE; p.strokeWidth = W / 300f; p.color = 0x50FFFFFF
                    c.drawRoundRect(r, rw * 0.12f, rw * 0.12f, p); p.style = Paint.Style.FILL
                }
            }
            5 -> {
                blob(W * 0.5f, H * 0.5f, H * 0.7f, pal[1], 160)
                repeat(40) {
                    val r = W * (0.02f + rnd.nextFloat() * 0.12f)
                    blob(rnd.nextFloat() * W, rnd.nextFloat() * H, r, pal[2 + it % 2], 60 + rnd.nextInt(120))
                }
            }
            6 -> {
                p.shader = LinearGradient(0f, 0f, 0f, H, pal[1], pal[0], Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, W, H, p); p.shader = null
                blob(W * 0.8f, H * 0.15f, W * 0.5f, pal[3], 70)
            }
            else -> {
                p.shader = LinearGradient(0f, 0f, 0f, H, pal[0], pal[1], Shader.TileMode.CLAMP)
                c.drawRect(0f, 0f, W, H, p); p.shader = null
                repeat(260) {
                    p.color = Color.WHITE; p.alpha = 60 + rnd.nextInt(195)
                    c.drawCircle(rnd.nextFloat() * W, rnd.nextFloat() * H * 0.8f, W / 900f * (1 + rnd.nextFloat() * 2), p)
                }
                p.alpha = 255
                val mx = W * 0.72f; val my = H * 0.2f; val mr = W * 0.12f
                blob(mx, my, mr * 3f, pal[3], 60)
                val moon = Path().apply { addCircle(mx, my, mr, Path.Direction.CW) }
                moon.op(Path().apply { addCircle(mx + mr * 0.45f, my - mr * 0.25f, mr * 0.9f, Path.Direction.CW) }, Path.Op.DIFFERENCE)
                p.color = 0xFFF6EBC8.toInt(); c.drawPath(moon, p)
            }
        }
        return b
    }

    private fun star(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint) {
        val path = Path()
        for (i in 0 until 16) {
            val a = Math.toRadians(i * 22.5 - 90)
            val rr = if (i % 2 == 0) r else r * 0.62f
            val x = cx + (cos(a) * rr).toFloat(); val y = cy + (sin(a) * rr).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        c.drawPath(path, p)
        c.drawCircle(cx, cy, r * 0.3f, p)
    }

    fun apply(ctx: Context, b: Bitmap, which: Int): Boolean = runCatching {
        val wm = WallpaperManager.getInstance(ctx)
        val flags = when (which) { 0 -> WallpaperManager.FLAG_SYSTEM; 1 -> WallpaperManager.FLAG_LOCK; else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK }
        wm.setBitmap(b, null, true, flags)
        true
    }.getOrDefault(false)

    fun save(ctx: Context, b: Bitmap): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "zujaj_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zujaj")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        ctx.contentResolver.openOutputStream(uri)?.use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }.getOrDefault(false)
}

object RenderMix {
    fun mix(a: Int, b: Int, t: Float) = com.smrh.zujaj.render.RenderCtx.mix(a, b, t)
}

@Composable
fun WallsTab(nav: Nav) {
    val wallItems = remember { WallGen.kinds.indices.flatMap { k -> (0 until 3).map { k to (k * 7L + it * 3L + 1) } } }
    Column(Modifier.fillMaxSize()) {
        Text(tr("Generated wallpapers made for glass widgets.", "خلفيات مولّدة مصممة لتناسب الويدجت الزجاجية."),
            color = Z.sub, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = pagePadding, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(wallItems) { (k, seed) ->
                val img by produceState<ImageBitmap?>(null, k, seed) { value = withContext(Dispatchers.Default) { WallGen.make(k, seed, 270, 585).asImageBitmap() } }
                Box(Modifier.aspectRatio(270f / 585f).clip(RoundedCornerShape(16.dp)).clickable { nav.go(Screen.Wall(k, seed)) }) {
                    img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                    Text(WallGen.kinds[k].let { if (Prefs.isArabicUi) it.ar else it.en } + if (WallGen.kinds[k].pro) " 👑" else "",
                        fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
                }
            }
        }
    }
}

@Composable
fun WallScreen(nav: Nav, s: Screen.Wall) {
    val ctx = LocalContext.current
    var seed by remember { mutableLongStateOf(s.seed) }
    val scope = rememberCoroutineScope()
    val dm = ctx.resources.displayMetrics
    val full by produceState<Bitmap?>(null, seed) { value = withContext(Dispatchers.Default) { WallGen.make(s.kind, seed, dm.widthPixels, dm.heightPixels) } }
    val locked = WallGen.kinds[s.kind].pro && !Prefs.isPro && !Flags.flags.value.freeAll
    Box(Modifier.fillMaxSize()) {
        full?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        TopBar(WallGen.kinds[s.kind].let { if (Prefs.isArabicUi) it.ar else it.en }, { nav.back() }) {
            IconButton(onClick = { seed = Random.nextLong(1, 1_000_000) }) { Icon(Icons.Rounded.Shuffle, null) }
        }
        Column(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (locked) {
                PrimaryButton(tr("Unlock with Pro", "افتح مع برو")) { nav.go(Screen.Paywall) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(tr("Home", "الرئيسية") to 0, tr("Lock", "القفل") to 1, tr("Both", "الاثنين") to 2).forEach { (l, w) ->
                        ZChip(l, w == 2) {
                            val b = full ?: return@ZChip
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { WallGen.apply(ctx, b, w) }
                                Toast.makeText(ctx, if (ok) tr("Wallpaper set ✓", "تم تعيين الخلفية ✓") else tr("Couldn't set wallpaper", "تعذر تعيين الخلفية"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    ZChip(tr("Save", "حفظ"), false) {
                        val b = full ?: return@ZChip
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { WallGen.save(ctx, b) }
                            Toast.makeText(ctx, if (ok) tr("Saved to Pictures/Zujaj", "تم الحفظ في الصور/Zujaj") else tr("Couldn't save", "تعذر الحفظ"), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}

@Suppress("unused")
private fun unusedPaint() = RectF()
