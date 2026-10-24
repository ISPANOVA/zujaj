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
    fun go(s: Screen) {
        // ignore double taps that would open the same screen twice
        if (entries.last().screen == s) return
        forward = true; entries.add(Entry(s, next++))
    }
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
            val ease = androidx.compose.animation.core.FastOutSlowInEasing
            (slideInHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { w -> w / 4 * dir } + fadeIn(androidx.compose.animation.core.tween(260, 40)))
                .togetherWith(slideOutHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { w -> -w / 8 * dir } +
                    fadeOut(androidx.compose.animation.core.tween(180)) + scaleOut(androidx.compose.animation.core.tween(340), targetScale = 0.97f))
        }, label = "nav", contentKey = { it.id }) { entry -> val s = entry.screen
            holder.SaveableStateProvider(entry.id) {
            // full window width: each screen adapts its own layout to the space (grids, split panes, reading width)
            // forms & reading pages keep a comfortable line length on big screens; catalogue/editor/wallpapers use the full width
            val reading = s is Screen.Settings || s is Screen.Paywall || s is Screen.Admin || s is Screen.Tutorial || s is Screen.MediaAccess || s is Screen.About
            Box(Modifier.fillMaxSize().statusBarsPadding().imePadding().displayCutoutPadding()
                .then(if (reading) Modifier.wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 720.dp) else Modifier)) {
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
