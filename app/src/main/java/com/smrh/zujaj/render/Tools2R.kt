package com.smrh.zujaj.render

import android.content.Intent
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import com.smrh.zujaj.render.RenderCtx.Companion.withAlpha
import kotlin.math.cos
import kotlin.math.sin

/** Shortcut & control designs (volume, sound mode, quick tools, places). */
object Tools2R {
    // ------------------------------------------------------------------ icons
    fun icon(r: RenderCtx, key: String, cx: Float, cy: Float, s: Float, color: Int) {
        val st = r.strokePaint(color, s * 0.11f)
        val c = r.canvas
        when (key) {
            "camera" -> {
                c.drawRoundRect(RectF(cx - s * 0.42f, cy - s * 0.26f, cx + s * 0.42f, cy + s * 0.32f), s * 0.1f, s * 0.1f, st)
                c.drawCircle(cx, cy + s * 0.03f, s * 0.14f, st)
                c.drawLine(cx - s * 0.14f, cy - s * 0.26f, cx - s * 0.08f, cy - s * 0.36f, st); c.drawLine(cx - s * 0.08f, cy - s * 0.36f, cx + s * 0.08f, cy - s * 0.36f, st); c.drawLine(cx + s * 0.08f, cy - s * 0.36f, cx + s * 0.14f, cy - s * 0.26f, st)
            }
            "wifi" -> {
                for (j in 1..3) { val rr = s * 0.15f * j; c.drawArc(RectF(cx - rr, cy + s * 0.22f - rr, cx + rr, cy + s * 0.22f + rr), 225f, 90f, false, st) }
                r.circle(cx, cy + s * 0.22f, s * 0.06f, color)
            }
            "torch" -> {
                val p = Path().apply {
                    moveTo(cx - s * 0.24f, cy - s * 0.42f); lineTo(cx + s * 0.24f, cy - s * 0.42f); lineTo(cx + s * 0.24f, cy - s * 0.2f)
                    lineTo(cx + s * 0.1f, cy); lineTo(cx + s * 0.1f, cy + s * 0.42f); lineTo(cx - s * 0.1f, cy + s * 0.42f)
                    lineTo(cx - s * 0.1f, cy); lineTo(cx - s * 0.24f, cy - s * 0.2f); close()
                }
                c.drawPath(p, st); c.drawLine(cx - s * 0.24f, cy - s * 0.28f, cx + s * 0.24f, cy - s * 0.28f, st)
            }
            "torch_off" -> { icon(r, "torch", cx, cy, s, color); c.drawLine(cx - s * 0.42f, cy - s * 0.42f, cx + s * 0.42f, cy + s * 0.42f, st) }
            "bell" -> {
                val p = Path().apply {
                    moveTo(cx - s * 0.32f, cy + s * 0.2f); quadTo(cx - s * 0.28f, cy + s * 0.06f, cx - s * 0.28f, cy - s * 0.06f)
                    quadTo(cx - s * 0.26f, cy - s * 0.38f, cx, cy - s * 0.38f); quadTo(cx + s * 0.26f, cy - s * 0.38f, cx + s * 0.28f, cy - s * 0.06f)
                    quadTo(cx + s * 0.28f, cy + s * 0.06f, cx + s * 0.32f, cy + s * 0.2f); close()
                }
                c.drawPath(p, st); c.drawLine(cx - s * 0.08f, cy + s * 0.32f, cx + s * 0.08f, cy + s * 0.32f, st)
            }
            "bell_off" -> { icon(r, "bell", cx, cy, s, color); c.drawLine(cx - s * 0.4f, cy - s * 0.4f, cx + s * 0.4f, cy + s * 0.4f, st) }
            "vibrate" -> {
                c.drawRoundRect(RectF(cx - s * 0.16f, cy - s * 0.32f, cx + s * 0.16f, cy + s * 0.32f), s * 0.06f, s * 0.06f, st)
                for (sx in listOf(-1f, 1f)) {
                    c.drawLine(cx + sx * s * 0.28f, cy - s * 0.16f, cx + sx * s * 0.28f, cy + s * 0.16f, st)
                    c.drawLine(cx + sx * s * 0.4f, cy - s * 0.08f, cx + sx * s * 0.4f, cy + s * 0.08f, st)
                }
            }
            "calc" -> {
                c.drawRoundRect(RectF(cx - s * 0.28f, cy - s * 0.36f, cx + s * 0.28f, cy + s * 0.36f), s * 0.07f, s * 0.07f, st)
                c.drawLine(cx - s * 0.14f, cy - s * 0.18f, cx + s * 0.14f, cy - s * 0.18f, st)
                for (gx in -1..1 step 2) for (gy in 0..1) r.circle(cx + gx * s * 0.11f, cy + s * 0.04f + gy * s * 0.16f, s * 0.045f, color)
            }
            "timer" -> {
                c.drawCircle(cx, cy + s * 0.05f, s * 0.32f, st)
                c.drawLine(cx, cy + s * 0.05f, cx + s * 0.12f, cy - s * 0.08f, st)
                c.drawLine(cx - s * 0.08f, cy - s * 0.38f, cx + s * 0.08f, cy - s * 0.38f, st)
            }
            "alarm" -> {
                c.drawCircle(cx, cy + s * 0.04f, s * 0.32f, st)
                c.drawLine(cx, cy + s * 0.04f, cx, cy - s * 0.14f, st); c.drawLine(cx, cy + s * 0.04f, cx + s * 0.12f, cy + s * 0.12f, st)
                c.drawLine(cx - s * 0.36f, cy - s * 0.26f, cx - s * 0.2f, cy - s * 0.38f, st); c.drawLine(cx + s * 0.36f, cy - s * 0.26f, cx + s * 0.2f, cy - s * 0.38f, st)
            }
            "translate" -> Glyphs.translate(c, cx, cy, s, color)
            "shield" -> {
                val p = Path().apply { moveTo(cx, cy - s * 0.38f); lineTo(cx + s * 0.3f, cy - s * 0.26f); lineTo(cx + s * 0.28f, cy + s * 0.06f); quadTo(cx + s * 0.22f, cy + s * 0.3f, cx, cy + s * 0.4f); quadTo(cx - s * 0.22f, cy + s * 0.3f, cx - s * 0.28f, cy + s * 0.06f); lineTo(cx - s * 0.3f, cy - s * 0.26f); close() }
                c.drawPath(p, st)
            }
            "chat" -> Glyphs.chat(c, cx, cy, s, color)
            "sliders" -> for (k in -1..1) {
                val y = cy + k * s * 0.22f
                c.drawLine(cx - s * 0.36f, y, cx + s * 0.36f, y, st)
                r.circle(cx + (if (k == 0) 0.16f else -0.14f) * s, y, s * 0.07f, color)
            }
            "food" -> {
                c.drawLine(cx - s * 0.14f, cy - s * 0.36f, cx - s * 0.14f, cy + s * 0.36f, st)
                for (k in -1..1) c.drawLine(cx - s * 0.14f + k * s * 0.08f, cy - s * 0.36f, cx - s * 0.14f + k * s * 0.08f, cy - s * 0.12f, st)
                c.drawLine(cx + s * 0.16f, cy - s * 0.36f, cx + s * 0.16f, cy + s * 0.36f, st)
                c.drawArc(RectF(cx + s * 0.06f, cy - s * 0.36f, cx + s * 0.26f, cy), 90f, 180f, false, st)
            }
            "fuel" -> {
                c.drawRoundRect(RectF(cx - s * 0.3f, cy - s * 0.34f, cx + s * 0.08f, cy + s * 0.36f), s * 0.05f, s * 0.05f, st)
                c.drawLine(cx - s * 0.22f, cy - s * 0.14f, cx, cy - s * 0.14f, st)
                c.drawLine(cx + s * 0.08f, cy - s * 0.1f, cx + s * 0.24f, cy, st); c.drawLine(cx + s * 0.24f, cy, cx + s * 0.24f, cy + s * 0.24f, st)
            }
            "pill" -> {
                c.save(); c.rotate(-40f, cx, cy)
                c.drawRoundRect(RectF(cx - s * 0.4f, cy - s * 0.16f, cx + s * 0.4f, cy + s * 0.16f), s * 0.16f, s * 0.16f, st)
                c.drawLine(cx, cy - s * 0.16f, cx, cy + s * 0.16f, st); c.restore()
            }
            "mosque" -> {
                c.drawArc(RectF(cx - s * 0.24f, cy - s * 0.3f, cx + s * 0.24f, cy + s * 0.18f), 180f, 180f, false, st)
                c.drawLine(cx - s * 0.3f, cy - s * 0.06f, cx - s * 0.3f, cy + s * 0.34f, st); c.drawLine(cx + s * 0.3f, cy - s * 0.06f, cx + s * 0.3f, cy + s * 0.34f, st)
                c.drawLine(cx - s * 0.38f, cy + s * 0.34f, cx + s * 0.38f, cy + s * 0.34f, st)
                c.drawLine(cx, cy - s * 0.3f, cx, cy - s * 0.42f, st)
            }
            "search" -> Glyphs.search(c, cx, cy, s, color)
            "mic" -> Glyphs.mic(c, cx, cy, s, color)
            "plus" -> Glyphs.plus(c, cx, cy, s, color)
            "minus" -> Glyphs.minus(c, cx, cy, s, color)
            "sparkle" -> Glyphs.sparkle(c, cx, cy, s, color)
        }
    }

    private fun audio(r: RenderCtx) = r.context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    private fun ringer(r: RenderCtx): Int = if (r.preview) AudioManager.RINGER_MODE_VIBRATE else runCatching { audio(r).ringerMode }.getOrDefault(AudioManager.RINGER_MODE_NORMAL)
    private fun volFrac(r: RenderCtx): Float = if (r.preview) 0.6f else runCatching {
        val am = audio(r); am.getStreamVolume(AudioManager.STREAM_MUSIC) / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    }.getOrDefault(0.5f)

    private fun circleBtn(r: RenderCtx, cx: Float, cy: Float, rad: Float, key: String, on: Boolean, action: HitAction) {
        r.circle(cx, cy, rad, if (on) r.accent else withAlpha(r.c1, 22))
        if (!on) r.canvas.drawCircle(cx, cy, rad - 0.5f * r.u, r.strokePaint(withAlpha(r.c1, 40), r.u))
        icon(r, key, cx, cy, rad * 0.95f, r.c1)
        r.hit(RectF(cx - rad, cy - rad, cx + rad, cy + rad), action)
    }

    // Volume slider — vertical pill with + / − and the level (S)
    fun volumeSlider(r: RenderCtx) {
        val pill = RectF(r.w * 0.42f, r.pad * 0.6f, r.w * 0.72f, r.h - r.pad * 0.6f)
        r.canvas.drawRoundRect(pill, pill.width() / 2, pill.width() / 2, r.strokePaint(withAlpha(r.c1, 70), 1.2f * r.u))
        val f = volFrac(r)
        Glyphs.plus(r.canvas, pill.centerX(), pill.top + 16 * r.u, 10 * r.u, r.c1)
        Glyphs.minus(r.canvas, pill.centerX(), pill.bottom - 16 * r.u, 10 * r.u, r.c1)
        val n = 12; val top = pill.top + 32 * r.u; val bot = pill.bottom - 32 * r.u
        for (k in 0 until n) {
            val y = bot - (bot - top) * k / (n - 1)
            r.circle(pill.centerX(), y, 2.2f * r.u, if (k < (f * n).toInt()) r.c1 else withAlpha(r.c1, 50))
        }
        r.text(r.n("${(f * 100).toInt()}%"), r.w * 0.3f, r.h / 2, 12 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
        r.hit(RectF(pill.left, pill.top, pill.right, pill.centerY()), HitAction.Cmd("media", "vol_up"))
        r.hit(RectF(pill.left, pill.centerY(), pill.right, pill.bottom), HitAction.Cmd("media", "vol_down"))
    }

    // Volume +/- — row with the level dots (S)
    fun volumeKeys(r: RenderCtx) {
        val f = volFrac(r); val cy = r.h * 0.42f
        circleBtn(r, r.pad + 20 * r.u, cy, 16 * r.u, "plus", false, HitAction.Cmd("media", "vol_up"))
        circleBtn(r, r.w - r.pad - 20 * r.u, cy, 16 * r.u, "minus", false, HitAction.Cmd("media", "vol_down"))
        r.text(r.n("${(f * 100).toInt()}%"), r.w / 2, cy - 8 * r.u, 11 * r.u, r.c1, r.sansBold, Paint.Align.CENTER)
        for (k in 0 until 8) r.circle(r.w / 2 - 14 * r.u + k * 4 * r.u, cy + 8 * r.u, 1.3f * r.u, if (k < (f * 8).toInt()) r.c1 else withAlpha(r.c1, 50))
        r.text(r.t("Volume", "الصوت"), r.w / 2, r.h * 0.82f, 10 * r.u, r.c2, r.sans, Paint.Align.CENTER)
    }

    // Sound mode — one big button that cycles ring → vibrate → silent (S)
    fun soundMode(r: RenderCtx) {
        val m = ringer(r)
        val (key, label) = when (m) {
            AudioManager.RINGER_MODE_SILENT -> "bell_off" to r.t("Silent", "صامت")
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate" to r.t("Vibrate", "اهتزاز")
            else -> "bell" to r.t("Ring", "رنين")
        }
        r.text(r.t("Sound mode", "وضع الصوت"), r.w - r.pad, r.h * 0.12f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        circleBtn(r, r.w / 2, r.h * 0.46f, r.h * 0.24f, key, true, HitAction.Cmd("ringer", "cycle"))
        r.text(label, r.w / 2, r.h * 0.85f, 13 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.CENTER)
    }

    // Sound buttons — silent / vibrate / ring, the active one filled (B)
    fun soundButtons(r: RenderCtx) {
        val m = ringer(r)
        val items = listOf(Triple("bell_off", r.t("Silent", "صامت"), AudioManager.RINGER_MODE_SILENT),
            Triple("vibrate", r.t("Vibrate", "اهتزاز"), AudioManager.RINGER_MODE_VIBRATE), Triple("bell", r.t("Ring", "رنين"), AudioManager.RINGER_MODE_NORMAL))
        val ord = Kit.ordered(r, items)
        val g = 6 * r.u; val cw = (r.w - 2 * r.pad * 0.6f - 2 * g) / 3
        for ((i, it) in ord.withIndex()) {
            val rc = RectF(r.pad * 0.6f + i * (cw + g), r.h * 0.18f, r.pad * 0.6f + i * (cw + g) + cw, r.h * 0.82f)
            val on = it.third == m
            r.canvas.drawRoundRect(rc, 14 * r.u, 14 * r.u, r.fillPaint(if (on) r.accent else withAlpha(r.c1, 18)))
            icon(r, it.first, rc.left + rc.width() * 0.3f, rc.centerY(), 15 * r.u, r.c1)
            r.text(it.second, rc.left + rc.width() * 0.48f, rc.centerY(), 11 * r.u * r.s1, r.c1, r.sansBold, maxW = rc.width() * 0.5f)
            r.hit(rc, HitAction.Cmd("ringer", when (it.third) { AudioManager.RINGER_MODE_SILENT -> "silent"; AudioManager.RINGER_MODE_VIBRATE -> "vibrate"; else -> "normal" }))
        }
    }

    // Floating keys — camera, wifi, sound, torch (B)
    fun floatingKeys(r: RenderCtx) {
        val m = ringer(r)
        val items = listOf(
            Triple("camera", r.t("Camera", "الكاميرا"), HitAction.Launch(Intents.camera())),
            Triple("wifi", r.t("Wi-Fi", "واي فاي"), HitAction.Launch(Intents.wifi())),
            Triple(if (m == AudioManager.RINGER_MODE_VIBRATE) "vibrate" else if (m == AudioManager.RINGER_MODE_SILENT) "bell_off" else "bell", r.t("Sound", "الصوت"), HitAction.Cmd("ringer", "cycle")),
            Triple("torch", r.t("Torch", "الكشاف"), HitAction.Cmd("torch")),
        )
        val ord = Kit.ordered(r, items)
        val cw = (r.w - 2 * r.pad) / ord.size
        for ((i, it) in ord.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            circleBtn(r, cx, r.h * 0.42f, minOf(cw, r.h) * 0.27f, it.first, it.first == "wifi" || it.first == "vibrate", it.third)
            r.text(it.second, cx, r.h * 0.84f, 9 * r.u * r.s2, r.c1, r.sans, Paint.Align.CENTER, maxW = cw)
        }
    }

    // Torch — one big switch (S)
    fun torch(r: RenderCtx) {
        r.text(r.t("Torch", "الكشاف"), r.w - r.pad, r.h * 0.12f, 9 * r.u, r.c2, r.sans, Paint.Align.RIGHT)
        circleBtn(r, r.w / 2, r.h * 0.48f, r.h * 0.24f, "torch_off", false, HitAction.Cmd("torch"))
        r.text(r.t("Tap to switch", "اضغط للتشغيل"), r.w / 2, r.h * 0.86f, 10 * r.u, r.c1, r.sansMed, Paint.Align.CENTER)
    }

    // Quick tools — calculator, timer, alarm, camera (B)
    fun quickTools(r: RenderCtx) {
        val items = listOf(
            Triple("calc", r.t("Calculator", "الحاسبة"), HitAction.Launch(Intents.calculator())),
            Triple("timer", r.t("Timer", "المؤقت"), HitAction.Launch(Intent(AlarmClock.ACTION_SHOW_TIMERS))),
            Triple("alarm", r.t("Alarm", "المنبه"), HitAction.Launch(Intents.alarms(r.context))),
            Triple("camera", r.t("Camera", "الكاميرا"), HitAction.Launch(Intents.camera())),
        )
        val ord = Kit.ordered(r, items)
        val cw = (r.w - 2 * r.pad) / ord.size
        for ((i, it) in ord.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.circle(cx, r.h * 0.4f, 15 * r.u, withAlpha(r.c1, 18))
            icon(r, it.first, cx, r.h * 0.4f, 15 * r.u, r.c1)
            r.text(it.second, cx, r.h * 0.8f, 9 * r.u * r.s2, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            r.hit(RectF(cx - cw / 2, 0f, cx + cw / 2, r.h), it.third)
        }
    }

    // Search & tools — search pill with mic, and four tool buttons (W)
    fun searchTools(r: RenderCtx) {
        val pill = RectF(r.pad * 0.7f, r.h * 0.1f, r.w - r.pad * 0.7f - 40 * r.u, r.h * 0.42f)
        r.pill(pill, withAlpha(r.c1, 20))
        r.circle(pill.left + pill.height() / 2, pill.centerY(), pill.height() * 0.36f, r.accent)
        Glyphs.search(r.canvas, pill.left + pill.height() / 2, pill.centerY(), pill.height() * 0.42f, r.c1)
        r.text(r.t("Search the web", "ابحث في الويب"), pill.left + pill.height() + 4 * r.u, pill.centerY(), 11 * r.u, r.c1, r.sans)
        r.hit(pill, HitAction.Launch(Intents.search(r.context, r.cfg.variant)))
        val mic = RectF(r.w - r.pad * 0.7f - 34 * r.u, pill.top, r.w - r.pad * 0.7f, pill.bottom)
        r.pill(mic, withAlpha(r.c1, 20))
        Glyphs.mic(r.canvas, mic.centerX(), mic.centerY(), 14 * r.u, r.c1)
        r.hit(mic, HitAction.Launch(Intents.voice(r.context)))
        val items = listOf(
            Triple("translate", r.t("Translate", "ترجمة"), HitAction.Launch(Intents.translate(r.context))),
            Triple("shield", r.t("Private", "خاص"), HitAction.Launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))),
            Triple("sparkle", r.t("AI", "ذكاء"), HitAction.Launch(Intents.assistant(r.context))),
            Triple("sliders", r.t("Settings", "الإعدادات"), HitAction.Launch(Intents.settings())),
        )
        val ord = Kit.ordered(r, items)
        val cw = (r.w - 2 * r.pad) / ord.size
        for ((i, it) in ord.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f)
            r.circle(cx, r.h * 0.64f, 14 * r.u, withAlpha(r.c1, 16))
            icon(r, it.first, cx, r.h * 0.64f, 14 * r.u, r.c1)
            r.text(it.second, cx, r.h * 0.88f, 8 * r.u, r.c2, r.sans, Paint.Align.CENTER, maxW = cw)
            r.hit(RectF(cx - cw / 2, r.h * 0.48f, cx + cw / 2, r.h), it.third)
        }
    }

    private fun blob(r: RenderCtx, cx: Float, cy: Float, rad: Float, color: Int, lobes: Int) {
        val p = Path()
        val n = 72
        for (k in 0..n) {
            val a = 2 * Math.PI * k / n
            val rr = rad * (1 + 0.1f * cos(lobes * a).toFloat())
            val x = cx + rr * cos(a).toFloat(); val y = cy + rr * sin(a).toFloat()
            if (k == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close(); r.canvas.drawPath(p, r.fillPaint(color))
    }

    private fun geo(q: String) = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(q)))

    // Around me — city + mini map + four nearby searches (W)
    fun aroundMe(r: RenderCtx) {
        val city = com.smrh.zujaj.data.Prefs.cityName.ifBlank { r.t("My area", "منطقتي") }
        r.text(city, r.w * 0.56f, r.h * 0.16f, 15 * r.u * r.s1, r.c1, r.sansBold, Paint.Align.RIGHT, maxW = r.w * 0.5f)
        r.text(r.t("Maps & places nearby", "الخرائط والأماكن القريبة"), r.w * 0.56f, r.h * 0.3f, 8.5f * r.u, r.c2, r.sans, Paint.Align.RIGHT, maxW = r.w * 0.5f)
        val map = RectF(r.w * 0.6f, r.pad * 0.6f, r.w - r.pad * 0.6f, r.h * 0.42f)
        r.canvas.drawRoundRect(map, 12 * r.u, 12 * r.u, r.fillPaint(0xFFE9ECEF.toInt()))
        r.canvas.save(); r.canvas.clipRect(map)
        for (k in 0..4) r.canvas.drawLine(map.left + k * 22 * r.u, map.top, map.left + k * 22 * r.u - 20 * r.u, map.bottom, r.strokePaint(0xFFFFFFFF.toInt(), 3 * r.u))
        r.canvas.drawLine(map.left, map.centerY() + 6 * r.u, map.right, map.centerY() - 4 * r.u, r.strokePaint(0xFFF5C26B.toInt(), 3 * r.u))
        r.canvas.restore()
        r.circle(map.centerX(), map.centerY(), 7 * r.u, 0xFFF08A3C.toInt()); r.circle(map.centerX(), map.centerY(), 2.6f * r.u, 0xFFFFFFFF.toInt())
        r.hit(map, HitAction.Launch(Intents.maps(r.context)))
        val items = listOf(
            Triple("food", 0xFF7B5CD6.toInt(), r.t("restaurants", "مطاعم")),
            Triple("fuel", 0xFFE08A1E.toInt(), r.t("gas station", "بنزينة")),
            Triple("pill", 0xFFE5566B.toInt(), r.t("pharmacy", "صيدلية")),
            Triple("mosque", 0xFF1F9D62.toInt(), r.t("mosque", "مسجد")),
        )
        val cw = (r.w - 2 * r.pad) / 4
        for ((i, it) in items.withIndex()) {
            val cx = r.pad + cw * (i + 0.5f); val cy = r.h * 0.72f
            blob(r, cx, cy, 18 * r.u, it.second, 5 + i % 3)
            icon(r, it.first, cx, cy, 15 * r.u, 0xFFFFFFFF.toInt())
            r.hit(RectF(cx - cw / 2, r.h * 0.5f, cx + cw / 2, r.h), HitAction.Launch(geo(it.third)))
        }
    }
}
