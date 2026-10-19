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
    fun update(d: WallDesign) { design = d; WallDesign.save(ctx, d) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    val reveal = remember { Animatable(1f) }
    var scanning by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showBlur by remember { mutableStateOf(false) }
    var showApply by remember { mutableStateOf(false) }
    var colorFor by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    // re-render the preview whenever something changes
    LaunchedEffect(design, photo, mask, size, reveal.value) {
        if (size.width <= 0) return@LaunchedEffect
        val w = size.width.coerceAtMost(900); val h = (w / aspect).roundToInt()
        preview = withContext(Dispatchers.Default) { runCatching { WallRender.compose(ctx, design, photo, mask, w, h, reveal = reveal.value).asImageBitmap() }.getOrNull() }
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

    /** ✦ — find the subject, then slide the selected (or every) layer behind it. */
    fun depth() {
        if (photo == null) { Toast.makeText(ctx, tr("Choose a photo first", "اختار صورة الأول"), Toast.LENGTH_SHORT).show(); return }
        if (design.layers.isEmpty()) { Toast.makeText(ctx, tr("Add a clock or widget first", "ضيف ساعة أو ويدجت الأول"), Toast.LENGTH_SHORT).show(); return }
        scope.launch {
            if (mask == null) {
                scanning = true
                val r = withContext(Dispatchers.Default) { Segment.run(ctx, photo!!) }
                scanning = false
                when (r) {
                    is Segment.Result.Ok -> {
                        val f = File(WallDesign.dir(ctx), "mask_${System.currentTimeMillis()}.png")
                        withContext(Dispatchers.IO) { f.outputStream().use { r.mask.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                        mask = r.mask
                        design = design.copy(mask = f.path)
                    }
                    is Segment.Result.Fail -> {
                        Toast.makeText(ctx, tr("Couldn't find a clear subject in this photo. Try another (or try again in a minute while the model downloads).",
                            "مالقيتش شكل واضح في الصورة دي. جرب صورة تانية (أو جرب كمان دقيقة لحد ما الموديل يتحمل)."), Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }
            }
            val targets = selected?.let { s -> design.layers.filter { it.id == s } } ?: design.layers
            val goBehind = targets.any { !it.behind }
            update(design.copy(layers = design.layers.map { l -> if (targets.any { it.id == l.id }) l.copy(behind = goBehind) else l }))
            if (goBehind) { reveal.snapTo(0f); reveal.animateTo(1f, tween(900)) }
        }
    }

    val accent = LocalAccent.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                Text(tr("Wallpapers", "الخلفيات"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("BETA", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
            IconButton(onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Rounded.Image, null) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                DropdownMenu(menu, { menu = false }, containerColor = Z.surface) {
                    DropdownMenuItem(text = { Text(tr("Change photo", "غيّر الصورة")) }, onClick = { menu = false; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                    DropdownMenuItem(text = { Text(tr("Remove all layers", "امسح كل الإضافات")) }, onClick = { menu = false; selected = null; update(design.copy(layers = emptyList())) })
                    DropdownMenuItem(text = { Text(tr("Find the subject again", "حدد الشكل من جديد")) }, onClick = { menu = false; mask = null; update(design.copy(mask = "", layers = design.layers.map { it.copy(behind = false) })) })
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
                }
                preview?.let { Image(it, null, Modifier.fillMaxSize()) }
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
                    Text(tr("Finding the subject…", "بحدد الشكل اللي في الصورة…"), color = Color.White, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp))
                }
                // gestures: tap selects, drag moves (with centre snapping)
                if (photo != null) Box(Modifier.fillMaxSize()
                    .pointerInput(design.layers.size) {
                        detectTapGestures { p ->
                            val hit = design.layers.lastOrNull { l -> WallRender.rect(l, size.width, size.height).contains(p.x, p.y) }
                            selected = hit?.id
                        }
                    }
                    .pointerInput(selected, design.layers.size) {
                        var start: WallLayer? = null
                        var acc = Offset.Zero
                        detectDragGestures(
                            onDragStart = { p ->
                                val hit = design.layers.lastOrNull { l -> WallRender.rect(l, size.width, size.height).contains(p.x, p.y) }
                                if (hit != null) selected = hit.id
                                start = hit; acc = Offset.Zero
                            },
                            onDragEnd = { snapX = false; start = null },
                            onDragCancel = { snapX = false; start = null },
                        ) { ch, d ->
                            val s = start ?: return@detectDragGestures
                            ch.consume(); acc += d
                            var cx = s.cx + acc.x / size.width
                            val cy = (s.cy + acc.y / size.height).coerceIn(0.02f, 0.98f)
                            snapX = abs(cx - 0.5f) < 0.018f
                            if (snapX) cx = 0.5f
                            change(s.id) { it.copy(cx = cx.coerceIn(0.02f, 0.98f), cy = cy) }
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
                        var w0 = l.w; var acc = 0f
                        detectDragGestures(onDragStart = { w0 = design.layers.first { it.id == l.id }.w; acc = 0f }) { ch, d ->
                            ch.consume(); acc += (-d.x + d.y) / size.width
                            change(l.id) { it.copy(w = (w0 + acc * 1.4f).coerceIn(0.12f, 1.2f)) }
                        }
                    })
                }
                // ✦ depth button
                if (photo != null) {
                    val spin = rememberInfiniteTransition(label = "spark")
                    val r by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "r")
                    val on = design.layers.any { it.behind }
                    Box(Modifier.align(Alignment.BottomStart).padding(14.dp).size(48.dp).clip(CircleShape)
                        .background(if (on) accent else Color.Black.copy(alpha = 0.45f)).clickable { depth() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = if (on) Color.Black else Color.White,
                            modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = if (scanning) r else 0f })
                    }
                }
                if (busy) CircularProgressIndicator(Modifier.align(Alignment.Center), color = accent)
            }
        }
        // ---------------------------------------------------------------- actions
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            WallAction(Icons.Rounded.Wallpaper, tr("Apply", "تطبيق"), photo != null) { showApply = true }
            WallAction(Icons.Rounded.Download, tr("Save", "حفظ"), photo != null) {
                scope.launch {
                    busy = true
                    val ok = withContext(Dispatchers.IO) { runCatching { saveToGallery(ctx, WallRender.compose(ctx, design, photo, mask, screenW, screenH)) }.isSuccess }
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
    if (showSheet) ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = Color(0xFF1D2231)) {
        LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 620.dp)) {
            item(span = { GridItemSpan(2) }) { Text(tr("Clocks & dates", "ساعات وتواريخ"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp)) }
            items(WallRender.kinds.filter { it.first != "text" }) { (k, a) ->
                val bmp by produceState<ImageBitmap?>(null, k) { value = withContext(Dispatchers.Default) { runCatching { WallRender.layerBitmap(ctx, WallLayer(kind = k), 420).asImageBitmap() }.getOrNull() } }
                Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                    .clickable { add(WallLayer(kind = k, w = if (a < 1.5f) 0.55f else 0.8f, cy = 0.3f)) }.padding(12.dp), contentAlignment = Alignment.Center) {
                    bmp?.let { Image(it, null, Modifier.fillMaxSize()) }
                }
            }
            item(span = { GridItemSpan(2) }) { Text(tr("Widgets", "ويدجتات"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp, top = 12.dp)) }
            items(Flags.visible().filter { it.shape != com.smrh.zujaj.catalog.Shape.LARGE }, span = { GridItemSpan(if (it.shape == com.smrh.zujaj.catalog.Shape.SMALL) 1 else 2) }) { st ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.07f))
                    .clickable { add(WallLayer(kind = "widget", styleId = st.id, w = if (st.shape == com.smrh.zujaj.catalog.Shape.SMALL) 0.45f else 0.9f, cy = 0.35f)) }.padding(10.dp)) {
                    WidgetPreview(st.defaultConfig().copy(bg = false), Modifier.fillMaxWidth(), density = 1.6f)
                }
            }
        }
    }
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
        if (l == null) colorFor = null else ColorPickerDialog(l.color, { colorFor = null }) { c -> change(id) { it.copy(color = c) } }
    }
    if (showApply) ApplyWallDialog(onDismiss = { showApply = false }) { target, live ->
        showApply = false
        scope.launch {
            busy = true
            WallDesign.save(ctx, design)
            val msg = withContext(Dispatchers.IO) { applyWallpaper(ctx, design, photo, mask, screenW, screenH, target, live) }
            busy = false
            msg?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show() }
        }
    }
}

@Composable
private fun WallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f }) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.08f)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(25.dp))
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
