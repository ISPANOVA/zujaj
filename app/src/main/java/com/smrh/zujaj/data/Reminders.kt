package com.smrh.zujaj.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Sunnah fasting reminders, sent the evening before (from 8 pm, checked by the periodic refresh):
 * Monday & Thursday, the white days (13–15 of each Hijri month), the day of Arafah (9 Dhu al-Hijjah)
 * and Ashura with Tasu'a (9 & 10 Muharram). Each kind can be switched on or off in Settings.
 */
object Reminders {
    enum class Kind(val key: String, val en: String, val ar: String) {
        MON_THU("rem_monthu", "Monday & Thursday fasting", "صيام الإتنين والخميس"),
        WHITE("rem_white", "The white days (13–15)", "الأيام البيض (13–15)"),
        ARAFAH("rem_arafah", "Day of Arafah", "يوم عرفة"),
        ASHURA("rem_ashura", "Tasu'a & Ashura", "تاسوعاء وعاشوراء"),
    }
    private const val CH = "reminders"

    fun on(k: Kind) = Prefs.getString(k.key, if (k == Kind.ARAFAH || k == Kind.ASHURA) "1" else "0") == "1"
    fun set(k: Kind, v: Boolean) = Prefs.setString(k.key, if (v) "1" else "0")
    fun anyOn() = Kind.values().any { on(it) }

    /** What tomorrow is, if anything we remind about (first match wins: the rarer day first). */
    fun tomorrow(today: LocalDate = LocalDate.now()): Pair<Kind, Int>? {
        val t = today.plusDays(1); val h = Calendars.hijri(t)
        return when {
            h.month == 12 && h.day == 9 -> Kind.ARAFAH to 0
            h.month == 1 && (h.day == 9 || h.day == 10) -> Kind.ASHURA to h.day
            h.month != 9 && h.day in 13..15 -> Kind.WHITE to h.day
            h.month != 9 && (t.dayOfWeek == DayOfWeek.MONDAY || t.dayOfWeek == DayOfWeek.THURSDAY) -> Kind.MON_THU to t.dayOfWeek.value
            else -> null
        }
    }

    /** Called by the periodic refresh. Sends at most one reminder per evening. */
    fun check(ctx: Context) {
        if (!anyOn() || LocalTime.now().hour < 20) return
        val today = LocalDate.now()
        val stamp = today.toEpochDay().toString()
        if (Prefs.getString("rem_last") == stamp) return
        val (k, d) = tomorrow(today) ?: return
        if (!on(k)) return
        Prefs.setString("rem_last", stamp)
        notify(ctx, k, d)
    }

    private fun notify(ctx: Context, k: Kind, d: Int) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val ar = Prefs.isArabicUi
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CH) == null)
            nm.createNotificationChannel(NotificationChannel(CH, if (ar) "تذكير الصيام" else "Fasting reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val title = when (k) {
            Kind.MON_THU -> if (ar) (if (d == 1) "بكرة الإتنين" else "بكرة الخميس") else (if (d == 1) "Tomorrow is Monday" else "Tomorrow is Thursday")
            Kind.WHITE -> if (ar) "بكرة من الأيام البيض" else "Tomorrow is one of the white days"
            Kind.ARAFAH -> if (ar) "بكرة يوم عرفة" else "Tomorrow is the day of Arafah"
            Kind.ASHURA -> if (ar) (if (d == 10) "بكرة عاشوراء" else "بكرة تاسوعاء") else (if (d == 10) "Tomorrow is Ashura" else "Tomorrow is Tasu'a")
        }
        val body = if (ar) "تذكير بالصيام — ما تنساش السحور." else "A reminder to fast — don't miss suhoor."
        val open = PendingIntent.getActivity(ctx, 41, Intent(ctx, com.smrh.zujaj.ui.AppActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(com.smrh.zujaj.R.drawable.ic_mono)
            .setContentTitle(title).setContentText(body).setContentIntent(open).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(ctx).notify(4100 + k.ordinal, n) }
    }
}
