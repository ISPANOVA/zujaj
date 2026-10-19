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
        for (name in listOf("person.jpg", "dog.jpg")) {
            val f = File(base, name)
            if (!f.exists()) { log.append("$name missing\n"); continue }
            val photo = BitmapFactory.decodeFile(f.path)
            val t0 = System.currentTimeMillis()
            val r = runBlocking { Segment.run(ctx, photo) }
            log.append("$name ${photo.width}x${photo.height}: ${r.javaClass.simpleName} ${if (r is Segment.Result.Fail) r.why else ""} in ${System.currentTimeMillis() - t0} ms\n")
            val mask = (r as? Segment.Result.Ok)?.mask
            val layers = listOf(
                WallLayer(kind = "clock_tall", cx = 0.5f, cy = 0.28f, w = 0.8f, behind = true),
                WallLayer(kind = "date_greg", cx = 0.5f, cy = 0.1f, w = 0.6f),
                WallLayer(kind = "day_calli", cx = 0.5f, cy = 0.62f, w = 0.6f, behind = false),
            )
            val d = WallDesign(layers = layers)
            val img = WallRender.compose(ctx, d, photo, mask, 1080, 2340)
            FileOutputStream(File(out, "wall_$name.png")).use { img.compress(Bitmap.CompressFormat.PNG, 90, it) }
            mask?.let { m -> FileOutputStream(File(out, "mask_$name.png")).use { m.compress(Bitmap.CompressFormat.PNG, 90, it) } }
        }
        File(out, "wall.txt").writeText(log.toString())
    }
}
