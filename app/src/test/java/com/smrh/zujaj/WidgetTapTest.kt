package com.smrh.zujaj

import android.app.Application
import android.content.Intent
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.ui.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Opening the app the way a home-screen widget does (cold start, warm start, after recreation). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w400dp-h860dp-xhdpi")
class WidgetTapTest {
    private fun ctx() = org.robolectric.RuntimeEnvironment.getApplication()

    private fun editIntent(id: Int) = Intent(ctx(), MainActivity::class.java)
        .setAction("zujaj.EDIT_$id").putExtra(MainActivity.EXTRA_EDIT_WIDGET, id)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    private fun settle() { repeat(20) { ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS) } }

    @Test fun coldStartFromWidgetEdit() {
        Prefs.init(ctx())
        Prefs.saveWidgetConfig(5, Catalog.byId("clock_dots")!!.defaultConfig())
        val c = Robolectric.buildActivity(MainActivity::class.java, editIntent(5)).setup()
        settle()
        c.pause().stop().destroy()
    }

    @Test fun coldStartFromWidgetOpen() {
        Prefs.init(ctx())
        val i = Intent(ctx(), MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, "tutorial").setAction("zujaj.OPEN.tutorial")
        Robolectric.buildActivity(MainActivity::class.java, i).setup(); settle()
        Robolectric.buildActivity(MainActivity::class.java, Intent(ctx(), MainActivity::class.java)).setup(); settle()
    }

    @Test fun warmStartAndRecreate() {
        Prefs.init(ctx())
        Prefs.saveWidgetConfig(6, Catalog.byId("prayer_grid")!!.defaultConfig())
        val c = Robolectric.buildActivity(MainActivity::class.java, Intent(ctx(), MainActivity::class.java)).setup()
        settle()
        c.newIntent(editIntent(6)); settle()
        c.recreate(); settle()
    }
}
