package com.smrh.zujaj

import android.app.Application
import kotlinx.coroutines.launch
import com.smrh.zujaj.billing.Billing
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.widget.Scheduler

class ZujajApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        Prefs.init(this)
        Flags.init(this)
        Billing.init(this)
        com.smrh.zujaj.ads.Ads.init(this)
        com.smrh.zujaj.ads.AdUnlock.init(this)
        runCatching { Scheduler.ensure(this) }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.smrh.zujaj.remote.Codes.revalidate(this@ZujajApp)
            com.smrh.zujaj.remote.Codes.watch(this@ZujajApp)
        }
    }
}

/**
 * Keeps the last crash (stack trace + version + what opened the app) in a private file, so the next
 * launch can offer to send it. Nothing leaves the phone unless the user taps "Send".
 */
object CrashLog {
    private const val FILE = "last_crash.txt"
    @Volatile var lastIntent: String = ""
    fun install(app: Application) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val sw = java.io.StringWriter(); e.printStackTrace(java.io.PrintWriter(sw))
                java.io.File(app.filesDir, FILE).writeText(
                    "Zujaj ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                        "thread=${t.name} opened-by=$lastIntent\n\n" + sw.toString().take(12000)
                )
            }
            prev?.uncaughtException(t, e)
        }
    }
    fun take(ctx: android.content.Context): String? {
        val f = java.io.File(ctx.filesDir, FILE)
        if (!f.exists()) return null
        return runCatching { f.readText() }.getOrNull().also { f.delete() }
    }
}
