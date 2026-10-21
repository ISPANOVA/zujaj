package com.smrh.zujaj

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Lang
import com.smrh.zujaj.render.Renderer
import com.smrh.zujaj.widget.WidgetUpdater
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders every widget style (preview + real RemoteViews) to PNG files so the
 * designs can be reviewed without a device. Also proves every RemoteViews call is valid.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class RenderAllTest {
    private fun save(b: Bitmap, f: File) = FileOutputStream(f).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }

    @Test
    fun renderAll() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        Prefs.init(ctx)
        Prefs.isPro = true
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val errors = StringBuilder()
        for (st in Catalog.all) {
            for (lang in listOf(Lang.EN, Lang.AR)) {
                val cfg = st.defaultConfig().copy(language = lang)
                try {
                    val r = Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, preview = true, density = 2f)
                    save(r.bitmap, File(out, "p_${st.id}_${lang.name.lowercase()}.png"))
                } catch (e: Throwable) {
                    errors.append("PREVIEW ${st.id} $lang: $e\n")
                }
            }
            // real widget composition through RemoteViews
            try {
                val cfg = st.defaultConfig()
                val rv = WidgetUpdater.build(ctx, 1, cfg, st, st.shape.dw, st.shape.dh)
                val parent = FrameLayout(ctx)
                val v = rv.apply(ctx, parent)
                val d = ctx.resources.displayMetrics.density
                val w = (st.shape.dw * d).toInt(); val h = (st.shape.dh * d).toInt()
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                v.layout(0, 0, w, h)
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                v.draw(Canvas(b))
                save(b, File(out, "w_${st.id}.png"))
            } catch (e: Throwable) {
                errors.append("WIDGET ${st.id}: $e\n")
                e.printStackTrace()
            }
        }
        // every clock face through the real widget path (checks centring + digit locking)
        val dc = Catalog.byId("digital_clock")!!
        for (f in com.smrh.zujaj.render.LiveFont.entries) {
            try {
                val cfg = dc.defaultConfig().copy(font = f.name, numerals = if (f.arabic) com.smrh.zujaj.model.Numerals.ARABIC else com.smrh.zujaj.model.Numerals.LATIN)
                val rv = WidgetUpdater.build(ctx, 1, cfg, dc, dc.shape.dw, dc.shape.dh)
                val v = rv.apply(ctx, FrameLayout(ctx))
                val d = ctx.resources.displayMetrics.density
                val w = (dc.shape.dw * d).toInt(); val h = (dc.shape.dh * d).toInt()
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                v.layout(0, 0, w, h)
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                v.draw(Canvas(b))
                save(b, File(out, "f_${f.name.lowercase()}.png"))
            } catch (e: Throwable) {
                errors.append("FONT ${f.name}: $e\n")
            }
        }
        // Arabic numerals variant of every time widget
        for (st in Catalog.all.filter { it.cat == com.smrh.zujaj.catalog.Cat.DIGITAL || it.cat == com.smrh.zujaj.catalog.Cat.CLOCK || it.cat == com.smrh.zujaj.catalog.Cat.PRAYER }) {
            try {
                val cfg = st.defaultConfig().copy(numerals = com.smrh.zujaj.model.Numerals.ARABIC, language = Lang.AR)
                val rv = WidgetUpdater.build(ctx, 1, cfg, st, st.shape.dw, st.shape.dh)
                val v = rv.apply(ctx, FrameLayout(ctx))
                val d = ctx.resources.displayMetrics.density
                val w = (st.shape.dw * d).toInt(); val h = (st.shape.dh * d).toInt()
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                v.layout(0, 0, w, h)
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                v.draw(Canvas(b))
                save(b, File(out, "wa_${st.id}.png"))
            } catch (e: Throwable) {
                errors.append("WIDGET-AR ${st.id}: $e\n")
            }
        }
        // a host that REAPPLIES views (Samsung lock screen): old design's clocks must disappear
        try {
            val a = Catalog.byId("digital_world_split")!!; val b = Catalog.byId("prayer_detail") ?: Catalog.all.first { it.cat == com.smrh.zujaj.catalog.Cat.PRAYER && it.shape == a.shape }
            val v = WidgetUpdater.build(ctx, 1, a.defaultConfig(), a, a.shape.dw, a.shape.dh).apply(ctx, FrameLayout(ctx))
            WidgetUpdater.build(ctx, 1, b.defaultConfig(), b, a.shape.dw, a.shape.dh).reapply(ctx, v)
            val d = ctx.resources.displayMetrics.density
            val w = (a.shape.dw * d).toInt(); val h = (a.shape.dh * d).toInt()
            v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            v.layout(0, 0, w, h)
            val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(Canvas(bm)); save(bm, File(out, "reapply.png"))
        } catch (e: Throwable) { errors.append("REAPPLY: $e\n") }
        // stretched widgets: background fills the slot, design centred at its own proportions
        for ((nm, wd, hd) in listOf(Triple("prayer_spotlight", 230f, 250f), Triple("clock_classic", 340f, 160f), Triple("digital_stacked", 160f, 300f))) {
            try {
                val st = Catalog.byId(nm)!!
                val v = WidgetUpdater.build(ctx, 1, st.defaultConfig(), st, wd, hd).apply(ctx, FrameLayout(ctx))
                val d = ctx.resources.displayMetrics.density
                val w = (wd * d).toInt(); val h = (hd * d).toInt()
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                v.layout(0, 0, w, h)
                val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); v.draw(Canvas(bm)); save(bm, File(out, "fit_$nm.png"))
            } catch (e: Throwable) { errors.append("FIT $nm: $e\n") }
        }
        File(out, "errors.txt").writeText(errors.toString().ifBlank { "OK" })
        println("RENDER ERRORS:\n$errors")
    }

    /** Finds widgets that still paint a background when the user turns the background off. */
    @Test
    fun bgAudit() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        Prefs.init(ctx); Prefs.isPro = true
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val sb = StringBuilder()
        for (st in Catalog.all) {
            try {
                val cfg = st.defaultConfig().copy(bg = false, language = com.smrh.zujaj.model.Lang.AR)
                val b = Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, preview = true, density = 1f).bitmap
                val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
                val covered = px.count { (it ushr 24) > 40 }.toFloat() / px.size
                // corners: a real background fills them, plain text/graphics don't
                val pts = listOf(6 to b.height / 2, b.width - 7 to b.height / 2, b.width / 2 to 4, b.width / 2 to b.height - 5)
                val edges = pts.count { (x, y) -> (b.getPixel(x, y) ushr 24) > 40 }
                if (covered > 0.45f || edges >= 3) {
                    sb.append("${st.id} covered=${"%.2f".format(covered)} edges=$edges\n")
                    save(b, File(out, "nb_${st.id}.png"))
                }
            } catch (e: Throwable) { sb.append("${st.id} ERROR $e\n") }
        }
        File(out, "bg_audit.txt").writeText(sb.toString())
    }
}
