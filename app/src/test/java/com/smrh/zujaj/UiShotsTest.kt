package com.smrh.zujaj

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.ui.Nav
import com.smrh.zujaj.ui.Root
import com.smrh.zujaj.ui.Screen
import com.smrh.zujaj.ui.ZujajTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h860dp-xhdpi")
open class UiShotsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    open val tag = ""

    @Test
    fun screens() {
        Prefs.init(rule.activity)
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val screens = listOf(
            "home" to Screen.Home,
            "editor" to Screen.Editor("prayer_grid"),
            "editor_small" to Screen.Editor("clock_dots"),
            "cat_clock" to Screen.Category("CLOCK"),
            "cat_prayer" to Screen.Category("PRAYER"),
            "editor_arabesque" to Screen.Editor("clock_arabesque"),
            "paywall" to Screen.Paywall,
            "settings" to Screen.Settings,
            "tutorial" to Screen.Tutorial,
            "editor_font" to Screen.Editor("digital_stacked"),
            "search" to Screen.Search,
            "wallpapers" to Screen.Wallpapers,
        )
        rule.mainClock.autoAdvance = false
        val nav = Nav(Screen.Home)
        rule.setContent { ZujajTheme { Root(nav) } }
        for ((name, s) in screens) {
            for (ar in listOf(false, true)) {
                rule.runOnUiThread {
                    Prefs.appLanguage = if (ar) "ar" else "en"
                    com.smrh.zujaj.ui.editorStartTab = if (name == "editor_font") 99 else 0
                    nav.reset(s)
                }
                repeat(6) {
                    rule.mainClock.advanceTimeBy(400)
                    Thread.sleep(700)
                }
                var img: Bitmap? = null
                rule.runOnUiThread {
                    val v = rule.activity.window.decorView
                    val b = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    v.draw(android.graphics.Canvas(b))
                    img = b
                }
                FileOutputStream(File(out, "ui${tag}_${name}_${if (ar) "ar" else "en"}.png")).use { img!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}


/** Tablet, landscape. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1280dp-h800dp-land-mdpi")
class UiShotsTestTablet : UiShotsTest() { override val tag = "_tab" }

/** Foldable, unfolded (near-square). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w840dp-h900dp-mdpi")
class UiShotsTestFold : UiShotsTest() { override val tag = "_fold" }

/** Compact phone (small 5-inch class). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h640dp-xhdpi")
class UiShotsTestCompact : UiShotsTest() { override val tag = "_compact" }

/** Large phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w430dp-h932dp-xhdpi")
class UiShotsTestLarge : UiShotsTest() { override val tag = "_large" }

/** Tablet, portrait. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w800dp-h1280dp-port-mdpi")
class UiShotsTestTabletPort : UiShotsTest() { override val tag = "_tabp" }
