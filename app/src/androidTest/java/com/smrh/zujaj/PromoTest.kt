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
        render(ctx, "ar")
        english(ctx)
    }

    /** English set: no Arabic and nothing religious — a mountain photo and everyday widgets. */
    private fun english(ctx: android.content.Context) {
        Prefs.appLanguage = "en"
        val out = File(ctx.getExternalFilesDir(null), "device/promo_en").apply { deleteRecursively(); mkdirs() }
        fun save(b: Bitmap, n: String) = FileOutputStream(File(out, "$n.png")).use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val testCtx = InstrumentationRegistry.getInstrumentation().context
        val mt = testCtx.assets.open("mountains.jpg").use { BitmapFactory.decodeStream(it) }
        val photo = File(ctx.getExternalFilesDir(null), "promo_mt.jpg").also { f -> FileOutputStream(f).use { mt.compress(Bitmap.CompressFormat.JPEG, 92, it) } }
        val soon = System.currentTimeMillis() + 23L * 86_400_000L
        val ids = mapOf<String, (com.smrh.zujaj.model.WidgetConfig) -> com.smrh.zujaj.model.WidgetConfig>(
            "clock_minimal" to { it }, "clock_modern" to { it }, "clock_orbit" to { it }, "digital_stacked" to { it }, "digital_neon" to { it },
            "digital_clock" to { it }, "weather_big" to { it }, "weather_small" to { it }, "w_today_tomorrow" to { it }, "date_calendar" to { it }, "dots_year" to { it },
            "event_ring" to { it.copy(event = "birthday", targetMillis = soon) }, "event_card" to { it.copy(event = "newyear") },
            "event_flip" to { it.copy(event = "", title = "Summer trip", targetMillis = soon) }, "event_banner" to { it.copy(event = "birthday", targetMillis = soon) },
            "photo_verse" to { it.copy(variant = 2, text = "Make today count") }, "photo_verse_sq" to { it.copy(variant = 2, text = "Good vibes only") },
        )
        for ((id, f) in ids) runCatching {
            val st = Catalog.byId(id) ?: return@runCatching
            var cfg = f(st.defaultConfig().copy(photos = listOf(photo.path), language = com.smrh.zujaj.model.Lang.EN))
            save(Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, true, 4f).bitmap, "w_$id")
            cfg = com.smrh.zujaj.ui.WidgetThemes.all.first { it.id == "liquid" }.apply(cfg)
            save(Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, true, 4f).bitmap, "wl_$id")
        }
        val mask = (runBlocking { Segment.run(ctx, mt) } as? Segment.Result.Ok)?.mask ?: WallRender.skyMask(mt)
        for (t in WallTemplates.all.filter { it.id in setOf("depth", "liquid", "minimal", "noir") }) runCatching {
            val d = WallTemplates.apply(t, WallDesign(py = 0.09f), mt, 1080, 2340, mask != null)
            save(WallRender.compose(ctx, d, mt, mask, 1080, 2340), "wall_${t.id}")
        }
        save(WallRender.compose(ctx, WallDesign(), mt, null, 1080, 2340), "wall_plain")
    }

    private fun render(ctx: android.content.Context, lang: String) {
        Prefs.appLanguage = lang
        val out = File(ctx.getExternalFilesDir(null), if (lang == "ar") "device/promo" else "device/promo_en").apply { mkdirs() }
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
