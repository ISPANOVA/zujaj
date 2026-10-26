package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

// ============================================================================ Ramadan: iftar & suhoor
/**
 * In Ramadan: a live countdown to Iftar (Maghrib) during the day and to the end of Suhoor (Fajr) at night,
 * with the day of Ramadan and both times. Outside Ramadan it counts the days until Ramadan starts.
 */
object RamadanR {
    private fun hm(r: RenderCtx, ms: Long) = r.n(SimpleDateFormat(if (r.cfg.hour24) "HH:mm" else "hh:mm", java.util.Locale.US).format(Date(ms)))

    /** Days until 1 Ramadan (0 = it's Ramadan). */
    fun daysToRamadan(today: LocalDate): Int {
        for (i in 0..400) { val h = Calendars.hijri(today.plusDays(i.toLong())); if (h.month == 9) return i }
        return -1
    }

    private data class State(val inRamadan: Boolean, val day: Int, val iftar: Boolean, val target: Long, val fajr: Long, val maghrib: Long, val daysLeft: Int)
    private fun state(r: RenderCtx): State {
        val now = r.now.toInstant().toEpochMilli()
        val today = r.today
        val h = Calendars.hijri(today)
        val d = PrayerRepo.today(now)
        val fajr = d.list[0].time; val maghrib = d.list[4].time
        val tomorrowFajr = PrayerRepo.today(now + 24 * 3600_000L).list[0].time
        val iftar = now in fajr until maghrib
        val target = when { now < fajr -> fajr; now < maghrib -> maghrib; else -> tomorrowFajr }
        // after Maghrib the next fast is tomorrow's, so the day shown is tomorrow's Ramadan day
        val inR = h.month == 9 || (now >= maghrib && Calendars.hijri(today.plusDays(1)).month == 9)
        val day = if (now >= maghrib) Calendars.hijri(today.plusDays(1)).day else h.day
        return State(inR, day, iftar, target, fajr, maghrib, if (inR) 0 else daysToRamadan(today))
    }

    fun wide(r: RenderCtx) {
        val s = state(r)
        val l = r.pad + 4 * r.u; val rt = r.w - r.pad - 4 * r.u
        val ar = r.ar
        val start = if (ar) rt else l; val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT
        // a small crescent beside the title
        crescent(r, if (ar) l + 14 * r.u else rt - 14 * r.u, r.h * 0.2f, 10 * r.u)
        if (!s.inRamadan) {
            r.text(r.t("Ramadan", "رمضان"), start, r.h * 0.2f, 11 * r.u, r.accent, r.sansBold, al)
            r.textTight(r.n(s.daysLeft), r.w / 2, r.h * 0.52f, 46 * r.u * r.s1, r.c1, r.sansBold)
            r.text(r.t("days to go", "يوم باقي"), r.w / 2, r.h * 0.8f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
            return
        }
        r.text(r.t("Ramadan · day ", "رمضان · اليوم ") + r.n(s.day), start, r.h * 0.2f, 10.5f * r.u, r.accent, r.sansBold, al)
        r.text(if (s.iftar) r.t("Iftar in", "الفطار بعد") else r.t("Suhoor ends in", "السحور ينتهي بعد"), start, r.h * 0.38f, 10 * r.u, r.c2, r.sans, al)
        r.liveCountdown(s.target, "", start, r.h * 0.56f, 30 * r.u * r.s1, r.c1, al, bold = true)
        // both times, the coming one highlighted
        val y = r.h * 0.84f
        val items = Kit.ordered(r, listOf(r.t("Suhoor", "السحور") to s.fajr, r.t("Iftar", "الفطار") to s.maghrib))
        val cw = (rt - l) / 2
        items.forEachIndexed { i, (k, t) ->
            val on = (k == r.t("Iftar", "الفطار")) == s.iftar
            val x = l + cw * (i + 0.5f)
            r.text(k + "  " + hm(r, t), x, y, 10 * r.u, if (on) r.accent else r.c2, if (on) r.sansBold else r.sans, Paint.Align.CENTER, maxW = cw)
        }
    }

    fun small(r: RenderCtx) {
        val s = state(r)
        val cx = r.w / 2
        crescent(r, cx, r.h * 0.2f, 9 * r.u)
        if (!s.inRamadan) {
            r.textTight(r.n(s.daysLeft), cx, r.h * 0.52f, 40 * r.u * r.s1, r.c1, r.sansBold)
            r.text(r.t("days to Ramadan", "يوم لرمضان"), cx, r.h * 0.8f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
            return
        }
        r.text(if (s.iftar) r.t("Iftar in", "الفطار بعد") else r.t("Suhoor ends", "السحور بعد"), cx, r.h * 0.38f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.liveCountdown(s.target, "", cx, r.h * 0.56f, 22 * r.u * r.s1, r.c1, Paint.Align.CENTER, bold = true)
        r.text(r.t("Day ", "اليوم ") + r.n(s.day), cx, r.h * 0.8f, 9 * r.u, r.accent, r.sansBold, Paint.Align.CENTER)
    }

    private fun crescent(r: RenderCtx, cx: Float, cy: Float, rad: Float) {
        val l = r.canvas.saveLayer(null, null)
        r.circle(cx, cy, rad, r.accent)
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) }
        r.canvas.drawCircle(cx + rad * 0.45f, cy - rad * 0.2f, rad * 0.88f, p)
        r.canvas.restoreToCount(l)
    }
}

// ============================================================================ Quran khatma
/**
 * A khatma tracker. goal = pages per daily wird; counter = pages read in this khatma (of 604);
 * days = the days a wird was logged; variant = khatmas completed. One tap logs one wird.
 */
object Quran {
    const val PAGES = 604
    fun tap(cfg: com.smrh.zujaj.model.WidgetConfig): Pair<com.smrh.zujaj.model.WidgetConfig, Boolean> {
        val g = cfg.goal.coerceIn(1, PAGES)
        val today = LocalDate.now().toEpochDay()
        val days = if (today in cfg.days) cfg.days else (cfg.days + today).takeLast(400)
        val next = cfg.counter + g
        return if (next >= PAGES) cfg.copy(counter = 0, variant = cfg.variant + 1, days = days) to true
        else cfg.copy(counter = next, days = days) to false
    }
    fun juz(page: Int) = (page / 20 + 1).coerceIn(1, 30)
    fun daysLeft(cfg: com.smrh.zujaj.model.WidgetConfig) = ((PAGES - cfg.counter) + cfg.goal.coerceAtLeast(1) - 1) / cfg.goal.coerceAtLeast(1)
}

object QuranR {
    private fun tapAll(r: RenderCtx) = r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("quran", "wird"))
    private fun doneToday(r: RenderCtx) = r.today.toEpochDay() in r.cfg.days
    private fun wirdLabel(r: RenderCtx): String {
        val g = r.cfg.goal
        return if (g % 20 == 0) r.n(g / 20) + r.t(if (g == 20) " juz" else " juz'", " جزء") else r.n(g) + r.t(" pages", " صفحة")
    }

    fun wide(r: RenderCtx) {
        val cfg = r.cfg
        val l = r.pad + 4 * r.u; val rt = r.w - r.pad - 4 * r.u
        val ar = r.ar; val start = if (ar) rt else l; val end = if (ar) l else rt
        val al = if (ar) Paint.Align.RIGHT else Paint.Align.LEFT; val alE = if (ar) Paint.Align.LEFT else Paint.Align.RIGHT
        r.text(r.t("Khatma", "الختمة") + (if (cfg.variant > 0) " · " + r.n(cfg.variant + 1) else ""), start, r.h * 0.17f, 10.5f * r.u, r.accent, r.sansBold, al)
        r.text(r.t("Wird: ", "الورد: ") + wirdLabel(r), end, r.h * 0.17f, 9 * r.u, r.c2, r.sans, alE)
        // where you are
        r.text(r.t("Juz ", "الجزء ") + r.n(Quran.juz(cfg.counter)), start, r.h * 0.42f, 24 * r.u * r.s1, r.c1, r.sansBold, al)
        r.text(r.t("page ", "صفحة ") + r.n(cfg.counter + 1) + r.n(" / ${Quran.PAGES}"), start, r.h * 0.6f, 9.5f * r.u, r.c2, r.sans, al)
        // today
        val done = doneToday(r)
        val chip = if (done) r.t("Today's wird ✓", "ورد النهارده ✓") else r.t("Tap when you read it", "اضغط لما تقراه")
        r.text(chip, end, r.h * 0.48f, 10 * r.u, if (done) r.accent else r.c1, r.sansBold, alE, maxW = (rt - l) * 0.5f)
        r.text(r.n(Quran.daysLeft(cfg)) + r.t(" days to finish", " يوم وتختم"), end, r.h * 0.62f, 9 * r.u, r.c2, r.sans, alE, maxW = (rt - l) * 0.5f)
        val frac = cfg.counter / Quran.PAGES.toFloat()
        Kit.slider(r, l, r.h * 0.82f, rt - l, if (ar) 1f - frac else frac, r.accent, withAlpha(r.c1, 34), 3.5f * r.u, 0f)
        tapAll(r)
    }

    fun small(r: RenderCtx) {
        val cfg = r.cfg
        val cx = r.w / 2; val cy = r.h / 2; val rad = minOf(r.w, r.h) / 2 - r.pad - 2 * r.u
        Kit.ring(r, cx, cy, rad, cfg.counter / Quran.PAGES.toFloat(), r.accent, withAlpha(r.c1, 26), 5 * r.u, 120f)
        r.text(r.t("Juz", "الجزء"), cx, cy - rad * 0.48f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.textTight(r.n(Quran.juz(cfg.counter)), cx, cy - rad * 0.02f, 32 * r.u * r.s1, r.c1, r.sansBold, maxW = rad * 1.2f)
        r.text(if (doneToday(r)) r.t("wird ✓", "الورد ✓") else r.t("tap to log", "اضغط وسجّل"), cx, cy + rad * 0.48f, 8 * r.u,
            if (doneToday(r)) r.accent else r.c2, r.sansBold, Paint.Align.CENTER, maxW = rad * 1.5f)
        tapAll(r)
    }
}

// ============================================================================ Smart widget
/**
 * One spot, the right thing: the next prayer around prayer time, the athkar in their time (until done),
 * and a clock the rest of the day. It borrows the real designs, dressed in this widget's colours.
 */
object SmartR {
    enum class Mode { PRAYER, ATHKAR, CLOCK }
    fun mode(r: RenderCtx): Mode {
        val now = r.now.toInstant().toEpochMilli()
        val d = runCatching { PrayerRepo.today(now) }.getOrNull() ?: return Mode.CLOCK
        val toNext = d.next.time - now; val sincePrev = now - d.prev.time
        if (toNext in 0..45 * 60_000L || sincePrev in 0..15 * 60_000L) return Mode.PRAYER
        val (key, ev) = Athkar.period(now)
        val (step, _) = Athkar.state(r.cfg, key)
        val unfinished = step < Athkar.list(ev).size
        val fajr = d.list[0].time; val asr = d.list[3].time; val maghrib = d.list[4].time
        val inWindow = (now in fajr..fajr + 3 * 3600_000L) || (now in asr..maghrib + 3600_000L)
        return if (inWindow && unfinished) Mode.ATHKAR else Mode.CLOCK
    }
    fun wide(r: RenderCtx) {
        when (mode(r)) {
            Mode.PRAYER -> Catalog.byId("prayer_journey")?.draw?.invoke(r)
            Mode.ATHKAR -> AthkarR.card(r)
            Mode.CLOCK -> Catalog.byId("clock_wide")?.draw?.invoke(r) ?: Catalog.byId("calli_lawn")?.draw?.invoke(r)
        }
    }
}
