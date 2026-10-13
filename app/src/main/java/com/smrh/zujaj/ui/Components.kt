package com.smrh.zujaj.ui

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
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.1f, 0f), radius = size.width * 0.9f), radius = size.width * 0.9f, center = Offset(size.width * 0.1f, 0f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFF5B4BFF).copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width, size.height * 0.55f), radius = size.width * 0.8f), radius = size.width * 0.8f, center = Offset(size.width, size.height * 0.55f))
        }
        content()
    }
}

@Composable
fun Glass(modifier: Modifier = Modifier, radius: Dp = 22.dp, alpha: Float = 0.06f, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = alpha + 0.03f), Color.White.copy(alpha = alpha))))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)))), RoundedCornerShape(radius)),
        content = content,
    )
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Z.text) }
        } else Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
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
    val bg by animateColorAsState(if (selected) accent else Color.White.copy(alpha = 0.06f), label = "chip")
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg)
            .border(1.dp, if (selected) Color.Transparent else Z.line, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = if (selected) Color(0xFF14110A) else Z.text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(Color(0xCC14110A)).padding(4.dp)) {
        Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(14.dp))
    }
}

@Composable
fun NewBadge(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(LocalAccent.current).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(tr("New", "جديد"), color = Color(0xFF14110A), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------------------ setting rows
@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, color = Z.sub, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 8.dp))
    Glass(Modifier.fillMaxWidth().padding(horizontal = 14.dp), radius = 20.dp, content = content)
}

@Composable
fun RowBase(label: String, sub: String? = null, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = Z.text)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Z.sub)
        }
        trailing()
    }
}

@Composable
fun SwitchRow(label: String, value: Boolean, sub: String? = null, onChange: (Boolean) -> Unit) {
    RowBase(label, sub, { onChange(!value) }) {
        Switch(value, onChange, colors = SwitchDefaults.colors(checkedTrackColor = LocalAccent.current, checkedThumbColor = Color.White))
    }
}

@Composable
fun <T> ChoiceRow(label: String, value: T, options: List<Pair<T, String>>, onChange: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    RowBase(label, onClick = { open = true }) {
        Box {
            Text((options.firstOrNull { it.first == value }?.second ?: "") + "  ▾", color = LocalAccent.current, style = MaterialTheme.typography.labelLarge)
            DropdownMenu(open, { open = false }, modifier = Modifier.background(Z.surface2)) {
                options.forEach { (v, l) -> DropdownMenuItem(text = { Text(l) }, onClick = { onChange(v); open = false }) }
            }
        }
    }
}

@Composable
fun StepperRow(label: String, value: Int, range: IntRange, step: Int = 1, suffix: String = "", onChange: (Int) -> Unit) {
    RowBase(label) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.Remove) { onChange((value - step).coerceIn(range)) }
            Text("$value$suffix", modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.labelLarge)
            RoundIcon(Icons.Rounded.Add) { onChange((value + step).coerceIn(range)) }
        }
    }
}

@Composable
fun RoundIcon(icon: ImageVector, size: Dp = 30.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Z.text, modifier = Modifier.size(size * 0.6f))
    }
}

@Composable
fun ColorDot(color: Int, size: Dp = 26.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(color)).border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape).clickable(onClick = onClick))
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Z.line))

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.82f))))
            .clickable(onClick = onClick).padding(horizontal = 26.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = Color(0xFF14110A), modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = Color(0xFF14110A), style = MaterialTheme.typography.labelLarge, fontSize = 15.sp)
    }
}

// ------------------------------------------------------------------ live preview of a widget
object PreviewCache {
    private val cache = object : LruCache<String, Bitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun get(ctx: android.content.Context, cfg: WidgetConfig, density: Float, stamp: Long): Bitmap {
        val key = "${cfg.hashCode()}|$density|$stamp|${Prefs.version.value}"
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
    val bmp by produceState<ImageBitmap?>(null, cfg, tick, ver, density) {
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
