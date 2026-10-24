package com.smrh.zujaj.ui

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
}

/** One back-stack entry; [id] is unique so each entry keeps its own saved UI state (tab, filters, scroll…). */
data class Entry(val screen: Screen, val id: Long)

class Nav(start: Screen) {
    private var next = 1L
    val entries = mutableStateListOf(Entry(start, 0L))
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
        else -> s::class.simpleName ?: "Home"
    }
    private fun dec(t: String): Screen? = runCatching {
        val p = t.split('\u0002')
        when (p[0]) {
            "E" -> Screen.Editor(p[1], p[2].toInt(), p[3].toBoolean(), p.getOrElse(4) { "" })
            "P" -> Screen.Pick(Shape.valueOf(p[1]), p[2].toInt())
            "C" -> Screen.Category(p[1])
            "Home" -> Screen.Home; "Search" -> Screen.Search; "Paywall" -> Screen.Paywall; "Settings" -> Screen.Settings
            "Admin" -> Screen.Admin; "Tutorial" -> Screen.Tutorial; "MediaAccess" -> Screen.MediaAccess
            "About" -> Screen.About; "Wallpapers" -> Screen.Wallpapers
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
            val icon = v.iconView
            v.view.animate().alpha(0f).setDuration(260).withEndAction { v.remove() }.start()
            icon.animate().scaleX(0.82f).scaleY(0.82f).setDuration(260)
                .setInterpolator(android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f)).start()
        }
        Prefs.init(this)
        Z.refresh(this)
        bars()
        super.onCreate(savedInstanceState)
        runCatching { com.smrh.zujaj.ads.Ads.start(this) }
        nav = Nav(startScreen(intent))
        // back from the background after the system reclaimed memory: same pages, same order, same state
        if (savedInstanceState != null) nav.restore(savedInstanceState.getStringArrayList("nav"))
        if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_CONFIGURE) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, configureId))
        }
        setContent {
            val ver by Prefs.version.collectAsState()
            key(ver.coerceAtMost(0), Prefs.isArabicUi) {
                ZujajTheme {
                    val light = Z.light
                    LaunchedEffect(light) { bars() }
                    Root(nav)
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
        val s = startScreen(intent)
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

@Composable
fun Root(nav: Nav) {
    BackHandler(enabled = nav.stack.size > 1) { nav.back() }
    AppBackdrop {
        val rtl = Prefs.isArabicUi
        // every screen keeps its remembered state (tab, chips, filters, scroll position) while it is in the
        // back stack, so Back returns to exactly where you were
        val holder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
        nav.onPop = { holder.removeState(it) }
        AnimatedContent(nav.topEntry, transitionSpec = {
            // new pages slide in from the reading edge; going back slides them away again
            val dir = (if (nav.forward) 1 else -1) * (if (rtl) -1 else 1)
            // one easing for every page move; a short, shallow slide so it reads as direction, not as a show
            (slideInHorizontally(Motion.std(Motion.SLOW)) { w -> minOf(w / 5, 160) * dir } + fadeIn(Motion.std(Motion.MED)))
                .togetherWith(slideOutHorizontally(Motion.std(Motion.SLOW)) { w -> -minOf(w / 10, 80) * dir } + fadeOut(Motion.std(Motion.FAST)))
        }, label = "nav", contentKey = { it.id }) { entry -> val s = entry.screen
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
                }
              }
            }
            }
        }
    }
}
