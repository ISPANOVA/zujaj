package com.smrh.zujaj

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.model.Lang
import com.smrh.zujaj.render.LiveFx
import com.smrh.zujaj.render.Renderer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Every frame of a few live widgets (for the promo video): fr_<id>_<lang>_<frame>.png. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class LiveFramesTest {
    @Test fun frames() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        Prefs.init(ctx); Prefs.isPro = true
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val ids = listOf("live_p_mihrab", "live_r_lantern", "live_p_rosette", "live_p_orb_s", "live_i_dhikr", "live_i_qibla", "live_i_crescent_s", "live_w_sky_l", "live_r_lanterns")
        for (id in ids) {
            val st = Catalog.byId(id) ?: continue
            for (lang in listOf(Lang.AR, Lang.EN)) {
                val cfg = st.defaultConfig().copy(language = lang)
                for (i in 0 until LiveFx.FRAMES) {
                    val b = LiveFx.with(LiveFx.ALL, i / LiveFx.FRAMES.toFloat()) { Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, preview = true, density = 2f).bitmap }
                    FileOutputStream(File(out, "fr_${id}_${lang.name.lowercase()}_$i.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
        }
    }
}
