package com.smrh.zujaj.ui

import androidx.compose.animation.togetherWith

import android.app.Activity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloat
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
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
    Triple("Glass", "زجاج", WidgetConfig("x", fill = Fill.GLASS, bgColor = 0xFF20202A.toInt(), texture = Texture.OFF)),
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

/** Tab the editor opens on (used by screenshot tests). */
internal var editorStartTab = 0

@Composable
fun EditorScreen(nav: Nav, s: Screen.Editor) {
    val ctx = LocalContext.current
    val style = Catalog.byId(s.styleId) ?: return
    var cfg by remember(s) {
        mutableStateOf(if (s.shared.isNotEmpty()) ShareDesign.decode(s.shared) ?: style.defaultConfig() else if (s.widgetId != -1) Prefs.widgetConfig(s.widgetId)?.takeIf { it.styleId == s.styleId } ?: style.defaultConfig() else style.defaultConfig())
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

    // does this design show a live time big enough to take a custom font?
    val hasTime = remember(style.id) {
        runCatching {
            com.smrh.zujaj.render.Renderer.render(ctx, style.defaultConfig().copy(font = ""), style.shape.dw, style.shape.dh, preview = false, density = 1f)
                .lives.any { it is com.smrh.zujaj.render.LiveClock && it.sizePx >= 18f } || Feat.HUB in style.feats
        }.getOrDefault(false)
    }
    var tab by remember(s) { mutableIntStateOf(editorStartTab) }
    var stage by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(style.name(ar), { if (!nav.back()) (ctx as? Activity)?.finish() }) {
                val accentArgb = LocalAccent.current.let { android.graphics.Color.argb(255, (it.red * 255).toInt(), (it.green * 255).toInt(), (it.blue * 255).toInt()) }
                IconButton(onClick = { Haptics.tick(ctx); runCatching { ShareDesign.share(ctx, cfg, accentArgb) } }) { Icon(Icons.Rounded.Share, null, tint = Z.text) }
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
                                .border(2.dp, if (i == stage) Color.White else Z.tint(0.15f), CircleShape)
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
            androidx.compose.animation.AnimatedContent(tab, Modifier.weight(1f), label = "tabs", transitionSpec = {
                val dir = (if (targetState > initialState) 1 else -1) * (if (ar) -1 else 1)
                val ease = androidx.compose.animation.core.FastOutSlowInEasing
                (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(320, easing = ease)) { it / 5 * dir } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(240, 60)))
                    .togetherWith(androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(320, easing = ease)) { -it / 5 * dir } + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)))
            }) { tab ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).animateContentSize(androidx.compose.animation.core.tween(280))) {
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
                Section(tr("Numerals", "الأرقام")) { NumeralsSwitch(cfg) { cfg = it } }
                if (hasTime) Section(tr("Clock font", "خط الساعة")) { ClockFontGrid(cfg, { cfg = it }) { nav.go(Screen.Paywall) } }
                if (Feat.HUB in f) Section(tr("Colours of each part", "ألوان كل جزء")) {
                    ChoiceRow(tr("Clock colours", "ألوان الساعة"), cfg.timeMerge, listOf(false to tr("Separate", "منفصلة"), true to tr("One gradient", "تدرج واحد"))) { cfg = cfg.copy(timeMerge = it) }
                    if (cfg.timeMerge) {
                        Divider(); RowBase(tr("Gradient start", "بداية التدرج")) { ColorDot(cfg.color1) { colorTarget = 1 } }
                        Divider(); RowBase(tr("Gradient end", "نهاية التدرج")) { ColorDot(cfg.accent2) { colorTarget = 5 } }
                    } else {
                        Divider(); RowBase(tr("Hours", "الساعات")) { ColorDot(cfg.color1) { colorTarget = 1 } }
                        Divider(); SwitchRow(tr("Gradient hours", "ساعات متدرجة"), cfg.hourGrad) { cfg = cfg.copy(hourGrad = it) }
                        if (cfg.hourGrad) { Divider(); RowBase(tr("Hours — 2nd colour", "اللون التاني للساعات")) { ColorDot(cfg.hourColor2) { colorTarget = 14 } } }
                        Divider(); RowBase(tr("Minutes", "الدقايق")) { ColorDot(cfg.accent) { colorTarget = 3 } }
                        Divider(); SwitchRow(tr("Gradient minutes", "دقايق متدرجة"), cfg.minGrad) { cfg = cfg.copy(minGrad = it) }
                        if (cfg.minGrad) { Divider(); RowBase(tr("Minutes — 2nd colour", "اللون التاني للدقايق")) { ColorDot(cfg.accent2) { colorTarget = 5 } } }
                    }
                    if (cfg.timeMerge || cfg.hourGrad || cfg.minGrad)
                        Text(tr("Gradients are drawn into the picture and refreshed every minute.", "التدرج بيترسم في الصورة وبيتحدث كل دقيقة."),
                            color = Z.sub, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    Divider(); RowBase(tr("Day name", "اسم اليوم")) { ColorDot(cfg.dayColor) { colorTarget = 6 } }
                    Divider(); SwitchRow(tr("Gradient day name", "اسم اليوم متدرج"), cfg.dayGrad) { cfg = cfg.copy(dayGrad = it) }
                    if (cfg.dayGrad) { Divider(); RowBase(tr("Day — 2nd colour", "اللون التاني لليوم")) { ColorDot(cfg.dayColor2) { colorTarget = 15 } } }
                    Divider(); RowBase(tr("Hijri date", "التاريخ الهجري")) { ColorDot(cfg.hijriColor) { colorTarget = 7 } }
                    Divider(); RowBase(tr("Gregorian date", "التاريخ الميلادي")) { ColorDot(cfg.color2) { colorTarget = 2 } }
                    Divider(); RowBase(tr("Prayer names", "أسماء الصلوات")) { ColorDot(cfg.nameColor) { colorTarget = 8 } }
                    Divider(); RowBase(tr("Prayer times", "مواعيد الصلوات")) { ColorDot(cfg.timeColor) { colorTarget = 9 } }
                    Divider(); RowBase(tr("Next prayer", "الصلاة الجاية")) { ColorDot(cfg.nextColor) { colorTarget = 12 } }
                    Divider(); RowBase(tr("Divider line", "الخط الفاصل")) { ColorDot(cfg.lineColor) { colorTarget = 13 } }
                }
                if (Feat.HUB !in f) Section(tr("Content", "المحتوى")) {
                    RowBase(tr("Color 1", "اللون ١")) { ColorDot(cfg.color1) { colorTarget = 1 } }
                    if (Feat.HUB !in f) { Divider(); RowBase(tr("Color 2", "اللون ٢")) { ColorDot(cfg.color2) { colorTarget = 2 } } }
                    if (Feat.ACCENT in f && Feat.HUB !in f) { Divider(); RowBase(tr("Accent", "لون مميز")) { ColorDot(cfg.accent) { colorTarget = 3 } } }
                }
                Section(tr("Text size", "حجم النص")) {
                    if (Feat.HUB in f) {
                        StepperRow(tr("Clock", "الساعة"), cfg.size1, 50..120, 5, "%") { cfg = cfg.copy(size1 = it) }
                        Divider(); StepperRow(tr("Day name", "اسم اليوم"), cfg.size2, 50..140, 5, "%") { cfg = cfg.copy(size2 = it) }
                        Divider(); StepperRow(tr("Dates", "التاريخ"), cfg.sizeDate, 60..150, 5, "%") { cfg = cfg.copy(sizeDate = it) }
                        Divider(); StepperRow(tr("Prayer names", "أسماء الصلوات"), cfg.sizeNames, 60..150, 5, "%") { cfg = cfg.copy(sizeNames = it) }
                        Divider(); StepperRow(tr("Prayer times", "مواعيد الصلوات"), cfg.sizeTimes, 60..150, 5, "%") { cfg = cfg.copy(sizeTimes = it) }
                    } else {
                        StepperRow(tr("Main text", "النص الأساسي"), cfg.size1, 50..160, 5, "%") { cfg = cfg.copy(size1 = it) }
                        Divider(); StepperRow(tr("Secondary text", "النص التاني"), cfg.size2, 50..160, 5, "%") { cfg = cfg.copy(size2 = it) }
                        Divider(); StepperRow(tr("Small text (labels, dates)", "النص الصغير (العناوين والتاريخ)"), cfg.size3, 60..160, 5, "%") { cfg = cfg.copy(size3 = it) }
                    }
                    Divider(); RowBase(tr("Reset sizes", "رجّع الأحجام زي ما كانت"), onClick = { cfg = cfg.copy(size1 = 100, size2 = 100, size3 = 100, sizeDate = 100, sizeNames = 100, sizeTimes = 100) }) {}
                }
                Section(tr("More", "إضافات")) {
                    StepperRow(tr("Spacing", "تباعد الحروف"), cfg.spacing, -10..40, 1) { cfg = cfg.copy(spacing = it) }
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
                    if (Feat.ROTATE in f) {
                        Divider()
                        ChoiceRow(tr("Words", "الكلام"), cfg.variant, listOf(
                            0 to tr("Duas", "أدعية"), 1 to tr("Verses", "آيات"),
                            2 to tr("Dhikr", "أذكار"), 3 to tr("My own words", "كلامي أنا"),
                        )) { cfg = cfg.copy(variant = it) }
                        if (cfg.variant == 3) { Divider(); TextInput(tr("Your words", "كلامك"), cfg.text, single = false) { cfg = cfg.copy(text = it) } }
                        Divider()
                        ChoiceRow(tr("Change every", "يتغير كل"), if (cfg.counter > 0) cfg.counter else 360, listOf(
                            30 to tr("30 min", "نص ساعة"), 60 to tr("1 hour", "ساعة"), 180 to tr("3 hours", "3 ساعات"),
                            360 to tr("6 hours", "6 ساعات"), 720 to tr("12 hours", "12 ساعة"), 1440 to tr("Daily", "يوميًا"),
                        )) { cfg = cfg.copy(counter = it) }
                    }
                    if (Feat.VERSE in f) {
                        Divider()
                        ChoiceRow(tr("Words on the photo", "الكلام على الصورة"), cfg.variant, listOf(
                            1 to tr("Verse of the day", "آية اليوم"), 0 to tr("Dhikr of the day", "ذكر اليوم"),
                            2 to tr("My own words", "كلامي أنا"), 3 to tr("Nothing", "بدون كلام"),
                        )) { cfg = cfg.copy(variant = it) }
                        if (cfg.variant == 2) { Divider(); TextInput(tr("Your words", "كلامك"), cfg.text, single = false) { cfg = cfg.copy(text = it) } }
                    }
                    if (Feat.EVENT in f) {
                        Divider()
                        Text(tr("Count down to", "العد لـ"), color = Z.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
                        androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val evs = listOf("ramadan" to tr("Ramadan", "رمضان"), "fitr" to tr("Eid al-Fitr", "عيد الفطر"), "adha" to tr("Eid al-Adha", "عيد الأضحى"),
                                "birthday" to tr("Birthday", "عيد ميلاد"), "exams" to tr("Exams", "الامتحانات"), "newyear" to tr("New Year", "السنة الجديدة"), "" to tr("Any date", "تاريخ تختاره"))
                            items(evs.size) { i -> val (k, l) = evs[i]; ZChip(l, cfg.event == k) { cfg = cfg.copy(event = k) } }
                        }
                        if (cfg.event in setOf("birthday", "exams", "")) {
                            Divider()
                            val label = if (cfg.targetMillis > 0) SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(Date(cfg.targetMillis)) else tr("Pick a date", "اختر تاريخاً")
                            RowBase(if (cfg.event == "birthday") tr("Birthday date", "تاريخ الميلاد") else tr("Date", "التاريخ"), sub = label,
                                onClick = { pickDateTime(ctx, cfg.targetMillis) { cfg = cfg.copy(targetMillis = it) } }) {}
                        }
                        Divider(); TextInput(tr("Title (optional)", "العنوان (اختياري)"), cfg.title) { cfg = cfg.copy(title = it) }
                    }
                    if (style.cat == com.smrh.zujaj.catalog.Cat.PRAYER || style.cat == com.smrh.zujaj.catalog.Cat.WEATHER) {
                        Divider()
                        SwitchRow(tr("Colours follow the time of day", "الألوان تتغير مع الوقت"), cfg.dayTint,
                            tr("Dawn, day, sunset and night colours on their own", "ألوان الفجر والنهار والمغرب والليل لوحدها")) { cfg = cfg.copy(dayTint = it) }
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
                    ChoiceRow(tr("Numerals", "الأرقام"), if (cfg.numerals == Numerals.ARABIC) Numerals.ARABIC else Numerals.LATIN, listOf(Numerals.LATIN to "123", Numerals.ARABIC to "١٢٣")) { cfg = cfg.copy(numerals = it) }
                    Divider()
                    ChoiceRow(tr("Arabic calligraphy", "الخط العربي"), cfg.calli, listOf(
                        0 to tr("Kufi", "كوفي مزخرف"), 1 to tr("Ruqaa", "رقعة"), 2 to tr("Nastaliq", "نستعليق"), 3 to tr("Naskh", "نسخ"),
                    )) { cfg = cfg.copy(calli = it) }
                    if (cfg.calli == 0) {
                        Divider(); SwitchRow(tr("Ornament", "زخرفة"), cfg.ornament) { cfg = cfg.copy(ornament = it) }
                        if (cfg.ornament) { Divider(); RowBase(tr("Ornament color", "لون الزخرفة")) { ColorDot(cfg.ornColor) { colorTarget = 4 } } }
                    }
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
        }

        // ---------------------------------------------------------------- apply
        val unlockVer by com.smrh.zujaj.ads.AdUnlock.changes.collectAsState()
        val locked = remember(unlockVer, style.id) { Flags.locked(style) }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Z.bg.copy(alpha = 0.96f), Z.bg)))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 14.dp)
        ) {
            AdUnlockCard(style.id, locked, unlockVer)
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (locked) tr("Unlock with Pro", "افتح مع برو") else if (s.widgetId != -1) tr("Save", "حفظ") else tr("Apply", "إضافة للشاشة"),
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Check,
            ) {
                when {
                    locked -> nav.go(Screen.Paywall)
                    s.widgetId != -1 -> {
                        Haptics.success(ctx)
                        Prefs.saveWidgetConfig(s.widgetId, cfg)
                        WidgetUpdater.update(ctx, s.widgetId)
                        if (cfg.cities.isNotEmpty()) Scheduler.refreshNow(ctx)
                        if (s.configure) (ctx as? MainActivity)?.finishConfigure(s.widgetId)
                        else { Toast.makeText(ctx, tr("Saved ✓", "تم الحفظ ✓"), Toast.LENGTH_SHORT).show(); if (!nav.back()) (ctx as? Activity)?.finish() }
                    }
                    else -> { Haptics.success(ctx); pin(ctx, cfg, style.shape) }
                }
            }
        }
    }

    if (colorTarget != -1) {
        val init = when (colorTarget) { 1 -> cfg.color1; 2 -> cfg.color2; 3 -> cfg.accent; 4 -> cfg.ornColor; 5 -> cfg.accent2; 6 -> cfg.dayColor; 7 -> cfg.hijriColor
            8 -> cfg.nameColor; 9 -> cfg.timeColor; 12 -> cfg.nextColor; 13 -> cfg.lineColor; 14 -> cfg.hourColor2; 15 -> cfg.dayColor2; 10 -> cfg.bgColor; else -> cfg.bgColor2 }
        ColorPickerDialog(init, { colorTarget = -1 }) { c ->
            cfg = when (colorTarget) {
                1 -> cfg.copy(color1 = c); 2 -> cfg.copy(color2 = c); 3 -> cfg.copy(accent = c); 4 -> cfg.copy(ornColor = c)
                5 -> cfg.copy(accent2 = c); 6 -> cfg.copy(dayColor = c); 7 -> cfg.copy(hijriColor = c)
                8 -> cfg.copy(nameColor = c); 9 -> cfg.copy(timeColor = c); 12 -> cfg.copy(nextColor = c); 13 -> cfg.copy(lineColor = c)
                14 -> cfg.copy(hourColor2 = c); 15 -> cfg.copy(dayColor2 = c)
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
    val ok = runCatching { mgr.requestPinAppWidget(ComponentName(ctx, WidgetUpdater.providerFor(shape)), null, cb) }.getOrDefault(false)
    if (!ok) {
        Prefs.setPending("manual", cfg)
        Toast.makeText(ctx, tr("Long-press the home screen → Widgets → Zujaj to place it.", "اضغط مطولاً على الشاشة ← الأدوات ← زجاج عشان تحطها."), Toast.LENGTH_LONG).show()
    }
}

@Suppress("unused")
private val w = Modifier.width(1.dp)


/** Free users: watch N ads to open this Pro widget for a few days; shows the progress or the time left. */
@Composable
private fun AdUnlockCard(id: String, locked: Boolean, ver: Int) {
    val ctx = LocalContext.current
    val U = com.smrh.zujaj.ads.AdUnlock
    val styleIsPro = remember(id) { com.smrh.zujaj.catalog.Catalog.byId(id)?.let { Flags.isPro(it) } ?: false }
    if (!styleIsPro || com.smrh.zujaj.data.Prefs.isPro || !U.enabled()) return
    val ready by com.smrh.zujaj.ads.Ads.ready.collectAsState()
    val ar = com.smrh.zujaj.data.Prefs.isArabicUi
    val shape = RoundedCornerShape(20.dp)
    if (!locked) {
        // already opened with ads: how long is left
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape).background(Color(0xFF3DDC97).copy(alpha = 0.14f))
            .border(1.dp, Color(0xFF3DDC97).copy(alpha = 0.4f), shape).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LockOpen, null, tint = Color(0xFF3DDC97), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(tr("Unlocked · ${U.leftText(id, false)} left", "مفتوحة · فاضل ${U.leftText(id, true)}"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        return
    }
    val need = U.needed(); val done = remember(ver) { U.watched(id) }
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "u").animateFloat(0.55f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "p")
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xFF7B5CD6).copy(alpha = 0.35f), Color(0xFFE7B75F).copy(alpha = 0.22f))))
        .border(1.dp, Z.tint(0.12f), shape)
        .clickable {
            val a = ctx as? Activity ?: return@clickable
            val shown = com.smrh.zujaj.ads.Ads.showReward(a, quota = false) {
                val opened = U.add(ctx, id)
                Toast.makeText(ctx, if (opened) tr("Unlocked for ${U.days()} days 🎉", "اتفتحت لمدة ${U.days()} أيام 🎉")
                    else tr("${U.watched(id)} of $need — keep going!", "${U.watched(id)} من $need — كمّل!"), Toast.LENGTH_SHORT).show()
            }
            if (!shown) Toast.makeText(ctx, tr("The ad is still loading — try again in a few seconds", "الإعلان لسه بيحمّل، جرب كمان ثواني"), Toast.LENGTH_SHORT).show()
        }.padding(horizontal = 14.dp, vertical = 11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PlayCircle, null, tint = Color.White.copy(alpha = if (ready) pulse else 0.5f), modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Watch $need ads · free for ${U.days()} days", "شوف $need إعلانات وافتحها ${U.days()} أيام ببلاش"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("Opens this widget only", "بتفتح الويدجت دي بس"), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            if (!ready) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(need) { i ->
                val on = i < done
                val f by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, label = "d$i")
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(Z.tint(0.18f))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(Color.White))
                }
            }
        }
    }
}
