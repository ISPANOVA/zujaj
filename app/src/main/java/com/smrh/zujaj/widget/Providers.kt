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

/** Receives the launcher callback after requestPinAppWidget succeeded. */
class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val token = intent.getStringExtra("token")
        if (id == -1) return
        Prefs.takePending(token)?.let { Prefs.saveWidgetConfig(id, it) }
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
            "counter", "tasbeeh" -> {
                val cfg = Prefs.widgetConfig(id) ?: return
                val next = when (arg) {
                    "inc" -> cfg.copy(counter = cfg.counter + 1)
                    "dec" -> cfg.copy(counter = (cfg.counter - 1).coerceAtLeast(0))
                    "reset" -> cfg.copy(counter = 0)
                    else -> cfg
                }
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
                    if (WidgetUpdater.needsBake(context, id, cfg)) runCatching { WidgetUpdater.update(context, id) }
                }
                return
            }
            else -> WidgetUpdater.updateAll(context)
        }
        Scheduler.ensure(context)
    }
}

class RefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Prefs.init(applicationContext)
        Scheduler.refreshWeatherIfNeeded(applicationContext)
        WidgetUpdater.updateAll(applicationContext)
        Scheduler.scheduleAlarms(applicationContext)
        Result.success()
    }
}

object Scheduler {
    private const val TICK = "com.smrh.zujaj.TICK"
    const val MINUTE = "com.smrh.zujaj.MINUTE"

    /** Next whole minute (non-waking: only delivered while the phone is awake, e.g. on the lock screen). */
    fun minuteTick(ctx: Context) = runCatching {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val t = now - now % 60_000 + 60_500
        val pi = PendingIntent.getBroadcast(ctx, 8, Intent(ctx, SystemEventsReceiver::class.java).setAction(MINUTE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.set(AlarmManager.RTC, t, pi)
    }

    fun ensure(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<RefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("zujaj_refresh", ExistingPeriodicWorkPolicy.KEEP, req)
        scheduleAlarms(ctx)
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
    }
}
