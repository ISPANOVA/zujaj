package com.smrh.zujaj.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.render.PrayerTrack
import java.io.File
import java.time.LocalDate

/** The Prayer Tracker's week as a square picture, shared through the system sheet. */
object WeekShare {
    fun share(ctx: Context) {
        val ar = Prefs.isArabicUi
        val week = PrayerTrack.week(); val n = week.sum()
        val s = 1080
        val b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawRect(0f, 0f, s.toFloat(), s.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, s.toFloat(), s.toFloat(), 0xFF0F1A2E.toInt(), 0xFF1E1240.toInt(), Shader.TileMode.CLAMP) })
        val rnd = java.util.Random(5)
        repeat(70) { c.drawCircle(rnd.nextFloat() * s, rnd.nextFloat() * s, 1.5f + rnd.nextFloat() * 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); alpha = 40 + rnd.nextInt(120) }) }
        fun p(size: Float, col: Int, bold: Boolean = true) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size; color = col; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        c.drawText(if (ar) "أسبوعي مع الصلاة" else "My prayer week", s / 2f, 190f, p(54f, 0xFFE7B75F.toInt()))
        c.drawText("$n / 35", s / 2f, 430f, p(190f, 0xFFFFFFFF.toInt()))
        c.drawText(if (ar) "صلاة في آخر ٧ أيام 🔥" else "prayers in the last 7 days 🔥", s / 2f, 520f, p(46f, 0xCCFFFFFF.toInt(), false))
        // seven days: a column of five dots each, today last
        val days = if (ar) week.reversed() else week
        val dates = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }.let { if (ar) it.reversed() else it }
        val left = 150f; val cw = (s - 2 * left) / 7
        days.forEachIndexed { i, k ->
            val x = left + cw * (i + 0.5f)
            for (j in 0 until 5) c.drawCircle(x, 640f + j * 52f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (j < k) 0xFF3DDC97.toInt() else 0x33FFFFFF })
            val dn = dates[i].dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, if (ar) java.util.Locale("ar") else java.util.Locale.ENGLISH)
            c.drawText(dn, x, 940f, p(30f, 0x99FFFFFF.toInt(), false))
        }
        c.drawText(if (ar) "زجاج · Zujaj" else "Zujaj · زجاج", s / 2f, 1030f, p(34f, 0x88FFFFFF.toInt()))
        val f = File(File(ctx.cacheDir, "share").apply { mkdirs() }, "week.png")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".share", f)
        val text = if (ar) "صلّيت $n من 35 صلاة الأسبوع ده 🤍 — من ويدجت متابعة الصلوات في زجاج" else "I prayed $n of 35 prayers this week 🤍 — with Zujaj's Prayer Tracker"
        val i = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_TEXT, text).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        ctx.startActivity(Intent.createChooser(i, if (ar) "شارك أسبوعك" else "Share your week").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
