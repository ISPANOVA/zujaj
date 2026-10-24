package com.smrh.zujaj.ui

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.BuildConfig
import com.smrh.zujaj.billing.Billing
import com.smrh.zujaj.billing.Plan
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.MediaRepo
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.CatalogFlags
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.widget.Scheduler
import com.smrh.zujaj.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// =============================================================================== MY WIDGETS
@Composable
fun MyWidgetsTab(nav: Nav) {
    val ctx = LocalContext.current
    val ver by Prefs.version.collectAsState()
    val ids = remember(ver) { WidgetUpdater.allIds(ctx) }
    if (ids.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(tr("No widgets on your home screen yet", "لا توجد ويدجت على شاشتك بعد"), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(tr("Pick any widget, customise it and tap Apply.", "اختر أي ويدجت وخصّصه ثم اضغط إضافة."), color = Z.sub, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            PrimaryButton(tr("How to add widgets", "طريقة إضافة الويدجت")) { nav.go(Screen.Tutorial) }
        }
        return
    }
    LazyColumn(contentPadding = pagePadding, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(tr("Widgets on your home screen", "الويدجت على شاشتك"), color = Z.sub, modifier = Modifier.padding(horizontal = 8.dp)) }
        items(ids) { id ->
            val cfg = Prefs.widgetConfig(id) ?: return@items
            val st = Catalog.byId(cfg.styleId) ?: return@items
            Glass(Modifier.fillMaxWidth(), radius = 22.dp) {
                Box(Modifier.fillMaxWidth().padding(14.dp), contentAlignment = Alignment.Center) {
                    WidgetPreview(cfg, if (st.shape == com.smrh.zujaj.catalog.Shape.SMALL) Modifier.width(150.dp) else Modifier.fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(st.name(Prefs.isArabicUi), modifier = Modifier.weight(1f))
                    TextButton(onClick = { nav.go(Screen.Editor(st.id, id)) }) {
                        Icon(Icons.Rounded.Edit, null, tint = LocalAccent.current, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text(tr("Edit", "تعديل"), color = LocalAccent.current)
                    }
                }
            }
        }
    }
}

// =============================================================================== SETTINGS
@SuppressLint("MissingPermission")
private suspend fun locate(ctx: Context): Boolean = withContext(Dispatchers.IO) {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val loc = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time } ?: return@withContext false
    Prefs.lat = loc.latitude; Prefs.lng = loc.longitude; Prefs.hasLocation = true
    @Suppress("DEPRECATION")
    val name = runCatching { Geocoder(ctx).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()?.let { it.locality ?: it.subAdminArea ?: it.adminArea } }.getOrNull()
    Prefs.cityName = name ?: ""
    Scheduler.refreshWeatherIfNeeded(ctx, force = true)
    WidgetUpdater.updateAll(ctx)
    true
}

@Composable
fun SettingsScreen(nav: Nav, asTab: Boolean = false) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCity by remember { mutableStateOf(false) }
    var taps by remember { mutableIntStateOf(0) }
    var showGate by remember { mutableStateOf(false) }
    var showRedeem by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    if (showImport) {
        var t by remember { mutableStateOf((ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager)?.primaryClip?.getItemAt(0)?.text?.toString()?.takeIf { ShareDesign.codeFrom(it) != null } ?: "") }
        ZDialog(androidx.compose.material.icons.Icons.Rounded.Link, tr("Open a shared design", "افتح تصميم اتبعتلك"), tr("Paste the link someone sent you", "الصق اللينك اللي اتبعتلك"), onDismiss = { showImport = false }) {
            var bad by remember { mutableStateOf(false) }
            ZField(t, { t = it; bad = false }, "zujaj.pages.dev/w#…", androidx.compose.material.icons.Icons.Rounded.Link, error = bad)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(tr("Open", "افتح"), Modifier.fillMaxWidth()) {
                val code = ShareDesign.codeFrom(t); val cfg = code?.let { ShareDesign.decode(it) }
                if (cfg == null) bad = true
                else { showImport = false; Haptics.success(ctx); nav.go(Screen.Editor(cfg.styleId, shared = code)) }
            }
            androidx.compose.material3.TextButton(onClick = { showImport = false }) { Text(tr("Cancel", "إلغاء"), color = Z.sub) }
        }
    }
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) scope.launch {
            val ok = locate(ctx)
            Toast.makeText(ctx, if (ok) tr("Location updated ✓", "تم تحديث الموقع ✓") else tr("Turn on location and try again", "شغّل الموقع وحاول مرة أخرى"), Toast.LENGTH_SHORT).show()
        }
    }
    val purple = Color(0xFF8B7CFF); val teal = Color(0xFF13B5A8); val gold = Color(0xFFE0A93B); val blue = Color(0xFF4D8DFF)
    val green = Color(0xFF34C77B); val rose = Color(0xFFFF5C8A); val orange = Color(0xFFFF8A4C); val grey = Color(0xFF7A7A86)
    Column(Modifier.fillMaxSize()) {
        if (!asTab) TopBar("", { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(tr("Settings", "الإعدادات"), fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = if (asTab) 18.dp else 0.dp, bottom = 12.dp))
            // ---- Pro card
            val isPro = Prefs.isPro
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(if (isPro) listOf(Color(0xFF3A2C12), Color(0xFF17130B)) else listOf(Color(0xFFF6D98B), Color(0xFFD6A445), Color(0xFFB07A2A))))
                .border(1.dp, Z.tint(0.16f), RoundedCornerShape(26.dp))
                .clickable { nav.go(Screen.Paywall) }.padding(18.dp)) {
                val fg = if (isPro) Z.pro else Color(0xFF2A1C05)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(fg.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = fg, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (isPro) tr("You are Pro ✓", "أنت مشترك برو ✓") else tr("Zujaj Pro", "زجاج برو"), color = fg, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(if (isPro) tr("Every widget is yours — thank you!", "كل الويدجت بتاعتك — شكراً ليك!") else tr("All widgets, no ads, every new design", "كل الويدجت، من غير إعلانات، وكل تصميم جديد"),
                            color = fg.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = fg)
                }
            }
            Section(tr("Appearance", "المظهر")) {
                ChoiceRow(tr("Language", "اللغة"), Prefs.appLanguage, listOf("system" to tr("System", "النظام"), "en" to "English", "ar" to "العربية"), Icons.Rounded.Language, blue) {
                    Prefs.appLanguage = it; WidgetUpdater.updateAll(ctx)
                }
                Divider()
                ChoiceRow(tr("Look", "الوضع"), Z.mode(), listOf(1 to tr("Dark", "غامق"), 2 to tr("Light", "فاتح"), 0 to tr("Like the phone", "زي التليفون")), Icons.Rounded.DarkMode, purple) {
                    Prefs.setString("theme_mode", it.toString()); Z.refresh(ctx)
                }
                Divider()
                SwitchRow(tr("Vibrate on touch", "اهتزاز خفيف مع اللمس"), Prefs.getString("haptics", "1") == "1", icon = Icons.Rounded.Vibration, iconColor = orange) {
                    Prefs.setString("haptics", if (it) "1" else "0")
                }
                Divider()
                RowBase(tr("App colour", "لون التطبيق"), icon = Icons.Rounded.Palette, iconColor = rose) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Z.accents.forEachIndexed { i, a ->
                            Box(Modifier.size(22.dp).clip(CircleShape).background(a.color)
                                .border(2.dp, if (i == (if (Z.accentIdx >= 0) Z.accentIdx else Prefs.accentIndex)) Color.White else Color.Transparent, CircleShape)
                                .clickable { Z.pickAccent(i) })
                        }
                    }
                }
                Divider()
                RowBase(tr("Widget themes", "ثيمات الويدجت"), sub = tr("One look for all your widgets", "شكل واحد لكل الويدجت"), onClick = { showThemes = true }, icon = Icons.Rounded.AutoAwesome, iconColor = purple) { Chevron() }
            }
            Section(tr("Location", "الموقع")) {
                RowBase(tr("Use my location", "استخدم موقعي"), sub = Prefs.cityName.ifBlank { if (Prefs.hasLocation) "%.2f, %.2f".format(Prefs.lat, Prefs.lng) else tr("Not set (Cairo)", "غير محدد (القاهرة)") },
                    onClick = { perm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }, icon = Icons.Rounded.MyLocation, iconColor = teal) {
                    Text(tr("Detect", "تحديد"), color = LocalAccent.current, fontWeight = FontWeight.SemiBold)
                }
                Divider()
                RowBase(tr("Choose city", "اختر مدينة"), onClick = { showCity = true }, icon = Icons.Rounded.LocationCity, iconColor = blue) { Chevron() }
            }
            Section(tr("Prayer times", "مواقيت الصلاة")) {
                ChoiceRow(tr("Method", "طريقة الحساب"), Prefs.prayerMethod, listOf(
                    "EGYPTIAN" to tr("Egyptian Authority", "الهيئة المصرية"),
                    "UMM_AL_QURA" to tr("Umm al-Qura", "أم القرى"),
                    "MUSLIM_WORLD_LEAGUE" to tr("Muslim World League", "رابطة العالم الإسلامي"),
                    "KARACHI" to tr("Karachi", "كراتشي"),
                    "DUBAI" to tr("Dubai", "دبي"),
                    "KUWAIT" to tr("Kuwait", "الكويت"),
                    "QATAR" to tr("Qatar", "قطر"),
                    "NORTH_AMERICA" to tr("ISNA", "أمريكا الشمالية"),
                    "MOON_SIGHTING_COMMITTEE" to tr("Moonsighting", "لجنة رؤية الهلال"),
                    "SINGAPORE" to tr("Singapore", "سنغافورة"),
                ), Icons.Rounded.Explore, green) { Prefs.prayerMethod = it; WidgetUpdater.updateAll(ctx); Scheduler.scheduleAlarms(ctx) }
                Divider()
                SwitchRow(tr("Hanafi Asr", "العصر حنفي"), Prefs.hanafi, icon = Icons.Rounded.WbTwilight, iconColor = orange) { Prefs.hanafi = it; WidgetUpdater.updateAll(ctx) }
                Divider()
                StepperRow(tr("Hijri adjustment", "تعديل الهجري"), Prefs.hijriOffset, -2..2, 1, icon = Icons.Rounded.DarkMode, iconColor = purple) { Prefs.hijriOffset = it; WidgetUpdater.updateAll(ctx) }
            }
            Section(tr("Widgets", "الويدجت")) {
                val exact = com.smrh.zujaj.widget.Scheduler.exactAllowed(ctx).also { touch(ver) }
                RowBase(tr("Exact clock time", "الساعة على الثانية"), sub = if (exact) tr("On ✓ — clocks change right on the minute", "شغالة ✓ — الساعة بتتغير مع التليفون بالظبط")
                    else tr("Tap to allow, so clocks never lag behind the phone", "اضغط واسمح عشان الساعة متتأخرش عن التليفون"),
                    onClick = { if (!exact) com.smrh.zujaj.widget.Scheduler.askExact(ctx) }, icon = Icons.Rounded.Schedule, iconColor = blue) { if (!exact) Chevron() }
                Divider()
                RowBase(tr("Open a shared design", "افتح تصميم اتبعتلك"), sub = tr("Paste the link someone sent you", "الصق اللينك اللي اتبعتلك"), onClick = { showImport = true }, icon = Icons.Rounded.Link, iconColor = blue) { Chevron() }
                Divider()
                SwitchRow(tr("Fahrenheit", "فهرنهايت"), Prefs.fahrenheit, icon = Icons.Rounded.Thermostat, iconColor = orange) { Prefs.fahrenheit = it; WidgetUpdater.updateAll(ctx) }
                Divider()
                RowBase(tr("Music access", "الوصول للميديا"), sub = if (MediaRepo.hasAccess(ctx)) tr("Enabled ✓", "مفعّل ✓") else tr("Needed for media widgets", "مطلوب لويدجت الميديا"),
                    onClick = { nav.go(Screen.MediaAccess) }, icon = Icons.Rounded.MusicNote, iconColor = rose) { Chevron() }
                Divider()
                RowBase(tr("Refresh all widgets", "تحديث كل الويدجت"), onClick = {
                    scope.launch(Dispatchers.IO) { Scheduler.refreshWeatherIfNeeded(ctx, true); WidgetUpdater.updateAll(ctx) }
                    Toast.makeText(ctx, tr("Refreshing…", "جارٍ التحديث…"), Toast.LENGTH_SHORT).show()
                }, icon = Icons.Rounded.Refresh, iconColor = teal) {}
                Divider()
                RowBase(tr("Battery optimisation", "تحسين البطارية"), sub = tr("Allow Zujaj to run so widgets stay fresh", "اسمح للتطبيق بالعمل لتبقى الويدجت محدّثة"), onClick = {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }, icon = Icons.Rounded.BatteryChargingFull, iconColor = green) { Chevron() }
                if (com.smrh.zujaj.widget.Oem.autoStartIntent(ctx) != null) {
                    Divider()
                    RowBase(tr("Auto-start (${com.smrh.zujaj.widget.Oem.brand})", "التشغيل التلقائي (${com.smrh.zujaj.widget.Oem.brand})"),
                        sub = tr("Turn Zujaj on so widgets never freeze", "فعّل زجاج عشان الويدجت متقفش أبداً"),
                        onClick = { com.smrh.zujaj.widget.Oem.openAutoStart(ctx) }, icon = Icons.Rounded.PowerSettingsNew, iconColor = orange) { Chevron() }
                }
                Divider()
                RowBase(tr("How to add widgets", "طريقة إضافة الويدجت"), onClick = { nav.go(Screen.Tutorial) }, icon = Icons.Rounded.HelpOutline, iconColor = blue) { Chevron() }
            }
            com.smrh.zujaj.ads.AdBanner(1, Modifier.padding(top = 10.dp))
            Section(tr("Subscription", "الاشتراك")) {
                RowBase(tr("Redeem code", "استخدام كود"), sub = codeStatus(), onClick = { showRedeem = true }, icon = Icons.Rounded.Redeem, iconColor = gold) {
                    Text(tr("Enter", "إدخال"), color = LocalAccent.current, fontWeight = FontWeight.SemiBold)
                }
                Divider()
                RowBase(tr("Restore purchases", "استعادة المشتريات"), onClick = { Billing.restore(); Toast.makeText(ctx, tr("Checking…", "جارٍ التحقق…"), Toast.LENGTH_SHORT).show() },
                    icon = Icons.Rounded.Restore, iconColor = teal) {}
                Divider()
                RowBase(tr("Manage subscription", "إدارة الاشتراك"), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}"))) }
                }, icon = Icons.Rounded.CreditCard, iconColor = blue) { Chevron() }
            }
            Section(tr("About", "حول")) {
                RowBase(tr("Rate Zujaj", "قيّم التطبيق"), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))) }
                }, icon = Icons.Rounded.Star, iconColor = gold) { Chevron() }
                Divider()
                RowBase(tr("Share the app", "شارك التطبيق"), onClick = { ctx.startActivity(Intent.createChooser(WidgetUpdater.shareIntent(ctx), null)) },
                    icon = Icons.Rounded.Share, iconColor = green) { Chevron() }
                Divider()
                RowBase(tr("Privacy & credits", "الخصوصية والحقوق"), onClick = { nav.go(Screen.About) }, icon = Icons.Rounded.Shield, iconColor = grey) { Chevron() }
                Divider()
                RowBase(tr("Version", "الإصدار"), sub = BuildConfig.VERSION_NAME + " · by SMRH", onClick = {
                    taps++
                    if (taps >= 7) { taps = 0; showGate = true }
                }, icon = Icons.Rounded.Info, iconColor = grey) {}
            }
            Spacer(Modifier.height(130.dp))
        }
    }
    if (showThemes) ThemesDialog { showThemes = false }
    if (showGate) AdminGateDialog({ showGate = false }) { showGate = false; nav.go(Screen.Admin) }
    if (showRedeem) RedeemDialog { showRedeem = false }
    if (showCity) CityPickerDialog({ showCity = false }) { c ->
        Prefs.lat = c.lat; Prefs.lng = c.lng; Prefs.cityName = c.name; Prefs.hasLocation = true
        scope.launch(Dispatchers.IO) { Scheduler.refreshWeatherIfNeeded(ctx, true); WidgetUpdater.updateAll(ctx) }
    }
}

// =============================================================================== PAYWALL
@Composable
fun PaywallScreen(nav: Nav) {
    val ctx = LocalContext.current
    val prices by Billing.prices.collectAsState()
    val pro by Billing.pro.collectAsState()
    val msg by Billing.message.collectAsState()
    var sel by remember { mutableStateOf(Plan.YEARLY) }
    var showRedeem by remember { mutableStateOf(false) }
    if (showRedeem) RedeemDialog { showRedeem = false; if (com.smrh.zujaj.data.Prefs.isPro) nav.back() }
    LaunchedEffect(msg) {
        when (msg) {
            "pro_ok" -> { Toast.makeText(ctx, tr("Welcome to Pro ✓", "أهلاً بك في برو ✓"), Toast.LENGTH_LONG).show(); nav.back() }
            "pending" -> Toast.makeText(ctx, tr("Payment pending…", "الدفع قيد المعالجة…"), Toast.LENGTH_LONG).show()
            "error" -> Toast.makeText(ctx, tr("Purchase failed", "فشل الشراء"), Toast.LENGTH_LONG).show()
        }
        if (msg != null) Billing.consumeMessage()
    }
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize()) {
        // a quiet gold wash behind the header — the only decoration on the page
        Box(Modifier.fillMaxWidth().height(320.dp).background(Brush.verticalGradient(listOf(Z.pro.copy(alpha = 0.16f), Color.Transparent))))
        Column(Modifier.fillMaxSize()) {
            TopBar("", { nav.back() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                // ---- header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFFF6D98B), Color(0xFFC8902E)))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF2A1C05), modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(tr("Zujaj Pro", "زجاج برو"), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Z.text)
                        Text(tr("Every widget, every look, no ads", "كل الويدجت، كل الأشكال، من غير إعلانات"), color = Z.sub, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                // ---- what you get: compact two-column list (four columns on wide screens)
                val benefits = listOf(
                    Triple(Icons.Rounded.Widgets, tr("All ${Catalog.all.size}+ widgets", "كل الويدجت (${Catalog.all.size}+)"), tr("Clocks, prayer, media, calendar", "ساعات، صلاة، ميديا، تقويم")),
                    Triple(Icons.Rounded.AutoAwesome, tr("Premium looks", "أشكال مميزة"), tr("Glass, aurora & textures", "زجاج، شفق وملمس")),
                    Triple(Icons.Rounded.Block, tr("No ads", "من غير إعلانات"), tr("Clean, uninterrupted app", "تطبيق نضيف من غير مقاطعة")),
                    Triple(Icons.Rounded.NewReleases, tr("New every month", "جديد كل شهر"), tr("Future drops included", "الإضافات الجاية معاك")),
                )
                val wide = LocalConfiguration.current.screenWidthDp >= 600
                benefits.chunked(if (wide) 4 else 2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (icon, t, sub) ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Z.tint(0.05f)).border(1.dp, Z.line, RoundedCornerShape(18.dp)).padding(14.dp)) {
                                Icon(icon, null, tint = Z.pro, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.height(10.dp))
                                Text(t, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Z.text)
                                Text(sub, color = Z.sub, fontSize = 12.sp, lineHeight = 16.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                if (pro) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Z.pro.copy(alpha = 0.12f)).border(1.dp, Z.pro.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = Z.pro, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(12.dp))
                        Column {
                            Text(tr("You have Pro", "انت مشترك في برو"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(tr("Manage or cancel any time in Google Play.", "تقدر تدير أو تلغي الاشتراك من جوجل بلاي في أي وقت."), color = Z.sub, fontSize = 13.sp)
                        }
                    }
                } else {
                    Text(tr("Choose a plan", "اختار الخطة"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Z.text, modifier = Modifier.padding(bottom = 8.dp))
                    // plan cards: side by side on wide screens, stacked on phones
                    @Composable
                    fun PlanCard(plan: Plan, mod: Modifier) {
                        val p = prices[plan]
                        val (title, sub) = when (plan) {
                            Plan.MONTHLY -> tr("Monthly", "شهري") to tr("Renews every month", "بيتجدد كل شهر")
                            Plan.YEARLY -> tr("Yearly", "سنوي") to (if (p?.trial == true) tr("Free trial, then yearly", "تجربة مجانية ثم سنوي") else tr("Renews every year", "بيتجدد كل سنة"))
                            Plan.LIFETIME -> tr("Lifetime", "مدى الحياة") to tr("One payment, yours forever", "تدفع مرة واحدة وتبقى ليك")
                        }
                        val on = sel == plan
                        val ring by androidx.compose.animation.animateColorAsState(if (on) accent else Z.line, label = "ring")
                        Box(mod) {
                            Row(Modifier.fillMaxWidth().padding(top = if (plan == Plan.YEARLY) 10.dp else 0.dp).clip(RoundedCornerShape(20.dp))
                                .background(if (on) accent.copy(alpha = 0.10f) else Z.tint(0.04f))
                                .border(if (on) 2.dp else 1.dp, ring, RoundedCornerShape(20.dp))
                                .clickable { Haptics.tick(ctx); sel = plan }.padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                // radio
                                Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) accent else Z.sub, CircleShape), contentAlignment = Alignment.Center) {
                                    androidx.compose.animation.AnimatedVisibility(on, enter = androidx.compose.animation.scaleIn(), exit = androidx.compose.animation.scaleOut()) {
                                        Box(Modifier.size(11.dp).clip(CircleShape).background(accent))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Z.text)
                                    Text(sub, color = Z.sub, fontSize = 12.sp)
                                }
                                Text(p?.price ?: "—", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Z.text)
                            }
                            if (plan == Plan.YEARLY) Text(if (p?.trial == true) tr("FREE TRIAL", "تجربة مجانية") else tr("MOST POPULAR", "الأكثر اختيارًا"),
                                color = Color(0xFF2A1C05), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp,
                                modifier = Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(50)).background(Z.pro).padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                    if (wide) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Plan.values().forEach { PlanCard(it, Modifier.weight(1f)) } }
                    else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { listOf(Plan.YEARLY, Plan.MONTHLY, Plan.LIFETIME).forEach { PlanCard(it, Modifier.fillMaxWidth()) } }
                }
                Spacer(Modifier.height(18.dp))
            }
            // ---- sticky actions + disclosure
            Column(Modifier.fillMaxWidth().background(Z.bg.copy(alpha = 0.96f)).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (!pro) PrimaryButton(tr("Continue", "متابعة"), Modifier.fillMaxWidth()) {
                    val ok = Billing.buy(ctx as Activity, sel)
                    if (!ok) Toast.makeText(ctx, tr("Store not available yet — try again shortly", "المتجر غير متاح حالياً — حاول بعد قليل"), Toast.LENGTH_LONG).show()
                }
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { Billing.restore(); Toast.makeText(ctx, tr("Checking your purchases…", "بنراجع مشترياتك…"), Toast.LENGTH_SHORT).show() }) { Text(tr("Restore purchases", "استرجاع المشتريات"), color = Z.sub) }
                    TextButton(onClick = { showRedeem = true }) { Text(tr("Have a code?", "معاك كود؟"), color = accent) }
                }
                Text(tr("Subscriptions renew automatically until cancelled in Google Play. Cancel anytime.", "الاشتراكات بتتجدد لوحدها لحد ما تلغيها من جوجل بلاي. تقدر تلغي في أي وقت."),
                    color = Z.sub, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

// =============================================================================== TUTORIAL / MEDIA / ABOUT
@Composable
fun TutorialScreen(nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Quick start", "البدء السريع"), { nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            val steps = listOf(
                tr("Pick a widget", "اختر ويدجت") to tr("Browse the Widgets tab and tap any design you like.", "تصفح قسم الويدجت واضغط على أي تصميم يعجبك."),
                tr("Customise it", "خصّصه") to tr("Change colours, background (solid, gradient, glass, aurora), texture, sizes, language and numerals.", "غيّر الألوان والخلفية (سادة، متدرج، زجاجي، شفق) والملمس والأحجام واللغة والأرقام."),
                tr("Tap Apply", "اضغط إضافة") to tr("Your launcher asks where to place it — confirm and it appears on your home screen.", "سيسألك المشغل عن مكان الويدجت — أكّد وستظهر على شاشتك."),
                tr("Or add from the launcher", "أو من المشغل") to tr("Long-press the home screen → Widgets → Zujaj → choose a size, then pick a design.", "اضغط مطولاً على الشاشة ← الأدوات ← زجاج ← اختر الحجم ثم التصميم."),
                tr("Edit anytime", "عدّل في أي وقت") to tr("Open the Mine tab, or long-press a widget → Settings/Edit (Android 12+).", "افتح قسم ويدجتاتي، أو اضغط مطولاً على الويدجت ← إعدادات (أندرويد ١٢+)."),
                tr("Lock screen", "شاشة القفل") to tr("Phones with lock-screen widget support (e.g. Samsung One UI 7+, Android 16 hub) list Zujaj widgets there too. ", "الهواتف التي تدعم ويدجت شاشة القفل (مثل سامسونج One UI 7+ وأندرويد 16) تعرض ويدجت زجاج هناك أيضاً."),
                tr("Media widgets", "ويدجت الميديا") to tr("Allow music access once so widgets show what's playing in any app.", "اسمح بالوصول للميديا مرة واحدة لتعرض الويدجت ما يُشغَّل في أي تطبيق."),
                tr("Keep widgets fresh", "حافظ على التحديث") to tr("If your phone kills background apps, allow Zujaj in battery settings.", "إذا كان هاتفك يغلق التطبيقات في الخلفية، اسمح لزجاج في إعدادات البطارية."),
            )
            steps.forEachIndexed { i, (t, d) ->
                Glass(Modifier.fillMaxWidth().padding(vertical = 6.dp), radius = 20.dp) {
                    Row(Modifier.padding(16.dp)) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(LocalAccent.current), contentAlignment = Alignment.Center) { Text("${i + 1}", color = Color.Black, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(12.dp))
                        Column { Text(t, style = MaterialTheme.typography.titleMedium); Text(d, color = Z.sub) }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun MediaAccessScreen(nav: Nav) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TopBar(tr("Music access", "الوصول للميديا"), { nav.back() })
        Spacer(Modifier.height(20.dp))
        Text(tr("Media widgets need permission to see what's playing.", "ويدجت الميديا تحتاج إذناً لمعرفة ما يتم تشغيله."), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Text(tr("Zujaj only reads the current song title, artist, artwork and play state. It never reads your messages or other notifications' content and nothing leaves your phone.",
            "زجاج يقرأ فقط اسم الأغنية والفنان والصورة وحالة التشغيل. لا يقرأ رسائلك أو محتوى الإشعارات الأخرى ولا يُرسل أي شيء خارج هاتفك."), color = Z.sub)
        Spacer(Modifier.height(20.dp))
        if (MediaRepo.hasAccess(ctx)) Text(tr("Access is enabled ✓", "الوصول مفعّل ✓"), color = LocalAccent.current)
        else PrimaryButton(tr("Open settings", "افتح الإعدادات")) {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        }
    }
}

@Composable
fun AboutScreen(nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Privacy & credits", "الخصوصية والحقوق"), { nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(tr("Privacy", "الخصوصية"), style = MaterialTheme.typography.titleMedium)
            Text(tr(
                "Zujaj has no accounts. Your widget settings, photos and contacts you choose stay on your phone. Your approximate location is sent only to the weather service (MET Norway) to get the forecast.\n\nADS\nZujaj contains ads. The free version shows ads from Google AdMob. AdMob may use your device's advertising ID and technical information to show and measure ads; where the law requires it (e.g. the EU/UK) we ask for your consent first. You can reset your advertising ID or turn off personalised ads in your phone's Google settings. Subscribers see no ads.\n\nPurchases are handled by Google Play. The app is intended for users aged 13 and over.",
                "زجاج من غير حسابات. إعدادات الويدجت والصور وجهات الاتصال اللي بتختارها بتفضل على تليفونك. موقعك التقريبي بيتبعت بس لخدمة الطقس (MET Norway) عشان تجيب التوقعات.\n\nالإعلانات\nالتطبيق فيه إعلانات. النسخة المجانية فيها إعلانات من Google AdMob. AdMob ممكن يستخدم معرّف الإعلانات بتاع جهازك ومعلومات تقنية عشان يعرض الإعلانات ويقيس أداءها، وفي الدول اللي القانون فيها بيطلب ده (زي أوروبا) بنطلب موافقتك الأول. تقدر تعيد ضبط معرّف الإعلانات أو توقف الإعلانات المخصصة من إعدادات جوجل في تليفونك. المشتركين مش بيشوفوا إعلانات.\n\nالمشتريات بتتم عن طريق جوجل بلاي. التطبيق موجّه لمن هم أكبر من 13 سنة."), color = Z.sub)
            Spacer(Modifier.height(10.dp))
            val uri = androidx.compose.ui.platform.LocalUriHandler.current
            Text(tr("Read the full privacy policy", "اقرأ سياسة الخصوصية كاملة"), color = LocalAccent.current, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                modifier = Modifier.clickable { runCatching { uri.openUri(if (Prefs.isArabicUi) "https://zujaj.pages.dev/privacy.html" else "https://zujaj.pages.dev/privacy-en.html") } }.padding(vertical = 6.dp))
            Spacer(Modifier.height(18.dp))
            Text(tr("Credits", "الحقوق"), style = MaterialTheme.typography.titleMedium)
            Text("• Weather data: MET Norway (api.met.no), CC BY 4.0\n• Prayer times: Adhan library (MIT)\n• Fonts: Manrope, Cairo, Playfair Display, Bodoni Moda, Big Shoulders, Oswald, Cormorant, Amiri, Aref Ruqaa, Reem Kufi, Rakkas — SIL Open Font License\n• Designed & built by SMRH", color = Z.sub)
        }
    }
}


// =============================================================================== ADMIN GATE
/** Holds the admin credentials for this app session only (never written to disk). */
object AdminSession {
    private const val PASS_HASH = "0db2cde54e86a0e70d3095763b58310744658fc5643843638f4e9f3ac74848a3"
    private const val EMAIL_HASH = "66b4246fab51306dc69ca5c7ffe3ca1e05123352709401cb5118208380223ea7"
    var unlocked = false
        private set
    var email = ""
        private set
    /** The gate password (used once to move the Firebase account onto the secret key). */
    var gatePass = ""
        private set
    /** Secret Firebase admin key handed out by the email-code service. */
    var pass = ""
        private set

    fun sha(s: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    fun checkPass(p: String) = sha(p) == PASS_HASH
    fun isAdminEmail(e: String?) = e != null && sha(e.trim().lowercase()) == EMAIL_HASH
    fun unlock(p: String, e: String, key: String) { gatePass = p; email = e.trim().lowercase(); pass = key; unlocked = true }

    // ---------------------------------------------------------- remember this device
    private const val TRUST_DAYS = 180L
    private const val KS_ALIAS = "zujaj_admin_trust"
    private fun aesKey(): javax.crypto.SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KS_ALIAS, null) as? javax.crypto.SecretKey)?.let { return it }
        val g = javax.crypto.KeyGenerator.getInstance(android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(android.security.keystore.KeyGenParameterSpec.Builder(KS_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return g.generateKey()
    }

    /** Stores the unlocked session encrypted with a key that never leaves this phone's keystore. */
    fun rememberDevice() = runCatching {
        val until = System.currentTimeMillis() + TRUST_DAYS * 86_400_000L
        val plain = org.json.JSONObject().put("e", email).put("k", pass).put("g", gatePass).put("u", until).toString()
        val c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding").apply { init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey()) }
        val enc = c.iv + c.doFinal(plain.toByteArray())
        Prefs.setString("adm_trust", android.util.Base64.encodeToString(enc, android.util.Base64.NO_WRAP))
    }

    private fun trusted(): org.json.JSONObject? = runCatching {
        val raw = Prefs.getString("adm_trust"); if (raw.isEmpty()) return null
        val b = android.util.Base64.decode(raw, android.util.Base64.NO_WRAP)
        val c = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        c.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey(), javax.crypto.spec.GCMParameterSpec(128, b, 0, 12))
        org.json.JSONObject(String(c.doFinal(b, 12, b.size - 12))).takeIf { it.optLong("u") > System.currentTimeMillis() && isAdminEmail(it.optString("e")) }
    }.getOrNull()

    fun isTrusted() = trusted() != null

    /** On a remembered phone the password alone opens the panel. */
    fun unlockTrusted(p: String): Boolean {
        val t = trusted() ?: return false
        if (!checkPass(p)) return false
        unlock(t.optString("g").ifEmpty { p }, t.optString("e"), t.optString("k")); return true
    }

    fun forgetDevice() { Prefs.setString("adm_trust", ""); Prefs.setString("adm_save", "") }

    /** "Save password": a remembered phone opens the panel straight away, nothing to type. */
    fun savedPassword() = Prefs.getString("adm_save") == "1"
    fun autoUnlock(): Boolean {
        if (!savedPassword()) return false
        val t = trusted() ?: return false
        val p = t.optString("g")
        if (!checkPass(p)) return false
        unlock(p, t.optString("e"), t.optString("k")); return true
    }

    fun otpUrl(ctx: Context): String = ctx.getString(com.smrh.zujaj.R.string.admin_otp_url).trim()
        .ifEmpty { com.smrh.zujaj.remote.Flags.flags.value.otpUrl.trim() }

    /** POSTs to the one-time-code service; returns its JSON reply. */
    suspend fun call(ctx: Context, body: org.json.JSONObject): org.json.JSONObject = withContext(Dispatchers.IO) {
        val url = otpUrl(ctx)
        require(url.isNotEmpty()) { "OTP service not configured" }
        val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 30000
        c.instanceFollowRedirects = true
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        var conn = c
        // Apps Script answers POST with a redirect to the result; follow it with GET
        var hops = 0
        while (conn.responseCode in 300..399 && hops++ < 5) {
            val loc = conn.getHeaderField("Location") ?: break
            conn = (java.net.URL(loc).openConnection() as java.net.HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 30000 }
        }
        org.json.JSONObject(conn.inputStream.bufferedReader().readText())
    }
}

@Composable
fun AdminGateDialog(onDismiss: () -> Unit, onOk: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // saved on this phone: open at once
    val auto = remember { AdminSession.autoUnlock() }
    if (auto) { LaunchedEffect(Unit) { Haptics.success(ctx); onOk() }; return }
    var step by remember { mutableIntStateOf(0) }
    var pass by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val trusted = remember { AdminSession.isTrusted() }
    var keep by remember { mutableStateOf(true) }
    fun body(action: String) = org.json.JSONObject().put("action", action).put("pass", pass).put("email", email.trim().lowercase())
    fun done() {
        if (keep) { AdminSession.rememberDevice(); Prefs.setString("adm_save", "1") }
        Haptics.success(ctx); onOk()
    }
    fun sendCode() {
        busy = true; err = null
        scope.launch {
            val r = runCatching { AdminSession.call(ctx, body("send")) }
            busy = false
            val j = r.getOrNull()
            when {
                j?.optBoolean("ok") == true -> { step = 2; code = "" }
                j?.optString("err") == "wait" -> { step = 2; err = tr("A code was just sent — check your inbox", "الكود اتبعت من شوية — شوف الإيميل") }
                j?.optString("err") == "blocked" -> err = tr("Too many attempts. Try again in 15 minutes", "محاولات كتير. جرب بعد ١٥ دقيقة")
                j != null -> err = tr("Wrong", "غير صحيح")
                else -> err = tr("No connection", "مفيش اتصال") + (r.exceptionOrNull()?.message?.let { " ($it)" } ?: "")
            }
        }
    }
    fun next() {
        if (busy) return
        Haptics.click(ctx)
        when (step) {
            0 -> if (trusted && AdminSession.unlockTrusted(pass)) done()
                else if (AdminSession.checkPass(pass)) { step = 1; err = null } else err = tr("Wrong password", "كلمة المرور غلط")
            1 -> if (!AdminSession.isAdminEmail(email)) err = tr("This email isn't allowed", "الإيميل ده مش مسموح")
                else if (AdminSession.otpUrl(ctx).isEmpty()) { AdminSession.unlock(pass, email, pass); done() }
                else sendCode()
            else -> {
                if (code.length != 6) { err = tr("Enter the 6 digits", "اكتب الـ ٦ أرقام"); return }
                busy = true
                scope.launch {
                    val j = runCatching { AdminSession.call(ctx, body("verify").put("code", code)) }.getOrNull()
                    busy = false
                    if (j?.optBoolean("ok") == true && j.optString("key").isNotEmpty()) { AdminSession.unlock(pass, email, j.optString("key")); done() }
                    else err = when (j?.optString("err")) {
                        "expired" -> tr("Code expired — send a new one", "الكود انتهى — ابعت واحد جديد")
                        "wrong" -> tr("Wrong code", "الكود غلط")
                        null -> tr("No connection", "مفيش اتصال")
                        else -> tr("Wrong", "غير صحيح")
                    }
                }
            }
        }
    }
    val titles = listOf(tr("Control panel", "لوحة التحكم"), tr("Your email", "إيميلك"), tr("Check your email", "شوف إيميلك"))
    val subs = listOf(
        if (trusted) tr("Remembered phone — just the password", "تليفون محفوظ — كلمة المرور بس") else tr("Enter the password to continue", "اكتب كلمة المرور عشان تكمل"),
        tr("The admin email for this app", "إيميل الأدمن بتاع التطبيق"),
        tr("We sent you a 6-digit code", "بعتنالك كود من ٦ أرقام"),
    )
    ZDialog(androidx.compose.material.icons.Icons.Rounded.AdminPanelSettings, titles[step], subs[step], onDismiss = onDismiss) {
        // progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            repeat(if (AdminSession.otpUrl(ctx).isEmpty()) 2 else 3) { i ->
                val w by androidx.compose.animation.core.animateDpAsState(if (i == step) 22.dp else 7.dp, label = "d")
                Box(Modifier.height(7.dp).width(w).clip(RoundedCornerShape(50)).background(if (i <= step) LocalAccent.current else Z.tint(0.15f)))
            }
        }
        androidx.compose.animation.AnimatedContent(step, label = "step", transitionSpec = {
            (androidx.compose.animation.slideInHorizontally { it / 3 } + androidx.compose.animation.fadeIn()).togetherWith(androidx.compose.animation.slideOutHorizontally { -it / 3 } + androidx.compose.animation.fadeOut())
        }) { st ->
            Column {
                when (st) {
                    0 -> ZField(pass, { pass = it; err = null }, tr("Password", "كلمة المرور"), androidx.compose.material.icons.Icons.Rounded.Lock, password = true, error = err != null,
                        keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, imeAction = androidx.compose.ui.text.input.ImeAction.Done))
                    1 -> ZField(email, { email = it; err = null }, "name@gmail.com", androidx.compose.material.icons.Icons.Rounded.AlternateEmail, error = err != null,
                        keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email))
                    else -> {
                        ZField(code, { code = it.filter { c -> c.isDigit() }.take(6); err = null }, "••••••", error = err != null, center = true, big = true,
                            keyboard = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword))
                        Text(tr("Resend code", "ابعت الكود تاني"), color = LocalAccent.current, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).clip(RoundedCornerShape(50)).clickable { if (!busy) sendCode() }.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(err != null) {
            Text(err ?: "", color = Color(0xFFE5484D), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        }
        Spacer(Modifier.height(12.dp))
        // save password on this phone
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Z.tint(0.05f)).clickable { Haptics.tick(ctx); keep = !keep }.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(androidx.compose.material.icons.Icons.Rounded.Key, null, tint = Z.sub, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Save password on this phone", "احفظ كلمة المرور على التليفون ده"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(tr("Opens straight away next time", "المرة الجاية هتفتح على طول"), fontSize = 12.sp, color = Z.sub)
            }
            androidx.compose.material3.Switch(keep, { keep = it }, colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = LocalAccent.current, checkedThumbColor = Color.White))
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if (busy) "…" else if (step == 2) tr("Verify", "تأكيد") else tr("Continue", "متابعة"), Modifier.fillMaxWidth()) { next() }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "إلغاء"), color = Z.sub) }
            if (trusted && step == 0) TextButton(onClick = { AdminSession.forgetDevice(); onDismiss() }) { Text(tr("Forget this phone", "انسى التليفون ده"), color = Color(0xFFE5484D)) }
        }
    }
}

// =============================================================================== REDEEM
fun codeStatus(): String? {
    val u = Prefs.codeProUntil
    if (u <= 0) return null
    if (u == Long.MAX_VALUE) return tr("Pro forever ✓ (${Prefs.codeUsed})", "برو للأبد ✓ (${Prefs.codeUsed})")
    val d = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(u))
    return if (u > System.currentTimeMillis()) tr("Pro until $d ✓", "برو لحد $d ✓") else tr("Code ended $d", "الكود انتهى $d")
}

@Composable
fun RedeemDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    var grants by remember { mutableStateOf("pro") }
    // close by itself a moment after the celebration
    LaunchedEffect(success) { if (success != null) { kotlinx.coroutines.delay(2800); onDismiss() } }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier.clip(RoundedCornerShape(30.dp)).background(Z.surface).background(Brush.verticalGradient(listOf(Z.pro.copy(alpha = 0.16f), Color.Transparent), 0f, 500f)).border(1.dp, Z.line, RoundedCornerShape(30.dp)),
        ) {
            androidx.compose.animation.AnimatedContent(success, transitionSpec = {
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.9f))
                    .togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)))
            }, label = "redeem") { done ->
                if (done != null) ProUnlocked(done, grants) else Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Z.pro.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(tr("Redeem a code", "استخدام كود"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(tr("Enter the code you received to unlock Pro", "اكتب الكود اللي وصلك عشان تفتح برو"), color = Z.sub, textAlign = TextAlign.Center, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    ZField(code, { code = it.uppercase(); msg = null }, "XXXX-XXXX-XXXX", androidx.compose.material.icons.Icons.Rounded.ConfirmationNumber, error = msg != null, mono = true,
                        keyboard = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Characters))
                    msg?.let { Text(it, color = Color(0xFFE5484D), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp)) }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(if (busy) "…" else tr("Redeem", "تفعيل"), Modifier.fillMaxWidth()) {
                        if (busy || code.isBlank()) return@PrimaryButton
                        busy = true
                        scope.launch {
                            val r = com.smrh.zujaj.remote.Codes.redeem(ctx, code)
                            busy = false
                            when (r) {
                                is com.smrh.zujaj.remote.RedeemResult.Ok -> {
                                    val d = if (r.until == Long.MAX_VALUE) tr("Forever", "للأبد")
                                        else tr("Until ", "لحد ") + java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(r.until))
                                    WidgetUpdater.updateAll(ctx)
                                    grants = r.grants
                                    success = d
                                }
                                is com.smrh.zujaj.remote.RedeemResult.Fail -> msg = when (r.reason) {
                                    com.smrh.zujaj.remote.RedeemResult.Reason.INVALID -> tr("This code doesn't exist", "الكود ده مش موجود")
                                    com.smrh.zujaj.remote.RedeemResult.Reason.DISABLED -> tr("This code was turned off", "الكود ده اتوقف")
                                    com.smrh.zujaj.remote.RedeemResult.Reason.EXPIRED -> tr("This code has expired", "الكود ده انتهت صلاحيته")
                                    com.smrh.zujaj.remote.RedeemResult.Reason.FULL -> tr("This code reached its limit", "الكود ده اكتمل عدده")
                                    com.smrh.zujaj.remote.RedeemResult.Reason.ALREADY_USED_EXPIRED -> tr("You already used this code and its time ended", "انت استخدمت الكود ده قبل كده ومدته خلصت")
                                    com.smrh.zujaj.remote.RedeemResult.Reason.OFFLINE -> tr("Check your internet and try again", "اتأكد من النت وحاول تاني")
                                }
                            }
                        }
                    }
                    TextButton(onClick = onDismiss) { Text(tr("Close", "إغلاق"), color = Z.sub) }
                }
            }
        }
    }
}

/** Celebration shown when Pro turns on: glowing crown, gold confetti and the end date. */
@Composable
fun ProUnlocked(until: String, grants: String = "pro") {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "pro")
    val glow by t.animateFloat(0.55f, 1f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "g")
    val pop = remember { androidx.compose.animation.core.Animatable(0.3f) }
    val fall = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { pop.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 260f)) }
        fall.animateTo(1f, androidx.compose.animation.core.tween(2600, easing = androidx.compose.animation.core.LinearEasing))
    }
    val pieces = remember { List(36) { Triple(kotlin.random.Random.nextFloat(), kotlin.random.Random.nextFloat() * 0.5f, kotlin.random.Random.nextInt(4)) } }
    Box(Modifier.fillMaxWidth().height(330.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cols = listOf(Color(0xFFE7B75F), Color(0xFFFFE3A3), Color(0xFFF2865E), Color.White)
            pieces.forEach { (x, delay, c) ->
                val p = ((fall.value - delay) / (1 - delay)).coerceIn(0f, 1f)
                if (p > 0f) {
                    val y = -20f + p * (size.height + 40f)
                    val xx = x * size.width + kotlin.math.sin(p * 9 + x * 20) * 18f
                    rotate(p * 540f + x * 90f, androidx.compose.ui.geometry.Offset(xx, y)) {
                        drawRect(cols[c].copy(alpha = 1f - p * 0.6f), topLeft = androidx.compose.ui.geometry.Offset(xx - 4f, y - 7f), size = androidx.compose.ui.geometry.Size(8f, 14f))
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Box(
                Modifier.size(96.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Brush.radialGradient(listOf(Z.pro.copy(alpha = 0.55f * glow), Z.pro.copy(alpha = 0.08f)))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(54.dp)) }
            Spacer(Modifier.height(18.dp))
            Text(when (grants) { "wall" -> tr("Wallpapers unlocked!", "الخلفيات اتفتحت!"); "both" -> tr("Pro + Wallpapers on!", "البرو والخلفيات اتفعّلوا!"); else -> tr("Pro is on!", "البرو اتفعّل!") }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Z.pro)
            Spacer(Modifier.height(6.dp))
            Text(if (grants == "wall") tr("Apply & Save are yours", "التطبيق والحفظ بقوا مفتوحين") else tr("Every widget is unlocked", "كل الويدجتات بقت مفتوحة"), color = Z.text, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Z.pro.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 6.dp)) {
                Text(until, color = Z.pro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}


@Composable
private fun Chevron() = Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Z.sub, modifier = Modifier.size(20.dp))
