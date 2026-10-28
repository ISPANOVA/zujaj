package com.smrh.zujaj.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smrh.zujaj.BuildConfig
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import com.smrh.zujaj.R
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Block

/** Logo + "Loading" shown for a moment when the app opens. */
object SplashOnce { var shown = false }

@Composable
fun Splash(show: Boolean) {
    // only once per app start — never again when the screen is rebuilt (language, colour…)
    var visible by remember { mutableStateOf(show && !SplashOnce.shown) }
    LaunchedEffect(Unit) { SplashOnce.shown = true; delay(1200); visible = false }
    AnimatedVisibility(visible, exit = fadeOut(androidx.compose.animation.core.tween(350)) + scaleOut(targetScale = 1.04f)) {
        Box(
            Modifier.fillMaxSize().background(Color(0xFF0B0B12))
                .clickable(MutableInteractionSource(), null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(112.dp).clip(RoundedCornerShape(30.dp)).background(colorResource(R.color.ic_bg)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_fg), null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(22.dp))
                Text(tr("Zujaj", "زجاج"), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(26.dp))
                CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFFE7B75F), strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text(tr("Loading…", "جاري التحميل…"), color = Color(0xFF9C9CA6), fontSize = 13.sp)
            }
        }
    }
}

/** Update prompt (optional or required) and the one-time message from the control panel. */
@Composable
fun AppGates(nav: Nav? = null) {
    val ctx = LocalContext.current
    val f by Flags.flags.collectAsState()
    // ---- ad blockers: checked on start and every time the app comes back
    val pro by com.smrh.zujaj.billing.Billing.pro.collectAsState()
    val wall by com.smrh.zujaj.billing.Billing.wall.collectAsState()
    val blocked by com.smrh.zujaj.security.AdBlock.state.collectAsState()
    var checking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) scope.launch { com.smrh.zujaj.security.AdBlock.check(ctx) }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val lock = f.adsOn && f.adblockLock && !pro && !wall && blocked != com.smrh.zujaj.security.AdBlock.Why.NONE && nav?.top !is Screen.Paywall
    if (lock) {
        AdBlockWall(blocked, checking, onCheck = {
            scope.launch { checking = true; com.smrh.zujaj.security.AdBlock.check(ctx); delay(400); checking = false }
        }, onPro = { nav?.go(Screen.Paywall) })
        return
    }
    val outdated = f.latestCode > BuildConfig.VERSION_CODE
    var laterFor by remember { mutableLongStateOf(Prefs.getString("upd_later").toLongOrNull() ?: 0L) }
    var seen by remember { mutableStateOf(Prefs.getString("notice_seen")) }
    fun openStore() {
        val id = ctx.packageName
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
    when {
        outdated && f.forceUpdate -> Box(
            Modifier.fillMaxSize().background(Color(0xF20B0B12)).clickable(MutableInteractionSource(), null) {}.padding(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(colorResource(R.color.ic_bg))) {
                    Image(painterResource(R.drawable.ic_fg), null, Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(20.dp))
                Text(tr("Update required", "لازم تحدّث التطبيق"), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(f.updateNote.ifBlank { tr("A new version is out. Update to keep using Zujaj.", "فيه إصدار جديد. حدّث التطبيق عشان تكمل استخدامه.") },
                    color = Color(0xFFBDBDC6), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(tr("Update now", "حدّث دلوقتي"), Modifier.fillMaxWidth()) { openStore() }
            }
        }
        outdated && laterFor < f.latestCode -> GateDialog(
            tr("New update", "فيه تحديث جديد"),
            f.updateNote.ifBlank { tr("A new version of Zujaj is available.", "فيه إصدار جديد من زجاج.") },
            tr("Update", "حدّث"), { openStore() },
            tr("Later", "بعدين"), { laterFor = f.latestCode; Prefs.setString("upd_later", f.latestCode.toString()) },
        )
        f.noticeOnce && f.notice.isNotBlank() && seen != f.notice -> GateDialog(
            tr("Message", "رسالة"), f.notice, tr("OK", "تمام"), { seen = f.notice; Prefs.setString("notice_seen", f.notice) }, null, null,
        )
        else -> { BatteryIntro(); if (Prefs.getString("bat_intro") == "1") TimeAccessCheck() }
    }
}

@Composable
private fun GateDialog(title: String, body: String, ok: String, onOk: () -> Unit, other: String?, onOther: (() -> Unit)?) {
    androidx.compose.ui.window.Dialog(onDismissRequest = { (onOther ?: onOk)() }) {
        Column(Modifier.clip(RoundedCornerShape(26.dp)).background(Z.surface).padding(22.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(body, color = Z.sub)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (other != null && onOther != null) TextButton(onClick = onOther) { Text(other, color = Z.sub) }
                TextButton(onClick = onOk) { Text(ok, color = LocalAccent.current, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ------------------------------------------------------------------ first run: keep widgets alive
private fun ignoringBattery(ctx: android.content.Context): Boolean = runCatching {
    (ctx.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
}.getOrDefault(true)

/** Shown once on first launch: battery "Unrestricted" + the brand's auto-start, so widgets never freeze. */
@Composable
fun BatteryIntro() {
    val ctx = LocalContext.current
    var done by remember { mutableStateOf(Prefs.getString("bat_intro") == "1") }
    if (done) return
    // wait until the splash has gone
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(1700); while (!Onboarding.done()) delay(400); delay(500); ready = true }
    if (!ready) return
    val oem = remember { com.smrh.zujaj.widget.Oem.autoStartIntent(ctx) != null }
    var battOk by remember { mutableStateOf(ignoringBattery(ctx)) }
    var autoTapped by remember { mutableStateOf(false) }
    // re-check when the user comes back from Settings
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) battOk = ignoringBattery(ctx) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    fun finish() { Prefs.setString("bat_intro", "1"); done = true }
    LaunchedEffect(Unit) { if (battOk && !oem) finish() }
    if (battOk && !oem) return
    val gold = Color(0xFFE7B75F)
    androidx.compose.ui.window.Dialog(onDismissRequest = { finish() }) {
        Column(
            Modifier.clip(RoundedCornerShape(30.dp))
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF1E1A10), Z.surface, Z.surface)))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(gold.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.BatteryChargingFull, null, tint = gold, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(tr("Keep your widgets alive", "خلّي الويدجت شغالة على طول"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(tr("Some phones pause apps to save battery, so the clock and prayer times can freeze. Two quick steps fix it:",
                "بعض التليفونات بتوقف التطبيقات عشان توفّر البطارية، فالساعة ومواقيت الصلاة ممكن تقف. خطوتين سريعين يحلّوها:"),
                color = Z.sub, textAlign = TextAlign.Center, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            StepRow(1, tr("Battery: Unrestricted", "البطارية: بدون قيود"),
                tr("App info → Battery → Unrestricted", "معلومات التطبيق ← البطارية ← غير مقيّد"), battOk) {
                runCatching {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
            }
            if (oem) {
                Spacer(Modifier.height(10.dp))
                StepRow(2, tr("Auto-start (${com.smrh.zujaj.widget.Oem.brand})", "التشغيل التلقائي (${com.smrh.zujaj.widget.Oem.brand})"),
                    tr("Turn Zujaj on in the list", "فعّل زجاج في القايمة"), autoTapped) {
                    autoTapped = true; com.smrh.zujaj.widget.Oem.openAutoStart(ctx)
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (battOk) tr("Done", "تمام") else tr("Continue", "متابعة"), Modifier.fillMaxWidth()) { finish() }
            TextButton(onClick = { finish() }) { Text(tr("Later (Settings → Battery)", "بعدين (من الإعدادات ← البطارية)"), color = Z.sub, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun StepRow(n: Int, title: String, sub: String, ok: Boolean, onClick: () -> Unit) {
    val gold = Color(0xFFE7B75F)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Z.tint(0.05f))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(if (ok) Color(0xFF3DDC97).copy(alpha = 0.18f) else gold.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            if (ok) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, tint = Color(0xFF3DDC97), modifier = Modifier.size(18.dp))
            else Text("$n", color = gold, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(sub, color = Z.sub, fontSize = 12.sp)
        }
        Text(if (ok) tr("Done", "تم") else tr("Open", "افتح"), color = if (ok) Color(0xFF3DDC97) else gold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}


/** Full-screen stop while an ad blocker is on. Nothing behind it can be touched. */
@Composable
private fun AdBlockWall(why: com.smrh.zujaj.security.AdBlock.Why, checking: Boolean, onCheck: () -> Unit, onPro: () -> Unit) {
    val ctx = LocalContext.current
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize().background(Color(0xFA08080D)).clickable(MutableInteractionSource(), null) {}.systemBarsPadding().padding(26.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFFE5484D).copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Block, null, tint = Color(0xFFE5484D), modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(tr("Ad blocker detected", "فيه مانع إعلانات شغال"), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(tr("Zujaj is free thanks to a few small ads. Please turn off your ad blocker to keep using the app — or go Pro for no ads at all.",
                "زجاج مجاني بفضل شوية إعلانات صغيرة. اقفل مانع الإعلانات عشان تكمل استخدام التطبيق — أو اشترك برو ومفيش إعلانات خالص."),
                color = Color(0xFFBDBDC6), textAlign = TextAlign.Center)
            Spacer(Modifier.height(14.dp))
            Text(when (why) {
                com.smrh.zujaj.security.AdBlock.Why.PRIVATE_DNS -> tr("Turn off “Private DNS” (Settings → Network → Private DNS → Off or Automatic).", "اقفل «DNS الخاص» (الإعدادات ← الشبكة ← DNS خاص ← إيقاف أو تلقائي).")
                com.smrh.zujaj.security.AdBlock.Why.APP -> tr("Turn off AdGuard / Blokada / your blocking VPN.", "اقفل AdGuard أو Blokada أو الـ VPN اللي بيمنع الإعلانات.")
                else -> tr("Turn off any ad-blocking app, VPN or DNS.", "اقفل أي تطبيق أو VPN أو DNS بيمنع الإعلانات.")
            }, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(Z.tint(0.06f)).padding(12.dp))
            Spacer(Modifier.height(22.dp))
            PrimaryButton(if (checking) tr("Checking…", "بتأكد…") else tr("I turned it off — check again", "قفلته — اتأكد تاني"), Modifier.fillMaxWidth()) { if (!checking) onCheck() }
            Spacer(Modifier.height(8.dp))
            if (why == com.smrh.zujaj.security.AdBlock.Why.PRIVATE_DNS) TextButton(onClick = {
                runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }) { Text(tr("Open network settings", "افتح إعدادات الشبكة"), color = Color.White) }
            TextButton(onClick = onPro) { Text(tr("Go Pro — no ads", "اشترك برو — من غير إعلانات"), color = accent, fontWeight = FontWeight.Bold) }
        }
    }
}

// ------------------------------------------------------------------ gradient time: mandatory permissions
/**
 * Designs whose time is drawn into the picture (with a gradient) must be redrawn by the app every minute.
 * Phones only allow that on time with: exact alarms, battery "Unrestricted" and (on some brands) auto-start.
 * Without them the time can lag, so those designs require all three; otherwise the phone draws the time itself.
 */
object TimeAccess {
    fun exact(ctx: android.content.Context) = com.smrh.zujaj.widget.Scheduler.exactAllowed(ctx)
    fun battery(ctx: android.content.Context) = ignoringBattery(ctx)
    fun hasAutoStart(ctx: android.content.Context) = com.smrh.zujaj.widget.Oem.autoStartIntent(ctx) != null
    /** Auto-start can't be read back, so the user confirms it. */
    fun autoStart(ctx: android.content.Context) = !hasAutoStart(ctx) || Prefs.getString("autostart_ok") == "1"
    fun ok(ctx: android.content.Context) = exact(ctx) && battery(ctx) && autoStart(ctx)

    /** Widgets on the home screen that draw a gradient time. */
    fun gradientWidgets(ctx: android.content.Context): List<Int> = com.smrh.zujaj.widget.WidgetUpdater.allIds(ctx).filter { id ->
        val cfg = Prefs.widgetConfig(id) ?: return@filter false
        com.smrh.zujaj.catalog.Catalog.byId(cfg.styleId)?.bakeTime?.invoke(cfg) == true
    }

    /** Once, after updating: widgets made before this choice existed keep their gradient look. */
    fun migrate(ctx: android.content.Context) {
        if (Prefs.getString("grad_mig") == "1") return
        Prefs.setString("grad_mig", "1")
        com.smrh.zujaj.widget.WidgetUpdater.allIds(ctx).forEach { id ->
            val cfg = Prefs.widgetConfig(id) ?: return@forEach
            val st = com.smrh.zujaj.catalog.Catalog.byId(cfg.styleId) ?: return@forEach
            if (!cfg.gradTime && st.bakeTime(cfg.copy(gradTime = true))) Prefs.saveWidgetConfig(id, cfg.copy(gradTime = true))
        }
    }

    /** Switches every gradient widget to the always-exact time drawn by the phone. */
    fun makePlain(ctx: android.content.Context) {
        gradientWidgets(ctx).forEach { id ->
            Prefs.widgetConfig(id)?.let { Prefs.saveWidgetConfig(id, it.copy(gradTime = false)) }
            runCatching { com.smrh.zujaj.widget.WidgetUpdater.update(ctx, id) }
        }
    }
}

/**
 * The required steps for gradient time. [onPlain] = use the always-exact time without the gradient instead.
 * [onReady] runs once everything is allowed.
 */
@Composable
fun TimeAccessDialog(title: String, onPlain: () -> Unit, onReady: () -> Unit, onDismiss: () -> Unit, plainLabel: String = tr("Use it without the gradient", "استخدمها من غير تدرّج")) {
    val ctx = LocalContext.current
    var exact by remember { mutableStateOf(TimeAccess.exact(ctx)) }
    var batt by remember { mutableStateOf(TimeAccess.battery(ctx)) }
    var auto by remember { mutableStateOf(TimeAccess.autoStart(ctx)) }
    var autoOpened by remember { mutableStateOf(false) }
    val oem = remember { TimeAccess.hasAutoStart(ctx) }
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) { exact = TimeAccess.exact(ctx); batt = TimeAccess.battery(ctx); auto = TimeAccess.autoStart(ctx) }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val all = exact && batt && auto
    ZDialog(androidx.compose.material.icons.Icons.Rounded.Check, title,
        tr("Gradient clocks are drawn by Zujaj every minute. To stay on time on every phone, these are required:",
            "الساعة المتدرّجة زجاج بيرسمها كل دقيقة بنفسه. عشان تفضل مظبوطة على أي تليفون لازم الخطوات دي:"),
        onDismiss = onDismiss) {
        StepRow(1, tr("Alarms & reminders", "المنبهات والتذكيرات"), tr("Allow it for Zujaj", "اسمح بيها لزجاج"), exact) { com.smrh.zujaj.widget.Scheduler.askExact(ctx) }
        StepRow(2, tr("Battery: Unrestricted", "البطارية: بدون قيود"), tr("App info → Battery → Unrestricted", "معلومات التطبيق ← البطارية ← غير مقيّد"), batt) {
            runCatching {
                ctx.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { runCatching { ctx.startActivity(Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
        }
        if (oem) {
            StepRow(3, tr("Auto-start (${com.smrh.zujaj.widget.Oem.brand})", "التشغيل التلقائي (${com.smrh.zujaj.widget.Oem.brand})"),
                tr("Turn Zujaj on in the list", "فعّل زجاج في القايمة"), auto) { com.smrh.zujaj.widget.Oem.openAutoStart(ctx); autoOpened = true }
            if (autoOpened && !auto) TextButton(onClick = { Prefs.setString("autostart_ok", "1"); auto = true }) {
                Text(tr("I turned it on ✓", "فعّلته ✓"), color = LocalAccent.current, fontWeight = FontWeight.Bold)
            }
        }
        PrimaryButton(if (all) tr("Done", "تمام") else tr("Finish the steps above", "كمّل الخطوات اللي فوق"), Modifier.fillMaxWidth()) { if (all) onReady() }
        TextButton(onClick = onPlain, modifier = Modifier.fillMaxWidth()) { Text(plainLabel, color = Z.sub) }
    }
}

/** On opening the app: gradient widgets without the required permissions → allow them, or switch to exact time. */
@Composable
fun TimeAccessCheck() {
    val ctx = LocalContext.current
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1500); while (!Onboarding.done()) delay(500)
        show = TimeAccess.gradientWidgets(ctx).isNotEmpty() && !TimeAccess.ok(ctx)
    }
    if (show) TimeAccessDialog(
        title = tr("Your clock widget may run late", "ويدجت الساعة عندك ممكن تتأخر"),
        plainLabel = tr("Switch to exact time (no gradient)", "حوّلها لوقت مظبوط (من غير تدرّج)"),
        onPlain = { TimeAccess.makePlain(ctx); show = false; android.widget.Toast.makeText(ctx, tr("Done — the time is now always exact", "تمام — الوقت بقى مظبوط على طول"), android.widget.Toast.LENGTH_LONG).show() },
        onReady = { show = false; com.smrh.zujaj.widget.WidgetUpdater.updateAll(ctx) },
        onDismiss = { },   // required: pick one of the two
    )
}
