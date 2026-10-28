package com.smrh.zujaj.ui

import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
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
/**
 * Widgets already placed on the phone. An adaptive grid (one column on phones, more as the window
 * widens); each card shows the live design on a home-screen-like backdrop with a direct Edit action.
 */
@Composable
fun MyWidgetsTab(nav: Nav) {
    val ctx = LocalContext.current
    val ver by Prefs.version.collectAsState()
    val ids = remember(ver) { WidgetUpdater.allIds(ctx).filter { Prefs.widgetConfig(it)?.let { c -> Catalog.byId(c.styleId) } != null } }
    val pane = LocalPaneWidth.current
    val gut = Sp.gutter(pane)
    Column(Modifier.fillMaxSize()) {
        TabTitle(tr("My widgets", "ويدجتاتي"), if (ids.isEmpty()) null else tr("${ids.size} on your home screen", "${ids.size} على شاشتك"))
        if (ids.isEmpty()) {
            EmptyState(Icons.Rounded.Dashboard, tr("Nothing on your home screen yet", "لسه مفيش ويدجت على شاشتك"),
                tr("Pick any design, make it yours and tap Apply — it will show up here so you can edit it later.",
                    "اختار أي تصميم وعدّله واضغط إضافة — هيظهر هنا عشان تعدّله بعدين."),
                tr("How to add widgets", "طريقة إضافة الويدجت")) { nav.go(Screen.Tutorial) }
            return@Column
        }
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(if (pane >= 600) 300.dp else 280.dp),
            contentPadding = PaddingValues(start = gut, end = gut, bottom = LocalDockSpace.current),
            horizontalArrangement = Arrangement.spacedBy(Sp.l), verticalArrangement = Arrangement.spacedBy(Sp.l),
        ) {
            items(ids.size, key = { ids[it] }) { i ->
                val id = ids[i]
                val cfg = Prefs.widgetConfig(id) ?: return@items
                val st = Catalog.byId(cfg.styleId) ?: return@items
                Column(Modifier.clip(RoundedCornerShape(Rad.l)).background(Z.surface).border(1.dp, Z.line, RoundedCornerShape(Rad.l))
                    .clickable { nav.go(Screen.Editor(st.id, id)) }) {
                    Box(Modifier.fillMaxWidth().height(190.dp).padding(8.dp).clip(RoundedCornerShape(Rad.m)).background(Backdrops.brush(i)), contentAlignment = Alignment.Center) {
                        WidgetPreview(cfg, (if (st.shape == com.smrh.zujaj.catalog.Shape.SMALL) Modifier.width(140.dp) else Modifier.fillMaxWidth()).padding(14.dp))
                    }
                    Row(Modifier.fillMaxWidth().padding(start = Sp.l, end = Sp.s, top = 2.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(st.name(Prefs.isArabicUi), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(sizeLabel(st.shape), color = Z.sub, fontSize = 12.sp)
                        }
                        TextButton(onClick = { nav.go(Screen.Editor(st.id, id)) }) {
                            Icon(Icons.Rounded.Edit, null, tint = LocalAccent.current, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp)); Text(tr("Edit", "تعديل"), color = LocalAccent.current, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================== SETTINGS
private suspend fun locate(ctx: Context): Boolean = withContext(Dispatchers.IO) {
    if (!com.smrh.zujaj.data.LocationRepo.detect(ctx)) return@withContext false
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
    var showLoc by remember { mutableStateOf(false) }
    var showHealth by remember { mutableStateOf(false) }
    var showPrayerMore by rememberSaveable { mutableStateOf(false) }
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
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val purple = Color(0xFF8B7CFF); val teal = Color(0xFF13B5A8); val gold = Color(0xFFE0A93B); val blue = Color(0xFF4D8DFF)
    val green = Color(0xFF34C77B); val rose = Color(0xFFFF5C8A); val orange = Color(0xFFFF8A4C); val grey = Color(0xFF7A7A86)
    // Settings is a reference page people scan, so: compact title, a quiet membership strip, then groups.
    // From 840dp the groups flow into two columns instead of one long strip with empty margins.
    val pane = LocalPaneWidth.current
    val twoCols = pane >= 840
    val gut = Sp.gutter(pane)
    val proStrip: @Composable () -> Unit = {
        val isPro = Prefs.isPro
        Row(Modifier.fillMaxWidth().padding(horizontal = Sp.l).clip(RoundedCornerShape(Rad.l))
            .background(Z.pro.copy(alpha = if (isPro) 0.08f else 0.12f))
            .border(1.dp, Z.pro.copy(alpha = 0.35f), RoundedCornerShape(Rad.l))
            .clickable { nav.go(Screen.Paywall) }.padding(horizontal = Sp.l, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Z.pro, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isPro) tr("You're a Pro member", "إنت مشترك برو") else tr("Zujaj Pro", "زجاج برو"), color = Z.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (isPro) tr("Every widget is yours — thank you!", "كل الويدجت بتاعتك — شكراً ليك!") else tr("All widgets, no ads, every new design", "كل الويدجت، من غير إعلانات، وكل تصميم جديد"),
                    color = Z.sub, fontSize = 13.sp)
            }
            if (!isPro) Text(tr("See plans", "شوف الخطط"), color = Z.pro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Z.sub)
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (!asTab) TopBar("", { nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = (gut - Sp.l).coerceAtLeast(0.dp))) {
            Text(tr("Settings", "الإعدادات"), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = Sp.l + 4.dp, end = Sp.l, top = if (asTab) Sp.l + 4.dp else 0.dp, bottom = Sp.m))
            proStrip()
            // Five plain groups, the everyday options first. Rarely-touched switches live one tap deeper
            // ("More prayer options", "Keep widgets up to date") instead of filling the page.
            val left: @Composable ColumnScope.() -> Unit = {
            Section(tr("General", "عام")) {
                ChoiceRow(tr("Language", "اللغة"), Prefs.appLanguage, listOf("system" to tr("System", "النظام")) + com.smrh.zujaj.data.I18n.all.map { it.code to it.native }, Icons.Rounded.Language, blue) {
                    Prefs.appLanguage = it; WidgetUpdater.updateAll(ctx)
                }
                Divider()
                ChoiceRow(tr("Appearance", "المظهر"), Z.mode(), listOf(1 to tr("Dark", "غامق"), 2 to tr("Light", "فاتح"), 0 to tr("Like the phone", "زي التليفون")), Icons.Rounded.DarkMode, purple) {
                    Prefs.setString("theme_mode", it.toString()); Z.refresh(ctx)
                }
                Divider()
                RowBase(tr("App colour", "لون التطبيق"), icon = Icons.Rounded.Palette, iconColor = rose) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Z.accents.forEachIndexed { i, a ->
                            val on = i == (if (Z.accentIdx >= 0) Z.accentIdx else Prefs.accentIndex)
                            Box(Modifier.size(24.dp).clip(CircleShape).background(a.color)
                                .border(2.dp, if (on) Z.text else Color.Transparent, CircleShape)
                                .clickable { Haptics.tick(ctx); Z.pickAccent(i) })
                        }
                    }
                }
                Divider()
                SwitchRow(tr("Vibrate on touch", "اهتزاز خفيف مع اللمس"), Prefs.getString("haptics", "1") == "1", icon = Icons.Rounded.Vibration, iconColor = orange) {
                    Prefs.setString("haptics", if (it) "1" else "0")
                }
            }
            Section(tr("Prayer & location", "الصلاة والموقع")) {
                RowBase(tr("Location", "الموقع"), sub = Prefs.cityName.ifBlank { if (Prefs.hasLocation) "%.2f, %.2f".format(Prefs.lat, Prefs.lng) else tr("Not set — using Cairo", "مش متحدد — القاهرة") },
                    onClick = { showLoc = true }, icon = Icons.Rounded.MyLocation, iconColor = teal) { Chevron() }
                Divider()
                ChoiceRow(tr("Calculation method", "طريقة الحساب"), Prefs.prayerMethod, listOf(
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
                RowBase(tr("More prayer options", "خيارات صلاة أكتر"), sub = tr("Hanafi Asr, Hijri date adjustment", "العصر حنفي، تعديل التاريخ الهجري"),
                    onClick = { showPrayerMore = !showPrayerMore }, icon = Icons.Rounded.Tune, iconColor = grey) {
                    val rot by androidx.compose.animation.core.animateFloatAsState(if (showPrayerMore) 90f else 0f, Motion.std(), label = "pm")
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Z.sub, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot })
                }
                androidx.compose.animation.AnimatedVisibility(showPrayerMore, enter = androidx.compose.animation.expandVertically(Motion.std()) + androidx.compose.animation.fadeIn(Motion.std()),
                    exit = androidx.compose.animation.shrinkVertically(Motion.std()) + androidx.compose.animation.fadeOut(Motion.std(Motion.FAST))) {
                    Column(Modifier.background(Z.tint(0.025f))) {
                        Divider()
                        SwitchRow(tr("Hanafi Asr", "العصر حنفي"), Prefs.hanafi, sub = tr("Later Asr time (Hanafi school)", "وقت عصر متأخر (المذهب الحنفي)"), icon = Icons.Rounded.WbTwilight, iconColor = orange) { Prefs.hanafi = it; WidgetUpdater.updateAll(ctx) }
                        Divider()
                        StepperRow(tr("Hijri date ± days", "تعديل الهجري ± يوم"), Prefs.hijriOffset, -2..2, 1, icon = Icons.Rounded.DarkMode, iconColor = purple) { Prefs.hijriOffset = it; WidgetUpdater.updateAll(ctx) }
                    }
                }
            }
            Section(tr("Fasting reminders", "تذكير الصيام")) {
                // sent the evening before; Android 13+ asks for notification permission the first time one is switched on
                com.smrh.zujaj.data.Reminders.Kind.values().forEachIndexed { i, k ->
                    if (i > 0) Divider()
                    SwitchRow(tr(k.en, k.ar), com.smrh.zujaj.data.Reminders.on(k), sub = if (i == 0) tr("A notification the evening before", "إشعار بالليل قبلها") else null,
                        icon = Icons.Rounded.NotificationsActive, iconColor = if (i % 2 == 0) gold else purple) { v ->
                        com.smrh.zujaj.data.Reminders.set(k, v)
                        if (v && android.os.Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            Section(tr("Widgets", "الويدجت")) {
                RowBase(tr("Widget themes", "ثيمات الويدجت"), sub = tr("One look for all your widgets", "شكل واحد لكل الويدجت"), onClick = { showThemes = true }, icon = Icons.Rounded.AutoAwesome, iconColor = purple) { Chevron() }
                Divider()
                val exact = com.smrh.zujaj.widget.Scheduler.exactAllowed(ctx).also { touch(ver) }
                val media = MediaRepo.hasAccess(ctx)
                RowBase(tr("Keep widgets up to date", "خلّي الويدجت محدّثة"),
                    sub = if (exact) tr("Clock is exact ✓ · battery & auto-start inside", "الساعة مظبوطة ✓ · البطارية والتشغيل التلقائي جوه")
                    else tr("1 thing to fix so clocks don't lag", "حاجة واحدة محتاج تظبطها عشان الساعة متتأخرش"),
                    onClick = { showHealth = true }, icon = Icons.Rounded.BatteryChargingFull, iconColor = if (exact) green else orange) {
                    if (!exact) Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF8A4C)))
                    Spacer(Modifier.width(6.dp)); Chevron()
                }
                Divider()
                RowBase(tr("Music access", "الوصول للميديا"), sub = if (media) tr("Enabled ✓", "مفعّل ✓") else tr("Needed for media widgets", "مطلوب لويدجت الميديا"),
                    onClick = { nav.go(Screen.MediaAccess) }, icon = Icons.Rounded.MusicNote, iconColor = rose) { Chevron() }
                Divider()
                ChoiceRow(tr("Temperature", "درجة الحرارة"), Prefs.fahrenheit, listOf(false to "°C", true to "°F"), Icons.Rounded.Thermostat, orange) { Prefs.fahrenheit = it; WidgetUpdater.updateAll(ctx) }
                Divider()
                RowBase(tr("Refresh all widgets", "تحديث كل الويدجت"), onClick = {
                    scope.launch(Dispatchers.IO) { Scheduler.refreshWeatherIfNeeded(ctx, true); WidgetUpdater.updateAll(ctx) }
                    Toast.makeText(ctx, tr("Refreshing…", "جارٍ التحديث…"), Toast.LENGTH_SHORT).show()
                }, icon = Icons.Rounded.Refresh, iconColor = teal) {}
            }
            }
            val right: @Composable ColumnScope.() -> Unit = {
            Section(tr("Help & purchases", "المساعدة والمشتريات")) {
                RowBase(tr("How to add widgets", "طريقة إضافة الويدجت"), onClick = { nav.go(Screen.Tutorial) }, icon = Icons.Rounded.HelpOutline, iconColor = blue) { Chevron() }
                Divider()
                RowBase(tr("Open a shared design", "افتح تصميم اتبعتلك"), onClick = { showImport = true }, icon = Icons.Rounded.Link, iconColor = blue) { Chevron() }
                Divider()
                RowBase(tr("Redeem code", "استخدام كود"), sub = codeStatus(), onClick = { showRedeem = true }, icon = Icons.Rounded.Redeem, iconColor = gold) { Chevron() }
                Divider()
                RowBase(tr("Restore purchases", "استعادة المشتريات"), onClick = { Billing.restore(); Toast.makeText(ctx, tr("Checking…", "جارٍ التحقق…"), Toast.LENGTH_SHORT).show() },
                    icon = Icons.Rounded.Restore, iconColor = teal) {}
                Divider()
                RowBase(tr("Manage subscription", "إدارة الاشتراك"), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}"))) }
                }, icon = Icons.Rounded.CreditCard, iconColor = blue) { Chevron() }
            }
            Section(tr("About", "عن التطبيق")) {
                RowBase(tr("Rate Zujaj", "قيّم التطبيق"), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))) }
                }, icon = Icons.Rounded.Star, iconColor = gold) { Chevron() }
                Divider()
                RowBase(tr("Share the app", "شارك التطبيق"), onClick = { ctx.startActivity(Intent.createChooser(WidgetUpdater.shareIntent(ctx), null)) },
                    icon = Icons.Rounded.Share, iconColor = green) { Chevron() }
                Divider()
                RowBase(tr("Privacy & credits", "الخصوصية والحقوق"), onClick = { nav.go(Screen.About) }, icon = Icons.Rounded.Shield, iconColor = grey) { Chevron() }
            }
            // version line, quietly at the end (7 taps → control panel, as before)
            Text(tr("Zujaj ", "زجاج ") + BuildConfig.VERSION_NAME + " · by SMRH", color = Z.sub.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = Sp.l).clickable(remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, null) {
                    taps++
                    if (taps >= 7) { taps = 0; showGate = true }
                }.padding(vertical = 10.dp))
            // one clearly separated ad, after the settings people actually came for
            com.smrh.zujaj.ads.AdBanner(1, Modifier.padding(top = Sp.s))
            }
            if (twoCols) Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), content = left)
                Column(Modifier.weight(1f), content = right)
            } else { left(); right() }
            Spacer(Modifier.height(LocalDockSpace.current + 10.dp))
        }
    }
    if (showThemes) ThemesDialog { showThemes = false }
    if (showLoc) ZDialog(Icons.Rounded.MyLocation, tr("Your location", "موقعك"),
        tr("Used for prayer times and weather. It never leaves your phone except to fetch the forecast.", "بنستخدمه لمواقيت الصلاة والطقس بس، ومش بيخرج من تليفونك غير عشان نجيب الطقس."),
        iconColor = Color(0xFF13B5A8), onDismiss = { showLoc = false }) {
        PrimaryButton(tr("Detect automatically", "حدّد تلقائياً"), Modifier.fillMaxWidth(), icon = Icons.Rounded.MyLocation) {
            showLoc = false; perm.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
        Spacer(Modifier.height(8.dp))
        androidx.compose.material3.OutlinedButton(onClick = { showLoc = false; showCity = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(50)) {
            Text(tr("Choose a city", "اختار مدينة"), color = Z.text)
        }
    }
    if (showHealth) ZDialog(Icons.Rounded.BatteryChargingFull, tr("Keep widgets up to date", "خلّي الويدجت محدّثة"),
        tr("Some phones pause apps to save battery. These keep clocks and prayer times on time.", "بعض التليفونات بتوقف التطبيقات عشان توفر بطارية. دول بيخلّوا الساعة والمواقيت مظبوطة."),
        iconColor = Color(0xFF34C77B), onDismiss = { showHealth = false }) {
        val exact = com.smrh.zujaj.widget.Scheduler.exactAllowed(ctx)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.l)).border(1.dp, Z.line, RoundedCornerShape(Rad.l))) {
            RowBase(tr("Exact clock", "الساعة بالثانية"), sub = if (exact) tr("Allowed ✓", "مسموح ✓") else tr("Tap to allow", "اضغط واسمح"),
                onClick = { if (!exact) com.smrh.zujaj.widget.Scheduler.askExact(ctx) }, icon = Icons.Rounded.Schedule, iconColor = blue) { if (!exact) Chevron() }
            Divider()
            RowBase(tr("Battery optimisation", "تحسين البطارية"), sub = tr("Let Zujaj run in the background", "خلّي زجاج يشتغل في الخلفية"), onClick = {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }, icon = Icons.Rounded.BatteryChargingFull, iconColor = green) { Chevron() }
            if (com.smrh.zujaj.widget.Oem.autoStartIntent(ctx) != null) {
                Divider()
                RowBase(tr("Auto-start (${com.smrh.zujaj.widget.Oem.brand})", "التشغيل التلقائي (${com.smrh.zujaj.widget.Oem.brand})"),
                    onClick = { com.smrh.zujaj.widget.Oem.openAutoStart(ctx) }, icon = Icons.Rounded.PowerSettingsNew, iconColor = orange) { Chevron() }
            }
        }
        androidx.compose.material3.TextButton(onClick = { showHealth = false }, modifier = Modifier.padding(top = 8.dp)) { Text(tr("Done", "تمام"), color = Z.sub) }
    }
    if (showGate) AdminGateDialog({ showGate = false }) { showGate = false; nav.go(Screen.Admin) }
    if (showRedeem) RedeemDialog { showRedeem = false }
    if (showCity) CityPickerDialog({ showCity = false }) { c ->
        Prefs.saveLocation(c.lat, c.lng, c.name, auto = false)
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
    // one purchase sheet at a time: Continue locks until Play answers (or a short timeout if it never opens)
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(msg) { if (msg != null) busy = false }
    LaunchedEffect(busy) { if (busy) { kotlinx.coroutines.delay(6000); busy = false } }
    val pane = LocalPaneWidth.current
    val split = pane >= 840
    val benefits = listOf(
        Triple(Icons.Rounded.Widgets, tr("All ${Catalog.all.size}+ widgets", "كل الويدجت (${Catalog.all.size}+)"), tr("Clocks, prayer, media, calendar", "ساعات، صلاة، ميديا، تقويم")),
        Triple(Icons.Rounded.AutoAwesome, tr("Premium looks", "أشكال مميزة"), tr("Glass, aurora & textures", "زجاج، شفق وملمس")),
        Triple(Icons.Rounded.Block, tr("No ads", "من غير إعلانات"), tr("Clean, uninterrupted app", "تطبيق نضيف من غير مقاطعة")),
        Triple(Icons.Rounded.NewReleases, tr("New every month", "جديد كل شهر"), tr("Future drops included", "الإضافات الجاية معاك")),
    )

    val intro: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.size(60.dp).clip(RoundedCornerShape(Rad.l)).background(Brush.linearGradient(listOf(Color(0xFFF6D98B), Color(0xFFC8902E)))), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFF2A1C05), modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(Sp.l))
            Text(tr("Zujaj Pro", "زجاج برو"), fontSize = if (split) 34.sp else 28.sp, fontWeight = FontWeight.ExtraBold, color = Z.text)
            Text(tr("Every widget, every look, no ads.", "كل الويدجت، كل الأشكال، من غير إعلانات."), color = Z.sub, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(Sp.xl))
            // benefits as a checklist with one supporting line each: scannable, no card-in-card
            benefits.forEach { (icon, t, sub) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(Rad.s)).background(Z.pro.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Z.pro, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Z.text)
                        Text(sub, color = Z.sub, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }

    @Composable
    fun PlanCard(plan: Plan, mod: Modifier) {
        val p = prices[plan]
        val (title, sub) = when (plan) {
            Plan.MONTHLY -> tr("Monthly", "شهري") to tr("Renews every month", "بيتجدد كل شهر")
            Plan.YEARLY -> tr("Yearly", "سنوي") to (if (p?.trial == true) tr("Free trial, then yearly", "تجربة مجانية ثم سنوي") else tr("Renews every year", "بيتجدد كل سنة"))
            Plan.LIFETIME -> tr("Lifetime", "مدى الحياة") to tr("One payment, yours forever", "تدفع مرة واحدة وتبقى ليك")
        }
        val on = sel == plan
        val ring by androidx.compose.animation.animateColorAsState(if (on) accent else Z.line, Motion.color(), label = "ring")
        val fill by androidx.compose.animation.animateColorAsState(if (on) accent.copy(alpha = 0.09f) else Z.surface, Motion.color(), label = "fill")
        Row(mod.heightIn(min = 72.dp).clip(RoundedCornerShape(Rad.l)).background(fill)
            .border(if (on) 2.dp else 1.dp, ring, RoundedCornerShape(Rad.l))
            .clickable { Haptics.tick(ctx); sel = plan }.padding(horizontal = Sp.l, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(CircleShape).border(2.dp, if (on) accent else Z.sub, CircleShape), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(on, enter = androidx.compose.animation.fadeIn(Motion.std(Motion.FAST)) + androidx.compose.animation.scaleIn(Motion.std(Motion.FAST)),
                    exit = androidx.compose.animation.fadeOut(Motion.std(Motion.FAST))) {
                    Box(Modifier.size(11.dp).clip(CircleShape).background(accent))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Z.text)
                    // the only label we show is a real one: the store says this plan has a trial
                    if (plan == Plan.YEARLY && p?.trial == true) {
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Free trial", "تجربة مجانية"), color = Color(0xFF2A1C05), fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Z.pro).padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Text(sub, color = Z.sub, fontSize = 12.5.sp)
            }
            // price straight from Google Play (localised); a dash while the store is loading
            Text(p?.price ?: "—", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Z.text)
        }
    }

    val plans: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Sp.s + 2.dp)) {
            if (pro) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.l)).background(Z.pro.copy(alpha = 0.12f)).border(1.dp, Z.pro.copy(alpha = 0.5f), RoundedCornerShape(Rad.l)).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Z.pro, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(12.dp))
                    Column {
                        Text(tr("You have Pro", "انت مشترك في برو"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(tr("Manage or cancel any time in Google Play.", "تقدر تدير أو تلغي الاشتراك من جوجل بلاي في أي وقت."), color = Z.sub, fontSize = 13.sp)
                    }
                }
            } else {
                Text(tr("Choose your plan", "اختار خطتك"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Z.text, modifier = Modifier.padding(bottom = 2.dp))
                listOf(Plan.YEARLY, Plan.MONTHLY, Plan.LIFETIME).forEach { PlanCard(it, Modifier.fillMaxWidth()) }
            }
        }
    }

    val actions: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!pro) {
                val loading = prices.isEmpty()
                PrimaryButton(if (busy) tr("Opening Google Play…", "بنفتح جوجل بلاي…") else tr("Continue", "متابعة"), Modifier.fillMaxWidth()) {
                    if (busy) return@PrimaryButton
                    if (loading) { Toast.makeText(ctx, tr("Store not available yet — try again shortly", "المتجر غير متاح حالياً — حاول بعد قليل"), Toast.LENGTH_LONG).show(); return@PrimaryButton }
                    busy = true
                    val ok = Billing.buy(ctx as Activity, sel)
                    if (!ok) { busy = false; Toast.makeText(ctx, tr("Store not available yet — try again shortly", "المتجر غير متاح حالياً — حاول بعد قليل"), Toast.LENGTH_LONG).show() }
                }
            }
            Row(horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = { Billing.restore(); Toast.makeText(ctx, tr("Checking your purchases…", "بنراجع مشترياتك…"), Toast.LENGTH_SHORT).show() }) { Text(tr("Restore purchases", "استرجاع المشتريات"), color = Z.sub) }
                TextButton(onClick = { showRedeem = true }) { Text(tr("Have a code?", "معاك كود؟"), color = accent) }
            }
            Text(tr("Subscriptions renew automatically until cancelled in Google Play. Cancel anytime.", "الاشتراكات بتتجدد لوحدها لحد ما تلغيها من جوجل بلاي. تقدر تلغي في أي وقت."),
                color = Z.sub, fontSize = 11.5.sp, textAlign = TextAlign.Center, lineHeight = 15.sp)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // a quiet gold wash behind the header — the only decoration on the page
        Box(Modifier.fillMaxWidth().height(320.dp).background(Brush.verticalGradient(listOf(Z.pro.copy(alpha = 0.14f), Color.Transparent))))
        Column(Modifier.fillMaxSize()) {
            TopBar("", { nav.back() })
            if (split) {
                // tablets / unfolded: the pitch on one side, the decision on the other — nothing to scroll past
                Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = Sp.gutter(pane)), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = Sp.l)) { intro() }
                    Column(Modifier.weight(1f).widthIn(max = 520.dp).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(top = Sp.l, bottom = Sp.l)) {
                        plans(); Spacer(Modifier.height(Sp.xl)); actions()
                    }
                }
            } else if (pane >= 600) {
                // medium windows (tablet portrait, folded-open book posture): one centred column, actions right
                // under the plans instead of pinned to a far-away bottom edge
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = Sp.xl)) {
                        intro(); Spacer(Modifier.height(Sp.xl)); plans(); Spacer(Modifier.height(Sp.xl)); actions(); Spacer(Modifier.height(Sp.l))
                    }
                }
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    intro(); Spacer(Modifier.height(Sp.l)); plans(); Spacer(Modifier.height(Sp.l))
                }
                // sticky actions + disclosure
                Column(Modifier.fillMaxWidth().background(Z.bg.copy(alpha = 0.97f)).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) { actions() }
            }
        }
    }
}

// =============================================================================== TUTORIAL / MEDIA / ABOUT
@Composable
fun TutorialScreen(nav: Nav) {
    Column(Modifier.fillMaxSize()) {
        TopBar(tr("Quick start", "البدء السريع"), { nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 8.dp)) {
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
            // a numbered path with a thin connecting line — reads as steps, not as eight identical cards
            steps.forEachIndexed { i, (t, d) ->
                Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(horizontal = 6.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(if (i < 3) LocalAccent.current else Z.tint(0.08f)), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = if (i < 3) Color(0xFF14110A) else Z.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (i < steps.lastIndex) Box(Modifier.width(2.dp).weight(1f).background(Z.tint(0.08f)))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.padding(bottom = 22.dp, top = 3.dp)) {
                        Text(t, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Z.text)
                        Spacer(Modifier.height(3.dp))
                        Text(d, color = Z.sub, fontSize = 14.sp, lineHeight = 20.sp)
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
                "Zujaj has no accounts. Your widget settings, photos and contacts you choose stay on your phone. Your approximate location is sent only to the weather service (MET Norway) to get the forecast.\n\nADS\nZujaj contains ads. The free version shows ads from Google AdMob. AdMob may use your device's advertising ID and technical information to show and measure ads; where the law requires it (e.g. the EU/UK) we ask for your consent first. You can reset your advertising ID or turn off personalised ads in your phone's Google settings. Subscribers see no ads.\n\nUSAGE COUNTS\nTo know how the app is doing, Zujaj adds 1 to a few anonymous daily totals in Google Firebase: that the app was opened today, a first install, which widget design was added, a free wallpaper set, and a purchase. Nothing that identifies you or your phone is sent — no ID, no name, no location, no content.\n\nPurchases are handled by Google Play. The app is intended for users aged 13 and over.",
                "زجاج من غير حسابات. إعدادات الويدجت والصور وجهات الاتصال اللي بتختارها بتفضل على تليفونك. موقعك التقريبي بيتبعت بس لخدمة الطقس (MET Norway) عشان تجيب التوقعات.\n\nالإعلانات\nالتطبيق فيه إعلانات. النسخة المجانية فيها إعلانات من Google AdMob. AdMob ممكن يستخدم معرّف الإعلانات بتاع جهازك ومعلومات تقنية عشان يعرض الإعلانات ويقيس أداءها، وفي الدول اللي القانون فيها بيطلب ده (زي أوروبا) بنطلب موافقتك الأول. تقدر تعيد ضبط معرّف الإعلانات أو توقف الإعلانات المخصصة من إعدادات جوجل في تليفونك. المشتركين مش بيشوفوا إعلانات.\n\nإحصائيات الاستخدام\nعشان نعرف التطبيق ماشي إزاي، زجاج بيزوّد 1 على شوية أعداد يومية مجهولة في Google Firebase: إن التطبيق اتفتح النهارده، أول تثبيت، أنهي تصميم ويدجت اتضاف، خلفية مجانية اتحطت، وعملية شراء. مفيش أي حاجة بتعرّف عليك أو على تليفونك بتتبعت — لا معرّف ولا اسم ولا موقع ولا محتوى.\n\nالمشتريات بتتم عن طريق جوجل بلاي. التطبيق موجّه لمن هم أكبر من 13 سنة."), color = Z.sub)
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
