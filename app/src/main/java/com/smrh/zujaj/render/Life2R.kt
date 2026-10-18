package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.LocalDate

/** Habits, notes and focus timers. */
object Life2R {
    private val today get() = LocalDate.now().toEpochDay()
    private fun doneToday(r: RenderCtx) = today in r.cfg.days
    private fun streak(r: RenderCtx): Int {
        val set = r.cfg.days.toSet(); var d = today; var n = 0
        if (d !in set) d -= 1
        while (d in set) { n++; d-- }
        return n
    }
    private fun title(r: RenderCtx) = r.cfg.title.ifBlank { r.t("Drink water", "شرب الماء") }
    private fun check(r: RenderCtx, cx: Float, cy: Float, rad: Float, on: Boolean) {
        if (on) r.circle(cx, cy, rad, r.accent) else r.canvas.drawCircle(cx, cy, rad, r.strokePaint(withAlpha(r.c1, 120), 1.2f * r.u))
        if (on) {
            val p = r.strokePaint(r.cfg.bgColor or 0xFF000000.toInt(), rad * 0.22f)
            r.canvas.drawLine(cx - rad * 0.42f, cy, cx - rad * 0.1f, cy + rad * 0.32f, p)
            r.canvas.drawLine(cx - rad * 0.1f, cy + rad * 0.32f, cx + rad * 0.45f, cy - rad * 0.32f, p)
        }
    }
    private fun toggleToday(r: RenderCtx, rc: RectF) = r.hit(rc, HitAction.Cmd("habit", today.toString()))
    private fun weekDots(r: RenderCtx, l: Float, rr: Float, y: Float, dot: Float) {
        val set = r.cfg.days.toSet()
        val cw = (rr - l) / 7
        for (k in 0 until 7) {
            val d = today - 6 + k
            val col = if (r.ar) 6 - k else k
            r.circle(l + cw * (col + 0.5f), y, dot, if (d in set) r.accent else withAlpha(r.c1, 40))
        }
    }

    // ---------------------------------------------------------------- habits
    // Habit — check, name, streak number and the week (S)
    fun habit(r: RenderCtx) {
        check(r, r.pad + 14 * r.u, r.h * 0.2f, 12 * r.u, doneToday(r))
        r.text(title(r), r.w - r.pad, r.h * 0.16f, 12 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.RIGHT, maxW = r.w * 0.6f)
        r.text(if (doneToday(r)) r.t("done today", "تم اليوم") else r.t("not yet", "لسه"), r.w - r.pad, r.h * 0.29f, 8.5f * r.u, r.accent, r.sans, Paint.Align.RIGHT)
        r.textTight(r.n(streak(r)), r.w - r.pad - 16 * r.u, r.h * 0.52f, 30 * r.u * r.s1, r.c1, r.sansBold)
        r.text(r.t("day streak", "أيام متتالية"), r.w - r.pad, r.h * 0.7f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        weekDots(r, r.pad, r.w - r.pad, r.h * 0.88f, 4 * r.u)
        toggleToday(r, RectF(0f, 0f, r.w, r.h))
    }

    // Habit ring — a big check in a ring of the week (S)
    fun habitRing(r: RenderCtx) {
        val cx = r.w / 2; val cy = r.h * 0.42f; val rad = r.h * 0.3f
        val set = r.cfg.days.toSet()
        for (k in 0 until 7) {
            val a = Math.toRadians(-90 + k * 360.0 / 7)
            r.circle(cx + rad * kotlin.math.cos(a).toFloat(), cy + rad * kotlin.math.sin(a).toFloat(), 3.4f * r.u, if ((today - 6 + k) in set) r.accent else withAlpha(r.c1, 40))
        }
        check(r, cx, cy, rad * 0.55f, doneToday(r))
        r.text(title(r), cx, r.h * 0.88f, 12 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        toggleToday(r, RectF(0f, 0f, r.w, r.h))
    }

    // Habit mini — streak in a big circle (S)
    fun habitMini(r: RenderCtx) {
        r.text(title(r), r.w / 2, r.h * 0.14f, 12 * r.u * r.s2, r.c1, r.sansBold, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.circle(r.w / 2, r.h * 0.5f, r.h * 0.24f, if (doneToday(r)) r.accent else withAlpha(r.c1, 26))
        r.textTight(r.n(streak(r)), r.w / 2, r.h * 0.5f, 30 * r.u * r.s1, r.c1, r.sansBold)
        r.text(r.t("days in a row", "أيام متتالية"), r.w / 2, r.h * 0.88f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        toggleToday(r, RectF(0f, 0f, r.w, r.h))
    }

    // Habit strip — the last 14 days as two rows of dots (B)
    fun habitStrip(r: RenderCtx) {
        val set = r.cfg.days.toSet()
        r.text(title(r), r.pad + 4 * r.u, r.h * 0.22f, 10 * r.u, r.c1, r.sansBold, maxW = r.w * 0.5f)
        r.text(r.n(streak(r)) + r.t(" days", " يوم"), r.w - r.pad - 4 * r.u, r.h * 0.22f, 9 * r.u, r.accent, r.sansBold, Paint.Align.RIGHT)
        val cw = (r.w - 2 * r.pad) / 14
        for (k in 0 until 14) {
            val d = today - 13 + k
            val col = if (r.ar) 13 - k else k
            r.circle(r.pad + cw * (col + 0.5f), r.h * 0.62f, minOf(5 * r.u, cw * 0.32f), if (d in set) r.accent else withAlpha(r.c1, 40))
        }
        toggleToday(r, RectF(0f, 0f, r.w, r.h))
    }

    // Habit week — a month grid of done days (S)
    fun habitWeek(r: RenderCtx) {
        val set = r.cfg.days.toSet()
        r.text(title(r), r.pad, r.h * 0.12f, 10 * r.u, r.c1, r.sansBold, maxW = r.w * 0.7f)
        check(r, r.w - r.pad - 8 * r.u, r.h * 0.12f, 7 * r.u, doneToday(r))
        val cols = 7; val rows = 5
        val cell = minOf((r.w - 2 * r.pad) / cols, (r.h * 0.72f) / rows)
        val x0 = r.w / 2 - cell * cols / 2
        for (k in 0 until cols * rows) {
            val d = today - (cols * rows - 1 - k)
            val col = k % cols; val row = k / cols
            r.circle(x0 + cell * ((if (r.ar) cols - 1 - col else col) + 0.5f), r.h * 0.26f + cell * (row + 0.5f), cell * 0.32f, if (d in set) r.accent else withAlpha(r.c1, 36))
        }
        toggleToday(r, RectF(0f, 0f, r.w, r.h))
    }

    // Habit pills — progress pill "x of 7 this week" + mark button (W)
    fun habitPills(r: RenderCtx) {
        val set = r.cfg.days.toSet()
        val wk = (0 until 7).count { (today - it) in set }
        r.text(r.n("$wk / 7") + r.t(" this week", " هذا الأسبوع"), r.pad + 4 * r.u, r.h * 0.18f, 9 * r.u, r.c2, r.sans)
        val pill = RectF(r.pad, r.h * 0.34f, r.w - r.pad, r.h * 0.62f)
        r.pill(pill, withAlpha(r.c1, 20))
        r.pill(RectF(pill.left, pill.top, pill.left + pill.width() * maxOf(wk / 7f, 0.18f), pill.bottom), r.accent)
        r.text(title(r), pill.left + 12 * r.u, pill.centerY(), 11 * r.u * r.s2, r.c1, r.sansBold, maxW = pill.width() * 0.6f)
        weekDots(r, r.pad, r.w - r.pad, r.h * 0.82f, 4 * r.u)
        toggleToday(r, RectF(0f, 0f, r.w, r.h))
    }

    // ---------------------------------------------------------------- notes
    private fun noteText(r: RenderCtx) = r.cfg.text.ifBlank { r.t("Return the books to the library and call Ahmed about Saturday.", "أعد الكتب إلى المكتبة، واتصل بأحمد بخصوص موعد السبت.") }
    private fun noteTitle(r: RenderCtx) = r.cfg.title.ifBlank { r.t("Note", "ملاحظة") }

    // Note — title with a dot, wrapped text, date (S)
    fun note(r: RenderCtx) {
        r.circle(r.w - r.pad - 3 * r.u, r.h * 0.12f, 3 * r.u, r.accent)
        r.text(noteTitle(r), r.w - r.pad - 10 * r.u, r.h * 0.12f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.7f)
        r.paragraph(noteText(r), RectF(r.pad, r.h * 0.22f, r.w - r.pad, r.h * 0.82f), 13 * r.u * r.s1, r.c1, r.sansMed, android.text.Layout.Alignment.ALIGN_NORMAL, vCenter = false)
        r.text(Kit.gregShort(r), r.pad, r.h * 0.9f, 8 * r.u, r.c2, r.sans)
    }

    // Sticky — yellow top strip, bigger text (S)
    fun sticky(r: RenderCtx) {
        r.canvas.drawRect(0f, 0f, r.w, 5 * r.u, r.fillPaint(r.accent))
        r.paragraph(noteText(r), RectF(r.pad, r.h * 0.12f, r.w - r.pad, r.h * 0.84f), 14 * r.u * r.s1, r.c1, r.sansMed, android.text.Layout.Alignment.ALIGN_NORMAL, vCenter = false)
        r.text(Kit.gregShort(r), r.pad, r.h * 0.9f, 8 * r.u, r.c2, r.sans)
    }

    // Note strip — dot + title + one line, and a + button (B)
    fun noteStrip(r: RenderCtx) {
        r.circle(r.pad + 4 * r.u, r.h * 0.34f, 3 * r.u, r.accent)
        r.text(noteTitle(r), r.pad + 12 * r.u, r.h * 0.34f, 13 * r.u * r.s1, r.c1, r.sansBold, maxW = r.w * 0.7f)
        r.text(noteText(r), r.pad + 12 * r.u, r.h * 0.64f, 10 * r.u, r.c2, r.sans, maxW = r.w * 0.72f)
        r.circle(r.w - r.pad - 14 * r.u, r.h / 2, 12 * r.u, withAlpha(r.c1, 30))
        Glyphs.plus(r.canvas, r.w - r.pad - 14 * r.u, r.h / 2, 12 * r.u, r.c1)
    }

    // Big note — the whole text, small date (S / W)
    fun bigNote(r: RenderCtx) {
        r.paragraph(noteText(r), RectF(r.pad, r.pad, r.w - r.pad, r.h * 0.84f), 12 * r.u * r.s1, r.c1, r.sansMed, android.text.Layout.Alignment.ALIGN_NORMAL, vCenter = false)
        r.text(Kit.gregShort(r), r.pad, r.h * 0.9f, 7.5f * r.u, r.c2, r.sans)
    }

    // Notes list — each line of the text is a note (W)
    fun notesList(r: RenderCtx) {
        val items = noteText(r).split('\n').map { it.trim() }.filter { it.isNotEmpty() }.take(4).ifEmpty { listOf(noteText(r)) }
        r.text(noteTitle(r), r.pad + 4 * r.u, r.h * 0.1f, 9 * r.u, r.c2, r.sansBold)
        val rowH = (r.h * 0.84f) / items.size.coerceAtLeast(3)
        items.forEachIndexed { i, s ->
            val y = r.h * 0.2f + rowH * (i + 0.5f)
            r.circle(if (r.ar) r.w - r.pad - 3 * r.u else r.pad + 3 * r.u, y, 2.5f * r.u, r.accent)
            r.text(s, if (r.ar) r.w - r.pad - 12 * r.u else r.pad + 12 * r.u, y, 10.5f * r.u * r.s1, r.c1, r.sansMed, if (r.ar) Paint.Align.RIGHT else Paint.Align.LEFT, maxW = r.w - 2 * r.pad - 16 * r.u)
        }
    }

    // ---------------------------------------------------------------- focus
    private fun running(r: RenderCtx) = r.cfg.targetMillis > System.currentTimeMillis()
    private fun minutes(r: RenderCtx) = r.cfg.goal.coerceIn(1, 180)
    private fun focusTime(r: RenderCtx, cx: Float, cy: Float, size: Float, align: Paint.Align = Paint.Align.CENTER) {
        if (running(r)) r.liveCountdown(r.cfg.targetMillis, "", cx, cy, size, r.c1, align, bold = true)
        else r.text(r.n("${minutes(r)}:00"), cx, cy, size, r.c1, r.sansBold, align)
    }
    private fun buttons(r: RenderCtx, cx: Float, cy: Float, rad: Float) {
        // stop
        r.circle(cx - rad * 1.5f, cy, rad * 0.8f, withAlpha(r.c1, 26))
        r.canvas.drawRect(cx - rad * 1.5f - rad * 0.22f, cy - rad * 0.22f, cx - rad * 1.5f + rad * 0.22f, cy + rad * 0.22f, r.fillPaint(r.c1))
        r.hit(RectF(cx - rad * 2.4f, cy - rad, cx - rad * 0.6f, cy + rad), HitAction.Cmd("focus", "stop"))
        // start / pause
        r.circle(cx + rad * 0.6f, cy, rad, r.accent)
        if (running(r)) Glyphs.pause(r.canvas, cx + rad * 0.6f, cy, rad * 0.9f, 0xFFFFFFFF.toInt()) else Glyphs.play(r.canvas, cx + rad * 0.6f, cy, rad * 0.9f, 0xFFFFFFFF.toInt())
        r.hit(RectF(cx - rad * 0.4f, cy - rad, cx + rad * 1.6f, cy + rad), HitAction.Cmd("focus", if (running(r)) "stop" else "start"))
    }

    // Focus — label, timer, line, controls (S)
    fun focus(r: RenderCtx) {
        r.text(r.cfg.title.ifBlank { r.t("Focus", "تركيز") }, r.w - r.pad, r.h * 0.12f, 10 * r.u, r.c1, r.sansMed, Paint.Align.RIGHT)
        if (running(r)) r.circle(r.w - r.pad - r.measure(r.cfg.title.ifBlank { r.t("Focus", "تركيز") }, 10 * r.u) - 8 * r.u, r.h * 0.12f, 3 * r.u, r.accent)
        focusTime(r, r.w / 2, r.h * 0.4f, 30 * r.u * r.s1)
        Kit.slider(r, r.pad + 6 * r.u, r.h * 0.6f, r.w - 2 * r.pad - 12 * r.u, 0.8f, r.accent, withAlpha(r.c1, 30), 2 * r.u, 0f)
        buttons(r, r.w / 2, r.h * 0.8f, 12 * r.u)
    }

    // Focus centre — big timer centred, controls below (S)
    fun focusCentre(r: RenderCtx) {
        r.text(r.cfg.title.ifBlank { r.t("Focus", "تركيز") }, r.w / 2, r.h * 0.12f, 10 * r.u, r.c2, r.sansMed, Paint.Align.CENTER)
        focusTime(r, r.w / 2, r.h * 0.38f, 32 * r.u * r.s1)
        r.text(r.n("${minutes(r)}") + r.t(" min", " دقيقة"), r.w / 2, r.h * 0.56f, 8.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        buttons(r, r.w / 2, r.h * 0.8f, 13 * r.u)
    }

    // Focus board — timer with a segmented session bar (W)
    fun focusBoard(r: RenderCtx) {
        focusTime(r, r.w - r.pad - 4 * r.u, r.h * 0.3f, 30 * r.u * r.s1, Paint.Align.RIGHT)
        r.text(r.cfg.title.ifBlank { r.t("Focus session", "جلسة تركيز") }, r.pad + 4 * r.u, r.h * 0.2f, 10 * r.u, r.c1, r.sansBold)
        r.text(r.n("${minutes(r)}") + r.t(" min", " دقيقة"), r.pad + 4 * r.u, r.h * 0.34f, 8.5f * r.u, r.c2, r.sans)
        val n = 12; val w = (r.w - 2 * r.pad - 8 * r.u) / n
        for (k in 0 until n) r.canvas.drawLine(r.pad + 4 * r.u + w * k + 2 * r.u, r.h * 0.56f, r.pad + 4 * r.u + w * (k + 1) - 2 * r.u, r.h * 0.56f,
            r.strokePaint(if (k >= n - 4) r.accent else withAlpha(r.c1, 40), 2 * r.u))
        buttons(r, r.w / 2, r.h * 0.8f, 12 * r.u)
    }
}
