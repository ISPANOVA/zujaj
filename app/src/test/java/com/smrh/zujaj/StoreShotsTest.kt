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

/** Screens for the store pictures: the main pages, and Home in every accent colour. Files ss_<name>_<lang>.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h860dp-xhdpi")
class StoreShotsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun shots() {
        Prefs.init(rule.activity)
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val list = mutableListOf(
            Triple("home", Screen.Home as Screen, 0), Triple("live", Screen.Category("LIVE"), 0), Triple("ramadan", Screen.Category("ramadan"), 0),
            Triple("editor_live", Screen.Editor("live_p_mihrab"), 0), Triple("editor_prayer", Screen.Editor("prayer_hub"), 0),
            Triple("settings", Screen.Settings, 0), Triple("wallpapers", Screen.Wallpapers, 0), Triple("cat_islamic", Screen.Category("ISLAMIC"), 0),
            Triple("cat_weather", Screen.Category("WEATHER"), 0), Triple("cat_calli", Screen.Category("CALLI"), 0),
        )
        for (a in 0 until com.smrh.zujaj.ui.Z.accents.size) list += Triple("accent$a", Screen.Home as Screen, a)
        list += Triple("settings_accent3", Screen.Settings, 3)
        rule.mainClock.autoAdvance = false
        val nav = Nav(Screen.Home)
        rule.setContent { ZujajTheme { Root(nav) } }
        for ((name, s, acc) in list) for (ar in listOf(true, false)) {
            rule.runOnUiThread {
                Prefs.appLanguage = if (ar) "ar" else "en"
                com.smrh.zujaj.ui.Z.pickAccent(acc)
                com.smrh.zujaj.ui.homeStartTab = 0; com.smrh.zujaj.ui.editorStartTab = 0
                nav.reset(s)
            }
            repeat(6) { rule.mainClock.advanceTimeBy(400); Thread.sleep(600) }
            var img: Bitmap? = null
            rule.runOnUiThread {
                val v = rule.activity.window.decorView
                img = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also { v.draw(android.graphics.Canvas(it)) }
            }
            FileOutputStream(File(out, "ss_${name}_${if (ar) "ar" else "en"}.png")).use { img!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
