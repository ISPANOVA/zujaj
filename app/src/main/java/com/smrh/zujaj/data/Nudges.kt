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
 * Two gentle notifications, checked by the periodic refresh:
 * - once ever, 3+ days after install, if only one widget (or none) is on the screen: "try the prayer beads";
 * - Friday evenings, for people using the Prayer Tracker: this week's count, tap to share it as a picture.
 */
object Nudges {
    private const val CH = "tips"

    fun check(ctx: Context) {
        val today = LocalDate.now().toEpochDay()
        val first = Prefs.getString("first_day").toLongOrNull() ?: today.also { Prefs.setString("first_day", it.toString()) }
        val hour = LocalTime.now().hour
        val ids = com.smrh.zujaj.widget.WidgetUpdater.allIds(ctx)
        val ar = Prefs.isArabicUi
        if (Prefs.getString("nudge_beads") != "1" && today - first >= 3 && hour in 12..21 && ids.size <= 1) {
            Prefs.setString("nudge_beads", "1")
            notify(ctx, 4201, if (ar) "📿 جرّب ويدجت المسبحة" else "📿 Try the Prayer Beads widget",
                if (ar) "سبّح من على الشاشة الرئيسية بلمسة، والموبايل يهزّ مع كل ٣٣." else "Count your tasbeeh right on the home screen — a buzz at every 33.",
                "style:sig_beads")
        }
        val tracker = ids.any { Prefs.widgetConfig(it)?.styleId == "sig_tracker" }
        if (tracker && LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY && hour >= 21 && Prefs.getString("week_sent") != today.toString()) {
            val n = com.smrh.zujaj.render.PrayerTrack.week().sum()
            if (n > 0) {
                Prefs.setString("week_sent", today.toString())
                notify(ctx, 4202, if (ar) "صلّيت $n من 35 الأسبوع ده 🔥" else "You prayed $n of 35 this week 🔥",
                    if (ar) "اضغط عشان تشارك أسبوعك" else "Tap to share your week", "week_share")
            }
        }
    }

    /** The control panel's campaign, once per phone. */
    suspend fun campaign(ctx: Context) {
        val c = runCatching { com.smrh.zujaj.remote.Campaign.get() }.getOrNull() ?: return
        if (c.until in 1 until System.currentTimeMillis() || Prefs.getString("camp_seen") == c.id.toString() || c.title.isBlank()) return
        Prefs.setString("camp_seen", c.id.toString())
        notify(ctx, 4203, c.title, c.body, c.open.ifBlank { "home" })
    }

    private fun notify(ctx: Context, id: Int, title: String, body: String, open: String) {
        if (Build.VERSION.SDK_INT >= 33 && ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CH) == null)
            nm.createNotificationChannel(NotificationChannel(CH, if (Prefs.isArabicUi) "اقتراحات وملخصات" else "Tips & summaries", NotificationManager.IMPORTANCE_DEFAULT))
        val i = Intent(ctx, com.smrh.zujaj.ui.AppActivity::class.java).putExtra("open", open).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(ctx, id, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, CH).setSmallIcon(com.smrh.zujaj.R.drawable.ic_mono)
            .setContentTitle(title).setContentText(body).setContentIntent(pi).setAutoCancel(true).build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }
}
