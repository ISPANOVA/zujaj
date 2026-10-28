package com.smrh.zujaj.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.smrh.zujaj.R
import com.smrh.zujaj.catalog.Cat
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.catalog.Shape
import com.smrh.zujaj.catalog.Upd
import com.smrh.zujaj.catalog.WidgetStyle
import com.smrh.zujaj.data.MediaRepo
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.ContactAction
import com.smrh.zujaj.model.ContactRef
import com.smrh.zujaj.model.Tap
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.DateR
import com.smrh.zujaj.render.Glyphs
import com.smrh.zujaj.render.Hands
import com.smrh.zujaj.remote.Flags
import com.smrh.zujaj.render.HitAction
import com.smrh.zujaj.render.LiveAnalog
import com.smrh.zujaj.render.LiveChrono
import com.smrh.zujaj.render.LiveClock
import com.smrh.zujaj.render.LiveFont
import com.smrh.zujaj.render.Renderer
import com.smrh.zujaj.ui.MainActivity

object WidgetUpdater {
    val providers = listOf(
        SmallProvider::class.java to Shape.SMALL,
        WideProvider::class.java to Shape.WIDE,
        BannerProvider::class.java to Shape.BANNER,
        LargeProvider::class.java to Shape.LARGE,
        TallProvider::class.java to Shape.TALL,
        XlProvider::class.java to Shape.XL,
    )

    fun providerFor(shape: Shape): Class<*> = providers.first { it.second == shape }.first

    fun shapeOf(ctx: Context, id: Int): Shape {
        val info = AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id) ?: return Shape.SMALL
        val cls = info.provider.className
        return providers.firstOrNull { it.first.name == cls }?.second ?: Shape.SMALL
    }

    fun allIds(ctx: Context): List<Int> {
        val m = AppWidgetManager.getInstance(ctx)
        return providers.flatMap { m.getAppWidgetIds(ComponentName(ctx, it.first)).toList() }
    }

    fun updateAll(ctx: Context) {
        allIds(ctx).forEach { runCatching { update(ctx, it) } }
    }

    fun updateWhere(ctx: Context, pred: (WidgetStyle) -> Boolean) {
        allIds(ctx).forEach { id ->
            val st = Prefs.widgetConfig(id)?.let { Catalog.byId(it.styleId) } ?: return@forEach
            if (pred(st)) runCatching { update(ctx, id) }
        }
    }

    fun updateCategory(ctx: Context, what: String) {
        val u = when (what) {
            "media" -> Upd.MEDIA
            "weather" -> Upd.WEATHER
            "prayer" -> Upd.PRAYER
            "system" -> Upd.SYSTEM
            else -> Upd.DAILY
        }
        updateWhere(ctx) { it.upd.contains(u) }
    }

    /** Resolve the config for a widget, creating a sensible default if missing. */
    fun configFor(ctx: Context, id: Int): WidgetConfig {
        Prefs.widgetConfig(id)?.let { return it }
        val shape = shapeOf(ctx, id)
        val pending = Prefs.takePending(null)
        val cfg = if (pending != null && Catalog.byId(pending.styleId) != null) pending
        else Catalog.all.first { it.shape == shape }.defaultConfig()
        Prefs.saveWidgetConfig(id, cfg)
        return cfg
    }

    /** Widget size in dp (portrait). */
    fun sizeDp(ctx: Context, id: Int, shape: Shape): Pair<Float, Float> {
        val o: Bundle = AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id)
        var w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH).toFloat()
        var h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT).toFloat()
        if (w < 30 || h < 30) {
            w = shape.cols * 80f
            h = shape.rows * 80f
        }
        return w to h
    }

    fun update(ctx: Context, id: Int) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val cfg = configFor(ctx, id)
        val style = Catalog.byId(cfg.styleId) ?: return
        val (wDp, hDp) = sizeDp(ctx, id, style.shape)
        val views = build(ctx, id, cfg, style, wDp, hDp)
        mgr.updateAppWidget(id, views)
    }

    /** Lock-screen hosts don't load app fonts, so Arabic-Indic digits must be drawn into the image. */
    fun isKeyguard(ctx: Context, id: Int): Boolean = runCatching {
        AppWidgetManager.getInstance(ctx).getAppWidgetOptions(id)
            .getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1) == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD
    }.getOrDefault(false)

    /** True when the phone's own clock digits differ from what this widget wants and the host ignores our fonts. */
    fun needsBake(ctx: Context, id: Int, cfg: WidgetConfig): Boolean {
        val wantArabic = cfg.numerals == com.smrh.zujaj.model.Numerals.ARABIC
        val systemArabic = runCatching { android.icu.text.DecimalFormatSymbols.getInstance(android.icu.util.ULocale.getDefault()).digits[0] != '0' }.getOrDefault(false)
        return wantArabic != systemArabic && isKeyguard(ctx, id)
    }

    /** Widgets whose time is drawn into the picture and redrawn every minute. */
    fun bakes(ctx: Context, id: Int, cfg: WidgetConfig): Boolean =
        needsBake(ctx, id, cfg) || (Catalog.byId(cfg.styleId)?.bakeTime?.invoke(cfg) == true)

    private fun resetSlots(rv: RemoteViews) {
        Slots.clocks.values.forEach { list -> list.forEach { (w, _) -> rv.setViewVisibility(w, View.GONE) } }
        Slots.chronos.forEach { (w, v) -> rv.setViewVisibility(w, View.GONE); rv.setChronometer(v, 0L, null, false) }
        Slots.hits.forEach { (w, v) -> rv.setViewVisibility(w, View.GONE); rv.setOnClickPendingIntent(v, null) }
        rv.setViewVisibility(R.id.an_wrap, View.GONE)
        rv.setViewVisibility(R.id.fx, View.GONE)
        rv.setImageViewBitmap(R.id.fx, null)
    }

    /** The widget blurred behind a dark veil, a crown and "Pro ended — tap to renew". */
    fun lockedPicture(ctx: Context, src: android.graphics.Bitmap): android.graphics.Bitmap {
        val bw = src.width; val bh = src.height
        // cheap blur: shrink a lot, then scale back up with filtering (twice for smoothness)
        val small = android.graphics.Bitmap.createScaledBitmap(src, (bw / 14).coerceAtLeast(2), (bh / 14).coerceAtLeast(2), true)
        val mid = android.graphics.Bitmap.createScaledBitmap(small, (bw / 4).coerceAtLeast(2), (bh / 4).coerceAtLeast(2), true)
        val out = android.graphics.Bitmap.createBitmap(bw, bh, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out)
        // keep the original rounded shape: draw the blur only where the widget has pixels
        val shape = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        c.drawBitmap(src, 0f, 0f, shape)
        shape.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        shape.isFilterBitmap = true
        c.drawBitmap(mid, null, android.graphics.Rect(0, 0, bw, bh), shape)
        shape.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_ATOP)
        shape.color = 0xA60B0B12.toInt(); shape.isFilterBitmap = false
        c.drawRect(0f, 0f, bw.toFloat(), bh.toFloat(), shape)
        val ar = Prefs.isArabicUi
        val m = minOf(bw, bh).toFloat()
        val s = m * 0.16f
        val gold = 0xFFE7B75F.toInt()
        val cy = bh / 2f
        val font = com.smrh.zujaj.render.Fonts.get(ctx, if (ar) com.smrh.zujaj.render.F.CAIRO_BOLD else com.smrh.zujaj.render.F.SANS_BOLD)
        val title = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt(); textSize = m * 0.10f; textAlign = android.graphics.Paint.Align.CENTER; typeface = font
        }
        val sub = android.graphics.Paint(title).apply { color = 0xFFCFCFD6.toInt(); textSize = m * 0.072f }
        val t1 = if (ar) "انتهى البرو" else "Pro ended"
        val t2 = if (ar) "اضغط للتجديد" else "Tap to renew"
        val maxW = bw * 0.86f
        if (title.measureText(t1) > maxW) title.textSize *= maxW / title.measureText(t1)
        if (sub.measureText(t2) > maxW) sub.textSize *= maxW / sub.measureText(t2)
        val total = s * 1.1f + title.textSize * 1.3f + sub.textSize * 1.5f
        var y = cy - total / 2
        // crown inside a soft gold circle
        c.drawCircle(bw / 2f, y + s * 0.55f, s * 0.78f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0x33E7B75F })
        Glyphs.crown(c, bw / 2f, y + s * 0.55f, s * 0.75f, gold)
        y += s * 1.1f + title.textSize * 1.05f
        c.drawText(t1, bw / 2f, y, title)
        y += sub.textSize * 1.5f
        c.drawText(t2, bw / 2f, y, sub)
        return out
    }

    internal fun build(ctx: Context, id: Int, cfg: WidgetConfig, style: WidgetStyle, wDp: Float, hDp: Float): RemoteViews {
        val density = ctx.resources.displayMetrics.density
        // Any design fits any widget size: when the placed widget's proportions differ a lot from
        // the design (e.g. a wide design in a square lock-screen slot) it is drawn at its own
        // proportions, as big as possible, centred in the slot.
        val bake = needsBake(ctx, id, cfg) || style.bakeTime(cfg)
        val res = Renderer.renderFitted(ctx, cfg, wDp, hDp, bake = bake)
        if (bake) Scheduler.minuteTick(ctx)
        val k = density / res.scale // bitmap px → device px
        val rv = RemoteViews(ctx.packageName, R.layout.widget_root)
        // Some hosts (Samsung lock screen, many launchers) REAPPLY new RemoteViews on top of the
        // old views instead of inflating fresh ones, so anything a previous design switched on
        // (extra clocks, countdowns, tap areas, overlays) would stay visible. Reset everything first.
        resetSlots(rv)
        rv.setViewPadding(R.id.bg, 0, 0, 0, 0)
        rv.setViewPadding(R.id.fx, 0, 0, 0, 0)
        if (Flags.locked(style)) {
            // Pro expired / not owned: show a lock overlay and send taps to the paywall.
            rv.setImageViewBitmap(R.id.bg, lockedPicture(ctx, res.bitmap))
            rv.setOnClickPendingIntent(R.id.bg, activity(ctx, id * 100 + 97, appMain(ctx, "paywall")))
            return rv
        }
        rv.setImageViewBitmap(R.id.bg, res.bitmap)
        val wPx = (wDp * density).toInt()
        val hPx = (hDp * density).toInt()

        fun place(wrap: Int, l: Float, t: Float, r: Float, b: Float) {
            rv.setViewVisibility(wrap, View.VISIBLE)
            rv.setViewPadding(
                wrap,
                (l * k).toInt().coerceAtLeast(0), (t * k).toInt().coerceAtLeast(0),
                (wPx - r * k).toInt().coerceAtLeast(0), (hPx - b * k).toInt().coerceAtLeast(0),
            )
        }

        val used = HashMap<Any, Int>()
        var chrono = 0
        for (live in res.lives) {
            when (live) {
                is LiveClock -> {
                    // the requested face, or the plain one of the same digit system when its slots are used up
                    val fallback = if (live.font.arabic) LiveFont.AR_CAIRO else LiveFont.SANS_BOLD
                    val face = listOf(live.font, fallback, if (live.font.arabic) LiveFont.AR_CAIRO else LiveFont.SANS)
                        .firstOrNull { used.getOrDefault(it, 0) < (Slots.clocks[it]?.size ?: 0) } ?: continue
                    val n = used.getOrDefault(face, 0)
                    used[face] = n + 1
                    val (wrap, v) = Slots.clocks.getValue(face)[n]
                    place(wrap, live.rect.left, live.rect.top, live.rect.right, live.rect.bottom)
                    rv.setCharSequence(v, "setFormat12Hour", live.pattern)
                    rv.setCharSequence(v, "setFormat24Hour", live.pattern)
                    if (!live.zone.isNullOrBlank()) rv.setString(v, "setTimeZone", live.zone)
                    rv.setTextColor(v, live.color)
                    rv.setTextViewTextSize(v, TypedValue.COMPLEX_UNIT_PX, live.sizePx * k)
                    rv.setInt(v, "setGravity", live.gravity)
                }
                is LiveChrono -> {
                    val base = if (live.arabic) 4 else if (live.bold) 2 else 0
                    val slot = base + (chrono % 2)
                    chrono++
                    val (wrap, v) = Slots.chronos[slot]
                    place(wrap, live.rect.left, live.rect.top, live.rect.right, live.rect.bottom)
                    val target = SystemClock.elapsedRealtime() + (live.target - System.currentTimeMillis())
                    rv.setChronometer(v, target, live.prefix + "%s", true)
                    rv.setChronometerCountDown(v, true)
                    rv.setTextColor(v, live.color)
                    rv.setTextViewTextSize(v, TypedValue.COMPLEX_UNIT_PX, live.sizePx * k)
                    rv.setInt(v, "setGravity", live.gravity)
                }
                is LiveAnalog -> {
                    place(R.id.an_wrap, live.rect.left, live.rect.top, live.rect.right, live.rect.bottom)
                    if (Build.VERSION.SDK_INT >= 31) {
                        val size = (live.rect.width() * k).toInt().coerceIn(64, 600)
                        val blank = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        rv.setIcon(R.id.an_clock, "setDial", Icon.createWithBitmap(blank))
                        rv.setIcon(R.id.an_clock, "setHourHand", Icon.createWithBitmap(Hands.handBitmap(size, live.hands, Hands.Which.HOUR)))
                        rv.setIcon(R.id.an_clock, "setMinuteHand", Icon.createWithBitmap(Hands.handBitmap(size, live.hands, Hands.Which.MINUTE)))
                        if (live.hands.seconds) rv.setIcon(R.id.an_clock, "setSecondHand", Icon.createWithBitmap(Hands.handBitmap(size, live.hands, Hands.Which.SECOND)))
                        if (!live.zone.isNullOrBlank()) rv.setString(R.id.an_clock, "setTimeZone", live.zone)
                    }
                }
            }
        }

        res.fx?.let {
            rv.setImageViewBitmap(R.id.fx, it)
            rv.setViewVisibility(R.id.fx, View.VISIBLE)
        }

        // Default tap on the background
        defaultTap(ctx, id, cfg, style)?.let { rv.setOnClickPendingIntent(R.id.bg, it) }

        // Hit regions
        res.hits.take(Slots.hits.size).forEachIndexed { i, hit ->
            val (wrap, v) = Slots.hits[i]
            place(wrap, hit.rect.left, hit.rect.top, hit.rect.right, hit.rect.bottom)
            pendingFor(ctx, id, i, hit.action)?.let { rv.setOnClickPendingIntent(v, it) }
        }
        return rv
    }

    // ------------------------------------------------------------------ intents
    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun editIntent(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, com.smrh.zujaj.ui.AppActivity::class.java)
            .setAction("zujaj.EDIT_$id")
            .putExtra(MainActivity.EXTRA_EDIT_WIDGET, id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(ctx, id * 100 + 99, i, FLAGS)
    }

    private fun activity(ctx: Context, req: Int, intent: Intent): PendingIntent =
        PendingIntent.getActivity(ctx, req, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), FLAGS)

    private fun broadcast(ctx: Context, req: Int, id: Int, cmd: String, arg: String): PendingIntent {
        val i = Intent(ctx, ActionReceiver::class.java)
            .setAction("zujaj.CMD.$cmd.$arg.$id")
            .putExtra("id", id).putExtra("cmd", cmd).putExtra("arg", arg)
        return PendingIntent.getBroadcast(ctx, req, i, FLAGS)
    }

    fun contactIntent(c: ContactRef): Intent {
        val digits = c.phone.filter { it.isDigit() || it == '+' }
        return when (c.action) {
            ContactAction.CALL, ContactAction.OPEN -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:$digits"))
            ContactAction.SMS -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$digits"))
            ContactAction.WHATSAPP -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits.removePrefix("+")))
        }
    }

    fun appMain(ctx: Context, extra: String? = null): Intent =
        Intent(ctx, com.smrh.zujaj.ui.AppActivity::class.java).apply { if (extra != null) { putExtra(MainActivity.EXTRA_OPEN, extra); action = "zujaj.OPEN.$extra" } }

    private fun pendingFor(ctx: Context, id: Int, slot: Int, a: HitAction): PendingIntent? {
        val req = id * 100 + slot
        return when (a) {
            is HitAction.App -> activity(ctx, req, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName(a.app.pkg, a.app.cls)))
            is HitAction.Launch -> activity(ctx, req, Intent(a.intent))
            is HitAction.Contact -> activity(ctx, req, contactIntent(a.contact))
            is HitAction.Cmd -> when (a.cmd) {
                "edit" -> editIntent(ctx, id)
                "share" -> activity(ctx, req, Intent.createChooser(shareIntent(ctx), null))
                "tutorial" -> activity(ctx, req, appMain(ctx, "tutorial"))
                "media" -> when {
                    a.arg == "open" -> activity(ctx, req, MediaRepo.launchIntent(ctx) ?: appMain(ctx, "media_access"))
                    a.arg.startsWith("vol") -> broadcast(ctx, req, id, a.cmd, a.arg)
                    !MediaRepo.hasAccess(ctx) -> activity(ctx, req, appMain(ctx, "media_access"))
                    else -> broadcast(ctx, req, id, a.cmd, a.arg)
                }
                else -> broadcast(ctx, req, id, a.cmd, a.arg)
            }
        }
    }

    fun shareIntent(ctx: Context): Intent = Intent(Intent.ACTION_SEND).setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "Zujaj – glass widgets for Android\nhttps://play.google.com/store/apps/details?id=${ctx.packageName}")

    private fun defaultTap(ctx: Context, id: Int, cfg: WidgetConfig, style: WidgetStyle): PendingIntent? {
        val req = id * 100 + 98
        return when (cfg.tap) {
            Tap.NONE -> null
            Tap.EDIT -> editIntent(ctx, id)
            Tap.OPEN_APP -> cfg.tapApp?.let { activity(ctx, req, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(ComponentName(it.pkg, it.cls))) }
                ?: editIntent(ctx, id)
            Tap.DEFAULT -> when (style.cat) {
                Cat.CLOCK, Cat.DIGITAL -> activity(ctx, req, com.smrh.zujaj.render.Intents.alarms(ctx))
                Cat.DATE -> activity(ctx, req, DateR.calendarIntent())
                Cat.MEDIA -> activity(ctx, req, MediaRepo.launchIntent(ctx) ?: appMain(ctx, "media_access"))
                Cat.WEATHER, Cat.PRAYER, Cat.ISLAMIC -> activity(ctx, req, appMain(ctx, "settings"))
                else -> editIntent(ctx, id)
            }
        }
    }
}
