package com.smrh.zujaj.render

import android.graphics.Paint
import android.graphics.RectF
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.LocalDate

/** Tasbih counters — tap anywhere to count; vibrates on each completed round. */
object Tasbih2R {
    private fun phrase(r: RenderCtx) = r.cfg.text.ifBlank { "سبحان الله" }
    private fun goal(r: RenderCtx) = r.cfg.goal.coerceAtLeast(1)
    private fun count(r: RenderCtx) = r.cfg.counter
    private fun inRound(r: RenderCtx) = count(r) % goal(r)
    private fun rounds(r: RenderCtx) = count(r) / goal(r)
    private fun streak(r: RenderCtx): Int {
        val set = r.cfg.days.toSet(); var d = LocalDate.now().toEpochDay(); var n = 0
        if (d !in set) d -= 1
        while (d in set) { n++; d-- }
        return n
    }
    private fun tapAll(r: RenderCtx) = r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("tasbeeh", "inc"))

    // Beads — a ring of beads filling up, phrase + count inside (S)
    fun beads(r: RenderCtx) {
        val cx = r.w / 2; val cy = r.h / 2; val rad = minOf(r.w, r.h) / 2 - r.pad
        val n = goal(r).coerceAtMost(33)
        val on = (inRound(r) * n / goal(r))
        Kit.dotsRing(r, cx, cy, rad, n, on, r.accent, withAlpha(r.accent, 60), minOf(3.6f * r.u, rad * 3.14f / n * 0.4f))
        Kit.calli(r, phrase(r), cx, cy - rad * 0.36f, 13 * r.u, r.accent, maxW = rad * 1.3f, maxH = rad * 0.3f)
        r.textTight(r.n(inRound(r)), cx, cy + 4 * r.u, 34 * r.u * r.s1, r.c1, r.sansBold, maxW = rad * 1.2f)
        r.text(r.n(rounds(r)) + r.t(" rounds", " دورات"), cx, cy + rad * 0.5f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        tapAll(r)
    }

    // Ring — arc progress with the count and goal (S)
    fun ring(r: RenderCtx) {
        val cx = r.w / 2; val cy = r.h / 2; val rad = minOf(r.w, r.h) / 2 - r.pad - 2 * r.u
        Kit.ring(r, cx, cy, rad, inRound(r) / goal(r).toFloat(), r.accent, withAlpha(r.c1, 26), 5 * r.u, 120f)
        Kit.calli(r, phrase(r), cx, cy - rad * 0.42f, 12 * r.u, r.accent, maxW = rad * 1.3f, maxH = rad * 0.26f)
        r.textTight(r.n(inRound(r)), cx, cy + 2 * r.u, 34 * r.u * r.s1, r.c1, r.sansBold, maxW = rad * 1.3f)
        r.text(r.n("${inRound(r)} / ${goal(r)}"), cx, cy + rad * 0.5f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        tapAll(r)
    }

    // Calendar — phrase, streak, progress and a grid of the last 35 days (W)
    fun calendar(r: RenderCtx) {
        val split = r.w * 0.55f
        r.textTight(r.n(streak(r)), r.pad + 20 * r.u, r.h * 0.22f, 30 * r.u * r.s1, r.c1, r.sansBold)
        Kit.calli(r, phrase(r), split / 2 + r.pad / 2, r.h * 0.48f, 15 * r.u, r.accent, maxW = split - r.pad, maxH = r.h * 0.16f)
        r.text(r.t("day streak", "يوم متتابع"), split / 2 + r.pad / 2, r.h * 0.62f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        Kit.slider(r, r.pad + 4 * r.u, r.h * 0.76f, split - r.pad - 14 * r.u, inRound(r) / goal(r).toFloat(), r.accent, withAlpha(r.c1, 30), 3 * r.u, 0f)
        r.text(r.n("${inRound(r)} / ${goal(r)}"), split - 10 * r.u, r.h * 0.88f, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        val set = r.cfg.days.toSet(); val today = LocalDate.now().toEpochDay()
        val cols = 4; val rows = 7
        val area = RectF(split + 12 * r.u, r.pad * 0.7f, r.w - r.pad, r.h - r.pad * 0.7f)
        val cell = minOf(area.width() / cols, area.height() / rows)
        val x0 = area.centerX() - cell * cols / 2
        for (k in 0 until cols * rows) {
            val d = today - (cols * rows - 1 - k)
            val col = k / rows; val row = k % rows
            val rc = RectF(x0 + col * cell + 1.4f * r.u, area.top + row * cell + 1.4f * r.u, x0 + (col + 1) * cell - 1.4f * r.u, area.top + (row + 1) * cell - 1.4f * r.u)
            r.canvas.drawRoundRect(rc, 3 * r.u, 3 * r.u, r.fillPaint(if (d in set) r.accent else withAlpha(r.c1, 22)))
        }
        tapAll(r)
    }

    // Stats — phrase, today/rounds/goal row, progress, and a dot ring (W)
    fun stats(r: RenderCtx) {
        val ringCx = r.w - r.pad - r.h * 0.36f; val cy = r.h / 2; val rad = r.h * 0.34f
        Kit.dotsRing(r, ringCx, cy, rad, 33, inRound(r) * 33 / goal(r), r.accent, withAlpha(r.c1, 40), 2.4f * r.u)
        r.textTight(r.n(inRound(r)), ringCx, cy, 26 * r.u * r.s1, r.c1, r.sansBold, maxW = rad * 1.3f)
        val right = ringCx - rad - 14 * r.u
        Kit.calli(r, phrase(r), (r.pad + right) / 2, r.h * 0.14f, 14 * r.u, r.c1, maxW = right - r.pad, maxH = r.h * 0.18f)
        val items = listOf(r.t("Today", "اليوم") to r.n(count(r)), r.t("Rounds", "الأشواط") to r.n(rounds(r)),
            r.t("Goal", "الهدف") to r.n(goal(r)), r.t("Left", "يتبقى") to r.n(goal(r) - inRound(r)))
        val cw = (right - r.pad) / 4
        Kit.ordered(r, items).forEachIndexed { i, (k, v) ->
            val x = r.pad + cw * (i + 0.5f)
            r.text(k, x, r.h * 0.38f, 7.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            r.text(v, x, r.h * 0.52f, 11 * r.u, if (k == r.t("Left", "يتبقى")) r.accent else r.c1, r.sansBold, Paint.Align.CENTER)
        }
        Kit.slider(r, r.pad, r.h * 0.68f, right - r.pad, inRound(r) / goal(r).toFloat(), r.accent, withAlpha(r.c1, 30), 2.5f * r.u, 0f)
        r.text(r.n(streak(r)) + r.t(" day streak", " يوم متتابع"), (r.pad + right) / 2, r.h * 0.84f, 8.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        tapAll(r)
    }

    // Bars — arc counter on the left and a bar chart of the last days (W)
    fun bars(r: RenderCtx) {
        val cx = r.pad + r.h * 0.32f; val cy = r.h * 0.42f; val rad = r.h * 0.28f
        Kit.ring(r, cx, cy, rad, inRound(r) / goal(r).toFloat(), r.accent, withAlpha(r.c1, 26), 5 * r.u, 150f)
        r.textTight(r.n(inRound(r)), cx, cy, 26 * r.u * r.s1, r.c1, r.sansBold)
        r.text(r.t("today ", "اليوم ") + r.n(count(r)), cx, r.h * 0.86f, 9 * r.u, r.c1, r.sansBold, Paint.Align.CENTER)
        val l = cx + rad + 18 * r.u
        Kit.calli(r, phrase(r), (l + r.w - r.pad) / 2, r.h * 0.2f, r.h * 0.22f * r.s2, r.c1, maxW = r.w - r.pad - l, maxH = r.h * 0.26f)
        val set = r.cfg.days.toSet(); val today = LocalDate.now().toEpochDay()
        val n = 14; val bw = (r.w - r.pad - l) / n
        for (k in 0 until n) {
            val d = today - (n - 1 - k)
            val h = if (d in set) r.h * (0.18f + 0.18f * ((d * 7 % 5) / 4f)) else r.h * 0.06f
            val x = l + bw * (k + 0.5f)
            val p = r.strokePaint(if (d in set) r.accent else withAlpha(r.c1, 40), bw * 0.5f)
            r.canvas.drawLine(x, r.h * 0.74f, x, r.h * 0.74f - h, p)
        }
        r.text(r.n(streak(r)) + r.t(" day streak", " يوم متتابع"), l, r.h * 0.88f, 8 * r.u, r.c2, r.sans)
        r.text(r.n(rounds(r)) + r.t(" rounds", " دورة"), r.w - r.pad, r.h * 0.88f, 8 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        tapAll(r)
    }
}
