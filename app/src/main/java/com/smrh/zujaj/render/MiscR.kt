package com.smrh.zujaj.render

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import com.smrh.zujaj.data.Calendars
import com.smrh.zujaj.data.PrayerRepo
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.data.SystemRepo
import com.smrh.zujaj.model.ContactAction
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// =====================================================================================  APPS
object AppsR {
    fun grid(r: RenderCtx, cols: Int, rows: Int) {
        val p = r.pad * 0.8f
        val cw = (r.w - 2 * p) / cols
        val ch = (r.h - 2 * p) / rows
        val cell = min(cw, ch)
        val iconS = cell * 0.66f * r.s1
        val apps = r.cfg.apps
        for (i in 0 until cols * rows) {
            val cx = p + cw * (i % cols + 0.5f)
            val cy = p + ch * (i / cols + 0.5f)
            val tile = RectF(cx - cell * 0.42f, cy - cell * 0.42f, cx + cell * 0.42f, cy + cell * 0.42f)
            r.canvas.drawRoundRect(tile, cell * 0.3f, cell * 0.3f, r.fillPaint(withAlpha(r.c1, 16)))
            val app = apps.getOrNull(i)
            val bmp = app?.let { AppIcons.icon(r.context, it, iconS.toInt()) }
            if (app != null && bmp != null) {
                AppIcons.drawIcon(r.canvas, bmp, RectF(cx - iconS / 2, cy - iconS / 2, cx + iconS / 2, cy + iconS / 2))
                r.hit(tile, HitAction.App(app))
            } else {
                Glyphs.plus(r.canvas, cx, cy, cell * 0.4f, withAlpha(r.c1, 120))
                r.hit(tile, HitAction.Cmd("edit"))
            }
        }
    }

    fun dock(r: RenderCtx) {
        val p = r.pad * 0.6f
        val n = 4
        val cw = (r.w - 2 * p) / n
        val iconS = min(cw, r.h) * 0.5f * r.s1
        for (i in 0 until n) {
            val cx = p + cw * (i + 0.5f)
            val cy = r.h * 0.42f
            val app = r.cfg.apps.getOrNull(i)
            val bmp = app?.let { AppIcons.icon(r.context, it, iconS.toInt()) }
            val cell = RectF(cx - cw / 2, 0f, cx + cw / 2, r.h)
            if (app != null && bmp != null) {
                AppIcons.drawIcon(r.canvas, bmp, RectF(cx - iconS / 2, cy - iconS / 2, cx + iconS / 2, cy + iconS / 2))
                r.text(app.label, cx, r.h * 0.82f, 9 * r.u * r.s2, r.c1, r.sans, Paint.Align.CENTER, maxW = cw * 0.9f)
                r.hit(cell, HitAction.App(app))
            } else {
                r.circle(cx, cy, iconS / 2, withAlpha(r.c1, 26))
                Glyphs.plus(r.canvas, cx, cy, iconS * 0.6f, r.c1)
                r.hit(cell, HitAction.Cmd("edit"))
            }
        }
    }
}

// =====================================================================================  PEOPLE
object PeopleR {
    private val palette = intArrayOf(0xFF6A5CFF.toInt(), 0xFFE8478B.toInt(), 0xFF2EB67D.toInt(), 0xFFFF9F43.toInt(), 0xFF38C6D9.toInt(), 0xFFB45CFF.toInt())

    private fun avatar(r: RenderCtx, i: Int, cx: Float, cy: Float, rad: Float) {
        val c = r.cfg.contacts.getOrNull(i)
        if (c == null) {
            r.circle(cx, cy, rad, withAlpha(r.c1, 22))
            Glyphs.plus(r.canvas, cx, cy, rad * 0.9f, withAlpha(r.c1, 140))
            r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Cmd("edit"))
            return
        }
        val b = if (c.photo.isNotBlank()) AppIcons.loadFile(c.photo, 300) else null
        if (b != null) {
            val save = r.canvas.save()
            r.canvas.clipPath(Path().apply { addCircle(cx, cy, rad, Path.Direction.CW) })
            Renderer.drawImage(r.canvas, b, RectF(cx - rad, cy - rad, cx + rad, cy + rad), 0f)
            r.canvas.restoreToCount(save)
        } else {
            r.circle(cx, cy, rad, palette[(c.name.hashCode() and 0x7fffffff) % palette.size])
            val initials = c.name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1) }.uppercase()
            r.text(initials, cx, cy, rad * 0.75f, Color.WHITE, r.sansBold, Paint.Align.CENTER)
        }
        // action badge
        val br = rad * 0.32f
        val bx = cx + rad * 0.72f; val by = cy + rad * 0.72f
        val (bc, glyph) = when (c.action) {
            ContactAction.CALL -> 0xFF2EB67D.toInt() to 0
            ContactAction.SMS -> 0xFF3B82F6.toInt() to 1
            ContactAction.WHATSAPP -> 0xFF25D366.toInt() to 1
            ContactAction.OPEN -> 0xFF8E8E99.toInt() to 2
        }
        r.circle(bx, by, br, bc)
        when (glyph) {
            0 -> Glyphs.phone(r.canvas, bx, by, br * 1.3f, Color.WHITE)
            1 -> Glyphs.chat(r.canvas, bx, by, br * 1.3f, Color.WHITE)
            else -> Glyphs.person(r.canvas, bx, by - br * 0.1f, br * 1.3f, Color.WHITE)
        }
        r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Contact(c))
    }

    fun grid(r: RenderCtx, cols: Int, rows: Int) {
        val p = r.pad * 0.8f
        val cw = (r.w - 2 * p) / cols
        val ch = (r.h - 2 * p) / rows
        val rad = min(cw, ch) * 0.38f * r.s1
        for (i in 0 until cols * rows) avatar(r, i, p + cw * (i % cols + 0.5f), p + ch * (i / cols + 0.5f), rad)
    }

    fun favorite(r: RenderCtx) {
        val rad = min(r.w, r.h) * 0.27f * r.s1
        avatar(r, 0, r.w / 2, r.h * 0.42f, rad)
        val name = r.cfg.contacts.firstOrNull()?.name ?: r.t("Add person", "أضف شخص")
        r.text(name, r.w / 2, r.h * 0.84f, 12 * r.u * r.s2, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }
}

// =====================================================================================  SEARCH
object SearchR {
    private fun engineIcon(r: RenderCtx, engine: Int, cx: Float, cy: Float, s: Float) {
        val pkg = when (engine) { 1 -> Intents.BRAVE; 2 -> Intents.SAMSUNG; else -> Intents.GOOGLE }
        val b = AppIcons.iconForPkg(r.context, pkg, s.toInt())
        if (b != null) AppIcons.drawIcon(r.canvas, b, RectF(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2))
        else Glyphs.search(r.canvas, cx, cy, s, r.c1)
    }

    private fun round(r: RenderCtx, cx: Float, cy: Float, rad: Float, kind: String, fill: Boolean = true) {
        if (fill) r.circle(cx, cy, rad, withAlpha(r.c1, 30))
        val s = rad * 1.05f
        when (kind) {
            "mic" -> { Glyphs.mic(r.canvas, cx, cy, s, r.c1); r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.voice(r.context))) }
            "lens" -> { Glyphs.lens(r.canvas, cx, cy, s, r.c1); r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.lens(r.context))) }
            "ai" -> { Glyphs.sparkle(r.canvas, cx, cy, s, r.c1); r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.assistant(r.context))) }
            "tr" -> { Glyphs.translate(r.canvas, cx, cy, s, r.c1); r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.translate(r.context))) }
            "map" -> { Glyphs.pin(r.canvas, cx, cy, s, r.c1); r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.maps(r.context))) }
            "yt" -> { Glyphs.video(r.canvas, cx, cy, s, r.c1); r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.youtube(r.context))) }
        }
    }

    fun bar(r: RenderCtx, engine: Int) {
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Launch(Intents.search(r.context, engine)))
        engineIcon(r, engine, r.pad + 16 * r.u, r.h / 2, 26 * r.u * r.s1)
        round(r, r.w - r.pad - 56 * r.u, r.h / 2, 15 * r.u, "mic", false)
        round(r, r.w - r.pad - 18 * r.u, r.h / 2, 15 * r.u, "lens", false)
    }

    fun plus(r: RenderCtx) {
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Launch(Intents.search(r.context, 0)))
        engineIcon(r, 0, r.pad + 16 * r.u, r.h / 2, 26 * r.u * r.s1)
        round(r, r.w - r.pad - 94 * r.u, r.h / 2, 15 * r.u, "map", false)
        round(r, r.w - r.pad - 56 * r.u, r.h / 2, 15 * r.u, "yt", false)
        round(r, r.w - r.pad - 18 * r.u, r.h / 2, 15 * r.u, "tr", false)
    }

    fun actions(r: RenderCtx) {
        val p = r.pad * 0.8f
        val bar = RectF(p, p, r.w - p, r.h / 2 - 4 * r.u)
        r.pill(bar, withAlpha(r.c1, 30))
        r.hit(bar, HitAction.Launch(Intents.search(r.context, 0)))
        engineIcon(r, 0, bar.left + bar.height() / 2, bar.centerY(), bar.height() * 0.42f)
        Glyphs.mic(r.canvas, bar.right - bar.height() / 2, bar.centerY(), bar.height() * 0.5f, r.c1)
        val rad = (r.h / 2 - p) * 0.42f
        round(r, r.w * 0.3f, r.h * 0.74f, rad, "ai")
        round(r, r.w * 0.7f, r.h * 0.74f, rad, "tr")
    }

    fun actionsGrid(r: RenderCtx) {
        val rad = min(r.w, r.h) * 0.17f
        val d = rad * 1.25f
        round(r, r.w / 2 - d, r.h / 2 - d, rad, "ai")
        round(r, r.w / 2 + d, r.h / 2 - d, rad, "mic")
        round(r, r.w / 2 - d, r.h / 2 + d, rad, "lens")
        round(r, r.w / 2 + d, r.h / 2 + d, rad, "tr")
    }

    fun big(r: RenderCtx) {
        val p = r.pad
        val bar = RectF(p, p, r.w - p, r.h * 0.48f)
        r.pill(bar, withAlpha(r.c1, 34))
        r.hit(bar, HitAction.Launch(Intents.search(r.context, 0)))
        engineIcon(r, 0, bar.left + bar.height() / 2, bar.centerY(), bar.height() * 0.4f)
        r.text(r.t("Search Google", "ابحث في جوجل"), bar.left + bar.height(), bar.centerY(), 14 * r.u * r.s2, r.c1, r.sans)
        Glyphs.mic(r.canvas, bar.right - bar.height() / 2, bar.centerY(), bar.height() * 0.42f, r.c1)
        r.hit(RectF(bar.right - bar.height(), bar.top, bar.right, bar.bottom), HitAction.Launch(Intents.voice(r.context)))
        val kinds = listOf("ai", "tr", "lens", "map")
        val step = (r.w - 2 * p) / 4
        kinds.forEachIndexed { i, k -> round(r, p + step * (i + 0.5f), r.h * 0.76f, min(step, r.h * 0.4f) * 0.34f, k) }
    }
}

// =====================================================================================  SYSTEM
object SystemR {
    private fun row(r: RenderCtx, kind: Int, y: Float, x: Float, width: Float, s: com.smrh.zujaj.data.SysInfo, size: Float = 11f) {
        val (label, frac, extra) = when (kind) {
            0 -> Triple(r.t("Battery", "البطارية") + " " + r.n("${s.battery}%"), s.battery / 100f, if (s.charging) r.t("Charging", "يشحن") else "")
            1 -> Triple(r.t("Storage", "التخزين") + " " + r.n("${s.storageUsedPct}%"), s.storageUsedPct / 100f, r.n(String.format(java.util.Locale.US, "%.1fGB", s.storageFreeGb)) + r.t(" free", " متاح"))
            else -> Triple(r.t("RAM", "الرام") + " " + r.n("${s.ramUsedPct}%"), s.ramUsedPct / 100f, r.n(String.format(java.util.Locale.US, "%.1fGB", s.ramFreeGb)) + r.t(" free", " متاح"))
        }
        val lw = r.text(label, x, y - 9 * r.u, size * r.u * r.s1, r.c1, r.sansMed)
        if (extra.isNotEmpty()) r.text(extra, x + lw + 8 * r.u, y - 9 * r.u, (size - 3) * r.u * r.s2, r.c2, r.sans)
        val ix = x + width - 7 * r.u
        when (kind) {
            0 -> Glyphs.battery(r.canvas, ix, y - 9 * r.u, 16 * r.u, r.c1, frac)
            1 -> Glyphs.storage(r.canvas, ix, y - 9 * r.u, 16 * r.u, r.c1)
            else -> Glyphs.chip(r.canvas, ix, y - 9 * r.u, 16 * r.u, r.c1)
        }
        r.progressBar(x, y + 7 * r.u, width, frac, r.c1, withAlpha(r.c1, 40), 4 * r.u)
        r.hit(RectF(x, y - 20 * r.u, x + width, y + 14 * r.u), HitAction.Launch(when (kind) { 0 -> Intents.battery(); 1 -> Intents.storage(); else -> Intents.settings() }))
    }

    fun single(r: RenderCtx, kind: Int) {
        val s = SystemRepo.read(r.context)
        row(r, kind, r.h / 2, r.pad + 8 * r.u, r.w - 2 * r.pad - 16 * r.u, s)
    }

    fun info(r: RenderCtx) {
        val s = SystemRepo.read(r.context)
        val x = r.pad + 6 * r.u
        val w = r.w - 2 * x
        for (k in 0..2) row(r, k, r.h * (0.24f + 0.29f * k), x, w, s, 10f)
    }

    fun device(r: RenderCtx) {
        val s = SystemRepo.read(r.context)
        val x = r.pad + 6 * r.u
        val w = r.w - 2 * x
        r.text("${s.brand} · ${s.model}", x, r.pad + 6 * r.u, 13 * r.u * r.s1, r.c1, r.sansBold, maxW = w)
        r.text(s.device, x, r.pad + 22 * r.u, 9 * r.u, r.c2, r.sans, maxW = w)
        for (k in 0..2) row(r, k, r.pad + 54 * r.u + k * 38 * r.u, x, w, s, 10f)
        val y = r.h - r.pad - 34 * r.u
        r.text(r.n("Android ${s.android} · API ${s.sdk}"), x, y, 11 * r.u, r.c1, r.sansMed)
        r.text("${s.build} · ${r.t("Patch", "التحديث")} ${r.n(s.patch)}", x, y + 13 * r.u, 8.5f * r.u, r.c2, r.sans, maxW = w)
        r.text(r.n(String.format(java.util.Locale.US, "%.1f°C · %.1fV · %s · %s", s.tempC, s.voltage, s.tech, s.health)), x, y + 28 * r.u, 10 * r.u, r.c1, r.sans, maxW = w)
    }

    fun ring(r: RenderCtx) {
        val s = SystemRepo.read(r.context)
        val cx = r.w / 2; val cy = r.h / 2
        val rad = min(r.w, r.h) / 2 - r.pad - 4 * r.u
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        val col = if (s.battery <= 20) 0xFFE5484D.toInt() else r.accentOr(r.c1)
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 36), 9 * r.u))
        r.canvas.drawArc(rect, -90f, 360f * s.battery / 100f, false, r.strokePaint(col, 9 * r.u))
        r.text(r.n("${s.battery}%"), cx, cy - 4 * r.u, 28 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(if (s.charging) r.t("Charging", "يشحن") else r.t("Battery", "البطارية"), cx, cy + 20 * r.u, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
        r.hit(rect, HitAction.Launch(Intents.battery()))
    }
}

// =====================================================================================  QUOTES
object QuoteR {
    fun custom(r: RenderCtx) {
        val text = r.cfg.text.ifBlank { r.t("Your favorite quote here", "اكتب اقتباسك المفضل هنا") }
        val author = r.cfg.author.ifBlank { r.t("Author Name", "اسم الكاتب") }
        r.text("”", r.w - r.pad - 22 * r.u, r.h - r.pad - 4 * r.u, 70 * r.u, withAlpha(r.c1, 50), F.PLAYFAIR, Paint.Align.CENTER)
        r.paragraph(text, RectF(r.pad + 10 * r.u, r.pad, r.w - r.pad - 10 * r.u, r.h * 0.66f), 17 * r.u * r.s1, r.c1, if (r.ar) F.AMIRI else F.SANS_MED)
        r.text("— $author —", r.w / 2, r.h * 0.76f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
    }

    fun text(r: RenderCtx) {
        val t = r.cfg.text.ifBlank { r.t("Write anything you want right on your home screen", "اكتب أي شيء تريده على شاشتك الرئيسية") }
        r.paragraph(t, RectF(r.pad + 8 * r.u, r.pad, r.w - r.pad - 8 * r.u, r.h - r.pad), 16 * r.u * r.s1, r.c1, r.sansMed)
    }

    fun calli(r: RenderCtx) {
        val t = r.cfg.text.ifBlank { "وَقُل رَّبِّ زِدْنِي عِلْمًا" }
        r.paragraph(t, RectF(r.pad, r.pad * 0.6f, r.w - r.pad, r.h - r.pad * 0.6f), 40 * r.u * r.s1, r.c1, r.calliFont, lineMult = 1.0f)
    }

    fun sticky(r: RenderCtx) {
        val p = r.pad
        val title = r.cfg.title.ifBlank { r.t("Note", "ملاحظة") }
        r.text(title, if (r.ar) r.w - p else p, p + 6 * r.u, 14 * r.u * r.s1, r.c1, r.sansBold, if (r.ar) Paint.Align.RIGHT else Paint.Align.LEFT, maxW = r.w - 2 * p)
        val body = r.cfg.text.ifBlank { r.t("Tap edit to write your note…", "اضغط تعديل واكتب ملاحظتك…") }
        r.paragraph(body, RectF(p, p + 20 * r.u, r.w - p, r.h - p), 13 * r.u * r.s2, r.c2, r.sans,
            if (r.ar) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL, vCenter = false)
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("edit"))
    }
}

// =====================================================================================  PHOTO
object PhotoR {
    private fun pick(r: RenderCtx): android.graphics.Bitmap? {
        val ps = r.cfg.photos
        if (ps.isEmpty()) return null
        val idx = (r.today.toEpochDay() % ps.size).toInt()
        return AppIcons.loadFile(ps[idx])
    }

    private fun empty(r: RenderCtx, rect: RectF, rad: Float) {
        Renderer.drawArtPlaceholder(r.canvas, rect, rad)
        r.text(r.t("Tap to add photos", "اضغط لإضافة صور"), rect.centerX(), rect.centerY(), 12 * r.u, Color.WHITE, r.sansMed, Paint.Align.CENTER, maxW = rect.width() * 0.9f)
        r.hit(rect, HitAction.Cmd("edit"))
    }

    fun frame(r: RenderCtx) {
        val rect = RectF(r.pad, r.pad, r.w - r.pad, r.h - r.pad)
        val rad = max(0f, Renderer.cornerPx(r) - r.pad * 0.6f)
        val b = pick(r)
        if (b == null) empty(r, rect, rad) else Renderer.drawImage(r.canvas, b, rect, rad)
    }

    fun polaroid(r: RenderCtx) {
        val p = 8 * r.u
        r.canvas.drawRoundRect(RectF(0f, 0f, r.w, r.h), Renderer.cornerPx(r), Renderer.cornerPx(r), r.fillPaint(0xFFF7F5F0.toInt()))
        val rect = RectF(p, p, r.w - p, r.h * 0.78f)
        val b = pick(r)
        if (b == null) empty(r, rect, 6 * r.u) else Renderer.drawImage(r.canvas, b, rect, 6 * r.u)
        r.text(r.cfg.text.ifBlank { r.t("Memories", "ذكريات") }, r.w / 2, r.h * 0.89f, 13 * r.u, 0xFF333333.toInt(), (if (r.ar) r.sansMed else F.PLAYFAIR), Paint.Align.CENTER, maxW = r.w - 2 * p)
    }

    fun caption(r: RenderCtx) {
        val rect = RectF(0f, 0f, r.w, r.h)
        val b = pick(r)
        if (b == null) { empty(r, rect, 0f); return }
        Renderer.drawImage(r.canvas, b, rect, 0f)
        val g = Paint(); g.shader = LinearGradient(0f, r.h * 0.45f, 0f, r.h, Color.TRANSPARENT, withAlpha(Color.BLACK, 200), Shader.TileMode.CLAMP)
        r.canvas.drawRect(rect, g)
        r.text(r.cfg.text.ifBlank { r.t("Add a caption", "أضف تعليقاً") }, 16 * r.u, r.h - 22 * r.u, 15 * r.u * r.s1, Color.WHITE, r.sansBold, maxW = r.w - 32 * r.u)
    }
}

// =====================================================================================  COUNTERS
object CounterR {
    private fun target(r: RenderCtx): Long {
        if (r.cfg.targetMillis > 0) return r.cfg.targetMillis
        val ny = LocalDate.of(r.today.year + 1, 1, 1)
        return ny.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun title(r: RenderCtx) = r.cfg.title.ifBlank { if (r.cfg.targetMillis > 0) r.t("Add Title", "أضف عنواناً") else r.t("New Year", "السنة الجديدة") }

    private fun daysLeft(r: RenderCtx): Long {
        val t = java.time.Instant.ofEpochMilli(target(r)).atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(r.today, t).coerceAtLeast(0)
    }

    fun countdownBanner(r: RenderCtx) {
        r.text(title(r), r.pad + 10 * r.u, r.h / 2, 11 * r.u * r.s2, r.c2, r.sans, maxW = r.w * 0.3f)
        r.text(r.n(daysLeft(r).toString()), r.w / 2, r.h / 2, 26 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        r.text(r.t("Days Left", "يوم متبقي"), r.w - r.pad - 10 * r.u, r.h / 2, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
    }

    fun countdownSmall(r: RenderCtx) {
        r.text(title(r), r.w / 2, r.h * 0.2f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.circle(r.w / 2, r.h * 0.5f, r.h * 0.2f, withAlpha(r.c1, 40))
        r.text(r.n(daysLeft(r).toString()), r.w / 2, r.h * 0.5f, 24 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.h * 0.36f)
        r.text(r.t("Days Left", "يوم متبقي"), r.w / 2, r.h * 0.82f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
    }

    fun countdownLine(r: RenderCtx) {
        val cx = r.pad + 44 * r.u
        r.circle(cx, r.h / 2, 32 * r.u, withAlpha(r.c1, 40))
        r.text(r.n(daysLeft(r).toString()), cx, r.h / 2, 24 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER, maxW = 56 * r.u)
        val lx = cx + 50 * r.u
        r.canvas.drawLine(lx, r.h * 0.3f, lx, r.h * 0.7f, r.strokePaint(withAlpha(r.c1, 50), r.u))
        val x = lx + 16 * r.u
        r.text(title(r), x, r.h * 0.36f, 15 * r.u * r.s1, r.c1, r.sansMed, maxW = r.w - x - r.pad)
        val t = java.time.Instant.ofEpochMilli(target(r)).atZone(ZoneId.systemDefault()).toLocalDate()
        r.text("${Calendars.monthName(t.monthValue, r.ar)} ${r.n(t.dayOfMonth)}, ${r.n(t.year)}", x, r.h * 0.55f, 10 * r.u * r.s2, r.c2, r.sans)
        r.liveCountdown(target(r), r.t("in ", "بعد "), x, r.h * 0.7f, 10 * r.u * r.s2, r.c2)
    }

    private fun incBar(r: RenderCtx, rect: RectF) {
        r.canvas.drawRoundRect(rect, rect.height() * 0.3f, rect.height() * 0.3f, r.fillPaint(withAlpha(r.c1, 50)))
        Glyphs.plus(r.canvas, rect.centerX(), rect.centerY(), rect.height() * 0.8f, r.c1)
        r.hit(rect, HitAction.Cmd("counter", "inc"))
    }

    fun counter(r: RenderCtx) {
        r.text(r.cfg.title.ifBlank { r.t("Add Title", "أضف عنواناً") }, r.w / 2, r.h * 0.2f, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.text(r.n(r.cfg.counter.toString().padStart(3, '0')), r.w / 2, r.h * 0.46f, 30 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        incBar(r, RectF(r.pad, r.h * 0.68f, r.w - r.pad, r.h - r.pad * 0.8f))
        val m = RectF(r.w - r.pad - 22 * r.u, r.pad * 0.5f, r.w - r.pad * 0.5f, r.pad * 0.5f + 22 * r.u)
        Glyphs.minus(r.canvas, m.centerX(), m.centerY(), 14 * r.u, r.c2)
        r.hit(m, HitAction.Cmd("counter", "dec"))
    }

    fun counterWide(r: RenderCtx) {
        r.text(r.cfg.title.ifBlank { r.t("Add Title", "أضف عنواناً") }, r.w / 2, r.h * 0.2f, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
        r.text(r.n(r.cfg.counter.toString().padStart(3, '0')), r.w / 2, r.h * 0.46f, 34 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        incBar(r, RectF(r.pad, r.h * 0.7f, r.w - r.pad, r.h - r.pad * 0.7f))
        val m = RectF(r.w - r.pad - 26 * r.u, r.pad * 0.5f, r.w - r.pad * 0.5f, r.pad * 0.5f + 26 * r.u)
        Glyphs.minus(r.canvas, m.centerX(), m.centerY(), 16 * r.u, r.c2)
        r.hit(m, HitAction.Cmd("counter", "dec"))
    }

    fun habit(r: RenderCtx) {
        val d = r.today
        val done = r.cfg.days.toSet()
        val title = r.cfg.title.ifBlank { r.t("Habit", "عادة") }
        val tw = min(r.w * 0.28f, r.measure(title, 13 * r.u * r.s1, r.sansMed) + 4 * r.u)
        r.text(title, r.pad + 6 * r.u, r.h * 0.4f, 13 * r.u * r.s1, r.c1, r.sansMed, maxW = r.w * 0.28f)
        // streak
        var streak = 0
        var k = d.toEpochDay()
        while (done.contains(k)) { streak++; k-- }
        r.text(r.t("${r.n(streak)} day streak", "${r.n(streak)} يوم متتالي"), r.pad + 6 * r.u, r.h * 0.66f, 9 * r.u, r.c2, r.sans, maxW = r.w * 0.28f)
        val start = r.pad + 6 * r.u + tw + 10 * r.u
        val cw = (r.w - r.pad - 4 * r.u - start) / 7
        val rad = min(cw, r.h) * 0.3f
        for (i in 0 until 7) {
            val day = d.minusDays((6 - i).toLong())
            val cx = start + cw * (i + 0.5f)
            val cy = r.h * 0.44f
            val isDone = done.contains(day.toEpochDay())
            if (isDone) r.circle(cx, cy, rad, r.accentOr(r.c1))
            else r.canvas.drawCircle(cx, cy, rad, r.strokePaint(withAlpha(r.c1, if (day == d) 200 else 90), 2 * r.u))
            r.text(Calendars.dayName(day, r.ar).take(if (r.ar) 2 else 1), cx, r.h * 0.8f, 8.5f * r.u, if (day == d) r.c1 else r.c2, r.sans, Paint.Align.CENTER)
            r.hit(RectF(cx - cw / 2, 0f, cx + cw / 2, r.h), HitAction.Cmd("habit", day.toEpochDay().toString()))
        }
    }

    fun yearProgress(r: RenderCtx) {
        val d = r.today
        val frac = d.dayOfYear.toFloat() / d.lengthOfYear()
        val cx = r.w / 2; val cy = r.h / 2
        val rad = min(r.w, r.h) / 2 - r.pad - 4 * r.u
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 36), 8 * r.u))
        r.canvas.drawArc(rect, -90f, 360f * frac, false, r.strokePaint(r.accentOr(r.c1), 8 * r.u))
        r.text(r.n("${(frac * 100).toInt()}%"), cx, cy - 4 * r.u, 26 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(r.n(d.year), cx, cy + 20 * r.u, 10 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER)
    }
}

// =====================================================================================  ISLAMIC
object IslamicR {
    val adhkar = listOf(
        "سبحان الله وبحمده، سبحان الله العظيم",
        "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير",
        "اللهم صلِّ وسلم على نبينا محمد",
        "أستغفر الله العظيم وأتوب إليه",
        "لا حول ولا قوة إلا بالله",
        "سبحان الله، والحمد لله، ولا إله إلا الله، والله أكبر",
        "اللهم إنك عفوٌّ تحب العفو فاعفُ عني",
        "رضيت بالله ربًّا، وبالإسلام دينًا، وبمحمد ﷺ نبيًّا",
        "اللهم أعنّي على ذكرك وشكرك وحسن عبادتك",
        "يا حي يا قيوم برحمتك أستغيث",
        "سبحان الله وبحمده عدد خلقه ورضا نفسه وزنة عرشه ومداد كلماته",
        "اللهم إني أسألك علمًا نافعًا ورزقًا طيبًا وعملًا متقبلًا",
    )

    fun tasbeeh(r: RenderCtx) {
        val goal = r.cfg.goal.coerceAtLeast(1)
        val count = r.cfg.counter
        val inRound = count % goal
        val rounds = count / goal
        val cx = r.w / 2; val cy = r.h * 0.54f
        val rad = min(r.w, r.h) * 0.33f
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.text(r.cfg.text.ifBlank { "سبحان الله" }, cx, r.pad + 8 * r.u, 12 * r.u * r.s2, r.c2, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad - 30 * r.u)
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 36), 6 * r.u))
        r.canvas.drawArc(rect, -90f, 360f * inRound / goal, false, r.strokePaint(r.accentOr(r.c1), 6 * r.u))
        r.text(r.n(inRound.toString()), cx, cy - 3 * r.u, 30 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(r.n("× $rounds  ·  $goal"), cx, cy + 20 * r.u, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.hit(RectF(0f, r.pad + 18 * r.u, r.w, r.h), HitAction.Cmd("tasbeeh", "inc"))
        val rs = RectF(r.w - r.pad - 20 * r.u, r.pad * 0.4f, r.w - r.pad * 0.4f, r.pad * 0.4f + 22 * r.u)
        r.canvas.drawArc(RectF(rs.centerX() - 6 * r.u, rs.centerY() - 6 * r.u, rs.centerX() + 6 * r.u, rs.centerY() + 6 * r.u), 30f, 300f, false, r.strokePaint(r.c2, 1.6f * r.u))
        r.hit(rs, HitAction.Cmd("tasbeeh", "reset"))
    }

    fun tasbeehWide(r: RenderCtx) {
        val goal = r.cfg.goal.coerceAtLeast(1)
        val count = r.cfg.counter
        val inRound = count % goal
        r.paragraph(r.cfg.text.ifBlank { "أستغفر الله" }, RectF(r.w * 0.45f, r.pad, r.w - r.pad, r.h - r.pad), 34 * r.u * r.s2, r.c1, r.calliFont)
        val cx = r.pad + 64 * r.u; val cy = r.h / 2
        val rad = min(r.h / 2 - r.pad, 60 * r.u)
        val rect = RectF(cx - rad, cy - rad, cx + rad, cy + rad)
        r.canvas.drawArc(rect, 0f, 360f, false, r.strokePaint(withAlpha(r.c1, 36), 6 * r.u))
        r.canvas.drawArc(rect, -90f, 360f * inRound / goal, false, r.strokePaint(r.accentOr(r.c1), 6 * r.u))
        r.text(r.n(inRound.toString()), cx, cy - 4 * r.u, 30 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(r.n("× ${count / goal}"), cx, cy + 20 * r.u, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("tasbeeh", "inc"))
        val rs = RectF(r.pad * 0.4f, r.pad * 0.4f, r.pad * 0.4f + 24 * r.u, r.pad * 0.4f + 24 * r.u)
        r.canvas.drawArc(RectF(rs.centerX() - 6 * r.u, rs.centerY() - 6 * r.u, rs.centerX() + 6 * r.u, rs.centerY() + 6 * r.u), 30f, 300f, false, r.strokePaint(r.c2, 1.6f * r.u))
        r.hit(rs, HitAction.Cmd("tasbeeh", "reset"))
    }

    fun dhikr(r: RenderCtx) {
        val t = adhkar[(r.today.dayOfYear) % adhkar.size]
        r.text(r.t("Dhikr of the day", "ذكر اليوم"), r.w / 2, r.pad + 4 * r.u, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.paragraph(t, RectF(r.pad + 6 * r.u, r.pad + 16 * r.u, r.w - r.pad - 6 * r.u, r.h - r.pad), 24 * r.u * r.s1, r.c1, F.AMIRI, lineMult = 1.25f)
    }

    /** 0 = new moon … 0.5 = full … 1 */
    fun moonAge(d: LocalDate): Double {
        val known = LocalDate.of(2000, 1, 6) // a new moon
        val days = ChronoUnit.DAYS.between(known, d) + 0.75
        val syn = 29.530588853
        return ((days % syn) + syn) % syn / syn
    }

    fun moon(r: RenderCtx) {
        val age = moonAge(r.today)
        val cx = r.w / 2; val cy = r.h * 0.42f
        val rad = min(r.w, r.h) * 0.26f
        r.circle(cx, cy, rad, withAlpha(r.c1, 28))
        // lit part
        val path = Path()
        val lit = Path()
        val steps = 64
        val phase = age * 2 * Math.PI
        val k = cos(phase).toFloat() // 1 new → -1 full
        val waxing = age < 0.5
        for (i in 0..steps) {
            val a = Math.PI * i / steps - Math.PI / 2
            val y = cy + (sin(a) * rad).toFloat()
            val xEdge = (cos(a) * rad).toFloat()
            val x = if (waxing) cx + xEdge else cx - xEdge
            if (i == 0) lit.moveTo(x, y) else lit.lineTo(x, y)
        }
        for (i in steps downTo 0) {
            val a = Math.PI * i / steps - Math.PI / 2
            val y = cy + (sin(a) * rad).toFloat()
            val xEdge = (cos(a) * rad * k).toFloat()
            val x = if (waxing) cx + xEdge else cx - xEdge
            lit.lineTo(x, y)
        }
        lit.close()
        path.addPath(lit)
        r.canvas.drawPath(path, r.fillPaint(0xFFF3EFD9.toInt()))
        val names = if (r.ar) listOf("محاق", "هلال متزايد", "تربيع أول", "أحدب متزايد", "بدر", "أحدب متناقص", "تربيع أخير", "هلال متناقص")
        else listOf("New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent")
        val idx = ((age * 8 + 0.5).toInt()) % 8
        r.text(names[idx], cx, r.h * 0.78f, 11 * r.u * r.s2, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        val h = Calendars.hijri(r.today)
        r.text("${r.n(h.day)} ${Calendars.hijriMonth(h.month, r.ar)}", cx, r.h * 0.9f, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun qibla(r: RenderCtx) {
        val deg = PrayerRepo.qibla()
        val cx = r.w / 2; val cy = r.h * 0.45f
        val rad = min(r.w, r.h) * 0.3f
        r.canvas.drawCircle(cx, cy, rad, r.strokePaint(withAlpha(r.c1, 70), 2 * r.u))
        for (i in 0 until 36) {
            val a = Math.toRadians(i * 10.0 - 90)
            val l = if (i % 9 == 0) 0.82f else 0.9f
            r.canvas.drawLine(cx + (cos(a) * rad * l).toFloat(), cy + (sin(a) * rad * l).toFloat(), cx + (cos(a) * rad).toFloat(), cy + (sin(a) * rad).toFloat(), r.strokePaint(withAlpha(r.c1, 120), r.u))
        }
        r.text(r.t("N", "ش"), cx, cy - rad - 9 * r.u, 10 * r.u, r.accentOr(r.c1), r.sansBold, Paint.Align.CENTER)
        val a = Math.toRadians(deg - 90)
        val tx = cx + (cos(a) * rad * 0.7f).toFloat(); val ty = cy + (sin(a) * rad * 0.7f).toFloat()
        r.canvas.drawLine(cx, cy, tx, ty, r.strokePaint(r.c1, 3 * r.u))
        Glyphs.kaaba(r.canvas, tx, ty, 18 * r.u, r.c1)
        r.circle(cx, cy, 3 * r.u, r.c1)
        r.text(r.n("${deg.toInt()}°") + r.t(" from North", " من الشمال"), cx, r.h * 0.84f, 10 * r.u * r.s2, r.c1, r.sansMed, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
        r.text(Prefs.cityName.ifBlank { r.t("Qibla", "القبلة") }, cx, r.h * 0.94f, 8.5f * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = r.w - 2 * r.pad)
    }

    fun ramadan(r: RenderCtx) {
        val today = r.today
        val h = Calendars.hijri(today)
        if (h.month == 9) {
            r.textTight("رمضان", r.w * 0.3f, r.h / 2, r.h * 0.5f, r.c1, r.calliFont, maxW = r.w * 0.4f)
            r.text(r.t("Day ${h.day}", "اليوم ${r.n(h.day)}"), r.w * 0.72f, r.h / 2, 20 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
            return
        }
        var days = 0
        var d = today
        while (days < 400) {
            val hh = Calendars.hijri(d)
            if (hh.month == 9 && hh.day == 1) break
            d = d.plusDays(1); days++
        }
        r.textTight("رمضان", r.pad + 60 * r.u, r.h / 2, r.h * 0.5f, r.c1, r.calliFont, maxW = 100 * r.u)
        r.text(r.n(days.toString()), r.w * 0.62f, r.h / 2, 28 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(r.t("days left", "يوماً متبقياً"), r.w - r.pad - 8 * r.u, r.h / 2, 11 * r.u * r.s2, r.c2, r.sans, Paint.Align.RIGHT)
    }

    fun hijriBig(r: RenderCtx) {
        val h = Calendars.hijri(r.today)
        r.textTight(r.n(h.day), r.w / 2, r.h * 0.34f, 54 * r.u * r.s1, r.c1, F.SANS_BOLD)
        r.textTight(Calendars.hijriMonth(h.month, true), r.w / 2, r.h * 0.66f, 30 * r.u * r.s2, r.c1, r.calliFont, maxW = r.w - 2 * r.pad, maxH = r.h * 0.24f)
        r.text(r.n("${h.year}") + r.t(" AH", " هـ"), r.w / 2, r.h * 0.88f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
    }
}

// =====================================================================================  TOOLS
object ToolsR {
    fun share(r: RenderCtx) {
        Glyphs.share(r.canvas, r.w / 2, r.h * 0.4f, 36 * r.u, r.accentOr(0xFFE7B75F.toInt()))
        r.text(r.t("Share the app", "شارك التطبيق"), r.w / 2, r.h * 0.66f, 12 * r.u, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(r.t("With friends", "مع أصدقائك"), r.w / 2, r.h * 0.78f, 9.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("share"))
    }

    fun tutorials(r: RenderCtx) {
        Glyphs.playCircle(r.canvas, r.w / 2, r.h * 0.4f, 38 * r.u, 0xFFE7B75F.toInt())
        r.text(r.t("Tutorials", "الشرح"), r.w / 2, r.h * 0.66f, 12 * r.u, r.c1, r.sansBold, Paint.Align.CENTER)
        r.text(r.t("Quick start guide", "دليل البدء السريع"), r.w / 2, r.h * 0.78f, 9.5f * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.hit(RectF(0f, 0f, r.w, r.h), HitAction.Cmd("tutorial"))
    }

    fun sunArc(r: RenderCtx) {
        val times = PrayerRepo.timesFor(r.today)
        val rise = times[1].time; val set = times[4].time
        val now = r.now.toInstant().toEpochMilli()
        val frac = ((now - rise).toFloat() / (set - rise)).coerceIn(0f, 1f)
        val left = r.pad + 20 * r.u; val right = r.w - r.pad - 20 * r.u
        val base = r.h * 0.66f
        val ry = (base - r.pad - 12 * r.u).coerceAtMost((right - left) / 2)
        val rect = RectF(left, base - ry, right, base + ry)
        val arc = r.strokePaint(withAlpha(r.c1, 60), 2 * r.u).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(5 * r.u, 5 * r.u), 0f) }
        r.canvas.save(); r.canvas.clipRect(0f, 0f, r.w, base)
        r.canvas.drawArc(rect, 180f, 180f, false, arc)
        r.canvas.drawArc(rect, 180f, 180f * frac, false, r.strokePaint(r.accentOr(0xFFFFC53D.toInt()), 2.5f * r.u))
        r.canvas.restore()
        r.canvas.drawLine(r.pad, base, r.w - r.pad, base, r.strokePaint(withAlpha(r.c1, 50), r.u))
        val a = Math.toRadians(180.0 + 180.0 * frac)
        val sx = rect.centerX() + (cos(a) * rect.width() / 2).toFloat()
        val sy = rect.centerY() + (sin(a) * rect.height() / 2).toFloat()
        if (now in rise..set) Glyphs.sun(r.canvas, sx, sy, 22 * r.u, 0xFFFFC53D.toInt())
        else Glyphs.moon(r.canvas, r.w / 2, base - 26 * r.u, 22 * r.u, 0xFFF3EFD9.toInt())
        val f = java.text.SimpleDateFormat(if (r.cfg.hour24) "HH:mm" else "hh:mm", java.util.Locale.US)
        r.text(r.t("Sunrise", "الشروق"), left, base + 14 * r.u, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.text(r.n(f.format(java.util.Date(rise))), left, base + 28 * r.u, 13 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        r.text(r.t("Sunset", "الغروب"), right, base + 14 * r.u, 9 * r.u, r.c2, r.sans, Paint.Align.CENTER)
        r.text(r.n(f.format(java.util.Date(set))), right, base + 28 * r.u, 13 * r.u * r.s1, r.c1, r.sansMed, Paint.Align.CENTER)
        val dayLen = (set - rise) / 60000
        r.text(r.t("Daylight ", "طول النهار ") + r.n("${dayLen / 60}:${(dayLen % 60).toString().padStart(2, '0')}"), r.w / 2, base + 22 * r.u, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
    }

    fun quick(r: RenderCtx) {
        val items = listOf("torch", "alarm", "camera", "calc", "wifi")
        val step = (r.w - 2 * r.pad) / items.size
        val rad = min(step * 0.46f, r.h * 0.40f)
        items.forEachIndexed { i, k ->
            val cx = r.pad + step * (i + 0.5f); val cy = r.h / 2
            r.circle(cx, cy, rad, withAlpha(r.c1, 34))
            r.canvas.drawCircle(cx, cy, rad - r.u * 0.5f, r.strokePaint(withAlpha(r.c1, 22), r.u))
            val s = rad * 1.05f
            when (k) {
                "torch" -> {
                    val path = Path().apply {
                        moveTo(cx - s * 0.22f, cy - s * 0.4f); lineTo(cx + s * 0.22f, cy - s * 0.4f); lineTo(cx + s * 0.22f, cy - s * 0.18f)
                        lineTo(cx + s * 0.08f, cy + s * 0.02f); lineTo(cx + s * 0.08f, cy + s * 0.42f); lineTo(cx - s * 0.08f, cy + s * 0.42f)
                        lineTo(cx - s * 0.08f, cy + s * 0.02f); lineTo(cx - s * 0.22f, cy - s * 0.18f); close()
                    }
                    r.canvas.drawPath(path, r.fillPaint(r.c1))
                    r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Cmd("torch"))
                }
                "alarm" -> {
                    r.canvas.drawCircle(cx, cy + s * 0.04f, s * 0.32f, r.strokePaint(r.c1, s * 0.08f))
                    r.canvas.drawLine(cx, cy + s * 0.04f, cx, cy - s * 0.14f, r.strokePaint(r.c1, s * 0.08f))
                    r.canvas.drawLine(cx, cy + s * 0.04f, cx + s * 0.12f, cy + s * 0.12f, r.strokePaint(r.c1, s * 0.08f))
                    r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.alarms(r.context)))
                }
                "camera" -> {
                    r.canvas.drawRoundRect(RectF(cx - s * 0.38f, cy - s * 0.24f, cx + s * 0.38f, cy + s * 0.3f), s * 0.08f, s * 0.08f, r.strokePaint(r.c1, s * 0.08f))
                    r.canvas.drawCircle(cx, cy + s * 0.03f, s * 0.13f, r.strokePaint(r.c1, s * 0.08f))
                    r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.camera()))
                }
                "calc" -> {
                    r.canvas.drawRoundRect(RectF(cx - s * 0.28f, cy - s * 0.36f, cx + s * 0.28f, cy + s * 0.36f), s * 0.06f, s * 0.06f, r.strokePaint(r.c1, s * 0.08f))
                    r.canvas.drawLine(cx - s * 0.14f, cy - s * 0.18f, cx + s * 0.14f, cy - s * 0.18f, r.strokePaint(r.c1, s * 0.07f))
                    for (gx in -1..1 step 2) for (gy in 0..1) r.circle(cx + gx * s * 0.11f, cy + s * 0.04f + gy * s * 0.16f, s * 0.04f, r.c1)
                    r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.calculator()))
                }
                "wifi" -> {
                    for (j in 1..3) {
                        val rr = s * 0.14f * j
                        r.canvas.drawArc(RectF(cx - rr, cy + s * 0.22f - rr, cx + rr, cy + s * 0.22f + rr), 225f, 90f, false, r.strokePaint(r.c1, s * 0.07f))
                    }
                    r.circle(cx, cy + s * 0.22f, s * 0.05f, r.c1)
                    r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), HitAction.Launch(Intents.wifi()))
                }
            }
        }
    }
}

@Suppress("unused")
private fun unused() = ceil(1.0)
