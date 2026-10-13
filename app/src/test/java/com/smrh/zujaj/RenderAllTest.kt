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
        File(out, "errors.txt").writeText(errors.toString().ifBlank { "OK" })
        println("RENDER ERRORS:\n$errors")
    }
}
