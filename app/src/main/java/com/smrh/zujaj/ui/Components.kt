package com.smrh.zujaj.ui

import androidx.compose.foundation.layout.widthIn
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.Renderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// ------------------------------------------------------------------ backdrop
@Composable
fun AppBackdrop(content: @Composable BoxScope.() -> Unit) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize().background(Z.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            fun glow(c: Color, a: Float, x: Float, y: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(c.copy(alpha = a), Color.Transparent), center = Offset(x, y), radius = r), radius = r, center = Offset(x, y))
            val k = if (Z.light) 0.45f else 1f
            val r = minOf(size.width, 900f * density)
            glow(Color(0xFF6C5CE7), 0.22f * k, size.width * 0.12f, -r * 0.18f, r * 1.05f)
            glow(accent, 0.07f * k, size.width * 0.92f, size.height * 1.04f, r * 0.9f)
        }
        content()
    }
}

@Composable
fun Glass(modifier: Modifier = Modifier, radius: Dp = 22.dp, alpha: Float = 0.06f, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Z.tint(alpha + 0.015f))
            .border(1.dp, Z.tint(0.08f), RoundedCornerShape(radius)),
        content = content,
    )
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Sp.xs), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            // the keyboard goes away first, so leaving never leaves it floating over the previous screen
            IconButton(onClick = { focus.clearFocus(); onBack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr("Back", "رجوع"), tint = Z.text) }
        } else Spacer(Modifier.width(Sp.m))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(22.dp)) {
            val s = size.width
            drawRoundRect(color = accent, topLeft = Offset(s * 0.05f, s * 0.05f), size = androidx.compose.ui.geometry.Size(s * 0.62f, s * 0.62f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.16f), style = androidx.compose.ui.graphics.drawscope.Stroke(s * 0.1f))
            drawRoundRect(color = Color.White.copy(alpha = 0.85f), topLeft = Offset(s * 0.33f, s * 0.33f), size = androidx.compose.ui.geometry.Size(s * 0.62f, s * 0.62f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.16f))
        }
        Spacer(Modifier.width(8.dp))
        Text("ZUJAJ", fontFamily = Z.bodoni, fontSize = 19.sp, letterSpacing = 4.sp, color = Z.text)
    }
}

// ------------------------------------------------------------------ chips & badges
@Composable
fun ZChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val ctx = LocalContext.current
    val bg by animateColorAsState(if (selected) accent.copy(alpha = 0.16f) else Z.tint(0.045f), Motion.color(), label = "chip")
    val bd by animateColorAsState(if (selected) accent.copy(alpha = 0.7f) else Z.tint(0.07f), Motion.color(), label = "chipb")
    val fg by animateColorAsState(if (selected) accent else Z.text.copy(alpha = 0.86f), Motion.color(), label = "chipf")
    Box(
        Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(Rad.s)).background(bg)
            .border(1.dp, bd, RoundedCornerShape(Rad.s))
            .clickable { Haptics.tick(ctx); onClick() }.padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(50))
            .background(Brush.linearGradient(listOf(Color(0xFFF6D98B), Color(0xFFC9963A))))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) { Text("PRO", color = Color(0xFF2A1C05), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp) }
}

@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Color(0xCC0B0B12)).padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF3DDC97)))
        Spacer(Modifier.width(4.dp))
        Text(tr("NEW", "جديد"), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/** Mini "wallpapers" the widgets are shown on, so previews look like a real home screen. */
object Backdrops {
    private val sets = listOf(
        listOf(Color(0xFF1B1830), Color(0xFF0B0B14), Color(0xFF231536)),
        listOf(Color(0xFF3A1C5C), Color(0xFF14243F), Color(0xFF0E3B44)),
        listOf(Color(0xFF4A2A1A), Color(0xFF1E1420), Color(0xFF3A1730)),
        listOf(Color(0xFF0E3446), Color(0xFF0A1622), Color(0xFF123A35)),
        listOf(Color(0xFF2E2B26), Color(0xFF14130F), Color(0xFF3A3020)),
        listOf(Color(0xFFD9D4CC), Color(0xFFB8B0A4), Color(0xFFE8E2D8)),
    )
    val count get() = sets.size
    fun brush(i: Int): Brush = Brush.linearGradient(sets[i.mod(sets.size)])
}

@Composable
fun Segmented(items: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val accent = LocalAccent.current
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.m)).background(Z.surface2)
            .border(1.dp, Z.line, RoundedCornerShape(Rad.m)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { i, label ->
            val sel = i == selected
            val segBg by animateColorAsState(if (sel) accent else Color.Transparent, Motion.color(), label = "seg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(Rad.m - 4.dp))
                    .background(segBg)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (sel) Color(0xFF14110A) else Z.sub, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------ setting rows
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title.uppercase(), color = Z.sub, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, letterSpacing = if (Prefs.isArabicUi) 0.sp else 1.2.sp,
        modifier = Modifier.padding(start = Sp.l + 4.dp, end = Sp.l + 4.dp, top = Sp.l, bottom = Sp.s))
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Sp.l).clip(RoundedCornerShape(Rad.l))
            .background(Z.surface)
            .border(1.dp, Z.line, RoundedCornerShape(Rad.l)),
        content = content,
    )
}

/** Coloured rounded square with an icon, used at the start of setting rows. */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 32.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(color.copy(alpha = if (Z.light) 0.12f else 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.56f))
    }
}

@Composable
fun RowBase(label: String, sub: String? = null, onClick: (() -> Unit)? = null, icon: ImageVector? = null, iconColor: Color = Color(0xFF8B7CFF), trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp).padding(horizontal = Sp.l, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { IconTile(icon, iconColor); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp), color = Z.text)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp), color = Z.sub, modifier = Modifier.padding(top = 1.dp))
        }
        trailing()
    }
}

@Composable
fun SwitchRow(label: String, value: Boolean, sub: String? = null, icon: ImageVector? = null, iconColor: Color = Color(0xFF8B7CFF), onChange: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    RowBase(label, sub, { Haptics.tick(ctx); onChange(!value) }, icon, iconColor) {
        Switch(value, { Haptics.tick(ctx); onChange(it) }, colors = SwitchDefaults.colors(checkedTrackColor = LocalAccent.current, checkedThumbColor = Color.White))
    }
}

@Composable
fun <T> ChoiceRow(label: String, value: T, options: List<Pair<T, String>>, icon: ImageVector? = null, iconColor: Color = Color(0xFF8B7CFF), onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    RowBase(label, onClick = { open = true }, icon = icon, iconColor = iconColor) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(options.firstOrNull { it.first == value }?.second ?: "", color = LocalAccent.current, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Icon(Icons.Rounded.UnfoldMore, null, tint = LocalAccent.current.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            }
            DropdownMenu(open, { open = false }, modifier = Modifier.background(Z.surface2)) {
                options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onChange(v); open = false }) }
            }
        }
    }
}

@Composable
fun StepperRow(label: String, value: Int, range: IntRange, step: Int = 1, suffix: String = "", icon: ImageVector? = null, iconColor: Color = Color(0xFF8B7CFF), onChange: (Int) -> Unit) {
    RowBase(label, icon = icon, iconColor = iconColor) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.Remove) { onChange((value - step).coerceIn(range)) }
            Text("$value$suffix", modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            RoundIcon(Icons.Rounded.Add) { onChange((value + step).coerceIn(range)) }
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, size: Dp = 30.dp, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Box(Modifier.pressScale().size(size).clip(CircleShape).background(Z.tint(0.08f)).clickable { Haptics.tick(ctx); onClick() }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Z.text, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun ColorDot(color: Int, size: Dp = 26.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(color)).border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape).clickable(onClick = onClick))
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().padding(start = Sp.l + 46.dp, end = Sp.l).height(1.dp).background(Z.tint(0.055f)))

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val ctx = LocalContext.current
    // a soft breathing glow under the button and a light sweep across it
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "btn")
    val breathe by t.animateFloat(0.35f, 0.7f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1600), androidx.compose.animation.core.RepeatMode.Reverse), label = "b")
    val sweep by t.animateFloat(-0.5f, 1.6f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2600, delayMillis = 900)), label = "s")
    Row(
        modifier.pressScale().clip(RoundedCornerShape(50))
            .drawWithContent {
                // the button itself glows: a bright core that breathes, a lit top edge and a light sweep
                val lit = androidx.compose.ui.graphics.lerp(accent, Color.White, 0.18f + 0.22f * (breathe - 0.35f) / 0.35f)
                drawRect(Brush.horizontalGradient(listOf(accent, lit, accent)))
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), 0f, size.height * 0.55f))
                val x = size.width * sweep
                drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent), Offset(x - size.height * 1.4f, 0f), Offset(x, size.height)))
                drawContent()
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))), RoundedCornerShape(50))
            .clickable { Haptics.click(ctx); onClick() }.padding(horizontal = 26.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = Color(0xFF14110A), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Color(0xFF14110A), style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
    }
}

/** Shrinks a touch while pressed and springs back — makes every tap feel physical. */
fun Modifier.pressScale(min: Float = 0.94f): Modifier = composed {
    var down by remember { mutableStateOf(false) }
    val k by androidx.compose.animation.core.animateFloatAsState(if (down) min else 1f, androidx.compose.animation.core.spring(0.45f, 700f), label = "press")
    this.graphicsLayer { scaleX = k; scaleY = k }.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false); down = true
            waitForUpOrCancellation(); down = false
        }
    }
}

/** A coloured halo behind the element (drawn outside its bounds). */
fun Modifier.glow(color: Color, radius: Dp, alpha: Float = 0.5f): Modifier = this.drawBehind {
    val r = radius.toPx()
    drawRoundRect(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = Offset(size.width / 2, size.height / 2 + r * 0.25f), radius = maxOf(size.width, size.height) / 2 + r),
        topLeft = Offset(-r, -r * 0.6f), size = androidx.compose.ui.geometry.Size(size.width + 2 * r, size.height + 2 * r),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2 + r),
    )
}

// ------------------------------------------------------------------ live preview of a widget
object PreviewCache {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun key(cfg: WidgetConfig, density: Float, stamp: Long) = "${cfg.hashCode()}|$density|$stamp|${Prefs.version.value}"
    /** Already-rendered bitmap, so a screen coming back (Back) shows its widgets on the very first frame. */
    fun peek(cfg: WidgetConfig, density: Float, stamp: Long): Bitmap? = cache.get(key(cfg, density, stamp))
    fun get(ctx: android.content.Context, cfg: WidgetConfig, density: Float, stamp: Long): Bitmap {
        val key = key(cfg, density, stamp)
        cache.get(key)?.let { return it }
        val st = Catalog.byId(cfg.styleId)!!
        val b = Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, preview = true, density = density).bitmap
        cache.put(key, b)
        return b
    }
}

@Composable
fun rememberTicker(periodMs: Long): Long {
    var t by remember { mutableLongStateOf(System.currentTimeMillis() / periodMs) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs - System.currentTimeMillis() % periodMs + 20)
            t = System.currentTimeMillis() / periodMs
        }
    }
    return t
}

@Composable
fun WidgetPreview(cfg: WidgetConfig, modifier: Modifier = Modifier, density: Float = 2.2f, live: Boolean = false) {
    val ctx = LocalContext.current
    val st = Catalog.byId(cfg.styleId) ?: return
    val ver by Prefs.version.collectAsState()
    val tick = rememberTicker(if (live && cfg.seconds) 1000L else 30_000L)
    val bmp by produceState(remember { PreviewCache.peek(cfg, density, tick)?.asImageBitmap() }, cfg, tick, ver, density) {
        value = withContext(Dispatchers.Default) { runCatching { PreviewCache.get(ctx, cfg, density, tick).asImageBitmap() }.getOrNull() }
    }
    Box(modifier.aspectRatio(st.shape.dw / st.shape.dh)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize()) }
    }
}

val pagePadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 120.dp)

/** Reads a value so the caller recomposes when it changes. */
@Suppress("UNUSED_PARAMETER")
fun touch(v: Any?) {}

// ------------------------------------------------------------------ modern dialog + text field
/** A soft card dialog that pops in: icon on top, title, a line under it, then the content. */
@Composable
fun ZDialog(icon: ImageVector, title: String, sub: String? = null, iconColor: Color = LocalAccent.current, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val k by androidx.compose.animation.core.animateFloatAsState(if (shown) 1f else 0f, Motion.std(Motion.MED), label = "dlg")
        // a dialog keeps a dialog's proportions on every screen: never wider than 440dp, centred
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth().padding(horizontal = 22.dp).graphicsLayer { scaleX = 0.96f + 0.04f * k; scaleY = 0.96f + 0.04f * k; alpha = k.coerceIn(0f, 1f) }
                .clip(RoundedCornerShape(Rad.xl)).background(Z.surface)
                .border(1.dp, Z.line, RoundedCornerShape(Rad.xl)).padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(Rad.l)).background(iconColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Z.text, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (sub != null) {
                Spacer(Modifier.height(6.dp))
                Text(sub, color = Z.sub, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

/** Filled rounded field: soft background, accent ring when focused, optional icon and show/hide for passwords. */
@Composable
fun ZField(
    value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector? = null, password: Boolean = false, error: Boolean = false,
    keyboard: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    center: Boolean = false, mono: Boolean = false, big: Boolean = false, singleLine: Boolean = true, modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    var focused by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    val ring by animateColorAsState(when { error -> Color(0xFFE5484D); focused -> accent; else -> Color.Transparent }, label = "ring")
    val shake = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(error) { if (error) { for (x in listOf(10f, -10f, 7f, -7f, 3f, 0f)) shake.animateTo(x, androidx.compose.animation.core.tween(45)) } }
    val style = MaterialTheme.typography.bodyLarge.copy(
        color = Z.text, fontSize = if (big) 24.sp else 16.sp, letterSpacing = if (big) 6.sp else 0.sp,
        textAlign = if (center) androidx.compose.ui.text.style.TextAlign.Center else androidx.compose.ui.text.style.TextAlign.Start,
        fontFamily = if (mono) androidx.compose.ui.text.font.FontFamily.Monospace else MaterialTheme.typography.bodyLarge.fontFamily,
    )
    androidx.compose.foundation.text.BasicTextField(
        value, onChange, singleLine = singleLine, textStyle = style, keyboardOptions = keyboard,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(accent),
        visualTransformation = if (password && !show) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = modifier.fillMaxWidth().graphicsLayer { translationX = shake.value }.onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Z.tint(0.07f)).border(1.5.dp, ring, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = if (big) 16.dp else 15.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = if (focused) accent else Z.sub, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)) }
                Box(Modifier.weight(1f), contentAlignment = if (center) Alignment.Center else Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, style = style.copy(color = Z.sub.copy(alpha = 0.7f)))
                    inner()
                }
                if (password) Icon(if (show) androidx.compose.material.icons.Icons.Rounded.VisibilityOff else androidx.compose.material.icons.Icons.Rounded.Visibility, null,
                    tint = Z.sub, modifier = Modifier.padding(start = 10.dp).size(20.dp).clip(CircleShape).clickable { show = !show })
            }
        },
    )
}
