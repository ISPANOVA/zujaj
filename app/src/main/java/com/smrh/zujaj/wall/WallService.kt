package com.smrh.zujaj.wall

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.smrh.zujaj.data.Prefs

/** Live wallpaper: draws the saved design and redraws every minute so clocks stay correct. */
class WallService : WallpaperService() {
    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine() {
        private val h = Handler(Looper.getMainLooper())
        private var visible = false
        private var photo: Bitmap? = null
        private var mask: Bitmap? = null
        private var loadedFor = ""
        private val tick = object : Runnable {
            override fun run() {
                draw()
                val now = System.currentTimeMillis()
                h.postDelayed(this, 60_000 - now % 60_000 + 200)
            }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            h.removeCallbacks(tick)
            if (v) tick.run()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) { draw() }
        override fun onDestroy() { h.removeCallbacks(tick); super.onDestroy() }

        private fun draw() {
            if (!visible) return
            val ctx = applicationContext
            Prefs.init(ctx)
            val d = WallDesign.load(ctx)
            val key = d.image + "|" + d.mask
            if (key != loadedFor) {
                photo = WallRender.loadImage(d.image); mask = WallRender.loadImage(d.mask, 1600); loadedFor = key
            }
            val holder = surfaceHolder
            val f = holder.surfaceFrame
            if (f.width() <= 0) return
            val bmp = runCatching { WallRender.compose(ctx, d, photo, mask, f.width(), f.height()) }.getOrNull() ?: return
            val c = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try { c.drawBitmap(bmp, 0f, 0f, null) } finally { holder.unlockCanvasAndPost(c) }
        }
    }
}
