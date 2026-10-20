package com.smrh.zujaj

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.widget.ActionReceiver
import com.smrh.zujaj.widget.WidgetUpdater
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Runs on a real Android (emulator) device: inflates every widget exactly as a launcher would
 * (RemoteViews.apply, live TextClocks attached to a window), takes app screenshots and
 * exercises widget buttons. Results go to the app's external files dir and are pulled by CI.
 */
@RunWith(AndroidJUnit4::class)
class DeviceTest {
    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val out: File get() = File(ctx.getExternalFilesDir(null), "device").apply { mkdirs() }
    private val log = StringBuilder()

    private fun save(b: Bitmap, name: String) = FileOutputStream(File(out, "$name.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    @Test
    fun widgetsOnDevice() {
        Prefs.init(ctx)
        val density = ctx.resources.displayMetrics.density
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        val errors = StringBuilder()
        for (lang in listOf("ar", "en")) {
            Prefs.appLanguage = lang
            for (st in Catalog.all) {
                val wDp = st.shape.dw; val hDp = st.shape.dh
                val wPx = (wDp * density).toInt(); val hPx = (hDp * density).toInt()
                var bmp: Bitmap? = null
                try {
                    scenario.onActivity { a ->
                        val cfg = st.defaultConfig()
                        val rv = WidgetUpdater.build(a, 990001, cfg, st, wDp, hDp)
                        val host = FrameLayout(a).apply { setBackgroundColor(Color.rgb(48, 56, 72)) }
                        val v: View = rv.apply(a, host)
                        host.addView(v, FrameLayout.LayoutParams(wPx, hPx))
                        a.setContentView(host)
                    }
                    Thread.sleep(450)
                    scenario.onActivity { a ->
                        val host = a.findViewById<View>(android.R.id.content)
                        val v = (host as android.view.ViewGroup).getChildAt(0)
                        val b = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
                        v.draw(Canvas(b)); bmp = b
                    }
                    if (lang == "ar" || st.id.startsWith("calli") ) Unit
                    bmp?.let { save(it, "d_${st.id}_$lang") }
                } catch (t: Throwable) {
                    errors.append("${st.id} [$lang]: ${t.javaClass.simpleName}: ${t.message}\n")
                }
            }
        }
        scenario.close()
        File(out, "widget_errors.txt").writeText(errors.toString().ifEmpty { "OK\n" })
    }

    @Test
    fun appScreens() {
        Prefs.init(ctx)
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        for (lang in listOf("ar", "en")) {
            Prefs.appLanguage = lang
            Prefs.setString("bat_intro", "1")
            val i = Intent(ctx, com.smrh.zujaj.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ctx.startActivity(i)
            Thread.sleep(4500)
            ui.takeScreenshot()?.let { save(it, "app_home_$lang") }
            for (n in 2..4) {
                ui.executeShellCommand("input swipe 540 1900 540 500 400").close()
                Thread.sleep(1800)
                ui.takeScreenshot()?.let { save(it, "app_home_${lang}_$n") }
            }
            for ((k, extra) in listOf("settings" to "settings", "paywall" to "paywall")) {
                ctx.startActivity(Intent(ctx, com.smrh.zujaj.ui.MainActivity::class.java).putExtra(com.smrh.zujaj.ui.MainActivity.EXTRA_OPEN, extra).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                Thread.sleep(3000)
                ui.takeScreenshot()?.let { save(it, "app_${k}_$lang") }
                if (k == "settings") {
                    ui.executeShellCommand("input swipe 540 1900 540 500 400").close(); Thread.sleep(1500)
                    ui.takeScreenshot()?.let { save(it, "app_settings_${lang}_2") }
                }
            }
            ctx.startActivity(Intent(ctx, com.smrh.zujaj.ui.MainActivity::class.java).putExtra(com.smrh.zujaj.ui.MainActivity.EXTRA_EDIT_WIDGET, -5).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        // tablet-sized screen
        Prefs.appLanguage = "ar"
        ui.executeShellCommand("wm size 1600x2560").close(); ui.executeShellCommand("wm density 320").close()
        Thread.sleep(5000)
        ctx.startActivity(Intent(ctx, com.smrh.zujaj.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Thread.sleep(10000)
        ui.takeScreenshot()?.let { save(it, "app_tablet_home") }
        ctx.startActivity(Intent(ctx, com.smrh.zujaj.ui.MainActivity::class.java).putExtra(com.smrh.zujaj.ui.MainActivity.EXTRA_OPEN, "settings").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Thread.sleep(9000)
        ui.takeScreenshot()?.let { save(it, "app_tablet_settings") }
        ui.executeShellCommand("wm size reset").close(); ui.executeShellCommand("wm density reset").close()
        Thread.sleep(1500)
        // first-run battery dialog
        Prefs.appLanguage = "ar"; Prefs.setString("bat_intro", "")
        ctx.startActivity(Intent(ctx, com.smrh.zujaj.ui.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Thread.sleep(5000)
        ui.takeScreenshot()?.let { save(it, "app_battery_ar") }
        Prefs.setString("bat_intro", "1")
    }

    @Test
    fun widgetButtons() {
        Prefs.init(ctx)
        val r = ActionReceiver()
        fun send(id: Int, cmd: String, arg: String = "") = runCatching {
            r.onReceive(ctx, Intent(ctx, ActionReceiver::class.java).putExtra("id", id).putExtra("cmd", cmd).putExtra("arg", arg))
        }.exceptionOrNull()?.let { log.append("  ! $cmd/$arg threw ${it.javaClass.simpleName}: ${it.message}\n") }
        val id = 990002
        Prefs.saveWidgetConfig(id, Catalog.byId("tasbih_beads")!!.defaultConfig())
        repeat(3) { send(id, "tasbeeh", "inc") }
        log.append("tasbih counter after 3 taps = ${Prefs.widgetConfig(id)?.counter} (expect 3), days=${Prefs.widgetConfig(id)?.days}\n")
        Prefs.saveWidgetConfig(id, Catalog.byId("focus_card")!!.defaultConfig())
        send(id, "focus", "start")
        val left = (Prefs.widgetConfig(id)!!.targetMillis - System.currentTimeMillis()) / 60000
        log.append("focus started, minutes left = $left (expect 24-25)\n")
        send(id, "focus", "stop")
        log.append("focus stopped, target = ${Prefs.widgetConfig(id)?.targetMillis} (expect 0)\n")
        Prefs.saveWidgetConfig(id, Catalog.byId("habit_card")!!.defaultConfig())
        send(id, "habit", java.time.LocalDate.now().toEpochDay().toString())
        log.append("habit done today = ${Prefs.widgetConfig(id)?.days?.contains(java.time.LocalDate.now().toEpochDay())} (expect true)\n")
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val before = am.ringerMode
        send(id, "ringer", "vibrate")
        log.append("ringer: before=$before after vibrate=${am.ringerMode} (expect 1)\n")
        send(id, "ringer", "normal")
        log.append("ringer after normal=${am.ringerMode} (expect 2)\n")
        val v0 = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        send(id, "media", "vol_up")
        log.append("volume: $v0 -> ${am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)} after vol_up\n")
        val alarm = com.smrh.zujaj.render.Intents.alarms(ctx)
        log.append("clock intent resolves: ${alarm.resolveActivity(ctx.packageManager) != null} ($alarm)\n")
        log.append("calculator resolves: ${com.smrh.zujaj.render.Intents.calculator().resolveActivity(ctx.packageManager) != null}\n")
        File(out, "buttons.txt").writeText(log.toString())
    }
}
