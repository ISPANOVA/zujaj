package com.smrh.zujaj.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.smrh.zujaj.data.Prefs

/**
 * Gradient clocks are pictures the app redraws itself. Alarms alone are not enough on phones that freeze apps in
 * the background (Realme/OPPO/OnePlus ColorOS, Xiaomi HyperOS, vivo…): the system delays them by minutes.
 * This small foreground service keeps the app awake enough to hear the system's own minute tick (ACTION_TIME_TICK,
 * sent only to running apps) and screen-on, and redraws the gradient widgets at once. It runs only while at least
 * one such widget is on the home screen, and stops itself otherwise. Its notification sits in a silent channel.
 */
class ClockService : Service() {
    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            Prefs.init(c)
            // screen on: redraw at once (also brings back lock-screen widgets a host may have dropped)
            if (i.action == Intent.ACTION_SCREEN_ON || i.action == Intent.ACTION_USER_PRESENT) Scheduler.drawnMinute = -1
            if (!redraw(c)) stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        val n = notification(this)
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTE_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTE_ID, n)
        }.onFailure { stopSelf(); return }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(rx, f)
        redraw(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // every start through startForegroundService must be answered with startForeground
        runCatching { if (Build.VERSION.SDK_INT >= 34) startForeground(NOTE_ID, notification(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(NOTE_ID, notification(this)) }
        running = true
        if (!needed() || !redraw(this)) { running = false; stopSelf() }
        return START_STICKY
    }

    override fun onDestroy() { running = false; runCatching { unregisterReceiver(rx) }; super.onDestroy() }

    companion object {
        private const val NOTE_ID = 7301
        private const val CHANNEL = "clock_keeper"
        @Volatile var running = false

        /**
         * Only phones that freeze apps in the background need the keeper (and its notification); elsewhere
         * (Samsung, Pixel, Motorola, Nokia…) the 5-second alarm keeps the time on the minute by itself.
         */
        fun needed(): Boolean {
            val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
            return listOf("realme", "oppo", "oneplus", "xiaomi", "redmi", "poco", "vivo", "iqoo", "huawei", "honor", "infinix", "tecno", "itel", "meizu").any { it in m }
        }

        /** Widgets whose time is a picture: gradient clocks, and every lock-screen widget (lock screens ignore app fonts). */
        private fun bakedIds(ctx: Context) = WidgetUpdater.allIds(ctx).filter { id -> Prefs.widgetConfig(id)?.let { WidgetUpdater.bakes(ctx, id, it) } == true }

        /** Redraws the gradient clocks that show a past minute. Returns false when there are none on the home screen. */
        fun redraw(ctx: Context): Boolean {
            val ids = bakedIds(ctx)
            if (ids.isEmpty()) return false
            val minute = System.currentTimeMillis() / 60_000
            if (minute != Scheduler.drawnMinute) {
                ids.forEach { runCatching { WidgetUpdater.update(ctx, it) } }
                Scheduler.drawnMinute = minute
            }
            return true
        }

        /** Starts the keeper when a gradient clock is on the home screen (allowed from the background because the app is battery-unrestricted). */
        fun ensure(ctx: Context) = runCatching {
            if (running || !needed() || bakedIds(ctx).isEmpty()) return@runCatching
            val i = Intent(ctx, ClockService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }

        private fun notification(ctx: Context): Notification {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            val ar = Prefs.isArabicUi
            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL) == null)
                nm.createNotificationChannel(NotificationChannel(CHANNEL, if (ar) "ساعة الويدجت" else "Clock widgets", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false); description = if (ar) "بيخلّي ساعة الويدجت مظبوطة بالدقيقة" else "Keeps clock widgets on the minute"
                })
            val open = PendingIntent.getActivity(ctx, 77, Intent(ctx, com.smrh.zujaj.ui.AppActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(ctx, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(ctx)
            return b.setSmallIcon(com.smrh.zujaj.R.drawable.ic_mono)
                .setContentTitle(if (ar) "ساعة الويدجت مظبوطة" else "Clock widgets on time")
                .setContentText(if (ar) "زجاج بيحدّث الساعة المتدرّجة كل دقيقة" else "Zujaj redraws your gradient clocks every minute")
                .setOngoing(true).setContentIntent(open)
                .apply { if (Build.VERSION.SDK_INT >= 31) setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) }
                .build()
        }
    }
}
