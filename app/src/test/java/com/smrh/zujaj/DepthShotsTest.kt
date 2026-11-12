package com.smrh.zujaj

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.smrh.zujaj.data.Prefs
import com.smrh.zujaj.wall.DepthScenes
import com.smrh.zujaj.wall.WallRender
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/** Every ready-made depth wallpaper with its clock: dp_<id>.png, plus one with every studio option on. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class DepthShotsTest {
    @Test fun all() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        Prefs.init(ctx)
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val W = 270; val H = 585
        for (it in DepthScenes.all) {
            runCatching {
                val pt = DepthScenes.paint(it, W, H)
                val d = DepthScenes.design(it, "", "", W, H)
                val b = WallRender.compose(ctx, d, pt.full, pt.fg, W, H)
                FileOutputStream(File(out, "dp_${it.id}.png")).use { o -> b.compress(Bitmap.CompressFormat.PNG, 100, o) }
            }.onFailure { e -> println("dp ${it.id}: $e") }
        }
        // studio options on one wallpaper
        val it = DepthScenes.all[0]
        val pt = DepthScenes.paint(it, W, H)
        val base = DepthScenes.design(it, "", "", W, H)
        val looks = listOf(
            "persp" to { l: com.smrh.zujaj.wall.WallLayer -> l.copy(persp = 1) },
            "front" to { l: com.smrh.zujaj.wall.WallLayer -> l.copy(front = 1) },
            "blend" to { l: com.smrh.zujaj.wall.WallLayer -> l.copy(blend = 3) },
            "split" to { l: com.smrh.zujaj.wall.WallLayer -> l.copy(fill = 1, font = 5, tfmt = 4) },
        )
        for ((n, f) in looks) runCatching {
            val d = base.copy(layers = base.layers.map { l -> if (l.kind.startsWith("clock_")) f(l) else l })
            val b = WallRender.compose(ctx, d, pt.full, pt.fg, W, H)
            FileOutputStream(File(out, "dpx_$n.png")).use { o -> b.compress(Bitmap.CompressFormat.PNG, 100, o) }
        }.onFailure { e -> println("dpx $n: $e") }
    }
}
