package com.smrh.zujaj.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Lock
import com.smrh.zujaj.wall.WallHistory
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.wall.Segment
import com.smrh.zujaj.wall.WallDesign
import com.smrh.zujaj.wall.WallLayer
import com.smrh.zujaj.wall.WallRender
import com.smrh.zujaj.wall.WallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

// =============================================================================== gate
object WallGate {
    private const val HASH = "a8507078dfc0d0e845232dadf9612197aa72a5eb68c2c0137764bbe01a114d88"
    fun ok(p: String) = java.security.MessageDigest.getInstance("SHA-256").digest(p.trim().toByteArray()).joinToString("") { "%02x".format(it) } == HASH
    var unlocked: Boolean
        get() = Prefs.getString("wall_ok") == "1"
        set(v) = Prefs.setString("wall_ok", if (v) "1" else "")
}

/** "Wallpapers · coming soon" — only people with the password get in. */
@Composable
fun WallGateDialog(onDismiss: () -> Unit, onOk: () -> Unit) {
    var pass by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFF1B2030), Z.surface))).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(LocalAccent.current.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Wallpaper, null, tint = LocalAccent.current, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Wallpapers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Coming soon · قريباً", color = LocalAccent.current, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text(tr("Still in testing. Enter the password to try it.", "لسه تحت التجربة. اكتب الباسورد عشان تجربها."), color = Z.sub, textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(pass, { pass = it; err = false }, singleLine = true, isError = err, placeholder = { Text(tr("Password", "الباسورد")) },
                visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
            if (err) Text(tr("Wrong password", "الباسورد غلط"), color = Color(0xFFE5484D), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))
            PrimaryButton(tr("Open", "افتح"), Modifier.fillMaxWidth()) { if (WallGate.ok(pass)) { WallGate.unlocked = true; onOk() } else err = true }
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = Z.sub) }
        }
    }
}

// =============================================================================== editor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dm = ctx.resources.displayMetrics
    val screenW = minOf(dm.widthPixels, dm.heightPixels); val screenH = maxOf(dm.widthPixels, dm.heightPixels)
    val aspect = screenW / screenH.toFloat()
    var design by remember { mutableStateOf(WallDesign.load(ctx)) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var mask by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { photo = WallRender.loadImage(design.image); mask = WallRender.loadImage(design.mask, 1600) }
    }
    // save a moment after the last change (not on every finger move)
    LaunchedEffect(design) { kotlinx.coroutines.delay(500); withContext(Dispatchers.IO) { WallDesign.save(ctx, design) } }
    fun update(d: WallDesign) { design = d }
    var selected by remember { mutableStateOf<Long?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scanning by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showBlur by remember { mutableStateOf(false) }
    var showApply by remember { mutableStateOf(false) }
    var colorFor by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showBuy by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val owned by com.smrh.zujaj.billing.Billing.wall.collectAsState()
    val accent = LocalAccent.current
    val found = remember { Animatable(0f) }      // outline animation 0..1
    val subjectIn = remember { Animatable(1f) }  // subject fade over the layers

    // static parts of the preview — only rebuilt when the photo / mask / background settings change
    val pw = size.width; val ph = size.height
    val bg by produceState<ImageBitmap?>(null, photo, design.blur, design.dim, pw) {
        value = if (pw <= 0) null else withContext(Dispatchers.Default) { runCatching { WallRender.background(design, photo, pw, ph).asImageBitmap() }.getOrNull() }
    }
    val subject by produceState<ImageBitmap?>(null, photo, mask, design.dim, pw) {
        val p = photo; val m = mask
        value = if (pw <= 0 || p == null || m == null) null else withContext(Dispatchers.Default) { runCatching { WallRender.subject(design, p, m, pw, ph).asImageBitmap() }.getOrNull() }
    }
    val outline by produceState<ImageBitmap?>(null, photo, mask, pw) {
        val p = photo; val m = mask
        value = if (pw <= 0 || p == null || m == null) null else withContext(Dispatchers.Default) { runCatching { WallRender.outline(p, m, pw, ph, accent.toArgbInt()).asImageBitmap() }.getOrNull() }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val f = File(WallDesign.dir(ctx), "photo_${System.currentTimeMillis()}.jpg")
            withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { i.copyTo(it) } } }
            File(design.image).takeIf { design.image.isNotBlank() }?.delete()
            File(design.mask).takeIf { design.mask.isNotBlank() }?.delete()
            photo = withContext(Dispatchers.IO) { WallRender.loadImage(f.path) }
            mask = null
            update(design.copy(image = f.path, mask = "", layers = design.layers.map { it.copy(behind = false) }))
            busy = false
        }
    }

    fun add(l: WallLayer) { update(design.copy(layers = design.layers + l)); selected = l.id; showSheet = false }
    fun change(id: Long, f: (WallLayer) -> WallLayer) = update(design.copy(layers = design.layers.map { if (it.id == id) f(it) else it }))

    /** ✦ — find the subject (glowing outline), then slide the selected (or every) layer behind it. */
    fun depth() {
        if (photo == null) { Toast.makeText(ctx, tr("Choose a photo first", "اختار صورة الأول"), Toast.LENGTH_SHORT).show(); return }
        if (design.layers.isEmpty()) { Toast.makeText(ctx, tr("Add a clock or widget first", "ضيف ساعة أو ويدجت الأول"), Toast.LENGTH_SHORT).show(); return }
        scope.launch {
            var fresh = false
            if (mask == null) {
                scanning = true
                val r = withContext(Dispatchers.Default) { Segment.run(ctx, photo!!) }
                when (r) {
                    is Segment.Result.Ok -> {
                        val f = File(WallDesign.dir(ctx), "mask_${System.currentTimeMillis()}.png")
                        withContext(Dispatchers.IO) { f.outputStream().use { r.mask.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                        mask = r.mask; fresh = true
                        update(design.copy(mask = f.path))
                    }
                    is Segment.Result.Fail -> {
                        scanning = false
                        Toast.makeText(ctx, tr("Couldn't find a clear subject in this photo. Try another one.", "مالقيتش شكل واضح في الصورة دي. جرب صورة تانية."), Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
                scanning = false
            }
            val targets = selected?.let { s -> design.layers.filter { it.id == s } } ?: design.layers
            val goBehind = targets.any { !it.behind }
            if (goBehind) {
                // trace the subject with a glowing outline, then tuck the layers behind it
                kotlinx.coroutines.delay(120)
                found.snapTo(0f)
                found.animateTo(1f, tween(if (fresh) 1100 else 700, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                subjectIn.snapTo(0f)
                update(design.copy(layers = design.layers.map { l -> if (targets.any { it.id == l.id }) l.copy(behind = true) else l }))
                subjectIn.animateTo(1f, tween(650))
                found.animateTo(0f, tween(700))
            } else update(design.copy(layers = design.layers.map { l -> if (targets.any { it.id == l.id }) l.copy(behind = false) else l }))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                Text(tr("Wallpapers", "الخلفيات"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("BETA", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            IconButton(onClick = { showHistory = true }) { Icon(Icons.Rounded.Collections, null) }
            IconButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Rounded.Image, null) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                DropdownMenu(menu, { menu = false }, containerColor = Z.surface) {
                    DropdownMenuItem(text = { Text(tr("Change photo", "غيّر الصورة")) }, onClick = { menu = false; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    DropdownMenuItem(text = { Text(tr("Remove all layers", "امسح كل الإضافات")) }, onClick = { menu = false; selected = null; update(design.copy(layers = emptyList())) })
                    DropdownMenuItem(text = { Text(tr("Detect the shape again", "حدد الشكل من جديد")) }, onClick = { menu = false; mask = null; update(design.copy(mask = "", layers = design.layers.map { it.copy(behind = false) })) })
                    DropdownMenuItem(text = { Text(tr("Use horizon (sky) detection", "حدد بالسما (للجبال والمباني)")) }, onClick = {
                        menu = false
                        val p = photo ?: return@DropdownMenuItem
                        scope.launch {
                            val m = withContext(Dispatchers.Default) { WallRender.skyMask(p) }
                            if (m == null) { Toast.makeText(ctx, tr("No clear sky in this photo", "مفيش سما واضحة في الصورة"), Toast.LENGTH_SHORT).show(); return@launch }
                            val f = File(WallDesign.dir(ctx), "mask_${System.currentTimeMillis()}.png")
                            withContext(Dispatchers.IO) { f.outputStream().use { m.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                            mask = m; update(design.copy(mask = f.path))
                            found.snapTo(0f); found.animateTo(1f, tween(900)); found.animateTo(0f, tween(700))
                        }
                    })
                }
            }
        }
        // ---------------------------------------------------------------- canvas
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.aspectRatio(aspect).clip(RoundedCornerShape(26.dp)).background(Color(0xFF141824))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(26.dp))
                    .onSizeChanged { size = it }
            ) {
                if (photo == null) {
                    Column(Modifier.fillMaxSize().clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(76.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Image, null, tint = accent, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(tr("Choose a photo", "اختار صورة"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(tr("Any photo from your phone", "أي صورة من تليفونك"), color = Z.sub, fontSize = 13.sp)
                    }
                } else {
                    bg?.let { Image(it, null, Modifier.fillMaxSize()) }
                    val hasMask = mask != null && subject != null
                    design.layers.filter { it.behind && hasMask }.forEach { l -> LayerView(l, size, selected == l.id) }
                    if (hasMask && design.layers.any { it.behind }) subject?.let { Image(it, null, Modifier.fillMaxSize().graphicsLayer { alpha = subjectIn.value }) }
                    design.layers.filter { !it.behind || !hasMask }.forEach { l -> LayerView(l, size, selected == l.id) }
                    // "found it" — glowing outline sweeping across the subject
                    if (found.value > 0f) outline?.let { o ->
                        Canvas(Modifier.fillMaxSize()) {
                            val x = this.size.width * found.value
                            drawContext.canvas.save()
                            drawContext.canvas.clipRect(0f, 0f, x, this.size.height)
                            drawImage(o, alpha = (0.35f + 0.65f * found.value).coerceAtMost(1f))
                            drawContext.canvas.restore()
                            if (found.value < 1f) drawLine(Brush.verticalGradient(listOf(Color.Transparent, Color.White, Color.Transparent)), Offset(x, 0f), Offset(x, this.size.height), 4f)
                        }
                    }
                }
                // scanning shimmer while the subject is being found
                if (scanning) {
                    val t = rememberInfiniteTransition(label = "scan")
                    val y by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse), label = "y")
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.Black.copy(alpha = 0.25f))
                        val yy = this.size.height * y
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = 0.45f), Color.Transparent), yy - 120f, yy + 120f), Offset(0f, yy - 120f), androidx.compose.ui.geometry.Size(this.size.width, 240f))
                        drawLine(Color.White.copy(alpha = 0.9f), Offset(0f, yy), Offset(this.size.width, yy), 3f)
                    }
                    Text(tr("Finding the shape…", "بحدد الشكل اللي في الصورة…"), color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp))
                }
                // gestures: tap selects, drag moves smoothly (state only, nothing is re-rendered)
                if (photo != null) Box(Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { p ->
                            val hit = design.layers.lastOrNull { l -> WallRender.rect(l, size.width, size.height).contains(p.x, p.y) }
                            selected = hit?.id
                        }
                    }
                    .pointerInput(Unit) {
                        var id: Long? = null
                        detectDragGestures(
                            onDragStart = { p ->
                                val hit = design.layers.lastOrNull { l -> WallRender.rect(l, size.width, size.height).contains(p.x, p.y) }
                                if (hit != null) selected = hit.id
                                id = hit?.id
                            },
                            onDragEnd = { snapX = false; id = null },
                            onDragCancel = { snapX = false; id = null },
                        ) { ch, d ->
                            val lid = id ?: return@detectDragGestures
                            ch.consume()
                            change(lid) { l ->
                                var cx = l.cx + d.x / size.width
                                val near = abs(cx - 0.5f) < 0.012f
                                if (near && !snapX) cx = 0.5f
                                snapX = near
                                l.copy(cx = cx.coerceIn(0.02f, 0.98f), cy = (l.cy + d.y / size.height).coerceIn(0.02f, 0.98f))
                            }
                        }
                    })
                if (snapX) Canvas(Modifier.fillMaxSize()) { drawLine(accent, Offset(this.size.width / 2, 0f), Offset(this.size.width / 2, this.size.height), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))) }
                // selection box + handles
                design.layers.firstOrNull { it.id == selected }?.let { l ->
                    val rc = WallRender.rect(l, size.width, size.height)
                    val dens = LocalDensity.current
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.White, Offset(rc.left, rc.top), androidx.compose.ui.geometry.Size(rc.width(), rc.height()),
                            style = Stroke(2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))))
                    }
                    @Composable
                    fun handle(x: Float, y: Float, icon: androidx.compose.ui.graphics.vector.ImageVector, mod: Modifier = Modifier, onClick: (() -> Unit)? = null) {
                        val s = with(dens) { 30.dp.toPx() }
                        Box(Modifier.offset { IntOffset((x - s / 2).roundToInt(), (y - s / 2).roundToInt()) }.size(30.dp).clip(CircleShape).background(Color.White)
                            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).then(mod), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = Color.Black, modifier = Modifier.size(17.dp))
                        }
                    }
                    handle(rc.right, rc.top, Icons.Rounded.Close) { selected = null; update(design.copy(layers = design.layers.filter { it.id != l.id })) }
                    handle(rc.left, rc.top, Icons.Rounded.ContentCopy) { add(l.copy(id = System.nanoTime(), cy = (l.cy + 0.08f).coerceAtMost(0.95f))) }
                    handle(rc.right, rc.bottom, Icons.Rounded.Palette) { colorFor = l.id }
                    handle(rc.left, rc.bottom, Icons.Rounded.OpenInFull, Modifier.pointerInput(l.id) {
                        detectDragGestures { ch, d ->
                            ch.consume()
                            change(l.id) { it.copy(w = (it.w + (-d.x + d.y) / size.width * 1.4f).coerceIn(0.12f, 1.2f)) }
                        }
                    })
                }
                // ✦ depth button
                if (photo != null) {
                    val spin = rememberInfiniteTransition(label = "spark")
                    val r by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "r")
                    val on = design.layers.any { it.behind } && mask != null
                    Box(Modifier.align(Alignment.BottomStart).padding(14.dp).size(52.dp).clip(CircleShape)
                        .background(if (on) accent else Color.Black.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { depth() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = if (on) Color.Black else Color.White,
                            modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = if (scanning) r else 0f; val k = if (scanning) 1f + 0.12f * kotlin.math.sin(r / 57f) else 1f; scaleX = k; scaleY = k })
                    }
                }
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center), color = accent)
            }
        }
        // ---------------------------------------------------------------- actions
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            WallAction(Icons.Rounded.Wallpaper, tr("Apply", "تطبيق"), photo != null, locked = !owned) { if (owned) showApply = true else showBuy = true }
            WallAction(Icons.Rounded.Download, tr("Save", "حفظ"), photo != null, locked = !owned) {
                if (!owned) { showBuy = true; return@WallAction }
                scope.launch {
                    busy = true
                    val ok = withContext(Dispatchers.IO) { runCatching {
                        val full = WallRender.compose(ctx, design, photo, mask, screenW, screenH)
                        saveToGallery(ctx, full); WallHistory.add(ctx, design, full)
                    }.isSuccess }
                    busy = false
                    Toast.makeText(ctx, if (ok) tr("Saved to Photos ✓", "اتحفظت في الصور ✓") else tr("Couldn't save", "مقدرتش أحفظ"), Toast.LENGTH_SHORT).show()
                }
            }
            WallAction(Icons.Rounded.FormatQuote, tr("Text", "نص"), photo != null) { showText = true }
            WallAction(Icons.Rounded.Widgets, tr("Widgets", "ويدجت"), photo != null) { showSheet = true }
            WallAction(Icons.Rounded.WaterDrop, tr("Blur", "تغبيش"), photo != null) { showBlur = true }
        }
    }

    // ---------------------------------------------------------------- sheets & dialogs
    if (showSheet) ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = Color(0xFF1D2231)) { WidgetPickerSheet(onAdd = { add(it) }) }
    if (showText) {
        var t by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showText = false }, containerColor = Z.surface,
            title = { Text(tr("Your text", "كلامك")) },
            text = { OutlinedTextField(t, { t = it }, placeholder = { Text(tr("Type your text", "اكتب كلامك")) }) },
            confirmButton = { TextButton(onClick = { showText = false; if (t.isNotBlank()) add(WallLayer(kind = "text", text = t.trim(), w = 0.8f, cy = 0.62f)) }) { Text(tr("Add", "ضيف"), color = accent) } },
            dismissButton = { TextButton(onClick = { showText = false }) { Text(tr("Cancel", "إلغاء")) } })
    }
    if (showBlur) AlertDialog(onDismissRequest = { showBlur = false }, containerColor = Z.surface,
        title = { Text(tr("Background", "الخلفية")) },
        text = {
            Column {
                Text(tr("Blur", "التغبيش") + "  ${design.blur}%")
                Slider(design.blur.toFloat(), { update(design.copy(blur = it.roundToInt())) }, valueRange = 0f..100f)
                Text(tr("Darken", "التغميق") + "  ${design.dim}%")
                Slider(design.dim.toFloat(), { update(design.copy(dim = it.roundToInt())) }, valueRange = 0f..70f)
            }
        },
        confirmButton = { TextButton(onClick = { showBlur = false }) { Text(tr("Done", "تمام"), color = accent) } })
    colorFor?.let { id ->
        val l = design.layers.firstOrNull { it.id == id }
        if (l == null) colorFor = null
        else LayerColorSheet(l, onAuto = {
            val p = photo ?: return@LayerColorSheet
            val (a, b) = WallRender.autoColors(p, l, maxOf(size.width, 1), maxOf(size.height, 1))
            change(id) { it.copy(color = a, color2 = b, gDir = 0) }
        }, onChange = { nl -> change(id) { nl } }, onDismiss = { colorFor = null })
    }
    if (showBuy) WallBuySheet { showBuy = false }
    if (showHistory) ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = Color(0xFF1D2231)) {
        var items by remember { mutableStateOf(WallHistory.list(ctx)) }
        Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
            Text(tr("My wallpapers", "خلفياتي"), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(6.dp))
            if (items.isEmpty()) Text(tr("Wallpapers you save or apply show up here so you can edit them again.", "أي خلفية تحفظها أو تطبقها هتظهر هنا عشان تعدّل عليها تاني."),
                color = Z.sub, modifier = Modifier.padding(vertical = 30.dp, horizontal = 6.dp))
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 560.dp)) {
                items(items, key = { it.dir.name }) { it ->
                    val bmp by produceState<ImageBitmap?>(null, it.thumb) { value = withContext(Dispatchers.IO) { runCatching { android.graphics.BitmapFactory.decodeFile(it.thumb.path).asImageBitmap() }.getOrNull() } }
                    Box(Modifier.aspectRatio(aspect).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f))
                        .pointerInput(it.dir.name) {
                            detectTapGestures(
                                onTap = { _ ->
                                    scope.launch {
                                        val d = withContext(Dispatchers.IO) { WallHistory.open(ctx, it) }
                                        photo = withContext(Dispatchers.IO) { WallRender.loadImage(d.image) }
                                        mask = withContext(Dispatchers.IO) { WallRender.loadImage(d.mask, 1600) }
                                        design = d; selected = null; showHistory = false
                                    }
                                },
                                onLongPress = { _ -> WallHistory.delete(it); items = WallHistory.list(ctx) },
                            )
                        }) {
                        bmp?.let { b -> Image(b, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                    }
                }
            }
            if (items.isNotEmpty()) Text(tr("Tap to edit · long-press to delete", "اضغط للتعديل · اضغط مطولاً للمسح"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, start = 6.dp))
        }
    }
    if (showApply) ApplyWallDialog(onDismiss = { showApply = false }) { target, live ->
        showApply = false
        scope.launch {
            busy = true
            WallDesign.save(ctx, design)
            val msg = withContext(Dispatchers.IO) {
                runCatching { WallHistory.add(ctx, design, WallRender.compose(ctx, design, photo, mask, screenW / 2, screenH / 2)) }
                applyWallpaper(ctx, design, photo, mask, screenW, screenH, target, live)
            }
            busy = false
            msg?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
        }
    }
}

private fun Color.toArgbInt(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

/** One layer drawn from its own cached bitmap and placed with an offset — moving it is free. */
@Composable
private fun LayerView(l: WallLayer, box: IntSize, selected: Boolean) {
    val ctx = LocalContext.current
    if (box.width <= 0) return
    val rc = WallRender.rect(l, box.width, box.height)
    val wBucket = (rc.width() / 24f).roundToInt() * 24 // re-render only every 24 px of resizing
    val minute = rememberTicker(30_000L)
    val bmp by produceState<ImageBitmap?>(null, l.kind, l.styleId, l.text, l.color, l.color2, l.gDir, wBucket, minute) {
        value = withContext(Dispatchers.Default) { runCatching { WallRender.styled(ctx, l, wBucket.coerceAtLeast(24)).asImageBitmap() }.getOrNull() }
    }
    val dens = LocalDensity.current
    bmp?.let { b ->
        val h = rc.width() * b.height / b.width.toFloat()
        Image(b, null, Modifier
            .offset { IntOffset(rc.left.roundToInt(), (rc.centerY() - h / 2).roundToInt()) }
            .size(with(dens) { rc.width().toDp() }, with(dens) { h.toDp() })
            .graphicsLayer { shadowElevation = 0f; alpha = l.alpha / 255f })
    }
}

/** Organised picker: clocks & dates, then widgets by category; small ones in pairs, wide ones full width. */
@Composable
private fun WidgetPickerSheet(onAdd: (WallLayer) -> Unit) {
    val ctx = LocalContext.current
    val cats = remember { listOf("wall") + com.smrh.zujaj.catalog.Cat.values().filter { c -> Flags.visible().any { it.cat == c && it.shape != com.smrh.zujaj.catalog.Shape.LARGE } }.map { it.name } }
    var cat by remember { mutableStateOf("wall") }
    Column(Modifier.heightIn(max = 640.dp)) {
        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats.size) { i ->
                val k = cats[i]
                val label = if (k == "wall") tr("Clocks & dates", "ساعات وتواريخ") else com.smrh.zujaj.catalog.Cat.valueOf(k).let { if (Prefs.isArabicUi) it.ar else it.en }
                ZChip(label, cat == k) { cat = k }
            }
        }
        androidx.compose.animation.AnimatedContent(cat, label = "cat") { k ->
            LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (k == "wall") {
                    val list = WallRender.kinds.filter { it.first != "text" }
                    items(list, span = { (_, a) -> GridItemSpan(if (a < 2f) 1 else 2) }) { (kind, a) ->
                        val bmp by produceState<ImageBitmap?>(null, kind) { value = withContext(Dispatchers.Default) { runCatching { WallRender.layerBitmap(ctx, WallLayer(kind = kind), 460).asImageBitmap() }.getOrNull() } }
                        Box(Modifier.fillMaxWidth().height(if (a < 2f) 130.dp else 92.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                            .clickable { onAdd(WallLayer(kind = kind, w = if (a < 1.5f) 0.55f else 0.8f, cy = 0.3f)) }.padding(12.dp), contentAlignment = Alignment.Center) {
                            bmp?.let { Image(it, null, Modifier.fillMaxSize()) }
                        }
                    }
                } else {
                    val list = pack(Flags.visible().filter { it.cat.name == k && it.shape != com.smrh.zujaj.catalog.Shape.LARGE })
                    items(list, span = { GridItemSpan(if (it.shape == com.smrh.zujaj.catalog.Shape.SMALL) 1 else 2) }) { st ->
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                            .clickable { onAdd(WallLayer(kind = "widget", styleId = st.id, w = if (st.shape == com.smrh.zujaj.catalog.Shape.SMALL) 0.45f else 0.9f, cy = 0.35f)) }.padding(10.dp)) {
                            WidgetPreview(st.defaultConfig().copy(bg = false), Modifier.fillMaxWidth(), density = 1.6f)
                        }
                    }
                }
            }
        }
    }
}

/** Solid or gradient colour for a layer, plus "match the wallpaper". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerColorSheet(l: WallLayer, onAuto: () -> Unit, onChange: (WallLayer) -> Unit, onDismiss: () -> Unit) {
    val swatches = listOf(0xFFFFFFFF, 0xFF111111, 0xFFE7B75F, 0xFFF2865E, 0xFFE5484D, 0xFFFF6B9A, 0xFFB07BF0, 0xFF5B8DEF, 0xFF38C6D9, 0xFF3DDC97, 0xFFB7E34A, 0xFFF3E3C2).map { it.toInt() }
    var custom by remember { mutableIntStateOf(0) } // 1 = first colour, 2 = second
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1D2231)) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Colour", "اللون"), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f)).padding(3.dp)) {
                    listOf(tr("Solid", "سادة") to false, tr("Gradient", "متدرج") to true).forEach { (s, g) ->
                        val on = (l.color2 != 0) == g
                        Text(s, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (on) Color.Black else Z.text,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) LocalAccent.current else Color.Transparent)
                                .clickable { onChange(if (g) l.copy(color2 = if (l.color2 == 0) 0xFFE7B75F.toInt() else l.color2) else l.copy(color2 = 0)) }
                                .padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // ✨ match the wallpaper
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF7B5CD6).copy(alpha = 0.45f), Color(0xFFE7B75F).copy(alpha = 0.35f))))
                .clickable(onClick = onAuto).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(tr("Match the wallpaper", "على حسب الخلفية"), fontWeight = FontWeight.Bold)
                    Text(tr("Picks a gradient from your photo", "يختار تدرج متناسق من صورتك"), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            @Composable
            fun row(title: String, value: Int, which: Int, set: (Int) -> WallLayer) {
                Text(title, color = Z.sub, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(swatches.size) { i ->
                        val c = swatches[i]
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == value) 3.dp else 1.dp, if (c == value) LocalAccent.current else Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable { onChange(set(c)) })
                    }
                    item {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                            .clickable { custom = which })
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            row(if (l.color2 != 0) tr("From", "من") else tr("Colour", "اللون"), l.color, 1) { l.copy(color = it) }
            if (l.color2 != 0) {
                row(tr("To", "إلى"), l.color2, 2) { l.copy(color2 = it) }
                Text(tr("Direction", "الاتجاه"), color = Z.sub, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(tr("↓ Down", "↓ لتحت"), tr("→ Across", "↔ بالعرض"), tr("↘ Diagonal", "↘ مايل")).forEachIndexed { i, s -> ZChip(s, l.gDir == i) { onChange(l.copy(gDir = i)) } }
                }
            }
        }
    }
    if (custom != 0) ColorPickerDialog(if (custom == 1) l.color else l.color2, { custom = 0 }) { c -> onChange(if (custom == 1) l.copy(color = c) else l.copy(color2 = c)) }
}

@Composable
private fun WallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, locked: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f }) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(if (locked) Z.pro.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(25.dp))
            if (locked) Icon(Icons.Rounded.Lock, null, tint = Z.pro, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(13.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = Z.text)
    }
}

/** 0 home, 1 lock, 2 both. */
@Composable
private fun ApplyWallDialog(onDismiss: () -> Unit, onApply: (Int, Boolean) -> Unit) {
    var target by remember { mutableIntStateOf(2) }
    var live by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Z.surface,
        title = { Text(tr("Set as wallpaper", "تعيين كخلفية")) },
        text = {
            Column {
                listOf(tr("Home screen", "الشاشة الرئيسية"), tr("Lock screen", "شاشة القفل"), tr("Both", "الاتنين")).forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { target = i }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(target == i, { target = i }); Text(s)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Live clock", "ساعة حيّة"), fontWeight = FontWeight.SemiBold)
                        Text(tr("Keeps the time updating (live wallpaper). Off = a still picture.", "الوقت بيفضل يتحدث (خلفية حيّة). لو مقفول = صورة ثابتة."), color = Z.sub, fontSize = 12.sp)
                    }
                    Switch(live, { live = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(target, live) }) { Text(tr("Apply", "تطبيق"), color = LocalAccent.current, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء")) } })
}

private fun saveToGallery(ctx: android.content.Context, bmp: Bitmap) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "Zujaj_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zujaj")
    }
    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("no uri")
    ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
}

/** Returns a message to show, or null. */
private fun applyWallpaper(ctx: android.content.Context, d: WallDesign, photo: Bitmap?, mask: Bitmap?, W: Int, H: Int, target: Int, live: Boolean): String? {
    val wm = WallpaperManager.getInstance(ctx)
    return runCatching {
        if (live) {
            // still picture on the lock screen if asked, live wallpaper for the rest
            if (target == 1) wm.setBitmap(WallRender.compose(ctx, d, photo, mask, W, H), null, true, WallpaperManager.FLAG_LOCK)
            if (target != 1) {
                val i = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                    .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(ctx, WallService::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
                return tr("Tap “Set wallpaper” on the next screen", "اضغط «تعيين الخلفية» في الشاشة الجاية")
            }
            tr("Lock screen set ✓", "اتعينت على شاشة القفل ✓")
        } else {
            val flags = when (target) { 0 -> WallpaperManager.FLAG_SYSTEM; 1 -> WallpaperManager.FLAG_LOCK; else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK }
            wm.setBitmap(WallRender.compose(ctx, d, photo, mask, W, H), null, true, flags)
            tr("Wallpaper set ✓", "الخلفية اتعينت ✓")
        }
    }.getOrElse { tr("Couldn't set the wallpaper: ", "مقدرتش أعين الخلفية: ") + it.message }
}

@Suppress("unused") private val keepCatalog = Catalog


/** Wallpapers purchase sheet: monthly, yearly or lifetime (prices come from Google Play). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallBuySheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val plans by com.smrh.zujaj.billing.Billing.wallPlans.collectAsState()
    val owned by com.smrh.zujaj.billing.Billing.wall.collectAsState()
    var redeem by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(com.smrh.zujaj.billing.WALL_YEARLY) }
    LaunchedEffect(owned) { if (owned) onDismiss() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1D1A12)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Z.pro.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Wallpaper, null, tint = Z.pro, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(tr("Unlock Wallpapers", "افتح الخلفيات"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(tr("Apply your designs to the home & lock screen and save them to your photos.", "طبّق تصميماتك على الشاشة الرئيسية وشاشة القفل واحفظها في الصور."),
                color = Z.sub, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            listOf(tr("Clock behind the subject (depth effect)", "الساعة ورا الشكل (تأثير العمق)"), tr("Live clock that keeps updating", "ساعة حيّة بتتحدث لوحدها"),
                tr("Your designs saved to edit again", "تصميماتك محفوظة تعدّل عليها تاني")).forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Z.pro, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(10.dp)); Text(it, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            val options = listOf(
                Triple(com.smrh.zujaj.billing.WALL_MONTHLY, tr("Monthly", "شهري"), tr("Billed every month", "بيتجدد كل شهر")),
                Triple(com.smrh.zujaj.billing.WALL_YEARLY, tr("Yearly", "سنوي"), tr("Best value", "أوفر اختيار")),
                Triple(com.smrh.zujaj.billing.WALL_PRODUCT, tr("Lifetime", "مدى الحياة"), tr("Pay once, yours forever", "تدفع مرة واحدة وتبقى ليك")),
            )
            options.forEach { (id, name, sub) ->
                val on = pick == id
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (on) Z.pro.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
                    .border(if (on) 1.5.dp else 1.dp, if (on) Z.pro else Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .clickable { pick = id }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(sub, color = Z.sub, fontSize = 12.sp)
                    }
                    Text(plans[id]?.price ?: "—", fontWeight = FontWeight.Bold, color = if (on) Z.pro else Z.text)
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(tr("Continue", "متابعة"), Modifier.fillMaxWidth()) {
                val a = ctx as? android.app.Activity
                if (a == null || !com.smrh.zujaj.billing.Billing.buyWall(a, pick)) Toast.makeText(ctx, tr("Google Play isn't ready — try again in a moment", "جوجل بلاي مش جاهز، جرب كمان شوية"), Toast.LENGTH_SHORT).show()
            }
            Row {
                TextButton(onClick = { com.smrh.zujaj.billing.Billing.restore() }) { Text(tr("Restore", "استرجاع"), color = Z.sub) }
                TextButton(onClick = { redeem = true }) { Text(tr("Have a code?", "معاك كود؟"), color = Z.pro) }
            }
            Text(tr("Subscriptions renew automatically until cancelled in Google Play.", "الاشتراكات بتتجدد لوحدها لحد ما تلغيها من جوجل بلاي."), color = Z.sub, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
    if (redeem) RedeemDialog { redeem = false }
}

/** Shown once after the update: what the new Wallpapers page does. */
@Composable
fun WallIntroDialog(onTry: () -> Unit, onDismiss: () -> Unit) {
    val t = rememberInfiniteTransition(label = "intro")
    val glow by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "g")
    val sweep by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "s")
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(30.dp)).background(Brush.verticalGradient(listOf(Color(0xFF2A1F3D), Z.surface))).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("NEW · جديد", color = LocalAccent.current, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            // a tiny animated demo: a mountain in front of a clock
            Canvas(Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(22.dp))) {
                val w = size.width; val h = size.height
                drawRect(Brush.verticalGradient(listOf(Color(0xFFF2A26B), Color(0xFF7B5CD6))))
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = h * 0.62f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
                drawContext.canvas.nativeCanvas.drawText("10:55", w / 2, h * 0.62f, p)
                val m = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h); lineTo(0f, h * 0.62f); lineTo(w * 0.22f, h * 0.42f); lineTo(w * 0.38f, h * 0.55f); lineTo(w * 0.55f, h * 0.3f)
                    lineTo(w * 0.72f, h * 0.5f); lineTo(w * 0.86f, h * 0.4f); lineTo(w, h * 0.58f); lineTo(w, h); close()
                }
                drawPath(m, Color(0xFF241A35))
                drawPath(m, Color.White.copy(alpha = 0.5f * glow), style = Stroke(4f))
                val x = w * sweep
                drawLine(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.7f), Color.Transparent)), Offset(x, 0f), Offset(x, h), 3f)
            }
            Spacer(Modifier.height(16.dp))
            Text(tr("Wallpapers are here", "الخلفيات وصلت"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(tr("Pick any photo, add a clock or any widget, then tap ✦ — Zujaj finds the mountain, building or person and slides the clock behind it.",
                "اختار أي صورة، ضيف ساعة أو أي ويدجت، واضغط ✦ — زجاج بيتعرّف على الجبل أو المبنى أو الشخص ويحط الساعة وراه."),
                color = Z.sub, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(tr("Try it", "جرّبها"), Modifier.fillMaxWidth(), onClick = onTry)
            TextButton(onClick = onDismiss) { Text(tr("Later", "بعدين"), color = Z.sub) }
        }
    }
}
