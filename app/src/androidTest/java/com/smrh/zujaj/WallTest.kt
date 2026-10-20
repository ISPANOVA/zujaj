package com.smrh.zujaj

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.wall.Segment
import com.smrh.zujaj.wall.WallDesign
import com.smrh.zujaj.wall.WallLayer
import com.smrh.zujaj.wall.WallRender
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/** Depth wallpaper on a real device: cut out the subject and put the clock behind it. */
@RunWith(AndroidJUnit4::class)
class WallTest {
    @Test
    fun depth() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        Prefs.init(ctx)
        val base = ctx.getExternalFilesDir(null)!!
        val out = File(base, "device").apply { mkdirs() }
        val log = StringBuilder()
        var domeMask: Bitmap? = null
        val testCtx = InstrumentationRegistry.getInstrumentation().context
        for (name in listOf("dome.jpg", "person.jpg", "dog.jpg")) {
            val f = File(base, name)
            val photo = if (name == "dome.jpg") testCtx.assets.open(name).use { BitmapFactory.decodeStream(it) }
                else { if (!f.exists()) { log.append("$name missing\n"); continue }; BitmapFactory.decodeFile(f.path) }
            val t0 = System.currentTimeMillis()
            val r = runBlocking { Segment.run(ctx, photo) }
            log.append("coverage=${(r as? Segment.Result.Ok)?.mask?.let { WallRender.coverage(it) }} ")
            log.append("$name ${photo.width}x${photo.height}: ${r.javaClass.simpleName} ${if (r is Segment.Result.Fail) r.why else ""} in ${System.currentTimeMillis() - t0} ms\n")
            val mask = (r as? Segment.Result.Ok)?.mask
            if (name == "dome.jpg") domeMask = mask
            val layers = listOf(
                WallLayer(kind = "clock_tall", cx = 0.5f, cy = if (name == "dome.jpg") 0.36f else 0.28f, w = 0.8f, behind = true, color2 = 0xFFE7B75F.toInt()),
                WallLayer(kind = "date_greg", cx = 0.5f, cy = 0.1f, w = 0.6f),
                WallLayer(kind = "day_calli", cx = 0.5f, cy = 0.62f, w = 0.6f, behind = false),
            )
            val auto = layers.map { l -> val (a, b) = WallRender.autoColors(photo, l, 1080, 2340); log.append("  auto ${l.kind}: #%08X → #%08X\n".format(a, b)); l.copy(color = a, color2 = b) }
            FileOutputStream(File(out, "auto_$name.png")).use { WallRender.compose(ctx, WallDesign(layers = auto), photo, mask, 1080, 2340).compress(Bitmap.CompressFormat.PNG, 90, it) }
            val d = WallDesign(layers = layers)
            val img = WallRender.compose(ctx, d, photo, mask, 1080, 2340)
            FileOutputStream(File(out, "wall_$name.png")).use { img.compress(Bitmap.CompressFormat.PNG, 90, it) }
            mask?.let { m -> FileOutputStream(File(out, "mask_$name.png")).use { m.compress(Bitmap.CompressFormat.PNG, 90, it) } }
        }
        // photo smaller than the screen: each fill mode + glass layers
        val dome = testCtx.assets.open("dome.jpg").use { BitmapFactory.decodeStream(it) }
        for (mode in 0..3) {
            val d = WallDesign(zoom = 0.62f, py = 0.12f, fillMode = mode, layers = listOf(
                WallLayer(kind = "clock_tall", cx = 0.5f, cy = 0.2f, w = 0.8f, glassFill = true),
                WallLayer(kind = "date_greg", cx = 0.5f, cy = 0.07f, w = 0.55f, glass = true),
            ))
            FileOutputStream(File(out, "fill_$mode.png")).use { WallRender.compose(ctx, d, dome, null, 540, 1170).compress(Bitmap.CompressFormat.PNG, 90, it) }
        }
        // templates on the dome photo
        for (t in com.smrh.zujaj.wall.WallTemplates.all) {
            runCatching {
                val d = com.smrh.zujaj.wall.WallTemplates.apply(t, WallDesign(), dome, 540, 1170, domeMask != null)
                FileOutputStream(File(out, "tpl_${t.id}.png")).use { WallRender.compose(ctx, d, dome, domeMask, 540, 1170).compress(Bitmap.CompressFormat.PNG, 90, it) }
            }.onFailure { log.append("template ${t.id} FAILED $it\n") }
        }
        // photo looks
        for (i in WallRender.filters.indices) {
            val d = WallDesign(filter = i, topShade = if (i == 0) 50 else 0)
            FileOutputStream(File(out, "filter_$i.png")).use { WallRender.compose(ctx, d, dome, null, 270, 585).compress(Bitmap.CompressFormat.PNG, 90, it) }
        }
        // new widgets, drawn over the photo so glass shows what it does
        val photoFile = File(base, "dome_copy.jpg").also { f -> FileOutputStream(f).use { dome.compress(Bitmap.CompressFormat.JPEG, 90, it) } }
        val backdrop = WallRender.compose(ctx, WallDesign(), dome, null, 720, 1560)
        val widgets = listOf("event_ring", "event_card", "event_flip", "event_banner", "photo_verse", "photo_verse_sq", "photo_verse_lg", "prayer_hub", "prayer_hub@liquid", "prayer_hub@tint", "digital_stacked@liquid", "event_card@ar")
        for (key in widgets) {
            runCatching {
                val (id, mode) = key.split("@").let { it[0] to it.getOrElse(1) { "" } }
                val st = com.smrh.zujaj.catalog.Catalog.byId(id) ?: com.smrh.zujaj.catalog.Catalog.all.first { it.shape == com.smrh.zujaj.catalog.Shape.SMALL }
                var cfg = st.defaultConfig().copy(photos = listOf(photoFile.path))
                if (mode == "liquid") cfg = com.smrh.zujaj.ui.WidgetThemes.all.first { it.id == "liquid" }.apply(cfg)
                if (mode == "tint") cfg = cfg.copy(dayTint = true)
                if (mode == "ar") cfg = cfg.copy(language = com.smrh.zujaj.model.Lang.AR)
                val res = com.smrh.zujaj.render.Renderer.render(ctx, cfg, st.shape.dw, st.shape.dh, true, 2.5f)
                val pad = 40
                val img = Bitmap.createBitmap(res.bitmap.width + 2 * pad, res.bitmap.height + 2 * pad, Bitmap.Config.ARGB_8888)
                val c = android.graphics.Canvas(img)
                c.drawBitmap(backdrop, null, android.graphics.Rect(0, 0, img.width, img.height), null)
                c.drawBitmap(res.bitmap, pad.toFloat(), pad.toFloat(), null)
                FileOutputStream(File(out, "w_${key.replace('@', '_')}.png")).use { img.compress(Bitmap.CompressFormat.PNG, 90, it) }
            }.onFailure { log.append("widget $key FAILED $it\n") }
        }
        File(out, "wall.txt").writeText(log.toString())
    }
}
