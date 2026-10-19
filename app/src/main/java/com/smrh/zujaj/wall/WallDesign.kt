package com.smrh.zujaj.wall

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** One thing placed on the wallpaper. Position/size are fractions of the screen. */
@Serializable
data class WallLayer(
    val id: Long = System.nanoTime(),
    /** clock_tall, clock_thin, clock_stack, day_calli, month_greg, month_hijri, date_pair, date_greg, date_hijri,
     *  hijri_full, dual_full, prayer_next, prayer_in, prayer_row, location, text, widget */
    val kind: String,
    val styleId: String = "",
    val text: String = "",
    val color: Int = 0xFFFFFFFF.toInt(),
    val cx: Float = 0.5f,
    val cy: Float = 0.3f,
    /** width as a fraction of the screen width */
    val w: Float = 0.6f,
    val behind: Boolean = false,
    val alpha: Int = 255,
    val shadow: Boolean = true,
    /** second colour; 0 = solid colour */
    val color2: Int = 0,
    /** gradient direction: 0 top→bottom, 1 left→right, 2 diagonal */
    val gDir: Int = 0,
)

@Serializable
data class WallDesign(
    val image: String = "",
    val mask: String = "",
    val layers: List<WallLayer> = emptyList(),
    val blur: Int = 0,
    val dim: Int = 0,
) {
    fun toJson() = J.encodeToString(this)

    companion object {
        private val J = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        private fun file(ctx: Context) = File(ctx.filesDir, "wall/design.json")
        fun dir(ctx: Context) = File(ctx.filesDir, "wall").apply { mkdirs() }
        fun load(ctx: Context): WallDesign = runCatching { J.decodeFromString<WallDesign>(file(ctx).readText()) }.getOrDefault(WallDesign())
        fun save(ctx: Context, d: WallDesign) { dir(ctx); file(ctx).writeText(d.toJson()) }
    }
}
