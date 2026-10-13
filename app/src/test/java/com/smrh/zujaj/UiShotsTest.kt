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
class UiShotsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun screens() {
        Prefs.init(rule.activity)
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val screens = listOf(
            "home" to Screen.Home,
            "editor" to Screen.Editor("prayer_grid"),
            "editor_small" to Screen.Editor("clock_dots"),
            "paywall" to Screen.Paywall,
            "settings" to Screen.Settings,
            "tutorial" to Screen.Tutorial,
        )
        val nav = Nav(Screen.Home)
        rule.setContent { ZujajTheme { Root(nav) } }
        for ((name, s) in screens) {
            for (ar in listOf(false, true)) {
                rule.runOnUiThread {
                    Prefs.appLanguage = if (ar) "ar" else "en"
                    nav.stack.clear(); nav.stack.add(s)
                }
                rule.mainClock.advanceTimeBy(1500)
                rule.waitForIdle()
                Thread.sleep(400)
                rule.mainClock.advanceTimeBy(500)
                val img = rule.onRoot().captureToImage().asAndroidBitmap()
                FileOutputStream(File(out, "ui_${name}_${if (ar) "ar" else "en"}.png")).use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
