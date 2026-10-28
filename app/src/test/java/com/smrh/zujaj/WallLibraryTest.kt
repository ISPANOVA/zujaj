package com.smrh.zujaj

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import com.smrh.zujaj.ui.WallLibrary
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class WallLibraryTest {
    /** All 50 wallpapers on one contact sheet (10 columns × 5 rows) for review. */
    @Test fun sheet() {
        val w = 216; val h = 468
        val out = File(System.getProperty("user.dir"), "build/previews").apply { mkdirs() }
        val sheet = Bitmap.createBitmap(w * 10, h * 5, Bitmap.Config.ARGB_8888)
        val c = Canvas(sheet)
        WallLibrary.all.forEachIndexed { i, wp -> c.drawBitmap(WallLibrary.paint(wp, w, h), (i % 10 * w).toFloat(), (i / 10 * h).toFloat(), null) }
        FileOutputStream(File(out, "wl_sheet.png")).use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // one at full phone size to check detail
        FileOutputStream(File(out, "wl_full.png")).use { WallLibrary.paint(WallLibrary.all[5], 1080, 2340).compress(Bitmap.CompressFormat.PNG, 90, it) }
    }
}
