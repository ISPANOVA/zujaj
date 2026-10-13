package com.smrh.zujaj.render

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.smrh.zujaj.model.AppRef

/** Multi-line text fitted into [rect] (shrinks until it fits). */
fun RenderCtx.paragraph(
    s: String, rect: RectF, maxSize: Float, color: Int = c1, font: F = sans,
    align: Layout.Alignment = Layout.Alignment.ALIGN_CENTER, lineMult: Float = 1.1f, vCenter: Boolean = true,
) {
    if (s.isBlank()) return
    val tp = TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; typeface = tf(font); letterSpacing = cfg.spacing / 100f
    }
    var size = maxSize
    var layout: StaticLayout
    val width = rect.width().toInt().coerceAtLeast(10)
    while (true) {
        tp.textSize = size
        layout = StaticLayout.Builder.obtain(s, 0, s.length, tp, width)
            .setAlignment(align).setLineSpacing(0f, lineMult).setIncludePad(false).build()
        if (layout.height <= rect.height() || size < 7 * u) break
        size *= 0.92f
    }
    canvas.save()
    val top = if (vCenter) rect.top + (rect.height() - layout.height) / 2 else rect.top
    canvas.translate(rect.left, top)
    layout.draw(canvas)
    canvas.restore()
}

object AppIcons {
    private val cache = LruCache<String, Bitmap>(64)

    fun icon(ctx: Context, app: AppRef, sizePx: Int): Bitmap? {
        val key = "${app.pkg}/${app.cls}@$sizePx"
        cache.get(key)?.let { return it }
        val pm = ctx.packageManager
        val d = runCatching {
            if (app.cls.isNotBlank()) pm.getActivityIcon(android.content.ComponentName(app.pkg, app.cls))
            else pm.getApplicationIcon(app.pkg)
        }.getOrNull() ?: return null
        val b = d.toBitmap(sizePx.coerceAtLeast(8), sizePx.coerceAtLeast(8))
        cache.put(key, b)
        return b
    }

    fun iconForPkg(ctx: Context, pkg: String, sizePx: Int): Bitmap? =
        if (installed(ctx, pkg)) icon(ctx, AppRef(pkg, ""), sizePx) else null

    fun installed(ctx: Context, pkg: String) = runCatching { ctx.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    fun drawIcon(c: Canvas, b: Bitmap, dst: RectF) {
        c.drawBitmap(b, null, dst, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG))
    }

    fun loadFile(path: String, maxSide: Int = 900): Bitmap? = runCatching {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, o)
        var sample = 1
        while (o.outWidth / sample > maxSide * 2 || o.outHeight / sample > maxSide * 2) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}

/** Intents used by widget buttons (resolved at render time). */
object Intents {
    const val GOOGLE = "com.google.android.googlequicksearchbox"
    const val BRAVE = "com.brave.browser"
    const val SAMSUNG = "com.sec.android.app.sbrowser"
    const val TRANSLATE = "com.google.android.apps.translate"
    const val MAPS = "com.google.android.apps.maps"
    const val YOUTUBE = "com.google.android.youtube"
    const val GEMINI = "com.google.android.apps.bard"

    fun launch(ctx: Context, pkg: String, fallbackUrl: String? = null): Intent {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
        return i ?: Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl ?: "https://play.google.com/store/apps/details?id=$pkg"))
    }

    fun search(ctx: Context, engine: Int): Intent = when (engine) {
        1 -> launch(ctx, BRAVE, "https://search.brave.com")
        2 -> launch(ctx, SAMSUNG, "https://www.google.com")
        else -> if (AppIcons.installed(ctx, GOOGLE)) Intent("android.search.action.GLOBAL_SEARCH").setPackage(GOOGLE)
        else Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
    }

    fun voice(ctx: Context): Intent =
        if (AppIcons.installed(ctx, GOOGLE)) Intent("android.speech.action.WEB_SEARCH").setPackage(GOOGLE)
        else Intent("android.speech.action.WEB_SEARCH")

    fun lens(ctx: Context): Intent =
        if (AppIcons.installed(ctx, GOOGLE)) Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens")).setPackage(GOOGLE)
        else Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com"))

    fun assistant(ctx: Context): Intent =
        if (AppIcons.installed(ctx, GEMINI)) launch(ctx, GEMINI) else Intent(Intent.ACTION_ASSIST)

    fun translate(ctx: Context) = launch(ctx, TRANSLATE, "https://translate.google.com")
    fun maps(ctx: Context) = launch(ctx, MAPS, "https://maps.google.com")
    fun youtube(ctx: Context) = launch(ctx, YOUTUBE, "https://youtube.com")
    fun webSearch(q: String) = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)

    fun alarms() = Intent(AlarmClock.ACTION_SHOW_ALARMS)
    fun camera() = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
    fun calculator() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
    fun settings() = Intent(Settings.ACTION_SETTINGS)
    fun wifi() = if (android.os.Build.VERSION.SDK_INT >= 29) Intent(Settings.Panel.ACTION_WIFI) else Intent(Settings.ACTION_WIFI_SETTINGS)
    fun battery() = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
    fun storage() = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
}
