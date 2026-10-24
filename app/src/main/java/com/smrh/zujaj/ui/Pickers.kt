package com.smrh.zujaj.ui

import androidx.compose.foundation.layout.widthIn
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.smrh.zujaj.data.Cities
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.AppRef
import com.smrh.zujaj.model.City
import com.smrh.zujaj.model.ContactRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

val swatches = listOf(
    0xFFFFFFFF, 0xFFE8E6E1, 0xFF9C9CA6, 0xFF5A5A66, 0xFF2A2A33, 0xFF1A1A1F, 0xFF0B0B0E, 0xFF000000,
    0xFFE7B75F, 0xFFFFC53D, 0xFFFF8A4C, 0xFFE5484D, 0xFFFF6B9A, 0xFFE8478B, 0xFFB45CFF, 0xFF8B7CFF,
    0xFF6A5CFF, 0xFF3B82F6, 0xFF4DB5FF, 0xFF38C6D9, 0xFF3DD9B4, 0xFF2EB67D, 0xFF8BC34A, 0xFFF4D35E,
    0xFF0E2A47, 0xFF1F3B2D, 0xFF3D1F2E, 0xFF2D2140, 0xFF402A1A, 0xFF1C2E33, 0xFFF7F5F0, 0xFFD9C7A3,
).map { it.toInt() }

@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1]) }
    var v by remember { mutableFloatStateOf(hsv[2]) }
    val current = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(Z.surface).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color(current)).border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape))
                Spacer(Modifier.width(12.dp))
                Text("#%06X".format(0xFFFFFF and current), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(14.dp))
            LazyVerticalGrid(GridCells.Fixed(8), modifier = Modifier.height(150.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(swatches.size) { i ->
                    val c = swatches[i]
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color(c)).border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape).clickable {
                        val t = FloatArray(3); android.graphics.Color.colorToHSV(c, t); h = t[0]; s = t[1]; v = t[2]
                    })
                }
            }
            Text(tr("Hue", "اللون"), color = Z.sub)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 1f, 1f) })))
            Slider(h, { h = it }, valueRange = 0f..359f, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
            Text(tr("Saturation", "التشبع"), color = Z.sub)
            Slider(s, { s = it }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LocalAccent.current))
            Text(tr("Brightness", "السطوع"), color = Z.sub)
            Slider(v, { v = it }, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = LocalAccent.current))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = Z.sub) }
                TextButton(onClick = { onPick(current or 0xFF000000.toInt()); onDismiss() }) { Text(tr("Done", "تم"), color = LocalAccent.current) }
            }
        }
    }
}

data class AppEntry(val ref: AppRef, val icon: Bitmap?)

suspend fun loadApps(ctx: Context): List<AppEntry> = withContext(Dispatchers.IO) {
    val pm = ctx.packageManager
    val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
    list.map {
        val label = it.loadLabel(pm).toString()
        AppEntry(AppRef(it.activityInfo.packageName, it.activityInfo.name, label), runCatching { it.loadIcon(pm).toBitmap(96, 96) }.getOrNull())
    }.filter { it.ref.pkg != ctx.packageName }.sortedBy { it.ref.label.lowercase() }
}

@Composable
fun AppsPickerDialog(initial: List<AppRef>, max: Int, onDismiss: () -> Unit, onDone: (List<AppRef>) -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    val chosen = remember { mutableStateListOf<AppRef>().also { it.addAll(initial) } }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = loadApps(ctx) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(0.94f).fillMaxHeight(0.85f).clip(RoundedCornerShape(Rad.xl)).background(Z.surface).padding(16.dp)) {
            Text(tr("Choose apps", "اختر التطبيقات") + "  (${chosen.size}/$max)", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text(tr("Search", "بحث")) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(apps.filter { q.isBlank() || it.ref.label.contains(q, true) }) { a ->
                    val idx = chosen.indexOfFirst { it.pkg == a.ref.pkg && it.cls == a.ref.cls }
                    Row(Modifier.fillMaxWidth().clickable {
                        if (idx >= 0) chosen.removeAt(idx) else if (chosen.size < max) chosen.add(a.ref)
                    }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        a.icon?.let { Image(it.asImageBitmap(), null, Modifier.size(36.dp)) }
                        Spacer(Modifier.width(12.dp))
                        Text(a.ref.label, modifier = Modifier.weight(1f))
                        if (idx >= 0) Box(Modifier.size(26.dp).clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) {
                            Text("${idx + 1}", color = Color.Black)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { chosen.clear() }) { Text(tr("Clear", "مسح"), color = Z.sub) }
                TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = Z.sub) }
                TextButton(onClick = { onDone(chosen.toList()); onDismiss() }) { Text(tr("Done", "تم"), color = LocalAccent.current) }
            }
        }
    }
}

@Composable
fun CityPickerDialog(onDismiss: () -> Unit, onPick: (City) -> Unit) {
    var q by remember { mutableStateOf("") }
    val ar = Prefs.isArabicUi
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(0.94f).fillMaxHeight(0.8f).clip(RoundedCornerShape(Rad.xl)).background(Z.surface).padding(16.dp)) {
            Text(tr("Choose a city", "اختر مدينة"), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(q, { q = it }, singleLine = true, placeholder = { Text(tr("Search", "بحث")) }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(Cities.list.filter { q.isBlank() || it.en.contains(q, true) || it.ar.contains(q) }) { c ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(c.toCity(ar)); onDismiss() }.padding(vertical = 12.dp)) {
                        Text(if (ar) c.ar else c.en, modifier = Modifier.weight(1f))
                        Text(c.zone.substringAfter('/').replace('_', ' '), color = Z.sub)
                    }
                }
            }
        }
    }
}

/** Pick a phone number (grants temporary read access to just that row – no contacts permission needed). */
class PickPhone : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? = intent?.data
}

fun readPickedPhone(ctx: Context, uri: Uri): ContactRef? = runCatching {
    val cols = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
    )
    ctx.contentResolver.query(uri, cols, null, null, null)?.use { c ->
        if (!c.moveToFirst()) return@use null
        val name = c.getString(0) ?: ""
        val num = c.getString(1) ?: ""
        val photo = c.getString(2)
        var path = ""
        if (photo != null) runCatching {
            ctx.contentResolver.openInputStream(Uri.parse(photo))?.use { ins ->
                val f = File(ctx.filesDir, "people/${UUID.randomUUID()}.jpg").also { it.parentFile?.mkdirs() }
                FileOutputStream(f).use { ins.copyTo(it) }
                path = f.path
            }
        }
        ContactRef(name, num, path)
    }
}.getOrNull()

/** Copy picked images into private storage (downscaled) so widgets can always read them. */
fun importPhoto(ctx: Context, uri: Uri): String? = runCatching {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, o) }
    var sample = 1
    while (o.outWidth / sample > 2400 || o.outHeight / sample > 2400) sample *= 2
    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
    // honour EXIF rotation
    val rot = runCatching {
        ctx.contentResolver.openInputStream(uri)?.use {
            when (androidx.exifinterface.media.ExifInterface(it).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1)) {
                6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)
    val out = if (rot != 0f) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, android.graphics.Matrix().apply { postRotate(rot) }, true) else bmp
    val f = File(ctx.filesDir, "photos/${UUID.randomUUID()}.jpg").also { it.parentFile?.mkdirs() }
    FileOutputStream(f).use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    f.path
}.getOrNull()

@Suppress("unused")
private fun Color.argb() = toArgb()

@Composable
fun CheckMark() = androidx.compose.material3.Icon(Icons.Rounded.Check, null, tint = LocalAccent.current)
