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
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayCircle
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
import androidx.compose.material.icons.rounded.OpenWith
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
import androidx.compose.ui.graphics.asAndroidBitmap
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
import com.smrh.zujaj.wall.WallFit
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.smrh.zujaj.wall.WallDesign
import com.smrh.zujaj.wall.WallLayer
import com.smrh.zujaj.wall.WallRender
import com.smrh.zujaj.wall.WallService
import com.smrh.zujaj.wall.WallTemplates
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Fingerprint
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
            Modifier.clip(RoundedCornerShape(28.dp)).background(Z.surface).background(Brush.verticalGradient(listOf(LocalAccent.current.copy(alpha = 0.14f), Color.Transparent), 0f, 420f)).padding(22.dp),
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
            ZField(pass, { pass = it; err = false }, tr("Password", "الباسورد"), Icons.Rounded.Lock, password = true, error = err)
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
        withContext(Dispatchers.IO) { photo = WallRender.loadImage(design.image); mask = WallRender.loadImage(design.mask, 1600, trim = false) }
    }
    // save a moment after the last change (not on every finger move)
    LaunchedEffect(design) { kotlinx.coroutines.delay(500); withContext(Dispatchers.IO) { WallDesign.save(ctx, design) } }
    var selected by remember { mutableStateOf<Long?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // fitted clocks follow the subject and the date line follows the clock on every change
    fun update(d: WallDesign) { design = WallFit.apply(d, photo, mask, size.width, size.height) }
    var scanning by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showBlur by remember { mutableStateOf(false) }
    var showApply by remember { mutableStateOf(false) }
    var colorFor by remember { mutableStateOf<Long?>(null) }
    var menu by remember { mutableStateOf(false) }
    var snapX by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    // "move photo" mode: live zoom / pan, saved when Done is pressed
    var adjust by remember { mutableStateOf(false) }
    var az by remember { mutableFloatStateOf(1f) }
    var ax by remember { mutableFloatStateOf(0f) }
    var ay by remember { mutableFloatStateOf(0f) }
    var showBuy by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf("") }
    val doSaveRef = remember { mutableStateOf({}) }
    var showHistory by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var lockPreview by remember { mutableStateOf(false) }
    // the page opens on "My wallpapers"; the editor shows once a wallpaper is opened or a new one is started
    var gallery by remember { mutableStateOf(true) }
    var galleryVer by remember { mutableIntStateOf(0) }
    androidx.activity.compose.BackHandler(enabled = !gallery) {
        if (adjust) { adjust = false } else { selected = null; lockPreview = false; gallery = true; galleryVer++ }
    }
    val owned by com.smrh.zujaj.billing.Billing.wall.collectAsState()
    val accent = LocalAccent.current
    val found = remember { Animatable(0f) }      // outline animation 0..1
    val subjectIn = remember { Animatable(1f) }  // subject fade over the layers

    // static parts of the preview — only rebuilt when the photo / mask / background settings change
    val pw = size.width; val ph = size.height
    val bg by produceState<ImageBitmap?>(null, photo, design.blur, design.dim, design.filter, design.topShade, design.fillMode, design.zoom, design.px, design.py, pw) {
        value = if (pw <= 0) null else withContext(Dispatchers.Default) { runCatching { WallRender.background(design, photo, pw, ph).asImageBitmap() }.getOrNull() }
    }
    val subject by produceState<ImageBitmap?>(null, photo, mask, design.dim, design.filter, design.topShade, design.zoom, design.px, design.py, pw) {
        val p = photo; val m = mask
        value = if (pw <= 0 || p == null || m == null) null else withContext(Dispatchers.Default) { runCatching { WallRender.subject(design, p, m, pw, ph).asImageBitmap() }.getOrNull() }
    }
    val outline by produceState<ImageBitmap?>(null, photo, mask, design.zoom, design.px, design.py, pw) {
        val p = photo; val m = mask
        value = if (pw <= 0 || p == null || m == null) null else withContext(Dispatchers.Default) { runCatching { WallRender.outline(p, m, pw, ph, accent.toArgbInt(), design).asImageBitmap() }.getOrNull() }
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
            update(design.copy(image = f.path, mask = "", zoom = 1f, px = 0f, py = 0f, layers = design.layers.map { it.copy(behind = false) }))
            busy = false
            az = 1f; ax = 0f; ay = 0f; adjust = true; selected = null; gallery = false
        }
    }

    fun add(l: WallLayer) { update(design.copy(layers = design.layers + l)); selected = l.id; showSheet = false }
    fun change(id: Long, f: (WallLayer) -> WallLayer) = update(design.copy(layers = design.layers.map { if (it.id == id) f(it) else it }))

    /** ✦ — find the subject (glowing outline), then slide the selected (or every) layer behind it. */
    fun depth(only: List<Long>? = null) {
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
            val targets = only?.let { ids -> design.layers.filter { it.id in ids } } ?: selected?.let { s -> design.layers.filter { it.id == s } } ?: design.layers
            val goBehind = only != null || targets.any { !it.behind }
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

    if (gallery) WallGallery(
        ver = galleryVer, aspect = aspect, draft = design.takeIf { it.image.isNotBlank() && photo != null }, draftPhoto = photo, draftMask = mask,
        onBack = { nav.back() },
        onNew = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onDraft = { gallery = false },
        onAuto = { showHistory = true },
        onOpen = { item ->
            scope.launch {
                busy = true
                val d = withContext(Dispatchers.IO) { WallHistory.open(ctx, item) }
                photo = withContext(Dispatchers.IO) { WallRender.loadImage(d.image) }
                mask = withContext(Dispatchers.IO) { WallRender.loadImage(d.mask, 1600, trim = false) }
                design = d; selected = null; adjust = false; busy = false; gallery = false
            }
        },
    ) else Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (adjust) adjust = false else { selected = null; lockPreview = false; gallery = true; galleryVer++ } }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
            Column(Modifier.weight(1f)) {
                Text(if (adjust) tr("Position the photo", "ظبط الصورة") else tr("Edit wallpaper", "تعديل الخلفية"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (adjust) tr("Pinch to zoom · drag to move", "قرّب وبعّد بصباعين · اسحب عشان تحركها") else tr("Tap anything to move or style it", "اضغط على أي حاجة عشان تحركها أو تغير شكلها"), color = Z.sub, fontSize = 12.sp)
            }
            if (!adjust) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                DropdownMenu(menu, { menu = false }, containerColor = Z.surface) {
                    DropdownMenuItem(text = { Text(tr("Share", "مشاركة")) }, onClick = {
                        menu = false
                        val p = photo ?: return@DropdownMenuItem
                        scope.launch {
                            busy = true
                            val err = withContext(Dispatchers.IO) { runCatching { shareWall(ctx, design, p, mask, screenW, screenH, !owned) }.exceptionOrNull() }
                            busy = false
                            if (err != null) Toast.makeText(ctx, tr("Couldn't share", "مقدرتش أشارك"), Toast.LENGTH_SHORT).show()
                        }
                    })
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
                    .border(1.dp, Z.tint(0.08f), RoundedCornerShape(26.dp))
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
                    val photoMove = Modifier.graphicsLayer {
                        if (adjust) {
                            val k = az / design.zoom
                            scaleX = k; scaleY = k
                            translationX = ax * size.width - k * design.px * size.width
                            translationY = ay * size.height - k * design.py * size.height
                        }
                    }
                    bg?.let { Image(it, null, Modifier.fillMaxSize().then(photoMove)) }
                    val hasMask = mask != null && subject != null
                    design.layers.filter { it.behind && hasMask }.forEach { l -> LayerView(l, size, selected == l.id, if ((l.glass || l.glassFill) && !adjust) bg else null) }
                    if (hasMask && design.layers.any { it.behind }) subject?.let { Image(it, null, Modifier.fillMaxSize().then(photoMove).graphicsLayer { alpha = subjectIn.value }) }
                    design.layers.filter { !it.behind || !hasMask }.forEach { l -> LayerView(l, size, selected == l.id, if ((l.glass || l.glassFill) && !adjust) bg else null) }
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
                if (photo != null && adjust) Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val W = size.width.toFloat(); val H = size.height.toFloat()
                        val nz = (az * zoom).coerceIn(0.35f, 5f)
                        val k = nz / az
                        // zoom around the fingers: keep the point under the centroid in place
                        val cx = W / 2 + ax * W; val cy = H / 2 + ay * H
                        val ncx = centroid.x + (cx - centroid.x) * k + pan.x
                        val ncy = centroid.y + (cy - centroid.y) * k + pan.y
                        az = nz
                        ax = ((ncx - W / 2) / W).coerceIn(-1.2f, 1.2f)
                        ay = ((ncy - H / 2) / H).coerceIn(-1.2f, 1.2f)
                    }
                })
                if (photo != null && !adjust) Box(Modifier.fillMaxSize()
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
                                l.copy(cx = cx.coerceIn(0.02f, 0.98f), cy = (l.cy + d.y / size.height).coerceIn(0.02f, 0.98f), stick = if (l.kind == "date_line") false else l.stick)
                            }
                        }
                    })
                if (snapX) Canvas(Modifier.fillMaxSize()) { drawLine(accent, Offset(this.size.width / 2, 0f), Offset(this.size.width / 2, this.size.height), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))) }
                // selection box + handles
                if (!adjust) design.layers.firstOrNull { it.id == selected }?.let { l ->
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
                    // edge handles: stretch only the width or only the height (free shape, like iPhone)
                    @Composable
                    fun edge(x: Float, y: Float, vertical: Boolean, onDrag: (Float) -> Unit) {
                        val lw = with(dens) { (if (vertical) 10.dp else 34.dp).toPx() }; val lh = with(dens) { (if (vertical) 34.dp else 10.dp).toPx() }
                        val touch = with(dens) { 44.dp.toPx() }
                        Box(Modifier.offset { IntOffset((x - touch / 2).roundToInt(), (y - touch / 2).roundToInt()) }.size(44.dp)
                            .pointerInput(l.id, vertical) { detectDragGestures { ch, d -> ch.consume(); onDrag(if (vertical) d.x else d.y) } },
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.size(with(dens) { lw.toDp() }, with(dens) { lh.toDp() }).clip(RoundedCornerShape(50)).background(Color.White)
                                .border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(50)))
                        }
                    }
                    edge(rc.right, rc.centerY(), true) { dx ->
                        change(l.id) { o ->
                            val hPx = o.w * size.width / WallRender.aspect(o)
                            val nw = (o.w + dx / size.width).coerceIn(0.1f, 1.2f)
                            o.copy(w = nw, ar = (nw * size.width / hPx).coerceIn(0.25f, 10f))
                        }
                    }
                    edge(rc.centerX(), rc.bottom, false) { dy ->
                        change(l.id) { o ->
                            val hPx = (o.w * size.width / WallRender.aspect(o) + dy).coerceAtLeast(16f)
                            o.copy(ar = (o.w * size.width / hPx).coerceIn(0.25f, 10f))
                        }
                    }
                }
                // how it will look on the lock screen (system clock, notifications, shortcuts)
                if (photo != null && lockPreview && !adjust) LockOverlay()
                if (photo != null && !adjust) {
                    Box(Modifier.align(Alignment.TopStart).padding(12.dp).size(40.dp).clip(CircleShape)
                        .background(if (lockPreview) accent else Color.Black.copy(alpha = 0.5f)).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickable { lockPreview = !lockPreview; if (lockPreview) selected = null }, contentAlignment = Alignment.Center) {
                        Icon(if (lockPreview) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = if (lockPreview) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                // move / zoom the photo
                if (photo != null) {
                    if (adjust) {
                    } else {
                        Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { az = design.zoom; ax = design.px; ay = design.py; selected = null; adjust = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.OpenWith, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                // ✦ depth button
                if (photo != null && !adjust) {
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
        fun doSave() {
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
        doSaveRef.value = { doSave() }
        // ---------------------------------------------------------------- actions
        androidx.compose.animation.AnimatedContent(adjust, label = "bar", transitionSpec = {
            (androidx.compose.animation.fadeIn(tween(220)) + androidx.compose.animation.slideInVertically(tween(260)) { it / 3 }) togetherWith androidx.compose.animation.fadeOut(tween(150))
        }) { adj ->
            if (adj) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(Z.tint(0.08f))
                    .border(1.dp, Z.tint(0.1f), RoundedCornerShape(18.dp)).clickable { az = 1f; ax = 0f; ay = 0f }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text(tr("Reset", "رجّعها"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
                Box(Modifier.weight(1.4f).height(54.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.8f))))
                    .clickable {
                        update(design.copy(zoom = az, px = ax, py = ay)); adjust = false
                    }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text(tr("Done", "تمام"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
                // tools in one floating glass bar
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Z.tint(0.06f))
                    .border(1.dp, Z.tint(0.08f), RoundedCornerShape(22.dp)).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    WallTool(Icons.Rounded.AutoAwesomeMosaic, tr("Styles", "قوالب")) { showTemplates = true }
                    WallTool(Icons.Rounded.Widgets, tr("Widgets", "ويدجت")) { showSheet = true }
                    WallTool(Icons.Rounded.FormatQuote, tr("Text", "نص")) { showText = true }
                    WallTool(Icons.Rounded.Tune, tr("Look", "الشكل")) { showBlur = true }
                    WallTool(Icons.Rounded.Crop, tr("Photo", "الصورة")) { az = design.zoom; ax = design.px; ay = design.py; selected = null; adjust = true }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(18.dp)).background(Z.tint(0.08f))
                        .border(1.dp, Z.tint(0.1f), RoundedCornerShape(18.dp))
                        .clickable { if (!owned) { pending = "save"; showBuy = true } else doSave() }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(tr("Save", "حفظ"), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (!owned) { Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Lock, null, tint = Z.pro, modifier = Modifier.size(14.dp)) }
                        }
                    }
                    Box(Modifier.weight(1.5f).height(52.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.8f))))
                        .clickable { if (owned) showApply = true else { pending = "apply"; showBuy = true } }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wallpaper, null, tint = Color.Black, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                            Text(tr("Set as wallpaper", "تعيين كخلفية"), color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (!owned) { Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Lock, null, tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)) }
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- sheets & dialogs
    if (showSheet) ModalBottomSheet(onDismissRequest = { showSheet = false }, containerColor = Z.surface2) { WidgetPickerSheet(onAdd = { add(it) }) }
    if (showText) {
        var t by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showText = false }, containerColor = Z.surface,
            title = { Text(tr("Your text", "كلامك")) },
            text = { OutlinedTextField(t, { t = it }, placeholder = { Text(tr("Type your text", "اكتب كلامك")) }) },
            confirmButton = { TextButton(onClick = { showText = false; if (t.isNotBlank()) add(WallLayer(kind = "text", text = t.trim(), w = 0.8f, cy = 0.62f)) }) { Text(tr("Add", "ضيف"), color = accent) } },
            dismissButton = { TextButton(onClick = { showText = false }) { Text(tr("Cancel", "إلغاء")) } })
    }
    if (showBlur) ModalBottomSheet(onDismissRequest = { showBlur = false }, containerColor = Z.surface2) {
        LookSheet(design, photo) { update(it) }
    }
    if (showTemplates) ModalBottomSheet(onDismissRequest = { showTemplates = false }, containerColor = Z.surface2) {
        TemplatesSheet(design, photo, mask, aspect) { t ->
            showTemplates = false
            val nd = WallTemplates.apply(t, design, photo, maxOf(size.width, 1), maxOf(size.height, 1), mask != null)
            update(nd); selected = null; lockPreview = false
            val behindIds = nd.layers.filter { it.behind }.map { it.id }
            if (t.depth || behindIds.isNotEmpty()) depth(behindIds)
            Toast.makeText(ctx, tr("Tap any part to move, resize or recolour it", "اضغط على أي جزء عشان تحركه أو تكبره أو تغير لونه"), Toast.LENGTH_SHORT).show()
        }
    }
    colorFor?.let { id ->
        val l = design.layers.firstOrNull { it.id == id }
        if (l == null) colorFor = null
        else LayerColorSheet(l, onAuto = {
            val p = photo ?: return@LayerColorSheet
            val (a, b) = WallRender.autoColors(p, l, maxOf(size.width, 1), maxOf(size.height, 1), design.dim, design)
            change(id) { it.copy(color = a, color2 = b, gDir = 0) }
        }, onTint = {
            val p = photo ?: return@LayerColorSheet
            val (a, _) = WallRender.autoColors(p, l, maxOf(size.width, 1), maxOf(size.height, 1), design.dim, design)
            change(id) { it.copy(color = a, color2 = 0) }
        }, onChange = { nl -> change(id) { nl } }, onDismiss = { colorFor = null })
    }
    if (showBuy) WallBuySheet(onReward = { if (pending == "apply") showApply = true else if (pending == "save") doSaveRef.value() }) { showBuy = false }
    if (showHistory) ModalBottomSheet(onDismissRequest = { showHistory = false }, containerColor = Z.surface2) {
        var items by remember { mutableStateOf(WallHistory.list(ctx)) }
        var rotMode by remember { mutableStateOf(false) }
        var picked by remember { mutableStateOf(setOf<String>()) }
        var every by remember { mutableIntStateOf(1440) }
        var rot by remember { mutableStateOf(com.smrh.zujaj.wall.WallRotation.load(ctx)) }
        Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("My wallpapers", "خلفياتي"), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(6.dp).weight(1f))
                if (items.size >= 2) Text(if (rotMode) tr("Cancel", "إلغاء") else tr("Auto-change", "تغيير تلقائي"), color = accent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { rotMode = !rotMode; picked = emptySet() }.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            rot?.let { r ->
                if (!rotMode) Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Autorenew, null, tint = accent, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(tr("Auto-change is on · ${r.items.size} wallpapers", "التغيير التلقائي شغال · ${r.items.size} خلفيات"), fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(tr("Stop", "إيقاف"), color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        modifier = Modifier.clickable { com.smrh.zujaj.wall.WallRotation.clear(ctx); rot = null }.padding(6.dp))
                }
            }
            if (rotMode) {
                Text(tr("Pick 2 or more wallpapers — your phone switches between them on its own.", "اختار خلفيتين أو أكتر — التليفون هيبدّل بينهم لوحده."), color = Z.sub, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60 to tr("Every hour", "كل ساعة"), 360 to tr("Every 6 hours", "كل ٦ ساعات"), 1440 to tr("Every day", "كل يوم")).forEach { (m, l) ->
                        ZChip(l, every == m) { every = m }
                    }
                }
            }
            if (items.isEmpty()) Text(tr("Wallpapers you save or apply show up here so you can edit them again.", "أي خلفية تحفظها أو تطبقها هتظهر هنا عشان تعدّل عليها تاني."),
                color = Z.sub, modifier = Modifier.padding(vertical = 30.dp, horizontal = 6.dp))
            LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 560.dp)) {
                items(items, key = { it.dir.name }) { it ->
                    val bmp by produceState<ImageBitmap?>(null, it.thumb) { value = withContext(Dispatchers.IO) { runCatching { android.graphics.BitmapFactory.decodeFile(it.thumb.path).asImageBitmap() }.getOrNull() } }
                    val on = it.dir.name in picked
                    Box(Modifier.aspectRatio(aspect).clip(RoundedCornerShape(14.dp)).background(Z.tint(0.06f))
                        .then(if (on) Modifier.border(2.5.dp, accent, RoundedCornerShape(14.dp)) else Modifier)
                        .pointerInput(it.dir.name, rotMode) {
                            detectTapGestures(
                                onTap = { _ ->
                                    if (rotMode) { picked = if (it.dir.name in picked) picked - it.dir.name else picked + it.dir.name; return@detectTapGestures }
                                    scope.launch {
                                        val d = withContext(Dispatchers.IO) { WallHistory.open(ctx, it) }
                                        photo = withContext(Dispatchers.IO) { WallRender.loadImage(d.image) }
                                        mask = withContext(Dispatchers.IO) { WallRender.loadImage(d.mask, 1600, trim = false) }
                                        design = d; selected = null; showHistory = false
                                    }
                                },
                                onLongPress = { _ -> WallHistory.delete(it); items = WallHistory.list(ctx) },
                            )
                        }) {
                        bmp?.let { b -> Image(b, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                        if (rotMode) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(if (on) accent else Color.Black.copy(alpha = 0.45f))
                            .border(1.5.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
                            if (on) Icon(Icons.Rounded.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            if (items.isNotEmpty() && !rotMode) Text(tr("Tap to edit · long-press to delete", "اضغط للتعديل · اضغط مطولاً للمسح"), color = Z.sub, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, start = 6.dp))
            if (rotMode) PrimaryButton(tr("Start auto-change", "شغّل التغيير التلقائي") + if (picked.isNotEmpty()) " (${picked.size})" else "", Modifier.fillMaxWidth().padding(top = 12.dp)) {
                when {
                    picked.size < 2 -> Toast.makeText(ctx, tr("Pick at least 2 wallpapers", "اختار خلفيتين على الأقل"), Toast.LENGTH_SHORT).show()
                    !owned -> { pending = ""; showBuy = true }
                    else -> {
                        val order = items.map { i -> i.dir.name }.filter { n -> n in picked }
                        com.smrh.zujaj.wall.WallRotation.save(ctx, com.smrh.zujaj.wall.WallRotation(order, every))
                        rot = com.smrh.zujaj.wall.WallRotation.load(ctx); rotMode = false; showHistory = false
                        runCatching {
                            ctx.startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(ctx, WallService::class.java))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                        Toast.makeText(ctx, tr("Tap “Set wallpaper” on the next screen", "اضغط «تعيين الخلفية» في الشاشة الجاية"), Toast.LENGTH_LONG).show()
                    }
                }
            }
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
private fun LayerView(l: WallLayer, box: IntSize, selected: Boolean, under: ImageBitmap? = null) {
    val ctx = LocalContext.current
    if (box.width <= 0) return
    val rc = WallRender.rect(l, box.width, box.height)
    val wBucket = (rc.width() / 24f).roundToInt() * 24 // re-render only every 24 px of resizing
    val minute = rememberTicker(30_000L)
    val arKey = (l.ar * 50).roundToInt()
    // frosted glass plate, rebuilt only every few px of movement
    if (under != null && l.glass) {
        val gx = (rc.left / 6f).roundToInt(); val gy = (rc.top / 6f).roundToInt(); val gh = (rc.height() / 6f).roundToInt()
        val plate by produceState<ImageBitmap?>(null, under, gx, gy, wBucket, gh) {
            value = withContext(Dispatchers.Default) { runCatching { WallRender.glassPlate(under.asAndroidBitmap(), rc)?.asImageBitmap() }.getOrNull() }
        }
        val gr = WallRender.glassRect(rc)
        val d0 = LocalDensity.current
        plate?.let { Image(it, null, Modifier.offset { IntOffset(gr.left.roundToInt(), gr.top.roundToInt()) }.size(with(d0) { gr.width().toDp() }, with(d0) { gr.height().toDp() })) }
    }
    val glassUnder = if (l.glassFill) under else null
    val px6 = if (glassUnder != null) (rc.left / 6f).roundToInt() * 100000 + (rc.top / 6f).roundToInt() else 0
    val bmp by produceState<ImageBitmap?>(null, l.kind, l.styleId, l.text, l.color, l.color2, l.gDir, wBucket, minute, arKey, glassUnder, px6) {
        value = withContext(Dispatchers.Default) { runCatching {
            val b = WallRender.styled(ctx, l, wBucket.coerceAtLeast(24))
            if (glassUnder != null) {
                val h = if (l.ar > 0f) rc.height() else rc.width() * b.height / b.width.toFloat()
                WallRender.glassify(b, glassUnder.asAndroidBitmap(), android.graphics.RectF(rc.left, rc.centerY() - h / 2, rc.right, rc.centerY() + h / 2)).asImageBitmap()
            } else b.asImageBitmap()
        }.getOrNull() }
    }
    val dens = LocalDensity.current
    bmp?.let { b ->
        val h = if (l.ar > 0f) rc.height() else rc.width() * b.height / b.width.toFloat()
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
    val cats = remember { listOf("wall") + com.smrh.zujaj.catalog.Cat.values().filter { c -> Flags.visible().any { it.cat == c && it.shape != com.smrh.zujaj.catalog.Shape.LARGE && it.shape != com.smrh.zujaj.catalog.Shape.XL } }.map { it.name } }
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
                        Box(Modifier.fillMaxWidth().height(if (a < 2f) 130.dp else 92.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF232634))
                            .clickable { onAdd(WallLayer(kind = kind, w = if (a < 1.5f) 0.55f else 0.8f, cy = 0.3f)) }.padding(12.dp), contentAlignment = Alignment.Center) {
                            bmp?.let { Image(it, null, Modifier.fillMaxSize()) }
                        }
                    }
                } else {
                    val list = pack(Flags.visible().filter { it.cat.name == k && it.shape != com.smrh.zujaj.catalog.Shape.LARGE && it.shape != com.smrh.zujaj.catalog.Shape.XL })
                    items(list, span = { GridItemSpan(if (it.shape == com.smrh.zujaj.catalog.Shape.SMALL || it.shape == com.smrh.zujaj.catalog.Shape.TALL) 1 else 2) }) { st ->
                        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF232634))
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
private fun LayerColorSheet(l: WallLayer, onAuto: () -> Unit, onTint: () -> Unit, onChange: (WallLayer) -> Unit, onDismiss: () -> Unit) {
    val swatches = listOf(0xFFFFFFFF, 0xFF111111, 0xFFE7B75F, 0xFFF2865E, 0xFFE5484D, 0xFFFF6B9A, 0xFFB07BF0, 0xFF5B8DEF, 0xFF38C6D9, 0xFF3DDC97, 0xFFB7E34A, 0xFFF3E3C2).map { it.toInt() }
    var custom by remember { mutableIntStateOf(0) } // 1 = first colour, 2 = second
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Z.surface2) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 28.dp).verticalScroll(rememberScrollState())) {
            if (l.kind == "date_line") {
                Text(tr("Next to the date", "جنب التاريخ"), color = Z.sub, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("" to tr("Weather", "الطقس"), "prayer" to tr("Next prayer", "الصلاة الجاية"), "none" to tr("Nothing", "ولا حاجة")).forEach { (k, s) -> ZChip(s, l.text == k) { onChange(l.copy(text = k)) } }
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f))
                    .clickable { onChange(l.copy(stick = !l.stick)) }.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Stay above the clock", "يفضل فوق الساعة"), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(l.stick, { onChange(l.copy(stick = it)) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f))
                .clickable { onChange(l.copy(glass = !l.glass)) }.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Glass background", "خلفية زجاج"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(tr("Frosted glass behind it, like iPhone", "زجاج مطفي وراه زي الآيفون"), color = Z.sub, fontSize = 12.sp)
                }
                Switch(l.glass, { onChange(l.copy(glass = it)) })
            }
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f))
                .clickable { onChange(l.copy(glassFill = !l.glassFill)) }.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(tr("Glass widget", "الويدجت نفسها زجاج"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(tr("The numbers and shapes become frosted glass", "الأرقام والأشكال نفسها تبقى زجاج شفاف"), color = Z.sub, fontSize = 12.sp)
                }
                Switch(l.glassFill, { onChange(l.copy(glassFill = it)) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Colour", "اللون"), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(50)).background(Z.tint(0.08f)).padding(3.dp)) {
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
            Spacer(Modifier.height(10.dp))
            // one soft colour from the photo, like iPhone
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f))
                .clickable(onClick = onTint).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Palette, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(tr("Tint from the photo", "لون من الصورة"), fontWeight = FontWeight.Bold)
                    Text(tr("One soft colour taken from your wallpaper", "لون هادي واحد مأخوذ من صورتك"), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
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
                            .border(if (c == value) 3.dp else 1.dp, if (c == value) LocalAccent.current else Z.tint(0.2f), CircleShape)
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
private fun WallTool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val k by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.88f else 1f, androidx.compose.animation.core.spring(0.5f, 600f), label = "k")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { scaleX = k; scaleY = k }.clip(RoundedCornerShape(14.dp))
        .clickable(src, androidx.compose.material3.ripple(), onClick = onClick).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Z.text, maxLines = 1)
    }
}

/** First page: start a new wallpaper or open one you made before. */
@Composable
private fun WallGallery(ver: Int, aspect: Float, draft: WallDesign?, draftPhoto: Bitmap?, draftMask: Bitmap?, onBack: () -> Unit, onNew: () -> Unit, onDraft: () -> Unit, onAuto: () -> Unit, onOpen: (WallHistory.Item) -> Unit) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    var items by remember { mutableStateOf(emptyList<WallHistory.Item>()) }
    LaunchedEffect(ver) { items = withContext(Dispatchers.IO) { WallHistory.list(ctx) } }
    var toDelete by remember { mutableStateOf<WallHistory.Item?>(null) }
    val t = rememberInfiniteTransition(label = "g")
    val sheen by t.animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "sh")
    val bob by t.animateFloat(-3f, 3f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "b")
    val cols = if (LocalConfiguration.current.screenWidthDp >= 600) 4 else 2
    val draftImg by produceState<ImageBitmap?>(null, draft, draftPhoto) {
        val d = draft; val p = draftPhoto
        value = if (d == null || p == null) null else withContext(Dispatchers.Default) {
            runCatching { WallRender.compose(ctx, d, p, draftMask, 360, (360 / aspect).toInt()).asImageBitmap() }.getOrNull()
        }
    }
    LazyVerticalGrid(GridCells.Fixed(cols), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-8).dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
                Column(Modifier.weight(1f)) {
                    Text(tr("Wallpapers", "الخلفيات"), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                    Text(tr("Your photos, with a clock that lives in them", "صورك، والساعة جوّاها"), color = Z.sub, fontSize = 13.sp)
                }
                if (items.size >= 2) Box(Modifier.size(42.dp).clip(CircleShape).background(Z.tint(0.08f)).clickable(onClick = onAuto), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Autorenew, null, modifier = Modifier.size(21.dp))
                }
            }
        }
        // create
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), Color(0xFF7B5CD6), Color(0xFF2B2140))))
                .clickable(onClick = onNew)) {
                Canvas(Modifier.fillMaxSize()) {
                    val x = size.width * sheen
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(x - 160f, 0f), Offset(x + 160f, size.height)))
                    drawCircle(Color.White.copy(alpha = 0.07f), size.height * 0.9f, Offset(size.width * 0.92f, size.height * 0.1f))
                }
                Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("New wallpaper", "خلفية جديدة"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(tr("Pick a photo — we'll do the rest", "اختار صورة والباقي علينا"), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Box(Modifier.graphicsLayer { translationY = bob }.size(58.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = Color.Black, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
        if (draft != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Z.tint(0.06f))
                    .border(1.dp, Z.tint(0.08f), RoundedCornerShape(22.dp)).clickable(onClick = onDraft).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.height(74.dp).aspectRatio(aspect).clip(RoundedCornerShape(12.dp)).background(Z.tint(0.06f))) {
                        draftImg?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr("Continue editing", "كمّل التعديل"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(tr("The wallpaper you were working on", "الخلفية اللي كنت شغال عليها"), color = Z.sub, fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Z.sub)
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(tr("My wallpapers", "خلفياتي"), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (items.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Z.tint(0.04f)).padding(vertical = 34.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Collections, null, tint = Z.sub, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(10.dp))
                Text(tr("Wallpapers you save or set show up here", "الخلفيات اللي تحفظها أو تعيّنها هتظهر هنا"), color = Z.sub, textAlign = TextAlign.Center, fontSize = 14.sp)
            }
        }
        itemsIndexed(items, key = { _, it -> it.dir.name }) { i, it ->
            val bmp by produceState<ImageBitmap?>(null, it.thumb) { value = withContext(Dispatchers.IO) { runCatching { android.graphics.BitmapFactory.decodeFile(it.thumb.path).asImageBitmap() }.getOrNull() } }
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(40L * (i % 12)); shown = true }
            val a by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0f, tween(380), label = "a")
            Box(Modifier.graphicsLayer { alpha = a; translationY = (1f - a) * 40f; val k = 0.94f + 0.06f * a; scaleX = k; scaleY = k }
                .aspectRatio(aspect).clip(RoundedCornerShape(22.dp)).background(Z.tint(0.06f))
                .pointerInput(it.dir.name) { detectTapGestures(onTap = { _ -> onOpen(it) }, onLongPress = { _ -> toDelete = it }) }) {
                bmp?.let { b -> Image(b, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).clickable { toDelete = it },
                    contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MoreHoriz, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
        }
    }
    toDelete?.let { item ->
        AlertDialog(onDismissRequest = { toDelete = null }, containerColor = Z.surface,
            title = { Text(tr("Delete this wallpaper?", "تمسح الخلفية دي؟")) },
            confirmButton = { TextButton(onClick = { WallHistory.delete(item); items = items.filter { it.dir != item.dir }; toDelete = null }) { Text(tr("Delete", "امسح"), color = Color(0xFFE5484D)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(tr("Cancel", "إلغاء")) } })
    }
}

@Composable
private fun WallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, locked: Boolean = false, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.4f }) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(if (locked) Z.pro.copy(alpha = 0.14f) else Z.tint(0.08f)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(23.dp))
            if (locked) Icon(Icons.Rounded.Lock, null, tint = Z.pro, modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(13.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Z.text, maxLines = 1)
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
    com.smrh.zujaj.wall.WallRotation.clear(ctx)
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
fun WallBuySheet(onReward: () -> Unit = {}, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val adReady by com.smrh.zujaj.ads.Ads.ready.collectAsState()
    val adVer by com.smrh.zujaj.ads.Ads.changes.collectAsState()
    val flags by com.smrh.zujaj.remote.Flags.flags.collectAsState()
    LaunchedEffect(Unit) { com.smrh.zujaj.ads.Ads.preload(ctx) }
    val plans by com.smrh.zujaj.billing.Billing.wallPlans.collectAsState()
    val owned by com.smrh.zujaj.billing.Billing.wall.collectAsState()
    var redeem by remember { mutableStateOf(false) }
    var pick by remember { mutableStateOf(com.smrh.zujaj.billing.WALL_YEARLY) }
    LaunchedEffect(owned) { if (owned) onDismiss() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val sc by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0.88f, androidx.compose.animation.core.spring(0.6f, 320f), label = "sc")
        // phones: near full width · tablets/foldables: a comfortable fixed-width card
        Box(Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 480.dp).padding(horizontal = 18.dp).graphicsLayer { scaleX = sc; scaleY = sc; alpha = ((sc - 0.88f) / 0.12f).coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(30.dp)).background(Z.surface).background(Brush.verticalGradient(listOf(Z.pro.copy(alpha = 0.16f), Color.Transparent)))
            .border(1.dp, Z.line, RoundedCornerShape(30.dp))) {
        Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp).clip(CircleShape).background(Z.tint(0.08f)).clickable(onClick = onDismiss), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).padding(top = 24.dp, bottom = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Z.pro.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Wallpaper, null, tint = Z.pro, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(tr("Unlock Wallpapers", "افتح الخلفيات"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(tr("Apply your designs to the home & lock screen and save them to your photos.", "طبّق تصميماتك على الشاشة الرئيسية وشاشة القفل واحفظها في الصور."),
                color = Z.sub, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Z.tint(0.04f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                listOf(tr("Clock behind the subject (depth effect)", "الساعة ورا الشكل (تأثير العمق)"), tr("Live clock that keeps updating", "ساعة حيّة بتتحدث لوحدها"),
                    tr("Your designs saved to edit again", "تصميماتك محفوظة تعدّل عليها تاني")).forEach {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = Z.pro, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text(it, fontSize = 14.sp, color = Z.text)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            val options = listOf(
                Triple(com.smrh.zujaj.billing.WALL_MONTHLY, tr("Monthly", "شهري"), tr("Billed every month", "بيتجدد كل شهر")),
                Triple(com.smrh.zujaj.billing.WALL_YEARLY, tr("Yearly", "سنوي"), tr("Billed every year", "بيتجدد كل سنة")),
                Triple(com.smrh.zujaj.billing.WALL_PRODUCT, tr("Lifetime", "مدى الحياة"), tr("Pay once, yours forever", "تدفع مرة واحدة وتبقى ليك")),
            )
            options.forEach { (id, name, sub) ->
                val on = pick == id
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (on) Z.pro.copy(alpha = 0.14f) else Z.tint(0.05f))
                    .border(if (on) 1.5.dp else 1.dp, if (on) Z.pro else Z.tint(0.08f), RoundedCornerShape(18.dp))
                    .clickable { Haptics.tick(ctx); pick = id }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) Z.pro else Z.sub, CircleShape), contentAlignment = Alignment.Center) {
                        if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(Z.pro))
                    }
                    Spacer(Modifier.width(12.dp))
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
            if (flags.adsOn && flags.adWallPerDay > 0) {
                val left = remember(adVer) { com.smrh.zujaj.ads.Ads.left() }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Z.tint(0.06f))
                    .border(1.dp, Z.tint(0.1f), RoundedCornerShape(18.dp))
                    .clickable(enabled = left > 0) {
                        val a = ctx as? android.app.Activity ?: return@clickable
                        if (!com.smrh.zujaj.ads.Ads.showReward(a) { onDismiss(); onReward() })
                            Toast.makeText(ctx, tr("The ad is still loading — try again in a few seconds", "الإعلان لسه بيحمّل، جرب كمان ثواني"), Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayCircle, null, tint = if (left > 0) Z.text else Z.sub, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (left > 0) {
                            Text(tr("Or watch a short ad — use it once", "أو شوف إعلان قصير واستخدمها مرة"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(tr("$left of ${flags.adWallPerDay} free uses left today", "فاضلك $left من ${flags.adWallPerDay} مرات النهارده"), color = Z.sub, fontSize = 12.sp)
                        } else {
                            val h = (com.smrh.zujaj.ads.Ads.waitMs() / 3_600_000L); val m = (com.smrh.zujaj.ads.Ads.waitMs() / 60_000L) % 60
                            Text(tr("No free uses left today", "خلصت المرات المجانية النهارده"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Z.sub)
                            Text(tr("Come back in ${h}h ${m}m or subscribe", "ارجع بعد ${h} ساعة و${m} دقيقة أو اشترك"), color = Z.sub, fontSize = 12.sp)
                        }
                    }
                    if (left > 0 && !adReady) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Z.sub)
                }
            }
            Row {
                TextButton(onClick = { com.smrh.zujaj.billing.Billing.restore(); Toast.makeText(ctx, tr("Checking your purchases…", "بنراجع مشترياتك…"), Toast.LENGTH_SHORT).show() }) { Text(tr("Restore purchases", "استرجاع المشتريات"), color = Z.sub) }
                TextButton(onClick = { redeem = true }) { Text(tr("Have a code?", "معاك كود؟"), color = Z.pro) }
            }
            Text(tr("Subscriptions renew automatically until cancelled in Google Play.", "الاشتراكات بتتجدد لوحدها لحد ما تلغيها من جوجل بلاي."), color = Z.sub, fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
    if (redeem) RedeemDialog { redeem = false }
    }
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


// ===================================================================================== look / templates / lock preview / share

/** Photo look: filters (with live thumbnails), blur, darken and a shade at the top. */
@Composable
private fun LookSheet(design: WallDesign, photo: Bitmap?, onChange: (WallDesign) -> Unit) {
    val accent = LocalAccent.current
    val thumb by produceState<Bitmap?>(null, photo) {
        value = photo?.let { p -> withContext(Dispatchers.Default) { runCatching {
            val s = 220f / maxOf(p.width, p.height); Bitmap.createScaledBitmap(p, (p.width * s).toInt().coerceAtLeast(2), (p.height * s).toInt().coerceAtLeast(2), true)
        }.getOrNull() } }
    }
    Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
        Text(tr("Photo look", "شكل الصورة"), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(10.dp))
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(WallRender.filters.size) { i ->
                val on = design.filter == i
                val img by produceState<ImageBitmap?>(null, thumb, i) {
                    val t = thumb
                    value = if (t == null) null else withContext(Dispatchers.Default) { runCatching {
                        val o = Bitmap.createBitmap(t.width, t.height, Bitmap.Config.ARGB_8888)
                        android.graphics.Canvas(o).drawBitmap(t, 0f, 0f, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply { colorFilter = WallRender.filterOf(i) })
                        o.asImageBitmap()
                    }.getOrNull() }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable { onChange(design.copy(filter = i)) }) {
                    Box(Modifier.size(68.dp).clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f))
                        .border(if (on) 2.5.dp else 0.dp, if (on) accent else Color.Transparent, RoundedCornerShape(16.dp))) {
                        img?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop) }
                    }
                    Spacer(Modifier.height(5.dp))
                    val (en, ar) = WallRender.filters[i]
                    Text(tr(en, ar), fontSize = 11.sp, color = if (on) accent else Z.text, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        @Composable
        fun slider(label: String, v: Int, max: Int, set: (Int) -> Unit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f)); Text("$v%", color = Z.sub, fontSize = 13.sp)
            }
            Slider(v.toFloat(), { set(it.roundToInt()) }, valueRange = 0f..max.toFloat())
        }
        if (design.zoom < 0.999f || design.px != 0f || design.py != 0f) {
            Text(tr("Fill the empty space", "تكملة الفراغ حوالين الصورة"), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 14.dp)) {
                listOf(tr("Extend", "امتداد"), tr("Mirror", "مراية"), tr("Blur", "ضباب"), tr("Colour", "لون")).forEachIndexed { i, t -> ZChip(t, design.fillMode == i) { onChange(design.copy(fillMode = i)) } }
            }
        }
        slider(tr("Shade at the top (clock area)", "تغميق من فوق (مكان الساعة)"), design.topShade, 80) { onChange(design.copy(topShade = it)) }
        slider(tr("Darken", "التغميق"), design.dim, 70) { onChange(design.copy(dim = it)) }
        slider(tr("Blur", "التغبيش"), design.blur, 100) { onChange(design.copy(blur = it)) }
    }
}

/** Ready-made looks, previewed on the user's own photo. Tap one, then edit any part. */
@Composable
private fun TemplatesSheet(design: WallDesign, photo: Bitmap?, mask: Bitmap?, aspect: Float, onPick: (WallTemplates.Template) -> Unit) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 26.dp)) {
        Text(tr("Styles", "قوالب جاهزة"), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(horizontal = 4.dp))
        Text(tr("Pick one — you can still move, resize and recolour everything.", "اختار واحد — وتقدر بعدها تحرك وتكبّر وتغيّر لون أي حاجة."), color = Z.sub, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
        Spacer(Modifier.height(6.dp))
        LazyVerticalGrid(GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 600.dp)) {
            items(WallTemplates.all, key = { it.id }) { t ->
                val img by produceState<ImageBitmap?>(null, t.id, photo, mask) {
                    value = withContext(Dispatchers.Default) { runCatching {
                        val tw = 300; val th = (tw / aspect).toInt()
                        val d = WallTemplates.apply(t, design.copy(layers = emptyList()), photo, tw, th, mask != null)
                        WallRender.compose(ctx, d, photo, if (t.depth || d.layers.any { it.behind }) mask else null, tw, th).asImageBitmap()
                    }.getOrNull() }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onPick(t) }) {
                    Box(Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(16.dp)).background(Z.tint(0.06f))) {
                        val im = img
                        if (im == null) CircularProgressIndicator(Modifier.align(Alignment.Center).size(22.dp), strokeWidth = 2.dp, color = accent)
                        else Image(im, null, Modifier.fillMaxSize())
                        if (t.depth) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("✦", color = Color.White, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(tr(t.en, t.ar), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
}

/** Lock-screen preview: status bar, the system clock, two notifications and the corner shortcuts. */
@Composable
private fun LockOverlay() {
    val now by produceState(java.time.LocalTime.now()) { while (true) { value = java.time.LocalTime.now(); kotlinx.coroutines.delay(15_000) } }
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val white = Color.White.copy(alpha = 0.92f)
        // status bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Wifi, null, tint = white, modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp))
            Icon(Icons.Rounded.SignalCellularAlt, null, tint = white, modifier = Modifier.size(11.dp)); Spacer(Modifier.width(3.dp))
            Icon(Icons.Rounded.BatteryFull, null, tint = white, modifier = Modifier.size(11.dp))
        }
        // the phone's own clock
        Column(Modifier.fillMaxWidth().padding(top = (w * 0.13f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("%d:%02d".format(java.util.Locale.US, if (now.hour % 12 == 0) 12 else now.hour % 12, now.minute), color = white,
                fontSize = (w * 0.17f).sp, fontWeight = FontWeight.Light, lineHeight = (w * 0.18f).sp)
        }
        // notifications
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = (w * 0.42f).dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            repeat(2) { i ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xD91C1C1E)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(if (i == 0) Color(0xFF25D366) else Color(0xFF5B8DEF)))
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth(0.45f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.75f)))
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth(0.8f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.4f)))
                    }
                }
            }
        }
        // shortcuts + fingerprint
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Call, null, tint = white, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.Fingerprint, null, tint = white.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.PhotoCamera, null, tint = white, modifier = Modifier.size(16.dp)) }
        }
    }
}

/** Shares the finished wallpaper with a small "Zujaj" mark (a bigger one when Wallpapers isn't unlocked). */
private fun shareWall(ctx: android.content.Context, d: WallDesign, photo: Bitmap, mask: Bitmap?, W: Int, H: Int, big: Boolean) {
    val img = WallRender.compose(ctx, d, photo, mask, W, H)
    val c = android.graphics.Canvas(img)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE; alpha = if (big) 210 else 150
        textSize = W * (if (big) 0.05f else 0.026f); textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        setShadowLayer(W * 0.006f, 0f, W * 0.002f, android.graphics.Color.argb(120, 0, 0, 0))
    }
    c.drawText("Zujaj · زجاج", W / 2f, H * (if (big) 0.6f else 0.965f), p)
    val dir = java.io.File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = java.io.File(dir, "zujaj_wallpaper.jpg")
    f.outputStream().use { img.compress(Bitmap.CompressFormat.JPEG, 92, it) }
    val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".share", f)
    val send = Intent(Intent.ACTION_SEND).setType("image/jpeg").putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + ctx.packageName)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
