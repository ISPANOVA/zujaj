package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.PrayerRepo
import java.time.LocalDate

/**
 * Seasons: days to Ramadan, Eid al-Fitr and Eid al-Adha. During Ramadan it turns into the day's
 * imsakiya (suhoor / iftar with a live countdown); on the Eid days it says Eid Mubarak.
 * Colours come from the user's text/accent colours and the background from the editor, like any widget.
 */
object SeasonR {
    data class Ev(val en: String, val ar: String, val days: Int, val date: LocalDate)

    /** The three occasions with days left (searching up to ~13 months ahead). */
    fun events(today: LocalDate): List<Ev> {
        var ram: Ev? = null; var fitr: Ev? = null; var adha: Ev? = null
        for (i in 0..400) {
            val d = today.plusDays(i.toLong()); val h = Calendars.hijri(d)
            if (ram == null && h.month == 9 && h.day == 1) ram = Ev("Ramadan", "رمضان", i, d)
            if (fitr == null && h.month == 10 && h.day == 1) fitr = Ev("Eid al-Fitr", "عيد الفطر", i, d)
            if (adha == null && h.month == 12 && h.day == 10) adha = Ev("Eid al-Adha", "عيد الأضحى", i, d)
            if (ram != null && fitr != null && adha != null) break
        }
        return listOfNotNull(ram, fitr, adha).sortedBy { it.days }
    }

    /** Eid days themselves (1–3 Shawwal, 10–13 Dhu al-Hijjah). */
    private fun eidToday(today: LocalDate): Pair<String, String>? {
        val h = Calendars.hijri(today)
        return when {
            h.month == 10 && h.day in 1..3 -> "Eid Mubarak" to "عيد فطر مبارك"
            h.month == 12 && h.day in 10..13 -> "Eid Mubarak" to "عيد أضحى مبارك"
            else -> null
        }
    }

    private fun inRamadan(r: RenderCtx): Boolean {
        val now = r.now.toInstant().toEpochMilli()
        val h = Calendars.hijri(r.today)
        if (h.month == 9) return true
        // the evening before 1 Ramadan already shows the first suhoor
        val maghrib = PrayerRepo.today(now).list[4].time
        return now >= maghrib && Calendars.hijri(r.today.plusDays(1)).month == 9
    }

    private fun dateLabel(r: RenderCtx, d: LocalDate): String =
        if (r.ar) Calendars.dayName(d, true) + " " + r.n(d.dayOfMonth) + " " + arMonths[d.monthValue - 1]
        else d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + d.dayOfMonth + " " +
            d.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
    private val arMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

    private fun eid(r: RenderCtx, msg: Pair<String, String>, small: Boolean) {
        val cx = r.w / 2
        r.textTight(r.t(msg.first, msg.second), cx, r.h * 0.46f, r.h * (if (small) 0.2f else 0.3f) * r.s1, r.c1, r.calliFont,
            maxW = r.w - 2 * r.pad - 8 * r.u, maxH = r.h * 0.42f)
        r.text(r.t("Taqabbal Allahu minna wa minkum", "تقبّل الله منا ومنكم"), cx, r.h * 0.8f, (if (small) 8.5f else 10f) * r.u * r.s2,
            r.accent, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun wide(r: RenderCtx) {
        eidToday(r.today)?.let { eid(r, it, false); return }
        if (inRamadan(r)) { RamadanR.wide(r); return }
        val ev = events(r.today)
        val next = ev.firstOrNull() ?: return
        val ar = r.ar
        val l = r.pad + 4 * r.u; val rt = r.w - r.pad - 4 * r.u
        val start = if (ar) rt else l; val end = if (ar) l else rt
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        val oal = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        // the coming occasion: its name big on one side, the days left on the other
        r.text(r.t("Coming up", "المناسبة الجاية"), start, r.h * 0.16f, 9.5f * r.u * r.s2, r.accent, r.sansBold, al, maxW = r.w * 0.5f)
        val half = (rt - l) * 0.56f
        r.textTight(r.t(next.en, next.ar), if (ar) rt - half / 2 else l + half / 2, r.h * 0.42f, r.h * 0.24f * r.s1, r.c1, r.calliFont, maxW = half, maxH = r.h * 0.3f)
        r.text(dateLabel(r, next.date), if (ar) rt - half / 2 else l + half / 2, r.h * 0.63f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = half)
        val numX = if (ar) l + (rt - l - half) / 2 else rt - (rt - l - half) / 2
        r.textTight(r.n(next.days), numX, r.h * 0.38f, r.h * 0.3f * r.s1, r.c1, r.sansBold, maxW = rt - l - half - 6 * r.u)
        r.text(r.t(if (next.days == 1) "day left" else "days left", "يوم باقي"), numX, r.h * 0.63f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = rt - l - half)
        // all three, as a row of small chips along the bottom
        val y = r.h * 0.85f
        val list = Kit.ordered(r, ev)
        val cw = (rt - l) / list.size
        list.forEachIndexed { i, e ->
            val on = e == next
            val x = l + cw * (i + 0.5f)
            val label = r.t(e.en.removePrefix("Eid "), e.ar.removePrefix("عيد ")) + "  " + r.n(e.days)
            val tw = minOf(r.measure(label, 8.5f * r.u, r.sansBold) + 14 * r.u, cw - 4 * r.u)
            val pill = RectF(x - tw / 2, y - 9 * r.u, x + tw / 2, y + 9 * r.u)
            r.canvas.drawRoundRect(pill, 9 * r.u, 9 * r.u, r.fillPaint(RenderCtx.withAlpha(if (on) r.accent else r.c1, if (on) 60 else 22)))
            r.text(label, x, y, 8.5f * r.u, if (on) r.accent else r.c2, r.sansBold, Paint.Align.CENTER, maxW = tw - 8 * r.u)
        }
    }

    fun small(r: RenderCtx) {
        eidToday(r.today)?.let { eid(r, it, true); return }
        if (inRamadan(r)) { RamadanR.small(r); return }
        val next = events(r.today).firstOrNull() ?: return
        val cx = r.w / 2
        r.text(r.t(next.en, next.ar), cx, r.h * 0.2f, 11 * r.u * r.s2, r.accent, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.textTight(r.n(next.days), cx, r.h * 0.5f, r.h * 0.34f * r.s1, r.c1, r.sansBold, maxW = r.w - 2 * r.pad - 8 * r.u)
        r.text(r.t(if (next.days == 1) "day left" else "days left", "يوم باقي"), cx, r.h * 0.74f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.text(dateLabel(r, next.date), cx, r.h * 0.87f, 8 * r.u, RenderCtx.withAlpha(r.c2, 200), r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }
}
