package com.smrh.zujaj.ui

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

class Nav(start: Screen) {
    val stack = mutableStateListOf(start)
    val top get() = stack.last()
    /** Direction of the last move, for the page transition. */
    var forward = true
        private set
    fun go(s: Screen) { forward = true; stack.add(s) }
    fun back(): Boolean = if (stack.size > 1) { forward = false; stack.removeAt(stack.lastIndex); true } else false
    fun replace(s: Screen) { stack[stack.lastIndex] = s }
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
            key(ver.coerceAtMost(0), Prefs.isArabicUi, Prefs.accentIndex) {
                ZujajTheme {
                    val light = Z.light
                    LaunchedEffect(light) { bars() }
                    Root(nav)
                    if (configureId == AppWidgetManager.INVALID_APPWIDGET_ID) AppGates(nav)
                    if (configureId == AppWidgetManager.INVALID_APPWIDGET_ID) OnboardingScreen()
                    Splash(show = configureId == AppWidgetManager.INVALID_APPWIDGET_ID && savedInstanceState == null)
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
        AnimatedContent(nav.top, transitionSpec = {
            // new pages slide in from the reading edge; going back slides them away again
            val dir = (if (nav.forward) 1 else -1) * (if (rtl) -1 else 1)
            val ease = androidx.compose.animation.core.FastOutSlowInEasing
            (slideInHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { w -> w / 4 * dir } + fadeIn(androidx.compose.animation.core.tween(260, 40)))
                .togetherWith(slideOutHorizontally(androidx.compose.animation.core.tween(340, easing = ease)) { w -> -w / 8 * dir } +
                    fadeOut(androidx.compose.animation.core.tween(180)) + scaleOut(androidx.compose.animation.core.tween(340), targetScale = 0.97f))
        }, label = "nav") { s ->
            // tablets & big screens: keep the layout at phone-like width, centred, so nothing stretches
            Box(Modifier.fillMaxSize().statusBarsPadding().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 720.dp)) {
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
