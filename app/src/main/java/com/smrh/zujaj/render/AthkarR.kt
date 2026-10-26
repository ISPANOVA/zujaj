package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.Instant
import java.time.ZoneId

/**
 * Morning & evening athkar. The widget picks the right list by itself (morning from Fajr until Asr,
 * evening from Asr until the next Fajr), shows one dhikr at a time with its count, and each tap counts.
 * When a dhikr reaches its number it moves to the next; when the list is done it says so until the next period.
 *
 * State lives in the widget's config: targetMillis = the period it belongs to, counter = step * 1000 + count.
 */
object Athkar {
    data class Dhikr(val ar: String, val en: String, val times: Int)

    val morning = listOf(
        Dhikr("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "We have reached the morning and the dominion belongs to Allah", 1),
        Dhikr("اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ", "O Allah, by You we enter the morning and by You we enter the evening", 1),
        Dhikr("بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", "In the name of Allah, with whose name nothing can harm", 3),
        Dhikr("رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا", "I am pleased with Allah as my Lord, Islam as my religion and Muhammad ﷺ as my Prophet", 3),
        Dhikr("لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", "None has the right to be worshipped but Allah alone", 10),
        Dhikr("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Glory and praise be to Allah", 100),
        Dhikr("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "I seek Allah's forgiveness and turn to Him", 100),
    )
    val evening = listOf(
        Dhikr("أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ", "We have reached the evening and the dominion belongs to Allah", 1),
        Dhikr("اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ", "O Allah, by You we enter the evening and by You we enter the morning", 1),
        Dhikr("أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", "I seek refuge in the perfect words of Allah from the evil of what He created", 3),
        Dhikr("بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", "In the name of Allah, with whose name nothing can harm", 3),
        Dhikr("رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا", "I am pleased with Allah as my Lord, Islam as my religion and Muhammad ﷺ as my Prophet", 3),
        Dhikr("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "Glory and praise be to Allah", 100),
        Dhikr("أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ", "I seek Allah's forgiveness and turn to Him", 100),
    )

    /** Current period: (key, isEvening). Morning = Fajr → Asr; evening = Asr → next Fajr (belongs to that day). */
    fun period(now: Long = System.currentTimeMillis()): Pair<Long, Boolean> {
        val zone = ZoneId.systemDefault()
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
        val d = runCatching { PrayerRepo.today(now) }.getOrNull()
        val fajr = d?.list?.getOrNull(0)?.time; val asr = d?.list?.getOrNull(3)?.time
        val evening: Boolean; val key: Long
        if (fajr == null || asr == null) {
            val h = Instant.ofEpochMilli(now).atZone(zone).hour
            evening = h !in 4..14; key = (if (h < 4) day - 1 else day) * 2 + (if (evening) 1 else 0)
        } else when {
            now < fajr -> { evening = true; key = (day - 1) * 2 + 1 }
            now < asr -> { evening = false; key = day * 2 }
            else -> { evening = true; key = day * 2 + 1 }
        }
        return key to evening
    }

    fun list(evening: Boolean) = if (evening) evening else morning

    /** (step, count) for the current period — a new period starts from the beginning. */
    fun state(cfg: WidgetConfig, key: Long): Pair<Int, Int> =
        if (cfg.targetMillis != key) 0 to 0 else (cfg.counter / 1000) to (cfg.counter % 1000)

    /** One tap. Returns the new config and whether a dhikr (or the whole list) was just completed. */
    fun tap(cfg: WidgetConfig): Pair<WidgetConfig, Boolean> {
        val (key, ev) = period()
        val l = list(ev)
        var (step, count) = state(cfg, key)
        if (step >= l.size) return cfg.copy(targetMillis = key) to false
        count++
        var finished = false
        if (count >= l[step].times) { step++; count = 0; finished = true }
        return cfg.copy(targetMillis = key, counter = step * 1000 + count) to finished
    }
}

object AthkarR {
    private fun tapAll(r: RenderCtx) = r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("athkar", "inc"))
    private fun title(r: RenderCtx, ev: Boolean) = if (ev) r.t("Evening athkar", "أذكار المساء") else r.t("Morning athkar", "أذكار الصباح")

    // Card (W): title + step dots, the dhikr itself, then the count with a progress line
    fun card(r: RenderCtx) {
        val (key, ev) = Athkar.period(r.now.toInstant().toEpochMilli())
        val l = Athkar.list(ev)
        val (step, count) = Athkar.state(r.cfg, key)
        val done = step >= l.size
        val left = r.pad + 4 * r.u; val right = r.w - r.pad - 4 * r.u
        val ar = r.ar
        // header
        r.text(title(r, ev), if (ar) right else left, r.h * 0.15f, 10 * r.u, r.accent, r.sansBold, if (ar) Paint.Align.RIGHT else Paint.Align.LEFT)
        val dots = l.size; val gap = 7 * r.u; val dot = 2.6f * r.u
        for (i in 0 until dots) {
            val x = if (ar) left + i * gap + dot else right - (dots - 1 - i) * gap - dot
            val idx = if (ar) dots - 1 - i else i
            r.circle(x, r.h * 0.15f, dot, if (idx < step) r.accent else withAlpha(r.c1, 50))
        }
        if (done) {
            Kit.calli(r, r.t("Done for this " + (if (ev) "evening" else "morning"), if (ev) "تمّت أذكار المساء" else "تمّت أذكار الصباح"), r.w / 2, r.h * 0.5f, 18 * r.u * r.s1, r.c1, maxW = right - left, maxH = r.h * 0.26f)
            r.text(r.t("May Allah accept it ✓", "تقبّل الله ✓"), r.w / 2, r.h * 0.76f, 10 * r.u * r.s2, r.accent, r.sansMed, Paint.Align.CENTER)
            r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("edit"))
            return
        }
        val d = l[step]
        // the dhikr in Arabic always (it's a recitation); English UI adds the meaning underneath
        r.paragraph(d.ar, RectF(left, r.h * 0.25f, right, if (ar) r.h * 0.70f else r.h * 0.58f), 15 * r.u * r.s1, r.c1, F.CAIRO_BOLD, android.text.Layout.Alignment.ALIGN_CENTER, 1.25f)
        if (!ar) r.paragraph(d.en, RectF(left, r.h * 0.59f, right, r.h * 0.70f), 8 * r.u, r.c2, r.sans, android.text.Layout.Alignment.ALIGN_CENTER)
        // count
        val y = r.h * 0.84f
        val label = r.n("$count / ${d.times}")
        val lw = r.measure(label, 11 * r.u, r.sansBold) + 10 * r.u
        val barL = if (ar) left + lw else left; val barR = if (ar) right else right - lw
        r.progressBar(barL, y, barR - barL, count / d.times.toFloat(), r.accent, withAlpha(r.c1, 34), 3.2f * r.u)
        r.text(label, if (ar) left else right, y, 11 * r.u, r.c1, r.sansBold, if (ar) Paint.Align.LEFT else Paint.Align.RIGHT)
        tapAll(r)
    }

    // Small (S): a ring of the current dhikr's count, its short form and the step
    fun small(r: RenderCtx) {
        val (key, ev) = Athkar.period(r.now.toInstant().toEpochMilli())
        val l = Athkar.list(ev)
        val (step, count) = Athkar.state(r.cfg, key)
        val cx = r.w / 2; val cy = r.h / 2; val rad = minOf(r.w, r.h) / 2 - r.pad - 2 * r.u
        if (step >= l.size) {
            Kit.ring(r, cx, cy, rad, 1f, r.accent, withAlpha(r.c1, 26), 5 * r.u, 120f)
            r.textTight("✓", cx, cy - rad * 0.08f, 34 * r.u, r.accent, r.sansBold)
            r.text(title(r, ev), cx, cy + rad * 0.48f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = rad * 1.5f)
            r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("edit"))
            return
        }
        val d = l[step]
        val overall = (step + count / d.times.toFloat()) / l.size
        Kit.ring(r, cx, cy, rad, overall, r.accent, withAlpha(r.c1, 26), 5 * r.u, 120f)
        r.text(title(r, ev), cx, cy - rad * 0.52f, 7.5f * r.u, r.accent, r.sansBold, Paint.Align.CENTER, maxW = rad * 1.5f)
        r.textTight(r.n(count), cx, cy - rad * 0.04f, 30 * r.u * r.s1, r.c1, r.sansBold, maxW = rad * 1.2f)
        r.text(r.n("/ ${d.times}"), cx, cy + rad * 0.30f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.text(r.n("${step + 1} · ${l.size}"), cx, cy + rad * 0.56f, 7.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        tapAll(r)
    }
}
