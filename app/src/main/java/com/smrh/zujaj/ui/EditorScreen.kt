package com.smrh.zujaj.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.Feat
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Cal
import com.smrh.zujaj.model.ContactAction
import com.smrh.zujaj.model.Fill
import com.smrh.zujaj.model.Lang
import com.smrh.zujaj.model.Numerals
import com.smrh.zujaj.model.Tap
import com.smrh.zujaj.model.Texture
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.widget.PinReceiver
import com.smrh.zujaj.widget.Scheduler
import com.smrh.zujaj.widget.WidgetUpdater
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

private val presets = listOf(
    // name, fill, bg, bg2, c1, c2, texture
    Triple("Graphite", "جرافيت", WidgetConfig("x")),
    Triple("Glass", "زجاج", WidgetConfig("x", fill = Fill.GLASS, bgColor = 0xFF20202A.toInt(), texture = Texture.NOISE)),
    Triple("Aurora", "شفق", WidgetConfig("x", fill = Fill.AURORA, bgColor = 0xFF0E0E16.toInt(), bgColor2 = 0xFF4C3BCF.toInt(), accent = 0xFFE8478B.toInt())),
    Triple("Gold", "ذهبي", WidgetConfig("x", fill = Fill.GRADIENT, bgColor = 0xFF17130B.toInt(), bgColor2 = 0xFF3A2C12.toInt(), color1 = 0xFFF2D49A.toInt(), color2 = 0xFFB8995E.toInt(), accent = 0xFFE7B75F.toInt())),
    Triple("Paper", "ورق", WidgetConfig("x", bgColor = 0xFFF2EFE8.toInt(), color1 = 0xFF1B1B1F.toInt(), color2 = 0xFF6E6A62.toInt(), texture = Texture.GRAIN)),
    Triple("Ocean", "محيط", WidgetConfig("x", fill = Fill.GRADIENT, bgColor = 0xFF06223A.toInt(), bgColor2 = 0xFF0E5068.toInt(), color2 = 0xFF8FC6D8.toInt(), accent = 0xFF38C6D9.toInt())),
    Triple("Rose", "وردي", WidgetConfig("x", fill = Fill.GRADIENT, bgColor = 0xFF2A0E1C.toInt(), bgColor2 = 0xFF5C1F3A.toInt(), color2 = 0xFFE7A9C2.toInt(), accent = 0xFFFF6B9A.toInt())),
    Triple("Clear", "شفاف", WidgetConfig("x", bg = false)),
)

private fun WidgetConfig.applyLook(p: WidgetConfig) = copy(
    bg = p.bg, fill = p.fill, bgColor = p.bgColor, bgColor2 = p.bgColor2, texture = p.texture,
    color1 = p.color1, color2 = p.color2, accent = p.accent, border = p.border,
)

@Composable
fun EditorScreen(nav: Nav, s: Screen.Editor) {
    val ctx = LocalContext.current
    val style = Catalog.byId(s.styleId) ?: return
    var cfg by remember(s) {
        mutableStateOf(if (s.widgetId != -1) Prefs.widgetConfig(s.widgetId)?.takeIf { it.styleId == s.styleId } ?: style.defaultConfig() else style.defaultConfig())
    }
    var fav by remember { mutableStateOf(Prefs.favorites.contains(style.id)) }
    var colorTarget by remember { mutableIntStateOf(-1) }
    var showApps by remember { mutableStateOf(false) }
    var showTapApp by remember { mutableStateOf(false) }
    var showCity by remember { mutableStateOf(false) }
    val ar = Prefs.isArabicUi
    val f = style.feats

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            val paths = uris.mapNotNull { importPhoto(ctx, it) }
            cfg = cfg.copy(photos = (cfg.photos + paths).takeLast(style.maxItems.coerceAtLeast(1)))
        }
    }
    val contactLauncher = rememberLauncherForActivityResult(PickPhone()) { uri ->
        uri?.let { readPickedPhone(ctx, it) }?.let { c ->
            cfg = cfg.copy(contacts = (cfg.contacts + c).takeLast(style.maxItems.coerceAtLeast(1)))
        }
    }

    var tab by remember { mutableIntStateOf(0) }
    var stage by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(style.name(ar), { if (!nav.back()) (ctx as? Activity)?.finish() }) {
                IconButton(onClick = {
                    fav = !fav
                    Prefs.favorites = if (fav) Prefs.favorites + style.id else Prefs.favorites - style.id
                }) { Icon(if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null, tint = if (fav) LocalAccent.current else Z.text) }
                IconButton(onClick = { cfg = style.defaultConfig().copy(apps = cfg.apps, contacts = cfg.contacts, photos = cfg.photos, cities = cfg.cities) }) {
                    Icon(Icons.Rounded.Refresh, null, tint = Z.text)
                }
            }
            // ---------------------------------------------------------------- preview
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(30.dp))
                    .background(Backdrops.brush(stage))
                    .padding(top = 26.dp, bottom = 46.dp, start = if (style.shape == Shape.SMALL) 84.dp else 22.dp, end = if (style.shape == Shape.SMALL) 84.dp else 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                WidgetPreview(cfg, Modifier.fillMaxWidth(), density = 3f, live = true)
            }
            // backdrop picker floats over the stage's bottom edge
            Row(
                Modifier.fillMaxWidth().offset(y = (-36).dp), horizontalArrangement = Arrangement.Center,
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color(0xCC0B0B12)).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(Backdrops.count) { i ->
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(Backdrops.brush(i))
                                .border(2.dp, if (i == stage) Color.White else Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable { stage = i }
                        )
                    }
                }
            }
            Segmented(
                listOf(tr("Look", "الشكل"), tr("Content", "المحتوى"), tr("Region", "اللغة"), tr("More", "المزيد")),
                tab, Modifier.padding(horizontal = 16.dp).offset(y = (-22).dp),
            ) { tab = it }
            // ---------------------------------------------------------------- controls
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (tab == 0) {
                Section(tr("Style presets", "أنماط جاهزة")) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { (en, a, p) -> ZChip(if (ar) a else en, false) { cfg = cfg.applyLook(p) } }
                    }
                }

                Section(tr("Background", "الخلفية")) {
                    SwitchRow(tr("Enable", "تفعيل"), cfg.bg) { cfg = cfg.copy(bg = it) }
                    if (cfg.bg) {
                        Divider()
                        ChoiceRow(tr("Fill", "التعبئة"), cfg.fill, listOf(
                            Fill.SOLID to tr("Solid", "سادة"), Fill.GRADIENT to tr("Gradient", "متدرج"),
                            Fill.GLASS to tr("Glass", "زجاجي"), Fill.AURORA to tr("Aurora", "شفق"),
                        )) { cfg = cfg.copy(fill = it) }
                        Divider()
                        RowBase(tr("Color", "اللون")) { ColorDot(cfg.bgColor) { colorTarget = 10 } }
                        if (cfg.fill == Fill.GRADIENT || cfg.fill == Fill.AURORA) {
                            Divider(); RowBase(tr("Color 2", "اللون ٢")) { ColorDot(cfg.bgColor2) { colorTarget = 11 } }
                        }
                        Divider()
                        StepperRow(tr("Opacity", "الشفافية"), cfg.bgAlpha, 0..100, 5, "%") { cfg = cfg.copy(bgAlpha = it) }
                        Divider()
                        ChoiceRow(tr("Texture", "الملمس"), cfg.texture, listOf(
                            Texture.OFF to tr("Off", "بدون"), Texture.NOISE to tr("Noise", "حبيبات"), Texture.GRAIN to tr("Grain", "خشن"),
                            Texture.LINES to tr("Lines", "خطوط"), Texture.DOTS to tr("Dots", "نقاط"), Texture.GRID to tr("Grid", "شبكة"), Texture.WAVES to tr("Waves", "أمواج"),
                        )) { cfg = cfg.copy(texture = it) }
                        Divider()
                        StepperRow(tr("Corners", "الحواف"), cfg.corner, 0..60, 2) { cfg = cfg.copy(corner = it) }
                        Divider()
                        SwitchRow(tr("Border", "إطار"), cfg.border) { cfg = cfg.copy(border = it) }
                    }
                }

                }
                if (tab == 1) {
                Section(tr("Content", "المحتوى")) {
                    RowBase(tr("Color 1", "اللون ١")) { ColorDot(cfg.color1) { colorTarget = 1 } }
                    if (Feat.COLOR2 in f || true) { Divider(); RowBase(tr("Color 2", "اللون ٢")) { ColorDot(cfg.color2) { colorTarget = 2 } } }
                    if (Feat.ACCENT in f) { Divider(); RowBase(tr("Accent", "لون مميز")) { ColorDot(cfg.accent) { colorTarget = 3 } } }
                    Divider(); StepperRow(tr("Size 1", "الحجم ١"), cfg.size1, 50..160, 5, "%") { cfg = cfg.copy(size1 = it) }
                    Divider(); StepperRow(tr("Size 2", "الحجم ٢"), cfg.size2, 50..160, 5, "%") { cfg = cfg.copy(size2 = it) }
                    Divider(); StepperRow(tr("Spacing", "تباعد الحروف"), cfg.spacing, -10..40, 1) { cfg = cfg.copy(spacing = it) }
                    if (Feat.CALENDAR in f) {
                        Divider(); ChoiceRow(tr("Calendar", "التقويم"), cfg.calendar, listOf(Cal.GREGORIAN to tr("Gregorian", "ميلادي"), Cal.HIJRI to tr("Hijri", "هجري"))) { cfg = cfg.copy(calendar = it) }
                    }
                    if (Feat.HOUR24 in f || style.feats.contains(Feat.SECONDS)) {
                        Divider(); SwitchRow(tr("24-hour", "نظام ٢٤ ساعة"), cfg.hour24) { cfg = cfg.copy(hour24 = it) }
                    }
                    if (Feat.SECONDS in f) {
                        Divider(); SwitchRow(tr("Seconds hand", "عقرب الثواني"), cfg.seconds, tr("Android 12+", "أندرويد ١٢ فأحدث")) { cfg = cfg.copy(seconds = it) }
                    }
                    if (Feat.TITLE in f) {
                        Divider(); TextInput(tr("Title", "العنوان"), cfg.title) { cfg = cfg.copy(title = it) }
                    }
                    if (Feat.TEXT in f) {
                        Divider(); TextInput(tr("Text", "النص"), cfg.text, single = false) { cfg = cfg.copy(text = it) }
                    }
                    if (Feat.AUTHOR in f) {
                        Divider(); TextInput(tr("Author", "الكاتب"), cfg.author) { cfg = cfg.copy(author = it) }
                    }
                    if (Feat.GOAL in f) {
                        Divider(); ChoiceRow(tr("Goal", "الهدف"), cfg.goal, listOf(33 to "33", 99 to "99", 100 to "100", 1000 to "1000")) { cfg = cfg.copy(goal = it) }
                        Divider(); RowBase(tr("Reset counter", "تصفير العداد"), sub = cfg.counter.toString(), onClick = { cfg = cfg.copy(counter = 0) }) {}
                    }
                    if (Feat.TARGET_DATE in f) {
                        Divider()
                        val label = if (cfg.targetMillis > 0) SimpleDateFormat("d MMM yyyy · HH:mm", java.util.Locale.getDefault()).format(Date(cfg.targetMillis)) else tr("Pick a date", "اختر تاريخاً")
                        RowBase(tr("Target date", "التاريخ المستهدف"), sub = label, onClick = { pickDateTime(ctx, cfg.targetMillis) { cfg = cfg.copy(targetMillis = it) } }) {}
                    }
                    if (Feat.APPS in f) {
                        Divider()
                        RowBase(tr("Apps", "التطبيقات"), sub = "${cfg.apps.size}/${style.maxItems}", onClick = { showApps = true }) { Text(tr("Choose", "اختيار"), color = LocalAccent.current) }
                    }
                    if (Feat.CONTACTS in f) {
                        Divider()
                        RowBase(tr("People", "الأشخاص"), sub = cfg.contacts.joinToString { it.name }.ifBlank { "0/${style.maxItems}" }, onClick = { contactLauncher.launch(Unit) }) {
                            Text("+", color = LocalAccent.current)
                        }
                        cfg.contacts.forEachIndexed { i, c ->
                            Divider()
                            ChoiceRow(c.name, c.action, listOf(
                                ContactAction.CALL to tr("Call", "اتصال"), ContactAction.SMS to tr("Message", "رسالة"),
                                ContactAction.WHATSAPP to "WhatsApp", ContactAction.OPEN to tr("Remove", "حذف"),
                            )) { a ->
                                cfg = if (a == ContactAction.OPEN) cfg.copy(contacts = cfg.contacts.filterIndexed { j, _ -> j != i })
                                else cfg.copy(contacts = cfg.contacts.mapIndexed { j, x -> if (j == i) x.copy(action = a) else x })
                            }
                        }
                    }
                    if (Feat.PHOTOS in f) {
                        Divider()
                        RowBase(tr("Photos", "الصور"), sub = tr("${cfg.photos.size} selected · changes daily", "${cfg.photos.size} صورة · تتغير يومياً"),
                            onClick = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Text(tr("Add", "إضافة"), color = LocalAccent.current)
                        }
                        if (cfg.photos.isNotEmpty()) { Divider(); RowBase(tr("Clear photos", "حذف الصور"), onClick = { cfg = cfg.copy(photos = emptyList()) }) {} }
                    }
                    if (Feat.CITIES in f) {
                        Divider()
                        RowBase(tr("Cities", "المدن"), sub = cfg.cities.joinToString { it.name }.ifBlank { tr("Default", "افتراضي") }, onClick = { showCity = true }) {
                            Text("+", color = LocalAccent.current)
                        }
                        if (cfg.cities.isNotEmpty()) { Divider(); RowBase(tr("Clear cities", "مسح المدن"), onClick = { cfg = cfg.copy(cities = emptyList()) }) {} }
                    }
                }

                }
                if (tab == 2) {
                Section(tr("Region", "المنطقة")) {
                    ChoiceRow(tr("Language", "اللغة"), cfg.language, listOf(Lang.AUTO to tr("Auto", "تلقائي"), Lang.EN to "English", Lang.AR to "العربية")) { cfg = cfg.copy(language = it) }
                    Divider()
                    ChoiceRow(tr("Numerals", "الأرقام"), cfg.numerals, listOf(Numerals.AUTO to tr("Auto", "تلقائي"), Numerals.LATIN to "123", Numerals.ARABIC to "١٢٣")) { cfg = cfg.copy(numerals = it) }
                    Divider()
                    ChoiceRow(tr("Arabic calligraphy", "الخط العربي"), cfg.calli, listOf(
                        0 to tr("Thuluth", "ثلث"), 1 to tr("Ruqaa", "رقعة"), 2 to tr("Naskh", "نسخ"), 3 to tr("Kufi", "كوفي"),
                    )) { cfg = cfg.copy(calli = it) }
                }

                }
                if (tab == 3) {
                Section(tr("Advanced", "متقدم")) {
                    StepperRow(tr("Padding", "الهوامش"), cfg.padding, 0..40, 2) { cfg = cfg.copy(padding = it) }
                    Divider()
                    ChoiceRow(tr("Tap action", "عند الضغط"), cfg.tap, listOf(
                        Tap.DEFAULT to tr("Default", "افتراضي"), Tap.EDIT to tr("Edit widget", "تعديل"),
                        Tap.OPEN_APP to tr("Open app…", "فتح تطبيق…"), Tap.NONE to tr("Nothing", "لا شيء"),
                    )) { cfg = cfg.copy(tap = it); if (it == Tap.OPEN_APP) showTapApp = true }
                }
                }
                Spacer(Modifier.height(130.dp))
            }
        }

        // ---------------------------------------------------------------- apply
        val locked = Flags.locked(style)
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Z.bg.copy(alpha = 0.96f), Z.bg)))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 14.dp)
        ) {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (locked) tr("Unlock with Pro", "افتح مع برو") else if (s.widgetId != -1) tr("Save", "حفظ") else tr("Apply", "إضافة للشاشة"),
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Check,
            ) {
                when {
                    locked -> nav.go(Screen.Paywall)
                    s.widgetId != -1 -> {
                        Prefs.saveWidgetConfig(s.widgetId, cfg)
                        WidgetUpdater.update(ctx, s.widgetId)
                        if (cfg.cities.isNotEmpty()) Scheduler.refreshNow(ctx)
                        if (s.configure) (ctx as? MainActivity)?.finishConfigure(s.widgetId)
                        else { Toast.makeText(ctx, tr("Saved ✓", "تم الحفظ ✓"), Toast.LENGTH_SHORT).show(); if (!nav.back()) (ctx as? Activity)?.finish() }
                    }
                    else -> pin(ctx, cfg, style.shape)
                }
            }
        }
    }

    if (colorTarget != -1) {
        val init = when (colorTarget) { 1 -> cfg.color1; 2 -> cfg.color2; 3 -> cfg.accent; 10 -> cfg.bgColor; else -> cfg.bgColor2 }
        ColorPickerDialog(init, { colorTarget = -1 }) { c ->
            cfg = when (colorTarget) {
                1 -> cfg.copy(color1 = c); 2 -> cfg.copy(color2 = c); 3 -> cfg.copy(accent = c)
                10 -> cfg.copy(bgColor = c); else -> cfg.copy(bgColor2 = c)
            }
        }
    }
    if (showApps) AppsPickerDialog(cfg.apps, style.maxItems, { showApps = false }) { cfg = cfg.copy(apps = it) }
    if (showTapApp) AppsPickerDialog(listOfNotNull(cfg.tapApp), 1, { showTapApp = false }) { cfg = cfg.copy(tapApp = it.firstOrNull()) }
    if (showCity) CityPickerDialog({ showCity = false }) { c -> cfg = cfg.copy(cities = (cfg.cities + c).takeLast(style.maxItems.coerceAtLeast(1))) }
}

@Composable
private fun TextInput(label: String, value: String, single: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = single, maxLines = if (single) 1 else 4,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private fun pickDateTime(ctx: Context, initial: Long, onPick: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { if (initial > 0) timeInMillis = initial else add(Calendar.DAY_OF_YEAR, 30) }
    DatePickerDialog(ctx, { _, y, m, d ->
        TimePickerDialog(ctx, { _, hh, mm ->
            cal.set(y, m, d, hh, mm, 0)
            onPick(cal.timeInMillis)
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
}

/** Ask the launcher to place the widget (Android 8+). */
fun pin(ctx: Context, cfg: WidgetConfig, shape: Shape) {
    val mgr = AppWidgetManager.getInstance(ctx)
    if (!mgr.isRequestPinAppWidgetSupported) {
        Toast.makeText(ctx, tr("Your launcher doesn't support direct adding. Long-press the home screen → Widgets → Zujaj.", "المشغل لا يدعم الإضافة المباشرة. اضغط مطولاً على الشاشة ← الأدوات ← زجاج."), Toast.LENGTH_LONG).show()
        Prefs.setPending("manual", cfg)
        return
    }
    val token = UUID.randomUUID().toString()
    Prefs.setPending(token, cfg)
    val cb = PendingIntent.getBroadcast(
        ctx, token.hashCode(), Intent(ctx, PinReceiver::class.java).putExtra("token", token),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )
    mgr.requestPinAppWidget(ComponentName(ctx, WidgetUpdater.providerFor(shape)), null, cb)
}

@Suppress("unused")
private val w = Modifier.width(1.dp)
