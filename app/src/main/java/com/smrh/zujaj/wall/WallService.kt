package com.smrh.zujaj.wall

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.smrh.zujaj.data.Prefs

/**
 * Live wallpaper: draws the saved design and redraws every minute so clocks stay correct.
 * With parallax on, tilting the phone moves the sky, the clock and the shape by different amounts;
 * with an unlock animation set, the clock eases in each time the screen is unlocked.
 */
class WallService : WallpaperService() {
    override fun onCreateEngine(): Engine = DepthEngine()

    inner class DepthEngine : Engine(), SensorEventListener {
        private val h = Handler(Looper.getMainLooper())
        private var visible = false
        private var photo: Bitmap? = null
        private var mask: Bitmap? = null
        private var loadedFor = ""
        private var parts: WallRender.Parts? = null
        private var design: WallDesign? = null
        private var partsKey = ""
        private val sm by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
        private var tx = 0f; private var ty = 0f   // smoothed tilt, -1..1
        private var listening = false
        private var animStart = 0L
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        private val tick = object : Runnable {
            override fun run() {
                partsKey = ""   // new minute: rebuild the clock
                draw()
                val now = System.currentTimeMillis()
                h.postDelayed(this, 60_000 - now % 60_000 + 200)
            }
        }
        private val frame = object : Runnable {
            override fun run() {
                draw()
                if (visible && SystemClock.uptimeMillis() - animStart < ANIM_MS + 40) h.postDelayed(this, 16)
            }
        }
        private val unlock = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { startAnim() }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            runCatching { registerReceiver(unlock, IntentFilter(Intent.ACTION_USER_PRESENT)) }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            h.removeCallbacks(tick)
            if (v) { tick.run(); startSensor() } else stopSensor()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) { partsKey = ""; draw() }
        override fun onDestroy() {
            h.removeCallbacks(tick); h.removeCallbacks(frame); stopSensor()
            runCatching { unregisterReceiver(unlock) }
            super.onDestroy()
        }

        private fun startAnim() {
            if ((design?.anim ?: 0) == 0) return
            animStart = SystemClock.uptimeMillis()
            h.removeCallbacks(frame); h.post(frame)
        }

        private fun startSensor() {
            if (listening || (design?.parallax ?: 0) == 0) return
            val s = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            listening = sm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME)
        }
        private fun stopSensor() { if (listening) { sm.unregisterListener(this); listening = false } }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        override fun onSensorChanged(e: SensorEvent) {
            val nx = (e.values[0] / 9.81f).coerceIn(-1f, 1f)
            val ny = ((e.values[1] / 9.81f) - 0.6f).coerceIn(-1f, 1f)
            val ox = tx; val oy = ty
            tx += (nx - tx) * 0.12f; ty += (ny - ty) * 0.12f
            if (kotlin.math.abs(tx - ox) + kotlin.math.abs(ty - oy) > 0.002f) draw()
        }

        private fun draw() {
            if (!visible) return
            val ctx = applicationContext
            Prefs.init(ctx)
            // auto-change: one of the saved designs, picked by the time
            val d = WallRotation.current(ctx)?.let { WallRotation.designIn(it) } ?: WallDesign.load(ctx)
            val key = d.image + "|" + d.mask
            if (key != loadedFor) {
                photo = WallRender.loadImage(d.image); mask = WallRender.loadImage(d.mask, 1600, trim = false); loadedFor = key; partsKey = ""
            }
            val holder = surfaceHolder
            val f = holder.surfaceFrame
            if (f.width() <= 0) return
            val W = f.width(); val H = f.height()
            val pk = "$W×$H|${d.hashCode()}"
            if (pk != partsKey) {
                parts = runCatching { WallRender.parts(ctx, d, photo, mask, W, H) }.getOrNull() ?: return
                design = d; partsKey = pk
                if (d.parallax > 0) startSensor() else stopSensor()
            }
            val p = parts ?: return
            val c = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try { paint(c, p, d, W, H) } finally { holder.unlockCanvasAndPost(c) }
        }

        private fun paint(c: Canvas, p: WallRender.Parts, d: WallDesign, W: Int, H: Int) {
            val amp = floatArrayOf(0f, 0.012f, 0.022f, 0.035f)[d.parallax.coerceIn(0, 3)] * W
            fun plane(b: Bitmap?, depth: Float, scale: Float = 1f, alpha: Int = 255, dx: Float = 0f, dy: Float = 0f) {
                b ?: return
                c.save()
                c.translate(-tx * amp * depth + dx, ty * amp * depth + dy)
                if (scale != 1f) c.scale(scale, scale, W / 2f, H / 2f)
                paint.alpha = alpha
                c.drawBitmap(b, 0f, 0f, paint)
                c.restore()
            }
            // reveal on unlock
            val t = if (d.anim == 0 || animStart == 0L) 1f else ((SystemClock.uptimeMillis() - animStart) / ANIM_MS.toFloat()).coerceIn(0f, 1f)
            val e = 1f - (1f - t) * (1f - t) * (1f - t)
            val off = (1f - e) * H * 0.06f
            val (adx, ady) = when (d.anim) { 2 -> 0f to off; 3 -> 0f to -off; 4 -> off to 0f; 5 -> -off to 0f; else -> 0f to 0f }
            val ascale = if (d.anim == 1) 0.92f + 0.08f * e else 1f
            val aal = (255 * e).toInt()
            // the photo (sky and shape together) moves one way, the clock the other — that gap is the depth
            // the photo is a bit bigger than the screen so tilting never shows an edge
            plane(p.bg, -1f, if (d.parallax > 0) 1f + 2.4f * amp / W else 1f)
            plane(p.behind, 0.6f, ascale, aal, adx, ady)
            plane(p.subject, -1f, if (d.parallax > 0) 1f + 2.4f * amp / W else 1f)
            plane(p.top, 0.9f, ascale, aal, adx, ady)
        }
    }

    companion object { const val ANIM_MS = 650L }
}
