package com.smrh.zujaj.ui

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.BugReport
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.displayCutoutPadding

import androidx.compose.foundation.layout.imePadding

import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.widget.WidgetUpdater

sealed class Screen {
    data object Home : Screen()
    data class Editor(val styleId: String, val widgetId: Int = -1, val configure: Boolean = false, val shared: String = "") : Screen()
    data class Pick(val shape: Shape, val widgetId: Int) : Screen()
    data class Category(val key: String) : Screen()
    data object Search : Screen()
    data object Paywall : Screen()
    data object Settings : Screen()
    data object Admin : Screen()
    data object Tutorial : Screen()
    data object MediaAccess : Screen()
    data object About : Screen()
    data object Wallpapers : Screen()
    data class SetDetail(val id: String) : Screen()
    data object Gallery : Screen()
    data object Sets : Screen()
}

/** One back-stack entry; [id] is unique so each entry keeps its own saved UI state (tab, filters, scroll…). */
data class Entry(val screen: Screen, val id: Long)

class Nav(start: Screen) {
    // ids are unique across app restarts: the saved UI state of an old page can never be handed to a
    // different page that happens to reuse its id (that mix-up crashed the app when it was reopened
    // from a widget after Android had closed it in the background)
    private var next = System.currentTimeMillis() * 1000
    val entries = mutableStateListOf(Entry(start, next++))
    val stack: List<Screen> get() = entries.map { it.screen }
    val top get() = entries.last().screen
    val topEntry get() = entries.last()
    /** Direction of the last move, for the page transition. */
    var forward = true
        private set
    /** Called when an entry leaves the stack for good, so its saved state can be dropped. */
    var onPop: (Long) -> Unit = {}
    private var lastPush = Long.MIN_VALUE / 2
    /** Time source for the double-tap guard (replaceable in tests). */
    var clock: () -> Long = { android.os.SystemClock.uptimeMillis() }
    fun go(s: Screen) {
        // ignore double taps that would open the same screen twice, and a second push fired within the
        // same tap (two cards hit at once) — both used to leave a duplicate page in the history
        val now = clock()
        if (entries.last().screen == s || now - lastPush < 300) return
        lastPush = now
        forward = true; entries.add(Entry(s, next++))
    }

    /** The whole history as text, so it survives the system killing the app in the background. */
    fun save(): ArrayList<String> = ArrayList(entries.map { "${it.id}\u0001${enc(it.screen)}" })
    fun restore(list: List<String>?) {
        val back = list?.mapNotNull { line ->
            val (id, rest) = line.split('\u0001', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
            dec(rest)?.let { Entry(it, id.toLongOrNull() ?: return@mapNotNull null) }
        }.orEmpty()
        if (back.isEmpty()) return
        entries.clear(); entries.addAll(back); next = back.maxOf { it.id } + 1
    }
    private fun enc(s: Screen): String = when (s) {
        is Screen.Editor -> listOf("E", s.styleId, s.widgetId, s.configure, s.shared).joinToString("\u0002")
        is Screen.Pick -> listOf("P", s.shape.name, s.widgetId).joinToString("\u0002")
        is Screen.Category -> "C\u0002" + s.key
        is Screen.SetDetail -> "S\u0002" + s.id
        // fixed names (class names are renamed in release builds, so they can't be stored)
        Screen.Search -> "Search"; Screen.Paywall -> "Paywall"; Screen.Settings -> "Settings"; Screen.Admin -> "Admin"
        Screen.Tutorial -> "Tutorial"; Screen.MediaAccess -> "MediaAccess"; Screen.About -> "About"; Screen.Wallpapers -> "Wallpapers"; Screen.Gallery -> "Gallery"; Screen.Sets -> "Sets"
        Screen.Home -> "Home"
    }
    private fun dec(t: String): Screen? = runCatching {
        val p = t.split('\u0002')
        when (p[0]) {
            "E" -> Screen.Editor(p[1], p[2].toInt(), p[3].toBoolean(), p.getOrElse(4) { "" })
            "P" -> Screen.Pick(Shape.valueOf(p[1]), p[2].toInt())
            "C" -> Screen.Category(p[1])
            "S" -> Screen.SetDetail(p[1])
            "Home" -> Screen.Home; "Search" -> Screen.Search; "Paywall" -> Screen.Paywall; "Settings" -> Screen.Settings
            "Admin" -> Screen.Admin; "Tutorial" -> Screen.Tutorial; "MediaAccess" -> Screen.MediaAccess
            "About" -> Screen.About; "Wallpapers" -> Screen.Wallpapers; "Gallery" -> Screen.Gallery; "Sets" -> Screen.Sets
            else -> null
        }
    }.getOrNull()
    fun back(): Boolean = if (entries.size > 1) { forward = false; onPop(entries.removeAt(entries.lastIndex).id); true } else false
    /** Start over at [s] (tests, deep links). */
    fun reset(s: Screen) { entries.forEach { onPop(it.id) }; entries.clear(); entries.add(Entry(s, next++)) }
    fun replace(s: Screen) { val old = entries.last(); onPop(old.id); entries[entries.lastIndex] = Entry(s, next++) }
}

open class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_EDIT_WIDGET = "edit_widget"
        const val EXTRA_OPEN = "open"
    }

    private lateinit var nav: Nav
    var configureId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        // system splash: shown only while the first frame is prepared (no artificial delay), then the logo
        // shrinks and fades into the app
        val splash = installSplashScreen()
        splash.setOnExitAnimationListener { v: androidx.core.splashscreen.SplashScreenViewProvider ->
            // When the app is opened from a widget (or a warm start), Android shows the splash WITHOUT an
            // icon, and asking for the icon then throws. Everything here is optional polish, so it must
            // never be able to crash: animate what exists, and always remove the splash.
            runCatching {
                v.view.animate().alpha(0f).setDuration(260).withEndAction { runCatching { v.remove() } }.start()
                runCatching { v.iconView }.getOrNull()?.animate()?.scaleX(0.82f)?.scaleY(0.82f)?.setDuration(260)
                    ?.setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))?.start()
            }.onFailure { runCatching { v.remove() } }
        }
        Prefs.init(this)
        appCtx = applicationContext
        runCatching { TimeAccess.migrate(this) }
        runCatching { WallMatch.follow(this) }
        if (savedInstanceState == null) {
            com.smrh.zujaj.remote.Stats.opened()
            // seasonal sets from the control panel
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { runCatching { com.smrh.zujaj.remote.RemoteSets.load() } }
        }
        Z.refresh(this)
        bars()
        super.onCreate(savedInstanceState)
        runCatching { com.smrh.zujaj.ads.Ads.start(this) }
        com.smrh.zujaj.CrashLog.lastIntent = "${intent?.action} ${intent?.extras?.keySet()?.joinToString()}"
        // a bad or stale widget intent must never stop the app from opening: fall back to Home
        nav = Nav(runCatching { startScreen(intent) }.getOrDefault(Screen.Home))
        // back from the background after the system reclaimed memory: same pages, same order, same state
        if (savedInstanceState != null) nav.restore(savedInstanceState.getStringArrayList("nav"))
        if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, configureId))
        }
        setContent {
            val ver by Prefs.version.collectAsState()
            key(ver.coerceAtMost(0), Prefs.uiLang) {
                ZujajTheme {
                    val light = Z.light
                    LaunchedEffect(light) { bars() }
                    Root(nav)
                    CrashReportDialog()
                    RewardedInterIntro()
                    if (configureId == AppWidgetManager.INVALID_APPWIDGET_ID) AppGates(nav)
                    if (configureId == AppWidgetManager.INVALID_APPWIDGET_ID) OnboardingScreen()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::nav.isInitialized) outState.putStringArrayList("nav", nav.save())
    }

    private fun bars() {
        val st = if (Z.light) SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) else SystemBarStyle.dark(0)
        enableEdgeToEdge(statusBarStyle = st, navigationBarStyle = st)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Z.refresh(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        com.smrh.zujaj.CrashLog.lastIntent = "new: ${intent.action} ${intent.extras?.keySet()?.joinToString()}"
        val s = runCatching { startScreen(intent) }.getOrDefault(Screen.Home)
        if (s != Screen.Home) nav.go(s)
    }

    private fun startScreen(i: Intent?): Screen {
        if (i == null) return Screen.Home
        if (i.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE) {
            configureId = i.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val shape = WidgetUpdater.shapeOf(this, configureId)
            // New or existing (e.g. the ⚙ on a Samsung lock-screen widget): choose ANY design for it
            return Screen.Pick(shape, configureId)
        }
        // a design someone shared: zujaj://w/CODE or zujaj.pages.dev/w#CODE
        ShareDesign.codeFrom(i.data)?.let { code -> ShareDesign.decode(code)?.let { return Screen.Editor(it.styleId, shared = code) } }
        val edit = i.getIntExtra(EXTRA_EDIT_WIDGET, -1)
        if (edit != -1) {
            val cfg = WidgetUpdater.configFor(this, edit)
            if (Catalog.byId(cfg.styleId) != null) return Screen.Editor(cfg.styleId, edit)
        }
        return when (i.getStringExtra(EXTRA_OPEN)) {
            "tutorial" -> Screen.Tutorial
            "media_access" -> Screen.MediaAccess
            "paywall" -> Screen.Paywall
            "settings" -> Screen.Settings
            else -> Screen.Home
        }
    }

    fun finishConfigure(id: Int) {
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        finish()
    }
}

class ConfigureActivity : MainActivity()

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun Root(nav: Nav) {
    BackHandler(enabled = nav.stack.size > 1) { nav.back() }
    AppBackdrop {
        val rtl = Prefs.isRtlUi
        // every screen keeps its remembered state (tab, chips, filters, scroll position) while it is in the
        // back stack, so Back returns to exactly where you were
        val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        nav.onPop = { holder.removeState(it) }
        androidx.compose.animation.SharedTransitionLayout {
        val shared = this
        AnimatedContent(nav.topEntry, transitionSpec = {
            // new pages slide in from the reading edge; going back slides them away again
            val dir = (if (nav.forward) 1 else -1) * (if (rtl) -1 else 1)
            // one easing for every page move; a short, shallow slide so it reads as direction, not as a show
            // Material "shared axis": the old page leaves quickly with a short nudge, the new one arrives a beat
            // later from 30dp away. Short distances + opacity only → GPU-cheap and smooth even while the new
            // page is still drawing its widget previews.
            val d = (30 * density).toInt() * dir
            (slideInHorizontally(androidx.compose.animation.core.tween(300, 60, Motion.ease)) { d } + fadeIn(androidx.compose.animation.core.tween(210, 90, androidx.compose.animation.core.LinearOutSlowInEasing)))
                .togetherWith(slideOutHorizontally(androidx.compose.animation.core.tween(300, easing = Motion.ease)) { -d } + fadeOut(androidx.compose.animation.core.tween(90, easing = androidx.compose.animation.core.FastOutLinearInEasing)))
                .using(androidx.compose.animation.SizeTransform(clip = false))
        }, label = "nav", contentKey = { it.id }) { entry -> val s = entry.screen
            val navScope = this
            androidx.compose.runtime.CompositionLocalProvider(LocalSharedScope provides shared, LocalNavScope provides navScope) {
            holder.SaveableStateProvider(entry.id) {
            // full window width: each screen adapts its own layout to the space (grids, split panes, reading width)
            // forms & reading pages keep a comfortable line length on big screens; catalogue/editor/wallpapers use the full width
            // only long-form text pages keep a reading measure; everything else lays itself out for the window
            // (Settings & Paywall split into panes on wide screens, the admin form gets two columns)
            val reading = s is Screen.Tutorial || s is Screen.MediaAccess || s is Screen.About
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().imePadding().displayCutoutPadding()
                .then(if (reading) Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 720.dp)
                    // the control panel is a dense form: a wide-but-bounded column keeps label and field together
                    else if (s is Screen.Admin) Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 960.dp) else Modifier)) {
              androidx.compose.runtime.CompositionLocalProvider(LocalPaneWidth provides maxWidth.value.toInt()) {
                when (s) {
                    Screen.Home -> HomeScreen(nav)
                    is Screen.Editor -> EditorScreen(nav, s)
                    is Screen.Pick -> PickScreen(nav, s)
                    is Screen.Category -> CategoryScreen(nav, s)
                    Screen.Search -> SearchScreen(nav)
                    Screen.Paywall -> PaywallScreen(nav)
                    Screen.Settings -> SettingsScreen(nav)
                    Screen.Admin -> AdminScreen(nav)
                    Screen.Tutorial -> TutorialScreen(nav)
                    Screen.MediaAccess -> MediaAccessScreen(nav)
                    Screen.About -> AboutScreen(nav)
                    Screen.Wallpapers -> WallpaperScreen(nav)
                    is Screen.SetDetail -> SetDetailScreen(nav, s.id)
                    Screen.Sets -> SetsScreen(nav)
                    Screen.Gallery -> GalleryScreen(nav)
                }
              }
            }
            }
            }
        }
        }
        WidgetAddedHost(nav)
    }
}

/** The page transition scopes, so a widget card can grow into the editor (shared element). */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
val LocalSharedScope = androidx.compose.runtime.compositionLocalOf<androidx.compose.animation.SharedTransitionScope?> { null }
val LocalNavScope = androidx.compose.runtime.compositionLocalOf<androidx.compose.animation.AnimatedVisibilityScope?> { null }

/** The widget card and the editor's preview share this key, so opening a widget morphs the card into the editor. */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedWidget(styleId: String): Modifier {
    val sts = LocalSharedScope.current ?: return this
    val avs = LocalNavScope.current ?: return this
    return with(sts) { this@sharedWidget.sharedBounds(rememberSharedContentState("w_$styleId"), avs, resizeMode = androidx.compose.animation.SharedTransitionScope.ResizeMode.RemeasureToBounds) }
}

/** After a crash: say so plainly and let the user send the details (they decide; nothing is sent automatically). */
@Composable
private fun CrashReportDialog() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var report by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.smrh.zujaj.CrashLog.take(ctx)) }
    val r = report ?: return
    ZDialog(androidx.compose.material.icons.Icons.Rounded.BugReport, tr("Sorry — Zujaj closed last time", "معلش — التطبيق قفل المرة اللي فاتت"),
        tr("Send us the technical details so we can fix it. They contain no personal data.", "ابعتلنا التفاصيل التقنية عشان نصلحها. مفيهاش أي بيانات شخصية."),
        iconColor = androidx.compose.ui.graphics.Color(0xFFFF8A4C), onDismiss = { report = null }) {
        PrimaryButton(tr("Send report", "ابعت التقرير"), androidx.compose.ui.Modifier.fillMaxWidth()) {
            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Zujaj crash").putExtra(Intent.EXTRA_TEXT, r)
            runCatching { ctx.startActivity(Intent.createChooser(send, null)) }
            report = null
        }
        androidx.compose.material3.TextButton(onClick = {
            (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("crash", r))
            android.widget.Toast.makeText(ctx, tr("Copied", "اتنسخ"), android.widget.Toast.LENGTH_SHORT).show()
            report = null
        }) { androidx.compose.material3.Text(tr("Copy", "انسخ"), color = Z.sub) }
    }
}

/**
 * The intro Google requires before a rewarded interstitial: it names the reward, counts down, and can be
 * skipped. Skipping shows nothing; letting it run (or tapping Watch) plays the ad.
 */
@Composable
private fun RewardedInterIntro() {
    val target by com.smrh.zujaj.ads.AdPolicy.offer.collectAsState()
    val t = target ?: return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val act = ctx as? android.app.Activity ?: return
    val name = remember(t) { if (t.isEmpty()) null else com.smrh.zujaj.catalog.Catalog.byId(t)?.name(Prefs.isArabicUi) }
    var left by androidx.compose.runtime.remember(t) { androidx.compose.runtime.mutableIntStateOf(5) }
    LaunchedEffect(t) {
        while (left > 0) { kotlinx.coroutines.delay(1000); left-- }
        com.smrh.zujaj.ads.AdPolicy.playRewarded(act)
    }
    val reward = if (name != null) tr("one step towards unlocking “$name”", "خطوة من فتح «$name»")
        else tr("one extra free wallpaper today", "خلفية مجانية زيادة النهارده")
    ZDialog(androidx.compose.material.icons.Icons.Rounded.CardGiftcard, tr("A short ad — with a gift", "إعلان قصير — وليك هدية"),
        tr("Watch it to the end and get $reward.", "كمّله للآخر وخد $reward."),
        onDismiss = { com.smrh.zujaj.ads.AdPolicy.declineRewarded() }) {
        PrimaryButton(tr("Watch now ($left)", "شوف دلوقتي ($left)"), androidx.compose.ui.Modifier.fillMaxWidth()) { com.smrh.zujaj.ads.AdPolicy.playRewarded(act) }
        androidx.compose.material3.TextButton(onClick = { com.smrh.zujaj.ads.AdPolicy.declineRewarded() }) {
            androidx.compose.material3.Text(tr("No thanks", "لا شكراً"), color = Z.sub)
        }
    }
}

/** "✓ Added" sheet after a widget lands on the home screen: add another, or fine-tune this one. */
@Composable
fun WidgetAddedHost(nav: Nav) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.DisposableEffect(Unit) { com.smrh.zujaj.widget.WidgetEvents.listening = true; onDispose { com.smrh.zujaj.widget.WidgetEvents.listening = false } }
    val ev by com.smrh.zujaj.widget.WidgetEvents.added.collectAsState()
    val (id, styleId) = ev ?: return
    val st = com.smrh.zujaj.catalog.Catalog.byId(styleId)
    fun close() { com.smrh.zujaj.widget.WidgetEvents.added.value = null }
    androidx.compose.runtime.LaunchedEffect(id) { Haptics.success(ctx) }
    ZDialog(Icons.Rounded.CheckCircle, tr("Added to your home screen", "اتضافت على الشاشة ✓"),
        st?.let { (if (Prefs.isArabicUi) it.ar else it.en) }, iconColor = Color(0xFF3DDC97), onDismiss = { close() }) {
        val cfg = Prefs.widgetConfig(id)
        if (cfg != null) Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(Rad.l)).background(Backdrops.brush(1)).padding(16.dp), contentAlignment = Alignment.Center) {
            WidgetPreview(cfg, if (st?.shape == com.smrh.zujaj.catalog.Shape.SMALL) Modifier.fillMaxWidth(0.5f) else Modifier.fillMaxWidth(), density = 2f)
        }
        PrimaryButton(tr("Add another widget", "ضيف ويدجت تانية"), Modifier.fillMaxWidth()) { close(); nav.reset(Screen.Home) }
        androidx.compose.material3.TextButton(onClick = { close(); nav.go(Screen.Editor(styleId, id)) }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Fine-tune this one", "عدّل دي"), color = LocalAccent.current, fontWeight = FontWeight.Bold)
        }
        androidx.compose.material3.TextButton(onClick = { close() }, modifier = Modifier.fillMaxWidth()) { Text(tr("Done", "تمام"), color = Z.sub) }
    }
}
