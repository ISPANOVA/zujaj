package com.smrh.zujaj

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Lang
import com.smrh.zujaj.render.Renderer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Every widget as placed on a home screen at three slot sizes (its own, squarer, wider): rs_<id>_<k>.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class ResizeShotsTest {
    @Test fun sizes() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        Prefs.init(ctx); Prefs.isPro = true
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        for (st in Catalog.all) {
            val cfg = st.defaultConfig().copy(language = Lang.AR)
            val w = st.shape.dw; val h = st.shape.dh
            val slots = listOf(w to h, (w * 0.62f) to (h * 1.15f), (w * 1.0f) to (h * 0.62f))
            slots.forEachIndexed { k, (sw, sh) ->
                runCatching {
                    val b = Renderer.renderFitted(ctx, cfg, sw, sh, density = 1.5f).bitmap
                    FileOutputStream(File(out, "rs_${st.id}_$k.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
    }
}
