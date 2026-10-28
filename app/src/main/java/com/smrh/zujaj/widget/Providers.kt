package com.smrh.zujaj.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smrh.zujaj.data.MediaRepo
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.data.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

open class BaseProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Prefs.init(context)
        appWidgetIds.forEach { runCatching { WidgetUpdater.update(context, it) } }
        Scheduler.ensure(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        Prefs.init(context)
        runCatching { WidgetUpdater.update(context, appWidgetId) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Prefs.init(context)
        appWidgetIds.forEach { Prefs.deleteWidgetConfig(it) }
    }

    override fun onEnabled(context: Context) {
        Prefs.init(context)
        Scheduler.ensure(context)
    }
}

class SmallProvider : BaseProvider()
class WideProvider : BaseProvider()
class BannerProvider : BaseProvider()
class LargeProvider : BaseProvider()
class TallProvider : BaseProvider()
class XlProvider : BaseProvider()

/** Receives the launcher callback after requestPinAppWidget succeeded. */
class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val token = intent.getStringExtra("token")
        if (id == -1) return
        Prefs.takePending(token)?.let { Prefs.saveWidgetConfig(id, it); com.smrh.zujaj.remote.Stats.widgetAdded(it.styleId) }
        runCatching { WidgetUpdater.update(context, id) }
        Toast.makeText(context, if (Prefs.isArabicUi) "تمت إضافة الويدجت ✓" else "Widget added ✓", Toast.LENGTH_SHORT).show()
    }
}

/** Button presses inside widgets. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        val id = intent.getIntExtra("id", -1)
        val cmd = intent.getStringExtra("cmd") ?: return
        val arg = intent.getStringExtra("arg") ?: ""
        when (cmd) {
            "media" -> {
                when (arg) {
                    "vol_up", "vol_down" -> {
                        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (arg == "vol_up") AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    }
                    else -> {
                        if (!MediaRepo.hasAccess(context)) {
                            context.startActivity(WidgetUpdater.appMain(context, "media_access").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            return
                        }
                        MediaRepo.command(context, arg)
                    }
                }
                if (id != -1) runCatching { WidgetUpdater.update(context, id) }
            }
            "quran" -> {
                val cfg = Prefs.widgetConfig(id) ?: return
                val (next, khatma) = com.smrh.zujaj.render.Quran.tap(cfg)
                Prefs.saveWidgetConfig(id, next)
                vibrate(context)
                if (khatma) android.widget.Toast.makeText(context, if (Prefs.isArabicUi) "ختمة مباركة ✓" else "Khatma complete ✓", android.widget.Toast.LENGTH_LONG).show()
                WidgetUpdater.update(context, id)
            }
            "athkar" -> {
                val cfg = Prefs.widgetConfig(id) ?: return
                val (next, finishedOne) = com.smrh.zujaj.render.Athkar.tap(cfg)
                Prefs.saveWidgetConfig(id, next)
                if (finishedOne) vibrate(context)
                WidgetUpdater.update(context, id)
            }
            "counter", "tasbeeh" -> {
                val cfg = Prefs.widgetConfig(id) ?: return
                val next = when (arg) {
                    "inc" -> cfg.copy(counter = cfg.counter + 1)
                    "dec" -> cfg.copy(counter = (cfg.counter - 1).coerceAtLeast(0))
                    "reset" -> cfg.copy(counter = 0)
                    else -> cfg
                }.let { c -> if (cmd == "tasbeeh" && arg == "inc") { val d = LocalDate.now().toEpochDay(); if (d in c.days) c else c.copy(days = (c.days + d).takeLast(400)) } else c }
                Prefs.saveWidgetConfig(id, next)
                if (cmd == "tasbeeh" && arg == "inc" && next.goal > 0 && next.counter % next.goal == 0) vibrate(context)
                WidgetUpdater.update(context, id)
            }
            "habit" -> {
                val cfg = Prefs.widgetConfig(id) ?: return
                val day = arg.toLongOrNull() ?: return
                if (day > LocalDate.now().toEpochDay()) return
                val days = if (cfg.days.contains(day)) cfg.days - day else (cfg.days + day).takeLast(400)
                Prefs.saveWidgetConfig(id, cfg.copy(days = days))
                WidgetUpdater.update(context, id)
            }
            "torch" -> toggleTorch(context)
            "ringer" -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val target = when (arg) {
                    "silent" -> AudioManager.RINGER_MODE_SILENT
                    "vibrate" -> AudioManager.RINGER_MODE_VIBRATE
                    "normal" -> AudioManager.RINGER_MODE_NORMAL
                    else -> when (am.ringerMode) { AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE; AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT; else -> AudioManager.RINGER_MODE_NORMAL }
                }
                val ok = runCatching { am.ringerMode = target }.isSuccess && am.ringerMode == target
                if (!ok) {
                    // silent needs "Do not disturb" access on newer Android
                    runCatching { context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    Toast.makeText(context, if (Prefs.isArabicUi) "اسمح لزجاج بالوصول لوضع عدم الإزعاج" else "Allow Zujaj Do Not Disturb access", Toast.LENGTH_LONG).show()
                }
                WidgetUpdater.updateAll(context)
            }
            "focus" -> {
                val cfg = Prefs.widgetConfig(id) ?: return
                val next = if (arg == "start") cfg.copy(targetMillis = System.currentTimeMillis() + cfg.goal.coerceIn(1, 180) * 60_000L) else cfg.copy(targetMillis = 0L)
                Prefs.saveWidgetConfig(id, next)
                WidgetUpdater.update(context, id)
            }
        }
    }

    private fun vibrate(ctx: Context) = runCatching {
        val v = if (android.os.Build.VERSION.SDK_INT >= 31)
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        else ctx.getSystemService(android.os.Vibrator::class.java)
        v.vibrate(android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    }

    companion object {
        @Volatile private var torchOn = false
        fun toggleTorch(ctx: Context) = runCatching {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } ?: return@runCatching
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
        }
    }
}

/** Refresh on system events and on our own alarms. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED, Intent.ACTION_POWER_DISCONNECTED -> WidgetUpdater.updateCategory(context, "system")
            Scheduler.MINUTE -> {
                // redraw only widgets whose time is drawn into the picture (they reschedule the tick)
                WidgetUpdater.allIds(context).forEach { id ->
                    val cfg = Prefs.widgetConfig(id) ?: return@forEach
                    if (WidgetUpdater.bakes(context, id, cfg)) runCatching { WidgetUpdater.update(context, id) }
                }
                return
            }
            Scheduler.PRO_END -> { com.smrh.zujaj.billing.Billing.refreshPro(); WidgetUpdater.updateAll(context) }
            else -> WidgetUpdater.updateAll(context)
        }
        Scheduler.ensure(context)
        // after a restart / update: refresh location & weather right away instead of waiting for the next cycle
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) Scheduler.refreshNow(context)
    }
}

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Prefs.init(applicationContext)
        // auto location: back after a restart, and follows the user when they travel
        if (runCatching { com.smrh.zujaj.data.LocationRepo.refreshQuietly(applicationContext) }.getOrDefault(false)) {
            Scheduler.scheduleAlarms(applicationContext); Scheduler.refreshWeatherIfNeeded(applicationContext, force = true)
        }
        Scheduler.refreshWeatherIfNeeded(applicationContext)
        runCatching { com.smrh.zujaj.data.Reminders.check(applicationContext) }
        // Pro: re-check the code and the Play subscription every few hours (without opening the app)
        val now = System.currentTimeMillis()
        if (now - Prefs.getString("pro_check").toLongOrNull().let { it ?: 0L } > 3 * 3600_000L) {
            Prefs.setString("pro_check", now.toString())
            runCatching { com.smrh.zujaj.remote.Codes.revalidate(applicationContext) }
            runCatching { com.smrh.zujaj.billing.Billing.recheck() }
        }
        com.smrh.zujaj.billing.Billing.refreshPro()
        WidgetUpdater.updateAll(applicationContext)
        Scheduler.scheduleAlarms(applicationContext)
        Result.success()
    }
}

object Scheduler {
    private const val TICK = "com.smrh.zujaj.TICK"
    const val MINUTE = "com.smrh.zujaj.MINUTE"
    const val PRO_END = "com.smrh.zujaj.PRO_END"

    /** Next whole minute (non-waking: only delivered while the phone is awake, e.g. on the lock screen). */
    fun minuteTick(ctx: Context) = runCatching {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        // right on the minute (a few ms after, so the new minute is drawn)
        val t = now - now % 60_000 + 60_000 + 40
        val pi = PendingIntent.getBroadcast(ctx, 8, Intent(ctx, SystemEventsReceiver::class.java).setAction(MINUTE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (exactAllowed(ctx)) am.setExact(AlarmManager.RTC, t, pi) else am.setWindow(AlarmManager.RTC, t, 1_000, pi)
    }

    /** "Alarms & reminders" access lets clock widgets change exactly on the minute. */
    fun exactAllowed(ctx: Context): Boolean = android.os.Build.VERSION.SDK_INT < 31 ||
        (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    fun askExact(ctx: Context) = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 31) ctx.startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            android.net.Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun ensure(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("zujaj_refresh", ExistingPeriodicWorkPolicy.KEEP, req)
        scheduleAlarms(ctx)
        // re-arm the minute tick if the chain broke (process killed by the phone)
        if (WidgetUpdater.allIds(ctx).any { id -> Prefs.widgetConfig(id)?.let { WidgetUpdater.bakes(ctx, id, it) } == true }) minuteTick(ctx)
    }

    fun refreshNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork("zujaj_now", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RefreshWorker>().build())
    }

    fun refreshWeatherIfNeeded(ctx: Context, force: Boolean = false) {
        val main = WeatherRepo.main()
        if (force || main == null || System.currentTimeMillis() - main.updated > 40 * 60_000L) {
            WeatherRepo.fetch(Prefs.cityName, Prefs.lat, Prefs.lng)
        }
        // secondary cities used by widgets
        WidgetUpdater.allIds(ctx).mapNotNull { Prefs.widgetConfig(it) }.flatMap { it.cities }.distinctBy { "${it.lat},${it.lng}" }
            .filter { it.lat != 0.0 || it.lng != 0.0 }
            .forEach { c ->
                val w = WeatherRepo.cached(c.lat, c.lng)
                if (force || w == null || System.currentTimeMillis() - w.updated > 40 * 60_000L) WeatherRepo.fetch(c.name, c.lat, c.lng)
            }
    }

    /** Wake at the next prayer time and at midnight to redraw. */
    fun scheduleAlarms(ctx: Context) = runCatching {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val next = PrayerRepo.today().next.time
        val midnight = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 5_000
        val t = minOf(next + 2_000, midnight)
        val pi = PendingIntent.getBroadcast(ctx, 7, Intent(ctx, SystemEventsReceiver::class.java).setAction(TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setAndAllowWhileIdle(AlarmManager.RTC, t, pi)
        // lock Pro widgets the moment a redeem code's time runs out
        val until = Prefs.codeProUntil
        val pe = PendingIntent.getBroadcast(ctx, 9, Intent(ctx, SystemEventsReceiver::class.java).setAction(PRO_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (until > System.currentTimeMillis() && until != Long.MAX_VALUE) am.setAndAllowWhileIdle(AlarmManager.RTC, until + 1_000, pe) else am.cancel(pe)
    }
}
