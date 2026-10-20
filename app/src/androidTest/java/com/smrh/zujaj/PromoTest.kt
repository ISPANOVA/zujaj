package com.smrh.zujaj

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smrh.zujaj.catalog.Catalog
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.render.Renderer
import com.smrh.zujaj.wall.Segment
import com.smrh.zujaj.wall.WallDesign
import com.smrh.zujaj.wall.WallRender
import com.smrh.zujaj.wall.WallTemplates
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** High-resolution pictures for the store / ads: widgets on transparent background and finished wallpapers. */
@RunWith(AndroidJUnit4::class)
class PromoTest {
    @Test
    fun promo() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.init(ctx)
        Prefs.appLanguage = "ar"
        val out = File(ctx.getExternalFilesDir(null), "device/promo").apply { mkdirs() }
        fun save(b: Bitmap, n: String) = FileOutputStream(File(out, "$n.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val testCtx = InstrumentationRegistry.getInstrumentation().context
        val dome = testCtx.assets.open("dome.jpg").use { BitmapFactory.decodeStream(it) }
        val photo = File(ctx.getExternalFilesDir(null), "promo_photo.jpg").also { f -> FileOutputStream(f).use { dome.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
        val ids = listOf("prayer_hub", "calli_lawn", "clock_arabesque", "clock_damascene", "digital_neon", "days_thuluth", "weather_big", "event_ring",
            "event_card", "event_flip", "photo_verse", "photo_verse_sq", "w_today_tomorrow", "prayer_journey", "dual_banner", "date_calendar", "clock_minimal", "digital_stacked")
        for (id in ids) runCatching {
            val st = Catalog.byId(id) ?: return@runCatching
            var cfg = st.defaultConfig().copy(photos = listOf(photo.path))
            save(Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, true, 4f).bitmap, "w_$id")
            cfg = com.smrh.zujaj.ui.WidgetThemes.all.first { it.id == "liquid" }.apply(cfg)
            save(Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, true, 4f).bitmap, "wl_$id")
        }
        val mask = (runBlocking { Segment.run(ctx, dome) } as? Segment.Result.Ok)?.mask
        for (t in WallTemplates.all) runCatching {
            val d = WallTemplates.apply(t, WallDesign(), dome, 1080, 2340, mask != null)
            save(WallRender.compose(ctx, d, dome, mask, 1080, 2340), "wall_${t.id}")
        }
        save(WallRender.compose(ctx, WallDesign(), dome, null, 1080, 2340), "wall_plain")
    }
}
