package com.smrh.zujaj.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.model.WidgetConfig
import com.smrh.zujaj.render.Renderer
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Share a widget design as a short link (and a picture of it). The link carries the whole design —
 * colours, fonts, sizes — but never personal things (photos, contacts, apps, cities).
 * zujaj.pages.dev/w#CODE opens the app (or the Play Store); zujaj://w/CODE opens it directly.
 */
object ShareDesign {
    const val WEB = "https://zujaj.pages.dev/w#"

    fun encode(cfg: WidgetConfig): String {
        val clean = cfg.copy(photos = emptyList(), contacts = emptyList(), apps = emptyList(), cities = emptyList(), tapApp = null)
        val raw = clean.toJson().toByteArray()
        val d = Deflater(Deflater.BEST_COMPRESSION, true).apply { setInput(raw); finish() }
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!d.finished()) { val n = d.deflate(buf); out.write(buf, 0, n) }
        d.end()
        return Base64.encodeToString(out.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun decode(code: String): WidgetConfig? = runCatching {
        val bytes = Base64.decode(code.trim(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val inf = Inflater(true).apply { setInput(bytes) }
        val out = ByteArrayOutputStream(); val buf = ByteArray(1024)
        while (!inf.finished()) { val n = inf.inflate(buf); if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break; out.write(buf, 0, n) }
        inf.end()
        WidgetConfig.fromJson(out.toString(Charsets.UTF_8.name()))?.takeIf { Catalog.byId(it.styleId) != null }
    }.getOrNull()

    /** Finds a design code in anything pasted: a full link, zujaj://w/CODE, or the bare code. */
    fun codeFrom(text: String?): String? {
        val t = text?.trim().orEmpty()
        if (t.isEmpty()) return null
        val c = when {
            "#" in t -> t.substringAfterLast('#')
            t.startsWith("zujaj://") -> t.substringAfterLast('/')
            else -> t.substringAfterLast(' ')
        }.trim()
        return c.takeIf { decode(it) != null }
    }

    fun codeFrom(uri: Uri?): String? {
        uri ?: return null
        return when {
            uri.scheme == "zujaj" -> uri.lastPathSegment
            uri.host == "zujaj.pages.dev" -> uri.fragment ?: uri.getQueryParameter("d")
            else -> null
        }?.takeIf { decode(it) != null }
    }

    /** A clean picture of the widget on a soft card, for the share sheet. */
    private fun picture(ctx: Context, cfg: WidgetConfig, accent: Int): File {
        val st = Catalog.byId(cfg.styleId)!!
        val w = Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, preview = true, density = 3f).bitmap
        val pad = 90; val foot = 110
        val out = Bitmap.createBitmap(w.width + pad * 2, w.height + pad * 2 + foot, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, out.width.toFloat(), out.height.toFloat(), 0xFF16131F.toInt(), 0xFF0A0A10.toInt(), Shader.TileMode.CLAMP)
        })
        c.drawCircle(out.width * 0.15f, 0f, out.width * 0.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(out.width * 0.15f, 0f, out.width * 0.6f, (accent and 0x00FFFFFF) or 0x40000000, 0, Shader.TileMode.CLAMP)
        })
        c.drawBitmap(w, pad.toFloat(), pad.toFloat(), Paint(Paint.FILTER_BITMAP_FLAG))
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt(); textSize = 40f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
        c.drawText(if (com.smrh.zujaj.data.Prefs.isArabicUi) "زجاج · Zujaj" else "Zujaj · زجاج", out.width / 2f, out.height - foot / 2f - 10f, p)
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "design.png")
        f.outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return f
    }

    fun share(ctx: Context, cfg: WidgetConfig, accent: Int) {
        val link = WEB + encode(cfg)
        val text = tr("I made this widget in Zujaj — open it and it's yours to edit:\n$link",
            "عملت الويدجت دي في زجاج — افتح اللينك وهتبقى عندك وتعدّل عليها:\n$link")
        val i = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, text)
        val img = runCatching { picture(ctx, cfg, accent) }.getOrNull()
        if (img != null) {
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".share", img)
            i.type = "image/png"; i.putExtra(Intent.EXTRA_STREAM, uri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else i.type = "text/plain"
        ctx.startActivity(Intent.createChooser(i, tr("Share design", "شارك التصميم")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun copyLink(ctx: Context, cfg: WidgetConfig) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Zujaj", WEB + encode(cfg)))
    }
}
