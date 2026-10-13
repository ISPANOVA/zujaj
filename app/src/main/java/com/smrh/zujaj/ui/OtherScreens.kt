package com.smrh.zujaj.ui

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
fun SettingsScreen(nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCity by remember { mutableStateOf(false) }
    var taps by remember { mutableIntStateOf(0) }
    val ver by Prefs.version.collectAsState()
    touch(ver)
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) scope.launch {
            val ok = locate(ctx)
            Toast.makeText(ctx, if (ok) tr("Location updated ✓", "تم تحديث الموقع ✓") else tr("Turn on location and try again", "شغّل الموقع وحاول مرة أخرى"), Toast.LENGTH_SHORT).show()
        }
    }
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Settings", "الإعدادات"), { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Glass(Modifier.fillMaxWidth().padding(horizontal = 14.dp).clickable { nav.go(Screen.Paywall) }, radius = 22.dp, alpha = 0.09f) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (Prefs.isPro) tr("You are Pro ✓", "أنت مشترك برو ✓") else tr("Zujaj Pro", "زجاج برو"), style = MaterialTheme.typography.titleMedium)
                        Text(tr("All widgets, icon styles & wallpapers", "كل الويدجت والأيقونات والخلفيات"), color = Z.sub, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Section(tr("Appearance", "المظهر")) {
                ChoiceRow(tr("Language", "اللغة"), Prefs.appLanguage, listOf("system" to tr("System", "النظام"), "en" to "English", "ar" to "العربية")) {
                    Prefs.appLanguage = it; WidgetUpdater.updateAll(ctx)
                }
                Divider()
                RowBase(tr("Accent", "لون التطبيق")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Z.accents.forEachIndexed { i, a ->
                            Box(Modifier.size(24.dp).clip(CircleShape).background(a.color)
                                .border(2.dp, if (i == Prefs.accentIndex) Color.White else Color.Transparent, CircleShape)
                                .clickable { Prefs.accentIndex = i })
                        }
                    }
                }
            }
            Section(tr("Location", "الموقع")) {
                RowBase(tr("Use my location", "استخدم موقعي"), sub = Prefs.cityName.ifBlank { if (Prefs.hasLocation) "%.2f, %.2f".format(Prefs.lat, Prefs.lng) else tr("Not set (Cairo)", "غير محدد (القاهرة)") },
                    onClick = { perm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }) {
                    Text(tr("Detect", "تحديد"), color = LocalAccent.current)
                }
                Divider()
                RowBase(tr("Choose city", "اختر مدينة"), onClick = { showCity = true }) { Text("▾", color = LocalAccent.current) }
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
                )) { Prefs.prayerMethod = it; WidgetUpdater.updateAll(ctx); Scheduler.scheduleAlarms(ctx) }
                Divider()
                SwitchRow(tr("Hanafi Asr", "العصر حنفي"), Prefs.hanafi) { Prefs.hanafi = it; WidgetUpdater.updateAll(ctx) }
                Divider()
                StepperRow(tr("Hijri adjustment", "تعديل الهجري"), Prefs.hijriOffset, -2..2, 1) { Prefs.hijriOffset = it; WidgetUpdater.updateAll(ctx) }
            }
            Section(tr("Widgets", "الويدجت")) {
                SwitchRow(tr("Fahrenheit", "فهرنهايت"), Prefs.fahrenheit) { Prefs.fahrenheit = it; WidgetUpdater.updateAll(ctx) }
                Divider()
                RowBase(tr("Music access", "الوصول للميديا"), sub = if (MediaRepo.hasAccess(ctx)) tr("Enabled ✓", "مفعّل ✓") else tr("Needed for media widgets", "مطلوب لويدجت الميديا"),
                    onClick = { nav.go(Screen.MediaAccess) }) {}
                Divider()
                RowBase(tr("Refresh all widgets", "تحديث كل الويدجت"), onClick = {
                    scope.launch(Dispatchers.IO) { Scheduler.refreshWeatherIfNeeded(ctx, true); WidgetUpdater.updateAll(ctx) }
                    Toast.makeText(ctx, tr("Refreshing…", "جارٍ التحديث…"), Toast.LENGTH_SHORT).show()
                }) {}
                Divider()
                RowBase(tr("Battery optimisation", "تحسين البطارية"), sub = tr("Allow Zujaj to run so widgets stay fresh", "اسمح للتطبيق بالعمل لتبقى الويدجت محدّثة"), onClick = {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }) {}
                Divider()
                RowBase(tr("How to add widgets", "طريقة إضافة الويدجت"), onClick = { nav.go(Screen.Tutorial) }) {}
            }
            Section(tr("Subscription", "الاشتراك")) {
                RowBase(tr("Restore purchases", "استعادة المشتريات"), onClick = { Billing.restore(); Toast.makeText(ctx, tr("Checking…", "جارٍ التحقق…"), Toast.LENGTH_SHORT).show() }) {}
                Divider()
                RowBase(tr("Manage subscription", "إدارة الاشتراك"), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}"))) }
                }) {}
            }
            Section(tr("About", "حول")) {
                RowBase(tr("Rate Zujaj", "قيّم التطبيق"), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))) }
                }) {}
                Divider()
                RowBase(tr("Share the app", "شارك التطبيق"), onClick = { ctx.startActivity(Intent.createChooser(WidgetUpdater.shareIntent(ctx), null)) }) {}
                Divider()
                RowBase(tr("Privacy & credits", "الخصوصية والحقوق"), onClick = { nav.go(Screen.About) }) {}
                Divider()
                RowBase(tr("Version", "الإصدار"), sub = BuildConfig.VERSION_NAME + " · by SMRH", onClick = {
                    taps++
                    if (taps >= 7) { taps = 0; nav.go(Screen.Admin) }
                }) {}
            }
            Spacer(Modifier.height(60.dp))
        }
    }
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
    LaunchedEffect(msg) {
        when (msg) {
            "pro_ok" -> { Toast.makeText(ctx, tr("Welcome to Pro ✓", "أهلاً بك في برو ✓"), Toast.LENGTH_LONG).show(); nav.back() }
            "pending" -> Toast.makeText(ctx, tr("Payment pending…", "الدفع قيد المعالجة…"), Toast.LENGTH_LONG).show()
            "error" -> Toast.makeText(ctx, tr("Purchase failed", "فشل الشراء"), Toast.LENGTH_LONG).show()
        }
        if (msg != null) Billing.consumeMessage()
    }
    Column(Modifier.fillMaxSize()) {
        TopBar("", { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF3A2C12), Color(0xFF17130B)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(tr("Zujaj Pro", "زجاج برو"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(tr("Unlock every widget and future drops", "افتح كل الويدجت وكل الإضافات القادمة"), color = Z.sub, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            listOf(
                tr("All ${Catalog.all.size}+ widgets — clocks, prayer, media, calendar…", "كل الويدجت (${Catalog.all.size}+) — ساعات، صلاة، ميديا، تقويم…"),
                tr("Every icon style & wallpaper", "كل ستايلات الأيقونات والخلفيات"),
                tr("Glass, aurora & texture backgrounds", "خلفيات زجاجية وشفق وملمس"),
                tr("New widgets every month", "ويدجت جديدة كل شهر"),
                tr("Support an independent developer ♥", "ادعم مطوراً مستقلاً ♥"),
            ).forEach {
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = LocalAccent.current, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp)); Text(it)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (pro) {
                Text(tr("You already have Pro ✓", "أنت مشترك في برو بالفعل ✓"), color = LocalAccent.current, style = MaterialTheme.typography.titleMedium)
            } else {
                Plan.values().forEach { plan ->
                    val p = prices[plan]
                    val (title, sub) = when (plan) {
                        Plan.MONTHLY -> tr("Monthly", "شهري") to tr("Billed every month", "يُجدد كل شهر")
                        Plan.YEARLY -> tr("Yearly", "سنوي") to (if (p?.trial == true) tr("Free trial, then yearly", "تجربة مجانية ثم سنوي") else tr("Best value", "الأوفر"))
                        Plan.LIFETIME -> tr("Lifetime", "مدى الحياة") to tr("Pay once, keep forever", "ادفع مرة واحدة للأبد")
                    }
                    val selected = sel == plan
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (selected) LocalAccent.current.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f))
                            .border(1.5.dp, if (selected) LocalAccent.current else Z.line, RoundedCornerShape(20.dp))
                            .clickable { sel = plan }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                if (plan == Plan.YEARLY) { Spacer(Modifier.width(8.dp)); NewBadge() }
                            }
                            Text(sub, color = Z.sub, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(p?.price ?: "—", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(tr("Continue", "متابعة"), Modifier.fillMaxWidth()) {
                    val ok = Billing.buy(ctx as Activity, sel)
                    if (!ok) Toast.makeText(ctx, tr("Store not available yet — try again shortly", "المتجر غير متاح حالياً — حاول بعد قليل"), Toast.LENGTH_LONG).show()
                }
                TextButton(onClick = { Billing.restore() }) { Text(tr("Restore purchases", "استعادة المشتريات"), color = Z.sub) }
            }
            Text(tr("Subscriptions renew automatically until cancelled in Google Play. Cancel anytime.", "الاشتراكات تتجدد تلقائياً حتى تلغيها من جوجل بلاي. يمكنك الإلغاء في أي وقت."),
                color = Z.sub, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp))
            Spacer(Modifier.height(30.dp))
        }
    }
}

// =============================================================================== ADMIN
@Composable
fun AdminScreen(nav: Nav) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = Flags.auth()
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Admin dashboard", "لوحة التحكم"), { nav.back() })
        if (auth == null) {
            Text(tr("Firebase isn't configured in this build. Add the project keys in res/values/firebase.xml.", "Firebase غير مُعد في هذه النسخة. أضف مفاتيح المشروع في res/values/firebase.xml."),
                modifier = Modifier.padding(20.dp), color = Z.sub)
            return
        }
        var user by remember { mutableStateOf(auth.currentUser) }
        if (user == null) {
            var email by remember { mutableStateOf("") }
            var pass by remember { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            Column(Modifier.padding(20.dp)) {
                OutlinedTextField(email, { email = it.trim() }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(pass, { pass = it }, label = { Text(tr("Password", "كلمة المرور")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                PrimaryButton(if (busy) "…" else tr("Sign in", "دخول"), Modifier.fillMaxWidth()) {
                    if (busy) return@PrimaryButton
                    busy = true
                    scope.launch {
                        val r = runCatching { auth.signInWithEmailAndPassword(email, pass).await() }
                        busy = false
                        r.onSuccess { user = auth.currentUser }.onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_LONG).show() }
                    }
                }
                TextButton(onClick = {
                    scope.launch {
                        val r = runCatching { auth.createUserWithEmailAndPassword(email, pass).await(); auth.currentUser?.sendEmailVerification()?.await() }
                        r.onSuccess { user = auth.currentUser; Toast.makeText(ctx, tr("Account created – verify your email", "تم إنشاء الحساب – فعّل بريدك"), Toast.LENGTH_LONG).show() }
                            .onFailure { Toast.makeText(ctx, it.message, Toast.LENGTH_LONG).show() }
                    }
                }) { Text(tr("Create admin account (first time)", "إنشاء حساب الأدمن (أول مرة)"), color = Z.sub) }
            }
            return
        }
        val flags by Flags.flags.collectAsState()
        var pro by remember(flags) { mutableStateOf((flags.pro ?: Catalog.all.filter { it.pro }.map { it.id }).toSet()) }
        var fresh by remember(flags) { mutableStateOf((flags.fresh ?: Catalog.all.filter { it.isNew }.map { it.id }).toSet()) }
        var hidden by remember(flags) { mutableStateOf(flags.hidden.toSet()) }
        var freeAll by remember(flags) { mutableStateOf(flags.freeAll) }
        var notice by remember(flags) { mutableStateOf(flags.notice) }
        var q by remember { mutableStateOf("") }
        Column(Modifier.padding(horizontal = 14.dp)) {
            Text("${user?.email} " + if (user?.isEmailVerified == true) "✓" else tr("(email not verified)", "(البريد غير مفعّل)"), color = Z.sub, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Everything free (promo)", "كل شيء مجاني (عرض)"), modifier = Modifier.weight(1f))
                Switch(freeAll, { freeAll = it }, colors = SwitchDefaults.colors(checkedTrackColor = LocalAccent.current))
            }
            OutlinedTextField(notice, { notice = it }, label = { Text(tr("Banner message to all users", "رسالة تظهر لكل المستخدمين")) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(q, { q = it }, label = { Text(tr("Search widgets", "ابحث عن ويدجت")) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(tr("Widget", "الويدجت"), modifier = Modifier.weight(1f), color = Z.sub, fontSize = 12.sp)
                Text("Pro", modifier = Modifier.width(58.dp), color = Z.sub, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text(tr("New", "جديد"), modifier = Modifier.width(58.dp), color = Z.sub, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text(tr("Hide", "إخفاء"), modifier = Modifier.width(58.dp), color = Z.sub, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 14.dp, bottom = 100.dp)) {
            items(Catalog.all.filter { q.isBlank() || it.en.contains(q, true) || it.ar.contains(q) || it.cat.en.contains(q, true) }) { st ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(64.dp)) { WidgetPreview(st.defaultConfig(), Modifier.fillMaxWidth(), density = 1f) }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(st.name(Prefs.isArabicUi), fontSize = 13.sp)
                        Text(if (Prefs.isArabicUi) st.cat.ar else st.cat.en, fontSize = 10.sp, color = Z.sub)
                    }
                    Box(Modifier.width(58.dp), contentAlignment = Alignment.Center) { Switch(st.id in pro, { pro = if (it) pro + st.id else pro - st.id }, colors = SwitchDefaults.colors(checkedTrackColor = Z.pro)) }
                    Box(Modifier.width(58.dp), contentAlignment = Alignment.Center) { Switch(st.id in fresh, { fresh = if (it) fresh + st.id else fresh - st.id }, colors = SwitchDefaults.colors(checkedTrackColor = LocalAccent.current)) }
                    Box(Modifier.width(58.dp), contentAlignment = Alignment.Center) { Switch(st.id in hidden, { hidden = if (it) hidden + st.id else hidden - st.id }) }
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            PrimaryButton(tr("Publish to all users", "نشر لكل المستخدمين"), Modifier.fillMaxWidth()) {
                scope.launch {
                    val r = runCatching { Flags.save(CatalogFlags(pro.toList(), fresh.toList(), hidden.toList(), freeAll, notice.trim())) }
                    Toast.makeText(ctx, if (r.isSuccess) tr("Published ✓ — live for everyone", "تم النشر ✓ — ظاهر للجميع الآن") else (r.exceptionOrNull()?.message ?: "Error"), Toast.LENGTH_LONG).show()
                }
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
                tr("Lock screen", "شاشة القفل") to tr("Phones with lock-screen widget support (e.g. Samsung One UI 7+, Android 16 hub) list Zujaj widgets there too. Use the Walls tab to put a matching wallpaper on the lock screen.", "الهواتف التي تدعم ويدجت شاشة القفل (مثل سامسونج One UI 7+ وأندرويد 16) تعرض ويدجت زجاج هناك أيضاً. واستخدم قسم الخلفيات لوضع خلفية متناسقة على شاشة القفل."),
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
                "Zujaj has no accounts, no ads and no analytics. Your widget settings, photos and contacts you choose stay on your phone. Your approximate location is sent only to the weather service (MET Norway) to get the forecast. Purchases are handled by Google Play.",
                "زجاج بدون حسابات أو إعلانات أو تتبع. إعدادات الويدجت والصور وجهات الاتصال التي تختارها تبقى على هاتفك. يُرسل موقعك التقريبي فقط لخدمة الطقس (MET Norway) لجلب التوقعات. المشتريات تتم عبر جوجل بلاي."), color = Z.sub)
            Spacer(Modifier.height(18.dp))
            Text(tr("Credits", "الحقوق"), style = MaterialTheme.typography.titleMedium)
            Text("• Weather data: MET Norway (api.met.no), CC BY 4.0\n• Prayer times: Adhan library (MIT)\n• Fonts: Manrope, Cairo, Playfair Display, Bodoni Moda, Big Shoulders, Oswald, Cormorant, Amiri, Aref Ruqaa, Reem Kufi, Rakkas — SIL Open Font License\n• Designed & built by SMRH", color = Z.sub)
        }
    }
}
